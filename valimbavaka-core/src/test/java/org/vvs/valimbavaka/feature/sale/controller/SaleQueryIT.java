package org.vvs.valimbavaka.feature.sale.controller;

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

/** VTE-07: the day's sales, paged, with articles, payment, promo and total; a seller only reads her own. */
class SaleQueryIT extends StockApiTestSupport {

    private static final String SALES = "/api/v1/sales";
    private static final String SELLER_PASSWORD = "mot-de-passe-vendeuse-long";

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User seller;
    private String sellerToken;
    private UUID soap;
    private String soapName;

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
        seller = createSeller("Miora", "Rakoto");
        sellerToken = login(seller.getEmail());
        soapName = uniqueName();
        soap = createArticle(soapName);
        move(soap, "STORE", StockMovementType.RECEIPT, "20");
    }

    @Test
    void getSales_seller_listsHerSaleWithArticlesPaymentPromoAndTotal() throws Exception {
        // Arrange
        var saleId = createSale(sellerToken, "STORE", seller.getId(), "CASH", "2");

        // Act + Assert
        list(sellerToken, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.items[0].id").value(saleId.toString()))
                .andExpect(jsonPath("$.items[0].number").value(startsWith("VTE-")))
                .andExpect(jsonPath("$.items[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$.items[0].salesChannelLabel").value("Boutique"))
                .andExpect(jsonPath("$.items[0].sellerName").value("Miora Rakoto"))
                .andExpect(jsonPath("$.items[0].paymentMethod").value("CASH"))
                .andExpect(jsonPath("$.items[0].status").value("VALIDATED"))
                .andExpect(jsonPath("$.items[0].subtotal").value(17000.0))
                .andExpect(jsonPath("$.items[0].discountAmount").value(0.0))
                .andExpect(jsonPath("$.items[0].total").value(17000.0))
                .andExpect(jsonPath("$.items[0].lines[0].articleId").value(soap.toString()))
                .andExpect(jsonPath("$.items[0].lines[0].articleName").value(soapName))
                .andExpect(jsonPath("$.items[0].lines[0].quantity").value(2.0))
                .andExpect(jsonPath("$.items[0].lines[0].lineTotal").value(17000.0))
                .andExpect(jsonPath("$.items[0].lines[0].unitCost").doesNotExist());
    }

    @Test
    void getSales_seller_neverSeesTheSalesOfAColleagueEvenWhenAskingForThem() throws Exception {
        // Arrange
        var colleague = createSeller("Lova", "Rabe");
        createSale(adminToken(), "STORE", colleague.getId(), "CASH", "1");

        // Act + Assert
        list(sellerToken, "?sellerId=" + colleague.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void getSales_administrator_filtersOnTheSellerAndTheChannel() throws Exception {
        // Arrange
        createSale(sellerToken, "STORE", seller.getId(), "CASH", "1");
        var facebookSale = createSale(sellerToken, "FACEBOOK", seller.getId(), "MVOLA", "1");
        var bySeller = "?sellerId=" + seller.getId();

        // Act + Assert
        list(adminToken(), bySeller).andExpect(status().isOk()).andExpect(jsonPath("$.totalCount").value(2));
        list(adminToken(), bySeller + "&salesChannelId=" + channel("FACEBOOK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].id").value(facebookSale.toString()))
                .andExpect(jsonPath("$.items[0].salesChannelLabel").value("Facebook"))
                .andExpect(jsonPath("$.items[0].paymentMethod").value("MVOLA"));
    }

    @Test
    void getSales_mostRecentFirstByDefault_andSortOnTheTotalOnDemand() throws Exception {
        // Arrange
        var small = createSale(sellerToken, "STORE", seller.getId(), "CASH", "1");
        var big = createSale(sellerToken, "STORE", seller.getId(), "CASH", "3");
        createSale(sellerToken, "STORE", seller.getId(), "CARD", "2");

        // Act + Assert
        list(sellerToken, "?sortBy=total&sortDirection=ASC")
                .andExpect(jsonPath("$.items[0].id").value(small.toString()))
                .andExpect(jsonPath("$.items[2].id").value(big.toString()));
        list(sellerToken, "")
                .andExpect(jsonPath("$.items[0].paymentMethod").value("CARD"))
                .andExpect(jsonPath("$.items[2].id").value(small.toString()));
        list(sellerToken, "?sortBy=unitCost")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].paymentMethod").value("CARD"));
    }

    @Test
    void getSales_pageOfOne_returnsTheCounts() throws Exception {
        // Arrange
        createSale(sellerToken, "STORE", seller.getId(), "CASH", "1");
        createSale(sellerToken, "STORE", seller.getId(), "CASH", "1");

        // Act + Assert
        list(sellerToken, "?page=2&size=1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.pageSize").value(1))
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void getSales_anotherDay_returnsAnEmptyPage() throws Exception {
        // Arrange
        createSale(sellerToken, "STORE", seller.getId(), "CASH", "1");

        // Act + Assert
        list(sellerToken, "?date=2000-01-01")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void getSales_sizeBounds_accepts100AndRejects101() throws Exception {
        list(sellerToken, "?size=100").andExpect(status().isOk());
        list(sellerToken, "?size=101")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void getSales_malformedDate_returns400() throws Exception {
        list(sellerToken, "?date=08-11-2026")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void getSales_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(SALES))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getSales_deliveryDriver_returns403() throws Exception {
        list(tokenOf(RoleName.DELIVERY_DRIVER), "")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private ResultActions list(String token, String parameters) throws Exception {
        return mockMvc.perform(get(SALES + parameters).header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private UUID createSale(String token, String channelCode, UUID sellerId, String payment, String quantity)
            throws Exception {
        var body = "{\"salesChannelId\":\"%s\",\"sellerId\":\"%s\",\"paymentMethod\":\"%s\",\"lines\":[%s]}"
                .formatted(channel(channelCode), sellerId, payment, line(quantity));
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

    private String line(String quantity) {
        return "{\"articleId\":\"%s\",\"quantity\":%s}".formatted(soap, quantity);
    }

    private UUID channel(String code) {
        return jdbcTemplate.queryForObject("select id from sales_channels where code = ?", UUID.class, code);
    }

    private User createSeller(String firstName, String lastName) {
        var email = "vendeuse-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(RoleName.SELLER).orElseThrow();
        var profile = new UserProfile(email, firstName, lastName);
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
