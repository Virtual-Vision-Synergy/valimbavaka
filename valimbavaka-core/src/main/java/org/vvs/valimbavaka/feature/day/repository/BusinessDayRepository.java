package org.vvs.valimbavaka.feature.day.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import org.vvs.valimbavaka.feature.day.domain.BusinessDay;
import org.vvs.valimbavaka.shared.operation.OperationRepository;

/** A business day is never deleted (JNL-02): it is closed (JR-06). One day per date (unique constraint). */
public interface BusinessDayRepository extends OperationRepository<BusinessDay, UUID> {

    /** Flushes at once: two simultaneous openings of the same date end in a 409 instead of a late failure. */
    BusinessDay saveAndFlush(BusinessDay day);

    /** Day of the date locked in writing until the end of the transaction: two closings run one after the other. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from BusinessDay d where d.businessDate = :date")
    Optional<BusinessDay> findByBusinessDateForUpdate(LocalDate date);

    boolean existsByBusinessDate(LocalDate businessDate);

    Optional<BusinessDay> findByBusinessDate(LocalDate businessDate);

    /** Last day opened before the given date (previous working day, JR-02); empty for the very first day. */
    Optional<BusinessDay> findFirstByBusinessDateLessThanOrderByBusinessDateDesc(LocalDate businessDate);
}
