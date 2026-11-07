package org.vvs.valimbavaka.feature.transfer.service.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.feature.referential.dto.StockLocationRDto;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.transfer.dto.TransferSummaryRDto;
import org.vvs.valimbavaka.feature.transfer.repository.TransferSummary;
import org.vvs.valimbavaka.security.AuthenticatedUser;
import org.vvs.valimbavaka.security.AuthenticatedUserLoader;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/**
 * Turns a page of the transfer history (TRF-01) into rows ready to display: labels of the locations and full name
 * of the issuer, each looked up once per page.
 */
@Component
class TransferHistory {

    static final String SYSTEM_ISSUER = "Système";
    static final String UNKNOWN_ISSUER = "Compte inconnu";

    private static final PageQuery ALL_LOCATIONS = new PageQuery(1, 100, null, "code", null);

    private final ReferentialService referentialService;
    private final AuthenticatedUserLoader userLoader;

    TransferHistory(ReferentialService referentialService, AuthenticatedUserLoader userLoader) {
        this.referentialService = referentialService;
        this.userLoader = userLoader;
    }

    PageResponse<TransferSummaryRDto> toRDtos(Page<TransferSummary> page) {
        Map<UUID, String> labels = referentialService.getStockLocations(ALL_LOCATIONS).items().stream()
                .collect(Collectors.toMap(StockLocationRDto::id, StockLocationRDto::label));
        var names = new HashMap<UUID, String>();
        return PageResponse.from(page.map(row -> toRDto(row, labels, issuerName(row.createdBy(), names))));
    }

    private String issuerName(@Nullable UUID userId, Map<UUID, String> names) {
        if (userId == null) {
            return SYSTEM_ISSUER;
        }
        return names.computeIfAbsent(userId, this::loadName);
    }

    /** Account removed since the transfer: the identifier stays, the name says so. */
    private String loadName(UUID userId) {
        return userLoader.findById(userId).map(TransferHistory::fullName).orElse(UNKNOWN_ISSUER);
    }

    private static String fullName(AuthenticatedUser user) {
        return user.firstName() + " " + user.lastName();
    }

    private static TransferSummaryRDto toRDto(TransferSummary row, Map<UUID, String> labels, String issuer) {
        return new TransferSummaryRDto(
                row.id(),
                row.number(),
                row.origin(),
                row.status(),
                row.fromLocationId(),
                labels.get(row.fromLocationId()),
                row.toLocationId(),
                labels.get(row.toLocationId()),
                row.lineCount().intValue(),
                row.quantity(),
                row.createdAt(),
                row.createdBy(),
                issuer);
    }
}
