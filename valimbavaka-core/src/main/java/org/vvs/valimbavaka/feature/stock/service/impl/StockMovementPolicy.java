package org.vvs.valimbavaka.feature.stock.service.impl;

import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementCommand;
import org.vvs.valimbavaka.feature.stock.repository.StockMovementRepository;

/** Business rules of a movement: known article and location, quantity fitting the type, no negative stock. */
@Component
class StockMovementPolicy {

    private static final int QUANTITY_SCALE = 3;

    private final ArticleService articleService;
    private final ReferentialService referentialService;
    private final StockMovementRepository stockMovementRepository;

    StockMovementPolicy(
            ArticleService articleService,
            ReferentialService referentialService,
            StockMovementRepository stockMovementRepository) {
        this.articleService = articleService;
        this.referentialService = referentialService;
        this.stockMovementRepository = stockMovementRepository;
    }

    /** Takes the lock of the article and location until the end of the transaction, then checks the balance. */
    ArticleRDto check(StockMovementCommand command) {
        var article = articleService.getArticleById(command.articleId());
        if (!referentialService.isActiveStockLocation(command.locationId())) {
            throw new BusinessException("L'emplacement de stock est inconnu ou inactif.");
        }
        checkQuantity(command);
        stockMovementRepository.lock(command.articleId() + ":" + command.locationId());
        checkBalance(command);
        return article;
    }

    private static void checkQuantity(StockMovementCommand command) {
        var quantity = command.quantity();
        if (!command.type().accepts(quantity)) {
            throw new BusinessException("La quantité d'un mouvement %s ne peut pas être %s."
                    .formatted(command.type(), quantity.signum() == 0 ? "nulle" : "de ce signe"));
        }
        if (quantity.stripTrailingZeros().scale() > QUANTITY_SCALE) {
            throw new BusinessException("La quantité accepte 3 décimales au maximum.");
        }
    }

    private void checkBalance(StockMovementCommand command) {
        var balance = stockMovementRepository.balance(command.articleId(), command.locationId());
        if (balance.add(command.quantity()).signum() < 0) {
            throw new ConflictException("Stock insuffisant à cet emplacement : %s disponible."
                    .formatted(balance.stripTrailingZeros().toPlainString()));
        }
    }
}
