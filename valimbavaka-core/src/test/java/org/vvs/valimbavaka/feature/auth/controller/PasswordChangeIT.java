package org.vvs.valimbavaka.feature.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.domain.SellerTerms;
import org.vvs.valimbavaka.feature.user.domain.User;
import org.vvs.valimbavaka.feature.user.domain.UserProfile;
import org.vvs.valimbavaka.feature.user.repository.RoleRepository;
import org.vvs.valimbavaka.feature.user.repository.UserRepository;
import org.vvs.valimbavaka.support.IntegrationTest;

/** AUTH-01 / SYS-05 : un compte au mot de passe provisoire n'a aucun droit tant qu'il ne l'a pas changé. */
@IntegrationTest
@AutoConfigureMockMvc
class PasswordChangeIT {

    private static final String TEMPORARY_PASSWORD = "mot-de-passe-provisoire-long";
    private static final String NEW_PASSWORD = "nouveau-mot-de-passe-choisi";
    private static final String SALES_CHANNELS = "/api/v1/sales-channels";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void login_temporaryPassword_grantsNoPermissionUntilItIsChanged() throws Exception {
        // Arrange
        var email = createSellerWithTemporaryPassword();

        // Act
        var token = accessToken(login(email, TEMPORARY_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.passwordChangeRequired").value(true))
                .andExpect(jsonPath("$.user.permissions").isEmpty()));

        // Assert
        mockMvc.perform(get(SALES_CHANNELS).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void changePassword_validRequest_liftsTheRequirementAndRestoresThePermissions() throws Exception {
        // Arrange
        var email = createSellerWithTemporaryPassword();
        var token = accessToken(login(email, TEMPORARY_PASSWORD));

        // Act
        mockMvc.perform(changePassword(token, TEMPORARY_PASSWORD, NEW_PASSWORD)).andExpect(status().isNoContent());

        // Assert
        login(email, TEMPORARY_PASSWORD).andExpect(status().isUnauthorized());
        var newToken = accessToken(login(email, NEW_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.passwordChangeRequired").value(false))
                .andExpect(jsonPath("$.user.permissions[?(@ == 'referentials.read')]")
                        .isNotEmpty()));
        mockMvc.perform(get(SALES_CHANNELS).header(HttpHeaders.AUTHORIZATION, bearer(newToken)))
                .andExpect(status().isOk());
    }

    @Test
    void changePassword_wrongCurrentPassword_returns422() throws Exception {
        // Arrange
        var email = createSellerWithTemporaryPassword();
        var token = accessToken(login(email, TEMPORARY_PASSWORD));

        // Act + Assert
        mockMvc.perform(changePassword(token, "pas-le-bon-mot-de-passe", NEW_PASSWORD))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void changePassword_newPasswordShorterThan15_returns422() throws Exception {
        // Arrange
        var email = createSellerWithTemporaryPassword();
        var token = accessToken(login(email, TEMPORARY_PASSWORD));

        // Act + Assert
        mockMvc.perform(changePassword(token, TEMPORARY_PASSWORD, "court-14-carac"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void changePassword_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(TEMPORARY_PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    private String createSellerWithTemporaryPassword() {
        var email = "vendeuse-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(RoleName.SELLER).orElseThrow();
        var channelId = jdbcTemplate.queryForObject("select id from sales_channels limit 1", UUID.class);
        userRepository.save(User.create(
                new UserProfile(email, "Hanta", "Rasolofoson"),
                passwordEncoder.encode(TEMPORARY_PASSWORD),
                role,
                new SellerTerms(channelId, new BigDecimal("5.00"))));
        jdbcTemplate.update("UPDATE users SET is_password_change_required = TRUE WHERE email = ?", email);
        return email;
    }

    private ResultActions login(String email, String password) throws Exception {
        var body = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String accessToken(ResultActions login) throws Exception {
        return JsonPath.read(login.andReturn().getResponse().getContentAsString(), "$.accessToken");
    }

    private static MockHttpServletRequestBuilder changePassword(
            String token, String currentPassword, String newPassword) {
        return post("/api/v1/auth/password")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(currentPassword, newPassword));
    }

    private static String body(String currentPassword, String newPassword) {
        return "{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}".formatted(currentPassword, newPassword);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
