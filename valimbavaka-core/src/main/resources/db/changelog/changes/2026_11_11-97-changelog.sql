-- liquibase formatted sql

-- changeset randy:2026_11_11-97-1
-- comment: FAC-06 - livre des ventes : vue en lecture seule qui range les factures et les avoirs dans une meme liste ; l'avoir est une ligne en negatif (facture et avoir sont deux lignes, la facture contre-passee garde son montant) ; une facture annulee sans avoir garde son numero mais compte 0
CREATE VIEW sales_book_entries AS
SELECT i.id,
       CAST('INVOICE' AS VARCHAR(20))                                              AS entry_type,
       i.number,
       CAST(NULL AS VARCHAR(20))                                                   AS invoice_number,
       i.sale_number,
       i.business_date,
       i.issued_at,
       i.sales_channel_label,
       i.seller_name,
       i.payment_method,
       CAST(i.status AS VARCHAR(20))                                               AS status,
       CAST(NULL AS VARCHAR(500))                                                  AS reason,
       CAST(CASE WHEN i.status = 'CANCELLED' THEN 0 ELSE i.subtotal END AS NUMERIC(19, 2)) AS subtotal,
       CAST(CASE WHEN i.status = 'CANCELLED' THEN 0 ELSE i.discount_amount END AS NUMERIC(19, 2)) AS discount_amount,
       CAST(CASE WHEN i.status = 'CANCELLED' THEN 0 ELSE i.total END AS NUMERIC(19, 2)) AS total
FROM invoices i
UNION ALL
SELECT c.id,
       CAST('CREDIT_NOTE' AS VARCHAR(20)),
       c.number,
       c.invoice_number,
       c.sale_number,
       c.business_date,
       c.issued_at,
       i.sales_channel_label,
       i.seller_name,
       i.payment_method,
       CAST(NULL AS VARCHAR(20)),
       c.reason,
       CAST(-c.subtotal AS NUMERIC(19, 2)),
       CAST(-c.discount_amount AS NUMERIC(19, 2)),
       CAST(-c.total AS NUMERIC(19, 2))
FROM credit_notes c
         JOIN invoices i ON i.id = c.invoice_id;
-- rollback DROP VIEW sales_book_entries;

-- changeset randy:2026_11_11-97-2
-- comment: FAC-06 - permission d'exporter le livre des ventes
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'invoices.export', 'Exporter le livre des ventes', now(), now());
-- rollback DELETE FROM permissions WHERE code = 'invoices.export';

-- changeset randy:2026_11_11-97-3
-- comment: FAC-06 - le livre des ventes est exporte par l'administrateur seulement
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'invoices.export'
WHERE r.name = 'ADMIN';
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'invoices.export');
