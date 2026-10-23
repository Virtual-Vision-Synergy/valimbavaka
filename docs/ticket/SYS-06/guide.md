# Guide d'intégration — SYS-06 · Audit : auteur des opérations en UUID

Statut : ✅ complet au 2026-10-23 — vérifié sur le code, les tests (CI + exécution locale) et la base · Ticket : [#112](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/112) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Toute entité qui hérite de `BaseEntity` reçoit automatiquement un identifiant UUID v7, les dates de création et de modification, l'UUID du compte qui l'a créée (`created_by`) et modifiée (`updated_by`), et une version pour le verrou optimiste. L'auteur vient du jeton de l'utilisateur connecté : il n'est jamais saisi à la main ni fourni par un DTO.

## Contrat exposé

### Type partagé : `shared/domain/BaseEntity`

| Champ | Colonne | Remplissage |
|---|---|---|
| `id` | `id` UUID | `@UuidGenerator(style = VERSION_7)` |
| `createdAt` / `updatedAt` | `created_at` / `updated_at` `TIMESTAMPTZ` | audit JPA (`@CreatedDate`, `@LastModifiedDate`), en UTC |
| `createdBy` / `updatedBy` | `created_by` / `updated_by` **UUID**, nullables | audit JPA (`@CreatedBy`, `@LastModifiedBy`) via `SecurityAuditorAware` |
| `version` | `version` BIGINT, défaut 0 | `@Version` (verrou optimiste ; aujourd'hui un conflit remonte en 500, voir points d'attention) |

- `SecurityAuditorAware implements AuditorAware<UUID>` (`security/audit`) lit le claim `sub` du jeton d'accès via `JwtSubject.userIdOf`. Hors requête authentifiée (démarrage, tâche planifiée), l'auteur est vide, donc `null`.
- `AuditableUser` (`security/audit`) est le contrat du principal (`UUID getId()`), porté par `security/` pour qu'il ne dépende d'aucune feature.
- Les entités héritent de `BaseEntity` ; les DTO d'entrée ne contiennent jamais `id`, `createdBy`, `updatedBy`.
- Les entités qui en héritent aujourd'hui : utilisateur, rôle, référentiels (canaux, gammes, emplacements), fournisseur, jetons de rafraîchissement ; `JournalEntry` a son propre auteur nominatif (`actor_id`, `actor_name`).

### Migrations

- `2026_10_15-01-4` : `created_by` / `updated_by` passés de BIGINT à UUID (anciennes valeurs mises à `NULL`), `created_at` / `updated_at` en `TIMESTAMPTZ`, colonne `version` sur `roles` et `users`.
- Les nouvelles tables (`sales_channels`, `product_ranges`, `stock_locations`, `suppliers`, `refresh_tokens`) créent directement `created_by UUID`, `updated_by UUID`, `version`.

## Règles métier pour les autres tickets

- **Toute nouvelle table métier** a les colonnes `id UUID`, `created_at`, `updated_at`, `created_by UUID`, `updated_by UUID`, `version BIGINT NOT NULL DEFAULT 0`, et l'entité hérite de `BaseEntity`.
- Ne jamais ajouter un `created_by` de type `BIGINT`, `VARCHAR` ou un nom d'utilisateur : toujours l'UUID.
- Ne jamais renseigner l'auteur à la main (`setCreatedBy`) : il n'y a pas de setter, et les tests ne doivent pas non plus l'imposer.
- Pour afficher « créé par », résoudre l'UUID au moment de la lecture ou le copier dans le journal ; l'UUID est stable, le nom peut changer ([JNL-01](../JNL-01/guide.md) fige le nom au moment de l'opération).
- Une entité mise à jour sans changement de valeur n'est pas modifiée : `updated_by` ne bouge pas.
- Toute modification concurrente lève une `ObjectOptimisticLockingFailureException`. Elle n'est pas encore traduite en 409 par `GlobalExceptionHandler` (réponse 500) : ajouter ce handler dans `core/exception` plutôt que dans chaque feature.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/`)
- `shared/domain/BaseEntity.java`
- `security/audit/SecurityAuditorAware.java`, `AuditableUser.java`
- `security/jwt/JwtSubject.java` (lecture du `sub`)

**Migration**
- `valimbavaka-core/src/main/resources/db/changelog/changes/2026_10_15-01-changelog.sql` (changeset `2026_10_15-01-4`)

**Tests** : voir le scénario ; la couverture directe est faible.

## Dépendances

**Ce ticket s'appuie sur** : [SYS-05](../SYS-05/guide.md).

**Ces tickets s'appuient sur lui** : [JNL-01](../JNL-01/guide.md), [LIV-07](../LIV-07/description.md) ; de fait toutes les entités métier.

## Comment l'utiliser depuis un autre ticket

```java
@Entity
@Table(name = "articles")
@Getter
public class Article extends BaseEntity {
    @Column(name = "name", nullable = false)
    private String name;
}
```

```sql
CREATE TABLE articles (
    id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_articles PRIMARY KEY (id)
);
```

## Points d'attention

- Les anciennes lignes `created_by` / `updated_by` ont été mises à `NULL` lors du changement de type (sans conséquence tant qu'il n'y avait pas de production).
- Les écritures faites au démarrage (administrateur initial) ou par une tâche planifiée n'ont pas d'auteur : `created_by` reste `NULL`. Un accompagnement par le journal (auteur « Système ») est possible via [JNL-01](../JNL-01/guide.md).
- L'audit ne s'applique qu'aux entités passant par JPA ; une écriture en SQL direct (`JdbcClient`, comme `idempotency_keys`) ne renseigne pas d'auteur.
- L'activation de l'audit JPA dépend du bean `AuditorAware<UUID>` : un second bean du même type casserait l'injection.
- La table technique `dev` (premier changeset `2026_09_15-01`), seule encore auditée en `bigint`, a été supprimée avec `revinfo` / `revchanges` par `2026_10_23-02`.
