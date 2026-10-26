package org.vvs.valimbavaka.feature.article.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
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

import org.vvs.valimbavaka.feature.article.repository.ArticleRepository;
import org.vvs.valimbavaka.feature.referential.repository.ProductRangeRepository;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.domain.User;
import org.vvs.valimbavaka.feature.user.domain.UserProfile;
import org.vvs.valimbavaka.feature.user.repository.RoleRepository;
import org.vvs.valimbavaka.feature.user.repository.UserRepository;
import org.vvs.valimbavaka.support.IntegrationTest;

/**
 * ART-01: the article sheet is readable by the roles holding articles.read, written by the administrator only,
 * never deleted, and every write is traced in the journal.
 */
@IntegrationTest
@AutoConfigureMockMvc
class ArticleSecurityIT {

    private static final String ARTICLES = "/api/v1/articles";
    private static final String ADMIN_EMAIL = "admin.test@valimbavaka.mg";
    private static final String ADMIN_PASSWORD = "mot-de-passe-de-test-tres-long";
    private static final String PASSWORD = "mot-de-passe-livreur-long";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private ProductRangeRepository productRangeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void getArticles_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(ARTICLES))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getArticles_withDeliveryDriverRole_returns403() throws Exception {
        // Arrange
        var token = accessToken(createUser(RoleName.DELIVERY_DRIVER), PASSWORD);

        // Act + Assert
        mockMvc.perform(get(ARTICLES).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getArticles_withSellerRole_returns200() throws Exception {
        // Arrange
        var token = accessToken(createUser(RoleName.SELLER), PASSWORD);

        // Act + Assert
        mockMvc.perform(get(ARTICLES).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void createArticle_withSellerRole_returns403() throws Exception {
        // Arrange
        var token = accessToken(createUser(RoleName.SELLER), PASSWORD);

        // Act + Assert
        mockMvc.perform(postJson(ARTICLES, finishedBody(uniqueName())).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void createArticle_withoutToken_returns401() throws Exception {
        mockMvc.perform(postJson(ARTICLES, finishedBody(uniqueName()))).andExpect(status().isUnauthorized());
    }

    @Test
    void createArticle_finishedProductByAdmin_returns201WithCodeAndTracesInJournal() throws Exception {
        // Arrange
        var token = adminToken();
        var name = uniqueName();

        // Act + Assert
        mockMvc.perform(postJson(ARTICLES, finishedBody(name)).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.code").value(matchesPattern("ART-\\d{4,}")))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.type").value("FINISHED"))
                .andExpect(jsonPath("$.rangeId").value(rangeId().toString()))
                .andExpect(jsonPath("$.unit").value("PIECE"))
                .andExpect(jsonPath("$.cost").value(3120.5))
                .andExpect(jsonPath("$.price").value(8500))
                .andExpect(jsonPath("$.threshold").value(40))
                .andExpect(jsonPath("$.active").value(true));
        mockMvc.perform(get("/api/v1/journal").param("search", name).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.items[0].action").value("CREATE"))
                .andExpect(jsonPath("$.items[0].resourceType").value("ARTICLE"));
    }

    @Test
    void createArticle_rawMaterialAndSemiFinished_areRecordedWithoutRangeNorPrice() throws Exception {
        // Arrange
        var token = adminToken();

        // Act + Assert
        for (var type : new String[] {"RAW_MATERIAL", "SEMI_FINISHED"}) {
            var body = """
                    {"name":"%s","type":"%s","unit":"KILOGRAM","threshold":5}
                    """.formatted(uniqueName(), type);
            mockMvc.perform(postJson(ARTICLES, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.type").value(type))
                    .andExpect(jsonPath("$.rangeId").doesNotExist())
                    .andExpect(jsonPath("$.price").doesNotExist());
        }
    }

    @Test
    void createArticle_rawMaterialWithRangeAndPrice_dropsThem() throws Exception {
        // Arrange
        var body = """
                {"name":"%s","type":"RAW_MATERIAL","unit":"KILOGRAM","threshold":5,
                 "rangeId":"%s","price":15000,"cost":12000}
                """.formatted(uniqueName(), rangeId());

        // Act + Assert
        mockMvc.perform(postJson(ARTICLES, body).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rangeId").doesNotExist())
                .andExpect(jsonPath("$.price").doesNotExist())
                .andExpect(jsonPath("$.cost").value(12000));
    }

    @Test
    void createArticle_massAssignmentOfIdCodeAndStatus_isIgnored() throws Exception {
        // Arrange
        var forcedId = UUID.randomUUID();
        var body = """
                {"id":"%s","code":"HACK-1","active":false,"name":"%s","type":"RAW_MATERIAL",
                 "unit":"PIECE","threshold":0}
                """.formatted(forcedId, uniqueName());

        // Act
        var result = mockMvc.perform(postJson(ARTICLES, body).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isCreated())
                .andReturn();

        // Assert
        var id = UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
        var saved = articleRepository.findById(id).orElseThrow();
        assertThat(id).isNotEqualTo(forcedId);
        assertThat(saved.getCode()).startsWith("ART-");
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void createArticle_emptyBody_returns422() throws Exception {
        mockMvc.perform(postJson(ARTICLES, "{}").header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createArticle_invalidValues_returns422() throws Exception {
        // Arrange
        var token = adminToken();
        var invalidBodies = new String[] {
            "{\"name\":\"" + "a".repeat(121) + "\",\"type\":\"RAW_MATERIAL\",\"unit\":\"PIECE\",\"threshold\":0}",
            "{\"name\":\"   \",\"type\":\"RAW_MATERIAL\",\"unit\":\"PIECE\",\"threshold\":0}",
            "{\"name\":\"X\",\"type\":\"RAW_MATERIAL\",\"unit\":\"PIECE\",\"threshold\":-1}",
            "{\"name\":\"X\",\"type\":\"RAW_MATERIAL\",\"unit\":\"PIECE\",\"threshold\":0,\"cost\":-0.01}",
            "{\"name\":\"X\",\"type\":\"RAW_MATERIAL\",\"unit\":\"PIECE\",\"threshold\":0,\"cost\":10.123}",
            "{\"name\":\"X\",\"type\":\"RAW_MATERIAL\",\"unit\":\"PIECE\",\"threshold\":0,\"barcode\":\"12345\"}",
            "{\"name\":\"X\",\"type\":\"RAW_MATERIAL\",\"unit\":\"PIECE\",\"threshold\":0,\"barcode\":\"abcdefghijklm\"}"
        };

        // Act + Assert
        for (var body : invalidBodies) {
            mockMvc.perform(postJson(ARTICLES, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
    }

    @Test
    void createArticle_boundaryValues_areAccepted() throws Exception {
        // Arrange
        var name = "a".repeat(120);
        var body = """
                {"name":"%s","type":"RAW_MATERIAL","unit":"LITER","threshold":0,"cost":0,
                 "barcode":"%s"}
                """.formatted(name, uniqueBarcode());

        // Act + Assert
        mockMvc.perform(postJson(ARTICLES, body).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cost").value(0))
                .andExpect(jsonPath("$.threshold").value(0));
    }

    @Test
    void createArticle_unknownTypeOrUnit_returns400() throws Exception {
        // Arrange
        var body = "{\"name\":\"X\",\"type\":\"PACKAGING\",\"unit\":\"PIECE\",\"threshold\":0}";

        // Act + Assert
        mockMvc.perform(postJson(ARTICLES, body).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void createArticle_finishedProductWithoutRangeOrPrice_returns422() throws Exception {
        // Arrange
        var token = adminToken();
        var withoutRange = "{\"name\":\"X\",\"type\":\"FINISHED\",\"unit\":\"PIECE\",\"threshold\":0,\"price\":8500}";
        var withoutPrice = """
                {"name":"X","type":"FINISHED","unit":"PIECE","threshold":0,"rangeId":"%s"}
                """.formatted(rangeId());

        // Act + Assert
        mockMvc.perform(postJson(ARTICLES, withoutRange).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("La gamme est obligatoire pour un produit fini."));
        mockMvc.perform(postJson(ARTICLES, withoutPrice).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Le prix est obligatoire pour un produit fini."));
    }

    @Test
    void createArticle_unknownRangeOrSupplier_returns422() throws Exception {
        // Arrange
        var token = adminToken();
        var unknownRange = """
                {"name":"X","type":"FINISHED","unit":"PIECE","threshold":0,"price":8500,"rangeId":"%s"}
                """.formatted(UUID.randomUUID());
        var unknownSupplier = """
                {"name":"X","type":"RAW_MATERIAL","unit":"PIECE","threshold":0,"supplierId":"%s"}
                """.formatted(UUID.randomUUID());

        // Act + Assert
        mockMvc.perform(postJson(ARTICLES, unknownRange).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent());
        mockMvc.perform(postJson(ARTICLES, unknownSupplier).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void createArticle_withActiveSupplier_linksTheArticleToIt() throws Exception {
        // Arrange
        var token = adminToken();
        var supplierId = createSupplier(token);
        var body = """
                {"name":"%s","type":"RAW_MATERIAL","unit":"KILOGRAM","threshold":5,"supplierId":"%s"}
                """.formatted(uniqueName(), supplierId);

        // Act + Assert
        mockMvc.perform(postJson(ARTICLES, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.supplierId").value(supplierId.toString()));
        mockMvc.perform(get(ARTICLES).param("supplierId", supplierId.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    void createArticle_deactivatedSupplier_returns422() throws Exception {
        // Arrange
        var token = adminToken();
        var supplierId = createSupplier(token);
        mockMvc.perform(patch("/api/v1/suppliers/" + supplierId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
        var body = """
                {"name":"%s","type":"RAW_MATERIAL","unit":"PIECE","threshold":0,"supplierId":"%s"}
                """.formatted(uniqueName(), supplierId);

        // Act + Assert
        mockMvc.perform(postJson(ARTICLES, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void createArticle_barcodeAlreadyUsed_returns409() throws Exception {
        // Arrange
        var token = adminToken();
        var barcode = uniqueBarcode();
        createArticle(token, rawMaterialBody(uniqueName(), barcode));

        // Act + Assert
        mockMvc.perform(postJson(ARTICLES, rawMaterialBody(uniqueName(), barcode))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void createArticle_sameNameTwice_isAcceptedWithTwoDifferentCodes() throws Exception {
        // Arrange
        var token = adminToken();
        var name = uniqueName();

        // Act
        var first = createArticle(token, rawMaterialBody(name, null));
        var second = createArticle(token, rawMaterialBody(name, null));

        // Assert
        assertThat(articleRepository.findById(first).orElseThrow().getCode())
                .isNotEqualTo(articleRepository.findById(second).orElseThrow().getCode());
    }

    @Test
    void getArticleById_existingArticle_returnsTheWholeSheet() throws Exception {
        // Arrange
        var token = adminToken();
        var name = uniqueName();
        var id = createArticle(token, finishedBody(name));

        // Act + Assert
        mockMvc.perform(get(ARTICLES + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.type").value("FINISHED"))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void getArticleById_unknownId_returns404() throws Exception {
        mockMvc.perform(get(ARTICLES + "/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void getArticles_byTypeAndSearch_returnsOnlyMatchingArticles() throws Exception {
        // Arrange
        var token = adminToken();
        var prefix = uniqueName();
        createArticle(token, finishedBody(prefix + " fini"));
        createArticle(token, rawMaterialBody(prefix + " matiere", null));

        // Act + Assert
        mockMvc.perform(get(ARTICLES)
                        .param("type", "RAW_MATERIAL")
                        .param("search", prefix.toUpperCase())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].name").value(prefix + " matiere"));
    }

    @Test
    void getArticles_unknownTypeOrSizeAboveLimit_isRefused() throws Exception {
        // Arrange
        var token = adminToken();

        // Act + Assert
        mockMvc.perform(get(ARTICLES).param("type", "PACKAGING").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(ARTICLES).param("size", "100").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(get(ARTICLES).param("size", "101").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void getArticles_sortOutsideTheWhiteList_fallsBackToDefaultSort() throws Exception {
        mockMvc.perform(get(ARTICLES).param("sortBy", "supplierId; drop table articles")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk());
    }

    @Test
    void updateArticle_byAdmin_changesEveryField() throws Exception {
        // Arrange
        var token = adminToken();
        var id = createArticle(token, finishedBody(uniqueName()));
        var supplierId = createSupplier(token);
        var name = uniqueName();
        var barcode = uniqueBarcode();
        var body = """
                {"name":"%s","barcode":"%s","type":"SEMI_FINISHED","unit":"KILOGRAM","cost":4100.25,
                 "threshold":12.5,"supplierId":"%s"}
                """.formatted(name, barcode, supplierId);

        // Act + Assert
        mockMvc.perform(putJson(ARTICLES + "/" + id, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.barcode").value(barcode))
                .andExpect(jsonPath("$.type").value("SEMI_FINISHED"))
                .andExpect(jsonPath("$.unit").value("KILOGRAM"))
                .andExpect(jsonPath("$.cost").value(4100.25))
                .andExpect(jsonPath("$.threshold").value(12.5))
                .andExpect(jsonPath("$.supplierId").value(supplierId.toString()))
                .andExpect(jsonPath("$.rangeId").doesNotExist())
                .andExpect(jsonPath("$.price").doesNotExist());
    }

    @Test
    void updateArticle_withoutToken_returns401() throws Exception {
        mockMvc.perform(putJson(ARTICLES + "/" + UUID.randomUUID(), finishedBody(uniqueName())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateArticle_withSellerRole_returns403AndKeepsTheSheet() throws Exception {
        // Arrange
        var adminToken = adminToken();
        var name = uniqueName();
        var id = createArticle(adminToken, finishedBody(name));
        var token = accessToken(createUser(RoleName.SELLER), PASSWORD);

        // Act + Assert
        mockMvc.perform(putJson(ARTICLES + "/" + id, finishedBody(uniqueName()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
        assertThat(articleRepository.findById(id).orElseThrow().getName()).isEqualTo(name);
    }

    @Test
    void updateArticle_unknownId_returns404() throws Exception {
        mockMvc.perform(putJson(ARTICLES + "/" + UUID.randomUUID(), finishedBody(uniqueName()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void updateArticle_staleVersion_returns409AndKeepsTheSheet() throws Exception {
        // Arrange
        var token = adminToken();
        var name = uniqueName();
        var id = createArticle(token, finishedBody(name));
        var body = """
                {"name":"%s","type":"RAW_MATERIAL","unit":"PIECE","threshold":0,"version":42}
                """.formatted(uniqueName());

        // Act + Assert
        mockMvc.perform(putJson(ARTICLES + "/" + id, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
        assertThat(articleRepository.findById(id).orElseThrow().getName()).isEqualTo(name);
    }

    @Test
    void updateArticle_barcodeOfAnotherArticle_returns409() throws Exception {
        // Arrange
        var token = adminToken();
        var barcode = uniqueBarcode();
        createArticle(token, rawMaterialBody(uniqueName(), barcode));
        var id = createArticle(token, rawMaterialBody(uniqueName(), null));

        // Act + Assert
        mockMvc.perform(putJson(ARTICLES + "/" + id, rawMaterialBody(uniqueName(), barcode))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateArticle_ownBarcodeUnchanged_isAccepted() throws Exception {
        // Arrange
        var token = adminToken();
        var barcode = uniqueBarcode();
        var id = createArticle(token, rawMaterialBody(uniqueName(), barcode));

        // Act + Assert
        mockMvc.perform(putJson(ARTICLES + "/" + id, rawMaterialBody(uniqueName(), barcode))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void updateArticle_invalidData_returns422AndKeepsTheSheet() throws Exception {
        // Arrange
        var token = adminToken();
        var name = uniqueName();
        var id = createArticle(token, finishedBody(name));
        var body = "{\"name\":\"\",\"type\":\"FINISHED\",\"unit\":\"PIECE\",\"threshold\":-5}";

        // Act + Assert
        mockMvc.perform(putJson(ARTICLES + "/" + id, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        assertThat(articleRepository.findById(id).orElseThrow().getName()).isEqualTo(name);
    }

    @Test
    void updateArticle_massAssignmentOfIdCodeAndStatus_isIgnored() throws Exception {
        // Arrange
        var token = adminToken();
        var id = createArticle(token, finishedBody(uniqueName()));
        var code = articleRepository.findById(id).orElseThrow().getCode();
        var body = """
                {"id":"%s","code":"HACK-1","active":false,"name":"%s","type":"RAW_MATERIAL",
                 "unit":"PIECE","threshold":0}
                """.formatted(UUID.randomUUID(), uniqueName());

        // Act
        mockMvc.perform(putJson(ARTICLES + "/" + id, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.active").value(true));

        // Assert
        assertThat(articleRepository.findById(id).orElseThrow().isActive()).isTrue();
    }

    @Test
    void updateArticle_withVersionRead_savesAndTracesUpdateInJournal() throws Exception {
        // Arrange
        var token = adminToken();
        var name = uniqueName();
        var id = createArticle(token, finishedBody(name));
        var body = finishedBody(name).replace("\"threshold\":40", "\"threshold\":25").replace("}", ",\"version\":0}");

        // Act
        mockMvc.perform(putJson(ARTICLES + "/" + id, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.threshold").value(25));

        // Assert
        mockMvc.perform(get("/api/v1/journal").param("search", name).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].action").value("UPDATE"))
                .andExpect(jsonPath("$.items[0].resourceId").value(id.toString()));
    }

    @Test
    void updateArticle_withoutChange_leavesNoNewJournalLine() throws Exception {
        // Arrange
        var token = adminToken();
        var name = uniqueName();
        var id = createArticle(token, finishedBody(name));

        // Act
        mockMvc.perform(putJson(ARTICLES + "/" + id, finishedBody("  " + name + " "))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        // Assert
        mockMvc.perform(get("/api/v1/journal").param("search", name).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].action").value("CREATE"));
    }

    @Test
    void setActive_byAdmin_deactivatesThenReactivatesAndTracesOnlyRealChanges() throws Exception {
        // Arrange
        var token = adminToken();
        var name = uniqueName();
        var id = createArticle(token, finishedBody(name));

        // Act + Assert
        for (var active : new boolean[] {false, false, true}) {
            mockMvc.perform(patch(ARTICLES + "/" + id + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"active\":" + active + "}")
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(active));
        }
        mockMvc.perform(get("/api/v1/journal").param("search", name).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.items.length()").value(3));
    }

    @Test
    void setActive_withoutActiveField_returns422() throws Exception {
        // Arrange
        var token = adminToken();
        var id = createArticle(token, finishedBody(uniqueName()));

        // Act + Assert
        mockMvc.perform(patch(ARTICLES + "/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void setActive_withSellerRole_returns403() throws Exception {
        // Arrange
        var id = createArticle(adminToken(), finishedBody(uniqueName()));
        var token = accessToken(createUser(RoleName.SELLER), PASSWORD);

        // Act + Assert
        mockMvc.perform(patch(ARTICLES + "/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void setActive_unknownId_returns404() throws Exception {
        mockMvc.perform(patch(ARTICLES + "/" + UUID.randomUUID() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isNotFound());
    }

    @Test
    void createArticle_byAdmin_fillsTheAuditAuthorWithHisAccountId() throws Exception {
        // Arrange
        var token = adminToken();
        var adminId = userRepository
                .findByEmailWithRoleAndPermissions(ADMIN_EMAIL)
                .orElseThrow()
                .getId();

        // Act
        var id = createArticle(token, finishedBody(uniqueName()));

        // Assert
        var saved = articleRepository.findById(id).orElseThrow();
        assertThat(saved.getCreatedBy()).isEqualTo(adminId);
        assertThat(saved.getUpdatedBy()).isEqualTo(adminId);
    }

    @Test
    void deleteArticle_isNotExposed() throws Exception {
        // Arrange
        var token = adminToken();
        var id = createArticle(token, finishedBody(uniqueName()));

        // Act + Assert
        mockMvc.perform(delete(ARTICLES + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isMethodNotAllowed());
        assertThat(articleRepository.findById(id)).isPresent();
    }

    private UUID createArticle(String token, String body) throws Exception {
        MvcResult result = mockMvc.perform(postJson(ARTICLES, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    private UUID createSupplier(String token) throws Exception {
        var body = "{\"name\":\"Fournisseur-%s\",\"type\":\"LOCAL\"}".formatted(UUID.randomUUID());
        MvcResult result = mockMvc.perform(
                        postJson("/api/v1/suppliers", body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    private UUID rangeId() {
        return productRangeRepository.findAll().getFirst().getId();
    }

    private String finishedBody(String name) {
        return """
                {"name":"%s","type":"FINISHED","unit":"PIECE","rangeId":"%s","cost":3120.5,"price":8500,
                 "threshold":40}
                """.formatted(name, rangeId());
    }

    private static String rawMaterialBody(String name, String barcode) {
        var barcodePart = barcode == null ? "" : ",\"barcode\":\"%s\"".formatted(barcode);
        return "{\"name\":\"%s\",\"type\":\"RAW_MATERIAL\",\"unit\":\"KILOGRAM\",\"threshold\":5%s}"
                .formatted(name, barcodePart);
    }

    private static String uniqueName() {
        return "Article-" + UUID.randomUUID();
    }

    private static String uniqueBarcode() {
        return String.format("%013d", Math.abs(UUID.randomUUID().getMostSignificantBits()) % 10_000_000_000_000L);
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

    private String adminToken() throws Exception {
        return accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
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

    private String createUser(RoleName roleName) {
        var email = "user-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(roleName).orElseThrow();
        userRepository.save(
                User.create(new UserProfile(email, "Tiana", "Ranaivoson"), passwordEncoder.encode(PASSWORD), role));
        return email;
    }
}
