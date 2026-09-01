package com.technicalblog.service;

import com.technicalblog.dto.request.CategoryRequest;
import com.technicalblog.dto.response.ArticleSummaryResponse;
import com.technicalblog.dto.response.CategoryDetailResponse;
import com.technicalblog.dto.response.CategoryResponse;
import com.technicalblog.entity.Article;
import com.technicalblog.entity.Category;
import com.technicalblog.exception.BusinessRuleException;
import com.technicalblog.exception.DuplicateResourceException;
import com.technicalblog.exception.ResourceNotFoundException;
import com.technicalblog.mapper.ArticleMapper;
import com.technicalblog.mapper.CategoryMapper;
import com.technicalblog.repository.ArticleRepository;
import com.technicalblog.repository.CategoryRepository;
import com.technicalblog.security.SecurityUtils;
import com.technicalblog.util.SlugUtils;
import com.technicalblog.util.TextUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ArticleRepository articleRepository;
    private final CategoryMapper categoryMapper;
    private final ArticleMapper articleMapper;

    public CategoryService(CategoryRepository categoryRepository,
                           ArticleRepository articleRepository,
                           CategoryMapper categoryMapper,
                           ArticleMapper articleMapper) {
        this.categoryRepository = categoryRepository;
        this.articleRepository = articleRepository;
        this.categoryMapper = categoryMapper;
        this.articleMapper = articleMapper;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .map(category -> categoryMapper.toResponse(category,
                        articleRepository.countByCategoryId(category.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        Category category = getById(id);
        return categoryMapper.toResponse(category, articleRepository.countByCategoryId(id));
    }

    /** Category with its article list, used by the category page. Drafts are visible to admins only. */
    @Transactional(readOnly = true)
    public CategoryDetailResponse findDetailBySlug(String slug) {
        Category category = categoryRepository.findBySlug(normaliseSlug(slug))
                .orElseThrow(() -> ResourceNotFoundException.of("Category", slug));
        return categoryMapper.toDetail(category, articlesOf(category));
    }

    /** Every category with its articles, used by the roadmap accordion on the home page. */
    @Transactional(readOnly = true)
    public List<CategoryDetailResponse> findRoadmap() {
        return categoryRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .map(category -> categoryMapper.toDetail(category, articlesOf(category)))
                .toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        String requestedSlug = TextUtils.trimToNull(request.slug());
        String slug;

        if (requestedSlug != null) {
            if (categoryRepository.existsBySlug(requestedSlug)) {
                throw new DuplicateResourceException("A category with the slug " + requestedSlug + " already exists");
            }
            slug = requestedSlug;
        } else {
            slug = SlugUtils.uniqueSlug(name, name, categoryRepository::existsBySlug);
        }

        Category category = Category.builder()
                .name(name)
                .slug(slug)
                .description(TextUtils.trimToNull(request.description()))
                .displayOrder(request.displayOrder() == null ? 0 : request.displayOrder())
                .build();

        return categoryMapper.toResponse(categoryRepository.save(category), 0);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getById(id);
        String name = request.name().trim();
        String requestedSlug = TextUtils.trimToNull(request.slug());

        if (requestedSlug != null) {
            if (categoryRepository.existsBySlugAndIdNot(requestedSlug, id)) {
                throw new DuplicateResourceException("A category with the slug " + requestedSlug + " already exists");
            }
            category.setSlug(requestedSlug);
        } else if (!category.getName().equalsIgnoreCase(name)) {
            category.setSlug(SlugUtils.uniqueSlug(name, name, slug -> categoryRepository.existsBySlugAndIdNot(slug, id)));
        }

        category.setName(name);
        category.setDescription(TextUtils.trimToNull(request.description()));
        category.setDisplayOrder(request.displayOrder() == null ? category.getDisplayOrder() : request.displayOrder());

        return categoryMapper.toResponse(category, articleRepository.countByCategoryId(id));
    }

    @Transactional
    public void delete(Long id) {
        Category category = getById(id);
        long articleCount = articleRepository.countByCategoryId(id);
        if (articleCount > 0) {
            throw new BusinessRuleException("This category still has " + articleCount
                    + " article(s). Move or delete them before deleting the category.");
        }
        categoryRepository.delete(category);
    }

    private Category getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id));
    }

    private List<ArticleSummaryResponse> articlesOf(Category category) {
        List<Article> articles = SecurityUtils.isAdmin()
                ? articleRepository.findByCategoryIdOrderByDisplayOrderAscTitleAsc(category.getId())
                : articleRepository.findByCategoryIdAndPublishedTrueOrderByDisplayOrderAscTitleAsc(category.getId());
        return articles.stream().map(articleMapper::toSummary).toList();
    }

    private String normaliseSlug(String slug) {
        String normalised = TextUtils.trimToNull(slug);
        if (normalised == null) {
            throw ResourceNotFoundException.of("Category", slug);
        }
        return normalised.toLowerCase();
    }
}
