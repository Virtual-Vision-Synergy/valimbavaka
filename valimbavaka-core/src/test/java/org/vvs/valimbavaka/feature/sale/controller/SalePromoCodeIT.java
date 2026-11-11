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
 * VTE-04: a promo code typed at the till is checked by the server, which computes the discount; the code competes
 * with the automatic promotions (one promotion per sale). The automatic promotions left by other test classes are
 * ended first, and the promotions of the test (all with a code starting with {@code VT4}) are ended afterwards, so
 * nothing leaks into the sales of other classes.
 */
class SalePromoCodeIT extends StockApiTestSupport {

    private static final String SALES = "/api/v1/sales";
    private static final String SELLER_PASSWORD = "mot-de-passe-vendeuse-long";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Indian/Antananarivo");
    private static final String CODE_PREFIX = "VT4";
    private static final String END_CODES =
            "update promotions set starts_on = date '2000-01-01', ends_on = date '2000-01-02' where code like ?";
    private static final String SALE_BODY =
            "{\"salesChannelId\":\"%s\",\"sellerId\":\"%s\",\"paymentMethod\":\"CASH\",\"lines\":[%s]%s}";

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
        move(soap, "STORE", StockMovementType.RECEIPT, "60");
    }

    @AfterEach
    void endThePromotionsOfTheTest() {
        endAutomaticPromotions();
        jdbcTemplate.update(END_CODES, CODE_PREFIX + "%");
    }

    @Test
    void createSale_validCode_appliesTheDiscountAndCountsTheUse() throws Exception {
        // Arrange : 20 % with a code, 3 soaps at 8 500 = 25 500
        var promo = saveCoded(PromotionKind.PERCENT, "20", "STORE", null, null);

        // Act
        createSale("STORE", "3", promo.code())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(25500.0))
                .andExpect(jsonPath("$.discountAmount").value(5100.0))
                .andExpect(jsonPath("$.total").value(20400.0))
                .andExpect(jsonPath("$.promotionId").value(promo.id().toString()));

        // Assert
        assertThat(usageCount(promo.id())).isEqualTo(1);
    }

    @Test
    void createSale_codeInLowerCaseWithSpaces_isReadWithoutCaseAndSpaces() throws Exception {
        // Arrange
        var promo = saveCoded(PromotionKind.PERCENT, "20", "STORE", null, null);

        // Act + Assert
        createSale("STORE", "3", "  " + promo.code().toLowerCase() + " ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discountAmount").value(5100.0));
    }

    @Test
    void createSale_blankCode_isASaleWithoutCode() throws Exception {
        // Arrange
        var promo = saveCoded(PromotionKind.PERCENT, "20", "STORE", null, null);

        // Act + Assert
        createSale("STORE", "3", "   ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discountAmount").value(0.0))
                .andExpect(jsonPath("$.promotionId").doesNotExist());
        assertThat(usageCount(promo.id())).isZero();
    }

    @Test
    void createSale_codeGivesMoreThanTheAutomaticPromotion_theCodeWinsAndTheAutomaticOneIsNotUsed() throws Exception {
        // Arrange : automatic 10 %, code 20 %
        var automatic = saveAutomatic("10");
        var promo = saveCoded(PromotionKind.PERCENT, "20", "STORE", null, null);

        // Act
        createSale("STORE", "3", promo.code())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discountAmount").value(5100.0))
                .andExpect(jsonPath("$.promotionId").value(promo.id().toString()));

        // Assert
        assertThat(usageCount(promo.id())).isEqualTo(1);
        assertThat(usageCount(automatic)).isZero();
    }

    @Test
    void createSale_automaticPromotionGivesMoreThanTheCode_theAutomaticOneWinsAndTheCodeIsNotUsed() throws Exception {
        // Arrange : automatic 30 %, code 20 %
        var automatic = saveAutomatic("30");
        var promo = saveCoded(PromotionKind.PERCENT, "20", "STORE", null, null);

        // Act
        createSale("STORE", "3", promo.code())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discountAmount").value(7650.0))
                .andExpect(jsonPath("$.promotionId").value(automatic.toString()));

        // Assert : no accumulation, the code was not used
        assertThat(usageCount(promo.id())).isZero();
        assertThat(usageCount(automatic)).isEqualTo(1);
    }

    @Test
    void createSale_amountCode_takesTheAmountOffTheBasket() throws Exception {
        // Arrange : 2 000 ariary off
        var promo = saveCoded(PromotionKind.AMOUNT, "2000", "STORE", null, null);

        // Act + Assert
        createSale("STORE", "3", promo.code())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discountAmount").value(2000.0))
                .andExpect(jsonPath("$.total").value(23500.0));
    }

    @Test
    void createSale_unknownCode_returns404AndRecordsNothing() throws Exception {
        // Arrange
        var before = salesCount();

        // Act + Assert
        createSale("STORE", "3", "VT4ZZZZZZZ")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        assertThat(salesCount()).isEqualTo(before);
    }

    @Test
    void createSale_expiredCode_returns422() throws Exception {
        // Arrange : ended yesterday
        var promo = saveCoded(PromotionKind.PERCENT, "20", "STORE", null, period(-10, -1));

        // Act + Assert
        createSale("STORE", "3", promo.code())
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        assertThat(usageCount(promo.id())).isZero();
    }

    @Test
    void createSale_codeNotStartedYet_returns422() throws Exception {
        // Arrange : starts tomorrow
        var promo = saveCoded(PromotionKind.PERCENT, "20", "STORE", null, period(1, 10));

        // Act + Assert
        createSale("STORE", "3", promo.code()).andExpect(status().isUnprocessableContent());
    }

    @Test
    void createSale_codeOfAnotherChannel_returns422() throws Exception {
        // Arrange : the code is only valid on Facebook
        var promo = saveCoded(PromotionKind.PERCENT, "20", "FACEBOOK", null, null);

        // Act + Assert
        createSale("STORE", "3", promo.code())
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createSale_codeAtItsUsageLimit_returns422() throws Exception {
        // Arrange : limit of 1, already used once
        var promo = saveCoded(PromotionKind.PERCENT, "20", "STORE", 1, null);
        jdbcTemplate.update("update promotions set usage_count = 1 where id = ?", promo.id());

        // Act + Assert
        createSale("STORE", "3", promo.code()).andExpect(status().isUnprocessableContent());
        assertThat(usageCount(promo.id())).isEqualTo(1);
    }

    @Test
    void createSale_codeThatReachesNoArticleOfTheBasket_returns422() throws Exception {
        // Arrange : the code targets another article
        var other = createArticle(uniqueName());
        var promo = saveCoded(PromotionKind.PERCENT, "20", "STORE", null, null, other);

        // Act + Assert
        createSale("STORE", "3", promo.code()).andExpect(status().isUnprocessableContent());
        assertThat(usageCount(promo.id())).isZero();
    }

    @Test
    void createSale_malformedCode_returns422() throws Exception {
        createSale("STORE", "3", "a!")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createSale_discountAndPromotionSentByTheClient_areIgnored() throws Exception {
        // Arrange : a client that tries to impose its own discount and total, without any code
        var forged = ",\"discountAmount\":9999,\"total\":1,\"promotionId\":\"" + UUID.randomUUID() + "\"";
        var body = SALE_BODY.formatted(channel("STORE"), seller.getId(), line("3"), forged);

        // Act + Assert
        mockMvc.perform(post(SALES)
                        .header(HttpHeaders.AUTHORIZATION, bearer(sellerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discountAmount").value(0.0))
                .andExpect(jsonPath("$.total").value(25500.0))
                .andExpect(jsonPath("$.promotionId").doesNotExist());
    }

    @Test
    void getPromoCode_validCode_returnsThePromotionWithTheArticlesItReaches() throws Exception {
        // Arrange
        var promo = saveCoded(PromotionKind.PERCENT, "20", "STORE", null, null);

        // Act + Assert
        getPromoCode(sellerToken, promo.code().toLowerCase(), "STORE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(promo.id().toString()))
                .andExpect(jsonPath("$.kind").value("PERCENT"))
                .andExpect(jsonPath("$.value").value(20.0))
                .andExpect(jsonPath("$.articleIds[0]").value(soap.toString()))
                .andExpect(jsonPath("$.channelIds[0]").value(channel("STORE").toString()));
        assertThat(usageCount(promo.id())).isZero();
    }

    @Test
    void getPromoCode_unknownExpiredOrOtherChannel_returns404Or422() throws Exception {
        // Arrange
        var expired = saveCoded(PromotionKind.PERCENT, "20", "STORE", null, period(-10, -1));
        var facebook = saveCoded(PromotionKind.PERCENT, "20", "FACEBOOK", null, null);

        // Act + Assert
        getPromoCode(sellerToken, "VT4ZZZZZZZ", "STORE").andExpect(status().isNotFound());
        getPromoCode(sellerToken, expired.code(), "STORE").andExpect(status().isUnprocessableContent());
        getPromoCode(sellerToken, facebook.code(), "STORE").andExpect(status().isUnprocessableContent());
    }

    @Test
    void getPromoCode_withoutTokenOrWithADeliveryDriver_returns401Then403() throws Exception {
        mockMvc.perform(get(SALES + "/promo-codes/VT4ABCDEF")
                        .param("salesChannelId", channel("STORE").toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        getPromoCode(tokenOf(RoleName.DELIVERY_DRIVER), "VT4ABCDEF", "STORE")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private ResultActions getPromoCode(String token, String code, String channelCode) throws Exception {
        return mockMvc.perform(get(SALES + "/promo-codes/" + code)
                .param("salesChannelId", channel(channelCode).toString())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private int usageCount(UUID promotionId) {
        return jdbcTemplate.queryForObject(
                "select usage_count from promotions where id = ?", Integer.class, promotionId);
    }

    /** Period from {@code from} to {@code to} days around today (Antananarivo). */
    private static PromotionPeriod period(int from, int to) {
        var today = LocalDate.now(BUSINESS_ZONE);
        return new PromotionPeriod(today.plusDays(from), today.plusDays(to));
    }

    private int salesCount() {
        return jdbcTemplate.queryForObject("select count(*) from sales", Integer.class);
    }

    /** Automatic promotion (no code) on the soap of the test, valid today on the store. */
    private UUID saveAutomatic(String percent) {
        var details = new PromotionDetails(
                "Auto " + uniqueName(),
                null,
                PromotionKind.PERCENT,
                new BigDecimal(percent),
                null,
                PromotionScope.ARTICLES,
                Set.of(soap),
                period(0, 10),
                Set.of(channel("STORE")),
                null);
        return promotionRepository.save(Promotion.create(details)).getId();
    }

    private CodedPromotion saveCoded(
            PromotionKind kind, String value, String channelCode, Integer usageLimit, PromotionPeriod period) {
        return saveCoded(kind, value, channelCode, usageLimit, period, soap);
    }

    /** Promotion with a generated code, on the article {@code target}, valid today unless a period is given. */
    private CodedPromotion saveCoded(
            PromotionKind kind,
            String value,
            String channelCode,
            Integer usageLimit,
            PromotionPeriod period,
            UUID target) {
        var code = CODE_PREFIX
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        var details = new PromotionDetails(
                "Code " + uniqueName(),
                null,
                kind,
                new BigDecimal(value),
                code,
                PromotionScope.ARTICLES,
                Set.of(target),
                period == null ? period(0, 10) : period,
                Set.of(channel(channelCode)),
                usageLimit);
        return new CodedPromotion(
                promotionRepository.save(Promotion.create(details)).getId(), code);
    }

    private ResultActions createSale(String channelCode, String quantity, String promoCode) throws Exception {
        var code = ",\"promoCode\":\"" + promoCode + "\"";
        var body = SALE_BODY.formatted(channel(channelCode), seller.getId(), line(quantity), code);
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

    private record CodedPromotion(UUID id, String code) {}
}
