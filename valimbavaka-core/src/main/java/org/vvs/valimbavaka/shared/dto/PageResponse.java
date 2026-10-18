package org.vvs.valimbavaka.shared.dto;

import java.util.List;

import org.springframework.data.domain.Page;

/** Contrat de pagination VVS (convention §7.3) : page en base 1, indépendant de Spring Data. */
public record PageResponse<T>(List<T> items, int page, int pageSize, long totalCount, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber() + 1, page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
