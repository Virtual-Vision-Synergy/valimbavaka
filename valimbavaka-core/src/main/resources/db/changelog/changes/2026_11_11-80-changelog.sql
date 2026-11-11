-- liquibase formatted sql

-- changeset randy:2026_11_11-80-1
-- comment: PRO-02 - promotion appliquee a une vente (reference pour le suivi PRO-03), vide si aucune remise
ALTER TABLE sales ADD COLUMN promotion_id UUID;
ALTER TABLE sales ADD CONSTRAINT fk_sales_promotion FOREIGN KEY (promotion_id) REFERENCES promotions (id);
CREATE INDEX ix_sales_promotion ON sales (promotion_id) WHERE promotion_id IS NOT NULL;
-- rollback DROP INDEX ix_sales_promotion;
-- rollback ALTER TABLE sales DROP CONSTRAINT fk_sales_promotion;
-- rollback ALTER TABLE sales DROP COLUMN promotion_id;
