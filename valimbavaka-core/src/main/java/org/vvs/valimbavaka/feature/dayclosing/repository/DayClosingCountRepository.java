package org.vvs.valimbavaka.feature.dayclosing.repository;

import java.util.List;
import java.util.UUID;

import org.vvs.valimbavaka.feature.dayclosing.domain.DayClosingCount;
import org.vvs.valimbavaka.shared.operation.OperationRepository;

/** Counts are replaced by a recount and never deleted (JNL-02). One line per day, location and article. */
public interface DayClosingCountRepository extends OperationRepository<DayClosingCount, UUID> {

    <S extends DayClosingCount> List<S> saveAll(Iterable<S> counts);

    List<DayClosingCount> findByBusinessDayId(UUID businessDayId);
}
