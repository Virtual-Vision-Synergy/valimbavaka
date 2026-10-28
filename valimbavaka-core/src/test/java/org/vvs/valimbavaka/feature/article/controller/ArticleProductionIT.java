package org.vvs.valimbavaka.feature.article.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import org.vvs.valimbavaka.feature.user.domain.RoleName;

/** ART-04: MP/SF are the possible production inputs, finished products the possible outputs (active only). */
class ArticleProductionIT extends ArticleApiTestSupport {

    private static final String PRODUCTION = ARTICLES + "/production";

    @Test
    void getProductionArticles_inputUsage_listsActiveRawMaterialsAndSemiFinishedOnly() throws Exception {
        // Arrange
        var token = adminToken();
        var prefix = uniquePrefix();
        createCatalogue(token, prefix);

        // Act + Assert
        mockMvc.perform(get(PRODUCTION)
                        .param("usage", "INPUT")
                        .param("search", prefix)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.items[*].name").value(containsInAnyOrder(prefix + "MP", prefix + "SF")));
    }

    @Test
    void getProductionArticles_outputUsage_listsActiveFinishedProductsOnly() throws Exception {
        // Arrange
        var token = adminToken();
        var prefix = uniquePrefix();
        createCatalogue(token, prefix);

        // Act + Assert
        mockMvc.perform(get(PRODUCTION)
                        .param("usage", "OUTPUT")
                        .param("search", prefix)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].name").value(prefix + "PF"))
                .andExpect(jsonPath("$.items[0].type").value("FINISHED"));
    }

    @Test
    void getProductionArticles_noMatchingArticle_returnsEmptyPage() throws Exception {
        mockMvc.perform(get(PRODUCTION)
                        .param("usage", "INPUT")
                        .param("search", uniquePrefix())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void getProductionArticles_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(PRODUCTION).param("usage", "INPUT"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getProductionArticles_withDeliveryDriverRole_returns403() throws Exception {
        // Arrange
        var token = accessToken(createUser(RoleName.DELIVERY_DRIVER), PASSWORD);

        // Act + Assert
        mockMvc.perform(get(PRODUCTION).param("usage", "INPUT").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getProductionArticles_withSellerRole_returns200() throws Exception {
        // Arrange
        var token = accessToken(createUser(RoleName.SELLER), PASSWORD);

        // Act + Assert
        mockMvc.perform(get(PRODUCTION).param("usage", "OUTPUT").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void getProductionArticles_unknownUsage_returns400() throws Exception {
        var token = adminToken();

        mockMvc.perform(get(PRODUCTION).param("usage", "PACKAGING").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void getProductionArticles_missingUsage_returns400() throws Exception {
        mockMvc.perform(get(PRODUCTION).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    /** One article of each type plus a deactivated raw material, all named with the given prefix. */
    private void createCatalogue(String token, String prefix) throws Exception {
        createArticle(token, rawMaterialBody(prefix + "MP", null));
        createArticle(token, semiFinishedBody(prefix + "SF"));
        createArticle(token, finishedBody(prefix + "PF"));
        var inactiveId = createArticle(token, rawMaterialBody(prefix + "MP-inactif", null));
        mockMvc.perform(patch(ARTICLES + "/" + inactiveId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    private static String semiFinishedBody(String name) {
        return "{\"name\":\"%s\",\"type\":\"SEMI_FINISHED\",\"unit\":\"KILOGRAM\",\"threshold\":2}".formatted(name);
    }

    private static String uniquePrefix() {
        return "Prod-" + UUID.randomUUID().toString().substring(0, 8) + "-";
    }
}
