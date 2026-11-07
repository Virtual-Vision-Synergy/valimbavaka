# Guide d'intégration — TRF-04 · Détail d'un transfert

Statut : ✅ écrit le 2026-11-08 — tests verts en CI (voir « Résultat de l'exécution » du scénario) · Ticket : [#37](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/37) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

L'administrateur clique sur le numéro d'un transfert dans l'historique (`/transfers`) : le panneau latéral affiche le bon complet, lu sur `GET /api/v1/transfers/{id}` (numéro, origine, statut, trajet, date, émetteur, note, puis **chaque article avec son code, son nom, sa quantité et son unité**, dans l'ordre saisi). Le serveur résout lui-même les articles (une lecture), les libellés d'emplacement et le nom de l'émetteur, avec les mêmes règles que l'historique (TRF-01). Le panneau n'affiche plus de données de démonstration.

## Contrat exposé

### Endpoints backend (`/api/v1/transfers`)

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie (`XxxRDto`) | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/transfers/{id}` | `transfers.read` (administrateur seulement) | `id` (UUID, chemin) | 200 `TransferDetailRDto` | 400 (`id` mal formé), 401, 403, 404 (transfert inconnu) |

- Contrôleur : `TransferController.getTransfer` (`feature/transfer/controller/TransferController.java:70`). C'est l'URL du `Location` renvoyé par `POST /api/v1/transfers/returns` (TRF-03).
- `TransferDetailRDto` (`feature/transfer/dto/TransferDetailRDto.java`) : `id`, `number`, `origin`, `status`, `fromLocationId`, `fromLocationLabel`, `toLocationId`, `toLocationLabel`, `note` (`null` si vide), `lines`, `quantity` (somme des lignes), `createdAt`, `createdBy`, `createdByName`.
  - Libellés et émetteur : mêmes règles que `TransferSummaryRDto` (TRF-01) — libellé `null` si l'emplacement n'est plus actif ; « Système » sans `createdBy` ; « Compte inconnu » si le compte n'existe plus.
- `TransferDetailLineRDto` (`feature/transfer/dto/TransferDetailLineRDto.java`) : `articleId`, `articleCode`, `articleName`, `unit` (`PIECE`, `KILOGRAM`, `LITER`), `quantity`. Code, nom et unité sont ceux **actuels** de l'article (désactivé compris).

### Services et ports Java réutilisables

| Port | Méthode | Rôle |
|---|---|---|
| `TransferService` (`service/TransferService.java:43`) | `TransferDetailRDto getTransfer(UUID transferId)` | Fiche complète d'un transfert ; `NotFoundException` (404) s'il est inconnu. Réutilisable par une impression du bon ou un export. |
| `ArticleService` (`feature/article/service/ArticleService.java:33`, **ajout**) | `List<ArticleRDto> getArticlesByIds(Collection<UUID> articleIds)` | Fiches de plusieurs articles en **une requête** (actifs ou non ; un identifiant inconnu est absent). À préférer à une boucle de `getArticleById` pour afficher des lignes (production, inventaire, ventes). Repository : `ArticleRepository.findByIdIn` (`:104`). |
| `TransferSheet` (`service/impl/TransferSheet.java:36`, interne) | `toRDto(Transfer)` | Assemble la fiche : articles (une lecture), libellés (`TransferHistory.locationLabels`, `:48`) et émetteur (`TransferHistory.issuerName`, `:57`), désormais partagés avec l'historique. |

### Frontend (`features/transfers`)

- Route inchangée : `/transfers` (garde `permissionGuard('transfers.read')`). Le clic sur un numéro (`selectedId`) ouvre `components/transfer-detail` dans `vvs-side-panel`.
- `data-access/transfer-service.ts:36` : `get(id)` → `GET /api/v1/transfers/{id}` (`Observable<TransferDetailRDto>`). Lecture seule : pas de file hors ligne.
- `data-access/transfer-models.ts` : `TransferDetailRDto`, `TransferDetailLineRDto` alignés sur le serveur (unité typée par `ArticleUnit` de `features/article`). Les types de la maquette (`TransferRowRDto`, `TransferRowStatus`) et `transfer-fixtures.ts` sont **supprimés** ; `TRANSFER_STATUS_TONES` ne couvre plus que `VALIDATED` et `RETURN`.
- `components/transfer-detail` : en-tête (numéro, badge de statut, origine · `<time datetime>` · émetteur, trajet avec « Emplacement désactivé » si libellé `null`, ±quantité totale, note si présente), liste nommée des lignes (nom, code, quantité + unité courte), états chargement et erreur (« Réessayer »).
- `pages/transfer-list-page` : « Créer un retour » du panneau ouvre `/transfers/return` (`openReturn`, `transfer-list-page.ts:86`) ; « Imprimer le bon » reste une action de démonstration.
- Traductions (fr, mg) : ajout `transfers.note`, `transfers.linesLabel`, `transfers.unit.{PIECE,KILOGRAM,LITER}` (« u. », « kg », « L ») ; retrait des clés de la maquette `transfers.status.{IN_TRANSIT,RECEIVED,DISCREPANCY}`, `transfers.detailPending`, `pending`, `receivedLine`, `gapLine`, `moreLines`.

### Données

- Aucune migration : tables `transfers` / `transfer_lines` (TRF-03, `2026_11_08-21-changelog.sql`) et permission `transfers.read` (AUTH-01).
- Journal : aucune écriture (consultation).

## Règles métier à respecter par les autres tickets

- Le détail est **réservé à `transfers.read`** (administrateur), comme l'historique ; la vendeuse qui saisit un retour n'y a pas accès.
- Les lignes sont renvoyées **toutes** (100 au maximum, limite de TRF-03) et **dans l'ordre de saisie** (`line_number`).
- Un nouveau statut (ex. `CANCELLED`) doit avoir sa traduction `transfers.status.*` et son ton dans `TRANSFER_STATUS_TONES` (le type `Record<TransferStatus, Tone>` le rendra obligatoire côté front).
- Pour afficher les articles d'un ensemble de lignes, passer par `ArticleService.getArticlesByIds` (une requête), pas par une boucle.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/`)
- `transfer/controller/TransferController.java` (+ `getTransfer`)
- `transfer/service/TransferService.java` (+ `getTransfer`) ; `transfer/service/impl/TransferServiceImpl.java` (+ `getTransfer`) ; `transfer/service/impl/TransferSheet.java` (nouveau) ; `transfer/service/impl/TransferHistory.java` (`locationLabels`, `issuerName` rendus visibles dans le paquet)
- `transfer/dto/TransferDetailRDto.java`, `TransferDetailLineRDto.java` (nouveaux)
- `article/service/ArticleService.java`, `article/service/impl/ArticleServiceImpl.java` (+ `getArticlesByIds`) ; `article/repository/ArticleRepository.java` (+ `findByIdIn`)

