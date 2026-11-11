package org.vvs.valimbavaka.feature.invoice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import org.vvs.valimbavaka.feature.stock.controller.StockApiTestSupport;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.domain.SellerTerms;
import org.vvs.valimbavaka.feature.user.domain.User;
import org.vvs.valimbavaka.feature.user.domain.UserProfile;
import org.vvs.valimbavaka.feature.user.repository.RoleRepository;
import org.vvs.valimbavaka.feature.user.repository.UserRepository;

/** FAC-05: the consolidated totals are the sum of the invoices of each status, for the administrator only. */
class InvoiceStatusIT extends StockApiTestSupport {

    private static final String TOTALS = "/api/v1/invoices/status-totals";
    private static final String DAY = "2001-02-03";
    private static final String NEXT_DAY = "2001-02-04";
    private static final String EMPTY_PERIOD = "?from=2001-03-01&to=2001-03-31";

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void startWithoutInvoiceOfThePeriod() {
        deleteSeededInvoices();
    }

    @AfterEach
    void deleteSeededInvoices() {
        jdbcTemplate.update("delete from invoices where business_date between date '2001-01-01' and date '2001-12-31'");
    }

    @Test
    void getStatusTotals_invoicesOfEveryStatus_sumsCountAndAmountOfEachStatus() throws Exception {
        // Arrange
        seed(DAY, "VALIDATED", "1000.00");
        seed(DAY, "VALIDATED", "2500.00");
        seed(DAY, "REVERSED", "4000.00");
        seed(DAY, "CANCELLED", "500.00");

        // Act + Assert
        totals(adminToken(), "?from=" + DAY + "&to=" + DAY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statuses.length()").value(3))
                .andExpect(jsonPath("$.statuses[0].status").value("VALIDATED"))
                .andExpect(jsonPath("$.statuses[0].invoiceCount").value(2))
                .andExpect(jsonPath("$.statuses[0].amount").value(3500.0))
                .andExpect(jsonPath("$.statuses[1].status").value("CANCELLED"))
                .andExpect(jsonPath("$.statuses[1].invoiceCount").value(1))
                .andExpect(jsonPath("$.statuses[1].amount").value(500.0))
                .andExpect(jsonPath("$.statuses[2].status").value("REVERSED"))
                .andExpect(jsonPath("$.statuses[2].invoiceCount").value(1))
                .andExpect(jsonPath("$.statuses[2].amount").value(4000.0))
                .andExpect(jsonPath("$.invoiceCount").value(4))
                .andExpect(jsonPath("$.amount").value(8000.0));
    }

    @Test
    void getStatusTotals_periodBounds_areInclusiveAndOtherDaysAreLeftOut() throws Exception {
        // Arrange
        seed(DAY, "VALIDATED", "1000.00");
        seed(NEXT_DAY, "VALIDATED", "2000.00");
        seed("2001-02-05", "VALIDATED", "4000.00");

        // Act + Assert
        totals(adminToken(), "?from=" + DAY + "&to=" + NEXT_DAY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value(DAY))
                .andExpect(jsonPath("$.to").value(NEXT_DAY))
                .andExpect(jsonPath("$.invoiceCount").value(2))
                .andExpect(jsonPath("$.amount").value(3000.0));
    }

    @Test
    void getStatusTotals_periodWithoutInvoice_listsEveryStatusAtZero() throws Exception {
        totals(adminToken(), EMPTY_PERIOD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statuses.length()").value(3))
                .andExpect(jsonPath("$.statuses[0].invoiceCount").value(0))
                .andExpect(jsonPath("$.statuses[0].amount").value(0.0))
                .andExpect(jsonPath("$.statuses[2].invoiceCount").value(0))
                .andExpect(jsonPath("$.invoiceCount").value(0))
                .andExpect(jsonPath("$.amount").value(0.0));
    }

    @Test
    void getStatusTotals_saleThenCreditNote_movesTheAmountFromPaidToCredited() throws Exception {
        // Arrange
        var sale = recordSale();
        var day = jdbcTemplate.queryForObject(
                "select business_date::text from invoices where sale_id = ?", String.class, sale);
        var period = "?from=" + day + "&to=" + day;
        var paid = amountOf(period, 0);
        var credited = amountOf(period, 2);

        // Act
        var invoice = jdbcTemplate.queryForObject("select id from invoices where sale_id = ?", UUID.class, sale);
        mockMvc.perform(post("/api/v1/invoices/" + invoice + "/credit-note")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Retour de la cliente\"}"))
                .andExpect(status().isCreated());

        // Assert
        assertThat(amountOf(period, 0)).isEqualByComparingTo(paid.subtract(new BigDecimal("25500")));
        assertThat(amountOf(period, 2)).isEqualByComparingTo(credited.add(new BigDecimal("25500")));
    }

    @Test
    void getStatusTotals_withoutDates_returnsTheCurrentMonth() throws Exception {
        totals(adminToken(), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").isNotEmpty())
                .andExpect(jsonPath("$.to").isNotEmpty())
                .andExpect(jsonPath("$.statuses.length()").value(3));
    }

    @Test
    void getStatusTotals_fromAfterTo_returns422() throws Exception {
        totals(adminToken(), "?from=2026-11-10&to=2026-11-01")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void getStatusTotals_malformedDate_returns400() throws Exception {
        totals(adminToken(), "?from=01-11-2026")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void getStatusTotals_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(TOTALS))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getStatusTotals_sellerAndDeliveryDriver_return403() throws Exception {
        totals(tokenOf(RoleName.SELLER), "")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        totals(tokenOf(RoleName.DELIVERY_DRIVER), "")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private ResultActions totals(String token, String parameters) throws Exception {
        return mockMvc.perform(get(TOTALS + parameters).header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private BigDecimal amountOf(String period, int statusIndex) throws Exception {
        var json = totals(adminToken(), period)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return new BigDecimal(JsonPath.read(json, "$.statuses[%d].amount".formatted(statusIndex))
                .toString());
    }

    /** An invoice written directly (no foreign key), on a day no other test uses. */
    private void seed(String businessDate, String status, String total) {
        jdbcTemplate.update(
                """
                insert into invoices (id, number, sale_id, sale_number, issued_at, business_date, payment_method,
                    issuer_name, status, subtotal, discount_amount, total, created_at, updated_at, version)
                values (gen_random_uuid(), ?, gen_random_uuid(), 'VTE-TEST', now(), ?::date, 'CASH', 'Test', ?,
                    ?::numeric, 0, ?::numeric, now(), now(), 0)
                """, "FAC-T-" + UUID.randomUUID().toString().substring(0, 12), businessDate, status, total, total);
    }

    /** Records a sale of 3 articles at 8 500 Ar (25 500 Ar) through the API: its invoice is issued with it. */
    private UUID recordSale() throws Exception {
        endAutomaticPromotions();
        var openDay = mockMvc.perform(post("/api/v1/days")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[]}"))
                .andReturn()
                .getResponse()
                .getStatus();
        assertThat(openDay).isIn(201, 409);
        var soap = createArticle(uniqueName());
        move(soap, "STORE", StockMovementType.RECEIPT, "10");
        var channel = jdbcTemplate.queryForObject("select id from sales_channels where code = 'STORE'", UUID.class);
        var email = "vendeuse-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(RoleName.SELLER).orElseThrow();
        var seller = userRepository.save(User.create(
                new UserProfile(email, "Miora", "Rakoto"),
                passwordEncoder.encode("mot-de-passe-long-1"),
                role,
                new SellerTerms(channel, new BigDecimal("5.00"))));
        var body = "{\"salesChannelId\":\"%s\",\"sellerId\":\"%s\",\"paymentMethod\":\"CASH\",\"lines\":[%s]}"
                .formatted(channel, seller.getId(), "{\"articleId\":\"%s\",\"quantity\":3}".formatted(soap));
        var json = mockMvc.perform(post("/api/v1/sales")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }
}
