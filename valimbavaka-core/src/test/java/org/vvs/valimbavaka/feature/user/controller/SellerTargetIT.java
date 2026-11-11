package org.vvs.valimbavaka.feature.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.stock.controller.StockApiTestSupport;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.domain.SellerTerms;
import org.vvs.valimbavaka.feature.user.domain.User;
import org.vvs.valimbavaka.feature.user.domain.UserProfile;
import org.vvs.valimbavaka.feature.user.repository.RoleRepository;
import org.vvs.valimbavaka.feature.user.repository.UserRepository;

/**
 * COM-04 (PO decision of 07/11/2026): the monthly target of a seller is a field of her user record, set by the
 * administrator only. A seller can never write it, not even on her own profile.
 */
class SellerTargetIT extends StockApiTestSupport {

    private static final String USERS = "/api/v1/users";
    private static final String SELLER_PASSWORD = "mot-de-passe-vendeuse-long";

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID channelId;
    private User seller;

    @BeforeEach
    void createASellerOnItsOwnChannel() {
        channelId = UUID.randomUUID();
        var code = "IT-" + channelId.toString().substring(0, 8);
        jdbcTemplate.update("insert into sales_channels (id, code, label) values (?, ?, ?)", channelId, code, code);
        var role = roleRepository.findByName(RoleName.SELLER).orElseThrow();
        var profile = new UserProfile("vendeuse-" + UUID.randomUUID() + "@valimbavaka.mg", "Miora", "Rakoto");
        var terms = new SellerTerms(channelId, new BigDecimal("5.00"));
        seller = userRepository.save(User.create(profile, passwordEncoder.encode(SELLER_PASSWORD), role, terms));
    }

