-- liquibase formatted sql

-- changeset randy:2026_11_11-94-1
-- comment: FAC-04 - compteur des numeros d'avoir, une ligne par annee (AV-2026-0001...) ; incremente dans la transaction de l'avoir, donc sans trou et sur en concurrence
CREATE TABLE credit_note_counters
(
    year       INTEGER NOT NULL,
    last_value BIGINT  NOT NULL,
    CONSTRAINT pk_credit_note_counters PRIMARY KEY (year),
    CONSTRAINT ck_credit_note_counters_last_value CHECK (last_value > 0)
);
-- rollback DROP TABLE credit_note_counters;

-- changeset randy:2026_11_11-94-2
-- comment: FAC-04 - avoir motive : copie figee de la facture contre-passee, jamais modifie ni supprime (JNL-02) ; un seul avoir par facture ; invoice_id et sale_id sans cle etrangere (pas de lien entre features)
CREATE TABLE credit_notes
(
    id              UUID           NOT NULL,
    number          VARCHAR(20)    NOT NULL,
    invoice_id      UUID           NOT NULL,
    invoice_number  VARCHAR(20)    NOT NULL,
    sale_id         UUID           NOT NULL,
    sale_number     VARCHAR(20)    NOT NULL,
    issued_at       TIMESTAMPTZ    NOT NULL,
    business_date   DATE           NOT NULL,
    reason          VARCHAR(500)   NOT NULL,
    issuer_name     VARCHAR(150)   NOT NULL,
    issuer_nif      VARCHAR(50),
    subtotal        NUMERIC(19, 2) NOT NULL,
    discount_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    total           NUMERIC(19, 2) NOT NULL,
    created_at      TIMESTAMPTZ    NOT NULL,
    updated_at      TIMESTAMPTZ    NOT NULL,
    created_by      UUID,
    updated_by      UUID,
    version         BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT pk_credit_notes PRIMARY KEY (id),
    CONSTRAINT uq_credit_notes_number UNIQUE (number),
    CONSTRAINT uq_credit_notes_invoice_id UNIQUE (invoice_id),
    CONSTRAINT ck_credit_notes_reason CHECK (char_length(btrim(reason)) > 0),
    CONSTRAINT ck_credit_notes_subtotal CHECK (subtotal >= 0),
    CONSTRAINT ck_credit_notes_discount_amount CHECK (discount_amount >= 0 AND discount_amount <= subtotal),
    CONSTRAINT ck_credit_notes_total CHECK (total = subtotal - discount_amount)
);
CREATE INDEX ix_credit_notes_business_date ON credit_notes (business_date);
CREATE INDEX ix_credit_notes_sale_id ON credit_notes (sale_id);

CREATE TABLE credit_note_lines
(
    credit_note_id UUID           NOT NULL,
    line_number    INTEGER        NOT NULL,
    article_id     UUID           NOT NULL,
    label          VARCHAR(255)   NOT NULL,
    quantity       NUMERIC(12, 3) NOT NULL,
    unit_price     NUMERIC(19, 2) NOT NULL,
    line_total     NUMERIC(19, 2) NOT NULL,
    CONSTRAINT pk_credit_note_lines PRIMARY KEY (credit_note_id, line_number),
    CONSTRAINT fk_credit_note_lines_credit_note FOREIGN KEY (credit_note_id) REFERENCES credit_notes (id),
    CONSTRAINT ck_credit_note_lines_quantity CHECK (quantity > 0),
    CONSTRAINT ck_credit_note_lines_unit_price CHECK (unit_price >= 0),
    CONSTRAINT ck_credit_note_lines_line_total CHECK (line_total >= 0)
);
-- rollback DROP TABLE credit_note_lines;
-- rollback DROP TABLE credit_notes;
