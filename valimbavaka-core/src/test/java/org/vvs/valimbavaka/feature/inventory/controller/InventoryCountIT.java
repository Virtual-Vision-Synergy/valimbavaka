package org.vvs.valimbavaka.feature.inventory.controller;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

/** INV-02: physical count of a line, gap against the theoretical stock frozen at the count, rights and journal. */
@IntegrationTest
@AutoConfigureMockMvc
class InventoryCountIT {

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
    private String sessionId;
    private UUID lineId;

    @BeforeEach
    void setUp() throws Exception {
        // The database is shared by the tests: no session may stay in progress from another test.
        jdbcTemplate.update("update inventory_sessions set status = 'CANCELLED' where status = 'IN_PROGRESS'");
        adminToken = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var articleId = createArticle();
        jdbcTemplate.update(
                "insert into stock_movements (id, article_id, location_id, type, quantity, occurred_at)"
                        + " values (gen_random_uuid(), ?, ?, 'RECEIPT', 12, now())",
                articleId,
                location("STORE"));
        var opened = mockMvc.perform(asAdmin(post(INVENTORIES)).content(openingBody()))
                .andExpect(status().isCreated())
                .andReturn();
        sessionId = JsonPath.read(opened.getResponse().getContentAsString(), "$.id");
        lineId = jdbcTemplate.queryForObject(
                "select id from inventory_lines where session_id = ? and article_id = ?",
                UUID.class,
                UUID.fromString(sessionId),
                articleId);
    }

    @Test
    void countLine_quantityBelowStock_returns200WithNegativeGapAndTracesInJournal() throws Exception {
        // Act + Assert
        mockMvc.perform(asAdmin(put(countUrl())).content(count("10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lineId.toString()))
                .andExpect(jsonPath("$.theoreticalQuantity").value(12.0))
                .andExpect(jsonPath("$.countedQuantity").value(10.0))
                .andExpect(jsonPath("$.gap").value(-2.0));
        mockMvc.perform(asAdmin(get("/api/v1/journal")).param("search", "Comptage de"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].action").value("UPDATE"))
                .andExpect(jsonPath("$.items[0].resourceType").value("INVENTORY_SESSION"));
    }

    @Test
    void countLine_thenStockMoves_keepsTheGapFrozenInTheLines() throws Exception {
        // Arrange
        mockMvc.perform(asAdmin(put(countUrl())).content(count("10"))).andExpect(status().isOk());
        var articleId =
                jdbcTemplate.queryForObject("select article_id from inventory_lines where id = ?", UUID.class, lineId);
        jdbcTemplate.update(
                "insert into stock_movements (id, article_id, location_id, type, quantity, occurred_at)"
                        + " values (gen_random_uuid(), ?, ?, 'SALE', -1, now())",
                articleId,
                location("STORE"));

        // Act + Assert
        // The article created by the test has the highest code: first line in descending order.
        mockMvc.perform(asAdmin(get(INVENTORIES + "/" + sessionId + "/lines")).param("sortDirection", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(lineId.toString()))
                .andExpect(jsonPath("$.items[0].theoreticalQuantity").value(12.0))
                .andExpect(jsonPath("$.items[0].gap").value(-2.0));
    }

    @Test
    void countLine_recountEqualToStock_returnsZeroGap() throws Exception {
        // Arrange
        mockMvc.perform(asAdmin(put(countUrl())).content(count("10"))).andExpect(status().isOk());

        // Act + Assert
        mockMvc.perform(asAdmin(put(countUrl())).content(count("12")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gap").value(0.0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"countedQuantity\":-1}", "{\"countedQuantity\":null}", "{\"countedQuantity\":1.2345}"})
    void countLine_invalidQuantity_returns422(String body) throws Exception {
        mockMvc.perform(asAdmin(put(countUrl())).content(body))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void countLine_massAssignmentOfIdAndTheoretical_isIgnored() throws Exception {
        // Arrange
        var forged = "{\"id\":\"%s\",\"theoreticalQuantity\":10,\"gap\":0,\"countedQuantity\":10}"
                .formatted(UUID.randomUUID());

        // Act + Assert
        mockMvc.perform(asAdmin(put(countUrl())).content(forged))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lineId.toString()))
                .andExpect(jsonPath("$.theoreticalQuantity").value(12.0))
                .andExpect(jsonPath("$.gap").value(-2.0));
    }

    @Test
    void countLine_closedSession_returns409() throws Exception {
        // Arrange
        jdbcTemplate.update(
                "update inventory_sessions set status = 'CLOSED', closed_at = now() where id = ?",
                UUID.fromString(sessionId));

        // Act + Assert
        mockMvc.perform(asAdmin(put(countUrl())).content(count("10")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void countLine_unknownLine_returns404() throws Exception {
        mockMvc.perform(asAdmin(put(countUrl(sessionId, UUID.randomUUID()))).content(count("10")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void countLine_unknownSession_returns404() throws Exception {
        mockMvc.perform(asAdmin(put(countUrl(UUID.randomUUID(), lineId))).content(count("10")))
                .andExpect(status().isNotFound());
    }

    @Test
    void countLine_withoutToken_returns401() throws Exception {
        mockMvc.perform(put(countUrl()).contentType(MediaType.APPLICATION_JSON).content(count("10")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void countLine_withSellerRole_returns403() throws Exception {
        // Arrange
        var token = sellerToken();

        // Act + Assert
        mockMvc.perform(put(countUrl())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(count("10")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private String countUrl() {
        return countUrl(sessionId, lineId);
    }

    private static String countUrl(Object session, Object line) {
        return INVENTORIES + "/" + session + "/lines/" + line + "/count";
    }

    private static String count(String quantity) {
        return "{\"countedQuantity\":" + quantity + "}";
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON);
    }

    private String openingBody() {
        var tomorrow = LocalDate.now(ZoneId.of("Indian/Antananarivo")).plusDays(1);
        return """
                {"locationId":"%s","scope":"ALL","rangeId":null,"responsibleId":"%s","countDate":"%s"}
                """.formatted(location("STORE"), adminId(), tomorrow).strip();
    }

    private UUID location(String code) {
        return jdbcTemplate.queryForObject("select id from stock_locations where code = ?", UUID.class, code);
    }

    private UUID adminId() {
        return jdbcTemplate.queryForObject("select id from users where email = ?", UUID.class, ADMIN_EMAIL);
    }

    private UUID createArticle() throws Exception {
        var rangeId = jdbcTemplate.queryForObject("select id from product_ranges limit 1", UUID.class);
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
