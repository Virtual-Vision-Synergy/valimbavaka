package org.vvs.valimbavaka.feature.report.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
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
 * RPT-01 to RPT-04: daily, monthly and yearly report filtered by channel and range, compared with the previous
 * period, with its turnover curve and its split by range, administrator only. The database is shared with other test
 * classes: every assertion is a difference between two readings of the same report.
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
    void getReport_comparison_isThePreviousPeriodWithItsOwnIndicators() throws Exception {
        // Arrange
        var day = "?granularity=DAY";
        var previousBefore = read(day, "$.comparison.sales.revenue");
        var yesterday = LocalDate.now(ZoneId.of("Indian/Antananarivo")).minusDays(1);

        // Act
        createSale("STORE", soap, "1");

        // Assert: today's sale moves the period, not the day before
        assertThat(read(day, "$.comparison.sales.revenue")).isEqualTo(previousBefore);
        assertThat(read(day, "$.comparison.from")).isEqualTo(yesterday.toString());
        assertThat(read(day, "$.comparison.to")).isEqualTo(yesterday.toString());
    }

    @Test
    void getReport_comparison_variationIsConsistentWithBothPeriods() throws Exception {
        // Arrange
        createSale("STORE", soap, "1");

        // Act
        var json = report(adminToken(), "?granularity=DAY")
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert
        var current = ((Number) JsonPath.read(json, "$.sales.revenue")).doubleValue();
        var previous = ((Number) JsonPath.read(json, "$.comparison.sales.revenue")).doubleValue();
        Object variation = JsonPath.read(json, "$.comparison.variation.revenue");
        if (previous == 0.0) {
            assertThat(variation).isNull();
        } else {
            var expected = (current - previous) * 100 / previous;
            assertThat(((Number) variation).doubleValue()).isCloseTo(expected, within(0.051));
        }
    }

    @Test
    void getReport_periodAndPreviousWithoutSale_hasNoPercentage() throws Exception {
        report(adminToken(), "?granularity=YEAR&date=2000-06-15")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comparison.from").value("1999-01-01"))
                .andExpect(jsonPath("$.comparison.to").value("1999-12-31"))
                .andExpect(jsonPath("$.comparison.sales.revenue").value(0.0))
                .andExpect(jsonPath("$.comparison.sales.salesCount").value(0))
                .andExpect(jsonPath("$.comparison.variation.revenue").value(nullValue()))
                .andExpect(jsonPath("$.comparison.variation.salesCount").value(nullValue()))
                .andExpect(jsonPath("$.comparison.variation.averageBasket").value(nullValue()));
    }

    @Test
    void getReport_comparisonWithFilters_appliesTheSameFiltersToThePreviousPeriod() throws Exception {
        report(adminToken(), "?granularity=MONTH&date=2000-03-10&rangeId=" + UUID.randomUUID())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comparison.from").value("2000-02-01"))
                .andExpect(jsonPath("$.comparison.to").value("2000-02-29"))
                .andExpect(jsonPath("$.comparison.sales.salesCount").value(0));
    }

    @Test
    void getReport_monthTrend_hasOnePointPerDayAndItsSumIsTheRevenue() throws Exception {
        // Arrange
        var today = today();
        var before = trendRevenue(MONTH, today);
        createSale("STORE", soap, "1");
        createSale("FACEBOOK", cream, "2");

        // Act
        var json = reportJson(MONTH);

        // Assert
        assertThat(trendRevenue(MONTH, today) - before).isEqualTo(25500.0);
        assertThat((List<?>) JsonPath.read(json, "$.trend")).hasSize(today.lengthOfMonth());
        assertThat((List<?>) JsonPath.read(json, "$.trend[?(@.date=='" + today + "')]"))
                .hasSize(1);
        assertThat(JsonPath.<String>read(json, "$.trend[0].date"))
                .isEqualTo(today.withDayOfMonth(1).toString());
        assertThat(trendSum(json)).isCloseTo(salesRevenue(json), within(0.01));
    }

    @Test
    void getReport_yearTrend_hasTwelveMonthPointsThatIncludeTheSale() throws Exception {
        // Arrange
        var year = "?granularity=YEAR";
        var firstOfMonth = today().withDayOfMonth(1);
        var before = trendRevenue(year, firstOfMonth);
        createSale("STORE", soap, "1");

        // Act
        var json = reportJson(year);

        // Assert
        assertThat(trendRevenue(year, firstOfMonth) - before).isEqualTo(8500.0);
        assertThat((List<?>) JsonPath.read(json, "$.trend")).hasSize(12);
        assertThat(JsonPath.<String>read(json, "$.trend[0].date")).isEqualTo(today().getYear() + "-01-01");
        assertThat(trendSum(json)).isCloseTo(salesRevenue(json), within(0.01));
    }

    @Test
    void getReport_dayTrend_isASinglePointOfTheDay() throws Exception {
        // Arrange
        var day = "?granularity=DAY";
        var before = trendRevenue(day, today());
        createSale("STORE", soap, "1");

        // Act + Assert
        assertThat((List<?>) JsonPath.read(reportJson(day), "$.trend")).hasSize(1);
        assertThat(trendRevenue(day, today()) - before).isEqualTo(8500.0);
    }

    @Test
    void getReport_trendWithFilters_appliesTheChannelAndTheRange() throws Exception {
        // Arrange
        var today = today();
        var soapFilter = MONTH + "&salesChannelId=" + channel("FACEBOOK") + "&rangeId=" + soapRange;
        var creamFilter = MONTH + "&rangeId=" + creamRange;
        var soapBefore = trendRevenue(soapFilter, today);
        var creamBefore = trendRevenue(creamFilter, today);
        createSale("FACEBOOK", List.of(soap, cream), List.of("1", "2"));
        createSale("STORE", soap, "1");

        // Act + Assert
        assertThat(trendRevenue(soapFilter, today) - soapBefore).isEqualTo(8500.0);
        assertThat(trendRevenue(creamFilter, today) - creamBefore).isEqualTo(17000.0);
    }

    @Test
    void getReport_cancelledSale_isNotInTheTrend() throws Exception {
        // Arrange
        var before = trendRevenue(MONTH, today());
        var saleId = createSale("STORE", soap, "1");
        jdbcTemplate.update("update sales set status = 'CANCELLED' where id = ?", saleId);

        // Act + Assert
        assertThat(trendRevenue(MONTH, today())).isEqualTo(before);
    }

    @Test
    void getReport_pastPeriodWithoutSale_hasAContinuousTrendOfZeros() throws Exception {
        report(adminToken(), "?granularity=MONTH&date=2000-02-10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trend.length()").value(29))
                .andExpect(jsonPath("$.trend[0].date").value("2000-02-01"))
                .andExpect(jsonPath("$.trend[28].date").value("2000-02-29"))
                .andExpect(jsonPath("$.trend[0].revenue").value(0.0))
                .andExpect(jsonPath("$.trend[28].revenue").value(0.0));
        report(adminToken(), "?granularity=YEAR&date=2000-06-15")
                .andExpect(jsonPath("$.trend.length()").value(12))
                .andExpect(jsonPath("$.trend[11].date").value("2000-12-01"))
                .andExpect(jsonPath("$.trend[11].revenue").value(0.0));
    }

    @Test
    void getReport_ranges_splitTheRevenueOfEachRangeAndTheSharesAddUpTo100() throws Exception {
        // Arrange
        var soapBefore = rangeRevenue(MONTH, soapRange);
        var creamBefore = rangeRevenue(MONTH, creamRange);
        var salesBefore = revenue(MONTH);
        var totalBefore = rangesRevenue(reportJson(MONTH));
        createSale("STORE", List.of(soap, cream), List.of("1", "2"));

        // Act
        var json = reportJson(MONTH);

        // Assert: 8500 for the soap range, 17000 for the cream range, the same total as the report
        assertThat(rangeRevenue(MONTH, soapRange) - soapBefore).isEqualTo(8500.0);
        assertThat(rangeRevenue(MONTH, creamRange) - creamBefore).isEqualTo(17000.0);
        assertThat(rangesRevenue(json) - totalBefore).isEqualTo(revenue(MONTH) - salesBefore);
        assertThat(itemsSum(json, "revenue")).isCloseTo(rangesRevenue(json), within(0.01));
        assertThat(itemsSum(json, "share")).isCloseTo(100.0, within(0.0001));
    }

    @Test
    void getReport_ranges_areSortedByDescendingRevenue() throws Exception {
        // Arrange
        createSale("STORE", List.of(soap, cream), List.of("1", "2"));

        // Act
        var values = (List<?>) JsonPath.read(reportJson(MONTH), "$.ranges.items[*].revenue");

        // Assert
        var amounts =
                values.stream().map(value -> ((Number) value).doubleValue()).toList();
        assertThat(amounts).isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    void getReport_ranges_channelFilterAppliesButRangeFilterIsIgnored() throws Exception {
        // Arrange
        var facebook = MONTH + "&salesChannelId=" + channel("FACEBOOK");
        var facebookSoap = facebook + "&rangeId=" + soapRange;
        var soapBefore = rangeRevenue(facebook, soapRange);
        var creamBefore = rangeRevenue(facebookSoap, creamRange);
        createSale("FACEBOOK", List.of(soap, cream), List.of("1", "1"));
        createSale("STORE", soap, "3");

        // Act + Assert: the store sale is out, and the soap filter still shows the cream range
        assertThat(rangeRevenue(facebook, soapRange) - soapBefore).isEqualTo(8500.0);
        assertThat(rangeRevenue(facebookSoap, creamRange) - creamBefore).isEqualTo(8500.0);
    }

    @Test
    void getReport_ranges_cancelledSaleIsNotCounted() throws Exception {
        // Arrange
        var before = rangeRevenue(MONTH, soapRange);
        var saleId = createSale("STORE", soap, "1");
        jdbcTemplate.update("update sales set status = 'CANCELLED' where id = ?", saleId);

        // Act + Assert
        assertThat(rangeRevenue(MONTH, soapRange)).isEqualTo(before);
    }

    @Test
    void getReport_ranges_pastPeriodWithoutSale_isEmptyWithoutDividingByZero() throws Exception {
        report(adminToken(), "?granularity=YEAR&date=2000-06-15")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ranges.revenue").value(0.0))
                .andExpect(jsonPath("$.ranges.items.length()").value(0));
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

    private LocalDate today() {
        return LocalDate.now(ZoneId.of("Indian/Antananarivo"));
    }

    private String reportJson(String parameters) throws Exception {
        return report(adminToken(), parameters)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    /** Turnover of the point of the curve at {@code date}. */
    private double trendRevenue(String parameters, LocalDate date) throws Exception {
        var values = (List<?>) JsonPath.read(reportJson(parameters), "$.trend[?(@.date=='" + date + "')].revenue");
        return ((Number) values.get(0)).doubleValue();
    }

    private double salesRevenue(String json) {
        return ((Number) JsonPath.read(json, "$.sales.revenue")).doubleValue();
    }

    private double trendSum(String json) {
        var values = (List<?>) JsonPath.read(json, "$.trend[*].revenue");
        return values.stream()
                .mapToDouble(value -> ((Number) value).doubleValue())
                .sum();
    }

    /** Turnover of one range in the ranges block, 0 when the range sold nothing (it has no row). */
    private double rangeRevenue(String parameters, UUID rangeId) throws Exception {
        var path = "$.ranges.items[?(@.rangeId=='" + rangeId + "')].revenue";
        var values = (List<?>) JsonPath.read(reportJson(parameters), path);
        return values.isEmpty() ? 0.0 : ((Number) values.get(0)).doubleValue();
    }

    private double rangesRevenue(String json) {
        return ((Number) JsonPath.read(json, "$.ranges.revenue")).doubleValue();
    }

    private double itemsSum(String json, String field) {
        var values = (List<?>) JsonPath.read(json, "$.ranges.items[*]." + field);
        return values.stream()
                .mapToDouble(value -> ((Number) value).doubleValue())
                .sum();
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
