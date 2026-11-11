package org.vvs.valimbavaka.feature.invoice.service.impl;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.invoice.dto.SalesBookRowRDto;
import org.vvs.valimbavaka.feature.invoice.mapper.SalesBookMapper;
import org.vvs.valimbavaka.feature.invoice.repository.SalesBookRepository;
import org.vvs.valimbavaka.feature.invoice.service.SalesBookService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;
import org.vvs.valimbavaka.shared.dto.SortDirection;

@Service
@Transactional(readOnly = true)
public class SalesBookServiceImpl implements SalesBookService {

    private static final Set<String> SORT_FIELDS = Set.of("businessDate", "issuedAt", "number", "total");
    private static final Sort TIE_BREAK = Sort.by("issuedAt", "number");

    private final SalesBookRepository salesBookRepository;
    private final SalesBookMapper salesBookMapper;

    SalesBookServiceImpl(SalesBookRepository salesBookRepository, SalesBookMapper salesBookMapper) {
        this.salesBookRepository = salesBookRepository;
        this.salesBookMapper = salesBookMapper;
    }

    @Override
    public PageResponse<SalesBookRowRDto> getSalesBook(PageQuery query, LocalDate from, LocalDate to) {
        checkPeriod(from, to);
        var requested = query.toPageable(SORT_FIELDS, "businessDate", SortDirection.ASC);
        // A stable order inside a day, so that two pages never overlap nor skip a line.
        var pageable = PageRequest.of(
                requested.getPageNumber(), requested.getPageSize(), requested.getSort().and(TIE_BREAK));
        var page = salesBookRepository.search(from, to, query.searchTerm(), pageable);
        return PageResponse.from(page.map(salesBookMapper::toRDto));
    }

    private static void checkPeriod(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessException("La date de début doit précéder la date de fin.");
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAX_PERIOD_DAYS) {
            throw new BusinessException("La période du livre des ventes ne peut pas dépasser %d jours."
                    .formatted(MAX_PERIOD_DAYS));
        }
    }
}
