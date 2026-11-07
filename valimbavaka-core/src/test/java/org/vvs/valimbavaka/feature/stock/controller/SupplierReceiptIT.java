package org.vvs.valimbavaka.feature.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.user.domain.RoleName;

/** STK-06: supplier receipt putting the goods in stock, corrected by a reversal, reserved to stocks.receive. */
class SupplierReceiptIT extends StockApiTestSupport {

    private static final String RECEIPTS = STOCKS + "/receipts";

    @Test
    void receive_freeEntryWithoutLocation_returns201AndPutsTheGoodsInTheWarehouse() throws Exception {
        // Arrange
        var token = adminToken();
        var supplierId = createSupplier(token);
        var articleId = createArticle(uniqueName());

        // Act
        var result = mockMvc.perform(receive(receiptBody(supplierId, articleId, "12"), token))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.number").value(matchesPattern("REC-\\d{6}")))
                .andExpect(jsonPath("$.status").value("VALIDATED"))
                .andExpect(jsonPath("$.supplierId").value(supplierId.toString()))
                .andExpect(jsonPath("$.locationId").value(location("WAREHOUSE").toString()))
                .andExpect(jsonPath("$.restockOrderId").isEmpty())
                .andExpect(jsonPath("$.lines[0].articleId").value(articleId.toString()))
                .andExpect(jsonPath("$.lines[0].quantity").value(12.0))
                .andExpect(jsonPath("$.lines[0].lineCost").value(37446.0))
                .andExpect(jsonPath("$.totalCost").value(37446.0))
                .andReturn();

        // Assert
        var receiptId = idOf(result);
        assertThat(balance(articleId, "WAREHOUSE")).isEqualByComparingTo("12");
        assertThat(movements(receiptId, StockMovementType.RECEIPT)).isEqualTo(1L);
        assertThat(traces(receiptId, "CREATE")).isEqualTo(1L);
    }

    @Test
    void receive_chosenLocation_putsTheGoodsThere() throws Exception {
        // Arrange
        var token = adminToken();
        var articleId = createArticle(uniqueName());
        var body = """
                {"supplierId":"%s","locationId":"%s","lines":[{"articleId":"%s","quantity":2.5,"unitCost":0}]}
                """.formatted(createSupplier(token), location("STORE"), articleId);

        // Act
        mockMvc.perform(receive(body, token)).andExpect(status().isCreated());

        // Assert
        assertThat(balance(articleId, "STORE")).isEqualByComparingTo("2.5");
        assertThat(balance(articleId, "WAREHOUSE")).isEqualByComparingTo("0");
    }

