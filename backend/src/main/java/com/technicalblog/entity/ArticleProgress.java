package com.technicalblog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * What one reader has done with one article: read it, starred it, or both.
 * Signed in readers keep this on the server so it follows them across devices.
 */
@Entity
@Table(name = "article_progress",
        uniqueConstraints = @UniqueConstraint(name = "uk_progress_user_article",
                columnNames = {"user_id", "article_id"}),
        indexes = @Index(name = "idx_progress_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_progress_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_progress_article"))
    private Article article;

    @Column(nullable = false)
    private boolean completed;

    @Column(nullable = false)
    private boolean favourite;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }
}
