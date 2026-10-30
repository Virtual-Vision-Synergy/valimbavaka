-- liquibase formatted sql

-- changeset kei:2026_10_30-01-1
-- comment: STK-03 - permission de consulter la valorisation du stock par emplacement
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'stocks.value', 'Consulter la valeur du stock', now(), now());
-- rollback DELETE FROM permissions WHERE code = 'stocks.value';

-- changeset kei:2026_10_30-01-2
-- comment: STK-03 - la valorisation de l'actif est reservee a l'administrateur
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'stocks.value'
WHERE r.name = 'ADMIN';
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'stocks.value');
