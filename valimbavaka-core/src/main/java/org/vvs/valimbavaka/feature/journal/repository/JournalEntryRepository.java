package org.vvs.valimbavaka.feature.journal.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import org.vvs.valimbavaka.feature.journal.domain.JournalEntry;

/** Étend {@link Repository} et non JpaRepository : ni suppression ni modification n'est exposée (append-only). */
public interface JournalEntryRepository extends Repository<JournalEntry, UUID> {

    JournalEntry save(JournalEntry entry);

    /** Recherche insensible à la casse sur l'auteur, le type de ressource et le libellé ; vide = tout. */
    @Query("""
            select e from JournalEntry e
            where lower(e.actorName) like lower(concat('%', :search, '%'))
               or lower(e.resourceType) like lower(concat('%', :search, '%'))
               or lower(e.description) like lower(concat('%', :search, '%'))
            """)
    Page<JournalEntry> search(String search, Pageable pageable);

    /** Même recherche, limitée aux entrées dont occurredAt est dans [from, to[ (un jour UTC). */
    @Query("""
            select e from JournalEntry e
            where e.occurredAt >= :from and e.occurredAt < :to
              and (lower(e.actorName) like lower(concat('%', :search, '%'))
               or lower(e.resourceType) like lower(concat('%', :search, '%'))
               or lower(e.description) like lower(concat('%', :search, '%')))
            """)
    Page<JournalEntry> searchBetween(String search, Instant from, Instant to, Pageable pageable);
}
