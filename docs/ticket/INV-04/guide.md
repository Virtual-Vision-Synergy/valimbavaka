# Guide d'intégration — INV-04 · Clôture d'une session d'inventaire

Statut : ✅ écrit le 2026-11-08 — tests exécutés en CI (voir « Résultat de l'exécution » du scénario) · Ticket : [#80](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/80) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

L'administrateur clôture une session d'inventaire dont toutes les lignes sont comptées et tous les écarts expliqués. Le serveur ajuste alors, **dans une seule transaction**, le stock de l'emplacement aux quantités comptées : un mouvement `INVENTORY` signé (compté − stock actuel) par article, référencé sur la session, puis la session passe `CLOSED` et la clôture est tracée au journal. La page `/inventory` n'est plus simulée pour la clôture.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| POST | `/api/v1/inventories/{id}/close` | `inventory.close` (administrateur) | aucune (un corps envoyé est ignoré) | 200 `InventorySessionRDto` (contrat d'INV-01) avec `status` `CLOSED`, `closedAt` | 401, 403, 404 (session inconnue), 409 (session plus `IN_PROGRESS`, ou stock devenu insuffisant pendant la clôture), 422 (ligne non comptée, écart sans motif, emplacement désactivé) |

Messages 422 : « Clôture impossible : article(s) non compté(s) ART-0031, ART-0032 (2). » / « Clôture impossible : écart(s) sans motif ART-0031 (1). » (5 codes au plus, puis « … » et le total).

### Services et ports Java réutilisables

| Port | Méthode | Rôle |
|---|---|---|
| `feature.inventory.service.InventoryClosingService` | `InventorySessionRDto closeSession(UUID sessionId)` | Clôture (`InventoryClosingServiceImpl.java:69`) : verrou de la session, contrôles (`checkReady`, l. 94), ajustements (`adjustStock`, l. 120), `CLOSED`, journal. |
| `feature.inventory.domain.InventorySession` | `void close(Instant at)` (`InventorySession.java:77`) | Seule façon de passer `CLOSED` et de renseigner `closed_at` ; l'appelant vérifie `IN_PROGRESS`. |
| `feature.inventory.repository.InventorySessionRepository` | `findByIdForUpdate(UUID)` (l. 29, `PESSIMISTIC_WRITE`), `saveAndFlush` (l. 24) | Verrou de la session pendant la clôture ; écriture immédiate pour renvoyer la nouvelle `version`. |
| `feature.inventory.repository.InventoryLineRepository` | `findBySessionIdOrderByArticleCode(UUID)` (l. 23) | Toutes les lignes d'une session, par code article (utile à INV-05). |

Contrôleur : `feature/inventory/controller/InventoryClosingController.java` (même préfixe `/api/v1/inventories`).

### Frontend (`features/inventory`)

- `data-access/inventory-service.ts` : `close(sessionId)` (l. 102), `POST …/{id}/close` via `OutboxService` (file hors ligne, `Idempotency-Key`), renvoie `SubmitResult<InventorySessionRDto>`.
- `pages/inventory-page` : `confirmClose` (`inventory-page.ts:177`) ; succès → `closedAt` (heure locale de `closedAt` serveur), badge « Clôturée », champs désactivés, toast `inventory.closedToast` ; en file → toast `common.queuedSaved`, feuille laissée ouverte ; refus → toast `danger` `inventory.closeFailed`. Template inchangé.
- `data-access/inventory-models.ts` : `InventoryCloseWDto` (DTO de démonstration) **supprimé** ; `InventoryState.toRequest` supprimé.
- Traductions : `inventory.closedToast` réécrite (plus de « démonstration »), `inventory.closeFailed` ajoutée (fr, mg).

### Données

- Migration `2026_11_08-82-changelog.sql` (1 changeset, rollback fourni) : permission `inventory.close` accordée à `ADMIN`. Aucune table : `inventory_sessions.status` / `closed_at` existaient (INV-01).
- Mouvements : `stock_movements` de type `INVENTORY`, quantité signée, `reference_id` = id de la session (STK-01). Une ligne dont le compté égale le stock actuel ne crée aucun mouvement.
- Journal : `UPDATE` · `INVENTORY_SESSION` · id de la session · « Clôture de la session d'inventaire INV-0012 · Boutique : 12 article(s), 3 ajustement(s) de stock » ; plus, par mouvement, la ligne `CREATE` · `STOCK_MOVEMENT` écrite par `StockMovementService.record`.

## Règles métier pour les autres tickets

- **Préalables à la clôture** (serveur, comme `InventoryState.canClose` à l'écran) : toutes les lignes comptées ; toute ligne `hasGap()` a un motif (recommandation d'INV-03).
- **Ajustement = compté − stock actuel de l'emplacement** au moment de la clôture, pas l'écart figé au comptage : le stock système égale toujours le compté (critère). L'écart affiché (compté − théorique figé) peut donc différer du mouvement si une vente a eu lieu entre comptage et clôture.
- **Tout ou rien** : un échec d'un mouvement (emplacement désactivé : 422 ; stock devenu négatif par une vente concurrente : 409) annule la clôture entière.
- **Session `CLOSED` figée** : comptage et motif refusés en 409 (INV-02, INV-03) ; une nouvelle clôture → 409. Ni la session ni les mouvements ne se suppriment (JNL-02) ; corriger un ajustement erroné passe par un nouveau mouvement, pas par une réouverture.
- **Historique (INV-05)** : sessions closes via `GET /api/v1/inventories?status=CLOSED` (tri `createdAt`) ; `closedAt` renseigné ; les ajustements d'une session se retrouvent par `stock_movements.reference_id`.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/inventory/`)
- `controller/InventoryClosingController.java` (nouveau)
- `service/InventoryClosingService.java`, `service/impl/InventoryClosingServiceImpl.java` (nouveaux)
- `domain/InventorySession.java` (`close`)
- `repository/InventorySessionRepository.java` (`findByIdForUpdate`, `saveAndFlush`), `repository/InventoryLineRepository.java` (`findBySessionIdOrderByArticleCode`)

**Migrations Liquibase**
- `valimbavaka-core/src/main/resources/db/changelog/changes/2026_11_08-82-changelog.sql`, déclarée dans `db.changelog-master.yaml`

**Frontend** (`valimbavaka-ui/src/app/features/inventory/`)
- `data-access/inventory-service.ts`, `inventory-models.ts`, `inventory-state.ts`
- `pages/inventory-page/inventory-page.ts` ; `inventory.routes.ts` (commentaire)
- `valimbavaka-ui/public/i18n/fr.json`, `mg.json`

**Tests**
- Backend : `feature/inventory/service/impl/InventoryClosingServiceImplTest`, `feature/inventory/controller/InventoryClosingIT`
- Frontend : `pages/inventory-page/inventory-page.spec.ts` (bloc « clôture (INV-04) »), `data-access/inventory-service.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** :
- [INV-01](../INV-01/guide.md) (sessions, règle d'ajustement), [INV-02](../INV-02/guide.md) (comptage, 409 sur session close), [INV-03](../INV-03/guide.md) (`hasGap`, motifs), [STK-01](../STK-01/guide.md) (`StockMovementService.record`, type `INVENTORY`), [JNL-01](../JNL-01/guide.md) (`JournalService.record`), [JNL-02](../JNL-02/guide.md) (`OperationRepository`, aucune suppression), [SYS-07](../SYS-07/guide.md) (file hors ligne).

**Ces tickets s'appuient sur lui** :
- [INV-05](../INV-05/description.md) · Historique des sessions d'inventaire.

## Comment l'utiliser depuis un autre ticket

```ts
inventoryService.close(session.id).subscribe((result) => {
  if (result.body) {
    console.log(result.body.status, result.body.closedAt); // 'CLOSED', '2026-11-08T14:30:00Z'
  }
});
```

Retrouver les ajustements d'une session (INV-05, SQL) : `select * from stock_movements where type = 'INVENTORY' and reference_id = :sessionId`.

## Points d'attention

- **Hypothèse : clôture refusée (422) tant qu'une ligne n'est pas comptée**, plutôt que de laisser le stock des articles non comptés inchangé ; c'est la règle de l'écran (`canClose`) et la seule qui garantit le critère pour tout le périmètre.
- **Hypothèse : motif exigé côté serveur pour chaque ligne `hasGap()`** (recommandation d'INV-03).
- **Hypothèse : permission dédiée `inventory.close`** (administrateur), distincte de `inventory.update`, parce que la clôture modifie le stock. L'écran n'interroge pas la permission : le bouton reste visible, le serveur décide (403).
- **Hypothèse : action journalisée en `UPDATE`** (changement de statut) ; `JournalAction` n'a pas de valeur « clôture ».
- **Ajustement sur le stock actuel** : une vente enregistrée entre le comptage et la clôture est « absorbée » (le stock final vaut le compté, pas compté − vente). C'est la règle fixée par INV-01 et voulue par le critère ; si le PO préfère l'écart figé (compté − théorique figé), changer le calcul de `adjustStock`. Le blocage des ventes pendant le comptage (mention de la maquette) n'est toujours pas implémenté.
- **Course faible** : le stock actuel est lu avant le verrou par article de `StockMovementPolicy` ; une vente validée exactement pendant la clôture peut laisser un léger écart, ou faire échouer la clôture en 409 si le stock deviendrait négatif (rien n'est alors écrit).
- **Concurrence** : deux clôtures simultanées sont sérialisées par le verrou `PESSIMISTIC_WRITE` (la seconde reçoit 409) ; pas de test concurrent automatisé. Un comptage envoyé pendant la clôture n'est pas bloqué par ce verrou (les saisies d'INV-02/03 ne verrouillent pas la session).
- **Hors ligne** : une clôture mise en file laisse la feuille modifiable ; des saisies faites ensuite partent après la clôture et seront refusées en 409.
- **Pas de réouverture ni d'annulation** d'une session close (JNL-02) ; un ajustement erroné se corrige par un nouveau mouvement de stock.
- Le bandeau « en construction » de `/inventory` reste affiché jusqu'à INV-05 (liste « Sessions précédentes » encore vide).
- Traductions malgaches `inventory.closedToast` / `inventory.closeFailed` rédigées sans relecture native.
- Branche créée sur `feature/INV-03-motif-ecart-ligne`, puis `develop` fusionné une fois INV-01 à INV-03 mergés : PR vers `develop`.
