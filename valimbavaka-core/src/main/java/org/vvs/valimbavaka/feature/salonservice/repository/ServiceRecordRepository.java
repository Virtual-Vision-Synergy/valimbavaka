package org.vvs.valimbavaka.feature.salonservice.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import org.vvs.valimbavaka.feature.salonservice.domain.ServiceRecord;
import org.vvs.valimbavaka.feature.salonservice.dto.SalonConsumptionRDto;
import org.vvs.valimbavaka.shared.operation.OperationRepository;

/** A performed service is a validated operation (JNL-02): saved and read, never updated nor deleted. */
public interface ServiceRecordRepository extends OperationRepository<ServiceRecord, UUID> {

    /** Next number of the service record sequence (gaps are possible, a number is never reused). */
    @Query(value = "select nextval('service_record_number_seq')", nativeQuery = true)
    long nextNumber();

    /** Search on number, service, customer and seller; empty {@code search} = all. */
    @Query(value = """
            select r from ServiceRecord r
            where lower(r.number) like lower(concat('%', :search, '%'))
               or lower(r.serviceName) like lower(concat('%', :search, '%'))
               or lower(coalesce(r.customerName, '')) like lower(concat('%', :search, '%'))
               or lower(r.sellerName) like lower(concat('%', :search, '%'))
            """, countQuery = """
            select count(r) from ServiceRecord r
            where lower(r.number) like lower(concat('%', :search, '%'))
               or lower(r.serviceName) like lower(concat('%', :search, '%'))
               or lower(coalesce(r.customerName, '')) like lower(concat('%', :search, '%'))
               or lower(r.sellerName) like lower(concat('%', :search, '%'))
            """)
    Page<ServiceRecord> search(String search, Pageable pageable);

    /** Services performed in {@code [from, to[}, oldest first. */
    @Query("""
            select r from ServiceRecord r
            where r.createdAt >= :from and r.createdAt < :to
            order by r.createdAt, r.number
            """)
    List<ServiceRecord> findPerformedBetween(Instant from, Instant to);

    /** Quantity consumed per article by the services performed in {@code [from, to[}. */
    @Query("""
            select new org.vvs.valimbavaka.feature.salonservice.dto.SalonConsumptionRDto(
                p.articleId, sum(p.quantity))
            from ServiceRecord r join r.products p
            where r.createdAt >= :from and r.createdAt < :to
            group by p.articleId
            order by p.articleId
            """)
    List<SalonConsumptionRDto> consumptionBetween(Instant from, Instant to);
}
