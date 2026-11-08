package org.vvs.valimbavaka.feature.day.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.vvs.valimbavaka.feature.day.domain.BusinessDay;
import org.vvs.valimbavaka.shared.operation.OperationRepository;

/** A business day is never deleted (JNL-02): it is closed (JR-06). One day per date (unique constraint). */
public interface BusinessDayRepository extends OperationRepository<BusinessDay, UUID> {

    /** Flushes at once: two simultaneous openings of the same date end in a 409 instead of a late failure. */
    BusinessDay saveAndFlush(BusinessDay day);

    boolean existsByBusinessDate(LocalDate businessDate);

    Optional<BusinessDay> findByBusinessDate(LocalDate businessDate);

    /** Last day opened before the given date (previous working day, JR-02); empty for the very first day. */
    Optional<BusinessDay> findFirstByBusinessDateLessThanOrderByBusinessDateDesc(LocalDate businessDate);
}
