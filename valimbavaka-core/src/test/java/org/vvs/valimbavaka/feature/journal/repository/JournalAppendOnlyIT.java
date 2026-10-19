package org.vvs.valimbavaka.feature.journal.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.domain.JournalEntry;
import org.vvs.valimbavaka.feature.journal.domain.JournalEntry.Actor;
import org.vvs.valimbavaka.feature.journal.domain.JournalEntry.Subject;
import org.vvs.valimbavaka.support.IntegrationTest;

/** JNL-01 : la base elle-même refuse de modifier ou d'effacer une ligne du journal. */
@IntegrationTest
class JournalAppendOnlyIT {

    @Autowired
    private JournalEntryRepository sut;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void save_newEntry_isPersistedWithItsGeneratedId() {
        // Act
        var saved = sut.save(newEntry());

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(count(saved.getId())).isEqualTo(1);
    }

    @Test
    void update_existingEntry_isRejectedByTheDatabase() {
        // Arrange
        var id = sut.save(newEntry()).getId();

        // Act + Assert
        assertThatThrownBy(() ->
                        jdbcTemplate.update("update journal_entries set description = 'falsifié' where id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
    }

    @Test
    void delete_existingEntry_isRejectedByTheDatabase() {
        // Arrange
        var id = sut.save(newEntry()).getId();

        // Act + Assert
        assertThatThrownBy(() -> jdbcTemplate.update("delete from journal_entries where id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
        assertThat(count(id)).isEqualTo(1);
    }

    @Test
    void truncate_journal_isRejectedByTheDatabase() {
        // Act + Assert
        assertThatThrownBy(() -> jdbcTemplate.execute("truncate table journal_entries"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
    }

    private int count(UUID id) {
        return jdbcTemplate.queryForObject("select count(*) from journal_entries where id = ?", Integer.class, id);
    }

    private static JournalEntry newEntry() {
        return JournalEntry.of(
                Instant.now(),
                new Actor(UUID.randomUUID(), "Rasoa Rakoto"),
                JournalAction.CREATE,
                new Subject("USER", UUID.randomUUID(), "Création de test"));
    }
}
