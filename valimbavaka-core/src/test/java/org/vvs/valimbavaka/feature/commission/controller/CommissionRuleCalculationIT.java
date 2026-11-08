package org.vvs.valimbavaka.feature.commission.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import org.vvs.valimbavaka.feature.commission.domain.CommissionBase;
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
 * COM-06: a rule saved through the API is the one the calculation uses ({@link CommissionRateResolver}, called by
 * COM-02), as soon as it is created, modified, deactivated or reactivated. Each test works on its own sales channel
 * (the database is shared) and on a seller paid 5.00 % on her own.
 */
@IntegrationTest
@AutoConfigureMockMvc
class CommissionRuleCalculationIT {

    private static final String RULES = "/api/v1/commission-rules";
    private static final String ADMIN_EMAIL = "admin.test@valimbavaka.mg";
    private static final String ADMIN_PASSWORD = "mot-de-passe-de-test-tres-long";
    private static final String PASSWORD = "mot-de-passe-vendeuse-long";
    private static final String SELLER_RATE = "5.00";

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
    private CommissionRateResolver sut;

    @Test
    void createRule_byAdmin_isStoredAndAppliedWithItsRateAndBase() throws Exception {
        // Arrange
        var token = adminToken();
        var channelId = createChannel();
        var sellerId = createSeller(channelId);
        var beforeRule = sut.resolve(sellerId, channelId, null);

        // Act
        var ruleId = createRule(token, channelId, "4.75", "MARGIN");

        // Assert
        mockMvc.perform(get(RULES + "/" + ruleId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.rate").value(4.75));
        assertThat(beforeRule.source()).isEqualTo(CommissionRateSource.SELLER);
        assertThat(beforeRule.rate()).isEqualByComparingTo(SELLER_RATE);
        var applicable = sut.resolve(sellerId, channelId, null);
        assertThat(applicable.source()).isEqualTo(CommissionRateSource.SALES_CHANNEL);
        assertThat(applicable.rate()).isEqualByComparingTo("4.75");
        assertThat(applicable.base()).isEqualTo(CommissionBase.MARGIN);
        assertThat(applicable.ruleId()).isEqualTo(ruleId);
    }

    @Test
    void updateRule_newRate_isAppliedByTheNextCalculation() throws Exception {
        // Arrange
        var token = adminToken();
        var channelId = createChannel();
        var sellerId = createSeller(channelId);
        var ruleId = createRule(token, channelId, "3", "REVENUE");
        var update = body(uniqueName(), channelId, "6.5", "REVENUE");

        // Act
        mockMvc.perform(putJson(RULES + "/" + ruleId, update).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        // Assert
        var applicable = sut.resolve(sellerId, channelId, null);
        assertThat(applicable.source()).isEqualTo(CommissionRateSource.SALES_CHANNEL);
        assertThat(applicable.rate()).isEqualByComparingTo("6.50");
        assertThat(applicable.base()).isEqualTo(CommissionBase.REVENUE);
    }

    @Test
    void setActive_deactivateThenReactivate_switchesTheCalculationBackAndForth() throws Exception {
        // Arrange
        var token = adminToken();
        var channelId = createChannel();
        var sellerId = createSeller(channelId);
        var ruleId = createRule(token, channelId, "3", "REVENUE");

        // Act
        setActive(token, ruleId, false);
        var whileInactive = sut.resolve(sellerId, channelId, null);
        setActive(token, ruleId, true);
        var onceReactivated = sut.resolve(sellerId, channelId, null);

        // Assert
        assertThat(whileInactive.source()).isEqualTo(CommissionRateSource.SELLER);
        assertThat(whileInactive.rate()).isEqualByComparingTo(SELLER_RATE);
        assertThat(onceReactivated.source()).isEqualTo(CommissionRateSource.SALES_CHANNEL);
        assertThat(onceReactivated.rate()).isEqualByComparingTo("3.00");
        assertThat(onceReactivated.ruleId()).isEqualTo(ruleId);
    }

    @Test
    void createRule_onAnotherChannel_leavesThisChannelOnTheSellerRate() throws Exception {
        // Arrange
        var token = adminToken();
        var sellerChannelId = createChannel();
        var otherChannelId = createChannel();
        var sellerId = createSeller(sellerChannelId);

        // Act
        createRule(token, otherChannelId, "3", "REVENUE");

        // Assert
        var onSellerChannel = sut.resolve(sellerId, sellerChannelId, null);
        var onOtherChannel = sut.resolve(sellerId, otherChannelId, null);
        assertThat(onSellerChannel.source()).isEqualTo(CommissionRateSource.SELLER);
        assertThat(onSellerChannel.rate()).isEqualByComparingTo(SELLER_RATE);
        assertThat(onOtherChannel.source()).isEqualTo(CommissionRateSource.SALES_CHANNEL);
        assertThat(onOtherChannel.rate()).isEqualByComparingTo("3.00");
    }

    @Test
    void createRule_refusedWith422_leavesTheCalculationUnchanged() throws Exception {
        // Arrange
        var token = adminToken();
        var channelId = createChannel();
        var sellerId = createSeller(channelId);

        // Act
        mockMvc.perform(postJson(RULES, body(uniqueName(), channelId, "100.01", "REVENUE"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent());

        // Assert
        var applicable = sut.resolve(sellerId, channelId, null);
        assertThat(applicable.source()).isEqualTo(CommissionRateSource.SELLER);
        assertThat(applicable.rate()).isEqualByComparingTo(SELLER_RATE);
        assertThat(applicable.ruleId()).isNull();
    }

    private UUID createRule(String token, UUID channelId, String rate, String base) throws Exception {
        MvcResult result = mockMvc.perform(postJson(RULES, body(uniqueName(), channelId, rate, base))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    private void setActive(String token, UUID ruleId, boolean active) throws Exception {
        var json = "{\"active\":%s}".formatted(active);
        mockMvc.perform(patch(RULES + "/" + ruleId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(active));
    }

    /** A fresh active channel per test: the partial unique index forbids two active rules on one channel. */
    private UUID createChannel() {
        var id = UUID.randomUUID();
        var code = "IT-" + id.toString().substring(0, 8);
        jdbcTemplate.update("insert into sales_channels (id, code, label) values (?, ?, ?)", id, code, "Canal " + code);
        return id;
    }

    private UUID createSeller(UUID channelId) {
        var email = "vendeuse-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(RoleName.SELLER).orElseThrow();
        var seller = userRepository.save(User.create(
                new UserProfile(email, "Miora", "Andriamasy"),
                passwordEncoder.encode(PASSWORD),
                role,
                new SellerTerms(channelId, new BigDecimal(SELLER_RATE))));
        return seller.getId();
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

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private String adminToken() throws Exception {
        var body = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(ADMIN_EMAIL, ADMIN_PASSWORD);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }
}
