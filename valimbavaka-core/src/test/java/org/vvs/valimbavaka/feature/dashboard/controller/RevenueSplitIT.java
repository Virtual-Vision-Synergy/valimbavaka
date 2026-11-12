package org.vvs.valimbavaka.feature.dashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
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

/**
 * DSH-04: split of today's turnover by product range and by sales channel, administrator only. The database is
 * shared with the other test classes: every test reads the split before and after its own sale and compares.
 */
class RevenueSplitIT extends StockApiTestSupport {

    private static final String SPLIT = "/api/v1/dashboard/revenue-split";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Indian/Antananarivo");

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User seller;
    private UUID soap;
    private UUID soapRange;

    /** A sale needs today's day opened (JR-01); another test class may have removed it. */
    @BeforeEach
    void openTodayCreateSellerAndStock() throws Exception {
        endAutomaticPromotions();
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
        soapRange = jdbcTemplate.queryForObject("select range_id from articles where id = ?", UUID.class, soap);
        move(soap, "STORE", StockMovementType.RECEIPT, "50");
    }

    /** The sale screen reads the first 100 sellers only: the one made here must not stay among them. */
    @AfterEach
    void deactivateSeller() {
        jdbcTemplate.update("update users set is_active = false where id = ?", seller.getId());
    }

    @Test
    void getRevenueSplit_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(SPLIT))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getRevenueSplit_sellerAndDeliveryDriver_return403() throws Exception {
        mockMvc.perform(get(SPLIT).header(HttpHeaders.AUTHORIZATION, bearer(tokenOf(RoleName.SELLER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(get(SPLIT).header(HttpHeaders.AUTHORIZATION, bearer(tokenOf(RoleName.DELIVERY_DRIVER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getRevenueSplit_administrator_returnsTodayWithRangesAndChannels() throws Exception {
        split().andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(LocalDate.now(BUSINESS_ZONE).toString()))
                .andExpect(jsonPath("$.byRange[0].rangeId").isNotEmpty())
                .andExpect(jsonPath("$.byRange[0].code").isNotEmpty())
                .andExpect(jsonPath("$.byRange[0].label").isNotEmpty())
                .andExpect(jsonPath("$.byRange[0].revenue").isNumber())
                .andExpect(jsonPath("$.byRange[0].sharePercent").isNumber())
                .andExpect(jsonPath("$.byChannel[0].salesChannelId").isNotEmpty())
                .andExpect(jsonPath("$.byChannel[0].revenue").isNumber())
                .andExpect(jsonPath("$.byChannel[0].sharePercent").isNumber());
    }

    @Test
    void getRevenueSplit_newSaleToday_raisesItsRangeAndItsChannelAndKeepsSharesAtOneHundred() throws Exception {
        // Arrange
        var before = reading();

        // Act
        createSale("2");
        var after = reading();

        // Assert
        assertThat(after.rangeRevenue() - before.rangeRevenue()).isEqualTo(17000.0);
        assertThat(after.channelRevenue() - before.channelRevenue()).isEqualTo(17000.0);
        assertThat(after.rangeShares()).isEqualByComparingTo("100.0");
        assertThat(after.channelShares()).isEqualByComparingTo("100.0");
    }

    @Test
    void getRevenueSplit_cancelledSaleToday_leavesItsRangeAndItsChannel() throws Exception {
        // Arrange
        var number = createSale("1");
        var before = reading();

        // Act
        jdbcTemplate.update("update sales set status = 'CANCELLED' where number = ?", number);
        var after = reading();

        // Assert
        assertThat(before.rangeRevenue() - after.rangeRevenue()).isEqualTo(8500.0);
        assertThat(before.channelRevenue() - after.channelRevenue()).isEqualTo(8500.0);
    }

    private ResultActions split() throws Exception {
        return mockMvc.perform(get(SPLIT).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())));
    }

    private Reading reading() throws Exception {
        var json = split().andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var rangePath = "$.byRange[?(@.rangeId=='%s')].revenue".formatted(soapRange);
        var channelPath = "$.byChannel[?(@.salesChannelId=='%s')].revenue".formatted(channel("STORE"));
        return new Reading(
                JsonPath.<List<Number>>read(json, rangePath).getFirst().doubleValue(),
                JsonPath.<List<Number>>read(json, channelPath).getFirst().doubleValue(),
                sum(json, "$.byRange[*].sharePercent"),
                sum(json, "$.byChannel[*].sharePercent"));
    }

    private static BigDecimal sum(String json, String path) {
        return JsonPath.<List<Number>>read(json, path).stream()
                .map(share -> BigDecimal.valueOf(share.doubleValue()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Turnover of the range of the test article and of the store channel, and the total of the shares of each axis. */
    private record Reading(
            double rangeRevenue, double channelRevenue, BigDecimal rangeShares, BigDecimal channelShares) {}

    private String createSale(String quantity) throws Exception {
        var line = "{\"articleId\":\"%s\",\"quantity\":%s}".formatted(soap, quantity);
        var body = "{\"salesChannelId\":\"%s\",\"sellerId\":\"%s\",\"paymentMethod\":\"CASH\",\"lines\":[%s]}"
                .formatted(channel("STORE"), seller.getId(), line);
        var response = mockMvc.perform(post("/api/v1/sales")
                        .content(body)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.number");
    }

    private UUID channel(String code) {
        return jdbcTemplate.queryForObject("select id from sales_channels where code = ?", UUID.class, code);
    }
}
