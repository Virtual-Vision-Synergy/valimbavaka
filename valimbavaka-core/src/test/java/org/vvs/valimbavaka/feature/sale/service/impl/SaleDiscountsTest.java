package org.vvs.valimbavaka.feature.sale.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionBasketLine;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionDiscount;
import org.vvs.valimbavaka.feature.promotion.service.PromotionService;
import org.vvs.valimbavaka.feature.sale.domain.PaymentMethod;
import org.vvs.valimbavaka.feature.sale.domain.PricedSaleLine;
import org.vvs.valimbavaka.feature.sale.domain.SaleCommand;
import org.vvs.valimbavaka.feature.sale.domain.SaleDiscount;
import org.vvs.valimbavaka.feature.sale.domain.SaleLineCommand;

/** PRO-02, VTE-04: the server decides the discount of a sale from the automatic promotions and the promo code. */
@ExtendWith(MockitoExtension.class)
class SaleDiscountsTest {

    private static final UUID CHANNEL = UUID.randomUUID();
    private static final UUID SOAP = UUID.randomUUID();
    private static final UUID OIL = UUID.randomUUID();
    private static final UUID RANGE = UUID.randomUUID();

    @Mock
    private PromotionService promotionService;

    @Mock
    private ArticleService articleService;

    private SaleDiscounts sut;

    @BeforeEach
    void setUp() {
        sut = new SaleDiscounts(promotionService, articleService);
    }

    @Test
    void of_catalogueLine_pricesTheBasketWithTheRangeOfTheArticleAndKeepsTheDiscount() {
        // Arrange
        var promotionId = UUID.randomUUID();
        var command = command(new SaleLineCommand(SOAP, new BigDecimal("2"), null));
        when(articleService.getArticlesByIds(List.of(SOAP))).thenReturn(List.of(article(SOAP, RANGE)));
        when(promotionService.priceBasket(any(), any()))
                .thenReturn(new PromotionDiscount(promotionId, "Octobre rose", new BigDecimal("2550.00")));

        // Act
        var discount = sut.of(command, List.of(priced(SOAP, "2", "8500")));

        // Assert
        assertThat(discount).isEqualTo(new SaleDiscount(promotionId, new BigDecimal("2550.00")));
        var basket = List.of(new PromotionBasketLine(SOAP, RANGE, new BigDecimal("8500"), new BigDecimal("2")));
        verify(promotionService).priceBasket(CHANNEL, basket);
    }

    @Test
    void of_agreedPriceLine_isLeftOutOfTheBasket() {
        // Arrange : an order delivered (LIV-03) carries the price agreed beforehand
        var command = command(new SaleLineCommand(SOAP, BigDecimal.ONE, new BigDecimal("8000")));

        // Act
        var discount = sut.of(command, List.of(priced(SOAP, "1", "8000")));

        // Assert
        assertThat(discount).isEqualTo(SaleDiscount.NONE);
        verifyNoInteractions(promotionService, articleService);
    }

    @Test
    void of_agreedAndCatalogueLines_onlyPricesTheCatalogueLine() {
        // Arrange
        var command = command(
                new SaleLineCommand(SOAP, BigDecimal.ONE, new BigDecimal("8000")),
                new SaleLineCommand(OIL, BigDecimal.ONE, null));
        when(articleService.getArticlesByIds(List.of(OIL))).thenReturn(List.of(article(OIL, null)));
        when(promotionService.priceBasket(any(), any())).thenReturn(PromotionDiscount.NONE);

        // Act
        var discount = sut.of(command, List.of(priced(SOAP, "1", "8000"), priced(OIL, "1", "12000")));

        // Assert
        assertThat(discount.promotionId()).isNull();
        assertThat(discount.amount()).isEqualByComparingTo("0");
        var basket = List.of(new PromotionBasketLine(OIL, null, new BigDecimal("12000"), BigDecimal.ONE));
        verify(promotionService).priceBasket(CHANNEL, basket);
    }

    @Test
    void of_promoCode_pricesTheBasketWithTheCodeAndKeepsTheDiscount() {
        // Arrange
        var promotionId = UUID.randomUUID();
        var command = commandWithCode("OCTO20", new SaleLineCommand(SOAP, new BigDecimal("2"), null));
        when(articleService.getArticlesByIds(List.of(SOAP))).thenReturn(List.of(article(SOAP, RANGE)));
        when(promotionService.priceBasket(eq(CHANNEL), eq("OCTO20"), any()))
                .thenReturn(new PromotionDiscount(promotionId, "Code Octobre", new BigDecimal("3400.00")));

        // Act
        var discount = sut.of(command, List.of(priced(SOAP, "2", "8500")));

        // Assert
        assertThat(discount).isEqualTo(new SaleDiscount(promotionId, new BigDecimal("3400.00")));
        var basket = List.of(new PromotionBasketLine(SOAP, RANGE, new BigDecimal("8500"), new BigDecimal("2")));
        verify(promotionService).priceBasket(CHANNEL, "OCTO20", basket);
        verify(promotionService, never()).priceBasket(eq(CHANNEL), any());
    }

    @Test
    void of_promoCodeOnAgreedPriceLinesOnly_isRefusedByTheServerBecauseTheBasketIsEmpty() {
        // Arrange : an agreed price is never discounted, so the code has nothing to apply to
        var command = commandWithCode("OCTO20", new SaleLineCommand(SOAP, BigDecimal.ONE, new BigDecimal("8000")));
        when(promotionService.priceBasket(CHANNEL, "OCTO20", List.of()))
                .thenThrow(new BusinessException("Ce code promo ne donne aucune remise sur ce panier."));
        var lines = List.of(priced(SOAP, "1", "8000"));

        // Act + Assert
        assertThatThrownBy(() -> sut.of(command, lines)).isInstanceOf(BusinessException.class);
    }

    @Test
    void of_noPromoCode_neverAsksForACode() {
        // Arrange
        var command = command(new SaleLineCommand(SOAP, BigDecimal.ONE, null));
        when(articleService.getArticlesByIds(List.of(SOAP))).thenReturn(List.of(article(SOAP, null)));
        when(promotionService.priceBasket(any(), any())).thenReturn(PromotionDiscount.NONE);

        // Act
        sut.of(command, List.of(priced(SOAP, "1", "8500")));

        // Assert
        verify(promotionService, never()).priceBasket(any(), any(String.class), any());
    }

    private static SaleCommand commandWithCode(String code, SaleLineCommand... lines) {
        return new SaleCommand(CHANNEL, UUID.randomUUID(), PaymentMethod.CASH, List.of(lines), code);
    }

    private static SaleCommand command(SaleLineCommand... lines) {
        return new SaleCommand(CHANNEL, UUID.randomUUID(), PaymentMethod.CASH, List.of(lines));
    }

    private static PricedSaleLine priced(UUID articleId, String quantity, String unitPrice) {
        return new PricedSaleLine(articleId, new BigDecimal(quantity), new BigDecimal(unitPrice), null);
    }

    private static ArticleRDto article(UUID id, UUID rangeId) {
        return new ArticleRDto(
                id,
                "ART-0031",
                "Savon noir 100 g",
                null,
                ArticleType.FINISHED,
                rangeId,
                ArticleUnit.PIECE,
                new BigDecimal("3120.50"),
                new BigDecimal("8500"),
                BigDecimal.TEN,
                null,
                true,
                Instant.EPOCH,
                Instant.EPOCH,
                0);
    }
}
