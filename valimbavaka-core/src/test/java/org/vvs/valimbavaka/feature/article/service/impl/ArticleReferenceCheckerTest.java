package org.vvs.valimbavaka.feature.article.service.impl;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;
import org.vvs.valimbavaka.feature.article.dto.ArticleWDto;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.supplier.service.SupplierService;

@ExtendWith(MockitoExtension.class)
class ArticleReferenceCheckerTest {

    private static final UUID RANGE_ID = UUID.randomUUID();
    private static final UUID SUPPLIER_ID = UUID.randomUUID();

    @Mock
    private ReferentialService referentialService;

    @Mock
    private SupplierService supplierService;

    private ArticleReferenceChecker sut;

    @BeforeEach
    void setUp() {
        sut = new ArticleReferenceChecker(referentialService, supplierService);
    }

    @Test
    void check_finishedProductWithoutRange_throwsBusinessException() {
        // Arrange
        var request = request(ArticleType.FINISHED, null, new BigDecimal("8500"), null);

        // Act + Assert
        assertThatThrownBy(() -> sut.check(request, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("La gamme est obligatoire pour un produit fini.");
    }

    @Test
    void check_finishedProductWithoutPrice_throwsBusinessException() {
        // Arrange
        when(referentialService.isActiveProductRange(RANGE_ID)).thenReturn(true);
        var request = request(ArticleType.FINISHED, RANGE_ID, null, null);

        // Act + Assert
        assertThatThrownBy(() -> sut.check(request, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Le prix est obligatoire pour un produit fini.");
    }

    @Test
    void check_unknownOrInactiveRange_throwsBusinessException() {
        // Arrange
        when(referentialService.isActiveProductRange(RANGE_ID)).thenReturn(false);
        var request = request(ArticleType.FINISHED, RANGE_ID, new BigDecimal("8500"), null);

        // Act + Assert
        assertThatThrownBy(() -> sut.check(request, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Gamme introuvable");
    }

    @Test
    void check_unknownOrInactiveSupplier_throwsBusinessException() {
        // Arrange
        when(supplierService.isActiveSupplier(SUPPLIER_ID)).thenReturn(false);
        var request = request(ArticleType.RAW_MATERIAL, null, null, SUPPLIER_ID);

        // Act + Assert
        assertThatThrownBy(() -> sut.check(request, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Fournisseur introuvable ou désactivé");
    }

    @Test
    void check_supplierDeactivatedButKeptByTheSheet_passesWithoutAskingTheSupplier() {
        // Arrange
        var request = request(ArticleType.RAW_MATERIAL, null, null, SUPPLIER_ID);

        // Act + Assert
        assertThatCode(() -> sut.check(request, SUPPLIER_ID)).doesNotThrowAnyException();
        verifyNoInteractions(supplierService);
    }

    @Test
    void check_rawMaterialWithoutRangeNorPrice_passesWithoutAskingReferentials() {
        // Arrange
        var request = request(ArticleType.RAW_MATERIAL, null, null, null);

        // Act + Assert
        assertThatCode(() -> sut.check(request, null)).doesNotThrowAnyException();
        verifyNoInteractions(referentialService, supplierService);
    }

    @Test
    void check_finishedProductWithActiveRangeAndSupplier_passes() {
        // Arrange
        when(referentialService.isActiveProductRange(RANGE_ID)).thenReturn(true);
        when(supplierService.isActiveSupplier(SUPPLIER_ID)).thenReturn(true);
        var request = request(ArticleType.FINISHED, RANGE_ID, new BigDecimal("8500"), SUPPLIER_ID);

        // Act + Assert
        assertThatCode(() -> sut.check(request, null)).doesNotThrowAnyException();
    }

    private static ArticleWDto request(ArticleType type, UUID rangeId, BigDecimal price, UUID supplierId) {
        return new ArticleWDto(
                "Savon noir 100 g",
                null,
                type,
                rangeId,
                ArticleUnit.PIECE,
                null,
                price,
                BigDecimal.TEN,
                supplierId,
                null);
    }
}
