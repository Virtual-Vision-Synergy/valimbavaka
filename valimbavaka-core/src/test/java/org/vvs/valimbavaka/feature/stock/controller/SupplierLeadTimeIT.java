package org.vvs.valimbavaka.feature.stock.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

/**
 * FRS-04: each supplier carries a restocking lead time, defaulted from its type (LOCAL 7 days, FOREIGN 30 days), and
 * the threshold alerts show it with the expected delivery date.
 */
class SupplierLeadTimeIT extends StockApiTestSupport {

    private static final String SUPPLIERS = "/api/v1/suppliers";
    private static final String ALERTS = STOCKS + "/alerts";

    @Test
    void createSupplier_foreignWithoutLeadTime_returnsThirtyDays() throws Exception {
        postSupplier("{\"name\":\"%s\",\"type\":\"FOREIGN\"}".formatted(uniqueName()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.leadTimeDays").value(30));
    }

    @Test
    void createSupplier_localWithoutLeadTime_returnsSevenDays() throws Exception {
        postSupplier("{\"name\":\"%s\",\"type\":\"LOCAL\"}".formatted(uniqueName()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.leadTimeDays").value(7));
    }

    @ParameterizedTest(name = "délai {0} -> 201")
    @ValueSource(ints = {0, 365})
    void createSupplier_leadTimeOnTheBounds_isAccepted(int leadTimeDays) throws Exception {
        postSupplier(supplierBody(uniqueName(), "LOCAL", leadTimeDays))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.leadTimeDays").value(leadTimeDays));
    }

    @ParameterizedTest(name = "délai {0} -> 422")
    @ValueSource(ints = {-1, 366})
    void createSupplier_leadTimeOutOfBounds_returns422(int leadTimeDays) throws Exception {
        postSupplier(supplierBody(uniqueName(), "LOCAL", leadTimeDays))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void getAlerts_articleOfAForeignSupplier_returnsLeadTimeAndExpectedDeliveryDate() throws Exception {
        // Arrange
        var supplierName = uniqueName();
        var supplierId = createSupplier(supplierBody(supplierName, "FOREIGN", 45));
        var articleName = uniqueName();
        createArticleOf(articleName, supplierId);
        var expected = LocalDate.now(ZoneId.of("Indian/Antananarivo")).plusDays(45);

        // Act + Assert
        mockMvc.perform(get(ALERTS)
                        .param("search", articleName)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].supplierId").value(supplierId.toString()))
                .andExpect(jsonPath("$.items[0].supplierName").value(supplierName))
                .andExpect(jsonPath("$.items[0].leadTimeDays").value(45))
                .andExpect(jsonPath("$.items[0].expectedDeliveryDate").value(expected.toString()));
    }

    @Test
    void getAlerts_articleWithoutSupplier_returnsNullLeadTime() throws Exception {
        // Arrange
        var articleName = uniqueName();
        createArticle(articleName);

        // Act + Assert
        mockMvc.perform(get(ALERTS)
                        .param("search", articleName)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].supplierId").doesNotExist())
                .andExpect(jsonPath("$.items[0].leadTimeDays").doesNotExist())
                .andExpect(jsonPath("$.items[0].expectedDeliveryDate").doesNotExist());
    }

    private static String supplierBody(String name, String type, int leadTimeDays) {
        return "{\"name\":\"%s\",\"type\":\"%s\",\"leadTimeDays\":%d}".formatted(name, type, leadTimeDays);
    }

    private ResultActions postSupplier(String body) throws Exception {
        return mockMvc.perform(post(SUPPLIERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())));
    }

    private UUID createSupplier(String body) throws Exception {
        MvcResult result = postSupplier(body).andExpect(status().isCreated()).andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    /** Never moved article with a threshold of 40: always an alert. */
    private void createArticleOf(String name, UUID supplierId) throws Exception {
        var rangeId = jdbcTemplate.queryForObject("select id from product_ranges limit 1", UUID.class);
        var body = """
                {"name":"%s","type":"FINISHED","unit":"PIECE","rangeId":"%s","cost":3120.5,"price":8500,
                 "threshold":40,"supplierId":"%s"}
                """.formatted(name, rangeId, supplierId);
        mockMvc.perform(post("/api/v1/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isCreated());
    }
}
