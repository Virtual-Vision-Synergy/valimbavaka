package org.vvs.valimbavaka.feature.delivery.dto;

import java.time.Instant;

/**
 * Step of the history of an order (LIV-06): a journal line written for the order, with its time, its author and its
 * label. {@code action} is the journal action ({@code CREATE} for the creation, {@code UPDATE} for a change of status,
 * {@code CANCEL} or {@code REVERSE} for an undo).
 */
public record DeliveryEventRDto(Instant occurredAt, String actorName, String action, String description) {}
