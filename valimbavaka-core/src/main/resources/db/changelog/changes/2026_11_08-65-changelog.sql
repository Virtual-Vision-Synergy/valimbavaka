-- liquibase formatted sql

-- changeset randy:2026_11_08-65-1
-- comment: LIV-04 - motif obligatoire d'un echec de livraison, commentaire du livreur et retour de stock (RET-...) qui a remis les articles en stock
ALTER TABLE delivery_orders ADD COLUMN failure_reason VARCHAR(30);
ALTER TABLE delivery_orders ADD COLUMN failure_note VARCHAR(280);
ALTER TABLE delivery_orders ADD COLUMN return_transfer_id UUID;
ALTER TABLE delivery_orders ADD CONSTRAINT fk_delivery_orders_return_transfer
    FOREIGN KEY (return_transfer_id) REFERENCES transfers (id);
ALTER TABLE delivery_orders ADD CONSTRAINT ck_delivery_orders_failure_reason
    CHECK (failure_reason IN ('ABSENT', 'ADDRESS_NOT_FOUND', 'REFUSED', 'OTHER'));
ALTER TABLE delivery_orders ADD CONSTRAINT ck_delivery_orders_failed_has_reason
    CHECK (status <> 'FAILED' OR (failure_reason IS NOT NULL AND return_transfer_id IS NOT NULL));
-- rollback ALTER TABLE delivery_orders DROP CONSTRAINT ck_delivery_orders_failed_has_reason;
-- rollback ALTER TABLE delivery_orders DROP CONSTRAINT ck_delivery_orders_failure_reason;
-- rollback ALTER TABLE delivery_orders DROP CONSTRAINT fk_delivery_orders_return_transfer;
-- rollback ALTER TABLE delivery_orders DROP COLUMN return_transfer_id;
-- rollback ALTER TABLE delivery_orders DROP COLUMN failure_note;
-- rollback ALTER TABLE delivery_orders DROP COLUMN failure_reason;

-- changeset randy:2026_11_08-65-2
-- comment: LIV-04 - permission de signaler l'echec d'une livraison (livreur pour ses commandes, administrateur)
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'deliveries.fail', 'Signaler l''echec d''une livraison', now(), now());
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'deliveries.fail'
WHERE r.name IN ('ADMIN', 'DELIVERY_DRIVER');
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'deliveries.fail');
-- rollback DELETE FROM permissions WHERE code = 'deliveries.fail';
