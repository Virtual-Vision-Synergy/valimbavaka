# Guide d'intégration — JNL-01 · Journal append-only des opérations

Statut : ✔ rempli depuis le code et les tests au 2026-10-23 — à valider par le traiteur · Ticket : [#14](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/14) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Brique commune de traçabilité : chaque module appelle `JournalService.record(...)` pour toute opération métier. La ligne écrite est horodatée, nominative (auteur = compte authentifié) et ne peut plus être modifiée ni supprimée : la base elle-même refuse `UPDATE`, `DELETE` et `TRUNCATE`. L'administrateur consulte le journal du plus récent au plus ancien.

## Contrat exposé

### Endpoint backend

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/journal` | `journal.read` (administrateur seul) | `PageQuery` : `page` (base 1), `size` (≤ 100), `search`, `sortBy`, `sortDirection` | 200 `PageResponse<JournalEntryRDto>` | 401, 403 |

- Tri autorisé : `occurredAt` (défaut, décroissant), `actorName`, `action`, `resourceType`.
- `search` : insensible à la casse sur l'auteur, le type de ressource et le libellé.
- **Aucune écriture n'est exposée** : ni `POST`, `PUT`, `PATCH`, `DELETE` sur `/api/v1/journal` (le test `journal_writeVerbs_areNotExposed` le vérifie).

`JournalEntryRDto` : `id`, `occurredAt`, `actorId` (nul pour le système), `actorName`, `action` (`CREATE`, `UPDATE`, `DELETE`, `CANCEL`, `REVERSE`), `resourceType`, `resourceId`, `description`.

### Service Java à appeler depuis les autres features

```java
// feature.journal.service.JournalService
void record(JournalAction action, String resourceType, UUID resourceId, String description);
```

- `resourceType` en majuscules (ex. `USER`, `SUPPLIER`), `resourceId` = UUID de la ressource, `description` lisible par l'administrateur, **sans donnée sensible** (mot de passe, jeton).
- S'exécute dans la **transaction de l'appelant** : si la trace ne peut pas s'écrire, l'opération métier est annulée. L'appelant doit donc être `@Transactional`.
- Auteur : le compte authentifié (claim `sub` du jeton) ; « Système » hors requête authentifiée ; « Compte inconnu » (avec l'identifiant conservé) si le compte n'existe plus. Le nom est figé au moment de l'opération.
- Les écritures de journal ne sont pas rejouées par l'idempotence : c'est l'opération métier qui l'est (voir [SYS-02](../SYS-02/guide.md)).

### Frontend (`features/journal`)

- Route lazy `/journal` (permission `journal.read`), page `pages/journal-list-page` : qui a fait quoi, action traduite, recherche, pagination serveur, message si le journal est vide ou en erreur, aucune action de modification ou de suppression.
- `data-access/journal-service.ts` : `list(query)` en lecture seule ; types dans `journal-models.ts` (`JournalEntryRDto`, `JournalAction`).

### Données

- Table `journal_entries` (`id` UUID v7, `occurred_at` `TIMESTAMPTZ`, `actor_id`, `actor_name`, `action`, `resource_type`, `resource_id`, `description` ≤ 500) avec index sur `occurred_at` (décroissant), `actor_id` et `(resource_type, resource_id)`.
- Append-only garanti **en base** par les triggers `trg_journal_entries_append_only` (UPDATE / DELETE) et `trg_journal_entries_no_truncate` (TRUNCATE) qui lèvent une exception ; contrainte `CHECK` sur `action` (étendue à `CANCEL`, `REVERSE` par JNL-02).
- Permission `journal.read` créée et attribuée à `ADMIN` (migration `2026_10_19-02`).

## Règles métier à respecter par les autres tickets

- **Toute opération métier appelle `JournalService.record(...)`** dans la même transaction : création, modification, désactivation, annulation, contre-passation, clôture, etc. Aucun ticket ne doit écrire dans `journal_entries` autrement.
- Choisir l'action la plus précise : `CREATE`, `UPDATE`, `DELETE`, `CANCEL`, `REVERSE`.
- Ne pas tracer si rien n'a changé (état déjà celui demandé) : les services existants (`setActive`) n'écrivent aucune ligne dans ce cas.
- Ne jamais mettre de mot de passe, jeton ou donnée personnelle superflue dans `description`.
- Ne pas créer de repository qui permettrait de supprimer ou modifier une ligne : `JournalEntryRepository` étend `Repository` (pas `JpaRepository`) et n'a que `save` et `search`.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/journal/`)
- `controller/JournalController.java`
- `domain/JournalEntry.java` (entité `@Immutable`), `domain/JournalAction.java`
- `dto/JournalEntryRDto.java`, `mapper/JournalMapper.java`
- `repository/JournalEntryRepository.java`
- `service/JournalService.java`, `service/impl/JournalServiceImpl.java`

