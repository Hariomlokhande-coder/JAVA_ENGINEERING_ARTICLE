package com.technicalblog.mapper;

import com.technicalblog.dto.response.ArticleResponse;
import com.technicalblog.dto.response.ArticleSummaryResponse;
import com.technicalblog.entity.Article;
import com.technicalblog.entity.Category;
import com.technicalblog.entity.Tag;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Maps articles to their API shapes. Must be called inside a transaction because
 * the tag collection is loaded lazily.
 */
@Component
public class ArticleMapper {

    public ArticleSummaryResponse toSummary(Article article) {
        Category category = article.getCategory();
        return new ArticleSummaryResponse(
                article.getId(),
                article.getTitle(),
                article.getSlug(),
                article.getDescription(),
                article.getDisplayOrder(),
                article.getThumbnailUrl(),
                article.getGithubUrl(),
                article.getYoutubeUrl(),
                article.isPublished(),
                article.getDifficulty(),
                category.getId(),
                category.getName(),
                category.getSlug(),
                tagNames(article),
                article.getCreatedAt(),
                article.getUpdatedAt());
    }

    public ArticleResponse toResponse(Article article) {
        Category category = article.getCategory();
        return new ArticleResponse(
                article.getId(),
                article.getTitle(),
                article.getSlug(),
                article.getDescription(),
                article.getContent(),
                article.getDisplayOrder(),
                article.getGithubUrl(),
                article.getYoutubeUrl(),
                article.getThumbnailUrl(),
                article.isPublished(),
                article.getDifficulty(),
                category.getId(),
                category.getName(),
                category.getSlug(),
                tagNames(article),
                article.getCreatedAt(),
                article.getUpdatedAt());
    }

    private List<String> tagNames(Article article) {
        return article.getTags().stream()
                .map(Tag::getName)
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}
