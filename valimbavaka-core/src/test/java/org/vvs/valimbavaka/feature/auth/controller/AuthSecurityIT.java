package org.vvs.valimbavaka.feature.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.test.web.servlet.MvcResult;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.domain.SellerTerms;
import org.vvs.valimbavaka.feature.user.domain.User;
import org.vvs.valimbavaka.feature.user.domain.UserProfile;
import org.vvs.valimbavaka.feature.user.repository.RoleRepository;
import org.vvs.valimbavaka.feature.user.repository.UserRepository;
import org.vvs.valimbavaka.support.IntegrationTest;

import com.jayway.jsonpath.JsonPath;

/** Sécurité de bout en bout (AUTH-01) : le rôle vient du compte, les droits sont contrôlés côté serveur. */
@IntegrationTest
@AutoConfigureMockMvc
class AuthSecurityIT {

    private static final String ADMIN_EMAIL = "admin.test@valimbavaka.mg";
    private static final String ADMIN_PASSWORD = "mot-de-passe-de-test-tres-long";
    private static final String PASSWORD = "mot-de-passe-vendeuse-long";

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
    void login_adminCredentials_returnsAdminRoleWithItsPermissionsAndHttpOnlyRefreshCookie() throws Exception {
        // Act
        var result = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.permissions[?(@ == 'users.read')]").isNotEmpty())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
        assertThat(result.andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .contains("vvs_refresh=").contains("HttpOnly").contains("SameSite=Strict");
    }

    @Test
    void login_bodyCarryingAnotherRole_ignoresItAndKeepsTheRoleOfTheAccount() throws Exception {
        // Arrange
        var email = createDeliveryDriver();
        var body = "{\"email\":\"%s\",\"password\":\"%s\",\"role\":\"ADMIN\"}".formatted(email, PASSWORD);

        // Act + Assert
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("DELIVERY_DRIVER"))
                .andExpect(jsonPath("$.user.permissions[?(@ == 'users.read')]").isEmpty());
    }

    @Test
    void login_wrongPassword_returns401WithUniqueMessage() throws Exception {
        // Act + Assert
        login(ADMIN_EMAIL, "mauvais-mot-de-passe")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void login_afterFiveFailures_locksTheAccountEvenWithTheRightPassword() throws Exception {
        // Arrange
        var email = createDeliveryDriver();
        for (int attempt = 0; attempt < 5; attempt++) {
            login(email, "mauvais-mot-de-passe").andExpect(status().isUnauthorized());
        }

        // Act + Assert
        login(email, PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void getUsers_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void getUsers_withNonAdminRole_returns403() throws Exception {
        // Arrange
        var token = accessToken(createDeliveryDriver(), PASSWORD);

        // Act + Assert
        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getUsers_withAdminRole_returns200() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        // Act + Assert
        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void me_withSellerToken_returnsOnlyTheSellerPermissions() throws Exception {
        // Arrange
        var token = accessToken(createSeller(), PASSWORD);

        // Act + Assert
        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SELLER"))
                .andExpect(jsonPath("$.permissions[?(@ == 'sales.read')]").isNotEmpty())
                .andExpect(jsonPath("$.permissions[?(@ == 'deliveries.read')]").isEmpty())
                .andExpect(jsonPath("$.permissions[?(@ == 'users.read')]").isEmpty());
    }

    @Test
    void refresh_reusedCookie_isRejectedAndRevokesTheWholeSession() throws Exception {
        // Arrange
        var firstCookie = login(ADMIN_EMAIL, ADMIN_PASSWORD).andReturn().getResponse().getCookie("vvs_refresh");
        var rotated = mockMvc.perform(post("/api/v1/auth/refresh").cookie(firstCookie))
                .andExpect(status().isOk()).andReturn().getResponse().getCookie("vvs_refresh");

        // Act + Assert : l'ancien cookie est rejeté, puis le nouveau l'est aussi (famille révoquée)
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(firstCookie)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(rotated)).andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_afterLogout_returns401() throws Exception {
        // Arrange
        var cookie = login(ADMIN_EMAIL, ADMIN_PASSWORD).andReturn().getResponse().getCookie("vvs_refresh");
        mockMvc.perform(post("/api/v1/auth/logout").cookie(cookie)).andExpect(status().isNoContent());

        // Act + Assert
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(cookie)).andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_withoutCookie_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_fromForeignOrigin_returns403EvenWithAValidCookie() throws Exception {
        // Arrange
        var cookie = login(ADMIN_EMAIL, ADMIN_PASSWORD).andReturn().getResponse().getCookie("vvs_refresh");

        // Act + Assert
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(cookie).header(HttpHeaders.ORIGIN, "https://evil.example"))
                .andExpect(status().isForbidden());
    }

    @Test
    void refresh_fromAllowedOrigin_returns200() throws Exception {
        // Arrange
        var cookie = login(ADMIN_EMAIL, ADMIN_PASSWORD).andReturn().getResponse().getCookie("vvs_refresh");

        // Act + Assert
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(cookie).header(HttpHeaders.ORIGIN, "http://localhost:4200"))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        var body = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String accessToken(String email, String password) throws Exception {
        MvcResult result = login(email, password).andExpect(status().isOk()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private String createDeliveryDriver() {
        var email = "livreur-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(RoleName.DELIVERY_DRIVER).orElseThrow();
        userRepository.save(User.create(new UserProfile(email, "Tiana", "Ranaivoson"),
                passwordEncoder.encode(PASSWORD), role));
        return email;
    }

    private String createSeller() {
        var email = "vendeuse-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(RoleName.SELLER).orElseThrow();
        var channelId = jdbcTemplate.queryForObject("select id from sales_channels limit 1", UUID.class);
        userRepository.save(User.create(new UserProfile(email, "Hanta", "Rasolofoson"),
                passwordEncoder.encode(PASSWORD), role, new SellerTerms(channelId, new BigDecimal("5.00"))));
        return email;
    }
}
