# Guide d'intégration — SHO-01 · Enregistrement d'une sortie shooting

Statut : ✅ écrit le 2026-11-08 — **tests non exécutés** (voir « Résultat de l'exécution » du scénario) · Ticket : [#43](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/43) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Une **sortie shooting** retire des produits d'un emplacement de stock pour une séance photo, sans les compter comme perte : elle crée une opération `shootings` (statut `OUT`, « Sorti ») et, dans la même transaction, un mouvement de stock `SHOOTING` négatif par article (port STK-01). Le domaine (entité, statut, retard calculé, numéro `SHO-xxxx`) est prêt pour la réintégration (**SHO-02**) et le suivi des sorties en cours (**SHO-03**).

## Contrat exposé

### Endpoints backend (`/api/v1/shootings`)

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/shootings` | `services.read` (administrateur, vendeuse) | `PageQuery` (`page`, `size` ≤ 100, `search` sur numéro et auteur, `sortBy` ∈ `createdAt` (défaut, DESC), `number`, `expectedReturnAt`, `status`) + `status` facultatif (`OUT`, `RETURNED`) | 200 `PageResponse<ShootingRDto>` | 401, 403, 422 |
| GET | `/api/v1/shootings/{id}` | `services.read` | — | 200 `ShootingRDto` | 401, 403, 404 |
| POST | `/api/v1/shootings` | `shootings.create` (administrateur, vendeuse) | `ShootingWDto` | 201 `ShootingRDto` + `Location` | 401, 403, 404 (article), 409 (stock insuffisant), 422 |

- `ShootingWDto` : `locationId` (emplacement de sortie, obligatoire), `items` (1 à 50 `ShootingItemWDto` : `articleId`, `quantity` > 0, 3 décimales au plus), `expectedReturnAt` (instant ISO, obligatoire), `note` (≤ 255, facultative). Ni numéro, ni statut, ni auteur : `id`, `number`, `status`, `takenBy`, `late` envoyés par le client sont ignorés.
- `ShootingRDto` : `id`, `number` (`SHO-0001`…), `locationId`, `items` (`articleId`, `articleCode`, `articleName`, `quantity`), `takenById`, `takenBy` (nom complet du compte qui a saisi), `expectedReturnAt`, `note`, `status` (`OUT` | `RETURNED`), `late` (encore `OUT` après `expectedReturnAt`), `createdAt`.
- Pas de `PUT`, `PATCH` ni `DELETE` (JNL-02) : la seule suite prévue d'une sortie est sa réintégration (SHO-02).

### Services et ports Java réutilisables

| Élément | Rôle |
|---|---|
| `feature.shooting.service.ShootingService` | `getShootings(PageQuery, ShootingStatus)`, `getShootingById(UUID)`, `createShooting(ShootingWDto)` |
| `feature.shooting.domain.Shooting` | entité (`BaseEntity` : version, `createdBy`) ; `open(number, ShootingDraft)` (`domain/Shooting.java:63`) pose `OUT` ; `isLateAt(Instant)` (`:76`) ; articles en `@ElementCollection` `ShootingItem` (code et nom copiés à la sortie) |
| `feature.shooting.domain.ShootingStatus` | `OUT`, `RETURNED` (la contrainte `CHECK` accepte déjà `RETURNED` : SHO-02 n'a pas de migration de statut à faire) |
| `service/impl/ShootingPolicy` | règles d'une nouvelle sortie (`draft`, `:48`) et calcul du retard avec le `Clock` (`isLate`, `:64`) |
| `repository/ShootingRepository` | étend `OperationRepository` (pas de suppression) ; `nextNumber()` (séquence), `search(search, status, pageable)` |

### Frontend (`features/services`)

- Route `/services` inchangée (permission `services.read`, toujours marquée `underConstruction` car le salon reste en démonstration).
- `data-access/shooting-service.ts` : `list()` lit les 100 sorties les plus récentes et renvoie des `ShootingRowRDto` ; `stock()` lit `GET /api/v1/stocks` (articles et quantité par emplacement) pour le formulaire ; `create(ShootingWDto)` passe par la file hors ligne (`OutboxService`, `Idempotency-Key`).
- `data-access/shooting-mapping.ts` : `toShootingRow`, `displayStatus` (`RETURNED` › `LATE` si `late` › `OUT`), `toLocalTimestamp` (instant → `AAAA-MM-JJTHH:MM` local).
- `data-access/service-models.ts` : types du backend `ShootingRDto`, `ShootingItemRDto`, `ShootingWDto`, `ShootingItemWDto`, `ShootingStoredStatus` ; type d'affichage renommé `ShootingRowRDto` (ex-`ShootingRDto` de la maquette) pour `ShootingList`, composant inchangé.
- `components/shooting-form` (nouveau) : emplacement (boutique par défaut), articles + quantités avec le disponible de l'emplacement, retour prévu (`datetime-local`, demain par défaut), note ; émet `submitted: ShootingWDto`. Affiché dans l'onglet Shooting si l'utilisateur a `shootings.create`.
- `pages/services-page` : les sorties (onglet Shooting et encart « Sorties en cours » du salon) viennent de l'API ; toast « Sortie SHO-xxxx enregistrée : statut Sorti » ou `common.queuedSaved` hors ligne ; erreur serveur affichée sous le formulaire.

### Données

- Migration `2026_11_08-61-changelog.sql` (5 changesets, rollback fourni) : séquence `shooting_number_seq` ; table `shootings` (`number` unique, `location_id` → `stock_locations`, `taken_by_name`, `expected_return_at`, `note`, `status` CHECK `OUT`/`RETURNED`, colonnes d'audit et `version`) ; table `shooting_items` (clé `shooting_id` + `line_no`, `article_id` → `articles`, `article_code`, `article_name`, `quantity` > 0, un article une fois par sortie) ; permission `shootings.create` donnée à `ADMIN` et `SELLER`.
- Stock : un mouvement `SHOOTING` de `−quantité` par article sur `location_id`, `reference_id` = id de la sortie.
- Journal : ressource `SHOOTING`, action `CREATE`, libellé « Sortie shooting SHO-0042 : 2 article(s), retour prévu le 2026-11-08T16:00:00Z » ; plus une ligne `STOCK_MOVEMENT` par article (STK-01).

## Règles métier pour les autres tickets

- **Statut Sorti = `OUT`** dès l'enregistrement ; « En retard » n'est **pas stocké** : `late` = `OUT` et `expectedReturnAt` < maintenant (horloge injectée).
- **Tout ou rien** : si un article manque de stock (409) ou est inconnu (404), rien n'est écrit (ni sortie, ni mouvement, ni journal).
- Retour prévu strictement dans le futur, emplacement actif, chaque article une seule fois (422 sinon, avant toute écriture).
- L'auteur est le compte authentifié (`createdBy`, nom copié dans `taken_by_name`) : pas de saisie libre de « qui a pris ».
- **SHO-02 (réintégration)** : retrouver les quantités dans `Shooting.getItems()`, enregistrer pour chacune un mouvement `RETURN` positif (l'entrepôt d'après son critère, ou `getLocationId()` si le client préfère l'emplacement d'origine) avec `referenceId` = id de la sortie, passer le statut à `RETURNED` (méthode à ajouter sur `Shooting`, contrôle « déjà réintégrée » → 409), journal `UPDATE` `SHOOTING`, permission à créer (`shootings.return`, administrateur) ; brancher `ShootingList.returned` (aujourd'hui simulé par `SalonService.returnShooting`).
- **SHO-03 (suivi)** : sorties en cours = `status = OUT` (`GET /api/v1/shootings?status=OUT`), en retard = `late` ; valeur immobilisée = Σ quantité × coût (`ArticleService.getCosts`) ; prévoir une requête d'agrégat dédiée plutôt que la page de 100.
- JR-04 (clôture) : la colonne shooting de la réconciliation = somme des mouvements `SHOOTING` du jour.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/shooting/`)
- `controller/ShootingController.java`
- `service/ShootingService.java` ; `service/impl/ShootingServiceImpl.java`, `service/impl/ShootingPolicy.java`
- `domain/Shooting.java`, `ShootingItem.java`, `ShootingStatus.java`, `ShootingDraft.java`
- `repository/ShootingRepository.java` ; `dto/ShootingWDto.java`, `ShootingItemWDto.java`, `ShootingRDto.java`, `ShootingItemRDto.java` ; `mapper/ShootingMapper.java`

