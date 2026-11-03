# Scénarios de test — FRS-04 · Délai de réapprovisionnement selon le type de fournisseur

Statut : ☐ rédigé, tests à lancer (CI ou local) · Ticket : [#20](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/20) · [description](description.md) · [guide](guide.md)

Légende de la colonne « Statut » : ✅ auto = test automatique vert · ✅ base = vérifié en base après migrations · 🔍 code = vérifié par lecture du code (aucun test) · ❌ écart = comportement différent de l'attendu · ⏭ hors périmètre = renvoyé à un autre ticket · 🧪 recette = à faire à la main · ☐ = test écrit, pas encore exécuté. Détail : [CONSIGNE.md](../CONSIGNE.md#4-remplir-le-scénario-de-test).

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

**Règle retenue (option B, validée le 2026-11-03)** : chaque fournisseur porte un délai `leadTimeDays` (entier, 0 à 365 jours). S'il n'est pas saisi, il prend le défaut du type : `LOCAL` = 7 jours, `FOREIGN` = 30 jours. Les alertes de seuil affichent le fournisseur de l'article, son délai et la date de livraison prévue (aujourd'hui à Madagascar + délai). À couverture égale, l'alerte au délai le plus long passe en premier.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (fiche fournisseur et alertes), `SELLER` (alertes en lecture), `DELIVERY_DRIVER` (refusé) |
| Données de départ | « Huiles Ravo » (`LOCAL`, délai 7 j), « Provence Essences » (`FOREIGN`, délai 30 j) ; article « Savon noir 100 g » de seuil 40, jamais mouvementé, rattaché à l'un d'eux |
| Migration | `2026_11_03-01-changelog.sql` : colonne `suppliers.lead_time_days` remplie 7 / 30 selon le type pour les fournisseurs existants |
| Tickets à avoir déployés avant | FRS-01, STK-04 |

