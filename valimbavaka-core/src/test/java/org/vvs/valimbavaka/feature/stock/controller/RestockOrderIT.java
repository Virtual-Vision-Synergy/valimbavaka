package org.vvs.valimbavaka.feature.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.user.domain.RoleName;

/** STK-05: restock order generated from the articles under their threshold, reserved to stocks.restock. */
class RestockOrderIT extends StockApiTestSupport {

    private static final String RESTOCK_ORDERS = STOCKS + "/restock-orders";

    @Test
    void generate_articleUnderThreshold_returns201WithItsLineAndTracesTheJournal() throws Exception {
        // Arrange
        var articleId = createArticle(uniqueName());
        move(articleId, "WAREHOUSE", StockMovementType.RECEIPT, "10");
        move(articleId, "STORE", StockMovementType.RECEIPT, "2.5");

        // Act
        var result = mockMvc.perform(generate(articlesBody(articleId), adminToken()))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.reference").value(matchesPattern("BR-\\d{6}")))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.lines.length()").value(1))
                .andExpect(jsonPath("$.lines[0].lineNumber").value(1))
                .andExpect(jsonPath("$.lines[0].articleId").value(articleId.toString()))
                .andExpect(jsonPath("$.lines[0].unit").value("PIECE"))
                .andExpect(jsonPath("$.lines[0].stockQuantity").value(12.5))
                .andExpect(jsonPath("$.lines[0].threshold").value(40.0))
                .andExpect(jsonPath("$.lines[0].quantity").value(30.0))
                .andExpect(jsonPath("$.lines[0].supplierId").isEmpty())
                .andReturn();

        // Assert
        var id = UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
        var traces = jdbcTemplate.queryForObject(
                "select count(*) from journal_entries where resource_type = 'RESTOCK_ORDER' and resource_id = ?"
                        + " and action = 'CREATE'",
                Long.class,
                id);
        assertThat(traces).isEqualTo(1L);
    }

    @Test
    void generate_articleAtItsThreshold_returns409AndCreatesNoOrder() throws Exception {
        // Arrange
        var articleId = createArticle(uniqueName());
        move(articleId, "WAREHOUSE", StockMovementType.RECEIPT, "40");
        var before = orderCount();

        // Act + Assert
        mockMvc.perform(generate(articlesBody(articleId), adminToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
        assertThat(orderCount()).isEqualTo(before);
    }

    @Test
    void generate_massAssignmentOfIdStatusAndReference_isIgnored() throws Exception {
        // Arrange
        var articleId = createArticle(uniqueName());
        var forcedId = UUID.randomUUID();
        var body = """
                {"id":"%s","status":"RECEIVED","reference":"BR-999999","articleIds":["%s"]}
                """.formatted(forcedId, articleId);

        // Act + Assert
        mockMvc.perform(generate(body, adminToken()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(not(forcedId.toString())))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.reference").value(not("BR-999999")));
    }

    @Test
    void generate_nullArticleId_returns422() throws Exception {
        mockMvc.perform(generate("{\"articleIds\":[null]}", adminToken()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void getRestockOrderById_afterGeneration_returnsTheSameOrder() throws Exception {
        // Arrange
        var token = adminToken();
        var articleId = createArticle(uniqueName());
        var created = mockMvc.perform(generate(articlesBody(articleId), token))
                .andExpect(status().isCreated())
                .andReturn();
        String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        // Act + Assert
        mockMvc.perform(get(RESTOCK_ORDERS + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.lines[0].articleId").value(articleId.toString()))
                .andExpect(jsonPath("$.lines[0].quantity").value(40.0));
        mockMvc.perform(get(RESTOCK_ORDERS).param("size", "1").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(id));
    }

    @Test
    void getRestockOrderById_unknownId_returns404() throws Exception {
        mockMvc.perform(get(RESTOCK_ORDERS + "/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void getRestockOrders_sizeAbove100_returns422() throws Exception {
        var auth = bearer(adminToken());

        mockMvc.perform(get(RESTOCK_ORDERS).param("size", "101").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void generate_withoutToken_returns401() throws Exception {
        mockMvc.perform(post(RESTOCK_ORDERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void generate_withSellerRole_returns403() throws Exception {
        mockMvc.perform(generate("{}", tokenOf(RoleName.SELLER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getRestockOrders_withDeliveryDriverRole_returns403() throws Exception {
        var auth = bearer(tokenOf(RoleName.DELIVERY_DRIVER));

        mockMvc.perform(get(RESTOCK_ORDERS).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isForbidden());
    }

    private static MockHttpServletRequestBuilder generate(String body, String token) {
        return post(RESTOCK_ORDERS)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private static String articlesBody(UUID articleId) {
        return "{\"articleIds\":[\"%s\"]}".formatted(articleId);
    }

    private long orderCount() {
        var count = jdbcTemplate.queryForObject("select count(*) from restock_orders", Long.class);
        return count == null ? 0 : count;
    }
}
