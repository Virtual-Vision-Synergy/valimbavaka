-- liquibase formatted sql

-- changeset randy:2026_11_11-60-1
-- comment: COM-03 - validation des commissions par l'administrateur : statut VALIDATED et date de validation (une commission annulee apres validation garde sa date)
ALTER TABLE commissions
    ADD COLUMN validated_at TIMESTAMPTZ;
ALTER TABLE commissions
    DROP CONSTRAINT ck_commissions_status;
ALTER TABLE commissions
    ADD CONSTRAINT ck_commissions_status CHECK (status IN ('CALCULATED', 'VALIDATED', 'CANCELLED'));
ALTER TABLE commissions
    ADD CONSTRAINT ck_commissions_validated_at CHECK (status <> 'VALIDATED' OR validated_at IS NOT NULL);
-- rollback ALTER TABLE commissions DROP CONSTRAINT ck_commissions_validated_at;
-- rollback UPDATE commissions SET status = 'CALCULATED' WHERE status = 'VALIDATED';
-- rollback ALTER TABLE commissions DROP CONSTRAINT ck_commissions_status;
-- rollback ALTER TABLE commissions ADD CONSTRAINT ck_commissions_status CHECK (status IN ('CALCULATED', 'CANCELLED'));
-- rollback ALTER TABLE commissions DROP COLUMN validated_at;

-- changeset randy:2026_11_11-60-2
-- comment: COM-03 - validation des commissions reservee a l'administrateur
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'commissions.validate', 'Valider les commissions des vendeuses', now(), now());
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'commissions.validate'
WHERE r.name = 'ADMIN';
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'commissions.validate');
-- rollback DELETE FROM permissions WHERE code = 'commissions.validate';
