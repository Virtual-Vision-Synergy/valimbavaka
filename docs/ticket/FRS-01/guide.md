# Guide d'intégration — FRS-01 · Fiche fournisseur complète

Statut : ✅ complet au 2026-10-23 — vérifié sur le code, les tests (CI + exécution locale) et la base · Ticket : [#17](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/17) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

L'administrateur consulte et modifie la fiche d'un fournisseur : nom, type (local ou étranger), contact, téléphone, courriel, adresse, notes, état actif. Un fournisseur ne se supprime pas, il se désactive. Chaque écriture est tracée dans le journal. Le bloc « articles fournis » est prévu dans l'interface mais reste vide tant que [ART-01](../ART-01/description.md) n'est pas livré.

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
- Journal : `CREATE` à la création, `UPDATE` à la modification et au changement d'état, ressource `SUPPLIER`.
- Verrou optimiste : `SupplierRDto.version` est renvoyée par le formulaire dans `SupplierWDto.version` ; si la fiche a changé entre-temps, 409 « The supplier was modified in the meantime: reload it and try again. ». Un conflit détecté au commit (verrou JPA) ou un doublon refusé par la base donnent aussi 409.

### Service Java

`feature.supplier.service.SupplierService` : `getSuppliers(query, type, active)`, `getSupplierById`, `createSupplier`, `updateSupplier`, `setActive`. Les autres features ne doivent pas lire `SupplierRepository` : pas d'association JPA entre features, un article référence son fournisseur par `supplierId` (UUID).

### Frontend (`features/supplier`)

- Routes lazy `/suppliers` (garde `suppliers.read`), `/suppliers/new`, `/suppliers/:id/edit`.
- `data-access/supplier-service.ts` : `list(query)`, `getById`, `create`, `update`, `setActive`. Types dans `supplier-models.ts` (`SupplierRDto`, `SupplierWDto`, `SupplierStatusWDto`).
- `pages/supplier-list-page` (liste, activation / désactivation) et `pages/supplier-form-page` (création et modification ; bloc « articles fournis » vide en attendant ART-01).

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
- `supplier.routes.ts`, `data-access/*`, `pages/supplier-list-page/*`, `pages/supplier-form-page/*`
- Attention : le dossier `features/suppliers/` (avec un « s ») contient la **maquette** de DSG-01 (fixtures, pas d'appel API) et n'est pas branché sur la route `/suppliers`.

**Tests**
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/supplier/controller/SupplierSecurityIT.java`
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/supplier/repository/SupplierRepositoryIT.java`
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/supplier/service/impl/SupplierServiceImplTest.java`
- `valimbavaka-ui/src/app/features/supplier/data-access/supplier-service.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** : [JNL-01](../JNL-01/guide.md) (journal), [JNL-02](../JNL-02/guide.md) (pas de suppression), [AUTH-01](../AUTH-01/guide.md) (permissions).

**Ces tickets s'appuient sur lui** : [FRS-02](../FRS-02/description.md), [FRS-03](../FRS-03/description.md), [FRS-04](../FRS-04/description.md), [ART-01](../ART-01/description.md), [STK-05](../STK-05/description.md), [STK-06](../STK-06/description.md).

## Comment l'utiliser depuis un autre ticket

```ts
// Sélecteur de fournisseur (ex. fiche article)
this.supplierService
  .list({ page: 1, size: 100, sortBy: 'name' })
  .subscribe((page) => this.suppliers.set(page.items.filter((s) => s.active)));
```

Le filtre `type` / `active` du backend est disponible pour un appel direct : `GET /api/v1/suppliers?type=LOCAL&active=true`.

## Points d'attention

- **Chevauchement avec FRS-02 (ouvert)** : la création, la modification et l'activation existent déjà côté serveur et dans l'interface (formulaire, liste). FRS-02, FRS-03 (filtre par type) et le champ `search` doivent partir de cet existant, pas le refaire. Le filtre par type a son paramètre `type` côté API ; l'interface n'est pas vérifiée dans cette note.
- Le bloc « articles fournis » est vide : il se remplit avec [ART-01](../ART-01/description.md), qui référence le fournisseur depuis l'article. Prévoir un endpoint de lecture des articles par fournisseur dans ART-01.
- L'écriture n'utilise pas encore `OutboxService` : un fournisseur ne peut pas être saisi hors ligne ([SYS-07](../SYS-07/description.md)).
- Messages d'erreur serveur en anglais (« A supplier with this name already exists. ») : le journal aussi (« Supplier created: … »), alors que les autres modules écrivent en français.
- Pas de test d'interface pour les pages liste et formulaire (seul `supplier-service.spec.ts` existe).
