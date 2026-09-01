package com.technicalblog.mapper;

import com.technicalblog.dto.response.ArticleSummaryResponse;
import com.technicalblog.dto.response.CategoryDetailResponse;
import com.technicalblog.dto.response.CategoryResponse;
import com.technicalblog.entity.Category;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category, long articleCount) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getDisplayOrder(),
                articleCount,
                category.getCreatedAt(),
                category.getUpdatedAt());
    }

    public CategoryDetailResponse toDetail(Category category, List<ArticleSummaryResponse> articles) {
        return new CategoryDetailResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getDisplayOrder(),
                articles);
    }
}
