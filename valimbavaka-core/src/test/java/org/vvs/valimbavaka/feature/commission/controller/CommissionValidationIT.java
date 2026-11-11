package org.vvs.valimbavaka.feature.commission.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
 * COM-03: the administrator validates the commissions of a seller over a period; the status to validate / validated is
 * shown by seller, cancelled commissions are never counted nor validated. Each test works on its own sales channel and
 * seller (the database is shared); a sale of one article brings 8 500 Ar × 5 % = 425 Ar of commission.
 */
class CommissionValidationIT extends StockApiTestSupport {

    private static final String COMMISSIONS = "/api/v1/commissions";
    private static final String SELLERS = COMMISSIONS + "/sellers";
    private static final String VALIDATION = COMMISSIONS + "/validation";
    private static final String FROM = "2020-01-01";
    private static final String TO = "2099-12-31";
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
    void getSellers_sellerWithComputedCommissions_isToValidateWithCountAndTotal() throws Exception {
        // Arrange : deux ventes de 8 500 Ar, 425 Ar de commission chacune
        createSale();
        createSale();

        // Act + Assert
        sellers()
                .andExpect(status().isOk())
                .andExpect(jsonPath(mine("sellerName")).value("Miora Rakoto"))
                .andExpect(jsonPath(mine("status")).value("TO_VALIDATE"))
                .andExpect(jsonPath(mine("toValidateCount")).value(2))
                .andExpect(jsonPath(mine("toValidateAmount")).value(850.0))
                .andExpect(jsonPath(mine("validatedCount")).value(0))
                .andExpect(jsonPath(mine("validatedAmount")).value(0.0));
    }

