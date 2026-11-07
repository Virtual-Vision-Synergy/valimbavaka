package org.vvs.valimbavaka.feature.transfer.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.transfer.domain.Transfer;
import org.vvs.valimbavaka.feature.transfer.domain.TransferCommand;
import org.vvs.valimbavaka.feature.transfer.domain.TransferLineCommand;
import org.vvs.valimbavaka.feature.transfer.domain.TransferOrigin;
import org.vvs.valimbavaka.feature.transfer.domain.TransferStatus;

@ExtendWith(MockitoExtension.class)
class TransferSheetTest {

    private static final UUID STORE = UUID.randomUUID();
    private static final UUID WAREHOUSE = UUID.randomUUID();
    private static final UUID SOAP = UUID.randomUUID();
    private static final UUID OIL = UUID.randomUUID();
    private static final List<ArticleRDto> ARTICLES = List.of(
            article(SOAP, "ART-0001", "Savon au curcuma", ArticleUnit.PIECE),
            article(OIL, "ART-0002", "Huile de coco", ArticleUnit.LITER));

    @Mock
    private TransferHistory history;

    @Mock
    private ArticleService articleService;

    private TransferSheet sut;

    @BeforeEach
    void setUp() {
        sut = new TransferSheet(history, articleService);
        when(history.locationLabels()).thenReturn(Map.of(STORE, "Boutique", WAREHOUSE, "Entrepôt"));
    }

    @Test
    void toRDto_returnWithTwoLines_showsArticlesQuantitiesRouteAndIssuer() {
        // Arrange
        var transfer = transfer(STORE, " Invendus du samedi ", line(OIL, "2.5"), line(SOAP, "3"));
        when(articleService.getArticlesByIds(anyCollection())).thenReturn(ARTICLES);
        when(history.issuerName(isNull(), any())).thenReturn(TransferHistory.SYSTEM_ISSUER);

        // Act
        var sheet = sut.toRDto(transfer);

        // Assert
        assertThat(sheet.number()).isEqualTo("RET-0015");
        assertThat(sheet.origin()).isEqualTo(TransferOrigin.RETURN);
        assertThat(sheet.status()).isEqualTo(TransferStatus.RETURN);
        assertThat(sheet.fromLocationLabel()).isEqualTo("Boutique");
        assertThat(sheet.toLocationLabel()).isEqualTo("Entrepôt");
        assertThat(sheet.note()).isEqualTo("Invendus du samedi");
        assertThat(sheet.quantity()).isEqualByComparingTo("5.5");
        assertThat(sheet.createdByName()).isEqualTo(TransferHistory.SYSTEM_ISSUER);
        assertThat(sheet.lines())
                .extracting(row -> row.articleCode() + " " + row.articleName() + " " + row.unit())
                .containsExactly("ART-0002 Huile de coco LITER", "ART-0001 Savon au curcuma PIECE");
        assertThat(sheet.lines().getFirst().quantity()).isEqualByComparingTo("2.5");
        assertThat(sheet.lines().get(1).articleId()).isEqualTo(SOAP);
    }

    @Test
    void toRDto_severalLines_readsAllTheArticlesOnce() {
        // Arrange
        var transfer = transfer(STORE, null, line(OIL, "1"), line(SOAP, "1"));
        when(articleService.getArticlesByIds(anyCollection())).thenReturn(ARTICLES);

        // Act
        sut.toRDto(transfer);

        // Assert
        verify(articleService, times(1)).getArticlesByIds(List.of(OIL, SOAP));
        verify(history, times(1)).locationLabels();
    }

    @Test
    void toRDto_locationDeactivatedSince_keepsTheIdentifierWithoutLabel() {
        // Arrange
        var deactivated = UUID.randomUUID();
        var transfer = transfer(deactivated, null, line(SOAP, "1"));
        when(articleService.getArticlesByIds(anyCollection())).thenReturn(ARTICLES);

        // Act
        var sheet = sut.toRDto(transfer);

        // Assert
        assertThat(sheet.fromLocationId()).isEqualTo(deactivated);
        assertThat(sheet.fromLocationLabel()).isNull();
        assertThat(sheet.toLocationLabel()).isEqualTo("Entrepôt");
        assertThat(sheet.note()).isNull();
    }

    @Test
    void toRDto_articleMissing_failsInsteadOfShowingAnEmptyLine() {
        // Arrange
        var transfer = transfer(STORE, null, line(SOAP, "1"));
        when(articleService.getArticlesByIds(anyCollection())).thenReturn(List.of());

        // Act + Assert
        assertThatThrownBy(() -> sut.toRDto(transfer))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(SOAP.toString());
    }

    private static Transfer transfer(UUID from, @Nullable String note, TransferLineCommand... lines) {
        var command = new TransferCommand(TransferOrigin.RETURN, from, WAREHOUSE, List.of(lines), note);
        return Transfer.create("RET-0015", command);
    }

    private static TransferLineCommand line(UUID articleId, String quantity) {
        return new TransferLineCommand(articleId, new BigDecimal(quantity));
    }

    private static ArticleRDto article(UUID id, String code, String name, ArticleUnit unit) {
        var now = Instant.parse("2026-11-08T07:30:00Z");
        return new ArticleRDto(
                id,
                code,
                name,
                null,
                ArticleType.FINISHED,
                null,
                unit,
                null,
                null,
                BigDecimal.ZERO,
                null,
                false,
                now,
                now,
                0L);
    }
}
