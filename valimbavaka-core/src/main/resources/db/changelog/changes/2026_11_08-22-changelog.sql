-- liquibase formatted sql

-- changeset randy:2026_11_08-22-1
-- comment: STK-06 - numero des receptions fournisseur (REC-000001, REC-000002...) attribue par le serveur
CREATE SEQUENCE supplier_receipt_number_seq START WITH 1 INCREMENT BY 1;
-- rollback DROP SEQUENCE supplier_receipt_number_seq;

-- changeset randy:2026_11_08-22-2
-- comment: STK-06 - reception d'une livraison fournisseur ; jamais supprimee, une erreur se contre-passe (REVERSED)
CREATE TABLE supplier_receipts
(
    id               UUID         NOT NULL,
    number           VARCHAR(20)  NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    supplier_id      UUID         NOT NULL,
    supplier_name    VARCHAR(150) NOT NULL,
    location_id      UUID         NOT NULL,
    restock_order_id UUID,
    note             VARCHAR(500),
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    created_by       UUID,
    updated_by       UUID,
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_supplier_receipts PRIMARY KEY (id),
    CONSTRAINT uq_supplier_receipts_number UNIQUE (number),
    CONSTRAINT fk_supplier_receipts_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id),
    CONSTRAINT fk_supplier_receipts_location FOREIGN KEY (location_id) REFERENCES stock_locations (id),
    CONSTRAINT fk_supplier_receipts_restock_order FOREIGN KEY (restock_order_id) REFERENCES restock_orders (id),
    CONSTRAINT ck_supplier_receipts_status CHECK (status IN ('VALIDATED', 'REVERSED'))
);
CREATE INDEX ix_supplier_receipts_created_at ON supplier_receipts (created_at);
CREATE INDEX ix_supplier_receipts_supplier_id ON supplier_receipts (supplier_id);
-- un bon de reapprovisionnement n'est repris que par une seule reception
CREATE UNIQUE INDEX uq_supplier_receipts_restock_order ON supplier_receipts (restock_order_id)
    WHERE restock_order_id IS NOT NULL;

CREATE TABLE supplier_receipt_lines
(
    supplier_receipt_id UUID           NOT NULL,
    line_number         INTEGER        NOT NULL,
    article_id          UUID           NOT NULL,
    article_code        VARCHAR(20)    NOT NULL,
    article_name        VARCHAR(120)   NOT NULL,
    quantity            NUMERIC(12, 3) NOT NULL,
    unit_cost           NUMERIC(14, 2) NOT NULL,
    CONSTRAINT pk_supplier_receipt_lines PRIMARY KEY (supplier_receipt_id, line_number),
    CONSTRAINT uq_supplier_receipt_lines_article UNIQUE (supplier_receipt_id, article_id),
    CONSTRAINT fk_supplier_receipt_lines_receipt FOREIGN KEY (supplier_receipt_id) REFERENCES supplier_receipts (id),
    CONSTRAINT fk_supplier_receipt_lines_article FOREIGN KEY (article_id) REFERENCES articles (id),
    CONSTRAINT ck_supplier_receipt_lines_quantity CHECK (quantity > 0),
    CONSTRAINT ck_supplier_receipt_lines_unit_cost CHECK (unit_cost >= 0)
);
CREATE INDEX ix_supplier_receipt_lines_article_id ON supplier_receipt_lines (article_id);
-- rollback DROP TABLE supplier_receipt_lines;
-- rollback DROP TABLE supplier_receipts;

-- changeset randy:2026_11_08-22-3
-- comment: STK-06 - permission d'enregistrer, de consulter et de contre-passer les receptions fournisseur
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'stocks.receive', 'Enregistrer et consulter les receptions fournisseur', now(), now());
-- rollback DELETE FROM permissions WHERE code = 'stocks.receive';

-- changeset randy:2026_11_08-22-4
-- comment: STK-06 - reception reservee a l'administrateur (user story)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'stocks.receive'
WHERE r.name = 'ADMIN';
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'stocks.receive');
