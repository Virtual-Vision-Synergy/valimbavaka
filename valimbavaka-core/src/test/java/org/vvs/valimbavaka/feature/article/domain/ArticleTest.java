package org.vvs.valimbavaka.feature.article.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ArticleTest {

    private static final UUID RANGE_ID = UUID.randomUUID();

    @Test
    void create_finishedProduct_keepsRangePriceAndIsActive() {
        // Act
        var sut = Article.create("ART-0001", finished("Savon noir 100 g"));

        // Assert
        assertThat(sut.getCode()).isEqualTo("ART-0001");
        assertThat(sut.getType()).isEqualTo(ArticleType.FINISHED);
        assertThat(sut.getRangeId()).isEqualTo(RANGE_ID);
        assertThat(sut.getPrice()).isEqualByComparingTo("8500");
        assertThat(sut.isActive()).isTrue();
    }

    @Test
    void create_rawMaterial_dropsRangeAndPrice() {
        // Arrange
        var details = new ArticleDetails(
                "Beurre de karité brut",
                null,
                ArticleType.RAW_MATERIAL,
                RANGE_ID,
                ArticleUnit.KILOGRAM,
                new BigDecimal("12000"),
                new BigDecimal("15000"),
                new BigDecimal("5"),
                null);

        // Act
        var sut = Article.create("ART-0002", details);

        // Assert
        assertThat(sut.getRangeId()).isNull();
        assertThat(sut.getPrice()).isNull();
        assertThat(sut.getCost()).isEqualByComparingTo("12000");
    }

    @Test
    void create_nameBarcodeWithSpaces_isStrippedAndBlankBarcodeBecomesNull() {
        // Act
        var sut = Article.create(
                "ART-0003",
                new ArticleDetails(
                        "  Gommage café 200 g ",
                        "  ",
                        ArticleType.FINISHED,
                        RANGE_ID,
                        ArticleUnit.PIECE,
                        null,
                        new BigDecimal("12500"),
                        BigDecimal.ZERO,
                        null));

        // Assert
        assertThat(sut.getName()).isEqualTo("Gommage café 200 g");
        assertThat(sut.getBarcode()).isNull();
    }

    @Test
    void update_sameValuesWithOtherScale_returnsFalse() {
        // Arrange
        var sut = Article.create("ART-0004", finished("Savon noir 100 g"));
        var same = new ArticleDetails(
                " Savon noir 100 g ",
                null,
                ArticleType.FINISHED,
                RANGE_ID,
                ArticleUnit.PIECE,
                new BigDecimal("3120.0"),
                new BigDecimal("8500.000"),
                new BigDecimal("40"),
                null);

        // Act + Assert
        assertThat(sut.update(same)).isFalse();
    }

    @Test
    void update_changedPrice_returnsTrueAndKeepsTheCode() {
        // Arrange
        var sut = Article.create("ART-0005", finished("Savon noir 100 g"));
        var changed = new ArticleDetails(
                "Savon noir 100 g",
                null,
                ArticleType.FINISHED,
                RANGE_ID,
                ArticleUnit.PIECE,
                new BigDecimal("3120"),
                new BigDecimal("9000.50"),
                new BigDecimal("40"),
                null);

        // Act
        var result = sut.update(changed);

        // Assert
        assertThat(result).isTrue();
        assertThat(sut.getPrice()).isEqualByComparingTo("9000.50");
        assertThat(sut.getCode()).isEqualTo("ART-0005");
    }

    @Test
    void update_finishedProductBecomesSemiFinished_dropsRangeAndPrice() {
        // Arrange
        var sut = Article.create("ART-0006", finished("Pâte à savon"));
        var changed = new ArticleDetails(
                "Pâte à savon",
                null,
                ArticleType.SEMI_FINISHED,
                RANGE_ID,
                ArticleUnit.KILOGRAM,
                new BigDecimal("3120"),
                new BigDecimal("8500"),
                new BigDecimal("40"),
                null);

        // Act
        var result = sut.update(changed);

        // Assert
        assertThat(result).isTrue();
        assertThat(sut.getType()).isEqualTo(ArticleType.SEMI_FINISHED);
        assertThat(sut.getRangeId()).isNull();
        assertThat(sut.getPrice()).isNull();
    }

    @Test
    void deactivateThenActivate_togglesTheState() {
        // Arrange
        var sut = Article.create("ART-0007", finished("Savon noir 100 g"));

        // Act + Assert
        sut.deactivate();
        assertThat(sut.isActive()).isFalse();
        sut.activate();
        assertThat(sut.isActive()).isTrue();
    }

    private static ArticleDetails finished(String name) {
        return new ArticleDetails(
                name,
                null,
                ArticleType.FINISHED,
                RANGE_ID,
                ArticleUnit.PIECE,
                new BigDecimal("3120"),
                new BigDecimal("8500"),
                new BigDecimal("40"),
                null);
    }
}