**Migrations Liquibase**
- `valimbavaka-core/src/main/resources/db/changelog/changes/2026_10_19-02-changelog.sql` (table, triggers, permission)
- `valimbavaka-core/src/main/resources/db/changelog/changes/2026_10_20-02-changelog.sql` (actions `CANCEL`, `REVERSE`, voir JNL-02)

**Frontend** (`valimbavaka-ui/src/app/features/journal/`)
- `journal.routes.ts`, `data-access/journal-service.ts`, `data-access/journal-models.ts`, `pages/journal-list-page/*`

**Tests**
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/journal/controller/JournalSecurityIT.java`
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/journal/repository/JournalAppendOnlyIT.java`
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/journal/service/impl/JournalServiceImplTest.java`
- `valimbavaka-ui/src/app/features/journal/data-access/journal-service.spec.ts`, `pages/journal-list-page/journal-list-page.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** :
- [AUTH-01](../AUTH-01/guide.md) (permissions, auteur authentifié), [SYS-06](../SYS-06/guide.md) (auteur des opérations en UUID, audit)

**Ces tickets s'appuient sur lui** : tous les tickets qui écrivent une donnée métier, dont [AUTH-03](../AUTH-03/guide.md), [AUTH-04](../AUTH-04/guide.md), [JNL-02](../JNL-02/guide.md), [FRS-01](../FRS-01/guide.md), [FRS-02](../FRS-02/description.md), [ART-01](../ART-01/description.md), [STK-01](../STK-01/description.md), [PRD-01](../PRD-01/description.md), [VTE-01](../VTE-01/description.md) et la suite (liste complète dans `description.md`).

## Comment l'utiliser depuis un autre ticket

```java
@Service
@Transactional(readOnly = true)
public class SupplierServiceImpl implements SupplierService {

    private final JournalService journalService;

    @Override
    @Transactional
    public SupplierRDto createSupplier(SupplierWDto request) {
        var saved = supplierRepository.save(...);
        journalService.record(JournalAction.CREATE, "SUPPLIER", saved.getId(),
                "Création du fournisseur " + saved.getName());
        return supplierMapper.toRDto(saved);
    }
}
```

## Points d'attention

- `GET /api/v1/journal` n'a pas de filtre par ressource, par auteur ou par période : seule la recherche texte existe. Un ticket qui a besoin de l'historique d'une ressource précise (par exemple [ART-06](../ART-06/description.md), [LIV-07](../LIV-07/description.md)) devra ajouter ce filtre.
- Le journal ne stocke qu'un libellé : pas de valeur avant / après. Les tickets qui exigent le détail d'un changement doivent le mettre dans `description` (≤ 500 caractères) ou prévoir un autre mécanisme.
- Le critère « chaque opération métier génère une ligne » est garanti par convention d'appel, pas par un mécanisme automatique : un module qui oublie `record` n'est pas détecté par un test global.
- Les tests d'intégration Testcontainers (triggers de base) demandent Docker ; ils tournent en CI.
