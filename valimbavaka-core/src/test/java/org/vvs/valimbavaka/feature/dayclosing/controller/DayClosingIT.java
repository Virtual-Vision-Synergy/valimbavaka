package org.vvs.valimbavaka.feature.dayclosing.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
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
 * JR-04: closing reconciliation of today, administrator only. The database is shared with the other test classes:
 * every test works on its own article, so its line is exact whatever the others recorded today.
 */
class DayClosingIT extends StockApiTestSupport {

    private static final String CLOSING = "/api/v1/days/closing";
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
    void openTodayCreateSellerAndArticle() throws Exception {
        endAutomaticPromotions();
        openToday();
        var email = "vendeuse-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(RoleName.SELLER).orElseThrow();
        var terms = new SellerTerms(channel("STORE"), new BigDecimal("5.00"));
        seller = userRepository.save(User.create(
                new UserProfile(email, "Aaron", "Rakoto"), passwordEncoder.encode("mot-de-passe-long-1"), role, terms));
        soap = createArticle(uniqueName());
        move(soap, "WAREHOUSE", StockMovementType.RECEIPT, "200");
    }

    @Test
    void getClosing_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(CLOSING))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getClosing_sellerAndDeliveryDriver_return403() throws Exception {
        mockMvc.perform(get(CLOSING).header(HttpHeaders.AUTHORIZATION, bearer(tokenOf(RoleName.SELLER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(get(CLOSING).header(HttpHeaders.AUTHORIZATION, bearer(tokenOf(RoleName.DELIVERY_DRIVER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getClosing_dayNotOpened_returns409() throws Exception {
        // Arrange
        jdbcTemplate.update("delete from business_day_transfers");
        jdbcTemplate.update("delete from business_days");

        // Act + Assert
        try {
            closing()
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLICT"))
                    .andExpect(jsonPath("$.detail").value(startsWith("La journée du ")));
        } finally {
            openToday();
        }
    }

    @Test
    void getClosing_openDay_listsStoreDeliveryAndSalonButNotTheWarehouse() throws Exception {
        closing()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessDate")
                        .value(LocalDate.now(BUSINESS_ZONE).toString()))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.locations[*].code").value(List.of("STORE", "DELIVERY", "TRADE_SHOW")));
    }

    @Test
    void getClosing_storeActivity_showsTransferredSoldReturnedAndShooting() throws Exception {
        // Arrange
        transfer(soap, "STORE", "30");
        createSale("5");
        var returned = shootingOf("2");
        shootingOf("1");
        returnShooting(returned);

        // Act
        var line = lineOf(closingJson(), "STORE");

        // Assert
        assertThat(number(line, "opening")).isZero();
        assertThat(number(line, "transferred")).isEqualTo(30.0);
        assertThat(number(line, "returned")).isEqualTo(2.0);
        assertThat(number(line, "sold")).isEqualTo(5.0);
        assertThat(number(line, "delivered")).isZero();
        assertThat(number(line, "salon")).isZero();
        assertThat(number(line, "shooting")).isEqualTo(3.0);
        assertThat(number(line, "theoretical")).isEqualTo(24.0);
        assertThat(number(line, "systemStock")).isEqualTo(24.0);
        assertThat(number(line, "gap")).isZero();
        assertThat(line.get("reference")).isNotNull();
    }

    @Test
    void getClosing_saleInTheDeliveryLocationAndDeliveryMovement_areDelivered() throws Exception {
        // Arrange
        transfer(soap, "DELIVERY", "10");
        move(soap, "DELIVERY", StockMovementType.SALE, "-4");
        move(soap, "DELIVERY", StockMovementType.DELIVERY, "-1");

        // Act
        var line = lineOf(closingJson(), "DELIVERY");

        // Assert
        assertThat(number(line, "transferred")).isEqualTo(10.0);
        assertThat(number(line, "delivered")).isEqualTo(5.0);
        assertThat(number(line, "sold")).isZero();
        assertThat(number(line, "theoretical")).isEqualTo(5.0);
        assertThat(number(line, "gap")).isZero();
    }

    @Test
    void getClosing_salonConsumption_showsTheSalonQuantity() throws Exception {
        // Arrange: a performed service takes its products out of the salon with a TRADE_SHOW movement (SRV-02)
        transfer(soap, "TRADE_SHOW", "6");
        move(soap, "TRADE_SHOW", StockMovementType.TRADE_SHOW, "-3");

        // Act
        var line = lineOf(closingJson(), "TRADE_SHOW");

        // Assert
        assertThat(number(line, "transferred")).isEqualTo(6.0);
        assertThat(number(line, "salon")).isEqualTo(3.0);
        assertThat(number(line, "theoretical")).isEqualTo(3.0);
    }

    @Test
    void getClosing_stockLeftByAnEarlierDay_isTheOpeningAndNotAMovementOfToday() throws Exception {
        // Arrange
        moveAt(soap, "STORE", "7", Instant.now().minus(2, ChronoUnit.DAYS));
        transfer(soap, "STORE", "3");

        // Act
        var line = lineOf(closingJson(), "STORE");

        // Assert
        assertThat(number(line, "opening")).isEqualTo(7.0);
        assertThat(number(line, "transferred")).isEqualTo(3.0);
        assertThat(number(line, "theoretical")).isEqualTo(10.0);
        assertThat(number(line, "systemStock")).isEqualTo(10.0);
        assertThat(number(line, "gap")).isZero();
    }

    @Test
    void getClosing_transferBackToTheWarehouse_lowersTheTransferredQuantity() throws Exception {
        // Arrange
        transfer(soap, "STORE", "30");
        move(soap, "STORE", StockMovementType.TRANSFER, "-4");
        move(soap, "WAREHOUSE", StockMovementType.TRANSFER, "4");

        // Act
        var line = lineOf(closingJson(), "STORE");

        // Assert
        assertThat(number(line, "transferred")).isEqualTo(26.0);
        assertThat(number(line, "theoretical")).isEqualTo(26.0);
    }

    @Test
    void getClosing_inventoryAdjustment_showsAGapBetweenTheoreticalAndStock() throws Exception {
        // Arrange
        transfer(soap, "STORE", "10");
        move(soap, "STORE", StockMovementType.INVENTORY, "-2");

        // Act
        var line = lineOf(closingJson(), "STORE");

        // Assert
        assertThat(number(line, "theoretical")).isEqualTo(10.0);
        assertThat(number(line, "systemStock")).isEqualTo(8.0);
        assertThat(number(line, "gap")).isEqualTo(-2.0);
    }

    @Test
    void getClosing_articleOnlyInTheWarehouse_hasNoLineInAnyLocation() throws Exception {
        // Act
        var json = closingJson();

        // Assert
        for (var code : List.of("STORE", "DELIVERY", "TRADE_SHOW")) {
            assertThat(linesOf(json, code)).isEmpty();
        }
    }

    @Test
    void getClosing_dateParameter_isIgnoredAndTodayIsReconciled() throws Exception {
        // Act + Assert
        mockMvc.perform(get(CLOSING)
                        .param("date", "2020-01-01")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessDate")
                        .value(LocalDate.now(BUSINESS_ZONE).toString()));
    }

    private ResultActions closing() throws Exception {
        return mockMvc.perform(get(CLOSING).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())));
    }

    private String closingJson() throws Exception {
        return closing().andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private List<Map<String, Object>> linesOf(String json, String code) {
        var path = "$.locations[?(@.code=='%s')].lines[?(@.articleId=='%s')]".formatted(code, soap);
        return JsonPath.read(json, path);
    }

    private Map<String, Object> lineOf(String json, String code) {
        var lines = linesOf(json, code);
        assertThat(lines).hasSize(1);
        return lines.getFirst();
    }

    private static double number(Map<String, Object> line, String field) {
        return ((Number) line.get(field)).doubleValue();
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

    /** Morning transfer: the stock leaves the warehouse (the movements of TRF-03, written directly here). */
    private void transfer(UUID articleId, String toLocation, String quantity) {
        move(articleId, "WAREHOUSE", StockMovementType.TRANSFER, "-" + quantity);
        move(articleId, toLocation, StockMovementType.TRANSFER, quantity);
    }

    /** A movement dated in the past: the append-only table accepts the insert, not an update. */
    private void moveAt(UUID articleId, String locationCode, String quantity, Instant at) {
        jdbcTemplate.update(
                "insert into stock_movements (id, article_id, location_id, type, quantity, occurred_at) "
                        + "values (?, ?, ?, 'TRANSFER', ?, ?)",
                UUID.randomUUID(),
                articleId,
                location(locationCode),
                new BigDecimal(quantity),
                Timestamp.from(at));
    }

    private void createSale(String quantity) throws Exception {
        var line = "{\"articleId\":\"%s\",\"quantity\":%s}".formatted(soap, quantity);
        var body = "{\"salesChannelId\":\"%s\",\"sellerId\":\"%s\",\"paymentMethod\":\"CASH\",\"lines\":[%s]}"
                .formatted(channel("STORE"), seller.getId(), line);
        mockMvc.perform(post("/api/v1/sales")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    /** Shooting exit from the store; returns the id of the new exit. */
    private String shootingOf(String quantity) throws Exception {
        var body = """
                {"locationId":"%s","items":[{"articleId":"%s","quantity":%s}],"expectedReturnAt":"%s"}
                """.formatted(location("STORE"), soap, quantity, Instant.now().plus(1, ChronoUnit.DAYS));
        var json = mockMvc.perform(post("/api/v1/shootings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private void returnShooting(String shootingId) throws Exception {
        mockMvc.perform(post("/api/v1/shootings/" + shootingId + "/return")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk());
    }

    private UUID channel(String code) {
        return jdbcTemplate.queryForObject("select id from sales_channels where code = ?", UUID.class, code);
    }
}
