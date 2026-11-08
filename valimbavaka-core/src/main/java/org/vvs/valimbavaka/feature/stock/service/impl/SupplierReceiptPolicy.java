package org.vvs.valimbavaka.feature.stock.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.stock.domain.SupplierReceiptCommand;
import org.vvs.valimbavaka.feature.stock.domain.SupplierReceiptLineCommand;
import org.vvs.valimbavaka.feature.stock.dto.SupplierReceiptLineWDto;
import org.vvs.valimbavaka.feature.stock.dto.SupplierReceiptWDto;
import org.vvs.valimbavaka.feature.supplier.service.SupplierService;

/**
 * Rules of a supplier receipt checked before anything is written (STK-06): active supplier, active location (the
 * warehouse when none is given), known articles, each article once. Quantities and costs are checked by the DTO.
 */
@Component
class SupplierReceiptPolicy {

    static final String DEFAULT_LOCATION = "WAREHOUSE";

    private final SupplierService supplierService;
    private final ReferentialService referentialService;
    private final ArticleService articleService;

    SupplierReceiptPolicy(
            SupplierService supplierService, ReferentialService referentialService, ArticleService articleService) {
        this.supplierService = supplierService;
        this.referentialService = referentialService;
        this.articleService = articleService;
    }

    /** 404 if the supplier or an article is unknown, 422 if the supplier is deactivated or a rule is broken. */
    SupplierReceiptCommand check(SupplierReceiptWDto request) {
        var supplier = supplierService.getSupplierById(request.supplierId());
        if (!supplier.active()) {
            throw new BusinessException(
                    "Le fournisseur %s est désactivé : réactivez-le avant d'enregistrer sa livraison."
                            .formatted(supplier.name()));
        }
        return new SupplierReceiptCommand(
                supplier.id(),
                supplier.name(),
                location(request.locationId()),
                request.restockOrderId(),
                blankToNull(request.note()),
                lines(request.lines()));
    }

    private UUID location(@Nullable UUID locationId) {
        if (locationId == null) {
            return referentialService.requireActiveStockLocationId(DEFAULT_LOCATION);
        }
        if (!referentialService.isActiveStockLocation(locationId)) {
            throw new BusinessException("L'emplacement de stock est inconnu ou inactif.");
        }
        return locationId;
    }

    private List<SupplierReceiptLineCommand> lines(List<SupplierReceiptLineWDto> lines) {
        var articleIds = new HashSet<UUID>();
        var commands = new ArrayList<SupplierReceiptLineCommand>();
        for (var line : lines) {
            if (!articleIds.add(line.articleId())) {
                throw new BusinessException("Un article ne peut figurer qu'une fois dans une réception.");
            }
            var article = articleService.getArticleById(line.articleId());
            commands.add(new SupplierReceiptLineCommand(
                    article.id(), article.code(), article.name(), line.quantity(), line.unitCost()));
        }
        return commands;
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
