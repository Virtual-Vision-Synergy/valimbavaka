-- liquibase formatted sql

-- changeset randy:2026_11_11-90-1
-- comment: FAC-01 - compteur des numeros de facture, une ligne par annee (FAC-2026-0001...) ; incremente dans la transaction de la vente, donc sans trou et sur en concurrence
CREATE TABLE invoice_counters
(
    year       INTEGER NOT NULL,
    last_value BIGINT  NOT NULL,
    CONSTRAINT pk_invoice_counters PRIMARY KEY (year),
    CONSTRAINT ck_invoice_counters_last_value CHECK (last_value > 0)
);
-- rollback DROP TABLE invoice_counters;

-- changeset randy:2026_11_11-90-2
-- comment: FAC-01 - facture emise a l'encaissement d'une vente : instantane fige (noms, prix, emetteur), jamais modifiee ni supprimee (JNL-02) ; sale_id sans cle etrangere (pas de lien entre features)
CREATE TABLE invoices
(
    id                  UUID           NOT NULL,
    number              VARCHAR(20)    NOT NULL,
    sale_id             UUID           NOT NULL,
    sale_number         VARCHAR(20)    NOT NULL,
    issued_at           TIMESTAMPTZ    NOT NULL,
    business_date       DATE           NOT NULL,
    sales_channel_label VARCHAR(100),
    seller_id           UUID,
    seller_name         VARCHAR(255),
    payment_method      VARCHAR(20)    NOT NULL,
    issuer_name         VARCHAR(150)   NOT NULL,
    issuer_nif          VARCHAR(50),
    status              VARCHAR(20)    NOT NULL,
    subtotal            NUMERIC(19, 2) NOT NULL,
    discount_amount     NUMERIC(19, 2) NOT NULL DEFAULT 0,
    total               NUMERIC(19, 2) NOT NULL,
    created_at          TIMESTAMPTZ    NOT NULL,
    updated_at          TIMESTAMPTZ    NOT NULL,
    created_by          UUID,
    updated_by          UUID,
    version             BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT pk_invoices PRIMARY KEY (id),
    CONSTRAINT uq_invoices_number UNIQUE (number),
    CONSTRAINT uq_invoices_sale_id UNIQUE (sale_id),
    CONSTRAINT ck_invoices_status CHECK (status IN ('VALIDATED', 'CANCELLED', 'REVERSED')),
    CONSTRAINT ck_invoices_subtotal CHECK (subtotal >= 0),
    CONSTRAINT ck_invoices_discount_amount CHECK (discount_amount >= 0 AND discount_amount <= subtotal),
    CONSTRAINT ck_invoices_total CHECK (total = subtotal - discount_amount)
);
CREATE INDEX ix_invoices_business_date ON invoices (business_date);
CREATE INDEX ix_invoices_seller_id ON invoices (seller_id);

CREATE TABLE invoice_lines
(
    invoice_id  UUID           NOT NULL,
    line_number INTEGER        NOT NULL,
    article_id  UUID           NOT NULL,
    label       VARCHAR(255)   NOT NULL,
    quantity    NUMERIC(12, 3) NOT NULL,
    unit_price  NUMERIC(19, 2) NOT NULL,
    line_total  NUMERIC(19, 2) NOT NULL,
    CONSTRAINT pk_invoice_lines PRIMARY KEY (invoice_id, line_number),
    CONSTRAINT fk_invoice_lines_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id),
    CONSTRAINT ck_invoice_lines_quantity CHECK (quantity > 0),
    CONSTRAINT ck_invoice_lines_unit_price CHECK (unit_price >= 0),
    CONSTRAINT ck_invoice_lines_line_total CHECK (line_total >= 0)
);
-- rollback DROP TABLE invoice_lines;
-- rollback DROP TABLE invoices;
