package org.vvs.valimbavaka.feature.dayclosing.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

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

/**
 * JR-06: lock of today's closing, administrator only. The database is shared with the other test classes: a test that
 * closes today's day puts it back to OPEN and removes its count lines afterwards, so that no later class finds the
 * day closed. Every line of today's reconciliation must be counted to lock, so the helpers count all of them.
 */
class DayClosingLockIT extends StockApiTestSupport {

    private static final String CLOSING = "/api/v1/days/closing";
    private static final String COUNTS = "/api/v1/days/closing/counts";
    private static final String LOCK = "/api/v1/days/closing/lock";

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User seller;
    private UUID soap;

    /** Today's day opened (another test class may have removed it), 10 units of a new article in the store. */
    @BeforeEach
    void openTodayAndStockTheStore() throws Exception {
        endAutomaticPromotions();
        openToday();
        var email = "vendeuse-" + UUID.randomUUID() + "@valimbavaka.mg";
        var role = roleRepository.findByName(RoleName.SELLER).orElseThrow();
        var terms = new SellerTerms(channel("STORE"), new BigDecimal("5.00"));
        seller = userRepository.save(User.create(
                new UserProfile(email, "Zoé", "Rakoto"), passwordEncoder.encode("mot-de-passe-long-1"), role, terms));
        soap = createArticle(uniqueName());
        move(soap, "WAREHOUSE", StockMovementType.RECEIPT, "200");
        move(soap, "WAREHOUSE", StockMovementType.TRANSFER, "-10");
        move(soap, "STORE", StockMovementType.TRANSFER, "10");
    }

    @AfterEach
    void reopenTodayAndRemoveTheCounts() {
        jdbcTemplate.update("update business_days set status = 'OPEN' where status = 'CLOSED'");
        jdbcTemplate.update("delete from day_closing_counts");
        jdbcTemplate.update("update users set is_active = false where id = ?", seller.getId());
    }

