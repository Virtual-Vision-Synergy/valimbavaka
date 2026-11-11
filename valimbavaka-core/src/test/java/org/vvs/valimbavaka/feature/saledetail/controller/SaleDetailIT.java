package org.vvs.valimbavaka.feature.saledetail.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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

/**
 * VTE-09: detail of a sale with its lines, invoice and the commission rule applied; a seller only reads her own sales
 * (404 otherwise), the administrator reads every sale. Each test works on its own channel, so a channel rule never
 * reaches the other tests.
 */
class SaleDetailIT extends StockApiTestSupport {

    private static final String SALES = "/api/v1/sales";
    private static final String SELLER_PASSWORD = "mot-de-passe-vendeuse-long";

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID channelId;
    private User seller;
    private String sellerToken;
    private UUID soap;
    private String soapName;

    /** A sale needs today's day opened (JR-01); another test class may have removed it. */
    @BeforeEach
    void openTodayCreateChannelSellerAndStock() throws Exception {
        var status = mockMvc.perform(post("/api/v1/days")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[]}"))
                .andReturn()
                .getResponse()
                .getStatus();
        assertThat(status).isIn(201, 409);
        endAutomaticPromotions();
        channelId = createChannel();
        seller = createSeller("Miora", "Rakoto");
        sellerToken = login(seller.getEmail());
        soapName = uniqueName();
        soap = createArticle(soapName);
        move(soap, "STORE", StockMovementType.RECEIPT, "20");
    }

    @Test
    void getDetail_administratorWithChannelRule_returnsLinesInvoiceAndTheRuleApplied() throws Exception {
        // Arrange : règle du canal 4 % sur le CA, vendeuse à 5 % (la règle du canal gagne, sans cumul)
        var ruleId = createRule("Règle test VTE-09", "4.00", "REVENUE");
        var saleId = createSale(adminToken(), "3");

        // Act + Assert : 3 × 8 500 = 25 500 Ar, commission 25 500 × 4 % = 1 020 Ar
        detail(adminToken(), saleId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saleId.toString()))
                .andExpect(jsonPath("$.number").value(startsWith("VTE-")))
                .andExpect(jsonPath("$.salesChannelLabel").value(startsWith("Canal IT-")))
                .andExpect(jsonPath("$.sellerName").value("Miora Rakoto"))
                .andExpect(jsonPath("$.paymentMethod").value("CASH"))
                .andExpect(jsonPath("$.status").value("VALIDATED"))
                .andExpect(jsonPath("$.total").value(25500.0))
                .andExpect(jsonPath("$.promotion").doesNotExist())
                .andExpect(jsonPath("$.lines.length()").value(1))
                .andExpect(jsonPath("$.lines[0].articleName").value(soapName))
                .andExpect(jsonPath("$.lines[0].articleCode").isNotEmpty())
                .andExpect(jsonPath("$.lines[0].quantity").value(3.0))
                .andExpect(jsonPath("$.lines[0].unitPrice").value(8500.0))
                .andExpect(jsonPath("$.lines[0].lineTotal").value(25500.0))
                .andExpect(jsonPath("$.lines[0].unitCost").doesNotExist())
                .andExpect(jsonPath("$.invoice.number").value(startsWith("FAC-")))
                .andExpect(jsonPath("$.invoice.status").value("VALIDATED"))
                .andExpect(jsonPath("$.commission.rateSource").value("SALES_CHANNEL"))
                .andExpect(jsonPath("$.commission.ruleId").value(ruleId.toString()))
                .andExpect(jsonPath("$.commission.ruleName").value("Règle test VTE-09"))
                .andExpect(jsonPath("$.commission.base").value("REVENUE"))
                .andExpect(jsonPath("$.commission.baseAmount").value(25500.0))
                .andExpect(jsonPath("$.commission.rate").value(4.0))
                .andExpect(jsonPath("$.commission.amount").value(1020.0))
                .andExpect(jsonPath("$.commission.status").value("CALCULATED"));
    }

    @Test
    void getDetail_noRuleOnTheChannel_commissionComesFromTheSellerRateWithoutRuleName() throws Exception {
        // Arrange
        var saleId = createSale(adminToken(), "2");

        // Act + Assert : 2 × 8 500 = 17 000 Ar × 5 % = 850 Ar
        detail(adminToken(), saleId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commission.rateSource").value("SELLER"))
                .andExpect(jsonPath("$.commission.ruleId").doesNotExist())
                .andExpect(jsonPath("$.commission.ruleName").doesNotExist())
                .andExpect(jsonPath("$.commission.rate").value(5.0))
                .andExpect(jsonPath("$.commission.amount").value(850.0));
    }

    @Test
    void getDetail_seller_readsHerOwnSaleWithHerCommission() throws Exception {
        // Arrange
        var saleId = createSale(sellerToken, "1");

        // Act + Assert
        detail(sellerToken, saleId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sellerName").value("Miora Rakoto"))
                .andExpect(jsonPath("$.commission.amount").value(425.0))
                .andExpect(jsonPath("$.invoice.number").value(startsWith("FAC-")));
    }

    @Test
    void getDetail_sellerAskingForAColleaguesSale_returns404() throws Exception {
        // Arrange : la vente est créditée à une collègue, saisie par l'administrateur
        var colleague = createSeller("Lova", "Rabe");
        var saleId = createSaleFor(adminToken(), colleague.getId(), "1");

        // Act + Assert
        detail(sellerToken, saleId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        detail(adminToken(), saleId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sellerName").value("Lova Rabe"));
    }

    @Test
    void getDetail_unknownSale_returns404() throws Exception {
        detail(adminToken(), UUID.randomUUID())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void getDetail_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(SALES + "/" + UUID.randomUUID() + "/detail"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getDetail_deliveryDriver_returns403() throws Exception {
        detail(tokenOf(RoleName.DELIVERY_DRIVER), UUID.randomUUID())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private ResultActions detail(String token, UUID saleId) throws Exception {
        return mockMvc.perform(get(SALES + "/" + saleId + "/detail").header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private UUID createSale(String token, String quantity) throws Exception {
        return createSaleFor(token, seller.getId(), quantity);
    }

    private UUID createSaleFor(String token, UUID sellerId, String quantity) throws Exception {
        var line = "{\"articleId\":\"%s\",\"quantity\":%s}".formatted(soap, quantity);
        var body = "{\"salesChannelId\":\"%s\",\"sellerId\":\"%s\",\"paymentMethod\":\"CASH\",\"lines\":[%s]}"
                .formatted(channelId, sellerId, line);
        var json = mockMvc.perform(post(SALES)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private UUID createRule(String name, String rate, String base) throws Exception {
        var body = "{\"name\":\"%s\",\"salesChannelId\":\"%s\",\"rate\":%s,\"base\":\"%s\"}"
                .formatted(name, channelId, rate, base);
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
    private UUID createChannel() {
        var id = UUID.randomUUID();
        var code = "IT-" + id.toString().substring(0, 8);
        jdbcTemplate.update("insert into sales_channels (id, code, label) values (?, ?, ?)", id, code, "Canal " + code);
        return id;
    }

    private User createSeller(String firstName, String lastName) {
        var email = "vendeuse-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(RoleName.SELLER).orElseThrow();
        var profile = new UserProfile(email, firstName, lastName);
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
