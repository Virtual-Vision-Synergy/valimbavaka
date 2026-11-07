-- liquibase formatted sql

-- changeset randy:2026_11_08-61-1
-- comment: SHO-01 - sequence du numero de sortie shooting (SHO-0001, SHO-0002...) attribue par le serveur
CREATE SEQUENCE shooting_number_seq START WITH 1 INCREMENT BY 1;
-- rollback DROP SEQUENCE shooting_number_seq;

-- changeset randy:2026_11_08-61-2
-- comment: SHO-01 - sorties shooting : produits sortis d'un emplacement pour une seance photo (statut OUT, puis RETURNED avec SHO-02)
CREATE TABLE shootings
(
    id                 UUID         NOT NULL,
    number             VARCHAR(20)  NOT NULL,
    location_id        UUID         NOT NULL,
    taken_by_name      VARCHAR(255) NOT NULL,
    expected_return_at TIMESTAMPTZ  NOT NULL,
    note               VARCHAR(255),
    status             VARCHAR(20)  NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL,
    updated_at         TIMESTAMPTZ  NOT NULL,
    created_by         UUID,
    updated_by         UUID,
    version            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_shootings PRIMARY KEY (id),
    CONSTRAINT uq_shootings_number UNIQUE (number),
    CONSTRAINT fk_shootings_location FOREIGN KEY (location_id) REFERENCES stock_locations (id),
    CONSTRAINT ck_shootings_status CHECK (status IN ('OUT', 'RETURNED'))
);
CREATE INDEX ix_shootings_location_id ON shootings (location_id);
CREATE INDEX ix_shootings_status ON shootings (status);
-- rollback DROP TABLE shootings;

-- changeset randy:2026_11_08-61-3
-- comment: SHO-01 - articles d'une sortie shooting (code et nom copies a la sortie, chaque article une seule fois)
CREATE TABLE shooting_items
(
    shooting_id  UUID           NOT NULL,
    line_no      INTEGER        NOT NULL,
    article_id   UUID           NOT NULL,
    article_code VARCHAR(20)    NOT NULL,
    article_name VARCHAR(120)   NOT NULL,
    quantity     NUMERIC(12, 3) NOT NULL,
    CONSTRAINT pk_shooting_items PRIMARY KEY (shooting_id, line_no),
    CONSTRAINT fk_shooting_items_shooting FOREIGN KEY (shooting_id) REFERENCES shootings (id),
    CONSTRAINT fk_shooting_items_article FOREIGN KEY (article_id) REFERENCES articles (id),
    CONSTRAINT uq_shooting_items_article UNIQUE (shooting_id, article_id),
    CONSTRAINT ck_shooting_items_quantity CHECK (quantity > 0)
);
CREATE INDEX ix_shooting_items_article_id ON shooting_items (article_id);
-- rollback DROP TABLE shooting_items;

-- changeset randy:2026_11_08-61-4
-- comment: SHO-01 - permission d'enregistrer une sortie shooting
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'shootings.create', 'Enregistrer une sortie shooting', now(), now());
-- rollback DELETE FROM permissions WHERE code = 'shootings.create';

-- changeset randy:2026_11_08-61-5
-- comment: SHO-01 - la sortie shooting est saisie par la vendeuse ou l'administrateur (lecture via services.read)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'shootings.create'
WHERE r.name IN ('ADMIN', 'SELLER');
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'shootings.create');
