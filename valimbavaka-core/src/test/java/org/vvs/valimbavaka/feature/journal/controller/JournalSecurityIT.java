package org.vvs.valimbavaka.feature.journal.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.domain.User;
import org.vvs.valimbavaka.feature.user.domain.UserProfile;
import org.vvs.valimbavaka.feature.user.repository.RoleRepository;
import org.vvs.valimbavaka.feature.user.repository.UserRepository;
import org.vvs.valimbavaka.support.IntegrationTest;

/** JNL-01 : l'administrateur seul consulte le journal, et chaque opération y laisse une ligne nominative. */
@IntegrationTest
@AutoConfigureMockMvc
class JournalSecurityIT {

    private static final String ADMIN_EMAIL = "admin.test@valimbavaka.mg";
    private static final String ADMIN_PASSWORD = "mot-de-passe-de-test-tres-long";
    private static final String ADMIN_NAME = "Rasoa Rakoto";
    private static final String PASSWORD = "mot-de-passe-livreur-long";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void getEntries_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/journal")).andExpect(status().isUnauthorized());
    }

    @Test
    void getEntries_withNonAdminRole_returns403() throws Exception {
        // Arrange
        var token = accessToken(createDeliveryDriver(), PASSWORD);

        // Act + Assert
        mockMvc.perform(get("/api/v1/journal").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getEntries_withTodayUtcDate_listsTodaysOperationsOldestFirst() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var today = LocalDate.now(ZoneOffset.UTC).toString();

        // Act + Assert
        mockMvc.perform(get("/api/v1/journal").param("date", today).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void getEntries_withDateInThePast_returnsNoTodayEntry() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        // Act + Assert
        mockMvc.perform(get("/api/v1/journal")
                        .param("date", "2000-01-01")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void getEntries_withMalformedDate_returns400() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        // Act + Assert
        mockMvc.perform(get("/api/v1/journal").param("date", "29/10/2026").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void getEntries_withDateAndNonAdminRole_returns403() throws Exception {
        // Arrange
        var token = accessToken(createDeliveryDriver(), PASSWORD);

        // Act + Assert
        mockMvc.perform(get("/api/v1/journal").param("date", "2026-10-29").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUser_byAdmin_leavesANominativeTimestampedLineInTheJournal() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var email = "nouveau-" + UUID.randomUUID() + "@valimbavaka.mg";
        var roleId = roleRepository
                .findByName(RoleName.DELIVERY_DRIVER)
                .orElseThrow()
                .getId();
        var body = """
                {"email":"%s","password":"%s","firstName":"Tiana","lastName":"Rabe","roleId":"%s"}
                """.formatted(email, PASSWORD, roleId);

        // Act
        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // Assert
        mockMvc.perform(get("/api/v1/journal").param("search", email).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].actorName").value(ADMIN_NAME))
                .andExpect(jsonPath("$.items[0].action").value("CREATE"))
                .andExpect(jsonPath("$.items[0].resourceType").value("USER"))
                .andExpect(jsonPath("$.items[0].occurredAt").isNotEmpty());
    }

    @Test
    void journal_writeVerbs_areNotExposed() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        // Act + Assert
        mockMvc.perform(post("/api/v1/journal")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/v1/journal/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().is4xxClientError());
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

    private String createDeliveryDriver() {
        var email = "livreur-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(RoleName.DELIVERY_DRIVER).orElseThrow();
        userRepository.save(
                User.create(new UserProfile(email, "Tiana", "Ranaivoson"), passwordEncoder.encode(PASSWORD), role));
        return email;
    }
}
