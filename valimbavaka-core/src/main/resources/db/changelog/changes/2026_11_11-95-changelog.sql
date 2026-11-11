-- liquibase formatted sql

-- changeset randy:2026_11_11-95-1
-- comment: FAC-04 - permission d'emettre un avoir motive
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'invoices.credit', 'Emettre un avoir motive', now(), now());
-- rollback DELETE FROM permissions WHERE code = 'invoices.credit';

-- changeset randy:2026_11_11-95-2
-- comment: FAC-04 - l'avoir est emis par l'administrateur seulement
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'invoices.credit'
WHERE r.name = 'ADMIN';
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'invoices.credit');
