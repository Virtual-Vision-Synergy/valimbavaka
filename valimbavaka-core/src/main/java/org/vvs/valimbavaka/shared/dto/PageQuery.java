package org.vvs.valimbavaka.shared.dto;

import java.util.Set;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Paramètres de liste communs : page (base 1), size (20 par défaut, 100 max), search, sortBy, sortDirection. */
public record PageQuery(
        @Min(1) Integer page,
        @Min(1) @Max(100) Integer size,
        @Size(max = 100) @Nullable String search,
        @Nullable String sortBy,
        @Nullable SortDirection sortDirection) {

    private static final int DEFAULT_SIZE = 20;

    /** page et size sont absents de l'URL quand non fournis : défauts appliqués ici, jamais null ensuite. */
    public PageQuery(@Nullable Integer page, @Nullable Integer size, @Nullable String search,
            @Nullable String sortBy, @Nullable SortDirection sortDirection) {
        this.page = page == null ? 1 : page;
        this.size = size == null ? DEFAULT_SIZE : size;
        this.search = search;
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
    }

    /** Terme de recherche normalisé : jamais null, sans espaces superflus. */
    public String searchTerm() {
        return search == null ? "" : search.strip();
    }

    /** Le champ de tri provient d'une liste blanche, jamais directement de l'URL (§7.3). */
    public Pageable toPageable(Set<String> allowedSortFields, String defaultSortField,
            SortDirection defaultDirection) {
        var field = sortBy != null && allowedSortFields.contains(sortBy) ? sortBy : defaultSortField;
        var direction = sortDirection == null ? defaultDirection : sortDirection;
        return PageRequest.of(page - 1, size, Sort.by(Sort.Direction.valueOf(direction.name()), field));
    }
}
