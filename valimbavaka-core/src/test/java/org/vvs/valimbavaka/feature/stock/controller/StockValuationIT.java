package org.vvs.valimbavaka.feature.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.user.domain.RoleName;

/**
 * STK-03: quantity and value at cost of the stock per location, reserved to stocks.value (administrator). The
 * database is shared with the other integration tests, so the assertions compare the valuation before and after.
 */
class StockValuationIT extends StockApiTestSupport {

    private static final String VALUATION = STOCKS + "/valuation";

    @Test
    void getValuation_afterReceiptAndTransfer_addsQuantityAndValueAtCostToEachLocation() throws Exception {
        // Arrange
        var articleId = createArticle(uniqueName(), "3120.5");
        var before = valuation();
        move(articleId, "WAREHOUSE", StockMovementType.RECEIPT, "10");
        move(articleId, "WAREHOUSE", StockMovementType.TRANSFER, "-4");
        move(articleId, "STORE", StockMovementType.TRANSFER, "4");

        // Act
        var after = valuation();

        // Assert
        assertThat(delta(before, after, "WAREHOUSE", "quantity")).isEqualByComparingTo("6");
        assertThat(delta(before, after, "WAREHOUSE", "value")).isEqualByComparingTo("18723.00");
        assertThat(delta(before, after, "STORE", "quantity")).isEqualByComparingTo("4");
        assertThat(delta(before, after, "STORE", "value")).isEqualByComparingTo("12482.00");
        assertThat(total(after, "value").subtract(total(before, "value"))).isEqualByComparingTo("31205.00");
    }

    @Test
    void getValuation_articleWithoutCost_isCountedInQuantityButNotInValue() throws Exception {
        // Arrange
        var articleId = createArticle(uniqueName(), "null");
        var before = valuation();
        move(articleId, "TRADE_SHOW", StockMovementType.RECEIPT, "7");

        // Act
        var after = valuation();

        // Assert
        assertThat(delta(before, after, "TRADE_SHOW", "quantity")).isEqualByComparingTo("7");
        assertThat(delta(before, after, "TRADE_SHOW", "unvaluedQuantity")).isEqualByComparingTo("7");
        assertThat(delta(before, after, "TRADE_SHOW", "value")).isEqualByComparingTo("0");
    }

    @Test
    void getValuation_inactiveArticle_isStillValued() throws Exception {
        // Arrange
        var articleId = createArticle(uniqueName(), "1000");
        var before = valuation();
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "3");
        mockMvc.perform(patch("/api/v1/articles/" + articleId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk());

        // Act
        var after = valuation();

        // Assert
        assertThat(delta(before, after, "DELIVERY", "quantity")).isEqualByComparingTo("3");
        assertThat(delta(before, after, "DELIVERY", "value")).isEqualByComparingTo("3000.00");
    }

    @Test
    void getValuation_returnsEveryActiveLocation() throws Exception {
        mockMvc.perform(get(VALUATION).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locations.length()").value(4))
                .andExpect(jsonPath("$.locations[?(@.code=='DELIVERY')].label").exists());
    }

    @Test
    void getValuation_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(VALUATION))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getValuation_withSellerRole_returns403() throws Exception {
        // Arrange
        var token = tokenOf(RoleName.SELLER);

        // Act + Assert
        mockMvc.perform(get(VALUATION).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getValuation_withDeliveryDriverRole_returns403() throws Exception {
        // Arrange
        var token = tokenOf(RoleName.DELIVERY_DRIVER);

        // Act + Assert
        mockMvc.perform(get(VALUATION).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    private String valuation() throws Exception {
        return mockMvc.perform(get(VALUATION).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private static BigDecimal delta(String before, String after, String code, String field) {
        return field(after, code, field).subtract(field(before, code, field));
    }

    private static BigDecimal field(String json, String code, String field) {
        List<Map<String, Object>> locations = JsonPath.read(json, "$.locations[?(@.code=='" + code + "')]");
        return new BigDecimal(locations.getFirst().get(field).toString());
    }

    private static BigDecimal total(String json, String field) {
        return new BigDecimal(JsonPath.read(json, "$." + field).toString());
    }
}
