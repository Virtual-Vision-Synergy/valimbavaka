package org.vvs.valimbavaka.feature.article.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.shared.excel.web.ExcelHttpMessageConverter;

/** ART-08: the administrator exports the article list as xlsx, without the purchase cost; nobody else can. */
class ArticleExportIT extends ArticleApiTestSupport {

    private static final String EXPORTABLE = ARTICLES + "/exportable";
    private static final String EXPORT = EXPORTABLE + "/export";
    private static final List<String> HEADERS =
            List.of("Code", "Nom", "Code-barres", "Type", "Unité", "Prix de vente", "Seuil minimum", "Actif");

    @Test
    void exportArticles_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(EXPORT))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void exportArticles_withSellerRole_returns403() throws Exception {
        // Arrange
        var token = accessToken(createUser(RoleName.SELLER), PASSWORD);

        // Act + Assert
        mockMvc.perform(get(EXPORT).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void exportArticles_withDeliveryDriverRole_returns403() throws Exception {
        // Arrange
        var token = accessToken(createUser(RoleName.DELIVERY_DRIVER), PASSWORD);

        // Act + Assert
        mockMvc.perform(get(EXPORT).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getExportableArticles_withSellerRole_returns403() throws Exception {
        // Arrange
        var token = accessToken(createUser(RoleName.SELLER), PASSWORD);

        // Act + Assert
        mockMvc.perform(get(EXPORTABLE).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void exportArticles_byAdmin_returnsXlsxWithHeadersAndNoCostColumn() throws Exception {
        // Arrange
        var token = adminToken();
        var name = uniqueName();
        createArticle(token, finishedBody(name));

        // Act
        var bytes = mockMvc.perform(exportRequest(token, name))
                .andExpect(status().isOk())
                .andExpect(content().contentType(ExcelHttpMessageConverter.XLSX))
                .andExpect(header().string(
                                HttpHeaders.CONTENT_DISPOSITION,
                                allOf(startsWith("attachment"), containsString("filename=\"articles.xlsx\""))))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        // Assert
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheet("Articles");
            assertThat(texts(sheet.getRow(0))).containsExactlyElementsOf(HEADERS);
            assertThat(sheet.getLastRowNum()).isEqualTo(1);
            var row = texts(sheet.getRow(1));
            assertThat(row.get(0)).startsWith("ART-");
            assertThat(row.subList(1, 8)).containsExactly(name, "", "FINISHED", "PIECE", "8,500.00", "40.00", "TRUE");
            assertThat(sheet.getRow(1).getCell(5).getNumericCellValue()).isEqualTo(8500.0);
            assertThat(String.join("|", texts(sheet.getRow(0)))).doesNotContain("Coût");
        }
    }

    @Test
    void exportArticles_filteredByType_containsOnlyMatchingArticles() throws Exception {
        // Arrange
        var token = adminToken();
        var prefix = uniqueName();
        createArticle(token, finishedBody(prefix + "-fini"));
        createArticle(token, rawMaterialBody(prefix + "-matiere", null));

        // Act
        var bytes = download(exportRequest(token, prefix).param("type", "RAW_MATERIAL"));

        // Assert
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheet("Articles");
            assertThat(sheet.getLastRowNum()).isEqualTo(1);
            assertThat(texts(sheet.getRow(1)).get(1)).isEqualTo(prefix + "-matiere");
        }
    }

    @Test
    void exportArticles_inactiveArticle_isExportedWithActiveFalse() throws Exception {
        // Arrange
        var token = adminToken();
        var name = uniqueName();
        var id = createArticle(token, finishedBody(name));
        mockMvc.perform(patch(ARTICLES + "/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        // Act
        var bytes = download(exportRequest(token, name));

        // Assert
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(texts(workbook.getSheet("Articles").getRow(1)).get(7)).isEqualTo("FALSE");
        }
    }

    @Test
    void exportArticles_noMatch_returnsHeaderRowOnly() throws Exception {
        // Act
        var bytes = download(exportRequest(adminToken(), "aucun-article-" + uniqueName()));

        // Assert
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheet("Articles");
            assertThat(sheet.getLastRowNum()).isZero();
            assertThat(texts(sheet.getRow(0))).containsExactlyElementsOf(HEADERS);
        }
    }

    @Test
    void exportArticles_sizeAboveMaxRows_returns422() throws Exception {
        mockMvc.perform(get(EXPORT).param("size", "10001").header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void exportArticles_unknownType_returns400() throws Exception {
        mockMvc.perform(get(EXPORT).param("type", "PACKAGING").header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void getExportableArticles_byAdmin_returnsJsonRowsWithoutCost() throws Exception {
        // Arrange
        var token = adminToken();
        var name = uniqueName();
        createArticle(token, finishedBody(name));

        // Act + Assert
        mockMvc.perform(get(EXPORTABLE).param("search", name).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value(name))
                .andExpect(jsonPath("$.items[0].cost").doesNotExist())
                .andExpect(jsonPath("$.items[0].id").doesNotExist());
    }

    @Test
    void exportArticles_onListRoute_isNotAvailable() throws Exception {
        mockMvc.perform(get(ARTICLES + "/export").header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().is4xxClientError());
    }

    private static MockHttpServletRequestBuilder exportRequest(String token, String search) {
        return get(EXPORT).param("search", search).header(HttpHeaders.AUTHORIZATION, bearer(token));
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
}
