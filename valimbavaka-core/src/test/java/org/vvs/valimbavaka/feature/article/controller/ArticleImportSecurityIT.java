package org.vvs.valimbavaka.feature.article.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import org.vvs.valimbavaka.feature.user.domain.RoleName;

/** ART-07: the bulk import is reserved to the administrator ({@code articles.import}). */
class ArticleImportSecurityIT extends ArticleApiTestSupport {

    @Test
    void importArticles_withoutToken_returns401() throws Exception {
        mockMvc.perform(request())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void importArticles_withSellerRole_returns403() throws Exception {
        // Arrange
        var token = accessToken(createUser(RoleName.SELLER), PASSWORD);

        // Act + Assert
        mockMvc.perform(request().header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void importArticles_withDeliveryDriverRole_returns403() throws Exception {
        // Arrange
        var token = accessToken(createUser(RoleName.DELIVERY_DRIVER), PASSWORD);

        // Act + Assert
        mockMvc.perform(request().header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    private static MockMultipartHttpServletRequestBuilder request() {
        var csv = "name;type;unit;threshold\nSavon;RAW_MATERIAL;PIECE;1\n";
        return multipart(ARTICLES + "/import")
                .file(new MockMultipartFile("file", "a.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)));
    }
}
