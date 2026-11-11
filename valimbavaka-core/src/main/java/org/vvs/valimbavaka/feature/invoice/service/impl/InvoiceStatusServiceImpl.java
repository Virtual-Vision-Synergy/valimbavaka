package org.vvs.valimbavaka.feature.invoice.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.invoice.dto.InvoiceStatusTotalRDto;
import org.vvs.valimbavaka.feature.invoice.dto.InvoiceStatusTotalsRDto;
import org.vvs.valimbavaka.feature.invoice.repository.InvoiceRepository;
import org.vvs.valimbavaka.feature.invoice.repository.InvoiceStatusTotal;
import org.vvs.valimbavaka.feature.invoice.service.InvoiceStatusService;
import org.vvs.valimbavaka.shared.operation.OperationStatus;

@Service
@Transactional(readOnly = true)
public class InvoiceStatusServiceImpl implements InvoiceStatusService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Indian/Antananarivo");

    private final InvoiceRepository invoiceRepository;
    private final Clock clock;

    InvoiceStatusServiceImpl(InvoiceRepository invoiceRepository, Clock clock) {
        this.invoiceRepository = invoiceRepository;
        this.clock = clock;
    }

    @Override
    public InvoiceStatusTotalsRDto getStatusTotals(@Nullable LocalDate from, @Nullable LocalDate to) {
        var last = to != null ? to : LocalDate.now(clock.withZone(BUSINESS_ZONE));
        var first = from != null ? from : last.withDayOfMonth(1);
        if (first.isAfter(last)) {
            throw new BusinessException("La date de début doit précéder la date de fin.");
        }
        var totals = invoiceRepository.totalsByStatus(first, last).stream()
                .collect(Collectors.toMap(InvoiceStatusTotal::status, Function.identity()));
        var statuses = Arrays.stream(OperationStatus.values())
                .map(status -> toRDto(status, totals))
                .toList();
        var count = statuses.stream().mapToLong(InvoiceStatusTotalRDto::invoiceCount).sum();
        var amount =
                statuses.stream().map(InvoiceStatusTotalRDto::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new InvoiceStatusTotalsRDto(first, last, count, amount, statuses);
    }

    private static InvoiceStatusTotalRDto toRDto(
            OperationStatus status, Map<OperationStatus, InvoiceStatusTotal> totals) {
        var total = totals.get(status);
        return total == null
                ? new InvoiceStatusTotalRDto(status, 0L, BigDecimal.ZERO)
                : new InvoiceStatusTotalRDto(status, total.invoiceCount(), total.amount());
    }
}
