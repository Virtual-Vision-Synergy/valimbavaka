-- liquibase formatted sql

-- changeset randy:2026_10_29-01-1
-- comment: ART-08 - permission d'export de la liste des articles
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'articles.export', 'Exporter la liste des articles', now(), now());
-- rollback DELETE FROM permissions WHERE code = 'articles.export';

-- changeset randy:2026_10_29-01-2
-- comment: ART-08 - export reserve a l'administrateur (articles.read reste donnee a la vendeuse)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'articles.export'
WHERE r.name = 'ADMIN';
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'articles.export');
