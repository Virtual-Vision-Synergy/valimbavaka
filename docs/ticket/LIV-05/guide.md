# Guide d'intégration — LIV-05 · Annulation d'une action récente

Statut : ✅ rédigé, tests à lancer (CI ou local) · Ticket : [#50](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/50) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Le livreur a marqué une livraison « livrée » ou « échec » par erreur : sur `/round/history`, le bouton « Annuler » apparaît pendant **5 minutes** sur les arrêts terminés. `POST /api/v1/deliveries/{id}/undo` remet la commande dans le statut d'avant (« à préparer » ou « en tournée ») et défait ses effets dans la même transaction : une commande livrée **annule sa vente** (décision PO du 07/11/2026 : livrée = vente) et remet les articles dans Livraison ; une commande en échec **défait son retour de stock** (les articles repartent de l'Entrepôt vers Livraison). L'annulation est tracée au journal.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie (`XxxRDto`) | Erreurs |
|---|---|---|---|---|---|
| POST | `/api/v1/deliveries/{id}/undo` (`DeliveryUndoController.undoLastAction`) | `deliveries.undo` (administrateur, livreur) | aucune (un corps envoyé est ignoré) | 200 `DeliveryOrderRDto` (statut d'avant, `saleId`, `failureReason`, `returnTransferId` vides, `previousStatus` vide) | 401, 403, 404 commande inconnue **ou d'un autre livreur**, 409 rien à annuler (à préparer, en tournée, annulée, déjà annulée, délai de 5 min dépassé), journée de la vente clôturée, articles de l'échec repartis de l'Entrepôt |

- Rejouable avec `Idempotency-Key` (SYS-02) : la file hors ligne ne défait qu'une fois.
- `DeliveryOrderRDto` porte deux champs de plus pour toutes les lectures de LIV-01 à LIV-06 : `previousStatus` (statut avant la dernière action livrée ou échec, `null` sinon) et `updatedAt` (dernière modification, début du délai).

### Services et ports Java réutilisables

- `DeliveryUndoService.undoLastAction(UUID)` (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/delivery/service/DeliveryUndoService.java`) ; délai `DeliveryUndoServiceImpl.UNDO_WINDOW` (5 minutes, `service/impl/DeliveryUndoServiceImpl.java:30`), horloge `Clock` injectée.
- `DeliveryOrder.ensureCanUndo(Instant, Duration)` et `DeliveryOrder.undo(Instant, Duration)` (`domain/DeliveryOrder.java:139`, `:147`) : 409 si le statut n'est ni `DELIVERED` ni `FAILED`, si `previousStatus` est vide ou si `updatedAt` date de plus de la fenêtre ; `undo` efface aussi `saleId`, motif, commentaire et `returnTransferId` (la contrainte `ck_delivery_orders_delivered_has_sale` n'est plus en jeu une fois le statut changé). `DeliveryOrder.moveTo` retient désormais `previousStatus`.
- `DeliveryStatus.isUndoable()` (`domain/DeliveryStatus.java`).
- **Nouveau port de la vente** : `SaleService.cancel(UUID saleId, String reason)` (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/sale/service/SaleService.java`, `SaleServiceImpl.cancel`) : annulation à chaud JNL-02 (`Sale.cancel()` → `CANCELLED`), mouvements de stock `RETURN` positifs dans l'emplacement du canal référençant la vente (`SaleLedger.unpost`), ligne de journal `CANCEL` · `SALE`, événement `SaleCancelledEvent` (pour FAC-04 et COM-02, comme `SaleRecordedEvent`). 404 vente inconnue ; 409 si elle n'est plus `VALIDATED` ou si sa journée n'est pas la journée ouverte. À appeler dans une transaction, sans contrôle de droit (comme `record`).
- `DeliveryReturn.undoReturn(DeliveryOrder)` (`service/impl/DeliveryReturn.java:46`, interne au paquet) : `TransferService.record` d'origine `MANUAL` de `WAREHOUSE` vers `DELIVERY`, mêmes lignes, note « Annulation du retour de la livraison LIV-… ». Le `RET-…` d'origine reste en base (un transfert enregistré ne se supprime pas) ; le transfert inverse le compense.

### Frontend (`valimbavaka-ui/src/app/features/deliveries/`)

- `pages/driver-history-page` : bouton « Annuler » (`aria-label` « Annuler l'action sur <client> ») sur les arrêts livrés ou en échec depuis moins de 5 minutes ; succès → toast `deliveries.driver.undoneToast` puis tournée relue ; refus du serveur → toast `deliveries.driver.undoRefusedToast` (ton `danger`) puis tournée relue. L'horloge est relue toutes les 30 s pour retirer le bouton passé le délai.
- `data-access/delivery-service.ts:127` `undoLastAction(id)` : `OutboxService.submit('POST', …/undo)`, sans corps (`queued` hors ligne).
- `data-access/delivery-mapping.ts:96` `UNDO_WINDOW_MS` et `isUndoable(delivery, now)` (indicatif : le serveur reste juge) ; `DeliveryRDto.changedAt` (reprend `updatedAt`) ; `delivery-models.ts` : `previousStatus` et `updatedAt` dans `DeliveryOrderRDto`.
- Traductions `deliveries.driver.undoLast`, `undoneToast`, `undoRefusedToast` (`public/i18n/fr.json`, `mg.json`).

### Données

- Migration `2026_11_11-01-changelog.sql` : colonne `delivery_orders.previous_status` (CHECK `TO_PREPARE` / `ON_ROUND`) et permission `deliveries.undo` donnée à `ADMIN` et `DELIVERY_DRIVER`. Rollback fourni. Les commandes déjà livrées ou en échec avant la migration n'ont pas de `previous_status` : elles ne sont pas annulables.
- Journal : `CANCEL` · `DELIVERY_ORDER` « Annulation de l'action sur la commande LIV-0042 : de « livrée » à « à préparer » » (ni nom ni téléphone du client, visible dans l'historique LIV-06) ; `CANCEL` · `SALE` « Annulation de la vente VTE-0007 : annulation de la livraison LIV-0042 » ; pour un échec, ligne `CREATE` · `TRANSFER` du transfert inverse (TRF-03).

## Règles métier à respecter par les autres tickets

- **Une seule action annulable : la dernière**, livrée ou échec, **5 minutes** après la dernière modification de la commande (`updatedAt`). Aucune autre opération ne modifie une commande aujourd'hui : si un ticket ajoute une modification (édition, réaffectation), il devra soit ne pas toucher `updatedAt`, soit accepter qu'elle ouvre une nouvelle fenêtre (voir Points d'attention).
- Annuler « livrée » = **annuler la vente** (statut `CANCELLED`, jamais supprimée) : les rapports (RPT-*, DSH-*) doivent exclure les ventes non `VALIDATED`. FAC-* écoute `SaleCancelledEvent` pour annuler ou compenser la facture (FAC-04) ; COM-02 pour retirer la commission.
- Annuler « échec » ne supprime pas le `RET-…` : il est compensé par un transfert `MANUAL` inverse ; JR-04 compte les retours par `status = FAILED`, pas par les `RET-…` : une commande annulée n'est plus `FAILED`.
- Après annulation, la commande peut être relivrée ou ré-échouée : une **nouvelle** vente ou un nouveau retour est créé (`uq_delivery_orders_sale` : l'ancienne vente n'est plus liée).
- Le livreur n'annule que ses commandes (404 sinon) ; l'administrateur toutes ; la vendeuse n'a pas `deliveries.undo`.
- Le serveur décide du délai : le bouton de l'écran n'est qu'une aide.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/`)
- `delivery/controller/DeliveryUndoController.java`
- `delivery/service/DeliveryUndoService.java`, `delivery/service/impl/DeliveryUndoServiceImpl.java`, `delivery/service/impl/DeliveryReturn.java` (`undoReturn`)
- `delivery/domain/DeliveryOrder.java`, `delivery/domain/DeliveryStatus.java`, `delivery/dto/DeliveryOrderRDto.java`
- `sale/service/SaleService.java`, `sale/service/impl/SaleServiceImpl.java` (`cancel`), `sale/service/impl/SaleLedger.java` (`unpost`), `sale/domain/Sale.java` (`cancel`), `sale/domain/SaleCancelledEvent.java`
- `valimbavaka-core/src/main/resources/db/changelog/changes/2026_11_11-01-changelog.sql` (+ `db.changelog-master.yaml`)

**Frontend** (`valimbavaka-ui/src/app/features/deliveries/`)
- `pages/driver-history-page/driver-history-page.ts`, `.html`, `.css`
- `data-access/delivery-service.ts`, `delivery-mapping.ts`, `delivery-models.ts`
- `valimbavaka-ui/public/i18n/fr.json`, `mg.json`

**Tests** : `DeliveryUndoIT`, `DeliveryUndoServiceImplTest`, `DeliveryOrderTest`, `DeliveryStatusTest`, `DeliveryReturnTest`, `SaleServiceImplTest`, `SaleLedgerTest`, `SaleTest` ; `driver-history-page.spec.ts`, `delivery-service.spec.ts`, `delivery-mapping.spec.ts` (et les fabriques de commandes de `driver-round-page.spec.ts` et `deliveries-page.spec.ts`) ; voir le [scénario](scenario-test.md#tests-existants).

## Dépendances

**Ce ticket s'appuie sur** :
- [LIV-03](../LIV-03/guide.md) : la vente liée (`saleId`), `DeliveryOrder.deliver`.
- [LIV-04](../LIV-04/guide.md) : le retour de stock (`returnTransferId`), `DeliveryOrder.fail`, `DeliveryReturn`.
- [LIV-06](../LIV-06/guide.md) : l'historique lit les lignes `CANCEL` écrites ici, sans rien brancher de plus.
- [LIV-02](../LIV-02/guide.md) (écran `/round/history`), [JNL-01](../JNL-01/description.md) (journal), VTE-01 (ventes), TRF-03 (transferts), STK-01 (mouvements), SYS-02 / SYS-07 (idempotence, file hors ligne).

**Aucun ticket ne dépend de celui-ci** ; FAC-04 et COM-02 pourront écouter `SaleCancelledEvent`.

## Comment l'utiliser depuis un autre ticket

```text
POST /api/v1/deliveries/{id}/undo        (Idempotency-Key conseillé, pas de corps)
→ 200 DeliveryOrderRDto { status: "TO_PREPARE", saleId: null, previousStatus: null }

Java : deliveryUndoService.undoLastAction(orderId);
       saleService.cancel(saleId, "annulation de la livraison LIV-0042");   // annulation d'une vente
Front : deliveryService.undoLastAction(id)
```

## Points d'attention

- **Hypothèses prises (session de nuit, sans validation PO)** :
  - « délai limité » = **5 minutes** (constante `UNDO_WINDOW` côté serveur, `UNDO_WINDOW_MS` côté écran) ; à confirmer par le PO ;
  - seules « livrée » et « échec » sont annulables (les seules actions de statut existantes) ; une action annulée ne peut pas être « rétablie », le livreur la ressaisit ;
  - défaire un échec crée un **transfert inverse** plutôt que de supprimer ou d'annuler le `RET-…` : `TransferStatus` n'a pas d'état « annulé » et un transfert enregistré ne se supprime pas ; l'historique des transferts montre donc le retour et son compensatoire ;
  - le contre-mouvement de stock d'une vente annulée est un mouvement `RETURN` (le type `SALE` n'accepte que des sorties) ;
  - annuler une livraison n'est possible que tant que **la journée de la vente est la journée ouverte** (sinon 409) : on ne touche pas à une journée clôturée (JR-04).
- Le début du délai est `updated_at` de la commande (audit JPA) : c'est exact tant qu'aucune autre opération ne modifie une commande. Si un ticket ajoute une édition de commande, ajouter une colonne `status_changed_at` pour ne pas rouvrir la fenêtre.
- Écran : le bouton est sur l'historique du jour (`/round/history`), pas sur la carte de `/round` (qui garde son annulation locale de 5 s de LIV-02, avant envoi). Le panneau de l'administrateur (`delivery-detail`) n'a pas de bouton d'annulation : l'API est ouverte à l'administrateur, l'écran reste à faire si le PO le demande.
- Hors ligne : une annulation mise en file part après la saisie qu'elle défait (ordre strict, SYS-10) ; si les 5 minutes sont passées au retour du réseau, le serveur répond 409 et le livreur voit « annulation refusée ».
- Les factures (FAC-01) ne sont pas encore générées dans le code : `SaleCancelledEvent` est publié mais personne ne l'écoute (FAC-04 / COM-02).
- Concurrence (deux téléphones) : verrou optimiste `version` puis 409 ; non automatisé, à vérifier en recette (R6).
