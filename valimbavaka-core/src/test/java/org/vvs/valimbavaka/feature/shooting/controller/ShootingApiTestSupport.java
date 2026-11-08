package org.vvs.valimbavaka.feature.shooting.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

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

/** Shared fixtures of the SHO-01 to SHO-03 integration tests: users, articles created through the API, stock. */
@IntegrationTest
@AutoConfigureMockMvc
abstract class ShootingApiTestSupport {

    protected static final String SHOOTINGS = "/api/v1/shootings";
    protected static final String ADMIN_EMAIL = "admin.test@valimbavaka.mg";
    protected static final String ADMIN_PASSWORD = "mot-de-passe-de-test-tres-long";
    protected static final String PASSWORD = "mot-de-passe-vendeuse-long";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    private StockMovementService stockMovementService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** Active finished article costing 3120.5 Ar, created through the API by the administrator. */
    protected UUID createArticle() throws Exception {
        return createArticle("3120.5");
    }

    /** Active finished article with this purchase cost ({@code "null"} = without cost). */
    protected UUID createArticle(String cost) throws Exception {
        var rangeId = jdbcTemplate.queryForObject("select id from product_ranges limit 1", UUID.class);
        var body = """
                {"name":"Article-%s","type":"FINISHED","unit":"PIECE","rangeId":"%s","cost":%s,"price":8500,
                 "threshold":0}
                """.formatted(UUID.randomUUID(), rangeId, cost);
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

    protected void receive(UUID articleId, String locationCode, String quantity) {
        stockMovementService.record(new StockMovementCommand(
                articleId, location(locationCode), StockMovementType.RECEIPT, new BigDecimal(quantity), null));
    }

    protected BigDecimal balance(UUID articleId, String locationCode) {
        return jdbcTemplate.queryForObject(
                "select coalesce(sum(quantity), 0) from stock_movements where article_id = ? and location_id = ?",
                BigDecimal.class,
                articleId,
                location(locationCode));
    }

    /** Exit of one article from the store, back tomorrow. */
    protected String shootingBody(UUID articleId, String quantity) {
        var item = "{\"articleId\":\"%s\",\"quantity\":%s}".formatted(articleId, quantity);
        return shootingBody(location("STORE"), tomorrow(), item);
    }

    protected static String shootingBody(UUID locationId, Instant expectedReturnAt, String items) {
        return """
                {"locationId":"%s","items":[%s],"expectedReturnAt":"%s","note":"Campagne de Noel"}
                """.formatted(locationId, items, expectedReturnAt);
    }

    protected static Instant tomorrow() {
        return Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
    }

    protected ResultActions postShooting(String token, String body) throws Exception {
        return mockMvc.perform(post(SHOOTINGS)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    /** Exit recorded by the administrator; returns its id. */
    protected UUID createShooting(String body) throws Exception {
        var response = postShooting(adminToken(), body)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(JsonPath.read(response, "$.id"));
    }

    protected ResultActions postReturn(String token, UUID shootingId) throws Exception {
        return mockMvc.perform(
                post(SHOOTINGS + "/" + shootingId + "/return").header(HttpHeaders.AUTHORIZATION, bearer(token)));
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
        var profile = new UserProfile(email, "Miora", "Andria");
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
