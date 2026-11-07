# Scénarios de test — TRF-03 · Enregistrement d'un retour de stock

Statut : ✅ exécuté le 2026-11-08 — **CI verte** (voir « Résultat de l'exécution ») · Ticket : [#36](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/36) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible. Légende : ✅ auto = test automatique exécuté et vert en CI · 🧪 recette manuelle.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôles utilisés | administrateur (`admin.test@valimbavaka.mg`), vendeuse (`transfers.return`, sans `transfers.read`), livreur (sans `transfers.return`) |
| Données de départ | emplacements `WAREHOUSE` (Entrepôt), `STORE` (Boutique), `DELIVERY`, `TRADE_SHOW` (SYS-05) ; article « Savon noir 100 g » créé par `POST /api/v1/articles` avec 10 en boutique (réception `RECEIPT` par `StockMovementService.record`, STK-01) |
| Tickets à avoir déployés avant | STK-01, JNL-01, JNL-02, SYS-05 (et SYS-07 / SYS-12 pour la file hors ligne) |
| Migration | `2026_11_08-21-changelog.sql` : tables `transfers`, `transfer_lines`, séquences `transfer_number_seq` et `stock_return_number_seq`, permission `transfers.return` (administrateur, vendeuse) |

## Scénarios issus des critères d'acceptation

### Critère 1 — Un transfert de retour est identifiable par un statut dédié

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| 1 · Retour boutique → entrepôt | 10 en boutique ; `POST /api/v1/transfers/returns` avec 4 « Savon noir » de `STORE` vers `WAREHOUSE`, note « Invendus du soir » | 201, `Location` posé, `status` = `RETURN`, `origin` = `RETURN`, numéro `RET-0001`… ; boutique 6, entrepôt 4 ; deux mouvements `TRANSFER` portant l'id du retour | `StockReturnIT.recordReturn_storeToWarehouse_returns201WithReturnStatusAndMovesTheStock` | ✅ auto |
| 1b · Statut fixé par l'origine | origine `RETURN` / `MORNING` / `MANUAL` | `RETURN` → statut `RETURN`, préfixe `RET` ; `MORNING` et `MANUAL` → `VALIDATED`, préfixe `TRF` | `TransferOriginTest.initialStatus_return_isTheDedicatedReturnStatus`, `TransferOriginTest.initialStatus_morningOrManual_isValidated` | ✅ auto |
| 1c · Service : retour enregistré | numéro de séquence 15 | `RET-0015`, statut `RETURN`, route et lignes reprises, puis `TransferLedger.post` | `TransferServiceImplTest.recordReturn_validRequest_savesAReturnWithItsDedicatedStatusAndMovesTheStock` | ✅ auto |
| 1d · Numéros distincts | deux retours successifs | deux numéros `RET-…` différents | `StockReturnIT.recordReturn_twoReturns_getDistinctReturnNumbers` | ✅ auto |
| 1e · Écran de saisie | vendeuse : `/transfers/return`, « Savon noir » 2,5 | `POST` avec `quantity` 2.5, toast « Retour RET-0016 enregistré. », retour à `/stocks` (à `/transfers` pour l'administrateur) | `stock-return-page.spec.ts` (« envoie le retour, annonce son numéro… », « revient à la liste des transferts… ») | ✅ auto |
| 1f · Valeurs par défaut | ouverture de l'écran | origine Boutique, destination Entrepôt ; seuls les articles en stock à l'origine sont proposés, avec la quantité disponible (« Savon noir (12) », « Huile de coco » à 0 absente) | `stock-return-page.spec.ts` (« propose par défaut… », « ne propose que les articles en stock… ») | ✅ auto |

### Critère 2 — Le modèle de transfert (origine, destination, lignes, statut) est réutilisable par TRF-01, TRF-04, TRF-02 et JR-01

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| 2 · Port Java pour l'ouverture de journée | `TransferService.record` avec origine `MORNING`, 1 « Savon noir » de l'entrepôt (20) vers la boutique | statut `VALIDATED` (TRF-02), numéro `TRF-…`, boutique +1 | `TransferServiceIT.record_morningTransfer_isValidatedWithATransferNumber`, `TransferServiceImplTest.record_morningTransfer_isValidatedWithATransferNumber` | ✅ auto |
| 2b · Deux mouvements par ligne | lignes 4 « Savon » et 1,5 « Huile » de la boutique vers l'entrepôt | `recordAll` reçoit, dans l'ordre : Savon −4 boutique, +4 entrepôt, Huile −1,5 boutique, +1,5 entrepôt (type `TRANSFER`) | `TransferLedgerTest.post_twoLines_recordsAnOutAndAnInTransferMovementPerLineThenTracesTheTransfer` | ✅ auto |
| 2c · Modèle persistant | retour avec note « &nbsp;&nbsp; » et deux lignes | lignes dans l'ordre saisi, note vide ramenée à `null` | `TransferOriginTest.create_returnWithBlankNote_keepsLinesInOrderAndDropsTheNote` | ✅ auto |
| 2d · Transferts croisés simultanés | 4 retours boutique → entrepôt [Savon, Huile] et 4 transferts entrepôt → boutique [Huile, Savon] en parallèle | les 8 réussissent, aucun interblocage (verrous pris dans un ordre stable par `StockMovementService.recordAll`) | `TransferServiceIT.record_crossedTransfersAtTheSameTime_neverDeadlock`, `StockMovementServiceImplTest.recordAll_crossedMovements_locksEveryArticleAndLocationInSortedOrderBeforeRecording` | ✅ auto |

## Sécurité et erreurs

Endpoint : `POST /api/v1/transfers/returns` (permission `transfers.return`). Le port `TransferService.record` n'est pas exposé.

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | retour valide sans `Authorization` | 401 `UNAUTHENTICATED` | `StockReturnIT.recordReturn_withoutToken_returns401` | ✅ auto |
| Mauvais rôle | jeton livreur | 403 `FORBIDDEN` | `StockReturnIT.recordReturn_withDeliveryDriverRole_returns403` | ✅ auto |
| Rôle autorisé | jeton vendeuse, 5 en boutique renvoyés | 201, statut `RETURN` | `StockReturnIT.recordReturn_withSellerRole_returns201` | ✅ auto |
| Mass assignment | `"id"` forcé, `"number":"RET-9999"`, `"status":"VALIDATED"`, `"origin":"MORNING"` | ignorés : autre id, numéro du serveur, statut et origine `RETURN` | `StockReturnIT.recordReturn_massAssignmentOfIdNumberAndStatus_isIgnored` | ✅ auto |
| Origine = destination | `STORE` → `STORE` | 422 `VALIDATION_FAILED` | `StockReturnIT.recordReturn_sameOriginAndDestination_returns422`, `TransferPolicyTest.check_sameOriginAndDestination_throwsBusinessException` | ✅ auto |
| Emplacement inconnu ou inactif | origine = UUID aléatoire | 422 | `StockReturnIT.recordReturn_unknownLocation_returns422`, `TransferPolicyTest.check_unknownOrInactiveLocation_throwsBusinessException` | ✅ auto |
| Aucune ligne | `"lines":[]` | 422 | `StockReturnIT.recordReturn_noLine_returns422`, `TransferPolicyTest.check_noLine_throwsBusinessException` | ✅ auto |
| Article inconnu | `articleId` aléatoire | 404 `NOT_FOUND`, rien n'est écrit | `StockReturnIT.recordReturn_unknownArticle_returns404`, `TransferPolicyTest.check_unknownArticle_propagatesNotFound` | ✅ auto |
| Stock insuffisant | 5 « Savon » et 0 « Huile » en boutique ; retour de 2 Savon + 1 Huile | 409 `CONFLICT` ; tout est annulé : boutique toujours à 5, aucune ligne `transfer_lines` | `StockReturnIT.recordReturn_insufficientStockOnOneLine_returns409AndMovesNothing`, `TransferLedgerTest.post_stockRefused_tracesNothing` | ✅ auto |
| Journal | retour de 2,5 depuis la livraison | une ligne `CREATE` / `TRANSFER` : « Retour de stock RET-… · Livraison → Entrepôt : 1 article(s), 2.5 unité(s) » (plus une ligne `STOCK_MOVEMENT` par mouvement, STK-01) | `StockReturnIT.recordReturn_validReturn_isTracedInTheJournal`, `TransferLedgerTest.post_twoLines_recordsAnOutAndAnInTransferMovementPerLineThenTracesTheTransfer` | ✅ auto |
| Refus métier avant écriture | règle violée | ni `save` ni mouvement | `TransferServiceImplTest.record_rejectedByThePolicy_savesNothingAndMovesNothing` | ✅ auto |

## Cas limites

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Quantité nulle | `0` | 422 | `StockReturnIT.recordReturn_zeroQuantity_returns422` ; front : `stock-return-page.spec.ts` (« refuse une quantité nulle ou à 4 décimales… ») | ✅ auto |
| Quantité négative | `-2` (WDto) ; `-1` par le port Java | 422 (sinon le transfert partirait à l'envers) | `StockReturnIT.recordReturn_negativeQuantity_returns422`, `TransferPolicyTest.check_negativeQuantityThroughThePort_throwsBusinessException` | ✅ auto |
| Décimales | `2.5` accepté ; `1.0005` refusé | 201 / 422 | `StockReturnIT.recordReturn_validReturn_isTracedInTheJournal`, `StockReturnIT.recordReturn_quantityWithFourDecimals_returns422` | ✅ auto |
| Saisie à virgule (front) | « 2,5 » | envoyé `2.5` | `stock-return-page.spec.ts` (« envoie le retour… ») | ✅ auto |
| Nombre de lignes | 101 lignes (maximum 100) | 422 | `StockReturnIT.recordReturn_101Lines_returns422`, `TransferPolicyTest.check_101Lines_throwsBusinessException` | ✅ auto |
| Doublon d'article | Savon 1 puis Savon 2 dans le même retour | 422 « qu'une fois » | `StockReturnIT.recordReturn_sameArticleTwice_returns422`, `TransferPolicyTest.check_sameArticleTwice_throwsBusinessException` | ✅ auto |
| Tout le stock retourné | 5 en boutique, retour de 5 | 201, boutique 0 | `StockReturnIT.recordReturn_withSellerRole_returns201` | ✅ auto |
| Rejeu hors ligne | même `Idempotency-Key` envoyée deux fois | 2ᵉ réponse identique (`Idempotency-Replayed: true`), un seul retour : boutique 0, entrepôt 3 | `StockReturnIT.recordReturn_replayedWithSameIdempotencyKey_recordsOneReturn` | ✅ auto |
| Saisie mise en file | réseau coupé | toast « Saisie enregistrée hors connexion… » (`common.queuedSaved`) | `stock-return-page.spec.ts` (« annonce une saisie mise en file… ») ; service : `transfer-service.spec.ts` | ✅ auto |
| Refus serveur affiché | 409 « Stock insuffisant à cet emplacement : 12 disponible. » | message dans l'alerte, on reste sur la page | `stock-return-page.spec.ts` (« affiche le refus du serveur… ») | ✅ auto |
| Origine = destination (front) | Boutique → Boutique | message « doivent être différentes », rien n'est envoyé | `stock-return-page.spec.ts` (« refuse une origine identique… ») | ✅ auto |
| Lignes du formulaire | ajouter puis retirer | jamais moins d'une ligne (bouton « Retirer » désactivé) | `stock-return-page.spec.ts` (« ajoute et retire des lignes… ») | ✅ auto |
| Deux retours simultanés du dernier stock | deux vendeuses renvoient les 3 dernières unités | un seul passe, l'autre 409 : verrou par article et emplacement de STK-01 | couvert par `StockReadIT.record_concurrentWithdrawals_neverTakeTheStockBelowZero` (STK-01) et 2d | ✅ auto |
| Article désactivé | retour d'un article désactivé | accepté par le serveur (STK-01) ; l'écran ne le propose pas (liste des articles actifs) | — (voir Points d'attention du guide) | ⏭ hors périmètre |

## Tests existants

**Backend** (`valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/`)
- `transfer/domain/TransferOriginTest.java`
- `transfer/service/impl/TransferPolicyTest.java`, `TransferLedgerTest.java`, `TransferServiceImplTest.java`
- `transfer/controller/StockReturnIT.java`, `transfer/service/TransferServiceIT.java` (socle : `stock/controller/StockApiTestSupport.java`, rendu `public`)
- `stock/service/impl/StockMovementServiceImplTest.java` (ajout : `recordAll_crossedMovements_locksEveryArticleAndLocationInSortedOrderBeforeRecording`)
- `ArchitectureTest` (existant) couvre la nouvelle feature : contrôleur mince, `@HasPermission`, DTO `record`, `TransferRepository` sans `delete`.

**Frontend** (`valimbavaka-ui/src/app/features/transfers/`)
- `data-access/transfer-service.spec.ts`
- `pages/stock-return-page/stock-return-page.spec.ts`

## Recette manuelle (🧪)

| Cas | Exemple | Résultat attendu | Statut |
|---|---|---|---|
| Parcours vendeuse | vendeuse, `/stocks` → « Retour de stock » → 2 savons de la boutique vers l'entrepôt | toast « Retour RET-… enregistré. », retour à `/stocks`, la boutique a 2 de moins et l'entrepôt 2 de plus | 🧪 |
| Parcours administrateur | administrateur, `/transfers` → « Retour de stock » | même écran, retour à `/transfers` après l'enregistrement | 🧪 |
| Hors ligne | couper le réseau, saisir un retour, rétablir | message de mise en file, puis un seul retour enregistré | 🧪 |
| Livreur | se connecter en livreur, ouvrir `/transfers/return` | redirigé vers l'accueil, aucun bouton « Retour de stock » | 🧪 |
| Mobile | écran ≤ 640 px | les champs d'une ligne s'empilent, bouton « Retirer » accessible | 🧪 |

## Résultat de l'exécution

- Date : 2026-11-08, CI GitHub Actions sur `feature/TRF-03-retour-stock` (commit `024a483`) : workflows « CI » (backend `mvn verify` avec Testcontainers, frontend `ng test`) et « Convention VVS » (Spotless, Checkstyle, Prettier, ESLint, commits et migrations), tous deux verts.
- Backend : 287 tests unitaires et 262 d'intégration, 0 échec ; dont TRF-03 : `TransferOriginTest` 3, `TransferPolicyTest` 8, `TransferLedgerTest` 2, `TransferServiceImplTest` 3, `StockMovementServiceImplTest` 3 (1 ajouté), `StockReturnIT` 18, `TransferServiceIT` 2.
- Frontend : 54 fichiers, 312 tests, 0 échec ; dont `stock-return-page.spec.ts` 9 et `transfer-service.spec.ts` 1.
- Premier passage : 2 tests front en échec (espion du toast posé avant l'ouverture de l'écran, défaut du test) et formatage Spotless ; corrigés (`fix(transfers)`, `style(transfer)`).
- Anomalies ouvertes : aucune.
- Étapes 🧪 restantes : 5.
