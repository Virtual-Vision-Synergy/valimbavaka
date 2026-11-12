package org.vvs.valimbavaka.feature.dashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

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
 * DSH-01: daily cockpit (turnover, sales, deliveries, stock gap), administrator only; DSH-02: its seven-day turnover
 * trend. The database is shared with the other test classes: every test reads the cockpit before and after its own
 * operations and compares.
 */
class DashboardIT extends StockApiTestSupport {

    private static final String DASHBOARD = "/api/v1/dashboard";
    private static final String TREND = DASHBOARD + "/revenue-trend";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Indian/Antananarivo");

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User seller;
    private UUID soap;

    /** A sale needs today's day opened (JR-01); another test class may have removed it. */
    @BeforeEach
    void openTodayCreateSellerAndStock() throws Exception {
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
        soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "50");
    }

    /** The sale screen reads the first 100 sellers only: the one made here must not stay among them. */
    @AfterEach
    void deactivateSeller() {
        jdbcTemplate.update("update users set is_active = false where id = ?", seller.getId());
    }

    @Test
    void getDashboard_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(DASHBOARD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getDashboard_sellerAndDeliveryDriver_return403() throws Exception {
        mockMvc.perform(get(DASHBOARD).header(HttpHeaders.AUTHORIZATION, bearer(tokenOf(RoleName.SELLER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(get(DASHBOARD).header(HttpHeaders.AUTHORIZATION, bearer(tokenOf(RoleName.DELIVERY_DRIVER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getDashboard_administrator_returnsTodayAndTheFourIndicators() throws Exception {
        dashboard()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessDate")
                        .value(LocalDate.now(BUSINESS_ZONE).toString()))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.generatedAt").isNotEmpty())
                .andExpect(jsonPath("$.revenue.amount").isNumber())
                .andExpect(jsonPath("$.revenue.previousAmount").isNumber())
                .andExpect(jsonPath("$.sales.channels").isArray())
                .andExpect(jsonPath("$.deliveries.total").isNumber())
                .andExpect(jsonPath("$.stockGap.units").isNumber());
    }

    @Test
    void getDashboard_newSale_raisesRevenueAndSalesCount() throws Exception {
        // Arrange
        var before = snapshot();

        // Act
        createSale("2");
        var after = snapshot();

        // Assert
        assertThat(after.salesCount() - before.salesCount()).isEqualTo(1);
        assertThat(after.revenue() - before.revenue()).isEqualTo(17000.0);
        assertThat(after.storeChannelCount() - before.storeChannelCount()).isEqualTo(1);
    }

    @Test
    void getDashboard_cancelledSale_leavesRevenueAndSalesCount() throws Exception {
        // Arrange
        var number = createSale("1");
        var before = snapshot();

        // Act
        jdbcTemplate.update("update sales set status = 'CANCELLED' where number = ?", number);
        var after = snapshot();

        // Assert
        assertThat(before.salesCount() - after.salesCount()).isEqualTo(1);
        assertThat(before.revenue() - after.revenue()).isEqualTo(8500.0);
    }

    @Test
    void getDashboard_ordersToday_countsOnRoundAndPendingButNotCancelledOnesInTheTotal() throws Exception {
        // Arrange
        var before = snapshot();
        var driver = createDriver();
        var onRound = createOrder(driver, "1");
        var cancelled = createOrder(driver, "1");
        createOrder(driver, "1");
        jdbcTemplate.update("update delivery_orders set status = 'ON_ROUND' where id = ?", onRound);
        jdbcTemplate.update("update delivery_orders set status = 'CANCELLED' where id = ?", cancelled);

        // Act
        var after = snapshot();

        // Assert
        assertThat(after.deliveriesTotal() - before.deliveriesTotal()).isEqualTo(2);
        assertThat(after.onRound() - before.onRound()).isEqualTo(1);
        assertThat(after.toPrepare() - before.toPrepare()).isEqualTo(1);
        assertThat(after.cancelled() - before.cancelled()).isEqualTo(1);
    }

    @Test
    void getDashboard_stockEnteredOutsideTheSixComponents_raisesTheStockGap() throws Exception {
        // Arrange
        var before = snapshot();
        var extra = createArticle(uniqueName());

        // Act
        move(extra, "STORE", StockMovementType.RECEIPT, "7");
        var after = snapshot();

        // Assert
        assertThat(after.gapUnits() - before.gapUnits()).isEqualTo(7.0);
        assertThat(after.gapArticles() - before.gapArticles()).isEqualTo(1);
    }

    @Test
    void getRevenueTrend_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(TREND))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getRevenueTrend_sellerAndDeliveryDriver_return403() throws Exception {
        mockMvc.perform(get(TREND).header(HttpHeaders.AUTHORIZATION, bearer(tokenOf(RoleName.SELLER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(get(TREND).header(HttpHeaders.AUTHORIZATION, bearer(tokenOf(RoleName.DELIVERY_DRIVER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getRevenueTrend_administrator_returnsSevenConsecutiveDaysEndingToday() throws Exception {
        var today = LocalDate.now(BUSINESS_ZONE);

        trend().andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value(today.minusDays(6).toString()))
                .andExpect(jsonPath("$.to").value(today.toString()))
                .andExpect(jsonPath("$.points.length()").value(7))
                .andExpect(jsonPath("$.points[0].date").value(today.minusDays(6).toString()))
                .andExpect(jsonPath("$.points[6].date").value(today.toString()))
                .andExpect(jsonPath("$.points[3].revenue").isNumber())
                .andExpect(jsonPath("$.points[3].salesCount").isNumber());
    }

    @Test
    void getRevenueTrend_newSaleToday_raisesOnlyTheLastPoint() throws Exception {
        // Arrange
        var before = trendReading();

        // Act
        createSale("2");
        var after = trendReading();

        // Assert
        assertThat(after.revenues().get(6) - before.revenues().get(6)).isEqualTo(17000.0);
        assertThat(after.counts().get(6) - before.counts().get(6)).isEqualTo(1);
        assertThat(after.revenues().subList(0, 6)).isEqualTo(before.revenues().subList(0, 6));
    }

    @Test
    void getRevenueTrend_cancelledSaleToday_leavesTheLastPoint() throws Exception {
        // Arrange
        var number = createSale("1");
        var before = trendReading();

        // Act
        jdbcTemplate.update("update sales set status = 'CANCELLED' where number = ?", number);
        var after = trendReading();

        // Assert
        assertThat(before.revenues().get(6) - after.revenues().get(6)).isEqualTo(8500.0);
        assertThat(before.counts().get(6) - after.counts().get(6)).isEqualTo(1);
    }

    private ResultActions trend() throws Exception {
        return mockMvc.perform(get(TREND).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())));
    }

    private TrendReading trendReading() throws Exception {
        var json = trend().andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Number> revenues = JsonPath.read(json, "$.points[*].revenue");
        List<Number> counts = JsonPath.read(json, "$.points[*].salesCount");
        return new TrendReading(
                revenues.stream().map(Number::doubleValue).toList(),
                counts.stream().map(Number::longValue).toList());
    }

    /** Revenue and sales count of the seven points of one reading of the trend, oldest first. */
    private record TrendReading(List<Double> revenues, List<Long> counts) {}

    private ResultActions dashboard() throws Exception {
        return mockMvc.perform(get(DASHBOARD).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())));
    }

    private Snapshot snapshot() throws Exception {
        var json =
                dashboard().andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return Snapshot.of(json, channel("STORE"));
    }

    private String createSale(String quantity) throws Exception {
        var line = "{\"articleId\":\"%s\",\"quantity\":%s}".formatted(soap, quantity);
        var body = "{\"salesChannelId\":\"%s\",\"sellerId\":\"%s\",\"paymentMethod\":\"CASH\",\"lines\":[%s]}"
                .formatted(channel("STORE"), seller.getId(), line);
        return JsonPath.read(created(post("/api/v1/sales").content(body)), "$.number");
    }

    private UUID createOrder(UUID driver, String quantity) throws Exception {
        var body = """
                {"customerName":"Rakoto Be","customerPhone":"034 12 345 67","address":"Lot II J 45, rue Pasteur",
                 "district":"Ivandry","driverId":"%s","scheduledDate":"%s",
                 "lines":[{"articleId":"%s","quantity":%s}]}
                """.formatted(driver, LocalDate.now(BUSINESS_ZONE), soap, quantity);
        return UUID.fromString(JsonPath.read(created(post("/api/v1/deliveries").content(body)), "$.id"));
    }

    private UUID createDriver() {
        var email = "livreur-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(RoleName.DELIVERY_DRIVER).orElseThrow();
        var driver = User.create(new UserProfile(email, "Tojo", "Nirina"), passwordEncoder.encode(PASSWORD), role);
        return userRepository.save(driver).getId();
    }

    private String created(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private UUID channel(String code) {
        return jdbcTemplate.queryForObject("select id from sales_channels where code = ?", UUID.class, code);
    }

    /** Indicators of one reading of the cockpit. */
    private record Snapshot(
            long salesCount,
            double revenue,
            long storeChannelCount,
            long deliveriesTotal,
            long onRound,
            long toPrepare,
            long cancelled,
            double gapUnits,
            long gapArticles) {

        static Snapshot of(String json, UUID storeChannel) {
            return new Snapshot(
                    longAt(json, "$.sales.count"),
                    doubleAt(json, "$.revenue.amount"),
                    channelCount(json, storeChannel),
                    longAt(json, "$.deliveries.total"),
                    longAt(json, "$.deliveries.onRound"),
                    longAt(json, "$.deliveries.toPrepare"),
                    longAt(json, "$.deliveries.cancelled"),
                    doubleAt(json, "$.stockGap.units"),
                    longAt(json, "$.stockGap.articles"));
        }

        private static long channelCount(String json, UUID channel) {
            var path = "$.sales.channels[?(@.salesChannelId=='%s')].salesCount".formatted(channel);
            return JsonPath.<List<Number>>read(json, path).get(0).longValue();
        }

        private static long longAt(String json, String path) {
            return JsonPath.<Number>read(json, path).longValue();
        }

        private static double doubleAt(String json, String path) {
            return JsonPath.<Number>read(json, path).doubleValue();
        }
    }
}
