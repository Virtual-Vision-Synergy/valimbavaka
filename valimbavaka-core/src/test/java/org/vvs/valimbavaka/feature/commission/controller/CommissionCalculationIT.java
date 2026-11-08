package org.vvs.valimbavaka.feature.commission.controller;

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

import org.vvs.valimbavaka.feature.sale.domain.PaymentMethod;
import org.vvs.valimbavaka.feature.sale.domain.SaleCommand;
import org.vvs.valimbavaka.feature.sale.domain.SaleLineCommand;
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
 * COM-02: every sale credited to a seller gets its commission, revenue (or margin) × the most precise rate (PO
 * decision of 07/11/2026), shown to the administrator. Each test works on its own sales channel and seller (the
 * database is shared); the articles cost 3120.50 Ar and sell for 8500 Ar, from the store.
 */
class CommissionCalculationIT extends StockApiTestSupport {

    private static final String COMMISSIONS = "/api/v1/commissions";
    private static final String SELLER_PASSWORD = "mot-de-passe-vendeuse-long";

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
    private UUID sellerId;

    /** A sale needs today's day opened (JR-01); another test class may have removed it. */
    @BeforeEach
    void openTodayWithAChannelAndASellerPaidFivePercent() throws Exception {
        var status = mockMvc.perform(post("/api/v1/days")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[]}"))
                .andReturn()
                .getResponse()
                .getStatus();
        assertThat(status).isIn(201, 409);
        channelId = createChannel();
        seller = createSeller(channelId);
        sellerId = seller.getId();
    }

