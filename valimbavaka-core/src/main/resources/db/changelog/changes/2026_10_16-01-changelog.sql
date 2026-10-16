-- liquibase formatted sql

-- changeset mana:2026_10_16-01-1
-- comment: SYS-05 - codes de roles en anglais (ADMINISTRATEUR -> ADMIN, VENDEUSE -> SELLER, LIVREUR -> DELIVERY_DRIVER), libelles inchanges
ALTER TABLE roles DROP CONSTRAINT ck_roles_name;
UPDATE roles SET name = 'ADMIN', updated_at = now() WHERE name = 'ADMINISTRATEUR';
UPDATE roles SET name = 'SELLER', updated_at = now() WHERE name = 'VENDEUSE';
UPDATE roles SET name = 'DELIVERY_DRIVER', updated_at = now() WHERE name = 'LIVREUR';
ALTER TABLE roles ADD CONSTRAINT ck_roles_name CHECK (name IN ('ADMIN', 'SELLER', 'DELIVERY_DRIVER'));
-- rollback ALTER TABLE roles DROP CONSTRAINT ck_roles_name;
-- rollback UPDATE roles SET name = 'ADMINISTRATEUR' WHERE name = 'ADMIN';
-- rollback UPDATE roles SET name = 'VENDEUSE' WHERE name = 'SELLER';
-- rollback UPDATE roles SET name = 'LIVREUR' WHERE name = 'DELIVERY_DRIVER';
-- rollback ALTER TABLE roles ADD CONSTRAINT ck_roles_name CHECK (name IN ('ADMINISTRATEUR', 'VENDEUSE', 'LIVREUR'));

-- changeset mana:2026_10_16-01-2
-- comment: SYS-05 - codes des referentiels en anglais (libelles inchanges ; gammes = noms de marque, non traduits)
UPDATE sales_channels SET code = 'STORE', updated_at = now() WHERE code = 'BOUTIQUE';
UPDATE sales_channels SET code = 'PHONE', updated_at = now() WHERE code = 'TELEPHONE';
UPDATE sales_channels SET code = 'DELIVERY', updated_at = now() WHERE code = 'LIVRAISON';
UPDATE stock_locations SET code = 'WAREHOUSE', updated_at = now() WHERE code = 'ENTREPOT';
UPDATE stock_locations SET code = 'STORE', updated_at = now() WHERE code = 'BOUTIQUE';
UPDATE stock_locations SET code = 'DELIVERY', updated_at = now() WHERE code = 'LIVRAISON';
UPDATE stock_locations SET code = 'TRADE_SHOW', updated_at = now() WHERE code = 'SALON';
-- rollback UPDATE stock_locations SET code = 'SALON' WHERE code = 'TRADE_SHOW';
-- rollback UPDATE stock_locations SET code = 'LIVRAISON' WHERE code = 'DELIVERY';
-- rollback UPDATE stock_locations SET code = 'BOUTIQUE' WHERE code = 'STORE';
-- rollback UPDATE stock_locations SET code = 'ENTREPOT' WHERE code = 'WAREHOUSE';
-- rollback UPDATE sales_channels SET code = 'LIVRAISON' WHERE code = 'DELIVERY';
-- rollback UPDATE sales_channels SET code = 'TELEPHONE' WHERE code = 'PHONE';
-- rollback UPDATE sales_channels SET code = 'BOUTIQUE' WHERE code = 'STORE';
