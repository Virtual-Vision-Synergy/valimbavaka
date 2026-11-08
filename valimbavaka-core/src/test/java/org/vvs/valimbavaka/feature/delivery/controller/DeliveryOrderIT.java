package org.vvs.valimbavaka.feature.delivery.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
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

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.user.domain.RoleName;

/** LIV-01: an order to deliver is created by a seller and added to the round of its assigned driver. */
class DeliveryOrderIT extends DeliveryApiTestSupport {

    @Test
    void createOrder_bySeller_returns201ToPrepareAndAddsItToTheRoundOfTheDriver() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        var day = tomorrow();

        // Act
        var content = createOrder(tokenOf(RoleName.SELLER), body(driverId, day, line(articleId, "2")))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.number").value(matchesPattern("LIV-\\d{4,}")))
                .andExpect(jsonPath("$.status").value("TO_PREPARE"))
                .andExpect(jsonPath("$.driverId").value(driverId.toString()))
                .andExpect(jsonPath("$.scheduledDate").value(day.toString()))
                .andExpect(jsonPath("$.stopNumber").value(1))
                .andExpect(jsonPath("$.customerName").value("Rakoto Be"))
                .andExpect(jsonPath("$.lines[0].unitPrice").value(8500.0))
                .andExpect(jsonPath("$.amount").value(17000.0))
                .andExpect(jsonPath("$.createdBy").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert
        var orderId = JsonPath.<String>read(content, "$.id");
        mockMvc.perform(get(DELIVERIES)
                        .param("driverId", driverId.toString())
                        .param("date", day.toString())
                        .param("sortBy", "stopNumber")
                        .param("sortDirection", "ASC")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].id").value(orderId));
    }

    @Test
    void createOrder_secondOrderOfTheSameRound_isAppendedAtTheNextStop() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        var token = adminToken();
        createdOrderId(token, body(driverId, tomorrow(), line(articleId, "1")));

        // Act + Assert
        createOrder(token, body(driverId, tomorrow(), line(articleId, "1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stopNumber").value(2));
        createOrder(token, body(driverId, tomorrow().plusDays(1), line(articleId, "1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stopNumber").value(1));
    }

    @Test
    void createOrder_validOrder_isTracedInTheJournal() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());

        // Act
        var orderId = createdOrderId(adminToken(), body(driverId, tomorrow(), line(articleId, "1.5")));

