-- liquibase formatted sql

-- changeset randy:2026_11_11-20-1
-- comment: JR-04 - permission de lire la reconciliation de cloture (JR-06 s'en servira pour verrouiller la journee)
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'days.close', 'Réconcilier et clôturer la journée', now(), now());
-- rollback DELETE FROM permissions WHERE code = 'days.close';

-- changeset randy:2026_11_11-20-2
-- comment: JR-04 - la cloture de journee est reservee a l'administrateur
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'days.close'
WHERE r.name = 'ADMIN';
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'days.close');
