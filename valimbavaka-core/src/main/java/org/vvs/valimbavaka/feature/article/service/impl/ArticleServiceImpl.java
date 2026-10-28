package org.vvs.valimbavaka.feature.article.service.impl;

import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.article.domain.Article;
import org.vvs.valimbavaka.feature.article.domain.ArticleDetails;
import org.vvs.valimbavaka.feature.article.domain.ArticleFilter;
import org.vvs.valimbavaka.feature.article.domain.ProductionUsage;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.dto.ArticleWDto;
import org.vvs.valimbavaka.feature.article.mapper.ArticleMapper;
import org.vvs.valimbavaka.feature.article.repository.ArticleRepository;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;
import org.vvs.valimbavaka.shared.dto.SortDirection;

@Service
@Transactional(readOnly = true)
public class ArticleServiceImpl implements ArticleService {

    private static final Logger log = LoggerFactory.getLogger(ArticleServiceImpl.class);

    private static final String RESOURCE_TYPE = "ARTICLE";
    private static final String BARCODE_ALREADY_USED = "Un article avec ce code-barres existe déjà.";
    private static final String STALE_VERSION =
            "L'article a été modifié entre-temps : rechargez la fiche puis réessayez.";
    private static final String NOT_AN_INPUT =
            "Seules les matières premières et les semi-finis peuvent entrer en production.";
    private static final String NOT_AN_OUTPUT = "Seuls les produits finis peuvent sortir de production.";
    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "code", "name", "type", "price");

    private final ArticleRepository articleRepository;
    private final ArticleMapper articleMapper;
    private final JournalService journalService;
    private final ArticleReferenceChecker referenceChecker;

    public ArticleServiceImpl(
            ArticleRepository articleRepository,
            ArticleMapper articleMapper,
            JournalService journalService,
            ArticleReferenceChecker referenceChecker) {
        this.articleRepository = articleRepository;
        this.articleMapper = articleMapper;
        this.journalService = journalService;
        this.referenceChecker = referenceChecker;
    }

    @Override
    public PageResponse<ArticleRDto> getArticles(PageQuery query, ArticleFilter filter) {
        var pageable = query.toPageable(SORT_FIELDS, "createdAt", SortDirection.DESC);
        var page = articleRepository.search(
                query.searchTerm(), filter.type(), filter.rangeId(), filter.supplierId(), filter.active(), pageable);
        return PageResponse.from(page.map(articleMapper::toRDto));
    }

    @Override
    public ArticleRDto getArticleById(UUID articleId) {
        return articleMapper.toRDto(findArticle(articleId));
    }

    @Override
    public PageResponse<ArticleRDto> getProductionArticles(PageQuery query, ProductionUsage usage) {
        var pageable = query.toPageable(SORT_FIELDS, "name", SortDirection.ASC);
        var page = articleRepository.searchActiveByTypes(query.searchTerm(), usage.types(), pageable);
        return PageResponse.from(page.map(articleMapper::toRDto));
    }

    @Override
    public ArticleRDto requireProductionArticle(UUID articleId, ProductionUsage usage) {
        var article = findArticle(articleId);
        if (!article.isActive()) {
            throw new BusinessException("Article désactivé, inutilisable en production : " + article.getCode());
        }
        if (!usage.accepts(article.getType())) {
            throw new BusinessException(usage == ProductionUsage.INPUT ? NOT_AN_INPUT : NOT_AN_OUTPUT);
        }
        return articleMapper.toRDto(article);
    }

    @Override
    @Transactional
    public ArticleRDto createArticle(ArticleWDto request) {
        referenceChecker.check(request, null);
        var details = toDetails(request);
        if (details.barcode() != null && articleRepository.existsByBarcode(details.barcode())) {
            throw new ConflictException(BARCODE_ALREADY_USED);
        }
        var code = "ART-%04d".formatted(articleRepository.nextCodeNumber());
        Article saved = articleRepository.save(Article.create(code, details));
        journalService.record(
                JournalAction.CREATE, RESOURCE_TYPE, saved.getId(), "Création de l'article " + label(saved));
        log.info("Article {} créé", saved.getId());
        return articleMapper.toRDto(saved);
    }

    @Override
    @Transactional
    public ArticleRDto updateArticle(UUID articleId, ArticleWDto request) {
        Article article = findArticle(articleId);
        if (request.version() != null && request.version() != article.getVersion()) {
            throw new ConflictException(STALE_VERSION);
        }
        referenceChecker.check(request, article.getSupplierId());
        var details = toDetails(request);
        if (details.barcode() != null && articleRepository.existsByBarcodeAndIdNot(details.barcode(), articleId)) {
            throw new ConflictException(BARCODE_ALREADY_USED);
        }
        if (article.update(details)) {
            journalService.record(
                    JournalAction.UPDATE, RESOURCE_TYPE, articleId, "Modification de l'article " + label(article));
        }
        return articleMapper.toRDto(article);
    }

    @Override
    @Transactional
    public ArticleRDto setActive(UUID articleId, boolean active) {
        Article article = findArticle(articleId);
        if (article.isActive() == active) {
            return articleMapper.toRDto(article);
        }
        if (active) {
            article.activate();
        } else {
            article.deactivate();
        }
        String action = active ? "Activation de l'article " : "Désactivation de l'article ";
        journalService.record(JournalAction.UPDATE, RESOURCE_TYPE, articleId, action + label(article));
        log.info("Article {} {}", articleId, active ? "activé" : "désactivé");
        return articleMapper.toRDto(article);
    }

    private Article findArticle(UUID articleId) {
        return articleRepository
                .findById(articleId)
                .orElseThrow(() -> new NotFoundException("Article introuvable : " + articleId));
    }

    private static String label(Article article) {
        return article.getCode() + " · " + article.getName();
    }

    private static ArticleDetails toDetails(ArticleWDto request) {
        return new ArticleDetails(
                request.name(),
                request.barcode() == null || request.barcode().isBlank()
                        ? null
                        : request.barcode().strip(),
                request.type(),
                request.rangeId(),
                request.unit(),
                request.cost(),
                request.price(),
                request.threshold(),
                request.supplierId());
    }
}
