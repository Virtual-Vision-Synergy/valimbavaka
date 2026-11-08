package org.vvs.valimbavaka.feature.day.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.feature.day.service.BusinessDayService;
import org.vvs.valimbavaka.feature.stock.controller.StockApiTestSupport;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.user.domain.RoleName;

/** JR-01: the morning dispatch opens the day with one validated transfer per destination and moves the stock. */
class BusinessDayIT extends StockApiTestSupport {

    private static final String DAYS = "/api/v1/days";
    private static final String OPENING = "/api/v1/days/opening";

    @Autowired
    private BusinessDayService businessDayService;

    /** One day per date: each test starts with today not opened yet. */
    @BeforeEach
    void forgetToday() {
        jdbcTemplate.update("delete from business_day_transfers");
        jdbcTemplate.update("delete from business_days");
    }

    @Test
    void openDay_twoDestinations_returns201WithOneValidatedMorningTransferPerDestination() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        var oil = createArticle(uniqueName());
        move(soap, "WAREHOUSE", StockMovementType.RECEIPT, "20");
        move(oil, "WAREHOUSE", StockMovementType.RECEIPT, "10");
        var lines = String.join(",", line(soap, "STORE", "12"), line(oil, "STORE", "4"), line(soap, "DELIVERY", "5"));

        // Act
        var json = openDay(adminToken(), body(lines))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.businessDate").isNotEmpty())
                .andExpect(jsonPath("$.transfers.length()").value(2))
                .andExpect(jsonPath("$.transfers[0].locationId")
                        .value(location("STORE").toString()))
                .andExpect(jsonPath("$.transfers[0].lineCount").value(2))
                .andExpect(jsonPath("$.transfers[0].quantity").value(16.0))
                .andExpect(jsonPath("$.transfers[1].locationId")
                        .value(location("DELIVERY").toString()))
                .andExpect(jsonPath("$.transfers[1].transferNumber").value(startsWith("TRF-")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert
        List<String> transferIds = JsonPath.read(json, "$.transfers[*].transferId");
        for (var transferId : transferIds) {
            var row = jdbcTemplate.queryForMap(
                    "select origin, status from transfers where id = ?", UUID.fromString(transferId));
            assertThat(row).containsEntry("origin", "MORNING").containsEntry("status", "VALIDATED");
        }
        assertThat(balance(soap, "WAREHOUSE")).isEqualByComparingTo("3");
        assertThat(balance(soap, "STORE")).isEqualByComparingTo("12");
        assertThat(balance(soap, "DELIVERY")).isEqualByComparingTo("5");
        assertThat(balance(oil, "STORE")).isEqualByComparingTo("4");
    }

    @Test
    void openDay_sameArticleTwiceForADestination_addsTheQuantitiesInOneLine() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "WAREHOUSE", StockMovementType.RECEIPT, "10");

        // Act + Assert
        openDay(adminToken(), body(line(soap, "STORE", "2") + "," + line(soap, "STORE", "3")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transfers[0].lineCount").value(1))
                .andExpect(jsonPath("$.transfers[0].quantity").value(5.0));
        assertThat(balance(soap, "STORE")).isEqualByComparingTo("5");
    }

    @Test
    void openDay_validDispatch_isTracedInTheJournal() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "WAREHOUSE", StockMovementType.RECEIPT, "5");

