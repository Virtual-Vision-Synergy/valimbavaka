package org.vvs.valimbavaka.feature.sale.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.referential.dto.SalesChannelRDto;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.sale.domain.PaymentMethod;
import org.vvs.valimbavaka.feature.sale.domain.PricedSaleLine;
import org.vvs.valimbavaka.feature.sale.domain.Sale;
import org.vvs.valimbavaka.feature.sale.domain.SaleHeader;
import org.vvs.valimbavaka.feature.sale.dto.SaleSummaryLineRDto;
import org.vvs.valimbavaka.feature.sale.dto.SaleSummaryRDto;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.dto.RoleRDto;
import org.vvs.valimbavaka.feature.user.dto.UserRDto;
import org.vvs.valimbavaka.feature.user.service.UserService;
import org.vvs.valimbavaka.shared.dto.PageResponse;
import org.vvs.valimbavaka.shared.operation.OperationStatus;

@ExtendWith(MockitoExtension.class)
class SaleSummaryAssemblerTest {

    private static final UUID STORE = UUID.randomUUID();
    private static final UUID PHONE = UUID.randomUUID();
    private static final UUID SELLER = UUID.randomUUID();
    private static final UUID SOAP = UUID.randomUUID();
    private static final UUID OIL = UUID.randomUUID();

    @Mock
    private ReferentialService referentialService;

    @Mock
    private UserService userService;

    @Mock
    private ArticleService articleService;

    private SaleSummaryAssembler sut;

    @BeforeEach
    void setUp() {
        sut = new SaleSummaryAssembler(referentialService, userService, articleService);
        var channels = List.of(new SalesChannelRDto(STORE, "STORE", "Boutique"));
        when(referentialService.getSalesChannels(any())).thenReturn(new PageResponse<>(channels, 1, 100, 1, 1));
    }

    @Test
    void toRDtos_twoSalesOfOneSeller_keepsThePageOrderAndReadsEachNameOnce() {
        // Arrange
        var first = sale("VTE-0041", STORE, SELLER, line(SOAP, "2", "8500"));
        var second = sale("VTE-0042", STORE, SELLER, line(SOAP, "1", "8500"), line(OIL, "1", "12000"));
        when(userService.getUserById(SELLER)).thenReturn(seller());
        when(articleService.getArticlesByIds(anyCollection()))
                .thenReturn(List.of(article(SOAP, "Savon coco"), article(OIL, "Huile de ricin")));

        // Act
        var result = sut.toRDtos(List.of(second.getId(), first.getId()), List.of(first, second));

        // Assert
        assertThat(result).extracting(SaleSummaryRDto::number).containsExactly("VTE-0042", "VTE-0041");
        var latest = result.getFirst();
        assertThat(latest.salesChannelLabel()).isEqualTo("Boutique");
        assertThat(latest.sellerName()).isEqualTo("Miora Rakoto");
        assertThat(latest.paymentMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(latest.status()).isEqualTo(OperationStatus.VALIDATED);
        assertThat(latest.discountAmount()).isEqualByComparingTo("0");
        assertThat(latest.total()).isEqualByComparingTo("20500");
        assertThat(latest.lines())
                .extracting(SaleSummaryLineRDto::articleName, SaleSummaryLineRDto::lineTotal)
                .containsExactly(
                        tuple("Savon coco", new BigDecimal("8500.00")),
                        tuple("Huile de ricin", new BigDecimal("12000.00")));
        verify(userService, times(1)).getUserById(SELLER);
        verify(articleService, times(1)).getArticlesByIds(anyCollection());
    }

    @Test
    void toRDtos_saleWithoutSellerOnADeactivatedChannel_hasNoSellerNameNorChannelLabel() {
        // Arrange
        var delivered = sale("VTE-0043", PHONE, null, line(SOAP, "1", "8500"));
        when(articleService.getArticlesByIds(anyCollection())).thenReturn(List.of());

        // Act
        var result = sut.toRDtos(List.of(delivered.getId()), List.of(delivered));

        // Assert
        var row = result.getFirst();
        assertThat(row.sellerId()).isNull();
        assertThat(row.sellerName()).isNull();
        assertThat(row.salesChannelLabel()).isNull();
        assertThat(row.lines().getFirst().articleName()).isNull();
        assertThat(row.lines().getFirst().articleId()).isEqualTo(SOAP);
    }

    @Test
    void toRDtos_emptyPage_returnsNoRow() {
        // Arrange
        when(articleService.getArticlesByIds(anyCollection())).thenReturn(List.of());

        // Act
        var result = sut.toRDtos(List.of(), List.of());

        // Assert
        assertThat(result).isEmpty();
    }

    private static Sale sale(String number, UUID channelId, @Nullable UUID sellerId, PricedSaleLine... lines) {
        var header = new SaleHeader(
                UUID.randomUUID(), LocalDate.of(2026, 11, 8), channelId, STORE, sellerId, PaymentMethod.CASH);
        var sale = Sale.record(number, header, List.of(lines));
        ReflectionTestUtils.setField(sale, "id", UUID.randomUUID());
        return sale;
    }

    private static PricedSaleLine line(UUID articleId, String quantity, String unitPrice) {
        return new PricedSaleLine(articleId, new BigDecimal(quantity), new BigDecimal(unitPrice), null);
    }

    private static UserRDto seller() {
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
                new BigDecimal("8500"),
                BigDecimal.TEN,
                null,
                true,
                Instant.EPOCH,
                Instant.EPOCH,
                0L);
    }
}
