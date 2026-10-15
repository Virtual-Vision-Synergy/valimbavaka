-- liquibase formatted sql

-- changeset mana:2026_10_15-01-1
-- comment: SYS-05 - roles metier Administrateur, Vendeuse, Livreur (corrige le seed ADMIN/USER de 2026_10_07-01-3)
UPDATE role SET name = 'ADMINISTRATEUR', description = 'Administrateur', updated_at = now() WHERE name = 'ADMIN';
INSERT INTO role (id, created_at, updated_at, name, description)
SELECT gen_random_uuid(), now(), now(), seed.name, seed.description
FROM (VALUES ('ADMINISTRATEUR', 'Administrateur'), ('VENDEUSE', 'Vendeuse'), ('LIVREUR', 'Livreur'))
         AS seed (name, description)
WHERE NOT EXISTS (SELECT 1 FROM role r WHERE r.name = seed.name);
UPDATE users SET role_id = (SELECT id FROM role WHERE name = 'VENDEUSE'), updated_at = now()
WHERE role_id IN (SELECT id FROM role WHERE name NOT IN ('ADMINISTRATEUR', 'VENDEUSE', 'LIVREUR'));
DELETE FROM role WHERE name NOT IN ('ADMINISTRATEUR', 'VENDEUSE', 'LIVREUR');
-- rollback INSERT INTO role (id, created_at, updated_at, name, description) VALUES (gen_random_uuid(), now(), now(), 'USER', 'Utilisateur standard');
-- rollback UPDATE users SET role_id = (SELECT id FROM role WHERE name = 'USER') WHERE role_id IN (SELECT id FROM role WHERE name IN ('VENDEUSE', 'LIVREUR'));
-- rollback DELETE FROM role WHERE name IN ('VENDEUSE', 'LIVREUR');
-- rollback UPDATE role SET name = 'ADMIN' WHERE name = 'ADMINISTRATEUR';

-- changeset mana:2026_10_15-01-2
-- comment: Table roles au pluriel et contraintes nommees selon la convention VVS (section 8.2)
ALTER TABLE role RENAME TO roles;
ALTER TABLE roles RENAME CONSTRAINT pk_role TO pk_roles;
ALTER TABLE roles RENAME CONSTRAINT uc_role_name TO uq_roles_name;
ALTER TABLE roles ADD CONSTRAINT ck_roles_name CHECK (name IN ('ADMINISTRATEUR', 'VENDEUSE', 'LIVREUR'));
ALTER TABLE users RENAME CONSTRAINT uc_users_email TO uq_users_email;
ALTER TABLE users RENAME CONSTRAINT fk_users_on_role TO fk_users_role_id;
ALTER INDEX idx_users_role_id RENAME TO ix_users_role_id;
-- rollback ALTER INDEX ix_users_role_id RENAME TO idx_users_role_id;
-- rollback ALTER TABLE users RENAME CONSTRAINT fk_users_role_id TO fk_users_on_role;
-- rollback ALTER TABLE users RENAME CONSTRAINT uq_users_email TO uc_users_email;
-- rollback ALTER TABLE roles DROP CONSTRAINT ck_roles_name;
-- rollback ALTER TABLE roles RENAME CONSTRAINT uq_roles_name TO uc_role_name;
-- rollback ALTER TABLE roles RENAME CONSTRAINT pk_roles TO pk_role;
-- rollback ALTER TABLE roles RENAME TO role;

-- changeset mana:2026_10_15-01-3
-- comment: Utilisateurs - booleens prefixes is_ et changement de mot de passe obligatoire (premier administrateur)
ALTER TABLE users RENAME COLUMN active TO is_active;
ALTER TABLE users ADD COLUMN is_password_change_required BOOLEAN NOT NULL DEFAULT FALSE;
-- rollback ALTER TABLE users DROP COLUMN is_password_change_required;
-- rollback ALTER TABLE users RENAME COLUMN is_active TO active;

