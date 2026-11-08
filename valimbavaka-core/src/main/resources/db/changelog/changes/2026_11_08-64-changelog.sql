-- liquibase formatted sql

-- changeset randy:2026_11_08-64-1
-- comment: LIV-01 - numeros des commandes a livrer (LIV-0001, LIV-0002...) attribues par le serveur
CREATE SEQUENCE delivery_order_number_seq START WITH 1 INCREMENT BY 1;
-- rollback DROP SEQUENCE delivery_order_number_seq;

-- changeset randy:2026_11_08-64-2
-- comment: LIV-01 - commande a livrer (client, adresse, livreur, jour de tournee, statut) et ses lignes au prix du jour
CREATE TABLE delivery_orders
(
    id             UUID           NOT NULL,
    number         VARCHAR(20)    NOT NULL,
    status         VARCHAR(20)    NOT NULL,
    customer_name  VARCHAR(120)   NOT NULL,
    customer_phone VARCHAR(30)    NOT NULL,
    address        VARCHAR(255)   NOT NULL,
    district       VARCHAR(80),
    driver_id      UUID           NOT NULL,
    scheduled_date DATE           NOT NULL,
    stop_number    INTEGER        NOT NULL,
    amount         NUMERIC(14, 2) NOT NULL,
    note           VARCHAR(500),
    created_at     TIMESTAMPTZ    NOT NULL,
    updated_at     TIMESTAMPTZ    NOT NULL,
    created_by     UUID,
    updated_by     UUID,
    version        BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT pk_delivery_orders PRIMARY KEY (id),
    CONSTRAINT uq_delivery_orders_number UNIQUE (number),
    CONSTRAINT fk_delivery_orders_driver FOREIGN KEY (driver_id) REFERENCES users (id),
    CONSTRAINT ck_delivery_orders_status CHECK (status IN ('TO_PREPARE', 'ON_ROUND', 'DELIVERED', 'FAILED',
                                                           'CANCELLED')),
    CONSTRAINT ck_delivery_orders_stop CHECK (stop_number > 0),
    CONSTRAINT ck_delivery_orders_amount CHECK (amount >= 0)
);
CREATE INDEX ix_delivery_orders_tour ON delivery_orders (driver_id, scheduled_date);
CREATE INDEX ix_delivery_orders_created_at ON delivery_orders (created_at);
CREATE INDEX ix_delivery_orders_status ON delivery_orders (status);

CREATE TABLE delivery_order_lines
(
    delivery_order_id UUID           NOT NULL,
    line_number       INTEGER        NOT NULL,
    article_id        UUID           NOT NULL,
    quantity          NUMERIC(12, 3) NOT NULL,
    unit_price        NUMERIC(12, 2) NOT NULL,
    CONSTRAINT pk_delivery_order_lines PRIMARY KEY (delivery_order_id, line_number),
    CONSTRAINT uq_delivery_order_lines_article UNIQUE (delivery_order_id, article_id),
    CONSTRAINT fk_delivery_order_lines_order FOREIGN KEY (delivery_order_id) REFERENCES delivery_orders (id),
    CONSTRAINT fk_delivery_order_lines_article FOREIGN KEY (article_id) REFERENCES articles (id),
    CONSTRAINT ck_delivery_order_lines_quantity CHECK (quantity > 0),
    CONSTRAINT ck_delivery_order_lines_unit_price CHECK (unit_price >= 0)
);
CREATE INDEX ix_delivery_order_lines_article_id ON delivery_order_lines (article_id);
-- rollback DROP TABLE delivery_order_lines;
-- rollback DROP TABLE delivery_orders;

-- changeset randy:2026_11_08-64-3
-- comment: LIV-01 - permission de creer une commande a livrer (administrateur et vendeuse)
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'deliveries.create', 'Creer une commande a livrer', now(), now());
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'deliveries.create'
WHERE r.name IN ('ADMIN', 'SELLER');
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'deliveries.create');
-- rollback DELETE FROM permissions WHERE code = 'deliveries.create';

-- changeset randy:2026_11_08-64-4
-- comment: LIV-01 - la vendeuse consulte les commandes a livrer qu'elle saisit (deliveries.read existait pour ADMIN et DELIVERY_DRIVER)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'deliveries.read'
WHERE r.name = 'SELLER';
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'deliveries.read') AND role_id IN (SELECT id FROM roles WHERE name = 'SELLER');
