package org.vvs.valimbavaka.feature.transfer.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.referential.dto.StockLocationRDto;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.transfer.domain.TransferCommand;
import org.vvs.valimbavaka.feature.transfer.domain.TransferLineCommand;
import org.vvs.valimbavaka.shared.dto.PageQuery;

/**
 * Rules of a transfer checked before anything is written: two distinct active locations, 1 to 100 lines, known
 * articles, each article once, positive quantities. The stock balance is checked by the stock movements.
 */
@Component
class TransferPolicy {

    static final int MAX_LINES = 100;

    private static final PageQuery ALL_LOCATIONS = new PageQuery(1, 100, null, "code", null);

    private final ReferentialService referentialService;
    private final ArticleService articleService;

    TransferPolicy(ReferentialService referentialService, ArticleService articleService) {
        this.referentialService = referentialService;
        this.articleService = articleService;
    }

    /** @return the route as written in the journal, e.g. {@code Boutique → Entrepôt} */
    String check(TransferCommand command) {
        var route = route(command);
        checkLines(command.lines());
        return route;
    }

    private String route(TransferCommand command) {
        if (command.fromLocationId().equals(command.toLocationId())) {
            throw new BusinessException("L'origine et la destination du transfert doivent être différentes.");
        }
        Map<UUID, String> labels = referentialService.getStockLocations(ALL_LOCATIONS).items().stream()
                .collect(Collectors.toMap(StockLocationRDto::id, StockLocationRDto::label));
        return label(labels, command.fromLocationId()) + " → " + label(labels, command.toLocationId());
    }

    private static String label(Map<UUID, String> labels, UUID locationId) {
        var label = labels.get(locationId);
        if (label == null) {
            throw new BusinessException("L'emplacement de stock est inconnu ou inactif.");
        }
        return label;
    }

    private void checkLines(List<TransferLineCommand> lines) {
        if (lines.isEmpty() || lines.size() > MAX_LINES) {
            throw new BusinessException("Un transfert compte de 1 à %d lignes.".formatted(MAX_LINES));
        }
        var articleIds = new HashSet<UUID>();
        for (var line : lines) {
            if (!articleIds.add(line.articleId())) {
                throw new BusinessException("Un article ne peut figurer qu'une fois dans un transfert.");
            }
            if (line.quantity().signum() <= 0) {
                throw new BusinessException("La quantité d'une ligne doit être supérieure à zéro.");
            }
            articleService.getArticleById(line.articleId());
        }
    }
}
