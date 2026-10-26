# Guide d'intégration — FRS-02 · Création / modification d'un fournisseur

Statut : ✅ complet au 2026-10-26 — vérifié sur le code et les tests (exécution locale) · Ticket : [#18](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/18) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

L'administrateur crée un fournisseur ou modifie toute sa fiche depuis un seul formulaire (`/suppliers/new`, `/suppliers/:id/edit`). L'API et le formulaire viennent de [FRS-01](../FRS-01/guide.md). FRS-02 les fiabilise : une modification sans changement réel n'écrit plus rien dans le journal, le formulaire refuse un nom vide ou trop long avec le bon message, et tout le parcours d'enregistrement est couvert par des tests. C'est ce formulaire que [SYS-07](../SYS-07/description.md) branchera sur la file hors ligne.

## Contrat exposé

### Endpoints backend (administrateur seul)

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| POST | `/api/v1/suppliers` | `suppliers.create` | `SupplierWDto` | 201 `SupplierRDto` + `Location` | 401, 403, 409 (nom déjà pris, toute casse), 422 |
| PUT | `/api/v1/suppliers/{id}` | `suppliers.update` | `SupplierWDto` | 200 `SupplierRDto` | 401, 403, 404, 409 (nom d'un autre fournisseur ou version périmée), 422 |

Le `PATCH /status` et les lectures sont décrits dans le [guide FRS-01](../FRS-01/guide.md#contrat-exposé).

Exemple de corps (`PUT`) :

```json
{
  "name": "Import Océan",
  "type": "FOREIGN",
  "contactName": "Mr Lee",
  "phone": "+33 1 00 00 00 00",
  "email": "lee@ocean.com",
  "address": "Marseille",
  "notes": "Livraison le mardi",
  "version": 3
}
```

- `SupplierWDto` : `name` (obligatoire, non blanc, ≤ 150), `type` (`LOCAL` / `FOREIGN`), `contactName` (≤ 150), `phone` (≤ 50), `email` (format courriel, ≤ 255), `address` (≤ 500), `notes` (≤ 2000), `version` (facultatif). `id` et `active` envoyés dans le corps sont ignorés.
- Normalisation serveur (`Supplier.apply`) : `strip` sur tous les textes, chaîne vide ou blanche → `null`, courriel en minuscules. Le client peut donc envoyer `""` pour un champ vidé.
- `PUT` est un remplacement complet de la fiche : un champ facultatif absent ou `null` est **effacé**. Toujours renvoyer tous les champs.
- `version` : envoyer `SupplierRDto.version` lue à l'ouverture ; une valeur différente donne 409 « Le fournisseur a été modifié entre-temps : rechargez la fiche puis réessayez. ». Sans `version`, la modification passe sans contrôle de la version lue (à éviter depuis un écran).

### Service Java

`feature.supplier.service.SupplierService` :

```java
SupplierRDto createSupplier(SupplierWDto request);
SupplierRDto updateSupplier(UUID supplierId, SupplierWDto request);
```

- Nouveau dans FRS-02 : `Supplier.update(SupplierDetails)` renvoie `boolean`, `true` seulement si une valeur a changé **après normalisation**. `updateSupplier` n'écrit la ligne `UPDATE` que dans ce cas.

### Frontend (`features/supplier`)

- Routes : `/suppliers/new` (création), `/suppliers/:id/edit` (modification, `id` lié en `input()`).
- `data-access/supplier-service.ts` : `create(body: SupplierWDto)`, `update(id, body: SupplierWDto)`.
- `pages/supplier-form-page` : formulaire réactif, validations alignées sur le serveur (nom obligatoire et non blanc, ≤ 150 ; courriel valide ; longueurs max), bouton désactivé pendant l'envoi, message du `ProblemDetail` affiché en cas d'erreur (la saisie est conservée), retour à `/suppliers` après succès.
- Messages sous chaque champ (`fieldError`) : `supplier.form.nameRequired`, `supplier.form.emailInvalid`, `supplier.form.tooLong` (nouvelle, paramètre `{{max}}` : « 150 caractères maximum ») dans `public/i18n/fr.json` et `mg.json`. Le courriel est plafonné à 254 caractères par `Validators.email` d'Angular (le serveur en accepte 255).

### Données

- Aucune migration : table `suppliers` de FRS-01 (`2026_10_21-01-changelog.sql`).
- Journal : `CREATE` « Création du fournisseur <nom> » à la création, `UPDATE` « Modification du fournisseur <nom> » à une modification réelle (« Activation / Désactivation du fournisseur <nom> » pour le statut), ressource `SUPPLIER`, identifiant = UUID du fournisseur.

## Règles métier pour les autres tickets

- Un enregistrement sans changement réel ne laisse aucune trace : ne pas compter les lignes `UPDATE` comme un nombre d'« enregistrements » du formulaire.
- Le nom est unique sans tenir compte de la casse ni des espaces autour ; le renommage vers le nom d'un autre fournisseur est refusé (409).
- Seul l'administrateur crée ou modifie ; vendeuse et livreur reçoivent 403.
- La mise en file hors ligne (SYS-07) doit rejouer le même corps, `version` comprise : un rejeu après une modification concurrente reçoit 409 et doit être présenté à l'utilisateur, pas réessayé en boucle.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/supplier/`)
- `domain/Supplier.java` : `update` renvoie `boolean`, méthode privée `details()`.
- `service/impl/SupplierServiceImpl.java` : `updateSupplier` ne trace que si la fiche a changé ; messages et libellés du journal en français.
- `dto/SupplierWDto.java`, `dto/SupplierStatusWDto.java` : messages de validation en français.

**Frontend** (`valimbavaka-ui/src/app/features/supplier/pages/supplier-form-page/`)
- `supplier-form-page.ts` : `Validators.pattern(/\S/)` sur le nom, `fieldError()` pour le message de chaque champ.
- `supplier-form-page.html` : un message d'erreur sous chacun des 7 champs (`aria-describedby`).
- `valimbavaka-ui/public/i18n/fr.json`, `mg.json` : clé `supplier.form.tooLong`.

**Tests**
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/supplier/controller/SupplierSecurityIT.java`
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/supplier/service/impl/SupplierServiceImplTest.java`
- `valimbavaka-ui/src/app/features/supplier/pages/supplier-form-page/supplier-form-page.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** : [FRS-01](../FRS-01/guide.md) (API, formulaire, verrou optimiste), [JNL-01](../JNL-01/guide.md) (journal, règle « pas de trace sans changement »).

**Ces tickets s'appuient sur lui** : [SYS-07](../SYS-07/description.md) (brancher ce formulaire sur la file hors ligne).

## Comment l'utiliser depuis un autre ticket

```ts
// Enregistrer une fiche ouverte : renvoyer tous les champs et la version lue.
const body: SupplierWDto = { ...this.form.getRawValue(), version: supplier.version };
this.supplierService.update(supplier.id, body).subscribe({
  next: () => void this.router.navigate(['/suppliers']),
  error: (error: unknown) => this.errorMessage.set(errorMessage(error)), // 409 : recharger la fiche
});
```

## Points d'attention

- Les messages d'erreur du serveur (`detail` du `ProblemDetail`) sont en français et affichés tels quels : ils ne suivent pas la langue choisie (malgache). Même limite pour tous les modules.
- Les lignes du journal écrites avant FRS-02 restent en anglais (« Supplier created: … ») : le journal est append-only.
- Pas encore de saisie hors ligne : `supplier-service.ts` appelle `HttpClient` directement ([SYS-07](../SYS-07/description.md)).
- Après un 409 de version, le formulaire garde la saisie mais ne recharge pas la fiche : l'utilisateur doit revenir à la liste et rouvrir.

> **Mise à jour SYS-07** : l'enregistrement de ce formulaire passe maintenant par `OutboxService.submit` (file hors ligne, clé d'idempotence) ; hors connexion la saisie est conservée puis envoyée une seule fois. Voir le [guide SYS-07](../SYS-07/guide.md).