**Migrations Liquibase**
- `valimbavaka-core/src/main/resources/db/changelog/changes/2026_11_08-61-changelog.sql`, déclarée dans `db.changelog-master.yaml`

**Frontend** (`valimbavaka-ui/src/app/features/services/`)
- `data-access/shooting-service.ts`, `shooting-mapping.ts`, `service-models.ts`, `service-fixtures.ts` (sorties de démonstration retirées), `service-service.ts` (commentaire)
- `components/shooting-form/*` (nouveau), `components/shooting-list/shooting-list.ts` (type renommé)
- `pages/services-page/*`
- `public/i18n/fr.json`, `mg.json` : clés `services.shooting.form.*`

**Tests**
- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/shooting/` (`domain/ShootingTest`, `service/impl/ShootingPolicyTest`, `service/impl/ShootingServiceImplTest`, `controller/ShootingCreationIT`, `controller/ShootingSecurityIT`, support `controller/ShootingApiTestSupport`)
- Frontend : `data-access/shooting-mapping.spec.ts`, `data-access/shooting-service.spec.ts`, `components/shooting-form/shooting-form.spec.ts`

## Dépendances

- Dépend de : [STK-01](../STK-01/guide.md) (`StockMovementService.record`, type `SHOOTING`, lecture `GET /api/v1/stocks`), [JNL-02](../JNL-02/guide.md) (`OperationRepository`), [JNL-01](../JNL-01/guide.md) (`JournalService.record`), [ART-01](../ART-01/guide.md) (`ArticleService.getArticleById`), [SYS-05](../SYS-05/guide.md) (`ReferentialService.isActiveStockLocation`), [SYS-07](../SYS-07/guide.md) (file hors ligne).
- Débloque : [SHO-02](../SHO-02/guide.md), [SHO-03](../SHO-03/guide.md).

## Comment l'utiliser depuis un autre ticket

Lire les sorties encore dehors (SHO-03), côté Java :

```java
PageResponse<ShootingRDto> out = shootingService.getShootings(
        new PageQuery(1, 100, null, "expectedReturnAt", SortDirection.ASC), ShootingStatus.OUT);
