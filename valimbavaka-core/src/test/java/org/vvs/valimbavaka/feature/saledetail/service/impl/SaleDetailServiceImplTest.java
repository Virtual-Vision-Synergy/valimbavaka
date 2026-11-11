package org.vvs.valimbavaka.feature.saledetail.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.AuditorAware;

import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.commission.domain.CommissionBase;
import org.vvs.valimbavaka.feature.commission.domain.CommissionRateSource;
import org.vvs.valimbavaka.feature.commission.domain.CommissionSourceType;
import org.vvs.valimbavaka.feature.commission.domain.CommissionStatus;
import org.vvs.valimbavaka.feature.commission.dto.CommissionRDto;
import org.vvs.valimbavaka.feature.commission.dto.CommissionRuleRDto;
import org.vvs.valimbavaka.feature.commission.service.CommissionRuleService;
import org.vvs.valimbavaka.feature.commission.service.CommissionService;
import org.vvs.valimbavaka.feature.invoice.dto.InvoiceRDto;
import org.vvs.valimbavaka.feature.invoice.service.InvoiceService;
import org.vvs.valimbavaka.feature.promotion.dto.PromotionRDto;
import org.vvs.valimbavaka.feature.promotion.service.PromotionService;
import org.vvs.valimbavaka.feature.referential.dto.SalesChannelRDto;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.sale.domain.PaymentMethod;
import org.vvs.valimbavaka.feature.sale.dto.SaleLineRDto;
import org.vvs.valimbavaka.feature.sale.dto.SaleRDto;
import org.vvs.valimbavaka.feature.sale.service.SaleService;
import org.vvs.valimbavaka.feature.user.dto.UserRDto;
import org.vvs.valimbavaka.feature.user.service.UserService;
import org.vvs.valimbavaka.shared.dto.PageResponse;
import org.vvs.valimbavaka.shared.operation.OperationStatus;

@ExtendWith(MockitoExtension.class)
class SaleDetailServiceImplTest {

    private static final UUID SALE_ID = UUID.randomUUID();
    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID CHANNEL_ID = UUID.randomUUID();
    private static final UUID ARTICLE_ID = UUID.randomUUID();
    private static final UUID RULE_ID = UUID.randomUUID();
    private static final UUID PROMOTION_ID = UUID.randomUUID();

    @Mock
    private SaleService saleService;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private CommissionService commissionService;

    @Mock
    private CommissionRuleService commissionRuleService;

    @Mock
    private PromotionService promotionService;

    @Mock
    private ArticleService articleService;

    @Mock
    private ReferentialService referentialService;

    @Mock
    private UserService userService;

    @Mock
    private AuditorAware<UUID> auditorAware;

