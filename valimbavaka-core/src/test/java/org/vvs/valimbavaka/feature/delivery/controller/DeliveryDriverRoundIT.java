package org.vvs.valimbavaka.feature.delivery.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

/** LIV-02: the driver reads his round of the day, stop by stop, with what he needs at the door. */
class DeliveryDriverRoundIT extends DeliveryApiTestSupport {

    @Test
    void getOrders_driverRoundOfToday_returnsHisStopsInOrderWithContactAndAmount() throws Exception {
        // Arrange
        var driverId = createDriver();
        var otherDriverId = createDriver();
        var articleId = createArticle(uniqueName());
        var today = LocalDate.now(BUSINESS_ZONE);
        var token = adminToken();
        var first = createdOrderId(token, body(driverId, today, line(articleId, "2")));
        var second = createdOrderId(token, body(driverId, today, line(articleId, "1")));
        createdOrderId(token, body(driverId, tomorrow(), line(articleId, "1")));
        createdOrderId(token, body(otherDriverId, today, line(articleId, "1")));

        // Act
        var round = roundOf(tokenOfDriver(driverId), driverId, today);

        // Assert
        round.andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.items[0].id").value(first.toString()))
                .andExpect(jsonPath("$.items[0].stopNumber").value(1))
                .andExpect(jsonPath("$.items[0].status").value("TO_PREPARE"))
                .andExpect(jsonPath("$.items[0].customerName").value("Rakoto Be"))
                .andExpect(jsonPath("$.items[0].address").value("Lot II J 45, rue Pasteur"))
                .andExpect(jsonPath("$.items[0].district").value("Ivandry"))
                .andExpect(jsonPath("$.items[0].customerPhone").value("034 12 345 67"))
                .andExpect(jsonPath("$.items[0].amount").value(17000.0))
                .andExpect(jsonPath("$.items[1].id").value(second.toString()))
                .andExpect(jsonPath("$.items[1].stopNumber").value(2))
                .andExpect(jsonPath("$.items[1].amount").value(8500.0));
    }

    @Test
    void getOrders_driverWithoutStopToday_returnsAnEmptyRound() throws Exception {
        // Arrange
        var driverId = createDriver();
        var today = LocalDate.now(BUSINESS_ZONE);

        // Act
        var round = roundOf(tokenOfDriver(driverId), driverId, today);

        // Assert
        round.andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void getOrders_driverRoundWithoutToken_returns401() throws Exception {
        mockMvc.perform(get(DELIVERIES).param("date", LocalDate.now(BUSINESS_ZONE).toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getOrders_driverRoundSortedOutsideTheWhitelist_stillReturnsHisStops() throws Exception {
        // Arrange
        var driverId = createDriver();
        var today = LocalDate.now(BUSINESS_ZONE);
        createdOrderId(adminToken(), body(driverId, today, line(createArticle(uniqueName()), "1")));

        // Act + Assert
        mockMvc.perform(get(DELIVERIES)
                        .param("driverId", driverId.toString())
                        .param("date", today.toString())
                        .param("sortBy", "customerPhone")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOfDriver(driverId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    /** Request of the round page: own driver id, the day, one page of 100 stops in stop order. */
    private ResultActions roundOf(String token, UUID driverId, LocalDate day) throws Exception {
        return mockMvc.perform(get(DELIVERIES)
                .param("driverId", driverId.toString())
                .param("date", day.toString())
                .param("page", "1")
                .param("size", "100")
                .param("sortBy", "stopNumber")
                .param("sortDirection", "ASC")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }
}
