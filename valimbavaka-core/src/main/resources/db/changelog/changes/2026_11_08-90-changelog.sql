-- liquibase formatted sql

-- changeset randy:2026_11_08-90-1
-- comment: COM-01 - regles de commission par canal de vente (nom, canal, taux, base de calcul) ; une seule regle active par canal
CREATE TABLE commission_rules
(
    id               UUID          NOT NULL,
    name             VARCHAR(100)  NOT NULL,
    sales_channel_id UUID          NOT NULL,
    rate             NUMERIC(5, 2) NOT NULL,
    calculation_base VARCHAR(20)   NOT NULL,
    is_active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ   NOT NULL,
    updated_at       TIMESTAMPTZ   NOT NULL,
    created_by       UUID,
    updated_by       UUID,
    version          BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_commission_rules PRIMARY KEY (id),
    CONSTRAINT fk_commission_rules_sales_channel_id FOREIGN KEY (sales_channel_id) REFERENCES sales_channels (id),
    CONSTRAINT ck_commission_rules_rate CHECK (rate BETWEEN 0 AND 100),
    CONSTRAINT ck_commission_rules_calculation_base CHECK (calculation_base IN ('REVENUE', 'MARGIN'))
);
CREATE UNIQUE INDEX uq_commission_rules_name ON commission_rules (lower(name));
CREATE UNIQUE INDEX uq_commission_rules_sales_channel_id_active ON commission_rules (sales_channel_id) WHERE is_active;
-- rollback DROP TABLE commission_rules;

-- changeset randy:2026_11_08-90-2
-- comment: COM-01 - permissions de lecture, creation et modification des regles de commission
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'commission-rules.read', 'Consulter les regles de commission', now(), now()),
       (gen_random_uuid(), 'commission-rules.create', 'Creer une regle de commission', now(), now()),
       (gen_random_uuid(), 'commission-rules.update', 'Modifier une regle de commission', now(), now());
-- rollback DELETE FROM permissions WHERE code IN ('commission-rules.read', 'commission-rules.create', 'commission-rules.update');

-- changeset randy:2026_11_08-90-3
-- comment: COM-01 - regles de commission reservees a l'administrateur
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code IN ('commission-rules.read', 'commission-rules.create', 'commission-rules.update')
WHERE r.name = 'ADMIN';
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code IN ('commission-rules.read', 'commission-rules.create', 'commission-rules.update'));
