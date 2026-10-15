package org.vvs.valimbavaka.shared.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class PageQueryTest {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "email");

    @Test
    void toPageable_noParameters_usesFirstPageOfTwentySortedByDefault() {
        // given
        var sut = new PageQuery(null, null, null, null, null);
        // when
        var pageable = sut.toPageable(SORT_FIELDS, "createdAt", SortDirection.DESC);
        // then
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Test
    void toPageable_pageTwo_convertsToZeroBasedIndex() {
        // given
        var sut = new PageQuery(2, 10, null, "email", SortDirection.ASC);
        // when
        var pageable = sut.toPageable(SORT_FIELDS, "createdAt", SortDirection.DESC);
        // then
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "email"));
    }

    @Test
    void toPageable_sortFieldOutsideWhitelist_fallsBackToDefault() {
        // given
        var sut = new PageQuery(1, 20, null, "password", null);
        // when
        var pageable = sut.toPageable(SORT_FIELDS, "createdAt", SortDirection.DESC);
        // then
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Test
    void searchTerm_blankPadding_isStripped() {
        // given
        var sut = new PageQuery(null, null, "  rakoto ", null, null);
        // when / then
        assertThat(sut.searchTerm()).isEqualTo("rakoto");
    }
}
