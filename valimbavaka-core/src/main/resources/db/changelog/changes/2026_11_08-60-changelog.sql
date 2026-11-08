-- liquibase formatted sql

-- changeset randy:2026_11_08-60-1
-- comment: PRO-01 - referentiel des promotions (type de remise, perimetre, periode, code facultatif, plafond)
CREATE TABLE promotions
(
    id             UUID          NOT NULL,
    name           VARCHAR(100)  NOT NULL,
    description    VARCHAR(500),
    kind           VARCHAR(20)   NOT NULL,
    discount_value NUMERIC(14, 2) NOT NULL,
    code           VARCHAR(12),
    scope          VARCHAR(20)   NOT NULL,
    starts_on      DATE          NOT NULL,
    ends_on        DATE          NOT NULL,
    usage_limit    INTEGER,
    usage_count    INTEGER       NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ   NOT NULL,
    updated_at     TIMESTAMPTZ   NOT NULL,
    created_by     UUID,
    updated_by     UUID,
    version        BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_promotions PRIMARY KEY (id),
    CONSTRAINT ck_promotions_kind CHECK (kind IN ('PERCENT', 'AMOUNT', 'BUY_X')),
    CONSTRAINT ck_promotions_scope CHECK (scope IN ('ALL', 'RANGES', 'ARTICLES')),
    CONSTRAINT ck_promotions_discount_value CHECK (discount_value > 0
        AND (kind <> 'PERCENT' OR discount_value <= 100)
        AND (kind <> 'BUY_X' OR (discount_value BETWEEN 2 AND 100 AND discount_value = trunc(discount_value)))),
    CONSTRAINT ck_promotions_code CHECK (code ~ '^[A-Z0-9]{3,12}$'),
    CONSTRAINT ck_promotions_period CHECK (ends_on >= starts_on),
    CONSTRAINT ck_promotions_usage_limit CHECK (usage_limit > 0),
    CONSTRAINT ck_promotions_usage_count CHECK (usage_count >= 0)
);
CREATE UNIQUE INDEX uq_promotions_code ON promotions (code) WHERE code IS NOT NULL;
CREATE INDEX ix_promotions_period ON promotions (starts_on, ends_on);
-- rollback DROP TABLE promotions;

-- changeset randy:2026_11_08-60-2
-- comment: PRO-01 - canaux de vente sur lesquels la promotion s'applique (au moins un)
CREATE TABLE promotion_channels
(
    promotion_id     UUID NOT NULL,
    sales_channel_id UUID NOT NULL,
    CONSTRAINT pk_promotion_channels PRIMARY KEY (promotion_id, sales_channel_id),
    CONSTRAINT fk_promotion_channels_promotion_id FOREIGN KEY (promotion_id) REFERENCES promotions (id),
    CONSTRAINT fk_promotion_channels_sales_channel_id FOREIGN KEY (sales_channel_id) REFERENCES sales_channels (id)
);
CREATE INDEX ix_promotion_channels_sales_channel_id ON promotion_channels (sales_channel_id);
-- rollback DROP TABLE promotion_channels;

-- changeset randy:2026_11_08-60-3
-- comment: PRO-01 - gammes (scope RANGES) ou articles (scope ARTICLES) cibles ; vide pour tout le catalogue
CREATE TABLE promotion_targets
(
    promotion_id UUID NOT NULL,
    target_id    UUID NOT NULL,
    CONSTRAINT pk_promotion_targets PRIMARY KEY (promotion_id, target_id),
    CONSTRAINT fk_promotion_targets_promotion_id FOREIGN KEY (promotion_id) REFERENCES promotions (id)
);
CREATE INDEX ix_promotion_targets_target_id ON promotion_targets (target_id);
-- rollback DROP TABLE promotion_targets;

-- changeset randy:2026_11_08-60-4
-- comment: PRO-01 - permission de creation des promotions
INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'promotions.create', 'Creer une promotion', now(), now());
-- rollback DELETE FROM permissions WHERE code = 'promotions.create';

-- changeset randy:2026_11_08-60-5
-- comment: PRO-01 - la creation des promotions est reservee a l'administrateur
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.code = 'promotions.create'
WHERE r.name = 'ADMIN';
-- rollback DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'promotions.create');
