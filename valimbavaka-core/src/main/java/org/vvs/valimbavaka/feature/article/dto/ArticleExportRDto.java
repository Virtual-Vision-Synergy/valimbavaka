package org.vvs.valimbavaka.feature.article.dto;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;
import org.vvs.valimbavaka.shared.excel.annotation.ExcelColumn;

/** One row of the article export (ART-08): the purchase cost and the internal identifiers are left out on purpose. */
public record ArticleExportRDto(
        @ExcelColumn(header = "Code", order = 1, width = 12) String code,
        @ExcelColumn(header = "Nom", order = 2, width = 30) String name,

        @ExcelColumn(header = "Code-barres", order = 3, width = 16) @Nullable
        String barcode,

        @ExcelColumn(header = "Type", order = 4, width = 16) ArticleType type,

        @ExcelColumn(header = "Unité", order = 5, width = 12)
        ArticleUnit unit,

        @ExcelColumn(header = "Prix de vente", order = 6, format = "#,##0.00") @Nullable
        BigDecimal price,

        @ExcelColumn(header = "Seuil minimum", order = 7, format = "#,##0.00")
        BigDecimal threshold,

        @ExcelColumn(header = "Actif", order = 8, width = 8) boolean active) {}
