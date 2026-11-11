package org.vvs.valimbavaka.feature.commission.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
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
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
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
 * COM-05: the commissions are exported as xlsx and PDF. A seller only exports HER commissions (the server imposes her
 * identifier, whatever she sends); the administrator exports all of them or one seller's. Each test works on its own
 * sales channel and sellers (the database is shared); a sale of one article brings 425 Ar of commission.
 */
class CommissionExportIT extends StockApiTestSupport {

    private static final String COMMISSIONS = "/api/v1/commissions";
    private static final String ADMIN_EXPORT = COMMISSIONS + "/export";
    private static final String STATEMENT = COMMISSIONS + "/follow-up/lines";
    private static final String STATEMENT_EXPORT = STATEMENT + "/export";
    private static final String FROM = "2020-01-01";
    private static final String TO = "2099-12-31";
    private static final String SELLER_PASSWORD = "mot-de-passe-vendeuse-long";
    private static final ZoneId MADAGASCAR = ZoneId.of("Indian/Antananarivo");
    private static final List<String> HEADERS =
            List.of("Date", "Opération", "Vendeuse", "CA (Ar)", "Base (Ar)", "Taux (%)", "Commission (Ar)", "Statut");

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID channelId;
    private User seller;
    private User otherSeller;
    private String sellerToken;

    /** A sale needs today's day opened (JR-01); another test class may have removed it. */
    @BeforeEach
    void openTodayWithAChannelAndTwoSellers() throws Exception {
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
        seller = createSeller("Miora");
        otherSeller = createSeller("Hanta");
        sellerToken = login(seller.getEmail());
    }

