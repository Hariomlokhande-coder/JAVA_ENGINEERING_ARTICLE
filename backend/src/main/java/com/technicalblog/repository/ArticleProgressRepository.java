package com.technicalblog.repository;

import com.technicalblog.entity.ArticleProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArticleProgressRepository extends JpaRepository<ArticleProgress, Long> {

    List<ArticleProgress> findByUserId(Long userId);

    Optional<ArticleProgress> findByUserIdAndArticleId(Long userId, Long articleId);

    @Modifying
    @Query("delete from ArticleProgress p where p.article.id = :articleId")
    void deleteByArticleId(@Param("articleId") Long articleId);
}
