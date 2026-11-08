package org.vvs.valimbavaka.feature.stock.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.stock.domain.SupplierReceiptLineCommand;
import org.vvs.valimbavaka.feature.stock.dto.SupplierReceiptLineWDto;
import org.vvs.valimbavaka.feature.stock.dto.SupplierReceiptWDto;
import org.vvs.valimbavaka.feature.supplier.domain.SupplierType;
import org.vvs.valimbavaka.feature.supplier.dto.SupplierRDto;
import org.vvs.valimbavaka.feature.supplier.service.SupplierService;

/** STK-06: rules of a supplier receipt checked before anything is written. */
@ExtendWith(MockitoExtension.class)
class SupplierReceiptPolicyTest {

    private static final UUID SUPPLIER_ID = UUID.randomUUID();
    private static final UUID ARTICLE_ID = UUID.randomUUID();
    private static final UUID WAREHOUSE_ID = UUID.randomUUID();

    @Mock
    private SupplierService supplierService;

    @Mock
    private ReferentialService referentialService;

    @Mock
    private ArticleService articleService;

    private SupplierReceiptPolicy sut;

    @BeforeEach
    void setUp() {
        sut = new SupplierReceiptPolicy(supplierService, referentialService, articleService);
    }

    @Test
    void check_withoutLocation_receivesInTheWarehouseWithTheCopiedArticleAndSupplier() {
        // Arrange
        givenSupplier(true);
        when(referentialService.requireActiveStockLocationId("WAREHOUSE")).thenReturn(WAREHOUSE_ID);
        when(articleService.getArticleById(ARTICLE_ID)).thenReturn(StockTestData.article(ARTICLE_ID, "ART-0031"));
        var request = request(null, "  Livraison de lundi ", line(ARTICLE_ID));

        // Act
        var result = sut.check(request);

        // Assert
        assertThat(result.locationId()).isEqualTo(WAREHOUSE_ID);
        assertThat(result.supplierName()).isEqualTo("Huiles Ravo");
        assertThat(result.note()).isEqualTo("Livraison de lundi");
        assertThat(result.lines())
                .containsExactly(new SupplierReceiptLineCommand(
                        ARTICLE_ID, "ART-0031", "Savon noir 100 g", new BigDecimal("12"), new BigDecimal("3000")));
    }

    @Test
    void check_deactivatedSupplier_throwsBusinessExceptionBeforeReadingTheArticles() {
        // Arrange
        givenSupplier(false);
        var request = request(null, null, line(ARTICLE_ID));

        // Act + Assert
        assertThatThrownBy(() -> sut.check(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("désactivé");
        verify(articleService, never()).getArticleById(any());
    }

    @Test
    void check_inactiveLocation_throwsBusinessException() {
        // Arrange
        givenSupplier(true);
        var locationId = UUID.randomUUID();
        when(referentialService.isActiveStockLocation(locationId)).thenReturn(false);
        var request = request(locationId, null, line(ARTICLE_ID));

        // Act + Assert
        assertThatThrownBy(() -> sut.check(request)).isInstanceOf(BusinessException.class);
    }

    @Test
    void check_sameArticleTwice_throwsBusinessException() {
        // Arrange
        givenSupplier(true);
        when(referentialService.requireActiveStockLocationId("WAREHOUSE")).thenReturn(WAREHOUSE_ID);
        when(articleService.getArticleById(ARTICLE_ID)).thenReturn(StockTestData.article(ARTICLE_ID, "ART-0031"));
        var request = request(null, null, line(ARTICLE_ID), line(ARTICLE_ID));

        // Act + Assert
        assertThatThrownBy(() -> sut.check(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Un article ne peut figurer qu'une fois dans une réception.");
    }

    private void givenSupplier(boolean active) {
        var now = Instant.parse("2026-11-08T08:00:00Z");
        var supplier = new SupplierRDto(
                SUPPLIER_ID, "Huiles Ravo", SupplierType.LOCAL, null, null, null, null, null, 7, active, now, now, 0);
        when(supplierService.getSupplierById(SUPPLIER_ID)).thenReturn(supplier);
    }

    private static SupplierReceiptWDto request(UUID locationId, String note, SupplierReceiptLineWDto... lines) {
        return new SupplierReceiptWDto(SUPPLIER_ID, locationId, null, List.of(lines), note);
    }

    private static SupplierReceiptLineWDto line(UUID articleId) {
        return new SupplierReceiptLineWDto(articleId, new BigDecimal("12"), new BigDecimal("3000"));
    }
}
