-- liquibase formatted sql

-- changeset randy:2026_10_29-03-1
-- comment: ART-07 - permission d'import d'articles en masse
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'articles.import', 'Importer des articles en masse', now(), now());
-- rollback DELETE FROM permissions WHERE code = 'articles.import';

-- changeset randy:2026_10_29-03-2
-- comment: ART-07 - l'import est reserve a l'administrateur
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'articles.import'
WHERE r.name = 'ADMIN';
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'articles.import');
