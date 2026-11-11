package org.vvs.valimbavaka.feature.report.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * RPT-01: daily, monthly and yearly report filtered by channel and range, administrator only. The database is shared
 * with other test classes: every assertion is a difference between two readings of the same report.
 */
class ReportIT extends StockApiTestSupport {

    private static final String REPORTS = "/api/v1/reports";
    private static final String MONTH = "?granularity=MONTH";

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User seller;
    private UUID soap;
    private UUID cream;
    private UUID soapRange;
    private UUID creamRange;

    /** A sale needs today's day opened (JR-01); another test class may have removed it. */
    @BeforeEach
    void openTodayCreateSellerAndTwoArticlesOfTwoRanges() throws Exception {
        endAutomaticPromotions();
        var status = mockMvc.perform(post("/api/v1/days")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[]}"))
                .andReturn()
                .getResponse()
                .getStatus();
        assertThat(status).isIn(201, 409);
        var email = "vendeuse-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(RoleName.SELLER).orElseThrow();
        var terms = new SellerTerms(channel("STORE"), new BigDecimal("5.00"));
        seller = userRepository.save(User.create(
                new UserProfile(email, "Miora", "Rakoto"), passwordEncoder.encode("mot-de-passe-long-1"), role, terms));
        var ranges = jdbcTemplate.queryForList("select id from product_ranges order by code limit 2", UUID.class);
        soapRange = ranges.get(0);
        creamRange = ranges.get(1);
        soap = articleOfRange(soapRange);
        cream = articleOfRange(creamRange);
    }

    @Test
    void getReport_monthWithoutFilter_sumsEveryValidatedSale() throws Exception {
        // Arrange
        var revenueBefore = revenue(MONTH);
        var countBefore = salesCount(MONTH);
        createSale("STORE", soap, "1");
        createSale("FACEBOOK", cream, "2");

        // Act + Assert
        assertThat(revenue(MONTH) - revenueBefore).isEqualTo(25500.0);
        assertThat(salesCount(MONTH) - countBefore).isEqualTo(2);
    }

    @Test
    void getReport_channelFilter_countsOnlyThatChannel() throws Exception {
        // Arrange
        var filter = MONTH + "&salesChannelId=" + channel("FACEBOOK");
        var before = revenue(filter);
        var countBefore = salesCount(filter);
        createSale("STORE", soap, "1");
        createSale("FACEBOOK", soap, "2");

        // Act + Assert
        assertThat(revenue(filter) - before).isEqualTo(17000.0);
        assertThat(salesCount(filter) - countBefore).isEqualTo(1);
    }

    @Test
    void getReport_rangeFilter_countsOnlyTheLinesOfThatRange() throws Exception {
        // Arrange
        var soapFilter = MONTH + "&rangeId=" + soapRange;
        var creamFilter = MONTH + "&rangeId=" + creamRange;
        var soapBefore = revenue(soapFilter);
        var creamBefore = revenue(creamFilter);
        var soapCountBefore = salesCount(soapFilter);
        var creamCountBefore = salesCount(creamFilter);
        createSale("STORE", List.of(soap, cream), List.of("1", "2"));

        // Act + Assert
        assertThat(revenue(soapFilter) - soapBefore).isEqualTo(8500.0);
        assertThat(revenue(creamFilter) - creamBefore).isEqualTo(17000.0);
        assertThat(salesCount(soapFilter) - soapCountBefore).isEqualTo(1);
        assertThat(salesCount(creamFilter) - creamCountBefore).isEqualTo(1);
    }

    @Test
    void getReport_channelAndRangeTogether_areBothApplied() throws Exception {
        // Arrange
        var filter = MONTH + "&salesChannelId=" + channel("STORE") + "&rangeId=" + creamRange;
        var before = revenue(filter);
        createSale("STORE", cream, "1");
        createSale("FACEBOOK", cream, "1");
        createSale("STORE", soap, "1");

        // Act + Assert
        assertThat(revenue(filter) - before).isEqualTo(8500.0);
    }

    @Test
    void getReport_dayMonthAndYear_includeTheSameSale() throws Exception {
        // Arrange
        var before = List.of(revenue("?granularity=DAY"), revenue(MONTH), revenue("?granularity=YEAR"));
        createSale("STORE", soap, "1");

        // Act + Assert
        assertThat(revenue("?granularity=DAY") - before.get(0)).isEqualTo(8500.0);
        assertThat(revenue(MONTH) - before.get(1)).isEqualTo(8500.0);
        assertThat(revenue("?granularity=YEAR") - before.get(2)).isEqualTo(8500.0);
    }

    @Test
    void getReport_withoutParameters_isTheCurrentMonth() throws Exception {
        report(adminToken(), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.granularity").value("MONTH"))
                .andExpect(jsonPath("$.from").isNotEmpty())
                .andExpect(jsonPath("$.to").isNotEmpty())
                .andExpect(jsonPath("$.salesChannelId").doesNotExist());
    }

    @Test
    void getReport_pastPeriodWithoutSale_returnsZeros() throws Exception {
        report(adminToken(), "?granularity=YEAR&date=2000-06-15")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2000-01-01"))
                .andExpect(jsonPath("$.to").value("2000-12-31"))
                .andExpect(jsonPath("$.sales.revenue").value(0.0))
                .andExpect(jsonPath("$.sales.salesCount").value(0))
                .andExpect(jsonPath("$.sales.averageBasket").value(0.0));
    }

    @Test
    void getReport_unknownRange_returnsZeros() throws Exception {
        report(adminToken(), MONTH + "&rangeId=" + UUID.randomUUID())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sales.salesCount").value(0));
    }

    @Test
    void getReport_cancelledSale_isNotCounted() throws Exception {
        // Arrange
        var before = revenue(MONTH);
        var saleId = createSale("STORE", soap, "1");
        jdbcTemplate.update("update sales set status = 'CANCELLED' where id = ?", saleId);

        // Act + Assert
        assertThat(revenue(MONTH)).isEqualTo(before);
    }

    @Test
    void getReport_futureDay_returns422() throws Exception {
        report(adminToken(), "?granularity=DAY&date=2999-01-01")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void getReport_unknownGranularity_returns400() throws Exception {
        report(adminToken(), "?granularity=WEEK")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void getReport_malformedDateOrId_returns400() throws Exception {
        report(adminToken(), "?date=01-11-2026").andExpect(status().isBadRequest());
        report(adminToken(), "?rangeId=not-a-uuid").andExpect(status().isBadRequest());
    }

    @Test
    void getReport_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(REPORTS))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getReport_sellerAndDeliveryDriver_return403() throws Exception {
        report(tokenOf(RoleName.SELLER), "")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        report(tokenOf(RoleName.DELIVERY_DRIVER), "")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private ResultActions report(String token, String parameters) throws Exception {
        return mockMvc.perform(get(REPORTS + parameters).header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private double revenue(String parameters) throws Exception {
        return ((Number) read(parameters, "$.sales.revenue")).doubleValue();
    }

    private long salesCount(String parameters) throws Exception {
        return ((Number) read(parameters, "$.sales.salesCount")).longValue();
    }

    private Object read(String parameters, String path) throws Exception {
        var json = report(adminToken(), parameters)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(json, path);
    }

    /** A finished article (price 8500) of the given range, with stock in the store. */
    private UUID articleOfRange(UUID rangeId) throws Exception {
        var article = createArticle(uniqueName());
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
