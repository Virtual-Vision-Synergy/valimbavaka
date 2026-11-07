# Scénarios de test — SHO-01 · Enregistrement d'une sortie shooting

Statut : ✍ rédigé le 2026-11-08 — **tests écrits, non exécutés** (voir « Résultat de l'exécution ») · Ticket : [#43](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/43) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible. Légende : ☐ écrit, pas encore exécuté · 🧪 recette manuelle.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | Vendeuse (`SELLER`) et administrateur (`ADMIN`) : `shootings.create` + `services.read` ; livreur (`DELIVERY_DRIVER`) pour les refus |
| Données de départ | Un article actif « Gommage café 200 g » avec 10 en boutique (`STORE`), reçus par `StockMovementService.record` (`RECEIPT`) ; emplacements du référentiel actifs |
| Tickets à avoir déployés avant | STK-01 (mouvements de stock), JNL-02 (`OperationRepository`), JNL-01 (journal) |
| Migration | `2026_11_08-61-changelog.sql` (tables `shootings`, `shooting_items`, séquence `shooting_number_seq`, permission `shootings.create`) |

Hypothèse retenue (pas de règle client écrite) : le « stock vendable » est le stock de l'emplacement d'où sortent les produits, **la boutique par défaut** dans l'écran ; l'utilisateur peut choisir un autre emplacement actif.

## Scénarios issus des critères d'acceptation

### Critère 1 — La sortie diminue le stock vendable et le statut passe à Sorti

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| 1 · Sortie par la vendeuse | 10 en boutique ; `POST /api/v1/shootings` avec 3 « Gommage café », retour demain | 201, `Location`, `number` `SHO-xxxx`, `status` `OUT`, `late` false, `takenBy` « Miora Andria » ; boutique à 7 | `ShootingCreationIT.createShooting_withSellerRole_decreasesStoreStockAndReturnsOut` | ☐ |
| 1b · Mouvement typé et journal | sortie de 1,5 | un mouvement `SHOOTING` de −1,5 dont `reference_id` = id de la sortie ; une ligne de journal `CREATE` / `SHOOTING` | `ShootingCreationIT.createShooting_validExit_writesReferencedShootingMovementAndJournalEntry` | ☐ |
| 1c · Un mouvement par article | 2 articles (1 et 2,5) | 2 appels `StockMovementService.record` : type `SHOOTING`, quantités −1 et −2.5, emplacement de la sortie, référence = sortie ; journal « Sortie shooting SHO-0042 … » | `ShootingServiceImplTest.createShooting_validRequest_recordsOneNegativeShootingMovementPerArticleAndJournal` | ☐ |
| 1d · Numéro et retard | séquence à 7 | numéro `SHO-0007`, statut affiché calculé avec l'horloge (`late`) | `ShootingServiceImplTest.createShooting_validRequest_returnsMappedExitWithLateFlag` | ☐ |
| 1e · Statut à la création | `Shooting.open` | statut `OUT`, articles et auteur repris | `ShootingTest.open_newExit_isOutWithItsItems` | ☐ |
| 1f · Retard | retour prévu dépassé, encore `OUT` | `isLateAt` vrai ; avant ou à l'heure prévue, faux | `ShootingTest.isLateAt_afterExpectedReturnWhileOut_returnsTrue`, `ShootingTest.isLateAt_beforeOrAtExpectedReturn_returnsFalse` | ☐ |
| 1g · Copie des articles | article ART-0031, note « ␣␣Campagne de Noël␣␣ » | code et nom copiés, auteur = compte connecté, note sans espaces ; note vide → `null` | `ShootingPolicyTest.draft_validRequest_copiesArticlesAuthorAndTrimmedNote`, `ShootingPolicyTest.draft_blankNote_isStoredAsNull` | ☐ |
| 1h · Liste | après une sortie, `GET /api/v1/shootings?status=OUT&search=<numéro>` | `totalCount` 1, statut `OUT` | `ShootingCreationIT.getShootings_afterCreation_listsTheExitAsOut` | ☐ |
| 1i · Écran : statut affiché | `OUT` + `late` false / true ; `RETURNED` | « Sorti » / « En retard » / « Réintégré » ; retour prévu en heure locale | `shooting-mapping.spec.ts` (5 tests) | ☐ |
| 1j · Écran : lecture et envoi | `GET /api/v1/shootings` (100 plus récentes) ; enregistrement | lignes converties ; `POST` par la file hors ligne avec `Idempotency-Key` | `shooting-service.spec.ts` (3 tests) | ☐ |
| 1k · Écran : formulaire | boutique 4 ; saisie de 3 | boutique proposée par défaut ; envoi `{locationId, items:[{articleId, quantity:3}], expectedReturnAt ISO, note:null}` | `shooting-form.spec.ts` (« propose la boutique… », « envoie emplacement… ») | ☐ |

## Sécurité et erreurs

Endpoints : `GET /api/v1/shootings`, `GET /api/v1/shootings/{id}` (`services.read`), `POST /api/v1/shootings` (`shootings.create`).

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `POST` puis `GET /api/v1/shootings` | 401 `UNAUTHENTICATED` | `ShootingSecurityIT.createShooting_withoutToken_returns401`, `ShootingSecurityIT.getShootings_withoutToken_returns401` | ☐ |
| Mauvais rôle | jeton livreur | 403 `FORBIDDEN`, stock inchangé | `ShootingSecurityIT.createShooting_withDeliveryDriverRole_returns403`, `ShootingSecurityIT.getShootings_withDeliveryDriverRole_returns403` | ☐ |
| Rôle autorisé en lecture | jeton vendeuse | 200 | `ShootingSecurityIT.getShootings_withSellerRole_returns200` | ☐ |
| Mass assignment | corps avec `id`, `number` « SHO-9999 », `status` « RETURNED », `takenBy`, `late` | ignorés : id et numéro du serveur, `OUT`, auteur = compte connecté (« Rasoa Rakoto »), `late` false | `ShootingSecurityIT.createShooting_massAssignmentOfIdNumberStatusAndAuthor_isIgnored` | ☐ |
| Retour prévu passé | retour il y a 1 h | 422 `VALIDATION_FAILED`, stock inchangé | `ShootingCreationIT.createShooting_expectedReturnInThePast_returns422`, `ShootingPolicyTest.draft_expectedReturnNotInTheFuture_throwsBusinessException` | ☐ |
| Aucun article | `items: []` | 422 `VALIDATION_FAILED` | `ShootingCreationIT.createShooting_withoutItems_returns422` | ☐ |
| Quantité nulle | quantité 0 | 422 `VALIDATION_FAILED` | `ShootingCreationIT.createShooting_zeroQuantity_returns422` | ☐ |
| Article en double | le même article deux fois | 422, stock inchangé | `ShootingCreationIT.createShooting_sameArticleTwice_returns422`, `ShootingPolicyTest.draft_sameArticleTwice_throwsBusinessException` | ☐ |
| Emplacement inactif | emplacement absent ou désactivé | 422 avant toute écriture | `ShootingPolicyTest.draft_inactiveLocation_throwsBusinessException` | ☐ |
| Article inconnu | UUID aléatoire | 404 `NOT_FOUND` | `ShootingCreationIT.createShooting_unknownArticle_returns404` | ☐ |
| Sortie inconnue | `GET /api/v1/shootings/{uuid aléatoire}` | 404 `NOT_FOUND` | `ShootingCreationIT.getShootingById_unknownId_returns404`, `ShootingServiceImplTest.getShootingById_unknownId_throwsNotFoundException` | ☐ |
| Stock insuffisant | 10 en entrepôt, 0 en boutique, sortie boutique de 1 | 409 `CONFLICT` | `ShootingCreationIT.createShooting_noStockInTheLocation_returns409` | ☐ |
| Tout ou rien | A = 5, B = 1 en boutique ; sortie A 2 + B 3 | 409 ; A reste à 5, B à 1, aucune ligne `shooting_items` ; pas de journal | `ShootingCreationIT.createShooting_oneArticleAboveStock_returns409AndChangesNothing`, `ShootingServiceImplTest.createShooting_insufficientStock_propagatesConflictWithoutJournal` | ☐ |

## Cas limites et hors ligne

| Cas | Résultat attendu | Test | Statut |
|---|---|---|---|
| Écran : aucune ligne | message « Ajoutez au moins un article. », rien n'est envoyé | `shooting-form.spec.ts` (« refuse une sortie sans article… ») | ☐ |
| Écran : quantité > disponible de l'emplacement | alerte « Stock insuffisant… », bouton bloqué | `shooting-form.spec.ts` (« signale une quantité supérieure… ») | ☐ |
| Écran : retour passé | message « Le retour prévu doit être dans le futur. » | `shooting-form.spec.ts` (« refuse un retour prévu déjà passé ») | ☐ |
| Décimales | 3 décimales au maximum (`@Digits`, puis règle STK-01) | couvert par `StockMovementPolicyTest.check_fourDecimals_throwsBusinessException` (STK-01) | ☐ |
| Plus de 50 articles | 422 (`@Size(max = 50)`) | — (validation déclarative) | 🧪 |
| Deux sorties simultanées du dernier article | une seule passe, l'autre 409 (verrou `pg_advisory_xact_lock` de STK-01) | couvert par le test de concurrence de STK-01 | 🧪 |
| Hors ligne | la sortie part en file (`common.queuedSaved`), rejouée sans doublon grâce à `Idempotency-Key` (SYS-02/SYS-07) ; un refus au rejeu (409 stock) reste dans la file en échec | `shooting-service.spec.ts` (« enregistre la sortie par POST… ») | 🧪 |

## Recette manuelle (🧪)

1. En vendeuse, onglet **Salon & shooting → Shooting** : le formulaire propose la boutique, ajouter un article, saisir une quantité, enregistrer. Le toast « Sortie SHO-xxxx enregistrée : statut Sorti » s'affiche, la sortie apparaît « Sorti » et la page **Stocks** montre la boutique diminuée.
2. Saisir plus que le disponible : l'alerte s'affiche ; forcer par l'API : 409.
3. Couper le réseau, enregistrer : la saisie est mise en file, puis synchronisée au retour du réseau.
4. **Journal** : une entrée « Sortie shooting SHO-xxxx … » et une entrée « Mouvement SHOOTING … » par article.

## Résultat de l'exécution

- Date : à lancer (CI ou local) — **aucun test n'a été exécuté** par le traiteur (CLAUDE.md règle n°3).
- Environnement : à renseigner (`mvn -B -ntp verify` avec Docker pour Testcontainers ; `npm ci && npx ng test --watch=false`).
- Tests écrits : 29 backend (12 unitaires, 17 d'intégration Testcontainers), 13 frontend.
- Anomalies ouvertes : aucune connue.