## Scénarios issus des critères d'acceptation

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Défaut d'un fournisseur étranger | `POST /suppliers` `{"name":"…","type":"FOREIGN"}` | 201, `leadTimeDays` = 30 | `SupplierLeadTimeIT.createSupplier_foreignWithoutLeadTime_returnsThirtyDays` ; `SupplierServiceImplTest.createSupplier_foreignWithoutLeadTime_takesThirtyDaysByDefault` | ☐ |
| 1b | Défaut d'un fournisseur local | `{"name":"Huiles Ravo","type":"LOCAL"}` | 201, `leadTimeDays` = 7 | `SupplierLeadTimeIT.createSupplier_localWithoutLeadTime_returnsSevenDays` ; `SupplierServiceImplTest.createSupplier_localWithoutLeadTime_takesSevenDaysByDefault` | ☐ |
| 1c | Délai saisi, quel que soit le type | `FOREIGN` avec `"leadTimeDays":12` | 12 conservé | `SupplierServiceImplTest.createSupplier_explicitLeadTime_keepsItWhateverTheType` | ☐ |
| 1d | Modifier seulement le délai | Huiles Ravo 7 → 3 | délai 3, ligne `UPDATE` au journal | `SupplierServiceImplTest.updateSupplier_onlyLeadTimeChanged_updatesItAndTracesInJournal` | ☐ |
| 1e | Changer le type sans délai | `LOCAL` → `FOREIGN`, `leadTimeDays` absent | délai 30 | `SupplierServiceImplTest.updateSupplier_typeChangedWithoutLeadTime_takesTheDefaultOfTheNewType` | ☐ |
| 1f | Alerte d'un article d'un fournisseur étranger | fournisseur `FOREIGN` délai 45, article seuil 40 sans stock | `supplierId`, `supplierName`, `leadTimeDays` = 45, `expectedDeliveryDate` = aujourd'hui (Madagascar) + 45 j | `SupplierLeadTimeIT.getAlerts_articleOfAForeignSupplier_returnsLeadTimeAndExpectedDeliveryDate` | ☐ |
| 1g | Délai et date calculés depuis le jour ouvré de Madagascar | horloge 2026-11-03T22:00Z (= 4 nov. à Madagascar), délais 7 et 30 | livraisons 2026-11-11 et 2026-12-04 ; une seule lecture groupée des fournisseurs | `StockServiceImplAlertTest.getAlerts_articlesWithSuppliers_returnLeadTimeAndDeliveryDateFromTheBusinessDay` | ☐ |
| 1h | Le délai départage deux alertes de même couverture | deux ruptures (couverture 0) : `FOREIGN` 30 j, `LOCAL` 7 j, puis sans fournisseur | ordre : étranger, local, sans fournisseur, puis les couvertures plus hautes | `StockAlertPolicyTest.mostUrgentFirst_sameCoverage_putsTheLongestLeadTimeFirstThenNoSupplier` ; `StockAlertPolicyTest.mostUrgentFirst_severalAlerts_sortsByCoverageThenByCode` (STK-04, inchangé) | ☐ |
| 1i | Calcul de l'alerte | stock 12,5, seuil 40, fournisseur `FOREIGN` 30 j, le 2026-11-03 | livraison prévue le 2026-12-03 | `StockAlertPolicyTest.toAlert_foreignSupplier_returnsSupplierLeadTimeAndExpectedDeliveryDate` | ☐ |
| 1j | Saisie du délai à l'écran (création) | nouvelle fiche, type `LOCAL` puis `FOREIGN` | champ à 7 puis 30, aide « 7 jours … 30 jours … » affichée | `supplier-form-page-lead-time.spec.ts` › « should propose 7 days for a local supplier then 30 days once foreign » | ☐ |
| 1k | Délai saisi gardé au changement de type | délai 12 puis type `FOREIGN` | `PUT` avec `leadTimeDays: 12` | `supplier-form-page-lead-time.spec.ts` › « should keep a lead time typed by hand when the type changes » | ☐ |
| 1l | Fiche relue : délai personnalisé conservé | fiche `FOREIGN` 45 j, type passé à `LOCAL` | champ reste à 45 | `supplier-form-page-lead-time.spec.ts` › « should show the lead time read and keep it when the type changes » | ☐ |
| 1m | Fiche relue : délai resté au défaut | fiche `FOREIGN` 30 j, type passé à `LOCAL` | champ passe à 7 | `supplier-form-page-lead-time.spec.ts` › « should switch a lead time left at the default of the type read to the new default » | ☐ |
| 1n | Délai sur la fiche détail | « Provence Essences » 45 j | « Délai de réapprovisionnement · 45 jours » | `supplier-detail.spec.ts` › « should show the restocking lead time of the supplier » | ☐ |
| 1o | Délai dans le tableau des alertes | alerte Provence Essences, 30 j, 2026-12-03 | colonne « Fournisseur · délai » : « Provence Essences », « 30 j · livraison prévue le 03/12/2026 » | `threshold-alerts.spec.ts` › « affiche le fournisseur, son délai et la date de livraison prévue » | ☐ |
| 1p | Envoi au serveur à la modification | type `LOCAL` → `FOREIGN`, délai resté à 7 | corps `PUT` avec `leadTimeDays: 30` | `supplier-form-page.spec.ts` › « should save every edited field with PUT then go back to the list » | ☐ |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Fiche fournisseur sans jeton | `GET /api/v1/suppliers` | 401 `UNAUTHENTICATED` | `SupplierSecurityIT.getSuppliers_withoutToken_returns401` (FRS-01, contrat inchangé) | ☐ |
| Fiche fournisseur, mauvais rôle | livreur | 403 `FORBIDDEN` | `SupplierSecurityIT.createSupplier_withNonAdminRole_returns403` | ☐ |
| Alertes sans jeton / livreur | `GET /api/v1/stocks/alerts` | 401 / 403 | `StockSecurityIT.getAlerts_withoutToken_returns401`, `StockSecurityIT.getAlerts_withDeliveryDriverRole_returns403` (STK-04) | ☐ |
| Mass assignment | `id`, `active` glissés dans le corps | ignorés (aucun champ `id`/`role` dans `SupplierWDto`) | `SupplierSecurityIT.createSupplier_massAssignmentOfIdAndStatus_isIgnored` | ☐ |
| Délai hors bornes | `-1`, `366` | 422 `VALIDATION_FAILED` | `SupplierLeadTimeIT.createSupplier_leadTimeOutOfBounds_returns422` | ☐ |
| Délai non entier envoyé à l'API | `"leadTimeDays": 2.5` | attendu : refus ; probable : tronqué à 2 par Jackson (`ACCEPT_FLOAT_AS_INT` actif par défaut, non désactivé dans le projet) | aucun test ; voir anomalie 1 | ☐ |
| Fournisseur inconnu | `PUT /suppliers/{uuid inconnu}` | 404 `NOT_FOUND` | `SupplierSecurityIT.updateSupplier_unknownId_returns404` | ☐ |
| Version périmée | modification concurrente | 409 `CONFLICT` | `SupplierSecurityIT.updateSupplier_staleVersion_returns409AndKeepsTheSheet` | ☐ |
| Journal | modification du seul délai | ligne `UPDATE` « Modification du fournisseur … » | `SupplierServiceImplTest.updateSupplier_onlyLeadTimeChanged_updatesItAndTracesInJournal` | ☐ |
| Journal, rien ne change | même fiche renvoyée sans délai, type inchangé | aucune nouvelle ligne (le défaut du type est déjà en place) | `SupplierSecurityIT.updateSupplier_withoutChange_leavesNoNewJournalLine` | ☐ |

