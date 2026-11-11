package org.vvs.valimbavaka.feature.sale.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.referential.dto.SalesChannelRDto;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.sale.domain.Sale;
import org.vvs.valimbavaka.feature.sale.domain.SaleLine;
import org.vvs.valimbavaka.feature.sale.dto.SaleSummaryLineRDto;
import org.vvs.valimbavaka.feature.sale.dto.SaleSummaryRDto;
import org.vvs.valimbavaka.feature.user.dto.UserRDto;
import org.vvs.valimbavaka.feature.user.service.UserService;
import org.vvs.valimbavaka.shared.dto.PageQuery;

/**
 * Rows of the day's sales list (VTE-07): the names are read once per page (channels, distinct sellers, articles of
 * the page), never per sale.
 */
@Component
class SaleSummaryAssembler {

    private static final PageQuery ALL_CHANNELS = new PageQuery(1, 100, null, "code", null);

    private final ReferentialService referentialService;
    private final UserService userService;
    private final ArticleService articleService;

    SaleSummaryAssembler(
            ReferentialService referentialService, UserService userService, ArticleService articleService) {
        this.referentialService = referentialService;
        this.userService = userService;
        this.articleService = articleService;
    }

    /** One row per id, in the order of {@code ids} (page order); {@code sales} holds those sales with their lines. */
    List<SaleSummaryRDto> toRDtos(List<UUID> ids, List<Sale> sales) {
        var byId = sales.stream().collect(Collectors.toMap(Sale::getId, Function.identity()));
        var page = ids.stream().map(byId::get).filter(Objects::nonNull).toList();
        var names = new Names(channelLabels(), sellerNames(page), articleNames(page));
        return page.stream().map(sale -> toRDto(sale, names)).toList();
    }

    private static SaleSummaryRDto toRDto(Sale sale, Names names) {
        var sellerId = sale.getSellerId();
        var lines =
                sale.getLines().stream().map(line -> toLineRDto(line, names)).toList();
        return new SaleSummaryRDto(
                sale.getId(),
                sale.getNumber(),
                sale.getBusinessDate(),
                sale.getCreatedAt(),
                sale.getSalesChannelId(),
                names.channels().get(sale.getSalesChannelId()),
                sellerId,
                sellerId == null ? null : names.sellers().get(sellerId),
                sale.getPaymentMethod(),
                sale.getStatus(),
                sale.getSubtotal(),
                sale.getDiscountAmount(),
                sale.getTotal(),
                lines);
    }

    private static SaleSummaryLineRDto toLineRDto(SaleLine line, Names names) {
        var articleId = line.getArticleId();
        return new SaleSummaryLineRDto(
                articleId, names.articles().get(articleId), line.getQuantity(), line.getLineTotal());
    }

    /** Active channels only: a channel deactivated since the sale has no label. */
    private Map<UUID, String> channelLabels() {
        return referentialService.getSalesChannels(ALL_CHANNELS).items().stream()
                .collect(Collectors.toMap(SalesChannelRDto::id, SalesChannelRDto::label));
    }

    /** « Miora Rakoto » for each distinct seller of the page (a handful at most). */
    private Map<UUID, String> sellerNames(List<Sale> page) {
        var names = new HashMap<UUID, String>();
        page.stream()
                .map(Sale::getSellerId)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(id -> names.put(id, fullName(userService.getUserById(id))));
        return names;
    }

    private Map<UUID, String> articleNames(List<Sale> page) {
        var ids = page.stream()
                .flatMap(sale -> sale.getLines().stream())
                .map(SaleLine::getArticleId)
                .collect(Collectors.toSet());
        return articleService.getArticlesByIds(ids).stream()
                .collect(Collectors.toMap(ArticleRDto::id, ArticleRDto::name));
    }

    private static String fullName(UserRDto user) {
        return "%s %s".formatted(user.firstName(), user.lastName());
    }

    private record Names(Map<UUID, String> channels, Map<UUID, String> sellers, Map<UUID, String> articles) {}
}
