package org.vvs.valimbavaka.feature.salonservice.controller;

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
 * SRV-01: the salon services catalogue is read by the seller, written by the administrator only, traced in the
 * journal, and the rate of a salon service wins over the channel rule and the seller's rate (COM-01 resolver).
 */
@IntegrationTest
@AutoConfigureMockMvc
class SalonServiceSecurityIT {

    private static final String SERVICES = "/api/v1/salon-services";
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
    void getServices_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(SERVICES))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getServices_withSellerRole_returns200() throws Exception {
        // Arrange
        var token = accessToken(createSeller(createChannel(), "5.00"), PASSWORD);

        // Act + Assert
        mockMvc.perform(get(SERVICES).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void createService_withSellerRole_returns403() throws Exception {
        // Arrange
        var token = accessToken(createSeller(createChannel(), "5.00"), PASSWORD);

        // Act + Assert
        mockMvc.perform(postJson(SERVICES, body(uniqueName(), 45, "12", "[]"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void createService_byAdmin_returns201WithProductsAndTracesInJournal() throws Exception {
        // Arrange
        var token = adminToken();
        var articleId = insertArticle(true);
        var name = uniqueName();
        var products = "[{\"articleId\":\"%s\",\"quantity\":15.5}]".formatted(articleId);

        // Act + Assert
        mockMvc.perform(postJson(SERVICES, body(name, 45, "12.5", products))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.durationMinutes").value(45))
                .andExpect(jsonPath("$.price").value(35000.0))
                .andExpect(jsonPath("$.commissionRate").value(12.5))
                .andExpect(jsonPath("$.products[0].articleId").value(articleId.toString()))
                .andExpect(jsonPath("$.products[0].quantity").value(15.5))
                .andExpect(jsonPath("$.active").value(true));
        mockMvc.perform(get("/api/v1/journal").param("search", name).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].action").value("CREATE"))
                .andExpect(jsonPath("$.items[0].resourceType").value("SALON_SERVICE"));
    }

    @Test
    void createService_withIdInBody_ignoresIt() throws Exception {
        // Arrange
        var token = adminToken();
        var forcedId = UUID.randomUUID();
        var json = """
                {"id":"%s","name":"%s","durationMinutes":30,"price":25000,"active":false}
                """.formatted(forcedId, uniqueName());

        // Act + Assert
        mockMvc.perform(postJson(SERVICES, json).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(not(forcedId.toString())))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.commissionRate").doesNotExist())
                .andExpect(jsonPath("$.products").isEmpty());
    }

    @Test
    void createService_durationZero_returns422() throws Exception {
        // Arrange
        var token = adminToken();

        // Act + Assert
        mockMvc.perform(postJson(SERVICES, body(uniqueName(), 0, "12", "[]"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createService_rateAboveHundred_returns422() throws Exception {
        // Arrange
        var token = adminToken();

        // Act + Assert
        mockMvc.perform(postJson(SERVICES, body(uniqueName(), 45, "100.01", "[]"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void createService_inactiveArticle_returns422() throws Exception {
        // Arrange
        var token = adminToken();
        var products = "[{\"articleId\":\"%s\",\"quantity\":1}]".formatted(insertArticle(false));

        // Act + Assert
        mockMvc.perform(postJson(SERVICES, body(uniqueName(), 45, "12", products))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createService_nameUsedWithOtherCase_returns409() throws Exception {
        // Arrange
        var token = adminToken();
        var name = uniqueName();
        createService(token, name, "12");

        // Act + Assert
        mockMvc.perform(postJson(SERVICES, body(name.toUpperCase(), 45, "12", "[]"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void updateService_staleVersion_returns409() throws Exception {
        // Arrange
        var token = adminToken();
        var name = uniqueName();
        var id = createService(token, name, "12");
        var update = """
                {"name":"%s","durationMinutes":60,"price":40000,"commissionRate":10,"version":0}
                """.formatted(name);
        mockMvc.perform(putJson(SERVICES + "/" + id, update).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durationMinutes").value(60));

        // Act + Assert
        mockMvc.perform(putJson(SERVICES + "/" + id, update).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isConflict());
    }

    @Test
    void setActive_deactivate_hidesServiceFromActiveList() throws Exception {
        // Arrange
        var token = adminToken();
        var name = uniqueName();
        var id = createService(token, name, null);

        // Act
        mockMvc.perform(patchJson(SERVICES + "/" + id + "/status", "{\"active\":false}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // Assert
        mockMvc.perform(get(SERVICES)
                        .param("search", name)
                        .param("active", "true")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0));
    }

    @Test
    void getServiceById_unknownId_returns404() throws Exception {
        // Arrange
        var token = adminToken();

        // Act + Assert
        mockMvc.perform(get(SERVICES + "/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void resolve_salonServiceRate_winsOverChannelRuleAndSellerRate() throws Exception {
        // Arrange
        var token = adminToken();
        var channelId = createChannel();
        var sellerId = userRepository
                .findByEmailWithRoleAndPermissions(createSeller(channelId, "5.00"))
                .orElseThrow()
                .getId();
        var rule = "{\"name\":\"Regle-%s\",\"salesChannelId\":\"%s\",\"rate\":3,\"base\":\"MARGIN\"}"
                .formatted(UUID.randomUUID(), channelId);
        mockMvc.perform(postJson("/api/v1/commission-rules", rule).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated());
        var withRate = createService(token, uniqueName(), "12.5");
        var withoutRate = createService(token, uniqueName(), null);

        // Act
        var salon = rateResolver.resolve(sellerId, channelId, withRate);
        var fallback = rateResolver.resolve(sellerId, channelId, withoutRate);

        // Assert
        assertThat(salon.source()).isEqualTo(CommissionRateSource.SALON_SERVICE);
        assertThat(salon.rate()).isEqualByComparingTo("12.50");
        assertThat(fallback.source()).isEqualTo(CommissionRateSource.SALES_CHANNEL);
        assertThat(fallback.rate()).isEqualByComparingTo("3.00");
    }

    private UUID createService(String token, String name, String rate) throws Exception {
        var request = postJson(SERVICES, body(name, 45, rate, "[]")).header(HttpHeaders.AUTHORIZATION, bearer(token));
        MvcResult result = mockMvc.perform(request).andExpect(status().isCreated()).andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    private UUID insertArticle(boolean active) {
        var id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into articles (id, code, name, type, unit, threshold, is_active, created_at, updated_at) "
                        + "values (?, ?, ?, 'RAW_MATERIAL', 'LITER', 0, ?, now(), now())",
                id,
                "IT-" + id.toString().substring(0, 8),
                "Huile " + id,
                active);
        return id;
    }

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
        return "Soin-" + UUID.randomUUID();
    }

    private static String body(String name, int duration, String rate, String products) {
        return "{\"name\":\"%s\",\"durationMinutes\":%d,\"price\":35000,\"commissionRate\":%s,\"products\":%s}"
                .formatted(name, duration, rate, products);
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
