-- liquibase formatted sql

-- changeset randy:2026_11_11-01-1
-- comment: LIV-05 - statut precedent d'une commande livree ou en echec, pour restaurer le statut pendant le delai d'annulation
ALTER TABLE delivery_orders ADD COLUMN previous_status VARCHAR(20);
ALTER TABLE delivery_orders ADD CONSTRAINT ck_delivery_orders_previous_status
    CHECK (previous_status IS NULL OR previous_status IN ('TO_PREPARE', 'ON_ROUND'));
-- rollback ALTER TABLE delivery_orders DROP CONSTRAINT ck_delivery_orders_previous_status;
-- rollback ALTER TABLE delivery_orders DROP COLUMN previous_status;

-- changeset randy:2026_11_11-01-2
-- comment: LIV-05 - permission d'annuler une action recente sur une livraison (livreur pour ses commandes, administrateur)
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'deliveries.undo', 'Annuler une action recente sur une livraison', now(), now());
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'deliveries.undo'
WHERE r.name IN ('ADMIN', 'DELIVERY_DRIVER');
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'deliveries.undo');
-- rollback DELETE FROM permissions WHERE code = 'deliveries.undo';
