# Guide d'intégration — LIV-08 · Fonctionnement hors ligne de la tournée

Statut : ✍ rédigé le 2026-11-11, tests à lancer · Ticket : [#53](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/53) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

L'envoi hors ligne de « Livrée » (LIV-03), « Échec » (LIV-04) et « Annuler » (LIV-05) était **déjà** branché sur la file
SYS-07/SYS-02 (`DeliveryService.markDelivered`, `reportFailure`, `undoLastAction` via `OutboxService.submit`) et classé
`ORDERED` par SYS-10 (`core/offline/sync-order.ts:44`). Ce ticket ne livre que l'écart : la tournée relue reflète les
saisies encore en file, elle se relit seule quand la file se vide, et une saisie en délai d'annulation part si l'onglet
se ferme.

## Contrat exposé

### Endpoints backend

Aucun (ticket frontend seul).

### Frontend

- `data-access/delivery-queue.ts` `withQueuedActions(stops, queued)` : rejoue sur les arrêts les saisies en file
  `POST /api/v1/deliveries/{id}/deliver|failure|undo` (dans l'ordre ; `undo` rend le statut du serveur).
- `data-access/delivery-service.ts:74-80` : `myRound` applique `withQueuedActions` avec `OutboxService.pending()` (la
  page historique LIV-05 en profite aussi).
- `pages/driver-round-page/driver-round-page.ts` : `reloadWhenQueueDrained` (effet sur `pendingCount`, relecture une
  fois quand la file passe de > 0 à 0 et qu'aucune saisie n'est en délai d'annulation) ; `host` `(window:pagehide)` →
  `commit()`.

## Règles métier à respecter par les autres tickets

- Toute nouvelle action de tournée en file doit être ajoutée à `ACTION_URL` de `delivery-queue.ts` pour rester visible
  après relecture.
- Le préfixe `/api/v1/deliveries` reste `ORDERED` (SYS-10) : une action de livraison ne double jamais une saisie plus
  ancienne bloquée.

## Fichiers concernés

`delivery-queue.ts`, `delivery-service.ts`, `driver-round-page.ts` ; tests `delivery-queue.spec.ts`,
`delivery-service.spec.ts`, `driver-round-page.spec.ts` (tous sous `valimbavaka-ui/src/app/features/deliveries/`).

## Dépendances

**Ce ticket s'appuie sur** : [LIV-03](../LIV-03/guide.md), [LIV-04](../LIV-04/guide.md), [LIV-05](../LIV-05/guide.md),
[SYS-02](../SYS-02/guide.md), SYS-07, SYS-10 (décision PO du 07/11/2026 : ordre strict par type).
**Ces tickets s'appuient sur lui** : aucun.

## Comment l'utiliser depuis un autre ticket

Rien à appeler : `DeliveryService.myRound()` renvoie déjà la tournée avec les saisies en file.

## Points d'attention

- **Hypothèse** : le critère est jugé couvert pour l'envoi (SYS-02/07/10 + LIV-03/04/05) ; seul l'affichage a été complété.
- **Hors périmètre, à décider** : ouvrir ou recharger `/round` sans réseau exige une application installable (service
  worker) et un cache de lecture ; il n'existe pas dans le dépôt. Un ticket dédié serait à ouvrir.
- Un arrêt dont l'action en file a été annulée (`undo`) reprend le statut du serveur lu, pas un statut déduit.
