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

/** FRS-01: the supplier sheet is admin-only, editable without deletion and traced in the journal. */
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
        MvcResult result = mockMvc.perform(postJson(SUPPLIERS, body(name))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
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
