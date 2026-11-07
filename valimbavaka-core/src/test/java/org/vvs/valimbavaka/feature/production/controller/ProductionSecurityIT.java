package org.vvs.valimbavaka.feature.production.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import org.vvs.valimbavaka.feature.user.domain.RoleName;

/**
 * PRD-01: production.read and production.create for administrator and seller, never for the delivery driver.
 * PRD-02: production.complete for the administrator only.
 */
class ProductionSecurityIT extends ProductionApiTestSupport {

    @Test
    void createBatch_withoutToken_returns401() throws Exception {
        mockMvc.perform(postJson(BATCHES, batchBody(UUID.randomUUID(), "1", UUID.randomUUID(), "")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getBatches_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(BATCHES))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getOperators_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(OPERATORS))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void createBatch_withDeliveryDriverRole_returns403() throws Exception {
        // Arrange
        var token = tokenOf(createUser(RoleName.DELIVERY_DRIVER));
        var body = batchBody(UUID.randomUUID(), "1", UUID.randomUUID(), input(UUID.randomUUID(), "1"));

        // Act + Assert
        mockMvc.perform(postJson(BATCHES, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getBatches_withDeliveryDriverRole_returns403() throws Exception {
        // Arrange
        var token = tokenOf(createUser(RoleName.DELIVERY_DRIVER));

        // Act + Assert
        mockMvc.perform(get(BATCHES).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getOperators_withDeliveryDriverRole_returns403() throws Exception {
        // Arrange
        var token = tokenOf(createUser(RoleName.DELIVERY_DRIVER));

        // Act + Assert
        mockMvc.perform(get(OPERATORS).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getBatches_withSellerRole_returns200() throws Exception {
        // Arrange
        var token = tokenOf(createUser(RoleName.SELLER));

        // Act + Assert
        mockMvc.perform(get(BATCHES).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void completeBatch_withoutToken_returns401() throws Exception {
        mockMvc.perform(post(BATCHES + "/" + UUID.randomUUID() + "/completion"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void completeBatch_withSellerRole_returns403() throws Exception {
        // Arrange
        var token = tokenOf(createUser(RoleName.SELLER));

        // Act + Assert
        mockMvc.perform(post(BATCHES + "/" + UUID.randomUUID() + "/completion")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void createCompletedBatch_withSellerRole_returns403() throws Exception {
        // Arrange
        var token = tokenOf(createUser(RoleName.SELLER));
        var body = batchBody(UUID.randomUUID(), "1", UUID.randomUUID(), input(UUID.randomUUID(), "1"));

        // Act + Assert
        mockMvc.perform(postJson("/api/v1/production/completed-batches", body)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void deleteBatch_isNotExposed() throws Exception {
        var auth = bearer(adminToken());

        mockMvc.perform(delete(BATCHES + "/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().is4xxClientError());
    }
}
