-- liquibase formatted sql

-- changeset mana:2026_11_03-01-1
-- comment: FRS-04 - delai de reapprovisionnement par fournisseur, initialise selon le type (LOCAL 7 j, FOREIGN 30 j)
ALTER TABLE suppliers ADD COLUMN lead_time_days INTEGER;
UPDATE suppliers SET lead_time_days = CASE type WHEN 'FOREIGN' THEN 30 ELSE 7 END;
ALTER TABLE suppliers ALTER COLUMN lead_time_days SET NOT NULL;
ALTER TABLE suppliers ADD CONSTRAINT ck_suppliers_lead_time_days CHECK (lead_time_days BETWEEN 0 AND 365);
-- rollback ALTER TABLE suppliers DROP COLUMN lead_time_days;
