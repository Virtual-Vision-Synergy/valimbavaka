package org.vvs.valimbavaka.feature.inventory.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.domain.SellerTerms;
import org.vvs.valimbavaka.feature.user.domain.User;
import org.vvs.valimbavaka.feature.user.domain.UserProfile;
import org.vvs.valimbavaka.feature.user.repository.RoleRepository;
import org.vvs.valimbavaka.feature.user.repository.UserRepository;
import org.vvs.valimbavaka.support.IntegrationTest;

/** INV-01: opening of an inventory session (four required fields), reading, rights and journal. */
@IntegrationTest
@AutoConfigureMockMvc
class InventoryOpeningIT {

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

    @BeforeEach
    void setUp() throws Exception {
        // The database is shared by the tests: no session may stay in progress from another test.
        jdbcTemplate.update("update inventory_sessions set status = 'CANCELLED' where status = 'IN_PROGRESS'");
        adminToken = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        createArticle();
    }

    @Test
    void openSession_withTheFourFields_returns201WithLinesAndTracesInJournal() throws Exception {
        // Act
        var result = mockMvc.perform(asAdmin(post(INVENTORIES)).content(body("STORE", "ALL", null)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.number").value(matchesPattern("INV-\\d{4,}")))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.locationCode").value("STORE"))
                .andExpect(jsonPath("$.scope").value("ALL"))
                .andExpect(jsonPath("$.countDate").value(tomorrow().toString()))
                .andExpect(jsonPath("$.responsibleId").value(adminId().toString()))
                .andReturn();

        // Assert
        var json = result.getResponse().getContentAsString();
        int lineCount = JsonPath.read(json, "$.lineCount");
        assertThat(lineCount).isPositive();
        String number = JsonPath.read(json, "$.number");
        mockMvc.perform(asAdmin(get("/api/v1/journal").param("search", number)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].action").value("CREATE"))
                .andExpect(jsonPath("$.items[0].resourceType").value("INVENTORY_SESSION"));
    }

    @Test
    void openSession_rangeScope_returns201WithTheRange() throws Exception {
        // Arrange
        var rangeId = rangeId();

        // Act + Assert
        mockMvc.perform(asAdmin(post(INVENTORIES)).content(body("WAREHOUSE", "RANGE", rangeId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scope").value("RANGE"))
                .andExpect(jsonPath("$.rangeId").value(rangeId.toString()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"locationId", "scope", "responsibleId", "countDate"})
    void openSession_missingRequiredField_returns422(String field) throws Exception {
        // Arrange
        var body = body("STORE", "ALL", null)
                .replaceFirst("\"" + field + "\":(\"[^\"]*\"|null)", "\"" + field + "\":null");

        // Act + Assert
        mockMvc.perform(asAdmin(post(INVENTORIES)).content(body))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void openSession_rangeScopeWithoutRange_returns422() throws Exception {
        mockMvc.perform(asAdmin(post(INVENTORIES)).content(body("STORE", "RANGE", null)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void openSession_countDateYesterday_returns422() throws Exception {
        // Arrange
        var body = body("STORE", "ALL", null).replace(tomorrow().toString(), tomorrow().minusDays(2).toString());

        // Act + Assert
        mockMvc.perform(asAdmin(post(INVENTORIES)).content(body))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void openSession_secondSessionInSameLocation_returns409() throws Exception {
        // Arrange
        mockMvc.perform(asAdmin(post(INVENTORIES)).content(body("STORE", "ALL", null)))
                .andExpect(status().isCreated());

        // Act + Assert
        mockMvc.perform(asAdmin(post(INVENTORIES)).content(body("STORE", "ALL", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void openSession_massAssignmentOfIdStatusAndNumber_isIgnored() throws Exception {
        // Arrange
        var forgedId = UUID.randomUUID();
        var forged = "{\"id\":\"%s\",\"status\":\"CLOSED\",\"number\":\"INV-9999\",".formatted(forgedId);
        var body = body("STORE", "ALL", null).replaceFirst("\\{", forged);

        // Act + Assert
        mockMvc.perform(asAdmin(post(INVENTORIES)).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(not(forgedId.toString())))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.number").value(not("INV-9999")));
    }

    @Test
    void openSession_withoutToken_returns401() throws Exception {
        mockMvc.perform(post(INVENTORIES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("STORE", "ALL", null)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void openSession_withSellerRole_returns403() throws Exception {
        // Arrange
        var token = sellerToken();

        // Act + Assert
        mockMvc.perform(post(INVENTORIES)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("STORE", "ALL", null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getSessions_withSellerRole_returns403() throws Exception {
        // Arrange
        var token = sellerToken();

        // Act + Assert
        mockMvc.perform(get(INVENTORIES).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void getSessions_inProgressFilter_returnsTheOpenedSession() throws Exception {
        // Arrange
        mockMvc.perform(asAdmin(post(INVENTORIES)).content(body("STORE", "ALL", null)))
                .andExpect(status().isCreated());

        // Act + Assert
        mockMvc.perform(asAdmin(get(INVENTORIES)).param("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].locationCode").value("STORE"));
    }

    @Test
    void getSessions_sizeOver100_returns422() throws Exception {
        mockMvc.perform(asAdmin(get(INVENTORIES)).param("size", "101"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void getSession_unknownId_returns404() throws Exception {
        mockMvc.perform(asAdmin(get(INVENTORIES + "/" + UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void getLines_openedSession_returnsUncountedLinesSortedByCode() throws Exception {
        // Arrange
        var created = mockMvc.perform(asAdmin(post(INVENTORIES)).content(body("STORE", "ALL", null)))
                .andExpect(status().isCreated())
                .andReturn();
        String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        // Act + Assert
        mockMvc.perform(asAdmin(get(INVENTORIES + "/" + id + "/lines")).param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].articleCode").exists())
                .andExpect(jsonPath("$.items[0].theoreticalQuantity").exists())
                .andExpect(jsonPath("$.items[0].countedQuantity").doesNotExist())
                .andExpect(jsonPath("$.items[0].gap").doesNotExist());
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON);
    }

    private String body(String locationCode, String scope, @Nullable UUID rangeId) {
        var range = rangeId == null ? "null" : "\"" + rangeId + "\"";
        return """
                {"locationId":"%s","scope":"%s","rangeId":%s,"responsibleId":"%s","countDate":"%s"}
                """.formatted(location(locationCode), scope, range, adminId(), tomorrow()).strip();
    }

    /** Tomorrow in Antananarivo: never in the past, even around midnight. */
    private static LocalDate tomorrow() {
        return LocalDate.now(ZoneId.of("Indian/Antananarivo")).plusDays(1);
    }

    private UUID location(String code) {
        return jdbcTemplate.queryForObject("select id from stock_locations where code = ?", UUID.class, code);
    }

    private UUID rangeId() {
        return jdbcTemplate.queryForObject("select id from product_ranges limit 1", UUID.class);
    }

    private UUID adminId() {
        return jdbcTemplate.queryForObject("select id from users where email = ?", UUID.class, ADMIN_EMAIL);
    }

    private void createArticle() throws Exception {
        var body = """
                {"name":"Article-%s","type":"FINISHED","unit":"PIECE","rangeId":"%s","cost":3120.5,"price":8500,
                 "threshold":40}
                """.formatted(UUID.randomUUID(), rangeId());
        mockMvc.perform(asAdmin(post("/api/v1/articles")).content(body)).andExpect(status().isCreated());
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
        MvcResult result = mockMvc.perform(
                        post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }
}
