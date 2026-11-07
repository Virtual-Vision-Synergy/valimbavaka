-- liquibase formatted sql

-- changeset randy:2026_11_08-21-1
-- comment: TRF-03 - numeros des bons de transfert (TRF-0001...) et des retours de stock (RET-0001...), attribues par le serveur
CREATE SEQUENCE transfer_number_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE stock_return_number_seq START WITH 1 INCREMENT BY 1;
-- rollback DROP SEQUENCE stock_return_number_seq;
-- rollback DROP SEQUENCE transfer_number_seq;

-- changeset randy:2026_11_08-21-2
-- comment: TRF-03 - bon de transfert entre deux emplacements (retour, transfert manuel, transferts du matin) et ses lignes
CREATE TABLE transfers
(
    id               UUID         NOT NULL,
    number           VARCHAR(20)  NOT NULL,
    origin           VARCHAR(20)  NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    from_location_id UUID         NOT NULL,
    to_location_id   UUID         NOT NULL,
    note             VARCHAR(500),
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    created_by       UUID,
    updated_by       UUID,
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_transfers PRIMARY KEY (id),
    CONSTRAINT uq_transfers_number UNIQUE (number),
    CONSTRAINT fk_transfers_from_location FOREIGN KEY (from_location_id) REFERENCES stock_locations (id),
    CONSTRAINT fk_transfers_to_location FOREIGN KEY (to_location_id) REFERENCES stock_locations (id),
    CONSTRAINT ck_transfers_origin CHECK (origin IN ('MORNING', 'MANUAL', 'RETURN')),
    CONSTRAINT ck_transfers_status CHECK (status IN ('VALIDATED', 'RETURN')),
    CONSTRAINT ck_transfers_route CHECK (from_location_id <> to_location_id)
);
CREATE INDEX ix_transfers_created_at ON transfers (created_at);

CREATE TABLE transfer_lines
(
    transfer_id UUID           NOT NULL,
    line_number INTEGER        NOT NULL,
    article_id  UUID           NOT NULL,
    quantity    NUMERIC(12, 3) NOT NULL,
    CONSTRAINT pk_transfer_lines PRIMARY KEY (transfer_id, line_number),
    CONSTRAINT uq_transfer_lines_article UNIQUE (transfer_id, article_id),
    CONSTRAINT fk_transfer_lines_transfer FOREIGN KEY (transfer_id) REFERENCES transfers (id),
    CONSTRAINT fk_transfer_lines_article FOREIGN KEY (article_id) REFERENCES articles (id),
    CONSTRAINT ck_transfer_lines_quantity CHECK (quantity > 0)
);
CREATE INDEX ix_transfer_lines_article_id ON transfer_lines (article_id);
-- rollback DROP TABLE transfer_lines;
-- rollback DROP TABLE transfers;

-- changeset randy:2026_11_08-21-3
-- comment: TRF-03 - permission d'enregistrer un retour de stock
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'transfers.return', 'Enregistrer un retour de stock', now(), now());
-- rollback DELETE FROM permissions WHERE code = 'transfers.return';

-- changeset randy:2026_11_08-21-4
-- comment: TRF-03 - le retour est saisi par la vendeuse (articles non ecoules) ou par l'administrateur
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'transfers.return'
WHERE r.name IN ('ADMIN', 'SELLER');
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'transfers.return');