    @Test
    void validate_pendingCommissions_returnsCountAndTotalAndSellerBecomesValidated() throws Exception {
        // Arrange
        createSale();
        createSale();

        // Act + Assert
        validation(sellerId, FROM, TO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sellerId").value(sellerId.toString()))
                .andExpect(jsonPath("$.sellerName").value("Miora Rakoto"))
                .andExpect(jsonPath("$.validatedCount").value(2))
                .andExpect(jsonPath("$.validatedAmount").value(850.0));
        sellers()
                .andExpect(jsonPath(mine("status")).value("VALIDATED"))
                .andExpect(jsonPath(mine("toValidateCount")).value(0))
                .andExpect(jsonPath(mine("validatedCount")).value(2))
                .andExpect(jsonPath(mine("validatedAmount")).value(850.0));
        mockMvc.perform(get(COMMISSIONS)
                        .param("sellerId", sellerId.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(jsonPath("$.items[0].status").value("VALIDATED"));
    }

    @Test
    void validate_validatedCommissions_areTracedInTheJournal() throws Exception {
        // Arrange
        var saleNumber = createSale();

        // Act
        validation(sellerId, FROM, TO).andExpect(status().isOk());

        // Assert
        var descriptions = jdbcTemplate.queryForList(
                "select description from journal_entries where resource_type = 'COMMISSION' and action = 'UPDATE' "
                        + "and description like ?",
                String.class,
                "Commission " + saleNumber + " %");
        var expected = "Commission %s · Miora Rakoto validée : 425.00 Ar".formatted(saleNumber);
        assertThat(descriptions).containsExactly(expected);
    }

    @Test
    void validate_twice_returns409() throws Exception {
        // Arrange
        createSale();
        validation(sellerId, FROM, TO).andExpect(status().isOk());

        // Act + Assert : une commission validée est définitive
        validation(sellerId, FROM, TO)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void validate_newSaleAfterAValidation_validatesOnlyTheNewCommission() throws Exception {
        // Arrange
        createSale();
        validation(sellerId, FROM, TO).andExpect(status().isOk());
        createSale();

        // Act + Assert
        sellers().andExpect(jsonPath(mine("status")).value("TO_VALIDATE"));
        validation(sellerId, FROM, TO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validatedCount").value(1))
                .andExpect(jsonPath("$.validatedAmount").value(425.0));
    }

    @Test
    void validate_periodWithoutTheCommissions_returns404AndValidatesNothing() throws Exception {
        // Arrange
        createSale();

        // Act + Assert
        validation(sellerId, "2020-01-01", "2020-01-31")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        sellers().andExpect(jsonPath(mine("toValidateCount")).value(1));
    }

    @Test
    void validate_unknownSeller_returns404() throws Exception {
        validation(UUID.randomUUID(), FROM, TO)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void validate_cancelledCommission_isNeitherCountedNorValidated() throws Exception {
        // Arrange : la seule vente est annulée (comme le fait un avoir, FAC-04)
        cancelSale(createSale());

        // Act + Assert : rien à valider, la vendeuse n'apparaît plus dans les totaux
        validation(sellerId, FROM, TO).andExpect(status().isNotFound());
        sellers().andExpect(jsonPath(entry()).isEmpty());
    }

    @Test
    void validate_oneSaleCancelledOneKept_validatesOnlyTheKeptOne() throws Exception {
        // Arrange
        cancelSale(createSale());
        createSale();

        // Act + Assert
        validation(sellerId, FROM, TO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validatedCount").value(1))
                .andExpect(jsonPath("$.validatedAmount").value(425.0));
    }

    @Test
    void cancel_saleWithAValidatedCommission_commissionIsCancelledKeepsItsDateAndJournalSaysSo() throws Exception {
        // Arrange : commission validée, puis la vente est annulée (avoir)
        var saleNumber = createSale();
        validation(sellerId, FROM, TO).andExpect(status().isOk());

        // Act
        cancelSale(saleNumber);

        // Assert : hypothèse prudente, CANCELLED mais la date de validation reste, et le journal le dit
        var row = jdbcTemplate.queryForMap(
                "select status, validated_at from commissions where source_number = ?", saleNumber);
        assertThat(row.get("status")).isEqualTo("CANCELLED");
        assertThat(row.get("validated_at")).isNotNull();
        var cancellations = jdbcTemplate.queryForObject(
                "select count(*) from journal_entries where resource_type = 'COMMISSION' and action = 'CANCEL' "
                        + "and description like ?",
                Integer.class,
                "% annulée avec l'opération " + saleNumber + " (déjà validée%");
        assertThat(cancellations).isEqualTo(1);
        sellers().andExpect(jsonPath(entry()).isEmpty());
    }

    @Test
    void validate_fromAfterToOrMissingField_returns422() throws Exception {
        validation(sellerId, "2026-11-30", "2026-11-01")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(post(VALIDATION)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"%s\",\"to\":\"%s\"}".formatted(FROM, TO)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void getSellers_fromAfterToOrSizeAbove100_returns422() throws Exception {
        mockMvc.perform(get(SELLERS)
                        .param("from", "2026-11-30")
                        .param("to", "2026-11-01")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(get(SELLERS)
                        .param("from", FROM)
                        .param("to", TO)
                        .param("size", "101")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void validate_extraFieldsInTheBody_areIgnored() throws Exception {
        // Arrange
        createSale();
        var body = "{\"sellerId\":\"%s\",\"from\":\"%s\",\"to\":\"%s\",\"status\":\"CANCELLED\",\"amount\":1}"
                .formatted(sellerId, FROM, TO);

        // Act + Assert : le statut et le montant viennent du serveur
        mockMvc.perform(post(VALIDATION)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validatedAmount").value(425.0));
    }

    @Test
    void commissionsValidation_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(SELLERS).param("from", FROM).param("to", TO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mockMvc.perform(post(VALIDATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(sellerId, FROM, TO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void commissionsValidation_sellerOrDeliveryDriver_returns403AndValidatesNothing() throws Exception {
        // Arrange : une vendeuse ne valide pas ses propres commissions
        createSale();
        var sellerToken = login(seller.getEmail());
        var driverToken = tokenOf(RoleName.DELIVERY_DRIVER);

        // Act + Assert
        for (var token : new String[] {sellerToken, driverToken}) {
            mockMvc.perform(post(VALIDATION)
                            .header(HttpHeaders.AUTHORIZATION, bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(sellerId, FROM, TO)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
            mockMvc.perform(get(SELLERS)
                            .param("from", FROM)
                            .param("to", TO)
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isForbidden());
        }
        sellers().andExpect(jsonPath(mine("toValidateCount")).value(1));
    }

    /** JSON path of the seller under test among the sellers of the page. */
    private String entry() {
        return "$.items[?(@.sellerId == '%s')]".formatted(sellerId);
    }

    /** JSON path of a field of the seller under test among the sellers of the page. */
    private String mine(String field) {
        return "$.items[?(@.sellerId == '%s')].%s".formatted(sellerId, field);
    }

    private ResultActions sellers() throws Exception {
        return mockMvc.perform(get(SELLERS)
                .param("from", FROM)
                .param("to", TO)
                .param("size", "100")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())));
    }

    private ResultActions validation(UUID id, String from, String to) throws Exception {
        return mockMvc.perform(post(VALIDATION)
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(id, from, to)));
    }

    private static String body(UUID id, String from, String to) {
        return "{\"sellerId\":\"%s\",\"from\":\"%s\",\"to\":\"%s\"}".formatted(id, from, to);
    }

    private void cancelSale(String saleNumber) {
        var saleId = jdbcTemplate.queryForObject("select id from sales where number = ?", UUID.class, saleNumber);
        saleService.cancel(saleId, "avoir");
    }

    /** One-article sale on the test channel, entered by the administrator for the seller; returns its number. */
    private String createSale() throws Exception {
        var articleId = createArticle(uniqueName());
        move(articleId, "STORE", StockMovementType.RECEIPT, "10");
        var line = "{\"articleId\":\"%s\",\"quantity\":1}".formatted(articleId);
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
