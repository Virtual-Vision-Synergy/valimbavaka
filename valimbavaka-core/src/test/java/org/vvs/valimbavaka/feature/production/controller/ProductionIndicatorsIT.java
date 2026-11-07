package org.vvs.valimbavaka.feature.production.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.stock.domain.StockMovementCommand;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.stock.service.StockMovementService;
import org.vvs.valimbavaka.feature.user.domain.RoleName;

/**
 * PRD-04: indicators of the month. The database is shared by the integration tests, so each test compares the
 * indicators before and after its own batches.
 */
class ProductionIndicatorsIT extends ProductionApiTestSupport {

    private static final String INDICATORS = "/api/v1/production/indicators";
    private static final ZoneId MADAGASCAR = ZoneId.of("Indian/Antananarivo");

    @Autowired
    private StockMovementService stockMovementService;

    @Test
    void getIndicators_batchCompletedThisMonth_addsTheBatchItsUnitsAndItsMaterialCost() throws Exception {
        // Arrange: 10 soaps for 2 kg of oil at 1 200 Ar (cost of createArticle) = 2 400 Ar of material
        var seller = bearer(tokenOf(createUser(RoleName.SELLER)));
        var before = indicators(seller);
        var oil = createArticle("RAW_MATERIAL");
        receive(oil, "2");

        // Act
        completeNewBatch(createArticle("FINISHED"), input(oil, "2"));
        var after = indicators(seller);

        // Assert
        assertThat(after.read("$.month", String.class)).isEqualTo(YearMonth.now(MADAGASCAR).toString());
        assertThat(delta(before, after, "completedBatches")).isEqualByComparingTo("1");
        assertThat(delta(before, after, "unitsProduced")).isEqualByComparingTo("10");
        assertThat(delta(before, after, "materialCost")).isEqualByComparingTo("2400");
        assertThat(delta(before, after, "uncostedBatches")).isEqualByComparingTo("0");
        assertThat(delta(before, after, "inProgressBatches")).isEqualByComparingTo("0");
        assertThat(after.read("$.materialCostPerUnit", Object.class)).isNotNull();
    }

    @Test
    void getIndicators_startedBatch_countsItInProgressOnly() throws Exception {
        // Arrange
        var admin = bearer(adminToken());
        var before = indicators(admin);
        var id = plan(createArticle("FINISHED"), input(createArticle("RAW_MATERIAL"), "1"));

        // Act
        mockMvc.perform(post(BATCHES + "/" + id + "/start").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk());
        var after = indicators(admin);

        // Assert
        assertThat(delta(before, after, "inProgressBatches")).isEqualByComparingTo("1");
        assertThat(delta(before, after, "completedBatches")).isEqualByComparingTo("0");
        assertThat(delta(before, after, "unitsProduced")).isEqualByComparingTo("0");
    }

    @Test
    void getIndicators_inputWithoutCost_countsTheBatchAsUncostedAndLeavesItOutOfTheCost() throws Exception {
        // Arrange
        var admin = bearer(adminToken());
        var before = indicators(admin);
        var oil = createArticle("RAW_MATERIAL");
        jdbcTemplate.update("update articles set cost = null where id = ?", oil);
        receive(oil, "2");

        // Act
        completeNewBatch(createArticle("FINISHED"), input(oil, "2"));
        var after = indicators(admin);

        // Assert
        assertThat(delta(before, after, "completedBatches")).isEqualByComparingTo("1");
        assertThat(delta(before, after, "unitsProduced")).isEqualByComparingTo("10");
        assertThat(delta(before, after, "uncostedBatches")).isEqualByComparingTo("1");
        assertThat(delta(before, after, "materialCost")).isEqualByComparingTo("0");
    }

    @Test
    void getIndicators_batchCompletedLastMonth_countsInThePreviousMonthOnly() throws Exception {
        // Arrange: completion moved to one hour before the first day of the month, Madagascar time
        var admin = bearer(adminToken());
        var before = indicators(admin);
        var oil = createArticle("RAW_MATERIAL");
        receive(oil, "2");
        var id = completeNewBatch(createArticle("FINISHED"), input(oil, "2"));
        var lastMonth = YearMonth.now(MADAGASCAR).atDay(1).atStartOfDay(MADAGASCAR).minusHours(1);
        var completedAt = Timestamp.from(lastMonth.toInstant());
        jdbcTemplate.update("update production_batches set completed_at = ? where id = ?", completedAt, id);

        // Act
        var after = indicators(admin);

        // Assert
        assertThat(delta(before, after, "completedBatches")).isEqualByComparingTo("0");
        assertThat(delta(before, after, "unitsProduced")).isEqualByComparingTo("0");
        assertThat(delta(before, after, "previousMonthUnits")).isEqualByComparingTo("10");
    }

    private DocumentContext indicators(String authorization) throws Exception {
        var json = mockMvc.perform(get(INDICATORS).header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.parse(json);
    }

    private static BigDecimal delta(DocumentContext before, DocumentContext after, String field) {
        return number(after, field).subtract(number(before, field));
    }

    private static BigDecimal number(DocumentContext indicators, String field) {
        return new BigDecimal(String.valueOf(indicators.read("$." + field, Object.class)));
    }

    /** Plans 10 units of {@code output} and completes them in one request (PRD-02). */
    private UUID completeNewBatch(UUID output, String inputs) throws Exception {
        return create("/api/v1/production/completed-batches", output, inputs);
    }

    private UUID plan(UUID output, String inputs) throws Exception {
        return create(BATCHES, output, inputs);
    }

    private UUID create(String url, UUID output, String inputs) throws Exception {
        var seller = createUser(RoleName.SELLER);
        var response = mockMvc.perform(postJson(url, batchBody(output, "10", seller.getId(), inputs))
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
}
