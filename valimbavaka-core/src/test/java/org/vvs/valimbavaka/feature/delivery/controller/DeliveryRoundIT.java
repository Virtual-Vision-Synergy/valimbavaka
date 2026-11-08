package org.vvs.valimbavaka.feature.delivery.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;

/** LIV-01: positions in a round under concurrent entries, and offline replays of the same order. */
class DeliveryRoundIT extends DeliveryApiTestSupport {

    @Test
    void createOrder_fourOrdersOfTheSameRoundAtTheSameTime_getDistinctStops() throws Exception {
        // Arrange
        var driverId = createDriver();
        var body = body(driverId, tomorrow(), line(createArticle(uniqueName()), "1"));
        var token = adminToken();
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(4);
        var tasks = new ArrayList<Callable<Integer>>();
        for (var i = 0; i < 4; i++) {
            tasks.add(() -> {
                start.await();
                var result = createOrder(token, body).andReturn().getResponse();
                assertThat(result.getStatus()).isEqualTo(201);
                return JsonPath.<Integer>read(result.getContentAsString(), "$.stopNumber");
            });
        }

        // Act
        var futures = tasks.stream().map(pool::submit).toList();
        start.countDown();
        var stops = new ArrayList<Integer>();
        for (var future : futures) {
            stops.add(future.get());
        }
        pool.shutdown();

        // Assert
        assertThat(stops).containsExactlyInAnyOrder(1, 2, 3, 4);
    }

    @Test
    void createOrder_replayedWithSameIdempotencyKey_createsOneOrder() throws Exception {
        // Arrange
        var driverId = createDriver();
        var body = body(driverId, tomorrow(), line(createArticle(uniqueName()), "1"));
        var key = UUID.randomUUID().toString();
        var token = adminToken();

        // Act
        var first = mockMvc.perform(post(DELIVERIES)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        var firstId = JsonPath.<String>read(first.getResponse().getContentAsString(), "$.id");
        mockMvc.perform(post(DELIVERIES)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andExpect(jsonPath("$.id").value(firstId));

        // Assert
        var orders = jdbcTemplate.queryForObject(
                "select count(*) from delivery_orders where driver_id = ?", Integer.class, driverId);
        assertThat(orders).isEqualTo(1);
    }
}
