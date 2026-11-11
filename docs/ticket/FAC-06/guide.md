# Guide d'intégration — FAC-06 · Export du livre des ventes

Statut : ☐ tests écrits, à lancer (CI ou local) · Ticket : [#76](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/76) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

L'administrateur exporte le livre des ventes d'une période en XLSX (et en PDF) depuis `/sales`. Le livre range les **factures** (FAC-01) et les **avoirs** (FAC-04) dans une même liste : chaque document est une ligne, l'avoir en négatif. La liste JSON est paginée, l'export réutilise le mécanisme de SYS-01 (`@ExcelExport`), sans dépendance nouvelle.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/invoices/sales-book` | `invoices.export` (ADMIN) | `from`, `to` (`yyyy-MM-dd`, obligatoires), `PageQuery` (`page`, `size`, `search`, `sortBy` ∈ `businessDate`/`issuedAt`/`number`/`total`, `sortDirection`) | 200 `PageResponse<SalesBookRowRDto>` | 400 (période absente ou mal formée), 401, 403, 422 (début après fin, plus de 366 jours) |
| GET | `/api/v1/invoices/sales-book/export` | idem | idem ; `size` vaut 10 000 par défaut | 200 `livre-des-ventes.xlsx`, onglet « Livre des ventes » | 400, 401, 403, 422 (période, ou plus de 10 000 lignes sans fichier partiel) |
| GET | `/api/v1/invoices/sales-book/export/pdf` | idem | idem | 200 `livre-des-ventes.pdf` (tableau A4 paysage) | idem |

Les deux routes d'export sont créées par `@ExcelExport` sur `SalesBookController.getSalesBook` (`controller/SalesBookController.java`) ; elles héritent de la permission et de la validation de la liste (SYS-01). Lecture seule : ni corps, ni écriture, donc ni mass assignment ni 409.

`SalesBookRowRDto` (`dto/SalesBookRowRDto.java`) : `id`, `businessDate`, `type` (« Facture » / « Avoir »), `number`, `invoiceNumber` (avoir seulement : facture d'origine), `saleNumber`, `salesChannelLabel`, `sellerName`, `paymentMethod`, `status` (facture seulement : « Validée », « Contre-passée », « Annulée »), `subtotal`, `discountAmount`, `total` (négatifs sur un avoir), `reason` (avoir seulement). Colonnes xlsx/PDF = ces champs, dans cet ordre, sauf `id`.

### Services et ports Java réutilisables

- `feature.invoice.service.SalesBookService.getSalesBook(PageQuery, from, to)` (`SalesBookServiceImpl.java`) : contrôle la période (`MAX_PERIOD_DAYS` = 366, bornes comprises), tri en liste blanche avec départage par `issuedAt` puis `number` (pages stables), recherche insensible à la casse sur les numéros de document, de vente, de facture d'origine et sur la vendeuse.
- `SalesBookRepository.search` (`repository/SalesBookRepository.java`) : lecture seule (`Repository`, ni `save` ni `delete`).

### Frontend

- Écran : `/sales` (`features/sales/pages/sales-page`). Le bouton « Livre des ventes » (visible avec la permission `invoices.export`, `aria-expanded`) ouvre `components/sales-book-export` : période (mois en cours par défaut), boutons « Exporter en XLSX » et « Exporter en PDF ». Une période à l'envers, vide ou de plus de 366 jours n'est pas envoyée (boutons désactivés, message `role="alert"`).
- Téléchargement et confirmation : `ExportService.download(SALES_BOOK_URL, format, params)` (SYS-01), toast « Fichier … généré. » ; échecs : toasts `export.forbidden`, `export.tooLarge`, `export.failed`.
- Traductions fr et mg : `sales.bookExport.*`.

### Données

- Migration `2026_11_11-97-changelog.sql` (3 changesets, rollback fourni) : vue `sales_book_entries` (`UNION ALL` des tables `invoices` et `credit_notes`), permission `invoices.export` attribuée au rôle ADMIN.
- Entité `SalesBookEntry` (`domain/SalesBookEntry.java`) : `@Immutable`, mappée sur la vue (pas de table, pas de `BaseEntity`).
- Journal : aucune écriture (l'action `EXPORT` n'existe pas, voir SYS-01).

## Règles métier à respecter par les autres tickets

- **Facture et avoir = deux lignes** ; l'avoir est en négatif ; la facture contre-passée garde son montant et son numéro (la numérotation reste continue). Somme d'une facture contre-passée et de son avoir = 0.
- **Une facture `CANCELLED` sans avoir** garde sa ligne mais compte 0 (la vue force ses montants à 0).
- **Période obligatoire et bornée** (366 jours) ; **jour comptable** (`business_date`), pas l'heure d'émission. L'avoir porte le jour comptable de sa facture (copié par `CreditNote.issue`), la journée de la vente devant être ouverte pour l'émettre.
- **Aucun chargement de toute la base** : tout passe par une page (≤ 100 en JSON, ≤ 10 000 à l'export, au-delà 422).
- **Colonnes exportées** : ne pas ajouter de donnée sensible à `SalesBookRowRDto` (les mêmes champs servent au JSON et aux deux fichiers).
- Un nouveau type de document comptable (ex. note de débit) s'ajoute par un `UNION ALL` dans une nouvelle migration de la vue (ne pas modifier la migration 97 une fois mergée).

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/invoice/`)
- `controller/SalesBookController.java`
- `service/SalesBookService.java` ; `service/impl/SalesBookServiceImpl.java`
- `domain/SalesBookEntry.java`, `SalesBookEntryType.java` ; `repository/SalesBookRepository.java`
- `mapper/SalesBookMapper.java` ; `dto/SalesBookRowRDto.java`

