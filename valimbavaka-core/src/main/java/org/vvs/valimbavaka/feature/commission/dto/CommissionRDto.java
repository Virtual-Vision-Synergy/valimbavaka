package org.vvs.valimbavaka.feature.commission.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.commission.domain.CommissionBase;
import org.vvs.valimbavaka.feature.commission.domain.CommissionRateSource;
import org.vvs.valimbavaka.feature.commission.domain.CommissionSourceType;
import org.vvs.valimbavaka.feature.commission.domain.CommissionStatus;
import org.vvs.valimbavaka.shared.excel.annotation.ExcelColumn;

/**
 * Commission computed on a sale or a salon service (COM-02): {@code amount} = {@code baseAmount} × {@code rate} / 100,
 * rounded half up to the ariary. {@code base} is the base actually applied ({@code REVENUE} or {@code MARGIN}),
 * {@code rateSource} the level the rate comes from and {@code ruleId} the channel rule applied, if any. The columns
 * marked {@code @ExcelColumn} are those of the xlsx and PDF statements (COM-05); identifiers stay out of the files.
 */
public record CommissionRDto(
        UUID id,
        CommissionSourceType sourceType,
        UUID sourceId,

        @ExcelColumn(header = "Opération", order = 2, width = 14)
        String sourceNumber,

        UUID sellerId,

        @ExcelColumn(header = "Vendeuse", order = 3, width = 28)
        String sellerName,

        UUID salesChannelId,
        @Nullable UUID salonServiceId,

        @ExcelColumn(header = "Date", order = 1, width = 12)
        LocalDate businessDate,

        @ExcelColumn(header = "CA (Ar)", order = 4, format = "#,##0.00", width = 16)
        BigDecimal revenue,

        @ExcelColumn(header = "Base (Ar)", order = 5, format = "#,##0.00", width = 16)
        BigDecimal baseAmount,

        CommissionBase base,

        @ExcelColumn(header = "Taux (%)", order = 6, format = "0.00", width = 10)
        BigDecimal rate,

        CommissionRateSource rateSource,
        @Nullable UUID ruleId,

        @ExcelColumn(header = "Commission (Ar)", order = 7, format = "#,##0.00", width = 18)
        BigDecimal amount,

        @ExcelColumn(header = "Statut", order = 8, width = 14)
        CommissionStatus status,

        Instant createdAt) {}
