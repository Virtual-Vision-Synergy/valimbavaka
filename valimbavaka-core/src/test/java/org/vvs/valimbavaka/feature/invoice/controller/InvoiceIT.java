package org.vvs.valimbavaka.feature.invoice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.invoice.repository.InvoiceRepository;
import org.vvs.valimbavaka.feature.stock.controller.StockApiTestSupport;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.domain.SellerTerms;
import org.vvs.valimbavaka.feature.user.domain.User;
import org.vvs.valimbavaka.feature.user.domain.UserProfile;
import org.vvs.valimbavaka.feature.user.repository.RoleRepository;
import org.vvs.valimbavaka.feature.user.repository.UserRepository;

/**
 * FAC-01: every recorded sale produces its numbered invoice; FAC-02: the invoice of a sale can be regenerated; FAC-03:
 * the invoice can be downloaded as a PDF.
 */
class InvoiceIT extends StockApiTestSupport {

    private static final String INVOICES = "/api/v1/invoices";
    private static final String SALES = "/api/v1/sales";
    private static final String SELLER_PASSWORD = "mot-de-passe-vendeuse-long";
    private static final String NUMBER_FORMAT = "FAC-\\d{4}-\\d{4,}";
    private static final int TEST_YEAR = 2099;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private User seller;
    private String sellerToken;

    @BeforeEach
    void openTodayAndCreateSeller() throws Exception {
        var status = openDay().andReturn().getResponse().getStatus();
        assertThat(status).isIn(201, 409);
        seller = createSeller("Miora");
        sellerToken = login(seller.getEmail());
    }

