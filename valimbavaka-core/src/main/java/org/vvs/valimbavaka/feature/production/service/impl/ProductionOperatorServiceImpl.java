package org.vvs.valimbavaka.feature.production.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.feature.production.dto.ProductionOperatorRDto;
import org.vvs.valimbavaka.feature.production.mapper.ProductionMapper;
import org.vvs.valimbavaka.feature.production.service.ProductionOperatorService;
import org.vvs.valimbavaka.feature.user.service.UserService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/** The seller reads the operators without users.read: only identity and name are exposed. */
@Service
@Transactional(readOnly = true)
public class ProductionOperatorServiceImpl implements ProductionOperatorService {

    private final UserService userService;
    private final ProductionMapper productionMapper;

    ProductionOperatorServiceImpl(UserService userService, ProductionMapper productionMapper) {
        this.userService = userService;
        this.productionMapper = productionMapper;
    }

    @Override
    public PageResponse<ProductionOperatorRDto> getOperators(PageQuery query) {
        var sellers = userService.getActiveSellers(query);
        return new PageResponse<>(
                sellers.items().stream().map(productionMapper::toOperatorRDto).toList(),
                sellers.page(),
                sellers.pageSize(),
                sellers.totalCount(),
                sellers.totalPages());
    }
}
