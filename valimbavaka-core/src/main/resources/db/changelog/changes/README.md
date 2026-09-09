# Migrations Liquibase

Liquibase est la source de verite unique du schema (`spring.jpa.hibernate.ddl-auto=validate`).

## Regles

1. Un fichier = un changement logique coherent (une table, un ajout de colonne, un index...).
2. **Jamais modifie une fois merge sur `main`** — Liquibase calcule un checksum par changeset ;
   modifier un fichier deja applique casse la synchro pour toute l'equipe. Besoin de corriger ?
   Nouveau fichier, nouveau changeset.
3. Nommage : `<sequence>__<feature>__<description-kebab-case>.sql`
    - `sequence` : 4 chiffres zero-paddes, strictement croissants (`0001`, `0002`, ...).
    - `feature` : le package feature concerne (`auth`, `stock`, `order`...) ou `core` pour le transverse.
    - `description` : kebab-case, explicite.
    - Exemple : `0001__auth__create-users-table.sql`
4. Chaque fichier est un changelog SQL formate Liquibase avec un `changeSet` unique et un
   `rollback` explicite.

## Gabarit

    --liquibase formatted sql

    --changeset <auteur>:0001__auth__create-users-table
    --comment: Table users pour la feature auth
    CREATE TABLE users (
        id              UUID PRIMARY KEY,
        email           VARCHAR(255) NOT NULL UNIQUE,
        password_hash   VARCHAR(255) NOT NULL,
        status          VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
        created_at      TIMESTAMP    NOT NULL DEFAULT now(),
        updated_at      TIMESTAMP    NOT NULL DEFAULT now()
    );
    --rollback DROP TABLE users;

## Avant d'ouvrir une PR

- Le changeset id est unique dans tout le projet.
- Un `--rollback` exploitable est fourni.
- La migration a ete testee en local (`docker compose up -d postgres` puis lancer l'appli).