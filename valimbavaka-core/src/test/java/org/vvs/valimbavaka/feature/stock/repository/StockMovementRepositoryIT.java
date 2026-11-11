package org.vvs.valimbavaka.feature.stock.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import org.vvs.valimbavaka.feature.stock.domain.StockMovement;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementCommand;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.support.IntegrationTest;

/** STK-01: sums per article and location, and the database itself refuses to alter or erase a movement. */
@IntegrationTest
class StockMovementRepositoryIT {

    @Autowired
    private StockMovementRepository sut;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void balances_movementsInTwoLocations_groupsByArticleAndLocation() {
        // Arrange
        var articleId = insertArticle();
        var warehouse = location("WAREHOUSE");
        var store = location("STORE");
        save(articleId, warehouse, StockMovementType.RECEIPT, "30");
        save(articleId, warehouse, StockMovementType.TRANSFER, "-10");
        save(articleId, store, StockMovementType.TRANSFER, "10");

        // Act
        var balances = sut.balances(List.of(articleId));

        // Assert
        assertThat(balances)
                .hasSize(2)
                .anySatisfy(b -> {
                    assertThat(b.locationId()).isEqualTo(warehouse);
                    assertThat(b.quantity()).isEqualByComparingTo("20");
                })
                .anySatisfy(b -> {
                    assertThat(b.locationId()).isEqualTo(store);
                    assertThat(b.quantity()).isEqualByComparingTo("10");
                });
    }

    @Test
    void balance_neverMoved_returnsZero() {
        assertThat(sut.balance(insertArticle(), location("WAREHOUSE"))).isEqualByComparingTo("0");
    }

    @Test
    void save_zeroQuantity_isRejectedByTheDatabase() {
        // Arrange
        var articleId = insertArticle();
        var warehouse = location("WAREHOUSE");

        // Act + Assert
        assertThatThrownBy(() -> save(articleId, warehouse, StockMovementType.INVENTORY, "0"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void update_existingMovement_isRejectedByTheDatabase() {
        // Arrange
        var id = save(insertArticle(), location("WAREHOUSE"), StockMovementType.RECEIPT, "5");

        // Act + Assert
        assertThatThrownBy(() -> jdbcTemplate.update("update stock_movements set quantity = 500 where id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
    }

    @Test
    void delete_existingMovement_isRejectedByTheDatabase() {
        // Arrange
        var id = save(insertArticle(), location("WAREHOUSE"), StockMovementType.RECEIPT, "5");

        // Act + Assert
        assertThatThrownBy(() -> jdbcTemplate.update("delete from stock_movements where id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
    }

    @Test
    void truncate_table_isRejectedByTheDatabase() {
        assertThatThrownBy(() -> jdbcTemplate.execute("truncate table stock_movements"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
    }

    @Test
    void totalsByType_movementsOfSeveralTypes_sumsEachTypeSeparately() {
        // Arrange
        var articleId = insertArticle();
        var store = location("STORE");
        var other = location("DELIVERY");
        save(articleId, store, StockMovementType.TRANSFER, "10");
        save(articleId, store, StockMovementType.TRANSFER, "-4");
        save(articleId, store, StockMovementType.SALE, "-2");
        save(articleId, other, StockMovementType.TRANSFER, "7");
        var now = Instant.now();

        // Act
        var totals =
                sut.totalsByType(now.minus(Duration.ofHours(1)), now.plus(Duration.ofHours(1)), List.of(store)).stream()
                        .filter(total -> total.articleId().equals(articleId))
                        .toList();

        // Assert
        assertThat(totals)
                .hasSize(2)
                .anySatisfy(total -> {
                    assertThat(total.type()).isEqualTo(StockMovementType.TRANSFER);
                    assertThat(total.quantity()).isEqualByComparingTo("6");
                })
                .anySatisfy(total -> {
                    assertThat(total.type()).isEqualTo(StockMovementType.SALE);
                    assertThat(total.quantity()).isEqualByComparingTo("-2");
                });
    }

    @Test
    void totalsByType_periodEndingBeforeTheMovement_leavesItOut() {
        // Arrange
        var articleId = insertArticle();
        var store = location("STORE");
        save(articleId, store, StockMovementType.TRANSFER, "10");
        var now = Instant.now();

        // Act
        var totals = sut.totalsByType(now.minus(Duration.ofDays(2)), now.minus(Duration.ofDays(1)), List.of(store));

        // Assert
        assertThat(totals).noneMatch(total -> total.articleId().equals(articleId));
    }

    @Test
    void balancesBefore_movementsOnBothSidesOfTheInstant_countsOnlyTheEarlierOnes() {
        // Arrange
        var articleId = insertArticle();
        var store = location("STORE");
        var now = Instant.now();
        saveAt(articleId, store, StockMovementType.TRANSFER, "5", now.minus(Duration.ofDays(2)));
        save(articleId, store, StockMovementType.TRANSFER, "3");

        // Act
        var yesterday = sut.balancesBefore(now.minus(Duration.ofDays(1)), List.of(store));
        var later = sut.balancesBefore(now.plus(Duration.ofHours(1)), List.of(store));

        // Assert
        assertThat(quantityOf(yesterday, articleId)).isEqualByComparingTo("5");
        assertThat(quantityOf(later, articleId)).isEqualByComparingTo("8");
    }

    @Test
    void balancesBefore_movementsCancellingOut_omitsTheArticle() {
        // Arrange
        var articleId = insertArticle();
        var store = location("STORE");
        save(articleId, store, StockMovementType.TRANSFER, "4");
        save(articleId, store, StockMovementType.TRANSFER, "-4");

        // Act
        var balances = sut.balancesBefore(Instant.now().plus(Duration.ofHours(1)), List.of(store));

        // Assert
        assertThat(balances).noneMatch(balance -> balance.articleId().equals(articleId));
    }

    private BigDecimal quantityOf(List<StockBalance> balances, UUID articleId) {
        return balances.stream()
                .filter(balance -> balance.articleId().equals(articleId))
                .map(StockBalance::quantity)
                .findFirst()
                .orElseThrow();
    }

    private void saveAt(UUID articleId, UUID locationId, StockMovementType type, String quantity, Instant at) {
        jdbcTemplate.update(
                "insert into stock_movements (id, article_id, location_id, type, quantity, occurred_at) "
                        + "values (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(),
                articleId,
                locationId,
                type.name(),
                new BigDecimal(quantity),
                Timestamp.from(at));
    }

    private UUID save(UUID articleId, UUID locationId, StockMovementType type, String quantity) {
        var command = new StockMovementCommand(articleId, locationId, type, new BigDecimal(quantity), null);
        return sut.save(StockMovement.of(command)).getId();
    }

    private UUID location(String code) {
        return jdbcTemplate.queryForObject("select id from stock_locations where code = ?", UUID.class, code);
    }

    private UUID insertArticle() {
        var id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into articles (id, code, name, type, unit, threshold, is_active, created_at, updated_at) "
                        + "values (?, ?, ?, 'FINISHED', 'PIECE', 0, true, now(), now())",
                id,
                "IT-" + id.toString().substring(0, 8),
                "Article " + id);
        return id;
    }
}
