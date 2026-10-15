package org.vvs.valimbavaka.iam.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.vvs.valimbavaka.iam.dto.RoleRDto;

/** Lecture seule : les rôles sont fixés par les migrations. */
public interface RoleService {
    Page<RoleRDto> getRoles(Pageable pageable);
}
