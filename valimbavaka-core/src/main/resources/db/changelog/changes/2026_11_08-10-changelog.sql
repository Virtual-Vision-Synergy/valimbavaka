-- liquibase formatted sql

-- changeset randy:2026_11_08-10-1
-- comment: JR-01 - journee de vente (une par date), ouverte par la repartition du matin ; CLOSED reserve a la cloture (JR-06)
CREATE TABLE business_days
(
    id            UUID        NOT NULL,
    business_date DATE        NOT NULL,
    status        VARCHAR(20) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    created_by    UUID,
    updated_by    UUID,
    version       BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_business_days PRIMARY KEY (id),
    CONSTRAINT uq_business_days_business_date UNIQUE (business_date),
    CONSTRAINT ck_business_days_status CHECK (status IN ('OPEN', 'CLOSED'))
);

CREATE TABLE business_day_transfers
(
    business_day_id UUID           NOT NULL,
    line_number     INTEGER        NOT NULL,
    transfer_id     UUID           NOT NULL,
    transfer_number VARCHAR(20)    NOT NULL,
    location_id     UUID           NOT NULL,
    line_count      INTEGER        NOT NULL,
    quantity        NUMERIC(14, 3) NOT NULL,
    CONSTRAINT pk_business_day_transfers PRIMARY KEY (business_day_id, line_number),
    CONSTRAINT uq_business_day_transfers_transfer UNIQUE (transfer_id),
    CONSTRAINT uq_business_day_transfers_location UNIQUE (business_day_id, location_id),
    CONSTRAINT fk_business_day_transfers_day FOREIGN KEY (business_day_id) REFERENCES business_days (id),
    CONSTRAINT fk_business_day_transfers_transfer FOREIGN KEY (transfer_id) REFERENCES transfers (id),
    CONSTRAINT fk_business_day_transfers_location FOREIGN KEY (location_id) REFERENCES stock_locations (id),
    CONSTRAINT ck_business_day_transfers_line_count CHECK (line_count > 0),
    CONSTRAINT ck_business_day_transfers_quantity CHECK (quantity > 0)
);
-- rollback DROP TABLE business_day_transfers;
-- rollback DROP TABLE business_days;

-- changeset randy:2026_11_08-10-2
-- comment: JR-01 - permission d'ouvrir la journee (repartition du matin)
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'days.open', 'Ouvrir la journée et répartir le stock du matin', now(), now());
-- rollback DELETE FROM permissions WHERE code = 'days.open';

-- changeset randy:2026_11_08-10-3
-- comment: JR-01 - la repartition du matin est reservee a l'administrateur
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'days.open'
WHERE r.name = 'ADMIN';
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'days.open');
