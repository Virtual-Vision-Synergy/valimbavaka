-- liquibase formatted sql

-- changeset randy:2026_11_11-21-1
-- comment: JR-05 - comptage du soir par jour, emplacement et article ; le theorique est fige au comptage et tout ecart exige un motif (JR-06 figera ces lignes au verrouillage)
CREATE TABLE day_closing_counts
(
    id                   UUID           NOT NULL,
    business_day_id      UUID           NOT NULL,
    location_id          UUID           NOT NULL,
    article_id           UUID           NOT NULL,
    theoretical_quantity NUMERIC(14, 3) NOT NULL,
    counted_quantity     NUMERIC(14, 3) NOT NULL,
    reason               VARCHAR(30),
    counted_at           TIMESTAMPTZ    NOT NULL,
    created_at           TIMESTAMPTZ    NOT NULL,
    updated_at           TIMESTAMPTZ    NOT NULL,
    created_by           UUID,
    updated_by           UUID,
    version              BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT pk_day_closing_counts PRIMARY KEY (id),
    CONSTRAINT uq_day_closing_counts_line UNIQUE (business_day_id, location_id, article_id),
    CONSTRAINT fk_day_closing_counts_day FOREIGN KEY (business_day_id) REFERENCES business_days (id),
    CONSTRAINT fk_day_closing_counts_location FOREIGN KEY (location_id) REFERENCES stock_locations (id),
    CONSTRAINT fk_day_closing_counts_article FOREIGN KEY (article_id) REFERENCES articles (id),
    CONSTRAINT ck_day_closing_counts_counted CHECK (counted_quantity >= 0),
    CONSTRAINT ck_day_closing_counts_reason_value CHECK (reason IN ('BREAKAGE', 'LOSS', 'ENTRY_ERROR', 'SAMPLE', 'OTHER')),
    CONSTRAINT ck_day_closing_counts_reason_gap CHECK ((counted_quantity = theoretical_quantity AND reason IS NULL)
        OR (counted_quantity <> theoretical_quantity AND reason IS NOT NULL))
);
-- rollback DROP TABLE day_closing_counts;
