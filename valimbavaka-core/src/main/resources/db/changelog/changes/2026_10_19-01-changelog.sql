-- liquibase formatted sql

-- changeset randy:2026_10_19-01-1
-- comment: AUTH-01 - permissions et association role/permissions (le role du compte determine les droits)
CREATE TABLE permissions
(
    id          UUID         NOT NULL,
    code        VARCHAR(100) NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    created_by  UUID,
    updated_by  UUID,
    version     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_permissions PRIMARY KEY (id),
    CONSTRAINT uq_permissions_code UNIQUE (code)
);
CREATE TABLE role_permissions
(
    role_id       UUID NOT NULL,
    permission_id UUID NOT NULL,
    CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role_id FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission_id FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
);
CREATE INDEX ix_role_permissions_permission_id ON role_permissions (permission_id);
-- rollback DROP TABLE role_permissions;
-- rollback DROP TABLE permissions;

-- changeset randy:2026_10_19-01-2
-- comment: AUTH-01 - donnees de reference des permissions (une lecture par module + gestion des utilisateurs)
INSERT INTO permissions (id, code, description, created_at, updated_at)
SELECT gen_random_uuid(), seed.code, seed.description, now(), now()
FROM (VALUES ('dashboard.read', 'Consulter le tableau de bord'),
             ('suppliers.read', 'Consulter les fournisseurs'),
             ('articles.read', 'Consulter les articles'),
             ('stocks.read', 'Consulter les stocks'),
             ('production.read', 'Consulter la production'),
             ('transfers.read', 'Consulter les transferts'),
             ('inventory.read', 'Consulter l''inventaire'),
             ('sales.read', 'Consulter les ventes'),
             ('deliveries.read', 'Consulter les livraisons'),
             ('promotions.read', 'Consulter les promotions'),
             ('commissions.read', 'Consulter les commissions'),
             ('services.read', 'Consulter les services'),
             ('reports.read', 'Consulter les rapports'),
             ('referentials.read', 'Consulter les referentiels metier'),
             ('roles.read', 'Consulter les roles'),
             ('users.read', 'Consulter les utilisateurs'),
             ('users.create', 'Creer un utilisateur'),
             ('users.update', 'Modifier un utilisateur'),
             ('users.delete', 'Supprimer un utilisateur')) AS seed (code, description);
-- rollback DELETE FROM permissions;

-- changeset randy:2026_10_19-01-3
-- comment: AUTH-01 - permissions de chaque role (ADMIN = tout ; SELLER = ventes et catalogue ; DELIVERY_DRIVER = livraisons)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON r.name = 'ADMIN'
UNION
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code IN ('dashboard.read', 'articles.read', 'stocks.read', 'sales.read',
                                          'promotions.read', 'commissions.read', 'services.read',
                                          'referentials.read')
WHERE r.name = 'SELLER'
UNION
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code IN ('dashboard.read', 'deliveries.read')
WHERE r.name = 'DELIVERY_DRIVER';
-- rollback DELETE FROM role_permissions;

-- changeset randy:2026_10_19-01-4
-- comment: AUTH-01 - verrouillage de compte apres echecs de connexion consecutifs
ALTER TABLE users
    ADD COLUMN failed_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN locked_until    TIMESTAMPTZ;
-- rollback ALTER TABLE users DROP COLUMN locked_until, DROP COLUMN failed_attempts;

-- changeset randy:2026_10_19-01-5
-- comment: AUTH-01 - jetons de rafraichissement opaques (stockes haches, rotation, famille revocable)
CREATE TABLE refresh_tokens
(
    id         UUID         NOT NULL,
    user_id    UUID         NOT NULL,
    family_id  UUID         NOT NULL,
    token_hash VARCHAR(64)  NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    is_revoked BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX ix_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX ix_refresh_tokens_family_id ON refresh_tokens (family_id);
-- rollback DROP TABLE refresh_tokens;

-- changeset randy:2026_10_19-01-6
-- comment: AUTH-01 - champs d'audit et verrou optimiste communs (BaseEntity) sur refresh_tokens
ALTER TABLE refresh_tokens
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN created_by UUID,
    ADD COLUMN updated_by UUID,
    ADD COLUMN version    BIGINT      NOT NULL DEFAULT 0;
-- rollback ALTER TABLE refresh_tokens DROP COLUMN version, DROP COLUMN updated_by, DROP COLUMN created_by, DROP COLUMN updated_at;
