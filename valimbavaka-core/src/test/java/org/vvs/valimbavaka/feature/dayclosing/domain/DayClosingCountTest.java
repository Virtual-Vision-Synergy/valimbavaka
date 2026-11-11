package org.vvs.valimbavaka.feature.dayclosing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class DayClosingCountTest {

    private static final Instant EARLIER = Instant.parse("2026-11-11T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-11-11T18:00:00Z");

    @Test
    void create_countWithAGap_keepsTheReason() {
        // Act
        var sut = count("8", "10", ClosingGapReason.BREAKAGE);

        // Assert
        assertThat(sut.hasGap()).isTrue();
        assertThat(sut.getReason()).isEqualTo(ClosingGapReason.BREAKAGE);
        assertThat(sut.getTheoreticalQuantity()).isEqualByComparingTo("10");
        assertThat(sut.getCountedAt()).isEqualTo(EARLIER);
    }

    @Test
    void create_countWithoutGap_dropsTheReasonSent() {
        // Act
        var sut = count("10.000", "10", ClosingGapReason.LOSS);

        // Assert
        assertThat(sut.hasGap()).isFalse();
        assertThat(sut.getReason()).isNull();
    }

    @Test
    void recount_differentCount_replacesTheValuesAndReturnsTrue() {
        // Arrange
        var sut = count("8", "10", ClosingGapReason.BREAKAGE);

        // Act
        var changed = sut.recount(result("9", "10", ClosingGapReason.LOSS, LATER));

        // Assert
        assertThat(changed).isTrue();
        assertThat(sut.getCountedQuantity()).isEqualByComparingTo("9");
        assertThat(sut.getReason()).isEqualTo(ClosingGapReason.LOSS);
        assertThat(sut.getCountedAt()).isEqualTo(LATER);
    }

    @Test
    void recount_sameValuesWithAnotherScale_changesNothingAndReturnsFalse() {
        // Arrange
        var sut = count("8", "10", ClosingGapReason.BREAKAGE);

        // Act
        var changed = sut.recount(result("8.000", "10.0", ClosingGapReason.BREAKAGE, LATER));

        // Assert
        assertThat(changed).isFalse();
        assertThat(sut.getCountedAt()).isEqualTo(EARLIER);
    }

    @Test
    void recount_gapClosed_clearsTheReason() {
        // Arrange
        var sut = count("8", "10", ClosingGapReason.BREAKAGE);

        // Act
        var changed = sut.recount(result("10", "10", null, LATER));

        // Assert
        assertThat(changed).isTrue();
        assertThat(sut.hasGap()).isFalse();
        assertThat(sut.getReason()).isNull();
    }

    @Test
    void recount_newTheoreticalWithTheSameCount_isAChange() {
        // Arrange: a sale moved the theoretical from 10 to 8 between two saves of the same count
        var sut = count("10", "10", null);

        // Act
        var changed = sut.recount(result("10", "8", ClosingGapReason.LOSS, LATER));

        // Assert
        assertThat(changed).isTrue();
        assertThat(sut.hasGap()).isTrue();
    }

    private static DayClosingCount count(String counted, String theoretical, ClosingGapReason reason) {
        var result = result(counted, theoretical, reason, EARLIER);
        return DayClosingCount.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), result);
    }

    private static CountResult result(String counted, String theoretical, ClosingGapReason reason, Instant at) {
        return new CountResult(new BigDecimal(counted), new BigDecimal(theoretical), reason, at);
    }
}
