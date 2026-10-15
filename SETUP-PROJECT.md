# SETUP-PROJECT — Mise en place de l'environnement de développement VVS

Ce guide met en place, pour toute l'équipe, la base de développement de **valimbavaka-core** (backend) et **valimbavaka-ui** (frontend) conforme à [`resources/convention-dev-VVS-V2.1.pdf`](resources/convention-dev-VVS-V2.1.pdf), avec **Liquibase comme source unique de vérité du schéma de base de données**.

À suivre une seule fois par développeur (setup initial), puis à chaque fois qu'une nouvelle feature démarre (squelette de dossiers).

---

## 0. Prérequis

| Outil | Version | Vérifier avec |
|---|---|---|
| JDK | 21 | `java -version` |
| Maven | 3.9+ (ou wrapper `./mvnw`) | `mvn -v` |
| Node.js | 24 (ou 22.22.3+, exigé par Angular 22) | `node -v` |
| npm | 11+ | `npm -v` |
| Docker Desktop | récent | `docker -v` |
| Git | récent | `git -v` |

IDE recommandé : IntelliJ IDEA / Rider (déjà configuré dans `.idea/`) + extension Angular Language Service côté frontend.

---

## 1. Base de données locale (Docker Compose + PostgreSQL)

Tout le monde travaille contre la **même image Postgres**, démarrée localement — jamais de partage d'une base distante en dev.

Créer `docker-compose.yml` à la racine du repo :

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: valimbavaka-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${DB_NAME:-valimbavaka}
      POSTGRES_USER: ${DB_USERNAME:-valimbavaka}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-valimbavaka}
    ports:
      - "${DB_PORT:-5432}:5432"
    volumes:
      - valimbavaka-postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME:-valimbavaka} -d ${DB_NAME:-valimbavaka}"]
      interval: 5s
      timeout: 5s
      retries: 10

volumes:
  valimbavaka-postgres-data:
```

Créer `.env.example` à la racine (chacun copie en `.env`, jamais commité) :

```
DB_HOST=localhost
DB_PORT=5432
DB_NAME=valimbavaka
DB_USERNAME=valimbavaka
DB_PASSWORD=valimbavaka
```

Ajouter au `.gitignore` racine : `.env`

Démarrer :
```bash
cp .env.example .env
docker compose up -d postgres
```

---

## 2. Liquibase — source unique de vérité du schéma

La dépendance `spring-boot-starter-liquibase` est déjà dans `valimbavaka-core/pom.xml`. Il reste à la câbler et à poser la convention de migration.

### 2.1 Configuration — `valimbavaka-core/src/main/resources/application.properties`

```properties
spring.application.name=valimbavaka-core

# --- Datasource (docker compose en local, override via variables d'env ailleurs) ---
spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:valimbavaka}
spring.datasource.username=${DB_USERNAME:valimbavaka}
spring.datasource.password=${DB_PASSWORD:valimbavaka}

# --- JPA / Hibernate ---
# Liquibase possede le schema. Hibernate ne cree/modifie JAMAIS une table, il valide seulement
# que les entites correspondent a ce que Liquibase a applique.
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
spring.jpa.properties.hibernate.format_sql=true

# --- Liquibase ---
spring.liquibase.enabled=true
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml
```

`ddl-auto=validate` est **non négociable** : c'est ce qui garantit que toute évolution de schéma passe par Liquibase et est donc tracée, revue en PR, et rejouable à l'identique sur tous les postes / environnements.

### 2.2 Structure des changelogs

```
valimbavaka-core/src/main/resources/db/changelog/
├── db.changelog-master.xml
└── changes/
    └── README.md
```

**`db.changelog-master.xml`**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        https://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <!--
        Chaque changement de schema = un fichier dans changes/, jamais modifie une fois merge sur main.
        Nommage : <sequence 4 chiffres>__<feature>__<description-kebab-case>.sql
        Le tri alphabetique du dossier = ordre d'application (d'ou le zero-padding).
        Voir changes/README.md pour le detail et le gabarit.
    -->
    <includeAll path="db/changelog/changes"
                relativeToChangelogFile="true"
                endsWithFilter=".sql"
                errorIfMissingOrEmpty="false"/>

</databaseChangeLog>
```

`endsWithFilter=".sql"` permet de garder un `README.md` dans `changes/` sans que Liquibase n'essaie de le parser (avec Liquibase 5.x, l'attribut `filter`/`resourceFilter` attend le nom qualifié d'une classe `IncludeAllFilter`, pas une regex — `endsWithFilter` est le bon attribut pour un simple filtre par suffixe). `errorIfMissingOrEmpty="false"` évite une erreur au tout premier démarrage quand le dossier est encore vide.