-- changeset mana:2026_10_15-01-4
-- comment: Audit aligne sur la convention (TIMESTAMPTZ en UTC, auteur UUID) et verrou optimiste
ALTER TABLE roles
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN created_by TYPE UUID USING NULL,
    ALTER COLUMN updated_by TYPE UUID USING NULL,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE users
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN created_by TYPE UUID USING NULL,
    ALTER COLUMN updated_by TYPE UUID USING NULL,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
-- rollback ALTER TABLE users DROP COLUMN version, ALTER COLUMN created_by TYPE BIGINT USING NULL, ALTER COLUMN updated_by TYPE BIGINT USING NULL, ALTER COLUMN created_at TYPE TIMESTAMP(6) USING created_at AT TIME ZONE 'UTC', ALTER COLUMN updated_at TYPE TIMESTAMP(6) USING updated_at AT TIME ZONE 'UTC';
-- rollback ALTER TABLE roles DROP COLUMN version, ALTER COLUMN created_by TYPE BIGINT USING NULL, ALTER COLUMN updated_by TYPE BIGINT USING NULL, ALTER COLUMN created_at TYPE TIMESTAMP(6) USING created_at AT TIME ZONE 'UTC', ALTER COLUMN updated_at TYPE TIMESTAMP(6) USING updated_at AT TIME ZONE 'UTC';

-- changeset mana:2026_10_15-01-5
-- comment: SYS-05 - referentiel des canaux de vente (liste a confirmer avec le metier)
CREATE TABLE sales_channels
(
    id         UUID         NOT NULL,
    code       VARCHAR(50)  NOT NULL,
    label      VARCHAR(100) NOT NULL,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    version    BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_sales_channels PRIMARY KEY (id),
    CONSTRAINT uq_sales_channels_code UNIQUE (code)
);
INSERT INTO sales_channels (id, code, label)
VALUES (gen_random_uuid(), 'BOUTIQUE', 'Boutique'),
       (gen_random_uuid(), 'FACEBOOK', 'Facebook'),
       (gen_random_uuid(), 'TELEPHONE', 'Téléphone'),
       (gen_random_uuid(), 'LIVRAISON', 'Livraison');
-- rollback DROP TABLE sales_channels;

-- changeset mana:2026_10_15-01-6
-- comment: SYS-05 - referentiel des gammes de produits
CREATE TABLE product_ranges
(
    id         UUID         NOT NULL,
    code       VARCHAR(50)  NOT NULL,
    label      VARCHAR(100) NOT NULL,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    version    BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_product_ranges PRIMARY KEY (id),
    CONSTRAINT uq_product_ranges_code UNIQUE (code)
);
INSERT INTO product_ranges (id, code, label)
VALUES (gen_random_uuid(), 'ZEZIKA', 'Zezika'),
       (gen_random_uuid(), 'BOTANIKA', 'Botanika'),
       (gen_random_uuid(), 'SOLIKA', 'Solika');
-- rollback DROP TABLE product_ranges;

-- changeset mana:2026_10_15-01-7
-- comment: SYS-05 - referentiel des emplacements de stock
CREATE TABLE stock_locations
(
    id         UUID         NOT NULL,
    code       VARCHAR(50)  NOT NULL,
    label      VARCHAR(100) NOT NULL,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    version    BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_stock_locations PRIMARY KEY (id),
    CONSTRAINT uq_stock_locations_code UNIQUE (code)
);
INSERT INTO stock_locations (id, code, label)
VALUES (gen_random_uuid(), 'ENTREPOT', 'Entrepôt'),
       (gen_random_uuid(), 'BOUTIQUE', 'Boutique'),
       (gen_random_uuid(), 'LIVRAISON', 'Livraison'),
       (gen_random_uuid(), 'SALON', 'Salon');
-- rollback DROP TABLE stock_locations;
