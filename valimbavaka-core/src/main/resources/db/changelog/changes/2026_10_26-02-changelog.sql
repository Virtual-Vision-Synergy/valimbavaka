-- liquibase formatted sql

-- changeset randy:2026_10_26-02-1
-- comment: SYS-12 - empreinte du corps : une cle reutilisee avec un contenu different est refusee
ALTER TABLE idempotency_keys
    ADD COLUMN request_hash VARCHAR(64);
-- rollback ALTER TABLE idempotency_keys DROP COLUMN request_hash;
