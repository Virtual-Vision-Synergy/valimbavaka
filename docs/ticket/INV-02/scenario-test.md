# Scénarios de test — INV-02 · Comptage physique avec calcul d'écart

Statut : ✍ rédigé le 2026-11-08 — **tests écrits, non exécutés** · Ticket : [#78](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/78) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

Écart = **compté − théorique**, où le théorique est le stock de l'emplacement **figé au moment du comptage** (règle posée par le guide d'INV-01). Négatif = manquant, positif = surplus.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | Administrateur `admin.test@valimbavaka.mg` (`inventory.read`, `inventory.update` ajoutée par la migration `2026_11_08-81`) ; vendeuse (sans permission d'inventaire) ; sans jeton |
| Données de départ | Un article actif `Article-<uuid>` (produit fini, coût 3120.5) avec un mouvement `RECEIPT` de **12** en `STORE` ; une session `IN_PROGRESS` ouverte sur `STORE` (périmètre `ALL`) contenant sa ligne |
| Tickets à avoir déployés avant | INV-01 (sessions et lignes), JNL-01 (journal), STK-01 (stock par emplacement) |

## Scénarios issus des critères d'acceptation

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | La saisie met à jour l'écart affiché immédiatement | théorique 12, on tape `10` (événement `input`, sans valider) | la colonne « Écart » affiche `−2` (signe `minus`) avant tout appel serveur | `inventory-page.spec.ts` « met à jour l’écart dès la saisie, avant tout enregistrement » | ☐ |
| 1b | Quantité validée enregistrée | `10` puis sortie du champ (`change`) | `PUT /api/v1/inventories/{id}/lines/{lineId}/count` avec `{"countedQuantity":10}` ; le théorique renvoyé par le serveur (11) remplace celui du chargement, écart `−1` | `inventory-page.spec.ts` « enregistre la quantité validée et reprend le théorique figé par le serveur » | ☐ |
| 1c | Comptage côté serveur | `{"countedQuantity":10}`, stock `STORE` = 12 | 200, `theoreticalQuantity` 12, `countedQuantity` 10, `gap` −2 ; ligne tracée au journal (`UPDATE`, `INVENTORY_SESSION`) | `InventoryCountIT.countLine_quantityBelowStock_returns200WithNegativeGapAndTracesInJournal`, `InventoryCountServiceImplTest.countLine_quantityBelowStock_freezesTheoreticalReturnsNegativeGapAndJournals` | ☐ |
| 1d | Théorique figé au comptage | comptage 10 sur 12, puis vente de 1 (`SALE` −1) | `GET …/lines` renvoie toujours théorique 12 et écart −2 | `InventoryCountIT.countLine_thenStockMoves_keepsTheGapFrozenInTheLines` | ☐ |
| 1e | Recomptage | 10 puis 12 | le dernier comptage remplace le précédent, théorique refigé, écart 0 | `InventoryCountIT.countLine_recountEqualToStock_returnsZeroGap`, `InventoryCountServiceImplTest.countLine_recount_replacesCountedAndRefreezesTheoretical` | ☐ |
| 1f | Article absent de l'emplacement | aucun mouvement, compté 3 | théorique 0, écart +3, coût unitaire nul accepté | `InventoryCountServiceImplTest.countLine_articleAbsentFromLocation_usesZeroAsTheoreticalAndReturnsPositiveGap` | ☐ |
| 1g | Page sans session en cours | `GET /api/v1/inventories?status=IN_PROGRESS` vide | message « Aucune session d'inventaire en cours… » et lien vers `/inventory/new` | `inventory-page.spec.ts` « invite à ouvrir une session quand aucune n’est en cours » | ☐ |
| 1h | Page avec session en cours | session `INV-0012` Boutique, 1 ligne théorique 12 | titre « Inventaire · Boutique », responsable « Hanta Rakoto », théorique 12, écart « — » | `inventory-page.spec.ts` « affiche la session en cours avec le théorique du serveur » | ☐ |
| 1i | Chargement de toutes les lignes | 2 pages de lignes (size 100) | les deux pages sont lues et fusionnées dans l'ordre | `inventory-service.spec.ts` « charge toutes les pages de lignes de la session en cours » | ☐ |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `PUT …/count` | 401 `UNAUTHENTICATED` | `InventoryCountIT.countLine_withoutToken_returns401` | ☐ |
| Mauvais rôle | jeton vendeuse | 403 `FORBIDDEN` | `InventoryCountIT.countLine_withSellerRole_returns403` | ☐ |
| Mass assignment | `"id":"<uuid>","theoreticalQuantity":10,"gap":0` ajoutés | ignorés : ligne de l'URL, théorique 12 calculé par le serveur, écart −2 | `InventoryCountIT.countLine_massAssignmentOfIdAndTheoretical_isIgnored` | ☐ |
| Données invalides | `-1`, `null`, `1.2345` | 422 `VALIDATION_FAILED` | `InventoryCountIT.countLine_invalidQuantity_returns422` (paramétré, 3 cas) | ☐ |
| Ligne inconnue ou d'une autre session | `lineId` aléatoire | 404 `NOT_FOUND` | `InventoryCountIT.countLine_unknownLine_returns404`, `InventoryCountServiceImplTest.countLine_lineOfAnotherSession_throwsNotFound` | ☐ |
| Session inconnue | `id` aléatoire | 404 | `InventoryCountIT.countLine_unknownSession_returns404`, `InventoryCountServiceImplTest.countLine_unknownSession_throwsNotFound` | ☐ |
| Session close | session `CLOSED` | 409 `CONFLICT` (« La session INV-0012 n'est plus en cours. »), rien n'est écrit | `InventoryCountIT.countLine_closedSession_returns409`, `InventoryCountServiceImplTest.countLine_closedSession_throwsConflictAndChangesNothing` | ☐ |
| Refus affiché à l'écran | 409 renvoyé au `PUT` | toast `danger` « Comptage de ART-0031 non enregistré : … » | `inventory-page.spec.ts` « prévient quand le serveur refuse le comptage (session clôturée) » | ☐ |
| Journal | comptage 10 sur 12 | `UPDATE` · `INVENTORY_SESSION` · « Comptage de ART-0031 dans INV-0012 : 10 compté(s), 12 attendu(s), écart -2 » | `InventoryCountServiceImplTest.countLine_quantityBelowStock_freezesTheoreticalReturnsNegativeGapAndJournals` | ☐ |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Borne basse | `0` compté, stock 0 | 200, écart 0 | `InventoryCountServiceImplTest.countLine_zeroCountedEqualToStock_returnsZeroGap` | ☐ |
| Décimales | `12.000` tapé, théorique 12 | écart affiché `0` (arrondi à 3 décimales, pas de `0.0000001`) | `inventory-page.spec.ts` « accepte une quantité décimale et un écart nul » | ☐ |
| 4 décimales | `1.2345` | 422 | `InventoryCountIT.countLine_invalidQuantity_returns422` | ☐ |
| Champ vidé | `""` puis sortie du champ | rien n'est envoyé, écart « — » | `inventory-page.spec.ts` « n’envoie rien quand le champ est vidé » | ☐ |
| Rejeu hors ligne (même quantité, même théorique) | `10.000` déjà enregistré, rejeu de `10` | aucune écriture, aucune ligne de journal, `countedAt` inchangé | `InventoryCountServiceImplTest.countLine_sameCountReplayed_changesNothingAndDoesNotJournal` | ☐ |
| Hors ligne | écriture via `OutboxService.submit('PUT', …)` | clé `Idempotency-Key` envoyée, saisie mise en file si le réseau est coupé (écart local conservé) | `inventory-service.spec.ts` « enregistre le comptage d’une ligne par PUT avec une clé d’idempotence » | ☐ |
| Deux administrateurs sur la même ligne | deux `PUT` simultanés | le dernier enregistré gagne (pas de version dans le corps) ; verrou optimiste JPA sur la ligne | — | 🔍 code (`BaseEntity.version`) |
| Accessibilité de la page | page chargée avec une session | aucune violation axe (WCAG 2.2 AA, hors contraste) | `inventory-page.spec.ts` « ne présente aucune violation d’accessibilité détectable » | ☐ |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/inventory/service/impl/InventoryCountServiceImplTest.java` (8 tests), `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/inventory/controller/InventoryCountIT.java` (10 tests dont 1 paramétré à 3 cas).
- Frontend : `valimbavaka-ui/src/app/features/inventory/pages/inventory-page/inventory-page.spec.ts` (8 tests), `valimbavaka-ui/src/app/features/inventory/data-access/inventory-service.spec.ts` (3 tests ajoutés). La page n'est plus dans `demo-pages.a11y.spec.ts` : son contrôle axe est fait dans sa propre spec, avec des données serveur.

## Recette manuelle (🧪)

| Étape | Attendu | Statut |
|---|---|---|
| Ouvrir une session sur la Boutique, aller sur `/inventory`, taper une quantité | l'écart change à chaque chiffre tapé ; sortie du champ = enregistrement (rechargement de la page : la quantité est conservée) | 🧪 |
| Scanner un code-barres puis taper la quantité | focus sur la ligne de l'article, même comportement | 🧪 |
| Couper le réseau, saisir deux quantités, rétablir | les saisies partent dans l'ordre, sans doublon au journal | 🧪 |
| Mobile (largeur 380 px) | le champ de comptage reste utilisable au pouce, clavier numérique | 🧪 |

## Résultat de l'exécution

- Date : 2026-11-08 — **à lancer (CI ou local)**.
- Anomalies : aucune constatée à ce stade (tests non exécutés).
- Étapes 🧪 restantes : 4.
