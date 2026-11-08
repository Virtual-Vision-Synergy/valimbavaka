-- liquibase formatted sql

-- changeset randy:2026_11_08-91-1
-- comment: SRV-01 - catalogue des services salon (nom, duree, prix, taux de commission facultatif)
CREATE TABLE salon_services
(
    id               UUID           NOT NULL,
    name             VARCHAR(100)   NOT NULL,
    duration_minutes INTEGER        NOT NULL,
    price            NUMERIC(14, 2) NOT NULL,
    commission_rate  NUMERIC(5, 2),
    is_active        BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ    NOT NULL,
    updated_at       TIMESTAMPTZ    NOT NULL,
    created_by       UUID,
    updated_by       UUID,
    version          BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT pk_salon_services PRIMARY KEY (id),
    CONSTRAINT ck_salon_services_duration_minutes CHECK (duration_minutes BETWEEN 1 AND 600),
    CONSTRAINT ck_salon_services_price CHECK (price >= 0),
    CONSTRAINT ck_salon_services_commission_rate CHECK (commission_rate BETWEEN 0 AND 100)
);
CREATE UNIQUE INDEX uq_salon_services_name ON salon_services (lower(name));
-- rollback DROP TABLE salon_services;

-- changeset randy:2026_11_08-91-2
-- comment: SRV-01 - produits habituellement consommes par un service salon (article, quantite dans son unite)
CREATE TABLE salon_service_products
(
    salon_service_id UUID           NOT NULL,
    article_id       UUID           NOT NULL,
    quantity         NUMERIC(12, 3) NOT NULL,
    CONSTRAINT pk_salon_service_products PRIMARY KEY (salon_service_id, article_id),
    CONSTRAINT fk_salon_service_products_salon_service_id FOREIGN KEY (salon_service_id) REFERENCES salon_services (id),
    CONSTRAINT fk_salon_service_products_article_id FOREIGN KEY (article_id) REFERENCES articles (id),
    CONSTRAINT ck_salon_service_products_quantity CHECK (quantity > 0)
);
CREATE INDEX ix_salon_service_products_article_id ON salon_service_products (article_id);
-- rollback DROP TABLE salon_service_products;

-- changeset randy:2026_11_08-91-3
-- comment: SRV-01 - creation et modification du catalogue salon reservees a l'administrateur (services.read existe deja)
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'services.create', 'Creer un service salon', now(), now()),
       (gen_random_uuid(), 'services.update', 'Modifier un service salon', now(), now());
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code IN ('services.create', 'services.update')
WHERE r.name = 'ADMIN';
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code IN ('services.create', 'services.update'));
-- rollback DELETE FROM permissions WHERE code IN ('services.create', 'services.update');
