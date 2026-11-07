-- liquibase formatted sql

-- changeset randy:2026_11_08-20-1
-- comment: STK-05 - sequence du numero de bon de reapprovisionnement (BR-000001, BR-000002...) attribue par le serveur
CREATE SEQUENCE restock_order_number_seq START WITH 1 INCREMENT BY 1;
-- rollback DROP SEQUENCE restock_order_number_seq;

-- changeset randy:2026_11_08-20-2
-- comment: STK-05 - en-tete du bon de reapprovisionnement ; STK-06 le passe a RECEIVED a la reception
CREATE TABLE restock_orders
(
    id         UUID        NOT NULL,
    reference  VARCHAR(20) NOT NULL,
    status     VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version    BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_restock_orders PRIMARY KEY (id),
    CONSTRAINT uq_restock_orders_reference UNIQUE (reference),
    CONSTRAINT ck_restock_orders_status CHECK (status IN ('PENDING', 'RECEIVED'))
);
CREATE INDEX ix_restock_orders_created_at ON restock_orders (created_at);
-- rollback DROP TABLE restock_orders;

-- changeset randy:2026_11_08-20-3
-- comment: STK-05 - lignes du bon : copie de l'alerte de seuil (STK-04) et du fournisseur (FRS-04) au moment de la generation
CREATE TABLE restock_order_lines
(
    id                     UUID           NOT NULL,
    restock_order_id       UUID           NOT NULL,
    line_number            INTEGER        NOT NULL,
    article_id             UUID           NOT NULL,
    article_code           VARCHAR(20)    NOT NULL,
    article_name           VARCHAR(120)   NOT NULL,
    unit                   VARCHAR(20)    NOT NULL,
    stock_quantity         NUMERIC(12, 3) NOT NULL,
    threshold              NUMERIC(12, 3) NOT NULL,
    quantity               NUMERIC(12, 3) NOT NULL,
    supplier_id            UUID,
    supplier_name          VARCHAR(150),
    lead_time_days         INTEGER,
    expected_delivery_date DATE,
    CONSTRAINT pk_restock_order_lines PRIMARY KEY (id),
    CONSTRAINT fk_restock_order_lines_order FOREIGN KEY (restock_order_id) REFERENCES restock_orders (id),
    CONSTRAINT fk_restock_order_lines_article FOREIGN KEY (article_id) REFERENCES articles (id),
    CONSTRAINT fk_restock_order_lines_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id),
    CONSTRAINT uq_restock_order_lines_number UNIQUE (restock_order_id, line_number),
    CONSTRAINT uq_restock_order_lines_article UNIQUE (restock_order_id, article_id),
    CONSTRAINT ck_restock_order_lines_unit CHECK (unit IN ('PIECE', 'KILOGRAM', 'LITER')),
    CONSTRAINT ck_restock_order_lines_quantity CHECK (quantity > 0),
    CONSTRAINT ck_restock_order_lines_stock CHECK (stock_quantity >= 0)
);
CREATE INDEX ix_restock_order_lines_article_id ON restock_order_lines (article_id);
-- rollback DROP TABLE restock_order_lines;

-- changeset randy:2026_11_08-20-4
-- comment: STK-05 - permission de generation et de lecture des bons de reapprovisionnement
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'stocks.restock', 'Generer et consulter les bons de reapprovisionnement', now(), now());
-- rollback DELETE FROM permissions WHERE code = 'stocks.restock';

-- changeset randy:2026_11_08-20-5
-- comment: STK-05 - bons de reapprovisionnement reserves a l'administrateur (stocks.read reste donnee a la vendeuse)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'stocks.restock'
WHERE r.name = 'ADMIN';
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'stocks.restock');
