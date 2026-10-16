# Migrations Liquibase

Liquibase est la source de verite unique du schema (`spring.jpa.hibernate.ddl-auto=validate`).

## Regles

1. Un fichier = un changement logique coherent (une table, un ajout de colonne, un index...).
2. **Jamais modifie une fois merge sur `main`** — Liquibase calcule un checksum par changeset ;
   modifier un fichier deja applique casse la synchro pour toute l'equipe. Besoin de corriger ?
   Nouveau fichier, nouveau changeset.
3. Nommage (celui deja en place dans le depot) : `<AAAA_MM_JJ>-<NN>-changelog.sql`, declare
   explicitement dans `db.changelog-master.yaml`. Identifiants de changeset :
   `<auteur>:<AAAA_MM_JJ>-<NN>-<n>` (ex. `mana:2026_10_15-01-1`). La date evite les collisions
   entre branches ; un fichier regroupe les changesets d'un meme ticket.
4. Chaque changeset porte un `-- comment:` (ticket + intention) et un `-- rollback` explicite.
5. Conventions SQL (convention VVS section 8.2) : tables au pluriel en snake_case, booleens
   `is_`/`has_`, contraintes `pk_`/`uq_`/`fk_`/`ck_`, index `ix_<table>_<colonnes>`,
   instants en `TIMESTAMPTZ`, auteurs d'audit en `UUID`.

## Gabarit

    -- liquibase formatted sql

    -- changeset <auteur>:2026_10_15-01-1
    -- comment: STK-01 - table des produits
    CREATE TABLE products
    (
        id         UUID         NOT NULL,
        name       VARCHAR(100) NOT NULL,
        is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
        created_at TIMESTAMPTZ  NOT NULL,
        updated_at TIMESTAMPTZ  NOT NULL,
        created_by UUID,
        updated_by UUID,
        version    BIGINT       NOT NULL DEFAULT 0,
        CONSTRAINT pk_products PRIMARY KEY (id),
        CONSTRAINT uq_products_name UNIQUE (name)
    );
    -- rollback DROP TABLE products;

## Avant d'ouvrir une PR

- Le changeset id est unique dans tout le projet.
- Un `--rollback` exploitable est fourni.
- La migration a ete testee en local (`docker compose up -d postgres` puis lancer l'appli).