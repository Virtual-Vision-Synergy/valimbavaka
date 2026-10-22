package org.vvs.valimbavaka.feature.supplier.service.impl;

import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.supplier.domain.Supplier;
import org.vvs.valimbavaka.feature.supplier.domain.SupplierDetails;
import org.vvs.valimbavaka.feature.supplier.domain.SupplierType;
import org.vvs.valimbavaka.feature.supplier.dto.SupplierRDto;
import org.vvs.valimbavaka.feature.supplier.dto.SupplierWDto;
import org.vvs.valimbavaka.feature.supplier.mapper.SupplierMapper;
import org.vvs.valimbavaka.feature.supplier.repository.SupplierRepository;
import org.vvs.valimbavaka.feature.supplier.service.SupplierService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;
import org.vvs.valimbavaka.shared.dto.SortDirection;

@Service
@Transactional(readOnly = true)
public class SupplierServiceImpl implements SupplierService {

    private static final Logger log = LoggerFactory.getLogger(SupplierServiceImpl.class);

    private static final String RESOURCE_TYPE = "SUPPLIER";
    private static final String NAME_ALREADY_USED = "A supplier with this name already exists.";
    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "name", "type");

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;
    private final JournalService journalService;

    public SupplierServiceImpl(
            SupplierRepository supplierRepository, SupplierMapper supplierMapper, JournalService journalService) {
        this.supplierRepository = supplierRepository;
        this.supplierMapper = supplierMapper;
        this.journalService = journalService;
    }

    @Override
    public PageResponse<SupplierRDto> getSuppliers(
            PageQuery query, @Nullable SupplierType type, @Nullable Boolean active) {
        var pageable = query.toPageable(SORT_FIELDS, "createdAt", SortDirection.DESC);
        return PageResponse.from(supplierRepository
                .search(query.searchTerm(), type, active, pageable)
                .map(supplierMapper::toRDto));
    }

    @Override
    public SupplierRDto getSupplierById(UUID supplierId) {
        return supplierMapper.toRDto(findSupplier(supplierId));
    }

    @Override
    @Transactional
    public SupplierRDto createSupplier(SupplierWDto request) {
        if (supplierRepository.existsByNameIgnoreCase(request.name().strip())) {
            throw new ConflictException(NAME_ALREADY_USED);
        }
        Supplier saved = supplierRepository.save(Supplier.create(toDetails(request)));
        journalService.record(
                JournalAction.CREATE, RESOURCE_TYPE, saved.getId(), "Supplier created: " + saved.getName());
        log.info("Supplier {} created", saved.getId());
        return supplierMapper.toRDto(saved);
    }

    @Override
    @Transactional
    public SupplierRDto updateSupplier(UUID supplierId, SupplierWDto request) {
        Supplier supplier = findSupplier(supplierId);
        if (supplierRepository.existsByNameIgnoreCaseAndIdNot(request.name().strip(), supplierId)) {
            throw new ConflictException(NAME_ALREADY_USED);
        }
        supplier.update(toDetails(request));
        journalService.record(
                JournalAction.UPDATE, RESOURCE_TYPE, supplierId, "Supplier updated: " + supplier.getName());
        return supplierMapper.toRDto(supplier);
    }

    @Override
    @Transactional
    public SupplierRDto setActive(UUID supplierId, boolean active) {
        Supplier supplier = findSupplier(supplierId);
        if (supplier.isActive() == active) {
            return supplierMapper.toRDto(supplier);
        }
        if (active) {
            supplier.activate();
        } else {
            supplier.deactivate();
        }
        String action = active ? "Supplier activated: " : "Supplier deactivated: ";
        journalService.record(JournalAction.UPDATE, RESOURCE_TYPE, supplierId, action + supplier.getName());
        log.info("Supplier {} {}", supplierId, active ? "activated" : "deactivated");
        return supplierMapper.toRDto(supplier);
    }

    private Supplier findSupplier(UUID supplierId) {
        return supplierRepository
                .findById(supplierId)
                .orElseThrow(() -> new NotFoundException("Supplier not found: " + supplierId));
    }

    private static SupplierDetails toDetails(SupplierWDto request) {
        return new SupplierDetails(
                request.name(),
                request.type(),
                request.contactName(),
                request.phone(),
                request.email(),
                request.address(),
                request.notes());
    }
}