    @Test
    void lock_withoutToken_returns401() throws Exception {
        mockMvc.perform(post(LOCK))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void lock_sellerAndDeliveryDriver_return403AndTheDayStaysOpen() throws Exception {
        for (var role : List.of(RoleName.SELLER, RoleName.DELIVERY_DRIVER)) {
            mockMvc.perform(post(LOCK).header(HttpHeaders.AUTHORIZATION, bearer(tokenOf(role))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }
        assertThat(todayStatus()).isEqualTo("OPEN");
    }

    @Test
    void lock_everyLineCounted_returns200WithTheClosedDayAndKeepsTheClosingReadable() throws Exception {
        // Arrange
        countEverything(null, null);

        // Act
        lock().andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.businessDayId").isNotEmpty());

        // Assert
        assertThat(todayStatus()).isEqualTo("CLOSED");
        mockMvc.perform(get(CLOSING).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void lock_everyLineCounted_writesOneJournalLineOnTheDay() throws Exception {
        // Arrange
        countEverything(null, null);
        var before = closingJournalLines();

        // Act
        lock().andExpect(status().isOk());

        // Assert
        assertThat(closingJournalLines()).isEqualTo(before + 1);
    }

    @Test
    void lock_gapWithReason_recordsTheGapsInTheJournal() throws Exception {
        // Arrange: 10 expected, 8 counted (the shared database may hold other gaps: the line lists the first ten)
        countEverything("8", "LOSS");

        // Act
        lock().andExpect(status().isOk());

        // Assert
        var description = jdbcTemplate.queryForObject(
                "select description from journal_entries where resource_type = 'BUSINESS_DAY' and action = 'UPDATE' "
                        + "and description like 'Clôture de la journée du %' order by occurred_at desc limit 1",
                String.class);
        assertThat(description).contains("ligne(s) comptée(s)").doesNotContain(", 0 écart(s)");
    }

    @Test
    void lock_lineNotCounted_returns422AndTheDayStaysOpen() throws Exception {
        // Arrange
        var before = closingJournalLines();

        // Act + Assert: the article of the test has no count
        lock().andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.detail").value(containsString("Toutes les lignes doivent être comptées")));
        assertThat(todayStatus()).isEqualTo("OPEN");
        assertThat(closingJournalLines()).isEqualTo(before);
    }

    @Test
    void lock_saleAfterTheCountLeavesAGapWithoutReason_returns422ThenPassesOnceRecounted() throws Exception {
        // Arrange: counted 10 = theoretical, then 2 units leave the store: the theoretical is now 8
        countEverything(null, null);
        move(soap, "STORE", StockMovementType.SALE, "-2");

        // Act + Assert: the gap is read again at the lock
        lock().andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(containsString("Un motif est obligatoire")))
                .andExpect(jsonPath("$.detail").value(containsString(reference())));
        assertThat(todayStatus()).isEqualTo("OPEN");

        // The administrator counts again on the new theoretical quantity
        countEverything(null, null);
        lock().andExpect(status().isOk());
    }

    @Test
    void lock_saleAfterTheCountRemovesTheGap_freezesTheNewTheoreticalAndClearsTheReason() throws Exception {
        // Arrange: 8 counted with a reason against 10; two units are sold afterwards: theoretical 8, no gap anymore
        countEverything("8", "BREAKAGE");
        move(soap, "STORE", StockMovementType.SALE, "-2");

        // Act
        lock().andExpect(status().isOk());

        // Assert
        var theoretical = jdbcTemplate.queryForObject(
                "select theoretical_quantity from day_closing_counts where article_id = ?", BigDecimal.class, soap);
        var reason = jdbcTemplate.queryForObject(
                "select reason from day_closing_counts where article_id = ?", String.class, soap);
        assertThat(theoretical).isEqualByComparingTo("8");
        assertThat(reason).isNull();
    }

    @Test
    void lock_twiceInARow_secondReturns409() throws Exception {
        // Arrange
        countEverything(null, null);
        lock().andExpect(status().isOk());

        // Act + Assert
        lock().andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.detail").value(containsString("déjà clôturée")));
    }

    @Test
    void lock_dayNotOpened_returns409() throws Exception {
        // Arrange: every day moves back in time, so that none is today's
        jdbcTemplate.update("update business_days set business_date = business_date - 400");

        // Act + Assert
        try {
            lock().andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLICT"))
                    .andExpect(jsonPath("$.detail").value(startsWith("La journée du ")));
        } finally {
            jdbcTemplate.update("update business_days set business_date = business_date + 400");
        }
    }

    @Test
    void lock_bodyWithRoleAndId_isIgnored() throws Exception {
        // Arrange
        countEverything(null, null);

        // Act + Assert: the lock reads no body (mass assignment)
        mockMvc.perform(post(LOCK)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\",\"id\":\"%s\",\"status\":\"OPEN\"}".formatted(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void afterTheLock_aSaleIsRefusedWith409() throws Exception {
        // Arrange
        countEverything(null, null);
        lock().andExpect(status().isOk());

        // Act + Assert
        var line = "{\"articleId\":\"%s\",\"quantity\":1}".formatted(soap);
        var body = "{\"salesChannelId\":\"%s\",\"sellerId\":\"%s\",\"paymentMethod\":\"CASH\",\"lines\":[%s]}"
                .formatted(channel("STORE"), seller.getId(), line);
        mockMvc.perform(post("/api/v1/sales")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.detail").value(containsString("clôturée")));
    }

    @Test
    void afterTheLock_aCountIsRefusedWith409AndTheFrozenCountsStay() throws Exception {
        // Arrange
        countEverything(null, null);
        lock().andExpect(status().isOk());
        var rows = countRows();

        // Act + Assert
        var body = "{\"counts\":[{\"locationId\":\"%s\",\"articleId\":\"%s\",\"counted\":3,\"reason\":\"LOSS\"}]}"
                .formatted(location("STORE"), soap);
        mockMvc.perform(put(COUNTS)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("déjà clôturée")));
        assertThat(countRows()).isEqualTo(rows);
    }

    @Test
    void lock_thenTheNextDay_opensWithTheClosedDayAsTheDayBefore() throws Exception {
        // Arrange: the day is locked, then every day moves back in time so that today has no day (the next morning)
        countEverything(null, null);
        lock().andExpect(status().isOk());
        jdbcTemplate.update("update business_days set business_date = business_date - 400");
        var token = bearer(adminToken());
        var today = "(now() at time zone 'Indian/Antananarivo')::date";
        var sql = "select to_char(" + today + " - 400, 'YYYY-MM-DD')";
        var dayBefore = jdbcTemplate.queryForObject(sql, String.class);

        // Act + Assert: JR-02 still reads the closed day (its morning quantities), and today opens normally
        try {
            mockMvc.perform(get("/api/v1/days/opening").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.previousDay").value(dayBefore))
                    .andExpect(jsonPath("$.day").doesNotExist());
            mockMvc.perform(post("/api/v1/days")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"lines\":[]}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("OPEN"));
        } finally {
            jdbcTemplate.update("delete from business_days where business_date = " + today);
            jdbcTemplate.update("update business_days set business_date = business_date + 400");
        }
    }

    @Test
    void lock_twoSimultaneousRequests_oneClosesTheDayTheOtherGets409() throws Exception {
        // Arrange
        countEverything(null, null);
        var token = adminToken();
        var start = new CountDownLatch(1);
        Callable<Integer> request = () -> {
            start.await();
            return mockMvc.perform(post(LOCK).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andReturn()
                    .getResponse()
                    .getStatus();
        };
        var pool = Executors.newFixedThreadPool(2);
        var before = closingJournalLines();

        // Act
        var first = pool.submit(request);
        var second = pool.submit(request);
        start.countDown();
        var statuses = new ArrayList<>(List.of(first.get(), second.get()));
        pool.shutdown();

        // Assert
        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        assertThat(closingJournalLines()).isEqualTo(before + 1);
        assertThat(todayStatus()).isEqualTo("CLOSED");
    }

    private ResultActions lock() throws Exception {
        return mockMvc.perform(post(LOCK).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())));
    }

    /**
     * Counts every line of today's reconciliation on its theoretical quantity (no gap); the line of the test article
     * takes {@code soapCounted} and {@code reason} instead when given.
     */
    private void countEverything(String soapCounted, String reason) throws Exception {
        var json = mockMvc.perform(get(CLOSING).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var counts = new ArrayList<String>();
        List<Map<String, Object>> locations = JsonPath.read(json, "$.locations");
        for (var location : locations) {
            var lines = JsonPath.<List<Map<String, Object>>>read(location, "$.lines");
            for (var line : lines) {
                counts.add(countOf(location.get("locationId"), line, soapCounted, reason));
            }
        }
        mockMvc.perform(put(COUNTS)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"counts\":[%s]}".formatted(String.join(",", counts))))
                .andExpect(status().isOk());
    }

    /**
     * Count of one line: its theoretical quantity, or 0 with the reason OTHER when another test class left a negative
     * theoretical in the shared database (a count cannot be negative).
     */
    private String countOf(Object locationId, Map<String, Object> line, String soapCounted, String soapReason) {
        var isSoap = soap.toString().equals(line.get("articleId"));
        var counted = new BigDecimal(String.valueOf(line.get("theoretical")));
        String reason = null;
        if (isSoap && soapCounted != null) {
            counted = new BigDecimal(soapCounted);
            reason = soapReason;
        } else if (counted.signum() < 0) {
            counted = BigDecimal.ZERO;
            reason = "OTHER";
        }
        var reasonJson = reason == null ? "" : ",\"reason\":\"%s\"".formatted(reason);
        return "{\"locationId\":\"%s\",\"articleId\":\"%s\",\"counted\":%s%s}"
                .formatted(locationId, line.get("articleId"), counted.toPlainString(), reasonJson);
    }

    /** The reference (article code) of the test article, as the closing shows it. */
    private String reference() {
        return jdbcTemplate.queryForObject("select code from articles where id = ?", String.class, soap);
    }

    private String todayStatus() {
        var today = "(now() at time zone 'Indian/Antananarivo')::date";
        return jdbcTemplate.queryForObject(
                "select status from business_days where business_date = " + today, String.class);
    }

    private int closingJournalLines() {
        var count = jdbcTemplate.queryForObject(
                "select count(*) from journal_entries where resource_type = 'BUSINESS_DAY' and action = 'UPDATE' "
                        + "and description like 'Clôture de la journée du %'",
                Integer.class);
        return count == null ? 0 : count;
    }

    private int countRows() {
        var rows = jdbcTemplate.queryForObject("select count(*) from day_closing_counts", Integer.class);
        return rows == null ? 0 : rows;
    }

    /** Opens today's day if no other test class did (201), tolerates the one already open (409). */
    private void openToday() throws Exception {
        var status = mockMvc.perform(post("/api/v1/days")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[]}"))
                .andReturn()
                .getResponse()
                .getStatus();
        assertThat(status).isIn(201, 409);
    }

    private UUID channel(String code) {
        return jdbcTemplate.queryForObject("select id from sales_channels where code = ?", UUID.class, code);
    }
}
