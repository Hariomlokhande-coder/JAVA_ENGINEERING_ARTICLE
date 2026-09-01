package com.technicalblog.security;

import com.technicalblog.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/** Creates and verifies the HS256 tokens used by the admin session. */
@Service
public class JwtService {

    private static final int MIN_SECRET_LENGTH = 32;
    private static final String ROLE_CLAIM = "role";

    private final SecretKey signingKey;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        byte[] secretBytes = properties.secret() == null
                ? new byte[0]
                : properties.secret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "app.jwt.secret must be at least " + MIN_SECRET_LENGTH + " characters long");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        this.properties = properties;
    }

    public String generateToken(String email, String role) {
        Instant issuedAt = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim(ROLE_CLAIM, role)
                .issuer(properties.issuer())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiryFrom(issuedAt)))
                .signWith(signingKey)
                .compact();
    }

    public Instant expiryFrom(Instant issuedAt) {
        return issuedAt.plusMillis(properties.expirationMs());
    }

    /** Returns the claims, or an empty optional when the token is missing, tampered with or expired. */
    public Optional<Claims> parse(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(properties.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public String roleClaim(Claims claims) {
        return claims.get(ROLE_CLAIM, String.class);
    }
}
