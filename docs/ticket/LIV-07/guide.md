# Guide d'intégration — LIV-07 · Historique de statut d'une livraison

Statut : ✅ rédigé, tests verts en CI (commit `4514ab8`) · Ticket : [#52](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/52) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Le critère (chaque changement de statut est horodaté et attribué à un auteur) est **déjà couvert par le code de LIV-06** : l'historique d'une commande est la liste de ses lignes de journal, chacune avec son heure et le nom de son auteur, et les transitions de LIV-03 (livrée), LIV-04 (échec) et LIV-05 (annulation d'une action) écrivent chacune leur ligne. LIV-07 ne change aucun code de production : il ajoute le test d'intégration de bout en bout qui le prouve, le scénario et ce guide.

## Contrat exposé

### Endpoints backend

Aucun nouvel endpoint. Contrat de LIV-06, inchangé : `GET /api/v1/deliveries/{id}/history` (`deliveries.read`), page de `DeliveryEventRDto` (`occurredAt`, `actorName`, `action`, `description`), plus ancienne étape d'abord. Voir le [guide de LIV-06](../LIV-06/guide.md#contrat-exposé).

### Services et ports Java réutilisables

- `DeliveryOrderService.getHistory` et `JournalService.getResourceEntries` (LIV-06), inchangés.
- Écritures : `DeliveryOrderServiceImpl` (création, `CREATE`), `DeliveryCompletionServiceImpl.markDelivered` et `DeliveryFailureServiceImpl.reportFailure` (`UPDATE`), `DeliveryUndoServiceImpl` (`CANCEL`), toutes sur la ressource `DELIVERY_ORDER`.

### Frontend

Aucun changement : la frise du panneau de détail (`components/delivery-detail/delivery-detail.html`) affiche déjà « heure · auteur » et le libellé de chaque étape.

### Données

Aucune table ni migration. Aucune ligne de journal nouvelle.

## Règles métier à respecter par les autres tickets

- Toute nouvelle transition de statut d'une commande doit écrire sa ligne de journal sur `DELIVERY_ORDER` (sans nom ni téléphone du client) : c'est ce qui la rend visible, horodatée et attribuée dans l'historique.
- L'auteur est l'utilisateur authentifié au moment de l'opération (SYS-06) ; hors requête, « Système ».
- Un refus (409, 404, 422) n'écrit aucune ligne.

## Fichiers concernés

- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/delivery/controller/DeliveryStatusHistoryIT.java` (nouveau, 3 tests)
- `docs/ticket/LIV-07/scenario-test.md`, `docs/ticket/LIV-07/guide.md`

## Dépendances

**Ce ticket s'appuie sur** :
- [LIV-03](../LIV-03/guide.md), [LIV-04](../LIV-04/guide.md), [LIV-05](../LIV-05/guide.md) : les transitions et leurs lignes de journal.
- [LIV-06](../LIV-06/guide.md) : l'endpoint et l'écran d'historique.
- [SYS-06](../SYS-06/guide.md) : auteur des opérations en UUID.

**Ces tickets s'appuient sur lui** :
- _(rien)_

## Comment l'utiliser depuis un autre ticket

Rien à appeler : lire `GET /api/v1/deliveries/{id}/history` (ou `DeliveryService.history(id)` côté front).

## Points d'attention

- **Hypothèse prise (session de nuit, sans validation)** : « changement de statut » = ligne de journal de la commande ; pas de table structurée « ancien statut → nouveau statut ». Le libellé porte déjà le nouveau statut (et « de X à Y » pour une annulation). Si Randy veut des colonnes structurées ou un filtre par statut dans l'historique, ce serait une table dédiée dans `feature/delivery` (piste déjà notée dans le guide de LIV-06), à chiffrer à part.
- Le nom de l'auteur est celui au moment de l'opération (copié dans le journal) : un renommage ultérieur ne le réécrit pas.
- Plus de 100 étapes pour une commande : seule la première page est lue par l'écran.
- Les cases de `description.md` ne sont pas cochées tant que les tests n'ont pas tourné.
