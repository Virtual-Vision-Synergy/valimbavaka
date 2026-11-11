package org.vvs.valimbavaka.feature.dayclosing.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.stock.controller.StockApiTestSupport;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.user.domain.RoleName;

/**
 * JR-05: evening count of today's closing with a mandatory reason for every gap, administrator only. The database is
 * shared with the other test classes: every test works on its own article, and the counts it saves are removed
 * afterwards so that the classes deleting the business days are not blocked by them.
 */
class DayClosingCountIT extends StockApiTestSupport {

    private static final String COUNTS = "/api/v1/days/closing/counts";
    private static final String CLOSING = "/api/v1/days/closing";

    private UUID soap;
    private UUID store;

    /** Today's day opened (another test class may have removed it), and 10 units of a new article in the store. */
    @BeforeEach
    void openTodayAndStockTheStore() throws Exception {
        openToday();
        store = location("STORE");
        soap = createArticle(uniqueName());
        move(soap, "WAREHOUSE", StockMovementType.RECEIPT, "200");
        move(soap, "WAREHOUSE", StockMovementType.TRANSFER, "-10");
        move(soap, "STORE", StockMovementType.TRANSFER, "10");
    }

    @AfterEach
    void removeTheSavedCounts() {
        jdbcTemplate.update("delete from day_closing_counts");
    }

