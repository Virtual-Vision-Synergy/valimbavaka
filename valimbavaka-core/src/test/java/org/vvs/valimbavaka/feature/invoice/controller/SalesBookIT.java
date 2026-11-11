package org.vvs.valimbavaka.feature.invoice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.stock.controller.StockApiTestSupport;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.domain.SellerTerms;
import org.vvs.valimbavaka.feature.user.domain.User;
import org.vvs.valimbavaka.feature.user.domain.UserProfile;
import org.vvs.valimbavaka.feature.user.repository.RoleRepository;
import org.vvs.valimbavaka.feature.user.repository.UserRepository;
import org.vvs.valimbavaka.shared.excel.web.ExcelHttpMessageConverter;

/**
 * FAC-06: the administrator lists and exports the sales book of a period; the invoice and its credit note are two
 * lines, the credit note in negative. The database is shared with other classes: each test searches for the numbers
 * of its own invoice, and never counts the lines of a whole period.
 */
class SalesBookIT extends StockApiTestSupport {

    private static final String BOOK = "/api/v1/invoices/sales-book";
    private static final String SALES = "/api/v1/sales";
    private static final String SELLER_PASSWORD = "mot-de-passe-vendeuse-long";
    private static final List<String> HEADERS = List.of(
            "Date",
            "Type",
            "Numéro",
            "Facture d'origine",
            "Vente",
            "Canal",
            "Vendeuse",
            "Règlement",
            "Statut",
            "Sous-total (Ar)",
            "Remise (Ar)",
            "Total (Ar)",
            "Motif de l'avoir");

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User seller;
    private String sellerToken;

    @BeforeEach
    void openTodayAndCreateSeller() throws Exception {
        endAutomaticPromotions();
        var status = openDay().andReturn().getResponse().getStatus();
        assertThat(status).isIn(201, 409);
        seller = createSeller();
        sellerToken = login(seller.getEmail());
    }

