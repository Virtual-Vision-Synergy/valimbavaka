package org.vvs.valimbavaka.feature.transfer.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.stock.controller.StockApiTestSupport;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.user.domain.RoleName;

/** TRF-03: a stock return moves the stock from one location to another and carries the dedicated RETURN status. */
class StockReturnIT extends StockApiTestSupport {

    private static final String RETURNS = "/api/v1/transfers/returns";

    @Test
    void recordReturn_storeToWarehouse_returns201WithReturnStatusAndMovesTheStock() throws Exception {
        // Arrange
        var articleId = createArticle(uniqueName());
        move(articleId, "STORE", StockMovementType.RECEIPT, "10");

        // Act
        var result = recordReturn(adminToken(), body("STORE", "WAREHOUSE", line(articleId, "4")))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.status").value("RETURN"))
                .andExpect(jsonPath("$.origin").value("RETURN"))
                .andExpect(jsonPath("$.number").value(matchesPattern("RET-\\d{4,}")))
                .andExpect(jsonPath("$.fromLocationId").value(location("STORE").toString()))
                .andExpect(jsonPath("$.toLocationId").value(location("WAREHOUSE").toString()))
                .andExpect(jsonPath("$.lines[0].articleId").value(articleId.toString()))
                .andExpect(jsonPath("$.lines[0].quantity").value(4.0))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn();

        // Assert
        assertThat(balance(articleId, "STORE")).isEqualByComparingTo("6");
        assertThat(balance(articleId, "WAREHOUSE")).isEqualByComparingTo("4");
        var transferId = UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
        var legs = jdbcTemplate.queryForObject(
                "select count(*) from stock_movements where reference_id = ? and type = 'TRANSFER'",
                Integer.class,
                transferId);
        assertThat(legs).isEqualTo(2);
    }

    @Test
    void recordReturn_validReturn_isTracedInTheJournal() throws Exception {
        // Arrange
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "3");

        // Act
        var result = recordReturn(adminToken(), body("DELIVERY", "WAREHOUSE", line(articleId, "2.5")))
                .andExpect(status().isCreated())
                .andReturn();

