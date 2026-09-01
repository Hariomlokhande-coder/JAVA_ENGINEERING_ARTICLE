package com.technicalblog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A single use token sent by email. Only a hash is stored, so a leaked database
 * still cannot be used to verify an address or reset a password.
 */
@Entity
@Table(name = "auth_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uk_auth_tokens_hash", columnNames = "token_hash"),
        indexes = @Index(name = "idx_auth_tokens_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_auth_tokens_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private AuthTokenType type;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant usedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public boolean isUsable() {
        return usedAt == null && expiresAt.isAfter(Instant.now());
    }
}
