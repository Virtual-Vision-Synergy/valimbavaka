-- liquibase formatted sql

-- changeset randy:2026_11_08-66-1
-- comment: LIV-03 - vente (canal Livraison) creee quand la commande est marquee livree ; une vente par commande, une commande livree a toujours sa vente (decision PO du 07/11/2026)
ALTER TABLE delivery_orders ADD COLUMN sale_id UUID;
ALTER TABLE delivery_orders ADD CONSTRAINT fk_delivery_orders_sale FOREIGN KEY (sale_id) REFERENCES sales (id);
ALTER TABLE delivery_orders ADD CONSTRAINT uq_delivery_orders_sale UNIQUE (sale_id);
ALTER TABLE delivery_orders ADD CONSTRAINT ck_delivery_orders_delivered_has_sale
    CHECK (status <> 'DELIVERED' OR sale_id IS NOT NULL);
-- rollback ALTER TABLE delivery_orders DROP CONSTRAINT ck_delivery_orders_delivered_has_sale;
-- rollback ALTER TABLE delivery_orders DROP CONSTRAINT uq_delivery_orders_sale;
-- rollback ALTER TABLE delivery_orders DROP CONSTRAINT fk_delivery_orders_sale;
-- rollback ALTER TABLE delivery_orders DROP COLUMN sale_id;

-- changeset randy:2026_11_08-66-2
-- comment: LIV-03 - permission de marquer une livraison livree (livreur pour ses commandes, administrateur)
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'deliveries.deliver', 'Marquer une livraison livree', now(), now());
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'deliveries.deliver'
WHERE r.name IN ('ADMIN', 'DELIVERY_DRIVER');
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'deliveries.deliver');
-- rollback DELETE FROM permissions WHERE code = 'deliveries.deliver';
