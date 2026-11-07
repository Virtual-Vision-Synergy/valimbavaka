-- liquibase formatted sql

-- changeset randy:2026_11_08-81-1
-- comment: INV-02 - permission de saisir le comptage physique d'une session d'inventaire, reservee a l'administrateur
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'inventory.update', 'Saisir le comptage d''une session d''inventaire', now(), now());
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'inventory.update'
WHERE r.name = 'ADMIN';
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'inventory.update');
-- rollback DELETE FROM permissions WHERE code = 'inventory.update';
