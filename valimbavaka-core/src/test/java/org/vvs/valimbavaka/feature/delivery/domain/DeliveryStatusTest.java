package org.vvs.valimbavaka.feature.delivery.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import org.vvs.valimbavaka.core.exception.ConflictException;

class DeliveryStatusTest {

    @ParameterizedTest
    @CsvSource({
        "TO_PREPARE, ON_ROUND",
        "TO_PREPARE, DELIVERED",
        "TO_PREPARE, FAILED",
        "TO_PREPARE, CANCELLED",
        "ON_ROUND, DELIVERED",
        "ON_ROUND, FAILED"
    })
    void canMoveTo_allowedTransition_returnsTrue(DeliveryStatus sut, DeliveryStatus target) {
        assertThat(sut.canMoveTo(target)).isTrue();
        assertThatCode(() -> sut.ensureCanMoveTo(target)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
        "TO_PREPARE, TO_PREPARE",
        "ON_ROUND, TO_PREPARE",
        "ON_ROUND, ON_ROUND",
        "ON_ROUND, CANCELLED",
        "DELIVERED, FAILED",
        "DELIVERED, ON_ROUND",
        "FAILED, DELIVERED",
        "CANCELLED, TO_PREPARE"
    })
    void canMoveTo_forbiddenTransition_returnsFalse(DeliveryStatus sut, DeliveryStatus target) {
        assertThat(sut.canMoveTo(target)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = DeliveryStatus.class, names = {"DELIVERED", "FAILED", "CANCELLED"})
    void isFinal_finalStatus_leavesToNoOtherStatus(DeliveryStatus sut) {
        assertThat(sut.isFinal()).isTrue();
        for (var target : DeliveryStatus.values()) {
            assertThat(sut.canMoveTo(target)).isFalse();
        }
    }

    @Test
    void ensureCanMoveTo_deliveredToFailed_throwsConflictWithFrenchLabels() {
        assertThatThrownBy(() -> DeliveryStatus.DELIVERED.ensureCanMoveTo(DeliveryStatus.FAILED))
                .isInstanceOf(ConflictException.class)
                .hasMessage("La livraison ne peut pas passer de « livrée » à « en échec ».");
    }
}
