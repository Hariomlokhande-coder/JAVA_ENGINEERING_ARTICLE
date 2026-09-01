package com.technicalblog.service;

import com.technicalblog.entity.AuthToken;
import com.technicalblog.entity.AuthTokenType;
import com.technicalblog.entity.User;
import com.technicalblog.exception.InvalidRequestException;
import com.technicalblog.repository.AuthTokenRepository;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/** Issues and redeems the one time tokens used for email links. */
@Service
public class AuthTokenService {

    private final AuthTokenRepository tokenRepository;

    public AuthTokenService(AuthTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    /** Returns the raw token to email out; only its hash reaches the database. */
    @Transactional
    public String issue(User user, AuthTokenType type, Duration validFor) {
        // Any earlier token of the same kind stops working right away.
        tokenRepository.deleteByUserIdAndType(user.getId(), type);

        String raw = Base64.getUrlEncoder().withoutPadding()
                .encodeToString((UUID.randomUUID() + ":" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8));

        tokenRepository.save(AuthToken.builder()
                .tokenHash(hash(raw))
                .user(user)
                .type(type)
                .expiresAt(Instant.now().plus(validFor))
                .build());

        return raw;
    }

    /** Validates a token and marks it used. The same link can never work twice. */
    @Transactional
    public User consume(String rawToken, AuthTokenType type) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRequestException("This link is not valid.");
        }

        AuthToken token = tokenRepository.findByTokenHashAndType(hash(rawToken), type)
                .orElseThrow(() -> new InvalidRequestException("This link is not valid or has already been used."));

        if (!token.isUsable()) {
            throw new InvalidRequestException("This link has expired. Please request a new one.");
        }

        token.setUsedAt(Instant.now());
        return token.getUser();
    }

    /** Clears out spent tokens once a day so the table does not grow forever. */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeSpentTokens() {
        int removed = tokenRepository.deleteExpiredOrUsed(Instant.now());
        if (removed > 0) {
            LoggerFactory.getLogger(AuthTokenService.class).info("Removed {} spent auth tokens", removed);
        }
    }

    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required but unavailable", ex);
        }
    }
}
