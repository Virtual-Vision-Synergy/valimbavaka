package org.vvs.valimbavaka.feature.article.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import org.vvs.valimbavaka.feature.article.domain.Article;
import org.vvs.valimbavaka.feature.article.domain.ArticleDetails;
import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;
import org.vvs.valimbavaka.feature.referential.repository.ProductRangeRepository;
import org.vvs.valimbavaka.feature.supplier.domain.Supplier;
import org.vvs.valimbavaka.feature.supplier.domain.SupplierDetails;
import org.vvs.valimbavaka.feature.supplier.domain.SupplierType;
import org.vvs.valimbavaka.feature.supplier.repository.SupplierRepository;
import org.vvs.valimbavaka.support.IntegrationTest;

/** ART-01: persistence, paged search, code sequence and barcode uniqueness of articles on PostgreSQL. */
@IntegrationTest
class ArticleRepositoryIT {

    @Autowired
    private ArticleRepository sut;

    @Autowired
    private ProductRangeRepository productRangeRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Test
    void save_newArticle_isPersistedWithAuditFieldsAndExactDecimals() {
        // Act
        var saved = sut.save(article(uniqueName(), ArticleType.FINISHED, null, "12500.50"));

        // Assert
        var reloaded = sut.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.isActive()).isTrue();
        assertThat(reloaded.getPrice()).isEqualByComparingTo("12500.50");
        assertThat(reloaded.getThreshold()).isEqualByComparingTo("40.000");
    }

    @Test
    void nextCodeNumber_calledTwice_returnsIncreasingNumbers() {
        // Act
        var first = sut.nextCodeNumber();
        var second = sut.nextCodeNumber();

        // Assert
        assertThat(second).isGreaterThan(first);
    }

    @Test
    void save_sameCodeTwice_isRefusedByTheDatabase() {
        // Arrange
        var code = "ART-T" + System.nanoTime() % 1_000_000;
        sut.save(Article.create(code, details(uniqueName(), ArticleType.FINISHED, null, "8500")));

        // Act + Assert
        assertThatThrownBy(() -> sut.save(Article.create(code, details(uniqueName(), ArticleType.FINISHED, null, "1"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsByBarcode_usedBarcode_returnsTrueAndIgnoresTheArticleItself() {
        // Arrange
        var barcode = uniqueBarcode();
        var saved = sut.save(article(uniqueName(), ArticleType.FINISHED, barcode, "8500"));

        // Act + Assert
        assertThat(sut.existsByBarcode(barcode)).isTrue();
        assertThat(sut.existsByBarcodeAndIdNot(barcode, saved.getId())).isFalse();
        assertThat(sut.existsByBarcode(uniqueBarcode())).isFalse();
    }

    @Test
    void save_sameBarcodeTwice_isRefusedButSeveralWithoutBarcodeAreAccepted() {
        // Arrange
        var barcode = uniqueBarcode();
        sut.save(article(uniqueName(), ArticleType.FINISHED, barcode, "8500"));
        sut.save(article(uniqueName(), ArticleType.FINISHED, null, "8500"));
        sut.save(article(uniqueName(), ArticleType.FINISHED, null, "8500"));

        // Act + Assert
        assertThatThrownBy(() -> sut.save(article(uniqueName(), ArticleType.FINISHED, barcode, "8500")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void search_byNameCodeOrBarcode_findsTheArticleWhateverTheCase() {
        // Arrange
        var prefix = "Recherche-" + UUID.randomUUID();
        var barcode = uniqueBarcode();
        var saved = sut.save(article(prefix + " Gommage", ArticleType.FINISHED, barcode, "12500"));

        // Act + Assert
        assertThat(sut.search(prefix.toUpperCase(), null, null, null, null, PageRequest.of(0, 10)).getContent())
                .extracting(Article::getId)
                .containsExactly(saved.getId());
        assertThat(sut.search(barcode, null, null, null, null, PageRequest.of(0, 10)).getContent())
                .extracting(Article::getId)
                .containsExactly(saved.getId());
        assertThat(sut.search(saved.getCode().toLowerCase(), null, null, null, null, PageRequest.of(0, 10))
                        .getContent())
                .extracting(Article::getId)
                .containsExactly(saved.getId());
    }

    @Test
    void search_withTypeRangeAndStatusFilters_returnsOnlyMatchingArticles() {
        // Arrange
        var rangeId = productRangeRepository.findAll().getFirst().getId();
        var prefix = "Filtre-" + UUID.randomUUID();
        var finished = sut.save(Article.create(
                nextCode(), details(prefix + "-fini", ArticleType.FINISHED, null, "8500", rangeId)));
        sut.save(article(prefix + "-matiere", ArticleType.RAW_MATERIAL, null, null));
        var inactive = article(prefix + "-inactif", ArticleType.FINISHED, null, "8500");
        inactive.deactivate();
        sut.save(inactive);

        // Act
        var page = sut.search(prefix, ArticleType.FINISHED, rangeId, null, true, PageRequest.of(0, 10));

        // Assert
        assertThat(page.getContent()).extracting(Article::getId).containsExactly(finished.getId());
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void search_bySupplier_returnsOnlyTheArticlesOfThatSupplier() {
        // Arrange
        var supplierName = "Fournisseur-" + UUID.randomUUID();
        var supplier = supplierRepository.save(
                Supplier.create(new SupplierDetails(supplierName, SupplierType.LOCAL, null, null, null, null, null)));
        var supplied = sut.save(Article.create(nextCode(), new ArticleDetails(
                uniqueName(), null, ArticleType.RAW_MATERIAL, null, ArticleUnit.KILOGRAM,
                new BigDecimal("12000"), null, new BigDecimal("5"), supplier.getId())));
        sut.save(article(uniqueName(), ArticleType.RAW_MATERIAL, null, null));

        // Act
        var page = sut.search("", null, null, supplier.getId(), null, PageRequest.of(0, 10));

        // Assert
        assertThat(page.getContent()).extracting(Article::getId).containsExactly(supplied.getId());
    }

    private Article article(String name, ArticleType type, String barcode, String price) {
        return Article.create(nextCode(), details(name, type, barcode, price));
    }

    private String nextCode() {
        return "ART-%04d".formatted(sut.nextCodeNumber());
    }

    private static ArticleDetails details(String name, ArticleType type, String barcode, String price) {
        return details(name, type, barcode, price, null);
    }

    private static ArticleDetails details(String name, ArticleType type, String barcode, String price, UUID rangeId) {
        return new ArticleDetails(
                name, barcode, type, rangeId, ArticleUnit.PIECE, null,
                price == null ? null : new BigDecimal(price), new BigDecimal("40"), null);
    }

    private static String uniqueName() {
        return "Article-" + UUID.randomUUID();
    }

    private static String uniqueBarcode() {
        return String.format("%013d", Math.abs(UUID.randomUUID().getMostSignificantBits()) % 10_000_000_000_000L);
    }
}
