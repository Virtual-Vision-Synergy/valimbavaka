# Guide d'intégration — LIV-04 · Signalement d'un échec de livraison

Statut : ✅ rédigé ; tests à lancer (CI ou local), voir le [scénario](scenario-test.md#résultat-de-lexécution) · Ticket : [#49](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/49) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Sur `/round`, le livreur touche « Échec », choisit un **motif obligatoire** (client absent, adresse introuvable, colis refusé, autre) et peut ajouter un commentaire. Après le délai d'annulation de 5 s (LIV-02), l'échec part vers `POST /api/v1/deliveries/{id}/failure` par la file hors ligne : le serveur passe la commande à `FAILED` (`DeliveryOrder.moveTo`) et enregistre, dans la même transaction, un **retour de stock** TRF-03 (`RET-…`) de l'emplacement Livraison vers l'Entrepôt pour chaque ligne. Une livraison en échec ne crée **ni vente ni facture** (décision PO du 07/11/2026 : seule « livrée », LIV-03, le fait).

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée (`XxxWDto`) | Sortie (`XxxRDto`) | Erreurs |
|---|---|---|---|---|---|
| POST | `/api/v1/deliveries/{id}/failure` (`DeliveryFailureController.reportFailure`) | `deliveries.fail` (administrateur, livreur) | `DeliveryFailureWDto` : `reason` (`ABSENT`, `ADDRESS_NOT_FOUND`, `REFUSED`, `OTHER`, obligatoire), `note` (facultatif, ≤ 280 caractères, espaces retirés, vide → `null`) | 200 `DeliveryOrderRDto` (`status` `FAILED`, `failureReason`, `failureNote`, `returnTransferId`) | 400 motif inconnu, 401, 403, 404 commande inconnue **ou d'un autre livreur**, 409 commande déjà livrée / en échec / annulée ou articles absents de la Livraison, 422 motif manquant, commentaire trop long, emplacement désactivé |

- `Idempotency-Key` (SYS-02) : un échec rejoué par la file hors ligne ne crée qu'un seul retour.
- `DeliveryOrderRDto` porte désormais `failureReason`, `failureNote` et `returnTransferId` (`null` hors échec) pour toutes les lectures de LIV-01/LIV-02.

### Services et ports Java réutilisables

- `DeliveryFailureService.reportFailure(UUID orderId, DeliveryFailureWDto)` (`feature/delivery/service/DeliveryFailureService.java`) : 404 si la commande n'est pas visible par l'appelant (livreur : seulement les siennes, via `UserService.isActiveDriver`), 409 **avant** tout mouvement de stock si la transition est interdite (`DeliveryStatus.ensureCanMoveTo`).
- `DeliveryOrder.fail(DeliveryFailure, UUID returnTransferId)` (`domain/DeliveryOrder.java:118`) : appelle `moveTo(FAILED)` puis enregistre motif, commentaire et retour. Ne jamais passer une commande à `FAILED` par `moveTo` seul : la contrainte `ck_delivery_orders_failed_has_reason` le refuserait en base.
- `DeliveryReturn.record(DeliveryOrder)` (package-private, `service/impl/DeliveryReturn.java`) : `TransferService.record` avec `TransferOrigin.RETURN`, de `DELIVERY` vers `WAREHOUSE`, mêmes articles et quantités.
- `DeliveryFailureReason.label()` : libellé français utilisé dans le journal (« client absent », « refusée par le client »…).

### Frontend

- Route : `/round` (inchangée, LIV-02). Le bouton « Échec » ouvre la feuille de motif (`driver-round-page.ts:125` `openFailure`, `:135` `confirmFailure`) ; « Enregistrer l'échec » applique la saisie localement puis l'envoie après `UNDO_DELAY_MS` ; un refus du serveur affiche le toast `deliveries.driver.refusedToast` (ton `danger`) et relit la tournée (`onRefused`).
- `data-access/delivery-service.ts:89` `reportFailure(id, DeliveryFailureWDto)` : `OutboxService.submit('POST', …/failure, body)` (résultat `queued` hors ligne).
- `data-access/delivery-service.ts:101` `updateStatus(id, change)` : `FAILED` part vers l'API ; `DELIVERED` reste simulé jusqu'à LIV-03.
- `data-access/delivery-models.ts:17` `FailureReason`, `:79` `DeliveryFailureWDto` ; `DeliveryOrderRDto.failureReason` / `failureNote`. `delivery-mapping.ts:50` reporte le motif sur la carte du tableau (colonne « Échecs »).
- Traductions : `deliveries.reason.*` (« Colis refusé »…) et `deliveries.driver.refusedToast` (`public/i18n/fr.json`, `mg.json`).

### Données

- Migration `2026_11_08-65-changelog.sql` : colonnes `failure_reason` (CHECK sur les 4 motifs), `failure_note` (280), `return_transfer_id` (FK `transfers`) sur `delivery_orders` ; contrainte « `FAILED` ⇒ motif et retour renseignés » ; permission `deliveries.fail` donnée à `ADMIN` et `DELIVERY_DRIVER`. Rollback fourni.
- Journal : `UPDATE` · `DELIVERY_ORDER` « Commande LIV-0042 en échec : client absent, 1 article(s) remis en stock » (**sans** le commentaire du livreur, qui peut nommer le client) ; le retour a sa propre ligne `CREATE` · `TRANSFER` écrite par TRF-03.

## Règles métier à respecter par les autres tickets

- `FAILED` est final (comme `DELIVERED` et `CANCELLED`) : seule l'annulation limitée dans le temps de LIV-05 pourra revenir en arrière, et elle devra **annuler le retour** `returnTransferId` (TRF-03, opération réversible) en même temps.
- Une commande en échec ne crée **ni vente, ni facture, ni mouvement `SALE`/`DELIVERY`** : uniquement deux mouvements `TRANSFER` (sortie Livraison, entrée Entrepôt).
- Les articles doivent être dans l'emplacement Livraison (transferts du matin, JR-01) : sinon 409, la commande reste à faire. JR-04 (clôture de journée) peut compter les échecs par `status = FAILED` et retrouver le retour par `returnTransferId`.
- Droits : le livreur ne signale que ses commandes (404 sinon, on ne révèle pas qu'elles existent) ; l'administrateur peut signaler l'échec de toute commande ; la vendeuse n'a pas `deliveries.fail`.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/delivery/`)
- `controller/DeliveryFailureController.java`
- `domain/DeliveryFailure.java`, `domain/DeliveryFailureReason.java`, `domain/DeliveryOrder.java` (`fail`, colonnes)
- `dto/DeliveryFailureWDto.java`, `dto/DeliveryOrderRDto.java`
- `service/DeliveryFailureService.java`, `service/impl/DeliveryFailureServiceImpl.java`, `service/impl/DeliveryReturn.java`
- `src/main/resources/db/changelog/changes/2026_11_08-65-changelog.sql` (+ `db.changelog-master.yaml`)

**Frontend** (`valimbavaka-ui/src/app/features/deliveries/`)
- `data-access/delivery-service.ts`, `delivery-models.ts`, `delivery-mapping.ts`
- `pages/driver-round-page/driver-round-page.ts`
- `valimbavaka-ui/public/i18n/fr.json`, `mg.json` ; `core/auth/auth-testing.ts` (permission `deliveries.fail` du livreur de test)

**Tests** : `DeliveryFailureIT`, `DeliveryFailureServiceImplTest`, `DeliveryReturnTest`, `DeliveryOrderTest` ; `delivery-service.spec.ts`, `delivery-mapping.spec.ts`, `driver-round-page.spec.ts` ; voir le [scénario](scenario-test.md).

## Dépendances

**Ce ticket s'appuie sur** :
- [LIV-02](../LIV-02/description.md) · [guide](../LIV-02/guide.md) (et LIV-01 : commandes, `DeliveryOrder.moveTo`)
- [JNL-01](../JNL-01/description.md) · [guide](../JNL-01/guide.md)
- TRF-03 (retour de stock `RET-…`), STK-01, SYS-02 / SYS-07 (idempotence, file hors ligne)

**Ces tickets s'appuient sur lui** :
- [LIV-05](../LIV-05/description.md) · [guide](../LIV-05/guide.md)
- [JR-04](../JR-04/description.md) · [guide](../JR-04/guide.md)

## Comment l'utiliser depuis un autre ticket

```text
POST /api/v1/deliveries/{id}/failure        (Idempotency-Key conseillé)
{"reason": "ABSENT", "note": "Portail fermé"}
→ 200 DeliveryOrderRDto { status: "FAILED", failureReason: "ABSENT", returnTransferId: "<id du RET-…>" }

Java : deliveryFailureService.reportFailure(orderId, new DeliveryFailureWDto(DeliveryFailureReason.ABSENT, null));
Front : deliveryService.reportFailure(id, { reason: 'ABSENT', note: null })
```

## Points d'attention

- **Décision PO du 07/11/2026** (LIV-01) : la vente et la facture naissent seulement quand la livraison est marquée livrée (LIV-03) ; l'échec ne touche qu'au stock.
- Hypothèse : les articles reviennent à l'**Entrepôt** (`WAREHOUSE`), pas dans un emplacement « retour » dédié ; le bon `RET-…` (statut `RETURN`) permet de les isoler si le client veut un contrôle avant remise en vente.
- Hypothèse : le motif « Autre » n'exige pas de commentaire ; le front envoie `OTHER` si aucun motif n'est transmis (`delivery-service.ts:103`).
- Concurrence (deux téléphones) : verrou optimiste `version` puis 409 ; non automatisé, à vérifier en recette (R4).
- Tests d'intégration : le stock en Livraison est posé par un mouvement `RECEIPT` direct, en attendant les transferts du matin de JR-01.
