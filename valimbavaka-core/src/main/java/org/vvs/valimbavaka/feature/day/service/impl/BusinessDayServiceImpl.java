package org.vvs.valimbavaka.feature.day.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.feature.day.domain.BusinessDay;
import org.vvs.valimbavaka.feature.day.domain.DayTransfer;
import org.vvs.valimbavaka.feature.day.dto.BusinessDayRDto;
import org.vvs.valimbavaka.feature.day.dto.DayOpeningRDto;
import org.vvs.valimbavaka.feature.day.dto.DayOpeningWDto;
import org.vvs.valimbavaka.feature.day.mapper.BusinessDayMapper;
import org.vvs.valimbavaka.feature.day.repository.BusinessDayRepository;
import org.vvs.valimbavaka.feature.day.service.BusinessDayService;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.transfer.domain.TransferCommand;
import org.vvs.valimbavaka.feature.transfer.domain.TransferLineCommand;
import org.vvs.valimbavaka.feature.transfer.domain.TransferOrigin;
import org.vvs.valimbavaka.feature.transfer.service.TransferService;

@Service
@Transactional(readOnly = true)
public class BusinessDayServiceImpl implements BusinessDayService {

    static final String RESOURCE_TYPE = "BUSINESS_DAY";

    private static final Logger log = LoggerFactory.getLogger(BusinessDayServiceImpl.class);

    private final BusinessDayRepository dayRepository;
    private final BusinessDayMapper dayMapper;
    private final DayOpeningPolicy policy;
    private final DayOpeningGrid grid;
    private final TransferService transferService;
    private final JournalService journalService;

    BusinessDayServiceImpl(
            BusinessDayRepository dayRepository,
            BusinessDayMapper dayMapper,
            DayOpeningPolicy policy,
            DayOpeningGrid grid,
            TransferService transferService,
            JournalService journalService) {
        this.dayRepository = dayRepository;
        this.dayMapper = dayMapper;
        this.policy = policy;
        this.grid = grid;
        this.transferService = transferService;
        this.journalService = journalService;
    }

    @Override
    public DayOpeningRDto getOpening() {
        var date = policy.today();
        var day = dayRepository.findByBusinessDate(date).map(dayMapper::toRDto).orElse(null);
        var previousDay = dayRepository
                .findFirstByBusinessDateLessThanOrderByBusinessDateDesc(date)
                .orElse(null);
        return grid.build(date, previousDay, day);
    }

    @Override
    @Transactional
    public BusinessDayRDto openDay(DayOpeningWDto request) {
        var date = policy.today();
        var plan = policy.check(date, request);
        var day = BusinessDay.open(date);
        var note = morningNote(date);
        plan.byDestination().forEach((destinationId, quantities) -> {
            var command = morningTransfer(plan.warehouseId(), destinationId, quantities, note);
            var transfer = transferService.record(command);
            day.addTransfer(transfer.id(), transfer.number(), destinationId, List.copyOf(quantities.values()));
        });
        var saved = dayRepository.saveAndFlush(day);
        journalService.record(JournalAction.CREATE, RESOURCE_TYPE, saved.getId(), description(saved));
        log.info(
                "Journée du {} ouverte ({} transfert(s) du matin)",
                date,
                saved.getTransfers().size());
        return dayMapper.toRDto(saved);
    }

    @Override
    public @Nullable BusinessDayRDto findToday() {
        var day = dayRepository.findByBusinessDate(policy.today());
        return day.map(dayMapper::toRDto).orElse(null);
    }

    @Override
    public BusinessDayRDto requireOpenDay() {
        var date = policy.today();
        var day = dayRepository.findByBusinessDate(date).filter(BusinessDay::isOpen);
        if (day.isEmpty()) {
            throw new ConflictException("La journée du %s n'est pas ouverte.".formatted(format(date)));
        }
        return dayMapper.toRDto(day.get());
    }

    private static TransferCommand morningTransfer(
            UUID warehouseId, UUID destinationId, Map<UUID, BigDecimal> quantities, String note) {
        var lines = quantities.entrySet().stream()
                .map(entry -> new TransferLineCommand(entry.getKey(), entry.getValue()))
                .toList();
        return new TransferCommand(TransferOrigin.MORNING, warehouseId, destinationId, lines, note);
    }

    /** TRF-02: a morning transfer names its day, e.g. {@code Ouverture de la journée du 08/11/2026}. */
    private static String morningNote(LocalDate date) {
        return "Ouverture de la journée du %s".formatted(format(date));
    }

    /** E.g. {@code Ouverture de la journée du 08/11/2026 : 2 transfert(s) du matin (TRF-0093, TRF-0094)}. */
    private static String description(BusinessDay day) {
        var date = format(day.getBusinessDate());
        if (day.getTransfers().isEmpty()) {
            return "Ouverture de la journée du %s sans transfert du matin".formatted(date);
        }
        var numbers =
                day.getTransfers().stream().map(DayTransfer::getTransferNumber).collect(Collectors.joining(", "));
        var template = "Ouverture de la journée du %s : %d transfert(s) du matin (%s)";
        return template.formatted(date, day.getTransfers().size(), numbers);
    }

    private static String format(LocalDate date) {
        return DayOpeningPolicy.DATE_FORMAT.format(date);
    }
}
