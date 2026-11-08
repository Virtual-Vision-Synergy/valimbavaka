package org.vvs.valimbavaka.feature.delivery.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.user.domain.RoleName;

/** LIV-04: a failed delivery requires a reason and brings its articles back from delivery to the warehouse. */
class DeliveryFailureIT extends DeliveryApiTestSupport {

    private static final String TRANSFER_SQL = "select number, origin, status from transfers where id = ?";
    private static final String ABSENT = "{\"reason\":\"ABSENT\",\"note\":\"  Portail fermé, personne  \"}";

    @Test
    void reportFailure_driverOwnOrder_returns200FailedAndMovesTheArticlesBackToTheWarehouse() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "5");
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "2")));

        // Act
        var result = reportFailure(tokenOfDriver(driverId), orderId, ABSENT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("ABSENT"))
                .andExpect(jsonPath("$.failureNote").value("Portail fermé, personne"))
                .andExpect(jsonPath("$.returnTransferId").isNotEmpty())
                .andReturn();

        // Assert
        assertThat(balance(articleId, "DELIVERY")).isEqualByComparingTo("3");
        assertThat(balance(articleId, "WAREHOUSE")).isEqualByComparingTo("2");
        var json = result.getResponse().getContentAsString();
        var transferId = UUID.fromString(JsonPath.read(json, "$.returnTransferId"));
        var transfer = jdbcTemplate.queryForMap(TRANSFER_SQL, transferId);
        assertThat((String) transfer.get("number")).startsWith("RET-");
        assertThat(transfer.get("origin")).isEqualTo("RETURN");
        assertThat(transfer.get("status")).isEqualTo("RETURN");
    }

    @Test
    void reportFailure_failedOrder_createsNoSaleMovementOnlyTheReturnTransfer() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "1");
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "1")));

        // Act
        reportFailure(tokenOfDriver(driverId), orderId, ABSENT).andExpect(status().isOk());

        // Assert
        List<String> types = jdbcTemplate.queryForList(
                "select type from stock_movements where article_id = ?", String.class, articleId);
        assertThat(types).containsExactlyInAnyOrder("RECEIPT", "TRANSFER", "TRANSFER");
    }

    @Test
    void reportFailure_validFailure_isTracedInTheJournalWithoutTheDriverComment() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "1");
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "1")));

        // Act
        reportFailure(tokenOfDriver(driverId), orderId, ABSENT).andExpect(status().isOk());

        // Assert
        var description = jdbcTemplate.queryForObject(
                "select description from journal_entries where resource_type = 'DELIVERY_ORDER' and resource_id = ? "
                        + "and action = 'UPDATE'",
                String.class,
                orderId);
        assertThat(description)
                .startsWith("Commande LIV-")
                .endsWith("en échec : client absent, 1 article(s) remis en stock")
                .doesNotContain("Portail");
    }

    @Test
    void reportFailure_administratorOnAnyOrder_returns200() throws Exception {
        // Arrange
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "1");
        var token = adminToken();
        var orderId = createdOrderId(token, body(createDriver(), today(), line(articleId, "1")));

        // Act + Assert
        reportFailure(token, orderId, "{\"reason\":\"ADDRESS_NOT_FOUND\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.failureReason").value("ADDRESS_NOT_FOUND"))
                .andExpect(jsonPath("$.failureNote").doesNotExist());
    }

    @Test
    void reportFailure_withoutReason_returns422AndKeepsTheOrder() throws Exception {
        // Arrange
        var driverId = createDriver();
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(createArticle(uniqueName()), "1")));

        // Act
        reportFailure(tokenOfDriver(driverId), orderId, "{\"note\":\"Pas de motif\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        // Assert
        assertThat(statusOf(orderId)).isEqualTo("TO_PREPARE");
    }

    @Test
    void reportFailure_commentOver280Characters_returns422() throws Exception {
        // Arrange
        var driverId = createDriver();
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(createArticle(uniqueName()), "1")));
        var body = "{\"reason\":\"OTHER\",\"note\":\"%s\"}".formatted("x".repeat(281));

        // Act + Assert
        reportFailure(tokenOfDriver(driverId), orderId, body)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void reportFailure_unknownReason_returns400() throws Exception {
        // Arrange
        var driverId = createDriver();
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(createArticle(uniqueName()), "1")));

        // Act + Assert
        reportFailure(tokenOfDriver(driverId), orderId, "{\"reason\":\"RAIN\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void reportFailure_withoutToken_returns401() throws Exception {
        mockMvc.perform(post(DELIVERIES + "/" + UUID.randomUUID() + "/failure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ABSENT))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void reportFailure_sellerRole_returns403() throws Exception {
        // Arrange
        var articleId = createArticle(uniqueName());
        var orderId = createdOrderId(adminToken(), body(createDriver(), today(), line(articleId, "1")));

        // Act + Assert
        reportFailure(tokenOf(RoleName.SELLER), orderId, ABSENT)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void reportFailure_orderOfAnotherDriver_returns404AndKeepsTheOrder() throws Exception {
        // Arrange
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "1");
        var orderId = createdOrderId(adminToken(), body(createDriver(), today(), line(articleId, "1")));

        // Act
        reportFailure(tokenOfDriver(createDriver()), orderId, ABSENT)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Assert
        assertThat(statusOf(orderId)).isEqualTo("TO_PREPARE");
        assertThat(balance(articleId, "DELIVERY")).isEqualByComparingTo("1");
    }

    @Test
    void reportFailure_unknownOrder_returns404() throws Exception {
        reportFailure(adminToken(), UUID.randomUUID(), ABSENT)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void reportFailure_orderAlreadyFailed_returns409AndReturnsTheArticlesOnlyOnce() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "4");
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "2")));
        var token = tokenOfDriver(driverId);
        reportFailure(token, orderId, ABSENT).andExpect(status().isOk());

        // Act
        reportFailure(token, orderId, "{\"reason\":\"REFUSED\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        // Assert
        assertThat(balance(articleId, "DELIVERY")).isEqualByComparingTo("2");
        assertThat(balance(articleId, "WAREHOUSE")).isEqualByComparingTo("2");
    }

    @Test
    void reportFailure_articlesNotInTheDeliveryLocation_returns409AndChangesNothing() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "2")));

        // Act
        reportFailure(tokenOfDriver(driverId), orderId, ABSENT)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        // Assert
        assertThat(statusOf(orderId)).isEqualTo("TO_PREPARE");
        var movements = jdbcTemplate.queryForObject(
                "select count(*) from stock_movements where article_id = ?", Integer.class, articleId);
        assertThat(movements).isZero();
    }

    @Test
    void reportFailure_forbiddenFieldsInTheBody_areIgnored() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "1");
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "1")));
        var body = """
                {"reason":"REFUSED","id":"%s","status":"DELIVERED","returnTransferId":"%s","driverId":"%s"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        // Act + Assert
        reportFailure(tokenOfDriver(driverId), orderId, body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.driverId").value(driverId.toString()));
        assertThat(balance(articleId, "WAREHOUSE")).isEqualByComparingTo("1");
    }

    @Test
    void reportFailure_replayedWithSameIdempotencyKey_failsTheOrderOnce() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "3");
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "1")));
        var token = tokenOfDriver(driverId);
        var key = UUID.randomUUID().toString();

        // Act
        replay(token, orderId, key).andExpect(status().isOk());
        replay(token, orderId, key)
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andExpect(jsonPath("$.status").value("FAILED"));

        // Assert
        assertThat(balance(articleId, "DELIVERY")).isEqualByComparingTo("2");
        assertThat(balance(articleId, "WAREHOUSE")).isEqualByComparingTo("1");
    }

    private ResultActions reportFailure(String token, UUID orderId, String body) throws Exception {
        return mockMvc.perform(post(DELIVERIES + "/" + orderId + "/failure")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions replay(String token, UUID orderId, String key) throws Exception {
        return mockMvc.perform(post(DELIVERIES + "/" + orderId + "/failure")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(ABSENT));
    }

    private String statusOf(UUID orderId) {
        return jdbcTemplate.queryForObject("select status from delivery_orders where id = ?", String.class, orderId);
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
