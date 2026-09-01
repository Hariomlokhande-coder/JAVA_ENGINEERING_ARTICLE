package com.technicalblog.repository;

import com.technicalblog.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    Optional<Article> findBySlug(String slug);

    Optional<Article> findBySlugAndPublishedTrue(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    long countByCategoryId(Long categoryId);

    @EntityGraph(attributePaths = "category")
    Page<Article> findByPublishedTrue(Pageable pageable);

    @EntityGraph(attributePaths = "category")
    List<Article> findByCategoryIdAndPublishedTrueOrderByDisplayOrderAscTitleAsc(Long categoryId);

    @EntityGraph(attributePaths = "category")
    List<Article> findByCategoryIdOrderByDisplayOrderAscTitleAsc(Long categoryId);

    @EntityGraph(attributePaths = "category")
    List<Article> findTop6ByCategoryIdAndPublishedTrueAndIdNotOrderByDisplayOrderAscTitleAsc(
            Long categoryId, Long excludedId);

    /** Public search over title, description and tag names. */
    @Query(value = """
            select distinct a from Article a
            left join a.tags t
            where a.published = true
              and (lower(a.title) like :keyword escape '!'
                or lower(coalesce(a.description, '')) like :keyword escape '!'
                or lower(t.name) like :keyword escape '!')
            """,
            countQuery = """
                    select count(distinct a) from Article a
                    left join a.tags t
                    where a.published = true
                      and (lower(a.title) like :keyword escape '!'
                        or lower(coalesce(a.description, '')) like :keyword escape '!'
                        or lower(t.name) like :keyword escape '!')
                    """)
    Page<Article> search(@Param("keyword") String keyword, Pageable pageable);

    /** Admin listing: drafts included. The caller passes % to match every article. */
    @Query(value = """
            select a from Article a
            where lower(a.title) like :keyword escape '!'
               or lower(a.slug) like :keyword escape '!'
            """,
            countQuery = """
                    select count(a) from Article a
                    where lower(a.title) like :keyword escape '!'
                       or lower(a.slug) like :keyword escape '!'
                    """)
    @EntityGraph(attributePaths = "category")
    Page<Article> searchForAdmin(@Param("keyword") String keyword, Pageable pageable);
}
