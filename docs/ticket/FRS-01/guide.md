# Guide d'intégration — FRS-01 · Fiche fournisseur complète

Statut : ✅ complet au 2026-10-23 — vérifié sur le code, les tests (CI + exécution locale) et la base · Ticket : [#17](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/17) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

L'administrateur consulte et modifie la fiche d'un fournisseur : nom, type (local ou étranger), contact, téléphone, courriel, adresse, notes, état actif. Un fournisseur ne se supprime pas, il se désactive. Chaque écriture est tracée dans le journal. Le bloc « articles fournis » liste les articles du fournisseur depuis [ART-01](../ART-01/guide.md).

## Contrat exposé

### Endpoints backend (`/api/v1/suppliers`, administrateur seul)

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/suppliers` | `suppliers.read` | `PageQuery` + `type` (`LOCAL`/`FOREIGN`) + `active` (booléen) | 200 `PageResponse<SupplierRDto>` | 401, 403 |
| GET | `/api/v1/suppliers/{id}` | `suppliers.read` | — | 200 `SupplierRDto` | 401, 403, 404 |
| POST | `/api/v1/suppliers` | `suppliers.create` | `SupplierWDto` | 201 `SupplierRDto` + `Location` | 401, 403, 409 (nom déjà pris), 422 |
| PUT | `/api/v1/suppliers/{id}` | `suppliers.update` | `SupplierWDto` | 200 `SupplierRDto` | 401, 403, 404, 409 (nom ou version périmée), 422 |
| PATCH | `/api/v1/suppliers/{id}/status` | `suppliers.update` | `SupplierStatusWDto` (`active`) | 200 `SupplierRDto` | 401, 403, 404, 422 |

Aucun `DELETE` (le test `deleteSupplier_isNotExposed` le vérifie).

- Tri autorisé : `createdAt` (défaut, décroissant), `name`, `type`. `search` porte sur le nom et le nom du contact.
- `SupplierWDto` : `name` (obligatoire, ≤ 150), `type` (obligatoire), `contactName` (≤ 150), `phone` (≤ 50), `email` (format courriel, ≤ 255), `address` (≤ 500), `notes` (≤ 2000), `version` (facultatif : version lue, une valeur périmée donne 409). **Ni `id`, ni `active`** : un `id` ou un statut envoyé dans le corps est ignoré (test de mass assignment).
- `SupplierRDto` : `id`, `name`, `type`, `contactName`, `phone`, `email`, `address`, `notes`, `active`, `createdAt`, `updatedAt`.
- Unicité du nom insensible à la casse (index `uq_suppliers_name` sur `lower(name)`) ; le nom est nettoyé (`strip`) avant comparaison.
- `PATCH /status` est idempotent : aucune modification ni ligne de journal si l'état est déjà celui demandé.
- Journal : `CREATE` à la création, `UPDATE` à une modification réelle (aucune ligne si rien ne change, FRS-02) et au changement d'état, ressource `SUPPLIER`.
- Verrou optimiste : `SupplierRDto.version` est renvoyée par le formulaire dans `SupplierWDto.version` ; si la fiche a changé entre-temps, 409 « Le fournisseur a été modifié entre-temps : rechargez la fiche puis réessayez. » (texte en français depuis FRS-02). Un conflit détecté au commit (verrou JPA) ou un doublon refusé par la base donnent aussi 409.

### Service Java

`feature.supplier.service.SupplierService` : `getSuppliers(query, type, active)`, `getSupplierById`, `createSupplier`, `updateSupplier`, `setActive`, et `isActiveSupplier(UUID)` (ajouté par ART-01 : vrai si le fournisseur existe et est actif, pour les contrôles de référence des autres features). Les autres features ne doivent pas lire `SupplierRepository` : pas d'association JPA entre features, un article référence son fournisseur par `supplierId` (UUID).

### Frontend (`features/supplier`)

- Routes lazy `/suppliers` (garde `suppliers.read`), `/suppliers/new`, `/suppliers/:id/edit`.
- `data-access/supplier-service.ts` : `list(query)`, `getById`, `create`, `update`, `setActive`. Types dans `supplier-models.ts` (`SupplierRDto`, `SupplierWDto`, `SupplierStatusWDto`).
- `pages/supplier-form-page` (création et modification ; bloc « articles fournis » : articles lus par `ArticleService.list({ supplierId })`, ART-01).

### Données

- Table `suppliers` (`id` UUID, `name`, `type` avec contrainte `LOCAL` / `FOREIGN`, `contact_name`, `phone`, `email`, `address`, `notes`, `is_active`, audit, `version`).
- Permissions `suppliers.create` et `suppliers.update` créées et données à `ADMIN` ; `suppliers.read` existait déjà (AUTH-01) et reste réservée à l'administrateur.
- Migration : `valimbavaka-core/src/main/resources/db/changelog/changes/2026_10_21-01-changelog.sql`.

## Règles métier pour les autres tickets

- Seul l'administrateur gère les fournisseurs ; une vendeuse ou un livreur reçoit 403 sur toute la ressource.
- Un fournisseur désactivé reste lisible et conserve son historique ; les écrans de sélection (réapprovisionnement, article) doivent proposer uniquement les fournisseurs `active = true` (le filtre `active` de la liste existe).
- Le type de fournisseur (`LOCAL` / `FOREIGN`) pilote le délai de réapprovisionnement : voir [FRS-04](../FRS-04/description.md).
- Référencer un fournisseur par son UUID, jamais par son nom (le nom est modifiable).

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/supplier/`)
- `controller/SupplierController.java`
- `domain/Supplier.java`, `SupplierDetails.java`, `SupplierType.java`
- `dto/SupplierRDto.java`, `SupplierWDto.java`, `SupplierStatusWDto.java`, `mapper/SupplierMapper.java`
- `repository/SupplierRepository.java`
- `service/SupplierService.java`, `service/impl/SupplierServiceImpl.java`

