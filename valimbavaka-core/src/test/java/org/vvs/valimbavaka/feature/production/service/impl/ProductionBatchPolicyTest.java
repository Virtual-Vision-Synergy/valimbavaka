package org.vvs.valimbavaka.feature.production.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.vvs.valimbavaka.feature.production.service.impl.ProductionTestData.BASE_ID;
import static org.vvs.valimbavaka.feature.production.service.impl.ProductionTestData.OIL_ID;
import static org.vvs.valimbavaka.feature.production.service.impl.ProductionTestData.OPERATOR_ID;
import static org.vvs.valimbavaka.feature.production.service.impl.ProductionTestData.OUTPUT_ID;
import static org.vvs.valimbavaka.feature.production.service.impl.ProductionTestData.finishedProduct;
import static org.vvs.valimbavaka.feature.production.service.impl.ProductionTestData.request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.article.domain.ProductionUsage;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class ProductionBatchPolicyTest {

    @Mock
    private ArticleService articleService;

    @Mock
    private UserService userService;

    private ProductionBatchPolicy sut;

    @BeforeEach
    void setUp() {
        sut = new ProductionBatchPolicy(articleService, userService);
    }

    @Test
    void check_validBatch_checksOutputAndEveryInputAndReturnsTheOutput() {
        // Arrange
        when(userService.isActiveSeller(OPERATOR_ID)).thenReturn(true);
        when(articleService.requireProductionArticle(OUTPUT_ID, ProductionUsage.OUTPUT))
                .thenReturn(finishedProduct());

        // Act
        var result = sut.check(request(OIL_ID, BASE_ID));

        // Assert
        assertThat(result.code()).isEqualTo("ART-0031");
        verify(articleService).requireProductionArticle(OIL_ID, ProductionUsage.INPUT);
        verify(articleService).requireProductionArticle(BASE_ID, ProductionUsage.INPUT);
    }

    @Test
    void check_sameInputTwice_throwsBusinessExceptionBeforeAnyLookup() {
        // Act + Assert
        assertThatThrownBy(() -> sut.check(request(OIL_ID, OIL_ID)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ProductionBatchPolicy.DUPLICATE_INPUT);
        verifyNoInteractions(articleService, userService);
    }

    @Test
    void check_operatorNotAnActiveSeller_throwsBusinessException() {
        // Arrange
        when(userService.isActiveSeller(OPERATOR_ID)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> sut.check(request(OIL_ID)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ProductionBatchPolicy.NOT_A_SELLER);
        verifyNoInteractions(articleService);
    }

    @Test
    void check_inputRefusedByArticleRules_propagatesTheError() {
        // Arrange
        when(userService.isActiveSeller(OPERATOR_ID)).thenReturn(true);
        when(articleService.requireProductionArticle(OUTPUT_ID, ProductionUsage.OUTPUT))
                .thenReturn(finishedProduct());
        when(articleService.requireProductionArticle(OIL_ID, ProductionUsage.INPUT))
                .thenThrow(new NotFoundException("Article introuvable"));

        // Act + Assert
        assertThatThrownBy(() -> sut.check(request(OIL_ID))).isInstanceOf(NotFoundException.class);
    }
}
