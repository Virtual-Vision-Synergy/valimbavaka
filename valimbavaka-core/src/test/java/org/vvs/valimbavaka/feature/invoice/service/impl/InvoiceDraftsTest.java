package org.vvs.valimbavaka.feature.invoice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.invoice.config.InvoiceProperties;
import org.vvs.valimbavaka.feature.invoice.domain.InvoiceLineDraft;
import org.vvs.valimbavaka.feature.referential.dto.SalesChannelRDto;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.sale.domain.PaymentMethod;
import org.vvs.valimbavaka.feature.sale.dto.SaleLineRDto;
import org.vvs.valimbavaka.feature.sale.dto.SaleRDto;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.dto.RoleRDto;
import org.vvs.valimbavaka.feature.user.dto.UserRDto;
import org.vvs.valimbavaka.feature.user.service.UserService;
import org.vvs.valimbavaka.shared.dto.PageResponse;
import org.vvs.valimbavaka.shared.operation.OperationStatus;

@ExtendWith(MockitoExtension.class)
class InvoiceDraftsTest {

    private static final Clock NOW = Clock.fixed(Instant.parse("2026-11-08T07:00:00Z"), ZoneOffset.UTC);
    private static final UUID STORE = UUID.randomUUID();
    private static final UUID SELLER = UUID.randomUUID();
    private static final UUID SOAP = UUID.randomUUID();
    private static final UUID OIL = UUID.randomUUID();

    @Mock
    private ReferentialService referentialService;

    @Mock
    private UserService userService;

    @Mock
    private ArticleService articleService;

    @BeforeEach
    void setUp() {
        var channels = List.of(new SalesChannelRDto(STORE, "STORE", "Boutique"));
        lenient()
                .when(referentialService.getSalesChannels(any()))
                .thenReturn(new PageResponse<>(channels, 1, 100, 1, 1));
    }

    @Test
    void of_saleOfTwoArticles_copiesNamesAndFrozenPricesOnTheLines() {
        // Arrange
        var sut = drafts(new InvoiceProperties("By Valimbavaka Cosmetics", "4001234567"));
        when(userService.getUserById(SELLER)).thenReturn(user());
        when(articleService.getArticlesByIds(anyCollection()))
                .thenReturn(List.of(article(SOAP, "Savon coco"), article(OIL, "Huile de ricin")));
        var sale = sale(SELLER, line(SOAP, "2.000", "8500.00", "17000.00"), line(OIL, "1.000", "12000.00", "12000.00"));

        // Act
        var result = sut.of(sale);

        // Assert
        assertThat(result.saleNumber()).isEqualTo("VTE-0042");
        assertThat(result.issuedAt()).isEqualTo(Instant.parse("2026-11-08T07:00:00Z"));
        assertThat(result.salesChannelLabel()).isEqualTo("Boutique");
        assertThat(result.sellerName()).isEqualTo("Miora Rakoto");
        assertThat(result.paymentMethod()).isEqualTo("MVOLA");
        assertThat(result.issuerName()).isEqualTo("By Valimbavaka Cosmetics");
        assertThat(result.issuerNif()).isEqualTo("4001234567");
        assertThat(result.total()).isEqualByComparingTo("29000.00");
        assertThat(result.lines())
                .extracting(InvoiceLineDraft::label, InvoiceLineDraft::unitPrice, InvoiceLineDraft::lineTotal)
                .containsExactly(
                        tuple("Savon coco", new BigDecimal("8500.00"), new BigDecimal("17000.00")),
                        tuple("Huile de ricin", new BigDecimal("12000.00"), new BigDecimal("12000.00")));
    }

    @Test
    void of_saleWithoutSellerOnADeactivatedChannelAndBlankNif_leavesThemEmpty() {
        // Arrange
        var sut = drafts(new InvoiceProperties("By Valimbavaka Cosmetics", "  "));
        when(articleService.getArticlesByIds(anyCollection())).thenReturn(List.of(article(SOAP, "Savon coco")));
        var sale = new SaleRDto(
                UUID.randomUUID(),
                "VTE-0043",
                UUID.randomUUID(),
                LocalDate.of(2026, 11, 8),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                PaymentMethod.ON_DELIVERY,
                OperationStatus.VALIDATED,
                new BigDecimal("8500.00"),
                new BigDecimal("0.00"),
                new BigDecimal("8500.00"),
                List.of(line(SOAP, "1.000", "8500.00", "8500.00")),
                Instant.EPOCH,
                null);

        // Act
        var result = sut.of(sale);

        // Assert
        assertThat(result.sellerId()).isNull();
        assertThat(result.sellerName()).isNull();
        assertThat(result.salesChannelLabel()).isNull();
        assertThat(result.issuerNif()).isNull();
        verify(userService, never()).getUserById(any());
    }

    @Test
    void of_articleUnknownToTheCatalogue_keepsItsIdAsLabelInsteadOfFailing() {
        // Arrange
        var sut = drafts(new InvoiceProperties("By Valimbavaka Cosmetics", null));
        when(userService.getUserById(SELLER)).thenReturn(user());
        when(articleService.getArticlesByIds(anyCollection())).thenReturn(List.of());

        // Act
        var result = sut.of(sale(SELLER, line(SOAP, "1.000", "8500.00", "8500.00")));

        // Assert
        assertThat(result.lines().getFirst().label()).isEqualTo(SOAP.toString());
        assertThat(result.issuerNif()).isNull();
    }

    private InvoiceDrafts drafts(InvoiceProperties properties) {
        return new InvoiceDrafts(referentialService, userService, articleService, properties, NOW);
    }

    private static SaleRDto sale(UUID sellerId, SaleLineRDto... lines) {
        var total = List.of(lines).stream().map(SaleLineRDto::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SaleRDto(
                UUID.randomUUID(),
                "VTE-0042",
                UUID.randomUUID(),
                LocalDate.of(2026, 11, 8),
                STORE,
                UUID.randomUUID(),
                sellerId,
                PaymentMethod.MVOLA,
                OperationStatus.VALIDATED,
                total,
                new BigDecimal("0.00"),
                total,
                List.of(lines),
                Instant.EPOCH,
                sellerId);
    }

    private static SaleLineRDto line(UUID articleId, String quantity, String unitPrice, String lineTotal) {
        return new SaleLineRDto(
                articleId, new BigDecimal(quantity), new BigDecimal(unitPrice), new BigDecimal(lineTotal));
    }

    private static UserRDto user() {
        var role = new RoleRDto(UUID.randomUUID(), RoleName.SELLER, "Vendeuse");
        return new UserRDto(
                SELLER,
                "miora@valimbavaka.mg",
                "Miora",
                "Rakoto",
                role,
                STORE,
                new BigDecimal("5.00"),
                true,
                false,
                Instant.EPOCH,
                Instant.EPOCH);
    }

    private static ArticleRDto article(UUID id, String name) {
        return new ArticleRDto(
                id,
                "ART-0001",
                name,
                null,
                ArticleType.FINISHED,
                null,
                ArticleUnit.PIECE,
                null,
                null,
                BigDecimal.TEN,
                null,
                true,
                Instant.EPOCH,
                Instant.EPOCH,
                0L);
    }
}
