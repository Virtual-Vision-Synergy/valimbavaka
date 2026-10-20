package org.vvs.valimbavaka.shared.operation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.vvs.valimbavaka.core.exception.ConflictException;

class OperationStatusTest {

    @Test
    void ensureCanCancel_validatedOperation_passes() {
        // Arrange
        var sut = OperationStatus.VALIDATED;

        // Act + Assert
        assertThatCode(sut::ensureCanCancel).doesNotThrowAnyException();
    }

    @Test
    void ensureCanReverse_validatedOperation_passes() {
        // Arrange
        var sut = OperationStatus.VALIDATED;

        // Act + Assert
        assertThatCode(sut::ensureCanReverse).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(value = OperationStatus.class, names = "VALIDATED", mode = EnumSource.Mode.EXCLUDE)
    void ensureCanCancel_alreadyClosedOperation_throwsConflict(OperationStatus sut) {
        // Act + Assert
        assertThatThrownBy(sut::ensureCanCancel)
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining(sut.name());
    }

    @ParameterizedTest
    @EnumSource(value = OperationStatus.class, names = "VALIDATED", mode = EnumSource.Mode.EXCLUDE)
    void ensureCanReverse_alreadyClosedOperation_throwsConflict(OperationStatus sut) {
        // Act + Assert
        assertThatThrownBy(sut::ensureCanReverse)
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining(sut.name());
    }
}
