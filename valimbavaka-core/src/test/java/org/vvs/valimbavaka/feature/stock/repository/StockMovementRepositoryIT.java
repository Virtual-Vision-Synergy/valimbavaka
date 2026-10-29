package org.vvs.valimbavaka.feature.stock.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
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
