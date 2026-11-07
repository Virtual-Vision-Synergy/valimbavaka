package org.vvs.valimbavaka.feature.shooting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.vvs.valimbavaka.core.exception.ConflictException;

class ShootingTest {

    private static final Instant EXPECTED_RETURN = Instant.parse("2026-11-08T16:00:00Z");

    @Test
    void open_newExit_isOutWithItsItems() {
        // Arrange
        var item = ShootingItem.of(UUID.randomUUID(), "ART-0031", "Gommage café 200 g", new BigDecimal("2"));
        var draft = new ShootingDraft(UUID.randomUUID(), "Miora Andria", EXPECTED_RETURN, null, List.of(item));

        // Act
        var sut = Shooting.open("SHO-0042", draft);

        // Assert
        assertThat(sut.getNumber()).isEqualTo("SHO-0042");
        assertThat(sut.getStatus()).isEqualTo(ShootingStatus.OUT);
        assertThat(sut.getTakenByName()).isEqualTo("Miora Andria");
        assertThat(sut.getItems()).containsExactly(item);
    }

    @Test
    void isLateAt_afterExpectedReturnWhileOut_returnsTrue() {
        // Arrange
        var sut = Shooting.open("SHO-0001", draftReturningAt(EXPECTED_RETURN));

        // Act
        var late = sut.isLateAt(EXPECTED_RETURN.plusSeconds(60));

        // Assert
        assertThat(late).isTrue();
    }

    @Test
    void isLateAt_beforeOrAtExpectedReturn_returnsFalse() {
        // Arrange
        var sut = Shooting.open("SHO-0001", draftReturningAt(EXPECTED_RETURN));

        // Act + Assert
        assertThat(sut.isLateAt(EXPECTED_RETURN.minusSeconds(60))).isFalse();
        assertThat(sut.isLateAt(EXPECTED_RETURN)).isFalse();
    }

    @Test
    void markReturned_outExit_isReturnedWithDateAndAuthor() {
        // Arrange
        var sut = Shooting.open("SHO-0042", draftReturningAt(EXPECTED_RETURN));
        var returnedAt = EXPECTED_RETURN.minusSeconds(600);

        // Act
        sut.markReturned(returnedAt, "Rasoa Rakoto");

        // Assert
        assertThat(sut.getStatus()).isEqualTo(ShootingStatus.RETURNED);
        assertThat(sut.getReturnedAt()).isEqualTo(returnedAt);
        assertThat(sut.getReturnedByName()).isEqualTo("Rasoa Rakoto");
    }

    @Test
    void markReturned_alreadyReturned_throwsConflictExceptionAndKeepsTheFirstReturn() {
        // Arrange
        var sut = Shooting.open("SHO-0042", draftReturningAt(EXPECTED_RETURN));
        sut.markReturned(EXPECTED_RETURN, "Rasoa Rakoto");

        // Act + Assert
        assertThatThrownBy(() -> sut.markReturned(EXPECTED_RETURN.plusSeconds(60), "Miora Andria"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("SHO-0042");
        assertThat(sut.getReturnedAt()).isEqualTo(EXPECTED_RETURN);
        assertThat(sut.getReturnedByName()).isEqualTo("Rasoa Rakoto");
    }

    @Test
    void isLateAt_returnedAfterExpectedReturn_returnsFalse() {
        // Arrange
        var sut = Shooting.open("SHO-0001", draftReturningAt(EXPECTED_RETURN));
        sut.markReturned(EXPECTED_RETURN.plusSeconds(3600), "Rasoa Rakoto");

        // Act + Assert
        assertThat(sut.isLateAt(EXPECTED_RETURN.plusSeconds(7200))).isFalse();
    }

    private static ShootingDraft draftReturningAt(Instant expectedReturnAt) {
        var item = ShootingItem.of(UUID.randomUUID(), "ART-0001", "Savon noir 100 g", BigDecimal.ONE);
        return new ShootingDraft(UUID.randomUUID(), "Miora Andria", expectedReturnAt, null, List.of(item));
    }
}
