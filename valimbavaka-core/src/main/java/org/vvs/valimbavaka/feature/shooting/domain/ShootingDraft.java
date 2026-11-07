package org.vvs.valimbavaka.feature.shooting.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * Checked content of a new shooting exit.
 *
 * @param locationId stock location the products leave
 * @param takenByName full name of the account recording the exit, copied for display
 * @param items articles already copied from their sheet, each article once
 */
public record ShootingDraft(
        UUID locationId,
        String takenByName,
        Instant expectedReturnAt,
        @Nullable String note,
        List<ShootingItem> items) {}
