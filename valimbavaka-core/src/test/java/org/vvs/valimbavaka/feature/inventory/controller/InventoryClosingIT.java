package org.vvs.valimbavaka.feature.inventory.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.domain.SellerTerms;
import org.vvs.valimbavaka.feature.user.domain.User;
import org.vvs.valimbavaka.feature.user.domain.UserProfile;
import org.vvs.valimbavaka.feature.user.repository.RoleRepository;
import org.vvs.valimbavaka.feature.user.repository.UserRepository;
import org.vvs.valimbavaka.support.IntegrationTest;

/**
 * INV-04: closing a session adjusts the stock of the location to the counted quantities, in one transaction, with
 * {@code INVENTORY} movements referencing the session; rights, refusals and journal. The session only holds the two
 * articles of a range created for the test, so that the articles of the other tests are not counted.
 */
@IntegrationTest
@AutoConfigureMockMvc
class InventoryClosingIT {

    private static final String INVENTORIES = "/api/v1/inventories";
    private static final String ADMIN_EMAIL = "admin.test@valimbavaka.mg";
    private static final String ADMIN_PASSWORD = "mot-de-passe-de-test-tres-long";
    private static final String PASSWORD = "mot-de-passe-vendeuse-long";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private UUID storeId;
    private String sessionId;
    private UUID soapId;
    private UUID oilId;

