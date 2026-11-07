-- liquibase formatted sql

-- changeset randy:2026_11_08-42-1
-- comment: PRD-03 - permission de faire avancer le statut d'un lot (planifie vers en cours)
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'production.update', 'Demarrer un lot de production (statut en cours)', now(), now());
-- rollback DELETE FROM permissions WHERE code = 'production.update';

-- changeset randy:2026_11_08-42-2
-- comment: PRD-03 - la vendeuse et l'administrateur demarrent un lot ; la vendeuse peut aussi le terminer (production.complete)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'production.update'
WHERE r.name IN ('ADMIN', 'SELLER')
UNION
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'production.complete'
WHERE r.name = 'SELLER';
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'production.update');
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'production.complete') AND role_id IN (SELECT id FROM roles WHERE name = 'SELLER');
