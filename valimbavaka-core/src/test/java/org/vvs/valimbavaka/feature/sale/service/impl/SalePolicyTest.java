package org.vvs.valimbavaka.feature.sale.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
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
import org.vvs.valimbavaka.feature.sale.domain.PaymentMethod;
import org.vvs.valimbavaka.feature.sale.domain.SaleCommand;
import org.vvs.valimbavaka.feature.sale.domain.SaleLineCommand;
import org.vvs.valimbavaka.feature.sale.dto.SaleChannelOptionRDto;
import org.vvs.valimbavaka.feature.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class SalePolicyTest {

    private static final UUID CHANNEL = UUID.randomUUID();
    private static final UUID STORE = UUID.randomUUID();
    private static final UUID SELLER = UUID.randomUUID();
    private static final UUID SOAP = UUID.randomUUID();

    @Mock
    private SaleChannels channels;

    @Mock
    private UserService userService;

    @Mock
    private ArticleService articleService;

    private SalePolicy sut;

    @BeforeEach
    void setUp() {
        sut = new SalePolicy(channels, userService, articleService);
    }

    @Test
    void check_finishedActiveArticle_pricesTheLineWithTheCurrentPriceAndCost() {
        // Arrange
        arrangeChannelAndSeller();
        when(articleService.getArticleById(SOAP)).thenReturn(article(ArticleType.FINISHED, true, "8500"));

        // Act
        var checked = sut.check(command(new SaleLineCommand(SOAP, new BigDecimal("3"), null)));

        // Assert
        assertThat(checked.channel().stockLocationId()).isEqualTo(STORE);
        var line = checked.lines().getFirst();
        assertThat(line.unitPrice()).isEqualByComparingTo("8500");
        assertThat(line.unitCost()).isEqualByComparingTo("3120.50");
        assertThat(line.quantity()).isEqualByComparingTo("3");
    }

    @Test
    void check_agreedPrice_keepsThePriceOfTheCommand() {
        // Arrange
        arrangeChannelAndSeller();
        when(articleService.getArticleById(SOAP)).thenReturn(article(ArticleType.FINISHED, true, "8500"));

        // Act
        var checked = sut.check(command(new SaleLineCommand(SOAP, BigDecimal.ONE, new BigDecimal("8000"))));

        // Assert
        assertThat(checked.lines().getFirst().unitPrice()).isEqualByComparingTo("8000");
    }

    @Test
    void check_agreedPriceOnAnArticleDeactivatedSince_keepsTheLine() {
        // Arrange
        arrangeChannelAndSeller();
        when(articleService.getArticleById(SOAP)).thenReturn(article(ArticleType.FINISHED, false, "8500"));

        // Act
        var checked = sut.check(command(new SaleLineCommand(SOAP, BigDecimal.ONE, new BigDecimal("8000"))));

        // Assert
        assertThat(checked.lines().getFirst().unitPrice()).isEqualByComparingTo("8000");
    }

    @Test
    void check_noSellerFromAnotherFeature_creditsNobody() {
        // Arrange
        when(channels.require(CHANNEL)).thenReturn(channel());
        when(articleService.getArticleById(SOAP)).thenReturn(article(ArticleType.FINISHED, true, "8500"));
        var line = new SaleLineCommand(SOAP, BigDecimal.ONE, new BigDecimal("8000"));

        // Act
        var checked = sut.check(new SaleCommand(CHANNEL, null, PaymentMethod.ON_DELIVERY, List.of(line)));

        // Assert
        assertThat(checked.lines()).hasSize(1);
        verifyNoInteractions(userService);
    }

    @Test
    void check_inactiveSeller_throwsBusinessException() {
        // Arrange
        when(channels.require(CHANNEL)).thenReturn(channel());
        when(userService.isActiveSeller(SELLER)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> sut.check(command(new SaleLineCommand(SOAP, BigDecimal.ONE, null))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("vendeuse");
    }

    @Test
    void check_noLine_throwsBusinessException() {
        // Arrange
        arrangeChannelAndSeller();

        // Act + Assert
        assertThatThrownBy(() -> sut.check(command()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Une vente compte de 1 à 100 lignes.");
    }

    @Test
    void check_hundredAndOneLines_throwsBusinessException() {
        // Arrange
        arrangeChannelAndSeller();
        var lines = Collections.nCopies(101, new SaleLineCommand(SOAP, BigDecimal.ONE, null));

        // Act + Assert
        assertThatThrownBy(() -> sut.check(new SaleCommand(CHANNEL, SELLER, PaymentMethod.CASH, lines)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void check_sameArticleTwice_throwsBusinessException() {
        // Arrange
        arrangeChannelAndSeller();
        when(articleService.getArticleById(SOAP)).thenReturn(article(ArticleType.FINISHED, true, "8500"));
        var line = new SaleLineCommand(SOAP, BigDecimal.ONE, null);

        // Act + Assert
        assertThatThrownBy(() -> sut.check(command(line, line)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Un article ne peut figurer qu'une fois dans une vente.");
    }

    @Test
    void check_zeroQuantity_throwsBusinessException() {
        // Arrange
        arrangeChannelAndSeller();

        // Act + Assert
        assertThatThrownBy(() -> sut.check(command(new SaleLineCommand(SOAP, BigDecimal.ZERO, null))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void check_rawMaterial_throwsBusinessException() {
        // Arrange
        arrangeChannelAndSeller();
        when(articleService.getArticleById(SOAP)).thenReturn(article(ArticleType.RAW_MATERIAL, true, null));

        // Act + Assert
        assertThatThrownBy(() -> sut.check(command(new SaleLineCommand(SOAP, BigDecimal.ONE, null))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("n'est pas vendable");
    }

    @Test
    void check_inactiveArticle_throwsBusinessException() {
        // Arrange
        arrangeChannelAndSeller();
        when(articleService.getArticleById(SOAP)).thenReturn(article(ArticleType.FINISHED, false, "8500"));

        // Act + Assert
        assertThatThrownBy(() -> sut.check(command(new SaleLineCommand(SOAP, BigDecimal.ONE, null))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("n'est pas vendable");
    }

    @Test
    void check_agreedPriceWithThreeDecimals_throwsBusinessException() {
        // Arrange
        arrangeChannelAndSeller();
        when(articleService.getArticleById(SOAP)).thenReturn(article(ArticleType.FINISHED, true, "8500"));
        var line = new SaleLineCommand(SOAP, BigDecimal.ONE, new BigDecimal("8000.005"));

        // Act + Assert
        assertThatThrownBy(() -> sut.check(command(line))).isInstanceOf(BusinessException.class);
    }

    private void arrangeChannelAndSeller() {
        when(channels.require(CHANNEL)).thenReturn(channel());
        when(userService.isActiveSeller(SELLER)).thenReturn(true);
    }

    private static SaleChannelOptionRDto channel() {
        return new SaleChannelOptionRDto(CHANNEL, "STORE", "Boutique", STORE);
    }

    private static SaleCommand command(SaleLineCommand... lines) {
        return new SaleCommand(CHANNEL, SELLER, PaymentMethod.CASH, List.of(lines));
    }

    private static ArticleRDto article(ArticleType type, boolean active, @Nullable String price) {
        return new ArticleRDto(
                SOAP,
                "ART-0031",
                "Savon noir 100 g",
                null,
                type,
                null,
                ArticleUnit.PIECE,
                new BigDecimal("3120.50"),
                price == null ? null : new BigDecimal(price),
                BigDecimal.TEN,
                null,
                active,
                Instant.EPOCH,
                Instant.EPOCH,
                0);
    }
}
