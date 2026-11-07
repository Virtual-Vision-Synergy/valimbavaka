package org.vvs.valimbavaka.feature.shooting.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.user.domain.RoleName;

/** SHO-01: reading with services.read, recording with shootings.create (administrator and seller only). */
class ShootingSecurityIT extends ShootingApiTestSupport {

    @Test
    void createShooting_withoutToken_returns401() throws Exception {
        mockMvc.perform(post(SHOOTINGS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(shootingBody(UUID.randomUUID(), "1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void createShooting_withDeliveryDriverRole_returns403() throws Exception {
        // Arrange
        var articleId = createArticle();
        receive(articleId, "STORE", "2");
        var token = tokenOf(RoleName.DELIVERY_DRIVER);

        // Act + Assert
        postShooting(token, shootingBody(articleId, "1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        assertThat(balance(articleId, "STORE")).isEqualByComparingTo("2");
    }

    @Test
    void getShootings_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(SHOOTINGS))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getShootings_withDeliveryDriverRole_returns403() throws Exception {
        // Arrange
        var token = tokenOf(RoleName.DELIVERY_DRIVER);

        // Act + Assert
        mockMvc.perform(get(SHOOTINGS).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getShootings_withSellerRole_returns200() throws Exception {
        // Arrange
        var token = tokenOf(RoleName.SELLER);

        // Act + Assert
        mockMvc.perform(get(SHOOTINGS).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void createShooting_massAssignmentOfIdNumberStatusAndAuthor_isIgnored() throws Exception {
        // Arrange
        var articleId = createArticle();
        receive(articleId, "STORE", "2");
        var forgedId = UUID.randomUUID();
        var body = """
                {"id":"%s","number":"SHO-9999","status":"RETURNED","takenBy":"Quelqu'un d'autre","late":true,
                 "locationId":"%s","expectedReturnAt":"%s","items":[{"articleId":"%s","quantity":1}]}
                """.formatted(forgedId, location("STORE"), tomorrow(), articleId);

        // Act
        var response = postShooting(adminToken(), body)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert
        assertThat(UUID.fromString(JsonPath.read(response, "$.id"))).isNotEqualTo(forgedId);
        assertThat((String) JsonPath.read(response, "$.number")).isNotEqualTo("SHO-9999");
        assertThat((String) JsonPath.read(response, "$.status")).isEqualTo("OUT");
        assertThat((String) JsonPath.read(response, "$.takenBy")).isEqualTo("Rasoa Rakoto");
        assertThat((Boolean) JsonPath.read(response, "$.late")).isFalse();
    }
}
