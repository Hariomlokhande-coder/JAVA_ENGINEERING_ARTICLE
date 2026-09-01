package com.technicalblog.controller;

import com.technicalblog.dto.request.LoginRequest;
import com.technicalblog.dto.request.RegisterRequest;
import com.technicalblog.dto.response.CurrentUserResponse;
import com.technicalblog.dto.response.LoginResponse;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Open sign up for readers who want their progress saved. */
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
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
