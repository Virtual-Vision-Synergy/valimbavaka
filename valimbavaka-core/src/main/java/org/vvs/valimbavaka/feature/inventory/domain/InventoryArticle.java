package org.vvs.valimbavaka.feature.inventory.domain;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

/** Article to count, as known when the session is opened (code, name and barcode are frozen on the line). */
public record InventoryArticle(UUID articleId, String code, String name, @Nullable String barcode) {}
