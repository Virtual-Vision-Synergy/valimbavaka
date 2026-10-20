-- liquibase formatted sql

-- changeset randy:2026_10_20-01-1
-- comment: SYS-02 - cles d'idempotence : un rejeu apres coupure reseau renvoie la reponse deja produite
CREATE TABLE idempotency_keys
(
    user_id         UUID        NOT NULL,
    idempotency_key UUID        NOT NULL,
    http_method     VARCHAR(10) NOT NULL,
    request_path    VARCHAR(500) NOT NULL,
    response_status INTEGER,
    response_body   TEXT,
    created_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_idempotency_keys PRIMARY KEY (user_id, idempotency_key)
);
CREATE INDEX ix_idempotency_keys_created_at ON idempotency_keys (created_at);
-- rollback DROP TABLE idempotency_keys;
