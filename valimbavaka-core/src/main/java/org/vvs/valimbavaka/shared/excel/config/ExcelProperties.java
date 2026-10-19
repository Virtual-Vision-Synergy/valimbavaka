package org.vvs.valimbavaka.shared.excel.config;

import java.time.ZoneId;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Réglages facultatifs de l'export Excel. {@code maxRows} plafonne chaque onglet et devient la taille de page
 * des routes {@code /export} ; {@code timeZone} sert à afficher les {@code Instant}.
 */
@Validated
@ConfigurationProperties(prefix = "vvs.excel")
public record ExcelProperties(
        @DefaultValue("10000") @Min(1) @Max(MAX_EXCEL_ROWS) int maxRows,
        @DefaultValue @Valid Column column,
        @DefaultValue @Valid Format format,
        @DefaultValue("Indian/Antananarivo") @NotNull ZoneId timeZone) {

    /** Lignes d'une feuille Excel, en-tête déduit. */
    public static final int MAX_EXCEL_ROWS = 1_048_575;

    /** Largeurs en caractères ; Excel plafonne une colonne à 255. */
    public record Column(
            @DefaultValue("255") @Min(1) @Max(255) int maxWidth,
            @DefaultValue("2") @PositiveOrZero int padding) {
    }

    public record Format(
            @DefaultValue("dd/mm/yyyy") @NotBlank String date,
            @DefaultValue("dd/mm/yyyy hh:mm") @NotBlank String datetime) {
    }
}
