package org.vvs.valimbavaka.feature.stock.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementCommand;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.stock.domain.SupplierReceipt;
import org.vvs.valimbavaka.feature.stock.domain.SupplierReceiptLine;
import org.vvs.valimbavaka.feature.stock.dto.SupplierReceiptRDto;
import org.vvs.valimbavaka.feature.stock.dto.SupplierReceiptWDto;
import org.vvs.valimbavaka.feature.stock.mapper.SupplierReceiptMapper;
import org.vvs.valimbavaka.feature.stock.repository.SupplierReceiptRepository;
import org.vvs.valimbavaka.feature.stock.service.RestockOrderService;
import org.vvs.valimbavaka.feature.stock.service.StockMovementService;
import org.vvs.valimbavaka.feature.stock.service.SupplierReceiptService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;
import org.vvs.valimbavaka.shared.dto.SortDirection;

@Service
@Transactional(readOnly = true)
public class SupplierReceiptServiceImpl implements SupplierReceiptService {

    static final String RESOURCE_TYPE = "SUPPLIER_RECEIPT";
    private static final String NOT_FOUND = "Réception fournisseur introuvable : ";
    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "number");

    private final SupplierReceiptRepository supplierReceiptRepository;
    private final SupplierReceiptPolicy policy;
    private final RestockOrderService restockOrderService;
    private final StockMovementService stockMovementService;
    private final SupplierReceiptMapper supplierReceiptMapper;
    private final JournalService journalService;

    SupplierReceiptServiceImpl(
            SupplierReceiptRepository supplierReceiptRepository,
            SupplierReceiptPolicy policy,
            RestockOrderService restockOrderService,
            StockMovementService stockMovementService,
            SupplierReceiptMapper supplierReceiptMapper,
            JournalService journalService) {
        this.supplierReceiptRepository = supplierReceiptRepository;
        this.policy = policy;
        this.restockOrderService = restockOrderService;
        this.stockMovementService = stockMovementService;
        this.supplierReceiptMapper = supplierReceiptMapper;
        this.journalService = journalService;
    }

    @Override
    @Transactional
    public SupplierReceiptRDto receive(SupplierReceiptWDto request) {
        var command = policy.check(request);
        var number = "REC-%06d".formatted(supplierReceiptRepository.nextNumber());
        var orderId = command.restockOrderId();
        var origin = orderId == null ? "saisie libre" : "bon " + restockOrderService.markReceived(orderId, number);
        var receipt = supplierReceiptRepository.save(SupplierReceipt.create(number, command));
        stockMovementService.recordAll(movements(receipt, UnaryOperator.identity()));
        var description = "Réception %s du fournisseur %s (%s) : %d article(s), %s unité(s)"
                .formatted(number, receipt.getSupplierName(), origin, receipt.getLines().size(), units(receipt));
        journalService.record(JournalAction.CREATE, RESOURCE_TYPE, receipt.getId(), description);
        return supplierReceiptMapper.toRDto(receipt);
    }

    @Override
    public PageResponse<SupplierReceiptRDto> getReceipts(PageQuery query) {
        var page = supplierReceiptRepository.findAll(query.toPageable(SORT_FIELDS, "createdAt", SortDirection.DESC));
        return PageResponse.from(page.map(supplierReceiptMapper::toRDto));
    }

    @Override
    public SupplierReceiptRDto getReceiptById(UUID receiptId) {
        return supplierReceiptMapper.toRDto(findReceipt(receiptId));
    }

    @Override
    @Transactional
    public SupplierReceiptRDto reverse(UUID receiptId, String reason) {
        var receipt = findReceipt(receiptId);
        receipt.reverse();
        stockMovementService.recordAll(movements(receipt, BigDecimal::negate));
        var saved = supplierReceiptRepository.save(receipt);
        journalService.record(
                JournalAction.REVERSE,
                RESOURCE_TYPE,
                saved.getId(),
                "Contre-passation de la réception %s : %s".formatted(saved.getNumber(), reason.strip()));
        return supplierReceiptMapper.toRDto(saved);
    }

    private SupplierReceipt findReceipt(UUID receiptId) {
        return supplierReceiptRepository
                .findById(receiptId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND + receiptId));
    }

    /** One {@code RECEIPT} movement per line in the receipt location; {@code sign} negates them for the reversal. */
    private static List<StockMovementCommand> movements(SupplierReceipt receipt, UnaryOperator<BigDecimal> sign) {
        return receipt.getLines().stream()
                .map(line -> new StockMovementCommand(
                        line.getArticleId(),
                        receipt.getLocationId(),
                        StockMovementType.RECEIPT,
                        sign.apply(line.getQuantity()),
                        receipt.getId()))
                .toList();
    }

    private static String units(SupplierReceipt receipt) {
        return receipt.getLines().stream()
                .map(SupplierReceiptLine::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .stripTrailingZeros()
                .toPlainString();
    }
}
