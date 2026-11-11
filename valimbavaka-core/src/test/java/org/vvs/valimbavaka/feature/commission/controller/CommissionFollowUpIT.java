package org.vvs.valimbavaka.feature.commission.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

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
 * COM-04: a seller follows her commission of the month and her monthly target; the server imposes her id (the
 * administrator names the seller). Each test works on its own sales channel and sellers (the database is shared); a
 * sale of one article brings 8 500 Ar of revenue and 8 500 × 5 % = 425 Ar of commission.
 */
class CommissionFollowUpIT extends StockApiTestSupport {

    private static final String FOLLOW_UP = "/api/v1/commissions/follow-up";
    private static final String LINES = FOLLOW_UP + "/lines";
    private static final String VALIDATION = "/api/v1/commissions/validation";
    private static final String SELLER_PASSWORD = "mot-de-passe-vendeuse-long";
    private static final ZoneId MADAGASCAR = ZoneId.of("Indian/Antananarivo");

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SaleService saleService;

    private UUID channelId;
    private User seller;
    private User otherSeller;
    private String sellerToken;

    /** A sale needs today's day opened (JR-01); another test class may have removed it. */
    @BeforeEach
    void openTodayWithAChannelAndTwoSellers() throws Exception {
        var status = mockMvc.perform(post("/api/v1/days")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[]}"))
                .andReturn()
                .getResponse()
                .getStatus();
        assertThat(status).isIn(201, 409);
        endAutomaticPromotions();
        channelId = UUID.randomUUID();
        var code = "IT-" + channelId.toString().substring(0, 8);
        jdbcTemplate.update("insert into sales_channels (id, code, label) values (?, ?, ?)", channelId, code, code);
        seller = createSeller("Miora");
        otherSeller = createSeller("Hanta");
        sellerToken = login(seller.getEmail());
    }

