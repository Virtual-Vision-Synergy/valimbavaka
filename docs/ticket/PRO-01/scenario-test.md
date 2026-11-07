# Scénarios de test — PRO-01 · Création d'une promotion

Statut : ☐ tests écrits, à exécuter (CI ou local) · Ticket : [#58](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/58) · [description](description.md) · [guide](guide.md)

Légende de la colonne « Statut » : ✅ auto = test automatique vert · ✅ base = vérifié en base après migrations · 🔍 code = vérifié par lecture du code (aucun test) · ❌ écart = comportement différent de l'attendu · ⏭ hors périmètre = renvoyé à un autre ticket · 🧪 recette = à faire à la main. Détail : [CONSIGNE.md](../CONSIGNE.md#4-remplir-le-scénario-de-test).

Classes de test :
- `PromotionRulesTest` = `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/promotion/service/impl/PromotionRulesTest.java`
- `PromotionServiceImplTest` = `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/promotion/service/impl/PromotionServiceImplTest.java`
- `PromotionSecurityIT` = `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/promotion/controller/PromotionSecurityIT.java`
- `promotion-request.spec.ts`, `promotion-service.spec.ts` = `valimbavaka-ui/src/app/features/promotions/data-access/`

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (création et lecture), `SELLER` (lecture seule, création refusée), `DELIVERY_DRIVER` (aucun accès) |
| Données de départ | référentiels SYS-05 : gammes `BOTANIKA`, `ZEZIKA`, `SOLIKA` ; canaux `BOUTIQUE`, `FACEBOOK`, `TELEPHONE`, `LIVRAISON` ; un produit fini actif pour le périmètre `ARTICLES` |
| Migration | `2026_11_08-60-changelog.sql` (tables `promotions`, `promotion_channels`, `promotion_targets`, permission `promotions.create`) |
| Tickets à avoir déployés avant | ART-01, JNL-01, SYS-05 (tous dans `develop`) |

## Scénarios issus des critères d'acceptation

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Le formulaire couvre l'ensemble des champs et enregistre la promotion | admin : « Octobre rose », `PERCENT` 15, gamme Botanika, du jour J à J+30, canal Boutique | 201, `Location`, `scope` `RANGES`, `targetIds` = [gamme], `channelIds` = [canal], `usageCount` 0, `status` `ACTIVE` | `PromotionSecurityIT.createPromotion_byAdmin_returns201WithStatusAndTracesInJournal` | ☐ |
| 1b | Saisie normalisée avant enregistrement | nom `"  Octobre rose "`, description `" "`, code `" rose15 "`, canal en double | nom `Octobre rose`, description `null`, code `ROSE15`, un seul canal, compteur 0, ligne de journal `CREATE` | `PromotionServiceImplTest.createPromotion_validRequest_savesNormalizedPromotionAndTracesInJournal` | ☐ |
| 1c | Promotion automatique (sans code) | code `"  "` | code `null`, aucune recherche de doublon de code | `PromotionServiceImplTest.createPromotion_withoutCode_savesAutomaticPromotionWithoutCodeLookup` | ☐ |
| 1d | Statut déduit des dates (jour ouvré à Antananarivo) | horloge 2026-11-07T22:00Z (= 8 nov. 01:00 à Tana), début le 9 nov. | `SCHEDULED` | `PromotionServiceImplTest.getPromotionById_periodStartingTomorrow_mapsScheduledStatus` | ☐ |
| 1e | Formulaire → `PromotionWDto` | gamme `BOTANIKA`, canaux Boutique + Téléphone | `scope` `RANGES`, `targetIds` `['range-botanika']`, `channelIds` des codes `BOUTIQUE` et `TELEPHONE` | `promotion-request.spec.ts` « cible la gamme choisie et traduit les canaux… » | ☐ |
| 1f | Tout le catalogue | portée `ALL` | `scope` `ALL`, `targetIds` vide | `promotion-request.spec.ts` « n'envoie aucune cible pour tout le catalogue » | ☐ |
| 1g | Envoi par la file hors ligne | en ligne | `POST /api/v1/promotions` avec `Idempotency-Key` UUID ; `queued` faux, `body` = promotion | `promotion-service.spec.ts` « crée la promotion par POST avec une clé d'idempotence… » | ☐ |
| 1h | Carte affichée après création | `usageCount` 0, `usageLimit` 100 | carte « 0 / 100 », remise accordée « — » | `promotion-request.spec.ts` « affiche le compteur et la limite… » | ☐ |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `POST /api/v1/promotions` sans `Authorization` | 401 `UNAUTHENTICATED` | `PromotionSecurityIT.createPromotion_withoutToken_returns401` | ☐ |
| Mauvais rôle (création) | jeton vendeuse | 403 `FORBIDDEN` | `PromotionSecurityIT.createPromotion_bySeller_returns403` | ☐ |
| Mauvais rôle (lecture) | jeton livreur, `GET /{id}` | 403 | `PromotionSecurityIT.getPromotionById_byDeliveryDriver_returns403` | ☐ |
| Lecture par la vendeuse | promotion créée par l'admin | 200, même `id` | `PromotionSecurityIT.getPromotionById_bySeller_returnsTheCreatedPromotion` | ☐ |
| Mass assignment | `"id"`, `"status":"ENDED"`, `"usageCount":99` dans le corps | ignorés : nouvel `id`, compteur 0 | `PromotionSecurityIT.createPromotion_massAssignmentOfIdStatusAndUsageCount_isIgnored` | ☐ |
| Données invalides (format) | nom `"  "`, `channelIds` vide | 422 `VALIDATION_FAILED` | `PromotionSecurityIT.createPromotion_missingNameAndNoChannel_returns422` | ☐ |
| Données invalides (règle) | `PERCENT` 100.01 | 422 `VALIDATION_FAILED` | `PromotionSecurityIT.createPromotion_percentAbove100_returns422`, `PromotionRulesTest.check_percentAbove100_throwsBusinessException` | ☐ |
| Ressource introuvable | `GET /api/v1/promotions/{uuid inconnu}` | 404 `NOT_FOUND` | `PromotionSecurityIT.getPromotionById_unknownId_returns404`, `PromotionServiceImplTest.getPromotionById_unknownId_throwsNotFound` | ☐ |
| Doublon | code `AB12CD34EF56` existant, renvoyé en minuscules | 409 `CONFLICT` | `PromotionSecurityIT.createPromotion_codeAlreadyUsedWithOtherCase_returns409`, `PromotionServiceImplTest.createPromotion_codeAlreadyUsed_throwsConflictWithoutSaving` | ☐ |
| Journal | création | ligne `CREATE`, `resourceType` `PROMOTION` | `PromotionSecurityIT.createPromotion_byAdmin_returns201WithStatusAndTracesInJournal` | ☐ |

## Cas limites

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Pourcentage au maximum | `PERCENT` 100, fin aujourd'hui | accepté | `PromotionRulesTest.check_percentOf100OnWholeCatalogueEndingToday_passes` | ☐ |
| Pourcentage maximum + 0.01 | `PERCENT` 100.01 | 422 | `PromotionRulesTest.check_percentAbove100_throwsBusinessException` | ☐ |
| Valeur nulle ou négative | `value` 0 ou -5 | 422 (`@DecimalMin` exclusif) | 🔍 `PromotionWDto` (pas de test dédié) | ☐ |
| « X achetés » hors bornes | `BUY_X` 2.5, 1, 101 | 422 « entre 2 et 100 » | `PromotionRulesTest.check_buyXNotWholeOrBelow2OrAbove100_throwsBusinessException` | ☐ |
| « X achetés » avec zéros | `BUY_X` 3.00 | accepté | `PromotionRulesTest.check_buyX3WithTrailingZeros_passes` | ☐ |
| Fin avant début | du J+10 au J+9 | 422, aucun référentiel interrogé | `PromotionRulesTest.check_endBeforeStart_throwsBusinessException` | ☐ |
| Période déjà terminée | du J-10 à J-1 | 422 « déjà terminée » | `PromotionRulesTest.check_periodAlreadyOverYesterday_throwsBusinessException` | ☐ |
| Minuit à Antananarivo | 22:00 UTC = 01:00 le lendemain à Tana | le jour métier est celui de Tana | `PromotionServiceImplTest.createPromotion_validRequest_savesNormalizedPromotionAndTracesInJournal` (vérifie `check(…, 2026-11-08)`) | ☐ |
| Canal désactivé ou inconnu | `channelIds` = [uuid inconnu] | 422 | `PromotionRulesTest.check_inactiveChannel_throwsBusinessException` | ☐ |
| Tout le catalogue avec des cibles | `ALL` + 1 gamme | 422 | `PromotionRulesTest.check_wholeCatalogueWithTargets_throwsBusinessException` | ☐ |
| Gammes sans cible | `RANGES`, `targetIds` vide | 422 | `PromotionRulesTest.check_rangesWithoutTarget_throwsBusinessException` | ☐ |
| Gamme inconnue | `RANGES` + uuid inconnu | 422 | `PromotionRulesTest.check_unknownRange_throwsBusinessException` | ☐ |
| Article vendu actif | `ARTICLES` + produit fini actif | accepté | `PromotionRulesTest.check_activeFinishedArticle_passes` | ☐ |
| Article non vendable | matière première, produit fini désactivé, uuid inconnu | 422 « Article introuvable, désactivé ou non vendu » | `PromotionRulesTest.check_rawMaterialInactiveOrUnknownArticle_throwsBusinessException` | ☐ |
| Liste de cibles au maximum + 1 | 101 `targetIds` | 422 (`@Size(max = 100)`) | 🔍 `PromotionWDto` (pas de test dédié) | ☐ |
| Gamme absente du référentiel côté écran | portée `SOLIKA` désactivée | aucune cible envoyée, le serveur répond 422 | `promotion-request.spec.ts` « n'envoie pas de cible inconnue… » | ☐ |
| Deux admins, même code au même instant | deux `POST` avec `ROSE15` | un 201, un 409 (index unique `uq_promotions_code`, violation traduite en 409 par `GlobalExceptionHandler`) | 🔍 migration + `GlobalExceptionHandler` (pas de test de concurrence) | ☐ |
| Hors ligne | réseau coupé à l'envoi | saisie en file, toast `common.queuedSaved`, rejouée avec la même `Idempotency-Key` (SYS-07) | 🧪 recette (mécanisme couvert par les tests de `OutboxService`) | ☐ |

## Tests existants

- Backend : `PromotionRulesTest` (12 tests), `PromotionServiceImplTest` (5 tests), `PromotionSecurityIT` (10 tests).
- Frontend : `promotion-request.spec.ts` (4 tests), `promotion-service.spec.ts` (1 test).
- `ArticleService.getArticlesByIds` (ajouté pour ce ticket) n'a pas de test direct : il est simulé dans `PromotionRulesTest` (voir Anomalies).

## Recette manuelle (🧪)

| # | Étape | Résultat attendu | Statut |
|---|---|---|---|
| R1 | Admin → Promotions → formulaire « Nouvelle promotion » : nom, Pourcentage 15, gamme Botanika, dates, Boutique + Facebook, Programmer | toast « Promotion « … » enregistrée », carte en tête de liste avec son statut | 🧪 |
| R2 | Même saisie avec un code déjà pris | toast « Promotion non enregistrée : Ce code promo est déjà utilisé… », formulaire conservé | 🧪 |
| R3 | Pourcentage 120 | message du formulaire ; si forcé, 422 affiché en toast | 🧪 |
| R4 | Couper le réseau (DevTools hors ligne), programmer une promotion, rétablir le réseau | toast « Saisie enregistrée hors connexion », puis synchronisation ; promotion visible dans le journal | 🧪 |
| R5 | Vendeuse : ouvrir Promotions et tenter de programmer | la page s'ouvre ; l'envoi est refusé (403) | 🧪 |

## Résultat de l'exécution

- Date : à lancer (CI ou local) — aucun test exécuté à l'écriture (Maven Central inaccessible depuis le poste de rédaction ; `tsc` et `eslint` passés sur les fichiers frontend touchés).
- Environnement : CI GitHub (`mvn -B -ntp verify`, `npx ng test --watch=false`).
- Anomalies ouvertes :
  1. (faible) `ArticleService.getArticlesByIds` sans test d'intégration propre — à ajouter dans `ArticleRepositoryIT` ou lors de PRO-02.
  2. (faible) Bornes `value` ≤ 0 et 101 cibles vérifiées par lecture de `PromotionWDto` seulement.
  3. (faible) Concurrence sur le code : couverte par l'index unique, pas par un test.
- Étapes 🧪 restantes : 5.
