-- liquibase formatted sql

-- changeset randy:2026_11_11-91-1
-- comment: FAC-02 - permission de regenerer la facture d'une vente existante
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'invoices.regenerate', 'Regenerer une facture', now(), now());
-- rollback DELETE FROM permissions WHERE code = 'invoices.regenerate';

-- changeset randy:2026_11_11-91-2
-- comment: FAC-02 - la facture est regeneree par la vendeuse ou par l'administrateur
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'invoices.regenerate'
WHERE r.name IN ('ADMIN', 'SELLER');
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'invoices.regenerate');
