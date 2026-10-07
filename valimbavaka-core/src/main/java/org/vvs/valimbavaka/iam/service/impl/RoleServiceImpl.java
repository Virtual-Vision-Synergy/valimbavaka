package org.vvs.valimbavaka.iam.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vvs.valimbavaka.iam.dto.RoleRDto;
import org.vvs.valimbavaka.iam.mapper.RoleMapper;
import org.vvs.valimbavaka.iam.repository.RoleRepository;
import org.vvs.valimbavaka.iam.service.RoleService;

@Service
public class RoleServiceImpl implements RoleService {
    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    public RoleServiceImpl(RoleRepository roleRepository, RoleMapper roleMapper) {
        this.roleRepository = roleRepository;
        this.roleMapper = roleMapper;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<RoleRDto> getRoles(Pageable pageable) {
        return roleRepository.findAll(pageable).map(roleMapper::toRDto);
    }
}
