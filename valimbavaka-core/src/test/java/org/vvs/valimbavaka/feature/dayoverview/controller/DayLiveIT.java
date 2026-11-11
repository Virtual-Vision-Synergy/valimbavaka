package org.vvs.valimbavaka.feature.dayoverview.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

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
 * JR-03: live view of today (sales, deliveries, salon services, feed), administrator only. The database is shared
 * with the other test classes: every test reads the view before and after its own operations and compares.
 */
class DayLiveIT extends StockApiTestSupport {

    private static final String LIVE = "/api/v1/days/live";
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

    @Test
    void getLive_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(LIVE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getLive_sellerAndDeliveryDriver_return403() throws Exception {
        mockMvc.perform(get(LIVE).header(HttpHeaders.AUTHORIZATION, bearer(tokenOf(RoleName.SELLER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(get(LIVE).header(HttpHeaders.AUTHORIZATION, bearer(tokenOf(RoleName.DELIVERY_DRIVER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getLive_administrator_returnsTodayAndOpenStatus() throws Exception {
        live().andExpect(status().isOk())
                .andExpect(jsonPath("$.businessDate").value(LocalDate.now(BUSINESS_ZONE).toString()))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.generatedAt").isNotEmpty())
                .andExpect(jsonPath("$.sales.channels").isArray())
                .andExpect(jsonPath("$.feed").isArray());
    }

    @Test
    void getLive_newSale_raisesTheSalesIndicatorsAndTopsTheFeed() throws Exception {
        // Arrange
        var before = snapshot();

        // Act
        var number = createSale("2");
        var after = snapshot();

        // Assert
        assertThat(after.salesCount() - before.salesCount()).isEqualTo(1);
        assertThat(after.revenue() - before.revenue()).isEqualTo(17000.0);
        var feed = after.json();
        assertThat(JsonPath.<String>read(feed, "$.feed[0].reference")).isEqualTo(number);
        assertThat(JsonPath.<String>read(feed, "$.feed[0].type")).isEqualTo("SALE");
        assertThat(JsonPath.<String>read(feed, "$.feed[0].status")).isEqualTo("VALIDATED");
        assertThat(JsonPath.<String>read(feed, "$.feed[0].actorName")).isEqualTo("Miora Rakoto");
    }

    @Test
    void getLive_cancelledSale_staysInTheFeedButLeavesTheIndicators() throws Exception {
        // Arrange
        var number = createSale("1");
        var before = snapshot();

        // Act
        jdbcTemplate.update("update sales set status = 'CANCELLED' where number = ?", number);
        var after = snapshot();

        // Assert
        assertThat(before.salesCount() - after.salesCount()).isEqualTo(1);
        assertThat(before.revenue() - after.revenue()).isEqualTo(8500.0);
        var path = "$.feed[?(@.reference=='%s')].status".formatted(number);
        var statuses = JsonPath.<List<String>>read(after.json(), path);
        assertThat(statuses).containsExactly("CANCELLED");
    }

    @Test
    void getLive_ordersToday_countsOrdersPerStatus() throws Exception {
        // Arrange
        var before = snapshot();
        var driver = createDriver();
        var first = createOrder(driver, "1");
        createOrder(driver, "1");
        jdbcTemplate.update("update delivery_orders set status = 'ON_ROUND' where id = ?", first);

        // Act
        var after = snapshot();

        // Assert
        assertThat(after.deliveriesTotal() - before.deliveriesTotal()).isEqualTo(2);
        assertThat(after.onRound() - before.onRound()).isEqualTo(1);
        assertThat(after.toPrepare() - before.toPrepare()).isEqualTo(1);
        var types = JsonPath.<List<String>>read(after.json(), "$.feed[?(@.type=='DELIVERY')].type");
        assertThat(types).isNotEmpty();
    }

    @Test
    void getLive_orderOfAnotherDay_isNotCounted() throws Exception {
        // Arrange
        var before = snapshot();
        var driver = createDriver();
        var tomorrow = LocalDate.now(BUSINESS_ZONE).plusDays(1);
        createOrderOn(driver, tomorrow, "1");

        // Act
        var after = snapshot();

        // Assert
        assertThat(after.deliveriesTotal()).isEqualTo(before.deliveriesTotal());
    }

    @Test
    void getLive_performedService_raisesTheServicesIndicatorsAndShowsInTheFeed() throws Exception {
        // Arrange
        var before = snapshot();
        var serviceBody = "{\"name\":\"Soin-%s\",\"durationMinutes\":45,\"price\":35000,\"products\":[]}"
                .formatted(UUID.randomUUID());
        var serviceId = idOf(post("/api/v1/salon-services").content(serviceBody));

        // Act
        var recordBody = "{\"salonServiceId\":\"%s\",\"customerName\":\"Hanta\",\"products\":[]}".formatted(serviceId);
        var number = numberOf(post("/api/v1/service-records").content(recordBody));
        var after = snapshot();

        // Assert
        assertThat(after.servicesCount() - before.servicesCount()).isEqualTo(1);
        assertThat(after.servicesRevenue() - before.servicesRevenue()).isEqualTo(35000.0);
        var path = "$.feed[?(@.reference=='%s')].type".formatted(number);
        var types = JsonPath.<List<String>>read(after.json(), path);
        assertThat(types).containsExactly("SERVICE");
    }

    @Test
    void getLive_manyOperations_returnsAtMostThirtyFeedItemsMostRecentFirst() throws Exception {
        // Arrange
        for (var index = 0; index < 3; index++) {
            createSale("1");
        }

        // Act
        var json = snapshot().json();

        // Assert
        List<String> times = JsonPath.read(json, "$.feed[*].occurredAt");
        assertThat(times).hasSizeLessThanOrEqualTo(30);
        var instants = times.stream().map(Instant::parse).toList();
        for (var index = 1; index < instants.size(); index++) {
            assertThat(instants.get(index - 1)).isAfterOrEqualTo(instants.get(index));
        }
    }

    private ResultActions live() throws Exception {
        return mockMvc.perform(get(LIVE).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())));
    }

    private Snapshot snapshot() throws Exception {
        var json = live().andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return Snapshot.of(json);
    }

    private String createSale(String quantity) throws Exception {
        var line = "{\"articleId\":\"%s\",\"quantity\":%s}".formatted(soap, quantity);
        var body = "{\"salesChannelId\":\"%s\",\"sellerId\":\"%s\",\"paymentMethod\":\"CASH\",\"lines\":[%s]}"
                .formatted(channel("STORE"), seller.getId(), line);
        return numberOf(post("/api/v1/sales").content(body));
    }

    private UUID createOrder(UUID driver, String quantity) throws Exception {
        return createOrderOn(driver, LocalDate.now(BUSINESS_ZONE), quantity);
    }

    private UUID createOrderOn(UUID driver, LocalDate day, String quantity) throws Exception {
        var body = """
                {"customerName":"Rakoto Be","customerPhone":"034 12 345 67","address":"Lot II J 45, rue Pasteur",
                 "district":"Ivandry","driverId":"%s","scheduledDate":"%s",
                 "lines":[{"articleId":"%s","quantity":%s}]}
                """.formatted(driver, day, soap, quantity);
        return idOf(post("/api/v1/deliveries").content(body));
    }

    private UUID createDriver() {
        var email = "livreur-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(RoleName.DELIVERY_DRIVER).orElseThrow();
        var driver = User.create(new UserProfile(email, "Tojo", "Nirina"), passwordEncoder.encode(PASSWORD), role);
        return userRepository.save(driver).getId();
    }

    private UUID idOf(MockHttpServletRequestBuilder request)
            throws Exception {
        return UUID.fromString(JsonPath.read(created(request), "$.id"));
    }

    private String numberOf(MockHttpServletRequestBuilder request)
            throws Exception {
        return JsonPath.read(created(request), "$.number");
    }

    private String created(MockHttpServletRequestBuilder request)
            throws Exception {
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

    /** Indicators of one reading of the live view. */
    private record Snapshot(
            String json,
            long salesCount,
            double revenue,
            long deliveriesTotal,
            long onRound,
            long toPrepare,
            long servicesCount,
            double servicesRevenue) {

        static Snapshot of(String json) {
            return new Snapshot(
                    json,
                    longAt(json, "$.sales.count"),
                    doubleAt(json, "$.sales.revenue"),
                    longAt(json, "$.deliveries.total"),
                    longAt(json, "$.deliveries.onRound"),
                    longAt(json, "$.deliveries.toPrepare"),
                    longAt(json, "$.services.count"),
                    doubleAt(json, "$.services.revenue"));
        }

        private static long longAt(String json, String path) {
            return JsonPath.<Number>read(json, path).longValue();
        }

        private static double doubleAt(String json, String path) {
            return JsonPath.<Number>read(json, path).doubleValue();
        }
    }
}
