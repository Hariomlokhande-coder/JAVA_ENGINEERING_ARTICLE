package com.technicalblog.controller;

import com.technicalblog.dto.request.EmailRequest;
import com.technicalblog.dto.request.LoginRequest;
import com.technicalblog.dto.request.RegisterRequest;
import com.technicalblog.dto.request.ResetPasswordRequest;
import com.technicalblog.dto.response.CurrentUserResponse;
import com.technicalblog.dto.response.LoginResponse;
import com.technicalblog.dto.response.MessageResponse;
import com.technicalblog.security.ClientIpResolver;
import com.technicalblog.security.SecurityUtils;
import com.technicalblog.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Open sign up. The account stays unusable until the address is confirmed. */
    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request,
                                                    HttpServletRequest httpRequest) {
        authService.register(request, ClientIpResolver.resolve(httpRequest));
        return ResponseEntity.accepted().body(new MessageResponse(
                "Check your email for the confirmation link."));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(new MessageResponse("Your email is confirmed. You can sign in now."));
    }

    /** Always answers the same way, so it cannot be used to find out who has an account. */
    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(@Valid @RequestBody EmailRequest request,
                                                              HttpServletRequest httpRequest) {
        authService.resendVerification(request, ClientIpResolver.resolve(httpRequest));
        return ResponseEntity.ok(new MessageResponse(
                "If that address needs confirming, a new link is on its way."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody EmailRequest request,
                                                          HttpServletRequest httpRequest) {
        authService.forgotPassword(request, ClientIpResolver.resolve(httpRequest));
        return ResponseEntity.ok(new MessageResponse(
                "If that address has an account, a reset link is on its way."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(new MessageResponse("Your password has been changed. You can sign in now."));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request, ClientIpResolver.resolve(httpRequest)));
    }

    /** Lets the frontend confirm that a stored token still belongs to a live account. */
    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> currentUser() {
        return ResponseEntity.ok(authService.currentUser(SecurityUtils.currentUserEmail()));
    }
}
