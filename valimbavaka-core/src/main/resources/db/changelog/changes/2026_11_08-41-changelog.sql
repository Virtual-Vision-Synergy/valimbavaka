-- liquibase formatted sql

-- changeset randy:2026_11_08-41-1
-- comment: PRD-02 - cout unitaire des entrants fige a la validation du lot (NULL tant que le lot n'est pas valide ou si l'article n'a pas de cout)
ALTER TABLE production_batch_inputs ADD COLUMN unit_cost NUMERIC(14, 2);
ALTER TABLE production_batch_inputs ADD CONSTRAINT ck_production_batch_inputs_unit_cost CHECK (unit_cost IS NULL OR unit_cost >= 0);
-- rollback ALTER TABLE production_batch_inputs DROP COLUMN unit_cost;

-- changeset randy:2026_11_08-41-2
-- comment: PRD-02 - date de validation du lot (indicateurs PRD-04), renseignee seulement au statut COMPLETED
ALTER TABLE production_batches ADD COLUMN completed_at TIMESTAMPTZ;
ALTER TABLE production_batches ADD CONSTRAINT ck_production_batches_completed_at CHECK ((status = 'COMPLETED') = (completed_at IS NOT NULL));
CREATE INDEX ix_production_batches_completed_at ON production_batches (completed_at);
-- rollback ALTER TABLE production_batches DROP COLUMN completed_at;

-- changeset randy:2026_11_08-41-3
-- comment: PRD-02 - permission de valider un lot (mouvements de stock), reservee a l'administrateur
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'production.complete', 'Valider un lot de production (mouvements de stock)', now(), now());
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'production.complete'
WHERE r.name = 'ADMIN';
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'production.complete');
-- rollback DELETE FROM permissions WHERE code = 'production.complete';
