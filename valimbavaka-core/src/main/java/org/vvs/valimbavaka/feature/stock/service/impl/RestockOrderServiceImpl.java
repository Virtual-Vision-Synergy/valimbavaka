package org.vvs.valimbavaka.feature.stock.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.stock.domain.RestockOrder;
import org.vvs.valimbavaka.feature.stock.domain.RestockOrderLine;
import org.vvs.valimbavaka.feature.stock.dto.RestockOrderRDto;
import org.vvs.valimbavaka.feature.stock.dto.RestockOrderWDto;
import org.vvs.valimbavaka.feature.stock.dto.StockAlertRDto;
import org.vvs.valimbavaka.feature.stock.mapper.RestockOrderMapper;
import org.vvs.valimbavaka.feature.stock.repository.RestockOrderLineRepository;
import org.vvs.valimbavaka.feature.stock.repository.RestockOrderRepository;
import org.vvs.valimbavaka.feature.stock.service.RestockOrderService;
import org.vvs.valimbavaka.feature.stock.service.StockService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;
import org.vvs.valimbavaka.shared.dto.SortDirection;

@Service
@Transactional(readOnly = true)
public class RestockOrderServiceImpl implements RestockOrderService {

    static final String RESOURCE_TYPE = "RESTOCK_ORDER";
    static final String NOTHING_TO_ORDER =
            "Aucun article sélectionné n'est sous son seuil minimum : aucun bon à générer.";
    private static final String NOT_FOUND = "Bon de réapprovisionnement introuvable : ";
    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "reference");

    private final StockService stockService;
    private final RestockOrderRepository restockOrderRepository;
    private final RestockOrderLineRepository restockOrderLineRepository;
    private final RestockOrderMapper restockOrderMapper;
    private final JournalService journalService;

    public RestockOrderServiceImpl(
            StockService stockService,
            RestockOrderRepository restockOrderRepository,
            RestockOrderLineRepository restockOrderLineRepository,
            RestockOrderMapper restockOrderMapper,
            JournalService journalService) {
        this.stockService = stockService;
        this.restockOrderRepository = restockOrderRepository;
        this.restockOrderLineRepository = restockOrderLineRepository;
        this.restockOrderMapper = restockOrderMapper;
        this.journalService = journalService;
    }

    @Override
    @Transactional
    public RestockOrderRDto generate(RestockOrderWDto request) {
        var alerts = alertsToOrder(request.articleIds());
        if (alerts.isEmpty()) {
            throw new ConflictException(NOTHING_TO_ORDER);
        }
        var order = restockOrderRepository.save(RestockOrder.create(restockOrderRepository.nextNumber()));
        var lines = new ArrayList<RestockOrderLine>();
        for (var alert : alerts) {
            var command = restockOrderMapper.toLineCommand(alert);
            lines.add(RestockOrderLine.of(order.getId(), lines.size() + 1, command));
        }
        var saved = restockOrderLineRepository.saveAll(lines);
        journalService.record(
                JournalAction.CREATE,
                RESOURCE_TYPE,
                order.getId(),
                "Génération du bon de réapprovisionnement %s : %d article(s)"
                        .formatted(order.getReference(), saved.size()));
        return restockOrderMapper.toRDto(order, saved);
    }

    @Override
    public PageResponse<RestockOrderRDto> getRestockOrders(PageQuery query) {
        var page = restockOrderRepository.findAll(query.toPageable(SORT_FIELDS, "createdAt", SortDirection.DESC));
        var lines = linesByOrder(page.getContent().stream().map(RestockOrder::getId).toList());
        return PageResponse.from(
                page.map(order -> restockOrderMapper.toRDto(order, lines.getOrDefault(order.getId(), List.of()))));
    }

    @Override
    public RestockOrderRDto getRestockOrderById(UUID restockOrderId) {
        var order = restockOrderRepository
                .findById(restockOrderId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND + restockOrderId));
        var lines = linesByOrder(List.of(restockOrderId));
        return restockOrderMapper.toRDto(order, lines.getOrDefault(restockOrderId, List.of()));
    }

    /** Alerts of the moment (STK-04), restricted to the requested articles; none requested = all of them. */
    private List<StockAlertRDto> alertsToOrder(@Nullable List<UUID> articleIds) {
        var alerts = stockService.getAllAlerts();
        if (articleIds == null || articleIds.isEmpty()) {
            return alerts;
        }
        var requested = Set.copyOf(articleIds);
        return alerts.stream().filter(alert -> requested.contains(alert.articleId())).toList();
    }

    /** Lines of the given orders in one query, kept in line number order. */
    private Map<UUID, List<RestockOrderLine>> linesByOrder(Collection<UUID> orderIds) {
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        return restockOrderLineRepository.findLinesOf(orderIds).stream()
                .collect(Collectors.groupingBy(RestockOrderLine::getRestockOrderId));
    }
}
