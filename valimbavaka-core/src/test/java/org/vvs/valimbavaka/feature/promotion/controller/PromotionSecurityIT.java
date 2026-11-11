package org.vvs.valimbavaka.feature.promotion.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.promotion.repository.PromotionRepository;
import org.vvs.valimbavaka.feature.referential.repository.ProductRangeRepository;
import org.vvs.valimbavaka.feature.referential.repository.SalesChannelRepository;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.domain.SellerTerms;
import org.vvs.valimbavaka.feature.user.domain.User;
import org.vvs.valimbavaka.feature.user.domain.UserProfile;
import org.vvs.valimbavaka.feature.user.repository.RoleRepository;
import org.vvs.valimbavaka.feature.user.repository.UserRepository;
import org.vvs.valimbavaka.support.IntegrationTest;

/** PRO-01: an administrator creates a promotion; the sellers read it; every creation is traced in the journal. */
@IntegrationTest
@AutoConfigureMockMvc
class PromotionSecurityIT {

    private static final String PROMOTIONS = "/api/v1/promotions";
    private static final String ADMIN_EMAIL = "admin.test@valimbavaka.mg";
    private static final String ADMIN_PASSWORD = "mot-de-passe-de-test-tres-long";
    private static final String PASSWORD = "mot-de-passe-vendeuse-long";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private SalesChannelRepository salesChannelRepository;

    @Autowired
    private ProductRangeRepository productRangeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void createPromotion_withoutToken_returns401() throws Exception {
        mockMvc.perform(postJson(PROMOTIONS, body(uniqueName(), null)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void createPromotion_bySeller_returns403() throws Exception {
        // Arrange
        var token = accessToken(createUser(RoleName.SELLER), PASSWORD);

        // Act + Assert
        mockMvc.perform(postJson(PROMOTIONS, body(uniqueName(), null)).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void createPromotion_byAdmin_returns201WithStatusAndTracesInJournal() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var name = uniqueName();

        // Act + Assert
        mockMvc.perform(postJson(PROMOTIONS, body(name, null)).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.kind").value("PERCENT"))
                .andExpect(jsonPath("$.scope").value("RANGES"))
                .andExpect(jsonPath("$.targetIds[0]").value(rangeId().toString()))
                .andExpect(jsonPath("$.channelIds[0]").value(channelId().toString()))
                .andExpect(jsonPath("$.usageCount").value(0))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        mockMvc.perform(get("/api/v1/journal").param("search", name).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].action").value("CREATE"))
                .andExpect(jsonPath("$.items[0].resourceType").value("PROMOTION"));
    }

    @Test
    void getPromotionById_bySeller_returnsTheCreatedPromotion() throws Exception {
        // Arrange
        var adminToken = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var id = createPromotion(adminToken, body(uniqueName(), null));
        var sellerToken = accessToken(createUser(RoleName.SELLER), PASSWORD);

        // Act + Assert
        mockMvc.perform(get(PROMOTIONS + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer(sellerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void getPromotionById_byDeliveryDriver_returns403() throws Exception {
        // Arrange
        var token = accessToken(createUser(RoleName.DELIVERY_DRIVER), PASSWORD);

        // Act + Assert
        mockMvc.perform(get(PROMOTIONS + "/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPromotionById_unknownId_returns404() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        // Act + Assert
        mockMvc.perform(get(PROMOTIONS + "/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void createPromotion_massAssignmentOfIdStatusAndUsageCount_isIgnored() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var forcedId = UUID.randomUUID();
        var body = body(uniqueName(), null)
                .replaceFirst("\\{", "{\"id\":\"%s\",\"status\":\"ENDED\",\"usageCount\":99,".formatted(forcedId));

        // Act
        var id = createPromotion(token, body);

        // Assert
        assertThat(id).isNotEqualTo(forcedId);
        assertThat(promotionRepository.findById(id).orElseThrow().getUsageCount())
                .isZero();
    }

    @Test
    void createPromotion_missingNameAndNoChannel_returns422() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var body = """
                {"name":"  ","kind":"PERCENT","value":15,"scope":"ALL","startsOn":"%s","endsOn":"%s","channelIds":[]}
                """.formatted(today(), today().plusDays(30));

        // Act + Assert
        mockMvc.perform(postJson(PROMOTIONS, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createPromotion_percentAbove100_returns422() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var body = body(uniqueName(), null).replace("\"value\":15", "\"value\":100.01");

        // Act + Assert
        mockMvc.perform(postJson(PROMOTIONS, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createPromotion_codeAlreadyUsedWithOtherCase_returns409() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var code = uniqueCode();
        createPromotion(token, body(uniqueName(), code));

        // Act + Assert
        mockMvc.perform(postJson(PROMOTIONS, body(uniqueName(), code.toLowerCase()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    private UUID createPromotion(String token, String body) throws Exception {
        MvcResult result = mockMvc.perform(postJson(PROMOTIONS, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    /** 15 % on the first range, from today for 30 days, on the first sales channel; optional code. */
    private String body(String name, String code) {
        var codePart = code == null ? "" : ",\"code\":\"%s\"".formatted(code);
        return """
                {"name":"%s","kind":"PERCENT","value":15,"scope":"RANGES","targetIds":["%s"],
                 "startsOn":"%s","endsOn":"%s","channelIds":["%s"]%s}
                """.formatted(name, rangeId(), today(), today().plusDays(30), channelId(), codePart);
    }

    private UUID rangeId() {
        return productRangeRepository.findAll().getFirst().getId();
    }

    private UUID channelId() {
        return salesChannelRepository.findByCode("STORE").orElseThrow().getId();
    }

    private static LocalDate today() {
        return LocalDate.now(ZoneId.of("Indian/Antananarivo"));
    }

    private static String uniqueName() {
        return "Promotion-" + UUID.randomUUID();
    }

    private static String uniqueCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
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

    private String createUser(RoleName roleName) {
        var email = "promo-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(roleName).orElseThrow();
        var terms = SellerTerms.NONE;
        if (roleName == RoleName.SELLER) {
            terms = new SellerTerms(channelId(), new BigDecimal("5.00"));
        }
        userRepository.save(User.create(
                new UserProfile(email, "Tiana", "Ranaivoson"), passwordEncoder.encode(PASSWORD), role, terms));
        return email;
    }

    private static MockHttpServletRequestBuilder postJson(String url, String body) {
        return post(url).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
