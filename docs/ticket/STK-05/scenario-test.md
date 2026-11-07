# Scénarios de test — STK-05 · Génération d'un bon de réapprovisionnement

Statut : ☐ tests écrits, à exécuter · Ticket : [#34](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/34) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (nouvelle permission `stocks.restock`) ; `SELLER` et `DELIVERY_DRIVER` pour les 403 |
| Données de départ | « Savon noir 100 g » (`ART-0031`, pièce, seuil 40) avec 10 à l'entrepôt et 2,5 en boutique, fournisseur « Huiles Ravo » (`LOCAL`, délai 7 j) ; un article au seuil 40 avec 40 en stock (pas d'alerte) ; un article sans fournisseur (fabrication interne) |
| Tickets à avoir déployés avant | FRS-01, FRS-04 (délai), STK-04 (alertes de seuil), JNL-01 (journal) |

## Scénarios issus des critères d'acceptation

### Critère 1 — L'action confirme la génération du bon à partir des articles sous seuil

Règle : le bon reprend les alertes de seuil du moment (STK-04), dans le même ordre (plus urgentes d'abord).
Chaque ligne copie le stock, le seuil, le fournisseur, le délai et la date de livraison prévue ; la quantité à
commander est la quantité suggérée de l'alerte. Le serveur attribue la référence `BR-000001`, `BR-000002`…

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Génération sur un article sous le seuil | `POST /api/v1/stocks/restock-orders` `{"articleIds":["<ART-0031>"]}`, stock 12,5 / seuil 40 | 201, en-tête `Location`, `reference` `BR-dddddd`, `status` `PENDING`, une ligne n° 1 : `stockQuantity` 12.5, `threshold` 40, `quantity` 30, `unit` PIECE | `RestockOrderIT.generate_articleUnderThreshold_returns201WithItsLineAndTracesTheJournal` | ☐ |
| 1b | Sans sélection : toutes les alertes, plus urgentes d'abord | Alertes `ART-0002` (0 / 50) puis `ART-0001` (12,5 / 40, Huiles Ravo, 7 j), numéro 7 | `BR-000007`, lignes 1 `ART-0002` et 2 `ART-0001` ; fournisseur, délai et livraison prévue (15/11) copiés ; `ART-0002` sans fournisseur | `RestockOrderServiceImplTest.generate_noArticleSelected_ordersEveryAlertMostUrgentFirstAndTracesIt` | ☐ |
| 1c | Sélection restreinte | `articleIds` = `ART-0001`, un UUID inconnu, `ART-0001` en double | Une seule ligne `ART-0001`, numérotée 1 | `RestockOrderServiceImplTest.generate_selectedArticles_keepsOnlyTheSelectedOnesStillUnderThreshold` | ☐ |
| 1d | Sélection vide | `{"articleIds":[]}` | Toutes les alertes | `RestockOrderServiceImplTest.generate_emptySelection_ordersEveryAlert` | ☐ |
| 1e | Lecture du bon généré | `GET /api/v1/stocks/restock-orders/{id}` puis liste `size=1` | Même bon, mêmes lignes ; le plus récent en tête de liste | `RestockOrderIT.getRestockOrderById_afterGeneration_returnsTheSameOrder` | ☐ |
| 1f | Écran : confirmation | Administrateur, « Bon de réapprovisionnement » puis « Valider le bon » | `POST` avec `["art-1"]` ; message « Bon de réapprovisionnement BR-000012 généré · 1 article(s) » ; brouillon fermé | `stock-page.spec.ts` › « génère le bon à partir des articles sous le seuil et confirme sa référence (STK-05) » | ☐ |
| 1g | Écran : brouillon | 3 alertes dont 2 « Huiles Ravo » et 1 sans fournisseur | Groupes « Huiles Ravo · délai 7 j » et « Fabrication interne (atelier) » ; quantités 30 / 20 / 10 u. ; « Livraison prévue le 15 nov. » | `restock-draft.spec.ts` › « regroupe les articles par fournisseur avec la quantité suggérée et la livraison prévue » | ☐ |
| 1h | Service front | `generateRestockOrder(['art-1','art-2'])` | `POST /api/v1/stocks/restock-orders`, corps `{articleIds}`, en-tête `Idempotency-Key` | `stock-service.spec.ts` › « génère un bon de réapprovisionnement par la file hors ligne, avec une clé d’idempotence (STK-05) » | ☐ |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `POST /restock-orders` `{}` | 401 `UNAUTHENTICATED` | `RestockOrderIT.generate_withoutToken_returns401` | ☐ |
| Mauvais rôle (génération) | jeton vendeuse | 403 `FORBIDDEN` | `RestockOrderIT.generate_withSellerRole_returns403` | ☐ |
| Mauvais rôle (lecture) | jeton livreur, `GET /restock-orders` | 403 | `RestockOrderIT.getRestockOrders_withDeliveryDriverRole_returns403` | ☐ |
| Mass assignment | `{"id":"<uuid>","status":"RECEIVED","reference":"BR-999999","articleIds":[…]}` | 201, `id` et `reference` attribués par le serveur, `status` `PENDING` | `RestockOrderIT.generate_massAssignmentOfIdStatusAndReference_isIgnored` | ☐ |
| Données invalides | `{"articleIds":[null]}` | 422 `VALIDATION_FAILED` | `RestockOrderIT.generate_nullArticleId_returns422` | ☐ |
| Ressource inconnue | `GET /restock-orders/<uuid aléatoire>` | 404 `NOT_FOUND` | `RestockOrderIT.getRestockOrderById_unknownId_returns404`, `RestockOrderServiceImplTest.getRestockOrderById_unknownId_throwsNotFound` | ☐ |
| État incompatible | article à 40 / 40 sélectionné seul | 409 `CONFLICT`, aucun bon créé | `RestockOrderIT.generate_articleAtItsThreshold_returns409AndCreatesNoOrder` | ☐ |
| Journal | génération réussie | une ligne `CREATE` `RESTOCK_ORDER` « Génération du bon de réapprovisionnement BR-000007 : 2 article(s) » | `RestockOrderIT.generate_articleUnderThreshold_returns201WithItsLineAndTracesTheJournal`, `RestockOrderServiceImplTest.generate_noArticleSelected_ordersEveryAlertMostUrgentFirstAndTracesIt` | ☐ |
| Écran : vendeuse | `SELLER` sur la page des stocks | bouton « Bon de réapprovisionnement » absent (le serveur refuse de toute façon) | `stock-page.spec.ts` › « ne propose pas le bon de réapprovisionnement à une vendeuse (permission stocks.restock) » | ☐ |

## Cas limites

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Aucune alerte | stock de tous les articles ≥ seuil | 409, ni numéro consommé ni journal | `RestockOrderServiceImplTest.generate_noAlertAtAll_throwsConflict` | ☐ |
| Sélection sans article sous le seuil | seul un article remonté au-dessus du seuil entre l'affichage et la validation | 409, rien n'est enregistré | `RestockOrderServiceImplTest.generate_noSelectedArticleUnderThreshold_throwsConflictAndSavesNothing` | ☐ |
| Écran : 409 | le serveur répond 409 | message « Le bon n'a pas été généré : … » (ton danger), brouillon conservé | `stock-page.spec.ts` › « garde le brouillon et signale le refus quand plus aucun article n’est sous le seuil (409) » | ☐ |
| Double clic | « Valider le bon » pendant l'envoi | bouton désactivé | `restock-draft.spec.ts` › « émet la validation et la bloque pendant la génération » | ☐ |
| Pagination | `size=101` | 422 | `RestockOrderIT.getRestockOrders_sizeAbove100_returns422` | ☐ |
| Bon relu | `GET /{id}` d'un bon sans ligne (cas théorique) | 200, `lines` vide | `RestockOrderServiceImplTest.getRestockOrderById_knownId_returnsItsLines` | ☐ |
| Borne de sélection | 501 identifiants | 422 (`@Size(max = 500)`) | — (contrainte lue dans `RestockOrderWDto`) | 🔍 code |
| Hors ligne | réseau coupé au clic | saisie en file (`Idempotency-Key`), message « Saisie enregistrée hors connexion » ; le rejeu avec la même clé ne crée pas un second bon (`IdempotencyFilter`, SYS-07) | — | 🧪 recette |
| Deux administrateurs en même temps | deux générations simultanées | deux bons distincts, numéros différents (séquence) | — | 🧪 recette |
| Date | génération à 22 h UTC | la livraison prévue part du jour de Madagascar (`StockAlertPolicy.BUSINESS_ZONE`, FRS-04) | couvert par `StockServiceImplAlertTest` (FRS-04) | ☐ |

## Tests existants

- Back : `feature/stock/service/impl/RestockOrderServiceImplTest.java` (unitaire) ; `feature/stock/controller/RestockOrderIT.java` (Testcontainers).
- Non-régression STK-04 / FRS-04 (refactorisation de `StockServiceImpl.getAlerts`) : `StockAlertIT`, `StockServiceImplAlertTest`, `SupplierLeadTimeIT`.
- Front : `components/restock-draft/restock-draft.spec.ts` (nouveau), `pages/stock-page/stock-page.spec.ts`, `data-access/stock-service.spec.ts`.

## Recette manuelle (🧪)

1. En administrateur, page Stocks : « Bon de réapprovisionnement » coche les articles en alerte et ouvre le brouillon groupé par fournisseur. « Valider le bon » affiche « Bon de réapprovisionnement BR-00000N généré · N article(s) » ; le journal montre la ligne `CREATE` correspondante.
2. Couper le réseau puis valider : la saisie part en file ; au retour du réseau un seul bon est créé.
3. Deux administrateurs valident en même temps : deux bons de références différentes.
4. En vendeuse : pas de bouton ; `POST /api/v1/stocks/restock-orders` répond 403.

## Résultat de l'exécution

- Date : à lancer (CI ou local) — aucun test exécuté par le traiteur.
- Environnement : back non compilé localement (Maven Central inaccessible) ; front : `tsc` app et spec, `eslint` et `prettier` sans erreur sur les fichiers touchés.
- Anomalies ouvertes : aucune connue. Étapes 🧪 restantes : 4.
