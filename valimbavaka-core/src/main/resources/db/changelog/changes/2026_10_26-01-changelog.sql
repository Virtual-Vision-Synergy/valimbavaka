-- liquibase formatted sql

-- changeset randy:2026_10_26-01-1
-- comment: ART-01 - sequence du code article (ART-0001, ART-0002...) attribue par le serveur
CREATE SEQUENCE article_code_seq START WITH 1 INCREMENT BY 1;
-- rollback DROP SEQUENCE article_code_seq;

-- changeset randy:2026_10_26-01-2
-- comment: ART-01 - table des articles (fiche article complete)
CREATE TABLE articles
(
    id          UUID           NOT NULL,
    code        VARCHAR(20)    NOT NULL,
    name        VARCHAR(120)   NOT NULL,
    barcode     VARCHAR(13),
    type        VARCHAR(20)    NOT NULL,
    range_id    UUID,
    unit        VARCHAR(20)    NOT NULL,
    cost        NUMERIC(14, 2),
    price       NUMERIC(14, 2),
    threshold   NUMERIC(12, 3) NOT NULL DEFAULT 0,
    supplier_id UUID,
    is_active   BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ    NOT NULL,
    updated_at  TIMESTAMPTZ    NOT NULL,
    created_by  UUID,
    updated_by  UUID,
    version     BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT pk_articles PRIMARY KEY (id),
    CONSTRAINT uq_articles_code UNIQUE (code),
    CONSTRAINT fk_articles_range FOREIGN KEY (range_id) REFERENCES product_ranges (id),
    CONSTRAINT fk_articles_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id),
    CONSTRAINT ck_articles_type CHECK (type IN ('RAW_MATERIAL', 'SEMI_FINISHED', 'FINISHED')),
    CONSTRAINT ck_articles_unit CHECK (unit IN ('PIECE', 'KILOGRAM', 'LITER')),
    CONSTRAINT ck_articles_cost CHECK (cost IS NULL OR cost >= 0),
    CONSTRAINT ck_articles_price CHECK (price IS NULL OR price >= 0),
    CONSTRAINT ck_articles_threshold CHECK (threshold >= 0)
);
CREATE UNIQUE INDEX uq_articles_barcode ON articles (barcode) WHERE barcode IS NOT NULL;
CREATE INDEX ix_articles_supplier_id ON articles (supplier_id);
CREATE INDEX ix_articles_range_id ON articles (range_id);
-- rollback DROP TABLE articles;

-- changeset randy:2026_10_26-01-3
-- comment: ART-01 - permissions de creation et de modification des articles
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'articles.create', 'Creer un article', now(), now()),
       (gen_random_uuid(), 'articles.update', 'Modifier un article', now(), now());
-- rollback DELETE FROM permissions WHERE code IN ('articles.create', 'articles.update');

-- changeset randy:2026_10_26-01-4
-- comment: ART-01 - permissions d'ecriture des articles reservees a l'administrateur (articles.read reste donnee a la vendeuse)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code IN ('articles.create', 'articles.update')
WHERE r.name = 'ADMIN';
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code IN ('articles.create', 'articles.update'));
