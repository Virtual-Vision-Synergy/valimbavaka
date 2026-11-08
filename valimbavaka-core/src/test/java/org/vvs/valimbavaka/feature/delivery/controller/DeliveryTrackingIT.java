package org.vvs.valimbavaka.feature.delivery.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import org.vvs.valimbavaka.feature.user.domain.RoleName;

/** LIV-06: the orders are followed by status and day, and each order shows its history (journal lines). */
class DeliveryTrackingIT extends DeliveryApiTestSupport {

    private static final List<String> STATUSES = List.of("TO_PREPARE", "ON_ROUND", "DELIVERED", "FAILED", "CANCELLED");

    @Test
    void getOrders_filteredByEachStatus_returnsOnlyTheOrderInThatStatus() throws Exception {
        // Arrange : one order per status in the round of a new driver
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        var orders = new UUID[STATUSES.size()];
        for (var i = 0; i < STATUSES.size(); i++) {
            orders[i] = createdOrderId(adminToken(), body(driverId, tomorrow(), line(articleId, "1")));
            setStatus(orders[i], STATUSES.get(i));
        }

        // Act + Assert
        for (var i = 0; i < STATUSES.size(); i++) {
            mockMvc.perform(get(DELIVERIES)
                            .param("driverId", driverId.toString())
                            .param("status", STATUSES.get(i))
                            .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCount").value(1))
                    .andExpect(jsonPath("$.items[0].id").value(orders[i].toString()))
                    .andExpect(jsonPath("$.items[0].status").value(STATUSES.get(i)));
        }
        mockMvc.perform(get(DELIVERIES)
                        .param("driverId", driverId.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(jsonPath("$.totalCount").value(STATUSES.size()));
    }

    @Test
    void getOrders_pastDayFilteredByStatus_returnsOnlyTheOrdersOfThatDay() throws Exception {
        // Arrange : two delivered orders, one of them yesterday
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        var yesterdayOrder = createdOrderId(adminToken(), body(driverId, tomorrow(), line(articleId, "1")));
        var tomorrowOrder = createdOrderId(adminToken(), body(driverId, tomorrow(), line(articleId, "2")));
        var yesterday = tomorrow().minusDays(2);
        jdbcTemplate.update("update delivery_orders set scheduled_date = ? where id = ?", yesterday, yesterdayOrder);
        setStatus(yesterdayOrder, "DELIVERED");
        setStatus(tomorrowOrder, "DELIVERED");

        // Act + Assert
        mockMvc.perform(get(DELIVERIES)
                        .param("driverId", driverId.toString())
                        .param("date", yesterday.toString())
                        .param("status", "DELIVERED")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOf(RoleName.SELLER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].id").value(yesterdayOrder.toString()));
    }

    @Test
    void getOrders_unknownStatus_returns400() throws Exception {
        mockMvc.perform(get(DELIVERIES)
                        .param("status", "RETURNED")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void getOrders_driverFilteringByStatus_onlyGetsHisOwnOrders() throws Exception {
        // Arrange
        var driverId = createDriver();
        var otherDriverId = createDriver();
        var articleId = createArticle(uniqueName());
        var ownOrder = createdOrderId(adminToken(), body(driverId, tomorrow(), line(articleId, "1")));
        createdOrderId(adminToken(), body(otherDriverId, tomorrow(), line(articleId, "1")));

        // Act + Assert
        mockMvc.perform(get(DELIVERIES)
                        .param("status", "TO_PREPARE")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOfDriver(driverId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].id").value(ownOrder.toString()));
    }

    @Test
    void getHistory_createdOrder_returnsTheCreationStepWithItsTimeAndAuthor() throws Exception {
        // Arrange
        var articleId = createArticle(uniqueName());
        var orderId = createdOrderId(adminToken(), body(createDriver(), tomorrow(), line(articleId, "2")));

        // Act + Assert
        mockMvc.perform(get(history(orderId)).header(HttpHeaders.AUTHORIZATION, bearer(tokenOf(RoleName.SELLER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].action").value("CREATE"))
                .andExpect(jsonPath("$.items[0].occurredAt").isNotEmpty())
                .andExpect(jsonPath("$.items[0].actorName").isNotEmpty())
                .andExpect(jsonPath("$.items[0].description", containsString(numberOf(orderId))))
                .andExpect(jsonPath("$.items[0].description", not(containsString("Rakoto"))))
                .andExpect(jsonPath("$.items[0].actorId").doesNotExist());
    }

    @Test
    void getHistory_linesOfTheOrderAndOfAnotherResource_listsOnlyTheOrderLinesOldestFirst() throws Exception {
        // Arrange : a later status change of the order and a line of another resource
        var articleId = createArticle(uniqueName());
        var orderId = createdOrderId(adminToken(), body(createDriver(), tomorrow(), line(articleId, "1")));
        insertJournalLine(orderId, "Commande %s en tournée".formatted(numberOf(orderId)));
        insertJournalLine(UUID.randomUUID(), "Commande d'une autre fiche");

        // Act + Assert
        mockMvc.perform(get(history(orderId)).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.items[0].action").value("CREATE"))
                .andExpect(jsonPath("$.items[1].action").value("UPDATE"))
                .andExpect(jsonPath("$.items[1].actorName").value("Système"))
                .andExpect(jsonPath("$.items[1].description", containsString("en tournée")));
    }

    @Test
    void getHistory_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(history(UUID.randomUUID())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getHistory_unknownOrder_returns404() throws Exception {
        mockMvc.perform(get(history(UUID.randomUUID())).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void getHistory_driverReadingHisOwnOrAnotherDriversOrder_returns200Or404() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        var ownOrder = createdOrderId(adminToken(), body(driverId, tomorrow(), line(articleId, "1")));
        var otherOrder = createdOrderId(adminToken(), body(createDriver(), tomorrow(), line(articleId, "1")));
        var token = tokenOfDriver(driverId);

        // Act + Assert
        mockMvc.perform(get(history(ownOrder)).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1));
        mockMvc.perform(get(history(otherOrder)).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getHistory_sizeAbove100_returns422() throws Exception {
        mockMvc.perform(get(history(UUID.randomUUID()))
                        .param("size", "101")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private static String history(UUID orderId) {
        return DELIVERIES + "/" + orderId + "/history";
    }

    private String numberOf(UUID orderId) {
        return jdbcTemplate.queryForObject("select number from delivery_orders where id = ?", String.class, orderId);
    }

    /** The status changes are not exposed yet (LIV-03, LIV-04): the test sets them in the database. */
    private void setStatus(UUID orderId, String status) {
        jdbcTemplate.update("update delivery_orders set status = ? where id = ?", status, orderId);
    }

    /** A journal line one hour after now, as a later transition would write it (the journal accepts inserts only). */
    private void insertJournalLine(UUID resourceId, String description) {
        jdbcTemplate.update("""
                insert into journal_entries (id, occurred_at, actor_id, actor_name, action, resource_type, resource_id,
                                             description)
                values (?, now() + interval '1 hour', null, 'Système', 'UPDATE', 'DELIVERY_ORDER', ?, ?)
                """, UUID.randomUUID(), resourceId, description);
    }
}