    @Test
    void createUser_sellerWithTarget_returnsAndStoresTheTarget() throws Exception {
        // Arrange
        var body = newSellerJson(",\"monthlyTarget\":1500000");

        // Act
        var json = mockMvc.perform(post(USERS)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.monthlyTarget").value(1500000.0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert
        var id = UUID.fromString(idOf(json));
        var stored = jdbcTemplate.queryForObject("select monthly_target from users where id = ?", BigDecimal.class, id);
        assertThat(stored).isEqualByComparingTo("1500000");
    }

    @Test
    void createUser_sellerWithoutTarget_hasNoTarget() throws Exception {
        mockMvc.perform(post(USERS)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newSellerJson("")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.monthlyTarget").doesNotExist());
    }

    @Test
    void updateUser_byAdministrator_setsChangesAndClearsTheTargetAndTracesIt() throws Exception {
        // Act + Assert : fixé, modifié puis retiré
        update(adminToken(), seller, "1000000").andExpect(status().isOk());
        update(adminToken(), seller, "2500000.50")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyTarget").value(2500000.5));
        mockMvc.perform(get(USERS + "/" + seller.getId()).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(jsonPath("$.monthlyTarget").value(2500000.5));
        update(adminToken(), seller, "null")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyTarget").doesNotExist());

        // Assert : trois lignes de journal avec l'ancien et le nouvel objectif
        var descriptions = jdbcTemplate.queryForList(
                "select description from journal_entries where resource_type = 'USER' and resource_id = ? "
                        + "and description like '%objectif mensuel%' order by occurred_at",
                String.class,
                seller.getId());
        assertThat(descriptions)
                .hasSize(3)
                .anyMatch(text -> text.endsWith("objectif mensuel : aucun → 1000000.00 Ar"))
                .anyMatch(text -> text.endsWith("objectif mensuel : 1000000.00 → 2500000.50 Ar"))
                .anyMatch(text -> text.endsWith("objectif mensuel : 2500000.50 → aucun Ar"));
    }

    @Test
    void updateUser_sameTarget_isNotTracedAgain() throws Exception {
        // Arrange
        update(adminToken(), seller, "1000000").andExpect(status().isOk());

        // Act : même objectif, écrit autrement
        update(adminToken(), seller, "1000000.00").andExpect(status().isOk());

        // Assert
        var traces = jdbcTemplate.queryForObject(
                "select count(*) from journal_entries where resource_type = 'USER' and resource_id = ? "
                        + "and description like '%objectif mensuel%'",
                Integer.class,
                seller.getId());
        assertThat(traces).isEqualTo(1);
    }

    @Test
    void updateUser_sellerOnHerOwnProfile_returns403AndTheTargetStaysUnchanged() throws Exception {
        // Arrange
        jdbcTemplate.update("update users set monthly_target = 1000000 where id = ?", seller.getId());
        var sellerToken = login(seller.getEmail());

        // Act + Assert : une vendeuse ne s'augmente pas son objectif
        update(sellerToken, seller, "1")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        var stored = jdbcTemplate.queryForObject(
                "select monthly_target from users where id = ?", BigDecimal.class, seller.getId());
        assertThat(stored).isEqualByComparingTo("1000000");
    }

    @Test
    void updateUser_withoutToken_returns401() throws Exception {
        mockMvc.perform(put(USERS + "/" + seller.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(seller, "1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void updateUser_invalidTarget_returns422() throws Exception {
        for (var target : new String[] {"0", "-5", "100.123"}) {
            update(adminToken(), seller, target)
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
    }

    @Test
    void updateUser_targetOnAnAdministrator_returns422() throws Exception {
        // Arrange : l'objectif n'a de sens que pour une vendeuse
        var administrator = userRepository.findByEmailWithRoleAndPermissions(ADMIN_EMAIL).orElseThrow();
        var identity = "\"email\":\"%s\",\"firstName\":\"Tiana\",\"lastName\":\"Admin\"".formatted(ADMIN_EMAIL);
        var role = "\"roleId\":\"%s\"".formatted(administrator.getRole().getId());
        var body = "{" + identity + "," + role + ",\"monthlyTarget\":1000000,\"active\":true}";

        // Act + Assert
        mockMvc.perform(put(USERS + "/" + administrator.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableContent());
    }

    private UUID sellerRoleId() {
        return roleRepository.findByName(RoleName.SELLER).orElseThrow().getId();
    }

    private ResultActions update(String token, User user, String target) throws Exception {
        return mockMvc.perform(put(USERS + "/" + user.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(user, target)));
    }

    /** Body of the modification of a seller; {@code target} is a JSON number or {@code null}. */
    private String body(User user, String target) {
        var identity = "\"email\":\"%s\",\"firstName\":\"Miora\",\"lastName\":\"Rakoto\"".formatted(user.getEmail());
        var terms = "\"salesChannelId\":\"%s\",\"commissionRate\":5.00".formatted(channelId);
        var role = "\"roleId\":\"%s\"".formatted(sellerRoleId());
        return "{" + identity + "," + role + "," + terms + ",\"monthlyTarget\":" + target + ",\"active\":true}";
    }

    /** Body of the creation of a seller on the test channel; {@code extra} adds JSON fields after the commission. */
    private String newSellerJson(String extra) {
        var email = "nouvelle-" + UUID.randomUUID() + "@valimbavaka.mg";
        var identity = "\"email\":\"%s\",\"password\":\"mot-de-passe-vendeuse-long\"".formatted(email);
        var names = "\"firstName\":\"Hanta\",\"lastName\":\"Rabe\"";
        var role = "\"roleId\":\"%s\"".formatted(sellerRoleId());
        var terms = "\"salesChannelId\":\"%s\",\"commissionRate\":4.5".formatted(channelId);
        return "{" + identity + "," + names + "," + role + "," + terms + extra + "}";
    }

    private static String idOf(String json) {
        return JsonPath.read(json, "$.id");
    }

    private String login(String email) throws Exception {
        var body = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, SELLER_PASSWORD);
        var json = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(json, "$.accessToken");
    }
}