**Frontend** (`valimbavaka-ui/src/app/features/supplier/`)
- `supplier.routes.ts`, `data-access/*`, `pages/supplier-form-page/*` ; liste et fiche latérale dans `features/suppliers/pages/supplier-list-page/*` et `features/suppliers/components/supplier-detail/*`
- Attention : le dossier `features/suppliers/` (avec un « s ») contient la **maquette** de DSG-01 (fixtures, pas d'appel API) et n'est pas branché sur la route `/suppliers`.

**Tests**
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/supplier/controller/SupplierSecurityIT.java`
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/supplier/repository/SupplierRepositoryIT.java`
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/supplier/service/impl/SupplierServiceImplTest.java`
- `valimbavaka-ui/src/app/features/supplier/data-access/supplier-service.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** : [JNL-01](../JNL-01/guide.md) (journal), [JNL-02](../JNL-02/guide.md) (pas de suppression), [AUTH-01](../AUTH-01/guide.md) (permissions).

**Ces tickets s'appuient sur lui** : [FRS-02](../FRS-02/description.md), [FRS-03](../FRS-03/description.md), [FRS-04](../FRS-04/description.md), [ART-01](../ART-01/guide.md), [STK-05](../STK-05/description.md), [STK-06](../STK-06/description.md).

## Comment l'utiliser depuis un autre ticket

```ts
// Sélecteur de fournisseur (ex. fiche article)
this.supplierService
  .list({ page: 1, size: 100, sortBy: 'name' })
  .subscribe((page) => this.suppliers.set(page.items.filter((s) => s.active)));
```

Le filtre `type` / `active` du backend est disponible pour un appel direct : `GET /api/v1/suppliers?type=LOCAL&active=true`.

## Points d'attention

- **Liste et fiche latérale (mise à jour)** : la liste `/suppliers` est désormais celle de la **maquette** (`features/suppliers/pages/supplier-list-page`, fiche à droite `features/suppliers/components/supplier-detail`), branchée sur `GET /api/v1/suppliers` ; l'ancienne liste `features/supplier/pages/supplier-list-page` est supprimée. Recherche (nom, contact) et filtre par type sont faits par le serveur (recherche temporisée 300 ms), pagination serveur de 8 par page. La fiche à droite lit `GET /api/v1/suppliers/{id}`, affiche coordonnées et notes, les articles fournis (`ArticleService.list({ supplierId })`, onglet lu à l'ouverture) et porte l'activation / désactivation et le lien « Modifier » (permission `suppliers.update`). Le formulaire de création et de modification (`supplier-form-page`) est inchangé. Retirés de la maquette car sans équivalent serveur : ville, NIF, délai de réapprovisionnement ([FRS-04](../FRS-04/description.md)), dernière réception, onglet « Réceptions », types `RAW_MATERIAL` / `PACKAGING` / `FINISHED` / `IMPORT` (le serveur ne connaît que `LOCAL` et `FOREIGN`).
- **FRS-02 (livré le 2026-10-26)** : la création et la modification sont parties de cet existant ; une modification sans changement réel n'écrit plus de ligne `UPDATE` (voir le [guide FRS-02](../FRS-02/guide.md)). FRS-03 a branché la liste sur les filtres `type` et `search` existants (voir le [guide FRS-03](../FRS-03/guide.md)).
- Le bloc « articles fournis » se remplit depuis [ART-01](../ART-01/guide.md) (2026-10-26) : pas d'endpoint dédié, c'est `GET /api/v1/articles?supplierId=…` (100 articles au plus affichés). Un fournisseur désactivé ne peut plus être choisi sur une nouvelle fiche article ; les fiches qui le portent déjà restent modifiables.
- L'écriture n'utilise pas encore `OutboxService` : un fournisseur ne peut pas être saisi hors ligne ([SYS-07](../SYS-07/description.md)).
- Messages serveur et libellés du journal passés en français par FRS-02 (« Un fournisseur avec ce nom existe déjà. », « Création du fournisseur … »).
- Pas de test d'interface pour la page liste ; le formulaire est couvert par `supplier-form-page.spec.ts` (FRS-02).
