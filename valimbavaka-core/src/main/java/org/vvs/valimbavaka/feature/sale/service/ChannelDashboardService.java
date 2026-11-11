package org.vvs.valimbavaka.feature.sale.service;

import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.sale.dto.ChannelDashboardRDto;

/** Sales dashboard by channel (VTE-08), reserved to the administrator. */
public interface ChannelDashboardService {

    /**
     * Turnover, number of sales and share of the total turnover of each channel over the business days
     * {@code [from, to]}, validated sales only (cancelled and reversed sales are not counted). {@code to} is today in
     * Madagascar (Indian/Antananarivo) when {@code null}, {@code from} the first day of that month when {@code null}.
     *
     * @throws org.vvs.valimbavaka.core.exception.BusinessException when {@code from} is after {@code to}
     */
    ChannelDashboardRDto getDashboard(@Nullable LocalDate from, @Nullable LocalDate to);
}
