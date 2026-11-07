package org.vvs.valimbavaka.feature.inventory.mapper;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.feature.inventory.domain.InventoryLine;
import org.vvs.valimbavaka.feature.inventory.domain.InventorySession;
import org.vvs.valimbavaka.feature.inventory.dto.InventoryLineRDto;
import org.vvs.valimbavaka.feature.inventory.dto.InventorySessionRDto;
import org.vvs.valimbavaka.feature.referential.dto.StockLocationRDto;

/** Builds the read views of a session and of its lines from the entities and the data of other features. */
@Component
public class InventoryMapper {

    public InventorySessionRDto toRDto(InventorySession session, StockLocationRDto location, long lineCount) {
        return new InventorySessionRDto(
                session.getId(),
                session.getNumber(),
                session.getLocationId(),
                location.code(),
                location.label(),
                session.getScope(),
                session.getRangeId(),
                session.getResponsibleId(),
                session.getResponsibleName(),
                session.getCountDate(),
                session.getStatus(),
                session.getCreatedAt(),
                session.getCreatedBy(),
                session.getClosedAt(),
                lineCount,
                session.getVersion());
    }

    /** @param currentQuantity stock of the article in the session's location, used while it is not counted */
    public InventoryLineRDto toRDto(InventoryLine line, BigDecimal currentQuantity, @Nullable BigDecimal unitCost) {
        var expected = line.getExpectedQuantity();
        var theoretical = expected == null ? currentQuantity : expected;
        var counted = line.getCountedQuantity();
        return new InventoryLineRDto(
                line.getId(),
                line.getArticleId(),
                line.getArticleCode(),
                line.getArticleName(),
                line.getBarcode(),
                unitCost,
                theoretical,
                counted,
                counted == null ? null : counted.subtract(theoretical),
                line.getReason());
    }
}
