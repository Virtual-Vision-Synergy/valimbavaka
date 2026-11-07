package org.vvs.valimbavaka.feature.transfer.service.impl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.transfer.domain.Transfer;
import org.vvs.valimbavaka.feature.transfer.domain.TransferLine;
import org.vvs.valimbavaka.feature.transfer.dto.TransferDetailLineRDto;
import org.vvs.valimbavaka.feature.transfer.dto.TransferDetailRDto;

/**
 * Turns a transfer into its full sheet (TRF-04): every line with the code, name and unit of its article (all the
 * articles in one read, deactivated ones included), the labels of the locations and the name of the issuer as in the
 * history (TRF-01).
 */
@Component
class TransferSheet {

    private final TransferHistory history;
    private final ArticleService articleService;

    TransferSheet(TransferHistory history, ArticleService articleService) {
        this.history = history;
        this.articleService = articleService;
    }

    TransferDetailRDto toRDto(Transfer transfer) {
        var labels = history.locationLabels();
        var articles = articlesOf(transfer.getLines());
        var lines = transfer.getLines().stream()
                .map(line -> toLineRDto(line, articles))
                .toList();
        return new TransferDetailRDto(
                transfer.getId(),
                transfer.getNumber(),
                transfer.getOrigin(),
                transfer.getStatus(),
                transfer.getFromLocationId(),
                labels.get(transfer.getFromLocationId()),
                transfer.getToLocationId(),
                labels.get(transfer.getToLocationId()),
                transfer.getNote(),
                lines,
                totalQuantity(transfer.getLines()),
                transfer.getCreatedAt(),
                transfer.getCreatedBy(),
                history.issuerName(transfer.getCreatedBy(), new HashMap<>()));
    }

    private Map<UUID, ArticleRDto> articlesOf(List<TransferLine> lines) {
        var ids = lines.stream().map(TransferLine::getArticleId).toList();
        return articleService.getArticlesByIds(ids).stream()
                .collect(Collectors.toMap(ArticleRDto::id, Function.identity()));
    }

    /** The article of a line always exists: {@code transfer_lines.article_id} references {@code articles}. */
    private static TransferDetailLineRDto toLineRDto(TransferLine line, Map<UUID, ArticleRDto> articles) {
        var article = articles.get(line.getArticleId());
        if (article == null) {
            throw new IllegalStateException("Article de la ligne introuvable : " + line.getArticleId());
        }
        return new TransferDetailLineRDto(
                article.id(), article.code(), article.name(), article.unit(), line.getQuantity());
    }

    private static BigDecimal totalQuantity(List<TransferLine> lines) {
        return lines.stream().map(TransferLine::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
