-- liquibase formatted sql

-- changeset randy:2026_10_16-02-1
-- comment: AUTH-03 - canal de vente et taux de commission d'une vendeuse
ALTER TABLE users
    ADD COLUMN sales_channel_id UUID,
    ADD COLUMN commission_rate  NUMERIC(5, 2),
    ADD CONSTRAINT fk_users_sales_channel_id FOREIGN KEY (sales_channel_id) REFERENCES sales_channels (id),
    ADD CONSTRAINT ck_users_commission_rate CHECK (commission_rate BETWEEN 0 AND 100);
CREATE INDEX ix_users_sales_channel_id ON users (sales_channel_id);
-- rollback DROP INDEX ix_users_sales_channel_id;
-- rollback ALTER TABLE users DROP COLUMN commission_rate, DROP COLUMN sales_channel_id;
