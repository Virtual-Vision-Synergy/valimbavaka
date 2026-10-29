package org.vvs.valimbavaka.feature.stock.controller;

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

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.stock.domain.StockMovementCommand;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.stock.service.StockMovementService;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.domain.SellerTerms;
import org.vvs.valimbavaka.feature.user.domain.User;
import org.vvs.valimbavaka.feature.user.domain.UserProfile;
import org.vvs.valimbavaka.feature.user.repository.RoleRepository;
import org.vvs.valimbavaka.feature.user.repository.UserRepository;
import org.vvs.valimbavaka.support.IntegrationTest;

/** Shared fixtures of the STK-01 integration tests: users, articles created through the API, movements. */
@IntegrationTest
@AutoConfigureMockMvc
abstract class StockApiTestSupport {

    protected static final String STOCKS = "/api/v1/stocks";
    protected static final String ADMIN_EMAIL = "admin.test@valimbavaka.mg";
    protected static final String ADMIN_PASSWORD = "mot-de-passe-de-test-tres-long";
    protected static final String PASSWORD = "mot-de-passe-livreur-long";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected StockMovementService stockMovementService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    protected UUID createArticle(String name) throws Exception {
        var rangeId = jdbcTemplate.queryForObject("select id from product_ranges limit 1", UUID.class);
        var body = """
                {"name":"%s","type":"FINISHED","unit":"PIECE","rangeId":"%s","cost":3120.5,"price":8500,
                 "threshold":40}
                """.formatted(name, rangeId);
        MvcResult result = mockMvc.perform(post("/api/v1/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    protected UUID location(String code) {
        return jdbcTemplate.queryForObject("select id from stock_locations where code = ?", UUID.class, code);
    }

    protected void move(UUID articleId, String locationCode, StockMovementType type, String quantity) {
        stockMovementService.record(
                new StockMovementCommand(articleId, location(locationCode), type, new BigDecimal(quantity), null));
    }

    protected static String uniqueName() {
        return "Article-" + UUID.randomUUID();
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    protected String adminToken() throws Exception {
        return accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    protected String tokenOf(RoleName roleName) throws Exception {
        return accessToken(createUser(roleName), PASSWORD);
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
        var email = "user-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(roleName).orElseThrow();
        var profile = new UserProfile(email, "Tiana", "Ranaivoson");
        var encoded = passwordEncoder.encode(PASSWORD);
        if (roleName == RoleName.SELLER) {
            var channelId = jdbcTemplate.queryForObject("select id from sales_channels limit 1", UUID.class);
            userRepository.save(
                    User.create(profile, encoded, role, new SellerTerms(channelId, new BigDecimal("5.00"))));
        } else {
            userRepository.save(User.create(profile, encoded, role));
        }
        return email;
    }
}
