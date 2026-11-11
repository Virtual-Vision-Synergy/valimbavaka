package org.vvs.valimbavaka.feature.promotion.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
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

import org.vvs.valimbavaka.feature.promotion.domain.Promotion;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionDetails;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionKind;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionPeriod;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionScope;
import org.vvs.valimbavaka.feature.promotion.repository.PromotionRepository;
import org.vvs.valimbavaka.feature.referential.repository.SalesChannelRepository;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.domain.SellerTerms;
import org.vvs.valimbavaka.feature.user.domain.User;
import org.vvs.valimbavaka.feature.user.domain.UserProfile;
import org.vvs.valimbavaka.feature.user.repository.RoleRepository;
import org.vvs.valimbavaka.feature.user.repository.UserRepository;
import org.vvs.valimbavaka.support.IntegrationTest;

/**
 * PRO-04: paged list of the promotions, searchable on name and code, filtered by the status deduced from the period.
 * Each test tags its promotions with a unique word and searches on it, so the data of other tests never interferes.
 */
@IntegrationTest
@AutoConfigureMockMvc
class PromotionListIT {

    private static final String PROMOTIONS = "/api/v1/promotions";
    private static final String ADMIN_EMAIL = "admin.test@valimbavaka.mg";
    private static final String ADMIN_PASSWORD = "mot-de-passe-de-test-tres-long";
    private static final String PASSWORD = "mot-de-passe-vendeuse-long";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private SalesChannelRepository salesChannelRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void getPromotions_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(PROMOTIONS))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getPromotions_byDeliveryDriver_returns403() throws Exception {
        // Arrange
        var token = accessToken(createUser(RoleName.DELIVERY_DRIVER), PASSWORD);

        // Act + Assert
        mockMvc.perform(get(PROMOTIONS).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getPromotions_statusFilter_returnsOnlyThePromotionsOfThatStatus() throws Exception {
        // Arrange
        var tag = uniqueTag();
        var today = today();
        save(tag + " programmée", null, new PromotionPeriod(today.plusDays(1), today.plusDays(5)));
        save(tag + " active", null, new PromotionPeriod(today, today));
        save(tag + " terminée", null, new PromotionPeriod(today.minusDays(5), today.minusDays(1)));
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        // Act + Assert
        for (var status : new String[] {"SCHEDULED", "ACTIVE", "ENDED"}) {
            mockMvc.perform(list(token, tag).param("status", status))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCount").value(1))
                    .andExpect(jsonPath("$.items[0].status").value(status));
        }
        mockMvc.perform(list(token, tag))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.items[0].name").value(tag + " programmée"));
    }

    @Test
    void getPromotions_bySellerSearchingTheCodeInLowerCase_findsThePromotion() throws Exception {
        // Arrange
        var code =
                UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        save(uniqueTag(), code, new PromotionPeriod(today(), today().plusDays(3)));
        var token = accessToken(createUser(RoleName.SELLER), PASSWORD);

        // Act + Assert
        mockMvc.perform(list(token, code.toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].code").value(code))
                .andExpect(
                        jsonPath("$.items[0].channelIds[0]").value(channelId().toString()));
    }

    @Test
    void getPromotions_secondPageOfTwo_returnsTheLastPromotion() throws Exception {
        // Arrange
        var tag = uniqueTag();
        for (int day = 0; day < 3; day++) {
            save(tag + " " + day, null, new PromotionPeriod(today().plusDays(day), today().plusDays(10)));
        }
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        // Act + Assert
        mockMvc.perform(list(token, tag).param("page", "2").param("size", "2").param("sortBy", "name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void getPromotions_unknownStatus_returns400() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        // Act + Assert
        mockMvc.perform(get(PROMOTIONS).param("status", "EXPIRED").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void getPromotions_sizeAboveLimitOrSortOutsideWhiteList_isRefusedOrIgnored() throws Exception {
        // Arrange
        var token = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        // Act + Assert
        mockMvc.perform(get(PROMOTIONS).param("size", "100").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(get(PROMOTIONS).param("size", "101").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(get(PROMOTIONS)
                        .param("sortBy", "code; drop table promotions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    /** Saved directly: an ended period cannot be created through the API (end before today, PRO-01). */
    private void save(String name, String code, PromotionPeriod period) {
        promotionRepository.save(Promotion.create(new PromotionDetails(
                name,
                null,
                PromotionKind.PERCENT,
                new BigDecimal("10"),
                code,
                PromotionScope.ALL,
                Set.of(),
                period,
                Set.of(channelId()),
                null)));
    }

    private MockHttpServletRequestBuilder list(String token, String search) {
        return get(PROMOTIONS).param("search", search).header(HttpHeaders.AUTHORIZATION, bearer(token));
    }

    private UUID channelId() {
        return salesChannelRepository.findByCode("STORE").orElseThrow().getId();
    }

    private static LocalDate today() {
        return LocalDate.now(ZoneId.of("Indian/Antananarivo"));
    }

    private static String uniqueTag() {
        return "Liste-" + UUID.randomUUID();
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
        var email = "promo-list-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(roleName).orElseThrow();
        var terms =
                roleName == RoleName.SELLER ? new SellerTerms(channelId(), new BigDecimal("5.00")) : SellerTerms.NONE;
        userRepository.save(User.create(
                new UserProfile(email, "Tiana", "Ranaivoson"), passwordEncoder.encode(PASSWORD), role, terms));
        return email;
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
