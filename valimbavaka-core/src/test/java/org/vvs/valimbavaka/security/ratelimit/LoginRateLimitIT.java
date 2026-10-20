package org.vvs.valimbavaka.security.ratelimit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.vvs.valimbavaka.support.IntegrationTest;

@IntegrationTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"vvs.rate-limit.login-per-ip=1000", "vvs.rate-limit.login-per-identifier=3"})
class LoginRateLimitIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void login_moreAttemptsThanAllowedForOneIdentifier_returns429WithRetryAfter() throws Exception {
        // Arrange
        var body = "{\"email\":\"cible@valimbavaka.mg\",\"password\":\"mauvais-mot-de-passe-long\"}";

        // Act + Assert
        for (int attempt = 0; attempt < 3; attempt++) {
            mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
    }

    @Test
    void login_otherIdentifierOnSameIp_isNotThrottled() throws Exception {
        // Arrange
        var body = "{\"email\":\"autre@valimbavaka.mg\",\"password\":\"mauvais-mot-de-passe-long\"}";

        // Act + Assert
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }
}
