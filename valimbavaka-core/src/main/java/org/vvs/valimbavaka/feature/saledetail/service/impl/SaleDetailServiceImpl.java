package org.vvs.valimbavaka.feature.saledetail.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.commission.domain.CommissionSourceType;
import org.vvs.valimbavaka.feature.commission.dto.CommissionRDto;
import org.vvs.valimbavaka.feature.commission.service.CommissionRuleService;
import org.vvs.valimbavaka.feature.commission.service.CommissionService;
import org.vvs.valimbavaka.feature.invoice.dto.InvoiceRDto;
import org.vvs.valimbavaka.feature.invoice.service.InvoiceService;
import org.vvs.valimbavaka.feature.promotion.service.PromotionService;
import org.vvs.valimbavaka.feature.referential.dto.SalesChannelRDto;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.sale.dto.SaleLineRDto;
import org.vvs.valimbavaka.feature.sale.dto.SaleRDto;
import org.vvs.valimbavaka.feature.sale.service.SaleService;
import org.vvs.valimbavaka.feature.saledetail.dto.SaleDetailCommissionRDto;
import org.vvs.valimbavaka.feature.saledetail.dto.SaleDetailInvoiceRDto;
import org.vvs.valimbavaka.feature.saledetail.dto.SaleDetailLineRDto;
import org.vvs.valimbavaka.feature.saledetail.dto.SaleDetailPromotionRDto;
import org.vvs.valimbavaka.feature.saledetail.dto.SaleDetailRDto;
import org.vvs.valimbavaka.feature.saledetail.service.SaleDetailService;
import org.vvs.valimbavaka.feature.user.service.UserService;
import org.vvs.valimbavaka.shared.dto.PageQuery;

@Service
@Transactional(readOnly = true)
public class SaleDetailServiceImpl implements SaleDetailService {

    private static final PageQuery ALL_CHANNELS = new PageQuery(1, 100, null, "code", null);

    private final SaleService saleService;
    private final InvoiceService invoiceService;
    private final CommissionService commissionService;
    private final CommissionRuleService commissionRuleService;
    private final PromotionService promotionService;
    private final ArticleService articleService;
    private final ReferentialService referentialService;
    private final UserService userService;
    private final AuditorAware<UUID> auditorAware;

    public SaleDetailServiceImpl(
            SaleService saleService,
            InvoiceService invoiceService,
            CommissionService commissionService,
            CommissionRuleService commissionRuleService,
            PromotionService promotionService,
            ArticleService articleService,
            ReferentialService referentialService,
            UserService userService,
            AuditorAware<UUID> auditorAware) {
        this.saleService = saleService;
        this.invoiceService = invoiceService;
        this.commissionService = commissionService;
        this.commissionRuleService = commissionRuleService;
        this.promotionService = promotionService;
        this.articleService = articleService;
        this.referentialService = referentialService;
        this.userService = userService;
        this.auditorAware = auditorAware;
    }

    @Override
    public SaleDetailRDto getDetail(UUID saleId) {
        var sale = saleService.getSaleById(saleId);
        requireReadable(sale);
        var sellerId = sale.sellerId();
        return new SaleDetailRDto(
                sale.id(),
                sale.number(),
                sale.businessDate(),
                sale.createdAt(),
                sale.salesChannelId(),
                channelLabel(sale.salesChannelId()),
                sellerId,
                sellerId == null ? null : sellerName(sellerId),
                sale.paymentMethod(),
                sale.status(),
                sale.subtotal(),
                sale.discountAmount(),
                sale.total(),
                promotion(sale.promotionId()),
                lines(sale.lines()),
                invoice(sale.id()),
                commission(sale.id()));
    }

    /** An active seller only reads the sales credited to her; the others answer 404, never 403 (no existence leak). */
    private void requireReadable(SaleRDto sale) {
        var own = auditorAware.getCurrentAuditor().filter(userService::isActiveSeller);
        if (own.isPresent() && !own.get().equals(sale.sellerId())) {
            throw new NotFoundException("Vente introuvable : " + sale.id());
        }
    }

    /** Active channels only: a channel deactivated since the sale has no label. */
    private @Nullable String channelLabel(UUID salesChannelId) {
        return referentialService.getSalesChannels(ALL_CHANNELS).items().stream()
                .filter(channel -> channel.id().equals(salesChannelId))
                .map(SalesChannelRDto::label)
                .findFirst()
                .orElse(null);
    }

    private String sellerName(UUID sellerId) {
        var seller = userService.getUserById(sellerId);
        return "%s %s".formatted(seller.firstName(), seller.lastName());
    }

    private @Nullable SaleDetailPromotionRDto promotion(@Nullable UUID promotionId) {
        if (promotionId == null) {
            return null;
        }
        var promotion = promotionService.getPromotionById(promotionId);
        return new SaleDetailPromotionRDto(promotion.id(), promotion.name(), promotion.code());
    }

    private List<SaleDetailLineRDto> lines(List<SaleLineRDto> lines) {
        var ids = lines.stream().map(SaleLineRDto::articleId).collect(Collectors.toSet());
        var articles = articleService.getArticlesByIds(ids).stream()
                .collect(Collectors.toMap(ArticleRDto::id, Function.identity()));
        return lines.stream()
                .map(line -> toLineRDto(line, articles.get(line.articleId())))
                .toList();
    }

    private static SaleDetailLineRDto toLineRDto(SaleLineRDto line, @Nullable ArticleRDto article) {
        return new SaleDetailLineRDto(
                line.articleId(),
                article == null ? null : article.code(),
                article == null ? null : article.name(),
                line.quantity(),
                line.unitPrice(),
                line.lineTotal());
    }

    private @Nullable SaleDetailInvoiceRDto invoice(UUID saleId) {
        return invoiceService
                .findInvoiceBySaleId(saleId)
                .map(SaleDetailServiceImpl::toInvoiceRDto)
                .orElse(null);
    }

    private static SaleDetailInvoiceRDto toInvoiceRDto(InvoiceRDto invoice) {
        return new SaleDetailInvoiceRDto(invoice.id(), invoice.number(), invoice.issuedAt(), invoice.status());
    }

    private @Nullable SaleDetailCommissionRDto commission(UUID saleId) {
        return commissionService
                .findBySource(CommissionSourceType.SALE, saleId)
                .map(this::toCommissionRDto)
                .orElse(null);
    }

    private SaleDetailCommissionRDto toCommissionRDto(CommissionRDto commission) {
        var ruleId = commission.ruleId();
        var ruleName = ruleId == null
                ? null
                : commissionRuleService.getRuleById(ruleId).name();
        return new SaleDetailCommissionRDto(
                commission.id(),
                commission.revenue(),
                commission.baseAmount(),
                commission.base(),
                commission.rate(),
                commission.rateSource(),
                ruleId,
                ruleName,
                commission.amount(),
                commission.status());
    }
}
