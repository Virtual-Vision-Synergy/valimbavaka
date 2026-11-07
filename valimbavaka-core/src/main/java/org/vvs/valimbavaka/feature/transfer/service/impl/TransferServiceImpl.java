package org.vvs.valimbavaka.feature.transfer.service.impl;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.feature.transfer.domain.Transfer;
import org.vvs.valimbavaka.feature.transfer.domain.TransferCommand;
import org.vvs.valimbavaka.feature.transfer.domain.TransferLineCommand;
import org.vvs.valimbavaka.feature.transfer.domain.TransferOrigin;
import org.vvs.valimbavaka.feature.transfer.dto.StockReturnWDto;
import org.vvs.valimbavaka.feature.transfer.dto.TransferRDto;
import org.vvs.valimbavaka.feature.transfer.dto.TransferSummaryRDto;
import org.vvs.valimbavaka.feature.transfer.mapper.TransferMapper;
import org.vvs.valimbavaka.feature.transfer.repository.TransferRepository;
import org.vvs.valimbavaka.feature.transfer.service.TransferService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;
import org.vvs.valimbavaka.shared.dto.SortDirection;

@Service
@Transactional(readOnly = true)
public class TransferServiceImpl implements TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferServiceImpl.class);
    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "number", "origin", "status");

    private final TransferRepository transferRepository;
    private final TransferMapper transferMapper;
    private final TransferPolicy policy;
    private final TransferLedger ledger;
    private final TransferHistory history;

    TransferServiceImpl(
            TransferRepository transferRepository,
            TransferMapper transferMapper,
            TransferPolicy policy,
            TransferLedger ledger,
            TransferHistory history) {
        this.transferRepository = transferRepository;
        this.transferMapper = transferMapper;
        this.policy = policy;
        this.ledger = ledger;
        this.history = history;
    }

    @Override
    @Transactional
    public TransferRDto record(TransferCommand command) {
        var route = policy.check(command);
        var transfer = transferRepository.save(Transfer.create(nextNumber(command.origin()), command));
        ledger.post(transfer, route);
        log.info("Transfert {} enregistré ({})", transfer.getNumber(), transfer.getStatus());
        return transferMapper.toRDto(transfer);
    }

    @Override
    @Transactional
    public TransferRDto recordReturn(StockReturnWDto request) {
        var lines = request.lines().stream()
                .map(line -> new TransferLineCommand(line.articleId(), line.quantity()))
                .toList();
        return record(new TransferCommand(
                TransferOrigin.RETURN, request.fromLocationId(), request.toLocationId(), lines, request.note()));
    }

    @Override
    public PageResponse<TransferSummaryRDto> getTransfers(PageQuery query, @Nullable TransferOrigin origin) {
        var pageable = query.toPageable(SORT_FIELDS, "createdAt", SortDirection.DESC);
        return history.toRDtos(transferRepository.search(query.searchTerm(), origin, pageable));
    }

    @Override
    public List<TransferRDto> getTransfersByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return transferRepository.findAllByIdIn(Set.copyOf(ids)).stream()
                .map(transferMapper::toRDto)
                .toList();
    }

    private String nextNumber(TransferOrigin origin) {
        var next = origin == TransferOrigin.RETURN
                ? transferRepository.nextReturnNumber()
                : transferRepository.nextTransferNumber();
        return "%s-%04d".formatted(origin.numberPrefix(), next);
    }
}