## Cas limites

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Bornes acceptées | 0 et 365 | 201, valeur gardée | `SupplierLeadTimeIT.createSupplier_leadTimeOnTheBounds_isAccepted` ; `supplier-form-page-lead-time.spec.ts` › « should accept a lead time of %s days (bounds) » | ☐ |
| Bornes refusées à l'écran | `-1`, `366`, `2.5`, champ vide | rien n'est envoyé, message « Nombre entier de jours entre 0 et 365 » | `supplier-form-page-lead-time.spec.ts` › « should send nothing and explain the bounds when the lead time is "%s" » | ☐ |
| Délai de 0 jour | fournisseur voisin, 0 j | livraison prévue aujourd'hui | `StockAlertPolicyTest.toAlert_zeroDayLeadTime_expectsDeliveryToday` | ☐ |
| Article sans fournisseur | fabrication interne | `supplierId`, `supplierName`, `leadTimeDays`, `expectedDeliveryDate` à `null` ; écran « Fabrication interne (atelier) » | `StockAlertPolicyTest.toAlert_noSupplier_leavesSupplierLeadTimeAndDateNull` ; `SupplierLeadTimeIT.getAlerts_articleWithoutSupplier_returnsNullLeadTime` ; `threshold-alerts.spec.ts` › « indique la fabrication interne pour un article sans fournisseur » | ☐ |
| Fournisseur introuvable | `supplierId` sans fiche | alerte rendue, champs fournisseur à `null` | `StockServiceImplAlertTest.getAlerts_supplierNotFound_returnsTheAlertWithoutLeadTime` | ☐ |
| Aucun identifiant à lire | aucune alerte avec fournisseur | aucune requête SQL | `SupplierServiceImplTest.getLeadTimes_noIdentifier_returnsEmptyMapWithoutQuery` | ☐ |
| Identifiants en double ou inconnus | `[A, inconnu, A]` | une entrée pour A, rien pour l'inconnu | `SupplierServiceImplTest.getLeadTimes_knownAndUnknownIdentifiers_returnsTheKnownOnesById` | ☐ |
| Fuseau et minuit | 22:00 UTC = 01:00 à Madagascar | la date part du jour malgache | `StockServiceImplAlertTest.getAlerts_articlesWithSuppliers_returnLeadTimeAndDeliveryDateFromTheBusinessDay` | ☐ |
| Fournisseur désactivé | article d'un fournisseur désactivé | délai et nom toujours affichés (lecture sans filtre de statut) | 🔍 code : `SupplierRepository.findAllByIdIn` ; à couvrir en recette | ☐ |
| Pagination des alertes | page 2, `size` = 1, page au-delà de la fin | inchangé depuis STK-04 | `StockServiceImplAlertTest.getAlerts_secondPageOfSizeOne_returnsTheLessUrgentAlert`, `getAlerts_pagePastTheEnd_returnsEmptyItemsWithTheTotal` | ☐ |
| Hors ligne | fiche enregistrée sans réseau | le délai part dans la file avec le reste du corps (`Idempotency-Key`) | `supplier-form-page-offline.spec.ts` (FRS-01, fixture complétée) | ☐ |
| Migration | fournisseurs existants avant FRS-04 | `LOCAL` → 7, `FOREIGN` → 30, colonne `NOT NULL`, `CHECK 0..365` | à vérifier sur une base migrée (✅ base après exécution) | ☐ |

