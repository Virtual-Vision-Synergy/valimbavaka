-- liquibase formatted sql

-- changeset randy:2026_11_08-82-1
-- comment: INV-04 - permission de cloturer une session d'inventaire (ajustement du stock), reservee a l'administrateur
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'inventory.close', 'Clôturer une session d''inventaire et ajuster le stock', now(), now());
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'inventory.close'
WHERE r.name = 'ADMIN';
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'inventory.close');
-- rollback DELETE FROM permissions WHERE code = 'inventory.close';
