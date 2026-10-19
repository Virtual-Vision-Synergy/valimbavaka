-- liquibase formatted sql

-- changeset randy:2026_10_19-02-1
-- comment: JNL-01 - journal append-only des operations (auteur nominatif, horodatage, ressource concernee)
CREATE TABLE journal_entries
(
    id            UUID         NOT NULL,
    occurred_at   TIMESTAMPTZ  NOT NULL,
    actor_id      UUID,
    actor_name    VARCHAR(255) NOT NULL,
    action        VARCHAR(20)  NOT NULL,
    resource_type VARCHAR(50)  NOT NULL,
    resource_id   UUID         NOT NULL,
    description   VARCHAR(500) NOT NULL,
    CONSTRAINT pk_journal_entries PRIMARY KEY (id),
    CONSTRAINT ck_journal_entries_action CHECK (action IN ('CREATE', 'UPDATE', 'DELETE'))
);
CREATE INDEX ix_journal_entries_occurred_at ON journal_entries (occurred_at DESC);
CREATE INDEX ix_journal_entries_actor_id ON journal_entries (actor_id);
CREATE INDEX ix_journal_entries_resource ON journal_entries (resource_type, resource_id);
-- rollback DROP TABLE journal_entries;

-- changeset randy:2026_10_19-02-2 splitStatements:false
-- comment: JNL-01 - append-only garanti par la base : UPDATE, DELETE et TRUNCATE sont refuses
CREATE FUNCTION reject_journal_entries_change() RETURNS TRIGGER AS
$$
BEGIN
    RAISE EXCEPTION 'journal_entries est append-only : % interdit', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_journal_entries_append_only
    BEFORE UPDATE OR DELETE ON journal_entries
    FOR EACH ROW EXECUTE FUNCTION reject_journal_entries_change();

CREATE TRIGGER trg_journal_entries_no_truncate
    BEFORE TRUNCATE ON journal_entries
    FOR EACH STATEMENT EXECUTE FUNCTION reject_journal_entries_change();
-- rollback DROP TRIGGER trg_journal_entries_no_truncate ON journal_entries;
-- rollback DROP TRIGGER trg_journal_entries_append_only ON journal_entries;
-- rollback DROP FUNCTION reject_journal_entries_change();

-- changeset randy:2026_10_19-02-3
-- comment: JNL-01 - permission de consultation du journal, reservee a l'administrateur
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'journal.read', 'Consulter le journal des operations', now(), now());
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'journal.read'
WHERE r.name = 'ADMIN';
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'journal.read');
-- rollback DELETE FROM permissions WHERE code = 'journal.read';
