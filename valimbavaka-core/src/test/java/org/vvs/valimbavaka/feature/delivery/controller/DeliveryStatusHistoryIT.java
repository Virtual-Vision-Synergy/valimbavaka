package org.vvs.valimbavaka.feature.delivery.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;

/**
 * LIV-07: every change of status of an order (creation, delivery, failure, undo) is a journal line with its time and
 * its author, and the history of the order lists them in the order they happened.
 */
class DeliveryStatusHistoryIT extends DeliveryApiTestSupport {

    private static final String DRIVER_NAME = "Tojo Nirina";

    /** A delivered order becomes a sale of the open day (LIV-03): the day must be opened. */
    @BeforeEach
    void openToday() throws Exception {
        var result = mockMvc.perform(post("/api/v1/days")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[]}"))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isIn(201, 409);
    }

    @Test
    void getHistory_createdDeliveredThenUndone_listsEachStepWithItsAuthorOldestFirst() throws Exception {
        // Arrange : created by the administrator, delivered by the driver, undone by the administrator
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "2");
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "1")));
        act(orderId, "deliver", tokenOfDriver(driverId));
        act(orderId, "undo", adminToken());

        // Act + Assert
        mockMvc.perform(get(history(orderId)).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.items[0].action").value("CREATE"))
                .andExpect(jsonPath("$.items[0].actorName", not(containsString(DRIVER_NAME))))
                .andExpect(jsonPath("$.items[1].action").value("UPDATE"))
                .andExpect(jsonPath("$.items[1].actorName").value(DRIVER_NAME))
                .andExpect(jsonPath("$.items[1].description", containsString("livrée")))
                .andExpect(jsonPath("$.items[2].action").value("CANCEL"))
                .andExpect(jsonPath("$.items[2].actorName", not(containsString(DRIVER_NAME))))
                .andExpect(jsonPath("$.items[2].description", containsString("de « livrée » à « à préparer »")));
        assertEveryStepHasATimeAndAnAuthorId(orderId, 3);
    }

    @Test
    void getHistory_failedOrder_showsTheFailureStepAttributedToTheDriver() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "1");
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "1")));
        mockMvc.perform(post(DELIVERIES + "/" + orderId + "/failure")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOfDriver(driverId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"ABSENT\"}"))
                .andExpect(status().isOk());

        // Act + Assert : the driver reads the history of his own order
        mockMvc.perform(get(history(orderId)).header(HttpHeaders.AUTHORIZATION, bearer(tokenOfDriver(driverId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.items[1].action").value("UPDATE"))
                .andExpect(jsonPath("$.items[1].actorName").value(DRIVER_NAME))
                .andExpect(jsonPath("$.items[1].occurredAt").isNotEmpty())
                .andExpect(jsonPath("$.items[1].description", containsString("en échec")))
                .andExpect(jsonPath("$.items[1].description", containsString("client absent")));
    }

    @Test
    void getHistory_refusedTransition_addsNoStep() throws Exception {
        // Arrange : an order delivered, then a failure report that is refused (409)
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "1");
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "1")));
        var token = tokenOfDriver(driverId);
        act(orderId, "deliver", token);
        mockMvc.perform(post(DELIVERIES + "/" + orderId + "/failure")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"ABSENT\"}"))
                .andExpect(status().isConflict());

        // Act + Assert
        mockMvc.perform(get(history(orderId)).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2));
    }

    private void assertEveryStepHasATimeAndAnAuthorId(UUID orderId, int expectedSteps) {
        var complete = jdbcTemplate.queryForObject(
                "select count(*) from journal_entries where resource_type = 'DELIVERY_ORDER' and resource_id = ? "
                        + "and occurred_at is not null and actor_id is not null",
                Integer.class,
                orderId);
        assertThat(complete).isEqualTo(expectedSteps);
    }

    private void act(UUID orderId, String action, String token) throws Exception {
        mockMvc.perform(post(DELIVERIES + "/" + orderId + "/" + action)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    private static String history(UUID orderId) {
        return DELIVERIES + "/" + orderId + "/history";
    }

    private static LocalDate today() {
        return LocalDate.now(BUSINESS_ZONE);
    }
}