    @Test
    void saveCounts_withoutToken_returns401() throws Exception {
        mockMvc.perform(put(COUNTS).contentType(MediaType.APPLICATION_JSON).content(body("8", "BREAKAGE")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void saveCounts_sellerAndDeliveryDriver_return403() throws Exception {
        for (var role : List.of(RoleName.SELLER, RoleName.DELIVERY_DRIVER)) {
            mockMvc.perform(put(COUNTS)
                            .header(HttpHeaders.AUTHORIZATION, bearer(tokenOf(role)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("8", "BREAKAGE")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }
        assertThat(savedRows()).isZero();
    }

    @Test
    void saveCounts_dayNotOpened_returns409() throws Exception {
        // Arrange: every day moves back in time, so that none is today's
        jdbcTemplate.update("update business_days set business_date = business_date - 400");

        // Act + Assert
        try {
            save(body("8", "BREAKAGE"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLICT"))
                    .andExpect(jsonPath("$.detail").value(startsWith("La journée du ")));
        } finally {
            jdbcTemplate.update("update business_days set business_date = business_date + 400");
        }
    }

    @Test
    void saveCounts_gapWithoutReason_returns422AndSavesNothing() throws Exception {
        // Act + Assert: 10 expected, 8 counted, no reason
        save(body("8", null))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.detail").value(containsString("Un motif est obligatoire")));
        assertThat(savedRows()).isZero();
        assertThat(line().get("counted")).isNull();
    }

    @Test
    void saveCounts_gapWithReason_returns200WithTheCountAndTheReasonOnTheLine() throws Exception {
        // Act
        var json = saveAndRead(body("8", "BREAKAGE"));

        // Assert
        var line = lineIn(json);
        assertThat(number(line, "counted")).isEqualTo(8.0);
        assertThat(line.get("reason")).isEqualTo("BREAKAGE");
        assertThat(number(line, "theoretical")).isEqualTo(10.0);
        assertThat(savedRows()).isEqualTo(1);
        var theoretical = jdbcTemplate.queryForObject(
                "select theoretical_quantity from day_closing_counts where article_id = ?", BigDecimal.class, soap);
        assertThat(theoretical).isEqualByComparingTo("10");
    }

    @Test
    void saveCounts_gapWithReason_writesAJournalLineOnTheDay() throws Exception {
        // Act
        save(body("8", "LOSS")).andExpect(status().isOk());

        // Assert
        var count = jdbcTemplate.queryForObject(
                "select count(*) from journal_entries where resource_type = 'BUSINESS_DAY' and action = 'UPDATE' "
                        + "and description like 'Comptage de clôture du %'",
                Integer.class);
        assertThat(count).isPositive();
    }

    @Test
    void saveCounts_countEqualToTheTheoretical_needsNoReason() throws Exception {
        // Act
        var json = saveAndRead(body("10", null));

        // Assert
        var line = lineIn(json);
        assertThat(number(line, "counted")).isEqualTo(10.0);
        assertThat(line.get("reason")).isNull();
    }

    @Test
    void saveCounts_reasonSentWithoutGap_isIgnored() throws Exception {
        // Act
        var json = saveAndRead(body("10", "LOSS"));

        // Assert
        assertThat(lineIn(json).get("reason")).isNull();
        var reason = jdbcTemplate.queryForObject(
                "select reason from day_closing_counts where article_id = ?", String.class, soap);
        assertThat(reason).isNull();
    }

    @Test
    void saveCounts_zeroCounted_isAGapThatNeedsAReason() throws Exception {
        // Act + Assert
        save(body("0", null)).andExpect(status().isUnprocessableContent());
        save(body("0", "LOSS")).andExpect(status().isOk());
        assertThat(number(line(), "counted")).isZero();
    }

    @Test
    void saveCounts_recount_replacesTheCountAndClearsTheReasonWhenTheGapIsGone() throws Exception {
        // Arrange
        save(body("8", "BREAKAGE")).andExpect(status().isOk());

        // Act
        save(body("10", null)).andExpect(status().isOk());

        // Assert
        assertThat(savedRows()).isEqualTo(1);
        var line = line();
        assertThat(number(line, "counted")).isEqualTo(10.0);
        assertThat(line.get("reason")).isNull();
    }

    @Test
    void saveCounts_sameRequestSentTwice_leavesASingleCount() throws Exception {
        // Act
        save(body("8", "SAMPLE")).andExpect(status().isOk());
        save(body("8", "SAMPLE")).andExpect(status().isOk());

        // Assert
        assertThat(savedRows()).isEqualTo(1);
        assertThat(line().get("reason")).isEqualTo("SAMPLE");
    }

    @Test
    void saveCounts_validLineAndGapWithoutReason_savesNeitherOfThem() throws Exception {
        // Arrange
        var other = createArticle(uniqueName());
        move(other, "WAREHOUSE", StockMovementType.RECEIPT, "50");
        move(other, "WAREHOUSE", StockMovementType.TRANSFER, "-5");
        move(other, "STORE", StockMovementType.TRANSFER, "5");
        var request = "{\"counts\":[%s,%s]}".formatted(count(soap, "10", null), count(other, "4", null));

        // Act + Assert
        save(request).andExpect(status().isUnprocessableContent());
        assertThat(savedRows()).isZero();
    }

    @Test
    void saveCounts_aSaleAfterTheCount_showsANewGapOnTheLineWhileTheSavedCountStays() throws Exception {
        // Arrange
        save(body("10", null)).andExpect(status().isOk());

        // Act
        move(soap, "STORE", StockMovementType.SALE, "-2");

        // Assert: the count (10) is kept, the theoretical follows the sale (8)
        var line = line();
        assertThat(number(line, "counted")).isEqualTo(10.0);
        assertThat(number(line, "theoretical")).isEqualTo(8.0);
    }

    @Test
    void saveCounts_unknownArticle_returns404() throws Exception {
        // Act + Assert
        var request = "{\"counts\":[%s]}".formatted(count(UUID.randomUUID(), "1", null));
        save(request)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void saveCounts_articleOfAnotherLocation_returns404() throws Exception {
        // Act + Assert: the article is held by the store only
        var request = "{\"counts\":[{\"locationId\":\"%s\",\"articleId\":\"%s\",\"counted\":10}]}"
                .formatted(location("DELIVERY"), soap);
        save(request).andExpect(status().isNotFound());
    }

    @Test
    void saveCounts_sameLineTwice_returns422() throws Exception {
        // Act + Assert
        var request = "{\"counts\":[%s,%s]}".formatted(count(soap, "10", null), count(soap, "9", "LOSS"));
        save(request).andExpect(status().isUnprocessableContent());
    }

    @Test
    void saveCounts_invalidBody_returns422() throws Exception {
        // Act + Assert
        save("{\"counts\":[]}").andExpect(status().isUnprocessableContent());
        save("{}").andExpect(status().isUnprocessableContent());
        save(body("-1", "LOSS")).andExpect(status().isUnprocessableContent());
        save(body("1.2345", "LOSS")).andExpect(status().isUnprocessableContent());
        save("{\"counts\":[{\"locationId\":\"%s\",\"articleId\":\"%s\"}]}".formatted(store, soap))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void saveCounts_unknownReason_returns400() throws Exception {
        // Act + Assert
        save(body("8", "FORGOTTEN")).andExpect(status().isBadRequest());
    }

    @Test
    void saveCounts_theoreticalGapAndIdSentByTheClient_areRefusedAndNothingIsSaved() throws Exception {
        // Arrange: the client claims a theoretical of 8, so that 8 would be no gap
        var request = "{\"counts\":[{\"locationId\":\"%s\",\"articleId\":\"%s\",\"counted\":8,\"theoretical\":8,"
                + "\"gap\":0,\"id\":\"%s\"}]}".formatted(store, soap, UUID.randomUUID());

        // Act + Assert: unknown fields are not read (the server computes both), the request is malformed
        save(request).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        assertThat(savedRows()).isZero();
    }

    @Test
    void getClosing_afterTheCount_returnsTheSavedCountsAndTheDayId() throws Exception {
        // Arrange
        save(body("7", "ENTRY_ERROR")).andExpect(status().isOk());

        // Act
        var json = mockMvc.perform(get(CLOSING).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessDayId").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert
        var line = lineIn(json);
        assertThat(number(line, "counted")).isEqualTo(7.0);
        assertThat(line.get("reason")).isEqualTo("ENTRY_ERROR");
    }

    private ResultActions save(String body) throws Exception {
        return mockMvc.perform(put(COUNTS)
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    /** Saves the counts, expects 200 and returns the reconciliation. */
    private String saveAndRead(String body) throws Exception {
        return save(body).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private String body(String counted, String reason) {
        return "{\"counts\":[%s]}".formatted(count(soap, counted, reason));
    }

    private String count(UUID articleId, String counted, String reason) {
        var reasonJson = reason == null ? "" : ",\"reason\":\"%s\"".formatted(reason);
        return "{\"locationId\":\"%s\",\"articleId\":\"%s\",\"counted\":%s%s}"
                .formatted(store, articleId, counted, reasonJson);
    }

    private Map<String, Object> line() throws Exception {
        var json = mockMvc.perform(get(CLOSING).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return lineIn(json);
    }

    private Map<String, Object> lineIn(String json) {
        List<Map<String, Object>> lines =
                JsonPath.read(json, "$.locations[?(@.code=='STORE')].lines[?(@.articleId=='%s')]".formatted(soap));
        assertThat(lines).hasSize(1);
        return lines.getFirst();
    }

    private static double number(Map<String, Object> line, String field) {
        return ((Number) line.get(field)).doubleValue();
    }

    private int savedRows() {
        var rows = jdbcTemplate.queryForObject("select count(*) from day_closing_counts", Integer.class);
        return rows == null ? 0 : rows;
    }

    /** Opens today's day if no other test class did (201), tolerates the one already open (409). */
    private void openToday() throws Exception {
        var status = mockMvc.perform(post("/api/v1/days")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[]}"))
                .andReturn()
                .getResponse()
                .getStatus();
        assertThat(status).isIn(201, 409);
    }
}
