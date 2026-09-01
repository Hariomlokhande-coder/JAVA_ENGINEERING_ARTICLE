package com.technicalblog.security;

import com.technicalblog.entity.User;
import com.technicalblog.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * Reads the bearer token and populates the security context.
 * The account is re-read on every request, so a deleted, demoted or unverified
 * account loses access immediately, and a token issued before the last password
 * change is refused. An absent or invalid token is ignored here, the entry point
 * decides the response.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            jwtService.parse(token).ifPresent(claims -> authenticate(claims, request));
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(Claims claims, HttpServletRequest request) {
        String email = claims.getSubject();
        String role = jwtService.roleClaim(claims);
        if (email == null || email.isBlank() || role == null || role.isBlank()) {
            return;
        }

        User account = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (account == null || !account.isEmailVerified()) {
            return;
        }

        // The token is only honoured while it still matches the role stored for the account.
        if (!account.getRole().name().equals(role)) {
            return;
        }

        if (issuedBeforePasswordChange(claims, account)) {
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                account.getEmail(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name())));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /** A password change ends every session that started before it. */
    private boolean issuedBeforePasswordChange(Claims claims, User account) {
        Instant changedAt = account.getCredentialsChangedAt();
        if (changedAt == null || claims.getIssuedAt() == null) {
            return false;
        }
        // Tokens carry whole second precision, so the comparison is made at that resolution.
        return claims.getIssuedAt().toInstant().isBefore(changedAt.minusSeconds(1));
    }
}
