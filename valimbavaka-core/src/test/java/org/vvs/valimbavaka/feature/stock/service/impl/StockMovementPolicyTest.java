package org.vvs.valimbavaka.feature.stock.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementCommand;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.stock.repository.StockMovementRepository;

@ExtendWith(MockitoExtension.class)
class StockMovementPolicyTest {

    private static final UUID ARTICLE_ID = UUID.randomUUID();
    private static final UUID LOCATION_ID = UUID.randomUUID();

    @Mock
    private ArticleService articleService;

    @Mock
    private ReferentialService referentialService;

    @Mock
    private StockMovementRepository stockMovementRepository;

    private StockMovementPolicy sut;

    @BeforeEach
    void setUp() {
        sut = new StockMovementPolicy(articleService, referentialService, stockMovementRepository);
    }

    @Test
    void check_validMovement_returnsTheArticleAndLocksBeforeReadingTheBalance() {
        // Arrange
        var article = StockTestData.article(ARTICLE_ID, "ART-0031");
        when(articleService.getArticleById(ARTICLE_ID)).thenReturn(article);
        when(referentialService.isActiveStockLocation(LOCATION_ID)).thenReturn(true);
        when(stockMovementRepository.balance(ARTICLE_ID, LOCATION_ID)).thenReturn(BigDecimal.ZERO);

        // Act
        var result = sut.check(command(StockMovementType.RECEIPT, "12"));

        // Assert
        assertThat(result).isSameAs(article);
        var order = inOrder(stockMovementRepository);
        order.verify(stockMovementRepository).lock(ARTICLE_ID + ":" + LOCATION_ID);
        order.verify(stockMovementRepository).balance(ARTICLE_ID, LOCATION_ID);
    }

    @Test
    void check_unknownArticle_propagatesNotFound() {
        // Arrange
        when(articleService.getArticleById(ARTICLE_ID)).thenThrow(new NotFoundException("Article introuvable"));

        // Act + Assert
        assertThatThrownBy(() -> sut.check(command(StockMovementType.RECEIPT, "1")))
                .isInstanceOf(NotFoundException.class);
        verify(stockMovementRepository, never()).lock(anyString());
    }

    @Test
    void check_inactiveLocation_throwsBusinessException() {
        // Arrange
        when(articleService.getArticleById(ARTICLE_ID)).thenReturn(StockTestData.article(ARTICLE_ID, "ART-0031"));
        when(referentialService.isActiveStockLocation(LOCATION_ID)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> sut.check(command(StockMovementType.RECEIPT, "1")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("emplacement");
    }

    @Test
    void check_zeroQuantity_throwsBusinessException() {
        // Arrange
        arrangeKnownArticleAndLocation();

        // Act + Assert
        assertThatThrownBy(() -> sut.check(command(StockMovementType.INVENTORY, "0")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("nulle");
    }

    @Test
    void check_positiveSale_throwsBusinessException() {
        // Arrange
        arrangeKnownArticleAndLocation();

        // Act + Assert
        assertThatThrownBy(() -> sut.check(command(StockMovementType.SALE, "2")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("SALE");
    }

    @Test
    void check_fourDecimals_throwsBusinessException() {
        // Arrange
        arrangeKnownArticleAndLocation();

        // Act + Assert
        assertThatThrownBy(() -> sut.check(command(StockMovementType.RECEIPT, "0.1234")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("3 décimales");
    }

    @Test
    void check_threeDecimals_isAccepted() {
        // Arrange
        arrangeKnownArticleAndLocation();
        when(stockMovementRepository.balance(ARTICLE_ID, LOCATION_ID)).thenReturn(BigDecimal.ZERO);

        // Act + Assert
        assertThat(sut.check(command(StockMovementType.RECEIPT, "0.125"))).isNotNull();
    }

    @Test
    void check_withdrawalAboveBalance_throwsConflict() {
        // Arrange
        arrangeKnownArticleAndLocation();
        when(stockMovementRepository.balance(ARTICLE_ID, LOCATION_ID)).thenReturn(new BigDecimal("3.000"));

        // Act + Assert
        assertThatThrownBy(() -> sut.check(command(StockMovementType.SALE, "-4")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("3 disponible");
    }

    @Test
    void check_withdrawalOfTheWholeBalance_isAccepted() {
        // Arrange
        arrangeKnownArticleAndLocation();
        when(stockMovementRepository.balance(ARTICLE_ID, LOCATION_ID)).thenReturn(new BigDecimal("3.000"));

        // Act + Assert
        assertThat(sut.check(command(StockMovementType.SALE, "-3"))).isNotNull();
    }

    private void arrangeKnownArticleAndLocation() {
        when(articleService.getArticleById(ARTICLE_ID)).thenReturn(StockTestData.article(ARTICLE_ID, "ART-0031"));
        when(referentialService.isActiveStockLocation(LOCATION_ID)).thenReturn(true);
    }

    private static StockMovementCommand command(StockMovementType type, String quantity) {
        return new StockMovementCommand(ARTICLE_ID, LOCATION_ID, type, new BigDecimal(quantity), null);
    }
}
