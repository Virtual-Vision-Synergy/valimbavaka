-- liquibase formatted sql

-- changeset randy:2026_11_08-11-1
-- comment: VTE-01 - numeros des ventes (VTE-0001...), attribues par le serveur ; des trous sont possibles, un numero n'est jamais reutilise
CREATE SEQUENCE sale_number_seq START WITH 1 INCREMENT BY 1;
-- rollback DROP SEQUENCE sale_number_seq;

-- changeset randy:2026_11_08-11-2
-- comment: VTE-01 - vente multicanal (canal, vendeuse, mode de reglement, journee) et ses lignes a prix figes ; jamais supprimee (JNL-02)
CREATE TABLE sales
(
    id                UUID           NOT NULL,
    number            VARCHAR(20)    NOT NULL,
    business_day_id   UUID           NOT NULL,
    business_date     DATE           NOT NULL,
    sales_channel_id  UUID           NOT NULL,
    stock_location_id UUID           NOT NULL,
    seller_id         UUID           NOT NULL,
    payment_method    VARCHAR(20)    NOT NULL,
    status            VARCHAR(20)    NOT NULL,
    subtotal          NUMERIC(19, 2) NOT NULL,
    discount_amount   NUMERIC(19, 2) NOT NULL DEFAULT 0,
    total             NUMERIC(19, 2) NOT NULL,
    created_at        TIMESTAMPTZ    NOT NULL,
    updated_at        TIMESTAMPTZ    NOT NULL,
    created_by        UUID,
    updated_by        UUID,
    version           BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT pk_sales PRIMARY KEY (id),
    CONSTRAINT uq_sales_number UNIQUE (number),
    CONSTRAINT fk_sales_business_day FOREIGN KEY (business_day_id) REFERENCES business_days (id),
    CONSTRAINT fk_sales_sales_channel FOREIGN KEY (sales_channel_id) REFERENCES sales_channels (id),
    CONSTRAINT fk_sales_stock_location FOREIGN KEY (stock_location_id) REFERENCES stock_locations (id),
    CONSTRAINT fk_sales_seller FOREIGN KEY (seller_id) REFERENCES users (id),
    CONSTRAINT ck_sales_payment_method CHECK (payment_method IN ('CASH', 'MVOLA', 'ORANGE_MONEY', 'CARD', 'ON_DELIVERY')),
    CONSTRAINT ck_sales_status CHECK (status IN ('VALIDATED', 'CANCELLED', 'REVERSED')),
    CONSTRAINT ck_sales_subtotal CHECK (subtotal >= 0),
    CONSTRAINT ck_sales_discount_amount CHECK (discount_amount >= 0 AND discount_amount <= subtotal),
    CONSTRAINT ck_sales_total CHECK (total = subtotal - discount_amount)
);
CREATE INDEX ix_sales_business_date ON sales (business_date);
CREATE INDEX ix_sales_seller_id_business_date ON sales (seller_id, business_date);
CREATE INDEX ix_sales_sales_channel_id ON sales (sales_channel_id);

CREATE TABLE sale_lines
(
    sale_id     UUID           NOT NULL,
    line_number INTEGER        NOT NULL,
    article_id  UUID           NOT NULL,
    quantity    NUMERIC(12, 3) NOT NULL,
    unit_price  NUMERIC(19, 2) NOT NULL,
    unit_cost   NUMERIC(19, 2),
    line_total  NUMERIC(19, 2) NOT NULL,
    CONSTRAINT pk_sale_lines PRIMARY KEY (sale_id, line_number),
    CONSTRAINT uq_sale_lines_article UNIQUE (sale_id, article_id),
    CONSTRAINT fk_sale_lines_sale FOREIGN KEY (sale_id) REFERENCES sales (id),
    CONSTRAINT fk_sale_lines_article FOREIGN KEY (article_id) REFERENCES articles (id),
    CONSTRAINT ck_sale_lines_quantity CHECK (quantity > 0),
    CONSTRAINT ck_sale_lines_unit_price CHECK (unit_price >= 0),
    CONSTRAINT ck_sale_lines_unit_cost CHECK (unit_cost IS NULL OR unit_cost >= 0),
    CONSTRAINT ck_sale_lines_line_total CHECK (line_total >= 0)
);
CREATE INDEX ix_sale_lines_article_id ON sale_lines (article_id);
-- rollback DROP TABLE sale_lines;
-- rollback DROP TABLE sales;

-- changeset randy:2026_11_08-11-3
-- comment: VTE-01 - permission d'enregistrer une vente (sales.read existait deja, AUTH-01)
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'sales.create', 'Enregistrer une vente', now(), now());
-- rollback DELETE FROM permissions WHERE code = 'sales.create';

-- changeset randy:2026_11_08-11-4
-- comment: VTE-01 - la vente est saisie par la vendeuse ou par l'administrateur
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'sales.create'
WHERE r.name IN ('ADMIN', 'SELLER');
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'sales.create');
