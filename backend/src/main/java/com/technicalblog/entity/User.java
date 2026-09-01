package com.technicalblog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
// Display names are not unique: two readers may both be called Rahul.
@Table(name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String username;

    /** Always stored lower cased, so one address cannot be registered twice. */
    @Column(nullable = false, length = 150)
    private String email;

    /** BCrypt hash, never the raw password. */
    @Column(nullable = false, length = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /**
     * Readers must confirm their address before they can sign in.
     * The default lets the column be added to a table that already has rows:
     * accounts created before this feature stay usable.
     */
    @Column(name = "email_verified", nullable = false, columnDefinition = "boolean default true")
    private boolean emailVerified;

    /**
     * Set whenever the password changes. Tokens issued before this moment are
     * refused, so a reset really does end every other session.
     */
    @Column(name = "credentials_changed_at")
    private Instant credentialsChangedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.email = this.email == null ? null : this.email.trim().toLowerCase(java.util.Locale.ENGLISH);
    }

    @PreUpdate
    void onUpdate() {
        this.email = this.email == null ? null : this.email.trim().toLowerCase(java.util.Locale.ENGLISH);
    }
}
