package org.vvs.valimbavaka.feature.article.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

/** ART-07: bulk import of articles from a CSV file (partial import, conflicts, overwrite mode, journal). */
class ArticleImportIT extends ArticleApiTestSupport {

    private static final String HEADER = "code;name;barcode;type;unit;threshold\n";

    @Test
    void importArticles_validFile_createsArticlesAndTracesEachOneInJournal() throws Exception {
        // Arrange
        var token = adminToken();
        var first = uniqueName();
        var second = uniqueName();
        var csv = HEADER
                + ";%s;;RAW_MATERIAL;KILOGRAM;5\n;%s;%s;SEMI_FINISHED;LITER;2,5\n"
                        .formatted(first, second, uniqueBarcode());

        // Act + Assert
        importFile(token, csv, "SKIP")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(2))
                .andExpect(jsonPath("$.conflicts").value(0))
                .andExpect(jsonPath("$.rows[0].code").value(matchesPattern("ART-\\d{4,}")))
                .andExpect(jsonPath("$.rows[1].status").value("CREATED"));
        mockMvc.perform(get(ARTICLES).param("search", second).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.items[0].threshold").value(2.5))
                .andExpect(jsonPath("$.items[0].active").value(true));
        mockMvc.perform(get("/api/v1/journal").param("search", first).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.items[0].action").value("CREATE"))
                .andExpect(jsonPath("$.items[0].resourceType").value("ARTICLE"));
    }

    @Test
    void importArticles_partialFile_keepsValidRowsAndReportsTheOthers() throws Exception {
        // Arrange
        var token = adminToken();
        var barcode = uniqueBarcode();
        createArticle(token, rawMaterialBody(uniqueName(), barcode));
        var csv = HEADER
                + ";%s;;RAW_MATERIAL;KILOGRAM;5\n;%s;%s;RAW_MATERIAL;KILOGRAM;5\n;;;RAW_MATERIAL;KILOGRAM;5\n"
                        .formatted(uniqueName(), uniqueName(), barcode);

        // Act + Assert
        importFile(token, csv, "SKIP")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(1))
                .andExpect(jsonPath("$.conflicts").value(1))
                .andExpect(jsonPath("$.invalid").value(1))
                .andExpect(jsonPath("$.rows[1].status").value("CONFLICT"))
                .andExpect(jsonPath("$.rows[2].message").value("Le nom est obligatoire"));
    }