    @Test
    void createSale_channelRuleOnRevenue_commissionIsTheTotalTimesTheRuleRate() throws Exception {
        // Arrange : règle du canal 4 % sur le CA, vendeuse à 5 % (la règle du canal gagne, sans cumul)
        var ruleId = createRule("4.00", "REVENUE");
        var soap = stockedArticle();

        // Act : 3 × 8 500 Ar = 25 500 Ar
        var saleNumber = createSale(soap, "3");

        // Assert : 25 500 × 4 % = 1 020 Ar
        listOfSeller()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].sourceType").value("SALE"))
                .andExpect(jsonPath("$.items[0].sourceNumber").value(saleNumber))
                .andExpect(jsonPath("$.items[0].sellerName").value("Miora Rakoto"))
                .andExpect(jsonPath("$.items[0].salesChannelId").value(channelId.toString()))
                .andExpect(jsonPath("$.items[0].revenue").value(25500.0))
                .andExpect(jsonPath("$.items[0].base").value("REVENUE"))
                .andExpect(jsonPath("$.items[0].baseAmount").value(25500.0))
                .andExpect(jsonPath("$.items[0].rate").value(4.0))
                .andExpect(jsonPath("$.items[0].rateSource").value("SALES_CHANNEL"))
                .andExpect(jsonPath("$.items[0].ruleId").value(ruleId.toString()))
                .andExpect(jsonPath("$.items[0].amount").value(1020.0))
                .andExpect(jsonPath("$.items[0].businessDate").isNotEmpty());
    }

    @Test
    void createSale_channelRuleOnMargin_commissionIsTheMarginTimesTheRuleRate() throws Exception {
        // Arrange : règle du canal 10 % sur la marge
        createRule("10.00", "MARGIN");
        var soap = stockedArticle();

        // Act : 2 × 8 500 = 17 000 Ar de CA, 2 × 3 120,50 = 6 241 Ar de coût
        createSale(soap, "2");

        // Assert : (17 000 − 6 241) × 10 % = 1 075,90 → 1 076 Ar
        listOfSeller()
                .andExpect(jsonPath("$.items[0].base").value("MARGIN"))
                .andExpect(jsonPath("$.items[0].revenue").value(17000.0))
                .andExpect(jsonPath("$.items[0].baseAmount").value(10759.0))
                .andExpect(jsonPath("$.items[0].amount").value(1076.0));
    }

    @Test
    void createSale_noRuleOnTheChannel_commissionUsesTheSellerRate() throws Exception {
        // Arrange
        var soap = stockedArticle();

        // Act
        createSale(soap, "3");

        // Assert : 25 500 × 5 % = 1 275 Ar
        listOfSeller()
                .andExpect(jsonPath("$.items[0].rateSource").value("SELLER"))
                .andExpect(jsonPath("$.items[0].rate").value(5.0))
                .andExpect(jsonPath("$.items[0].ruleId").doesNotExist())
                .andExpect(jsonPath("$.items[0].amount").value(1275.0));
    }

    @Test
    void createSale_validSale_commissionIsTracedInTheJournal() throws Exception {
        // Arrange
        var soap = stockedArticle();

        // Act
        var saleNumber = createSale(soap, "1");

        // Assert
        var descriptions = jdbcTemplate.queryForList(
                "select description from journal_entries where resource_type = 'COMMISSION' and description like ?",
                String.class,
                "Commission " + saleNumber + " %");
        var expected = "Commission %s · Miora Rakoto · 5.00 %% de 8500.00 Ar (CA) = 425.00 Ar".formatted(saleNumber);
        assertThat(descriptions).containsExactly(expected);
    }

    @Test
    void record_saleWithoutSeller_createsNoCommission() throws Exception {
        // Arrange : commande saisie par l'administrateur (LIV-03), décision PO du 07/11/2026
        var soap = stockedArticle();
        var lines = List.of(new SaleLineCommand(soap, BigDecimal.ONE, null));

        // Act
        var sale = saleService.record(new SaleCommand(channelId, null, PaymentMethod.CASH, lines));

        // Assert
        var count = jdbcTemplate.queryForObject(
                "select count(*) from commissions where source_id = ?", Integer.class, sale.id());
        assertThat(count).isZero();
    }

    @Test
    void getCommissions_periodWithoutTheSale_returnsAnEmptyPage() throws Exception {
        // Arrange
        createSale(stockedArticle(), "1");

        // Act + Assert
        mockMvc.perform(get(COMMISSIONS)
                        .param("sellerId", sellerId.toString())
                        .param("from", "2020-01-01")
                        .param("to", "2020-01-31")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void getCommissions_fromAfterToOrSizeAbove100_returns422() throws Exception {
        mockMvc.perform(get(COMMISSIONS)
                        .param("from", "2026-11-30")
                        .param("to", "2026-11-01")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(get(COMMISSIONS).param("size", "101").header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void getCommissions_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(COMMISSIONS))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getCommissions_sellerOrDeliveryDriver_returns403() throws Exception {
        var sellerToken = login(seller.getEmail());
        mockMvc.perform(get(COMMISSIONS).header(HttpHeaders.AUTHORIZATION, bearer(sellerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(get(COMMISSIONS).header(HttpHeaders.AUTHORIZATION, bearer(tokenOf(RoleName.DELIVERY_DRIVER))))
                .andExpect(status().isForbidden());
    }

    private ResultActions listOfSeller() throws Exception {
        return mockMvc.perform(get(COMMISSIONS)
                .param("sellerId", sellerId.toString())
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())));
    }

    /** Sale of the article on the test channel, entered by the administrator for the seller; returns its number. */
    private String createSale(UUID articleId, String quantity) throws Exception {
        var line = "{\"articleId\":\"%s\",\"quantity\":%s}".formatted(articleId, quantity);
        var body = "{\"salesChannelId\":\"%s\",\"sellerId\":\"%s\",\"paymentMethod\":\"CASH\",\"lines\":[%s]}"
                .formatted(channelId, sellerId, line);
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

    private UUID createRule(String rate, String base) throws Exception {
        var body = "{\"name\":\"Règle %s\",\"salesChannelId\":\"%s\",\"rate\":%s,\"base\":\"%s\"}"
                .formatted(UUID.randomUUID(), channelId, rate, base);
        var json = mockMvc.perform(post("/api/v1/commission-rules")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    /** A test channel has no location of its own: its stock leaves the store (VTE-01). */
    private UUID stockedArticle() throws Exception {
        var articleId = createArticle(uniqueName());
        move(articleId, "STORE", StockMovementType.RECEIPT, "10");
        return articleId;
    }

    private UUID createChannel() {
        var id = UUID.randomUUID();
        var code = "IT-" + id.toString().substring(0, 8);
        jdbcTemplate.update("insert into sales_channels (id, code, label) values (?, ?, ?)", id, code, "Canal " + code);
        return id;
    }

    private User createSeller(UUID salesChannelId) {
        var email = "vendeuse-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(RoleName.SELLER).orElseThrow();
        var profile = new UserProfile(email, "Miora", "Rakoto");
        var terms = new SellerTerms(salesChannelId, new BigDecimal("5.00"));
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
