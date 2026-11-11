-- liquibase formatted sql

-- changeset randy:2026_11_11-61-1
-- comment: COM-04 - objectif mensuel de chiffre d'affaires de la vendeuse, champ de sa fiche utilisateur (decision PO du 07/11/2026, modifiable par l'administrateur seul)
ALTER TABLE users
    ADD COLUMN monthly_target NUMERIC(19, 2);
ALTER TABLE users
    ADD CONSTRAINT ck_users_monthly_target CHECK (monthly_target IS NULL OR monthly_target > 0);
-- rollback ALTER TABLE users DROP CONSTRAINT ck_users_monthly_target;
-- rollback ALTER TABLE users DROP COLUMN monthly_target;
