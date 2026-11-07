package org.vvs.valimbavaka.feature.production.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.stock.domain.StockMovementCommand;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.stock.service.StockMovementService;
import org.vvs.valimbavaka.feature.user.domain.RoleName;

/** PRD-02: completing a batch moves inputs out of the warehouse and the output in, all or nothing. */
class ProductionCompletionIT extends ProductionApiTestSupport {

    private static final String COMPLETED_BATCHES = "/api/v1/production/completed-batches";

    @Autowired
    private StockMovementService stockMovementService;

    @Test
    void completeBatch_enoughStock_returns200CompletedWithMovementsAndFrozenCosts() throws Exception {
        // Arrange
        var output = createArticle("FINISHED");
        var oil = createArticle("RAW_MATERIAL");
        var base = createArticle("SEMI_FINISHED");
        receive(oil, "10");
        receive(base, "5");
        var id = plan(output, "30", input(oil, "0.45") + "," + input(base, "1.5"));

        // Act
        complete(id, bearer(adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty())
                .andExpect(jsonPath("$.inputs[0].unitCost").value(1200))
                .andExpect(jsonPath("$.inputs[1].unitCost").value(1200));

        // Assert
        assertThat(warehouseStock(oil)).isEqualByComparingTo("9.55");
        assertThat(warehouseStock(base)).isEqualByComparingTo("3.5");
        assertThat(warehouseStock(output)).isEqualByComparingTo("30");
        assertThat(productionMovements(id)).isEqualTo(3);
        assertThat(journalEntries(id, "UPDATE")).isEqualTo(1);
    }

    @Test
    void completeBatch_oneInputShort_returns409AndMovesNothing() throws Exception {
        // Arrange
        var output = createArticle("FINISHED");
        var oil = createArticle("RAW_MATERIAL");
        var base = createArticle("SEMI_FINISHED");
        receive(oil, "10");
        receive(base, "1");
        var id = plan(output, "30", input(oil, "0.45") + "," + input(base, "1.5"));

        // Act
        complete(id, bearer(adminToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        // Assert
        assertThat(warehouseStock(oil)).isEqualByComparingTo("10");
        assertThat(warehouseStock(output)).isZero();
        assertThat(productionMovements(id)).isZero();
        assertThat(batchStatus(id)).isEqualTo("PLANNED");
    }

    @Test
    void completeBatch_alreadyCompleted_returns409AndMovesTheStockOnlyOnce() throws Exception {
        // Arrange
        var output = createArticle("FINISHED");
        var oil = createArticle("RAW_MATERIAL");
        receive(oil, "10");
        var id = plan(output, "5", input(oil, "2"));
        var auth = bearer(adminToken());
        complete(id, auth)
                .andExpect(status().isOk());

        // Act
        complete(id, auth)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        // Assert
        assertThat(warehouseStock(oil)).isEqualByComparingTo("8");
        assertThat(productionMovements(id)).isEqualTo(2);
    }

    @Test
    void completeBatch_unknownBatch_returns404() throws Exception {
        complete(UUID.randomUUID(), bearer(adminToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void createCompletedBatch_enoughStock_returns201CompletedAndMovesTheStock() throws Exception {
        // Arrange
        var seller = createUser(RoleName.SELLER);
        var output = createArticle("FINISHED");
        var oil = createArticle("RAW_MATERIAL");
        receive(oil, "3");
        var body = batchBody(output, "12", seller.getId(), input(oil, "3"));

        // Act
        var response = mockMvc.perform(postJson(COMPLETED_BATCHES, body)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert
        UUID id = UUID.fromString(JsonPath.read(response, "$.id"));
        assertThat(warehouseStock(oil)).isZero();
        assertThat(warehouseStock(output)).isEqualByComparingTo("12");
        assertThat(productionMovements(id)).isEqualTo(2);
        assertThat(journalEntries(id, "CREATE")).isEqualTo(1);
        assertThat(journalEntries(id, "UPDATE")).isEqualTo(1);
    }

    @Test
    void createCompletedBatch_insufficientStock_returns409AndCreatesNoBatch() throws Exception {
        // Arrange
        var seller = createUser(RoleName.SELLER);
        var output = createArticle("FINISHED");
        var oil = createArticle("RAW_MATERIAL");
        receive(oil, "1");
        var body = batchBody(output, "12", seller.getId(), input(oil, "3"));

        // Act
        mockMvc.perform(postJson(COMPLETED_BATCHES, body).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isConflict());

        // Assert
        var batches = jdbcTemplate.queryForObject(
                "select count(*) from production_batches where output_article_id = ?", Integer.class, output);
        assertThat(batches).isZero();
        assertThat(warehouseStock(oil)).isEqualByComparingTo("1");
    }

    @Test
    void createCompletedBatch_bodyWithIdStatusAndCost_ignoresThem() throws Exception {
        // Arrange
        var seller = createUser(RoleName.SELLER);
        var output = createArticle("FINISHED");
        var oil = createArticle("RAW_MATERIAL");
        receive(oil, "2");
        var forged = UUID.randomUUID();
        var body = """
                {"id":"%s","status":"PLANNED","completedAt":"2020-01-01T00:00:00Z","outputArticleId":"%s",
                 "quantity":4,"operatorId":"%s","inputs":[{"articleId":"%s","quantity":2,"unitCost":1}]}
                """.formatted(forged, output, seller.getId(), oil);

        // Act + Assert
        mockMvc.perform(postJson(COMPLETED_BATCHES, body).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(not(forged.toString())))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").value(not("2020-01-01T00:00:00Z")))
                .andExpect(jsonPath("$.inputs[0].unitCost").value(1200));
    }

    private ResultActions complete(UUID batchId, String authorization) throws Exception {
        return mockMvc.perform(
                post(BATCHES + "/" + batchId + "/completion").header(HttpHeaders.AUTHORIZATION, authorization));
    }

    private UUID plan(UUID output, String quantity, String inputs) throws Exception {
        var seller = createUser(RoleName.SELLER);
        var response = mockMvc.perform(postJson(BATCHES, batchBody(output, quantity, seller.getId(), inputs))
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

    private BigDecimal warehouseStock(UUID articleId) {
        return jdbcTemplate.queryForObject(
                "select coalesce(sum(quantity), 0) from stock_movements where article_id = ? and location_id = ?",
                BigDecimal.class,
                articleId,
                warehouseId());
    }

    private Integer productionMovements(UUID batchId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from stock_movements where type = 'PRODUCTION' and reference_id = ?",
                Integer.class,
                batchId);
    }

    private Integer journalEntries(UUID batchId, String action) {
        return jdbcTemplate.queryForObject(
                "select count(*) from journal_entries where resource_type = 'PRODUCTION_BATCH' and resource_id = ?"
                        + " and action = ?",
                Integer.class,
                batchId,
                action);
    }

    private String batchStatus(UUID batchId) {
        return jdbcTemplate.queryForObject(
                "select status from production_batches where id = ?", String.class, batchId);
    }
}
