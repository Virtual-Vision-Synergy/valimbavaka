package org.vvs.valimbavaka.feature.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.vvs.valimbavaka.support.IntegrationTest;

import com.jayway.jsonpath.JsonPath;

/** Le contrat OpenAPI n'est jamais public : il exige un jeton valide (convention §7.7). */
@IntegrationTest
@AutoConfigureMockMvc
class OpenApiAccessIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocs_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
    }

    @Test
    void apiDocs_withToken_documentsTheAuthEndpoints() throws Exception {
        // Arrange
        var login = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin.test@valimbavaka.mg\",\"password\":\"mot-de-passe-de-test-tres-long\"}"))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(login, "$.accessToken");

        // Act + Assert
        mockMvc.perform(get("/v3/api-docs").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.responses['429']").exists());
    }
}
