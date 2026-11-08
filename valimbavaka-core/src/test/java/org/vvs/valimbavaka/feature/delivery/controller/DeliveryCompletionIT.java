package org.vvs.valimbavaka.feature.delivery.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.domain.SellerTerms;
import org.vvs.valimbavaka.feature.user.domain.User;
import org.vvs.valimbavaka.feature.user.domain.UserProfile;
import org.vvs.valimbavaka.feature.user.repository.RoleRepository;
import org.vvs.valimbavaka.feature.user.repository.UserRepository;

/**
 * LIV-03: the driver marks a delivery as delivered without any input; the order becomes a sale of the delivery
 * channel at its own prices (decision of the product owner, 07/11/2026).
 */
class DeliveryCompletionIT extends DeliveryApiTestSupport {

    private static final String SALE_SQL = """
            select s.number, c.code as channel, s.stock_location_id, s.seller_id, s.payment_method, s.status, s.total
            from sales s join sales_channels c on c.id = s.sales_channel_id where s.id = ?
            """;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** The sale needs today's day opened (JR-01); another test class may have removed it. */
    @BeforeEach
    void openToday() throws Exception {
        var result = mockMvc.perform(post("/api/v1/days")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[]}"))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isIn(201, 409);
    }

    @Test
    void markDelivered_driverOwnOrder_returns200DeliveredWithADeliverySale() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "5");
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "2")));

        // Act
        var result = markDelivered(tokenOfDriver(driverId), orderId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.saleId").isNotEmpty())
                .andReturn();

        // Assert
        var saleId = UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.saleId"));
        var sale = jdbcTemplate.queryForMap(SALE_SQL, saleId);
        assertThat((String) sale.get("number")).startsWith("VTE-");
        assertThat(sale.get("channel")).isEqualTo("DELIVERY");
        assertThat(sale.get("stock_location_id")).isEqualTo(location("DELIVERY"));
        assertThat(sale.get("seller_id")).isNull();
        assertThat(sale.get("payment_method")).isEqualTo("ON_DELIVERY");
        assertThat(sale.get("status")).isEqualTo("VALIDATED");
        assertThat((BigDecimal) sale.get("total")).isEqualByComparingTo("17000.00");
        assertThat(balance(articleId, "DELIVERY")).isEqualByComparingTo("3");
        assertThat(saleMovements(articleId)).isEqualTo(1);
    }

    @Test
    void markDelivered_orderEnteredBySeller_creditsTheSaleToTheSeller() throws Exception {
        // Arrange
        var sellerId = createSeller();
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "1");
        var orderId = createdOrderId(tokenOfDriver(sellerId), body(driverId, today(), line(articleId, "1")));

        // Act
        var saleId = deliveredSaleId(tokenOfDriver(driverId), orderId);

        // Assert
        var sale = jdbcTemplate.queryForMap(SALE_SQL, saleId);
        assertThat(sale.get("seller_id")).isEqualTo(sellerId);
    }

    @Test
    void markDelivered_priceChangedSinceTheOrder_sellsAtTheOrderPrice() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "2");
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "2")));
        jdbcTemplate.update("update articles set price = 9900 where id = ?", articleId);

        // Act
        var saleId = deliveredSaleId(tokenOfDriver(driverId), orderId);

        // Assert
        var unitPrice = jdbcTemplate.queryForObject(
                "select unit_price from sale_lines where sale_id = ?", BigDecimal.class, saleId);
        assertThat(unitPrice).isEqualByComparingTo("8500.00");
        var total = jdbcTemplate.queryForObject("select total from sales where id = ?", BigDecimal.class, saleId);
        assertThat(total).isEqualByComparingTo("17000.00");
    }

    @Test
    void markDelivered_articleDeactivatedSinceTheOrder_stillDeliversAtTheOrderPrice() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "1");
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "1")));
        jdbcTemplate.update("update articles set is_active = false where id = ?", articleId);

        // Act + Assert
        markDelivered(tokenOfDriver(driverId), orderId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    void markDelivered_validDelivery_isTracedInTheJournal() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "1");
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "1")));

        // Act
        deliveredSaleId(tokenOfDriver(driverId), orderId);

        // Assert
        var description = jdbcTemplate.queryForObject(
                "select description from journal_entries where resource_type = 'DELIVERY_ORDER' and resource_id = ? "
                        + "and action = 'UPDATE'",
                String.class,
                orderId);
        assertThat(description)
                .startsWith("Commande LIV-")
                .contains(" livrée : vente VTE-")
                .endsWith(", 1 article(s), 8500.00 Ar à la livraison")
                .doesNotContain("Rakoto");
    }

    @Test
    void markDelivered_administratorOnAnyOrder_returns200() throws Exception {
        // Arrange
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "1");
        var token = adminToken();
        var orderId = createdOrderId(token, body(createDriver(), today(), line(articleId, "1")));

        // Act + Assert
        markDelivered(token, orderId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    void markDelivered_withoutToken_returns401() throws Exception {
        mockMvc.perform(post(DELIVERIES + "/" + UUID.randomUUID() + "/deliver"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void markDelivered_sellerRole_returns403() throws Exception {
        // Arrange
        var articleId = createArticle(uniqueName());
        var orderId = createdOrderId(adminToken(), body(createDriver(), today(), line(articleId, "1")));

        // Act + Assert
        markDelivered(tokenOf(RoleName.SELLER), orderId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void markDelivered_orderOfAnotherDriver_returns404AndKeepsTheOrder() throws Exception {
        // Arrange
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "1");
        var orderId = createdOrderId(adminToken(), body(createDriver(), today(), line(articleId, "1")));

        // Act
        markDelivered(tokenOfDriver(createDriver()), orderId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Assert
        assertThat(statusOf(orderId)).isEqualTo("TO_PREPARE");
        assertThat(saleMovements(articleId)).isZero();
    }

    @Test
    void markDelivered_unknownOrder_returns404() throws Exception {
        markDelivered(adminToken(), UUID.randomUUID())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void markDelivered_orderAlreadyDelivered_returns409AndSellsOnlyOnce() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "4");
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "2")));
        var token = tokenOfDriver(driverId);
        markDelivered(token, orderId).andExpect(status().isOk());

        // Act
        markDelivered(token, orderId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        // Assert
        assertThat(balance(articleId, "DELIVERY")).isEqualByComparingTo("2");
        assertThat(saleMovements(articleId)).isEqualTo(1);
    }

    @Test
    void markDelivered_articlesNotInTheDeliveryLocation_returns409AndChangesNothing() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "1");
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "2")));

        // Act
        markDelivered(tokenOfDriver(driverId), orderId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        // Assert
        assertThat(statusOf(orderId)).isEqualTo("TO_PREPARE");
        assertThat(saleIdOf(orderId)).isNull();
        assertThat(balance(articleId, "DELIVERY")).isEqualByComparingTo("1");
        assertThat(saleMovements(articleId)).isZero();
    }

    @Test
    void markDelivered_dayNotOpened_returns409AndKeepsTheOrder() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "1");
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "1")));
        forgetToday();

        // Act
        markDelivered(tokenOfDriver(driverId), orderId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(startsWith("La journée du ")));

        // Assert
        assertThat(statusOf(orderId)).isEqualTo("TO_PREPARE");
        assertThat(balance(articleId, "DELIVERY")).isEqualByComparingTo("1");
    }

    @Test
    void markDelivered_forbiddenFieldsInTheBody_areIgnored() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "1");
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "1")));
        var body = """
                {"id":"%s","status":"FAILED","saleId":"%s","driverId":"%s","amount":1}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        // Act + Assert
        mockMvc.perform(post(DELIVERIES + "/" + orderId + "/deliver")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOfDriver(driverId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.driverId").value(driverId.toString()))
                .andExpect(jsonPath("$.amount").value(8500.0));
        assertThat(saleMovements(articleId)).isEqualTo(1);
    }

    @Test
    void markDelivered_replayedWithSameIdempotencyKey_sellsOnce() throws Exception {
        // Arrange
        var driverId = createDriver();
        var articleId = createArticle(uniqueName());
        move(articleId, "DELIVERY", StockMovementType.RECEIPT, "3");
        var orderId = createdOrderId(adminToken(), body(driverId, today(), line(articleId, "1")));
        var token = tokenOfDriver(driverId);
        var key = UUID.randomUUID().toString();

        // Act
        replay(token, orderId, key).andExpect(status().isOk());
        replay(token, orderId, key)
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andExpect(jsonPath("$.status").value("DELIVERED"));

        // Assert
        assertThat(balance(articleId, "DELIVERY")).isEqualByComparingTo("2");
        assertThat(saleMovements(articleId)).isEqualTo(1);
    }

    private ResultActions markDelivered(String token, UUID orderId) throws Exception {
        var url = DELIVERIES + "/" + orderId + "/deliver";
        return mockMvc.perform(post(url).header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private ResultActions replay(String token, UUID orderId, String key) throws Exception {
        return mockMvc.perform(post(DELIVERIES + "/" + orderId + "/deliver")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .header("Idempotency-Key", key));
    }

    private UUID deliveredSaleId(String token, UUID orderId) throws Exception {
        var json = markDelivered(token, orderId)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.saleId"));
    }

    /** Today not opened any more: the delivered orders let their sale go first (they reference it). */
    private void forgetToday() {
        jdbcTemplate.update(
                "update delivery_orders set status = 'CANCELLED', sale_id = null where sale_id is not null");
        jdbcTemplate.update("delete from sale_lines");
        jdbcTemplate.update("delete from sales");
        jdbcTemplate.update("delete from business_day_transfers");
        jdbcTemplate.update("delete from business_days");
    }

    /** Active seller with the shared test password (log in with {@code tokenOfDriver}). */
    private UUID createSeller() {
        var email = "vendeuse-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(RoleName.SELLER).orElseThrow();
        var profile = new UserProfile(email, "Miora", "Rakoto");
        var channelId = jdbcTemplate.queryForObject("select id from sales_channels where code = 'STORE'", UUID.class);
        var terms = new SellerTerms(channelId, new BigDecimal("5.00"));
        return userRepository.save(User.create(profile, passwordEncoder.encode(PASSWORD), role, terms)).getId();
    }

    private String statusOf(UUID orderId) {
        return jdbcTemplate.queryForObject("select status from delivery_orders where id = ?", String.class, orderId);
    }

    private @Nullable UUID saleIdOf(UUID orderId) {
        return jdbcTemplate.queryForObject("select sale_id from delivery_orders where id = ?", UUID.class, orderId);
    }

    private Integer saleMovements(UUID articleId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from stock_movements where article_id = ? and type = 'SALE'",
                Integer.class,
                articleId);
    }

    private BigDecimal balance(UUID articleId, String locationCode) {
        return jdbcTemplate.queryForObject(
                "select coalesce(sum(quantity), 0) from stock_movements where article_id = ? and location_id = ?",
                BigDecimal.class,
                articleId,
                location(locationCode));
    }

    private static LocalDate today() {
        return LocalDate.now(BUSINESS_ZONE);
    }
}
