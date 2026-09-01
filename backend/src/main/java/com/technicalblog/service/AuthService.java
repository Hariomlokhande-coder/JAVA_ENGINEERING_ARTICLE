package com.technicalblog.service;

import com.technicalblog.dto.request.LoginRequest;
import com.technicalblog.dto.request.RegisterRequest;
import com.technicalblog.dto.response.CurrentUserResponse;
import com.technicalblog.dto.response.LoginResponse;
import com.technicalblog.entity.Role;
import com.technicalblog.entity.User;
import com.technicalblog.exception.DuplicateResourceException;
import com.technicalblog.exception.ResourceNotFoundException;
import com.technicalblog.repository.UserRepository;
import com.technicalblog.exception.TooManyRequestsException;
import com.technicalblog.security.JwtService;
import com.technicalblog.security.LoginAttemptService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       JwtService jwtService,
                       LoginAttemptService loginAttemptService,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.loginAttemptService = loginAttemptService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Public sign up. New accounts always get the USER role, so an open form
     * can never hand out administrator rights.
     */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        String email = request.email().trim();
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        User user = userRepository.save(User.builder()
                .username(request.username().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build());

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return LoginResponse.bearer(token, user.getRole(), user.getUsername(), user.getEmail(),
                jwtService.expiryFrom(Instant.now()));
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
}
