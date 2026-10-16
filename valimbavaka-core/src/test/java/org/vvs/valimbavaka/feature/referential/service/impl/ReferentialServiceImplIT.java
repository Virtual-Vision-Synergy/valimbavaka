package org.vvs.valimbavaka.feature.referential.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.vvs.valimbavaka.feature.referential.dto.ProductRangeRDto;
import org.vvs.valimbavaka.feature.referential.dto.SalesChannelRDto;
import org.vvs.valimbavaka.feature.referential.dto.StockLocationRDto;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.support.IntegrationTest;

/** SYS-05 : les référentiels métier sont disponibles dès la première installation. */
@IntegrationTest
class ReferentialServiceImplIT {

    private static final PageQuery FIRST_PAGE = new PageQuery(null, null, null, null, null);

    @Autowired
    private ReferentialService sut;

    @Test
    void getSalesChannels_freshDatabase_returnsSeededChannels() {
        // when
        var page = sut.getSalesChannels(FIRST_PAGE);
        // then
        assertThat(page.items()).extracting(SalesChannelRDto::code)
                .containsExactlyInAnyOrder("STORE", "FACEBOOK", "PHONE", "DELIVERY");
    }

    @Test
    void getProductRanges_freshDatabase_returnsSeededRanges() {
        // when
        var page = sut.getProductRanges(FIRST_PAGE);
        // then
        assertThat(page.items()).extracting(ProductRangeRDto::label)
                .containsExactly("Botanika", "Solika", "Zezika");
    }

    @Test
    void getStockLocations_freshDatabase_returnsSeededLocations() {
        // when
        var page = sut.getStockLocations(FIRST_PAGE);
        // then
        assertThat(page.items()).extracting(StockLocationRDto::code)
                .containsExactlyInAnyOrder("WAREHOUSE", "STORE", "DELIVERY", "TRADE_SHOW");
        assertThat(page.page()).isEqualTo(1);
    }
}
