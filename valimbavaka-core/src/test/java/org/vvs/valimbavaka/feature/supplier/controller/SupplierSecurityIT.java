package org.vvs.valimbavaka.feature.supplier.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

import org.vvs.valimbavaka.feature.supplier.repository.SupplierRepository;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.domain.User;
import org.vvs.valimbavaka.feature.user.domain.UserProfile;
import org.vvs.valimbavaka.feature.user.repository.RoleRepository;
import org.vvs.valimbavaka.feature.user.repository.UserRepository;
import org.vvs.valimbavaka.support.IntegrationTest;

/**
 * FRS-01 / FRS-02: the supplier sheet is admin-only, created and updated without deletion and traced in the journal.
 */
@IntegrationTest
@AutoConfigureMockMvc
class SupplierSecurityIT {

    private static final String SUPPLIERS = "/api/v1/suppliers";
    private static final String ADMIN_EMAIL = "admin.test@valimbavaka.mg";
    private static final String ADMIN_PASSWORD = "mot-de-passe-de-test-tres-long";
    private static final String PASSWORD = "mot-de-passe-livreur-long";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void getSuppliers_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(SUPPLIERS)).andExpect(status().isUnauthorized());
    }

    @Test
    void createSupplier_withNonAdminRole_returns403() throws Exception {
        // Arrange
        var token = accessToken(createDeliveryDriver(), PASSWORD);

        // Act + Assert
        mockMvc.perform(postJson(SUPPLIERS, body(uniqueName())).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getSuppliers_withNonAdminRole_returns403() throws Exception {
        // Arrange
        var token = accessToken(createDeliveryDriver(), PASSWORD);

        // Act + Assert
        mockMvc.perform(get(SUPPLIERS).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createSupplier_byAdmin_returns201AndTracesInJournal() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var name = uniqueName();

        // Act + Assert
        mockMvc.perform(postJson(SUPPLIERS, body(name)).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.type").value("LOCAL"))
                .andExpect(jsonPath("$.active").value(true));
        mockMvc.perform(get("/api/v1/journal").param("search", name).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].action").value("CREATE"))
                .andExpect(jsonPath("$.items[0].resourceType").value("SUPPLIER"));
    }

    @Test
    void createSupplier_nameAlreadyUsedWithOtherCase_returns409() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var name = uniqueName();
        createSupplier(token, name);

        // Act + Assert
        mockMvc.perform(postJson(SUPPLIERS, body(name.toUpperCase())).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void createSupplier_missingNameAndType_returns422() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        // Act + Assert
        mockMvc.perform(postJson(SUPPLIERS, "{\"email\":\"not-an-email\"}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createSupplier_massAssignmentOfIdAndStatus_isIgnored() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var forcedId = UUID.randomUUID();
        var body = """
                {"id":"%s","name":"%s","type":"LOCAL","active":false}
                """.formatted(forcedId, uniqueName());

        // Act
        var result = mockMvc.perform(postJson(SUPPLIERS, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andReturn();

        // Assert
        var id = UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
        assertThat(id).isNotEqualTo(forcedId);
        assertThat(supplierRepository.findById(id).orElseThrow().isActive()).isTrue();
    }

    @Test
    void updateSupplier_byAdmin_changesEveryField() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var id = createSupplier(token, uniqueName());
        var name = uniqueName();
        var body = """
                {"name":"%s","type":"FOREIGN","contactName":"Mr Lee","phone":"+33 1 00 00 00 00",
                 "email":"lee@ocean.com","address":"Marseille","notes":"Raw materials"}
                """.formatted(name);

        // Act + Assert
        mockMvc.perform(put(SUPPLIERS + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.type").value("FOREIGN"))
                .andExpect(jsonPath("$.contactName").value("Mr Lee"))
                .andExpect(jsonPath("$.phone").value("+33 1 00 00 00 00"))
                .andExpect(jsonPath("$.email").value("lee@ocean.com"))
                .andExpect(jsonPath("$.address").value("Marseille"))
                .andExpect(jsonPath("$.notes").value("Raw materials"));
    }

    @Test
    void updateSupplier_staleVersion_returns409AndKeepsTheSheet() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var name = uniqueName();
        var id = createSupplier(token, name);
        var body = """
                {"name":"%s","type":"FOREIGN","version":42}
                """.formatted(uniqueName());

        // Act + Assert
        mockMvc.perform(put(SUPPLIERS + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
        assertThat(supplierRepository.findById(id).orElseThrow().getName()).isEqualTo(name);
    }

    @Test
    void updateSupplier_withoutToken_returns401() throws Exception {
        mockMvc.perform(putJson(SUPPLIERS + "/" + UUID.randomUUID(), body(uniqueName())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void updateSupplier_withNonAdminRole_returns403AndKeepsTheSheet() throws Exception {
        // Arrange
        var adminToken = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var name = uniqueName();
        var id = createSupplier(adminToken, name);
        var token = accessToken(createDeliveryDriver(), PASSWORD);

        // Act + Assert
        mockMvc.perform(putJson(SUPPLIERS + "/" + id, body(uniqueName()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        assertThat(supplierRepository.findById(id).orElseThrow().getName()).isEqualTo(name);
    }

    @Test
    void updateSupplier_unknownId_returns404() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        // Act + Assert
        mockMvc.perform(putJson(SUPPLIERS + "/" + UUID.randomUUID(), body(uniqueName()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void updateSupplier_tooLongNameAndInvalidEmail_returns422AndKeepsTheSheet() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var name = uniqueName();
        var id = createSupplier(token, name);
        var body = """
                {"name":"%s","type":"LOCAL","email":"abc"}
                """.formatted("a".repeat(151));

        // Act + Assert
        mockMvc.perform(putJson(SUPPLIERS + "/" + id, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        assertThat(supplierRepository.findById(id).orElseThrow().getName()).isEqualTo(name);
    }

    @Test
    void updateSupplier_nameOfAnotherSupplierWithOtherCase_returns409() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var taken = uniqueName();
        createSupplier(token, taken);
        var id = createSupplier(token, uniqueName());

        // Act + Assert
        mockMvc.perform(putJson(SUPPLIERS + "/" + id, body(taken.toUpperCase()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.detail").value("Un fournisseur avec ce nom existe déjà."));
    }

    @Test
    void updateSupplier_massAssignmentOfIdAndStatus_isIgnored() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var id = createSupplier(token, uniqueName());
        var body = """
                {"id":"%s","name":"%s","type":"LOCAL","active":false}
                """.formatted(UUID.randomUUID(), uniqueName());

        // Act
        mockMvc.perform(putJson(SUPPLIERS + "/" + id, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.active").value(true));

        // Assert
        assertThat(supplierRepository.findById(id).orElseThrow().isActive()).isTrue();
    }

    @Test
    void updateSupplier_withVersionRead_savesAndTracesUpdateInJournal() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var name = uniqueName();
        var id = createSupplier(token, name);
        long version = supplierRepository.findById(id).orElseThrow().getVersion();
        var body = """
                {"name":"%s","type":"LOCAL","notes":"Livraison le mardi","version":%d}
                """.formatted(name, version);

        // Act
        mockMvc.perform(putJson(SUPPLIERS + "/" + id, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Livraison le mardi"));

        // Assert
        mockMvc.perform(get("/api/v1/journal").param("search", name).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].action").value("UPDATE"))
                .andExpect(jsonPath("$.items[0].resourceId").value(id.toString()))
                .andExpect(jsonPath("$.items[0].description").value("Modification du fournisseur " + name));
    }

    @Test
    void updateSupplier_withoutChange_leavesNoNewJournalLine() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var name = uniqueName();
        var id = createSupplier(token, name);
        var body = """
                {"name":"  %s ","type":"LOCAL","contactName":" ","email":""}
                """.formatted(name);

        // Act
        mockMvc.perform(putJson(SUPPLIERS + "/" + id, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        // Assert
        mockMvc.perform(get("/api/v1/journal").param("search", name).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].action").value("CREATE"));
    }

    @Test
    void setActive_byAdmin_deactivatesSupplier() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var id = createSupplier(token, uniqueName());

        // Act + Assert
        mockMvc.perform(patch(SUPPLIERS + "/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void getSupplierById_unknownId_returns404() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        // Act + Assert
        mockMvc.perform(get(SUPPLIERS + "/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void createSupplier_byAdmin_fillsTheAuditAuthorWithHisAccountId() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var adminId = userRepository
                .findByEmailWithRoleAndPermissions(ADMIN_EMAIL)
                .orElseThrow()
                .getId();

        // Act
        var id = createSupplier(token, uniqueName());

        // Assert
        var saved = supplierRepository.findById(id).orElseThrow();
        assertThat(saved.getCreatedBy()).isEqualTo(adminId);
        assertThat(saved.getUpdatedBy()).isEqualTo(adminId);
    }

    @Test
    void deleteSupplier_isNotExposed() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var id = createSupplier(token, uniqueName());

        // Act + Assert
        mockMvc.perform(delete(SUPPLIERS + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isMethodNotAllowed());
        assertThat(supplierRepository.findById(id)).isPresent();
    }

    private UUID createSupplier(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(
                        postJson(SUPPLIERS, body(name)).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    private static String uniqueName() {
        return "Fournisseur-" + UUID.randomUUID();
    }

    private static String body(String name) {
        return "{\"name\":\"%s\",\"type\":\"LOCAL\"}".formatted(name);
    }

    private static MockHttpServletRequestBuilder postJson(String url, String body) {
        return post(url).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static MockHttpServletRequestBuilder putJson(String url, String body) {
        return put(url).contentType(MediaType.APPLICATION_JSON).content(body);
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