    @Test
    void exportStatement_seller_returnsOnlyHerCommissionsAsXlsx() throws Exception {
        // Arrange : deux ventes pour Miora, une pour Hanta
        var first = createSale(seller);
        var second = createSale(seller);
        createSale(otherSeller);

        // Act
        var bytes = download(statementRequest(sellerToken, null));

        // Assert : en-têtes, deux lignes de Miora, aucune de Hanta, montants numériques
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheet("Relevé");
            assertThat(texts(sheet.getRow(0))).containsExactlyElementsOf(HEADERS);
            assertThat(sheet.getLastRowNum()).isEqualTo(2);
            var numbers = List.of(
                    texts(sheet.getRow(1)).get(1), texts(sheet.getRow(2)).get(1));
            assertThat(numbers).containsExactlyInAnyOrder(first, second);
            for (int index = 1; index <= 2; index++) {
                assertThat(texts(sheet.getRow(index)).get(2)).isEqualTo("Miora Rakoto");
                assertThat(sheet.getRow(index).getCell(6).getNumericCellValue()).isEqualTo(425.0);
                assertThat(texts(sheet.getRow(index)).get(7)).isEqualTo("CALCULATED");
            }
        }
    }

    @Test
    void exportStatement_sellerNamingAnotherSeller_stillGetsHerOwn() throws Exception {
        // Arrange
        createSale(seller);
        createSale(otherSeller);

        // Act : la vendeuse envoie l'identifiant de sa collègue
        var bytes = download(statementRequest(sellerToken, otherSeller.getId()));

        // Assert : le serveur impose son identifiant
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheet("Relevé");
            assertThat(sheet.getLastRowNum()).isEqualTo(1);
            assertThat(texts(sheet.getRow(1)).get(2)).isEqualTo("Miora Rakoto");
        }
    }

    @Test
    void exportStatement_sellerWithoutCommission_returnsHeaderRowOnly() throws Exception {
        // Arrange : Hanta n'a rien vendu ce mois-ci
        createSale(seller);
        var otherToken = login(otherSeller.getEmail());

        // Act
        var bytes = download(statementRequest(otherToken, null));

        // Assert
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheet("Relevé");
            assertThat(sheet.getLastRowNum()).isZero();
            assertThat(texts(sheet.getRow(0))).containsExactlyElementsOf(HEADERS);
        }
    }

    @Test
    void exportStatement_adminNamingASeller_returnsThatSellersCommissionsWithTheFileName() throws Exception {
        // Arrange
        createSale(seller);
        createSale(otherSeller);

        // Act
        var bytes = mockMvc.perform(statementRequest(adminToken(), otherSeller.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(ExcelHttpMessageConverter.XLSX))
                .andExpect(header().string(
                                HttpHeaders.CONTENT_DISPOSITION,
                                allOf(startsWith("attachment"), containsString("releve-commissions.xlsx"))))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        // Assert
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheet("Relevé");
            assertThat(sheet.getLastRowNum()).isEqualTo(1);
            assertThat(texts(sheet.getRow(1)).get(2)).isEqualTo("Hanta Rakoto");
        }
    }

    @Test
    void exportStatement_adminWithoutSeller_returns422() throws Exception {
        mockMvc.perform(statementRequest(adminToken(), null))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void exportStatement_pdf_returnsAPdfOfHerCommissions() throws Exception {
        // Arrange
        createSale(seller);

        // Act + Assert
        var pdfRequest = get(STATEMENT_EXPORT + "/pdf").header(HttpHeaders.AUTHORIZATION, bearer(sellerToken));
        var bytes = mockMvc.perform(pdfRequest)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        assertThat(new String(bytes, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void exportStatement_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(STATEMENT_EXPORT))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void exportStatement_deliveryDriver_returns403() throws Exception {
        var driverToken = tokenOf(RoleName.DELIVERY_DRIVER);

        mockMvc.perform(get(STATEMENT_EXPORT).header(HttpHeaders.AUTHORIZATION, bearer(driverToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void exportCommissions_adminForOneSeller_returnsHerRowsOnlyWithoutIdentifiers() throws Exception {
        // Arrange
        var saleNumber = createSale(seller);
        createSale(otherSeller);

        // Act
        var bytes = download(adminRequest(adminToken(), seller.getId()));

        // Assert : une ligne, les colonnes annotées seulement
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheet("Commissions");
            assertThat(texts(sheet.getRow(0))).containsExactlyElementsOf(HEADERS);
            assertThat(sheet.getLastRowNum()).isEqualTo(1);
            assertThat(texts(sheet.getRow(1)).get(1)).isEqualTo(saleNumber);
            assertThat(texts(sheet.getRow(1)).get(2)).isEqualTo("Miora Rakoto");
        }
    }

    @Test
    void exportCommissions_adminWithoutSeller_containsEverySellersCommissions() throws Exception {
        // Arrange
        createSale(seller);
        createSale(otherSeller);

        // Act
        var bytes = download(adminRequest(adminToken(), null));

        // Assert : les deux vendeuses du test figurent dans le fichier (la base est partagée)
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheet("Commissions");
            var names = new ArrayList<String>();
            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                names.add(texts(sheet.getRow(index)).get(2));
            }
            assertThat(names).contains("Miora Rakoto", "Hanta Rakoto");
        }
    }

    @Test
    void exportCommissions_sellerOrWithoutToken_returns403Or401() throws Exception {
        mockMvc.perform(get(ADMIN_EXPORT).header(HttpHeaders.AUTHORIZATION, bearer(sellerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(get(ADMIN_EXPORT + "/pdf").header(HttpHeaders.AUTHORIZATION, bearer(sellerToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(ADMIN_EXPORT))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void exportCommissions_periodInverted_returns422() throws Exception {
        mockMvc.perform(get(ADMIN_EXPORT)
                        .param("from", "2026-11-30")
                        .param("to", "2026-11-01")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private static MockHttpServletRequestBuilder statementRequest(String token, UUID sellerId) {
        var request = get(STATEMENT_EXPORT).param("month", YearMonth.now(MADAGASCAR).toString());
        request.header(HttpHeaders.AUTHORIZATION, bearer(token));
        if (sellerId != null) {
            request.param("sellerId", sellerId.toString());
        }
        return request;
    }

    private static MockHttpServletRequestBuilder adminRequest(String token, UUID sellerId) {
        var request = get(ADMIN_EXPORT).param("from", FROM).param("to", TO);
        request.header(HttpHeaders.AUTHORIZATION, bearer(token));
        if (sellerId != null) {
            request.param("sellerId", sellerId.toString());
        }
        return request;
    }

    private byte[] download(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
    }

    private static List<String> texts(Row row) {
        var formatter = new DataFormatter(Locale.US);
        var values = new ArrayList<String>();
        for (int i = 0; i < row.getLastCellNum(); i++) {
            values.add(formatter.formatCellValue(row.getCell(i)));
        }
        return values;
    }

    /** One-article sale on the test channel, entered by the administrator for the seller; returns its number. */
    private String createSale(User creditedSeller) throws Exception {
        var articleId = createArticle(uniqueName());
        move(articleId, "STORE", StockMovementType.RECEIPT, "10");
        var line = "{\"articleId\":\"%s\",\"quantity\":1}".formatted(articleId);
        var body = "{\"salesChannelId\":\"%s\",\"sellerId\":\"%s\",\"paymentMethod\":\"CASH\",\"lines\":[%s]}"
                .formatted(channelId, creditedSeller.getId(), line);
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

    private User createSeller(String firstName) {
        var email = "vendeuse-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(RoleName.SELLER).orElseThrow();
        var profile = new UserProfile(email, firstName, "Rakoto");
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
