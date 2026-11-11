# Scénarios de test — LIV-07 · Historique de statut d'une livraison

Statut : ✅ tests automatiques exécutés (CI verte), recette 🧪 à passer · Ticket : [#52](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/52) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

> **Constat** : l'historique (endpoint, journal, frise du panneau de détail avec heure et auteur) a été livré par [LIV-06](../LIV-06/guide.md), et les transitions de [LIV-03](../LIV-03/guide.md), [LIV-04](../LIV-04/guide.md) et [LIV-05](../LIV-05/guide.md) écrivent chacune leur ligne de journal. LIV-07 ne code donc **aucune fonctionnalité** : il comble l'écart de preuve du critère (chaque changement de statut réel, horodaté et attribué à son auteur) par un test d'intégration de bout en bout.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôles utilisés | administrateur (`admin.test@valimbavaka.mg`, crée et annule) ; livreur « Tojo Nirina » (`DeliveryApiTestSupport.createDriver`, livre ou déclare l'échec) |
| Données de départ | jour ouvert (`POST /api/v1/days`) ; article fini actif (`StockApiTestSupport.createArticle`) avec du stock à l'emplacement `DELIVERY` ; commande saisie pour aujourd'hui, affectée à Tojo |
| Tickets à avoir déployés avant | LIV-03, LIV-04, LIV-05, LIV-06 (historique), SYS-06 (auteur en UUID) |
| Migration | aucune |

## Scénarios issus des critères d'acceptation

### Critère 1 — Chaque changement de statut est horodaté et attribué à un auteur

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| 1 · Cycle complet | commande créée par l'administrateur, livrée par Tojo, puis annulée (undo) par l'administrateur ; `GET /api/v1/deliveries/{id}/history` | 200, `totalCount` 3, du plus ancien au plus récent : `CREATE` (administrateur), `UPDATE` (« Tojo Nirina », libellé « livrée »), `CANCEL` (administrateur, « de « livrée » à « à préparer » ») ; en base, 3 lignes avec `occurred_at` et `actor_id` renseignés | `DeliveryStatusHistoryIT.getHistory_createdDeliveredThenUndone_listsEachStepWithItsAuthorOldestFirst` | ✅ auto |
| 2 · Échec | commande échouée par Tojo (motif « ABSENT »), historique lu par Tojo | 200, `totalCount` 2 ; étape 2 `UPDATE`, auteur « Tojo Nirina », `occurredAt` renseigné, libellé « en échec » et « client absent » | `DeliveryStatusHistoryIT.getHistory_failedOrder_showsTheFailureStepAttributedToTheDriver` | ✅ auto |
| 3 · Transition refusée | commande livrée, puis déclaration d'échec refusée (409) | 409 ; l'historique garde 2 étapes (création, livraison) : un refus n'écrit rien | `DeliveryStatusHistoryIT.getHistory_refusedTransition_addsNoStep` | ✅ auto |
| 4 · Étape de création (LIV-06) | commande créée, lue par la vendeuse | `CREATE`, `occurredAt` et `actorName` renseignés, ni `actorId` ni nom du client | `DeliveryTrackingIT.getHistory_createdOrder_returnsTheCreationStepWithItsTimeAndAuthor` | ✅ auto (LIV-06) |
| 5 · Écran (LIV-06) | clic sur la carte LIV-0001 | frise « 08/11 09:15 · Hanta Rabe » | `deliveries-page.spec.ts` (« ouvre le détail avec l'historique horodaté et attribué de la commande ») | ✅ auto (LIV-06) |
| 6 · Une ligne par transition (unitaires) | livraison, échec, annulation | `journalService.record(UPDATE/CANCEL, "DELIVERY_ORDER", id, libellé)` appelé une fois ; jamais en cas de refus | `DeliveryCompletionServiceImplTest`, `DeliveryFailureServiceImplTest`, `DeliveryUndoServiceImplTest` (LIV-03 à LIV-05) | ✅ auto (LIV-03 à 05) |

## Sécurité et erreurs

| Cas | Résultat attendu | Test | Statut |
|---|---|---|---|
| Sans jeton | 401 `UNAUTHENTICATED` | `DeliveryTrackingIT.getHistory_withoutToken_returns401` (LIV-06) | ✅ auto (LIV-06) |
| Livreur lisant la commande d'un autre | 404 `NOT_FOUND` | `DeliveryTrackingIT.getHistory_driverReadingHisOwnOrAnotherDriversOrder_returns200Or404` (LIV-06) | ✅ auto (LIV-06) |
| Commande inconnue / `size` > 100 | 404 / 422 | `DeliveryTrackingIT.getHistory_unknownOrder_returns404`, `getHistory_sizeAbove100_returns422` (LIV-06) | ✅ auto (LIV-06) |
| Mauvais rôle, mass assignment, 409 sur la lecture | sans objet : lecture seule, les trois rôles ont `deliveries.read` | — | ⏭ hors périmètre |

## Cas limites

- Auteur « Système » : une ligne écrite hors requête authentifiée affiche « Système » (`DeliveryTrackingIT.getHistory_linesOfTheOrderAndOfAnotherResource_listsOnlyTheOrderLinesOldestFirst`, LIV-06).
- Une annulation (undo) puis une nouvelle livraison ajoutent chacune leur étape : l'historique n'est jamais réécrit (journal en insertion seule).
- Plus de 100 étapes pour une commande : une seule page lue (voir guide).

## Tests existants

- Backend (nouveau) : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/delivery/controller/DeliveryStatusHistoryIT.java` (3 tests).
- Backend et front (LIV-06) : `DeliveryTrackingIT`, `DeliveryOrderServiceImplTest`, `JournalServiceImplTest`, `deliveries-page.spec.ts`, `delivery-service.spec.ts`, `delivery-mapping.spec.ts`.

## Recette manuelle (🧪)

| Étape | Résultat attendu | Statut |
|---|---|---|
| Livreur : marquer une commande livrée, puis l'annuler (LIV-05) ; administrateur : ouvrir la commande sur `/deliveries` | frise « Commande créée », « Livrée », « Annulation » avec heure d'Antananarivo et nom de chaque auteur | 🧪 recette |

## Résultat de l'exécution

- Date : 11/11/2026, commit `4514ab8` de `feature/LIV-07-historique-statut-livraison`.
- Environnement : CI GitHub (« CI », « Convention VVS » et « Qodana for JVM » lancés à la main) : tous verts, Qodana sans annotation ; `DeliveryStatusHistoryIT` 3 tests, 0 échec.
- Anomalies ouvertes : aucune connue.
