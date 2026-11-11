package org.vvs.valimbavaka.feature.sale.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
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

/** VTE-08: turnover, number of validated sales and share of the total per channel, administrator only. */
class ChannelDashboardIT extends StockApiTestSupport {

    private static final String DASHBOARD = "/api/v1/sales/channel-dashboard";
    private static final String ALL_DAYS = "?from=2000-01-01&to=2999-12-31";

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User seller;
    private UUID soap;

    /** A sale needs today's day opened (JR-01); another test class may have removed it. */
    @BeforeEach
    void openTodayCreateSellerAndStock() throws Exception {
        var status = mockMvc.perform(post("/api/v1/days")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[]}"))
                .andReturn()
                .getResponse()
                .getStatus();
        assertThat(status).isIn(201, 409);
        var email = "vendeuse-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(RoleName.SELLER).orElseThrow();
        var terms = new SellerTerms(channel("STORE"), new BigDecimal("5.00"));
        seller = userRepository.save(User.create(
                new UserProfile(email, "Miora", "Rakoto"), passwordEncoder.encode("mot-de-passe-long-1"), role, terms));
        soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "50");
    }

    @Test
    void getDashboard_administrator_returnsRevenueCountAndShareOfEachChannel() throws Exception {
        // Arrange
        var before = revenueOf("STORE");
        var facebookBefore = revenueOf("FACEBOOK");
        createSale("STORE", "1");
        createSale("STORE", "2");
        createSale("FACEBOOK", "1");

        // Act
        var json = dashboard(adminToken(), ALL_DAYS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channels[?(@.salesChannelLabel=='Boutique')].salesCount")
                        .isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert
        assertThat(revenueOf("STORE") - before).isEqualTo(25500.0);
        assertThat(revenueOf("FACEBOOK") - facebookBefore).isEqualTo(8500.0);
        BigDecimal total = new BigDecimal(JsonPath.read(json, "$.totalRevenue").toString());
        BigDecimal shares = new BigDecimal("0");
        for (Object share : JsonPath.<List<Object>>read(json, "$.channels[*].revenueSharePercent")) {
            shares = shares.add(new BigDecimal(share.toString()));
        }
        assertThat(total).isPositive();
        assertThat(shares.doubleValue()).isBetween(99.5, 100.5);
    }

    @Test
    void getDashboard_cancelledSale_isNotCounted() throws Exception {
        // Arrange
        var before = revenueOf("STORE");
        var saleId = createSale("STORE", "1");
        jdbcTemplate.update("update sales set status = 'CANCELLED' where id = ?", saleId);

        // Act + Assert
        assertThat(revenueOf("STORE")).isEqualTo(before);
    }

    @Test
    void getDashboard_periodWithoutSale_listsEveryChannelAtZero() throws Exception {
        dashboard(adminToken(), "?from=2000-01-01&to=2000-01-31")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(0.0))
                .andExpect(jsonPath("$.totalSalesCount").value(0))
                .andExpect(jsonPath("$.channels[0].revenue").value(0.0))
                .andExpect(jsonPath("$.channels[0].revenueSharePercent").value(0.0));
    }

    @Test
    void getDashboard_withoutDates_returnsTheCurrentMonth() throws Exception {
        dashboard(adminToken(), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").isNotEmpty())
                .andExpect(jsonPath("$.to").isNotEmpty());
    }

    @Test
    void getDashboard_fromAfterTo_returns422() throws Exception {
        dashboard(adminToken(), "?from=2026-11-10&to=2026-11-01")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void getDashboard_malformedDate_returns400() throws Exception {
        dashboard(adminToken(), "?from=01-11-2026")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void getDashboard_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(DASHBOARD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getDashboard_sellerAndDeliveryDriver_return403() throws Exception {
        dashboard(tokenOf(RoleName.SELLER), "")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        dashboard(tokenOf(RoleName.DELIVERY_DRIVER), "")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private ResultActions dashboard(String token, String parameters) throws Exception {
        return mockMvc.perform(get(DASHBOARD + parameters).header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    /** Turnover of the channel over every day, read through the API. */
    private double revenueOf(String code) throws Exception {
        var json = dashboard(adminToken(), ALL_DAYS)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var label = jdbcTemplate.queryForObject("select label from sales_channels where code = ?", String.class, code);
        var path = "$.channels[?(@.salesChannelLabel=='%s')].revenue".formatted(label);
        var values = JsonPath.<List<Number>>read(json, path);
        return values.get(0).doubleValue();
    }

    private UUID createSale(String channelCode, String quantity) throws Exception {
        var line = "{\"articleId\":\"%s\",\"quantity\":%s}".formatted(soap, quantity);
        var body = "{\"salesChannelId\":\"%s\",\"sellerId\":\"%s\",\"paymentMethod\":\"CASH\",\"lines\":[%s]}"
                .formatted(channel(channelCode), seller.getId(), line);
        var json = mockMvc.perform(post("/api/v1/sales")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
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
}
