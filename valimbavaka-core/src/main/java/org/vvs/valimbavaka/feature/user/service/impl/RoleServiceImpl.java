package org.vvs.valimbavaka.feature.user.service.impl;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vvs.valimbavaka.feature.user.dto.RoleRDto;
import org.vvs.valimbavaka.feature.user.mapper.RoleMapper;
import org.vvs.valimbavaka.feature.user.repository.RoleRepository;
import org.vvs.valimbavaka.feature.user.service.RoleService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;
import org.vvs.valimbavaka.shared.dto.SortDirection;

@Service
@Transactional(readOnly = true)
public class RoleServiceImpl implements RoleService {

    private static final Set<String> SORT_FIELDS = Set.of("name", "createdAt");

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    public RoleServiceImpl(RoleRepository roleRepository, RoleMapper roleMapper) {
        this.roleRepository = roleRepository;
        this.roleMapper = roleMapper;
    }

    @Override
    public PageResponse<RoleRDto> getRoles(PageQuery query) {
        var pageable = query.toPageable(SORT_FIELDS, "name", SortDirection.ASC);
        return PageResponse.from(roleRepository.findAll(pageable).map(roleMapper::toRDto));
    }
}
