package org.vvs.valimbavaka.feature.stock.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;

/** STK-04: articles under their minimum threshold with stock, threshold, shortage and suggested quantity. */
class StockAlertIT extends StockApiTestSupport {

    private static final String ALERTS = STOCKS + "/alerts";

    @Test
    void getAlerts_stockSpreadOverLocationsUnderThreshold_returnsShortageAndSuggestion() throws Exception {
        // Arrange
        var name = uniqueName();
        var articleId = createArticle(name);
        move(articleId, "WAREHOUSE", StockMovementType.RECEIPT, "10");
        move(articleId, "STORE", StockMovementType.RECEIPT, "2.5");

        // Act + Assert
        mockMvc.perform(get(ALERTS).param("search", name).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].articleId").value(articleId.toString()))
                .andExpect(jsonPath("$.items[0].unit").value("PIECE"))
                .andExpect(jsonPath("$.items[0].quantity").value(12.5))
                .andExpect(jsonPath("$.items[0].threshold").value(40.0))
                .andExpect(jsonPath("$.items[0].shortage").value(27.5))
                .andExpect(jsonPath("$.items[0].suggestedQuantity").value(30.0));
    }

    @Test
    void getAlerts_stockEqualToThreshold_isNotAnAlert() throws Exception {
        // Arrange
        var name = uniqueName();
        var articleId = createArticle(name);
        move(articleId, "WAREHOUSE", StockMovementType.RECEIPT, "40");

        // Act + Assert
        mockMvc.perform(get(ALERTS).param("search", name).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void getAlerts_thresholdZero_isNeverAnAlert() throws Exception {
        // Arrange
        var name = uniqueName();
        createArticle(name, "3120.5", "0");

        // Act + Assert
        mockMvc.perform(get(ALERTS).param("search", name).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0));
    }

    @Test
    void getAlerts_outOfStockArticle_returnsTheWholeThresholdAsShortage() throws Exception {
        // Arrange
        var name = uniqueName();
        createArticle(name, "3120.5", "15");

        // Act + Assert
        mockMvc.perform(get(ALERTS).param("search", name).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(0.0))
                .andExpect(jsonPath("$.items[0].shortage").value(15.0))
                .andExpect(jsonPath("$.items[0].suggestedQuantity").value(20.0));
    }

    @Test
    void getAlerts_inactiveArticle_isLeftOut() throws Exception {
        // Arrange
        var name = uniqueName();
        var articleId = createArticle(name);
        mockMvc.perform(patch("/api/v1/articles/" + articleId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk());

        // Act + Assert
        mockMvc.perform(get(ALERTS).param("search", name).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0));
    }

    @Test
    void getAlerts_sizeAbove100_returns422() throws Exception {
        mockMvc.perform(get(ALERTS).param("size", "101").header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void getAlerts_pageZero_returns422() throws Exception {
        mockMvc.perform(get(ALERTS).param("page", "0").header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
