package org.vvs.valimbavaka.shared.excel;

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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.security.annotation.HasPermission;
import org.vvs.valimbavaka.shared.excel.ExcelExportIT.DemoProductRDto;
import org.vvs.valimbavaka.shared.excel.annotation.ExcelExport;
import org.vvs.valimbavaka.shared.excel.event.ExcelExportedEvent;
import org.vvs.valimbavaka.shared.excel.web.ExcelHttpMessageConverter;
import org.vvs.valimbavaka.support.IntegrationTest;

/** Route jumelle {@code /export/pdf}, sur le contrôleur de démonstration de {@link ExcelExportIT}, pile complète. */
@IntegrationTest
@AutoConfigureMockMvc
@RecordApplicationEvents
@Import({ExcelExportIT.DemoController.class, PdfExportIT.XlsxOnlyController.class})
class PdfExportIT {

    private static final String BASE = "/api/v1/excel-demo";
    private static final String XLSX_ONLY = "/api/v1/pdf-demo/xlsx-only";
    private static final String RAW_PDF = "/api/v1/pdf-demo/raw";
    private static final byte[] RAW_BYTES = "%PDF-brut".getBytes(StandardCharsets.US_ASCII);
    private static final int MAX_ROWS = 10_000;

    private static final String ADMIN_LOGIN =
            "{\"email\":\"admin.test@valimbavaka.mg\",\"password\":\"mot-de-passe-de-test-tres-long\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationEvents events;

    private String token;

    @BeforeEach
    void logIn() throws Exception {
        var login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADMIN_LOGIN))
                .andReturn()
                .getResponse()
                .getContentAsString();
        token = JsonPath.read(login, "$.accessToken");
    }

    @Test
    void exportPdf_pagedRoute_returnsPdfWithTitleHeadersAndFormattedValues() throws Exception {
        // Act
        var response = mockMvc.perform(get(BASE + "/products/export/pdf").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(
                                HttpHeaders.CONTENT_DISPOSITION,
                                allOf(startsWith("attachment"), containsString("filename=\"produits.pdf\""))))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        // Assert
        String text = text(response);
        assertThat(text)
                .contains("Produits")
                .contains("Libellé", "Gamme", "Prix", "Mis en vente")
                .contains("Riz", "ÉPICERIE", "2 500,00", "19/10/2026")
                .contains("Produit 150")
                .doesNotContain("non exporté");
    }

    @Test
    void exportPdf_pagedRoute_appliesTheSameQueryParameters() throws Exception {
        // Act
        var response = mockMvc.perform(get(BASE + "/products/export/pdf")
                        .param("page", "2")
                        .param("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        // Assert
        assertThat(text(response)).contains("Savon").doesNotContain("Riz");
    }

    @Test
    void exportPdf_annotatedRoute_publishesOneEventWithPdfFilename() throws Exception {
        // Act
        mockMvc.perform(get(BASE + "/products/export/pdf").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk());

        // Assert
        assertThat(events.stream(ExcelExportedEvent.class))
                .containsExactly(new ExcelExportedEvent("PRODUCT", "produits.pdf", DemoProductRDto.class));
    }

    @Test
    void exportPdf_streamLongerThanMaxRows_returns422WithoutFile() throws Exception {
        mockMvc.perform(get(BASE + "/products/stream/export/pdf")
                        .param("count", String.valueOf(MAX_ROWS + 1))
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(header().doesNotExist(HttpHeaders.CONTENT_DISPOSITION))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(events.stream(ExcelExportedEvent.class)).isEmpty();
    }

    @Test
    void exportPdf_sizeAboveMaxRows_returns422() throws Exception {
        mockMvc.perform(get(BASE + "/products/export/pdf")
                        .param("size", String.valueOf(MAX_ROWS + 1))
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void exportPdf_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(BASE + "/products/export/pdf")).andExpect(status().isUnauthorized());
    }

    @Test
    void exportPdf_missingPermission_returns403() throws Exception {
        mockMvc.perform(get(BASE + "/restricted/export/pdf").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void exportPdf_pdfDisabled_isNotRegisteredButXlsxIs() throws Exception {
        mockMvc.perform(get(XLSX_ONLY + "/export/pdf").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(XLSX_ONLY + "/export").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(ExcelHttpMessageConverter.XLSX));
    }

    @Test
    void rawPdfRoute_notAnnotated_isWrittenAsIs() throws Exception {
        mockMvc.perform(get(RAW_PDF).header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(RAW_BYTES));
    }

    @Test
    void originalRoute_askingForPdf_isNotExported() throws Exception {
        mockMvc.perform(get(BASE + "/products")
                        .accept(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isNotAcceptable());
    }

    private String bearer() {
        return "Bearer " + token;
    }

    private static String text(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    @RestController
    @RequestMapping("/api/v1/pdf-demo")
    static class XlsxOnlyController {

        // Comme une facture déjà rendue (FAC-03) : le convertisseur d'export ne doit pas s'en emparer
        @GetMapping(path = "/raw", produces = MediaType.APPLICATION_PDF_VALUE)
        @HasPermission("users.read")
        public byte[] getRawPdf() {
            return RAW_BYTES.clone();
        }

        @GetMapping("/xlsx-only")
        @ExcelExport(pdf = false)
        @HasPermission("users.read")
        public List<DemoProductRDto> getProducts() {
            return List.of();
        }
    }
}
