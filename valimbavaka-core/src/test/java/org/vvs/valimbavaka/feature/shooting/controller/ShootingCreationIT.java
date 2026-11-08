package org.vvs.valimbavaka.feature.shooting.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.user.domain.RoleName;

/** SHO-01: the exit lowers the stock of its location and has status OUT, all or nothing. */
class ShootingCreationIT extends ShootingApiTestSupport {

    private static final String ITEM = "{\"articleId\":\"%s\",\"quantity\":%s}";

    @Test
    void createShooting_withSellerRole_decreasesStoreStockAndReturnsOut() throws Exception {
        // Arrange
        var articleId = createArticle();
        receive(articleId, "STORE", "10");
        var token = tokenOf(RoleName.SELLER);

        // Act + Assert
        postShooting(token, shootingBody(articleId, "3"))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.number").value(matchesPattern("SHO-\\d{4,}")))
                .andExpect(jsonPath("$.status").value("OUT"))
                .andExpect(jsonPath("$.late").value(false))
                .andExpect(jsonPath("$.takenBy").value("Miora Andria"))
                .andExpect(jsonPath("$.items[0].articleId").value(articleId.toString()))
                .andExpect(jsonPath("$.items[0].quantity").value(3));
        assertThat(balance(articleId, "STORE")).isEqualByComparingTo("7");
    }

    @Test
    void createShooting_validExit_writesReferencedShootingMovementAndJournalEntry() throws Exception {
        // Arrange
        var articleId = createArticle();
        receive(articleId, "STORE", "4");

        // Act
        var response = postShooting(adminToken(), shootingBody(articleId, "1.5"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert
        var shootingId = UUID.fromString(JsonPath.read(response, "$.id"));
        var movements = jdbcTemplate.queryForObject(
                "select count(*) from stock_movements where type = 'SHOOTING' and reference_id = ?"
                        + " and quantity = -1.5",
                Integer.class,
                shootingId);
        var journal = jdbcTemplate.queryForObject(
                "select count(*) from journal_entries where resource_type = 'SHOOTING' and resource_id = ?"
                        + " and action = 'CREATE'",
                Integer.class,
                shootingId);
        assertThat(movements).isEqualTo(1);
        assertThat(journal).isEqualTo(1);
    }

    @Test
    void createShooting_oneArticleAboveStock_returns409AndChangesNothing() throws Exception {
        // Arrange
        var soap = createArticle();
        var scrub = createArticle();
        receive(soap, "STORE", "5");
        receive(scrub, "STORE", "1");
        var items = ITEM.formatted(soap, 2) + "," + ITEM.formatted(scrub, 3);

        // Act + Assert
        postShooting(adminToken(), shootingBody(location("STORE"), tomorrow(), items))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
        assertThat(balance(soap, "STORE")).isEqualByComparingTo("5");
        assertThat(balance(scrub, "STORE")).isEqualByComparingTo("1");
        var lines = jdbcTemplate.queryForObject(
                "select count(*) from shooting_items where article_id in (?, ?)", Integer.class, soap, scrub);
        assertThat(lines).isZero();
    }

    @Test
    void createShooting_noStockInTheLocation_returns409() throws Exception {
        // Arrange
        var articleId = createArticle();
        receive(articleId, "WAREHOUSE", "10");

        // Act + Assert
        postShooting(adminToken(), shootingBody(articleId, "1")).andExpect(status().isConflict());
        assertThat(balance(articleId, "WAREHOUSE")).isEqualByComparingTo("10");
    }

    @Test
    void createShooting_unknownArticle_returns404() throws Exception {
        postShooting(adminToken(), shootingBody(UUID.randomUUID(), "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void createShooting_expectedReturnInThePast_returns422() throws Exception {
        // Arrange
        var articleId = createArticle();
        receive(articleId, "STORE", "2");
        var past = Instant.now().minus(1, ChronoUnit.HOURS);
        var body = shootingBody(location("STORE"), past, ITEM.formatted(articleId, 1));

        // Act + Assert
        postShooting(adminToken(), body)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        assertThat(balance(articleId, "STORE")).isEqualByComparingTo("2");
    }

    @Test
    void createShooting_withoutItems_returns422() throws Exception {
        postShooting(adminToken(), shootingBody(location("STORE"), tomorrow(), ""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createShooting_zeroQuantity_returns422() throws Exception {
        postShooting(adminToken(), shootingBody(createArticle(), "0"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createShooting_sameArticleTwice_returns422() throws Exception {
        // Arrange
        var articleId = createArticle();
        receive(articleId, "STORE", "5");
        var items = ITEM.formatted(articleId, 1) + "," + ITEM.formatted(articleId, 1);

        // Act + Assert
        postShooting(adminToken(), shootingBody(location("STORE"), tomorrow(), items))
                .andExpect(status().isUnprocessableContent());
        assertThat(balance(articleId, "STORE")).isEqualByComparingTo("5");
    }

    @Test
    void getShootings_afterCreation_listsTheExitAsOut() throws Exception {
        // Arrange
        var articleId = createArticle();
        receive(articleId, "STORE", "3");
        var token = adminToken();
        var response = postShooting(token, shootingBody(articleId, "1"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String number = JsonPath.read(response, "$.number");

        // Act + Assert
        mockMvc.perform(get(SHOOTINGS)
                        .param("status", "OUT")
                        .param("search", number)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].number").value(number))
                .andExpect(jsonPath("$.items[0].status").value("OUT"));
    }

    @Test
    void getShootingById_unknownId_returns404() throws Exception {
        mockMvc.perform(get(SHOOTINGS + "/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