**Migrations Liquibase** : aucune.

**Frontend** (`valimbavaka-ui/`)
- `src/app/features/transfers/components/transfer-detail/` (`.ts`, `.html`, `.css`)
- `src/app/features/transfers/data-access/transfer-service.ts`, `transfer-models.ts`, `transfer-tones.ts` ; `transfer-fixtures.ts` supprimé
- `src/app/features/transfers/pages/transfer-list-page/` (`.ts`, `.html`)
- `public/i18n/fr.json`, `mg.json`

**Tests**
- Backend : `feature/transfer/controller/TransferDetailIT.java` (8), `feature/transfer/service/impl/TransferSheetTest.java` (4), `TransferServiceImplTest.java` (2 ajoutés)
- Frontend : `components/transfer-detail/transfer-detail.spec.ts` (6, avec contrôle axe), `pages/transfer-list-page/transfer-list-page.spec.ts` (1 ajouté), `data-access/transfer-service.spec.ts` (1 ajouté)

## Dépendances

**Ce ticket s'appuie sur** :
- [TRF-01](../TRF-01/guide.md) : historique, sélection d'une ligne, résolution des libellés et de l'émetteur (`TransferHistory`)
- [TRF-03](../TRF-03/guide.md) : modèle `transfers` / `transfer_lines`, `TransferRepository`, statuts et origines
- [ART-01](../ART-01/guide.md) : fiches articles (`ArticleService`)
- [SYS-05](../SYS-05/guide.md) : libellés des emplacements
- [AUTH-01](../AUTH-01/guide.md) : permission `transfers.read`, port `AuthenticatedUserLoader`

**Ces tickets s'appuient sur lui** :
- _(aucun ticket déclaré)_ ; une impression du bon (SYS-01) ou les transferts du matin (JR-01 / TRF-02) peuvent réutiliser `TransferService.getTransfer`.

## Comment l'utiliser depuis un autre ticket

```ts
// Ouvrir le bon renvoyé par un retour (Location: /api/v1/transfers/{id})
this.transferService.get(created.id).subscribe((sheet) => sheet.lines.map((line) => line.articleName));
```

```java
// Impression ou export d'un bon : lignes avec code, nom, unité et quantité
var sheet = transferService.getTransfer(transferId);
```

## Points d'attention

- **Hypothèse (« détail complet »)** : lu comme tout ce que le bon contient (en-tête de l'historique + note + toutes les lignes avec article et quantité). Le résumé de la maquette (« 6 premières lignes + n autres lignes ») est abandonné : un bon a au plus 100 lignes, toutes affichées, le panneau défile.
- **Réception et écarts non implémentés** : la maquette montrait « reçu », « reçu x sur y » et les statuts `IN_TRANSIT`, `RECEIVED`, `DISCREPANCY` ; aucun ticket ne les porte (voir TRF-03) : retirés du panneau, des types et des traductions.
- **Unité** : chaque ligne affiche l'unité courte de l'article (« u. », « kg », « L ») ; le total du trajet (« −5.5 u. ») reste une somme brute de quantités, comme dans l'historique, même si les unités diffèrent.
- **Nom actuel de l'article** : code, nom et unité sont lus au moment de la consultation (pas copiés dans `transfer_lines`) ; un article renommé apparaît sous son nom actuel ; un article désactivé reste affiché.
- **Article introuvable** : impossible en base (clé étrangère, les articles ne sont jamais supprimés) ; si cela arrivait, `TransferSheet` lève une `IllegalStateException` (500) plutôt que d'afficher une ligne vide.
- **Actions du panneau** : « Créer un retour » ouvre `/transfers/return` sans préremplissage ; « Imprimer le bon » reste une action de démonstration (impression à traiter avec SYS-01 si le client la demande).
- **404 à l'écran** : traité comme toute erreur de chargement (état d'erreur avec « Réessayer ») ; en pratique le bon vient de la liste et existe.
- PR initialement empilée sur TRF-01 ; TRF-01 a été mergée dans `develop` et `develop` a été fusionnée dans la branche : la PR vise `develop`.
- Recette manuelle (🧪) à passer après le merge.
