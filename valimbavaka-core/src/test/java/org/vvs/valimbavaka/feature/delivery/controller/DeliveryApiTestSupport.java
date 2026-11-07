package org.vvs.valimbavaka.feature.delivery.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.stock.controller.StockApiTestSupport;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.domain.User;
import org.vvs.valimbavaka.feature.user.domain.UserProfile;
import org.vvs.valimbavaka.feature.user.repository.RoleRepository;
import org.vvs.valimbavaka.feature.user.repository.UserRepository;

/** Shared fixtures of the LIV-01 integration tests: drivers, request bodies and the POST call. */
abstract class DeliveryApiTestSupport extends StockApiTestSupport {

    protected static final String DELIVERIES = "/api/v1/deliveries";
    protected static final ZoneId BUSINESS_ZONE = ZoneId.of("Indian/Antananarivo");

    @Autowired
    private RoleRepository driverRoleRepository;

    @Autowired
    private UserRepository driverUserRepository;

    @Autowired
    private PasswordEncoder driverPasswordEncoder;

    /** Active delivery driver with the shared test password; returns his id. */
    protected UUID createDriver() {
        var email = "livreur-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = driverRoleRepository.findByName(RoleName.DELIVERY_DRIVER).orElseThrow();
        var profile = new UserProfile(email, "Tojo", "Nirina");
        return driverUserRepository.save(User.create(profile, driverPasswordEncoder.encode(PASSWORD), role)).getId();
    }

    protected String tokenOfDriver(UUID driverId) throws Exception {
        var email = jdbcTemplate.queryForObject("select email from users where id = ?", String.class, driverId);
        var body = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD);
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    protected static LocalDate tomorrow() {
        return LocalDate.now(BUSINESS_ZONE).plusDays(1);
    }

    protected static String line(UUID articleId, String quantity) {
        return "{\"articleId\":\"%s\",\"quantity\":%s}".formatted(articleId, quantity);
    }

    protected static String body(UUID driverId, LocalDate day, String lines) {
        return """
                {"customerName":"Rakoto Be","customerPhone":"034 12 345 67","address":"Lot II J 45, rue Pasteur",
                 "district":"Ivandry","driverId":"%s","scheduledDate":"%s","lines":[%s]}
                """.formatted(driverId, day, lines);
    }

    protected ResultActions createOrder(String token, String body) throws Exception {
        return mockMvc.perform(post(DELIVERIES)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    protected UUID createdOrderId(String token, String body) throws Exception {
        var result = createOrder(token, body).andExpect(status().isCreated()).andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }
}
