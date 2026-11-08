-- liquibase formatted sql

-- changeset randy:2026_11_08-62-1
-- comment: SHO-02 - date et auteur de la reintegration d'une sortie shooting, renseignes ensemble avec le statut RETURNED
ALTER TABLE shootings
    ADD COLUMN returned_at      TIMESTAMPTZ,
    ADD COLUMN returned_by_name VARCHAR(255),
    ADD CONSTRAINT ck_shootings_returned
        CHECK ((status = 'RETURNED') = (returned_at IS NOT NULL AND returned_by_name IS NOT NULL));
-- rollback ALTER TABLE shootings DROP CONSTRAINT ck_shootings_returned, DROP COLUMN returned_by_name, DROP COLUMN returned_at;

-- changeset randy:2026_11_08-62-2
-- comment: SHO-02 - permission de reintegrer les produits d'une sortie shooting
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'shootings.return', 'Reintegrer les produits d''une sortie shooting', now(), now());
-- rollback DELETE FROM permissions WHERE code = 'shootings.return';

-- changeset randy:2026_11_08-62-3
-- comment: SHO-02 - la reintegration est reservee a l'administrateur (user story SHO-02)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'shootings.return'
WHERE r.name = 'ADMIN';
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'shootings.return');