**`changes/README.md`** (convention pour toute l'équipe)
```markdown
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
```

### 2.3 Workflow quotidien

- Nouvelle table/colonne → nouveau fichier dans `changes/`, jamais d'édition d'un fichier déjà mergé.
- `mvn spring-boot:run` applique automatiquement les changelogs en attente au démarrage (comportement par défaut du starter).
- Besoin d'appliquer sans démarrer l'appli : `mvn liquibase:update`.
- Rollback du dernier changeset en local : `mvn liquibase:rollback -Dliquibase.rollbackCount=1`.

---

## 3. Architecture backend — Feature-Based Layering (FBL)

Package racine : `org.vvs.valimbavaka`. Le transverse (`core/`, `security/`, `shared/`) ne contient aucune logique métier et ne dépend d'aucune feature ; ces règles sont vérifiées par `ArchitectureTest` (ArchUnit) à chaque build.

```
org.vvs.valimbavaka
├── ValimbavakaCoreApplication.java
├── core
│   ├── config        ClockConfig, JpaAuditingConfig, WebConfig + CorsProperties (vvs.cors.*)
│   └── exception     BusinessException (422) ← NotFound (404), Conflict (409), Unauthorized (401),
│                     Forbidden (403) ; GlobalExceptionHandler → ProblemDetail RFC 9457 + "code"
├── security
│   ├── config        SecurityConfig (deny by default, chaîne /actuator/health, DelegatingPasswordEncoder)
│   ├── jwt           JwtFilter (en attente de la feature auth)
│   ├── handler       RestAuthenticationEntryPoint (401), RestAccessDeniedHandler (403), ProblemDetailWriter
│   ├── annotation    @HasPermission("users.create")
│   └── audit         SecurityAuditorAware (created_by / updated_by)
├── shared
│   ├── domain        BaseEntity (audit Instant/UUID + @Version ; l'@Id UUIDv7 est dans chaque entité)
│   └── dto           PageResponse (items, page base 1, pageSize, totalCount, totalPages), PageQuery, SortDirection
└── feature
    ├── user          controller · service/impl · domain · repository · dto · mapper · config · exception
    └── referential   controller · service/impl · domain · repository · dto · mapper
```

Règles à retenir (détail dans `resources/convention-dev-VVS-V2.1.pdf`) :

- Controllers minces : ils reçoivent et renvoient des DTO (`XxxRDto` / `XxxWDto`, records), sans enveloppe ni `try/catch`.
- Services : interface + `service/impl`, `@Transactional(readOnly = true)` sur la classe, `@Transactional` sur les écritures.
- Entités : constructeur `protected`, factory statique, pas de `@Setter`, `@Enumerated(STRING)`, `LAZY`.
- Listes : `PageQuery` en entrée (tri en liste blanche), `PageResponse` en sortie.
- Chaque package porte un `package-info.java` `@NullMarked` (JSpecify).
- Mappers MapStruct : `unmappedTargetPolicy = ERROR`, injection par constructeur.

### 3.1 Premier démarrage — compte administrateur (SYS-05)

Les rôles métier (`ADMINISTRATEUR`, `VENDEUSE`, `LIVREUR`) et les référentiels (canaux de vente, gammes, emplacements de stock) sont créés par Liquibase. Le premier compte administrateur, lui, vient de l'environnement — jamais d'un fichier versionné :

```properties
# .env (local) ou variables d'environnement (staging / prod, voir deploy/env.example)
ADMIN_EMAIL=admin@valimbavaka.mg
ADMIN_PASSWORD=un-mot-de-passe-provisoire-d-au-moins-15-caracteres
ADMIN_FIRST_NAME=Prénom
ADMIN_LAST_NAME=Nom
```

- Au démarrage, `AdminAccountInitializer` crée ce compte **uniquement si aucun administrateur n'existe** ; il devra changer son mot de passe à la première connexion.
- Base sans administrateur et variables absentes ou invalides : le démarrage est refusé avec un message qui nomme les variables en cause (jamais la valeur du mot de passe).
- Une fois le compte créé, les variables peuvent être retirées : elles sont ignorées.

### 3.2 Tests

```
src/test/java/org/vvs/valimbavaka
├── ArchitectureTest                 règles FBL (ArchUnit)
├── ValimbavakaCoreApplicationIT     démarrage + migrations sur base vierge
├── support/                         @IntegrationTest, TestcontainersConfiguration (postgres:16-alpine)
├── core/…, shared/…                 tests unitaires du transverse
└── feature/<feature>/…              miroir des packages main : *Test (unitaires) et *IT (intégration)
```

- `mvn test` : tests unitaires (Mockito, < 100 ms, pas de base). `mvn verify` : + tests `*IT` sur PostgreSQL via Testcontainers (**Docker requis**) + Checkstyle si `-Dcheckstyle.skip=false`.
- Nom des tests : `methode_scenario_resultatAttendu`, sujet testé nommé `sut`, blocs `// given / when / then`.

---

## 4. Architecture frontend — Angular par feature

```
src/app
├── app.ts · app.html · app.config.ts · app.routes.ts
├── core
│   ├── http        auth-interceptor.ts, error-interceptor.ts (HttpErrorResponse → ApiError), problem-detail.ts
│   ├── auth        auth-store.ts (jeton en mémoire), auth-guard.ts, has-permission-directive.ts
│   └── layout      main-layout/
├── shared
│   ├── components/
│   └── models      page-response.ts (PageResponse, PageQuery), http-params.ts
└── features
    ├── user
    │   ├── user.routes.ts      chargé en lazy loading depuis app.routes.ts
    │   ├── data-access/        user-models.ts (UserRDto, RoleName…), user-service.ts, role-service.ts
    │   └── pages/              user-list-page/, user-form-page/
    └── referential
        └── data-access/        referential-models.ts, referential-service.ts
```

- Composants standalone, `OnPush`, `inject()`, signaux ; zoneless (`provideZonelessChangeDetection`).
- Sélecteurs préfixés `vvs-` ; fichiers sans suffixe `.component` (`user-list-page.ts` → `UserListPage`), services `xxx-service.ts`.
- Une feature n'importe jamais une autre feature : le partagé va dans `shared/`.
- Les types reprennent exactement les DTO backend ; les erreurs 422 (`errors[].field`) sont réaffectées aux champs du formulaire.
- Tests : `npx ng test --watch=false` (Vitest), `npm run lint`, `npm run build`.

---

## 5. Qualité automatisée (recommandé)

### 5.1 Backend — Checkstyle

Ajouter dans `valimbavaka-core/pom.xml` (fait respecter automatiquement : ligne 120 car., méthode 30 lignes, 4 paramètres max, pas d'import wildcard) :

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.6.0</version>
    <configuration>
        <configLocation>checkstyle.xml</configLocation>
        <consoleOutput>true</consoleOutput>
        <failOnViolation>true</failOnViolation>
    </configuration>
    <executions>
        <execution>
            <phase>verify</phase>
            <goals><goal>check</goal></goals>
        </execution>
    </executions>
</plugin>
```

`valimbavaka-core/checkstyle.xml` (extrait minimal — à enrichir au besoin) :
```xml
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
        "https://checkstyle.org/dtds/configuration_1_3.dtd">
<module name="Checker">
    <module name="TreeWalker">
        <module name="LineLength"><property name="max" value="120"/></module>
        <module name="MethodLength"><property name="max" value="30"/></module>
        <module name="ParameterNumber"><property name="max" value="4"/></module>
        <module name="AvoidStarImport"/>
        <module name="UnusedImports"/>
        <module name="TypeName"/>
        <module name="MethodName"/>
        <module name="MemberName"/>
        <module name="ConstantName"/>
        <module name="PackageName"><property name="format" value="^[a-z]+(\.[a-z][a-z0-9]*)*$"/></module>
    </module>
</module>
```

Lancer : `mvn verify`

### 5.2 Frontend — ESLint

```bash
cd valimbavaka-ui
npx ng add @angular-eslint/schematics
```

Ajoute automatiquement la config recommandée + le target `lint` dans `angular.json`. Compléter ensuite `eslint.config.js` avec `"@typescript-eslint/no-explicit-any": "error"` pour interdire `any` sans dérogation explicite (convention section 1.4).

Lancer : `npm run lint`

---

## 6. Checklist finale

- [ ] `docker compose up -d postgres` démarre sans erreur
- [ ] `.env` créé localement (jamais commité)
- [ ] `application.properties` pointe sur Liquibase (`spring.liquibase.change-log=...`) et `ddl-auto=validate`
- [ ] `db/changelog/db.changelog-master.yaml` + `changes/README.md` en place
- [ ] `mvn spring-boot:run` démarre sans erreur (Liquibase s'exécute au boot, log `liquibase.changelog : Reading from ...`)
- [ ] Variables `ADMIN_*` renseignées dans `.env` (premier démarrage sur base vide, section 3.1)
- [ ] `mvn verify` passe (Docker démarré pour les tests Testcontainers)
- [ ] `npm run lint`, `npx ng test --watch=false` et `npm run build` passent
- [ ] `tsconfig.json` : `"strict": true`
- [ ] `npm start` (Angular) et `mvn spring-boot:run` (backend) tournent en parallèle sans erreur CORS
- [ ] (recommandé) Checkstyle backend + ESLint frontend branchés

Pour toute règle non couverte ici (nommage détaillé, SOLID, patterns, tests, grille de revue de code...), se référer à [`resources/convention-dev-VVS-V2.1.pdf`](resources/convention-dev-VVS-V2.1.pdf).
