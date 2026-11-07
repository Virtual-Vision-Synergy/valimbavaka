# Scénarios de test — INV-03 · Motif d'écart par ligne

Statut : ✍ écrit le 2026-11-08 — **tests à lancer** (voir « Résultat de l'exécution ») · Ticket : [#79](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/79) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

Une ligne est **en écart** quand elle est comptée et que `compté ≠ théorique figé au comptage` (règle d'INV-02). Les 5 motifs sont ceux de la maquette, déjà en base depuis INV-01 : `BREAKAGE` (Casse), `LOSS` (Perte ou vol), `ENTRY_ERROR` (Erreur de saisie), `UNDECLARED_SHOOTING` (Shooting non déclaré), `OTHER` (Autre motif).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | Administrateur `admin.test@valimbavaka.mg` (`inventory.read`, `inventory.update` posée par INV-02) ; vendeuse (sans permission d'inventaire) ; sans jeton |
| Données de départ | Un article actif `Article-<uuid>` avec un mouvement `RECEIPT` de **12** en `STORE` ; une session `IN_PROGRESS` ouverte sur `STORE` (périmètre `ALL`) ; selon le cas, la ligne comptée à 10 (écart −2), 13 (écart +1) ou 12 (écart 0) |
| Tickets à avoir déployés avant | INV-01 (colonne `reason`), INV-02 (comptage, permission `inventory.update`), JNL-01 (journal) |

## Scénarios issus des critères d'acceptation

Critère : « Un motif est sélectionnable pour toute ligne en écart. »

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Liste des motifs sur une ligne en écart | ligne comptée 10 pour 12 (écart −2), sans motif | la colonne « Motif de l'écart » affiche une liste « Choisir un motif… » + 5 motifs, marquée `aria-invalid="true"` | `inventory-page.spec.ts` « propose un motif pour une ligne en écart et l’enregistre » | ☐ |
| 1b | Choix enregistré | choix « Casse » | `PUT /api/v1/inventories/{id}/lines/{lineId}/reason` avec `{"reason":"BREAKAGE"}` ; la liste garde « Casse », `aria-invalid="false"` | `inventory-page.spec.ts` « propose un motif pour une ligne en écart et l’enregistre », `inventory-service.spec.ts` « enregistre le motif d’écart d’une ligne par PUT avec une clé d’idempotence » | ☐ |
| 1c | Motif côté serveur (écart négatif) | `{"reason":"BREAKAGE"}` sur la ligne à −2 | 200, `reason` `BREAKAGE`, `gap` −2 ; `GET …/lines` renvoie le motif ; journal `UPDATE` · `INVENTORY_SESSION` | `InventoryReasonIT.qualifyLine_lineWithGap_returns200WithReasonAndTracesInJournal`, `InventoryReasonServiceImplTest.qualifyLine_lineWithNegativeGap_recordsReasonAndJournals` | ☐ |
| 1d | Écart positif (surplus) | compté 3, théorique 0, `ENTRY_ERROR` | 200, motif enregistré, écart +3 | `InventoryReasonServiceImplTest.qualifyLine_lineWithPositiveGap_acceptsReason` | ☐ |
| 1e | Changer de motif | `ENTRY_ERROR` puis `UNDECLARED_SHOOTING` (écart +1) | le second remplace le premier ; nouvelle ligne de journal « … : Perte ou vol » côté service | `InventoryReasonIT.qualifyLine_otherReason_replacesPreviousReason`, `InventoryReasonServiceImplTest.qualifyLine_otherReason_replacesPreviousReason` | ☐ |
| 1f | Pas de motif sans écart (écran) | ligne non comptée, puis `12` tapé (écart 0) | aucune liste de motifs, « — » | `inventory-page.spec.ts` « ne propose aucun motif pour une ligne sans écart ou non comptée » | ☐ |
| 1g | Pas de motif sans écart (serveur) | ligne comptée 12 pour 12 | 422 `VALIDATION_FAILED` « La ligne ART-… n'est pas en écart : aucun motif à qualifier. » | `InventoryReasonIT.qualifyLine_lineWithoutGap_returns422`, `InventoryReasonServiceImplTest.qualifyLine_lineWithoutGap_throwsValidationFailed` | ☐ |
| 1h | Ligne non comptée | aucun comptage | 422 `VALIDATION_FAILED`, rien n'est écrit | `InventoryReasonIT.qualifyLine_lineNotCounted_returns422`, `InventoryReasonServiceImplTest.qualifyLine_lineNotCounted_throwsValidationFailedAndChangesNothing` | ☐ |
| 1i | Recomptage sans écart | ligne à −2 avec motif `LOSS`, recomptée à 12 | `PUT …/count` renvoie `gap` 0 et plus de motif | `InventoryReasonIT.countLine_recountWithoutGap_clearsTheReason`, `InventoryCountServiceImplTest.countLine_recountWithoutGap_clearsTheReason` | ☐ |
| 1j | Recomptage toujours en écart | ligne à −2 avec motif `BREAKAGE`, recomptée à 9 | écart −3, motif conservé | `InventoryCountServiceImplTest.countLine_recountStillWithGap_keepsTheReason` | ☐ |
| 1k | Écran après recomptage sans écart | ligne avec « Casse », recomptée 12 (serveur : motif nul), puis 10 retapé | la liste revient sur « Choisir un motif… » (le motif effacé par le serveur n'est pas réaffiché) | `inventory-page.spec.ts` « reprend le motif effacé par le serveur après un recomptage sans écart » | ☐ |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `PUT …/reason` | 401 `UNAUTHENTICATED` | `InventoryReasonIT.qualifyLine_withoutToken_returns401` | ☐ |
| Mauvais rôle | jeton vendeuse | 403 `FORBIDDEN` | `InventoryReasonIT.qualifyLine_withSellerRole_returns403` | ☐ |
| Mass assignment | `"id":"<uuid>","countedQuantity":12,"gap":0` ajoutés au motif `OTHER` | ignorés : ligne de l'URL, compté 10, écart −2, motif `OTHER` | `InventoryReasonIT.qualifyLine_massAssignmentOfIdAndQuantities_isIgnored` | ☐ |
| Motif absent | `{"reason":null}`, `{}` | 422 `VALIDATION_FAILED` | `InventoryReasonIT.qualifyLine_missingReason_returns422` (paramétré, 2 cas) | ☐ |
| Motif inconnu | `{"reason":"THEFT"}` | 400 `MALFORMED_REQUEST` (valeur hors énumération, illisible) | `InventoryReasonIT.qualifyLine_unknownReason_returns400` | ☐ |
| Ligne inconnue ou d'une autre session | `lineId` aléatoire | 404 `NOT_FOUND` | `InventoryReasonIT.qualifyLine_unknownLine_returns404`, `InventoryReasonServiceImplTest.qualifyLine_lineOfAnotherSession_throwsNotFound` | ☐ |
| Session inconnue | `id` aléatoire | 404 | `InventoryReasonIT.qualifyLine_unknownSession_returns404`, `InventoryReasonServiceImplTest.qualifyLine_unknownSession_throwsNotFound` | ☐ |
| Session close | session `CLOSED` | 409 `CONFLICT` (« La session INV-0012 n'est plus en cours. »), rien n'est écrit | `InventoryReasonIT.qualifyLine_closedSession_returns409`, `InventoryReasonServiceImplTest.qualifyLine_closedSession_throwsConflictAndChangesNothing` | ☐ |
| Refus affiché à l'écran | 422 renvoyé au `PUT …/reason` | toast `danger` « Motif de l'écart de ART-0031 non enregistré : … » | `inventory-page.spec.ts` « prévient quand le serveur refuse le motif » | ☐ |
| Journal | motif `BREAKAGE` | `UPDATE` · `INVENTORY_SESSION` · « Motif d'écart de ART-0031 dans INV-0012 : Casse » | `InventoryReasonServiceImplTest.qualifyLine_lineWithNegativeGap_recordsReasonAndJournals` | ☐ |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Rejeu hors ligne (même motif) | `BREAKAGE` déjà enregistré, rejoué | aucune écriture, aucune ligne de journal | `InventoryReasonServiceImplTest.qualifyLine_sameReasonReplayed_changesNothingAndDoesNotJournal` | ☐ |
| Retour sur « Choisir un motif… » | ligne avec `LOSS`, on choisit l'option vide | rien n'est envoyé ; la liste est de nouveau marquée invalide (le serveur garde `LOSS`, qui revient au rechargement) | `inventory-page.spec.ts` « n’envoie rien quand on revient sur « Choisir un motif… » » | ☐ |
| Hors ligne | écriture via `OutboxService.submit('PUT', …)` | clé `Idempotency-Key` envoyée ; le motif choisi reste affiché et part à la reconnexion, après le comptage mis en file avant lui | `inventory-service.spec.ts` « enregistre le motif d’écart d’une ligne par PUT avec une clé d’idempotence » | ☐ |
| Deux administrateurs sur la même ligne | deux motifs envoyés en même temps | le dernier enregistré gagne (pas de version dans le corps) | — | 🔍 code (`BaseEntity.version`) |
| Accessibilité de la page | page chargée avec une ligne en écart (liste de motifs visible) | aucune violation axe (WCAG 2.2 AA, hors contraste) | `inventory-page.spec.ts` « ne présente aucune violation d’accessibilité détectable » | ☐ |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/inventory/service/impl/InventoryReasonServiceImplTest.java` (9 tests), `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/inventory/controller/InventoryReasonIT.java` (13 tests dont 1 paramétré à 2 cas), `InventoryCountServiceImplTest` (2 tests ajoutés : `countLine_recountWithoutGap_clearsTheReason`, `countLine_recountStillWithGap_keepsTheReason`).
- Frontend : `valimbavaka-ui/src/app/features/inventory/pages/inventory-page/inventory-page.spec.ts` (5 tests ajoutés ; le contrôle axe charge désormais une ligne en écart pour couvrir la liste des motifs), `valimbavaka-ui/src/app/features/inventory/data-access/inventory-service.spec.ts` (1 test ajouté).

## Recette manuelle (🧪)

| Étape | Attendu | Statut |
|---|---|---|
| Sur `/inventory`, compter une ligne avec un écart puis choisir « Casse » | la liste garde « Casse » ; le filtre « Sans motif » diminue de 1 ; au rechargement de la page, « Casse » est toujours là | 🧪 |
| Recompter la même ligne à la valeur théorique, puis de nouveau avec un écart | la liste revient vide (« Choisir un motif… ») : le motif a été effacé | 🧪 |
| Couper le réseau, compter une ligne en écart et choisir un motif, rétablir | comptage puis motif partent dans l'ordre, une ligne de journal pour chacun | 🧪 |
| Journal | « Motif d'écart de ART-… dans INV-… : Casse » visible | 🧪 |

## Résultat de l'exécution

- Résultat de l'exécution : à lancer (CI ou local).
- Anomalies : —
