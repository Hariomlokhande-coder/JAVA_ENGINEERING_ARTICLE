package com.technicalblog.service;

import com.technicalblog.dto.request.ProgressRequest;
import com.technicalblog.dto.response.ProgressResponse;
import com.technicalblog.entity.Article;
import com.technicalblog.entity.ArticleProgress;
import com.technicalblog.entity.User;
import com.technicalblog.exception.ResourceNotFoundException;
import com.technicalblog.repository.ArticleProgressRepository;
import com.technicalblog.repository.ArticleRepository;
import com.technicalblog.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Reading progress and favourites for the signed in reader. */
@Service
public class ProgressService {

    private final ArticleProgressRepository progressRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    public ProgressService(ArticleProgressRepository progressRepository,
                           ArticleRepository articleRepository,
                           UserRepository userRepository) {
        this.progressRepository = progressRepository;
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ProgressResponse> findMine(String email) {
        User user = requireUser(email);
        return progressRepository.findByUserId(user.getId()).stream()
                .map(entry -> new ProgressResponse(entry.getArticle().getId(), entry.isCompleted(), entry.isFavourite()))
                .toList();
    }

    /** Creates or updates one entry. Fields left null keep their current value. */
    @Transactional
    public ProgressResponse save(String email, Long articleId, ProgressRequest request) {
        User user = requireUser(email);
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> ResourceNotFoundException.of("Article", articleId));

        ArticleProgress entry = progressRepository.findByUserIdAndArticleId(user.getId(), articleId)
                .orElseGet(() -> ArticleProgress.builder()
                        .user(user)
                        .article(article)
                        .completed(false)
                        .favourite(false)
                        .build());

        if (request.completed() != null) {
            entry.setCompleted(request.completed());
        }
        if (request.favourite() != null) {
            entry.setFavourite(request.favourite());
        }

        ArticleProgress saved = progressRepository.save(entry);
        return new ProgressResponse(articleId, saved.isCompleted(), saved.isFavourite());
    }

    private User requireUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ResourceNotFoundException.of("Account", email));
    }
}
