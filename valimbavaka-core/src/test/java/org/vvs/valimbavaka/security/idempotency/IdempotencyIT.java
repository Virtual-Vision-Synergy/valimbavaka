package org.vvs.valimbavaka.security.idempotency;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.repository.RoleRepository;
import org.vvs.valimbavaka.support.IntegrationTest;

/** SYS-02 : un rejeu d'écriture avec la même clé d'idempotence ne crée aucun doublon. */
@IntegrationTest
@AutoConfigureMockMvc
class IdempotencyIT {

    private static final String ADMIN_EMAIL = "admin.test@valimbavaka.mg";
    private static final String ADMIN_PASSWORD = "mot-de-passe-de-test-tres-long";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void createUser_replayedWithSameKey_returnsSameResponseWithoutDuplicate() throws Exception {
        // Arrange
        var token = adminToken();
        var key = UUID.randomUUID().toString();
        var body = newUserBody("replay-" + UUID.randomUUID() + "@valimbavaka.mg");
        var first = mockMvc.perform(createUser(body, token, key))
                .andExpect(status().isCreated())
                .andReturn();

        // Act + Assert : sans idempotence, le 2e appel serait un 409 (email déjà utilisé)
        mockMvc.perform(createUser(body, token, key))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andExpect(jsonPath("$.id")
                        .value(JsonPath.<String>read(first.getResponse().getContentAsString(), "$.id")));
    }

    @Test
    void createUser_sameBodyWithoutKey_isNotDeduplicated() throws Exception {
        // Arrange
        var token = adminToken();
        var body = newUserBody("sans-cle-" + UUID.randomUUID() + "@valimbavaka.mg");
        mockMvc.perform(createUser(body, token, null)).andExpect(status().isCreated());

        // Act + Assert
        mockMvc.perform(createUser(body, token, null)).andExpect(status().isConflict());
    }

    @Test
    void createUser_keyReusedForAnotherRequest_returns422() throws Exception {
        // Arrange
        var token = adminToken();
        var key = UUID.randomUUID().toString();
        mockMvc.perform(createUser(newUserBody("a-" + UUID.randomUUID() + "@valimbavaka.mg"), token, key))
                .andExpect(status().isCreated());

        // Act + Assert
        mockMvc.perform(patch("/api/v1/users/" + UUID.randomUUID() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}")
                        .header("Idempotency-Key", key)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createUser_invalidKey_returns400() throws Exception {
        mockMvc.perform(createUser("{}", adminToken(), "pas-un-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void createUser_keyWithoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpServletRequestBuilder createUser(String body, String token, String key) {
        var request = post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return key == null ? request : request.header("Idempotency-Key", key);
    }

    private String newUserBody(String email) {
        var role = roleRepository.findByName(RoleName.DELIVERY_DRIVER).orElseThrow();
        return ("{\"email\":\"%s\",\"password\":\"mot-de-passe-livreur-long\",\"firstName\":\"Tiana\","
                        + "\"lastName\":\"Rakoto\",\"roleId\":\"%s\"}")
                .formatted(email, role.getId());
    }

    private String adminToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }
}
