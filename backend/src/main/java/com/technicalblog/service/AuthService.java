package com.technicalblog.service;

import com.technicalblog.dto.request.EmailRequest;
import com.technicalblog.dto.request.LoginRequest;
import com.technicalblog.dto.request.RegisterRequest;
import com.technicalblog.dto.request.ResetPasswordRequest;
import com.technicalblog.dto.response.CurrentUserResponse;
import com.technicalblog.dto.response.LoginResponse;
import com.technicalblog.entity.AuthTokenType;
import com.technicalblog.entity.Role;
import com.technicalblog.entity.User;
import com.technicalblog.exception.ResourceNotFoundException;
import com.technicalblog.exception.TooManyRequestsException;
import com.technicalblog.repository.UserRepository;
import com.technicalblog.security.JwtService;
import com.technicalblog.security.LoginAttemptService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

    private static final Duration VERIFICATION_VALIDITY = Duration.ofHours(24);
    private static final Duration RESET_VALIDITY = Duration.ofHours(1);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;
    private final MailService mailService;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       JwtService jwtService,
                       LoginAttemptService loginAttemptService,
                       PasswordEncoder passwordEncoder,
                       AuthTokenService authTokenService,
                       MailService mailService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.loginAttemptService = loginAttemptService;
        this.passwordEncoder = passwordEncoder;
        this.authTokenService = authTokenService;
        this.mailService = mailService;
    }

    /**
     * Public sign up. New accounts always get the USER role, so an open form can never
     * hand out administrator rights, and they stay unusable until the address is confirmed.
     */
    @Transactional
    public void register(RegisterRequest request, String clientIp) {
        String email = request.email().trim().toLowerCase(Locale.ENGLISH);
        // Keyed on the address alone: a spammer uses a different email every time,
        // so counting per email would never trigger.
        guardAgainstAbuse("register|" + clientIp);

        Optional<User> existing = userRepository.findByEmailIgnoreCase(email);

        if (existing.isPresent()) {
            User account = existing.get();
            // Someone who never confirmed simply gets another link. A confirmed account
            // is left alone, and either way the caller sees the same answer, so this
            // endpoint cannot be used to find out who is registered.
            if (!account.isEmailVerified()) {
                mailService.sendVerification(account,
                        authTokenService.issue(account, AuthTokenType.EMAIL_VERIFICATION, VERIFICATION_VALIDITY));
            }
            return;
        }

        User user = userRepository.save(User.builder()
                .username(request.username().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .emailVerified(false)
                .build());

        mailService.sendVerification(user, authTokenService.issue(user, AuthTokenType.EMAIL_VERIFICATION,
                VERIFICATION_VALIDITY));
    }

    /** Turns a verification link into a usable account. */
    @Transactional
    public void verifyEmail(String token) {
        User user = authTokenService.consume(token, AuthTokenType.EMAIL_VERIFICATION);
        user.setEmailVerified(true);
    }

    /**
     * Sends another verification link. The response is the same whether or not the
     * address exists, so this cannot be used to discover who has an account.
     */
    @Transactional
    public void resendVerification(EmailRequest request, String clientIp) {
        guardAgainstAbuse("resend:" + request.email().toLowerCase(Locale.ENGLISH) + "|" + clientIp);

        userRepository.findByEmailIgnoreCase(request.email().trim())
                .filter(user -> !user.isEmailVerified())
                .ifPresent(user -> mailService.sendVerification(user,
                        authTokenService.issue(user, AuthTokenType.EMAIL_VERIFICATION, VERIFICATION_VALIDITY)));
    }

    /** Starts the reset flow. Also silent about whether the address is known. */
    @Transactional
    public void forgotPassword(EmailRequest request, String clientIp) {
        guardAgainstAbuse("forgot:" + request.email().toLowerCase(Locale.ENGLISH) + "|" + clientIp);

        userRepository.findByEmailIgnoreCase(request.email().trim())
                .ifPresent(user -> mailService.sendPasswordReset(user,
                        authTokenService.issue(user, AuthTokenType.PASSWORD_RESET, RESET_VALIDITY)));
    }

    /** Finishes the reset flow. A confirmed reset also proves the address works. */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = authTokenService.consume(request.token(), AuthTokenType.PASSWORD_RESET);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEmailVerified(true);
        // Ends every session that was opened with the old password.
        user.setCredentialsChangedAt(Instant.now());
        // The owner has just proved they control the address, so drop any lockout.
        loginAttemptService.clearFor(user.getEmail());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request, String clientIp) {
        String email = request.email().trim();
        String clientKey = email.toLowerCase(Locale.ENGLISH) + "|" + clientIp;

        Duration blockedFor = loginAttemptService.blockedFor(clientKey);
        if (!blockedFor.isZero()) {
            throw new TooManyRequestsException(
                    "Too many failed sign in attempts. Try again in " + (blockedFor.toMinutes() + 1) + " minute(s).");
        }

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (AuthenticationException ex) {
            loginAttemptService.recordFailure(clientKey);
            // One message for unknown email and wrong password so accounts cannot be enumerated.
            throw new BadCredentialsException("Invalid email or password");
        }

        loginAttemptService.recordSuccess(clientKey);

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        // Only reached with the correct password, so naming the reason reveals nothing new.
        if (!user.isEmailVerified()) {
            throw new DisabledException("Please confirm your email address first. Check your inbox for the link.");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        Instant expiresAt = jwtService.expiryFrom(Instant.now());

        return LoginResponse.bearer(token, user.getRole(), user.getUsername(), user.getEmail(), expiresAt);
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse currentUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ResourceNotFoundException.of("Account", email));
        return new CurrentUserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    /** Keeps the mail sending endpoints from being used as a spam cannon. */
    private void guardAgainstAbuse(String key) {
        Duration blockedFor = loginAttemptService.blockedFor(key);
        if (!blockedFor.isZero()) {
            throw new TooManyRequestsException(
                    "Too many requests. Try again in " + (blockedFor.toMinutes() + 1) + " minute(s).");
        }
        loginAttemptService.recordFailure(key);
    }

    /** Kept for callers that only need to know whether an account exists. */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email);
    }
}
