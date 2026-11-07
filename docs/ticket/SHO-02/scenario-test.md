# Scénarios de test — SHO-02 · Réintégration d'un produit shooting

Statut : ✅ rédigé le 2026-11-08 — **tests exécutés en CI, tous verts** (voir « Résultat de l'exécution ») · Ticket : [#44](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/44) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible. Légende : ✅ auto = exécuté en CI et vert · ☐ écrit, pas encore exécuté · 🧪 recette manuelle.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | Administrateur (`ADMIN`, « Rasoa Rakoto ») : `shootings.return` ; vendeuse (`SELLER`) pour le refus 403 (elle peut sortir, pas réintégrer) |
| Données de départ | Un article actif « Gommage café 200 g » : 10 reçus en boutique (`STORE`), puis une sortie shooting de 3 (SHO-01) → boutique à 7, sortie `OUT` |
| Tickets à avoir déployés avant | SHO-01 (sortie shooting, PR empilée), STK-01 (mouvements de stock), JNL-01 (journal) |
| Migration | `2026_11_08-62-changelog.sql` (colonnes `returned_at`, `returned_by_name`, contrainte `ck_shootings_returned`, permission `shootings.return` donnée à `ADMIN`) |

**Hypothèse retenue (cohérente avec SHO-01)** : le critère parle du « stock entrepôt », mais la réintégration remet les produits **dans l'emplacement d'où ils sont sortis** (`Shooting.locationId`, la boutique par défaut). Réintégrer en entrepôt une sortie faite en boutique serait un transfert implicite boutique → entrepôt, non demandé : écart signalé dans la PR, à confirmer par Randy.

## Scénarios issus des critères d'acceptation

### Critère 1 — Le statut passe à Retourné et le stock augmente en conséquence

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| 1 · Réintégration | boutique 10, sortie de 3 (boutique 7) ; `POST /api/v1/shootings/{id}/return` en administrateur | 200, `status` `RETURNED`, `late` false, `returnedBy` « Rasoa Rakoto », `returnedAt` renseigné ; boutique de nouveau à 10 | `ShootingReturnIT.returnShooting_outExit_restoresStockAndReturnsReturned` | ✅ auto |
| 1b · Mouvements typés et journal | 2 articles (2 et 1,5) sortis de la boutique | 2 mouvements `RETURN` positifs dans la boutique, `reference_id` = id de la sortie ; 1 ligne de journal `UPDATE` / `SHOOTING` ; stocks revenus à 5 et 4 | `ShootingReturnIT.returnShooting_validReturn_writesReferencedReturnMovementsAndUpdateJournalEntry` | ✅ auto |
| 1c · Emplacement d'origine | sortie de 2 depuis l'entrepôt (6) | entrepôt de nouveau à 6, boutique inchangée (0) | `ShootingReturnIT.returnShooting_exitFromWarehouse_putsTheStockBackInTheWarehouseOnly` | ✅ auto |
| 1d · Un mouvement par article | sortie SHO-0042 (1 savon, 2,5 gommages) | `StockMovementService.record` ×2 : type `RETURN`, quantités +1 et +2.5, emplacement de la sortie, référence = sortie ; journal `UPDATE` contenant « SHO-0042 » | `ShootingServiceImplTest.returnShooting_outExit_recordsOnePositiveReturnMovementPerArticleIntoItsLocationAndJournal` | ✅ auto |
| 1e · Statut du domaine | `markReturned(2026-11-08T15:50Z, "Rasoa Rakoto")` | `RETURNED`, date et auteur posés | `ShootingTest.markReturned_outExit_isReturnedWithDateAndAuthor` | ✅ auto |
| 1f · Plus de retard après retour | retour prévu 16:00, réintégrée 17:00, lu à 18:00 | `isLateAt` faux | `ShootingTest.isLateAt_returnedAfterExpectedReturn_returnsFalse` | ✅ auto |
| 1g · Date et auteur du serveur | horloge fixée au 2026-11-08T08:00Z, compte « Miora Andria » | `returnedAt` = horloge, `returnedByName` = nom du compte connecté | `ShootingPolicyTest.markReturned_outExit_setsNowAndTheAuthenticatedFullName` | ✅ auto |
| 1h · Liste filtrée | après réintégration, `GET /api/v1/shootings?status=RETURNED&search=<numéro>` puis `status=OUT` | 1 résultat `RETURNED` ; 0 en `OUT` | `ShootingReturnIT.getShootings_afterReturn_listsTheExitAsReturnedOnly` | ✅ auto |
| 1i · Écran : appel | `returnShooting('sho-1')` | `POST /api/v1/shootings/sho-1/return` sans corps, avec `Idempotency-Key` (file hors ligne) | `shooting-service.spec.ts` (« réintègre une sortie par POST /{id}/return… ») | ✅ auto |
| 1j · Écran : identifiant de ligne | sortie `sho-1` | la ligne affichée garde l'`id` du serveur | `shooting-mapping.spec.ts` (« reprend identifiant, numéro… ») | ✅ auto |
| 1k · Écran : bouton Réintégrer | une sortie « En retard » et une « Réintégré » | un seul bouton, pour la sortie dehors ; le clic émet `SHO-0041` | `shooting-list.spec.ts` (« propose Réintégrer pour la sortie encore dehors… ») | ✅ auto |

## Sécurité et erreurs

Endpoint : `POST /api/v1/shootings/{id}/return` (`shootings.return`, administrateur), sans corps.

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `POST /api/v1/shootings/{uuid}/return` | 401 `UNAUTHENTICATED` | `ShootingSecurityIT.returnShooting_withoutToken_returns401` | ✅ auto |
| Mauvais rôle | jeton vendeuse sur une sortie `OUT` (boutique 1) | 403 `FORBIDDEN` ; sortie toujours `OUT`, boutique à 1 | `ShootingSecurityIT.returnShooting_withSellerRole_returns403AndKeepsTheExitOut` | ✅ auto |
| Écran : sans permission | `canReturn` faux (vendeuse) | pas de bouton « Réintégrer » | `shooting-list.spec.ts` (« masque Réintégrer sans la permission… ») | ✅ auto |
| Mass assignment | corps `{"status":"OUT","locationId":<entrepôt>,"returnedBy":"Quelqu'un d'autre","returnedAt":"2020-01-01T00:00:00Z"}` | ignoré : `RETURNED`, auteur « Rasoa Rakoto », date du serveur, stock remis en boutique (2), entrepôt à 0 | `ShootingSecurityIT.returnShooting_massAssignmentOfStatusLocationAndAuthor_isIgnored` | ✅ auto |
| Sortie inconnue | UUID aléatoire | 404 `NOT_FOUND`, aucune écriture | `ShootingReturnIT.returnShooting_unknownId_returns404`, `ShootingServiceImplTest.returnShooting_unknownId_throwsNotFoundException` | ✅ auto |
| Déjà réintégrée | deuxième `POST …/return` | 409 `CONFLICT` ; stock inchangé (4), un seul mouvement `RETURN` | `ShootingReturnIT.returnShooting_alreadyReturned_returns409AndChangesNothing`, `ShootingServiceImplTest.returnShooting_alreadyReturned_propagatesConflictWithoutMovementNorJournal` | ✅ auto |
| Déjà réintégrée (domaine) | `markReturned` deux fois | `ConflictException` « … SHO-0042 … », date et auteur de la première réintégration conservés | `ShootingTest.markReturned_alreadyReturned_throwsConflictExceptionAndKeepsTheFirstReturn` | ✅ auto |
| Session absente | aucun auditeur courant | 401 `UNAUTHENTICATED` (`UnauthorizedException`), sortie toujours `OUT` | `ShootingPolicyTest.markReturned_withoutSession_throwsUnauthorizedExceptionAndKeepsTheExitOut` | ✅ auto |
| Emplacement désactivé depuis la sortie | emplacement de la sortie inactif | 422 `VALIDATION_FAILED` (règle STK-01), rien n'est écrit | couvert par `StockMovementPolicy.check` (STK-01) | 🧪 |
| Journal | réintégration acceptée | ligne `UPDATE` / `SHOOTING` « Réintégration shooting SHO-xxxx : n article(s) remis en stock » + une ligne `STOCK_MOVEMENT` par article | `ShootingReturnIT.returnShooting_validReturn_writesReferencedReturnMovementsAndUpdateJournalEntry` | ✅ auto |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Deux réintégrations simultanées | 4 administrateurs cliquent en même temps sur la même sortie | une seule passe (200), les autres 409 ; un seul mouvement `RETURN`, boutique à 3 (verrou `findByIdForUpdate`) | `ShootingReturnIT.returnShooting_concurrentReturns_acceptsOnlyOne` | ✅ auto |
| Sortie en retard | retour prévu dépassé, statut affiché « En retard » | la réintégration est acceptée comme pour une sortie dans les temps | `ShootingTest.isLateAt_returnedAfterExpectedReturn_returnsFalse` (le retard disparaît) | ✅ auto |
| Réintégration partielle | 3 sortis, 2 rendus (1 abîmé) | non prévue : tout ce qui est sorti est réintégré ; la perte se déclare à part (perte / inventaire) | — (hors périmètre, à décider) | ⏭ hors périmètre |
| Hors ligne | réseau coupé, clic « Réintégrer » | saisie en file (`common.queuedSaved`), ligne affichée « Réintégré » en attendant ; rejouée sans doublon grâce à `Idempotency-Key` ; un refus au rejeu (409 déjà réintégrée) reste en échec dans la file (SYS-07) | `shooting-service.spec.ts` (« réintègre une sortie par POST /{id}/return… ») | 🧪 |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/shooting/domain/ShootingTest.java`, `service/impl/ShootingPolicyTest.java`, `service/impl/ShootingServiceImplTest.java`, `controller/ShootingReturnIT.java` (nouveau), `controller/ShootingSecurityIT.java`, support `controller/ShootingApiTestSupport.java` (`createShooting`, `postReturn`).
- Frontend : `valimbavaka-ui/src/app/features/services/data-access/shooting-service.spec.ts`, `data-access/shooting-mapping.spec.ts`, `components/shooting-list/shooting-list.spec.ts` (nouveau) ; `src/app/features/demo-pages.a11y.spec.ts` (page Salon & shooting).

## Recette manuelle (🧪)

1. En administrateur, onglet **Salon & shooting → Shooting** : sur une sortie « Sorti » ou « En retard », cliquer **Réintégrer**. Le toast « Sortie SHO-xxxx réintégrée : statut Réintégré, stock remis à jour » s'affiche, la ligne passe à « Réintégré » et disparaît de l'encart « Sorties en cours » ; la page **Stocks** montre l'emplacement de sortie (boutique) remonté.
2. En vendeuse : le bouton **Réintégrer** n'apparaît pas ; forcer l'appel par l'API : 403.
3. Couper le réseau, réintégrer : la saisie part en file, la ligne s'affiche « Réintégré », puis la synchronisation remet le stock.
4. **Journal** : une entrée « Réintégration shooting SHO-xxxx … » et une entrée « Mouvement RETURN … » par article.

## Résultat de l'exécution

- Date : 2026-11-08 — CI GitHub Actions lancée à la main sur `feature/SHO-02-reintegration-produit-shooting` : `ci.yml` vert (commits `4eae7cc` et `117266b`) ; `conventions.yml` (Spotless, Checkstyle, Prettier, ESLint, migrations, gitleaks) vert sur `117266b` après un reformatage Spotless de `ShootingReturnIT`.
- Environnement : `mvn -B -ntp verify` avec Testcontainers PostgreSQL (290 tests unitaires et 269 d'intégration au total, 0 échec) ; `ng test` Vitest (318 tests, 56 fichiers, 0 échec).
- Tests du ticket : 18 backend ajoutés (8 unitaires : `ShootingTest` +3, `ShootingPolicyTest` +2, `ShootingServiceImplTest` +3 ; 10 d'intégration : `ShootingReturnIT` 7, `ShootingSecurityIT` +3) et 3 frontend ajoutés (`shooting-list.spec.ts` 2, `shooting-service.spec.ts` +1) plus 1 adapté (`shooting-mapping.spec.ts`), tous verts ; `demo-pages.a11y.spec.ts` (page Salon & shooting) vert.
- Anomalies ouvertes : aucune. Écart de spécification signalé : réintégration dans l'emplacement d'origine et non « en entrepôt » (voir l'hypothèse ci-dessus), à confirmer par Randy. Étapes 🧪 restantes : 2 (emplacement désactivé, hors ligne) + recette manuelle.
