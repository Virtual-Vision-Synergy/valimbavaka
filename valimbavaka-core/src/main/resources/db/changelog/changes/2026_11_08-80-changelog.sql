-- liquibase formatted sql

-- changeset randy:2026_11_08-80-1
-- comment: INV-01 - numero lisible des sessions d'inventaire (INV-0001), jamais reutilise
CREATE SEQUENCE inventory_session_number_seq START WITH 1 INCREMENT BY 1;
-- rollback DROP SEQUENCE inventory_session_number_seq;

-- changeset randy:2026_11_08-80-2
-- comment: INV-01 - session d'inventaire : emplacement, perimetre (tout ou une gamme), responsable, date de comptage
CREATE TABLE inventory_sessions
(
    id               UUID         NOT NULL,
    number           VARCHAR(20)  NOT NULL,
    location_id      UUID         NOT NULL,
    scope            VARCHAR(20)  NOT NULL,
    range_id         UUID,
    responsible_id   UUID         NOT NULL,
    responsible_name VARCHAR(201) NOT NULL,
    count_date       DATE         NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    closed_at        TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    created_by       UUID,
    updated_by       UUID,
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_inventory_sessions PRIMARY KEY (id),
    CONSTRAINT uq_inventory_sessions_number UNIQUE (number),
    CONSTRAINT fk_inventory_sessions_location FOREIGN KEY (location_id) REFERENCES stock_locations (id),
    CONSTRAINT fk_inventory_sessions_range FOREIGN KEY (range_id) REFERENCES product_ranges (id),
    CONSTRAINT fk_inventory_sessions_responsible FOREIGN KEY (responsible_id) REFERENCES users (id),
    CONSTRAINT ck_inventory_sessions_scope CHECK ((scope = 'ALL' AND range_id IS NULL)
        OR (scope = 'RANGE' AND range_id IS NOT NULL)),
    CONSTRAINT ck_inventory_sessions_status CHECK (status IN ('IN_PROGRESS', 'CLOSED', 'CANCELLED'))
);
-- Une seule session en cours par emplacement (filet de securite du controle applicatif, 409 en cas de course).
CREATE UNIQUE INDEX uq_inventory_sessions_location_in_progress
    ON inventory_sessions (location_id) WHERE status = 'IN_PROGRESS';
CREATE INDEX ix_inventory_sessions_status_created_at ON inventory_sessions (status, created_at);
-- rollback DROP TABLE inventory_sessions;

-- changeset randy:2026_11_08-80-3
-- comment: INV-01 - lignes a compter, figees a l'ouverture ; comptage (INV-02) et motif (INV-03) a remplir ensuite
CREATE TABLE inventory_lines
(
    id                UUID           NOT NULL,
    session_id        UUID           NOT NULL,
    article_id        UUID           NOT NULL,
    article_code      VARCHAR(20)    NOT NULL,
    article_name      VARCHAR(150)   NOT NULL,
    barcode           VARCHAR(50),
    expected_quantity NUMERIC(12, 3),
    counted_quantity  NUMERIC(12, 3),
    reason            VARCHAR(30),
    counted_at        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ    NOT NULL,
    updated_at        TIMESTAMPTZ    NOT NULL,
    created_by        UUID,
    updated_by        UUID,
    version           BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT pk_inventory_lines PRIMARY KEY (id),
    CONSTRAINT uq_inventory_lines_session_article UNIQUE (session_id, article_id),
    CONSTRAINT fk_inventory_lines_session FOREIGN KEY (session_id) REFERENCES inventory_sessions (id),
    CONSTRAINT fk_inventory_lines_article FOREIGN KEY (article_id) REFERENCES articles (id),
    CONSTRAINT ck_inventory_lines_counted CHECK (counted_quantity IS NULL OR counted_quantity >= 0),
    CONSTRAINT ck_inventory_lines_reason CHECK (reason IS NULL OR reason IN ('BREAKAGE', 'LOSS', 'ENTRY_ERROR',
                                                                              'UNDECLARED_SHOOTING', 'OTHER'))
);
-- rollback DROP TABLE inventory_lines;

-- changeset randy:2026_11_08-80-4
-- comment: INV-01 - permission d'ouvrir une session d'inventaire, reservee a l'administrateur
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'inventory.create', 'Ouvrir une session d''inventaire', now(), now());
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'inventory.create'
WHERE r.name = 'ADMIN';
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'inventory.create');
-- rollback DELETE FROM permissions WHERE code = 'inventory.create';
