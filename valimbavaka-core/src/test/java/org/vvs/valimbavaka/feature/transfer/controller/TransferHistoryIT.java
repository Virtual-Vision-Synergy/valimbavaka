package org.vvs.valimbavaka.feature.transfer.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
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

/** TRF-01: the administrator reads the history of the transfers (reference, date, route, status, issuer). */
class TransferHistoryIT extends StockApiTestSupport {

    private static final String TRANSFERS = "/api/v1/transfers";

    @Autowired
    private TransferService transferService;

    @Test
    void getTransfers_returnBySeller_showsReferenceDateRouteStatusAndIssuer() throws Exception {
        // Arrange
        var first = createArticle(uniqueName());
        var second = createArticle(uniqueName());
        move(first, "STORE", StockMovementType.RECEIPT, "5");
        move(second, "STORE", StockMovementType.RECEIPT, "5");
        var number = recordReturn(tokenOf(RoleName.SELLER), first, second);

        // Act + Assert
        list(adminToken(), "search", number)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].number").value(number))
                .andExpect(jsonPath("$.items[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$.items[0].origin").value("RETURN"))
                .andExpect(jsonPath("$.items[0].status").value("RETURN"))
                .andExpect(jsonPath("$.items[0].fromLocationId")
                        .value(location("STORE").toString()))
                .andExpect(jsonPath("$.items[0].fromLocationLabel").value(label("STORE")))
                .andExpect(jsonPath("$.items[0].toLocationId")
                        .value(location("WAREHOUSE").toString()))
                .andExpect(jsonPath("$.items[0].toLocationLabel").value(label("WAREHOUSE")))
                .andExpect(jsonPath("$.items[0].lineCount").value(2))
                .andExpect(jsonPath("$.items[0].quantity").value(3.5))
                .andExpect(jsonPath("$.items[0].createdBy").isNotEmpty())
                .andExpect(jsonPath("$.items[0].createdByName").value("Tiana Ranaivoson"));
    }

    @Test
    void getTransfers_transferGeneratedOutsideARequest_showsTheSystemAsIssuer() throws Exception {
        // Arrange
        var number = recordMorningTransfer();

        // Act + Assert
        list(adminToken(), "search", number)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].status").value("VALIDATED"))
                .andExpect(jsonPath("$.items[0].origin").value("MORNING"))
                .andExpect(jsonPath("$.items[0].createdBy").doesNotExist())
                .andExpect(jsonPath("$.items[0].createdByName").value("Système"));
    }

    @Test
    void getTransfers_noSort_listsTheNewestFirst() throws Exception {
        // Arrange
        recordMorningTransfer();
        recordMorningTransfer();

        // Act
        var json = list(adminToken(), "size", "100")
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert
        List<String> dates = JsonPath.read(json, "$.items[*].createdAt");
        var instants = dates.stream().map(Instant::parse).toList();
        assertThat(instants).hasSizeGreaterThanOrEqualTo(2).isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    void getTransfers_filteredByOrigin_returnsOnlyThatOrigin() throws Exception {
        // Arrange
        var morning = recordMorningTransfer();

        // Act + Assert
        list(adminToken(), "origin", "MORNING")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].origin", everyItem(is("MORNING"))))
                .andExpect(jsonPath("$.items[*].number", hasItem(morning)));
        list(adminToken(), "origin", "RETURN")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].origin", everyItem(is("RETURN"))));
    }

    @Test
    void getTransfers_searchWithoutMatch_returnsAnEmptyPage() throws Exception {
        list(adminToken(), "search", "ZZZ-" + UUID.randomUUID())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void getTransfers_sortOutsideTheWhiteList_returns200() throws Exception {
        list(adminToken(), "sortBy", "note").andExpect(status().isOk());
    }

    @Test
    void getTransfers_size100_returns200() throws Exception {
        list(adminToken(), "size", "100")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageSize").value(100));
    }

    @Test
    void getTransfers_sizeAbove100_returns422() throws Exception {
        list(adminToken(), "size", "101")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void getTransfers_pageZero_returns422() throws Exception {
        list(adminToken(), "page", "0")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void getTransfers_unknownOrigin_returns400() throws Exception {
        list(adminToken(), "origin", "TRUCK")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void getTransfers_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(TRANSFERS))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getTransfers_withSellerRole_returns403() throws Exception {
        list(tokenOf(RoleName.SELLER), "page", "1")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getTransfers_withDeliveryDriverRole_returns403() throws Exception {
        list(tokenOf(RoleName.DELIVERY_DRIVER), "page", "1")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private ResultActions list(String token, String param, String value) throws Exception {
        return mockMvc.perform(get(TRANSFERS).param(param, value).header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private String recordReturn(String token, UUID first, UUID second) throws Exception {
        var body = """
                {"fromLocationId":"%s","toLocationId":"%s","lines":[
                 {"articleId":"%s","quantity":2.5},{"articleId":"%s","quantity":1}]}
                """.formatted(location("STORE"), location("WAREHOUSE"), first, second);
        var result = mockMvc.perform(post(TRANSFERS + "/returns")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.number");
    }

    private String recordMorningTransfer() throws Exception {
        var articleId = createArticle(uniqueName());
        move(articleId, "WAREHOUSE", StockMovementType.RECEIPT, "3");
        var line = new TransferLineCommand(articleId, BigDecimal.ONE);
        var command = new TransferCommand(
                TransferOrigin.MORNING, location("WAREHOUSE"), location("STORE"), List.of(line), null);
        return transferService.record(command).number();
    }

    private String label(String code) {
        return jdbcTemplate.queryForObject("select label from stock_locations where code = ?", String.class, code);
    }
}
