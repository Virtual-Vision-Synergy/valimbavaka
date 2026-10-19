package org.vvs.valimbavaka.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LockPolicyTest {

    private final LockPolicy sut = new LockPolicy(5, Duration.ofMinutes(5));

    @ParameterizedTest
    @CsvSource({"1,0", "4,0", "5,5", "6,10", "7,15", "100,60"})
    void lockDurationFor_failedAttempts_growsAfterThresholdAndIsCapped(int failedAttempts, long expectedMinutes) {
        // Act
        var duration = sut.lockDurationFor(failedAttempts);

        // Assert
        assertThat(duration).isEqualTo(Duration.ofMinutes(expectedMinutes));
    }
}
