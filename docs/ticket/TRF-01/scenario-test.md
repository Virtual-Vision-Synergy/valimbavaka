# Scénarios de test — TRF-01 · Historique des transferts de stock

Statut : ✅ exécuté le 2026-11-08 — **CI verte** (voir « Résultat de l'exécution ») · Ticket : [#35](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/35) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible. Légende : ✅ auto = test automatique exécuté et vert en CI · 🧪 recette manuelle.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôles utilisés | administrateur Rasoa Rakoto (`admin.test@valimbavaka.mg`, `transfers.read`), vendeuse Tiana Ranaivoson (`transfers.return` sans `transfers.read`), livreur (aucune permission de transfert) |
| Données de départ | emplacements `STORE` (Boutique) et `WAREHOUSE` (Entrepôt) du référentiel SYS-05 ; deux articles avec 5 en boutique ; un retour saisi par la vendeuse (2,5 + 1 de `STORE` vers `WAREHOUSE`) ; un transfert du matin `MORNING` (1 de l'entrepôt vers la boutique) enregistré par `TransferService.record` hors requête |
| Tickets à avoir déployés avant | TRF-03 (modèle `transfers` / `transfer_lines`, mergée dans `develop`), SYS-05, AUTH-01 (permission `transfers.read` déjà donnée à `ADMIN` par `2026_10_19-01-changelog.sql`) |
| Migration | aucune : la table `transfers`, son index `ix_transfers_created_at` et la permission `transfers.read` existent déjà |

## Scénarios issus des critères d'acceptation

### Critère 1 — Chaque transfert affiche référence, date, origine, destination, statut, émetteur

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| 1 · Retour saisi par la vendeuse | administrateur : `GET /api/v1/transfers?search=RET-0007` | 200, 1 ligne : `number` `RET-0007`, `createdAt` renseigné, `fromLocationLabel` « Boutique », `toLocationLabel` « Entrepôt », `origin` et `status` `RETURN`, `lineCount` 2, `quantity` 3.5, `createdByName` « Tiana Ranaivoson » | `TransferHistoryIT.getTransfers_returnBySeller_showsReferenceDateRouteStatusAndIssuer` | ✅ auto |
| 1b · Transfert généré hors requête | transfert du matin enregistré par `TransferService.record` | `status` `VALIDATED`, `origin` `MORNING`, `createdBy` absent, `createdByName` « Système » | `TransferHistoryIT.getTransfers_transferGeneratedOutsideARequest_showsTheSystemAsIssuer` | ✅ auto |
| 1c · Résolution des libellés et du nom | ligne `RET-0007` Boutique → Entrepôt, émise par la vendeuse | libellés des emplacements actifs et « Prénom Nom » de l'émetteur | `TransferHistoryTest.toRDtos_returnBySeller_showsReferenceDateRouteStatusAndIssuer`, `TransferHistoryTest.toRDtos_transferWithoutIssuer_showsTheSystem` | ✅ auto |
| 1d · Écran : colonnes | liste avec `RET-0015` (vendeuse) et `TRF-0092` (Système) | 7 colonnes : N°, Trajet (Boutique → Entrepôt), Origine, Lignes, Date (`<time datetime="2026-10-19T16:05:00Z">`), Statut (« Retour », « Validé »), Émetteur | `transfer-list-page.spec.ts` (« should show the reference, date, route, status and issuer of each transfer ») | ✅ auto |
| 1e · Plus récent d'abord | deux transferts du matin successifs, `size=100` | dates non croissantes | `TransferHistoryIT.getTransfers_noSort_listsTheNewestFirst`, `TransferServiceImplTest.getTransfers_noSort_searchesNewestFirstWithTheTrimmedTermAndTheOrigin` | ✅ auto |
| 1f · Filtre par origine | `origin=MORNING`, puis `origin=RETURN` | uniquement des `MORNING` (dont le transfert créé), puis uniquement des `RETURN` | `TransferHistoryIT.getTransfers_filteredByOrigin_returnsOnlyThatOrigin` | ✅ auto |
| 1g · Écran : filtre et pages côté serveur | clic sur la puce « Retours », puis « Suivant » | requête `origin=RETURN&page=1` ; requête `page=2` ; à l'ouverture `page=1&size=9` sans `origin` ni `sortBy` | `transfer-list-page.spec.ts` (« should reload from the first page with the chosen origin », « should load the next page from the server », « should ask the server for the first page… »), `transfer-service.spec.ts` (« lit l'historique paginé… », « n'envoie pas de filtre d'origine… ») | ✅ auto |

## Sécurité et erreurs

Endpoint en lecture seule : pas de corps (pas de mass assignment possible), pas de 404 (liste), pas de 409, pas de ligne de journal (consultation).

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `GET /api/v1/transfers` sans `Authorization` | 401 `UNAUTHENTICATED` | `TransferHistoryIT.getTransfers_withoutToken_returns401` | ✅ auto |
| Vendeuse | jeton vendeuse (a `transfers.return`, pas `transfers.read`) | 403 `FORBIDDEN` | `TransferHistoryIT.getTransfers_withSellerRole_returns403` | ✅ auto |
| Livreur | jeton livreur | 403 `FORBIDDEN` | `TransferHistoryIT.getTransfers_withDeliveryDriverRole_returns403` | ✅ auto |
| `size` au-delà du maximum | `size=101` | 422 `VALIDATION_FAILED` | `TransferHistoryIT.getTransfers_sizeAbove100_returns422` | ✅ auto |
| Page 0 | `page=0` | 422 `VALIDATION_FAILED` | `TransferHistoryIT.getTransfers_pageZero_returns422` | ✅ auto |
| Origine inconnue | `origin=TRUCK` | 400 `MALFORMED_REQUEST` | `TransferHistoryIT.getTransfers_unknownOrigin_returns400` | ✅ auto |
| Écran hors permission | vendeuse sur `/transfers` | refusée par `permissionGuard('transfers.read')` (TRF-03) ; le serveur répond 403 de toute façon | `TransferHistoryIT.getTransfers_withSellerRole_returns403` | ✅ auto |

## Cas limites

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Borne `size` = 100 | `size=100` | 200, `pageSize` 100 | `TransferHistoryIT.getTransfers_size100_returns200` | ✅ auto |
| Tri hors liste blanche | `sortBy=note` | 200, tri par défaut sur la date | `TransferHistoryIT.getTransfers_sortOutsideTheWhiteList_returns200`, `TransferServiceImplTest.getTransfers_sortOutsideTheWhiteList_fallsBackToTheDate` | ✅ auto |
| Tri autorisé | `sortBy=number&sortDirection=ASC` | tri par numéro | `TransferServiceImplTest.getTransfers_sortByNumber_isAllowed` | ✅ auto |
| Recherche sans résultat / espaces autour | `search=ZZZ-…` ; `search="  ret-00 "` | page vide (`items` [], `totalCount` 0, `page` 1) ; terme nettoyé `ret-00`, insensible à la casse | `TransferHistoryIT.getTransfers_searchWithoutMatch_returnsAnEmptyPage`, `TransferServiceImplTest.getTransfers_noSort_searchesNewestFirstWithTheTrimmedTermAndTheOrigin` | ✅ auto |
| Liste vide à l'écran | aucune ligne | « Aucun transfert » | `transfer-list-page.spec.ts` (« should say so when there is no transfer yet ») | ✅ auto |
| Compte supprimé depuis | `createdBy` sans compte | `createdBy` conservé, `createdByName` « Compte inconnu » | `TransferHistoryTest.toRDtos_accountRemovedSince_showsAnUnknownAccount` | ✅ auto |
| Même émetteur sur plusieurs lignes | 3 retours de la même vendeuse sur la page | compte lu une seule fois par page | `TransferHistoryTest.toRDtos_sameIssuerOnSeveralRows_loadsTheAccountOnce` | ✅ auto |
| Emplacement désactivé depuis | origine absente des emplacements actifs | identifiant gardé, libellé `null` ; l'écran affiche « Emplacement désactivé » | `TransferHistoryTest.toRDtos_locationDeactivatedSince_keepsTheIdentifierWithoutLabel`, `transfer-list-page.spec.ts` (« should name a location deactivated since the transfer ») | ✅ auto |
| Accessibilité | liste chargée | aucune violation axe (WCAG 2.2 AA, hors contraste) | `transfer-list-page.spec.ts` (« should have no detectable accessibility violation once the list is loaded ») | ✅ auto |
| Dates et fuseau | `createdAt` `2026-10-19T16:05:00Z` | instant UTC du serveur affiché dans le fuseau du navigateur (`WhenPipe`, `toLocalTimestamp`) ; attribut `datetime` = valeur du serveur | `transfer-list-page.spec.ts` (« should show the reference, date… ») | ✅ auto |
| Hors ligne | consultation sans réseau | lecture seule, hors file d'attente : état d'erreur avec « Réessayer » (`ListState`) | — | 🧪 |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/transfer/controller/TransferHistoryIT.java` (13), `…/transfer/service/impl/TransferHistoryTest.java` (5), `…/transfer/service/impl/TransferServiceImplTest.java` (3 ajoutés).
- Frontend : `valimbavaka-ui/src/app/features/transfers/pages/transfer-list-page/transfer-list-page.spec.ts` (7), `…/transfers/data-access/transfer-service.spec.ts` (2 ajoutés). La page sort de `demo-pages.a11y.spec.ts` (elle appelle l'API) : son contrôle axe est dans sa propre spec.

## Recette manuelle (🧪)

| Étape | Attendu | Statut |
|---|---|---|
| Administrateur : saisir un retour (`/transfers/return`), revenir sur `/transfers` | le retour apparaît en tête, statut « Retour », émetteur = nom de l'administrateur, date « Aujourd'hui HH:MM » à l'heure locale | 🧪 |
| Vendeuse : saisir un retour, puis l'administrateur ouvre `/transfers` | émetteur = nom de la vendeuse | 🧪 |
| Puces « Répartition matinale », « Manuels », « Retours », « Tous » et pagination (plus de 9 transferts) | la liste se recharge depuis la page 1 ; « Suivant » charge la page 2 | 🧪 |
| Couper le réseau puis ouvrir `/transfers` | état d'erreur avec « Réessayer » ; au retour du réseau, « Réessayer » recharge | 🧪 |
| Écran étroit (< 700 px) | la colonne Origine se masque ; date et émetteur restent visibles | 🧪 |

## Résultat de l'exécution

- Date : 2026-11-08, CI GitHub Actions sur `feature/TRF-01-historique-transferts` (commit `98c2ec0`, après fusion de `develop`) : workflows « CI » (backend `mvn verify` avec Testcontainers, frontend `ng test`) et « Convention VVS » (Spotless, Checkstyle, Prettier, ESLint, commits et migrations), tous deux verts.
- Backend : 361 tests unitaires et 366 d'intégration, 0 échec ; dont TRF-01 : `TransferHistoryIT` 13, `TransferHistoryTest` 5, `TransferServiceImplTest` 6 (3 ajoutés).
- Frontend : 64 fichiers, 387 tests, 0 échec ; dont `transfer-list-page.spec.ts` 7 et `transfer-service.spec.ts` 3 (2 ajoutés).
- Premier passage : formatage Spotless de deux fichiers de test et 1 test front en échec (libellé du bouton « Page suivante » mal visé, défaut du test) ; corrigés (`style(transfer)`, `fix(transfers)`).
- Anomalies ouvertes : aucune.
- Étapes 🧪 restantes : 5.