        // Assert
        var description = jdbcTemplate.queryForObject(
                "select description from journal_entries where resource_type = 'DELIVERY_ORDER' and resource_id = ? "
                        + "and action = 'CREATE'",
                String.class,
                orderId);
        assertThat(description)
                .startsWith("Création de la commande à livrer LIV-")
                .contains("1 article(s), 12750.00 Ar")
                .contains("(arrêt 1)");
    }

    @Test
    void createOrder_withoutToken_returns401() throws Exception {
        var body = body(UUID.randomUUID(), tomorrow(), line(UUID.randomUUID(), "1"));
        mockMvc.perform(post(DELIVERIES).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void createOrder_withDeliveryDriverRole_returns403() throws Exception {
        var driverId = createDriver();
        createOrder(tokenOfDriver(driverId), body(driverId, tomorrow(), line(UUID.randomUUID(), "1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void createOrder_massAssignmentOfServerFields_isIgnored() throws Exception {
        // Arrange
        var driverId = createDriver();
        var forcedId = UUID.randomUUID();
        var body = """
                {"id":"%s","number":"LIV-9999","status":"DELIVERED","stopNumber":99,"amount":1,
                 "customerName":"Rakoto Be","customerPhone":"034 12 345 67","address":"Rue Pasteur",
                 "driverId":"%s","scheduledDate":"%s",
                 "lines":[{"articleId":"%s","quantity":1,"unitPrice":1}]}
                """.formatted(forcedId, driverId, tomorrow(), createArticle(uniqueName()));

        // Act + Assert
        createOrder(adminToken(), body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(not(forcedId.toString())))
                .andExpect(jsonPath("$.number").value(not("LIV-9999")))
                .andExpect(jsonPath("$.status").value("TO_PREPARE"))
                .andExpect(jsonPath("$.stopNumber").value(1))
                .andExpect(jsonPath("$.lines[0].unitPrice").value(8500.0))
                .andExpect(jsonPath("$.amount").value(8500.0));
    }

    @Test
    void createOrder_yesterday_returns422() throws Exception {
        var body = body(createDriver(), tomorrow().minusDays(2), line(createArticle(uniqueName()), "1"));
        assertValidationFailed(body);
    }

    @Test
    void createOrder_assignedToAnAdministrator_returns422() throws Exception {
        var adminId = jdbcTemplate.queryForObject(
                "select u.id from users u join roles r on r.id = u.role_id where r.name = 'ADMIN' limit 1", UUID.class);
        assertValidationFailed(body(adminId, tomorrow(), line(createArticle(uniqueName()), "1")));
    }

    @Test
    void createOrder_blankCustomerName_returns422() throws Exception {
        var body =
                body(createDriver(), tomorrow(), line(UUID.randomUUID(), "1")).replace("\"Rakoto Be\"", "\"   \"");
        assertValidationFailed(body);
    }

    @Test
    void createOrder_invalidPhone_returns422() throws Exception {
        var body =
                body(createDriver(), tomorrow(), line(UUID.randomUUID(), "1")).replace("034 12 345 67", "appelez-moi");
        assertValidationFailed(body);
    }

    @Test
    void createOrder_noLine_returns422() throws Exception {
        assertValidationFailed(body(createDriver(), tomorrow(), ""));
    }

    @Test
    void createOrder_101Lines_returns422() throws Exception {
        var lines = new StringBuilder(line(UUID.randomUUID(), "1"));
        for (var i = 1; i < 101; i++) {
            lines.append(',').append(line(UUID.randomUUID(), "1"));
        }
        assertValidationFailed(body(createDriver(), tomorrow(), lines.toString()));
    }

    @Test
    void createOrder_zeroQuantity_returns422() throws Exception {
        assertValidationFailed(body(createDriver(), tomorrow(), line(createArticle(uniqueName()), "0")));
    }

    @Test
    void createOrder_sameArticleTwice_returns422() throws Exception {
        var articleId = createArticle(uniqueName());
        assertValidationFailed(body(createDriver(), tomorrow(), line(articleId, "1") + "," + line(articleId, "2")));
    }

    @Test
    void createOrder_unknownArticle_returns404AndWritesNothing() throws Exception {
        // Arrange
        var driverId = createDriver();

        // Act + Assert
        createOrder(adminToken(), body(driverId, tomorrow(), line(UUID.randomUUID(), "1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        var orders = jdbcTemplate.queryForObject(
                "select count(*) from delivery_orders where driver_id = ?", Integer.class, driverId);
        assertThat(orders).isZero();
    }

    @Test
    void getOrder_unknownId_returns404() throws Exception {
        var url = DELIVERIES + "/" + UUID.randomUUID();
        mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void getOrder_driverReadingHisOwnOrAnotherDriversOrder_returns200Or404() throws Exception {
        // Arrange
        var driverId = createDriver();
        var otherDriverId = createDriver();
        var articleId = createArticle(uniqueName());
        var ownOrder = createdOrderId(adminToken(), body(driverId, tomorrow(), line(articleId, "1")));
        var otherOrder = createdOrderId(adminToken(), body(otherDriverId, tomorrow(), line(articleId, "1")));
        var token = tokenOfDriver(driverId);

        // Act + Assert
        mockMvc.perform(get(DELIVERIES + "/" + ownOrder).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ownOrder.toString()));
        mockMvc.perform(get(DELIVERIES + "/" + otherOrder).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrders_driverAskingForAnotherRound_onlyGetsHisOwnOrders() throws Exception {
        // Arrange
        var driverId = createDriver();
        var otherDriverId = createDriver();
        var articleId = createArticle(uniqueName());
        var ownOrder = createdOrderId(adminToken(), body(driverId, tomorrow(), line(articleId, "1")));
        createdOrderId(adminToken(), body(otherDriverId, tomorrow(), line(articleId, "1")));

        // Act + Assert
        mockMvc.perform(get(DELIVERIES)
                        .param("driverId", otherDriverId.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOfDriver(driverId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].id").value(ownOrder.toString()));
    }

    @Test
    void getOrders_withSellerRoleAndStatusFilter_returns200() throws Exception {
        mockMvc.perform(get(DELIVERIES)
                        .param("status", "TO_PREPARE")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOf(RoleName.SELLER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void getOrders_sizeAbove100_returns422() throws Exception {
        mockMvc.perform(get(DELIVERIES).param("size", "101").header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void getDrivers_bySeller_listsTheActiveDriversWithoutEmail() throws Exception {
        // Arrange
        var driverId = createDriver();

        // Act + Assert
        mockMvc.perform(get(DELIVERIES + "/drivers")
                        .param("size", "100")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOf(RoleName.SELLER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", hasItem(driverId.toString())))
                .andExpect(jsonPath("$.items[0].email").doesNotExist());
    }

    @Test
    void getDrivers_withDeliveryDriverRole_returns403() throws Exception {
        var token = tokenOfDriver(createDriver());
        mockMvc.perform(get(DELIVERIES + "/drivers").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    private void assertValidationFailed(String body) throws Exception {
        createOrder(adminToken(), body)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
