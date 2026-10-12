-- liquibase formatted sql

-- changeset vvs:2026_10_12-01-1
-- comment: Rôles métier (SYS-05) : Administrateur, Vendeuse, Livreur. Corrige les rôles ADMIN/USER de judic:2026_10_07-01-3.
UPDATE role
SET name        = 'Administrateur',
    description = 'Gère l''équipe, les référentiels et la configuration',
    updated_at  = now()
WHERE name = 'ADMIN';

INSERT INTO role (id, created_at, updated_at, name, description)
SELECT gen_random_uuid(), now(), now(), 'Administrateur', 'Gère l''équipe, les référentiels et la configuration'
WHERE NOT EXISTS (SELECT 1 FROM role WHERE name = 'Administrateur');

INSERT INTO role (id, created_at, updated_at, name, description)
VALUES (gen_random_uuid(), now(), now(), 'Vendeuse', 'Enregistre les ventes et les commandes'),
       (gen_random_uuid(), now(), now(), 'Livreur', 'Effectue les livraisons');

-- Les comptes éventuellement créés avec le rôle générique USER deviennent Vendeuse.
UPDATE users
SET role_id    = (SELECT id FROM role WHERE name = 'Vendeuse'),
    updated_at = now()
WHERE role_id = (SELECT id FROM role WHERE name = 'USER');

DELETE FROM role WHERE name = 'USER';
-- rollback INSERT INTO role (id, created_at, updated_at, name, description) VALUES (gen_random_uuid(), now(), now(), 'USER', 'Utilisateur standard');
-- rollback UPDATE users SET role_id = (SELECT id FROM role WHERE name = 'USER') WHERE role_id IN (SELECT id FROM role WHERE name IN ('Vendeuse', 'Livreur'));
-- rollback DELETE FROM role WHERE name IN ('Vendeuse', 'Livreur');
-- rollback UPDATE role SET name = 'ADMIN', description = 'Administrateur' WHERE name = 'Administrateur';

-- changeset vvs:2026_10_12-01-2
-- comment: Changement de mot de passe obligatoire à la prochaine connexion (compte admin initial)
ALTER TABLE users
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
-- rollback ALTER TABLE users DROP COLUMN must_change_password;

-- changeset vvs:2026_10_12-01-3
-- comment: Référentiel des canaux de vente (feature referential)
CREATE TABLE sales_channel
(
    id         UUID                           NOT NULL,
    created_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,
    code       VARCHAR(50)                    NOT NULL,
    name       VARCHAR(100)                   NOT NULL,
    CONSTRAINT pk_sales_channel PRIMARY KEY (id),
    CONSTRAINT uc_sales_channel_code UNIQUE (code),
    CONSTRAINT uc_sales_channel_name UNIQUE (name)
);

INSERT INTO sales_channel (id, created_at, updated_at, code, name)
VALUES (gen_random_uuid(), now(), now(), 'BOUTIQUE', 'Boutique'),
       (gen_random_uuid(), now(), now(), 'FACEBOOK', 'Facebook'),
       (gen_random_uuid(), now(), now(), 'TELEPHONE', 'Téléphone'),
       (gen_random_uuid(), now(), now(), 'LIVRAISON', 'Livraison');
-- rollback DROP TABLE sales_channel;

-- changeset vvs:2026_10_12-01-4
-- comment: Référentiel des gammes de produits (feature referential)
CREATE TABLE product_range
(
    id         UUID                           NOT NULL,
    created_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,
    code       VARCHAR(50)                    NOT NULL,
    name       VARCHAR(100)                   NOT NULL,
    CONSTRAINT pk_product_range PRIMARY KEY (id),
    CONSTRAINT uc_product_range_code UNIQUE (code),
    CONSTRAINT uc_product_range_name UNIQUE (name)
);

INSERT INTO product_range (id, created_at, updated_at, code, name)
VALUES (gen_random_uuid(), now(), now(), 'ZEZIKA', 'Zezika'),
       (gen_random_uuid(), now(), now(), 'BOTANIKA', 'Botanika'),
       (gen_random_uuid(), now(), now(), 'SOLIKA', 'Solika');
-- rollback DROP TABLE product_range;

-- changeset vvs:2026_10_12-01-5
-- comment: Référentiel des emplacements de stock (feature referential)
CREATE TABLE stock_location
(
    id         UUID                           NOT NULL,
    created_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,
    code       VARCHAR(50)                    NOT NULL,
    name       VARCHAR(100)                   NOT NULL,
    CONSTRAINT pk_stock_location PRIMARY KEY (id),
    CONSTRAINT uc_stock_location_code UNIQUE (code),
    CONSTRAINT uc_stock_location_name UNIQUE (name)
);

INSERT INTO stock_location (id, created_at, updated_at, code, name)
VALUES (gen_random_uuid(), now(), now(), 'ENTREPOT', 'Entrepôt'),
       (gen_random_uuid(), now(), now(), 'BOUTIQUE', 'Boutique'),
       (gen_random_uuid(), now(), now(), 'LIVRAISON', 'Livraison'),
       (gen_random_uuid(), now(), now(), 'SALON', 'Salon');
-- rollback DROP TABLE stock_location;
