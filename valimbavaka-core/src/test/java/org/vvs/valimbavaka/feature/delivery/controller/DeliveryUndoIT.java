package org.vvs.valimbavaka.feature.delivery.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.user.domain.RoleName;

/**
 * LIV-05: the driver undoes the last action (delivered or failed) for a limited time; the order goes back to its
 * previous status and the sale or the stock return it had created is cancelled.
 */
class DeliveryUndoIT extends DeliveryApiTestSupport {

    /** The delivery needs today's day opened (JR-01); another test class may have removed it. */
    @BeforeEach
    void openToday() throws Exception {
        var result = mockMvc.perform(post("/api/v1/days")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[]}"))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isIn(201, 409);
    }

    @Test
    void undo_deliveredOrder_restoresTheStatusCancelsTheSaleAndPutsTheStockBack() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "5");
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "2")));
        var token = tokenOfDriver(driverId);
        markDelivered(token, orderId).andExpect(status().isOk());
        var saleId = saleIdOf(orderId);

        // Act
        undo(token, orderId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TO_PREPARE"))
                .andExpect(jsonPath("$.saleId").doesNotExist())
                .andExpect(jsonPath("$.previousStatus").doesNotExist());

        // Assert
        assertThat(statusOf(orderId)).isEqualTo("TO_PREPARE");
        assertThat(saleIdOf(orderId)).isNull();
        var saleStatus = jdbcTemplate.queryForObject("select status from sales where id = ?", String.class, saleId);
        assertThat(saleStatus).isEqualTo("CANCELLED");
        assertThat(balance(articleId, "DELIVERY")).isEqualByComparingTo("5");
    }

    @Test
    void undo_orderDeliveredFromARound_restoresOnRound() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "1");
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "1")));
        jdbcTemplate.update("update delivery_orders set status = 'ON_ROUND' where id = ?", orderId);
        var token = tokenOfDriver(driverId);
        markDelivered(token, orderId).andExpect(status().isOk());

        // Act
        undo(token, orderId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ON_ROUND"));

        // Assert
        assertThat(statusOf(orderId)).isEqualTo("ON_ROUND");
    }

    @Test
    void undo_failedOrder_restoresTheStatusAndSendsTheArticlesBackToDelivery() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "3");
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "2")));
        var token = tokenOfDriver(driverId);
        reportFailure(token, orderId).andExpect(status().isOk());
        assertThat(balance(articleId, "DELIVERY")).isEqualByComparingTo("1");

        // Act
        undo(token, orderId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TO_PREPARE"))
                .andExpect(jsonPath("$.failureReason").doesNotExist())
                .andExpect(jsonPath("$.returnTransferId").doesNotExist());

        // Assert
        assertThat(statusOf(orderId)).isEqualTo("TO_PREPARE");
        assertThat(balance(articleId, "DELIVERY")).isEqualByComparingTo("3");
        assertThat(balance(articleId, "WAREHOUSE")).isEqualByComparingTo("0");
    }

    @Test
    void undo_deliveredOrder_isTracedInTheJournalWithoutCustomerData() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "1");
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "1")));
        var token = tokenOfDriver(driverId);
        markDelivered(token, orderId).andExpect(status().isOk());

        // Act
        undo(token, orderId).andExpect(status().isOk());

        // Assert
        var description = jdbcTemplate.queryForObject(
                "select description from journal_entries where resource_type = 'DELIVERY_ORDER' and resource_id = ? "
                        + "and action = 'CANCEL'",
                String.class,
                orderId);
        assertThat(description)
                .startsWith("Annulation de l'action sur la commande LIV-")
                .endsWith(" : de « livrée » à « à préparer »")
                .doesNotContain("Rakoto");
        var saleLines = jdbcTemplate.queryForObject(
                "select count(*) from journal_entries where resource_type = 'SALE' and action = 'CANCEL' "
                        + "and description like 'Annulation de la vente VTE-%'",
                Integer.class);
        assertThat(saleLines).isPositive();
    }

    @Test
    void undo_thenDeliveredAgain_createsASecondSaleAndKeepsTheFirstCancelled() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "2");
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "1")));
        var token = tokenOfDriver(driverId);
        markDelivered(token, orderId).andExpect(status().isOk());
        var firstSaleId = saleIdOf(orderId);
        undo(token, orderId).andExpect(status().isOk());

        // Act
        markDelivered(token, orderId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));

        // Assert
        assertThat(saleIdOf(orderId)).isNotNull().isNotEqualTo(firstSaleId);
        assertThat(balance(articleId, "DELIVERY")).isEqualByComparingTo("1");
    }

    @Test
    void undo_delayOver_returns409AndKeepsTheDelivery() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "1");
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "1")));
        var token = tokenOfDriver(driverId);
        markDelivered(token, orderId).andExpect(status().isOk());
        var saleId = saleIdOf(orderId);
        jdbcTemplate.update(
                "update delivery_orders set updated_at = now() - interval '10 minutes' where id = ?", orderId);

        // Act + Assert
        undo(token, orderId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
        assertThat(statusOf(orderId)).isEqualTo("DELIVERED");
        assertThat(saleIdOf(orderId)).isEqualTo(saleId);
        assertThat(balance(articleId, "DELIVERY")).isEqualByComparingTo("0");
    }

    @Test
    void undo_orderNotDeliveredNorFailed_returns409() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "1")));

        // Act + Assert
        undo(tokenOfDriver(driverId), orderId).andExpect(status().isConflict());
        assertThat(statusOf(orderId)).isEqualTo("TO_PREPARE");
    }

    @Test
    void undo_alreadyUndone_returns409() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "1");
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "1")));
        var token = tokenOfDriver(driverId);
        markDelivered(token, orderId).andExpect(status().isOk());
        undo(token, orderId).andExpect(status().isOk());

        // Act + Assert
        undo(token, orderId).andExpect(status().isConflict());
        assertThat(balance(articleId, "DELIVERY")).isEqualByComparingTo("1");
    }

    @Test
    void undo_failureWhoseArticlesLeftTheWarehouse_returns409AndKeepsTheFailure() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "2");
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "2")));
        var token = tokenOfDriver(driverId);
        reportFailure(token, orderId).andExpect(status().isOk());
        move(articleId, "WAREHOUSE", StockMovementType.INVENTORY, "-2");

        // Act + Assert
        undo(token, orderId).andExpect(status().isConflict());
        assertThat(statusOf(orderId)).isEqualTo("FAILED");
        assertThat(balance(articleId, "DELIVERY")).isEqualByComparingTo("0");
    }

    @Test
    void undo_administratorOnAnyOrder_returns200() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "1");
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "1")));
        markDelivered(tokenOfDriver(driverId), orderId).andExpect(status().isOk());

        // Act + Assert
        undo(adminToken(), orderId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TO_PREPARE"));
    }

    @Test
    void undo_withoutToken_returns401() throws Exception {
        // Act + Assert
        mockMvc.perform(post(DELIVERIES + "/" + UUID.randomUUID() + "/undo"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void undo_sellerRole_returns403() throws Exception {
        // Act + Assert
        undo(tokenOf(RoleName.SELLER), UUID.randomUUID())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void undo_orderOfAnotherDriver_returns404AndKeepsTheDelivery() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "1");
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "1")));
        markDelivered(tokenOfDriver(driverId), orderId).andExpect(status().isOk());

        // Act + Assert
        undo(tokenOfDriver(createDriver()), orderId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        assertThat(statusOf(orderId)).isEqualTo("DELIVERED");
    }

    @Test
    void undo_unknownOrder_returns404() throws Exception {
        // Act + Assert
        undo(adminToken(), UUID.randomUUID()).andExpect(status().isNotFound());
    }

    @Test
    void undo_forbiddenFieldsInTheBody_areIgnored() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "1");
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "1")));
        var token = tokenOfDriver(driverId);
        markDelivered(token, orderId).andExpect(status().isOk());

        // Act
        mockMvc.perform(post(DELIVERIES + "/" + orderId + "/undo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FAILED\",\"role\":\"ADMIN\",\"id\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk());

        // Assert
        assertThat(statusOf(orderId)).isEqualTo("TO_PREPARE");
    }

    @Test
    void undo_replayedWithSameIdempotencyKey_undoesOnce() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "1");
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "1")));
        var token = tokenOfDriver(driverId);
        markDelivered(token, orderId).andExpect(status().isOk());
        var key = UUID.randomUUID().toString();

        // Act
        var first = undoWithKey(token, orderId, key).andExpect(status().isOk()).andReturn();
        var second = undoWithKey(token, orderId, key).andExpect(status().isOk()).andReturn();

        // Assert
        assertThat(second.getResponse().getContentAsString())
                .isEqualTo(first.getResponse().getContentAsString());
        assertThat(balance(articleId, "DELIVERY")).isEqualByComparingTo("1");
    }

    private ResultActions undo(String token, UUID orderId) throws Exception {
        return mockMvc.perform(
                post(DELIVERIES + "/" + orderId + "/undo").header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private ResultActions undoWithKey(String token, UUID orderId, String key) throws Exception {
        return mockMvc.perform(post(DELIVERIES + "/" + orderId + "/undo")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .header("Idempotency-Key", key));
    }

    private ResultActions markDelivered(String token, UUID orderId) throws Exception {
        return mockMvc.perform(
                post(DELIVERIES + "/" + orderId + "/deliver").header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private ResultActions reportFailure(String token, UUID orderId) throws Exception {
        return mockMvc.perform(post(DELIVERIES + "/" + orderId + "/failure")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"ABSENT\"}"));
    }

    private String statusOf(UUID orderId) {
        return jdbcTemplate.queryForObject("select status from delivery_orders where id = ?", String.class, orderId);
    }

    private @Nullable UUID saleIdOf(UUID orderId) {
        return jdbcTemplate.queryForObject("select sale_id from delivery_orders where id = ?", UUID.class, orderId);
    }

    private BigDecimal balance(UUID articleId, String locationCode) {
        return jdbcTemplate.queryForObject(
                "select coalesce(sum(quantity), 0) from stock_movements where article_id = ? and location_id = ?",
                BigDecimal.class,
                articleId,
                location(locationCode));
    }

    private static LocalDate today() {
        return LocalDate.now(BUSINESS_ZONE);
    }
}
