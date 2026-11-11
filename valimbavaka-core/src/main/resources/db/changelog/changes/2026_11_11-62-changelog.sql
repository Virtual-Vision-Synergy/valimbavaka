-- liquibase formatted sql

-- changeset randy:2026_11_11-62-1
-- comment: COM-05 - recalcul des commissions d'une periode reserve a l'administrateur
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'commissions.recalculate', 'Recalculer les commissions non validees d''une periode', now(), now());
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'commissions.recalculate'
WHERE r.name = 'ADMIN';
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'commissions.recalculate');
-- rollback DELETE FROM permissions WHERE code = 'commissions.recalculate';
