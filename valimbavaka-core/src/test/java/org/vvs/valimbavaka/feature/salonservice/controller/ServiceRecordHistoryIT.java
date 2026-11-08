package org.vvs.valimbavaka.feature.salonservice.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import org.vvs.valimbavaka.feature.user.domain.RoleName;

/** SRV-03: day history of the connected account, with time, service, customer and consumed products. */
class ServiceRecordHistoryIT extends ServiceRecordApiTestSupport {

    private static final String MINE = RECORDS + "/mine";

    @Test
    void getMyServiceRecords_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(MINE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getMyServiceRecords_withDeliveryDriverRole_returns403() throws Exception {
        // Arrange
        var token = tokenOf(createUser(RoleName.DELIVERY_DRIVER));

        // Act + Assert
        mockMvc.perform(get(MINE).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getMyServiceRecords_twoSellers_returnsOnlyOwnRecordsOfTodayMostRecentFirst() throws Exception {
        // Arrange
        var oil = insertArticle("Huile de coco " + UUID.randomUUID());
        receive(oil, SALON, "1");
        var serviceId = createService(products(oil, "0.015"));
        var miora = tokenOf(createUser(RoleName.SELLER));
        var other = tokenOf(createUser(RoleName.SELLER));
        record(miora, body(serviceId, "Hanta", "[]"));
        record(miora, body(serviceId, "Voahirana", products(oil, "0.020")));
        record(other, body(serviceId, "Lova", "[]"));

        // Act + Assert
        mockMvc.perform(get(MINE).header(HttpHeaders.AUTHORIZATION, bearer(miora)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.items[0].customerName").value("Voahirana"))
                .andExpect(jsonPath("$.items[0].serviceName").exists())
                .andExpect(jsonPath("$.items[0].performedAt").exists())
                .andExpect(jsonPath("$.items[0].products[0].articleId").value(oil.toString()))
                .andExpect(jsonPath("$.items[0].products[0].quantity").value(0.02))
                .andExpect(jsonPath("$.items[1].customerName").value("Hanta"))
                .andExpect(jsonPath("$.items[1].products").isEmpty());
    }

    @Test
    void getMyServiceRecords_anotherDay_returnsEmptyPage() throws Exception {
        // Arrange
        var token = tokenOf(createUser(RoleName.SELLER));
        record(token, body(createService("[]"), "Hanta", "[]"));

        // Act + Assert
        mockMvc.perform(get(MINE).param("date", "2000-01-01").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void getMyServiceRecords_malformedDate_returns400() throws Exception {
        // Arrange
        var token = tokenOf(createUser(RoleName.SELLER));

        // Act + Assert
        mockMvc.perform(get(MINE).param("date", "08/11/2026").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    private void record(String token, String body) throws Exception {
        mockMvc.perform(postJson(RECORDS, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated());
    }

    private static String products(UUID articleId, String quantity) {
        return "[{\"articleId\":\"%s\",\"quantity\":%s}]".formatted(articleId, quantity);
    }

    private static String body(UUID serviceId, String customer, String products) {
        return "{\"salonServiceId\":\"%s\",\"customerName\":\"%s\",\"products\":%s}"
                .formatted(serviceId, customer, products);
    }
}
