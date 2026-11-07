package org.vvs.valimbavaka.feature.production.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.stock.domain.StockMovementCommand;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.stock.service.StockMovementService;
import org.vvs.valimbavaka.feature.user.domain.RoleName;

/**
 * PRD-05: detail and cost price of a batch. Articles of the test support cost 1 200 Ar and finished products sell at
 * 8 500 Ar.
 */
class ProductionCostIT extends ProductionApiTestSupport {

    @Autowired
    private StockMovementService stockMovementService;

    @Test
    void getBatchCost_sellerOnCompletedBatch_returnsTheCostFrozenAtCompletion() throws Exception {
        // Arrange: 10 soaps for 2 kg of oil at 1 200 Ar; the oil costs 5 000 Ar afterwards
        var seller = bearer(tokenOf(createUser(RoleName.SELLER)));
        var oil = createArticle("RAW_MATERIAL");
        receive(oil, "2");
        var id = create("/api/v1/production/completed-batches", createArticle("FINISHED"), input(oil, "2"));
        jdbcTemplate.update("update articles set cost = 5000 where id = ?", oil);

        // Act + Assert
        mockMvc.perform(get(BATCHES + "/" + id + "/cost").header(HttpHeaders.AUTHORIZATION, seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.estimated").value(false))
                .andExpect(jsonPath("$.inputs[0].articleId").value(oil.toString()))
                .andExpect(jsonPath("$.inputs[0].unitCost").value(1200.0))
                .andExpect(jsonPath("$.inputs[0].lineCost").value(2400.0))
                .andExpect(jsonPath("$.materialCost").value(2400.0))
                .andExpect(jsonPath("$.unitCost").value(240.0))
                .andExpect(jsonPath("$.uncostedInputs").value(0))
                .andExpect(jsonPath("$.salePrice").value(8500.0))
                .andExpect(jsonPath("$.marginPercent").value(97));
    }

    @Test
    void getBatchCost_plannedBatch_returnsAnEstimateFromTheCurrentCost() throws Exception {
        // Arrange: planned batch, the oil now costs 1 500 Ar
        var oil = createArticle("RAW_MATERIAL");
        var id = create(BATCHES, createArticle("FINISHED"), input(oil, "2"));
        jdbcTemplate.update("update articles set cost = 1500 where id = ?", oil);

        // Act + Assert
        mockMvc.perform(get(BATCHES + "/" + id + "/cost").header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.estimated").value(true))
                .andExpect(jsonPath("$.completedAt").doesNotExist())
                .andExpect(jsonPath("$.materialCost").value(3000.0))
                .andExpect(jsonPath("$.unitCost").value(300.0));
    }

    @Test
    void getBatchCost_inputWithoutCost_returnsNoTotalAndCountsTheInput() throws Exception {
        // Arrange
        var oil = createArticle("RAW_MATERIAL");
        jdbcTemplate.update("update articles set cost = null where id = ?", oil);
        var id = create(BATCHES, createArticle("FINISHED"), input(oil, "2"));

        // Act + Assert
        mockMvc.perform(get(BATCHES + "/" + id + "/cost").header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inputs[0].lineCost").doesNotExist())
                .andExpect(jsonPath("$.uncostedInputs").value(1))
                .andExpect(jsonPath("$.materialCost").doesNotExist())
                .andExpect(jsonPath("$.unitCost").doesNotExist())
                .andExpect(jsonPath("$.marginPercent").doesNotExist());
    }

    @Test
    void getBatchCost_unknownBatch_returns404() throws Exception {
        var url = BATCHES + "/" + UUID.randomUUID() + "/cost";

        mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void getBatchCost_malformedId_returns400() throws Exception {
        mockMvc.perform(get(BATCHES + "/abc/cost").header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    /** Creates 10 units of {@code output} as administrator, with a fresh seller responsible. */
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
        var warehouseId =
                jdbcTemplate.queryForObject("select id from stock_locations where code = 'WAREHOUSE'", UUID.class);
        stockMovementService.record(new StockMovementCommand(
                articleId, warehouseId, StockMovementType.RECEIPT, new BigDecimal(quantity), null));
    }
}
