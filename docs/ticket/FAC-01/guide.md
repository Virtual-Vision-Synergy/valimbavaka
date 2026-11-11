# Guide d'intégration — FAC-01 · Génération automatique de facture

Statut : ✅ écrit le 2026-11-11 · Ticket : [#71](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/71) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Chaque vente enregistrée (`SaleService.record`, donc le poste de caisse **et** une commande livrée, décision PO du 07/11/2026) produit **dans la même transaction** une facture numérotée `FAC-<année de la journée>-<NNNN>`, sans trou et sûre en concurrence. La facture est un **instantané figé** de la vente (noms, prix, mode de règlement, émetteur) : jamais modifiée ni supprimée. Le module `feature/invoice` écoute `SaleRecordedEvent` ; `sale` ne le connaît pas (aucun cycle). Deux lectures en GET exposent la facture ; l'écran `/sales` affiche la facture émise. Régénération, impression/PDF/envoi, avoir, statuts et livre des ventes restent à FAC-02 à FAC-06 : le modèle (statut, instantané, `sale_id` unique) est prêt pour eux.

## Contrat exposé

### Endpoints backend (`/api/v1/invoices`, lecture seule)

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie (`XxxRDto`) | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/invoices/sales/{saleId}` | `sales.read` (admin, vendeuse) | — | 200 `InvoiceRDto` | 400 (UUID mal formé), 401, 403, 404 |
| GET | `/api/v1/invoices/{id}` | `sales.read` (admin, vendeuse) | — | 200 `InvoiceRDto` | 400, 401, 403, 404 |

- **Aucun endpoint d'écriture** : une facture naît de l'enregistrement d'une vente, jamais d'une requête (donc ni mass assignment, ni 422, ni 409).
- **Vendeuse** : elle ne lit que les factures des ventes qui lui sont créditées ; celle d'une collègue, ou d'une vente sans vendeuse (commande livrée saisie par l'administrateur), répond **404** (même règle que VTE-07). L'administrateur lit tout.
- `InvoiceRDto` : `id`, `number` (`FAC-2026-0042`), `saleId`, `saleNumber`, `issuedAt`, `businessDate`, `status` (`OperationStatus`, `VALIDATED` à l'émission), `salesChannelLabel` (`null` si le canal était désactivé), `sellerId` et `sellerName` (`null` : aucune vendeuse), `paymentMethod` (nom du mode de la vente : `CASH`, `MVOLA`, `ORANGE_MONEY`, `CARD`, `ON_DELIVERY`), `issuerName`, `issuerNif` (`null` tant que non configuré), `subtotal`, `discountAmount`, `total`, `lines` (`InvoiceLineRDto` : `articleId`, `label` = nom de l'article à l'émission, `quantity`, `unitPrice`, `lineTotal`).

### Services et ports Java réutilisables

| Port | Méthode | Rôle |
|---|---|---|
| `feature.invoice.service.InvoiceService` | `InvoiceRDto issueForSale(UUID saleId)` (`InvoiceServiceImpl.java:58`) | Émet la facture d'une vente : lit la vente par `SaleService.getSaleById`, construit l'instantané (`InvoiceDrafts.of`, `InvoiceDrafts.java:51`), prend le numéro (`InvoiceRepository.nextNumber`, `InvoiceServiceImpl.java:66`), enregistre, trace au journal. **Idempotent** (une vente déjà facturée rend sa facture, aucun numéro consommé). À appeler dans la transaction de la vente ; c'est ce que fait l'écouteur. |
| idem | `getInvoiceById(UUID)`, `getInvoiceBySaleId(UUID)` | Lecture avec la règle « une vendeuse ne lit que ses ventes » (`InvoiceServiceImpl.java:90`) ; 404 sinon. |

- **Écouteur** : `service/impl/SaleInvoiceListener.java:23`, `@EventListener` **synchrone** sur `feature.sale.domain.SaleRecordedEvent` (publié par `SaleLedger`, VTE-01) : il tourne dans la transaction de la vente. Si la facture échoue, la vente est annulée avec elle (« pas de vente sans facture ») ; si la vente échoue (stock, journée fermée), aucune facture ni aucun numéro.
- **Numérotation** : table `invoice_counters` (une ligne par année) ; `InvoiceRepository.nextNumber(year)` fait un `INSERT … ON CONFLICT DO UPDATE … RETURNING` atomique. La ligne reste verrouillée jusqu'à la fin de la transaction de la vente : deux ventes simultanées s'attendent (numéros distincts), et une vente annulée **rend** son numéro (aucun trou). L'année est celle de la **journée ouverte** (`sales.business_date`), pas de l'instant UTC. Format `FAC-%d-%04d` (plus de 9 999 factures dans l'année : le numéro s'allonge, jamais tronqué).
- **Émetteur** : `InvoiceProperties` (`vvs.invoice.issuer-name`, défaut « By Valimbavaka Cosmetics » ; `vvs.invoice.issuer-nif` = variable d'environnement `INVOICE_ISSUER_NIF`, facultative, vide = absent).
- Domaine : `domain/Invoice.java:93` (`Invoice.issue`), `InvoiceLine`, `InvoiceDraft` / `InvoiceLineDraft` (instantané). Le domaine **ne dépend pas** du domaine `sale` (ArchUnit) : le mode de règlement est gardé en texte.

### Frontend (`features/sales`)

- Route `/sales` inchangée. Le panneau `components/invoice-panel` charge la facture de la vente sélectionnée (`rxResource` sur `InvoiceService.getBySale`, `invoice-panel.ts:34`) ; chargement, puis document de la facture émise (numéro, date `jj/mm/aaaa HH:MM`, lignes figées). Sans facture (404 : vente antérieure à FAC-01 ou d'une collègue) ou en erreur : l'**aperçu** de VTE-07 et sa note « Aucune facture n'a été émise… ».
- `data-access/invoice-service.ts` (nouveau) : `getBySale(saleId)`, `getById(invoiceId)`.
- `data-access/sales-models.ts` : `InvoiceRDto` et `InvoiceLineRDto` sont désormais les **miroirs du serveur** ; l'ancien modèle du document devient `InvoiceView` / `InvoiceLineView` (le composant `invoice-document` ne change que de type).
- `data-access/sales-mapping.ts` : `toInvoiceView` (`:50`, facture émise → document ; `VALIDATED` → badge « Payée », annulée ou contre-passée → « Avoir »), `toInvoicePreview` (`:77`, aperçu), `localDateTime` (`:39`).
- Traduction : `sales.invoice.previewNote` (fr, mg) reformulée. Les boutons PDF, Envoyer, Régénérer et Avoir restent des actions de démonstration (FAC-02 à FAC-04).

### Données

- Migration `2026_11_11-90-changelog.sql` (2 changesets, rollback fourni) : `invoice_counters` ; `invoices` (`number` unique, `sale_id` **unique**, instantané, `status` CHECK `VALIDATED`/`CANCELLED`/`REVERSED`, `total = subtotal - discount_amount`, audit et `version`) ; `invoice_lines` (`@ElementCollection`, ordre `line_number`). **Pas de clé étrangère** vers `sales`, `users` ni `articles` : la facture est un document autonome et les features ne se lient pas par la base.
- Permission : aucune nouvelle ; `sales.read` (admin, vendeuse) suffit.
- Journal : ressource `INVOICE`, action `CREATE`, libellé `Facture FAC-2026-0042 · vente VTE-0042 · 64000.00 Ar · Espèces`.

## Règles métier pour les autres tickets

- **Une facture par vente**, émise à l'enregistrement (`uq_invoices_sale_id`). Ne jamais appeler `issueForSale` depuis un autre module pour une vente qui vient d'être enregistrée : l'écouteur l'a déjà fait.
- **Facture = instantané** : lire `invoices` / `InvoiceRDto`, ne pas recalculer depuis la vente ni les fiches (nom d'article, prix, émetteur et NIF sont ceux du jour de l'émission).
- **FAC-02 (régénération)** : régénérer le **document** (PDF) à partir de l'instantané ; si le contenu doit changer, c'est un nouvel instantané sous le **même numéro** (colonnes `updatable = false` à relâcher exprès), jamais un nouveau numéro. `findBySaleId` / `issueForSale` rendent déjà l'existante. Une vente antérieure à FAC-01 n'a pas de facture : `issueForSale(saleId)` l'émet (rattrapage possible sans autre code).
- **FAC-04 (avoir)** : `Invoice.status` est le seul champ modifiable ; l'écouteur de `SaleCancelledEvent` (LIV-05) et `ReversibleOperationService<SaleRDto>.reverse` y mettent `CANCELLED` / `REVERSED`. **Pas encore écouté** : une vente annulée garde aujourd'hui sa facture `VALIDATED`. L'avoir devra avoir sa propre numérotation (compteur à ajouter : `invoice_counters` est par année et par type de document à étendre).
- **FAC-05 (statuts)** : `Invoice.status` est l'état de l'opération (`OperationStatus`) ; « Payée / Envoyée / En attente » de l'écran ne sont pas stockés (l'écran affiche « Payée » pour `VALIDATED`). Ajouter une colonne dédiée (ex. `sent_at`) par migration, sans toucher à `status`.
- **FAC-06 (livre des ventes)** : lire `invoices` par `business_date` (index `ix_invoices_business_date`), `VALIDATED` seulement pour le CA ; ajouter les requêtes d'agrégat dans un repository de lecture de `feature/invoice`.
- **Numéros** : ne jamais lire `invoice_counters` pour afficher « le prochain numéro » (inutile, et il dépend de la concurrence). Le numéro n'existe qu'après l'encaissement.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/invoice/`)
- `controller/InvoiceController.java`
- `service/InvoiceService.java` ; `service/impl/InvoiceServiceImpl.java`, `InvoiceDrafts.java`, `SaleInvoiceListener.java`
- `domain/Invoice.java`, `InvoiceLine.java`, `InvoiceDraft.java`, `InvoiceLineDraft.java`
- `repository/InvoiceRepository.java` ; `mapper/InvoiceMapper.java` ; `dto/InvoiceRDto.java`, `InvoiceLineRDto.java` ; `config/InvoiceProperties.java`
- `valimbavaka-core/src/main/resources/application.properties` (section Factures)

