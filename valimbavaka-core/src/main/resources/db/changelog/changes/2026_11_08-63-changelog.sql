-- liquibase formatted sql

-- changeset randy:2026_11_08-63-1
-- comment: SHO-03 - permission de suivre les sorties shooting en cours et leur valeur immobilisee (au cout d'achat)
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'shootings.track', 'Suivre les sorties shooting en cours et leur valeur immobilisee', now(), now());
-- rollback DELETE FROM permissions WHERE code = 'shootings.track';

-- changeset randy:2026_11_08-63-2
-- comment: SHO-03 - le suivi est reserve a l'administrateur (user story SHO-03 ; la valeur revele le cout d'achat, comme stocks.value)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'shootings.track'
WHERE r.name = 'ADMIN';
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'shootings.track');
