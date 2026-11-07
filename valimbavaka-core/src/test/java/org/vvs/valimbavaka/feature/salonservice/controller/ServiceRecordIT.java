package org.vvs.valimbavaka.feature.salonservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.salonservice.dto.SalonConsumptionRDto;
import org.vvs.valimbavaka.feature.salonservice.dto.ServiceRecordRDto;
import org.vvs.valimbavaka.feature.salonservice.service.ServiceRecordService;
import org.vvs.valimbavaka.feature.user.domain.RoleName;

/** SRV-02: recording a performed service deducts the salon stock of the consumed products and traces the journal. */
class ServiceRecordIT extends ServiceRecordApiTestSupport {

    @Autowired
    private ServiceRecordService serviceRecordService;

    @Test
    void recordService_withoutToken_returns401() throws Exception {
        mockMvc.perform(postJson(RECORDS, body(UUID.randomUUID(), "null")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void recordService_withDeliveryDriverRole_returns403() throws Exception {
        // Arrange
        var token = tokenOf(createUser(RoleName.DELIVERY_DRIVER));

        // Act + Assert
        mockMvc.perform(postJson(RECORDS, body(UUID.randomUUID(), "null"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void recordService_bySellerWithAdjustedProducts_deductsSalonStockAndTracesJournal() throws Exception {
        // Arrange
        var oil = insertArticle("Huile de coco " + UUID.randomUUID());
        receive(oil, SALON, "1");
        receive(oil, "STORE", "5");
        var serviceId = createService(products(oil, "0.015"));
        var token = tokenOf(createUser(RoleName.SELLER));

        // Act
        var result = mockMvc.perform(postJson(RECORDS, body(serviceId, products(oil, "0.020")))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.number").value(startsWith("PRE-")))
                .andExpect(jsonPath("$.price").value(35000.0))
                .andExpect(jsonPath("$.customerName").value("Hanta"))
                .andExpect(jsonPath("$.sellerName").value("Miora Andriamasy"))
                .andExpect(jsonPath("$.locationId").value(location(SALON).toString()))
                .andExpect(jsonPath("$.products[0].articleId").value(oil.toString()))
                .andExpect(jsonPath("$.products[0].quantity").value(0.02))
                .andReturn();

        // Assert
        assertThat(balance(oil, SALON)).isEqualByComparingTo("0.980");
        assertThat(balance(oil, "STORE")).isEqualByComparingTo("5");
        var number = JsonPath.<String>read(result.getResponse().getContentAsString(), "$.number");
        mockMvc.perform(get("/api/v1/journal")
                        .param("search", number)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].action").value("CREATE"))
                .andExpect(jsonPath("$.items[0].resourceType").value("SERVICE_RECORD"));
    }

    @Test
    void recordService_withoutProducts_consumesCatalogRecipe() throws Exception {
        // Arrange
        var oil = insertArticle("Huile " + UUID.randomUUID());
        receive(oil, SALON, "0.5");
        var serviceId = createService(products(oil, "0.015"));

        // Act
        mockMvc.perform(postJson(RECORDS, body(serviceId, "null"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.products[0].quantity").value(0.015));

        // Assert
        assertThat(balance(oil, SALON)).isEqualByComparingTo("0.485");
    }

    @Test
    void recordService_insufficientSalonStock_returns409AndWritesNothing() throws Exception {
        // Arrange
        var oil = insertArticle("Huile " + UUID.randomUUID());
        receive(oil, "STORE", "5");
        var serviceId = createService(products(oil, "0.015"));

        // Act
        mockMvc.perform(postJson(RECORDS, body(serviceId, "null"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        // Assert
        assertThat(balance(oil, SALON)).isEqualByComparingTo("0");
        var records = jdbcTemplate.queryForObject(
                "select count(*) from service_records where salon_service_id = ?", Integer.class, serviceId);
        assertThat(records).isZero();
    }

    @Test
    void recordService_inactiveService_returns422() throws Exception {
        // Arrange
        var token = adminToken();
        var serviceId = createService("[]");
        mockMvc.perform(patch("/api/v1/salon-services/" + serviceId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        // Act + Assert
        mockMvc.perform(postJson(RECORDS, body(serviceId, "[]")).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void recordService_unknownService_returns404() throws Exception {
        mockMvc.perform(postJson(RECORDS, body(UUID.randomUUID(), "[]"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void recordService_zeroQuantity_returns422() throws Exception {
        // Arrange
        var oil = insertArticle("Huile " + UUID.randomUUID());
        var serviceId = createService("[]");

        // Act + Assert
        mockMvc.perform(postJson(RECORDS, body(serviceId, products(oil, "0")))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void recordService_forbiddenFieldsInBody_areIgnored() throws Exception {
        // Arrange
        var serviceId = createService("[]");
        var email = createUser(RoleName.SELLER);
        var forcedId = UUID.randomUUID();
        var json = """
                {"id":"%s","number":"PRE-9999","salonServiceId":"%s","price":1,"sellerId":"%s","products":[]}
                """.formatted(forcedId, serviceId, UUID.randomUUID());

        // Act + Assert
        mockMvc.perform(postJson(RECORDS, json).header(HttpHeaders.AUTHORIZATION, bearer(tokenOf(email))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(not(forcedId.toString())))
                .andExpect(jsonPath("$.number").value(not("PRE-9999")))
                .andExpect(jsonPath("$.price").value(35000.0))
                .andExpect(jsonPath("$.sellerId").value(userId(email).toString()));
    }

    @Test
    void getServiceRecordById_unknownId_returns404() throws Exception {
        mockMvc.perform(get(RECORDS + "/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void getServiceRecords_bySeller_returnsPageMostRecentFirst() throws Exception {
        // Arrange
        var token = tokenOf(createUser(RoleName.SELLER));
        var serviceId = createService("[]");
        mockMvc.perform(postJson(RECORDS, body(serviceId, "[]")).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated());

        // Act + Assert
        mockMvc.perform(get(RECORDS).param("size", "1").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].salonServiceId").value(serviceId.toString()));
    }

    @Test
    void getSalonConsumption_twoServicesInPeriod_sumsQuantityPerArticle() throws Exception {
        // Arrange
        var oil = insertArticle("Huile " + UUID.randomUUID());
        receive(oil, SALON, "1");
        var serviceId = createService(products(oil, "0.015"));
        var token = adminToken();
        var from = Instant.now().minusSeconds(1);
        for (var quantity : new String[] {"0.015", "0.010"}) {
            mockMvc.perform(postJson(RECORDS, body(serviceId, products(oil, quantity)))
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isCreated());
        }
        var to = Instant.now().plusSeconds(1);

        // Act
        var consumption = serviceRecordService.getSalonConsumption(from, to);
        var records = serviceRecordService.getServiceRecordsBetween(from, to);

        // Assert
        var totals = consumption.stream()
                .filter(line -> line.articleId().equals(oil))
                .map(SalonConsumptionRDto::quantity)
                .toList();
        assertThat(totals).hasSize(1);
        assertThat(totals.getFirst()).isEqualByComparingTo("0.025");
        var performed = records.stream().map(ServiceRecordRDto::salonServiceId).filter(serviceId::equals).count();
        assertThat(performed).isEqualTo(2);
        assertThat(balance(oil, SALON)).isEqualByComparingTo("0.975");
    }

    private static String products(UUID articleId, String quantity) {
        return "[{\"articleId\":\"%s\",\"quantity\":%s}]".formatted(articleId, quantity);
    }

    private static String body(UUID serviceId, String products) {
        return "{\"salonServiceId\":\"%s\",\"customerName\":\"Hanta\",\"products\":%s}".formatted(serviceId, products);
    }
}
