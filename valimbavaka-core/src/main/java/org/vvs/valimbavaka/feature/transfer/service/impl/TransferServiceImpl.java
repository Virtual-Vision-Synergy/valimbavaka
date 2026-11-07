package org.vvs.valimbavaka.feature.transfer.service.impl;

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
import org.vvs.valimbavaka.feature.transfer.mapper.TransferMapper;
import org.vvs.valimbavaka.feature.transfer.repository.TransferRepository;
import org.vvs.valimbavaka.feature.transfer.service.TransferService;

@Service
@Transactional(readOnly = true)
public class TransferServiceImpl implements TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferServiceImpl.class);

    private final TransferRepository transferRepository;
    private final TransferMapper transferMapper;
    private final TransferPolicy policy;
    private final TransferLedger ledger;

    TransferServiceImpl(
            TransferRepository transferRepository,
            TransferMapper transferMapper,
            TransferPolicy policy,
            TransferLedger ledger) {
        this.transferRepository = transferRepository;
        this.transferMapper = transferMapper;
        this.policy = policy;
        this.ledger = ledger;
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

    private String nextNumber(TransferOrigin origin) {
        var next = origin == TransferOrigin.RETURN
                ? transferRepository.nextReturnNumber()
                : transferRepository.nextTransferNumber();
        return "%s-%04d".formatted(origin.numberPrefix(), next);
    }
}