```

Côté interface : `inject(ShootingService).list()` pour les lignes affichables, `create(dto)` pour une nouvelle sortie (résultat `SubmitResult<ShootingRDto>`, `queued` si hors ligne).

## Points d'attention

- **Hypothèse (pas de règle client écrite)** : « stock vendable » = stock de l'emplacement de sortie, la **boutique** proposée par défaut ; tout emplacement actif est accepté. À confirmer par Randy ; SHO-02 réintègre « en entrepôt » d'après son critère, ce qui ferait un transfert implicite boutique → entrepôt.
- **Lecture avec `services.read`** (permission existante du module Salon & shooting) ; seule l'écriture a une permission dédiée `shootings.create`.
- La sortie n'implémente pas `ReversibleOperationService` (annulation / contre-passation) : une erreur de saisie se corrige aujourd'hui par la réintégration (SHO-02). Une annulation à chaud reste à décider.
- Le formulaire propose les **100 premiers articles actifs** par nom (limite de `GET /api/v1/stocks`), comme la page des stocks ; les emplacements viennent du détail de stock du premier article.
- La réintégration (« Réintégrer ») reste **simulée** côté interface jusqu'à SHO-02.
- Pas d'annulation de sortie en file hors ligne : si le serveur la refuse au rejeu (stock vendu entre-temps → 409), elle reste en échec dans la file (SYS-07).
- Tests non exécutés : voir le scénario avant de s'appuyer sur ce contrat.