    @Test
    void getFollowUp_sellerWithSales_returnsTotalsOfTheMonthAndProgressTowardsTheTarget() throws Exception {
        // Arrange : 2 ventes de 8 500 Ar, objectif 20 000 Ar
        setTarget(seller, "20000.00");
        createSale(seller);
        createSale(seller);

        // Act + Assert : 17 000 Ar de CA = 85 % de l'objectif, 850 Ar de commission à valider
        followUp(sellerToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sellerId").value(seller.getId().toString()))
                .andExpect(jsonPath("$.sellerName").value("Miora Rakoto"))
                .andExpect(jsonPath("$.month").value(currentMonth().toString()))
                .andExpect(jsonPath("$.commissionCount").value(2))
                .andExpect(jsonPath("$.revenue").value(17000.0))
                .andExpect(jsonPath("$.commissionAmount").value(850.0))
                .andExpect(jsonPath("$.toValidateAmount").value(850.0))
                .andExpect(jsonPath("$.validatedAmount").value(0.0))
                .andExpect(jsonPath("$.monthlyTarget").value(20000.0))
                .andExpect(jsonPath("$.targetProgressPercent").value(85.0));
    }

    @Test
    void getFollowUp_sellerWithoutTarget_hasNullTargetAndProgress() throws Exception {
        // Arrange
        createSale(seller);

        // Act + Assert
        followUp(sellerToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commissionAmount").value(425.0))
                .andExpect(jsonPath("$.monthlyTarget").doesNotExist())
                .andExpect(jsonPath("$.targetProgressPercent").doesNotExist());
    }

    @Test
    void getFollowUp_revenueAboveTheTarget_progressExceeds100Percent() throws Exception {
        // Arrange : 8 500 Ar de CA pour un objectif de 5 000 Ar
        setTarget(seller, "5000.00");
        createSale(seller);

        // Act + Assert
        followUp(sellerToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetProgressPercent").value(170.0));
    }

    @Test
    void getFollowUp_sellerWithoutSaleThisMonth_returnsZeroTotals() throws Exception {
        // Arrange
        setTarget(seller, "20000.00");

        // Act + Assert
        followUp(sellerToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commissionCount").value(0))
                .andExpect(jsonPath("$.revenue").value(0.0))
                .andExpect(jsonPath("$.commissionAmount").value(0.0))
                .andExpect(jsonPath("$.targetProgressPercent").value(0.0));
    }

    @Test
    void getFollowUp_otherMonth_leavesTheSalesOfTheCurrentMonthOut() throws Exception {
        // Arrange
        createSale(seller);
        var previous = currentMonth().minusMonths(1).toString();

        // Act + Assert
        followUp(sellerToken, previous)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value(previous))
                .andExpect(jsonPath("$.from").value(previous + "-01"))
                .andExpect(jsonPath("$.commissionCount").value(0));
    }

    @Test
    void getFollowUp_cancelledSale_isNotCounted() throws Exception {
        // Arrange : une vente gardée, une vente annulée (avoir)
        createSale(seller);
        cancelSale(createSale(seller));

        // Act + Assert
        followUp(sellerToken, null)
                .andExpect(jsonPath("$.commissionCount").value(1))
                .andExpect(jsonPath("$.revenue").value(8500.0))
                .andExpect(jsonPath("$.commissionAmount").value(425.0));
    }

    @Test
    void getFollowUp_validatedCommissions_areSplitFromThoseToValidate() throws Exception {
        // Arrange : une vente validée, puis une nouvelle vente
        createSale(seller);
        validateCommissionsOfTheSeller();
        createSale(seller);

        // Act + Assert
        followUp(sellerToken, null)
                .andExpect(jsonPath("$.commissionAmount").value(850.0))
                .andExpect(jsonPath("$.validatedAmount").value(425.0))
                .andExpect(jsonPath("$.toValidateAmount").value(425.0));
    }

    @Test
    void getFollowUp_sellerNamingAnotherSeller_stillGetsHerOwnFigures() throws Exception {
        // Arrange : la vendeuse demande les chiffres d'une collègue
        createSale(seller);
        createSale(otherSeller);
        createSale(otherSeller);
        setTarget(otherSeller, "99999.00");

        // Act + Assert : le serveur impose son id
        followUp(sellerToken, null, otherSeller.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sellerId").value(seller.getId().toString()))
                .andExpect(jsonPath("$.commissionCount").value(1))
                .andExpect(jsonPath("$.monthlyTarget").doesNotExist());
        lines(sellerToken, otherSeller.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].sellerId").value(seller.getId().toString()));
    }

    @Test
    void getFollowUp_administratorNamingASeller_readsHerFigures() throws Exception {
        // Arrange
        setTarget(otherSeller, "10000.00");
        createSale(otherSeller);

        // Act + Assert
        followUp(adminToken(), null, otherSeller.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sellerName").value("Hanta Rakoto"))
                .andExpect(jsonPath("$.commissionAmount").value(425.0))
                .andExpect(jsonPath("$.targetProgressPercent").value(85.0));
    }

    @Test
    void getFollowUp_administratorWithoutSellerId_returns422() throws Exception {
        followUp(adminToken(), null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        lines(adminToken(), null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void getFollowUp_administratorWithUnknownOrNonSellerId_returns404() throws Exception {
        followUp(adminToken(), null, UUID.randomUUID())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        var adminId = jdbcTemplate.queryForObject("select id from users where email = ?", UUID.class, ADMIN_EMAIL);
        followUp(adminToken(), null, adminId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void getFollowUp_malformedMonth_returns400() throws Exception {
        followUp(sellerToken, "2026-13").andExpect(status().isBadRequest());
        followUp(sellerToken, "novembre").andExpect(status().isBadRequest());
    }

    @Test
    void getLines_sellerWithSales_returnsBaseRateAmountAndStatusOfEachSale() throws Exception {
        // Arrange : une vente validée, une vente à valider
        var validatedSale = createSale(seller);
        validateCommissionsOfTheSeller();
        var pendingSale = createSale(seller);

        // Act + Assert
        lines(sellerToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath(lineOf(validatedSale, "status")).value("VALIDATED"))
                .andExpect(jsonPath(lineOf(pendingSale, "status")).value("CALCULATED"))
                .andExpect(jsonPath(lineOf(pendingSale, "baseAmount")).value(8500.0))
                .andExpect(jsonPath(lineOf(pendingSale, "rate")).value(5.0))
                .andExpect(jsonPath(lineOf(pendingSale, "amount")).value(425.0));
    }

    @Test
    void getLines_cancelledSale_isListedWithItsStatusButNotCounted() throws Exception {
        // Arrange
        var cancelled = createSale(seller);
        cancelSale(cancelled);

        // Act + Assert
        lines(sellerToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].sourceNumber").value(cancelled))
                .andExpect(jsonPath("$.items[0].status").value("CANCELLED"));
        followUp(sellerToken, null).andExpect(jsonPath("$.commissionCount").value(0));
    }

    @Test
    void getLines_pagingAndSort_pageOfOneAndSizeAbove100() throws Exception {
        // Arrange
        createSale(seller);
        createSale(seller);

        // Act + Assert
        mockMvc.perform(get(LINES)
                        .param("size", "1")
                        .param("sortBy", "amount")
                        .header(HttpHeaders.AUTHORIZATION, bearer(sellerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
        mockMvc.perform(get(LINES).param("size", "101").header(HttpHeaders.AUTHORIZATION, bearer(sellerToken)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void followUp_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(FOLLOW_UP))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mockMvc.perform(get(LINES))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void followUp_deliveryDriver_returns403() throws Exception {
        var driverToken = tokenOf(RoleName.DELIVERY_DRIVER);
        followUp(driverToken, null, seller.getId())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        lines(driverToken, seller.getId()).andExpect(status().isForbidden());
    }

    /** JSON path of a field of the line of a sale among the lines of the page. */
    private static String lineOf(String saleNumber, String field) {
        return "$.items[?(@.sourceNumber == '%s')].%s".formatted(saleNumber, field);
    }

    private static YearMonth currentMonth() {
        return YearMonth.now(MADAGASCAR);
    }

    private ResultActions followUp(String token, String month) throws Exception {
        return followUp(token, month, null);
    }

    private ResultActions followUp(String token, String month, UUID sellerId) throws Exception {
        var request = get(FOLLOW_UP).header(HttpHeaders.AUTHORIZATION, bearer(token));
        if (month != null) {
            request.param("month", month);
        }
        if (sellerId != null) {
            request.param("sellerId", sellerId.toString());
        }
        return mockMvc.perform(request);
    }

    private ResultActions lines(String token, UUID sellerId) throws Exception {
        var request = get(LINES).header(HttpHeaders.AUTHORIZATION, bearer(token));
        if (sellerId != null) {
            request.param("sellerId", sellerId.toString());
        }
        return mockMvc.perform(request);
    }

    private void validateCommissionsOfTheSeller() throws Exception {
        var body = "{\"sellerId\":\"%s\",\"from\":\"2020-01-01\",\"to\":\"2099-12-31\"}".formatted(seller.getId());
        mockMvc.perform(post(VALIDATION)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    private void setTarget(User user, String target) {
        jdbcTemplate.update("update users set monthly_target = ? where id = ?", new BigDecimal(target), user.getId());
    }

    private void cancelSale(String saleNumber) {
        var saleId = jdbcTemplate.queryForObject("select id from sales where number = ?", UUID.class, saleNumber);
        saleService.cancel(saleId, "avoir");
    }

    /** One-article sale on the test channel, entered by the administrator for the seller; returns its number. */
    private String createSale(User forSeller) throws Exception {
        var articleId = createArticle(uniqueName());
        move(articleId, "STORE", StockMovementType.RECEIPT, "10");
        var line = "{\"articleId\":\"%s\",\"quantity\":1}".formatted(articleId);
        var body = "{\"salesChannelId\":\"%s\",\"sellerId\":\"%s\",\"paymentMethod\":\"CASH\",\"lines\":[%s]}"
                .formatted(channelId, forSeller.getId(), line);
        var json = mockMvc.perform(post("/api/v1/sales")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(json, "$.number");
    }

    private User createSeller(String firstName) {
        var email = "vendeuse-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(RoleName.SELLER).orElseThrow();
        var profile = new UserProfile(email, firstName, "Rakoto");
        var terms = new SellerTerms(channelId, new BigDecimal("5.00"));
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
