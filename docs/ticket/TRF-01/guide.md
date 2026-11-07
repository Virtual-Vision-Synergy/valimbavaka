# Guide d'intégration — TRF-01 · Historique des transferts de stock

Statut : ✅ écrit le 2026-11-08 — tests verts en CI (voir « Résultat de l'exécution » du scénario) · Ticket : [#35](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/35) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

L'administrateur consulte l'historique paginé des transferts de stock (retours, transferts du matin, transferts manuels) : chaque ligne donne la référence, la date, l'emplacement d'origine et de destination (libellés), l'origine du bon, le statut et l'émetteur (nom complet). Le serveur fait le travail de résolution (libellés des emplacements, nom de l'émetteur) en une passe par page ; l'écran `/transfers` n'affiche plus de données de démonstration pour la liste.

## Contrat exposé

### Endpoints backend (`/api/v1/transfers`)

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie (`XxxRDto`) | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/transfers` | `transfers.read` (administrateur seulement) | `PageQuery` (`page` base 1, `size` ≤ 100, `search`, `sortBy`, `sortDirection`) + `origin` facultatif (`MORNING`, `MANUAL`, `RETURN`) | 200 `PageResponse<TransferSummaryRDto>` | 400 (`origin` inconnue), 401, 403, 422 (`page` < 1, `size` > 100) |

- Contrôleur : `TransferController.getTransfers` (`feature/transfer/controller/TransferController.java:57`).
- Tri par défaut `createdAt` décroissant (plus récent d'abord) ; liste blanche `createdAt`, `number`, `origin`, `status` (`TransferServiceImpl.SORT_FIELDS`) ; tout autre `sortBy` retombe sur la date.
- `search` : contient, insensible à la casse, sur le numéro (`RET-0015`, `trf-00`).
- `TransferSummaryRDto` (`feature/transfer/dto/TransferSummaryRDto.java`) : `id`, `number`, `origin`, `status`, `fromLocationId`, `fromLocationLabel`, `toLocationId`, `toLocationLabel`, `lineCount`, `quantity` (somme des lignes), `createdAt` (date du transfert, instant UTC), `createdBy` (UUID ou `null`), `createdByName`.
  - Libellé d'emplacement `null` si l'emplacement n'est plus actif.
  - `createdByName` : « Prénom Nom » ; « Système » si le transfert a été généré hors requête (`createdBy` `null`, ex. TRF-02) ; « Compte inconnu » si le compte n'existe plus.

### Services et ports Java réutilisables

| Port | Méthode | Rôle |
|---|---|---|
| `TransferService` (`service/TransferService.java:34`) | `PageResponse<TransferSummaryRDto> getTransfers(PageQuery query, @Nullable TransferOrigin origin)` | Historique paginé, réutilisable par un export (SYS-01) ou un tableau de bord. |
| `TransferRepository` (`repository/TransferRepository.java:40`) | `Page<TransferSummary> search(String search, @Nullable TransferOrigin origin, Pageable pageable)` | Une seule requête JPQL (jointure sur `transfer_lines`, `count` et `sum` groupés par transfert) : pas de N+1 sur les lignes. Projection `TransferSummary` (`repository/TransferSummary.java`). |
| `TransferHistory` (`service/impl/TransferHistory.java:41`, interne) | `toRDtos(Page<TransferSummary>)` | Libellés des emplacements actifs (`ReferentialService.getStockLocations`, une lecture par page) et nom de l'émetteur (port `AuthenticatedUserLoader.findById`, une lecture par émetteur distinct de la page). |

### Frontend (`features/transfers`)

- Route `/transfers` (garde `permissionGuard('transfers.read')`, posé par TRF-03 dans `transfers.routes.ts`) : `pages/transfer-list-page`.
- `data-access/transfer-service.ts:30` : `list(query: TransferListQuery)` → `GET /api/v1/transfers` (`PageResponse<TransferSummaryRDto>`), `origin` envoyé seulement s'il est fourni. Lecture seule : pas de file hors ligne.
- `data-access/transfer-models.ts` : `TransferSummaryRDto`, `TransferListQuery` (= `PageQuery` + `origin?`). `TransferRowRDto` / `TransferRowStatus` ne servent plus qu'à la maquette du détail (TRF-04).
- `pages/transfer-list-page` : pagination (9 par page) et filtre par puces d'origine **côté serveur** (`rxResource` sur `page` et `origin` ; changer d'origine revient à la page 1). Colonnes : N°, Trajet, Origine, Lignes (· unités), Date (`<time datetime>`), Statut, Émetteur. Les puces n'affichent plus de compteur (le serveur ne les calcule pas).
- `data-access/when-pipe.ts` : `WhenPipe` accepte maintenant un instant ISO du serveur (`…Z` ou `±hh:mm`) et l'affiche dans le fuseau du navigateur (`toLocalTimestamp`) ; les horodatages locaux de la maquette restent acceptés.
- `data-access/transfer-tones.ts` : ton `success` pour `VALIDATED`.
- `components/transfer-detail` : si le détail n'existe pas (identifiant réel, TRF-04 pas encore livré), état vide « Détail du bon bientôt disponible » au lieu d'un chargement sans fin.
- Traductions (fr, mg) : `transfers.total`, `transfers.col.issuer`, `transfers.locationUnknown`, `transfers.detailPending` ; `transfers.week` retiré.

### Données

- Aucune migration : table `transfers` et index `ix_transfers_created_at` (TRF-03, `2026_11_08-21-changelog.sql`), permission `transfers.read` (AUTH-01, `2026_10_19-01-changelog.sql`, rôle `ADMIN`).
- Journal : aucune écriture (consultation).

## Règles métier à respecter par les autres tickets

- L'historique est **réservé à `transfers.read`** (administrateur) ; la vendeuse saisit des retours (`transfers.return`) sans voir l'historique.
- La date affichée est `createdAt` (date d'enregistrement du transfert) ; l'émetteur est `createdBy` (audit JPA), jamais une donnée saisie.
- Un transfert sans `createdBy` est affiché comme émis par « Système » : TRF-02 / JR-01, s'ils génèrent les transferts du matin dans une requête authentifiée, auront l'utilisateur qui ouvre la journée comme émetteur.
- Les lignes d'un transfert ne sont pas renvoyées par la liste (seulement `lineCount` et `quantity`) : le détail relève de `GET /api/v1/transfers/{id}` (TRF-04).
- Un nouveau statut (ex. `CANCELLED`) apparaîtra automatiquement dans la liste : penser à sa traduction `transfers.status.*` et à son ton dans `TRANSFER_STATUS_TONES`.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/transfer/`)
- `controller/TransferController.java` (+ `getTransfers`)
- `service/TransferService.java` (+ `getTransfers`) ; `service/impl/TransferServiceImpl.java` (+ `getTransfers`, `SORT_FIELDS`) ; `service/impl/TransferHistory.java` (nouveau)
- `repository/TransferRepository.java` (+ `search`) ; `repository/TransferSummary.java` (nouveau)
- `dto/TransferSummaryRDto.java` (nouveau)

**Migrations Liquibase** : aucune.

**Frontend** (`valimbavaka-ui/src/app/features/transfers/`)
- `data-access/transfer-service.ts`, `transfer-models.ts`, `transfer-tones.ts`, `when-pipe.ts`
- `pages/transfer-list-page/` (`.ts`, `.html`, `.css`)
- `components/transfer-detail/transfer-detail.html` (état vide)
- `features/demo-pages.a11y.spec.ts` (page retirée : elle appelle l'API) ; `public/i18n/fr.json`, `mg.json`

**Tests**
- Backend : `feature/transfer/controller/TransferHistoryIT.java`, `feature/transfer/service/impl/TransferHistoryTest.java`, `TransferServiceImplTest.java` (3 ajoutés)
- Frontend : `pages/transfer-list-page/transfer-list-page.spec.ts` (avec contrôle axe), `data-access/transfer-service.spec.ts` (2 ajoutés)

## Dépendances

**Ce ticket s'appuie sur** :
- [TRF-03](../TRF-03/guide.md) : modèle `transfers` / `transfer_lines`, `TransferRepository`, statuts et origines
- [SYS-05](../SYS-05/guide.md) : libellés des emplacements (`ReferentialService.getStockLocations`)
- [AUTH-01](../AUTH-01/guide.md) : permission `transfers.read`, port `AuthenticatedUserLoader`

**Ces tickets s'appuient sur lui** :
- [TRF-04](../TRF-04/guide.md) · détail : ouvre le bon depuis la ligne de l'historique (`selectedId` = `TransferSummaryRDto.id`) ; à brancher sur `GET /api/v1/transfers/{id}`

## Comment l'utiliser depuis un autre ticket

```ts
// Les 9 derniers retours, du plus récent au plus ancien
this.transferService.list({ page: 1, size: 9, origin: 'RETURN' }).subscribe((page) => page.items);
```

```java
// Export ou tableau de bord : les 100 derniers transferts du matin
transferService.getTransfers(new PageQuery(1, 100, null, null, null), TransferOrigin.MORNING);
```

## Points d'attention

- **Hypothèse (« origine, destination »)** : lues comme emplacements de départ et d'arrivée (libellés du référentiel) ; l'origine du bon (`MORNING`, `MANUAL`, `RETURN`) reste une colonne à part, masquée sur écran étroit (< 700 px) ; date et émetteur restent toujours visibles.
- **Hypothèse (émetteur)** : nom résolu par le serveur au moment de la lecture (pas copié dans `transfers`, contrairement au journal) : un compte renommé apparaît sous son nom actuel ; un compte supprimé → « Compte inconnu ».
- **Emplacement désactivé** : le référentiel n'expose que les emplacements actifs ; un transfert vers un emplacement désactivé depuis garde son UUID mais son libellé vaut `null` (« Emplacement désactivé » à l'écran). Aucune route ne désactive d'emplacement aujourd'hui.
- **Détail (TRF-04)** : le panneau latéral reste la maquette ; pour un transfert réel il affiche « Détail du bon bientôt disponible ». Plus de présélection de `TRF-0092` à l'ouverture.
- **Compteurs des puces retirés** : la maquette affichait le nombre de transferts par origine ; le serveur ne le calcule pas (à ajouter par un endpoint de synthèse si le client le demande).
- TRF-03 est mergée dans `develop` : la branche a été fusionnée avec `develop` (base de la PR).
- Recette manuelle (🧪) à passer après le merge.
