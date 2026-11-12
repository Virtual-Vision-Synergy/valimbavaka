package org.vvs.valimbavaka.feature.report.controller;

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
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
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
 * RPT-06: the report is exported as xlsx (one sheet per block) and as PDF, with the figures of {@code GET
 * /api/v1/reports}, administrator only. The database is shared with the other test classes: the sales of these tests
 * belong to a product range created here (and deactivated afterwards), and the range filter isolates them.
 */
class ReportExportIT extends StockApiTestSupport {

    private static final String REPORTS = "/api/v1/reports";
    private static final String XLSX = REPORTS + "/export";
    private static final String PDF = REPORTS + "/export/pdf";
    private static final String MONTH = "?granularity=MONTH";
    private static final DataFormatter FORMATTER = new DataFormatter();

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User seller;
    private UUID rangeId;
    private String rangeFilter;

    /** Opens today's day (JR-01), creates a seller and an empty product range. */
    @BeforeEach
    void openTodayCreateSellerAndRange() throws Exception {
        endAutomaticPromotions();
        var status = mockMvc.perform(post("/api/v1/days")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[]}"))
                .andReturn()
                .getResponse()
                .getStatus();
        assertThat(status).isIn(201, 409);
        var role = roleRepository.findByName(RoleName.SELLER).orElseThrow();
        var terms = new SellerTerms(channel("STORE"), new BigDecimal("5.00"));
        var email = "vendeuse-" + UUID.randomUUID() + "@valimbavaka.mg";
        seller = userRepository.save(User.create(
                new UserProfile(email, "Miora", "Rakoto"), passwordEncoder.encode("mot-de-passe-long-1"), role, terms));
        rangeId = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into product_ranges (id, code, label) values (?, ?, 'Gamme RPT-06')",
                rangeId,
                "RPT06-" + rangeId.toString().substring(0, 8));
        rangeFilter = MONTH + "&rangeId=" + rangeId;
    }

    @AfterEach
    void deactivateRange() {
        jdbcTemplate.update("update product_ranges set is_active = false where id = ?", rangeId);
    }

    @Test
    void exportXlsx_report_hasOneSheetPerBlockWithTheFiguresOfTheScreen() throws Exception {
        // Arrange: 3 x 8500 = 25500 and 1 x 8500, purchase cost 3120.50 each
        var soap = article("A-savon");
        var oil = article("B-huile");
        createSale("STORE", List.of(soap, oil), List.of("3", "1"));
        var json = mockMvc.perform(authorized(REPORTS + rangeFilter)).andReturn().getResponse().getContentAsString();

        // Act
        var bytes = download(XLSX + rangeFilter, ExcelHttpMessageConverter.XLSX);

        // Assert
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var names = List.of("Rapport", "Synthèse", "Évolution", "Gammes", "Classement");
            assertThat(workbook.sheetIterator()).toIterable().extracting(Sheet::getSheetName).isEqualTo(names);
            var summary = workbook.getSheet("Synthèse");
            var headers = text(summary.getRow(0));
            assertThat(headers).containsExactly("Indicateur", "Période", "Période précédente", "Variation (%)");
            assertThat(summary.getRow(1).getCell(1).getNumericCellValue()).isEqualTo(34000.0);
            assertThat(summary.getRow(1).getCell(1).getNumericCellValue())
                    .isEqualTo(((Number) JsonPath.read(json, "$.sales.revenue")).doubleValue());
            assertThat(summary.getRow(2).getCell(1).getNumericCellValue()).isEqualTo(1.0);
            assertThat(summary.getRow(3).getCell(1).getNumericCellValue()).isEqualTo(34000.0);
            var ranking = workbook.getSheet("Classement");
            assertThat(ranking.getLastRowNum()).isEqualTo(2);
            assertThat(ranking.getRow(1).getCell(1).getStringCellValue()).startsWith("A-savon");
            assertThat(ranking.getRow(1).getCell(2).getNumericCellValue()).isEqualTo(3.0);
            assertThat(ranking.getRow(1).getCell(3).getNumericCellValue()).isEqualTo(25500.0);
            assertThat(ranking.getRow(1).getCell(4).getNumericCellValue()).isEqualTo(16138.5);
            assertThat(ranking.getRow(1).getCell(5).getNumericCellValue()).isEqualTo(75.0);
            // the split by range ignores the range filter (RPT-04): the range of the test is one row among others
            var range = rowWith(workbook.getSheet("Gammes"), 1, "Gamme RPT-06");
            assertThat(range.getCell(2).getNumericCellValue()).isEqualTo(34000.0);
            assertThat(range.getCell(3).getNumericCellValue()).isPositive();
            var trend = workbook.getSheet("Évolution");
            int days = JsonPath.<List<Object>>read(json, "$.trend").size();
            assertThat(trend.getLastRowNum()).isEqualTo(days);
        }
    }

    @Test
    void exportXlsx_information_namesTheKindPeriodChannelAndRange() throws Exception {
        // Arrange
        var channelId = channel("FACEBOOK");

        // Act
        var bytes = download(XLSX + rangeFilter + "&salesChannelId=" + channelId, ExcelHttpMessageConverter.XLSX);

        // Assert
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var info = workbook.getSheet("Rapport");
            assertThat(text(info.getRow(0))).containsExactly("Information", "Valeur");
            assertThat(text(info.getRow(1))).containsExactly("Rapport", "Rapport mensuel");
            assertThat(text(info.getRow(2)).get(1)).startsWith("du 01/");
            assertThat(text(info.getRow(3)).get(0)).isEqualTo("Période précédente");
            var label = jdbcTemplate.queryForObject(
                    "select label from sales_channels where id = ?", String.class, channelId);
            assertThat(text(info.getRow(4))).containsExactly("Canal de vente", label);
            assertThat(text(info.getRow(5))).containsExactly("Gamme", "Gamme RPT-06");
        }
    }

    @Test
    void exportXlsx_channelFilter_keepsOnlyThatChannel() throws Exception {
        // Arrange
        var soap = article("A-savon");
        var oil = article("B-huile");
        createSale("STORE", soap, "1");
        createSale("FACEBOOK", oil, "2");

        // Act
        var bytes = download(
                XLSX + rangeFilter + "&salesChannelId=" + channel("FACEBOOK"), ExcelHttpMessageConverter.XLSX);

        // Assert
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var ranking = workbook.getSheet("Classement");
            assertThat(ranking.getLastRowNum()).isEqualTo(1);
            assertThat(ranking.getRow(1).getCell(1).getStringCellValue()).startsWith("B-huile");
            assertThat(workbook.getSheet("Synthèse").getRow(1).getCell(1).getNumericCellValue()).isEqualTo(17000.0);
        }
    }

    @Test
    void exportXlsx_pastPeriodWithoutSale_hasHeaderRowsOnlyAndZeroIndicators() throws Exception {
        // Act
        var bytes = download(XLSX + "?granularity=YEAR&date=2000-06-15", ExcelHttpMessageConverter.XLSX);

        // Assert
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getSheet("Classement").getLastRowNum()).isZero();
            assertThat(workbook.getSheet("Gammes").getLastRowNum()).isZero();
            assertThat(workbook.getSheet("Évolution").getLastRowNum()).isEqualTo(12);
            var summary = workbook.getSheet("Synthèse");
            assertThat(summary.getRow(1).getCell(1).getNumericCellValue()).isZero();
            assertThat(FORMATTER.formatCellValue(summary.getRow(1).getCell(3))).isEmpty();
        }
    }

    @Test
    void exportPdf_report_printsTheFiltersIndicatorsAndRows() throws Exception {
        // Arrange
        var soap = article("A-savon");
        createSale("STORE", soap, "3");

        // Act
        var bytes = download(PDF + rangeFilter, MediaType.APPLICATION_PDF);

        // Assert
        assertThat(new String(bytes, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        assertThat(pdfText(bytes))
                .contains("Rapport mensuel", "Gamme : Gamme RPT-06", "Canal de vente : Tous les canaux")
                .contains("Chiffre d'affaires : 25 500,00 Ar", "Nombre de ventes : 1", "Panier moyen : 25 500,00 Ar")
                .contains("Évolution", "Gamme", "Classement", "A-savon")
                .contains("16 138,50", "100,0");
    }

    @Test
    void exportPdf_nothingSold_stillProducesTheDocument() throws Exception {
        // Act
        var bytes = download(PDF + "?granularity=DAY&date=2000-06-15", MediaType.APPLICATION_PDF);

        // Assert
        assertThat(pdfText(bytes))
                .contains("Rapport journalier", "Période : 15/06/2000", "Chiffre d'affaires : 0,00 Ar")
                .contains("Nombre de ventes : 0 (précédent : 0, n/d)");
    }

    @Test
    void export_fileNames_followTheKindAndThePeriod() throws Exception {
        mockMvc.perform(authorized(XLSX + "?granularity=DAY&date=2026-11-12"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                                HttpHeaders.CONTENT_DISPOSITION,
                                allOf(startsWith("attachment"), containsString("rapport-journalier-2026-11-12.xlsx"))));
        mockMvc.perform(authorized(PDF + "?granularity=MONTH&date=2026-03-10"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                                HttpHeaders.CONTENT_DISPOSITION,
                                allOf(startsWith("attachment"), containsString("rapport-mensuel-2026-03.pdf"))));
        mockMvc.perform(authorized(PDF + "?granularity=YEAR&date=2025-07-01"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                                HttpHeaders.CONTENT_DISPOSITION, containsString("rapport-annuel-2025.pdf")));
    }

    @Test
    void export_withoutParameters_isTheCurrentMonth() throws Exception {
        mockMvc.perform(authorized(PDF))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("rapport-mensuel-")));
    }

    @Test
    void export_withoutToken_returns401() throws Exception {
        for (var route : List.of(XLSX, PDF)) {
            mockMvc.perform(get(route))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        }
    }

    @Test
    void export_sellerAndDeliveryDriver_return403() throws Exception {
        for (var route : List.of(XLSX, PDF)) {
            for (var role : List.of(RoleName.SELLER, RoleName.DELIVERY_DRIVER)) {
                mockMvc.perform(get(route).header(HttpHeaders.AUTHORIZATION, bearer(tokenOf(role))))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
            }
        }
    }

    @Test
    void export_unknownGranularity_returns400() throws Exception {
        for (var route : List.of(XLSX, PDF)) {
            mockMvc.perform(authorized(route + "?granularity=WEEK"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        }
    }

    @Test
    void export_periodThatHasNotBegun_returns422WithoutFile() throws Exception {
        for (var route : List.of(XLSX, PDF)) {
            mockMvc.perform(authorized(route + "?granularity=YEAR&date=2999-01-01"))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
    }

    private MockHttpServletRequestBuilder authorized(String url) throws Exception {
        return get(url).header(HttpHeaders.AUTHORIZATION, bearer(adminToken()));
    }

    private byte[] download(String url, MediaType type) throws Exception {
        ResultActions result = mockMvc.perform(authorized(url))
                .andExpect(status().isOk())
                .andExpect(content().contentType(type));
        return result.andReturn().getResponse().getContentAsByteArray();
    }

    private static Row rowWith(Sheet sheet, int column, String value) {
        for (Row row : sheet) {
            if (value.equals(FORMATTER.formatCellValue(row.getCell(column)))) {
                return row;
            }
        }
        throw new AssertionError("No row with " + value + " in column " + column + " of " + sheet.getSheetName());
    }

    private static List<String> text(Row row) {
        var values = new ArrayList<String>();
        row.cellIterator().forEachRemaining(cell -> values.add(FORMATTER.formatCellValue(cell)));
        return values;
    }

    private static String pdfText(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    /** A finished article (price 8500) of the test range, named {@code prefix-<unique>}, with stock in the store. */
    private UUID article(String prefix) throws Exception {
        var article = createArticle(prefix + "-" + uniqueName(), "3120.5");
        jdbcTemplate.update("update articles set range_id = ? where id = ?", rangeId, article);
        move(article, "STORE", StockMovementType.RECEIPT, "50");
        return article;
    }

    private UUID createSale(String channelCode, UUID article, String quantity) throws Exception {
        return createSale(channelCode, List.of(article), List.of(quantity));
    }

    private UUID createSale(String channelCode, List<UUID> articles, List<String> quantities) throws Exception {
        var lines = new StringBuilder();
        for (int index = 0; index < articles.size(); index++) {
            var line = "{\"articleId\":\"%s\",\"quantity\":%s}".formatted(articles.get(index), quantities.get(index));
            lines.append(index == 0 ? "" : ",").append(line);
        }
        var body = "{\"salesChannelId\":\"%s\",\"sellerId\":\"%s\",\"paymentMethod\":\"CASH\",\"lines\":[%s]}"
                .formatted(channel(channelCode), seller.getId(), lines);
        var json = mockMvc.perform(post("/api/v1/sales")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private UUID channel(String code) {
        return jdbcTemplate.queryForObject("select id from sales_channels where code = ?", UUID.class, code);
    }
}
