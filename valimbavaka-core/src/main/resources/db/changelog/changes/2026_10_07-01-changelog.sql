-- liquibase formatted sql

-- changeset judic:2026_10_07-01-1
-- comment: Table role (feature iam)
CREATE TABLE role
(
    id          UUID                           NOT NULL,
    created_at  TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at  TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    created_by  BIGINT,
    updated_by  BIGINT,
    name        VARCHAR(100)                   NOT NULL,
    description TEXT,
    CONSTRAINT pk_role PRIMARY KEY (id),
    CONSTRAINT uc_role_name UNIQUE (name)
);
-- rollback DROP TABLE role;

-- changeset judic:2026_10_07-01-2
-- comment: Table users (feature iam) — un seul rôle par utilisateur
CREATE TABLE users
(
    id         UUID                           NOT NULL,
    created_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,
    email      VARCHAR(255)                   NOT NULL,
    password   VARCHAR(255)                   NOT NULL,
    first_name VARCHAR(100)                   NOT NULL,
    last_name  VARCHAR(100)                   NOT NULL,
    role_id    UUID                           NOT NULL,
    active     BOOLEAN                        NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uc_users_email UNIQUE (email),
    CONSTRAINT fk_users_on_role FOREIGN KEY (role_id) REFERENCES role (id)
);
CREATE INDEX idx_users_role_id ON users (role_id);
-- rollback DROP TABLE users;

-- changeset judic:2026_10_07-01-3
-- comment: Rôles initiaux
INSERT INTO role (id, created_at, updated_at, name, description)
VALUES (gen_random_uuid(), now(), now(), 'ADMIN', 'Administrateur'),
       (gen_random_uuid(), now(), now(), 'USER', 'Utilisateur standard');
-- rollback DELETE FROM role WHERE name IN ('ADMIN', 'USER');
