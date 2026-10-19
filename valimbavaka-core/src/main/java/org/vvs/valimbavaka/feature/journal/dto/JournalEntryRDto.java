package org.vvs.valimbavaka.feature.journal.dto;

import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.journal.domain.JournalAction;

public record JournalEntryRDto(
        UUID id,
        Instant occurredAt,
        @Nullable UUID actorId,
        String actorName,
        JournalAction action,
        String resourceType,
        UUID resourceId,
        String description) {}
