# Scénarios de test — COM-01 · Règles de commission par canal

Statut : ✅ tests automatiques verts en CI, recette 🧪 à passer · Ticket : [#85](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/85) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

**Décision PO du 07/11/2026** (question client du ticket) : la commission la plus précise gagne, sans cumul : taux du service salon, sinon taux du canal (règle active de COM-01), sinon taux de l'utilisateur (AUTH-03).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (`admin.test@valimbavaka.mg`) pour toutes les écritures ; `SELLER` pour les refus 403 |
| Données de départ | canaux du référentiel SYS-05 (`BOUTIQUE`, `FACEBOOK`, `TELEPHONE`, `LIVRAISON`) ; en test d'intégration, un canal neuf par test (`IT-xxxxxxxx`, inséré par `CommissionRuleSecurityIT.createChannel`) car la base est partagée et un canal n'a qu'une règle active ; vendeuse Miora à 5,00 % sur ce canal |
| Tickets à avoir déployés avant | AUTH-03, JNL-01, SYS-05 ; migration `2026_11_08-90` |

## Scénarios issus des critères d'acceptation

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Le formulaire couvre nom, canal, taux et base de calcul | ouvrir `/commissions/rules/new` | 4 champs : « Nom », « Canal de vente » (canaux actifs), « Taux (%) », « Base de calcul » (Chiffre d'affaires / Marge brute) | `commission-rule-form-page.spec.ts` › should offer name, sales channel, rate and calculation base | ✅ auto |
| 1b | Création d'une règle | `"Facebook live"`, Facebook, `5.25`, `MARGIN` | POST `/api/v1/commission-rules`, retour à la liste | `commission-rule-form-page.spec.ts` › should create the rule with POST then go back to the rules ; `CommissionRuleSecurityIT.createRule_byAdmin_returns201AndTracesInJournal` | ✅ auto |
| 1c | Nom normalisé, taux à 2 décimales | `"  Boutique standard "`, `3.5` | nom `Boutique standard`, taux `3.50`, règle active | `CommissionRuleServiceImplTest.createRule_newNameOnFreeChannel_savesNormalizedActiveRuleAndTracesInJournal` | ✅ auto |
| 1d | Modification avec la version lue | taux `3.5` → `4`, version 2 | PUT avec `version: 2`, ligne `UPDATE` au journal | `commission-rule-form-page.spec.ts` › should save with PUT and send back the version read ; `CommissionRuleServiceImplTest.updateRule_newRate_appliesItAndTracesUpdate` | ✅ auto |
| 1e | Formulaire rempli en modification | règle Boutique standard | champs pré-remplis | `commission-rule-form-page.spec.ts` › should fill the form with the rule read from the server | ✅ auto |
| 1f | Liste des règles | 2 règles dont une inactive sur un canal désactivé | nom, libellé du canal (« Canal inactif » sinon), `3.5 %`, base, statut | `commission-rule-list-page.spec.ts` › should list name, channel label, rate, base and status of each rule | ✅ auto |
| 1g | Résolveur : le plus précis gagne (décision PO) | service salon à 10 %, règle canal à 3 %, vendeuse à 5 % | 10 % (`SALON_SERVICE`) ; sans taux salon : 3 % (`SALES_CHANNEL`) ; sans règle : 5 % (`SELLER`) ; rien : 0 (`NONE`) | `CommissionRateResolverImplTest` (6 tests) ; `CommissionRuleSecurityIT.resolve_channelRuleThenSellerRate_appliesTheMostPreciseLevel` | ✅ auto |
| 1h | Avant SRV-01 : pas de fournisseur de taux salon | service salon passé, aucun bean `SalonServiceCommissionRates` | le niveau salon est ignoré, on tombe sur le canal | `CommissionRateResolverImplTest.resolve_salonServiceWithoutProvider_fallsBackToChannelRule` | ✅ auto |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | GET `/api/v1/commission-rules` | 401 `UNAUTHENTICATED` | `CommissionRuleSecurityIT.getRules_withoutToken_returns401` | ✅ auto |
| Mauvais rôle (lecture) | jeton vendeuse | 403 `FORBIDDEN` | `CommissionRuleSecurityIT.getRules_withSellerRole_returns403` | ✅ auto |
| Mauvais rôle (écriture) | jeton vendeuse, POST | 403 | `CommissionRuleSecurityIT.createRule_withSellerRole_returns403` | ✅ auto |
| Mass assignment | `"id"` forcé et `"active":false` dans le corps | `id` ignoré, règle créée active | `CommissionRuleSecurityIT.createRule_withIdInBody_ignoresIt` | ✅ auto |
| Taux hors bornes | `100.01` | 422 `VALIDATION_FAILED` | `CommissionRuleSecurityIT.createRule_rateAboveHundred_returns422` | ✅ auto |
| Canal inconnu ou inactif | UUID aléatoire | 422 `VALIDATION_FAILED` (« Canal de vente introuvable ») | `CommissionRuleSecurityIT.createRule_unknownSalesChannel_returns422` ; `CommissionRuleServiceImplTest.createRule_unknownOrInactiveChannel_throwsBusinessException` | ✅ auto |
| Base inconnue | `"base":"PROFIT"` | 400 `MALFORMED_REQUEST` | `CommissionRuleSecurityIT.createRule_unknownBase_returns400` | ✅ auto |
| Ressource introuvable | GET `/{uuid aléatoire}` | 404 `NOT_FOUND` | `CommissionRuleSecurityIT.getRuleById_unknownId_returns404` ; `CommissionRuleServiceImplTest.getRuleById_unknownId_throwsNotFound` | ✅ auto |
| Deuxième règle active sur un canal | 2 POST sur le même canal | 409 `CONFLICT` | `CommissionRuleSecurityIT.createRule_secondActiveRuleOnSameChannel_returns409` ; `CommissionRuleServiceImplTest.createRule_channelAlreadyCoveredByActiveRule_throwsConflictWithoutSaving` | ✅ auto |
| Nom déjà pris (casse différente) | `REGLE-…` après `Regle-…` | 409 | `CommissionRuleSecurityIT.createRule_nameUsedWithOtherCase_returns409` ; `CommissionRuleServiceImplTest.createRule_nameAlreadyUsed_throwsConflictWithoutSaving` | ✅ auto |
| Version périmée | PUT avec `version: 0` après une 1ʳᵉ modification | 409 | `CommissionRuleSecurityIT.updateRule_staleVersion_returns409` ; `CommissionRuleServiceImplTest.updateRule_staleVersion_throwsConflictWithoutChange` | ✅ auto |
| Déplacer une règle vers un canal déjà couvert | Boutique → Facebook (déjà une règle active) | 409, rien au journal | `CommissionRuleServiceImplTest.updateRule_moveToChannelCoveredByAnotherActiveRule_throwsConflict` | ✅ auto |
| Réactiver une règle alors qu'une autre est active | désactiver A, créer B, réactiver A | 409 | `CommissionRuleSecurityIT.setActive_deactivateThenNewRuleOnSameChannel_isAccepted` ; `CommissionRuleServiceImplTest.setActive_activateWhileChannelCoveredByAnotherRule_throwsConflictAndStaysInactive` | ✅ auto |
| Journal | création, modification, (dés)activation | lignes `CREATE` / `UPDATE`, `resourceType = COMMISSION_RULE` | `CommissionRuleSecurityIT.createRule_byAdmin_returns201AndTracesInJournal` ; `CommissionRuleServiceImplTest.setActive_deactivate_marksInactiveAndTracesUpdate` | ✅ auto |
| Pas de suppression | — | aucun bouton ni endpoint `DELETE` (ArchUnit `no_delete_endpoints`) | `commission-rule-list-page.spec.ts` › should offer no way to delete a rule, only to deactivate it | ✅ auto |
| Message serveur affiché | 409 à la création ou à l'activation | message dans l'alerte, pas de navigation | `commission-rule-form-page.spec.ts` › should show the server message when the channel already has an active rule ; `commission-rule-list-page.spec.ts` › should show the server error when the channel already has another active rule | ✅ auto |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Taux au-dessus du maximum | `100.01` | rien n'est envoyé, message « entre 0 et 100 » | `commission-rule-form-page.spec.ts` › should send nothing and explain the bounds when the rate is %s | ✅ auto |
| Taux négatif | `-1` | idem | même test (`it.each`) | ✅ auto |
| 3 décimales | `3.555` | idem | même test (`it.each`) | ✅ auto |
| Borne maximale | `100` | envoyé | `commission-rule-form-page.spec.ts` › should accept a rate of exactly 100 | ✅ auto |
| Borne minimale | `0` | accepté par le serveur (`@DecimalMin("0.00")`, `CHECK rate BETWEEN 0 AND 100`) | 🔍 code `CommissionRuleWDto` | 🔍 code |
| Nom vide ou en espaces, canal absent | `"   "` | rien n'est envoyé, deux messages | `commission-rule-form-page.spec.ts` › should ask for a name and a channel when they are missing | ✅ auto |
| Même valeur ré-enregistrée | `" Boutique standard "`, `3` sur une règle à `3.00` | aucune ligne au journal | `CommissionRuleServiceImplTest.updateRule_sameValues_doesNotTraceAnything` | ✅ auto |
| Statut déjà demandé | activer une règle active | idempotent, rien au journal | `CommissionRuleServiceImplTest.setActive_sameState_isIdempotentAndTracesNothing` | ✅ auto |
| Liste vide | aucune règle | état vide « Aucune règle de commission » | `commission-rule-list-page.spec.ts` › should show the empty state when no rule exists | ✅ auto |
| Filtres de liste | `salesChannelId`, `active`, recherche | envoyés seulement s'ils sont choisis | `commission-rule-service.spec.ts` (2 tests) ; `CommissionRuleSecurityIT.setActive_deactivateThenNewRuleOnSameChannel_isAccepted` (filtre canal, 2 règles) | ✅ auto |
| Deux administrateurs créent en même temps sur le même canal | requêtes simultanées | l'index unique partiel `uq_commission_rules_sales_channel_id_active` refuse la seconde : 409 « Cette valeur est déjà utilisée. » (`GlobalExceptionHandler.handleDataIntegrity`) | 🔍 code | 🔍 code |
| Hors ligne | création, modification, (dés)activation | passent par la file `OutboxService` (SYS-07), toast « enregistré, en attente du réseau » | `commission-rule-service.spec.ts` › crée une règle par POST… / désactive une règle via PATCH /status… (passage par l'outbox en ligne) | ✅ auto |
| Vendeuse sans taux, aucun canal couvert | compte non vendeuse | taux `0.00`, source `NONE` | `CommissionRateResolverImplTest.resolve_noLevelGivesARate_returnsZeroWithSourceNone` | ✅ auto |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/commission/service/impl/CommissionRuleServiceImplTest.java` (12), `.../service/impl/CommissionRateResolverImplTest.java` (6), `.../controller/CommissionRuleSecurityIT.java` (14).
- Frontend : `valimbavaka-ui/src/app/features/commissions/data-access/commission-rule-service.spec.ts` (4), `.../pages/commission-rule-form-page/commission-rule-form-page.spec.ts` (10 avec `it.each`), `.../pages/commission-rule-list-page/commission-rule-list-page.spec.ts` (5), `valimbavaka-ui/src/app/core/layout/menu.spec.ts` (entrée « Règles de commission » visible pour l'administrateur seulement).

## Recette manuelle (🧪)

| Étape | Résultat attendu | Statut |
|---|---|---|
| Administrateur : menu Administration › Règles de commission, créer « Boutique standard », Boutique, 3 %, Chiffre d'affaires | règle listée, active, ligne au journal | 🧪 |
| Créer une 2ᵉ règle sur Boutique | message « déjà une règle de commission active » | 🧪 |
| Désactiver puis réactiver la règle (dialogue de confirmation) | statut mis à jour, journal | 🧪 |
| Vendeuse : l'entrée de menu n'apparaît pas, `/commissions/rules` redirige (guard) | pas d'accès | 🧪 |
| Mobile (≤ 640 px) : formulaire sur une colonne | lisible | 🧪 |

## Résultat de l'exécution

- Date : 2026-11-08.
- Environnement : CI GitHub sur `feature/COM-01-regles-commission-canal` (commit `78608d2`) : `ci.yml` vert (backend `mvn verify` : 288 tests unitaires + 256 tests d'intégration, dont `CommissionRuleServiceImplTest` 12, `CommissionRateResolverImplTest` 6, `CommissionRuleSecurityIT` 14 ; frontend `ng test` : 321 tests, dont 19 pour les règles de commission et `menu.spec.ts` 4) ; `conventions.yml` vert (Spotless, Checkstyle, Prettier, ESLint, gitleaks).
- Anomalies ouvertes : aucune à ce stade.
- Étapes 🧪 restantes : 5.
