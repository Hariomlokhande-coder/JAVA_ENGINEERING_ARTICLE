package com.technicalblog.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory brute force guard for the login endpoint.
 * Counts failures per client and blocks further attempts for a cooling off period.
 * State is per instance, which is the right scope for a single node deployment.
 */
@Service
public class LoginAttemptService {

    static final int MAX_ATTEMPTS = 5;
    static final Duration BLOCK_DURATION = Duration.ofMinutes(15);
    private static final Duration ATTEMPT_WINDOW = Duration.ofMinutes(15);
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final Map<String, Attempts> attemptsByClient = new ConcurrentHashMap<>();

    /** Remaining block time, or zero when the client may try again. */
    public Duration blockedFor(String clientKey) {
        Attempts attempts = attemptsByClient.get(clientKey);
        if (attempts == null || attempts.count.get() < MAX_ATTEMPTS) {
            return Duration.ZERO;
        }
        Duration elapsed = Duration.between(attempts.lastFailure, Instant.now());
        if (elapsed.compareTo(BLOCK_DURATION) >= 0) {
            attemptsByClient.remove(clientKey);
            return Duration.ZERO;
        }
        return BLOCK_DURATION.minus(elapsed);
    }

    public void recordFailure(String clientKey) {
        evictExpiredWhenCrowded();
        attemptsByClient.compute(clientKey, (key, existing) -> {
            Instant now = Instant.now();
            if (existing == null || Duration.between(existing.lastFailure, now).compareTo(ATTEMPT_WINDOW) > 0) {
                return new Attempts(now);
            }
            existing.count.incrementAndGet();
            existing.lastFailure = now;
            return existing;
        });
    }

    public void recordSuccess(String clientKey) {
        attemptsByClient.remove(clientKey);
    }

    /** Keeps the map bounded so a flood of unique clients cannot grow it without limit. */
    private void evictExpiredWhenCrowded() {
        if (attemptsByClient.size() < MAX_TRACKED_CLIENTS) {
            return;
        }
        Instant cutoff = Instant.now().minus(BLOCK_DURATION);
        attemptsByClient.values().removeIf(attempts -> attempts.lastFailure.isBefore(cutoff));
    }

    private static final class Attempts {
        private final AtomicInteger count = new AtomicInteger(1);
        private volatile Instant lastFailure;

        private Attempts(Instant lastFailure) {
            this.lastFailure = lastFailure;
        }
    }
}
