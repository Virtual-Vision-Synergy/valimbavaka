-- liquibase formatted sql

-- changeset randy:2026_10_23-01-1
-- comment: AUTH-03 / JNL-02 - un utilisateur ne se supprime jamais, il se desactive : retrait de la permission users.delete
DELETE FROM role_permissions
WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'users.delete');
DELETE FROM permissions WHERE code = 'users.delete';
-- rollback INSERT INTO permissions (id, code, description, created_at, updated_at) VALUES (gen_random_uuid(), 'users.delete', 'Supprimer un utilisateur', now(), now());
-- rollback INSERT INTO role_permissions (role_id, permission_id) SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code = 'users.delete' WHERE r.name = 'ADMIN';