        // Assert
        var transferId = UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
        var description = jdbcTemplate.queryForObject(
                "select description from journal_entries where resource_type = 'TRANSFER' and resource_id = ? "
                        + "and action = 'CREATE'",
                String.class,
                transferId);
        assertThat(description).startsWith("Retour de stock RET-").contains("1 article(s), 2.5 unité(s)");
    }

    @Test
    void recordReturn_twoReturns_getDistinctReturnNumbers() throws Exception {
        // Arrange
        var articleId = createArticle(uniqueName());
        move(articleId, "STORE", StockMovementType.RECEIPT, "2");

        // Act
        var first = numberOf(recordReturn(adminToken(), body("STORE", "WAREHOUSE", line(articleId, "1"))));
        var second = numberOf(recordReturn(adminToken(), body("STORE", "WAREHOUSE", line(articleId, "1"))));

        // Assert
        assertThat(first).startsWith("RET-");
        assertThat(second).startsWith("RET-").isNotEqualTo(first);
    }

    @Test
    void recordReturn_withSellerRole_returns201() throws Exception {
        // Arrange
        var articleId = createArticle(uniqueName());
        move(articleId, "STORE", StockMovementType.RECEIPT, "5");

        // Act + Assert
        recordReturn(tokenOf(RoleName.SELLER), body("STORE", "WAREHOUSE", line(articleId, "5")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RETURN"));
    }

    @Test
    void recordReturn_withoutToken_returns401() throws Exception {
        mockMvc.perform(post(RETURNS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("STORE", "WAREHOUSE", line(UUID.randomUUID(), "1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void recordReturn_withDeliveryDriverRole_returns403() throws Exception {
        recordReturn(tokenOf(RoleName.DELIVERY_DRIVER), body("STORE", "WAREHOUSE", line(UUID.randomUUID(), "1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void recordReturn_massAssignmentOfIdNumberAndStatus_isIgnored() throws Exception {
        // Arrange
        var articleId = createArticle(uniqueName());
        move(articleId, "STORE", StockMovementType.RECEIPT, "1");
        var forcedId = UUID.randomUUID();
        var body = """
                {"id":"%s","number":"RET-9999","status":"VALIDATED","origin":"MORNING",
                 "fromLocationId":"%s","toLocationId":"%s","lines":[%s]}
                """.formatted(forcedId, location("STORE"), location("WAREHOUSE"), line(articleId, "1"));

        // Act + Assert
        recordReturn(adminToken(), body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(not(forcedId.toString())))
                .andExpect(jsonPath("$.number").value(not("RET-9999")))
                .andExpect(jsonPath("$.status").value("RETURN"))
                .andExpect(jsonPath("$.origin").value("RETURN"));
    }

    @Test
    void recordReturn_insufficientStockOnOneLine_returns409AndMovesNothing() throws Exception {
        // Arrange
        var stocked = createArticle(uniqueName());
        var empty = createArticle(uniqueName());
        move(stocked, "STORE", StockMovementType.RECEIPT, "5");
        var body = body("STORE", "WAREHOUSE", line(stocked, "2") + "," + line(empty, "1"));

        // Act + Assert
        recordReturn(adminToken(), body)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
        assertThat(balance(stocked, "STORE")).isEqualByComparingTo("5");
        var transfers = jdbcTemplate.queryForObject(
                "select count(*) from transfer_lines where article_id = ?", Integer.class, stocked);
        assertThat(transfers).isZero();
    }

    @Test
    void recordReturn_sameOriginAndDestination_returns422() throws Exception {
        assertValidationFailed(body("STORE", "STORE", line(createArticle(uniqueName()), "1")));
    }

    @Test
    void recordReturn_noLine_returns422() throws Exception {
        assertValidationFailed(body("STORE", "WAREHOUSE", ""));
    }

    @Test
    void recordReturn_101Lines_returns422() throws Exception {
        var lines = new StringBuilder(line(UUID.randomUUID(), "1"));
        for (var i = 1; i < 101; i++) {
            lines.append(',').append(line(UUID.randomUUID(), "1"));
        }
        assertValidationFailed(body("STORE", "WAREHOUSE", lines.toString()));
    }

    @Test
    void recordReturn_zeroQuantity_returns422() throws Exception {
        assertValidationFailed(body("STORE", "WAREHOUSE", line(createArticle(uniqueName()), "0")));
    }

    @Test
    void recordReturn_negativeQuantity_returns422() throws Exception {
        assertValidationFailed(body("STORE", "WAREHOUSE", line(createArticle(uniqueName()), "-2")));
    }

    @Test
    void recordReturn_quantityWithFourDecimals_returns422() throws Exception {
        assertValidationFailed(body("STORE", "WAREHOUSE", line(createArticle(uniqueName()), "1.0005")));
    }

    @Test
    void recordReturn_sameArticleTwice_returns422() throws Exception {
        var articleId = createArticle(uniqueName());
        move(articleId, "STORE", StockMovementType.RECEIPT, "5");
        assertValidationFailed(body("STORE", "WAREHOUSE", line(articleId, "1") + "," + line(articleId, "2")));
    }

    @Test
    void recordReturn_unknownLocation_returns422() throws Exception {
        var body = """
                {"fromLocationId":"%s","toLocationId":"%s","lines":[%s]}
                """.formatted(UUID.randomUUID(), location("WAREHOUSE"), line(createArticle(uniqueName()), "1"));
        assertValidationFailed(body);
    }

    @Test
    void recordReturn_unknownArticle_returns404() throws Exception {
        recordReturn(adminToken(), body("STORE", "WAREHOUSE", line(UUID.randomUUID(), "1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void recordReturn_replayedWithSameIdempotencyKey_recordsOneReturn() throws Exception {
        // Arrange
        var articleId = createArticle(uniqueName());
        move(articleId, "STORE", StockMovementType.RECEIPT, "3");
        var body = body("STORE", "WAREHOUSE", line(articleId, "3"));
        var key = UUID.randomUUID().toString();
        var token = adminToken();

        // Act
        var first = mockMvc.perform(post(RETURNS)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        mockMvc.perform(post(RETURNS)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andExpect(jsonPath("$.id").value(idOf(first.getResponse().getContentAsString())));

        // Assert
        assertThat(balance(articleId, "STORE")).isEqualByComparingTo("0");
        assertThat(balance(articleId, "WAREHOUSE")).isEqualByComparingTo("3");
    }

    private ResultActions recordReturn(String token, String body) throws Exception {
        return mockMvc.perform(post(RETURNS)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private void assertValidationFailed(String body) throws Exception {
        recordReturn(adminToken(), body)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private String body(String fromCode, String toCode, String lines) {
        return """
                {"fromLocationId":"%s","toLocationId":"%s","lines":[%s],"note":"Invendus du soir"}
                """.formatted(location(fromCode), location(toCode), lines);
    }

    private static String line(UUID articleId, String quantity) {
        return "{\"articleId\":\"%s\",\"quantity\":%s}".formatted(articleId, quantity);
    }

    private static String idOf(String json) {
        return JsonPath.read(json, "$.id");
    }

    private static String numberOf(ResultActions result) throws Exception {
        return JsonPath.read(
                result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.number");
    }

    private BigDecimal balance(UUID articleId, String locationCode) {
        return jdbcTemplate.queryForObject(
                "select coalesce(sum(quantity), 0) from stock_movements where article_id = ? and location_id = ?",
                BigDecimal.class,
                articleId,
                location(locationCode));
    }
}