    @Test
    void createSale_storeSale_issuesAnInvoiceWithTheFrozenContentOfTheSale() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "10");

        // Act
        var saleId = recordSale(sellerToken, "MVOLA", soap, "3");

        // Assert
        getInvoiceOfSale(sellerToken, saleId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(matchesPattern(NUMBER_FORMAT)))
                .andExpect(jsonPath("$.saleId").value(saleId.toString()))
                .andExpect(jsonPath("$.saleNumber").value(matchesPattern("VTE-\\d{4,}")))
                .andExpect(jsonPath("$.status").value("VALIDATED"))
                .andExpect(jsonPath("$.salesChannelLabel").value("Boutique"))
                .andExpect(jsonPath("$.sellerId").value(seller.getId().toString()))
                .andExpect(jsonPath("$.sellerName").value("Miora Rakoto"))
                .andExpect(jsonPath("$.paymentMethod").value("MVOLA"))
                .andExpect(jsonPath("$.issuerName").value("By Valimbavaka Cosmetics"))
                .andExpect(jsonPath("$.issuedAt").isNotEmpty())
                .andExpect(jsonPath("$.subtotal").value(25500.0))
                .andExpect(jsonPath("$.discountAmount").value(0.0))
                .andExpect(jsonPath("$.total").value(25500.0))
                .andExpect(jsonPath("$.lines.length()").value(1))
                .andExpect(jsonPath("$.lines[0].articleId").value(soap.toString()))
                .andExpect(jsonPath("$.lines[0].unitPrice").value(8500.0))
                .andExpect(jsonPath("$.lines[0].lineTotal").value(25500.0));
        var number = invoiceNumber(saleId);
        var businessYear = jdbcTemplate.queryForObject(
                "select extract(year from business_date)::int from sales where id = ?", Integer.class, saleId);
        assertThat(number).startsWith("FAC-" + businessYear + "-");
    }

    @Test
    void createSale_twoSalesInARow_getConsecutiveIncreasingNumbers() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "10");

        // Act
        var first = recordSale(sellerToken, "CASH", soap, "1");
        var second = recordSale(sellerToken, "CARD", soap, "1");

        // Assert
        assertThat(sequenceOf(invoiceNumber(second))).isEqualTo(sequenceOf(invoiceNumber(first)) + 1);
    }

    @Test
    void createSale_validSale_tracesTheInvoiceInTheJournal() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "10");

        // Act
        var saleId = recordSale(sellerToken, "CARD", soap, "2");

        // Assert
        var invoiceId = jdbcTemplate.queryForObject("select id from invoices where sale_id = ?", UUID.class, saleId);
        var description = jdbcTemplate.queryForObject(
                "select description from journal_entries where resource_type = 'INVOICE' and resource_id = ? "
                        + "and action = 'CREATE'",
                String.class,
                invoiceId);
        assertThat(description).startsWith("Facture FAC-").endsWith(" · 17000.00 Ar · Carte");
    }

    @Test
    void createSale_eachPaymentMethodOfTheSale_isReportedOnTheInvoice() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "10");

        // Act
        var saleId = recordSale(sellerToken, "ORANGE_MONEY", soap, "1");

        // Assert
        getInvoiceOfSale(sellerToken, saleId)
                .andExpect(jsonPath("$.paymentMethod").value("ORANGE_MONEY"));
    }

    @Test
    void getInvoiceById_afterAnArticleRenameAndAPriceChange_keepsTheIssuedContent() throws Exception {
        // Arrange
        var name = uniqueName();
        var soap = createArticle(name);
        move(soap, "STORE", StockMovementType.RECEIPT, "10");
        var saleId = recordSale(sellerToken, "CASH", soap, "1");
        var invoiceId = jdbcTemplate.queryForObject("select id from invoices where sale_id = ?", UUID.class, saleId);
        jdbcTemplate.update("update articles set name = 'Renommé', price = 9900 where id = ?", soap);

        // Act + Assert
        getInvoice(sellerToken, invoiceId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].label").value(name))
                .andExpect(jsonPath("$.lines[0].unitPrice").value(8500.0))
                .andExpect(jsonPath("$.total").value(8500.0));
    }

    @Test
    void createSale_notEnoughStock_returns409AndIssuesNoInvoiceNorNumber() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "1");
        var invoicesBefore = countInvoices();
        var counterBefore = currentCounter();

        // Act
        createSale(sellerToken, "CASH", soap, "2").andExpect(status().isConflict());

        // Assert
        assertThat(countInvoices()).isEqualTo(invoicesBefore);
        assertThat(currentCounter()).isEqualTo(counterBefore);
    }

    @Test
    void nextNumber_transactionRolledBack_givesTheNumberBack() {
        // Arrange
        jdbcTemplate.update("delete from invoice_counters where year = ?", TEST_YEAR);
        var transaction = new TransactionTemplate(transactionManager);

        // Act
        var rolledBack = transaction.execute(status -> {
            var number = invoiceRepository.nextNumber(TEST_YEAR);
            status.setRollbackOnly();
            return number;
        });
        var committed = transaction.execute(status -> invoiceRepository.nextNumber(TEST_YEAR));
        var next = transaction.execute(status -> invoiceRepository.nextNumber(TEST_YEAR));

        // Assert
        assertThat(rolledBack).isEqualTo(1L);
        assertThat(committed).isEqualTo(1L);
        assertThat(next).isEqualTo(2L);
        jdbcTemplate.update("delete from invoice_counters where year = ?", TEST_YEAR);
    }

    @Test
    void createSale_sixSalesAtTheSameTime_getSixDistinctConsecutiveNumbers() throws Exception {
        // Arrange
        var articles = new ArrayList<UUID>();
        for (var i = 0; i < 6; i++) {
            var article = createArticle(uniqueName());
            move(article, "STORE", StockMovementType.RECEIPT, "5");
            articles.add(article);
        }
        var executor = Executors.newFixedThreadPool(6);
        List<Callable<UUID>> tasks = articles.stream()
                .<Callable<UUID>>map(article -> () -> recordSale(sellerToken, "CASH", article, "1"))
                .toList();

        // Act
        var saleIds = new ArrayList<UUID>();
        try {
            for (var future : executor.invokeAll(tasks)) {
                saleIds.add(future.get());
            }
        } finally {
            executor.shutdown();
        }

        // Assert
        var sequences = saleIds.stream()
                .map(id -> sequenceOf(invoiceNumber(id)))
                .sorted()
                .toList();
        assertThat(sequences).doesNotHaveDuplicates();
        assertThat(sequences.getLast() - sequences.getFirst()).isEqualTo(5L);
    }

    @Test
    void getInvoiceBySale_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(INVOICES + "/sales/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getInvoiceById_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(INVOICES + "/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getInvoiceBySale_deliveryDriver_returns403() throws Exception {
        getInvoiceOfSale(tokenOf(RoleName.DELIVERY_DRIVER), UUID.randomUUID())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getInvoiceById_deliveryDriver_returns403() throws Exception {
        getInvoice(tokenOf(RoleName.DELIVERY_DRIVER), UUID.randomUUID())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getInvoiceBySale_unknownSale_returns404() throws Exception {
        getInvoiceOfSale(adminToken(), UUID.randomUUID())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void getInvoiceById_unknownInvoice_returns404() throws Exception {
        getInvoice(adminToken(), UUID.randomUUID())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void getInvoiceBySale_malformedId_returns400() throws Exception {
        mockMvc.perform(get(INVOICES + "/sales/pas-un-uuid").header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void getInvoiceBySale_sellerAskingForAColleagueInvoice_returns404ButAdministratorReadsIt() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "10");
        var saleId = recordSale(sellerToken, "CASH", soap, "1");
        var colleagueToken = login(createSeller("Lova").getEmail());

        // Act + Assert
        getInvoiceOfSale(colleagueToken, saleId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        getInvoiceOfSale(adminToken(), saleId).andExpect(status().isOk());
    }

    @Test
    void createSale_deliveryChannelEnteredByTheAdministrator_isInvoicedWithTheDeliveryChannel() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "DELIVERY", StockMovementType.RECEIPT, "4");
        var deliverySale = "{\"salesChannelId\":\"%s\",\"sellerId\":\"%s\",\"paymentMethod\":\"ON_DELIVERY\","
                        .formatted(channel("DELIVERY"), seller.getId())
                + "\"lines\":[{\"articleId\":\"%s\",\"quantity\":1}]}".formatted(soap);

        // Act
        var saleId = createdId(submitSale(adminToken(), deliverySale));

        // Assert
        getInvoiceOfSale(adminToken(), saleId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salesChannelLabel").value("Livraison"))
                .andExpect(jsonPath("$.paymentMethod").value("ON_DELIVERY"));
    }

    @Test
    void regenerateInvoice_existingInvoice_returnsTheSameNumberAndContentWithoutUsingANumber() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "10");
        var saleId = recordSale(sellerToken, "MVOLA", soap, "3");
        var before =
                getInvoiceOfSale(sellerToken, saleId).andReturn().getResponse().getContentAsString();
        var counterBefore = currentCounter();
        var invoicesBefore = countInvoices();

        // Act
        var after = regenerate(sellerToken, saleId).andExpect(status().isOk());

        // Assert
        var json = after.andReturn().getResponse().getContentAsString();
        assertThat(json).isEqualTo(before);
        assertThat(currentCounter()).isEqualTo(counterBefore);
        assertThat(countInvoices()).isEqualTo(invoicesBefore);
    }

    @Test
    void regenerateInvoice_existingInvoice_repeatsTheLinesAmountsAndPaymentMethodOfTheSale() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "10");
        var saleId = recordSale(sellerToken, "ORANGE_MONEY", soap, "2");
        var sale = mockMvc.perform(get(SALES + "/" + saleId).header(HttpHeaders.AUTHORIZATION, bearer(sellerToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Act
        var invoice = regenerate(sellerToken, saleId)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert
        assertThat(text(invoice, "$.paymentMethod")).isEqualTo(text(sale, "$.paymentMethod"));
        assertThat(amount(invoice, "$.subtotal")).isEqualTo(amount(sale, "$.subtotal"));
        assertThat(amount(invoice, "$.discountAmount")).isEqualTo(amount(sale, "$.discountAmount"));
        assertThat(amount(invoice, "$.total")).isEqualTo(amount(sale, "$.total"));
        assertThat(text(invoice, "$.lines[0].articleId")).isEqualTo(text(sale, "$.lines[0].articleId"));
        assertThat(amount(invoice, "$.lines[0].quantity")).isEqualTo(amount(sale, "$.lines[0].quantity"));
        assertThat(amount(invoice, "$.lines[0].lineTotal")).isEqualTo(amount(sale, "$.lines[0].lineTotal"));
    }

    @Test
    void regenerateInvoice_afterAnArticleRenameAndAPriceChange_keepsTheIssuedContent() throws Exception {
        // Arrange
        var name = uniqueName();
        var soap = createArticle(name);
        move(soap, "STORE", StockMovementType.RECEIPT, "10");
        var saleId = recordSale(sellerToken, "CASH", soap, "1");
        jdbcTemplate.update("update articles set name = 'Renommé', price = 9900 where id = ?", soap);

        // Act + Assert
        regenerate(sellerToken, saleId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].label").value(name))
                .andExpect(jsonPath("$.lines[0].unitPrice").value(8500.0))
                .andExpect(jsonPath("$.total").value(8500.0));
    }

    @Test
    void regenerateInvoice_existingInvoice_tracesTheReeditionInTheJournal() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "10");
        var saleId = recordSale(sellerToken, "CASH", soap, "1");
        var invoiceId = jdbcTemplate.queryForObject("select id from invoices where sale_id = ?", UUID.class, saleId);

        // Act
        regenerate(sellerToken, saleId).andExpect(status().isOk());
        regenerate(adminToken(), saleId).andExpect(status().isOk());

        // Assert
        var descriptions = jdbcTemplate.queryForList(
                "select description from journal_entries where resource_type = 'INVOICE' and resource_id = ? "
                        + "and action = 'UPDATE'",
                String.class,
                invoiceId);
        assertThat(descriptions).hasSize(2);
        assertThat(descriptions).allMatch(d -> d.startsWith("Facture FAC-") && d.contains("régénérée · vente VTE-"));
    }

    @Test
    void regenerateInvoice_saleWithoutInvoice_issuesItWithTheNextNumberAndTracesIt() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "10");
        var saleId = recordSale(sellerToken, "CARD", soap, "2");
        deleteInvoiceOf(saleId);
        var counterBefore = currentCounter();

        // Act
        regenerate(sellerToken, saleId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(matchesPattern(NUMBER_FORMAT)))
                .andExpect(jsonPath("$.saleId").value(saleId.toString()))
                .andExpect(jsonPath("$.paymentMethod").value("CARD"))
                .andExpect(jsonPath("$.total").value(17000.0));

        // Assert
        assertThat(sequenceOf(invoiceNumber(saleId))).isEqualTo(counterBefore + 1);
        var created = jdbcTemplate.queryForObject(
                "select count(*) from journal_entries j join invoices i on i.id = j.resource_id "
                        + "where i.sale_id = ? and j.action = 'CREATE'",
                Integer.class,
                saleId);
        assertThat(created).isEqualTo(1);
    }

    @Test
    void regenerateInvoice_twiceOnASaleWithoutInvoice_issuesOneInvoiceOnly() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "10");
        var saleId = recordSale(sellerToken, "CASH", soap, "1");
        deleteInvoiceOf(saleId);

        // Act
        var first = regenerate(sellerToken, saleId).andReturn().getResponse().getContentAsString();
        var second = regenerate(sellerToken, saleId).andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(text(second, "$.id")).isEqualTo(text(first, "$.id"));
        assertThat(text(second, "$.number")).isEqualTo(text(first, "$.number"));
        assertThat(countInvoicesOf(saleId)).isEqualTo(1);
    }

    @Test
    void regenerateInvoice_withoutToken_returns401() throws Exception {
        mockMvc.perform(post(INVOICES + "/sales/" + UUID.randomUUID() + "/regenerate"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void regenerateInvoice_deliveryDriver_returns403() throws Exception {
        regenerate(tokenOf(RoleName.DELIVERY_DRIVER), UUID.randomUUID())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void regenerateInvoice_unknownSale_returns404AndCreatesNoInvoice() throws Exception {
        // Arrange
        var invoicesBefore = countInvoices();
        var counterBefore = currentCounter();

        // Act
        regenerate(adminToken(), UUID.randomUUID())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Assert
        assertThat(countInvoices()).isEqualTo(invoicesBefore);
        assertThat(currentCounter()).isEqualTo(counterBefore);
    }

    @Test
    void regenerateInvoice_malformedSaleId_returns400() throws Exception {
        mockMvc.perform(post(INVOICES + "/sales/pas-un-uuid/regenerate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void regenerateInvoice_bodyWithFreeLinesAndNumber_isIgnored() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "10");
        var saleId = recordSale(sellerToken, "CASH", soap, "1");
        var number = invoiceNumber(saleId);

        // Act
        mockMvc.perform(post(INVOICES + "/sales/" + saleId + "/regenerate")
                        .header(HttpHeaders.AUTHORIZATION, bearer(sellerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"FAC-1999-0001\",\"total\":1,\"lines\":[{\"label\":\"Libre\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(number))
                .andExpect(jsonPath("$.total").value(8500.0))
                .andExpect(jsonPath("$.lines.length()").value(1));
    }

    @Test
    void regenerateInvoice_sellerAskingForAColleagueSale_returns404ButAdministratorRegenerates() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "10");
        var saleId = recordSale(sellerToken, "CASH", soap, "1");
        var colleagueToken = login(createSeller("Lova").getEmail());

        // Act + Assert
        regenerate(colleagueToken, saleId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        regenerate(adminToken(), saleId).andExpect(status().isOk());
    }

    @Test
    void regenerateInvoice_sellerAskingForAColleagueSaleWithoutInvoice_returns404AndIssuesNothing() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "10");
        var saleId = recordSale(sellerToken, "CASH", soap, "1");
        deleteInvoiceOf(saleId);
        var colleagueToken = login(createSeller("Lova").getEmail());

        // Act
        regenerate(colleagueToken, saleId).andExpect(status().isNotFound());

        // Assert
        assertThat(countInvoicesOf(saleId)).isZero();
    }

    @Test
    void getInvoicePdf_issuedInvoice_returnsAPdfNamedAfterTheNumberWithTheFrozenContent() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "10");
        var saleId = recordSale(sellerToken, "MVOLA", soap, "3");
        var number = invoiceNumber(saleId);

        // Act
        var response = getPdf(sellerToken, invoiceId(saleId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn()
                .getResponse();
        var bytes = response.getContentAsByteArray();
        var disposition = response.getHeader(HttpHeaders.CONTENT_DISPOSITION);

        // Assert
        assertThat(disposition).startsWith("attachment").contains(number + ".pdf");
        assertThat(textOf(bytes))
                .contains("Facture " + number)
                .contains("By Valimbavaka Cosmetics")
                .contains("règlement : MVola")
                .contains("Vendeuse : Miora Rakoto")
                .contains("Total à payer : 25 500 Ar");
    }

    @Test
    void getInvoicePdf_downloadedTwice_changesNothingAndUsesNoNumber() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "10");
        var saleId = recordSale(sellerToken, "CASH", soap, "1");
        var invoiceId = invoiceId(saleId);
        var counterBefore = currentCounter();
        var invoicesBefore = countInvoices();

        // Act
        getPdf(sellerToken, invoiceId).andExpect(status().isOk());
        getPdf(adminToken(), invoiceId).andExpect(status().isOk());

        // Assert
        assertThat(currentCounter()).isEqualTo(counterBefore);
        assertThat(countInvoices()).isEqualTo(invoicesBefore);
        assertThat(invoiceNumber(saleId)).matches(NUMBER_FORMAT);
    }

    @Test
    void getInvoicePdf_afterAnArticleRenameAndAPriceChange_keepsTheIssuedContent() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "10");
        var saleId = recordSale(sellerToken, "CASH", soap, "2");
        var invoiceId = invoiceId(saleId);
        var issuedLabel = jdbcTemplate.queryForObject(
                "select label from invoice_lines where invoice_id = ?", String.class, invoiceId);
        jdbcTemplate.update("update articles set name = 'Renommé', price = 9900 where id = ?", soap);

        // Act
        var bytes = getPdf(sellerToken, invoiceId).andReturn().getResponse().getContentAsByteArray();

        // Assert
        assertThat(textOf(bytes)).contains(issuedLabel).contains("17 000 Ar").doesNotContain("Renommé");
    }

    @Test
    void getInvoicePdf_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(INVOICES + "/" + UUID.randomUUID() + "/pdf"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getInvoicePdf_deliveryDriver_returns403() throws Exception {
        getPdf(tokenOf(RoleName.DELIVERY_DRIVER), UUID.randomUUID())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getInvoicePdf_unknownInvoice_returns404() throws Exception {
        getPdf(adminToken(), UUID.randomUUID())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void getInvoicePdf_malformedId_returns400() throws Exception {
        mockMvc.perform(get(INVOICES + "/pas-un-uuid/pdf").header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void getInvoicePdf_sellerAskingForAColleagueInvoice_returns404ButAdministratorReadsIt() throws Exception {
        // Arrange
        var soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "10");
        var saleId = recordSale(sellerToken, "CASH", soap, "1");
        var invoiceId = invoiceId(saleId);
        var colleagueToken = login(createSeller("Lova").getEmail());

        // Act + Assert
        getPdf(colleagueToken, invoiceId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        getPdf(adminToken(), invoiceId).andExpect(status().isOk());
    }

    private void deleteInvoiceOf(UUID saleId) {
        jdbcTemplate.update(
                "delete from invoice_lines where invoice_id in (select id from invoices where sale_id = ?)", saleId);
        jdbcTemplate.update("delete from invoices where sale_id = ?", saleId);
    }

    private Integer countInvoicesOf(UUID saleId) {
        return jdbcTemplate.queryForObject("select count(*) from invoices where sale_id = ?", Integer.class, saleId);
    }

    private static String text(String json, String path) {
        return JsonPath.read(json, path);
    }

    private static double amount(String json, String path) {
        return JsonPath.<Number>read(json, path).doubleValue();
    }

    private ResultActions getPdf(String token, UUID invoiceId) throws Exception {
        return mockMvc.perform(
                get(INVOICES + "/" + invoiceId + "/pdf").header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private UUID invoiceId(UUID saleId) {
        return jdbcTemplate.queryForObject("select id from invoices where sale_id = ?", UUID.class, saleId);
    }

    private static String textOf(byte[] pdf) throws Exception {
        try (var document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private ResultActions regenerate(String token, UUID saleId) throws Exception {
        return mockMvc.perform(
                post(INVOICES + "/sales/" + saleId + "/regenerate").header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private UUID recordSale(String token, String payment, UUID articleId, String quantity) throws Exception {
        return createdId(createSale(token, payment, articleId, quantity));
    }

    private ResultActions createSale(String token, String payment, UUID articleId, String quantity) throws Exception {
        var body = "{\"salesChannelId\":\"%s\",\"sellerId\":\"%s\",\"paymentMethod\":\"%s\",\"lines\":[%s]}"
                .formatted(
                        channel("STORE"),
                        seller.getId(),
                        payment,
                        "{\"articleId\":\"%s\",\"quantity\":%s}".formatted(articleId, quantity));
        return submitSale(token, body);
    }

    private ResultActions submitSale(String token, String body) throws Exception {
        return mockMvc.perform(post(SALES)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions getInvoiceOfSale(String token, UUID saleId) throws Exception {
        return mockMvc.perform(get(INVOICES + "/sales/" + saleId).header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private ResultActions getInvoice(String token, UUID invoiceId) throws Exception {
        return mockMvc.perform(get(INVOICES + "/" + invoiceId).header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private ResultActions openDay() throws Exception {
        return mockMvc.perform(post("/api/v1/days")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lines\":[]}"));
    }

    private static UUID createdId(ResultActions actions) throws Exception {
        var json = actions.andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private String invoiceNumber(UUID saleId) {
        return jdbcTemplate.queryForObject("select number from invoices where sale_id = ?", String.class, saleId);
    }

    /** {@code FAC-2026-0042} gives 42. */
    private static long sequenceOf(String number) {
        return Long.parseLong(number.substring(number.lastIndexOf('-') + 1));
    }

    private Integer countInvoices() {
        return jdbcTemplate.queryForObject("select count(*) from invoices", Integer.class);
    }

    private Long currentCounter() {
        return jdbcTemplate.queryForObject(
                "select coalesce(max(last_value), 0) from invoice_counters where year = extract(year from now())::int",
                Long.class);
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
