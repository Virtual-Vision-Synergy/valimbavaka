package org.vvs.valimbavaka.feature.article.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import org.vvs.valimbavaka.feature.user.domain.RoleName;

/**
 * ART-01: modification and activation of an article (version, barcode, mass assignment) traced in the journal.
 */
class ArticleUpdateIT extends ArticleApiTestSupport {

    @Test
    void updateArticle_byAdmin_changesEveryField() throws Exception {
        // Arrange
        var token = adminToken();
        var id = createArticle(token, finishedBody(uniqueName()));
        var supplierId = createSupplier(token);
        var name = uniqueName();
        var barcode = uniqueBarcode();
        var body = """
                {"name":"%s","barcode":"%s","type":"SEMI_FINISHED","unit":"KILOGRAM","cost":4100.25,
                 "threshold":12.5,"supplierId":"%s"}
                """.formatted(name, barcode, supplierId);

        // Act + Assert
        mockMvc.perform(putJson(ARTICLES + "/" + id, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.barcode").value(barcode))
                .andExpect(jsonPath("$.type").value("SEMI_FINISHED"))
                .andExpect(jsonPath("$.unit").value("KILOGRAM"))
                .andExpect(jsonPath("$.cost").value(4100.25))
                .andExpect(jsonPath("$.threshold").value(12.5))
                .andExpect(jsonPath("$.supplierId").value(supplierId.toString()))
                .andExpect(jsonPath("$.rangeId").doesNotExist())
                .andExpect(jsonPath("$.price").doesNotExist());
    }

    @Test
    void updateArticle_withoutToken_returns401() throws Exception {
        mockMvc.perform(putJson(ARTICLES + "/" + UUID.randomUUID(), finishedBody(uniqueName())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateArticle_withSellerRole_returns403AndKeepsTheSheet() throws Exception {
        // Arrange
        var adminToken = adminToken();
        var name = uniqueName();
        var id = createArticle(adminToken, finishedBody(name));
        var token = accessToken(createUser(RoleName.SELLER), PASSWORD);

        // Act + Assert
        mockMvc.perform(putJson(ARTICLES + "/" + id, finishedBody(uniqueName()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
        assertThat(articleRepository.findById(id).orElseThrow().getName()).isEqualTo(name);
    }

    @Test
    void updateArticle_unknownId_returns404() throws Exception {
        mockMvc.perform(putJson(ARTICLES + "/" + UUID.randomUUID(), finishedBody(uniqueName()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void updateArticle_staleVersion_returns409AndKeepsTheSheet() throws Exception {
        // Arrange
        var token = adminToken();
        var name = uniqueName();
        var id = createArticle(token, finishedBody(name));
        var body = """
                {"name":"%s","type":"RAW_MATERIAL","unit":"PIECE","threshold":0,"version":42}
                """.formatted(uniqueName());

        // Act + Assert
        mockMvc.perform(putJson(ARTICLES + "/" + id, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
        assertThat(articleRepository.findById(id).orElseThrow().getName()).isEqualTo(name);
    }

    @Test
    void updateArticle_barcodeOfAnotherArticle_returns409() throws Exception {
        // Arrange
        var token = adminToken();
        var barcode = uniqueBarcode();
        createArticle(token, rawMaterialBody(uniqueName(), barcode));
        var id = createArticle(token, rawMaterialBody(uniqueName(), null));

        // Act + Assert
        mockMvc.perform(putJson(ARTICLES + "/" + id, rawMaterialBody(uniqueName(), barcode))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateArticle_ownBarcodeUnchanged_isAccepted() throws Exception {
        // Arrange
        var token = adminToken();
        var barcode = uniqueBarcode();
        var id = createArticle(token, rawMaterialBody(uniqueName(), barcode));

        // Act + Assert
        mockMvc.perform(putJson(ARTICLES + "/" + id, rawMaterialBody(uniqueName(), barcode))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void updateArticle_invalidData_returns422AndKeepsTheSheet() throws Exception {
        // Arrange
        var token = adminToken();
        var name = uniqueName();
        var id = createArticle(token, finishedBody(name));
        var body = "{\"name\":\"\",\"type\":\"FINISHED\",\"unit\":\"PIECE\",\"threshold\":-5}";

        // Act + Assert
        mockMvc.perform(putJson(ARTICLES + "/" + id, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        assertThat(articleRepository.findById(id).orElseThrow().getName()).isEqualTo(name);
    }

    @Test
    void updateArticle_massAssignmentOfIdCodeAndStatus_isIgnored() throws Exception {
        // Arrange
        var token = adminToken();
        var id = createArticle(token, finishedBody(uniqueName()));
        var code = articleRepository.findById(id).orElseThrow().getCode();
        var body = """
                {"id":"%s","code":"HACK-1","active":false,"name":"%s","type":"RAW_MATERIAL",
                 "unit":"PIECE","threshold":0}
                """.formatted(UUID.randomUUID(), uniqueName());

        // Act
        mockMvc.perform(putJson(ARTICLES + "/" + id, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.active").value(true));

        // Assert
        assertThat(articleRepository.findById(id).orElseThrow().isActive()).isTrue();
    }

    @Test
    void updateArticle_withVersionRead_savesAndTracesUpdateInJournal() throws Exception {
        // Arrange
        var token = adminToken();
        var name = uniqueName();
        var id = createArticle(token, finishedBody(name));
        var body = finishedBody(name).replace("\"threshold\":40", "\"threshold\":25").replace("}", ",\"version\":0}");

        // Act
        mockMvc.perform(putJson(ARTICLES + "/" + id, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.threshold").value(25));

        // Assert
        mockMvc.perform(get("/api/v1/journal").param("search", name).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].action").value("UPDATE"))
                .andExpect(jsonPath("$.items[0].resourceId").value(id.toString()));
    }

    @Test
    void updateArticle_withoutChange_leavesNoNewJournalLine() throws Exception {
        // Arrange
        var token = adminToken();
        var name = uniqueName();
        var id = createArticle(token, finishedBody(name));

        // Act
        mockMvc.perform(putJson(ARTICLES + "/" + id, finishedBody("  " + name + " "))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        // Assert
        mockMvc.perform(get("/api/v1/journal").param("search", name).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].action").value("CREATE"));
    }

    @Test
    void setActive_byAdmin_deactivatesThenReactivatesAndTracesOnlyRealChanges() throws Exception {
        // Arrange
        var token = adminToken();
        var name = uniqueName();
        var id = createArticle(token, finishedBody(name));

        // Act + Assert
        for (var active : new boolean[] {false, false, true}) {
            mockMvc.perform(patch(ARTICLES + "/" + id + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"active\":" + active + "}")
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(active));
        }
        mockMvc.perform(get("/api/v1/journal").param("search", name).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.items.length()").value(3));
    }

    @Test
    void setActive_withoutActiveField_returns422() throws Exception {
        // Arrange
        var token = adminToken();
        var id = createArticle(token, finishedBody(uniqueName()));

        // Act + Assert
        mockMvc.perform(patch(ARTICLES + "/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void setActive_withSellerRole_returns403() throws Exception {
        // Arrange
        var id = createArticle(adminToken(), finishedBody(uniqueName()));
        var token = accessToken(createUser(RoleName.SELLER), PASSWORD);

        // Act + Assert
        mockMvc.perform(patch(ARTICLES + "/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void setActive_unknownId_returns404() throws Exception {
        mockMvc.perform(patch(ARTICLES + "/" + UUID.randomUUID() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isNotFound());
    }
}
