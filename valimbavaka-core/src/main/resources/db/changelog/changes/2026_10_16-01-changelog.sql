-- liquibase formatted sql

-- changeset mana:2026_10_16-01-1
-- comment: SYS-05 - codes de roles en anglais (VENDEUSE -> SELLER, LIVREUR -> DELIVERY_DRIVER), libelles inchanges
ALTER TABLE roles DROP CONSTRAINT ck_roles_name;
UPDATE roles SET name = 'SELLER', updated_at = now() WHERE name = 'VENDEUSE';
UPDATE roles SET name = 'DELIVERY_DRIVER', updated_at = now() WHERE name = 'LIVREUR';
ALTER TABLE roles ADD CONSTRAINT ck_roles_name CHECK (name IN ('ADMINISTRATEUR', 'SELLER', 'DELIVERY_DRIVER'));
-- rollback ALTER TABLE roles DROP CONSTRAINT ck_roles_name;
-- rollback UPDATE roles SET name = 'VENDEUSE' WHERE name = 'SELLER';
-- rollback UPDATE roles SET name = 'LIVREUR' WHERE name = 'DELIVERY_DRIVER';
-- rollback ALTER TABLE roles ADD CONSTRAINT ck_roles_name CHECK (name IN ('ADMINISTRATEUR', 'VENDEUSE', 'LIVREUR'));
