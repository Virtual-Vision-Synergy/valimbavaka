package org.vvs.valimbavaka.feature.article.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/** ART-04: the article type alone decides the production usage. */
class ProductionUsageTest {

    @Test
    void accepts_rawMaterialAndSemiFinished_areInputsOnly() {
        // Arrange
        var sut = ProductionUsage.INPUT;

        // Act + Assert
        assertThat(sut.accepts(ArticleType.RAW_MATERIAL)).isTrue();
        assertThat(sut.accepts(ArticleType.SEMI_FINISHED)).isTrue();
        assertThat(sut.accepts(ArticleType.FINISHED)).isFalse();
    }

    @Test
    void accepts_finishedProduct_isOutputOnly() {
        // Arrange
        var sut = ProductionUsage.OUTPUT;

        // Act + Assert
        assertThat(sut.accepts(ArticleType.FINISHED)).isTrue();
        assertThat(sut.accepts(ArticleType.RAW_MATERIAL)).isFalse();
        assertThat(sut.accepts(ArticleType.SEMI_FINISHED)).isFalse();
    }

    @Test
    void types_everyArticleType_hasExactlyOneUsage() {
        for (ArticleType type : ArticleType.values()) {
            // Act
            var usages = Arrays.stream(ProductionUsage.values())
                    .filter(usage -> usage.accepts(type))
                    .count();

            // Assert
            assertThat(usages).as("usages of %s", type).isEqualTo(1);
        }
    }
}
