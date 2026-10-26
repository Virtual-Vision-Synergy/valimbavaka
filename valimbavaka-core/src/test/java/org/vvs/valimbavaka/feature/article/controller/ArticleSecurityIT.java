package org.vvs.valimbavaka.feature.article.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import org.vvs.valimbavaka.feature.user.domain.RoleName;

/**
 * ART-01: the article sheet is readable by the roles holding articles.read, written by the administrator only,
 * never deleted.
 */
class ArticleSecurityIT extends ArticleApiTestSupport {

    @Test
    void getArticles_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(ARTICLES))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getArticles_withDeliveryDriverRole_returns403() throws Exception {
        // Arrange
        var token = accessToken(createUser(RoleName.DELIVERY_DRIVER), PASSWORD);

        // Act + Assert
        mockMvc.perform(get(ARTICLES).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getArticles_withSellerRole_returns200() throws Exception {
        // Arrange
        var token = accessToken(createUser(RoleName.SELLER), PASSWORD);

        // Act + Assert
        mockMvc.perform(get(ARTICLES).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void createArticle_withSellerRole_returns403() throws Exception {
        // Arrange
        var token = accessToken(createUser(RoleName.SELLER), PASSWORD);

        // Act + Assert
        mockMvc.perform(postJson(ARTICLES, finishedBody(uniqueName())).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void createArticle_withoutToken_returns401() throws Exception {
        mockMvc.perform(postJson(ARTICLES, finishedBody(uniqueName()))).andExpect(status().isUnauthorized());
    }

    @Test
    void getArticleById_existingArticle_returnsTheWholeSheet() throws Exception {
        // Arrange
        var token = adminToken();
        var name = uniqueName();
        var id = createArticle(token, finishedBody(name));

        // Act + Assert
        mockMvc.perform(get(ARTICLES + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.type").value("FINISHED"))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void getArticleById_unknownId_returns404() throws Exception {
        mockMvc.perform(get(ARTICLES + "/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void getArticles_byTypeAndSearch_returnsOnlyMatchingArticles() throws Exception {
        // Arrange
        var token = adminToken();
        var prefix = uniqueName();
        createArticle(token, finishedBody(prefix + " fini"));
        createArticle(token, rawMaterialBody(prefix + " matiere", null));

        // Act + Assert
        mockMvc.perform(get(ARTICLES)
                        .param("type", "RAW_MATERIAL")
                        .param("search", prefix.toUpperCase())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].name").value(prefix + " matiere"));
    }

    @Test
    void getArticles_unknownTypeOrSizeAboveLimit_isRefused() throws Exception {
        // Arrange
        var token = adminToken();

        // Act + Assert
        mockMvc.perform(get(ARTICLES).param("type", "PACKAGING").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(ARTICLES).param("size", "100").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(get(ARTICLES).param("size", "101").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void getArticles_sortOutsideTheWhiteList_fallsBackToDefaultSort() throws Exception {
        mockMvc.perform(get(ARTICLES)
                        .param("sortBy", "supplierId; drop table articles")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk());
    }

    @Test
    void createArticle_byAdmin_fillsTheAuditAuthorWithHisAccountId() throws Exception {
        // Arrange
        var token = adminToken();
        var adminId = userRepository
                .findByEmailWithRoleAndPermissions(ADMIN_EMAIL)
                .orElseThrow()
                .getId();

        // Act
        var id = createArticle(token, finishedBody(uniqueName()));

        // Assert
        var saved = articleRepository.findById(id).orElseThrow();
        assertThat(saved.getCreatedBy()).isEqualTo(adminId);
        assertThat(saved.getUpdatedBy()).isEqualTo(adminId);
    }

    @Test
    void deleteArticle_isNotExposed() throws Exception {
        // Arrange
        var token = adminToken();
        var id = createArticle(token, finishedBody(uniqueName()));

        // Act + Assert
        mockMvc.perform(delete(ARTICLES + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isMethodNotAllowed());
        assertThat(articleRepository.findById(id)).isPresent();
    }
}
