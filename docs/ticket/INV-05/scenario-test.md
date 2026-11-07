# Scénarios de test — INV-05 · Historique des sessions d'inventaire

Statut : ✅ rédigé le 2026-11-08 — tests **à lancer** (voir « Résultat de l'exécution ») · Ticket : [#81](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/81) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

**Définition d'un écart** (INV-02, INV-03) : une ligne **comptée** dont la quantité comptée diffère du théorique figé au comptage (`InventoryLine.hasGap`). Une ligne non comptée n'est pas un écart. Le **nombre d'écarts** d'une session (`gapCount`) est le nombre de ces lignes, pas une somme d'unités.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | Administrateur `admin.test@valimbavaka.mg` (`inventory.read`, `inventory.update`, `inventory.close`) ; vendeuse (sans `inventory.read`) ; sans jeton |
| Données de départ (IT) | Celles d'`InventoryClosingIT` : une gamme `INV04-<8 car.>` avec deux articles, « savon » (stock **12** en `STORE`) et « huile » (stock **5**) ; une session `IN_PROGRESS` sur `STORE`, périmètre `RANGE`, 2 lignes |
| Tickets à avoir déployés avant | INV-01 (sessions, `GET /api/v1/inventories`), INV-02 (comptage), INV-03 (motifs), INV-04 (clôture) |

## Scénarios issus des critères d'acceptation

Critère : « Chaque session passée est consultable avec statut et nombre d'écarts. »

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Session close dans l'historique | savon compté 10 pour 12 (`BREAKAGE`), huile comptée 5 pour 5, clôture ; `GET /api/v1/inventories?status=CLOSED&size=1&locationId=<STORE>` | 200 ; `items[0]` = la session, `status` `CLOSED`, `closedAt` renseigné, `lineCount` **2**, `gapCount` **1** | `InventoryClosingIT.getSessions_closedFilter_returnsClosedSessionWithStatusAndGapCount` | ☐ |
| 1b | Une seule requête groupée par page | 2 sessions closes sur la page : 12 lignes / 3 écarts et 4 lignes / 0 écart | `lineCount` 12 et 4, `gapCount` 3 et 0, dans l'ordre de la page ; `InventoryLineRepository.tallies` appelé **une fois** | `InventoryServiceImplTest.getSessions_closedSessions_returnsLineAndGapCountsReadInOneGroupedQuery` | ☐ |
| 1c | Détail d'une session | `GET /api/v1/inventories/{id}` sur une session à 2 lignes dont 1 en écart | `status` `IN_PROGRESS`, `lineCount` 2, `gapCount` 1 | `InventoryServiceImplTest.getSession_sessionWithGaps_returnsItsStatusAndGapCount` | ☐ |
| 1d | Session ouverte | ouverture d'une session à 2 articles | `gapCount` **0** dans la réponse 201 | `InventoryServiceImplTest.openSession_validRequest_savesNumberedSessionWithOneLinePerArticleAndJournals` | ☐ |
| 2 | Page « Historique » | `/inventory/history` | `GET /api/v1/inventories?page=1&size=20&sortBy=createdAt&sortDirection=DESC`, sans `status` ; une ligne par session, la plus récente d'abord (emplacement, numéro, responsable) | `inventory-history-page.spec.ts` « charge toutes les sessions, les plus récentes d’abord, sans filtre de statut » | ☐ |
| 2b | Statut et nombre d'écarts affichés | INV-0013 `IN_PROGRESS` (12 articles, 0 écart) et INV-0012 `CLOSED` (40 articles, 3 écarts) | INV-0013 : « En cours », « Clôturée le » = « — », 12, 0 ; INV-0012 : « Clôturée », 40, **3** en rouge (`has-gaps`) | `inventory-history-page.spec.ts` « affiche le statut, le nombre d’articles et le nombre d’écarts de chaque session » | ☐ |
| 2c | Filtre par statut | pastille « Clôturée » | nouvelle requête avec `status=CLOSED` et `page=1` | `inventory-history-page.spec.ts` « recharge avec le statut choisi » | ☐ |
| 3 | Sessions précédentes sur la feuille de comptage | session en cours + une session close INV à 3 écarts | `GET …?status=CLOSED&size=5` en parallèle des lignes ; la carte « Sessions précédentes » affiche « 3 écart(s) » | `inventory-page.spec.ts` « affiche la session en cours, son théorique et les sessions précédentes (INV-05) », `inventory-service.spec.ts` « lit les dernières sessions clôturées avec leur nombre d’écarts à côté de la feuille » | ☐ |
| 3b | Accès à l'historique sans session en cours | aucune session `IN_PROGRESS` | liens « Ouvrir une session » et « Voir l'historique des sessions » (`/inventory/history`) ; aucune requête des sessions closes | `inventory-page.spec.ts` « invite à ouvrir une session, ou à consulter l’historique, quand aucune n’est en cours », `inventory-service.spec.ts` « renvoie une feuille vide quand aucune session n’est en cours » | ☐ |

## Sécurité et erreurs

Aucun nouvel endpoint : la lecture reste `GET /api/v1/inventories` et `GET /api/v1/inventories/{id}` (permission `inventory.read`, INV-01). Pas d'écriture, donc pas de mass assignment ni de 409.

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `GET /api/v1/inventories?status=CLOSED` | 401 `UNAUTHENTICATED` | `InventoryClosingIT.getSessions_withoutToken_returns401` | ☐ |
| Mauvais rôle | jeton vendeuse | 403 `FORBIDDEN` | `InventoryOpeningIT.getSessions_withSellerRole_returns403` (INV-01) | ☐ |
| Pagination invalide | `size=101` | 422 `VALIDATION_FAILED` | `InventoryOpeningIT.getSessions_sizeOver100_returns422` (INV-01) | ☐ |
| Session introuvable | `GET /api/v1/inventories/{id}` aléatoire | 404 `NOT_FOUND`, aucune lecture des écarts | `InventoryServiceImplTest.getSession_unknownId_throwsNotFoundException` | ☐ |
| Route front | `/inventory/history` | protégée par le guard `inventory.read` de la route parente `inventory` (`app.routes.ts`) ; le serveur décide | — | 🔍 code |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Aucune ligne comptée | session ouverte, rien compté | `gapCount` **0** (une ligne non comptée n'est pas un écart) | `InventoryClosingIT.getSession_nothingCounted_returnsZeroGap` | ☐ |
| Compté égal au théorique en décimal | savon compté `12.000` pour 12 ; huile comptée 4 pour 5 | `gapCount` **1** (comparaison numérique : `12.000` = 12) | `InventoryClosingIT.getSession_countedLines_countsOnlyLinesWithGap` | ☐ |
| Page vide | aucun résultat pour le filtre | `items` vide, aucune requête des écarts | `InventoryServiceImplTest.getSessions_emptyPage_returnsNoItemAndDoesNotQueryTallies` | ☐ |
| Historique vide à l'écran | aucune session pour le filtre | « Aucune session d'inventaire pour ce filtre. » | `inventory-history-page.spec.ts` « indique qu’aucune session ne correspond au filtre » | ☐ |
| Session close d'un emplacement désactivé | emplacement désactivé après la clôture | la session reste listée, libellé « Emplacement désactivé » (INV-01) | — | 🔍 code (`InventoryServiceImpl.locationOf`) |
| Hors ligne | page d'historique sans réseau | lecture seule : la liste ne se charge pas, état d'erreur avec « Réessayer » (`vvs-list-state`) ; rien n'est mis en file | — | 🔍 code |
| Accessibilité | page d'historique avec 2 sessions ; feuille de comptage avec une session précédente | aucune violation axe | `inventory-history-page.spec.ts` et `inventory-page.spec.ts` « ne présente aucune violation d’accessibilité détectable » | ☐ |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/inventory/service/impl/InventoryServiceImplTest.java` (3 tests ajoutés, 1 assertion ajoutée), `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/inventory/controller/InventoryClosingIT.java` (4 tests ajoutés, données d'INV-04 réutilisées), `InventoryClosingServiceImplTest` (`closedView` complété par `gapCount`).
- Frontend : `valimbavaka-ui/src/app/features/inventory/pages/inventory-history-page/inventory-history-page.spec.ts` (nouveau, 5 tests), `pages/inventory-page/inventory-page.spec.ts` (2 tests enrichis), `data-access/inventory-service.spec.ts` (1 test ajouté, 1 adapté).

## Recette manuelle (🧪)

| Étape | Attendu | Statut |
|---|---|---|
| Clôturer une session avec 2 écarts, puis ouvrir `/inventory` sur une nouvelle session | carte « Sessions précédentes » : la session close avec « 2 écart(s) » et la date de clôture | 🧪 |
| Cliquer « Voir l'historique des sessions » | page « Historique des sessions d'inventaire » sans bandeau « en construction », sessions les plus récentes d'abord, statut et nombre d'écarts sur chaque ligne | 🧪 |
| Filtrer « Clôturée », puis « Toutes », puis paginer | seules les sessions closes, puis toutes ; retour en page 1 à chaque changement de filtre | 🧪 |
| `/inventory` | plus de bandeau « en construction » | 🧪 |

## Résultat de l'exécution

- Date : _(à lancer : CI ou local)_
- Environnement : _(à compléter)_
- Anomalies ouvertes : _(à compléter)_