    @Test
    void importArticles_existingBarcodeInSkipMode_keepsExistingArticle() throws Exception {
        // Arrange
        var token = adminToken();
        var barcode = uniqueBarcode();
        var name = uniqueName();
        createArticle(token, rawMaterialBody(name, barcode));
        var csv = HEADER + ";Autre nom;%s;RAW_MATERIAL;KILOGRAM;5\n".formatted(barcode);

        // Act + Assert
        importFile(token, csv, "SKIP")
                .andExpect(jsonPath("$.conflicts").value(1))
                .andExpect(jsonPath("$.rows[0].message").value(containsString("existe déjà")));
        mockMvc.perform(get(ARTICLES).param("search", barcode).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.items[0].name").value(name));
    }

    @Test
    void importArticles_conflictReplayedInOverwriteMode_updatesArticleAndTracesIt() throws Exception {
        // Arrange
        var token = adminToken();
        var barcode = uniqueBarcode();
        createArticle(token, rawMaterialBody(uniqueName(), barcode));
        var newName = uniqueName();
        var csv = HEADER + ";%s;%s;RAW_MATERIAL;KILOGRAM;7\n".formatted(newName, barcode);
        importFile(token, csv, "SKIP").andExpect(jsonPath("$.conflicts").value(1));

        // Act
        var replay = importFile(token, csv, "OVERWRITE", "1");

        // Assert
        replay.andExpect(jsonPath("$.updated").value(1))
                .andExpect(jsonPath("$.rows[0].status").value("UPDATED"));
        mockMvc.perform(get(ARTICLES).param("search", barcode).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.items[0].name").value(newName))
                .andExpect(jsonPath("$.items[0].threshold").value(7));
        mockMvc.perform(get("/api/v1/journal")
                        .param("search", newName)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.items[0].action").value("UPDATE"));
    }

    @Test
    void importArticles_overwriteWithIdenticalData_reportsUnchanged() throws Exception {
        // Arrange
        var token = adminToken();
        var barcode = uniqueBarcode();
        var name = uniqueName();
        createArticle(token, rawMaterialBody(name, barcode));
        var csv = HEADER + ";%s;%s;RAW_MATERIAL;KILOGRAM;5\n".formatted(name, barcode);

        // Act + Assert
        importFile(token, csv, "OVERWRITE")
                .andExpect(jsonPath("$.unchanged").value(1))
                .andExpect(jsonPath("$.updated").value(0));
    }

    @Test
    void importArticles_onlyRows_leavesTheOtherRowsUntouched() throws Exception {
        // Arrange
        var token = adminToken();
        var csv = HEADER
                + ";%s;;RAW_MATERIAL;KILOGRAM;5\n;%s;;RAW_MATERIAL;KILOGRAM;5\n".formatted(uniqueName(), uniqueName());

        // Act + Assert
        importFile(token, csv, "SKIP", "2")
                .andExpect(jsonPath("$.created").value(1))
                .andExpect(jsonPath("$.rows.length()").value(1))
                .andExpect(jsonPath("$.rows[0].row").value(2));
    }

    @Test
    void importArticles_codeFromFile_isKeptAndNextGeneratedCodeDoesNotCollide() throws Exception {
        // Arrange
        var token = adminToken();
        var next = jdbcTemplate.queryForObject("select nextval('article_code_seq')", Long.class);
        var fileCode = "ART-%04d".formatted(next + 50);
        var csv = HEADER + "%s;%s;;RAW_MATERIAL;KILOGRAM;5\n".formatted(fileCode, uniqueName());

        // Act
        importFile(token, csv, "SKIP").andExpect(jsonPath("$.rows[0].code").value(fileCode));
        var generated = createArticle(token, rawMaterialBody(uniqueName(), null));

        // Assert
        var code = articleRepository.findById(generated).orElseThrow().getCode();
        assertThat(code).isNotEqualTo(fileCode);
        assertThat(Long.parseLong(code.substring("ART-".length()))).isGreaterThan(next + 50);
    }

    @Test
    void importArticles_decimalCommaAndLowerCaseEnums_areAccepted() throws Exception {
        // Arrange
        var token = adminToken();
        var csv = HEADER + ";%s;;raw_material;kilogram;0,125\n".formatted(uniqueName());

        // Act + Assert
        importFile(token, csv, "SKIP").andExpect(jsonPath("$.created").value(1));
    }

    @Test
    void importArticles_unknownColumnActive_returns422AndCreatesNothing() throws Exception {
        // Arrange
        var token = adminToken();
        var name = uniqueName();
        var csv = "name;type;unit;threshold;active\n%s;RAW_MATERIAL;KILOGRAM;5;false\n".formatted(name);

        // Act + Assert
        importFile(token, csv, "SKIP")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(get(ARTICLES).param("search", name).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void importArticles_emptyFile_returns422() throws Exception {
        // Act + Assert
        importFile(adminToken(), "", "SKIP")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void importArticles_moreThan1000Rows_returns422() throws Exception {
        // Arrange
        var rows = IntStream.rangeClosed(1, 1001).mapToObj(i -> ";Article %d;;RAW_MATERIAL;PIECE;1\n".formatted(i));
        var csv = HEADER + String.join("", rows.toList());

        // Act + Assert
        importFile(adminToken(), csv, "SKIP").andExpect(status().isUnprocessableContent());
    }

    @Test
    void importArticles_unknownConflictMode_returns400() throws Exception {
        // Act + Assert
        importFile(adminToken(), HEADER + ";A;;RAW_MATERIAL;PIECE;1\n", "REPLACE")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void importArticles_withoutFilePart_returns400() throws Exception {
        // Act + Assert
        mockMvc.perform(multipart(ARTICLES + "/import").header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isBadRequest());
    }

    private ResultActions importFile(String token, String csv, String mode, String... onlyRows) throws Exception {
        var file = new MockMultipartFile("file", "articles.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        var request = multipart(ARTICLES + "/import")
                .file(file)
                .param("onConflict", mode)
                .header(HttpHeaders.AUTHORIZATION, bearer(token));
        if (onlyRows.length > 0) {
            request.param("onlyRows", String.join(",", onlyRows));
        }
        return mockMvc.perform(request);
    }
}
