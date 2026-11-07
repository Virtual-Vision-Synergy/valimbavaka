package org.vvs.valimbavaka.feature.production.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

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

/** Shared fixtures of the PRD-01 integration tests: accounts, articles of each type, batch bodies. */
@IntegrationTest
@AutoConfigureMockMvc
abstract class ProductionApiTestSupport {

    protected static final String BATCHES = "/api/v1/production/batches";
    protected static final String OPERATORS = "/api/v1/production/operators";
    protected static final String ADMIN_EMAIL = "admin.test@valimbavaka.mg";
    protected static final String ADMIN_PASSWORD = "mot-de-passe-de-test-tres-long";
    protected static final String PASSWORD = "mot-de-passe-vendeuse-long";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** @param type RAW_MATERIAL, SEMI_FINISHED or FINISHED */
    protected UUID createArticle(String type) throws Exception {
        var rangeId = jdbcTemplate.queryForObject("select id from product_ranges limit 1", UUID.class);
        var finished = "FINISHED".equals(type);
        var body = """
                {"name":"%s","type":"%s","unit":"%s","rangeId":%s,"cost":1200,"price":%s,"threshold":5}
                """.formatted(
                        "Article-" + UUID.randomUUID(),
                        type,
                        finished ? "PIECE" : "KILOGRAM",
                        finished ? "\"" + rangeId + "\"" : "null",
                        finished ? "8500" : "null");
        MvcResult result = mockMvc.perform(
                        postJson("/api/v1/articles", body).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    protected static String batchBody(UUID outputId, String quantity, UUID operatorId, String inputs) {
        return """
                {"outputArticleId":"%s","quantity":%s,"operatorId":"%s","inputs":[%s]}
                """.formatted(outputId, quantity, operatorId, inputs);
    }

    protected static String input(UUID articleId, String quantity) {
        return "{\"articleId\":\"%s\",\"quantity\":%s}".formatted(articleId, quantity);
    }

    protected static MockHttpServletRequestBuilder postJson(String url, String body) {
        return post(url).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    protected String adminToken() throws Exception {
        return accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    protected String tokenOf(User user) throws Exception {
        return accessToken(user.getEmail(), PASSWORD);
    }

    protected String accessToken(String email, String password) throws Exception {
        var body = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
        MvcResult result = mockMvc.perform(postJson("/api/v1/auth/login", body))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    protected User createUser(RoleName roleName) {
        return createUser(roleName, "Tiana");
    }

    protected User createUser(RoleName roleName, String firstName) {
        var email = "user-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(roleName).orElseThrow();
        var profile = new UserProfile(email, firstName, "Ranaivoson");
        var encoded = passwordEncoder.encode(PASSWORD);
        if (roleName == RoleName.SELLER) {
            var channelId = jdbcTemplate.queryForObject("select id from sales_channels limit 1", UUID.class);
            return userRepository.save(
                    User.create(profile, encoded, role, new SellerTerms(channelId, new BigDecimal("5.00"))));
        }
        return userRepository.save(User.create(profile, encoded, role));
    }
}
