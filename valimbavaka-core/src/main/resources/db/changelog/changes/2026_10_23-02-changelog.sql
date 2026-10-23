-- liquibase formatted sql

-- changeset randy:2026_10_23-02-1
-- comment: SYS-06 - suppression des tables techniques du premier changeset (dev, revinfo, revchanges) : aucune entite ne les utilise et dev gardait un audit en BIGINT
DROP TABLE revchanges;
DROP TABLE revinfo;
DROP SEQUENCE revinfo_seq;
DROP TABLE dev;
-- rollback CREATE SEQUENCE IF NOT EXISTS revinfo_seq START WITH 1 INCREMENT BY 50;
-- rollback CREATE TABLE dev (id UUID NOT NULL, created_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL, updated_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL, created_by BIGINT, updated_by BIGINT, name VARCHAR(255) NOT NULL, CONSTRAINT pk_dev PRIMARY KEY (id), CONSTRAINT uc_dev_name UNIQUE (name));
-- rollback CREATE TABLE revinfo (rev BIGINT NOT NULL, revtstmp BIGINT, CONSTRAINT pk_revinfo PRIMARY KEY (rev));
-- rollback CREATE TABLE revchanges (rev BIGINT NOT NULL, entityname VARCHAR(255), CONSTRAINT fk_revchanges_on_default_tracking_modified_entities_changelog FOREIGN KEY (rev) REFERENCES revinfo (rev));
