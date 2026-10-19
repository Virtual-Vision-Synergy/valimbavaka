package org.vvs.valimbavaka.feature.journal.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.UuidGenerator;
import org.jspecify.annotations.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Ligne du journal : écrite une fois, jamais modifiée (la base refuse aussi UPDATE et DELETE). N'étend pas
 * {@code BaseEntity} : pas de version ni de champs de mise à jour, l'auteur et l'instant sont les siens.
 */
@Entity
@Immutable
@Table(name = "journal_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JournalEntry {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    /** Vide pour une opération système (aucun compte authentifié). */
    @Column(name = "actor_id", updatable = false)
    private @Nullable UUID actorId;

    /** Nom au moment de l'opération : reste lisible même si le compte est renommé ou supprimé. */
    @Column(name = "actor_name", nullable = false, updatable = false)
    private String actorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, updatable = false, length = 20)
    private JournalAction action;

    @Column(name = "resource_type", nullable = false, updatable = false, length = 50)
    private String resourceType;

    /** Référence par identifiant : pas d'association JPA vers une autre feature (convention §2.3). */
    @Column(name = "resource_id", nullable = false, updatable = false)
    private UUID resourceId;

    @Column(name = "description", nullable = false, updatable = false, length = 500)
    private String description;

    public static JournalEntry of(Instant occurredAt, Actor actor, JournalAction action, Subject subject) {
        var entry = new JournalEntry();
        entry.occurredAt = occurredAt;
        entry.actorId = actor.id();
        entry.actorName = actor.name();
        entry.action = action;
        entry.resourceType = subject.resourceType();
        entry.resourceId = subject.resourceId();
        entry.description = subject.description();
        return entry;
    }

    /** Auteur de l'opération. */
    public record Actor(@Nullable UUID id, String name) {}

    /** Ressource concernée et libellé de l'opération. */
    public record Subject(String resourceType, UUID resourceId, String description) {}
}
