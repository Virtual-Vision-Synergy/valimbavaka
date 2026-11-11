package org.vvs.valimbavaka.feature.sale.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
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

import org.vvs.valimbavaka.feature.promotion.domain.Promotion;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionDetails;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionKind;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionPeriod;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionScope;
import org.vvs.valimbavaka.feature.promotion.repository.PromotionRepository;
import org.vvs.valimbavaka.feature.stock.controller.StockApiTestSupport;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.domain.SellerTerms;
import org.vvs.valimbavaka.feature.user.domain.User;
import org.vvs.valimbavaka.feature.user.domain.UserProfile;
import org.vvs.valimbavaka.feature.user.repository.RoleRepository;
import org.vvs.valimbavaka.feature.user.repository.UserRepository;

/**
 * PRO-02: the server applies the automatic promotion of the channel to a sale entered at the counter and decides the
 * discount; the client sends no amount. Each test targets its own article, and the automatic promotions left by other
 * test classes are ended first, so the amounts are those of the promotions created here.
 */
class SalePromotionIT extends StockApiTestSupport {

    private static final String SALES = "/api/v1/sales";
    private static final String SELLER_PASSWORD = "mot-de-passe-vendeuse-long";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Indian/Antananarivo");

    @Autowired
    private PromotionRepository promotionRepository;

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
        move(soap, "STORE", StockMovementType.RECEIPT, "30");
    }

    /** The promotions of this test must not reach the sales of the next test classes. */
    @AfterEach
    void endThePromotionsOfTheTest() {
        endAutomaticPromotions();
    }

    @Test
    void createSale_automaticPercentPromotion_appliesItToTheTargetedArticle() throws Exception {
        // Arrange : 3 × 8 500 = 25 500 Ar, 15 % = 3 825 Ar
        var promotionId = save(PromotionKind.PERCENT, "15", null, Set.of(channel("STORE")), today());

        // Act + Assert
        createSale("STORE", "3")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(25500.0))
                .andExpect(jsonPath("$.discountAmount").value(3825.0))
                .andExpect(jsonPath("$.total").value(21675.0))
                .andExpect(jsonPath("$.promotionId").value(promotionId.toString()));
        var stored = jdbcTemplate.queryForObject(
                "select discount_amount from sales where promotion_id = ?", BigDecimal.class, promotionId);
        assertThat(stored).isEqualByComparingTo("3825");
    }

    @Test
    void createSale_automaticAmountPromotion_takesTheAmountOffTheTotalOnce() throws Exception {
        // Arrange
        save(PromotionKind.AMOUNT, "2000", null, Set.of(channel("STORE")), today());

        // Act + Assert
        createSale("STORE", "3")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discountAmount").value(2000.0))
                .andExpect(jsonPath("$.total").value(23500.0));
    }

    @Test
    void createSale_automaticBuyThreePromotion_givesOneFreeUnitPerGroupOfThree() throws Exception {
        // Arrange : 7 bought, 2 free = 17 000 Ar
        save(PromotionKind.BUY_X, "3", null, Set.of(channel("STORE")), today());

        // Act + Assert
        createSale("STORE", "7")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(59500.0))
                .andExpect(jsonPath("$.discountAmount").value(17000.0))
                .andExpect(jsonPath("$.total").value(42500.0));
    }

    @Test
    void createSale_twoAutomaticPromotions_appliesTheMostAdvantageousOneWithoutAccumulating() throws Exception {
        // Arrange : 10 % = 2 550 Ar of 25 500 Ar, against 1 000 Ar off
        var best = save(PromotionKind.PERCENT, "10", null, Set.of(channel("STORE")), today());
        save(PromotionKind.AMOUNT, "1000", null, Set.of(channel("STORE")), today());

        // Act + Assert
        createSale("STORE", "3")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discountAmount").value(2550.0))
                .andExpect(jsonPath("$.promotionId").value(best.toString()));
    }

    @Test
    void createSale_promotionOfAnotherChannel_givesNoDiscount() throws Exception {
        // Arrange
        save(PromotionKind.PERCENT, "15", null, Set.of(channel("FACEBOOK")), today());

        // Act + Assert
        createSale("STORE", "3")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discountAmount").value(0.0))
                .andExpect(jsonPath("$.promotionId").doesNotExist())
                .andExpect(jsonPath("$.total").value(25500.0));
    }

    @Test
    void createSale_promotionWithACode_isNotAppliedAutomatically() throws Exception {
        // Arrange : applied only when the code is typed (VTE-04)
        save(PromotionKind.PERCENT, "15", code(), Set.of(channel("STORE")), today());

        // Act + Assert
        createSale("STORE", "3")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discountAmount").value(0.0));
    }

    @Test
    void createSale_promotionNotStartedYet_givesNoDiscount() throws Exception {
        // Arrange
        var tomorrow = today().plusDays(1);
        savePeriod(PromotionKind.PERCENT, "15", new PromotionPeriod(tomorrow, tomorrow.plusDays(5)));

        // Act + Assert
        createSale("STORE", "3")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discountAmount").value(0.0));
    }

    @Test
    void createSale_promotionEndedYesterday_givesNoDiscount() throws Exception {
        // Arrange : the period is inclusive, it ended the day before
        var yesterday = today().minusDays(1);
        savePeriod(PromotionKind.PERCENT, "15", new PromotionPeriod(yesterday.minusDays(5), yesterday));

        // Act + Assert
        createSale("STORE", "3")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discountAmount").value(0.0));
    }

    @Test
    void createSale_promotionEndingToday_stillApplies() throws Exception {
        // Arrange
        savePeriod(PromotionKind.PERCENT, "20", new PromotionPeriod(today().minusDays(5), today()));

        // Act + Assert
        createSale("STORE", "3")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discountAmount").value(5100.0));
    }

    @Test
    void createSale_usageLimitReached_givesNoDiscount() throws Exception {
        // Arrange
        var promotionId = save(PromotionKind.PERCENT, "15", null, Set.of(channel("STORE")), today());
        jdbcTemplate.update("update promotions set usage_limit = 5, usage_count = 5 where id = ?", promotionId);

        // Act + Assert
        createSale("STORE", "3")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discountAmount").value(0.0));
    }

    @Test
    void createSale_discountAndPromotionSentByTheClient_areIgnored() throws Exception {
        // Arrange : no promotion running, the body tries to impose one (mass assignment)
        var body = "{\"salesChannelId\":\"%s\",\"sellerId\":\"%s\",\"paymentMethod\":\"CASH\",\"discountAmount\":9999,"
                        .formatted(channel("STORE"), seller.getId())
                + "\"promotionId\":\"%s\",\"total\":1,\"lines\":[%s]}".formatted(UUID.randomUUID(), line("3"));

        // Act + Assert
        mockMvc.perform(post(SALES)
                        .header(HttpHeaders.AUTHORIZATION, bearer(sellerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discountAmount").value(0.0))
                .andExpect(jsonPath("$.promotionId").doesNotExist())
                .andExpect(jsonPath("$.total").value(25500.0));
    }

    @Test
    void getForm_automaticPromotion_isListedWithTheArticlesItReachesAndItsChannels() throws Exception {
        // Arrange
        var promotionId = save(PromotionKind.PERCENT, "15", null, Set.of(channel("STORE")), today());
        var promotion = "$.promotions[?(@.id == '%s')]".formatted(promotionId);

        // Act + Assert
        mockMvc.perform(get(SALES + "/form").header(HttpHeaders.AUTHORIZATION, bearer(sellerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(promotion + ".kind").value("PERCENT"))
                .andExpect(jsonPath(promotion + ".value").value(15.0))
                .andExpect(jsonPath(promotion + ".articleIds[0]").value(soap.toString()))
                .andExpect(jsonPath(promotion + ".channelIds[0]")
                        .value(channel("STORE").toString()));
    }

    @Test
    void getForm_promotionWithACode_isNotListed() throws Exception {
        // Arrange
        var promotionId = save(PromotionKind.PERCENT, "15", code(), Set.of(channel("STORE")), today());
        var promotion = "$.promotions[?(@.id == '%s')]".formatted(promotionId);

        // Act + Assert
        mockMvc.perform(get(SALES + "/form").header(HttpHeaders.AUTHORIZATION, bearer(sellerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(promotion).isEmpty());
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

    /** Promotion on the article of the test, saved directly (the period of some tests cannot go through the API). */
    private UUID save(PromotionKind kind, String value, String code, Set<UUID> channels, LocalDate startsOn) {
        return saveFor(kind, value, code, channels, new PromotionPeriod(startsOn, startsOn.plusDays(10)));
    }

    private UUID savePeriod(PromotionKind kind, String value, PromotionPeriod period) {
        return saveFor(kind, value, null, Set.of(channel("STORE")), period);
    }

    private UUID saveFor(PromotionKind kind, String value, String code, Set<UUID> channels, PromotionPeriod period) {
        var details = new PromotionDetails(
                "Promo " + uniqueName(),
                null,
                kind,
                new BigDecimal(value),
                code,
                PromotionScope.ARTICLES,
                Set.of(soap),
                period,
                channels,
                null);
        return promotionRepository.save(Promotion.create(details)).getId();
    }

    private static String code() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private static LocalDate today() {
        return LocalDate.now(BUSINESS_ZONE);
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
