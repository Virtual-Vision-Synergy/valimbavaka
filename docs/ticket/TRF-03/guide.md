# Guide d'intégration — TRF-03 · Enregistrement d'un retour de stock

Statut : ✅ écrit le 2026-11-08 — **tests à lancer** (voir « Résultat de l'exécution » du scénario) · Ticket : [#36](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/36) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Ce ticket pose le **modèle de transfert de stock** entre deux emplacements : un bon (`transfers`) avec numéro, origine, emplacements de départ et d'arrivée, lignes (article, quantité) et statut. Un retour de stock (ex. boutique → entrepôt) est un transfert d'origine `RETURN`, numéroté `RET-0001`…, qui porte le **statut dédié `RETURN`** ; tout autre transfert est `VALIDATED` dès son enregistrement. Le stock bouge par deux mouvements `TRANSFER` par ligne (STK-01), dans la même transaction que le bon : tout passe ou rien. Les autres tickets (JR-01, TRF-02) créent leurs transferts par le port Java `TransferService.record`, sans réécrire ces règles.

## Contrat exposé

### Endpoints backend (`/api/v1/transfers`)

| Méthode | URL | Permission (`@HasPermission`) | Entrée (`XxxWDto`) | Sortie (`XxxRDto`) | Erreurs |
|---|---|---|---|---|---|
| POST | `/api/v1/transfers/returns` | `transfers.return` (administrateur, vendeuse) | `StockReturnWDto` | 201 `TransferRDto`, en-tête `Location: /api/v1/transfers/{id}` | 401, 403, 404 (article inconnu), 409 (stock insuffisant à l'origine), 422 (voir règles) |

- `StockReturnWDto` : `fromLocationId`, `toLocationId` (UUID du référentiel SYS-05, obligatoires), `lines` (1 à 100 `TransferLineWDto` : `articleId`, `quantity` > 0, 9 chiffres et 3 décimales au maximum), `note` (≤ 500, facultative, vide → `null`). Aucun `id`, `number`, `origin` ni `status` : ils sont fixés par le serveur (champs glissés ignorés).
- `TransferRDto` : `id`, `number` (`RET-0001`, `TRF-0001`), `origin` (`MORNING`, `MANUAL`, `RETURN`), `status` (`VALIDATED`, `RETURN`), `fromLocationId`, `toLocationId`, `note`, `lines` (`TransferLineRDto` : `articleId`, `quantity`, dans l'ordre saisi), `createdAt` (date du transfert), `createdBy` (émetteur ; `null` hors requête authentifiée).
- Le `Location` vise `GET /api/v1/transfers/{id}`, **pas encore exposé** : il sera livré par TRF-04.
- Écritures rejouables : avec `Idempotency-Key` (file hors ligne SYS-07 / SYS-12), un rejeu renvoie la même réponse sans second retour.

### Services et ports Java réutilisables

| Port | Méthode | Rôle |
|---|---|---|
| `feature.transfer.service.TransferService` (`TransferService.java:19`) | `TransferRDto record(TransferCommand command)` | **Seule façon d'enregistrer un transfert** (retour, transfert manuel, transferts du matin). Contrôle les règles, attribue le numéro, enregistre le bon, déplace le stock et écrit le journal. À appeler depuis une méthode `@Transactional`. |
| idem (`TransferService.java:22`) | `TransferRDto recordReturn(StockReturnWDto request)` | Retour de stock (origine `RETURN`), utilisé par le contrôleur. |
| `feature.stock.service.StockMovementService` (`StockMovementService.java:27`, ajout) | `List<StockMovementRDto> recordAll(List<StockMovementCommand> commands)` | Enregistre les mouvements d'une même opération en prenant d'abord **tous les verrous article / emplacement dans un ordre stable** : deux transferts croisés ne s'interbloquent pas. À préférer à plusieurs `record` dès qu'une opération touche plusieurs couples article / emplacement (production, inventaire…). |

`TransferCommand(origin, fromLocationId, toLocationId, lines, note)` et `TransferLineCommand(articleId, quantity)` (`feature/transfer/domain`) : `quantity` toujours **positive** (elle sort de l'origine et entre dans la destination).

### Frontend (`features/transfers`)

- Route `/transfers/return` (garde `permissionGuard('transfers.return')`) : page `pages/stock-return-page` (formulaire de retour). Les permissions sont désormais posées **par route** dans `transfers.routes.ts` (la liste `/transfers` garde `transfers.read`) ; le garde global de `app.routes.ts` a été retiré pour que la vendeuse accède au retour sans voir l'historique.
- Accès : bouton « Retour de stock » de `/transfers` (administrateur) et de `/stocks` (si `transfers.return`, donc aussi la vendeuse). Après l'enregistrement : `/transfers` si l'utilisateur a `transfers.read`, sinon `/stocks`.
- `data-access/transfer-service.ts` : `recordReturn(body)` → `OutboxService.submit('POST', '/api/v1/transfers/returns', body)` (`SubmitResult<TransferRDto>`, `queued` si hors ligne). `list()` et `get()` restent des **données de démonstration** (TRF-01, TRF-04).
- `data-access/transfer-models.ts` : types du serveur `TransferRDto`, `TransferLineRDto`, `StockReturnWDto`, `TransferLineWDto`, `TransferOrigin`, `TransferStatus` (`VALIDATED` | `RETURN`). Les types de la maquette ont été renommés `TransferRowRDto`, `TransferRowStatus`, `TransferDetailLineRDto` pour libérer les noms du serveur.
- `features/stocks/data-access/stock-service.ts` : ajout de `quantities()` (stocks bruts par `locationId`, une page de 100 articles actifs) pour proposer les articles disponibles à l'origine.
- Traductions : `transfers.return.*` et `transfers.status.VALIDATED` (fr, mg).

### Données

- Migration `2026_11_08-21-changelog.sql` (4 changesets, rollback fourni) :
  - séquences `transfer_number_seq` (`TRF-…`) et `stock_return_number_seq` (`RET-…`) ;
  - table `transfers` : `number` unique, `origin` et `status` (CHECK), `from_location_id` / `to_location_id` (→ `stock_locations`, CHECK différents), `note`, champs d'audit et `version` (`BaseEntity`), index sur `created_at` (historique TRF-01) ;
  - table `transfer_lines` : (`transfer_id`, `line_number`) en clé, `article_id` (→ `articles`, unique par transfert), `quantity NUMERIC(12,3) > 0` ;
  - permission `transfers.return` donnée à `ADMIN` et `SELLER`.
- Mouvements : pour chaque ligne, `TRANSFER` −q sur l'origine puis +q sur la destination, `reference_id` = id du transfert (`TransferLedger.post`, `TransferLedger.java:34`).
- Journal : ressource `TRANSFER`, action `CREATE`, libellé `Retour de stock RET-0001 · Boutique → Entrepôt : 2 article(s), 5.5 unité(s)` (préfixe `Transfert du matin` / `Transfert` selon l'origine), en plus des lignes `STOCK_MOVEMENT` de STK-01.

## Règles métier à respecter par les autres tickets

- **Statut** : fixé par l'origine (`TransferOrigin.initialStatus`, `TransferOrigin.java:31`) : `RETURN` → `RETURN` ; `MORNING`, `MANUAL` → `VALIDATED`. Un transfert généré par l'ouverture de journée (TRF-02) apparaît donc directement `VALIDATED`. Ne pas poser le statut à la main.
- **Numérotation** : `RET-%04d` pour les retours, `TRF-%04d` pour les autres, une séquence par préfixe ; des trous sont possibles (transaction annulée), un numéro n'est jamais réutilisé.
- **Contrôles** (`TransferPolicy.check`, `TransferPolicy.java:39`), tous en 422 sauf mention : origine ≠ destination ; emplacements connus et **actifs** ; 1 à 100 lignes ; chaque article une seule fois ; quantité > 0 ; article inconnu → 404. Puis, par STK-01 : 3 décimales au maximum (422) et **stock suffisant à l'origine** (409, rien n'est écrit).
- **Ne jamais écrire dans `transfers` / `transfer_lines` ni dans `stock_movements` à la main** : passer par `TransferService.record`. Le repository (`TransferRepository`) hérite d'`OperationRepository` : pas de suppression (JNL-02).
- **Une ligne = un article** : JR-01 doit regrouper les quantités d'un même article avant d'appeler le port ; **un bon par destination** (critère de JR-01) = un appel à `record` par destination.
- Les emplacements se désignent par UUID ; le libellé n'est utilisé que pour le journal.
- Article **désactivé** : accepté par le serveur (un retour reste possible, STK-01) ; l'écran ne propose que les articles actifs.
- Droits : `transfers.return` (administrateur, vendeuse) pour saisir un retour ; `transfers.read` (administrateur) pour l'historique (TRF-01).

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/transfer/`)
- `controller/TransferController.java`
- `service/TransferService.java` ; `service/impl/TransferServiceImpl.java`, `TransferPolicy.java` (règles), `TransferLedger.java` (mouvements + journal)
- `domain/Transfer.java` (entité), `TransferLine.java` (`@Embeddable`), `TransferOrigin.java`, `TransferStatus.java`, `TransferCommand.java`, `TransferLineCommand.java`
- `repository/TransferRepository.java`
- `dto/StockReturnWDto.java`, `TransferLineWDto.java`, `TransferRDto.java`, `TransferLineRDto.java` ; `mapper/TransferMapper.java`
- Autre feature modifiée : `feature/stock/service/StockMovementService.java` (+ `recordAll`), `service/impl/StockMovementServiceImpl.java`, `StockMovementPolicy.java` (`lockKey`)

**Migrations Liquibase**
- `valimbavaka-core/src/main/resources/db/changelog/changes/2026_11_08-21-changelog.sql`, déclarée dans `db.changelog-master.yaml`

**Frontend** (`valimbavaka-ui/src/app/`)
- `features/transfers/pages/stock-return-page/` (`.ts`, `.html`, `.css`) ; `transfers.routes.ts` ; `data-access/transfer-service.ts`, `transfer-models.ts`, `transfer-fixtures.ts`, `transfer-tones.ts` (renommages) ; `pages/transfer-list-page/` (bouton « Retour de stock »)
- `features/stocks/data-access/stock-service.ts` (`quantities()`), `features/stocks/pages/stock-page/` (bouton « Retour de stock »)
- `app.routes.ts` (garde déplacé), `core/auth/auth-testing.ts` (`transfers.return`), `public/i18n/fr.json`, `mg.json`

**Tests**
- Backend : `feature/transfer/` (`TransferOriginTest`, `TransferPolicyTest`, `TransferLedgerTest`, `TransferServiceImplTest`, `StockReturnIT`, `TransferServiceIT`) ; `feature/stock/service/impl/StockMovementServiceImplTest` (ajout) ; `StockApiTestSupport` rendu `public` pour être réutilisé
- Frontend : `features/transfers/data-access/transfer-service.spec.ts`, `pages/stock-return-page/stock-return-page.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** :
- [STK-01](../STK-01/guide.md) : `StockMovementService` (mouvements `TRANSFER`, contrôle du solde, verrou)
- [JNL-01](../JNL-01/guide.md) : `JournalService.record`
- [JNL-02](../JNL-02/guide.md) : `OperationRepository` (pas de suppression)
- [SYS-05](../SYS-05/guide.md) : emplacements de stock (`ReferentialService.getStockLocations`)
- [SYS-07](../SYS-07/guide.md) / [SYS-12](../SYS-12/guide.md) : file hors ligne et `Idempotency-Key`

**Ces tickets s'appuient sur lui** :
- [TRF-01](../TRF-01/guide.md) · historique : lire `transfers` (numéro, `created_at`, origine, destination, statut, `created_by`)
- [TRF-04](../TRF-04/guide.md) · détail : `GET /api/v1/transfers/{id}` à créer, lignes déjà persistées
- [JR-01](../JR-01/guide.md) / [TRF-02](../TRF-02/guide.md) · transferts du matin : `TransferService.record` avec `TransferOrigin.MORNING`

## Comment l'utiliser depuis un autre ticket

Ouverture de journée (JR-01 / TRF-02), un bon par destination, dans une méthode `@Transactional` :

```java
transferService.record(new TransferCommand(
        TransferOrigin.MORNING,
        warehouseLocationId,
        storeLocationId,
        List.of(new TransferLineCommand(soapId, new BigDecimal("24"))),
        null)); // statut VALIDATED, numéro TRF-0093, entrepôt −24, boutique +24
```

Côté interface (retour de stock) :

```ts
this.transferService.recordReturn({ fromLocationId, toLocationId, lines: [{ articleId, quantity: 2.5 }], note: null });
```

## Points d'attention

- **Hypothèse (statut dédié)** : le critère « identifiable par un statut dédié » est lu au pied de la lettre, comme dans la maquette (badge « Retour ») : le **statut** vaut `RETURN`, en plus de l'**origine** `RETURN`. Les statuts de la maquette `IN_TRANSIT`, `RECEIVED`, `DISCREPANCY` (réception avec écart) ne relèvent d'aucun ticket : non implémentés. Un statut `CANCELLED` (annulation, JNL-02) ajoutera une migration de la contrainte `ck_transfers_status`.
- **Hypothèse (destination libre)** : l'énoncé dit « ex. boutique → entrepôt » ; toute paire d'emplacements actifs différents est acceptée, l'écran propose Boutique → Entrepôt par défaut.
- **Hypothèse (mouvements)** : un retour bouge le stock par deux mouvements `TRANSFER` (règle de STK-01 « un transfert = deux mouvements »), pas par `RETURN` (type d'entrée seule, réservé aux retours client).
- **Historique et détail non livrés** : `GET /api/v1/transfers` (TRF-01) et `GET /api/v1/transfers/{id}` (TRF-04) restent à faire ; la liste `/transfers` reste en données de démonstration. TRF-01 remplacera `TransferRowRDto` / `TransferRowStatus` par les types du serveur et résoudra libellés d'emplacements, noms d'articles et nom de l'émetteur (`createdBy`).
- **Écran limité à 100 articles actifs** (une page de `GET /api/v1/stocks`) : au-delà, il faudra une recherche d'article.
- Le bouton « Créer un retour » du détail (maquette TRF-04) reste une action de démonstration : à brancher sur `/transfers/return` (avec préremplissage) par TRF-04 si souhaité.
- `demo-pages.a11y.spec.ts` ne couvre pas la nouvelle page (elle appelle l'API) ; champs étiquetés, `fieldset`/`legend`, boutons nommés par `aria-label`.
- Tests écrits mais pas encore exécutés : voir le scénario avant de s'appuyer sur ce contrat.
