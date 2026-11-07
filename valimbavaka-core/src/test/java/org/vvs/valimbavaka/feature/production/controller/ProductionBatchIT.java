package org.vvs.valimbavaka.feature.production.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.user.domain.RoleName;

/** PRD-01: a seller plans a batch (output, quantity, responsible seller, inputs); the server checks every line. */
class ProductionBatchIT extends ProductionApiTestSupport {

    @Test
    void createBatch_sellerWithValidBatch_returns201PlannedWithNumberAndInputs() throws Exception {
        // Arrange
        var seller = createUser(RoleName.SELLER);
        var output = createArticle("FINISHED");
        var oil = createArticle("RAW_MATERIAL");
        var base = createArticle("SEMI_FINISHED");
        var body = batchBody(output, "30", seller.getId(), input(oil, "0.45") + "," + input(base, "1.5"));

        // Act + Assert
        mockMvc.perform(postJson(BATCHES, body).header(HttpHeaders.AUTHORIZATION, bearer(tokenOf(seller))))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, startsWith(BATCHES + "/")))
                .andExpect(jsonPath("$.number").value(matchesPattern("LOT-\\d{5,}")))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.outputArticleId").value(output.toString()))
                .andExpect(jsonPath("$.quantity").value(30))
                .andExpect(jsonPath("$.operatorId").value(seller.getId().toString()))
                .andExpect(jsonPath("$.inputs.length()").value(2))
                .andExpect(jsonPath("$.inputs[0].articleId").value(oil.toString()))
                .andExpect(jsonPath("$.inputs[0].quantity").value(0.45))
                .andExpect(jsonPath("$.inputs[1].articleId").value(base.toString()));
    }

    @Test
    void createBatch_validBatch_isTracedInJournalAndMovesNoStock() throws Exception {
        // Arrange
        var seller = createUser(RoleName.SELLER);
        var output = createArticle("FINISHED");
        var oil = createArticle("RAW_MATERIAL");
        var body = batchBody(output, "12", seller.getId(), input(oil, "2"));

        // Act
        var response = mockMvc.perform(postJson(BATCHES, body).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert
        UUID id = UUID.fromString(JsonPath.read(response, "$.id"));
        var journal = jdbcTemplate.queryForObject(
                "select count(*) from journal_entries where resource_type = 'PRODUCTION_BATCH' and resource_id = ?"
                        + " and action = 'CREATE'",
                Integer.class,
                id);
        var movements = jdbcTemplate.queryForObject(
                "select count(*) from stock_movements where reference_id = ?", Integer.class, id);
        assertThat(journal).isEqualTo(1);
        assertThat(movements).isZero();
    }

    @Test
    void createBatch_twoBatches_receiveDistinctNumbers() throws Exception {
        // Arrange
        var seller = createUser(RoleName.SELLER);
        var body = batchBody(createArticle("FINISHED"), "1", seller.getId(), input(createArticle("RAW_MATERIAL"), "1"));
        var auth = bearer(tokenOf(seller));

        // Act
        String first = JsonPath.read(
                mockMvc.perform(postJson(BATCHES, body).header(HttpHeaders.AUTHORIZATION, auth))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.number");
        String second = JsonPath.read(
                mockMvc.perform(postJson(BATCHES, body).header(HttpHeaders.AUTHORIZATION, auth))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.number");

        // Assert
        assertThat(second).isNotEqualTo(first);
    }

    @Test
    void createBatch_finishedProductAsInput_returns422() throws Exception {
        // Arrange
        var seller = createUser(RoleName.SELLER);
        var body = batchBody(createArticle("FINISHED"), "5", seller.getId(), input(createArticle("FINISHED"), "1"));

        // Act + Assert
        mockMvc.perform(postJson(BATCHES, body).header(HttpHeaders.AUTHORIZATION, bearer(tokenOf(seller))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createBatch_rawMaterialAsOutput_returns422() throws Exception {
        // Arrange
        var seller = createUser(RoleName.SELLER);
        var rawMaterial = createArticle("RAW_MATERIAL");
        var body = batchBody(rawMaterial, "5", seller.getId(), input(createArticle("RAW_MATERIAL"), "1"));

        // Act + Assert
        mockMvc.perform(postJson(BATCHES, body).header(HttpHeaders.AUTHORIZATION, bearer(tokenOf(seller))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createBatch_sameInputTwice_returns422() throws Exception {
        // Arrange
        var seller = createUser(RoleName.SELLER);
        var oil = createArticle("RAW_MATERIAL");
        var body = batchBody(createArticle("FINISHED"), "5", seller.getId(), input(oil, "1") + "," + input(oil, "2"));

        // Act + Assert
        mockMvc.perform(postJson(BATCHES, body).header(HttpHeaders.AUTHORIZATION, bearer(tokenOf(seller))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Un entrant ne peut apparaître qu'une fois dans un lot."));
    }

    @Test
    void createBatch_operatorIsNotASeller_returns422() throws Exception {
        // Arrange
        var driver = createUser(RoleName.DELIVERY_DRIVER);
        var body = batchBody(createArticle("FINISHED"), "5", driver.getId(), input(createArticle("RAW_MATERIAL"), "1"));

        // Act + Assert
        mockMvc.perform(postJson(BATCHES, body).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("La personne responsable doit être une vendeuse active."));
    }

    @Test
    void createBatch_deactivatedSeller_returns422() throws Exception {
        // Arrange
        var seller = createUser(RoleName.SELLER);
        jdbcTemplate.update("update users set is_active = false where id = ?", seller.getId());
        var body = batchBody(createArticle("FINISHED"), "5", seller.getId(), input(createArticle("RAW_MATERIAL"), "1"));

        // Act + Assert
        mockMvc.perform(postJson(BATCHES, body).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createBatch_unknownInputArticle_returns404() throws Exception {
        // Arrange
        var seller = createUser(RoleName.SELLER);
        var body = batchBody(createArticle("FINISHED"), "5", seller.getId(), input(UUID.randomUUID(), "1"));

        // Act + Assert
        mockMvc.perform(postJson(BATCHES, body).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void createBatch_zeroQuantity_returns422() throws Exception {
        assertInvalid("0", "1");
    }

    @Test
    void createBatch_quantityWithFourDecimals_returns422() throws Exception {
        assertInvalid("1.2345", "1");
    }

    @Test
    void createBatch_quantityAboveNineIntegerDigits_returns422() throws Exception {
        assertInvalid("1000000000", "1");
    }

    @Test
    void createBatch_negativeInputQuantity_returns422() throws Exception {
        assertInvalid("5", "-1");
    }

    @Test
    void createBatch_noInput_returns422() throws Exception {
        // Arrange
        var seller = createUser(RoleName.SELLER);
        var body = batchBody(createArticle("FINISHED"), "5", seller.getId(), "");

        // Act + Assert
        mockMvc.perform(postJson(BATCHES, body).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createBatch_bodyWithIdStatusAndNumber_ignoresThem() throws Exception {
        // Arrange
        var seller = createUser(RoleName.SELLER);
        var forcedId = UUID.randomUUID();
        var body = """
                {"id":"%s","status":"COMPLETED","number":"LOT-HACK","role":"ADMIN","outputArticleId":"%s",
                 "quantity":3,"operatorId":"%s","inputs":[%s]}
                """.formatted(
                        forcedId, createArticle("FINISHED"), seller.getId(), input(createArticle("RAW_MATERIAL"), "1"));

        // Act + Assert
        mockMvc.perform(postJson(BATCHES, body).header(HttpHeaders.AUTHORIZATION, bearer(tokenOf(seller))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(not(forcedId.toString())))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.number").value(not("LOT-HACK")));
    }

    @Test
    void getBatchById_createdBatch_returnsItWithInputs() throws Exception {
        // Arrange
        var seller = createUser(RoleName.SELLER);
        var body = batchBody(createArticle("FINISHED"), "4", seller.getId(), input(createArticle("RAW_MATERIAL"), "2"));
        var auth = bearer(tokenOf(seller));
        String id = JsonPath.read(
                mockMvc.perform(postJson(BATCHES, body).header(HttpHeaders.AUTHORIZATION, auth))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.id");

        // Act + Assert
        mockMvc.perform(get(BATCHES + "/" + id).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.inputs.length()").value(1));
    }

    @Test
    void getBatchById_unknownBatch_returns404() throws Exception {
        mockMvc.perform(get(BATCHES + "/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void getBatches_searchOnNumber_returnsTheMatchingBatch() throws Exception {
        // Arrange
        var seller = createUser(RoleName.SELLER);
        var body = batchBody(createArticle("FINISHED"), "4", seller.getId(), input(createArticle("RAW_MATERIAL"), "2"));
        var auth = bearer(tokenOf(seller));
        String number = JsonPath.read(
                mockMvc.perform(postJson(BATCHES, body).header(HttpHeaders.AUTHORIZATION, auth))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.number");

        // Act + Assert
        mockMvc.perform(get(BATCHES).param("search", number.toLowerCase()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].number").value(number))
                .andExpect(jsonPath("$.items[0].inputs.length()").value(1));
    }

    @Test
    void getBatches_sizeAboveMaximum_returns422() throws Exception {
        mockMvc.perform(get(BATCHES).param("size", "101").header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void getOperators_sellerToken_listsActiveSellersOnly() throws Exception {
        // Arrange
        var marker = "Op" + UUID.randomUUID().toString().substring(0, 8);
        var seller = createUser(RoleName.SELLER, marker);
        var inactive = createUser(RoleName.SELLER, marker);
        jdbcTemplate.update("update users set is_active = false where id = ?", inactive.getId());
        createUser(RoleName.DELIVERY_DRIVER, marker);

        // Act + Assert
        mockMvc.perform(get(OPERATORS)
                        .param("search", marker)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOf(seller))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].id").value(seller.getId().toString()))
                .andExpect(jsonPath("$.items[0].firstName").value(marker))
                .andExpect(jsonPath("$.items[0].email").doesNotExist())
                .andExpect(jsonPath("$.items[0].commissionRate").doesNotExist());
    }

    private void assertInvalid(String quantity, String inputQuantity) throws Exception {
        // Arrange
        var seller = createUser(RoleName.SELLER);
        var rawMaterial = createArticle("RAW_MATERIAL");
        var body = batchBody(createArticle("FINISHED"), quantity, seller.getId(), input(rawMaterial, inputQuantity));

        // Act + Assert
        mockMvc.perform(postJson(BATCHES, body).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
