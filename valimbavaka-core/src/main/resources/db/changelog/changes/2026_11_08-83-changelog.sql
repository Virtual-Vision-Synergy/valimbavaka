-- liquibase formatted sql

-- changeset randy:2026_11_08-83-1
-- comment: COM-02 - commission calculee automatiquement pour chaque vente et chaque prestation creditee a une vendeuse (decision PO du 07/11/2026 : le taux le plus precis gagne, sans cumul)
CREATE TABLE commissions
(
    id                 UUID           NOT NULL,
    source_type        VARCHAR(20)    NOT NULL,
    source_id          UUID           NOT NULL,
    source_number      VARCHAR(20)    NOT NULL,
    seller_id          UUID           NOT NULL,
    seller_name        VARCHAR(255)   NOT NULL,
    sales_channel_id   UUID           NOT NULL,
    salon_service_id   UUID,
    business_date      DATE           NOT NULL,
    revenue            NUMERIC(19, 2) NOT NULL,
    base_amount        NUMERIC(19, 2) NOT NULL,
    calculation_base   VARCHAR(20)    NOT NULL,
    rate               NUMERIC(5, 2)  NOT NULL,
    rate_source        VARCHAR(20)    NOT NULL,
    commission_rule_id UUID,
    amount             NUMERIC(19, 2) NOT NULL,
    created_at         TIMESTAMPTZ    NOT NULL,
    updated_at         TIMESTAMPTZ    NOT NULL,
    created_by         UUID,
    updated_by         UUID,
    version            BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT pk_commissions PRIMARY KEY (id),
    CONSTRAINT uq_commissions_source UNIQUE (source_type, source_id),
    CONSTRAINT fk_commissions_seller_id FOREIGN KEY (seller_id) REFERENCES users (id),
    CONSTRAINT fk_commissions_sales_channel_id FOREIGN KEY (sales_channel_id) REFERENCES sales_channels (id),
    CONSTRAINT fk_commissions_salon_service_id FOREIGN KEY (salon_service_id) REFERENCES salon_services (id),
    CONSTRAINT fk_commissions_commission_rule_id FOREIGN KEY (commission_rule_id) REFERENCES commission_rules (id),
    CONSTRAINT ck_commissions_source_type CHECK (source_type IN ('SALE', 'SERVICE_RECORD')),
    CONSTRAINT ck_commissions_calculation_base CHECK (calculation_base IN ('REVENUE', 'MARGIN')),
    CONSTRAINT ck_commissions_rate CHECK (rate BETWEEN 0 AND 100),
    CONSTRAINT ck_commissions_rate_source CHECK (rate_source IN ('SALON_SERVICE', 'SALES_CHANNEL', 'SELLER', 'NONE')),
    CONSTRAINT ck_commissions_amounts CHECK (revenue >= 0 AND base_amount >= 0 AND amount >= 0)
);
CREATE INDEX ix_commissions_business_date ON commissions (business_date DESC);
CREATE INDEX ix_commissions_seller_id_business_date ON commissions (seller_id, business_date);
-- rollback DROP TABLE commissions;

-- changeset randy:2026_11_08-83-2
-- comment: COM-02 - consultation des commissions de toutes les vendeuses, reservee a l'administrateur (commissions.read reste celle de la vendeuse)
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'commissions.read-all', 'Consulter les commissions de toutes les vendeuses', now(), now());
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'commissions.read-all'
WHERE r.name = 'ADMIN';
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'commissions.read-all');
-- rollback DELETE FROM permissions WHERE code = 'commissions.read-all';