**Migration Liquibase**
- `valimbavaka-core/src/main/resources/db/changelog/changes/2026_11_11-97-changelog.sql`, déclarée à la fin de `db.changelog-master.yaml`

**Frontend** (`valimbavaka-ui/`)
- `src/app/features/sales/components/sales-book-export/` (composant, gabarit, style, spec)
- `src/app/features/sales/pages/sales-page/sales-page.ts`, `.html`
- `public/i18n/fr.json`, `mg.json` (bloc `sales.bookExport`)

**Tests**
- Backend : `feature/invoice/controller/SalesBookIT.java`, `feature/invoice/service/impl/SalesBookServiceImplTest.java`
- Frontend : `features/sales/components/sales-book-export/sales-book-export.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** :
- [FAC-01](../FAC-01/description.md) · [guide](../FAC-01/guide.md) (table `invoices`, index sur `business_date`)
- [FAC-04](../FAC-04/description.md) · [guide](../FAC-04/guide.md) (table `credit_notes`, statut `REVERSED`)
- [SYS-01](../SYS-01/description.md) · [guide](../SYS-01/guide.md) (`@ExcelExport`, `ExportService`)

**Ces tickets s'appuient sur lui** :
- _(rien)_

## Comment l'utiliser depuis un autre ticket

```ts
this.exportService.download('/api/v1/invoices/sales-book', 'xlsx', params); // params : from, to
```

## Points d'attention

- **Hypothèse (avoirs)** : la description ne dit pas comment figurent les factures contre-passées ; retenu : le classique « facture et avoir = deux lignes, l'avoir en négatif », qui garde la numérotation visible et un total net exact. À faire valider par le PO / la comptabilité.
- **Hypothèse (période)** : 366 jours au plus, imposé côté serveur (422) et côté écran. Un exercice complet tient dans une période ; au-delà, plusieurs exports.
- **Hypothèse (droit)** : permission propre `invoices.export` (ADMIN) plutôt que `sales.read`, car la vendeuse ne doit pas exporter les ventes de ses collègues. Elle ne voit pas le bouton.
- **Hypothèse (écran)** : le bouton « Livre des ventes » existait déjà sur `/sales` (maquette) ; il ouvre maintenant le panneau d'export. FAC-05 (écran des statuts) n'est pas modifié ici.
- **Tests d'intégration** : la base est partagée ; les tests cherchent le numéro de leur propre facture (`search`) et ne comptent jamais une période entière.
- **PDF construit en mémoire** (SYS-01) : jusqu'à 10 000 lignes ; le xlsx est en streaming.
- **Total général absent des fichiers** : le tableau exporté ne contient que les lignes (pas de ligne « Total net ») ; la comptabilité somme la colonne « Total (Ar) ». À ajouter par un ticket dédié si besoin (résumé du PDF de SYS-01).
- **Export non tracé au journal** : pas d'action `EXPORT` (voir SYS-01).
- Le commentaire de `sales.routes.ts` (« en construction tant que FAC-02 à FAC-06 manquent ») n'est pas retouché pour éviter un conflit avec FAC-05.
