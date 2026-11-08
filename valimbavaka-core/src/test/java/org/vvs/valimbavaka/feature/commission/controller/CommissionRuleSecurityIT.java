package org.vvs.valimbavaka.feature.commission.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
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

import org.vvs.valimbavaka.feature.commission.domain.CommissionRateSource;
import org.vvs.valimbavaka.feature.commission.service.CommissionRateResolver;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.domain.SellerTerms;
import org.vvs.valimbavaka.feature.user.domain.User;
import org.vvs.valimbavaka.feature.user.domain.UserProfile;
import org.vvs.valimbavaka.feature.user.repository.RoleRepository;
import org.vvs.valimbavaka.feature.user.repository.UserRepository;
import org.vvs.valimbavaka.support.IntegrationTest;

/**
 * COM-01: commission rules by sales channel are admin-only, traced in the journal, one active rule per channel, and
 * the resolver applies the most precise rate. Each test works on its own sales channel (the database is shared).
 */
@IntegrationTest
@AutoConfigureMockMvc
class CommissionRuleSecurityIT {

    private static final String RULES = "/api/v1/commission-rules";
    private static final String ADMIN_EMAIL = "admin.test@valimbavaka.mg";
    private static final String ADMIN_PASSWORD = "mot-de-passe-de-test-tres-long";
    private static final String PASSWORD = "mot-de-passe-vendeuse-long";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CommissionRateResolver rateResolver;

