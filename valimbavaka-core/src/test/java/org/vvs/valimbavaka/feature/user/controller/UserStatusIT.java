package org.vvs.valimbavaka.feature.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.domain.User;
import org.vvs.valimbavaka.feature.user.domain.UserProfile;
import org.vvs.valimbavaka.feature.user.repository.RoleRepository;
import org.vvs.valimbavaka.feature.user.repository.UserRepository;
import org.vvs.valimbavaka.support.IntegrationTest;

/**
 * AUTH-04 : activation/désactivation d'un compte, réservée à l'administrateur et tracée au journal. Le PUT applique
 * les mêmes garde-fous que le PATCH, et aucun compte ne se supprime.
 */
@IntegrationTest
@AutoConfigureMockMvc
class UserStatusIT {

    private static final String ADMIN_EMAIL = "admin.test@valimbavaka.mg";
    private static final String ADMIN_PASSWORD = "mot-de-passe-de-test-tres-long";
    private static final String PASSWORD = "mot-de-passe-livreur-long";
    private static final String DEACTIVATE = "{\"active\":false}";
    private static final String ACTIVATE = "{\"active\":true}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void setActive_withoutToken_returns401() throws Exception {
        mockMvc.perform(patchStatus(UUID.randomUUID(), DEACTIVATE)).andExpect(status().isUnauthorized());
    }

    @Test
    void setActive_withNonAdminRole_returns403() throws Exception {
        // Arrange
        var token = accessToken(createDriver(), PASSWORD);

        // Act + Assert
        mockMvc.perform(patchStatus(UUID.randomUUID(), DEACTIVATE).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void setActive_byAdmin_togglesStatusAndTracesInJournal() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var email = createDriver();
        var userId = idOf(email);

        // Act + Assert
        mockMvc.perform(patchStatus(userId, DEACTIVATE).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(get("/api/v1/journal").param("search", email).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].description").value("Désactivation de l'utilisateur " + email));
        mockMvc.perform(patchStatus(userId, ACTIVATE).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void setActive_deactivatedUser_cannotLogInAnymore() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var email = createDriver();
        mockMvc.perform(patchStatus(idOf(email), DEACTIVATE).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        // Act + Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void setActive_ownAccount_returns422() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        // Act + Assert
        mockMvc.perform(patchStatus(idOf(ADMIN_EMAIL), DEACTIVATE).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void setActive_missingStatus_returns422() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        // Act + Assert
        mockMvc.perform(patchStatus(UUID.randomUUID(), "{}").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void setActive_massAssignmentOfRole_isIgnored() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var userId = idOf(createDriver());
        var adminRoleId =
                roleRepository.findByName(RoleName.ADMIN).orElseThrow().getId();
        var body = "{\"active\":false,\"roleId\":\"%s\"}".formatted(adminRoleId);

        // Act
        mockMvc.perform(patchStatus(userId, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        // Assert
        var reloaded = userRepository.findByIdWithRole(userId).orElseThrow();
        assertThat(reloaded.getRole().getName()).isEqualTo(RoleName.DELIVERY_DRIVER);
    }

    @Test
    void updateUser_ownAccountDeactivated_returns422AndKeepsTheAccountActive() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var adminId = idOf(ADMIN_EMAIL);
        var body = adminUpdateBody(RoleName.ADMIN, false);

        // Act + Assert
        mockMvc.perform(putUser(adminId, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        assertThat(userRepository.findByIdWithRole(adminId).orElseThrow().isActive())
                .isTrue();
    }

    @Test
    void updateUser_ownAdministratorRoleRemoved_returns422AndKeepsTheRole() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var adminId = idOf(ADMIN_EMAIL);
        var body = adminUpdateBody(RoleName.DELIVERY_DRIVER, true);

        // Act + Assert
        mockMvc.perform(putUser(adminId, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        assertThat(userRepository
                        .findByIdWithRole(adminId)
                        .orElseThrow()
                        .getRole()
                        .getName())
                .isEqualTo(RoleName.ADMIN);
    }

    @Test
    void deleteUser_isNotExposed() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var userId = idOf(createDriver());

        // Act + Assert
        mockMvc.perform(delete("/api/v1/users/" + userId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isMethodNotAllowed());
        assertThat(userRepository.findById(userId)).isPresent();
    }

    private String adminUpdateBody(RoleName role, boolean active) {
        var roleId = roleRepository.findByName(role).orElseThrow().getId();
        return ("{\"email\":\"%s\",\"firstName\":\"Rasoa\",\"lastName\":\"Rakoto\","
                        + "\"roleId\":\"%s\",\"active\":%s}")
                .formatted(ADMIN_EMAIL, roleId, active);
    }

    private static MockHttpServletRequestBuilder putUser(UUID userId, String body) {
        return put("/api/v1/users/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private UUID idOf(String email) {
        return userRepository
                .findByEmailWithRoleAndPermissions(email)
                .orElseThrow()
                .getId();
    }

    private static MockHttpServletRequestBuilder patchStatus(UUID userId, String body) {
        return patch("/api/v1/users/" + userId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private String accessToken(String email, String password) throws Exception {
        var body = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private String createDriver() {
        var email = "livreur-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(RoleName.DELIVERY_DRIVER).orElseThrow();
        userRepository.save(
                User.create(new UserProfile(email, "Tiana", "Ranaivoson"), passwordEncoder.encode(PASSWORD), role));
        return email;
    }
}
