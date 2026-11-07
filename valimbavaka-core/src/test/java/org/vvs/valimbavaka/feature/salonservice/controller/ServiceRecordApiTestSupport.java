package org.vvs.valimbavaka.feature.salonservice.controller;

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

/** Shared fixtures of the SRV-02 integration tests: users, articles, catalogue services and salon stock. */
@IntegrationTest
@AutoConfigureMockMvc
abstract class ServiceRecordApiTestSupport {

    protected static final String RECORDS = "/api/v1/service-records";
    protected static final String SALON = "TRADE_SHOW";
    private static final String ADMIN_EMAIL = "admin.test@valimbavaka.mg";
    private static final String ADMIN_PASSWORD = "mot-de-passe-de-test-tres-long";
    private static final String PASSWORD = "mot-de-passe-vendeuse-long";

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

    protected UUID insertArticle(String name) {
        var id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into articles (id, code, name, type, unit, threshold, is_active, created_at, updated_at) "
                        + "values (?, ?, ?, 'RAW_MATERIAL', 'LITER', 0, true, now(), now())",
                id,
                "IT-" + id.toString().substring(0, 8),
                name);
        return id;
    }

    /** Creates an active catalogue service (price 35000 Ar) whose recipe is {@code productsJson}. */
    protected UUID createService(String productsJson) throws Exception {
        var body = "{\"name\":\"Soin-%s\",\"durationMinutes\":45,\"price\":35000,\"products\":%s}"
                .formatted(UUID.randomUUID(), productsJson);
        MvcResult result = mockMvc.perform(postJson("/api/v1/salon-services", body)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
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

    protected UUID location(String code) {
        return jdbcTemplate.queryForObject("select id from stock_locations where code = ?", UUID.class, code);
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

    protected String tokenOf(String email) throws Exception {
        return accessToken(email, PASSWORD);
    }

    /** Creates an account of this role (a seller gets a channel and a commission rate) and returns its email. */
    protected String createUser(RoleName roleName) {
        var email = "user-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(roleName).orElseThrow();
        var profile = new UserProfile(email, "Miora", "Andriamasy");
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

    protected UUID userId(String email) {
        return jdbcTemplate.queryForObject("select id from users where email = ?", UUID.class, email);
    }

    private String accessToken(String email, String password) throws Exception {
        var body = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
        MvcResult result = mockMvc.perform(postJson("/api/v1/auth/login", body))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }
}