        // Act
        var json = openDay(adminToken(), body(line(soap, "TRADE_SHOW", "2")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert
        var dayId = UUID.fromString(JsonPath.read(json, "$.id"));
        var description = jdbcTemplate.queryForObject(
                "select description from journal_entries where resource_type = 'BUSINESS_DAY' and resource_id = ? "
                        + "and action = 'CREATE'",
                String.class,
                dayId);
        assertThat(description).startsWith("Ouverture de la journée du ").contains("1 transfert(s) du matin (TRF-");
    }

    @Test
    void openDay_noLine_opensTheDayWithoutTransfer() throws Exception {
        openDay(adminToken(), body(""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.transfers.length()").value(0));
    }

    @Test
    void openDay_alreadyOpenedToday_returns409() throws Exception {
        // Arrange
        openDay(adminToken(), body("")).andExpect(status().isCreated());

        // Act + Assert
        openDay(adminToken(), body(""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void openDay_totalAboveWarehouseStock_returns409AndWritesNothing() throws Exception {
        // Arrange: 8 + 5 asked across two destinations, 12 at the warehouse.
        var soap = createArticle(uniqueName());
        move(soap, "WAREHOUSE", StockMovementType.RECEIPT, "12");

        // Act
        openDay(adminToken(), body(line(soap, "STORE", "8") + "," + line(soap, "DELIVERY", "5")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        // Assert
        assertThat(balance(soap, "WAREHOUSE")).isEqualByComparingTo("12");
        var days = jdbcTemplate.queryForObject("select count(*) from business_days", Integer.class);
        assertThat(days).isZero();
    }

    @Test
    void openDay_destinationIsTheWarehouse_returns422() throws Exception {
        var soap = createArticle(uniqueName());
        move(soap, "WAREHOUSE", StockMovementType.RECEIPT, "5");
        assertValidationFailed(body(line(soap, "WAREHOUSE", "1")));
    }

    @Test
    void openDay_unknownDestination_returns422() throws Exception {
        var lines = "{\"articleId\":\"%s\",\"destinationId\":\"%s\",\"quantity\":1}"
                .formatted(createArticle(uniqueName()), UUID.randomUUID());
        assertValidationFailed(body(lines));
    }

    @Test
    void openDay_zeroQuantity_returns422() throws Exception {
        assertValidationFailed(body(line(createArticle(uniqueName()), "STORE", "0")));
    }

    @Test
    void openDay_quantityWithFourDecimals_returns422() throws Exception {
        assertValidationFailed(body(line(createArticle(uniqueName()), "STORE", "1.0005")));
    }

    @Test
    void openDay_missingLines_returns422() throws Exception {
        assertValidationFailed("{}");
    }

    @Test
    void openDay_unknownArticle_returns404() throws Exception {
        openDay(adminToken(), body(line(UUID.randomUUID(), "STORE", "1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void openDay_massAssignmentOfIdDateAndStatus_isIgnored() throws Exception {
        // Arrange
        var forcedId = UUID.randomUUID();
        var body = """
                {"id":"%s","businessDate":"2020-01-01","status":"CLOSED","lines":[]}
                """.formatted(forcedId);

        // Act + Assert
        openDay(adminToken(), body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(not(forcedId.toString())))
                .andExpect(jsonPath("$.businessDate").value(not("2020-01-01")))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void openDay_replayedWithSameIdempotencyKey_opensTheDayOnce() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "WAREHOUSE", StockMovementType.RECEIPT, "4");
        var body = body(line(soap, "STORE", "4"));
        var key = UUID.randomUUID().toString();
        var token = adminToken();

        // Act
        var first = withKey(token, key, body).andExpect(status().isCreated()).andReturn();
        withKey(token, key, body)
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andExpect(jsonPath("$.id")
                        .value((String) JsonPath.read(first.getResponse().getContentAsString(), "$.id")));

        // Assert
        assertThat(balance(soap, "STORE")).isEqualByComparingTo("4");
    }

    @Test
    void openDay_withoutToken_returns401() throws Exception {
        mockMvc.perform(post(DAYS).contentType(MediaType.APPLICATION_JSON).content(body("")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void openDay_withSellerRole_returns403() throws Exception {
        openDay(tokenOf(RoleName.SELLER), body(""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getOpening_beforeOpening_listsTheWarehouseArticlesAndTheDestinations() throws Exception {
        // Arrange
        var stocked = createArticle(uniqueName());
        var empty = createArticle(uniqueName());
        move(stocked, "WAREHOUSE", StockMovementType.RECEIPT, "20");
        move(empty, "STORE", StockMovementType.RECEIPT, "3");

        // Act
        var json = getOpening(adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessDate").isNotEmpty())
                .andExpect(jsonPath("$.day").doesNotExist())
                .andExpect(jsonPath("$.warehouseLocationId")
                        .value(location("WAREHOUSE").toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert
        List<String> destinations = JsonPath.read(json, "$.destinations[*].code");
        assertThat(destinations).contains("STORE", "DELIVERY", "TRADE_SHOW").doesNotContain("WAREHOUSE");
        var stockedFilter = "$.lines[?(@.articleId == '%s')].warehouse".formatted(stocked);
        List<Object> warehouse = JsonPath.read(json, stockedFilter);
        assertThat(warehouse).hasSize(1);
        assertThat(new BigDecimal(warehouse.getFirst().toString())).isEqualByComparingTo("20");
        List<Object> absent = JsonPath.read(json, "$.lines[?(@.articleId == '%s')]".formatted(empty));
        assertThat(absent).isEmpty();
    }

    @Test
    void getOpening_afterOpening_returnsTodayWithItsMorningTransfers() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "WAREHOUSE", StockMovementType.RECEIPT, "6");
        openDay(adminToken(), body(line(soap, "STORE", "6"))).andExpect(status().isCreated());

        // Act + Assert
        getOpening(adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.day.status").value("OPEN"))
                .andExpect(jsonPath("$.day.transfers.length()").value(1))
                .andExpect(jsonPath("$.day.transfers[0].locationId")
                        .value(location("STORE").toString()));
    }

    @Test
    void getOpening_withSellerRole_returns403() throws Exception {
        getOpening(tokenOf(RoleName.SELLER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void requireOpenDay_beforeAndAfterOpening_throwsThenReturnsTheOpenDay() throws Exception {
        // Arrange
        assertThat(catchThrowable(businessDayService::requireOpenDay)).isInstanceOf(ConflictException.class);
        openDay(adminToken(), body("")).andExpect(status().isCreated());

        // Act
        var day = businessDayService.requireOpenDay();

        // Assert
        assertThat(day.status().name()).isEqualTo("OPEN");
    }

    private ResultActions openDay(String token, String body) throws Exception {
        return mockMvc.perform(post(DAYS)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions withKey(String token, String key, String body) throws Exception {
        return mockMvc.perform(post(DAYS)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions getOpening(String token) throws Exception {
        return mockMvc.perform(get(OPENING).header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private void assertValidationFailed(String body) throws Exception {
        openDay(adminToken(), body)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private static String body(String lines) {
        return "{\"lines\":[%s]}".formatted(lines);
    }

    private String line(UUID articleId, String destinationCode, String quantity) {
        return "{\"articleId\":\"%s\",\"destinationId\":\"%s\",\"quantity\":%s}"
                .formatted(articleId, location(destinationCode), quantity);
    }

    private BigDecimal balance(UUID articleId, String locationCode) {
        return jdbcTemplate.queryForObject(
                "select coalesce(sum(quantity), 0) from stock_movements where article_id = ? and location_id = ?",
                BigDecimal.class,
                articleId,
                location(locationCode));
    }
}
