package org.vvs.valimbavaka.feature.production.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.stock.domain.StockMovementCommand;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.stock.service.StockMovementService;
import org.vvs.valimbavaka.feature.user.domain.RoleName;

/** PRD-03: the status of a batch only moves forward, PLANNED then IN_PROGRESS then COMPLETED, seller included. */
class ProductionStatusIT extends ProductionApiTestSupport {

    @Autowired
    private StockMovementService stockMovementService;

    @Test
    void startBatch_plannedBatchBySeller_returns200InProgressAndMovesNothing() throws Exception {
        // Arrange
        var seller = bearer(tokenOf(createUser(RoleName.SELLER)));
        var oil = createArticle("RAW_MATERIAL");
        var id = plan(createArticle("FINISHED"), input(oil, "2"));

        // Act
        start(id, seller)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.completedAt").value(nullValue()));

        // Assert
        assertThat(batchStatus(id)).isEqualTo("IN_PROGRESS");
        assertThat(productionMovements(id)).isZero();
        assertThat(journalEntries(id)).isEqualTo(1);
    }

    @Test
    void startBatch_alreadyInProgress_returns409AndTracesTheStartOnce() throws Exception {
        // Arrange
        var seller = bearer(tokenOf(createUser(RoleName.SELLER)));
        var id = plan(createArticle("FINISHED"), input(createArticle("RAW_MATERIAL"), "2"));
        start(id, seller).andExpect(status().isOk());

        // Act
        start(id, seller)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        // Assert
        assertThat(journalEntries(id)).isEqualTo(1);
    }

    @Test
    void startBatch_completedBatch_returns409AndStaysCompleted() throws Exception {
        // Arrange
        var oil = createArticle("RAW_MATERIAL");
        receive(oil, "2");
        var id = plan(createArticle("FINISHED"), input(oil, "2"));
        var admin = bearer(adminToken());
        complete(id, admin).andExpect(status().isOk());

        // Act
        start(id, admin)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        // Assert
        assertThat(batchStatus(id)).isEqualTo("COMPLETED");
    }

    @Test
    void startBatch_unknownBatch_returns404() throws Exception {
        start(UUID.randomUUID(), bearer(adminToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void startBatch_bodyWithStatusAndCompletionDate_ignoresThem() throws Exception {
        // Arrange
        var id = plan(createArticle("FINISHED"), input(createArticle("RAW_MATERIAL"), "2"));
        var forged = "{\"status\":\"COMPLETED\",\"completedAt\":\"2020-01-01T00:00:00Z\"}";

        var request = post(BATCHES + "/" + id + "/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(forged)
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()));

        // Act + Assert
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.completedAt").value(nullValue()));
        assertThat(productionMovements(id)).isZero();
    }

    @Test
    void completeBatch_inProgressBatchBySeller_returns200CompletedAndMovesTheStock() throws Exception {
        // Arrange
        var seller = bearer(tokenOf(createUser(RoleName.SELLER)));
        var oil = createArticle("RAW_MATERIAL");
        receive(oil, "5");
        var id = plan(createArticle("FINISHED"), input(oil, "2"));
        start(id, seller).andExpect(status().isOk());

        // Act
        complete(id, seller)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());

        // Assert
        assertThat(productionMovements(id)).isEqualTo(2);
        assertThat(journalEntries(id)).isEqualTo(2);
    }

    @Test
    void getBatches_statusInProgress_listsTheStartedBatchOnly() throws Exception {
        // Arrange
        var admin = bearer(adminToken());
        var output = createArticle("FINISHED");
        var oil = createArticle("RAW_MATERIAL");
        var started = plan(output, input(oil, "1"));
        var planned = plan(output, input(oil, "1"));
        start(started, admin).andExpect(status().isOk());

        // Act + Assert
        inProgress(batchNumber(started), admin)
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].status").value("IN_PROGRESS"));
        inProgress(batchNumber(planned), admin)
                .andExpect(jsonPath("$.totalCount").value(0));
    }

    private ResultActions start(UUID batchId, String authorization) throws Exception {
        return mockMvc.perform(
                post(BATCHES + "/" + batchId + "/start").header(HttpHeaders.AUTHORIZATION, authorization));
    }

    private ResultActions complete(UUID batchId, String authorization) throws Exception {
        return mockMvc.perform(
                post(BATCHES + "/" + batchId + "/completion").header(HttpHeaders.AUTHORIZATION, authorization));
    }

    private ResultActions inProgress(String number, String authorization) throws Exception {
        var request = get(BATCHES)
                .param("status", "IN_PROGRESS")
                .param("search", number)
                .header(HttpHeaders.AUTHORIZATION, authorization);
        return mockMvc.perform(request).andExpect(status().isOk());
    }

    private UUID plan(UUID output, String inputs) throws Exception {
        var seller = createUser(RoleName.SELLER);
        var response = mockMvc.perform(postJson(BATCHES, batchBody(output, "10", seller.getId(), inputs))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(JsonPath.read(response, "$.id"));
    }

    private void receive(UUID articleId, String quantity) {
        stockMovementService.record(new StockMovementCommand(
                articleId, warehouseId(), StockMovementType.RECEIPT, new BigDecimal(quantity), null));
    }

    private UUID warehouseId() {
        return jdbcTemplate.queryForObject("select id from stock_locations where code = 'WAREHOUSE'", UUID.class);
    }

    private String batchStatus(UUID batchId) {
        return jdbcTemplate.queryForObject("select status from production_batches where id = ?", String.class, batchId);
    }

    private String batchNumber(UUID batchId) {
        return jdbcTemplate.queryForObject("select number from production_batches where id = ?", String.class, batchId);
    }

    private Integer productionMovements(UUID batchId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from stock_movements where type = 'PRODUCTION' and reference_id = ?",
                Integer.class,
                batchId);
    }

    private Integer journalEntries(UUID batchId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from journal_entries where resource_type = 'PRODUCTION_BATCH' and resource_id = ?"
                        + " and action = 'UPDATE'",
                Integer.class,
                batchId);
    }
}
