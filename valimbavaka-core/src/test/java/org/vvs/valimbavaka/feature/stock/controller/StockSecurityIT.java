package org.vvs.valimbavaka.feature.stock.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import org.vvs.valimbavaka.feature.user.domain.RoleName;

/** STK-01: the stock is readable with stocks.read (administrator, seller) and never writable through the API. */
class StockSecurityIT extends StockApiTestSupport {

    @Test
    void getStocks_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(STOCKS))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getStock_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(STOCKS + "/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    @Test
    void getStocks_withDeliveryDriverRole_returns403() throws Exception {
        // Arrange
        var token = tokenOf(RoleName.DELIVERY_DRIVER);

        // Act + Assert
        mockMvc.perform(get(STOCKS).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getStocks_withSellerRole_returns200() throws Exception {
        // Arrange
        var token = tokenOf(RoleName.SELLER);

        // Act + Assert
        mockMvc.perform(get(STOCKS).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void getAlerts_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(STOCKS + "/alerts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getAlerts_withDeliveryDriverRole_returns403() throws Exception {
        // Arrange
        var token = tokenOf(RoleName.DELIVERY_DRIVER);

        // Act + Assert
        mockMvc.perform(get(STOCKS + "/alerts").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getAlerts_withSellerRole_returns200() throws Exception {
        // Arrange
        var token = tokenOf(RoleName.SELLER);

        // Act + Assert
        mockMvc.perform(get(STOCKS + "/alerts").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void stocks_writeVerbs_areNotExposed() throws Exception {
        // Arrange
        var auth = bearer(adminToken());
        var id = UUID.randomUUID();

        // Act + Assert
        mockMvc.perform(post(STOCKS)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":10}"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(put(STOCKS + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":10}"))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(patch(STOCKS + "/" + id).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(delete(STOCKS + "/" + id).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().is4xxClientError());
    }
}
