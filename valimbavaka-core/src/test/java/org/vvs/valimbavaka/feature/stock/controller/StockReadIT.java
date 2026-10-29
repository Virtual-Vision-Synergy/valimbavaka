package org.vvs.valimbavaka.feature.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementCommand;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;

/** STK-01: quantity per location and total, computed from the typed movements and traced in the journal. */
class StockReadIT extends StockApiTestSupport {

    @Test
    void getStock_afterReceiptAndTransfer_returnsQuantityPerLocationAndTotal() throws Exception {
        // Arrange
        var articleId = createArticle(uniqueName());
        move(articleId, "WAREHOUSE", StockMovementType.RECEIPT, "30");
        move(articleId, "WAREHOUSE", StockMovementType.TRANSFER, "-10");
        move(articleId, "STORE", StockMovementType.TRANSFER, "10");
        move(articleId, "STORE", StockMovementType.SALE, "-3");

        // Act + Assert
        mockMvc.perform(get(STOCKS + "/" + articleId).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(27.0))
                .andExpect(
                        jsonPath("$.locations[?(@.code=='WAREHOUSE')].quantity").value(20.0))
                .andExpect(jsonPath("$.locations[?(@.code=='STORE')].quantity").value(7.0));
    }

    @Test
    void getStock_articleNeverMoved_returnsEveryActiveLocationAtZero() throws Exception {
        // Arrange
        var articleId = createArticle(uniqueName());

        // Act + Assert
        mockMvc.perform(get(STOCKS + "/" + articleId).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0.0))
                .andExpect(jsonPath("$.locations.length()").value(4));
    }

    @Test
    void getStock_unknownArticle_returns404() throws Exception {
        mockMvc.perform(get(STOCKS + "/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void getStocks_sizeAbove100_returns422() throws Exception {
        mockMvc.perform(get(STOCKS).param("size", "101").header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void getStocks_search_returnsThePagedStockOfTheMatchingArticle() throws Exception {
        // Arrange
        var name = uniqueName();
        var articleId = createArticle(name);
        move(articleId, "TRADE_SHOW", StockMovementType.RECEIPT, "5");

        // Act + Assert
        mockMvc.perform(get(STOCKS).param("search", name).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].articleId").value(articleId.toString()))
                .andExpect(jsonPath("$.items[0].total").value(5.0));
    }

    @Test
    void getStocks_inactiveArticle_isNotListed() throws Exception {
        // Arrange
        var name = uniqueName();
        var articleId = createArticle(name);
        mockMvc.perform(patch("/api/v1/articles/" + articleId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk());

        // Act + Assert
        mockMvc.perform(get(STOCKS).param("search", name).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0));
    }

    @Test
    void record_validMovement_isTracedInTheJournal() throws Exception {
        // Arrange
        var articleId = createArticle(uniqueName());

        // Act
        var movement = stockMovementService.record(new StockMovementCommand(
                articleId, location("WAREHOUSE"), StockMovementType.RECEIPT, new BigDecimal("12"), null));

        // Assert
        var count = jdbcTemplate.queryForObject(
                "select count(*) from journal_entries where resource_type = 'STOCK_MOVEMENT' "
                        + "and resource_id = ? and action = 'CREATE'",
                Integer.class,
                movement.id());
        assertThat(count).isEqualTo(1);
    }

    @Test
    void record_withdrawalAboveBalance_throwsConflictAndLeavesNoTrace() throws Exception {
        // Arrange
        var articleId = createArticle(uniqueName());
        move(articleId, "WAREHOUSE", StockMovementType.RECEIPT, "3");

        // Act + Assert
        assertThatThrownBy(() -> move(articleId, "WAREHOUSE", StockMovementType.SALE, "-4"))
                .isInstanceOf(ConflictException.class);
        var movements = jdbcTemplate.queryForObject(
                "select count(*) from stock_movements where article_id = ?", Integer.class, articleId);
        assertThat(movements).isEqualTo(1);
    }

    @Test
    void record_concurrentWithdrawals_neverTakeTheStockBelowZero() throws Exception {
        // Arrange
        var articleId = createArticle(uniqueName());
        move(articleId, "WAREHOUSE", StockMovementType.RECEIPT, "3");
        var attempts = 8;
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(attempts);
        var tasks = new ArrayList<Callable<Boolean>>();
        for (var i = 0; i < attempts; i++) {
            tasks.add(() -> {
                start.await();
                try {
                    move(articleId, "WAREHOUSE", StockMovementType.SALE, "-1");
                    return true;
                } catch (ConflictException e) {
                    return false;
                }
            });
        }

        // Act
        var futures = tasks.stream().map(pool::submit).toList();
        start.countDown();
        var accepted = 0;
        for (var future : futures) {
            accepted += future.get() ? 1 : 0;
        }
        pool.shutdown();

        // Assert
        assertThat(accepted).isEqualTo(3);
        var balance = jdbcTemplate.queryForObject(
                "select coalesce(sum(quantity), 0) from stock_movements where article_id = ?",
                BigDecimal.class,
                articleId);
        assertThat(balance).isEqualByComparingTo("0");
    }
}