    private SaleDetailServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new SaleDetailServiceImpl(
                saleService,
                invoiceService,
                commissionService,
                commissionRuleService,
                promotionService,
                articleService,
                referentialService,
                userService,
                auditorAware);
    }

    @Test
    void getDetail_administratorOnASaleWithPromotionInvoiceAndChannelRule_assemblesEverything() {
        // Arrange
        when(saleService.getSaleById(SALE_ID)).thenReturn(sale(SELLER_ID, PROMOTION_ID));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(UUID.randomUUID()));
        stubNames(true);
        var promotion = mock(PromotionRDto.class);
        when(promotion.id()).thenReturn(PROMOTION_ID);
        when(promotion.name()).thenReturn("Soldes de novembre");
        when(promotion.code()).thenReturn("NOV10");
        when(promotionService.getPromotionById(PROMOTION_ID)).thenReturn(promotion);
        var invoice = mock(InvoiceRDto.class);
        var invoiceId = UUID.randomUUID();
        when(invoice.id()).thenReturn(invoiceId);
        when(invoice.number()).thenReturn("FAC-2026-0042");
        when(invoice.issuedAt()).thenReturn(Instant.parse("2026-11-11T08:00:00Z"));
        when(invoice.status()).thenReturn(OperationStatus.VALIDATED);
        when(invoiceService.findInvoiceBySaleId(SALE_ID)).thenReturn(Optional.of(invoice));
        when(commissionService.findBySource(CommissionSourceType.SALE, SALE_ID))
                .thenReturn(Optional.of(commission(RULE_ID, CommissionRateSource.SALES_CHANNEL)));
        var rule = mock(CommissionRuleRDto.class);
        when(rule.name()).thenReturn("Boutique 4 %");
        when(commissionRuleService.getRuleById(RULE_ID)).thenReturn(rule);

        // Act
        var detail = sut.getDetail(SALE_ID);

        // Assert
        assertThat(detail.number()).isEqualTo("VTE-0042");
        assertThat(detail.salesChannelLabel()).isEqualTo("Boutique");
        assertThat(detail.sellerName()).isEqualTo("Miora Rakoto");
        assertThat(detail.promotion().name()).isEqualTo("Soldes de novembre");
        assertThat(detail.promotion().code()).isEqualTo("NOV10");
        assertThat(detail.lines()).singleElement().satisfies(line -> {
            assertThat(line.articleCode()).isEqualTo("ART-0001");
            assertThat(line.articleName()).isEqualTo("Savon coco");
            assertThat(line.unitPrice()).isEqualByComparingTo("8500");
        });
        assertThat(detail.invoice().number()).isEqualTo("FAC-2026-0042");
        assertThat(detail.commission().ruleName()).isEqualTo("Boutique 4 %");
        assertThat(detail.commission().rateSource()).isEqualTo(CommissionRateSource.SALES_CHANNEL);
        assertThat(detail.commission().amount()).isEqualByComparingTo("1020");
    }

    @Test
    void getDetail_sellerRateWithoutPromotionInvoiceOrRule_leavesThoseBlocksNull() {
        // Arrange
        when(saleService.getSaleById(SALE_ID)).thenReturn(sale(SELLER_ID, null));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(SELLER_ID));
        when(userService.isActiveSeller(SELLER_ID)).thenReturn(true);
        stubNames(true);
        when(invoiceService.findInvoiceBySaleId(SALE_ID)).thenReturn(Optional.empty());
        when(commissionService.findBySource(CommissionSourceType.SALE, SALE_ID))
                .thenReturn(Optional.of(commission(null, CommissionRateSource.SELLER)));

        // Act
        var detail = sut.getDetail(SALE_ID);

        // Assert
        assertThat(detail.promotion()).isNull();
        assertThat(detail.invoice()).isNull();
        assertThat(detail.commission().rateSource()).isEqualTo(CommissionRateSource.SELLER);
        assertThat(detail.commission().ruleId()).isNull();
        assertThat(detail.commission().ruleName()).isNull();
        verifyNoInteractions(promotionService, commissionRuleService);
    }

    @Test
    void getDetail_saleCreditedToNobody_hasNoSellerAndNoCommission() {
        // Arrange
        when(saleService.getSaleById(SALE_ID)).thenReturn(sale(null, null));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(UUID.randomUUID()));
        stubNames(false);
        when(invoiceService.findInvoiceBySaleId(SALE_ID)).thenReturn(Optional.empty());
        when(commissionService.findBySource(CommissionSourceType.SALE, SALE_ID)).thenReturn(Optional.empty());

        // Act
        var detail = sut.getDetail(SALE_ID);

        // Assert
        assertThat(detail.sellerId()).isNull();
        assertThat(detail.sellerName()).isNull();
        assertThat(detail.commission()).isNull();
    }

    @Test
    void getDetail_channelDeactivatedAndArticleGone_hasNullLabelAndNullArticleName() {
        // Arrange
        when(saleService.getSaleById(SALE_ID)).thenReturn(sale(null, null));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.empty());
        var page = new PageResponse<SalesChannelRDto>(List.of(), 1, 100, 0, 0);
        when(referentialService.getSalesChannels(any())).thenReturn(page);
        when(articleService.getArticlesByIds(anyCollection())).thenReturn(List.of());
        when(invoiceService.findInvoiceBySaleId(SALE_ID)).thenReturn(Optional.empty());
        when(commissionService.findBySource(CommissionSourceType.SALE, SALE_ID)).thenReturn(Optional.empty());

        // Act
        var detail = sut.getDetail(SALE_ID);

        // Assert
        assertThat(detail.salesChannelLabel()).isNull();
        assertThat(detail.lines()).singleElement().satisfies(line -> {
            assertThat(line.articleName()).isNull();
            assertThat(line.articleCode()).isNull();
            assertThat(line.articleId()).isEqualTo(ARTICLE_ID);
        });
    }

    @Test
    void getDetail_sellerOnAColleaguesSale_throwsNotFoundWithoutReadingTheOtherBlocks() {
        // Arrange
        var colleague = UUID.randomUUID();
        when(saleService.getSaleById(SALE_ID)).thenReturn(sale(colleague, null));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(SELLER_ID));
        when(userService.isActiveSeller(SELLER_ID)).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> sut.getDetail(SALE_ID)).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(invoiceService, commissionService, commissionRuleService, promotionService);
    }

    @Test
    void getDetail_sellerOnASaleCreditedToNobody_throwsNotFound() {
        // Arrange
        when(saleService.getSaleById(SALE_ID)).thenReturn(sale(null, null));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(SELLER_ID));
        when(userService.isActiveSeller(SELLER_ID)).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> sut.getDetail(SALE_ID)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getDetail_unknownSale_propagatesNotFound() {
        // Arrange
        var unknown = UUID.randomUUID();
        when(saleService.getSaleById(unknown)).thenThrow(new NotFoundException("Vente introuvable"));

        // Act + Assert
        assertThatThrownBy(() -> sut.getDetail(unknown)).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(invoiceService, commissionService);
    }

    /** Channel "Boutique", article "Savon coco" and, when {@code withSeller}, the seller "Miora Rakoto". */
    private void stubNames(boolean withSeller) {
        var page = new PageResponse<>(List.of(new SalesChannelRDto(CHANNEL_ID, "STORE", "Boutique")), 1, 100, 1, 1);
        when(referentialService.getSalesChannels(any())).thenReturn(page);
        var article = mock(ArticleRDto.class);
        when(article.id()).thenReturn(ARTICLE_ID);
        when(article.code()).thenReturn("ART-0001");
        when(article.name()).thenReturn("Savon coco");
        when(articleService.getArticlesByIds(anyCollection())).thenReturn(List.of(article));
        if (withSeller) {
            var seller = mock(UserRDto.class);
            when(seller.firstName()).thenReturn("Miora");
            when(seller.lastName()).thenReturn("Rakoto");
            when(userService.getUserById(SELLER_ID)).thenReturn(seller);
        }
    }

    private static SaleRDto sale(UUID sellerId, UUID promotionId) {
        var line = new SaleLineRDto(ARTICLE_ID, new BigDecimal("3"), new BigDecimal("8500"), new BigDecimal("25500"));
        return new SaleRDto(
                SALE_ID,
                "VTE-0042",
                UUID.randomUUID(),
                LocalDate.of(2026, 11, 11),
                CHANNEL_ID,
                UUID.randomUUID(),
                sellerId,
                PaymentMethod.CASH,
                OperationStatus.VALIDATED,
                new BigDecimal("25500"),
                BigDecimal.ZERO,
                promotionId,
                new BigDecimal("25500"),
                List.of(line),
                Instant.parse("2026-11-11T07:30:00Z"),
                null);
    }

    private static CommissionRDto commission(UUID ruleId, CommissionRateSource source) {
        var revenue = new BigDecimal("25500");
        return new CommissionRDto(
                UUID.randomUUID(),
                CommissionSourceType.SALE,
                SALE_ID,
                "VTE-0042",
                SELLER_ID,
                "Miora Rakoto",
                CHANNEL_ID,
                null,
                LocalDate.of(2026, 11, 11),
                revenue,
                revenue,
                CommissionBase.REVENUE,
                new BigDecimal("4.00"),
                source,
                ruleId,
                new BigDecimal("1020"),
                CommissionStatus.CALCULATED,
                Instant.parse("2026-11-11T07:30:00Z"));
    }
}