**Migrations Liquibase**
- `valimbavaka-core/src/main/resources/db/changelog/changes/2026_11_11-90-changelog.sql`, déclarée dans `db.changelog-master.yaml`

**Frontend** (`valimbavaka-ui/src/app/features/sales/`)
- `data-access/invoice-service.ts` (nouveau), `sales-models.ts`, `sales-mapping.ts` ; `components/invoice-panel/invoice-panel.ts`, `.html` ; `components/invoice-document/invoice-document.ts` (type) ; `sales.routes.ts` (commentaire)
- `valimbavaka-ui/public/i18n/fr.json`, `mg.json`

**Tests**
- Backend : `feature/invoice/controller/InvoiceIT.java`, `feature/invoice/domain/InvoiceTest.java`, `feature/invoice/service/impl/InvoiceServiceImplTest.java`, `InvoiceDraftsTest.java`, `SaleInvoiceListenerTest.java`, `feature/delivery/controller/DeliveryCompletionIT.java` (1 test ajouté)
- Frontend : `features/sales/data-access/invoice-service.spec.ts`, `sales-mapping.spec.ts`, `features/sales/components/invoice-panel/invoice-panel.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** :
- [VTE-01](../VTE-01/guide.md) : `SaleRecordedEvent`, `SaleService.getSaleById`, `sales.business_date` ; [VTE-05](../VTE-05/guide.md) : `Sale.paymentMethod` repris tel quel ; [VTE-07](../VTE-07/guide.md) : écran `/sales` et aperçu
- [LIV-03](../LIV-03/guide.md) : commande livrée = vente canal Livraison avec sa facture (décision PO du 07/11/2026) ; [LIV-05](../LIV-05/guide.md) : `SaleCancelledEvent` (non traité ici)
- [JNL-01](../JNL-01/guide.md) : `JournalService.record` ; [JNL-02](../JNL-02/guide.md) : `OperationRepository`, `OperationStatus`
- [SYS-05](../SYS-05/guide.md) (canaux), [AUTH-03](../AUTH-03/guide.md) (vendeuses), [ART-01](../ART-01/guide.md) (noms d'articles)

**Ces tickets s'appuient sur lui** :
- [FAC-02](../FAC-02/guide.md), [FAC-03](../FAC-03/guide.md), [FAC-04](../FAC-04/guide.md), [FAC-05](../FAC-05/guide.md), [FAC-06](../FAC-06/guide.md) (voir Règles métier)

## Comment l'utiliser depuis un autre ticket

Rien à appeler pour obtenir une facture : enregistrer la vente suffit.

```java
var sale = saleService.record(command);               // LIV-03, caisse… : la facture est créée dans la foulée
var invoice = invoiceService.getInvoiceBySaleId(sale.id()); // FAC-02 à FAC-06 : lire l'instantané
```

```ts
this.invoiceService.getBySale(sale.id); // Observable<InvoiceRDto> ; 404 = vente sans facture
```

## Points d'attention

- **Hypothèse (numéro)** : `FAC-<année>-<NNNN>`, compteur **par année de la journée ouverte**, sans trou ; l'ordre de numérotation est l'ordre de validation des transactions de vente. Si le métier veut un compteur unique sans année, c'est une migration du compteur (`year` fixe) et du format dans `InvoiceServiceImpl`.
- **Hypothèse (ventes antérieures)** : les ventes enregistrées avant ce ticket n'ont **pas** de facture (pas de rattrapage automatique, pour ne pas écrire une migration SQL non testée sur des données existantes) ; l'écran montre leur aperçu. À décider : rattrapage par FAC-02 (`issueForSale`) ou migration de reprise. Sans effet sur une base neuve.
- **Hypothèse (statut « Payée »)** : l'écran affiche « Payée » pour toute facture `VALIDATED` (la vente est encaissée) ; FAC-05 pourra le préciser.
- **Hypothèse (lecture)** : `sales.read` suffit (pas de permission `invoices.read`) ; une vendeuse reçoit 404 pour la facture d'une collègue. À faire valider par le PO.
- **Hypothèse (émetteur)** : « By Valimbavaka Cosmetics » et NIF par variable d'environnement `INVOICE_ISSUER_NIF` ; vide, la facture affiche « [à compléter] ». À renseigner en production (`.env`, `deploy/env.example` non modifiés pour ne pas entrer en conflit avec d'autres PR).
- **Non livré** (autres tickets) : régénération (FAC-02), impression / PDF / envoi (FAC-03), avoir et prise en compte de `SaleCancelledEvent` (FAC-04), statuts envoyée / payée (FAC-05), livre des ventes (FAC-06). La facture d'une vente annulée à chaud (LIV-05) reste `VALIDATED` d'ici FAC-04.
- **Tests d'intégration** : `invoices` n'a pas de clé étrangère vers `sales` : les tests qui vident `sales` (`BusinessDayIT`, `MorningTransferIT`, `SaleIT`, `DeliveryCompletionIT`) n'ont pas à vider `invoices`. `InvoiceIT.nextNumber_transactionRolledBack_givesTheNumberBack` utilise l'année 2099 et nettoie son compteur.
- Aucune ligne de `SaleServiceImpl`, `SaleLedger` ni de `features/pos` n'est modifiée (PRO-02 modifie le chemin d'écriture de la vente en parallèle) : l'événement existant suffit.
