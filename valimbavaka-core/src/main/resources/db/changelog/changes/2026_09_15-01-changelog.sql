-- liquibase formatted sql

-- changeset judic:1789471390262-1
CREATE SEQUENCE IF NOT EXISTS revinfo_seq START WITH 1 INCREMENT BY 50;

-- changeset judic:1789471390262-2
CREATE TABLE dev
(
    id         UUID                           NOT NULL,
    created_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,
    name       VARCHAR(255)                   NOT NULL,
    CONSTRAINT pk_dev PRIMARY KEY (id)
);

-- changeset judic:1789471390262-3
CREATE TABLE revchanges
(
    rev        BIGINT NOT NULL,
    entityname VARCHAR(255)
);

-- changeset judic:1789471390262-4
CREATE TABLE revinfo
(
    rev      BIGINT NOT NULL,
    revtstmp BIGINT,
    CONSTRAINT pk_revinfo PRIMARY KEY (rev)
);

-- changeset judic:1789471390262-5
ALTER TABLE dev
    ADD CONSTRAINT uc_dev_name UNIQUE (name);

-- changeset judic:1789471390262-6
ALTER TABLE revchanges
    ADD CONSTRAINT fk_revchanges_on_default_tracking_modified_entities_changelog FOREIGN KEY (rev) REFERENCES revinfo (rev);

