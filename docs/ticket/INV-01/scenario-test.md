# Scénarios de test — INV-01 · Ouverture d'une session d'inventaire

Statut : ✅ tests automatiques verts en CI (07/11/2026), 🧪 recette manuelle à faire · Ticket : [#77](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/77) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

Les « 4 informations » du critère sont lues comme : **emplacement**, **périmètre** (tout l'emplacement ou une gamme), **responsable** du comptage et **date du comptage** (hypothèse, voir le guide).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | Administrateur `admin.test@valimbavaka.mg` (permissions `inventory.read`, `inventory.create`) ; vendeuse (sans permission d'inventaire) ; sans jeton |
| Données de départ | Emplacements du référentiel (`WAREHOUSE`, `STORE`, `DELIVERY`, `TRADE_SHOW`) ; au moins un article actif, ex. `Article-<uuid>` produit fini de la 1re gamme, coût 3120.5, prix 8500 ; un compte actif « Hanta Rakoto » et un compte désactivé |
| Tickets à avoir déployés avant | STK-01, JNL-01, SYS-05 |

## Scénarios issus des critères d'acceptation

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Ouverture avec les 4 informations | `{"locationId":"<STORE>","scope":"ALL","rangeId":null,"responsibleId":"<admin>","countDate":"<demain>"}` | 201, `number` = `INV-0001`…, `status` `IN_PROGRESS`, `locationCode` `STORE`, `lineCount` > 0, en-tête `Location` | `InventoryOpeningIT.openSession_withTheFourFields_returns201WithLinesAndTracesInJournal` | ✅ auto |
| 1b | Emplacement, périmètre, responsable ou date absent (`null`) | `"locationId":null` (puis `scope`, `responsibleId`, `countDate`) | 422 `VALIDATION_FAILED` pour chacun des 4 champs | `InventoryOpeningIT.openSession_missingRequiredField_returns422` (paramétré, 4 cas) | ✅ auto |
| 1c | Périmètre par gamme | `"scope":"RANGE","rangeId":"<Zezika>"` sur `WAREHOUSE` | 201, `scope` `RANGE`, `rangeId` renvoyé | `InventoryOpeningIT.openSession_rangeScope_returns201WithTheRange` | ✅ auto |
| 1d | Gamme absente avec `RANGE` | `"scope":"RANGE","rangeId":null` | 422 | `InventoryOpeningIT.openSession_rangeScopeWithoutRange_returns422`, `InventoryOpeningPolicyTest.check_rangeScopeWithoutRange_throwsBusinessException` | ✅ auto |
| 1e | Gamme donnée avec `ALL` | `"scope":"ALL","rangeId":"<Zezika>"` | 422 (« Aucune gamme ne doit être choisie… ») | `InventoryOpeningPolicyTest.check_allScopeWithRange_throwsBusinessException` | ✅ auto |
| 1f | Formulaire : emplacement et responsable vides | responsable remis à « Choisir… » | rien n'est envoyé, « Ce champ est obligatoire. » sous les deux champs | `inventory-open-page.spec.ts` « should send nothing and flag the missing location and responsible » | ✅ auto |
| 1g | Formulaire : `RANGE` sans gamme | périmètre « Une gamme », gamme vide | rien n'est envoyé, message « Choisissez la gamme… » | `inventory-open-page.spec.ts` « should require a range when the scope is a range » | ✅ auto |
| 1h | Formulaire complet | Boutique, « Une gamme » Zezika, responsable = admin connecté, date du jour | `POST /api/v1/inventories` avec exactement les 4 informations, toast « Session INV-0012 ouverte », retour à `/inventory` | `inventory-open-page.spec.ts` « should open the session with the four fields and go back to the inventory » | ✅ auto |
| 1i | Valeurs proposées | ouverture du formulaire | responsable = admin connecté, date = aujourd'hui, comptes désactivés absents, champ gamme masqué en `ALL` | `inventory-open-page.spec.ts` « should propose the logged-in admin as responsible, today as date and only active accounts » | ✅ auto |
| 1j | Lignes créées à l'ouverture | session `STORE`, `ALL` | une ligne par article du périmètre, triée par code, `countedQuantity` et `gap` absents, `theoreticalQuantity` = stock actuel de l'emplacement | `InventoryOpeningIT.getLines_openedSession_returnsUncountedLinesSortedByCode`, `InventoryServiceImplTest.openSession_validRequest_savesNumberedSessionWithOneLinePerArticleAndJournals`, `InventoryServiceImplTest.getLines_lineNotCounted_returnsCurrentStockAsTheoreticalAndNoGap` | ✅ auto |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `POST /api/v1/inventories` | 401 `UNAUTHENTICATED` | `InventoryOpeningIT.openSession_withoutToken_returns401` | ✅ auto |
| Mauvais rôle (écriture) | jeton vendeuse | 403 `FORBIDDEN` | `InventoryOpeningIT.openSession_withSellerRole_returns403` | ✅ auto |
| Mauvais rôle (lecture) | jeton vendeuse sur `GET /api/v1/inventories` | 403 | `InventoryOpeningIT.getSessions_withSellerRole_returns403` | ✅ auto |
| Mass assignment | `"id":"<uuid>","status":"CLOSED","number":"INV-9999"` ajoutés au corps | ignorés : nouvel `id`, `IN_PROGRESS`, numéro de la séquence | `InventoryOpeningIT.openSession_massAssignmentOfIdStatusAndNumber_isIgnored` | ✅ auto |
| Données invalides | date d'hier | 422 | `InventoryOpeningIT.openSession_countDateYesterday_returns422` | ✅ auto |
| Emplacement inconnu ou inactif | `locationId` aléatoire | 422 (« Emplacement introuvable ou inactif ») | `InventoryOpeningPolicyTest.check_inactiveLocation_throwsBusinessException` | ✅ auto |
| Gamme inconnue ou inactive | `rangeId` aléatoire | 422 | `InventoryOpeningPolicyTest.check_inactiveRange_throwsBusinessException` | ✅ auto |
| Responsable inconnu | `responsibleId` aléatoire | 422 (et non 404 : c'est le corps qui est faux) | `InventoryOpeningPolicyTest.check_unknownResponsible_throwsBusinessException` | ✅ auto |
| Responsable désactivé | compte désactivé | 422 | `InventoryOpeningPolicyTest.check_inactiveResponsible_throwsBusinessException` | ✅ auto |
| Ressource introuvable | `GET /api/v1/inventories/<uuid aléatoire>` | 404 `NOT_FOUND` | `InventoryOpeningIT.getSession_unknownId_returns404`, `InventoryServiceImplTest.getSession_unknownId_throwsNotFoundException` | ✅ auto |
| Conflit | 2e ouverture sur `STORE` alors qu'une session y est `IN_PROGRESS` | 409 `CONFLICT` | `InventoryOpeningIT.openSession_secondSessionInSameLocation_returns409`, `InventoryOpeningPolicyTest.check_sessionAlreadyInProgressInLocation_throwsConflictException` | ✅ auto |
| Conflit affiché dans le formulaire | réponse 409 | message du serveur dans l'alerte, pas de navigation | `inventory-open-page.spec.ts` « should show the conflict when a session is already in progress in the location » | ✅ auto |
| Journal | ouverture réussie | ligne `CREATE`, ressource `INVENTORY_SESSION`, « Ouverture de la session d'inventaire INV-0012 · Boutique (12 articles) » | `InventoryOpeningIT.openSession_withTheFourFields_returns201WithLinesAndTracesInJournal`, `InventoryServiceImplTest.openSession_validRequest_…` | ✅ auto |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Date = aujourd'hui (borne) | 2026-11-08 à 01:30 heure de Tananarive (22:30 UTC la veille) | acceptée : « aujourd'hui » est calculé dans le fuseau `Indian/Antananarivo` | `InventoryOpeningPolicyTest.check_allScopeToday_returnsOpeningWithResponsibleName` | ✅ auto |
| Date = hier (borne − 1) | 2026-11-07 | 422 « … dans le passé » | `InventoryOpeningPolicyTest.check_countDateYesterday_throwsBusinessException` | ✅ auto |
| Date future | aujourd'hui + 3 jours | acceptée | `InventoryOpeningPolicyTest.check_rangeScopeWithActiveRange_keepsTheRange` | ✅ auto |
| Date passée dans le formulaire | 2020-01-01 | rien n'est envoyé, « … ne peut pas être passée. » | `inventory-open-page.spec.ts` « should refuse a count date in the past » | ✅ auto |
| Périmètre vide | gamme sans aucun article | 422 « Aucun article à compter dans ce périmètre. », rien n'est enregistré ni journalisé | `InventoryServiceImplTest.openSession_emptyScope_throwsBusinessExceptionAndSavesNothing` | ✅ auto |
| Article désactivé encore en stock | ART-0087 désactivé, 2 pièces en boutique | compté ; un article désactivé à 0 est exclu | `InventoryScopeResolverTest.articlesToCount_inactiveArticles_keepsOnlyThoseStillInStock` | ✅ auto |
| Plus de 100 articles | 101 articles | les 2 pages d'articles sont lues | `InventoryScopeResolverTest.articlesToCount_twoPages_readsEveryPage` | ✅ auto |
| Filtre par gamme | `RANGE` Zezika | la liste d'articles est filtrée par `rangeId` | `InventoryScopeResolverTest.articlesToCount_rangeScope_filtersArticlesByRange` | ✅ auto |
| Stock d'un seul emplacement | article à 12 en boutique, 30 à l'entrepôt | `getQuantities(STORE)` renvoie 12 seulement ; liste d'articles vide → aucune requête | `StockServiceImplTest.getQuantities_balancesInSeveralLocations_keepsOnlyTheRequestedLocation`, `StockServiceImplTest.getQuantities_noArticle_returnsEmptyMapWithoutQuery` | ✅ auto |
| Filtre de la liste | `GET /api/v1/inventories?status=IN_PROGRESS` après une ouverture | 1 session, `locationCode` `STORE` | `InventoryOpeningIT.getSessions_inProgressFilter_returnsTheOpenedSession` | ✅ auto |
| `size` = 101 | `GET /api/v1/inventories?size=101` | 422 | `InventoryOpeningIT.getSessions_sizeOver100_returns422` | ✅ auto |
| Deux administrateurs en même temps | deux ouvertures simultanées sur `STORE` | une seule passe ; l'index unique partiel `uq_inventory_sessions_location_in_progress` renvoie la 2e en 409 | — (garanti par la base) | 🔍 code |
| Hors ligne | réseau coupé, formulaire valide | saisie mise en file, toast « sera synchronisé », retour à `/inventory` | `inventory-open-page.spec.ts` « should keep an opening made offline and say it will be synchronized » | ✅ auto |
| Rejeu avec la même clé | `POST` avec `Idempotency-Key` | une seule session créée (filtre SYS-07) | `inventory-service.spec.ts` « ouvre une session par POST avec une clé d'idempotence » | ✅ auto |
| Paramètres de liste | statut et emplacement absents / présents | envoyés seulement s'ils sont choisis | `inventory-service.spec.ts` (2 cas « liste des sessions ») | ✅ auto |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/inventory/` — `service/impl/InventoryOpeningPolicyTest`, `InventoryScopeResolverTest`, `InventoryServiceImplTest` (données : `InventoryTestData`), `controller/InventoryOpeningIT` ; `feature/stock/service/impl/StockServiceImplTest` (2 tests `getQuantities_*`).
- Frontend : `valimbavaka-ui/src/app/features/inventory/data-access/inventory-service.spec.ts`, `pages/inventory-open-page/inventory-open-page.spec.ts`.

## Recette manuelle (🧪)

| Étape | Résultat attendu | Statut |
|---|---|---|
| Inventaire → « Ouvrir une session » → choisir Boutique, « Tout l'emplacement », soi-même, aujourd'hui → « Ouvrir la session » | toast « Session INV-xxxx ouverte », retour à la page d'inventaire (encore en démonstration, voir guide) | 🧪 |
| Recommencer sur la Boutique | alerte « Une session d'inventaire est déjà en cours dans cet emplacement. » | 🧪 |
| Choisir « Une gamme » | le champ Gamme apparaît et devient obligatoire | 🧪 |
| Écran mobile (≤ 640 px) | les champs passent sur une colonne | 🧪 |

## Résultat de l'exécution

- Date : 07/11/2026, CI GitHub sur `feature/INV-01-ouverture-session-inventaire` (commit cf9aca6) : tous les contrôles verts (Spotless, Checkstyle, Spring Boot, Angular, Prettier/ESLint, gitleaks, CodeQL, PR/commits/migrations).
- Backend : 289 tests unitaires + 259 tests d'intégration, 0 échec ; dont `InventoryOpeningPolicyTest` 10, `InventoryServiceImplTest` 4, `InventoryScopeResolverTest` 3, `StockServiceImplTest` 10, `InventoryOpeningIT` 17.
- Frontend : 313 tests (54 fichiers), 0 échec ; dont `inventory-open-page.spec.ts` 7, `inventory-service.spec.ts` 4, contrôle axe de la page inventaire.
- Corrections apportées pour passer la CI : formatage Spotless uniquement (`InventorySessionWDto`, `InventoryArticle`, `InventoryScopeResolver`, `InventorySessionRepository`, `InventoryServiceImplTest`, `InventoryScopeResolverTest`, `InventoryOpeningIT`).
- Qodana (avertissement, non bloquant) : `InventoryOpeningPolicy` et `InventoryScopeResolver` sont exposés hors de leur visibilité par le constructeur de `InventoryServiceImpl` (lignes 62-63).
- Anomalies ouvertes : aucune constatée ; à compléter après exécution.
- Étapes 🧪 restantes : 4.
