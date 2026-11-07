package org.vvs.valimbavaka.feature.transfer.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TransferOriginTest {

    @Test
    void initialStatus_return_isTheDedicatedReturnStatus() {
        assertThat(TransferOrigin.RETURN.initialStatus()).isEqualTo(TransferStatus.RETURN);
        assertThat(TransferOrigin.RETURN.numberPrefix()).isEqualTo("RET");
    }

    @Test
    void initialStatus_morningOrManual_isValidated() {
        assertThat(TransferOrigin.MORNING.initialStatus()).isEqualTo(TransferStatus.VALIDATED);
        assertThat(TransferOrigin.MANUAL.initialStatus()).isEqualTo(TransferStatus.VALIDATED);
        assertThat(TransferOrigin.MORNING.numberPrefix()).isEqualTo("TRF");
    }

    @Test
    void create_returnWithBlankNote_keepsLinesInOrderAndDropsTheNote() {
        // Arrange
        var first = new TransferLineCommand(UUID.randomUUID(), new BigDecimal("2"));
        var second = new TransferLineCommand(UUID.randomUUID(), new BigDecimal("0.5"));
        var command = new TransferCommand(
                TransferOrigin.RETURN, UUID.randomUUID(), UUID.randomUUID(), List.of(first, second), "   ");

        // Act
        var sut = Transfer.create("RET-0001", command);

        // Assert
        assertThat(sut.getStatus()).isEqualTo(TransferStatus.RETURN);
        assertThat(sut.getNote()).isNull();
        assertThat(sut.getLines())
                .extracting(TransferLine::getArticleId)
                .containsExactly(first.articleId(), second.articleId());
    }
}