    @Test
    void getRules_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(RULES))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getRules_withSellerRole_returns403() throws Exception {
        // Arrange
        var token = accessToken(createSeller(createChannel(), "5.00"), PASSWORD);

        // Act + Assert
        mockMvc.perform(get(RULES).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void createRule_withSellerRole_returns403() throws Exception {
        // Arrange
        var channelId = createChannel();
        var token = accessToken(createSeller(channelId, "5.00"), PASSWORD);

        // Act + Assert
        mockMvc.perform(postJson(RULES, body(uniqueName(), channelId, "3.00", "REVENUE"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createRule_byAdmin_returns201AndTracesInJournal() throws Exception {
        // Arrange
        var token = adminToken();
        var channelId = createChannel();
        var name = uniqueName();

        // Act + Assert
        mockMvc.perform(postJson(RULES, body(name, channelId, "3.5", "MARGIN"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.salesChannelId").value(channelId.toString()))
                .andExpect(jsonPath("$.rate").value(3.5))
                .andExpect(jsonPath("$.base").value("MARGIN"))
                .andExpect(jsonPath("$.active").value(true));
        mockMvc.perform(get("/api/v1/journal").param("search", name).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].action").value("CREATE"))
                .andExpect(jsonPath("$.items[0].resourceType").value("COMMISSION_RULE"));
    }

    @Test
    void createRule_withIdInBody_ignoresIt() throws Exception {
        // Arrange
        var token = adminToken();
        var forcedId = UUID.randomUUID();
        var json = """
                {"id":"%s","name":"%s","salesChannelId":"%s","rate":2,"base":"REVENUE","active":false}
                """.formatted(forcedId, uniqueName(), createChannel());

        // Act + Assert
        mockMvc.perform(postJson(RULES, json).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(not(forcedId.toString())))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createRule_rateAboveHundred_returns422() throws Exception {
        // Arrange
        var token = adminToken();

        // Act + Assert
        mockMvc.perform(postJson(RULES, body(uniqueName(), createChannel(), "100.01", "REVENUE"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createRule_unknownSalesChannel_returns422() throws Exception {
        // Arrange
        var token = adminToken();

        // Act + Assert
        mockMvc.perform(postJson(RULES, body(uniqueName(), UUID.randomUUID(), "3", "REVENUE"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createRule_unknownBase_returns400() throws Exception {
        // Arrange
        var token = adminToken();

        // Act + Assert
        mockMvc.perform(postJson(RULES, body(uniqueName(), createChannel(), "3", "PROFIT"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void createRule_secondActiveRuleOnSameChannel_returns409() throws Exception {
        // Arrange
        var token = adminToken();
        var channelId = createChannel();
        createRule(token, uniqueName(), channelId);

        // Act + Assert
        mockMvc.perform(postJson(RULES, body(uniqueName(), channelId, "4", "REVENUE"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void createRule_nameUsedWithOtherCase_returns409() throws Exception {
        // Arrange
        var token = adminToken();
        var name = uniqueName();
        createRule(token, name, createChannel());

        // Act + Assert
        mockMvc.perform(postJson(RULES, body(name.toUpperCase(), createChannel(), "4", "REVENUE"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isConflict());
    }

    @Test
    void setActive_deactivateThenNewRuleOnSameChannel_isAccepted() throws Exception {
        // Arrange
        var token = adminToken();
        var channelId = createChannel();
        var first = createRule(token, uniqueName(), channelId);

        // Act
        mockMvc.perform(patchJson(RULES + "/" + first + "/status", "{\"active\":false}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        createRule(token, uniqueName(), channelId);

        // Assert
        mockMvc.perform(patchJson(RULES + "/" + first + "/status", "{\"active\":true}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isConflict());
        mockMvc.perform(get(RULES)
                        .param("salesChannelId", channelId.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2));
    }

    @Test
    void updateRule_staleVersion_returns409() throws Exception {
        // Arrange
        var token = adminToken();
        var channelId = createChannel();
        var name = uniqueName();
        var id = createRule(token, name, channelId);
        var update = """
                {"name":"%s","salesChannelId":"%s","rate":6,"base":"REVENUE","version":0}
                """.formatted(name, channelId);
        mockMvc.perform(putJson(RULES + "/" + id, update).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate").value(6.0));

        // Act + Assert
        mockMvc.perform(putJson(RULES + "/" + id, update).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isConflict());
    }

    @Test
    void getRuleById_unknownId_returns404() throws Exception {
        // Arrange
        var token = adminToken();

        // Act + Assert
        mockMvc.perform(get(RULES + "/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void resolve_channelRuleThenSellerRate_appliesTheMostPreciseLevel() throws Exception {
        // Arrange
        var token = adminToken();
        var channelId = createChannel();
        var sellerEmail = createSeller(channelId, "5.00");
        var sellerId = userRepository
                .findByEmailWithRoleAndPermissions(sellerEmail)
                .orElseThrow()
                .getId();

        // Act
        var beforeRule = rateResolver.resolve(sellerId, channelId, null);
        var ruleId = createRule(token, uniqueName(), channelId);
        var withRule = rateResolver.resolve(sellerId, channelId, UUID.randomUUID());

        // Assert
        assertThat(beforeRule.source()).isEqualTo(CommissionRateSource.SELLER);
        assertThat(beforeRule.rate()).isEqualByComparingTo("5.00");
        assertThat(withRule.source()).isEqualTo(CommissionRateSource.SALES_CHANNEL);
        assertThat(withRule.rate()).isEqualByComparingTo("3.00");
        assertThat(withRule.ruleId()).isEqualTo(ruleId);
    }

    private UUID createRule(String token, String name, UUID channelId) throws Exception {
        MvcResult result = mockMvc.perform(postJson(RULES, body(name, channelId, "3", "REVENUE"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    /** A fresh active channel per test: the partial unique index forbids two active rules on one channel. */
    private UUID createChannel() {
        var id = UUID.randomUUID();
        var code = "IT-" + id.toString().substring(0, 8);
        jdbcTemplate.update("insert into sales_channels (id, code, label) values (?, ?, ?)", id, code, "Canal " + code);
        return id;
    }

    private String createSeller(UUID channelId, String rate) {
        var email = "vendeuse-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(RoleName.SELLER).orElseThrow();
        userRepository.save(User.create(
                new UserProfile(email, "Miora", "Andriamasy"),
                passwordEncoder.encode(PASSWORD),
                role,
                new SellerTerms(channelId, new BigDecimal(rate))));
        return email;
    }

    private static String uniqueName() {
        return "Regle-" + UUID.randomUUID();
    }

    private static String body(String name, UUID channelId, String rate, String base) {
        return "{\"name\":\"%s\",\"salesChannelId\":\"%s\",\"rate\":%s,\"base\":\"%s\"}"
                .formatted(name, channelId, rate, base);
    }

    private static MockHttpServletRequestBuilder postJson(String url, String body) {
        return post(url).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static MockHttpServletRequestBuilder putJson(String url, String body) {
        return put(url).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static MockHttpServletRequestBuilder patchJson(String url, String body) {
        return patch(url).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private String adminToken() throws Exception {
        return accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    private String accessToken(String email, String password) throws Exception {
        var body = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }
}
