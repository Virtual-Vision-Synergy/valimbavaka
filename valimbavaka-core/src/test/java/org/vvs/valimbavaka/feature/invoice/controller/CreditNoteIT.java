package org.vvs.valimbavaka.feature.invoice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

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

/** FAC-04: a motivated credit note reverses an issued invoice, cancels its sale and puts the articles back. */
class CreditNoteIT extends StockApiTestSupport {

    private static final String INVOICES = "/api/v1/invoices";
    private static final String SALES = "/api/v1/sales";
    private static final String SELLER_PASSWORD = "mot-de-passe-vendeuse-long";
    private static final String NUMBER_FORMAT = "AV-\\d{4}-\\d{4,}";
    private static final String REASON = "Produit défectueux rapporté par la cliente";

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SaleService saleService;

    private User seller;
    private String sellerToken;

    @BeforeEach
    void openTodayAndCreateSeller() throws Exception {
        endAutomaticPromotions();
        var status = openDay().andReturn().getResponse().getStatus();
        assertThat(status).isIn(201, 409);
        seller = createSeller("Miora");
        sellerToken = login(seller.getEmail());
    }

    @Test
    void issueCreditNote_validInvoice_returns201WithTheFrozenContentOfTheInvoice() throws Exception {
        // Arrange
        var soap = stockedArticle("10");
        var sale = recordSale(sellerToken, soap, "3");
        var invoice = invoiceOf(sale);

        // Act
        issue(adminToken(), invoice, "{\"reason\":\"  " + REASON + "  \"}")
                // Assert
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, INVOICES + "/" + invoice + "/credit-note"))
                .andExpect(jsonPath("$.number").value(matchesPattern(NUMBER_FORMAT)))
                .andExpect(jsonPath("$.invoiceId").value(invoice.toString()))
                .andExpect(jsonPath("$.invoiceNumber").value(matchesPattern("FAC-\\d{4}-\\d{4,}")))
                .andExpect(jsonPath("$.saleId").value(sale.toString()))
                .andExpect(jsonPath("$.reason").value(REASON))
                .andExpect(jsonPath("$.issuerName").value("By Valimbavaka Cosmetics"))
                .andExpect(jsonPath("$.subtotal").value(25500.0))
                .andExpect(jsonPath("$.total").value(25500.0))
                .andExpect(jsonPath("$.lines.length()").value(1))
                .andExpect(jsonPath("$.lines[0].articleId").value(soap.toString()))
                .andExpect(jsonPath("$.lines[0].quantity").value(3.0))
                .andExpect(jsonPath("$.lines[0].lineTotal").value(25500.0));
    }

    @Test
    void issueCreditNote_validInvoice_reversesTheInvoiceCancelsTheSaleAndPutsTheArticlesBackInStock()
            throws Exception {
        // Arrange
        var soap = stockedArticle("10");
        var sale = recordSale(sellerToken, soap, "3");
        var invoice = invoiceOf(sale);
        var invoiceNumber = invoiceNumber(sale);
        assertThat(balance(soap, "STORE")).isEqualByComparingTo("7");

        // Act
        issue(adminToken(), invoice, REASON).andExpect(status().isCreated());

        // Assert
        assertThat(balance(soap, "STORE")).isEqualByComparingTo("10");
        assertThat(saleStatus(sale)).isEqualTo("CANCELLED");
        assertThat(invoiceStatus(sale)).isEqualTo("REVERSED");
        assertThat(invoiceNumber(sale)).isEqualTo(invoiceNumber);
        mockMvc.perform(get(INVOICES + "/" + invoice).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVERSED"))
                .andExpect(jsonPath("$.total").value(25500.0));
        var returns = jdbcTemplate.queryForObject(
                "select count(*) from stock_movements where reference_id = ? and type = 'RETURN' and quantity = 3",
                Integer.class,
                sale);
        assertThat(returns).isEqualTo(1);
    }

    @Test
    void issueCreditNote_validInvoice_tracesTheReversalAndTheCancellationInTheJournal() throws Exception {
        // Arrange
        var sale = recordSale(sellerToken, stockedArticle("10"), "1");
        var invoice = invoiceOf(sale);

        // Act
        issue(adminToken(), invoice, REASON).andExpect(status().isCreated());

        // Assert
        var reversal = jdbcTemplate.queryForObject(
                "select description from journal_entries where resource_type = 'INVOICE' and resource_id = ? "
                        + "and action = 'REVERSE'",
                String.class,
                invoice);
        assertThat(reversal).startsWith("Avoir AV-").contains("de la facture FAC-").endsWith(" : " + REASON);
        var cancellation = jdbcTemplate.queryForObject(
                "select description from journal_entries where resource_type = 'SALE' and resource_id = ? "
                        + "and action = 'CANCEL'",
                String.class,
                sale);
        assertThat(cancellation).contains("avoir AV-").endsWith(REASON);
    }

    @Test
    void issueCreditNote_twoInvoices_getConsecutiveIncreasingNumbers() throws Exception {
        // Arrange
        var soap = stockedArticle("10");
        var first = invoiceOf(recordSale(sellerToken, soap, "1"));
        var second = invoiceOf(recordSale(sellerToken, soap, "1"));

        // Act
        var firstNumber = number(issue(adminToken(), first, REASON));
        var secondNumber = number(issue(adminToken(), second, REASON));

        // Assert
        assertThat(sequenceOf(secondNumber)).isEqualTo(sequenceOf(firstNumber) + 1);
    }

    @Test
    void issueCreditNote_reasonOfExactlyTheMaximumLength_returns201() throws Exception {
        // Arrange
        var invoice = invoiceOf(recordSale(sellerToken, stockedArticle("10"), "1"));

        // Act + Assert
        issue(adminToken(), invoice, "x".repeat(500)).andExpect(status().isCreated());
    }

    @Test
    void issueCreditNote_reasonOneCharacterTooLong_returns422AndChangesNothing() throws Exception {
        // Arrange
        var soap = stockedArticle("10");
        var sale = recordSale(sellerToken, soap, "2");

        // Act
        issue(adminToken(), invoiceOf(sale), "x".repeat(501))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        // Assert
        assertNothingChanged(sale, soap, "8");
    }

    @Test
    void issueCreditNote_missingOrBlankReason_returns422AndChangesNothing() throws Exception {
        // Arrange
        var soap = stockedArticle("10");
        var sale = recordSale(sellerToken, soap, "2");
        var invoice = invoiceOf(sale);

        // Act + Assert
        mockMvc.perform(creditNoteRequest(adminToken(), invoice, "{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        issue(adminToken(), invoice, "   ")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        assertNothingChanged(sale, soap, "8");
    }

    @Test
    void issueCreditNote_alreadyCredited_returns409AndUsesNoSecondNumberNorSecondRestock() throws Exception {
        // Arrange
        var soap = stockedArticle("10");
        var sale = recordSale(sellerToken, soap, "2");
        var invoice = invoiceOf(sale);
        issue(adminToken(), invoice, REASON).andExpect(status().isCreated());
        var counter = creditNoteCounter();

        // Act
        issue(adminToken(), invoice, "Deuxième tentative")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        // Assert
        assertThat(creditNoteCounter()).isEqualTo(counter);
        assertThat(creditNotesOf(invoice)).isEqualTo(1);
        assertThat(balance(soap, "STORE")).isEqualByComparingTo("10");
    }

    @Test
    void issueCreditNote_dayOfTheSaleClosed_returns409AndChangesNothing() throws Exception {
        // Arrange
        var soap = stockedArticle("10");
        var sale = recordSale(sellerToken, soap, "2");
        var invoice = invoiceOf(sale);
        var openDayId = jdbcTemplate.queryForObject("select business_day_id from sales where id = ?", UUID.class, sale);
        var closedDayId = closedDay();
        jdbcTemplate.update("update sales set business_day_id = ? where id = ?", closedDayId, sale);
        var counter = creditNoteCounter();

        // Act
        try {
            issue(adminToken(), invoice, REASON)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLICT"));
        } finally {
            jdbcTemplate.update("update sales set business_day_id = ? where id = ?", openDayId, sale);
            jdbcTemplate.update("delete from business_days where id = ?", closedDayId);
        }

        // Assert
        assertNothingChanged(sale, soap, "8");
        assertThat(creditNoteCounter()).isEqualTo(counter);
    }

    @Test
    void issueCreditNote_twoRequestsAtTheSameTime_issuesOneCreditNoteAndRejectsTheOther() throws Exception {
        // Arrange
        var soap = stockedArticle("10");
        var sale = recordSale(sellerToken, soap, "4");
        var invoice = invoiceOf(sale);
        var admin = adminToken();
        var executor = Executors.newFixedThreadPool(2);
        List<Callable<Integer>> tasks = List.of(
                () -> issue(admin, invoice, "Demande 1").andReturn().getResponse().getStatus(),
                () -> issue(admin, invoice, "Demande 2").andReturn().getResponse().getStatus());

        // Act
        var statuses = new ArrayList<Integer>();
        try {
            for (var future : executor.invokeAll(tasks)) {
                statuses.add(future.get());
            }
        } finally {
            executor.shutdown();
        }

        // Assert
        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        assertThat(creditNotesOf(invoice)).isEqualTo(1);
        assertThat(balance(soap, "STORE")).isEqualByComparingTo("10");
    }

    @Test
    void issueCreditNote_bodyWithForgedFields_isIgnored() throws Exception {
        // Arrange
        var sale = recordSale(sellerToken, stockedArticle("10"), "1");
        var invoice = invoiceOf(sale);
        var body = "{\"reason\":\"%s\",\"id\":\"%s\",\"invoiceId\":\"%s\",\"number\":\"AV-1999-0001\","
                        .formatted(REASON, UUID.randomUUID(), UUID.randomUUID())
                + "\"total\":1,\"lines\":[{\"label\":\"Libre\"}]}";

        // Act
        issue(adminToken(), invoice, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value(matchesPattern(NUMBER_FORMAT)))
                .andExpect(jsonPath("$.invoiceId").value(invoice.toString()))
                .andExpect(jsonPath("$.total").value(8500.0))
                .andExpect(jsonPath("$.lines[0].label").isNotEmpty());
    }

    @Test
    void issueCreditNote_saleAlreadyCancelledBeforeFac04_issuesItWithoutPuttingTheArticlesBackTwice()
            throws Exception {
        // Arrange: a sale cancelled by the undo of LIV-05 before credit notes existed leaves a VALIDATED invoice
        var soap = stockedArticle("10");
        var sale = recordSale(sellerToken, soap, "2");
        jdbcTemplate.update("update sales set status = 'CANCELLED' where id = ?", sale);

        // Act
        issue(adminToken(), invoiceOf(sale), REASON).andExpect(status().isCreated());

        // Assert
        assertThat(invoiceStatus(sale)).isEqualTo("REVERSED");
        assertThat(balance(soap, "STORE")).isEqualByComparingTo("8");
    }

    @Test
    void cancelSale_saleWithAnInvoice_issuesItsCreditNoteAutomatically() throws Exception {
        // Arrange
        var soap = stockedArticle("10");
        var sale = recordSale(sellerToken, soap, "2");
        var invoice = invoiceOf(sale);

        // Act
        saleService.cancel(sale, "annulation de la livraison LIV-0042");

        // Assert
        assertThat(invoiceStatus(sale)).isEqualTo("REVERSED");
        assertThat(creditNotesOf(invoice)).isEqualTo(1);
        var reason = jdbcTemplate.queryForObject(
                "select reason from credit_notes where invoice_id = ?", String.class, invoice);
        assertThat(reason).startsWith("Annulation de la vente VTE-");
        assertThat(balance(soap, "STORE")).isEqualByComparingTo("10");
        var reversals = jdbcTemplate.queryForObject(
                "select count(*) from journal_entries where resource_type = 'INVOICE' and resource_id = ? "
                        + "and action = 'REVERSE'",
                Integer.class,
                invoice);
        assertThat(reversals).isEqualTo(1);
    }

    @Test
    void cancelSale_saleWithoutInvoice_cancelsWithoutCreditNote() throws Exception {
        // Arrange
        var soap = stockedArticle("10");
        var sale = recordSale(sellerToken, soap, "2");
        deleteInvoiceOf(sale);
        var creditNotes = countCreditNotes();

        // Act
        saleService.cancel(sale, "vente d'avant la facturation");

        // Assert
        assertThat(saleStatus(sale)).isEqualTo("CANCELLED");
        assertThat(countCreditNotes()).isEqualTo(creditNotes);
    }

    @Test
    void issueCreditNote_withoutToken_returns401() throws Exception {
        mockMvc.perform(post(INVOICES + "/" + UUID.randomUUID() + "/credit-note")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void issueCreditNote_sellerOrDeliveryDriver_returns403AndChangesNothing() throws Exception {
        // Arrange
        var soap = stockedArticle("10");
        var sale = recordSale(sellerToken, soap, "2");
        var invoice = invoiceOf(sale);

        // Act + Assert
        issue(sellerToken, invoice, REASON)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        issue(tokenOf(RoleName.DELIVERY_DRIVER), invoice, REASON)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        assertNothingChanged(sale, soap, "8");
    }

    @Test
    void issueCreditNote_unknownInvoice_returns404() throws Exception {
        issue(adminToken(), UUID.randomUUID(), REASON)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void issueCreditNote_malformedInvoiceId_returns400() throws Exception {
        mockMvc.perform(post(INVOICES + "/pas-un-uuid/credit-note")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void getCreditNote_afterTheIssue_isReadByTheAdministratorAndByTheSellerOfTheSale() throws Exception {
        // Arrange
        var invoice = invoiceOf(recordSale(sellerToken, stockedArticle("10"), "1"));
        var number = number(issue(adminToken(), invoice, REASON));

        // Act + Assert
        readCreditNote(adminToken(), invoice)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(number))
                .andExpect(jsonPath("$.reason").value(REASON));
        readCreditNote(sellerToken, invoice)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(number));
    }

    @Test
    void getCreditNote_sellerAskingForAColleagueInvoice_returns404() throws Exception {
        // Arrange
        var invoice = invoiceOf(recordSale(sellerToken, stockedArticle("10"), "1"));
        issue(adminToken(), invoice, REASON).andExpect(status().isCreated());
        var colleagueToken = login(createSeller("Lova").getEmail());

        // Act + Assert
        readCreditNote(colleagueToken, invoice)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void getCreditNote_invoiceWithoutCreditNote_returns404() throws Exception {
        // Arrange
        var invoice = invoiceOf(recordSale(sellerToken, stockedArticle("10"), "1"));

        // Act + Assert
        readCreditNote(adminToken(), invoice)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void getCreditNote_withoutTokenOrAsDeliveryDriver_returns401Or403() throws Exception {
        mockMvc.perform(get(INVOICES + "/" + UUID.randomUUID() + "/credit-note"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        readCreditNote(tokenOf(RoleName.DELIVERY_DRIVER), UUID.randomUUID())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private void assertNothingChanged(UUID sale, UUID article, String expectedStock) {
        assertThat(invoiceStatus(sale)).isEqualTo("VALIDATED");
        assertThat(saleStatus(sale)).isEqualTo("VALIDATED");
        assertThat(balance(article, "STORE")).isEqualByComparingTo(expectedStock);
        assertThat(creditNotesOfSale(sale)).isZero();
    }

    private UUID stockedArticle(String quantity) throws Exception {
        var article = createArticle(uniqueName());
        move(article, "STORE", StockMovementType.RECEIPT, quantity);
        return article;
    }

    private ResultActions issue(String token, UUID invoiceId, String reasonOrBody) throws Exception {
        var body = reasonOrBody.startsWith("{") ? reasonOrBody : "{\"reason\":\"%s\"}".formatted(reasonOrBody);
        return mockMvc.perform(creditNoteRequest(token, invoiceId, body));
    }

    private static MockHttpServletRequestBuilder creditNoteRequest(
            String token, UUID invoiceId, String body) {
        return post(INVOICES + "/" + invoiceId + "/credit-note")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private ResultActions readCreditNote(String token, UUID invoiceId) throws Exception {
        return mockMvc.perform(
                get(INVOICES + "/" + invoiceId + "/credit-note").header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private static String number(ResultActions actions) throws Exception {
        var json = actions.andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(json, "$.number");
    }

    /** {@code AV-2026-0042} gives 42. */
    private static long sequenceOf(String number) {
        return Long.parseLong(number.substring(number.lastIndexOf('-') + 1));
    }

    private UUID invoiceOf(UUID saleId) {
        return jdbcTemplate.queryForObject("select id from invoices where sale_id = ?", UUID.class, saleId);
    }

    private String invoiceNumber(UUID saleId) {
        return jdbcTemplate.queryForObject("select number from invoices where sale_id = ?", String.class, saleId);
    }

    private String invoiceStatus(UUID saleId) {
        return jdbcTemplate.queryForObject("select status from invoices where sale_id = ?", String.class, saleId);
    }

    private String saleStatus(UUID saleId) {
        return jdbcTemplate.queryForObject("select status from sales where id = ?", String.class, saleId);
    }

    private int creditNotesOf(UUID invoiceId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from credit_notes where invoice_id = ?", Integer.class, invoiceId);
    }

    private int creditNotesOfSale(UUID saleId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from credit_notes where sale_id = ?", Integer.class, saleId);
    }

    private int countCreditNotes() {
        return jdbcTemplate.queryForObject("select count(*) from credit_notes", Integer.class);
    }

    private long creditNoteCounter() {
        return jdbcTemplate.queryForObject(
                "select coalesce(max(last_value), 0) from credit_note_counters "
                        + "where year = extract(year from now())::int",
                Long.class);
    }

    private BigDecimal balance(UUID articleId, String locationCode) {
        return jdbcTemplate.queryForObject(
                "select coalesce(sum(quantity), 0) from stock_movements where article_id = ? and location_id = ?",
                BigDecimal.class,
                articleId,
                location(locationCode));
    }

    /** A closed business day of the year 2000 (reused if an earlier test left it). */
    private UUID closedDay() {
        jdbcTemplate.update(
                "insert into business_days (id, business_date, status, created_at, updated_at) "
                        + "values (gen_random_uuid(), date '2000-01-01', 'CLOSED', now(), now()) "
                        + "on conflict (business_date) do nothing");
        return jdbcTemplate.queryForObject(
                "select id from business_days where business_date = date '2000-01-01'", UUID.class);
    }

    private void deleteInvoiceOf(UUID saleId) {
        jdbcTemplate.update(
                "delete from invoice_lines where invoice_id in (select id from invoices where sale_id = ?)", saleId);
        jdbcTemplate.update("delete from invoices where sale_id = ?", saleId);
    }

    private UUID recordSale(String token, UUID articleId, String quantity) throws Exception {
        var body = "{\"salesChannelId\":\"%s\",\"sellerId\":\"%s\",\"paymentMethod\":\"CASH\",\"lines\":[%s]}"
                .formatted(
                        channel("STORE"),
                        seller.getId(),
                        "{\"articleId\":\"%s\",\"quantity\":%s}".formatted(articleId, quantity));
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

    private ResultActions openDay() throws Exception {
        return mockMvc.perform(post("/api/v1/days")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lines\":[]}"));
    }

    private UUID channel(String code) {
        return jdbcTemplate.queryForObject("select id from sales_channels where code = ?", UUID.class, code);
    }

    private User createSeller(String firstName) {
        var email = "vendeuse-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(RoleName.SELLER).orElseThrow();
        var profile = new UserProfile(email, firstName, "Rakoto");
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
