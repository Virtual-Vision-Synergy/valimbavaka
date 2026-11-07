package org.vvs.valimbavaka.feature.shooting.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.user.domain.RoleName;

/**
 * SHO-03: exits still out, late exits and value at cost they tie up, reserved to shootings.track (administrator).
 * The database is shared with the other integration tests, so the assertions compare the indicators before and after.
 */
class ShootingSummaryIT extends ShootingApiTestSupport {

    private static final String SUMMARY = SHOOTINGS + "/summary";

    @Test
    void getSummary_exitRecorded_addsOneOutExitAndItsValueAtCost() throws Exception {
        // Arrange
        var articleId = createArticle();
        receive(articleId, "STORE", "5");
        var before = summary();

        // Act
        createShooting(shootingBody(articleId, "2"));
        var after = summary();

        // Assert
        assertThat(delta(before, after, "outCount")).isEqualByComparingTo("1");
        assertThat(delta(before, after, "lateCount")).isEqualByComparingTo("0");
        assertThat(delta(before, after, "immobilizedValue")).isEqualByComparingTo("6241.00");
        assertThat(delta(before, after, "unvaluedQuantity")).isEqualByComparingTo("0");
    }

    @Test
    void getSummary_exitPastItsExpectedReturn_isCountedLate() throws Exception {
        // Arrange
        var articleId = createArticle();
        receive(articleId, "STORE", "1");
        var shootingId = createShooting(shootingBody(articleId, "1"));
        var before = summary();
        jdbcTemplate.update(
                "update shootings set expected_return_at = now() - interval '1 hour' where id = ?", shootingId);

        // Act
        var after = summary();

        // Assert
        assertThat(delta(before, after, "lateCount")).isEqualByComparingTo("1");
        assertThat(delta(before, after, "outCount")).isEqualByComparingTo("0");
        assertThat(delta(before, after, "immobilizedValue")).isEqualByComparingTo("0");
    }

    @Test
    void getSummary_afterReturn_removesTheExitAndItsValue() throws Exception {
        // Arrange
        var articleId = createArticle();
        receive(articleId, "STORE", "3");
        var shootingId = createShooting(shootingBody(articleId, "3"));
        var before = summary();

        // Act
        postReturn(adminToken(), shootingId).andExpect(status().isOk());
        var after = summary();

        // Assert
        assertThat(delta(before, after, "outCount")).isEqualByComparingTo("-1");
        assertThat(delta(before, after, "immobilizedValue")).isEqualByComparingTo("-9361.50");
    }

    @Test
    void getSummary_articleWithoutCost_isCountedAsUnvaluedQuantityOnly() throws Exception {
        // Arrange
        var articleId = createArticle("null");
        receive(articleId, "STORE", "4");
        var before = summary();

        // Act
        createShooting(shootingBody(articleId, "2.5"));
        var after = summary();

        // Assert
        assertThat(delta(before, after, "outCount")).isEqualByComparingTo("1");
        assertThat(delta(before, after, "unvaluedQuantity")).isEqualByComparingTo("2.5");
        assertThat(delta(before, after, "immobilizedValue")).isEqualByComparingTo("0");
    }

    @Test
    void getSummary_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(SUMMARY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getSummary_withSellerRole_returns403() throws Exception {
        // Arrange
        var token = tokenOf(RoleName.SELLER);

        // Act + Assert
        mockMvc.perform(get(SUMMARY).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getSummary_withDeliveryDriverRole_returns403() throws Exception {
        // Arrange
        var token = tokenOf(RoleName.DELIVERY_DRIVER);

        // Act + Assert
        mockMvc.perform(get(SUMMARY).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private String summary() throws Exception {
        return mockMvc.perform(get(SUMMARY).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private static BigDecimal delta(String before, String after, String field) {
        return value(after, field).subtract(value(before, field));
    }

    private static BigDecimal value(String json, String field) {
        return new BigDecimal(JsonPath.read(json, "$." + field).toString());
    }
}
