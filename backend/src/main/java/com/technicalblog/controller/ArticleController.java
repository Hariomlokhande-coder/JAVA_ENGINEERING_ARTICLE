package com.technicalblog.controller;

import com.technicalblog.dto.request.ArticleRequest;
import com.technicalblog.dto.response.ArticleResponse;
import com.technicalblog.dto.response.ArticleSummaryResponse;
import com.technicalblog.dto.response.PageResponse;
import com.technicalblog.service.ArticleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** Reads are public, writes require the ADMIN role (enforced in SecurityConfig). */
@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<ArticleSummaryResponse>> findPublished(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(articleService.findPublished(page, size));
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponse<ArticleSummaryResponse>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(articleService.search(keyword, page, size));
    }

    /** Admin listing including drafts. Restricted in SecurityConfig. */
    @GetMapping("/manage")
    public ResponseEntity<PageResponse<ArticleSummaryResponse>> findForAdmin(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(articleService.findForAdmin(keyword, page, size));
    }

    @GetMapping("/category/{categorySlug}")
    public ResponseEntity<List<ArticleSummaryResponse>> findByCategory(@PathVariable String categorySlug) {
        return ResponseEntity.ok(articleService.findByCategorySlug(categorySlug));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ArticleResponse> findBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(articleService.findBySlug(slug));
    }

    @GetMapping("/slug/{slug}/related")
    public ResponseEntity<List<ArticleSummaryResponse>> findRelated(@PathVariable String slug) {
        return ResponseEntity.ok(articleService.findRelated(slug));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ArticleResponse> create(@Valid @RequestBody ArticleRequest request) {
        ArticleResponse created = articleService.create(request);
        return ResponseEntity.created(URI.create("/api/articles/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ArticleResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody ArticleRequest request) {
        return ResponseEntity.ok(articleService.update(id, request));
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<ArticleResponse> setPublished(@PathVariable Long id, @RequestParam boolean published) {
        return ResponseEntity.ok(articleService.setPublished(id, published));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        articleService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