    @Test
    void receive_fromRestockOrder_marksItReceivedAndRefusesASecondReceipt() throws Exception {
        // Arrange
        var token = adminToken();
        var supplierId = createSupplier(token);
        var articleId = createArticle(uniqueName());
        var orderId = generateRestockOrder(articleId, token);
        var body = """
                {"supplierId":"%s","restockOrderId":"%s","lines":[{"articleId":"%s","quantity":40,"unitCost":3000}]}
                """.formatted(supplierId, orderId, articleId);

        // Act
        mockMvc.perform(receive(body, token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.restockOrderId").value(orderId.toString()));

        // Assert
        mockMvc.perform(get(STOCKS + "/restock-orders/" + orderId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.status").value("RECEIVED"));
        mockMvc.perform(receive(body, token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
        assertThat(balance(articleId, "WAREHOUSE")).isEqualByComparingTo("40");
    }

    @Test
    void receive_unknownRestockOrder_returns404AndMovesNothing() throws Exception {
        // Arrange
        var token = adminToken();
        var articleId = createArticle(uniqueName());
        var body = """
                {"supplierId":"%s","restockOrderId":"%s","lines":[{"articleId":"%s","quantity":1,"unitCost":1}]}
                """.formatted(createSupplier(token), UUID.randomUUID(), articleId);

        // Act + Assert
        mockMvc.perform(receive(body, token)).andExpect(status().isNotFound());
        assertThat(balance(articleId, "WAREHOUSE")).isEqualByComparingTo("0");
    }

    @Test
    void receive_massAssignmentOfIdStatusAndNumber_isIgnored() throws Exception {
        // Arrange
        var token = adminToken();
        var forcedId = UUID.randomUUID();
        var body = """
                {"id":"%s","status":"REVERSED","number":"REC-999999","supplierId":"%s",
                 "lines":[{"articleId":"%s","quantity":1,"unitCost":10}]}
                """.formatted(forcedId, createSupplier(token), createArticle(uniqueName()));

        // Act + Assert
        mockMvc.perform(receive(body, token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(not(forcedId.toString())))
                .andExpect(jsonPath("$.status").value("VALIDATED"))
                .andExpect(jsonPath("$.number").value(not("REC-999999")));
    }

    @Test
    void receive_sameArticleTwice_returns422() throws Exception {
        // Arrange
        var token = adminToken();
        var articleId = createArticle(uniqueName());
        var body = """
                {"supplierId":"%s","lines":[{"articleId":"%s","quantity":1,"unitCost":10},
                 {"articleId":"%s","quantity":2,"unitCost":10}]}
                """.formatted(createSupplier(token), articleId, articleId);

        // Act + Assert
        mockMvc.perform(receive(body, token))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void receive_invalidLines_returns422() throws Exception {
        // Arrange
        var token = adminToken();
        var supplierId = createSupplier(token);
        var articleId = createArticle(uniqueName());

        // Act + Assert
        mockMvc.perform(receive("{\"supplierId\":\"%s\",\"lines\":[]}".formatted(supplierId), token))
                .andExpect(status().isUnprocessableContent());
        mockMvc.perform(receive(receiptBody(supplierId, articleId, "0"), token))
                .andExpect(status().isUnprocessableContent());
        var negativeCost = """
                {"supplierId":"%s","lines":[{"articleId":"%s","quantity":1,"unitCost":-1}]}
                """.formatted(supplierId, articleId);
        mockMvc.perform(receive(negativeCost, token)).andExpect(status().isUnprocessableContent());
    }

    @Test
    void receive_deactivatedSupplier_returns422() throws Exception {
        // Arrange
        var token = adminToken();
        var supplierId = createSupplier(token);
        mockMvc.perform(patch("/api/v1/suppliers/" + supplierId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk());

        // Act + Assert
        mockMvc.perform(receive(receiptBody(supplierId, createArticle(uniqueName()), "5"), token))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void receive_unknownSupplier_returns404() throws Exception {
        var token = adminToken();

        mockMvc.perform(receive(receiptBody(UUID.randomUUID(), createArticle(uniqueName()), "5"), token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void reverse_validatedReceipt_returnsReversedAndTakesTheGoodsBackOut() throws Exception {
        // Arrange
        var token = adminToken();
        var articleId = createArticle(uniqueName());
        var receiptId = createReceipt(articleId, "12", token);

        // Act
        mockMvc.perform(reverse(receiptId, "{\"reason\":\"Livraison saisie deux fois\"}", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVERSED"));

        // Assert
        assertThat(balance(articleId, "WAREHOUSE")).isEqualByComparingTo("0");
        assertThat(movements(receiptId, StockMovementType.RECEIPT)).isEqualTo(2L);
        assertThat(traces(receiptId, "REVERSE")).isEqualTo(1L);
        mockMvc.perform(reverse(receiptId, "{\"reason\":\"Encore\"}", token))
                .andExpect(status().isConflict());
    }

    @Test
    void reverse_goodsAlreadyMovedOut_returns409AndKeepsTheReceiptValidated() throws Exception {
        // Arrange
        var token = adminToken();
        var articleId = createArticle(uniqueName());
        var receiptId = createReceipt(articleId, "12", token);
        move(articleId, "WAREHOUSE", StockMovementType.TRANSFER, "-5");

        // Act + Assert
        mockMvc.perform(reverse(receiptId, "{\"reason\":\"Erreur de quantité\"}", token))
                .andExpect(status().isConflict());
        mockMvc.perform(get(RECEIPTS + "/" + receiptId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDATED"));
        assertThat(balance(articleId, "WAREHOUSE")).isEqualByComparingTo("7");
    }

    @Test
    void reverse_blankReason_returns422() throws Exception {
        var token = adminToken();
        var receiptId = createReceipt(createArticle(uniqueName()), "3", token);

        mockMvc.perform(reverse(receiptId, "{\"reason\":\"  \"}", token))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void getReceipts_afterReceipt_listsItFirstAndUnknownIdReturns404() throws Exception {
        // Arrange
        var token = adminToken();
        var receiptId = createReceipt(createArticle(uniqueName()), "3", token);

        // Act + Assert
        mockMvc.perform(get(RECEIPTS).param("size", "1").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(receiptId.toString()));
        mockMvc.perform(get(RECEIPTS + "/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(RECEIPTS).param("size", "101").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void receive_withoutToken_returns401() throws Exception {
        mockMvc.perform(post(RECEIPTS).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void receive_withSellerRole_returns403() throws Exception {
        var body = receiptBody(UUID.randomUUID(), UUID.randomUUID(), "1");

        mockMvc.perform(receive(body, tokenOf(RoleName.SELLER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void reverse_withDeliveryDriverRole_returns403() throws Exception {
        mockMvc.perform(reverse(UUID.randomUUID(), "{\"reason\":\"x\"}", tokenOf(RoleName.DELIVERY_DRIVER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteReceipt_isNotExposed() throws Exception {
        var auth = bearer(adminToken());

        mockMvc.perform(delete(RECEIPTS + "/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().is4xxClientError());
    }

    private UUID createSupplier(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/suppliers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"type\":\"LOCAL\"}".formatted(uniqueName())))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private UUID createReceipt(UUID articleId, String quantity, String token) throws Exception {
        var result = mockMvc.perform(receive(receiptBody(createSupplier(token), articleId, quantity), token))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private UUID generateRestockOrder(UUID articleId, String token) throws Exception {
        var result = mockMvc.perform(post(STOCKS + "/restock-orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"articleIds\":[\"%s\"]}".formatted(articleId)))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private static MockHttpServletRequestBuilder receive(String body, String token) {
        return post(RECEIPTS)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private static MockHttpServletRequestBuilder reverse(UUID receiptId, String body, String token) {
        return post(RECEIPTS + "/" + receiptId + "/reverse")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private static String receiptBody(UUID supplierId, UUID articleId, String quantity) {
        return """
                {"supplierId":"%s","lines":[{"articleId":"%s","quantity":%s,"unitCost":3120.5}]}
                """.formatted(supplierId, articleId, quantity);
    }

    private static UUID idOf(MvcResult result) throws Exception {
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    private BigDecimal balance(UUID articleId, String locationCode) {
        return jdbcTemplate.queryForObject(
                "select coalesce(sum(quantity), 0) from stock_movements where article_id = ? and location_id = ?",
                BigDecimal.class,
                articleId,
                location(locationCode));
    }

    private Long movements(UUID receiptId, StockMovementType type) {
        return jdbcTemplate.queryForObject(
                "select count(*) from stock_movements where reference_id = ? and type = ?",
                Long.class,
                receiptId,
                type.name());
    }

    private Long traces(UUID receiptId, String action) {
        return jdbcTemplate.queryForObject(
                "select count(*) from journal_entries where resource_type = 'SUPPLIER_RECEIPT' and resource_id = ?"
                        + " and action = ?",
                Long.class,
                receiptId,
                action);
    }
}
