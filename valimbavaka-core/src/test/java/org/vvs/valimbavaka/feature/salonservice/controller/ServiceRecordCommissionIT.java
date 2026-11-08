package org.vvs.valimbavaka.feature.salonservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.user.domain.RoleName;

/**
 * COM-02 on salon services: the service's own rate wins over the store rule and the seller's rate (PO decision of
 * 07/11/2026, no accumulation) and applies to the price; an administrator recording a service earns nothing.
 */
class ServiceRecordCommissionIT extends ServiceRecordApiTestSupport {

    @Test
    void recordService_serviceWithItsOwnRate_commissionIsThePriceTimesTheServiceRate() throws Exception {
        // Arrange : service à 35 000 Ar et 10 %, vendeuse à 5 %
        var serviceId = createServiceWithRate("10.00");
        var email = createUser(RoleName.SELLER);

        // Act
        var recordId = record(serviceId, tokenOf(email));

        // Assert : 35 000 × 10 % = 3 500 Ar
        var commission = commissionOf(recordId);
        assertThat(commission).isNotNull();
        assertThat(commission.get("source_type")).isEqualTo("SERVICE_RECORD");
        assertThat(commission.get("seller_id")).isEqualTo(userId(email));
        assertThat(commission.get("salon_service_id")).isEqualTo(serviceId);
        assertThat(commission.get("rate_source")).isEqualTo("SALON_SERVICE");
        assertThat((BigDecimal) commission.get("revenue")).isEqualByComparingTo("35000");
        assertThat((BigDecimal) commission.get("rate")).isEqualByComparingTo("10");
        assertThat((BigDecimal) commission.get("amount")).isEqualByComparingTo("3500");
    }

    @Test
    void recordService_byAdministrator_createsNoCommission() throws Exception {
        // Arrange
        var serviceId = createServiceWithRate("10.00");

        // Act
        var recordId = record(serviceId, adminToken());

        // Assert
        assertThat(commissionOf(recordId)).isNull();
    }

    private UUID record(UUID serviceId, String token) throws Exception {
        var body = "{\"salonServiceId\":\"%s\",\"products\":[]}".formatted(serviceId);
        var json = mockMvc.perform(postJson(RECORDS, body).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    /** Active catalogue service at 35000 Ar, without recipe, with its own commission rate. */
    private UUID createServiceWithRate(String rate) throws Exception {
        var body = "{\"name\":\"Soin-%s\",\"durationMinutes\":45,\"price\":35000,\"commissionRate\":%s,\"products\":[]}"
                .formatted(UUID.randomUUID(), rate);
        var json = mockMvc.perform(postJson("/api/v1/salon-services", body)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private @Nullable Map<String, Object> commissionOf(UUID serviceRecordId) {
        var rows = jdbcTemplate.queryForList(
                "select * from commissions where source_type = 'SERVICE_RECORD' and source_id = ?", serviceRecordId);
        return rows.isEmpty() ? null : rows.getFirst();
    }
}
