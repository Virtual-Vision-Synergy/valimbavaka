package org.vvs.valimbavaka.feature.article.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.article.repository.ArticleRepository;
import org.vvs.valimbavaka.feature.referential.repository.ProductRangeRepository;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.domain.User;
import org.vvs.valimbavaka.feature.user.domain.UserProfile;
import org.vvs.valimbavaka.feature.user.repository.RoleRepository;
import org.vvs.valimbavaka.feature.user.repository.UserRepository;
import org.vvs.valimbavaka.support.IntegrationTest;

/** Shared fixtures and HTTP helpers of the ART-01 integration tests (one class per concern, 300 lines max). */
@IntegrationTest
@AutoConfigureMockMvc
abstract class ArticleApiTestSupport {

    protected static final String ARTICLES = "/api/v1/articles";

    protected static final String ADMIN_EMAIL = "admin.test@valimbavaka.mg";

    protected static final String ADMIN_PASSWORD = "mot-de-passe-de-test-tres-long";

    protected static final String PASSWORD = "mot-de-passe-livreur-long";

    @Autowired

    protected MockMvc mockMvc;

    @Autowired

    protected ArticleRepository articleRepository;

    @Autowired

    protected ProductRangeRepository productRangeRepository;

    @Autowired

    protected UserRepository userRepository;

    @Autowired

    protected RoleRepository roleRepository;

    @Autowired

    protected PasswordEncoder passwordEncoder;

    protected UUID createArticle(String token, String body) throws Exception {
        MvcResult result = mockMvc.perform(postJson(ARTICLES, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    protected UUID createSupplier(String token) throws Exception {
        var body = "{\"name\":\"Fournisseur-%s\",\"type\":\"LOCAL\"}".formatted(UUID.randomUUID());
        MvcResult result = mockMvc.perform(
                        postJson("/api/v1/suppliers", body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    protected UUID rangeId() {
        return productRangeRepository.findAll().getFirst().getId();
    }

    protected String finishedBody(String name) {
        return """
                {"name":"%s","type":"FINISHED","unit":"PIECE","rangeId":"%s","cost":3120.5,"price":8500,
                 "threshold":40}
                """.formatted(name, rangeId());
    }

    protected static String rawMaterialBody(String name, String barcode) {
        var barcodePart = barcode == null ? "" : ",\"barcode\":\"%s\"".formatted(barcode);
        return "{\"name\":\"%s\",\"type\":\"RAW_MATERIAL\",\"unit\":\"KILOGRAM\",\"threshold\":5%s}"
                .formatted(name, barcodePart);
    }

    protected static String uniqueName() {
        return "Article-" + UUID.randomUUID();
    }

    protected static String uniqueBarcode() {
        return String.format("%013d", Math.abs(UUID.randomUUID().getMostSignificantBits()) % 10_000_000_000_000L);
    }

    protected static MockHttpServletRequestBuilder postJson(String url, String body) {
        return post(url).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    protected static MockHttpServletRequestBuilder putJson(String url, String body) {
        return put(url).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    protected String adminToken() throws Exception {
        return accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    protected String accessToken(String email, String password) throws Exception {
        var body = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    protected String createUser(RoleName roleName) {
        var email = "user-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(roleName).orElseThrow();
        userRepository.save(
                User.create(new UserProfile(email, "Tiana", "Ranaivoson"), passwordEncoder.encode(PASSWORD), role));
        return email;
    }
}
