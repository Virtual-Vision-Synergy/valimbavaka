# Scénarios de test — INV-04 · Clôture d'une session d'inventaire

Statut : ☐ tests écrits, exécution CI en cours (voir « Résultat de l'exécution ») · Ticket : [#80](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/80) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

**Règle de calcul** (INV-01, reprise par INV-02) : à la clôture, chaque article reçoit un mouvement `INVENTORY` de quantité **compté − stock actuel de l'emplacement** (et non l'écart figé au comptage), référencé sur la session (`reference_id` = id de la session). Ainsi le stock système vaut exactement le compté, même si une vente a eu lieu entre le comptage et la clôture. Une quantité nulle ne crée aucun mouvement.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | Administrateur `admin.test@valimbavaka.mg` (`inventory.read`, `inventory.update`, `inventory.close` posée par la migration `2026_11_08-82`) ; vendeuse (sans permission d'inventaire) ; sans jeton |
| Données de départ (IT) | Une gamme créée pour le test `INV04-<8 car.>` avec deux articles actifs : « savon » (`RECEIPT` **12** en `STORE`) et « huile » (`RECEIPT` **5** en `STORE`) ; une session `IN_PROGRESS` ouverte sur `STORE`, périmètre `RANGE` sur cette gamme (2 lignes, pour ne pas compter les articles des autres tests) |
| Tickets à avoir déployés avant | INV-01 (sessions, lignes), INV-02 (comptage, `inventory.update`), INV-03 (motifs), STK-01 (`StockMovementService.record`), JNL-01 (journal), JNL-02 (pas de suppression) |

## Scénarios issus des critères d'acceptation

Critère : « Après clôture, le stock système correspond aux quantités comptées. »

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Clôture avec écarts négatif et positif | savon compté 10 pour 12 (`BREAKAGE`), huile comptée 7 pour 5 (`ENTRY_ERROR`) | 200, `status` `CLOSED`, `closedAt` renseigné ; stock `STORE` savon = **10**, huile = **7** ; mouvements `INVENTORY` −2 et +2 avec `reference_id` = session | `InventoryClosingIT.closeSession_countedSession_adjustsStockToCountedQuantitiesAndTracesJournal` | ☐ |
| 1b | Mouvements signés, une seule transaction | 3 lignes : −2 (`BREAKAGE`), 0, +3 (`ENTRY_ERROR`) | 2 appels à `StockMovementService.record` (type `INVENTORY`, emplacement de la session, `referenceId` = session), dans l'ordre des codes ; session `CLOSED` à l'heure du `Clock` ; `saveAndFlush` | `InventoryClosingServiceImplTest.closeSession_countedLinesWithGaps_recordsSignedInventoryMovementsAndCloses` | ☐ |
| 1c | Ligne sans écart | savon compté 12 pour 12 | aucun mouvement pour le savon ; stock inchangé | `InventoryClosingIT.closeSession_lineWithoutGap_createsNoMovementForIt`, `InventoryClosingServiceImplTest.closeSession_noGap_closesWithoutAnyMovement` | ☐ |
| 1d | Vente entre comptage et clôture | savon compté 10 (stock 12), puis vente de 1 (stock 11) | mouvement `INVENTORY` **−1** (et non −2) ; stock final = 10 = compté | `InventoryClosingIT.closeSession_saleBetweenCountAndClosing_stockStillEqualsCount`, `InventoryClosingServiceImplTest.closeSession_saleAfterCount_adjustsToCurrentStockNotToFrozenGap` | ☐ |
| 1e | Quantité décimale (matière première) | compté 2.5, stock 3.000 | mouvement −0.5 | `InventoryClosingServiceImplTest.closeSession_decimalCountOfRawMaterial_recordsDecimalAdjustment` | ☐ |
| 1f | Article jamais entré dans l'emplacement | compté 4, aucun mouvement en `STORE` (stock 0) | mouvement +4 | `InventoryClosingServiceImplTest.closeSession_articleNeverMovedInLocation_adjustsFromZero` | ☐ |
| 1g | Clôture depuis l'écran | ligne en écart avec motif, bouton « Clôturer la session » puis confirmation | `POST /api/v1/inventories/{id}/close` ; toast « Session INV-0012 clôturée : stock ajusté aux quantités comptées » ; badge « Clôturée », « Session clôturée à HH:MM », champs de comptage et motifs désactivés | `inventory-page.spec.ts` « clôture la session sur le serveur puis verrouille la feuille », `inventory-service.spec.ts` « clôture une session par POST avec une clé d’idempotence et renvoie la session close » | ☐ |
| 1h | Bouton désactivé tant qu'il manque un motif | ligne à −2 sans motif | bouton désactivé, aide « Disponible quand tout est compté et chaque écart expliqué. » | `inventory-page.spec.ts` « n’autorise pas la clôture tant qu’un écart n’a pas de motif » | ☐ |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `POST …/close` | 401 `UNAUTHENTICATED` | `InventoryClosingIT.closeSession_withoutToken_returns401` | ☐ |
| Mauvais rôle | jeton vendeuse | 403 `FORBIDDEN`, session toujours `IN_PROGRESS` | `InventoryClosingIT.closeSession_withSellerRole_returns403AndChangesNothing` | ☐ |
| Mass assignment | corps `{"id":"<uuid>","status":"CANCELLED"}` | ignoré (l'endpoint ne lit pas de corps) : 200, id de l'URL, `CLOSED` | `InventoryClosingIT.closeSession_forgedBody_isIgnored` | ☐ |
| Ligne non comptée | huile jamais comptée | 422 `VALIDATION_FAILED` « Clôture impossible : article(s) non compté(s) ART-… (1). » ; session `IN_PROGRESS`, aucun mouvement, stock inchangé | `InventoryClosingIT.closeSession_lineNotCounted_returns422AndChangesNothing`, `InventoryClosingServiceImplTest.closeSession_lineNotCounted_throwsValidationFailedAndChangesNothing` | ☐ |
| Écart sans motif | savon à −2 sans motif | 422 « Clôture impossible : écart(s) sans motif ART-0031 (1). » ; rien n'est écrit | `InventoryClosingIT.closeSession_gapWithoutReason_returns422AndChangesNothing`, `InventoryClosingServiceImplTest.closeSession_gapWithoutReason_throwsValidationFailedAndChangesNothing` | ☐ |
| Session inconnue | `id` aléatoire | 404 `NOT_FOUND` | `InventoryClosingIT.closeSession_unknownSession_returns404`, `InventoryClosingServiceImplTest.closeSession_unknownSession_throwsNotFound` | ☐ |
| Session déjà close | deuxième `POST …/close` | 409 `CONFLICT` « La session INV-… n'est plus en cours. » ; un seul mouvement `INVENTORY`, stock = 11 | `InventoryClosingIT.closeSession_alreadyClosed_returns409AndAdjustsOnlyOnce`, `InventoryClosingServiceImplTest.closeSession_alreadyClosed_throwsConflictAndChangesNothing` | ☐ |
| Session annulée | statut `CANCELLED` | 409, aucun mouvement | `InventoryClosingServiceImplTest.closeSession_cancelledSession_throwsConflict` | ☐ |
| Saisie après clôture | `PUT …/count` sur la session close | 409 `CONFLICT` (contrôle d'INV-02) | `InventoryClosingIT.countLine_afterClosing_returns409` | ☐ |
| Refus affiché à l'écran | 422 au `POST …/close` | toast `danger` « Clôture de la session INV-0012 refusée : … » ; feuille toujours modifiable | `inventory-page.spec.ts` « prévient et laisse la feuille ouverte quand le serveur refuse la clôture » | ☐ |
| Journal | clôture de la session | `UPDATE` · `INVENTORY_SESSION` · id de la session · « Clôture de la session d'inventaire INV-0012 · Boutique : 3 article(s), 2 ajustement(s) de stock » ; plus une ligne `CREATE` · `STOCK_MOVEMENT` par mouvement (STK-01) | `InventoryClosingIT.closeSession_countedSession_adjustsStockToCountedQuantitiesAndTracesJournal`, `InventoryClosingServiceImplTest.closeSession_countedLinesWithGaps_recordsSignedInventoryMovementsAndCloses` | ☐ |
| JNL-02 : pas de suppression | session et mouvements | aucun endpoint `DELETE` ; repositories `OperationRepository` (ArchUnit `no_delete_endpoints`, `operation_repositories_declare_no_delete`) | `ArchitectureTest` | ☐ |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Plus de 5 lignes non comptées | ART-0001 à ART-0006 non comptés | message limité à 5 codes : « ART-0001, ART-0002, ART-0003, ART-0004, ART-0005… (6) » | `InventoryClosingServiceImplTest.closeSession_moreThanFiveLinesNotCounted_listsFiveCodesAndTheTotal` | ☐ |
| Aucun écart | toutes les lignes comptées au théorique | session `CLOSED`, aucun mouvement, journal « 0 ajustement(s) de stock » | `InventoryClosingServiceImplTest.closeSession_noGap_closesWithoutAnyMovement` | ☐ |
| Deux clôtures simultanées | deux administrateurs confirment en même temps | la session est verrouillée en écriture (`findByIdForUpdate`, `PESSIMISTIC_WRITE`) : la seconde attend la première puis reçoit 409 ; ajustements une seule fois | — (pas de test concurrent) | 🔍 code (`InventorySessionRepository.findByIdForUpdate`) |
| Échec d'un mouvement | emplacement désactivé depuis l'ouverture, ou vente concurrente rendant le stock négatif | 422 ou 409 de `StockMovementService.record` ; toute la clôture est annulée (une seule transaction) | — | 🔍 code (`InventoryClosingServiceImpl.closeSession`, `@Transactional`) |
| Hors ligne | clôture confirmée sans réseau | `POST` mis en file avec `Idempotency-Key` ; toast « Saisie enregistrée hors connexion… » ; la feuille reste ouverte jusqu'à la synchronisation | `inventory-service.spec.ts` « clôture une session par POST avec une clé d’idempotence et renvoie la session close » (clé) | ☐ |
| Rejeu de la clôture | la même clôture renvoyée après succès (rejeu de la file, double clic) | jamais de second ajustement : au pire 409 `CONFLICT` (session déjà close) | `InventoryClosingIT.closeSession_alreadyClosed_returns409AndAdjustsOnlyOnce` | ☐ |
| Accessibilité de la page | page avec une ligne en écart | aucune violation axe | `inventory-page.spec.ts` « ne présente aucune violation d’accessibilité détectable » | ☐ |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/inventory/service/impl/InventoryClosingServiceImplTest.java` (11 tests), `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/inventory/controller/InventoryClosingIT.java` (11 tests).
- Frontend : `valimbavaka-ui/src/app/features/inventory/pages/inventory-page/inventory-page.spec.ts` (3 tests ajoutés, bloc « clôture (INV-04) »), `valimbavaka-ui/src/app/features/inventory/data-access/inventory-service.spec.ts` (1 test ajouté).

## Recette manuelle (🧪)

| Étape | Attendu | Statut |
|---|---|---|
| Sur `/inventory`, compter toutes les lignes, qualifier chaque écart, cliquer « Clôturer la session » puis confirmer | badge « Clôturée », heure de clôture affichée, champs désactivés ; au rechargement, plus de session en cours | 🧪 |
| Ouvrir `/stocks` après la clôture | le stock de l'emplacement de chaque article vaut la quantité comptée | 🧪 |
| Journal | « Clôture de la session d'inventaire INV-… · … » et un « Mouvement INVENTORY de l'article … » par article ajusté | 🧪 |
| Couper le réseau, confirmer la clôture, rétablir | la clôture part à la reconnexion, après les comptages et motifs en file | 🧪 |

## Résultat de l'exécution

- Date : 2026-11-08 — à lancer (CI ou local) : tests écrits, non encore exécutés.
- Anomalies : aucune connue à ce stade.
- Étapes 🧪 restantes : 4.
