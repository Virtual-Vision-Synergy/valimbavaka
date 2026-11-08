package org.vvs.valimbavaka.feature.dashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.stock.controller.StockApiTestSupport;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.domain.SellerTerms;
import org.vvs.valimbavaka.feature.user.domain.User;
import org.vvs.valimbavaka.feature.user.domain.UserProfile;
import org.vvs.valimbavaka.feature.user.repository.RoleRepository;
import org.vvs.valimbavaka.feature.user.repository.UserRepository;

/** DSH-02: revenue of the last 7 days and 4 weeks, from the validated sales, for the administrator only. */
class DashboardIT extends StockApiTestSupport {

    private static final String REVENUE_TREND = "/api/v1/dashboard/revenue-trend";
    private static final String TODAY_AMOUNT = "$.last7Days[6].amount";

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void getRevenueTrend_admin_returnsSevenDaysAndFourWeeksEndingToday() throws Exception {
        var json = getRevenueTrend(adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.last7Days.length()").value(7))
                .andExpect(jsonPath("$.last4Weeks.length()").value(4))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String today = JsonPath.read(json, "$.today");
        String lastDay = JsonPath.read(json, "$.last7Days[6].date");
        assertThat(lastDay).isEqualTo(today);
    }

    @Test
    void getRevenueTrend_afterASale_addsItsTotalToTodayButNotOnceCancelled() throws Exception {
        // Arrange
        var adminToken = adminToken();
        var opened = openDay(adminToken).andReturn().getResponse().getStatus();
        assertThat(opened).isIn(201, 409);
        var before = todayAmount(adminToken);
        var soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "10");

        // Act: 3 × 8500 Ar
        var saleId = createSale(adminToken, soap, "3");
        var afterSale = todayAmount(adminToken);
        jdbcTemplate.update("update sales set status = 'CANCELLED' where id = ?", saleId);
        var afterCancel = todayAmount(adminToken);

        // Assert
        assertThat(afterSale.subtract(before)).isEqualByComparingTo("25500.00");
        assertThat(afterCancel).isEqualByComparingTo(before);
    }

    @Test
    void getRevenueTrend_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(REVENUE_TREND))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getRevenueTrend_sellerOrDeliveryDriver_returns403() throws Exception {
        getRevenueTrend(tokenOf(RoleName.SELLER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        getRevenueTrend(tokenOf(RoleName.DELIVERY_DRIVER)).andExpect(status().isForbidden());
    }

    private ResultActions getRevenueTrend(String token) throws Exception {
        return mockMvc.perform(get(REVENUE_TREND).header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private BigDecimal todayAmount(String token) throws Exception {
        var json = getRevenueTrend(token)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Object amount = JsonPath.read(json, TODAY_AMOUNT);
        return new BigDecimal(amount.toString());
    }

    private ResultActions openDay(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/days")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lines\":[]}"));
    }

    private UUID createSale(String token, UUID articleId, String quantity) throws Exception {
        var store = channel("STORE");
        var line = "{\"articleId\":\"%s\",\"quantity\":%s}".formatted(articleId, quantity);
        var body = "{\"salesChannelId\":\"%s\",\"sellerId\":\"%s\",\"paymentMethod\":\"CASH\",\"lines\":[%s]}"
                .formatted(store, createSeller(store).getId(), line);
        var json = mockMvc.perform(post("/api/v1/sales")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private UUID channel(String code) {
        return jdbcTemplate.queryForObject("select id from sales_channels where code = ?", UUID.class, code);
    }

    private User createSeller(UUID channelId) {
        var email = "vendeuse-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(RoleName.SELLER).orElseThrow();
        var profile = new UserProfile(email, "Miora", "Rakoto");
        var terms = new SellerTerms(channelId, new BigDecimal("5.00"));
        return userRepository.save(User.create(profile, passwordEncoder.encode(PASSWORD), role, terms));
    }
}
