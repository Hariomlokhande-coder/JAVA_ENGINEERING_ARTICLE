package com.technicalblog.service;

import com.technicalblog.dto.request.ArticleRequest;
import com.technicalblog.dto.response.ArticleResponse;
import com.technicalblog.dto.response.ArticleSummaryResponse;
import com.technicalblog.dto.response.PageResponse;
import com.technicalblog.entity.Article;
import com.technicalblog.entity.Category;
import com.technicalblog.entity.Difficulty;
import com.technicalblog.exception.DuplicateResourceException;
import com.technicalblog.exception.InvalidRequestException;
import com.technicalblog.exception.ResourceNotFoundException;
import com.technicalblog.mapper.ArticleMapper;
import com.technicalblog.repository.ArticleProgressRepository;
import com.technicalblog.repository.ArticleRepository;
import com.technicalblog.repository.CategoryRepository;
import com.technicalblog.security.SecurityUtils;
import com.technicalblog.util.SlugUtils;
import com.technicalblog.util.TextUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class ArticleService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MIN_KEYWORD_LENGTH = 2;
    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final Sort LATEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;
    private final ArticleProgressRepository progressRepository;
    private final TagService tagService;
    private final ArticleMapper articleMapper;

    public ArticleService(ArticleRepository articleRepository,
                          CategoryRepository categoryRepository,
                          ArticleProgressRepository progressRepository,
                          TagService tagService,
                          ArticleMapper articleMapper) {
        this.articleRepository = articleRepository;
        this.categoryRepository = categoryRepository;
        this.progressRepository = progressRepository;
        this.tagService = tagService;
        this.articleMapper = articleMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<ArticleSummaryResponse> findPublished(int page, int size) {
        Page<Article> result = articleRepository.findByPublishedTrue(pageable(page, size));
        return PageResponse.from(result, articleMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public PageResponse<ArticleSummaryResponse> search(String keyword, int page, int size) {
        Page<Article> result = articleRepository.search(likePattern(normaliseKeyword(keyword)), pageable(page, size));
        return PageResponse.from(result, articleMapper::toSummary);
    }

    /** Admin listing, drafts included. */
    @Transactional(readOnly = true)
    public PageResponse<ArticleSummaryResponse> findForAdmin(String keyword, int page, int size) {
        String trimmed = TextUtils.trimToNull(keyword);
        String pattern = trimmed == null ? "%" : likePattern(trimmed.toLowerCase(Locale.ENGLISH));
        Page<Article> result = articleRepository.searchForAdmin(pattern, pageable(page, size));
        return PageResponse.from(result, articleMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public List<ArticleSummaryResponse> findByCategorySlug(String categorySlug) {
        Category category = categoryRepository.findBySlug(requireSlug(categorySlug, "Category"))
                .orElseThrow(() -> ResourceNotFoundException.of("Category", categorySlug));

        List<Article> articles = SecurityUtils.isAdmin()
                ? articleRepository.findByCategoryIdOrderByDisplayOrderAscTitleAsc(category.getId())
                : articleRepository.findByCategoryIdAndPublishedTrueOrderByDisplayOrderAscTitleAsc(category.getId());

        return articles.stream().map(articleMapper::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public ArticleResponse findById(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Article", id));
        if (!article.isPublished() && !SecurityUtils.isAdmin()) {
            throw ResourceNotFoundException.of("Article", id);
        }
        return articleMapper.toResponse(article);
    }

    @Transactional(readOnly = true)
    public ArticleResponse findBySlug(String slug) {
        String normalised = requireSlug(slug, "Article");
        Article article = (SecurityUtils.isAdmin()
                ? articleRepository.findBySlug(normalised)
                : articleRepository.findBySlugAndPublishedTrue(normalised))
                .orElseThrow(() -> ResourceNotFoundException.of("Article", slug));
        return articleMapper.toResponse(article);
    }

    /** Published articles from the same category, used by the Related Articles block. */
    @Transactional(readOnly = true)
    public List<ArticleSummaryResponse> findRelated(String slug) {
        Article article = articleRepository.findBySlug(requireSlug(slug, "Article"))
                .orElseThrow(() -> ResourceNotFoundException.of("Article", slug));

        return articleRepository
                .findTop6ByCategoryIdAndPublishedTrueAndIdNotOrderByDisplayOrderAscTitleAsc(
                        article.getCategory().getId(), article.getId())
                .stream()
                .map(articleMapper::toSummary)
                .toList();
    }

    @Transactional
    public ArticleResponse create(ArticleRequest request) {
        Category category = categoryOf(request.categoryId());
        String title = request.title().trim();
        String requestedSlug = TextUtils.trimToNull(request.slug());
        String slug;

        if (requestedSlug != null) {
            if (articleRepository.existsBySlug(requestedSlug)) {
                throw new DuplicateResourceException("An article with the slug " + requestedSlug + " already exists");
            }
            slug = requestedSlug;
        } else {
            slug = SlugUtils.uniqueSlug(title, title, articleRepository::existsBySlug);
        }

        Article article = Article.builder()
                .title(title)
                .slug(slug)
                .description(TextUtils.trimToNull(request.description()))
                .content(request.content())
                .category(category)
                .displayOrder(request.displayOrder() == null ? 0 : request.displayOrder())
                .githubUrl(TextUtils.trimToNull(request.githubUrl()))
                .youtubeUrl(TextUtils.trimToNull(request.youtubeUrl()))
                .thumbnailUrl(TextUtils.trimToNull(request.thumbnailUrl()))
                .published(Boolean.TRUE.equals(request.published()))
                .difficulty(request.difficulty() == null ? Difficulty.EASY : request.difficulty())
                .build();

        Article saved = articleRepository.save(article);
        saved.replaceTags(tagService.resolveTags(request.tags()));

        return articleMapper.toResponse(saved);
    }

    @Transactional
    public ArticleResponse update(Long id, ArticleRequest request) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Article", id));

        Category category = categoryOf(request.categoryId());
        String title = request.title().trim();
        String requestedSlug = TextUtils.trimToNull(request.slug());

        if (requestedSlug != null) {
            if (articleRepository.existsBySlugAndIdNot(requestedSlug, id)) {
                throw new DuplicateResourceException("An article with the slug " + requestedSlug + " already exists");
            }
            article.setSlug(requestedSlug);
        } else if (!article.getTitle().equalsIgnoreCase(title)) {
            article.setSlug(SlugUtils.uniqueSlug(title, title,
                    candidate -> articleRepository.existsBySlugAndIdNot(candidate, id)));
        }

        article.setTitle(title);
        article.setDescription(TextUtils.trimToNull(request.description()));
        article.setContent(request.content());
        article.setCategory(category);
        article.setDisplayOrder(request.displayOrder() == null ? article.getDisplayOrder() : request.displayOrder());
        article.setGithubUrl(TextUtils.trimToNull(request.githubUrl()));
        article.setYoutubeUrl(TextUtils.trimToNull(request.youtubeUrl()));
        article.setThumbnailUrl(TextUtils.trimToNull(request.thumbnailUrl()));
        article.setPublished(Boolean.TRUE.equals(request.published()));
        article.setDifficulty(request.difficulty() == null ? Difficulty.EASY : request.difficulty());
        article.replaceTags(tagService.resolveTags(request.tags()));

        return articleMapper.toResponse(article);
    }

    /** Publish toggle for the dashboard, so a draft can go live without opening the form. */
    @Transactional
    public ArticleResponse setPublished(Long id, boolean published) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Article", id));
        article.setPublished(published);
        return articleMapper.toResponse(article);
    }

    @Transactional
    public void delete(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Article", id));
        // Reader progress points at this article, so it goes first.
        progressRepository.deleteByArticleId(id);
        // Article owns the tag relation, so deleting it also removes its article_tags rows.
        articleRepository.delete(article);
    }

    private Category categoryOf(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", categoryId));
    }

    private Pageable pageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize, LATEST_FIRST);
    }

    private String normaliseKeyword(String keyword) {
        String trimmed = TextUtils.trimToNull(keyword);
        if (trimmed == null || trimmed.length() < MIN_KEYWORD_LENGTH) {
            throw new InvalidRequestException("Search keyword must be at least " + MIN_KEYWORD_LENGTH + " characters");
        }
        String limited = trimmed.length() > MAX_KEYWORD_LENGTH ? trimmed.substring(0, MAX_KEYWORD_LENGTH) : trimmed;
        return limited.toLowerCase(Locale.ENGLISH);
    }

    /** Escapes the LIKE wildcards so a keyword such as 100% cannot match every row. */
    private String likePattern(String keyword) {
        String escaped = keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return "%" + escaped + "%";
    }

    private String requireSlug(String slug, String resource) {
        String normalised = TextUtils.trimToNull(slug);
        if (normalised == null) {
            throw ResourceNotFoundException.of(resource, slug);
        }
        return normalised.toLowerCase(Locale.ENGLISH);
    }
}
