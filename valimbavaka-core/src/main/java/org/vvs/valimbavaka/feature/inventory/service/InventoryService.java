package org.vvs.valimbavaka.feature.inventory.service;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.inventory.domain.InventoryStatus;
import org.vvs.valimbavaka.feature.inventory.dto.InventoryLineRDto;
import org.vvs.valimbavaka.feature.inventory.dto.InventorySessionRDto;
import org.vvs.valimbavaka.feature.inventory.dto.InventorySessionWDto;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/** Inventory sessions (INV-01): opening and reading; counting, gap reasons and closing are added by INV-02 to 04. */
public interface InventoryService {

    /** Sessions, most recent first by default; a null filter means "all". */
    PageResponse<InventorySessionRDto> getSessions(
            PageQuery query, @Nullable InventoryStatus status, @Nullable UUID locationId);

    /** 404 if the session is unknown. */
    InventorySessionRDto getSession(UUID sessionId);

    /** Lines of a session sorted by article code; 404 if the session is unknown. */
    PageResponse<InventoryLineRDto> getLines(UUID sessionId, PageQuery query);

    /**
     * Opens a session and creates one line per article of the scope; traced in the journal. 422 if a reference is
     * unknown or inactive, if the scope and the range do not match, if the count date is in the past or if the
     * scope holds no article; 409 if a session is already in progress in the location.
     */
    InventorySessionRDto openSession(InventorySessionWDto request);
}
