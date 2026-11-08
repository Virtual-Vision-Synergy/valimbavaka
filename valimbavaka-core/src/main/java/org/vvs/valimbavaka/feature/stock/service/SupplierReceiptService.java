package org.vvs.valimbavaka.feature.stock.service;

import java.util.UUID;

import org.vvs.valimbavaka.feature.stock.dto.SupplierReceiptRDto;
import org.vvs.valimbavaka.feature.stock.dto.SupplierReceiptWDto;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/** Receipts of supplier deliveries (STK-06): the purchased goods enter the stock. Never deleted (JNL-02). */
public interface SupplierReceiptService {

    /**
     * Records a validated receipt, one {@code RECEIPT} movement per line into the chosen location (the warehouse by
     * default), and traces it in the journal. When it takes back a restock order (STK-05), the order becomes
     * {@code RECEIVED}. 404 if the supplier, an article or the order is unknown, 422 if the supplier is deactivated,
     * the location inactive or an article given twice, 409 if the order is already received.
     */
    SupplierReceiptRDto receive(SupplierReceiptWDto request);

    /** Receipts with their lines, newest first by default; sort on {@code createdAt} or {@code number}. */
    PageResponse<SupplierReceiptRDto> getReceipts(PageQuery query);

    /** 404 if the receipt is unknown. */
    SupplierReceiptRDto getReceiptById(UUID receiptId);

    /**
     * Correction of an error: records the inverse movements (negative {@code RECEIPT}), marks the receipt
     * {@code REVERSED} and traces the reason in the journal ({@code REVERSE}). 404 if unknown, 409 if already reversed
     * or if the received goods are no longer in the location (stock would become negative).
     */
    SupplierReceiptRDto reverse(UUID receiptId, String reason);
}
