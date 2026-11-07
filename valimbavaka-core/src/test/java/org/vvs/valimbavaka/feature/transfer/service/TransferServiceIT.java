package org.vvs.valimbavaka.feature.transfer.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.vvs.valimbavaka.feature.stock.controller.StockApiTestSupport;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.transfer.domain.TransferCommand;
import org.vvs.valimbavaka.feature.transfer.domain.TransferLineCommand;
import org.vvs.valimbavaka.feature.transfer.domain.TransferOrigin;
import org.vvs.valimbavaka.feature.transfer.domain.TransferStatus;

/** TRF-03: the transfer port reused by the other features (morning transfers of JR-01 and TRF-02). */
class TransferServiceIT extends StockApiTestSupport {

    @Autowired
    private TransferService transferService;

    @Test
    void record_morningTransfer_isValidatedWithATransferNumber() throws Exception {
        // Arrange
        var articleId = createArticle(uniqueName());
        move(articleId, "WAREHOUSE", StockMovementType.RECEIPT, "20");

        // Act
        var transfer = transferService.record(command(TransferOrigin.MORNING, "WAREHOUSE", "STORE", articleId));

        // Assert
        assertThat(transfer.status()).isEqualTo(TransferStatus.VALIDATED);
        assertThat(transfer.origin()).isEqualTo(TransferOrigin.MORNING);
        assertThat(transfer.number()).matches("TRF-\\d{4,}");
        var store = jdbcTemplate.queryForObject(
                "select sum(quantity) from stock_movements where article_id = ? and location_id = ?",
                BigDecimal.class,
                articleId,
                location("STORE"));
        assertThat(store).isEqualByComparingTo("1");
    }

    @Test
    void record_crossedTransfersAtTheSameTime_neverDeadlock() throws Exception {
        // Arrange: each transfer locks the same two articles in both locations, listed in opposite orders.
        var first = createArticle(uniqueName());
        var second = createArticle(uniqueName());
        for (var articleId : List.of(first, second)) {
            move(articleId, "STORE", StockMovementType.RECEIPT, "50");
            move(articleId, "WAREHOUSE", StockMovementType.RECEIPT, "50");
        }
        var pairs = 4;
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(pairs * 2);
        var tasks = new ArrayList<Callable<Boolean>>();
        for (var i = 0; i < pairs; i++) {
            tasks.add(transferTask(start, command(TransferOrigin.RETURN, "STORE", "WAREHOUSE", first, second)));
            tasks.add(transferTask(start, command(TransferOrigin.MANUAL, "WAREHOUSE", "STORE", second, first)));
        }

        // Act
        var futures = tasks.stream().map(pool::submit).toList();
        start.countDown();
        var recorded = 0;
        for (var future : futures) {
            recorded += future.get() ? 1 : 0;
        }
        pool.shutdown();

        // Assert
        assertThat(recorded).isEqualTo(pairs * 2);
        var total = jdbcTemplate.queryForObject(
                "select sum(quantity) from stock_movements where article_id = ?", BigDecimal.class, first);
        assertThat(total).isEqualByComparingTo("100");
    }

    private Callable<Boolean> transferTask(CountDownLatch start, TransferCommand command) {
        return () -> {
            start.await();
            transferService.record(command);
            return true;
        };
    }

    private TransferCommand command(TransferOrigin origin, String fromCode, String toCode, UUID... articleIds) {
        var lines = new ArrayList<TransferLineCommand>();
        for (var articleId : articleIds) {
            lines.add(new TransferLineCommand(articleId, BigDecimal.ONE));
        }
        return new TransferCommand(origin, location(fromCode), location(toCode), lines, null);
    }
}
