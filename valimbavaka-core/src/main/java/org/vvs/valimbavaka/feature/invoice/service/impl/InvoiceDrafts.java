package org.vvs.valimbavaka.feature.invoice.service.impl;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.invoice.config.InvoiceProperties;
import org.vvs.valimbavaka.feature.invoice.domain.InvoiceDraft;
import org.vvs.valimbavaka.feature.invoice.domain.InvoiceLineDraft;
import org.vvs.valimbavaka.feature.referential.dto.SalesChannelRDto;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.sale.dto.SaleLineRDto;
import org.vvs.valimbavaka.feature.sale.dto.SaleRDto;
import org.vvs.valimbavaka.feature.user.dto.UserRDto;
import org.vvs.valimbavaka.feature.user.service.UserService;
import org.vvs.valimbavaka.shared.dto.PageQuery;

/**
 * Builds what is printed on the invoice of a sale: the sale's amounts and prices, plus the names read once at issue
 * time (channel, seller, articles), so that they stay as they were whatever happens to the sheets afterwards.
 */
@Component
class InvoiceDrafts {

    private static final PageQuery ALL_CHANNELS = new PageQuery(1, 100, null, "code", null);

    private final ReferentialService referentialService;
    private final UserService userService;
    private final ArticleService articleService;
    private final InvoiceProperties properties;
    private final Clock clock;

    InvoiceDrafts(
            ReferentialService referentialService,
            UserService userService,
            ArticleService articleService,
            InvoiceProperties properties,
            Clock clock) {
        this.referentialService = referentialService;
        this.userService = userService;
        this.articleService = articleService;
        this.properties = properties;
        this.clock = clock;
    }

    InvoiceDraft of(SaleRDto sale) {
        var articleNames = articleNames(sale);
        var lines = sale.lines().stream()
                .map(line -> toLineDraft(line, articleNames))
                .toList();
        var sellerId = sale.sellerId();
        return new InvoiceDraft(
                sale.id(),
                sale.number(),
                clock.instant(),
                sale.businessDate(),
                channelLabel(sale.salesChannelId()),
                sellerId,
                sellerId == null ? null : fullName(userService.getUserById(sellerId)),
                sale.paymentMethod().name(),
                properties.issuerName(),
                properties.issuerNifOrNull(),
                sale.subtotal(),
                sale.discountAmount(),
                sale.total(),
                lines);
    }

    private static InvoiceLineDraft toLineDraft(SaleLineRDto line, Map<UUID, String> articleNames) {
        var label = articleNames.getOrDefault(line.articleId(), line.articleId().toString());
        return new InvoiceLineDraft(line.articleId(), label, line.quantity(), line.unitPrice(), line.lineTotal());
    }

    /** Active channels only: a channel deactivated in the meantime leaves the label empty. */
    private @Nullable String channelLabel(UUID salesChannelId) {
        return referentialService.getSalesChannels(ALL_CHANNELS).items().stream()
                .filter(channel -> channel.id().equals(salesChannelId))
                .map(SalesChannelRDto::label)
                .findFirst()
                .orElse(null);
    }

    private Map<UUID, String> articleNames(SaleRDto sale) {
        var ids = sale.lines().stream().map(SaleLineRDto::articleId).collect(Collectors.toSet());
        return articleService.getArticlesByIds(ids).stream()
                .collect(Collectors.toMap(ArticleRDto::id, ArticleRDto::name));
    }

    private static String fullName(UserRDto user) {
        return "%s %s".formatted(user.firstName(), user.lastName());
    }
}
