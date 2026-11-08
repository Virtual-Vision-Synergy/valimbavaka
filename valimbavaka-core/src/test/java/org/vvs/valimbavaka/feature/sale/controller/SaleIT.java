package org.vvs.valimbavaka.feature.sale.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

/** VTE-01: a sale is recorded on a channel for a seller, in today's opened day, and takes the stock out. */
class SaleIT extends StockApiTestSupport {

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

    /** A sale needs today's day opened (JR-01); another test class may have removed it. */
    @BeforeEach
    void openTodayAndCreateSeller() throws Exception {
        var status = openDay().andReturn().getResponse().getStatus();
        assertThat(status).isIn(201, 409);
        seller = createSeller();
        sellerToken = login(seller.getEmail());
    }

    @Test
    void createSale_storeChannel_returns201AndTakesTheStockOutOfTheStore() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "10");

        // Act
        createSale(sellerToken, storeSale("CASH", line(soap, "3")))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, startsWith("/api/v1/sales/")))
                .andExpect(jsonPath("$.number").value(startsWith("VTE-")))
                .andExpect(jsonPath("$.status").value("VALIDATED"))
                .andExpect(jsonPath("$.sellerId").value(seller.getId().toString()))
                .andExpect(jsonPath("$.stockLocationId").value(location("STORE").toString()))
                .andExpect(jsonPath("$.paymentMethod").value("CASH"))
                .andExpect(jsonPath("$.lines[0].unitPrice").value(8500.0))
                .andExpect(jsonPath("$.lines[0].lineTotal").value(25500.0))
                .andExpect(jsonPath("$.subtotal").value(25500.0))
                .andExpect(jsonPath("$.discountAmount").value(0.0))
                .andExpect(jsonPath("$.total").value(25500.0))
                .andExpect(jsonPath("$.businessDate").isNotEmpty());

        // Assert
        assertThat(balance(soap, "STORE")).isEqualByComparingTo("7");
    }

    @Test
    void createSale_facebookChannel_takesTheStockOutOfTheStore() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "5");

        // Act
        createSale(sellerToken, body(channel("FACEBOOK"), seller.getId(), "MVOLA", line(soap, "2")))
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.salesChannelId").value(channel("FACEBOOK").toString()))
                .andExpect(jsonPath("$.stockLocationId").value(location("STORE").toString()));

        // Assert
        assertThat(balance(soap, "STORE")).isEqualByComparingTo("3");
    }

    @Test
    void createSale_deliveryChannel_takesTheStockOutOfTheDeliveryLocation() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "DELIVERY", StockMovementType.RECEIPT, "4");

        // Act
        createSale(adminToken(), body(channel("DELIVERY"), seller.getId(), "ON_DELIVERY", line(soap, "1")))
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.stockLocationId").value(location("DELIVERY").toString()));

        // Assert
        assertThat(balance(soap, "DELIVERY")).isEqualByComparingTo("3");
    }

    @Test
    void createSale_validSale_isTracedInTheJournalWithOneSaleMovementPerLine() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "10");

        // Act
        var saleId = createdId(createSale(sellerToken, storeSale("CARD", line(soap, "2"))));

        // Assert
        var description = jdbcTemplate.queryForObject(
                "select description from journal_entries where resource_type = 'SALE' and resource_id = ? "
                        + "and action = 'CREATE'",
                String.class,
                saleId);
        assertThat(description).startsWith("Vente VTE-").endsWith(" · Boutique · 1 article(s) · 17000.00 Ar · Carte");
        var movements = jdbcTemplate.queryForObject(
                "select count(*) from stock_movements where reference_id = ? and type = 'SALE' and quantity = -2",
                Integer.class,
                saleId);
        assertThat(movements).isEqualTo(1);
    }

    @Test
    void getSaleById_afterAPriceChange_keepsTheFrozenPrice() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "10");
        var saleId = createdId(createSale(sellerToken, storeSale("CASH", line(soap, "1"))));
        jdbcTemplate.update("update articles set price = 9900 where id = ?", soap);

        // Act + Assert
        getSale(sellerToken, saleId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].articleId").value(soap.toString()))
                .andExpect(jsonPath("$.lines[0].unitPrice").value(8500.0))
                .andExpect(jsonPath("$.lines[0].unitCost").doesNotExist())
                .andExpect(jsonPath("$.total").value(8500.0));
    }

    @Test
    void createSale_notEnoughStock_returns409AndRecordsNothing() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "1");
        var before = countSales();

        // Act
        createSale(sellerToken, storeSale("CASH", line(soap, "2")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        // Assert
        assertThat(countSales()).isEqualTo(before);
        assertThat(balance(soap, "STORE")).isEqualByComparingTo("1");
    }

    @Test
    void createSale_dayNotOpened_returns409() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "5");
        // LIV-03: a delivered order references its sale
        jdbcTemplate.update(
                "update delivery_orders set status = 'CANCELLED', sale_id = null where sale_id is not null");
        jdbcTemplate.update("delete from sale_lines");
        jdbcTemplate.update("delete from sales");
        jdbcTemplate.update("delete from business_day_transfers");
        jdbcTemplate.update("delete from business_days");

        // Act + Assert
        createSale(sellerToken, storeSale("CASH", line(soap, "1")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(startsWith("La journée du ")));
        assertThat(balance(soap, "STORE")).isEqualByComparingTo("5");
    }

    @Test
    void createSale_inactiveArticle_returns422() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "5");
        jdbcTemplate.update("update articles set is_active = false where id = ?", soap);

        // Act + Assert
        assertValidationFailed(storeSale("CASH", line(soap, "1")));
    }

    @Test
    void createSale_sameArticleTwice_returns422() throws Exception {
        var soap = createArticle(uniqueName());
        assertValidationFailed(storeSale("CASH", line(soap, "1") + "," + line(soap, "2")));
    }

    @Test
    void createSale_creditedToAnAdministrator_returns422() throws Exception {
        var soap = createArticle(uniqueName());
        var adminId = jdbcTemplate.queryForObject("select id from users where email = ?", UUID.class, ADMIN_EMAIL);
        assertValidationFailed(body(channel("STORE"), adminId, "CASH", line(soap, "1")));
    }

    @Test
    void createSale_unknownChannel_returns422() throws Exception {
        var soap = createArticle(uniqueName());
        assertValidationFailed(body(UUID.randomUUID(), seller.getId(), "CASH", line(soap, "1")));
    }

    @Test
    void createSale_noLineOrZeroQuantityOrMissingPayment_returns422() throws Exception {
        var soap = createArticle(uniqueName());
        assertValidationFailed(storeSale("CASH", ""));
        assertValidationFailed(storeSale("CASH", line(soap, "0")));
        assertValidationFailed("{\"salesChannelId\":\"%s\",\"sellerId\":\"%s\",\"lines\":[%s]}"
                .formatted(channel("STORE"), seller.getId(), line(soap, "1")));
    }

    @Test
    void createSale_missingSeller_returns422() throws Exception {
        var soap = createArticle(uniqueName());
        assertValidationFailed("{\"salesChannelId\":\"%s\",\"paymentMethod\":\"CASH\",\"lines\":[%s]}"
                .formatted(channel("STORE"), line(soap, "1")));
    }

    @Test
    void createSale_unknownArticle_returns404() throws Exception {
        createSale(sellerToken, storeSale("CASH", line(UUID.randomUUID(), "1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void createSale_serverFieldsInTheBody_areIgnored() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "5");
        var forcedId = UUID.randomUUID();
        var body = """
                {"id":"%s","number":"VTE-9999","status":"CANCELLED","total":1,"salesChannelId":"%s",
                 "sellerId":"%s","paymentMethod":"CASH",
                 "lines":[{"articleId":"%s","quantity":1,"unitPrice":1}]}
                """.formatted(forcedId, channel("STORE"), seller.getId(), soap);

        // Act + Assert
        createSale(sellerToken, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(not(forcedId.toString())))
                .andExpect(jsonPath("$.number").value(not("VTE-9999")))
                .andExpect(jsonPath("$.status").value("VALIDATED"))
                .andExpect(jsonPath("$.lines[0].unitPrice").value(8500.0))
                .andExpect(jsonPath("$.total").value(8500.0));
    }

    @Test
    void createSale_withoutToken_returns401() throws Exception {
        mockMvc.perform(post(SALES).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void createSaleAndForm_deliveryDriver_return403() throws Exception {
        var driverToken = tokenOf(RoleName.DELIVERY_DRIVER);
        var soap = createArticle(uniqueName());
        createSale(driverToken, storeSale("CASH", line(soap, "1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(get(SALES + "/form").header(HttpHeaders.AUTHORIZATION, bearer(driverToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getSaleById_unknownSale_returns404() throws Exception {
        getSale(sellerToken, UUID.randomUUID())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void getForm_seller_returnsChannelsWithTheirLocationSellersAndSellableArticlesWithStock() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "6");
        var store = location("STORE").toString();
        var article = "$.articles[?(@.id == '%s')]".formatted(soap);
        var channelPath = "$.channels[?(@.code == '%s')].stockLocationId";
        var sellerName = "$.sellers[?(@.id == '%s')].firstName".formatted(seller.getId());

        // Act + Assert
        mockMvc.perform(get(SALES + "/form").header(HttpHeaders.AUTHORIZATION, bearer(sellerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextNumber").isNumber())
                .andExpect(jsonPath(channelPath.formatted("STORE")).value(store))
                .andExpect(jsonPath(channelPath.formatted("PHONE")).value(store))
                .andExpect(jsonPath(sellerName).value("Miora"))
                .andExpect(jsonPath("$.sellers[0].email").doesNotExist())
                .andExpect(jsonPath(article + ".price").value(8500.0))
                .andExpect(jsonPath(article + ".stock['" + store + "']").value(6.0));
    }

    private ResultActions openDay() throws Exception {
        return mockMvc.perform(post("/api/v1/days")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lines\":[]}"));
    }

    private ResultActions createSale(String token, String body) throws Exception {
        return mockMvc.perform(post(SALES)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions getSale(String token, UUID saleId) throws Exception {
        return mockMvc.perform(get(SALES + "/" + saleId).header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private void assertValidationFailed(String body) throws Exception {
        createSale(sellerToken, body)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private static UUID createdId(ResultActions actions) throws Exception {
        var json = actions.andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private static String body(UUID channelId, UUID sellerId, String payment, String lines) {
        return "{\"salesChannelId\":\"%s\",\"sellerId\":\"%s\",\"paymentMethod\":\"%s\",\"lines\":[%s]}"
                .formatted(channelId, sellerId, payment, lines);
    }

    private String storeSale(String payment, String lines) {
        return body(channel("STORE"), seller.getId(), payment, lines);
    }

    private static String line(UUID articleId, String quantity) {
        return "{\"articleId\":\"%s\",\"quantity\":%s}".formatted(articleId, quantity);
    }

    private UUID channel(String code) {
        return jdbcTemplate.queryForObject("select id from sales_channels where code = ?", UUID.class, code);
    }

    private Integer countSales() {
        return jdbcTemplate.queryForObject("select count(*) from sales", Integer.class);
    }

    private BigDecimal balance(UUID articleId, String locationCode) {
        return jdbcTemplate.queryForObject(
                "select coalesce(sum(quantity), 0) from stock_movements where article_id = ? and location_id = ?",
                BigDecimal.class,
                articleId,
                location(locationCode));
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
