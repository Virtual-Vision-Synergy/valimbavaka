--liquibase formatted sql
-- changeset randy:2026_10_21-01-1
-- comment: FRS-01 - table fournisseurs
CREATE TABLE suppliers (
    id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    type VARCHAR(20) NOT NULL,
    contact_name VARCHAR(150),
    phone VARCHAR(50),
    email VARCHAR(255),
    address VARCHAR(500),
    notes VARCHAR(2000),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_suppliers PRIMARY KEY (id),
    CONSTRAINT ck_suppliers_type CHECK (type IN ('LOCAL', 'FOREIGN'))
);
CREATE UNIQUE INDEX uq_suppliers_name ON suppliers (lower(name));
-- rollback DROP TABLE suppliers;

-- changeset randy:2026_10_21-01-2
-- comment: FRS-01 - permissions de creation et de modification des fournisseurs
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'suppliers.create', 'Creer un fournisseur', now(), now()),
       (gen_random_uuid(), 'suppliers.update', 'Modifier un fournisseur', now(), now());
-- rollback DELETE FROM permissions WHERE code IN ('suppliers.create', 'suppliers.update');

-- changeset randy:2026_10_21-01-3
-- comment: FRS-01 - permissions d'ecriture des fournisseurs reservees a l'administrateur
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code IN ('suppliers.create', 'suppliers.update')
WHERE r.name = 'ADMIN';
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code IN ('suppliers.create', 'suppliers.update'));