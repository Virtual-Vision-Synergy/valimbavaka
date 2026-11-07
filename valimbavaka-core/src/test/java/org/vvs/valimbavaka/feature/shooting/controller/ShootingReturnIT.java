package org.vvs.valimbavaka.feature.shooting.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

/** SHO-02: the return puts the products back into the location they came from and sets the status RETURNED. */
class ShootingReturnIT extends ShootingApiTestSupport {

    private static final String ITEM = "{\"articleId\":\"%s\",\"quantity\":%s}";

    @Test
    void returnShooting_outExit_restoresStockAndReturnsReturned() throws Exception {
        // Arrange
        var articleId = createArticle();
        receive(articleId, "STORE", "10");
        var shootingId = createShooting(shootingBody(articleId, "3"));
        assertThat(balance(articleId, "STORE")).isEqualByComparingTo("7");

        // Act + Assert
        postReturn(adminToken(), shootingId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(shootingId.toString()))
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.late").value(false))
                .andExpect(jsonPath("$.returnedBy").value("Rasoa Rakoto"))
                .andExpect(jsonPath("$.returnedAt").isNotEmpty());
        assertThat(balance(articleId, "STORE")).isEqualByComparingTo("10");
    }

    @Test
    void returnShooting_validReturn_writesReferencedReturnMovementsAndUpdateJournalEntry() throws Exception {
        // Arrange
        var soap = createArticle();
        var scrub = createArticle();
        receive(soap, "STORE", "5");
        receive(scrub, "STORE", "4");
        var items = ITEM.formatted(soap, 2) + "," + ITEM.formatted(scrub, "1.5");
        var shootingId = createShooting(shootingBody(location("STORE"), tomorrow(), items));

        // Act
        postReturn(adminToken(), shootingId).andExpect(status().isOk());

        // Assert
        var movements = jdbcTemplate.queryForObject(
                "select count(*) from stock_movements where type = 'RETURN' and reference_id = ?"
                        + " and location_id = ? and quantity > 0",
                Integer.class,
                shootingId,
                location("STORE"));
        var journal = jdbcTemplate.queryForObject(
                "select count(*) from journal_entries where resource_type = 'SHOOTING' and resource_id = ?"
                        + " and action = 'UPDATE'",
                Integer.class,
                shootingId);
        assertThat(movements).isEqualTo(2);
        assertThat(journal).isEqualTo(1);
        assertThat(balance(soap, "STORE")).isEqualByComparingTo("5");
        assertThat(balance(scrub, "STORE")).isEqualByComparingTo("4");
    }

    @Test
    void returnShooting_exitFromWarehouse_putsTheStockBackInTheWarehouseOnly() throws Exception {
        // Arrange
        var articleId = createArticle();
        receive(articleId, "WAREHOUSE", "6");
        var shootingId = createShooting(
                shootingBody(location("WAREHOUSE"), tomorrow(), ITEM.formatted(articleId, 2)));

        // Act
        postReturn(adminToken(), shootingId).andExpect(status().isOk());

        // Assert
        assertThat(balance(articleId, "WAREHOUSE")).isEqualByComparingTo("6");
        assertThat(balance(articleId, "STORE")).isEqualByComparingTo("0");
    }

    @Test
    void returnShooting_alreadyReturned_returns409AndChangesNothing() throws Exception {
        // Arrange
        var articleId = createArticle();
        receive(articleId, "STORE", "4");
        var shootingId = createShooting(shootingBody(articleId, "1"));
        var token = adminToken();
        postReturn(token, shootingId).andExpect(status().isOk());

        // Act + Assert
        postReturn(token, shootingId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
        assertThat(balance(articleId, "STORE")).isEqualByComparingTo("4");
        assertThat(returnMovements(shootingId)).isEqualTo(1);
    }

    @Test
    void returnShooting_concurrentReturns_acceptsOnlyOne() throws Exception {
        // Arrange
        var articleId = createArticle();
        receive(articleId, "STORE", "3");
        var shootingId = createShooting(shootingBody(articleId, "2"));
        var token = adminToken();
        var attempts = 4;
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(attempts);
        var tasks = new ArrayList<Callable<Integer>>();
        for (var i = 0; i < attempts; i++) {
            tasks.add(() -> {
                start.await();
                return postReturn(token, shootingId).andReturn().getResponse().getStatus();
            });
        }

        // Act
        var futures = tasks.stream().map(pool::submit).toList();
        start.countDown();
        var statuses = new ArrayList<Integer>();
        for (var future : futures) {
            statuses.add(future.get());
        }
        pool.shutdown();

        // Assert
        assertThat(statuses).containsOnlyOnce(200).containsOnly(200, 409);
        assertThat(returnMovements(shootingId)).isEqualTo(1);
        assertThat(balance(articleId, "STORE")).isEqualByComparingTo("3");
    }

    @Test
    void returnShooting_unknownId_returns404() throws Exception {
        postReturn(adminToken(), UUID.randomUUID())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void getShootings_afterReturn_listsTheExitAsReturnedOnly() throws Exception {
        // Arrange
        var articleId = createArticle();
        receive(articleId, "STORE", "2");
        var shootingId = createShooting(shootingBody(articleId, "1"));
        var token = adminToken();
        var number = jdbcTemplate.queryForObject("select number from shootings where id = ?", String.class, shootingId);
        postReturn(token, shootingId).andExpect(status().isOk());

        // Act + Assert
        mockMvc.perform(get(SHOOTINGS)
                        .param("status", "RETURNED")
                        .param("search", number)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].status").value("RETURNED"));
        mockMvc.perform(get(SHOOTINGS)
                        .param("status", "OUT")
                        .param("search", number)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0));
    }

    private Integer returnMovements(UUID shootingId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from stock_movements where type = 'RETURN' and reference_id = ?",
                Integer.class,
                shootingId);
    }
}
