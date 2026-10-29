-- liquibase formatted sql

-- changeset randy:2026_10_29-01-1
-- comment: STK-01 - mouvements de stock types, source unique du stock (jamais modifie directement)
CREATE TABLE stock_movements
(
    id           UUID           NOT NULL,
    article_id   UUID           NOT NULL,
    location_id  UUID           NOT NULL,
    type         VARCHAR(20)    NOT NULL,
    quantity     NUMERIC(12, 3) NOT NULL,
    reference_id UUID,
    occurred_at  TIMESTAMPTZ    NOT NULL,
    CONSTRAINT pk_stock_movements PRIMARY KEY (id),
    CONSTRAINT fk_stock_movements_article FOREIGN KEY (article_id) REFERENCES articles (id),
    CONSTRAINT fk_stock_movements_location FOREIGN KEY (location_id) REFERENCES stock_locations (id),
    CONSTRAINT ck_stock_movements_type CHECK (type IN ('RECEIPT', 'TRANSFER', 'SALE', 'DELIVERY', 'RETURN',
                                                       'TRADE_SHOW', 'SHOOTING', 'PRODUCTION', 'INVENTORY')),
    CONSTRAINT ck_stock_movements_quantity CHECK (quantity <> 0)
);
CREATE INDEX ix_stock_movements_article_location ON stock_movements (article_id, location_id);
CREATE INDEX ix_stock_movements_reference_id ON stock_movements (reference_id) WHERE reference_id IS NOT NULL;
-- rollback DROP TABLE stock_movements;

-- changeset randy:2026_10_29-01-2 splitStatements:false
-- comment: STK-01 - append-only garanti par la base : un mouvement ne se modifie ni ne se supprime (correction = mouvement inverse)
CREATE FUNCTION reject_stock_movements_change() RETURNS TRIGGER AS
$$
BEGIN
    RAISE EXCEPTION 'stock_movements est append-only : % interdit', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_stock_movements_append_only
    BEFORE UPDATE OR DELETE ON stock_movements
    FOR EACH ROW EXECUTE FUNCTION reject_stock_movements_change();

CREATE TRIGGER trg_stock_movements_no_truncate
    BEFORE TRUNCATE ON stock_movements
    FOR EACH STATEMENT EXECUTE FUNCTION reject_stock_movements_change();
-- rollback DROP TRIGGER trg_stock_movements_no_truncate ON stock_movements;
-- rollback DROP TRIGGER trg_stock_movements_append_only ON stock_movements;
-- rollback DROP FUNCTION reject_stock_movements_change();
