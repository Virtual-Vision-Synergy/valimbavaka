package org.vvs.valimbavaka.feature.article.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;

/**
 * ART-01: creation of an article (types, range, price, supplier, barcode, validation) traced in the journal.
 */
class ArticleCreationIT extends ArticleApiTestSupport {

    @Test
    void createArticle_finishedProductByAdmin_returns201WithCodeAndTracesInJournal() throws Exception {
        // Arrange
        var token = adminToken();
        var name = uniqueName();

        // Act + Assert
        mockMvc.perform(postJson(ARTICLES, finishedBody(name)).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.code").value(matchesPattern("ART-\\d{4,}")))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.type").value("FINISHED"))
                .andExpect(jsonPath("$.rangeId").value(rangeId().toString()))
                .andExpect(jsonPath("$.unit").value("PIECE"))
                .andExpect(jsonPath("$.cost").value(3120.5))
                .andExpect(jsonPath("$.price").value(8500))
                .andExpect(jsonPath("$.threshold").value(40))
                .andExpect(jsonPath("$.active").value(true));
        mockMvc.perform(get("/api/v1/journal").param("search", name).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.items[0].action").value("CREATE"))
                .andExpect(jsonPath("$.items[0].resourceType").value("ARTICLE"));
    }

    @Test
    void createArticle_rawMaterialAndSemiFinished_areRecordedWithoutRangeNorPrice() throws Exception {
        // Arrange
        var token = adminToken();

        // Act + Assert
        for (var type : new String[] {"RAW_MATERIAL", "SEMI_FINISHED"}) {
            var body = """
                    {"name":"%s","type":"%s","unit":"KILOGRAM","threshold":5}
                    """.formatted(uniqueName(), type);
            mockMvc.perform(postJson(ARTICLES, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.type").value(type))
                    .andExpect(jsonPath("$.rangeId").doesNotExist())
                    .andExpect(jsonPath("$.price").doesNotExist());
        }
    }

    @Test
    void createArticle_rawMaterialWithRangeAndPrice_dropsThem() throws Exception {
        // Arrange
        var body = """
                {"name":"%s","type":"RAW_MATERIAL","unit":"KILOGRAM","threshold":5,
                 "rangeId":"%s","price":15000,"cost":12000}
                """.formatted(uniqueName(), rangeId());

        // Act + Assert
        mockMvc.perform(postJson(ARTICLES, body).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rangeId").doesNotExist())
                .andExpect(jsonPath("$.price").doesNotExist())
                .andExpect(jsonPath("$.cost").value(12000));
    }

    @Test
    void createArticle_massAssignmentOfIdCodeAndStatus_isIgnored() throws Exception {
        // Arrange
        var forcedId = UUID.randomUUID();
        var body = """
                {"id":"%s","code":"HACK-1","active":false,"name":"%s","type":"RAW_MATERIAL",
                 "unit":"PIECE","threshold":0}
                """.formatted(forcedId, uniqueName());

        // Act
        var result = mockMvc.perform(postJson(ARTICLES, body).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isCreated())
                .andReturn();

        // Assert
        var id = UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
        var saved = articleRepository.findById(id).orElseThrow();
        assertThat(id).isNotEqualTo(forcedId);
        assertThat(saved.getCode()).startsWith("ART-");
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void createArticle_emptyBody_returns422() throws Exception {
        mockMvc.perform(postJson(ARTICLES, "{}").header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createArticle_invalidValues_returns422() throws Exception {
        // Arrange
        var token = adminToken();
        var invalidBodies = new String[] {
            "{\"name\":\"" + "a".repeat(121) + "\",\"type\":\"RAW_MATERIAL\",\"unit\":\"PIECE\",\"threshold\":0}",
            "{\"name\":\"   \",\"type\":\"RAW_MATERIAL\",\"unit\":\"PIECE\",\"threshold\":0}",
            "{\"name\":\"X\",\"type\":\"RAW_MATERIAL\",\"unit\":\"PIECE\",\"threshold\":-1}",
            "{\"name\":\"X\",\"type\":\"RAW_MATERIAL\",\"unit\":\"PIECE\",\"threshold\":0,\"cost\":-0.01}",
            "{\"name\":\"X\",\"type\":\"RAW_MATERIAL\",\"unit\":\"PIECE\",\"threshold\":0,\"cost\":10.123}",
            "{\"name\":\"X\",\"type\":\"RAW_MATERIAL\",\"unit\":\"PIECE\",\"threshold\":0,\"barcode\":\"12345\"}",
            "{\"name\":\"X\",\"type\":\"RAW_MATERIAL\",\"unit\":\"PIECE\",\"threshold\":0,"
                    + "\"barcode\":\"abcdefghijklm\"}"
        };

        // Act + Assert
        for (var body : invalidBodies) {
            mockMvc.perform(postJson(ARTICLES, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
    }

    @Test
    void createArticle_boundaryValues_areAccepted() throws Exception {
        // Arrange
        var name = "a".repeat(120);
        var body = """
                {"name":"%s","type":"RAW_MATERIAL","unit":"LITER","threshold":0,"cost":0,
                 "barcode":"%s"}
                """.formatted(name, uniqueBarcode());

        // Act + Assert
        mockMvc.perform(postJson(ARTICLES, body).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cost").value(0))
                .andExpect(jsonPath("$.threshold").value(0));
    }

    @Test
    void createArticle_unknownTypeOrUnit_returns400() throws Exception {
        // Arrange
        var body = "{\"name\":\"X\",\"type\":\"PACKAGING\",\"unit\":\"PIECE\",\"threshold\":0}";

        // Act + Assert
        mockMvc.perform(postJson(ARTICLES, body).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void createArticle_finishedProductWithoutRangeOrPrice_returns422() throws Exception {
        // Arrange
        var token = adminToken();
        var withoutRange = "{\"name\":\"X\",\"type\":\"FINISHED\",\"unit\":\"PIECE\",\"threshold\":0,\"price\":8500}";
        var withoutPrice = """
                {"name":"X","type":"FINISHED","unit":"PIECE","threshold":0,"rangeId":"%s"}
                """.formatted(rangeId());

        // Act + Assert
        mockMvc.perform(postJson(ARTICLES, withoutRange).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("La gamme est obligatoire pour un produit fini."));
        mockMvc.perform(postJson(ARTICLES, withoutPrice).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Le prix est obligatoire pour un produit fini."));
    }

    @Test
    void createArticle_unknownRangeOrSupplier_returns422() throws Exception {
        // Arrange
        var token = adminToken();
        var unknownRange = """
                {"name":"X","type":"FINISHED","unit":"PIECE","threshold":0,"price":8500,"rangeId":"%s"}
                """.formatted(UUID.randomUUID());
        var unknownSupplier = """
                {"name":"X","type":"RAW_MATERIAL","unit":"PIECE","threshold":0,"supplierId":"%s"}
                """.formatted(UUID.randomUUID());

        // Act + Assert
        mockMvc.perform(postJson(ARTICLES, unknownRange).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent());
        mockMvc.perform(postJson(ARTICLES, unknownSupplier).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void createArticle_withActiveSupplier_linksTheArticleToIt() throws Exception {
        // Arrange
        var token = adminToken();
        var supplierId = createSupplier(token);
        var body = """
                {"name":"%s","type":"RAW_MATERIAL","unit":"KILOGRAM","threshold":5,"supplierId":"%s"}
                """.formatted(uniqueName(), supplierId);

        // Act + Assert
        mockMvc.perform(postJson(ARTICLES, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.supplierId").value(supplierId.toString()));
        mockMvc.perform(get(ARTICLES)
                        .param("supplierId", supplierId.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    void createArticle_deactivatedSupplier_returns422() throws Exception {
        // Arrange
        var token = adminToken();
        var supplierId = createSupplier(token);
        mockMvc.perform(patch("/api/v1/suppliers/" + supplierId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
        var body = """
                {"name":"%s","type":"RAW_MATERIAL","unit":"PIECE","threshold":0,"supplierId":"%s"}
                """.formatted(uniqueName(), supplierId);

        // Act + Assert
        mockMvc.perform(postJson(ARTICLES, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void createArticle_barcodeAlreadyUsed_returns409() throws Exception {
        // Arrange
        var token = adminToken();
        var barcode = uniqueBarcode();
        createArticle(token, rawMaterialBody(uniqueName(), barcode));

        // Act + Assert
        mockMvc.perform(postJson(ARTICLES, rawMaterialBody(uniqueName(), barcode))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void createArticle_sameNameTwice_isAcceptedWithTwoDifferentCodes() throws Exception {
        // Arrange
        var token = adminToken();
        var name = uniqueName();

        // Act
        var first = createArticle(token, rawMaterialBody(name, null));
        var second = createArticle(token, rawMaterialBody(name, null));

        // Assert
        assertThat(articleRepository.findById(first).orElseThrow().getCode())
                .isNotEqualTo(articleRepository.findById(second).orElseThrow().getCode());
    }
}
