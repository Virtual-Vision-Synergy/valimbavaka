-- liquibase formatted sql

-- changeset randy:2026_11_08-40-1
-- comment: PRD-01 - sequence du numero de lot (LOT-00001, LOT-00002...) attribue par le serveur
CREATE SEQUENCE production_batch_number_seq START WITH 1 INCREMENT BY 1;
-- rollback DROP SEQUENCE production_batch_number_seq;

-- changeset randy:2026_11_08-40-2
-- comment: PRD-01 - lot de production : produit obtenu, quantite, vendeuse responsable, statut (PRD-03)
CREATE TABLE production_batches
(
    id                UUID           NOT NULL,
    number            VARCHAR(20)    NOT NULL,
    output_article_id UUID           NOT NULL,
    quantity          NUMERIC(12, 3) NOT NULL,
    operator_id       UUID           NOT NULL,
    status            VARCHAR(20)    NOT NULL,
    created_at        TIMESTAMPTZ    NOT NULL,
    updated_at        TIMESTAMPTZ    NOT NULL,
    created_by        UUID,
    updated_by        UUID,
    version           BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT pk_production_batches PRIMARY KEY (id),
    CONSTRAINT uq_production_batches_number UNIQUE (number),
    CONSTRAINT fk_production_batches_output_article FOREIGN KEY (output_article_id) REFERENCES articles (id),
    CONSTRAINT fk_production_batches_operator FOREIGN KEY (operator_id) REFERENCES users (id),
    CONSTRAINT ck_production_batches_quantity CHECK (quantity > 0),
    CONSTRAINT ck_production_batches_status CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'COMPLETED'))
);
CREATE INDEX ix_production_batches_created_at ON production_batches (created_at);
-- rollback DROP TABLE production_batches;

-- changeset randy:2026_11_08-40-3
-- comment: PRD-01 - entrants d'un lot (matieres premieres et semi-finis), un article au plus une fois par lot
CREATE TABLE production_batch_inputs
(
    batch_id   UUID           NOT NULL,
    line_no    INTEGER        NOT NULL,
    article_id UUID           NOT NULL,
    quantity   NUMERIC(12, 3) NOT NULL,
    CONSTRAINT pk_production_batch_inputs PRIMARY KEY (batch_id, line_no),
    CONSTRAINT fk_production_batch_inputs_batch FOREIGN KEY (batch_id) REFERENCES production_batches (id),
    CONSTRAINT fk_production_batch_inputs_article FOREIGN KEY (article_id) REFERENCES articles (id),
    CONSTRAINT uq_production_batch_inputs_article UNIQUE (batch_id, article_id),
    CONSTRAINT ck_production_batch_inputs_quantity CHECK (quantity > 0)
);
CREATE INDEX ix_production_batch_inputs_article_id ON production_batch_inputs (article_id);
-- rollback DROP TABLE production_batch_inputs;

-- changeset randy:2026_11_08-40-4
-- comment: PRD-01 - permission de creer un lot de production
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'production.create', 'Creer un lot de production', now(), now());
-- rollback DELETE FROM permissions WHERE code = 'production.create';

-- changeset randy:2026_11_08-40-5
-- comment: PRD-01 - la vendeuse consulte et cree des lots ; l'administrateur cree aussi (il lit deja tout)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'production.create'
WHERE r.name IN ('ADMIN', 'SELLER')
UNION
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'production.read'
WHERE r.name = 'SELLER';
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'production.create');
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'production.read') AND role_id IN (SELECT id FROM roles WHERE name = 'SELLER');
