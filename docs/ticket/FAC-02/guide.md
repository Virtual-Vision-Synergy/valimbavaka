# Guide d'intégration — FAC-02 · Régénération d'une facture existante

Statut : ✅ écrit le 2026-11-11 · Ticket : [#72](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/72) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Un seul endpoint d'écriture est ajouté à `feature/invoice` : `POST /api/v1/invoices/sales/{saleId}/regenerate`, sans corps. Il part toujours d'une **vente enregistrée** et rend la **même facture** (même numéro, même instantané : lignes, montants, mode de règlement), sans consommer de numéro ; la réédition est tracée au journal. Une vente antérieure à FAC-01, sans facture, reçoit la sienne à cette occasion (rattrapage). Il est donc impossible de créer une facture hors caisse ou avec des lignes libres. Le bouton « Régénérer » de l'écran `/sales` est branché dessus. Le PDF, l'impression et l'envoi restent à FAC-03.

## Contrat exposé

### Endpoint backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| POST | `/api/v1/invoices/sales/{saleId}/regenerate` | `invoices.regenerate` (admin, vendeuse) | aucune (corps ignoré) | 200 `InvoiceRDto` | 400 (UUID mal formé), 401, 403, 404 |

- `InvoiceController.regenerateForSale` (`InvoiceController.java:62`) : 2 lignes, aucune logique.
- Vendeuse : seulement ses ventes (facture ou vente d'une collègue, ou sans vendeuse : **404**) ; l'administrateur régénère tout.
- Pas de 422 (aucun corps), pas de 409 en usage normal (voir Points d'attention).
- Même `InvoiceRDto` que FAC-01 ; la réponse est identique à `GET /api/v1/invoices/sales/{saleId}` tant que la facture existe déjà.

### Services Java réutilisables

| Port | Méthode | Rôle |
|---|---|---|
| `feature.invoice.service.InvoiceService` | `InvoiceRDto regenerateForSale(UUID saleId)` (`InvoiceServiceImpl.java:69`) | Facture existante : la rend inchangée (règle de lecture vendeuse), journal `UPDATE` « Facture FAC-2026-0042 régénérée · vente VTE-0042 ». Sans facture : contrôle la vente (`ensureSaleReadable`, 404 si inconnue ou d'une collègue) puis l'émet comme `issueForSale` (numéro suivant, journal `CREATE`). |
| idem | `issueForSale(UUID)` | Inchangée pour l'appelant ; le corps d'émission est désormais la méthode privée `issue(SaleRDto)` (`InvoiceServiceImpl.java:86`), partagée avec la régénération. |

### Frontend (`features/sales`)

- `data-access/invoice-service.ts` : `regenerate(saleId)` (POST sans corps).
- `components/invoice-panel/invoice-panel.ts` : `regenerate()` (bouton « Régénérer », désactivé pendant l'appel). Succès : la ressource `issued` est remplacée par la facture reçue (l'aperçu « À émettre » d'une vente sans facture devient la facture numérotée) et un message « Facture … régénérée » s'affiche ; échec : message d'erreur du serveur. Pas de file hors ligne (action de lecture/édition qui exige le serveur).
- Traductions fr et mg : `sales.invoice.regenerated`, `sales.invoice.regenerateFailed`. Le repère « FAC-03 → FAC-05 » remplace « FAC-02 → FAC-05 » sur le panneau.

### Données

- Migration `2026_11_11-91-changelog.sql` (2 changesets, rollback fourni) : permission `invoices.regenerate` donnée aux rôles `ADMIN` et `SELLER`. Aucune table ni colonne nouvelle.
- Journal : ressource `INVOICE`, action `UPDATE` pour une réédition (libellé « Facture FAC-… régénérée · vente VTE-… »), `CREATE` pour le rattrapage.

## Règles métier pour les autres tickets

- **Régénérer ne crée jamais de nouveau numéro** et ne modifie pas l'instantané. Une facture n'a toujours qu'un seul numéro et une seule vente (`uq_invoices_sale_id`).
- **FAC-03 (impression / PDF / envoi)** : produire le document à partir de `InvoiceRDto` (ou de `regenerateForSale` si l'on veut tracer une réédition) ; ne jamais recalculer depuis la vente ni les fiches.
- **FAC-04 (avoir)** : `regenerateForSale` rend la facture dans son état courant (`VALIDATED`, `CANCELLED` ou `REVERSED`) sans le changer ; l'avoir aura sa propre numérotation.
- **Droits** : réutiliser `invoices.regenerate` pour toute action « réémettre un document » ; les lectures restent sous `sales.read`.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/invoice/`)
- `controller/InvoiceController.java`, `service/InvoiceService.java`, `service/impl/InvoiceServiceImpl.java`

**Migrations Liquibase**
- `valimbavaka-core/src/main/resources/db/changelog/changes/2026_11_11-91-changelog.sql`, déclarée dans `db.changelog-master.yaml`

**Frontend** (`valimbavaka-ui/src/app/features/sales/`)
- `data-access/invoice-service.ts`, `components/invoice-panel/invoice-panel.ts`, `.html` ; `valimbavaka-ui/public/i18n/fr.json`, `mg.json`

**Tests**
- Backend : `feature/invoice/controller/InvoiceIT.java` (`regenerateInvoice_*`), `feature/invoice/service/impl/InvoiceServiceImplTest.java` (`regenerateForSale_*`)
- Frontend : `invoice-panel.spec.ts`, `invoice-service.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** : [FAC-01](../FAC-01/guide.md) (`issueForSale`, instantané, `findBySaleId`), [VTE-01](../VTE-01/guide.md) (vente enregistrée, `SaleService.getSaleById`), [JNL-01](../JNL-01/guide.md) (`JournalService.record`), [VTE-02](../VTE-02/guide.md) (poste de caisse : seule source de ventes).

**Ces tickets s'appuient sur lui** : aucun ; [FAC-03](../FAC-03/guide.md) peut s'en servir pour tracer une réédition.

## Comment l'utiliser

```ts
this.invoiceService.regenerate(sale.id); // Observable<InvoiceRDto> : même numéro, même contenu ; 404 si la vente n'est pas la sienne
```

## Points d'attention

- **Hypothèse (sens de « régénérer »)** : rendre le même document sous le même numéro, jamais un nouveau numéro ni un nouvel instantané. Le PDF/impression/envoi (FAC-03) produira le fichier ; ici on retrouve et trace la facture. À valider par le PO.
- **Hypothèse (rattrapage)** : traité ici car la description exige « une facture seulement à partir d'une vente enregistrée » et FAC-01 l'avait laissé à FAC-02 : la première régénération d'une vente d'avant FAC-01 émet sa facture (numéro de l'année de sa journée, ordre de rattrapage et non de vente). Pas de migration de reprise.
- **Hypothèse (permission)** : nouvelle permission `invoices.regenerate` (admin, vendeuse) plutôt que `sales.read` (lecture) ou `sales.create` (enregistrer une vente).
- **Hypothèse (journal)** : la réédition est tracée en `UPDATE` faute d'action dédiée (une action `REGENERATE` demanderait de modifier la contrainte `ck_journal_entries_action` et l'écran Journal) ; le libellé la distingue. L'instantané n'est pas modifié.
- **Concurrence** : deux rattrapages simultanés de la même vente : le second échoue sur `uq_invoices_sale_id`, 409 par `GlobalExceptionHandler`, et son numéro est rendu (même transaction). Non testé (cas limité aux ventes d'avant FAC-01).
- **Hors ligne** : aucune file ; la régénération exige le serveur.
- **Non livré** : impression, PDF, envoi (FAC-03), avoir (FAC-04), statuts (FAC-05), livre des ventes (FAC-06) ; les boutons correspondants restent des démonstrations.
