package org.vvs.valimbavaka.feature.salonservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.stock.domain.StockMovementCommand;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.stock.service.StockMovementService;
import org.vvs.valimbavaka.feature.user.domain.RoleName;

/**
 * SRV-04: the salon consumption of the services is the « Salon » column of the closing, administrator only. The
 * database is shared with the other test classes: every test works on its own article, and today's day is only
 * opened here, never closed.
 */
class ClosingSalonConsumptionIT extends ServiceRecordApiTestSupport {

    private static final String CLOSING = "/api/v1/days/closing";
    private static final String CLOSING_SALON = CLOSING + "/salon";

    @Autowired
    private StockMovementService movements;

    /** The closing needs today's day opened (JR-01); another test class may have removed it. */
    @BeforeEach
    void openToday() throws Exception {
        var status = mockMvc.perform(postJson("/api/v1/days", "{\"lines\":[]}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andReturn()
                .getResponse()
                .getStatus();
        assertThat(status).isIn(201, 409);
    }

    @Test
    void getSalonConsumption_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(CLOSING_SALON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getSalonConsumption_withDeliveryDriverRole_returns403() throws Exception {
        // Arrange
        var token = tokenOf(createUser(RoleName.DELIVERY_DRIVER));

        // Act + Assert
        mockMvc.perform(get(CLOSING_SALON).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getSalonConsumption_twoServices_consumedEqualsTheSalonColumnOfTheClosing() throws Exception {
        // Arrange: 0.015 + 0.020 of oil consumed by two services (SRV-02)
        var oil = insertArticle("Huile de coco " + UUID.randomUUID());
        receive(oil, SALON, "1");
        var serviceId = createService(products(oil, "0.015"));
        record(serviceId, "[]");
        record(serviceId, products(oil, "0.020"));

        // Act
        var salon = lineOf(salonJson(), oil);
        var closing = JsonPath.<List<Map<String, Object>>>read(
                closingJson(), "$.locations[?(@.code=='TRADE_SHOW')].lines[?(@.articleId=='%s')]".formatted(oil));

        // Assert
        assertThat(number(salon, "consumed")).isEqualTo(0.035);
        assertThat(number(salon, "inClosing")).isEqualTo(0.035);
        assertThat(number(salon, "gap")).isZero();
        assertThat(closing).hasSize(1);
        assertThat(number(closing.getFirst(), "salon")).isEqualTo(number(salon, "consumed"));
        assertThat(balance(oil, SALON)).isEqualByComparingTo("0.965");
    }

    @Test
    void getSalonConsumption_servicesOfToday_areCounted() throws Exception {
        // Arrange
        var before = servicesCount();
        record(createService("[]"), "[]");
        record(createService("[]"), "[]");

        // Act + Assert
        assertThat(servicesCount()).isEqualTo(before + 2);
    }

    @Test
    void getSalonConsumption_movementWithoutService_showsAGapAndDoesNotMatch() throws Exception {
        // Arrange: a salon exit that no service explains
        var gel = insertArticle("Gel " + UUID.randomUUID());
        receive(gel, SALON, "5");
        movements.record(new StockMovementCommand(
                gel, location(SALON), StockMovementType.TRADE_SHOW, new BigDecimal("-2"), null));

        // Act
        var json = salonJson();
        var line = lineOf(json, gel);

        // Assert
        assertThat(number(line, "consumed")).isZero();
        assertThat(number(line, "inClosing")).isEqualTo(2.0);
        assertThat(number(line, "gap")).isEqualTo(2.0);
        assertThat(JsonPath.<Boolean>read(json, "$.matches")).isFalse();
    }

    @Test
    void getSalonConsumption_forbiddenQueryParameters_areIgnored() throws Exception {
        // Act + Assert: the day is always today's, a date parameter changes nothing
        mockMvc.perform(get(CLOSING_SALON)
                        .param("date", "2020-01-01")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessDate").isNotEmpty())
                .andExpect(jsonPath("$.lines").isArray());
    }

    private int servicesCount() throws Exception {
        return JsonPath.read(salonJson(), "$.servicesCount");
    }

    private String salonJson() throws Exception {
        return mockMvc.perform(get(CLOSING_SALON).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String closingJson() throws Exception {
        return mockMvc.perform(get(CLOSING).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private static Map<String, Object> lineOf(String json, UUID articleId) {
        List<Map<String, Object>> lines = JsonPath.read(json, "$.lines[?(@.articleId=='%s')]".formatted(articleId));
        assertThat(lines).hasSize(1);
        return lines.getFirst();
    }

    private static double number(Map<String, Object> line, String field) {
        return ((Number) line.get(field)).doubleValue();
    }

    private void record(UUID serviceId, String products) throws Exception {
        var body = "{\"salonServiceId\":\"%s\",\"products\":%s}".formatted(serviceId, products);
        mockMvc.perform(postJson(RECORDS, body).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isCreated());
    }

    private static String products(UUID articleId, String quantity) {
        return "[{\"articleId\":\"%s\",\"quantity\":%s}]".formatted(articleId, quantity);
    }
}
