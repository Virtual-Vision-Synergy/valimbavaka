package org.vvs.valimbavaka.feature.article.service.impl;

import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.dto.ArticleWDto;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.supplier.service.SupplierService;

/** Business rules of the sheet that look at other features: a finished product needs a range and a price. */
@Component
class ArticleReferenceChecker {

    private final ReferentialService referentialService;
    private final SupplierService supplierService;

    ArticleReferenceChecker(ReferentialService referentialService, SupplierService supplierService) {
        this.referentialService = referentialService;
        this.supplierService = supplierService;
    }

    /**
     * Checks the sheet against the other features. A supplier deactivated since the last save stays accepted when
     * the sheet keeps it ({@code currentSupplierId}), so that the article can still be edited.
     */
    void check(ArticleWDto request, @Nullable UUID currentSupplierId) {
        if (request.type() == ArticleType.FINISHED) {
            checkSoldArticle(request);
        }
        if (isSupplierRefused(request.supplierId(), currentSupplierId)) {
            throw new BusinessException("Fournisseur introuvable ou désactivé : " + request.supplierId());
        }
    }

    private boolean isSupplierRefused(@Nullable UUID supplierId, @Nullable UUID currentSupplierId) {
        if (supplierId == null || supplierId.equals(currentSupplierId)) {
            return false;
        }
        return !supplierService.isActiveSupplier(supplierId);
    }

    private void checkSoldArticle(ArticleWDto request) {
        if (request.rangeId() == null) {
            throw new BusinessException("La gamme est obligatoire pour un produit fini.");
        }
        if (!referentialService.isActiveProductRange(request.rangeId())) {
            throw new BusinessException("Gamme introuvable : " + request.rangeId());
        }
        if (request.price() == null) {
            throw new BusinessException("Le prix est obligatoire pour un produit fini.");
        }
    }
}
