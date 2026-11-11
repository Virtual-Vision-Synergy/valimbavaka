package org.vvs.valimbavaka.feature.commission.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
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
 * COM-05: the administrator recalculates the computed commissions of a period after a rate changed. The most precise
 * rate wins, as at the calculation; validated and cancelled commissions are never touched; a second run changes
 * nothing. Each test works on its own sales channel and seller (the database is shared); a sale of one article brings
 * 8 500 Ar × 5 % = 425 Ar of commission.
 */
class CommissionRecalculationIT extends StockApiTestSupport {

    private static final String COMMISSIONS = "/api/v1/commissions";
    private static final String RECALCULATION = COMMISSIONS + "/recalculation";
    private static final String VALIDATION = COMMISSIONS + "/validation";
    private static final String FROM = "2020-01-01";
    private static final String TO = "2099-12-31";
    /** The recalculation is bounded to a year: a window around today (Madagascar day or UTC day). */
    private static final String PERIOD_FROM = LocalDate.now().minusDays(30).toString();

    private static final String PERIOD_TO = LocalDate.now().plusDays(30).toString();
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
        endAutomaticPromotions();
        channelId = UUID.randomUUID();
        var code = "IT-" + channelId.toString().substring(0, 8);
        jdbcTemplate.update("insert into sales_channels (id, code, label) values (?, ?, ?)", channelId, code, code);
        seller = createSeller(channelId);
        sellerId = seller.getId();
    }

    @Test
    void recalculate_sellerRateChanged_updatesTheCalculatedCommissions() throws Exception {
        // Arrange : une vente à 5 % (425 Ar), puis le taux de la vendeuse passe à 8 %
        createSale();
        setSellerRate("8.00");

        // Act + Assert : 8 500 × 8 % = 680 Ar
        recalculation(sellerId, PERIOD_FROM, PERIOD_TO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sellerId").value(sellerId.toString()))
                .andExpect(jsonPath("$.updatedCount").value(1))
                .andExpect(jsonPath("$.unchangedCount").value(0))
                .andExpect(jsonPath("$.validatedCount").value(0))
                .andExpect(jsonPath("$.previousAmount").value(425.0))
                .andExpect(jsonPath("$.newAmount").value(680.0));
        commissions()
                .andExpect(jsonPath("$.items[0].amount").value(680.0))
                .andExpect(jsonPath("$.items[0].rate").value(8.0))
                .andExpect(jsonPath("$.items[0].status").value("CALCULATED"));
    }

    @Test
    void recalculate_twice_secondRunChangesNothingAndWritesNoJournal() throws Exception {
        // Arrange
        var saleNumber = createSale();
        setSellerRate("8.00");
        recalculation(sellerId, PERIOD_FROM, PERIOD_TO).andExpect(jsonPath("$.updatedCount").value(1));

        // Act + Assert : rien à changer la deuxième fois
        recalculation(sellerId, PERIOD_FROM, PERIOD_TO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount").value(0))
                .andExpect(jsonPath("$.unchangedCount").value(1))
                .andExpect(jsonPath("$.previousAmount").value(680.0))
                .andExpect(jsonPath("$.newAmount").value(680.0));
        assertThat(recalculationEntries(saleNumber)).hasSize(1);
    }

    @Test
    void recalculate_changedCommission_isTracedInTheJournal() throws Exception {
        // Arrange
        var saleNumber = createSale();
        setSellerRate("8.00");

        // Act
        recalculation(sellerId, PERIOD_FROM, PERIOD_TO).andExpect(status().isOk());

        // Assert
        var expected = "Commission %s · Miora Rakoto recalculée : 425.00 → 680.00 Ar (taux 5.00 %% → 8.00 %%)"
                .formatted(saleNumber);
        assertThat(recalculationEntries(saleNumber)).containsExactly(expected);
    }

    @Test
    void recalculate_channelRuleCreatedAfterTheSale_winsOverTheSellerRateWithoutAccumulating() throws Exception {
        // Arrange : la règle du canal (3 % du CA) est la plus précise ; elle ne s'ajoute pas au taux de la vendeuse
        createSale();
        createChannelRule("3", "REVENUE");

        // Act + Assert : 8 500 × 3 % = 255 Ar (et non 5 % + 3 %)
        recalculation(sellerId, PERIOD_FROM, PERIOD_TO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount").value(1))
                .andExpect(jsonPath("$.newAmount").value(255.0));
        commissions()
                .andExpect(jsonPath("$.items[0].rateSource").value("SALES_CHANNEL"))
                .andExpect(jsonPath("$.items[0].amount").value(255.0));
    }

    @Test
    void recalculate_validatedCommission_isLeftUntouchedAndCounted() throws Exception {
        // Arrange : la commission est validée (définitive), puis le taux change
        createSale();
        validate();
        setSellerRate("8.00");

        // Act + Assert
        recalculation(sellerId, PERIOD_FROM, PERIOD_TO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount").value(0))
                .andExpect(jsonPath("$.unchangedCount").value(0))
                .andExpect(jsonPath("$.validatedCount").value(1));
        commissions()
                .andExpect(jsonPath("$.items[0].amount").value(425.0))
                .andExpect(jsonPath("$.items[0].status").value("VALIDATED"));
    }

    @Test
    void recalculate_cancelledCommission_isLeftUntouched() throws Exception {
        // Arrange : la vente est annulée (avoir), puis le taux change
        var saleNumber = createSale();
        cancelSale(saleNumber);
        setSellerRate("8.00");

        // Act + Assert
        recalculation(sellerId, PERIOD_FROM, PERIOD_TO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount").value(0))
                .andExpect(jsonPath("$.validatedCount").value(0));
        commissions()
                .andExpect(jsonPath("$.items[0].amount").value(425.0))
                .andExpect(jsonPath("$.items[0].status").value("CANCELLED"));
    }

    @Test
    void recalculate_oneSellerOnly_doesNotTouchTheOthers() throws Exception {
        // Arrange : deux vendeuses sur le même canal, le taux des deux passe à 8 %
        var other = createSeller(channelId);
        createSale();
        createSale(other.getId());
        setSellerRate("8.00");
        jdbcTemplate.update("update users set commission_rate = 8.00 where id = ?", other.getId());

        // Act
        recalculation(sellerId, PERIOD_FROM, PERIOD_TO).andExpect(jsonPath("$.updatedCount").value(1));

        // Assert : seule la vendeuse désignée est recalculée
        commissions().andExpect(jsonPath("$.items[0].amount").value(680.0));
        mockMvc.perform(get(COMMISSIONS)
                        .param("sellerId", other.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(jsonPath("$.items[0].amount").value(425.0));
    }

    @Test
    void recalculate_periodWithoutTheCommissions_changesNothing() throws Exception {
        // Arrange
        createSale();
        setSellerRate("8.00");

        // Act + Assert : période bornée dans le passé, sans vendeuse désignée
        mockMvc.perform(post(RECALCULATION)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"2020-01-01\",\"to\":\"2020-01-31\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount").value(0))
                .andExpect(jsonPath("$.unchangedCount").value(0));
        commissions().andExpect(jsonPath("$.items[0].amount").value(425.0));
    }

    @Test
    void recalculate_invalidPeriodOrMissingField_returns422() throws Exception {
        recalculation(sellerId, "2026-11-30", "2026-11-01")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        recalculation(sellerId, "2025-01-01", "2026-12-31")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(post(RECALCULATION)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sellerId\":\"%s\",\"from\":\"%s\"}".formatted(sellerId, FROM)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void recalculate_extraFieldsInTheBody_areIgnored() throws Exception {
        // Arrange
        createSale();
        setSellerRate("8.00");
        var body = "{\"sellerId\":\"%s\",\"from\":\"%s\",\"to\":\"%s\",\"status\":\"VALIDATED\",\"amount\":1}"
                .formatted(sellerId, PERIOD_FROM, PERIOD_TO);

        // Act + Assert : le statut et le montant viennent du serveur
        mockMvc.perform(post(RECALCULATION)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newAmount").value(680.0));
        commissions().andExpect(jsonPath("$.items[0].status").value("CALCULATED"));
    }

    @Test
    void recalculate_withoutToken_returns401() throws Exception {
        mockMvc.perform(post(RECALCULATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(sellerId, PERIOD_FROM, PERIOD_TO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void recalculate_sellerOrDeliveryDriver_returns403AndChangesNothing() throws Exception {
        // Arrange : une vendeuse ne recalcule pas ses propres commissions
        createSale();
        setSellerRate("8.00");
        var sellerToken = login(seller.getEmail());
        var driverToken = tokenOf(RoleName.DELIVERY_DRIVER);

        // Act + Assert
        for (var token : new String[] {sellerToken, driverToken}) {
            mockMvc.perform(post(RECALCULATION)
                            .header(HttpHeaders.AUTHORIZATION, bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(sellerId, PERIOD_FROM, PERIOD_TO)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }
        commissions().andExpect(jsonPath("$.items[0].amount").value(425.0));
    }

    private ResultActions commissions() throws Exception {
        return mockMvc.perform(get(COMMISSIONS)
                .param("sellerId", sellerId.toString())
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())));
    }

    private ResultActions recalculation(UUID id, String from, String to) throws Exception {
        return mockMvc.perform(post(RECALCULATION)
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(id, from, to)));
    }

    private static String body(UUID id, String from, String to) {
        return "{\"sellerId\":\"%s\",\"from\":\"%s\",\"to\":\"%s\"}".formatted(id, from, to);
    }

    private void validate() throws Exception {
        mockMvc.perform(post(VALIDATION)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(sellerId, FROM, TO)))
                .andExpect(status().isOk());
    }

    private void setSellerRate(String rate) {
        jdbcTemplate.update("update users set commission_rate = ? where id = ?", new BigDecimal(rate), sellerId);
    }

    private void createChannelRule(String rate, String base) throws Exception {
        var json = "{\"name\":\"Regle-%s\",\"salesChannelId\":\"%s\",\"rate\":%s,\"base\":\"%s\"}"
                .formatted(UUID.randomUUID(), channelId, rate, base);
        mockMvc.perform(post("/api/v1/commission-rules")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());
    }

    private List<String> recalculationEntries(String saleNumber) {
        return jdbcTemplate.queryForList(
                "select description from journal_entries where resource_type = 'COMMISSION' and action = 'UPDATE' "
                        + "and description like ?",
                String.class,
                "Commission " + saleNumber + " · %recalculée%");
    }

    private void cancelSale(String saleNumber) {
        var saleId = jdbcTemplate.queryForObject("select id from sales where number = ?", UUID.class, saleNumber);
        saleService.cancel(saleId, "avoir");
    }

    private String createSale() throws Exception {
        return createSale(sellerId);
    }

    /** One-article sale on the test channel, entered by the administrator for the seller; returns its number. */
    private String createSale(UUID creditedSellerId) throws Exception {
        var articleId = createArticle(uniqueName());
        move(articleId, "STORE", StockMovementType.RECEIPT, "10");
        var line = "{\"articleId\":\"%s\",\"quantity\":1}".formatted(articleId);
        var body = "{\"salesChannelId\":\"%s\",\"sellerId\":\"%s\",\"paymentMethod\":\"CASH\",\"lines\":[%s]}"
                .formatted(channelId, creditedSellerId, line);
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
