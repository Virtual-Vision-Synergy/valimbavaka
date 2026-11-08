package org.vvs.valimbavaka.feature.sale.service.impl;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.day.service.BusinessDayService;
import org.vvs.valimbavaka.feature.sale.domain.Sale;
import org.vvs.valimbavaka.feature.sale.domain.SaleCommand;
import org.vvs.valimbavaka.feature.sale.domain.SaleHeader;
import org.vvs.valimbavaka.feature.sale.domain.SaleLineCommand;
import org.vvs.valimbavaka.feature.sale.dto.SaleFormRDto;
import org.vvs.valimbavaka.feature.sale.dto.SaleRDto;
import org.vvs.valimbavaka.feature.sale.dto.SaleWDto;
import org.vvs.valimbavaka.feature.sale.mapper.SaleMapper;
import org.vvs.valimbavaka.feature.sale.repository.SaleRepository;
import org.vvs.valimbavaka.feature.sale.service.SaleService;

@Service
@Transactional(readOnly = true)
public class SaleServiceImpl implements SaleService {

    private static final Logger log = LoggerFactory.getLogger(SaleServiceImpl.class);

    private final SaleRepository saleRepository;
    private final SaleMapper saleMapper;
    private final BusinessDayService businessDayService;
    private final SalePolicy policy;
    private final SaleLedger ledger;
    private final SaleFormAssembler formAssembler;

    SaleServiceImpl(
            SaleRepository saleRepository,
            SaleMapper saleMapper,
            BusinessDayService businessDayService,
            SalePolicy policy,
            SaleLedger ledger,
            SaleFormAssembler formAssembler) {
        this.saleRepository = saleRepository;
        this.saleMapper = saleMapper;
        this.businessDayService = businessDayService;
        this.policy = policy;
        this.ledger = ledger;
        this.formAssembler = formAssembler;
    }

    @Override
    @Transactional
    public SaleRDto record(SaleCommand command) {
        var day = businessDayService.requireOpenDay();
        var checked = policy.check(command);
        var header = new SaleHeader(
                day.id(),
                day.businessDate(),
                checked.channel().id(),
                checked.channel().stockLocationId(),
                command.sellerId(),
                command.paymentMethod());
        var number = "VTE-%04d".formatted(saleRepository.nextNumber());
        var sale = saleRepository.save(Sale.record(number, header, checked.lines()));
        ledger.post(sale, checked.channel().label());
        log.info(
                "Vente {} enregistrée ({} ligne(s))",
                sale.getNumber(),
                sale.getLines().size());
        return saleMapper.toRDto(sale);
    }

    @Override
    @Transactional
    public SaleRDto createSale(SaleWDto request) {
        var lines = request.lines().stream()
                .map(line -> new SaleLineCommand(line.articleId(), line.quantity(), null))
                .toList();
        return record(new SaleCommand(request.salesChannelId(), request.sellerId(), request.paymentMethod(), lines));
    }

    @Override
    public SaleRDto getSaleById(UUID saleId) {
        return saleMapper.toRDto(findSale(saleId));
    }

    @Override
    public BigDecimal getPurchaseCost(UUID saleId) {
        return findSale(saleId).purchaseCost();
    }

    @Override
    public SaleFormRDto getForm() {
        return formAssembler.build(saleRepository.peekNextNumber());
    }

    private Sale findSale(UUID saleId) {
        return saleRepository
                .findById(saleId)
                .orElseThrow(() -> new NotFoundException("Vente introuvable : " + saleId));
    }
}
