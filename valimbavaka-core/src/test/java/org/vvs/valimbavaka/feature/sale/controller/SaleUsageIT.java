package org.vvs.valimbavaka.feature.sale.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.promotion.domain.Promotion;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionDetails;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionKind;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionPeriod;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionScope;
import org.vvs.valimbavaka.feature.promotion.repository.PromotionRepository;
import org.vvs.valimbavaka.feature.sale.service.SaleService;
import org.vvs.valimbavaka.feature.stock.controller.StockApiTestSupport;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.domain.SellerTerms;
import org.vvs.valimbavaka.feature.user.domain.User;
import org.vvs.valimbavaka.feature.user.domain.UserProfile;
import org.vvs.valimbavaka.feature.user.repository.RoleRepository;
import org.vvs.valimbavaka.feature.user.repository.UserRepository;

/**
 * PRO-03: a sale that carries a promotion counts one use of it, a cancelled sale gives it back, and the usage limit
 * holds even when several sales are recorded at the same moment. The automatic promotions left by other test classes
 * are ended first and the ones of the test are ended afterwards, so no promotion leaks into the sales of other classes.
 */
class SaleUsageIT extends StockApiTestSupport {

    private static final String SALES = "/api/v1/sales";
    private static final String SELLER_PASSWORD = "mot-de-passe-vendeuse-long";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Indian/Antananarivo");
    private static final int SIMULTANEOUS_SALES = 6;

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private SaleService saleService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User seller;
    private String sellerToken;
    private UUID soap;

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
        endAutomaticPromotions();
        seller = createSeller();
        sellerToken = login(seller.getEmail());
        soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "60");
    }

    @AfterEach
    void endThePromotionsOfTheTest() {
        endAutomaticPromotions();
    }

    @Test
    void createSale_promotionApplied_incrementsTheUsageCounterOnceAndTheSheetShowsIt() throws Exception {
        // Arrange
        var promotionId = save(null);

        // Act
        createSale("STORE", "3").andExpect(status().isCreated());

        // Assert
        assertThat(usageCount(promotionId)).isEqualTo(1);
        mockMvc.perform(get("/api/v1/promotions/" + promotionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usageCount").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createSale_noPromotionApplies_leavesTheCounterAtZero() throws Exception {
        // Arrange : the promotion is on another channel
        var promotionId = saveOn("FACEBOOK", null);

        // Act
        createSale("STORE", "3")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.promotionId").doesNotExist());

        // Assert
        assertThat(usageCount(promotionId)).isZero();
    }

    @Test
    void createSale_lastUseOfALimitedPromotion_reachesTheLimitAndTheNextSaleHasNoDiscount() throws Exception {
        // Arrange : limit of 1
        var promotionId = save(1);

        // Act + Assert
        createSale("STORE", "3")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discountAmount").value(3825.0));
        assertThat(usageCount(promotionId)).isEqualTo(1);
        createSale("STORE", "3")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discountAmount").value(0.0));
        assertThat(usageCount(promotionId)).isEqualTo(1);
    }

    @Test
    void createSale_severalSimultaneousSalesOnALimitedPromotion_neverGoPastTheLimit() throws Exception {
        // Arrange : 6 sales at the same moment, 2 uses left
        var promotionId = save(2);
        var pool = Executors.newFixedThreadPool(SIMULTANEOUS_SALES);
        var tasks = new ArrayList<Callable<Integer>>();
        for (var i = 0; i < SIMULTANEOUS_SALES; i++) {
            tasks.add(() -> createSale("STORE", "3").andReturn().getResponse().getStatus());
        }

        // Act
        try {
            for (var future : pool.invokeAll(tasks)) {
                assertThat(future.get()).isIn(201, 409);
            }
        } finally {
            pool.shutdownNow();
        }

        // Assert : the counter and the sales that carry the promotion agree, and stay within the limit
        var carrying = jdbcTemplate.queryForObject(
                "select count(*) from sales where promotion_id = ? and status = 'VALIDATED'",
                Integer.class,
                promotionId);
        assertThat(usageCount(promotionId)).isLessThanOrEqualTo(2).isEqualTo(carrying);
    }

    @Test
    void cancel_saleWithAPromotion_givesTheUseBack() throws Exception {
        // Arrange
        var promotionId = save(1);
        var saleId = recordSale();
        assertThat(usageCount(promotionId)).isEqualTo(1);

        // Act
        saleService.cancel(saleId, "erreur de saisie");

        // Assert : the use is back, the promotion applies again
        assertThat(usageCount(promotionId)).isZero();
        createSale("STORE", "3")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.promotionId").value(promotionId.toString()));
    }

    @Test
    void cancel_saleWithoutPromotion_leavesEveryCounterUntouched() throws Exception {
        // Arrange : an unrelated promotion already used once
        var promotionId = saveOn("FACEBOOK", null);
        jdbcTemplate.update("update promotions set usage_count = 1 where id = ?", promotionId);
        var saleId = recordSale();

        // Act
        saleService.cancel(saleId, "erreur de saisie");

        // Assert
        assertThat(usageCount(promotionId)).isEqualTo(1);
    }

    private UUID recordSale() throws Exception {
        var json = createSale("STORE", "3")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private int usageCount(UUID promotionId) {
        return jdbcTemplate.queryForObject(
                "select usage_count from promotions where id = ?", Integer.class, promotionId);
    }

    private UUID save(Integer usageLimit) {
        return saveOn("STORE", usageLimit);
    }

    /** Automatic 15 % promotion on the article of the test, valid today, on one channel. */
    private UUID saveOn(String channelCode, Integer usageLimit) {
        var today = LocalDate.now(BUSINESS_ZONE);
        var details = new PromotionDetails(
                "Promo " + uniqueName(),
                null,
                PromotionKind.PERCENT,
                new BigDecimal("15"),
                null,
                PromotionScope.ARTICLES,
                Set.of(soap),
                new PromotionPeriod(today, today.plusDays(10)),
                Set.of(channel(channelCode)),
                usageLimit);
        return promotionRepository.save(Promotion.create(details)).getId();
    }

    private ResultActions createSale(String channelCode, String quantity) throws Exception {
        var body = "{\"salesChannelId\":\"%s\",\"sellerId\":\"%s\",\"paymentMethod\":\"CASH\",\"lines\":[%s]}"
                .formatted(channel(channelCode), seller.getId(), line(quantity));
        return mockMvc.perform(post(SALES)
                .header(HttpHeaders.AUTHORIZATION, bearer(sellerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String line(String quantity) {
        return "{\"articleId\":\"%s\",\"quantity\":%s}".formatted(soap, quantity);
    }

    private UUID channel(String code) {
        return jdbcTemplate.queryForObject("select id from sales_channels where code = ?", UUID.class, code);
    }

    private User createSeller() {
        var email = "vendeuse-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(RoleName.SELLER).orElseThrow();
        var profile = new UserProfile(email, "Miora", "Rakoto");
        var terms = new SellerTerms(channel("STORE"), new BigDecimal("5.00"));
        return userRepository.save(User.create(profile, passwordEncoder.encode(SELLER_PASSWORD), role, terms));
    }

    private String login(String email) throws Exception {
        var body = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, SELLER_PASSWORD);
        var json = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(json, "$.accessToken");
    }
}
