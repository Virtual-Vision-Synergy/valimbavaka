package org.vvs.valimbavaka.feature.salonservice.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.salonservice.dto.SalonConsumptionRDto;
import org.vvs.valimbavaka.feature.salonservice.dto.ServiceRecordRDto;
import org.vvs.valimbavaka.feature.salonservice.dto.ServiceRecordWDto;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/**
 * Performed salon services (SRV-02): recording one takes its products out of the salon stock. Read by the commission
 * (COM-02), the day history (SRV-03), the live day view (JR-03) and the closing reconciliation (JR-04, SRV-04).
 */
public interface ServiceRecordService {

    /** Most recent first; search on number, service, customer and seller. */
    PageResponse<ServiceRecordRDto> getServiceRecords(PageQuery query);

    /**
     * Services recorded by the authenticated account during one business day ({@code Indian/Antananarivo}), today when
     * {@code date} is {@code null}; most recent first (SRV-03). {@code search} is not applied.
     */
    PageResponse<ServiceRecordRDto> getMyServiceRecords(PageQuery query, @Nullable LocalDate date);

    ServiceRecordRDto getServiceRecordById(UUID serviceRecordId);

    /**
     * Records a performed service and one {@code TRADE_SHOW} (salon) stock movement per consumed product, in one
     * transaction: 404 if the service or an article is unknown, 422 if the service is inactive or an article is listed
     * twice, 409 if the salon stock of an article would become negative.
     */
    ServiceRecordRDto recordService(ServiceRecordWDto request);

    /** Services performed in {@code [from, to[}, oldest first (COM-02, SRV-03, JR-03). */
    List<ServiceRecordRDto> getServiceRecordsBetween(Instant from, Instant to);

    /** Quantity consumed per article by the services performed in {@code [from, to[} (SRV-04, JR-04). */
    List<SalonConsumptionRDto> getSalonConsumption(Instant from, Instant to);
}
