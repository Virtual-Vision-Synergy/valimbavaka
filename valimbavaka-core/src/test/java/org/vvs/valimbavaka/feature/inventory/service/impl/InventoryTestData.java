package org.vvs.valimbavaka.feature.inventory.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryOpening;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryScope;

/** Shared fixtures of the INV-01 unit tests. */
final class InventoryTestData {

    static final UUID LOCATION_ID = UUID.randomUUID();

    private InventoryTestData() {}

    static InventoryOpening opening(@Nullable UUID rangeId) {
        var scope = rangeId == null ? InventoryScope.ALL : InventoryScope.RANGE;
        return new InventoryOpening(
                LOCATION_ID, scope, rangeId, UUID.randomUUID(), "Hanta Rakoto", LocalDate.of(2026, 11, 8));
    }

    static ArticleRDto article(UUID id, String code, boolean active) {
        var now = Instant.parse("2026-10-01T08:00:00Z");
        return new ArticleRDto(
                id,
                code,
                "Savon " + code,
                "321000000000" + code.charAt(code.length() - 1),
                ArticleType.FINISHED,
                null,
                ArticleUnit.PIECE,
                new BigDecimal("3120.50"),
                new BigDecimal("8500"),
                new BigDecimal("40"),
                null,
                active,
                now,
                now,
                0L);
    }
}
