-- liquibase formatted sql

-- changeset randy:2026_10_20-02-1
-- comment: JNL-02 - le journal trace l'annulation a chaud (CANCEL) et la contre-passation (REVERSE)
ALTER TABLE journal_entries DROP CONSTRAINT ck_journal_entries_action;
ALTER TABLE journal_entries
    ADD CONSTRAINT ck_journal_entries_action CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'CANCEL', 'REVERSE'));
-- rollback ALTER TABLE journal_entries DROP CONSTRAINT ck_journal_entries_action;
-- rollback ALTER TABLE journal_entries ADD CONSTRAINT ck_journal_entries_action CHECK (action IN ('CREATE', 'UPDATE', 'DELETE'));
