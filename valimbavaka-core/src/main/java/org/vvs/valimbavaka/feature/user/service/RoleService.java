package org.vvs.valimbavaka.feature.user.service;

import org.vvs.valimbavaka.feature.user.dto.RoleRDto;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/** Lecture seule : les rôles sont fixés par les migrations. */
public interface RoleService {

    PageResponse<RoleRDto> getRoles(PageQuery query);
}
