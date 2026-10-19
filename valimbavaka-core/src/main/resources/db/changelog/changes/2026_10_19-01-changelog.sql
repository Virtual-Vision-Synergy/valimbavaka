-- liquibase formatted sql

-- changeset mana:2026_10_19-01-1
-- comment: SYS-06 - auteurs d'audit de la table dev alignes sur l'identifiant utilisateur (UUID)
ALTER TABLE dev
    ALTER COLUMN created_by TYPE UUID USING NULL,
    ALTER COLUMN updated_by TYPE UUID USING NULL;
-- rollback ALTER TABLE dev ALTER COLUMN created_by TYPE BIGINT USING NULL, ALTER COLUMN updated_by TYPE BIGINT USING NULL;
