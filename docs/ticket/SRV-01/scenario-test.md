# Scénarios de test — SRV-01 · Catalogue des services salon

Statut : ✅ tests automatiques verts en CI, recette 🧪 à passer · Ticket : [#29](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/29) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

**Décision PO du 07/11/2026** (question client du ticket « la commission du service s'ajoute-t-elle ou remplace-t-elle celle de la vendeuse ? ») : la commission la plus précise gagne, sans cumul : taux du service salon, sinon taux du canal (règle active COM-01), sinon taux de l'utilisateur (AUTH-03). Le taux du service est **facultatif** et exprimé en **pourcentage** (voir « Points d'attention » du guide sur le forfait de la maquette).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (`admin.test@valimbavaka.mg`) pour les écritures ; `SELLER` (vendeuse Miora, canal neuf, 5,00 %) pour la lecture et les refus 403 |
| Données de départ | articles actifs « Huile de coco vierge » (`LITER`), « Masque argile rose 100 g » (`PIECE`) ; en test d'intégration, un article neuf par test (`SalonServiceSecurityIT.insertArticle`) et des noms uniques (`Soin-<uuid>`) car la base est partagée |
| Tickets à avoir déployés avant | ART-01 (articles), COM-01 (résolveur et port `SalonServiceCommissionRates`), JNL-01 ; migrations `2026_11_08-90` puis `2026_11_08-91` |

## Scénarios issus des critères d'acceptation

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Le formulaire couvre nom, durée, prix, commission et produits consommés | ouvrir `/services/catalog/new` | champs « Nom * », « Durée (min) * », « Prix (Ar) * », « Commission (%) », bloc « Produits consommés » | `salon-catalog-form-page.spec.ts` › should offer name, duration, price, commission and consumed products | ✅ auto |
| 1b | Création avec produits consommés | `"Gommage corps café"`, 30 min, 25 000 Ar, 10 %, masque argile × 1 | POST `/api/v1/salon-services` avec `products`, retour au catalogue | `salon-catalog-form-page.spec.ts` › should create the service with its consumed products then go back to the catalogue ; `SalonServiceSecurityIT.createService_byAdmin_returns201WithProductsAndTracesInJournal` | ✅ auto |
| 1c | Commission laissée vide | champ vide | `commissionRate: null`, `products: []` | `salon-catalog-form-page.spec.ts` › should send a null commission rate when the field is left empty ; `SalonCatalogServiceImplTest.createService_withoutProductsNorRate_savesEmptyRecipe` | ✅ auto |
| 1d | Normalisation | `"  Soin visage éclat "`, prix `35000`, taux `12.5`, quantité `15` | nom `Soin visage éclat`, prix `35000.00`, taux `12.50`, quantité `15.000`, produits triés par article | `SalonCatalogServiceImplTest.createService_validRequest_savesNormalizedActiveServiceAndTracesInJournal` ; `SalonServiceTest.create_rawDetails_normalizesNameScalesAndSortsProducts` | ✅ auto |
| 1e | Unité de l'article affichée | huile de coco (`LITER`) | « L » à côté de la quantité | `salon-catalog-form-page.spec.ts` › should show the unit of the chosen article | ✅ auto |
| 1f | Modification avec la version lue, retrait d'un produit | version 2, on retire la 2e ligne | PUT avec `version: 2` et un seul produit | `salon-catalog-form-page.spec.ts` › should save with PUT, the version read and without the removed product | ✅ auto |
| 1g | Fiche relue, article désactivé depuis | 2e produit absent des articles actifs | ligne gardée, option « Article inactif » | `salon-catalog-form-page.spec.ts` › should fill the form and keep an article deactivated since ; `SalonCatalogServiceImplTest.updateService_articleDeactivatedSinceButAlreadyListed_isAccepted` | ✅ auto |
| 1h | Liste du catalogue | soin visage (12,5 %, 2 produits), épilation (sans taux, 0 produit) | nom, `45 min`, prix en Ar, `12.5 %` ou « Canal ou vendeuse », nombre de produits | `salon-catalog-list-page.spec.ts` › should list name, duration, price, commission and number of products | ✅ auto |
| 1i | Commission du service prioritaire (décision PO) | service à 12,5 %, règle canal à 3 %, vendeuse à 5 % | 12,5 % (`SALON_SERVICE`) ; service sans taux : 3 % (`SALES_CHANNEL`) | `SalonServiceSecurityIT.resolve_salonServiceRate_winsOverChannelRuleAndSellerRate` ; `SalonServiceCommissionRatesImplTest` (3 tests) | ✅ auto |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | GET `/api/v1/salon-services` | 401 `UNAUTHENTICATED` | `SalonServiceSecurityIT.getServices_withoutToken_returns401` | ✅ auto |
| Vendeuse en lecture | jeton vendeuse, GET | 200 (le catalogue sert à enregistrer une prestation, SRV-02) | `SalonServiceSecurityIT.getServices_withSellerRole_returns200` ; `salon-catalog-list-page.spec.ts` › should let the seller read the catalogue without any write action | ✅ auto |
| Mauvais rôle (écriture) | jeton vendeuse, POST | 403 `FORBIDDEN` | `SalonServiceSecurityIT.createService_withSellerRole_returns403` | ✅ auto |
| Mass assignment | `"id"` forcé et `"active":false` dans le corps | `id` ignoré, service créé actif | `SalonServiceSecurityIT.createService_withIdInBody_ignoresIt` | ✅ auto |
| Durée invalide | `0` | 422 `VALIDATION_FAILED` | `SalonServiceSecurityIT.createService_durationZero_returns422` | ✅ auto |
| Taux hors bornes | `100.01` | 422 | `SalonServiceSecurityIT.createService_rateAboveHundred_returns422` | ✅ auto |
| Article inactif ou inconnu | article `is_active = false` | 422 « Article introuvable ou inactif » | `SalonServiceSecurityIT.createService_inactiveArticle_returns422` ; `SalonCatalogServiceImplTest.createService_unknownOrInactiveArticle_throwsBusinessException` | ✅ auto |
| Même article deux fois | huile de coco ×2 | 422 côté serveur, message côté écran, rien d'envoyé | `SalonCatalogServiceImplTest.createService_sameArticleListedTwice_throwsBusinessException` ; `salon-catalog-form-page.spec.ts` › should refuse the same article listed twice | ✅ auto |
| Ressource introuvable | GET `/api/v1/salon-services/<uuid aléatoire>` | 404 `NOT_FOUND` | `SalonServiceSecurityIT.getServiceById_unknownId_returns404` ; `SalonCatalogServiceImplTest.updateService_unknownId_throwsNotFound` | ✅ auto |
| Doublon de nom (casse ignorée) | `SOIN-…` après `Soin-…` | 409 `CONFLICT` | `SalonServiceSecurityIT.createService_nameUsedWithOtherCase_returns409` ; `SalonCatalogServiceImplTest.createService_nameAlreadyUsed_throwsConflictWithoutSaving` ; `salon-catalog-form-page.spec.ts` › should show the server message when the name is already used | ✅ auto |
| Version périmée | deux PUT avec `version: 0` | 1er 200, 2e 409 | `SalonServiceSecurityIT.updateService_staleVersion_returns409` ; `SalonCatalogServiceImplTest.updateService_staleVersion_throwsConflictWithoutChange` | ✅ auto |
| Journal | création, modification, (dés)activation | `CREATE` / `UPDATE`, ressource `SALON_SERVICE`, rien si rien ne change | `SalonServiceSecurityIT.createService_byAdmin_returns201WithProductsAndTracesInJournal` ; `SalonCatalogServiceImplTest.updateService_nothingChanged_writesNoJournalLine` ; `SalonCatalogServiceImplTest.setActive_deactivateThenDeactivateAgain_tracesOnce` | ✅ auto |
| Pas de suppression | `DELETE` | non exposé : désactivation seulement | `ArchitectureTest.no_delete_endpoints` ; `salon-catalog-list-page.spec.ts` › should deactivate a service after confirmation and reload the list | ✅ auto |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Durée : bornes | `0`, `601` (écran) ; `1` et `600` acceptés | 422 / message « de 1 à 600 minutes » | `salon-catalog-form-page.spec.ts` › should send nothing when durationMinutes is 0 / 601 | ✅ auto |
| Prix négatif | `-1` | rien d'envoyé, « positif » | `salon-catalog-form-page.spec.ts` › should send nothing when price is -1 | ✅ auto |
| Taux : 100,01 | `100.01` | rien d'envoyé, « entre 0 et 100 » | `salon-catalog-form-page.spec.ts` › should send nothing when commissionRate is 100.01 | ✅ auto |
| Mêmes valeurs, autre ordre de produits | `[argile, coco]` puis `[coco, argile]`, `35000` puis `35000.00` | aucun changement, aucune ligne de journal | `SalonServiceTest.update_sameValuesInOtherOrder_reportsNoChange` | ✅ auto |
| Quantité changée, taux retiré | coco 15 → 20, taux 10 → vide | changement appliqué, taux `null` | `SalonServiceTest.update_quantityChangedAndRateRemoved_appliesAndReportsChange` | ✅ auto |
| Désactivation idempotente | désactiver deux fois | une seule ligne de journal | `SalonCatalogServiceImplTest.setActive_deactivateThenDeactivateAgain_tracesOnce` | ✅ auto |
| Liste filtrée sur les actifs | service désactivé, `?active=true` | absent de la liste | `SalonServiceSecurityIT.setActive_deactivate_hidesServiceFromActiveList` | ✅ auto |
| Catalogue vide | aucun service | état vide « Aucun service salon » | `salon-catalog-list-page.spec.ts` › should show the empty state when the catalogue is empty | ✅ auto |
| Service inconnu ou sans taux pour le résolveur | UUID aléatoire, service sans taux | niveau salon sauté (repli canal puis vendeuse) | `SalonServiceCommissionRatesImplTest.findRate_unknownService_returnsEmpty`, `findRate_serviceWithoutRate_returnsEmpty` | ✅ auto |
| Ligne de produit `null` | `"products": [null, {coco × 2}]` | ligne `null` ignorée (type d'élément `@Nullable`, filtrée dans `SalonCatalogServiceImpl.toDetails`), 201 avec un seul produit, pas de 500 | `SalonCatalogServiceImplTest.createService_nullProductLine_ignoresLineAndSavesOthers` ; `SalonServiceSecurityIT.createService_nullProductLine_returns201AndIgnoresLine` | ☐ à lancer (CI) |
| Plus de 20 produits | 21 lignes | 422 (`@Size`) ; l'écran masque « Ajouter un produit » à 20 lignes | 🔍 code `SalonServiceWDto.products`, `SalonCatalogFormPage.addProduct` | 🔍 code |
| Hors ligne | création sans réseau | saisie mise en file (`OutboxService`, `Idempotency-Key`), toast « enregistrée hors ligne » | `salon-catalog-service.spec.ts` (écritures par la file) ; rejeu : guide SYS-07 | ✅ auto |
| Paramètres de liste | `page`, `search`, `active` | transmis tels quels ; `active` absent si non choisi | `salon-catalog-service.spec.ts` › envoie la pagination, la recherche et le statut choisis ; › n'envoie pas de statut quand il n'est pas choisi | ✅ auto |
| Menu | administrateur et vendeuse | entrée « Catalogue salon » (section Catalogue) | `menu.spec.ts` | ✅ auto |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/salonservice/domain/SalonServiceTest.java`, `.../salonservice/service/impl/SalonCatalogServiceImplTest.java`, `.../salonservice/service/impl/SalonServiceCommissionRatesImplTest.java`, `.../salonservice/controller/SalonServiceSecurityIT.java`.
- Frontend : `valimbavaka-ui/src/app/features/services/data-access/salon-catalog-service.spec.ts`, `.../services/pages/salon-catalog-list-page/salon-catalog-list-page.spec.ts`, `.../services/pages/salon-catalog-form-page/salon-catalog-form-page.spec.ts`, `valimbavaka-ui/src/app/core/layout/menu.spec.ts`.

## Recette manuelle (🧪)

| # | Étape | Résultat attendu | Statut |
|---|---|---|---|
| R1 | Administrateur : menu Catalogue › Catalogue salon › Nouveau service, saisir « Soin visage éclat », 45 min, 35 000 Ar, 12,5 %, huile de coco 0,015 L et masque argile 1 u. | service listé, actif, « 2 produit(s) » ; ligne `CREATE` au journal | 🧪 |
| R2 | Vendeuse : ouvrir `/services/catalog` | liste visible, ni « Nouveau service » ni boutons Modifier / Désactiver ; `/services/catalog/new` refusé | 🧪 |
| R3 | Désactiver l'huile de coco (fiche article), rouvrir le soin visage | la ligne reste, libellée « Article inactif », la fiche s'enregistre | 🧪 |
| R4 | Accessibilité de la fiche (lecteur d'écran, clavier) : libellés des lignes de produits, bouton « Retirer le produit » | chaque champ annoncé, bouton nommé | 🧪 |

## Résultat de l'exécution

- Date : 2026-11-08.
- Environnement : CI GitHub sur `feature/SRV-01-catalogue-services-salon` (commit `6f5c4f1`) : `ci.yml` vert (backend `mvn verify` : 304 tests unitaires + 269 tests d'intégration, dont `SalonCatalogServiceImplTest` 10, `SalonServiceCommissionRatesImplTest` 3, `SalonServiceTest` 3, `SalonServiceSecurityIT` 13 ; frontend `ng test` : 341 tests, dont `salon-catalog-form-page.spec.ts` 12, `salon-catalog-list-page.spec.ts` 4, `salon-catalog-service.spec.ts` 4, `menu.spec.ts` 4) ; `conventions.yml` vert (Spotless, Checkstyle, Prettier, ESLint, gitleaks).
- Anomalies ouvertes : aucune.
- Étapes 🧪 restantes : 4.