## Tests existants

- Backend :
  - `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/stock/controller/SupplierLeadTimeIT.java` (nouveau)
  - `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/stock/service/impl/StockAlertPolicyTest.java`
  - `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/stock/service/impl/StockServiceImplAlertTest.java`
  - `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/supplier/service/impl/SupplierServiceImplTest.java`
  - adaptés au nouveau constructeur ou champ : `StockServiceImplTest`, `StockTestData`, `SupplierRepositoryIT`, `ArticleRepositoryIT`
- Frontend :
  - `valimbavaka-ui/src/app/features/supplier/pages/supplier-form-page/supplier-form-page-lead-time.spec.ts` (nouveau)
  - `supplier-form-page.spec.ts`, `supplier-form-page-offline.spec.ts`, `suppliers/components/supplier-detail/supplier-detail.spec.ts`
  - `stocks/components/threshold-alerts/threshold-alerts.spec.ts`, `stocks/data-access/stock-service.spec.ts`, `stocks/pages/stock-page/stock-page.spec.ts`

## Recette manuelle (🧪)

| Étape | Résultat attendu | Statut |
|---|---|---|
| Créer « Provence Essences » en `FOREIGN` sans toucher au délai | le champ affiche 30, la fiche enregistrée montre « 30 jours » | 🧪 |
| Rattacher un article de seuil 40 sans stock à ce fournisseur, ouvrir Stocks | l'alerte montre « Provence Essences · 30 j · livraison prévue le … » (aujourd'hui + 30 j) | 🧪 |
| Désactiver ce fournisseur puis rouvrir Stocks | l'alerte garde le fournisseur et son délai | 🧪 |
| Affichage en malgache et sur mobile | libellés traduits, colonne lisible sans débordement | 🧪 |

## Résultat de l'exécution

- Date : à lancer (CI ou local)
- Ce qui a tourné : compilation `mvn test-compile`, `tsc -p tsconfig.spec.json`, ESLint et `ng build` OK ; aucun test exécuté (`mvn verify` et `ng test` à lancer)
- Anomalies :
  1. *Faible* : un délai décimal (`2.5`) envoyé directement à l'API est probablement tronqué à 2 sans erreur (comportement par défaut de Jackson, non vérifié). L'écran l'empêche avant l'envoi. Correction proposée : désactiver `ACCEPT_FLOAT_AS_INT` globalement (ticket transverse, touche tous les entiers de l'API) et ajouter un test d'intégration.
  2. *Faible* : un client qui omet `leadTimeDays` à la modification remet le délai au défaut du type (règle « `null` = défaut »). L'écran envoie toujours la valeur ; à garder en tête pour une future intégration (import CSV de fournisseurs, par exemple).
- Étapes 🧪 restantes : 4
