package org.vvs.valimbavaka.feature.report.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.stock.controller.StockApiTestSupport;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.domain.SellerTerms;
import org.vvs.valimbavaka.feature.user.domain.User;
import org.vvs.valimbavaka.feature.user.domain.UserProfile;
import org.vvs.valimbavaka.feature.user.repository.RoleRepository;
import org.vvs.valimbavaka.feature.user.repository.UserRepository;

/**
 * RPT-05: ranking of the best-selling articles of the report. The database is shared with the other test classes, so
 * every article of these tests belongs to a product range created here (and deactivated afterwards): the range filter
 * isolates them from the rest of the database.
 */
class ReportRankingIT extends StockApiTestSupport {

    private static final String REPORTS = "/api/v1/reports";
    private static final String MONTH = "?granularity=MONTH";

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User seller;
    private UUID rangeId;
    private String rangeFilter;

    /** Opens today's day (JR-01), creates a seller and an empty product range. */
    @BeforeEach
    void openTodayCreateSellerAndRange() throws Exception {
        endAutomaticPromotions();
        var status = mockMvc.perform(post("/api/v1/days")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[]}"))
                .andReturn()
                .getResponse()
                .getStatus();
        assertThat(status).isIn(201, 409);
        var role = roleRepository.findByName(RoleName.SELLER).orElseThrow();
        var terms = new SellerTerms(channel("STORE"), new BigDecimal("5.00"));
        var email = "vendeuse-" + UUID.randomUUID() + "@valimbavaka.mg";
        seller = userRepository.save(User.create(
                new UserProfile(email, "Miora", "Rakoto"), passwordEncoder.encode("mot-de-passe-long-1"), role, terms));
        rangeId = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into product_ranges (id, code, label) values (?, ?, 'Gamme RPT-05')",
                rangeId,
                "RPT05-" + rangeId.toString().substring(0, 8));
        rangeFilter = MONTH + "&rangeId=" + rangeId;
    }

    @AfterEach
    void deactivateRange() {
        jdbcTemplate.update("update product_ranges set is_active = false where id = ?", rangeId);
    }

    @Test
    void getReport_ranking_sortsByRevenueWithQuantityMarginAndShare() throws Exception {
        // Arrange: 3 x 8500 = 25500, then 8500, then 8500; purchase cost 3120.50 each
        var soap = article("A-savon", "3120.5");
        var oil = article("B-huile", "3120.5");
        var cream = article("C-creme", "3120.5");
        createSale("STORE", List.of(oil, soap, cream), List.of("1", "3", "1"));

        // Act + Assert
        report(rangeFilter)
                .andExpect(jsonPath("$.ranking.size").value(10))
                .andExpect(jsonPath("$.ranking.revenue").value(42500.0))
                .andExpect(jsonPath("$.ranking.items.length()").value(3))
                .andExpect(jsonPath("$.ranking.items[0].articleId").value(soap.toString()))
                .andExpect(jsonPath("$.ranking.items[0].quantity").value(3.0))
                .andExpect(jsonPath("$.ranking.items[0].revenue").value(25500.0))
                .andExpect(jsonPath("$.ranking.items[0].margin").value(16138.5))
                .andExpect(jsonPath("$.ranking.items[0].share").value(60.0))
                .andExpect(jsonPath("$.ranking.items[1].share").value(20.0));
    }

    @Test
    void getReport_ranking_tiedRevenueIsOrderedByName() throws Exception {
        // Arrange: the same 8500 for both, created in the reverse order of their names
        var cream = article("C-creme", "3120.5");
        var oil = article("B-huile", "3120.5");
        createSale("STORE", List.of(cream, oil), List.of("1", "1"));

        // Act + Assert
        report(rangeFilter)
                .andExpect(jsonPath("$.ranking.items[0].articleId").value(oil.toString()))
                .andExpect(jsonPath("$.ranking.items[1].articleId").value(cream.toString()));
    }

    @Test
    void getReport_ranking_channelFilterKeepsOnlyThatChannel() throws Exception {
        // Arrange
        var soap = article("A-savon", "3120.5");
        var oil = article("B-huile", "3120.5");
        createSale("STORE", soap, "1");
        createSale("FACEBOOK", oil, "2");

        // Act + Assert
        report(rangeFilter + "&salesChannelId=" + channel("FACEBOOK"))
                .andExpect(jsonPath("$.ranking.items.length()").value(1))
                .andExpect(jsonPath("$.ranking.items[0].articleId").value(oil.toString()))
                .andExpect(jsonPath("$.ranking.items[0].quantity").value(2.0))
                .andExpect(jsonPath("$.ranking.items[0].share").value(100.0));
        report(rangeFilter + "&salesChannelId=" + channel("STORE"))
                .andExpect(jsonPath("$.ranking.items.length()").value(1))
                .andExpect(jsonPath("$.ranking.items[0].articleId").value(soap.toString()));
    }

    @Test
    void getReport_ranking_rangeFilterKeepsOnlyTheArticlesOfThatRange() throws Exception {
        // Arrange
        var soap = article("A-savon", "3120.5");
        createSale("STORE", soap, "1");
        var otherRange = jdbcTemplate.queryForObject(
                "select id from product_ranges where id <> ? order by code limit 1", UUID.class, rangeId);

        // Act
        var json = reportJson(MONTH + "&rangeId=" + otherRange);

        // Assert
        var found = (List<?>) JsonPath.read(json, "$.ranking.items[?(@.articleId=='" + soap + "')]");
        assertThat(found).isEmpty();
    }

    @Test
    void getReport_ranking_cancelledSaleIsNotCounted() throws Exception {
        // Arrange
        var soap = article("A-savon", "3120.5");
        var oil = article("B-huile", "3120.5");
        createSale("STORE", soap, "1");
        var cancelled = createSale("STORE", oil, "5");
        jdbcTemplate.update("update sales set status = 'CANCELLED' where id = ?", cancelled);

        // Act + Assert
        report(rangeFilter)
                .andExpect(jsonPath("$.ranking.items.length()").value(1))
                .andExpect(jsonPath("$.ranking.items[0].articleId").value(soap.toString()))
                .andExpect(jsonPath("$.ranking.revenue").value(8500.0));
    }

    @Test
    void getReport_ranking_unknownPurchaseCost_hasNoMargin() throws Exception {
        // Arrange
        var soap = article("A-savon", "null");
        createSale("STORE", soap, "1");

        // Act + Assert
        report(rangeFilter)
                .andExpect(jsonPath("$.ranking.items[0].revenue").value(8500.0))
                .andExpect(jsonPath("$.ranking.items[0].margin").value(nullValue()));
    }

    @Test
    void getReport_ranking_isCutAtTenArticlesButTheSharesUseTheWholeTurnover() throws Exception {
        // Arrange: 11 articles at 8500 each
        var articles = new ArrayList<UUID>();
        var quantities = new ArrayList<String>();
        for (var index = 0; index < 11; index++) {
            articles.add(article("A-%02d".formatted(index), "3120.5"));
            quantities.add("1");
        }
        createSale("STORE", articles, quantities);

        // Act + Assert: 10 rows, the share is 8500 / 93500 and not 8500 / 85000
        report(rangeFilter)
                .andExpect(jsonPath("$.ranking.items.length()").value(10))
                .andExpect(jsonPath("$.ranking.revenue").value(93500.0))
                .andExpect(jsonPath("$.ranking.items[0].articleId").value(articles.get(0).toString()))
                .andExpect(jsonPath("$.ranking.items[0].share").value(9.1));
    }

    @Test
    void getReport_ranking_pastPeriodWithoutSale_isEmpty() throws Exception {
        report("?granularity=YEAR&date=2000-06-15&rangeId=" + rangeId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ranking.revenue").value(0.0))
                .andExpect(jsonPath("$.ranking.items.length()").value(0));
    }

    @Test
    void getReport_ranking_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(REPORTS + rangeFilter))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    private ResultActions report(String parameters) throws Exception {
        return mockMvc.perform(get(REPORTS + parameters).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk());
    }

    private String reportJson(String parameters) throws Exception {
        return report(parameters).andReturn().getResponse().getContentAsString();
    }

    /** A finished article (price 8500) of the test range, named {@code prefix-<unique>}, with stock in the store. */
    private UUID article(String prefix, String cost) throws Exception {
        var article = createArticle(prefix + "-" + uniqueName(), cost);
        jdbcTemplate.update("update articles set range_id = ? where id = ?", rangeId, article);
        move(article, "STORE", StockMovementType.RECEIPT, "50");
        return article;
    }

    private UUID createSale(String channelCode, UUID article, String quantity) throws Exception {
        return createSale(channelCode, List.of(article), List.of(quantity));
    }

    private UUID createSale(String channelCode, List<UUID> articles, List<String> quantities) throws Exception {
        var lines = new StringBuilder();
        for (int index = 0; index < articles.size(); index++) {
            var line = "{\"articleId\":\"%s\",\"quantity\":%s}".formatted(articles.get(index), quantities.get(index));
            lines.append(index == 0 ? "" : ",").append(line);
        }
        var body = "{\"salesChannelId\":\"%s\",\"sellerId\":\"%s\",\"paymentMethod\":\"CASH\",\"lines\":[%s]}"
                .formatted(channel(channelCode), seller.getId(), lines);
        var json = mockMvc.perform(post("/api/v1/sales")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private UUID channel(String code) {
        return jdbcTemplate.queryForObject("select id from sales_channels where code = ?", UUID.class, code);
    }
}
