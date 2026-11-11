package org.vvs.valimbavaka.feature.saledetail.service;

import java.util.UUID;

import org.vvs.valimbavaka.feature.saledetail.dto.SaleDetailRDto;

/** Detail of a sale (VTE-09): read only, assembled from the sale, its invoice and its commission. */
public interface SaleDetailService {

    /**
     * Detail of a sale. An active seller only reads the sales credited to her, and so her own commission: any other
     * sale answers 404 as if it did not exist; the administrator reads every sale.
     *
     * @throws org.vvs.valimbavaka.core.exception.NotFoundException if the sale is unknown or belongs to another seller
     */
    SaleDetailRDto getDetail(UUID saleId);
}
