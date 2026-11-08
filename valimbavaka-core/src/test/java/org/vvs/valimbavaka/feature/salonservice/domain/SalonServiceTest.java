package org.vvs.valimbavaka.feature.salonservice.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SalonServiceTest {

    private static final UUID COCONUT_OIL = UUID.fromString("00000000-0000-7000-8000-000000000001");
    private static final UUID CLAY_MASK = UUID.fromString("00000000-0000-7000-8000-000000000002");

    @Test
    void create_rawDetails_normalizesNameScalesAndSortsProducts() {
        // Arrange
        var details = details("  Soin visage éclat ", "35000", "12.5", List.of(clayMask("1"), coconutOil("15")));

        // Act
        var sut = SalonService.create(details);

        // Assert
        assertThat(sut.getName()).isEqualTo("Soin visage éclat");
        assertThat(sut.getPrice()).isEqualByComparingTo("35000.00");
        assertThat(sut.getPrice().scale()).isEqualTo(2);
        assertThat(sut.getCommissionRate()).isEqualByComparingTo("12.50");
        assertThat(sut.isActive()).isTrue();
        assertThat(sut.getProducts()).extracting(ConsumedProduct::getArticleId).containsExactly(COCONUT_OIL, CLAY_MASK);
        assertThat(sut.getProducts().get(0).getQuantity().scale()).isEqualTo(3);
    }

    @Test
    void update_sameValuesInOtherOrder_reportsNoChange() {
        // Arrange
        var sut = SalonService.create(details("Soin visage", "35000", null, List.of(coconutOil("15"), clayMask("1"))));
        var same = details(" Soin visage ", "35000.00", null, List.of(clayMask("1.000"), coconutOil("15")));

        // Act
        var changed = sut.update(same);

        // Assert
        assertThat(changed).isFalse();
    }

    @Test
    void update_quantityChangedAndRateRemoved_appliesAndReportsChange() {
        // Arrange
        var sut = SalonService.create(details("Soin visage", "35000", "10", List.of(coconutOil("15"))));

        // Act
        var changed = sut.update(details("Soin visage", "35000", null, List.of(coconutOil("20"))));

        // Assert
        assertThat(changed).isTrue();
        assertThat(sut.getCommissionRate()).isNull();
        assertThat(sut.getProducts().get(0).getQuantity()).isEqualByComparingTo("20.000");
    }

    private static SalonServiceDetails details(
            String name, String price, String commissionRate, List<ConsumedProduct> products) {
        var rate = commissionRate == null ? null : new BigDecimal(commissionRate);
        return new SalonServiceDetails(name, 45, new BigDecimal(price), rate, products);
    }

    private static ConsumedProduct coconutOil(String quantity) {
        return new ConsumedProduct(COCONUT_OIL, new BigDecimal(quantity));
    }

    private static ConsumedProduct clayMask(String quantity) {
        return new ConsumedProduct(CLAY_MASK, new BigDecimal(quantity));
    }
}