    @BeforeEach
    void setUp() throws Exception {
        // The database is shared by the tests: no session may stay in progress from another test.
        jdbcTemplate.update("update inventory_sessions set status = 'CANCELLED' where status = 'IN_PROGRESS'");
        adminToken = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        storeId = jdbcTemplate.queryForObject("select id from stock_locations where code = 'STORE'", UUID.class);
        var rangeId = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into product_ranges (id, code, label) values (?, ?, 'Gamme INV-04')",
                rangeId,
                "INV04-" + rangeId.toString().substring(0, 8));
        soapId = createArticle(rangeId);
        oilId = createArticle(rangeId);
        move(soapId, "RECEIPT", "12");
        move(oilId, "RECEIPT", "5");
        var opened = mockMvc.perform(asAdmin(post(INVENTORIES)).content(openingBody(rangeId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lineCount").value(2))
                .andReturn();
        sessionId = JsonPath.read(opened.getResponse().getContentAsString(), "$.id");
    }

    @Test
    void closeSession_countedSession_adjustsStockToCountedQuantitiesAndTracesJournal() throws Exception {
        // Arrange
        count(soapId, "10");
        qualify(soapId, "BREAKAGE");
        count(oilId, "7");
        qualify(oilId, "ENTRY_ERROR");

        // Act + Assert
        mockMvc.perform(asAdmin(post(closeUrl())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId))
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.closedAt").isNotEmpty());
        assertThat(stock(soapId)).isEqualByComparingTo("10");
        assertThat(stock(oilId)).isEqualByComparingTo("7");
        assertThat(inventoryMovement(soapId)).isEqualByComparingTo("-2");
        assertThat(inventoryMovement(oilId)).isEqualByComparingTo("2");
        mockMvc.perform(asAdmin(get("/api/v1/journal")).param("search", "Clôture de la session d'inventaire"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].action").value("UPDATE"))
                .andExpect(jsonPath("$.items[0].resourceType").value("INVENTORY_SESSION"))
                .andExpect(jsonPath("$.items[0].resourceId").value(sessionId));
    }

    @Test
    void closeSession_lineWithoutGap_createsNoMovementForIt() throws Exception {
        // Arrange
        count(soapId, "12");
        count(oilId, "4");
        qualify(oilId, "LOSS");

        // Act
        mockMvc.perform(asAdmin(post(closeUrl()))).andExpect(status().isOk());

        // Assert
        assertThat(inventoryMovementCount(soapId)).isZero();
        assertThat(stock(soapId)).isEqualByComparingTo("12");
        assertThat(stock(oilId)).isEqualByComparingTo("4");
    }

    @Test
    void closeSession_saleBetweenCountAndClosing_stockStillEqualsCount() throws Exception {
        // Arrange
        count(soapId, "10");
        qualify(soapId, "LOSS");
        count(oilId, "5");
        move(soapId, "SALE", "-1");

        // Act
        mockMvc.perform(asAdmin(post(closeUrl()))).andExpect(status().isOk());

        // Assert
        assertThat(inventoryMovement(soapId)).isEqualByComparingTo("-1");
        assertThat(stock(soapId)).isEqualByComparingTo("10");
    }

    @Test
    void closeSession_lineNotCounted_returns422AndChangesNothing() throws Exception {
        // Arrange
        count(soapId, "10");
        qualify(soapId, "BREAKAGE");

        // Act + Assert
        mockMvc.perform(asAdmin(post(closeUrl())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        assertThat(sessionStatus()).isEqualTo("IN_PROGRESS");
        assertThat(inventoryMovementCount(soapId)).isZero();
        assertThat(stock(soapId)).isEqualByComparingTo("12");
    }

    @Test
    void closeSession_gapWithoutReason_returns422AndChangesNothing() throws Exception {
        // Arrange
        count(soapId, "10");
        count(oilId, "5");

        // Act + Assert
        mockMvc.perform(asAdmin(post(closeUrl())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        assertThat(sessionStatus()).isEqualTo("IN_PROGRESS");
        assertThat(inventoryMovementCount(soapId)).isZero();
    }

    @Test
    void closeSession_alreadyClosed_returns409AndAdjustsOnlyOnce() throws Exception {
        // Arrange
        countAllWithoutGap();
        count(soapId, "11");
        qualify(soapId, "LOSS");
        mockMvc.perform(asAdmin(post(closeUrl()))).andExpect(status().isOk());

        // Act + Assert
        mockMvc.perform(asAdmin(post(closeUrl())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
        assertThat(inventoryMovementCount(soapId)).isEqualTo(1);
        assertThat(stock(soapId)).isEqualByComparingTo("11");
    }

    @Test
    void countLine_afterClosing_returns409() throws Exception {
        // Arrange
        countAllWithoutGap();
        mockMvc.perform(asAdmin(post(closeUrl()))).andExpect(status().isOk());

        // Act + Assert
        mockMvc.perform(asAdmin(put(lineUrl(soapId) + "/count")).content("{\"countedQuantity\":3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void closeSession_forgedBody_isIgnored() throws Exception {
        // Arrange
        countAllWithoutGap();
        var forged = "{\"id\":\"%s\",\"status\":\"CANCELLED\"}".formatted(UUID.randomUUID());

        // Act + Assert
        mockMvc.perform(asAdmin(post(closeUrl())).content(forged))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId))
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void closeSession_unknownSession_returns404() throws Exception {
        mockMvc.perform(asAdmin(post(INVENTORIES + "/" + UUID.randomUUID() + "/close")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void closeSession_withoutToken_returns401() throws Exception {
        mockMvc.perform(post(closeUrl()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void closeSession_withSellerRole_returns403AndChangesNothing() throws Exception {
        // Arrange
        countAllWithoutGap();
        var token = sellerToken();

        // Act + Assert
        mockMvc.perform(post(closeUrl()).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        assertThat(sessionStatus()).isEqualTo("IN_PROGRESS");
    }

    private void countAllWithoutGap() throws Exception {
        count(soapId, "12");
        count(oilId, "5");
    }

    private void count(UUID articleId, String quantity) throws Exception {
        mockMvc.perform(asAdmin(put(lineUrl(articleId) + "/count")).content("{\"countedQuantity\":" + quantity + "}"))
                .andExpect(status().isOk());
    }

    private void qualify(UUID articleId, String reason) throws Exception {
        mockMvc.perform(asAdmin(put(lineUrl(articleId) + "/reason")).content("{\"reason\":\"" + reason + "\"}"))
                .andExpect(status().isOk());
    }

    private String closeUrl() {
        return INVENTORIES + "/" + sessionId + "/close";
    }

    private String lineUrl(UUID articleId) {
        var lineId = jdbcTemplate.queryForObject(
                "select id from inventory_lines where session_id = ? and article_id = ?",
                UUID.class,
                UUID.fromString(sessionId),
                articleId);
        return INVENTORIES + "/" + sessionId + "/lines/" + lineId;
    }

    private void move(UUID articleId, String type, String quantity) {
        jdbcTemplate.update(
                "insert into stock_movements (id, article_id, location_id, type, quantity, occurred_at)"
                        + " values (gen_random_uuid(), ?, ?, ?, ?, now())",
                articleId,
                storeId,
                type,
                new BigDecimal(quantity));
    }

    private BigDecimal stock(UUID articleId) {
        return jdbcTemplate.queryForObject(
                "select coalesce(sum(quantity), 0) from stock_movements where article_id = ? and location_id = ?",
                BigDecimal.class,
                articleId,
                storeId);
    }

    private BigDecimal inventoryMovement(UUID articleId) {
        return jdbcTemplate.queryForObject(
                "select quantity from stock_movements where article_id = ? and location_id = ?"
                        + " and type = 'INVENTORY' and reference_id = ?",
                BigDecimal.class,
                articleId,
                storeId,
                UUID.fromString(sessionId));
    }

    private Integer inventoryMovementCount(UUID articleId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from stock_movements where article_id = ? and type = 'INVENTORY'",
                Integer.class,
                articleId);
    }

    private String sessionStatus() {
        return jdbcTemplate.queryForObject(
                "select status from inventory_sessions where id = ?", String.class, UUID.fromString(sessionId));
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON);
    }

    private String openingBody(UUID rangeId) {
        var tomorrow = LocalDate.now(ZoneId.of("Indian/Antananarivo")).plusDays(1);
        return """
                {"locationId":"%s","scope":"RANGE","rangeId":"%s","responsibleId":"%s","countDate":"%s"}
                """.formatted(storeId, rangeId, adminId(), tomorrow).strip();
    }

    private UUID adminId() {
        return jdbcTemplate.queryForObject("select id from users where email = ?", UUID.class, ADMIN_EMAIL);
    }

    private UUID createArticle(UUID rangeId) throws Exception {
        var body = """
                {"name":"Article-%s","type":"FINISHED","unit":"PIECE","rangeId":"%s","cost":3120.5,"price":8500,
                 "threshold":40}
                """.formatted(UUID.randomUUID(), rangeId);
        var created = mockMvc.perform(asAdmin(post("/api/v1/articles")).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(created.getResponse().getContentAsString(), "$.id"));
    }

    private String sellerToken() throws Exception {
        var email = "seller-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(RoleName.SELLER).orElseThrow();
        var channelId = jdbcTemplate.queryForObject("select id from sales_channels limit 1", UUID.class);
        userRepository.save(User.create(
                new UserProfile(email, "Tiana", "Ranaivoson"),
                passwordEncoder.encode(PASSWORD),
                role,
                new SellerTerms(channelId, new BigDecimal("5.00"))));
        return accessToken(email, PASSWORD);
    }

    private String accessToken(String email, String password) throws Exception {
        var body = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }
}
