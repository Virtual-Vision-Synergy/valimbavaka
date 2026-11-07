package org.vvs.valimbavaka.feature.articleoverview.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.JsonPathResultMatchers;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.promotion.domain.Promotion;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionDetails;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionKind;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionPeriod;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionScope;
import org.vvs.valimbavaka.feature.promotion.repository.PromotionRepository;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementCommand;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.stock.service.StockMovementService;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.domain.SellerTerms;
import org.vvs.valimbavaka.feature.user.domain.User;
import org.vvs.valimbavaka.feature.user.domain.UserProfile;
import org.vvs.valimbavaka.feature.user.repository.RoleRepository;
import org.vvs.valimbavaka.feature.user.repository.UserRepository;
import org.vvs.valimbavaka.support.IntegrationTest;

/**
 * ART-06: article sheet at 360° in one call. Each test creates its own articles; promotions are found by a unique
 * name, so the promotions of other tests never interfere.
 */
@IntegrationTest
@AutoConfigureMockMvc
class ArticleOverviewIT {

    private static final String ADMIN_EMAIL = "admin.test@valimbavaka.mg";
    private static final String ADMIN_PASSWORD = "mot-de-passe-de-test-tres-long";
    private static final String PASSWORD = "mot-de-passe-vendeuse-long";
    private static final String LOCATION_BY_CODE = "select id from stock_locations where code = ?";
    private static final String FIRST_RANGE = "select id from product_ranges where is_active order by code limit 1";
    private static final String FIRST_CHANNEL = "select id from sales_channels where is_active order by code limit 1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StockMovementService stockMovementService;

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void getOverview_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(overviewUrl(UUID.randomUUID())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getOverview_byDeliveryDriver_returns403() throws Exception {
        // Arrange
        var token = accessToken(createUser(RoleName.DELIVERY_DRIVER).getEmail(), PASSWORD);

        // Act + Assert
        mockMvc.perform(overview(UUID.randomUUID(), token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getOverview_unknownArticle_returns404() throws Exception {
        mockMvc.perform(overview(UUID.randomUUID(), adminToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void getOverview_finishedProductBySeller_showsStockBatchPromotionsAndMovements() throws Exception {
        // Arrange
        var seller = createUser(RoleName.SELLER);
        var soap = createArticle("FINISHED");
        var oil = createArticle("RAW_MATERIAL");
        var other = createArticle("FINISHED");
        move(oil, "WAREHOUSE", StockMovementType.RECEIPT, "10");
        var batchNumber = completeBatch(soap, oil, seller.getId());
        move(soap, "WAREHOUSE", StockMovementType.TRANSFER, "-2");
        move(soap, "STORE", StockMovementType.TRANSFER, "2");
        var tag = "Fiche-" + UUID.randomUUID();
        var today = LocalDate.now(ZoneId.of("Indian/Antananarivo"));
        var running = new PromotionPeriod(today, today.plusDays(3));
        save(tag + " article", PromotionScope.ARTICLES, Set.of(soap), running);
        save(tag + " gamme", PromotionScope.RANGES, Set.of(rangeId()), running);
        save(tag + " catalogue", PromotionScope.ALL, Set.of(), running);
        var ended = new PromotionPeriod(today.minusDays(5), today.minusDays(1));
        save(tag + " terminee", PromotionScope.ARTICLES, Set.of(soap), ended);
        save(tag + " autre", PromotionScope.ARTICLES, Set.of(other), running);

        // Act + Assert
        mockMvc.perform(overview(soap, accessToken(seller.getEmail(), PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.article.id").value(soap.toString()))
                .andExpect(jsonPath("$.stock.total").value(12.0))
                .andExpect(jsonPath("$.stock.locations[?(@.code=='WAREHOUSE')].quantity").value(10.0))
                .andExpect(jsonPath("$.stock.locations[?(@.code=='STORE')].quantity").value(2.0))
                .andExpect(jsonPath("$.productionBatches.length()").value(1))
                .andExpect(jsonPath("$.productionBatches[0].number").value(batchNumber))
                .andExpect(jsonPath("$.productionBatches[0].role").value("OUTPUT"))
                .andExpect(jsonPath("$.productionBatches[0].quantity").value(12.0))
                .andExpect(jsonPath("$.productionBatches[0].status").value("COMPLETED"))
                .andExpect(promotionNamed(tag + " article").isNotEmpty())
                .andExpect(promotionNamed(tag + " gamme").isNotEmpty())
                .andExpect(promotionNamed(tag + " catalogue").isNotEmpty())
                .andExpect(promotionNamed(tag + " terminee").isEmpty())
                .andExpect(promotionNamed(tag + " autre").isEmpty())
                .andExpect(jsonPath("$.recentMovements.length()").value(3))
                .andExpect(jsonPath("$.recentMovements[0].type").value("TRANSFER"))
                .andExpect(jsonPath("$.recentMovements[0].quantity").value(2.0))
                .andExpect(jsonPath("$.recentMovements[2].type").value("PRODUCTION"));
    }

    @Test
    void getOverview_rawMaterial_showsTheConsumedQuantityAndNoPromotion() throws Exception {
        // Arrange
        var seller = createUser(RoleName.SELLER);
        var soap = createArticle("FINISHED");
        var oil = createArticle("RAW_MATERIAL");
        move(oil, "WAREHOUSE", StockMovementType.RECEIPT, "10");
        completeBatch(soap, oil, seller.getId());
        var today = LocalDate.now(ZoneId.of("Indian/Antananarivo"));
        save("Fiche-" + UUID.randomUUID(), PromotionScope.ALL, Set.of(), new PromotionPeriod(today, today));

        // Act + Assert
        mockMvc.perform(overview(oil, adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.article.type").value("RAW_MATERIAL"))
                .andExpect(jsonPath("$.stock.total").value(7.0))
                .andExpect(jsonPath("$.activePromotions.length()").value(0))
                .andExpect(jsonPath("$.productionBatches[0].role").value("INPUT"))
                .andExpect(jsonPath("$.productionBatches[0].quantity").value(3.0))
                .andExpect(jsonPath("$.productionBatches[0].outputArticleId").value(soap.toString()))
                .andExpect(jsonPath("$.recentMovements.length()").value(2));
    }

    @Test
    void getOverview_articleWithoutHistory_returnsEmptyBlocks() throws Exception {
        // Arrange
        var oil = createArticle("RAW_MATERIAL");

        // Act + Assert
        mockMvc.perform(overview(oil, adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock.total").value(0.0))
                .andExpect(jsonPath("$.stock.locations.length()").value(4))
                .andExpect(jsonPath("$.productionBatches.length()").value(0))
                .andExpect(jsonPath("$.recentMovements.length()").value(0));
    }

    private static String overviewUrl(UUID articleId) {
        return "/api/v1/articles/" + articleId + "/overview";
    }

    private static MockHttpServletRequestBuilder overview(UUID articleId, String token) {
        return get(overviewUrl(articleId)).header(HttpHeaders.AUTHORIZATION, bearer(token));
    }

    private static JsonPathResultMatchers promotionNamed(String name) {
        return jsonPath("$.activePromotions[?(@.name == '" + name + "')]");
    }

    /** @param type RAW_MATERIAL or FINISHED */
    private UUID createArticle(String type) throws Exception {
        var finished = "FINISHED".equals(type);
        var body = """
                {"name":"%s","type":"%s","unit":"%s","rangeId":%s,"cost":1200,"price":%s,"threshold":5}
                """.formatted(
                        "Article-" + UUID.randomUUID(),
                        type,
                        finished ? "PIECE" : "KILOGRAM",
                        finished ? "\"" + rangeId() + "\"" : "null",
                        finished ? "8500" : "null");
        return UUID.fromString(JsonPath.read(postAsAdmin("/api/v1/articles", body), "$.id"));
    }

    /** Plans and completes a batch in one call (PRD-02): 12 outputs enter the warehouse, 3 inputs leave it. */
    private String completeBatch(UUID output, UUID input, UUID operatorId) throws Exception {
        var body = """
                {"outputArticleId":"%s","quantity":12,"operatorId":"%s",
                 "inputs":[{"articleId":"%s","quantity":3}]}
                """.formatted(output, operatorId, input);
        return JsonPath.read(postAsAdmin("/api/v1/production/completed-batches", body), "$.number");
    }

    private String postAsAdmin(String url, String body) throws Exception {
        MvcResult result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isCreated())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private void move(UUID articleId, String locationCode, StockMovementType type, String quantity) {
        var locationId = jdbcTemplate.queryForObject(LOCATION_BY_CODE, UUID.class, locationCode);
        stockMovementService.record(
                new StockMovementCommand(articleId, locationId, type, new BigDecimal(quantity), null));
    }

    /** Saved directly: an ended period cannot be created through the API (end before today, PRO-01). */
    private void save(String name, PromotionScope scope, Set<UUID> targetIds, PromotionPeriod period) {
        promotionRepository.save(Promotion.create(new PromotionDetails(
                name,
                null,
                PromotionKind.PERCENT,
                new BigDecimal("10"),
                null,
                scope,
                targetIds,
                period,
                Set.of(channelId()),
                null)));
    }

    private UUID rangeId() {
        return jdbcTemplate.queryForObject(FIRST_RANGE, UUID.class);
    }

    private UUID channelId() {
        return jdbcTemplate.queryForObject(FIRST_CHANNEL, UUID.class);
    }

    private String adminToken() throws Exception {
        return accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    private String accessToken(String email, String password) throws Exception {
        var body = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private User createUser(RoleName roleName) {
        var email = "fiche-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(roleName).orElseThrow();
        var terms =
                roleName == RoleName.SELLER ? new SellerTerms(channelId(), new BigDecimal("5.00")) : SellerTerms.NONE;
        return userRepository.save(User.create(
                new UserProfile(email, "Tiana", "Ranaivoson"), passwordEncoder.encode(PASSWORD), role, terms));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
