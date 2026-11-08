-- liquibase formatted sql

-- changeset randy:2026_11_08-92-1
-- comment: SRV-02 - sequence du numero de prestation (PRE-0001, PRE-0002...) attribue par le serveur
CREATE SEQUENCE service_record_number_seq START WITH 1 INCREMENT BY 1;
-- rollback DROP SEQUENCE service_record_number_seq;

-- changeset randy:2026_11_08-92-2
-- comment: SRV-02 - prestations salon : copie du service (nom, duree, prix), vendeuse, canal et emplacement salon
CREATE TABLE service_records
(
    id               UUID           NOT NULL,
    number           VARCHAR(20)    NOT NULL,
    salon_service_id UUID           NOT NULL,
    service_name     VARCHAR(100)   NOT NULL,
    duration_minutes INTEGER        NOT NULL,
    price            NUMERIC(14, 2) NOT NULL,
    customer_name    VARCHAR(100),
    seller_id        UUID           NOT NULL,
    seller_name      VARCHAR(255)   NOT NULL,
    sales_channel_id UUID           NOT NULL,
    location_id      UUID           NOT NULL,
    note             VARCHAR(255),
    created_at       TIMESTAMPTZ    NOT NULL,
    updated_at       TIMESTAMPTZ    NOT NULL,
    created_by       UUID,
    updated_by       UUID,
    version          BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT pk_service_records PRIMARY KEY (id),
    CONSTRAINT uq_service_records_number UNIQUE (number),
    CONSTRAINT fk_service_records_salon_service_id FOREIGN KEY (salon_service_id) REFERENCES salon_services (id),
    CONSTRAINT fk_service_records_seller_id FOREIGN KEY (seller_id) REFERENCES users (id),
    CONSTRAINT fk_service_records_sales_channel_id FOREIGN KEY (sales_channel_id) REFERENCES sales_channels (id),
    CONSTRAINT fk_service_records_location_id FOREIGN KEY (location_id) REFERENCES stock_locations (id),
    CONSTRAINT ck_service_records_price CHECK (price >= 0),
    CONSTRAINT ck_service_records_duration_minutes CHECK (duration_minutes BETWEEN 1 AND 600)
);
CREATE INDEX ix_service_records_created_at ON service_records (created_at DESC);
CREATE INDEX ix_service_records_salon_service_id ON service_records (salon_service_id);
CREATE INDEX ix_service_records_seller_id ON service_records (seller_id);
-- rollback DROP TABLE service_records;

-- changeset randy:2026_11_08-92-3
-- comment: SRV-02 - produits consommes par une prestation (code et nom copies, quantite dans l'unite de l'article)
CREATE TABLE service_record_products
(
    service_record_id UUID           NOT NULL,
    line_no           INTEGER        NOT NULL,
    article_id        UUID           NOT NULL,
    article_code      VARCHAR(20)    NOT NULL,
    article_name      VARCHAR(120)   NOT NULL,
    quantity          NUMERIC(12, 3) NOT NULL,
    CONSTRAINT pk_service_record_products PRIMARY KEY (service_record_id, line_no),
    CONSTRAINT fk_service_record_products_service_record_id FOREIGN KEY (service_record_id) REFERENCES service_records (id),
    CONSTRAINT fk_service_record_products_article_id FOREIGN KEY (article_id) REFERENCES articles (id),
    CONSTRAINT uq_service_record_products_article UNIQUE (service_record_id, article_id),
    CONSTRAINT ck_service_record_products_quantity CHECK (quantity > 0)
);
CREATE INDEX ix_service_record_products_article_id ON service_record_products (article_id);
-- rollback DROP TABLE service_record_products;

-- changeset randy:2026_11_08-92-4
-- comment: SRV-02 - la prestation est saisie par la vendeuse ou l'administrateur (lecture via services.read)
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'service-records.create', 'Enregistrer une prestation salon', now(), now());
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'service-records.create'
WHERE r.name IN ('ADMIN', 'SELLER');
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'service-records.create');
-- rollback DELETE FROM permissions WHERE code = 'service-records.create';