    @Test
    void getSalesBook_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(BOOK).param("from", "2026-01-01").param("to", "2026-01-31"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getSalesBook_withSellerRole_returns403() throws Exception {
        mockMvc.perform(bookRequest(sellerToken, "2026-01-01", "2026-01-31"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void exportSalesBook_withSellerRole_returns403() throws Exception {
        mockMvc.perform(exportRequest(sellerToken, "2026-01-01", "2026-01-31"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void exportSalesBook_withDeliveryDriverRole_returns403() throws Exception {
        mockMvc.perform(exportRequest(tokenOf(RoleName.DELIVERY_DRIVER), "2026-01-01", "2026-01-31"))
                .andExpect(status().isForbidden());
    }

    @Test
    void exportSalesBook_withoutPeriod_returns400() throws Exception {
        mockMvc.perform(get(BOOK + "/export").header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void getSalesBook_malformedDate_returns400() throws Exception {
        mockMvc.perform(bookRequest(adminToken(), "demain", "2026-01-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void getSalesBook_fromAfterTo_returns422() throws Exception {
        mockMvc.perform(bookRequest(adminToken(), "2026-02-01", "2026-01-31"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void exportSalesBook_periodOf367Days_returns422() throws Exception {
        mockMvc.perform(exportRequest(adminToken(), "2025-01-01", "2026-01-02"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void getSalesBook_periodOf366Days_returns200() throws Exception {
        mockMvc.perform(bookRequest(adminToken(), "2025-01-01", "2026-01-01")).andExpect(status().isOk());
    }

    @Test
    void getSalesBook_oneDayPeriod_returns200() throws Exception {
        mockMvc.perform(bookRequest(adminToken(), "2025-03-04", "2025-03-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void getSalesBook_validInvoice_listsOneInvoiceLineWithItsSnapshot() throws Exception {
        // Arrange
        var sale = recordSale(stockedArticle("10"), "3");
        var invoice = invoiceNumber(sale);

        // Act + Assert
        mockMvc.perform(bookRequest(adminToken(), businessDate(sale), businessDate(sale), invoice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].type").value("Facture"))
                .andExpect(jsonPath("$.items[0].number").value(invoice))
                .andExpect(jsonPath("$.items[0].invoiceNumber").doesNotExist())
                .andExpect(jsonPath("$.items[0].businessDate").value(businessDate(sale)))
                .andExpect(jsonPath("$.items[0].salesChannelLabel").isNotEmpty())
                .andExpect(jsonPath("$.items[0].sellerName").value(containsString("Rakoto")))
                .andExpect(jsonPath("$.items[0].status").value("Validée"))
                .andExpect(jsonPath("$.items[0].subtotal").value(25500.0))
                .andExpect(jsonPath("$.items[0].discountAmount").value(0.0))
                .andExpect(jsonPath("$.items[0].total").value(25500.0))
                .andExpect(jsonPath("$.items[0].reason").doesNotExist());
    }

    @Test
    void getSalesBook_reversedInvoice_listsTheInvoiceAndItsCreditNoteInNegative() throws Exception {
        // Arrange
        var sale = recordSale(stockedArticle("10"), "3");
        var invoice = invoiceNumber(sale);
        var creditNote = issueCreditNote(sale, "Produit défectueux");

        // Act + Assert
        mockMvc.perform(bookRequest(adminToken(), businessDate(sale), businessDate(sale), invoice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.items[0].type").value("Facture"))
                .andExpect(jsonPath("$.items[0].status").value("Contre-passée"))
                .andExpect(jsonPath("$.items[0].total").value(25500.0))
                .andExpect(jsonPath("$.items[1].type").value("Avoir"))
                .andExpect(jsonPath("$.items[1].number").value(creditNote))
                .andExpect(jsonPath("$.items[1].number").value(matchesPattern("AV-\\d{4}-\\d{4,}")))
                .andExpect(jsonPath("$.items[1].invoiceNumber").value(invoice))
                .andExpect(jsonPath("$.items[1].status").doesNotExist())
                .andExpect(jsonPath("$.items[1].reason").value("Produit défectueux"))
                .andExpect(jsonPath("$.items[1].subtotal").value(-25500.0))
                .andExpect(jsonPath("$.items[1].total").value(-25500.0))
                .andExpect(jsonPath("$.items[1].sellerName").value(containsString("Rakoto")));
    }

    @Test
    void getSalesBook_reversedInvoice_netTotalOfItsTwoLinesIsZero() throws Exception {
        // Arrange
        var sale = recordSale(stockedArticle("10"), "2");
        issueCreditNote(sale, "Erreur de saisie");

        // Act
        var json = mockMvc.perform(bookRequest(
                        adminToken(), businessDate(sale), businessDate(sale), saleNumber(sale)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert
        List<Number> totals = JsonPath.read(json, "$.items[*].total");
        var net = totals.stream()
                .map(total -> new BigDecimal(total.toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totals).hasSize(2);
        assertThat(net).isEqualByComparingTo("0");
    }

    @Test
    void getSalesBook_periodAfterTheInvoice_doesNotContainIt() throws Exception {
        // Arrange
        var sale = recordSale(stockedArticle("10"), "1");
        var day = LocalDate.parse(businessDate(sale));

        // Act + Assert
        mockMvc.perform(bookRequest(
                        adminToken(), day.plusDays(1).toString(), day.plusDays(10).toString(), invoiceNumber(sale)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void getSalesBook_periodEndingTheDayBefore_doesNotContainIt() throws Exception {
        // Arrange
        var sale = recordSale(stockedArticle("10"), "1");
        var day = LocalDate.parse(businessDate(sale));

        // Act + Assert
        mockMvc.perform(bookRequest(
                        adminToken(), day.minusDays(10).toString(), day.minusDays(1).toString(), invoiceNumber(sale)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0));
    }

    @Test
    void getSalesBook_pageSizeOne_servesTheTwoLinesOnTwoPagesInOrder() throws Exception {
        // Arrange
        var sale = recordSale(stockedArticle("10"), "1");
        var invoice = invoiceNumber(sale);
        issueCreditNote(sale, "Retour");
        var day = businessDate(sale);

        // Act + Assert
        mockMvc.perform(bookRequest(adminToken(), day, day, invoice).param("size", "1"))
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.items[0].type").value("Facture"));
        mockMvc.perform(bookRequest(adminToken(), day, day, invoice).param("size", "1").param("page", "2"))
                .andExpect(jsonPath("$.items[0].type").value("Avoir"));
    }

    @Test
    void getSalesBook_unknownSortField_isIgnored() throws Exception {
        mockMvc.perform(bookRequest(adminToken(), "2025-03-04", "2025-03-04").param("sortBy", "issuer_nif; drop"))
                .andExpect(status().isOk());
    }

    @Test
    void getSalesBook_searchNoMatch_returnsAnEmptyPage() throws Exception {
        // Arrange
        var sale = recordSale(stockedArticle("10"), "1");
        var day = businessDate(sale);

        // Act + Assert
        mockMvc.perform(bookRequest(adminToken(), day, day, "AUCUN-" + UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0));
    }

    @Test
    void exportSalesBook_byAdmin_returnsXlsxWithTheInvoiceAndTheNegativeCreditNote() throws Exception {
        // Arrange
        var sale = recordSale(stockedArticle("10"), "3");
        var invoice = invoiceNumber(sale);
        var creditNote = issueCreditNote(sale, "Produit défectueux");
        var day = businessDate(sale);

        // Act
        var bytes = mockMvc.perform(exportRequest(adminToken(), day, day).param("search", invoice))
                .andExpect(status().isOk())
                .andExpect(content().contentType(ExcelHttpMessageConverter.XLSX))
                .andExpect(header().string(
                                HttpHeaders.CONTENT_DISPOSITION,
                                allOf(startsWith("attachment"), containsString("livre-des-ventes.xlsx"))))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        // Assert
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheet("Livre des ventes");
            assertThat(texts(sheet.getRow(0))).containsExactlyElementsOf(HEADERS);
            assertThat(sheet.getLastRowNum()).isEqualTo(2);
            var invoiceRow = texts(sheet.getRow(1));
            assertThat(invoiceRow.subList(1, 3)).containsExactly("Facture", invoice);
            assertThat(invoiceRow.get(8)).isEqualTo("Contre-passée");
            assertThat(sheet.getRow(1).getCell(11).getNumericCellValue()).isEqualTo(25500.0);
            var creditRow = texts(sheet.getRow(2));
            assertThat(creditRow.subList(1, 4)).containsExactly("Avoir", creditNote, invoice);
            assertThat(creditRow.get(12)).isEqualTo("Produit défectueux");
            assertThat(sheet.getRow(2).getCell(11).getNumericCellValue()).isEqualTo(-25500.0);
        }
    }

    @Test
    void exportSalesBook_noLineInThePeriod_returnsTheHeaderRowOnly() throws Exception {
        // Act
        var bytes = mockMvc.perform(exportRequest(adminToken(), "2000-01-01", "2000-01-02"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        // Assert
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheet("Livre des ventes");
            assertThat(sheet.getLastRowNum()).isZero();
            assertThat(texts(sheet.getRow(0))).containsExactlyElementsOf(HEADERS);
        }
    }

    @Test
    void exportSalesBookPdf_byAdmin_returnsAPdfFile() throws Exception {
        // Arrange
        var sale = recordSale(stockedArticle("10"), "1");
        var day = businessDate(sale);

        // Act
        var bytes = mockMvc.perform(get(BOOK + "/export/pdf")
                        .param("from", day)
                        .param("to", day)
                        .param("search", invoiceNumber(sale))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        // Assert
        assertThat(new String(bytes, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    void exportSalesBook_sizeAboveMaxRows_returns422() throws Exception {
        mockMvc.perform(exportRequest(adminToken(), "2026-01-01", "2026-01-31").param("size", "10001"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private static MockHttpServletRequestBuilder bookRequest(String token, String from, String to) {
        return get(BOOK).param("from", from).param("to", to).header(HttpHeaders.AUTHORIZATION, bearer(token));
    }

    private static MockHttpServletRequestBuilder bookRequest(String token, String from, String to, String search) {
        return bookRequest(token, from, to).param("search", search);
    }

    private static MockHttpServletRequestBuilder exportRequest(String token, String from, String to) {
        return get(BOOK + "/export")
                .param("from", from)
                .param("to", to)
                .header(HttpHeaders.AUTHORIZATION, bearer(token));
    }

    private static List<String> texts(Row row) {
        var formatter = new DataFormatter(Locale.US);
        var values = new ArrayList<String>();
        for (int i = 0; i < row.getLastCellNum(); i++) {
            values.add(formatter.formatCellValue(row.getCell(i)));
        }
        return values;
    }

    private UUID stockedArticle(String quantity) throws Exception {
        var article = createArticle(uniqueName());
        move(article, "STORE", StockMovementType.RECEIPT, quantity);
        return article;
    }

    private String invoiceNumber(UUID saleId) {
        return jdbcTemplate.queryForObject("select number from invoices where sale_id = ?", String.class, saleId);
    }

    private String saleNumber(UUID saleId) {
        return jdbcTemplate.queryForObject("select number from sales where id = ?", String.class, saleId);
    }

    private String businessDate(UUID saleId) {
        return jdbcTemplate.queryForObject(
                "select business_date::text from invoices where sale_id = ?", String.class, saleId);
    }

    /** Issues the credit note of the invoice of the sale and returns its number. */
    private String issueCreditNote(UUID saleId, String reason) throws Exception {
        var invoiceId = jdbcTemplate.queryForObject("select id from invoices where sale_id = ?", UUID.class, saleId);
        var json = mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/credit-note")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"%s\"}".formatted(reason)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(json, "$.number");
    }

    private UUID recordSale(UUID articleId, String quantity) throws Exception {
        var body = "{\"salesChannelId\":\"%s\",\"sellerId\":\"%s\",\"paymentMethod\":\"CASH\",\"lines\":[%s]}"
                .formatted(
                        channel("STORE"),
                        seller.getId(),
                        "{\"articleId\":\"%s\",\"quantity\":%s}".formatted(articleId, quantity));
        var json = mockMvc.perform(post(SALES)
                        .header(HttpHeaders.AUTHORIZATION, bearer(sellerToken))
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
