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

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.validation.Valid;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.vvs.valimbavaka.security.annotation.HasPermission;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;
import org.vvs.valimbavaka.shared.excel.annotation.ExcelColumn;
import org.vvs.valimbavaka.shared.excel.annotation.ExcelExport;
import org.vvs.valimbavaka.shared.excel.event.ExcelExportedEvent;
import org.vvs.valimbavaka.shared.excel.web.ExcelHttpMessageConverter;
import org.vvs.valimbavaka.support.IntegrationTest;

import com.jayway.jsonpath.JsonPath;

/** Route jumelle {@code /export} sur un contrôleur de démonstration,, pile complète (sécurité incluse). */
@IntegrationTest
@AutoConfigureMockMvc
@RecordApplicationEvents
@Import(ExcelExportIT.DemoController.class)
class ExcelExportIT {

    private static final String BASE = "/api/v1/excel-demo";
    private static final int PRODUCT_COUNT = 150;
    private static final int MAX_ROWS = 10_000;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationEvents events;

    private String token;

    private static final String ADMIN_LOGIN =
            "{\"email\":\"admin.test@valimbavaka.mg\",\"password\":\"mot-de-passe-de-test-tres-long\"}";

    @BeforeEach
    void logIn() throws Exception {
        var login = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(ADMIN_LOGIN))
                .andReturn().getResponse().getContentAsString();
        token = JsonPath.read(login, "$.accessToken");
    }

    @Test
    void export_pagedRoute_returnsXlsxWithOrderedHeadersAndFormattedValues() throws Exception {
        // Act
        var response = mockMvc.perform(get(BASE + "/products/export").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(ExcelHttpMessageConverter.XLSX))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, allOf(
                        startsWith("attachment"),
                        containsString("filename=\"produits.xlsx\""))))
                .andReturn().getResponse().getContentAsByteArray();

        // Assert
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(response))) {
            Sheet sheet = workbook.getSheet("Produits");
            assertThat(texts(sheet.getRow(0))).containsExactly("Libellé", "Gamme", "Prix", "Mis en vente");
            assertThat(texts(sheet.getRow(1))).containsExactly("Riz", "ÉPICERIE", "2,500.00", "19/10/2026");
            assertThat(sheet.getLastRowNum()).isEqualTo(PRODUCT_COUNT);
        }
    }

    @Test
    void export_sizeAboveMaxRows_returns422() throws Exception {
        mockMvc.perform(get(BASE + "/products/export").param("size", String.valueOf(MAX_ROWS + 1))
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void originalRoute_sizeAbove100_isStillRejected() throws Exception {
        mockMvc.perform(get(BASE + "/products").param("size", "101").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void export_streamLongerThanMaxRows_returns422WithoutFile() throws Exception {
        mockMvc.perform(get(BASE + "/products/stream/export").param("count", String.valueOf(MAX_ROWS + 1))
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(header().doesNotExist(HttpHeaders.CONTENT_DISPOSITION))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Export limité à 10000 lignes : affinez les filtres"));
        assertThat(events.stream(ExcelExportedEvent.class)).isEmpty();
    }

    @Test
    void export_pagedRoute_appliesTheSameQueryParameters() throws Exception {
        // Act
        var response = mockMvc.perform(get(BASE + "/products/export").param("page", "2").param("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        // Assert
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(response))) {
            assertThat(workbook.getSheetAt(0).getLastRowNum()).isEqualTo(1);
            assertThat(texts(workbook.getSheetAt(0).getRow(1)).getFirst()).isEqualTo("Savon");
        }
    }

    @Test
    void export_streamRoute_exportsEveryElement() throws Exception {
        // Act
        var response = mockMvc.perform(get(BASE + "/products/stream/export")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("filename=\"DemoProductRDto.xlsx\"")))
                .andReturn().getResponse().getContentAsByteArray();

        // Assert
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(response))) {
            assertThat(workbook.getSheetName(0)).isEqualTo("DemoProductRDto");
            assertThat(workbook.getSheetAt(0).getLastRowNum()).isEqualTo(3);
        }
    }

    @Test
    void export_annotatedRoute_publishesOneEvent() throws Exception {
        // Act
        mockMvc.perform(get(BASE + "/products/export").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk());

        // Assert
        assertThat(events.stream(ExcelExportedEvent.class))
                .containsExactly(new ExcelExportedEvent("PRODUCT", "produits.xlsx", DemoProductRDto.class));
    }

    @Test
    void originalRoute_withAnyAccept_stillReturnsJsonPagedBy20() throws Exception {
        mockMvc.perform(get(BASE + "/products").accept(MediaType.ALL).header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items[0].label").value("Riz"))
                .andExpect(jsonPath("$.items.length()").value(20));
        assertThat(events.stream(ExcelExportedEvent.class)).isEmpty();
    }

    @Test
    void originalRoute_askingForXlsx_isNotExported() throws Exception {
        mockMvc.perform(get(BASE + "/products").accept(ExcelHttpMessageConverter.XLSX)
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    void export_notAnnotatedRoute_isNotRegistered() throws Exception {
        mockMvc.perform(get(BASE + "/plain/export").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void export_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(BASE + "/products/export")).andExpect(status().isUnauthorized());
    }

    @Test
    void export_missingPermission_returns403() throws Exception {
        mockMvc.perform(get(BASE + "/restricted/export").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isForbidden());
    }

    private String bearer() {
        return "Bearer " + token;
    }

    private static List<String> texts(Row row) {
        var formatter = new DataFormatter(Locale.US);
        var values = new ArrayList<String>();
        for (int i = 0; i < row.getLastCellNum(); i++) {
            values.add(formatter.formatCellValue(row.getCell(i)));
        }
        return values;
    }

    record DemoRangeRDto(String name) {
    }

    record DemoProductRDto(
            @ExcelColumn(header = "Libellé", order = 1) String label,
            @ExcelColumn(header = "Gamme", order = 2, expression = "range?.name.toUpperCase()") DemoRangeRDto range,
            @ExcelColumn(header = "Prix", order = 3, format = "#,##0.00", width = 12) BigDecimal price,
            @ExcelColumn(header = "Mis en vente", order = 4) LocalDate onSaleSince,
            String internalNote) {
    }

    @RestController
    @RequestMapping(BASE)
    static class DemoController {

        private static final List<DemoProductRDto> PRODUCTS = Stream.concat(
                Stream.of(new DemoProductRDto("Riz", new DemoRangeRDto("Épicerie"), new BigDecimal("2500"),
                                LocalDate.of(2026, 10, 19), "non exporté"),
                        new DemoProductRDto("Savon", null, new BigDecimal("800.5"), LocalDate.of(2026, 1, 2), "")),
                IntStream.rangeClosed(3, PRODUCT_COUNT).mapToObj(i ->
                        new DemoProductRDto("Produit " + i, null, BigDecimal.valueOf(i), null, "")))
                .toList();

        @GetMapping("/products")
        @ExcelExport(target = "PRODUCT", filename = "produits", sheet = "Produits")
        @HasPermission("users.read")
        public PageResponse<DemoProductRDto> getProducts(@Valid PageQuery query) {
            var items = PRODUCTS.stream().skip((long) (query.page() - 1) * query.size()).limit(query.size()).toList();
            return new PageResponse<>(items, query.page(), query.size(), PRODUCTS.size(), 1);
        }

        @GetMapping("/products/stream")
        @ExcelExport
        @HasPermission("users.read")
        public Stream<DemoProductRDto> streamProducts(@RequestParam(defaultValue = "3") int count) {
            return Stream.generate(() -> PRODUCTS.getFirst()).limit(count);
        }

        @GetMapping("/plain")
        @HasPermission("users.read")
        public List<DemoProductRDto> getPlain() {
            return PRODUCTS;
        }

        @GetMapping("/restricted")
        @ExcelExport
        @HasPermission("excel-demo.unknown")
        public List<DemoProductRDto> getRestricted() {
            return PRODUCTS;
        }
    }
}
