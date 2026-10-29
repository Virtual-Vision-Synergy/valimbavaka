package org.vvs.valimbavaka.feature.article.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import org.vvs.valimbavaka.feature.article.domain.Article;
import org.vvs.valimbavaka.feature.article.domain.ArticleType;

/** Extends {@link Repository}, not JpaRepository: no delete exposed, an article is deactivated. */
public interface ArticleRepository extends Repository<Article, UUID> {

    Article save(Article article);

    Optional<Article> findById(UUID id);

    Optional<Article> findByCode(String code);

    Optional<Article> findByBarcode(String barcode);

    boolean existsByBarcode(String barcode);

    boolean existsByBarcodeAndIdNot(String barcode, UUID id);

    /** Next number of the article code sequence (gaps are possible, a code is never reused). */
    @Query(value = "select nextval('article_code_seq')", nativeQuery = true)
    long nextCodeNumber();

    /**
     * Moves the code sequence up to {@code number} (never down) after an import used a code of the file, so that the
     * next generated code cannot collide with it.
     */
    @Query(
            value = "select setval('article_code_seq', greatest(:number, (select last_value from article_code_seq)))",
            nativeQuery = true)
    long advanceCodeSequence(long number);

    /** Case-insensitive search on code, name and barcode; empty {@code search} = all, null filters = all. */
    @Query(value = """
            select a from Article a
            where (lower(a.name) like lower(concat('%', :search, '%'))
                   or lower(a.code) like lower(concat('%', :search, '%'))
                   or a.barcode like concat('%', :search, '%'))
              and (:type is null or a.type = :type)
              and (:rangeId is null or a.rangeId = :rangeId)
              and (:supplierId is null or a.supplierId = :supplierId)
              and (:active is null or a.active = :active)
            """, countQuery = """
            select count(a) from Article a
            where (lower(a.name) like lower(concat('%', :search, '%'))
                   or lower(a.code) like lower(concat('%', :search, '%'))
                   or a.barcode like concat('%', :search, '%'))
              and (:type is null or a.type = :type)
              and (:rangeId is null or a.rangeId = :rangeId)
              and (:supplierId is null or a.supplierId = :supplierId)
              and (:active is null or a.active = :active)
            """)
    Page<Article> search(
            String search,
            @Nullable ArticleType type,
            @Nullable UUID rangeId,
            @Nullable UUID supplierId,
            @Nullable Boolean active,
            Pageable pageable);

    /** Active articles of the given types, with the same search as {@link #search} (ART-04: production selectors). */
    @Query(value = """
            select a from Article a
            where a.active = true
              and a.type in :types
              and (lower(a.name) like lower(concat('%', :search, '%'))
                   or lower(a.code) like lower(concat('%', :search, '%'))
                   or a.barcode like concat('%', :search, '%'))
            """, countQuery = """
            select count(a) from Article a
            where a.active = true
              and a.type in :types
              and (lower(a.name) like lower(concat('%', :search, '%'))
                   or lower(a.code) like lower(concat('%', :search, '%'))
                   or a.barcode like concat('%', :search, '%'))
            """)
    Page<Article> searchActiveByTypes(String search, Collection<ArticleType> types, Pageable pageable);
}
