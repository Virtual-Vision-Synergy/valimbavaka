package org.vvs.valimbavaka.feature.transfer.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.stock.controller.StockApiTestSupport;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.transfer.domain.TransferCommand;
import org.vvs.valimbavaka.feature.transfer.domain.TransferLineCommand;
import org.vvs.valimbavaka.feature.transfer.domain.TransferOrigin;
import org.vvs.valimbavaka.feature.transfer.service.TransferService;
import org.vvs.valimbavaka.feature.user.domain.RoleName;

/** TRF-04: the administrator opens the full sheet of a transfer (articles, quantities, route, issuer). */
class TransferDetailIT extends StockApiTestSupport {

    private static final String TRANSFERS = "/api/v1/transfers";

    @Autowired
    private TransferService transferService;

    @Test
    void getTransfer_returnBySeller_showsEveryArticleAndQuantityInTheEnteredOrder() throws Exception {
        // Arrange
        var first = createArticle(uniqueName());
        var second = createArticle(uniqueName());
        move(first, "STORE", StockMovementType.RECEIPT, "5");
        move(second, "STORE", StockMovementType.RECEIPT, "5");
        var id = recordReturn(tokenOf(RoleName.SELLER), first, second);

        // Act + Assert
        sheet(adminToken(), id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.number").value(startsWith("RET-")))
                .andExpect(jsonPath("$.origin").value("RETURN"))
                .andExpect(jsonPath("$.status").value("RETURN"))
                .andExpect(jsonPath("$.fromLocationLabel").value(label("STORE")))
                .andExpect(jsonPath("$.toLocationLabel").value(label("WAREHOUSE")))
                .andExpect(jsonPath("$.note").value("Invendus du samedi"))
                .andExpect(jsonPath("$.quantity").value(3.5))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.createdByName").value("Tiana Ranaivoson"))
                .andExpect(jsonPath("$.lines.length()").value(2))
                .andExpect(jsonPath("$.lines[0].articleId").value(first.toString()))
                .andExpect(jsonPath("$.lines[0].articleCode").value(code(first)))
                .andExpect(jsonPath("$.lines[0].articleName").value(name(first)))
                .andExpect(jsonPath("$.lines[0].unit").value("PIECE"))
                .andExpect(jsonPath("$.lines[0].quantity").value(2.5))
                .andExpect(jsonPath("$.lines[1].articleId").value(second.toString()))
                .andExpect(jsonPath("$.lines[1].quantity").value(1.0));
    }

    @Test
    void getTransfer_articleDeactivatedSince_stillShowsItsCodeAndName() throws Exception {
        // Arrange
        var articleId = createArticle(uniqueName());
        var id = recordMorningTransfer(articleId);
        var token = adminToken();
        mockMvc.perform(patch("/api/v1/articles/" + articleId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        // Act + Assert
        sheet(token, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].articleCode").value(code(articleId)))
                .andExpect(jsonPath("$.lines[0].articleName").value(name(articleId)));
    }

    @Test
    void getTransfer_transferGeneratedOutsideARequest_showsTheSystemAsIssuer() throws Exception {
        // Arrange
        var id = recordMorningTransfer(createArticle(uniqueName()));

        // Act + Assert
        sheet(adminToken(), id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origin").value("MORNING"))
                .andExpect(jsonPath("$.status").value("VALIDATED"))
                .andExpect(jsonPath("$.note").doesNotExist())
                .andExpect(jsonPath("$.createdBy").doesNotExist())
                .andExpect(jsonPath("$.createdByName").value("Système"));
    }

    @Test
    void getTransfer_unknownId_returns404() throws Exception {
        sheet(adminToken(), UUID.randomUUID().toString())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void getTransfer_malformedId_returns400() throws Exception {
        sheet(adminToken(), "pas-un-uuid")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void getTransfer_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(TRANSFERS + "/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getTransfer_withSellerRole_returns403() throws Exception {
        sheet(tokenOf(RoleName.SELLER), UUID.randomUUID().toString())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getTransfer_withDeliveryDriverRole_returns403() throws Exception {
        sheet(tokenOf(RoleName.DELIVERY_DRIVER), UUID.randomUUID().toString())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private ResultActions sheet(String token, String id) throws Exception {
        return mockMvc.perform(get(TRANSFERS + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private String recordReturn(String token, UUID first, UUID second) throws Exception {
        var body = """
                {"fromLocationId":"%s","toLocationId":"%s","note":"Invendus du samedi","lines":[
                 {"articleId":"%s","quantity":2.5},{"articleId":"%s","quantity":1}]}
                """.formatted(location("STORE"), location("WAREHOUSE"), first, second);
        var result = mockMvc.perform(post(TRANSFERS + "/returns")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private String recordMorningTransfer(UUID articleId) {
        move(articleId, "WAREHOUSE", StockMovementType.RECEIPT, "3");
        var line = new TransferLineCommand(articleId, BigDecimal.ONE);
        var command = new TransferCommand(
                TransferOrigin.MORNING, location("WAREHOUSE"), location("STORE"), List.of(line), null);
        return transferService.record(command).id().toString();
    }

    private String label(String code) {
        return jdbcTemplate.queryForObject("select label from stock_locations where code = ?", String.class, code);
    }

    private String code(UUID articleId) {
        return jdbcTemplate.queryForObject("select code from articles where id = ?", String.class, articleId);
    }

    private String name(UUID articleId) {
        return jdbcTemplate.queryForObject("select name from articles where id = ?", String.class, articleId);
    }
}
