package com.technicalblog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "articles",
        uniqueConstraints = @UniqueConstraint(name = "uk_articles_slug", columnNames = "slug"),
        indexes = {
                @Index(name = "idx_articles_category", columnList = "category_id"),
                @Index(name = "idx_articles_published", columnList = "published")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 240)
    private String slug;

    @Column(length = 500)
    private String description;

    /** Markdown body of the article. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_articles_category"))
    private Category category;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "github_url", length = 500)
    private String githubUrl;

    @Column(name = "youtube_url", length = 500)
    private String youtubeUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(nullable = false)
    private boolean published;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Difficulty difficulty;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "article_tags",
            joinColumns = @JoinColumn(name = "article_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"),
            foreignKey = @ForeignKey(name = "fk_article_tags_article"),
            inverseForeignKey = @ForeignKey(name = "fk_article_tags_tag"))
    @Builder.Default
    private Set<Tag> tags = new LinkedHashSet<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.displayOrder == null) {
            this.displayOrder = 0;
        }
        if (this.difficulty == null) {
            this.difficulty = Difficulty.EASY;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /** Replaces the tag set on the owning side, which is what writes the join table. */
    public void replaceTags(Set<Tag> newTags) {
        this.tags.clear();
        this.tags.addAll(newTags);
    }
}
