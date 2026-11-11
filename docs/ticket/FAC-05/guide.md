# Guide d'intégration — FAC-05 · Suivi des statuts de facture

Statut : ✅ implémenté (tests à lancer, voir le scénario) · Ticket : [#75](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/75) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

L'administrateur voit, sur une période, le nombre et le montant des factures **payées** (`VALIDATED`), **contre-passées par un avoir** (`REVERSED`) et **annulées** (`CANCELLED`, ventes annulées avant FAC-04), avec le **total consolidé** qui est la somme des trois statuts. Un seul endpoint de lecture, aucune migration, aucune nouvelle permission (`reports.read`, déjà réservée à l'administrateur). L'écran `/sales/statuses` (bouton « Statuts des factures » sur `/sales`) l'affiche.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie (`XxxRDto`) | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/invoices/status-totals?from=&to=` (`yyyy-MM-dd`, facultatifs) | `reports.read` (ADMIN) | | 200 `InvoiceStatusTotalsRDto` | 400 (date mal formée), 401, 403, 422 (`from` après `to`) |

- `InvoiceStatusTotalsRDto` : `from`, `to` (jours ouvrés inclus, `invoices.business_date`), `invoiceCount`, `amount` (somme des statuts), `statuses` : liste fixe de trois `InvoiceStatusTotalRDto` (`status`, `invoiceCount`, `amount`) dans l'ordre `VALIDATED`, `CANCELLED`, `REVERSED`, **à 0 si aucune facture** (le front n'a pas à deviner les lignes manquantes).
- Sans dates : du 1er du mois au jour courant (fuseau `Indian/Antananarivo`), comme VTE-08.
- Lecture seule : ni corps, ni mass assignment, ni 404, ni 409. Rien n'est écrit au journal (une lecture n'est pas tracée).

### Services et ports Java réutilisables

- `feature.invoice.service.InvoiceStatusService.getStatusTotals(from, to)` (`InvoiceStatusServiceImpl.java:39`) : valide la période (422), lit l'agrégat, complète les statuts absents, additionne.
- `InvoiceRepository.totalsByStatus(from, to)` (`InvoiceRepository.java:38`) : `count` et `sum(total)` groupés par statut ; renvoie des `InvoiceStatusTotal` (record public du package `repository`). Réutilisable par FAC-06 pour un agrégat par jour.
- Contrôleur : `controller/InvoiceStatusController.java` (séparé de `InvoiceController`, qui reste centré sur une facture).

### Frontend (`features/sales`)

- `pages/invoice-status-page` (nouveau) : filtres Du / Au (mois en cours par défaut), deux cartes (montant facturé, nombre de factures), tableau par statut avec pied « Total ». Période à l'envers : message, rien n'est envoyé. Erreur : `vvs-list-state` avec « Réessayer ».
- `data-access/invoice-status-service.ts` (`get({ from, to })`) et `invoice-status-models.ts` (miroirs de `InvoiceStatusTotalsRDto`).
- Route `/sales/statuses` (`sales.routes.ts`, garde `permissionGuard('reports.read')`) ; lien sur `sales-page` visible avec `reports.read`.
- Traductions fr et mg : `sales.statusTotals.*` (libellés `status_VALIDATED` « Payée », `status_CANCELLED` « Annulée », `status_REVERSED` « Avoir »).

### Données

- Aucune migration. Journal : aucune ligne (lecture).

## Règles métier pour les autres tickets

- Un total « par statut » se lit toujours sur `invoices.status` (instantané) ; le chiffre d'affaires net (FAC-06) est celui de `VALIDATED` seulement : une facture `REVERSED` ou `CANCELLED` n'est plus du chiffre d'affaires.
- Le total consolidé inclut les trois statuts ; ce n'est donc pas le chiffre d'affaires. Le CA est la ligne « Payée ».
- Il n'y a pas de facture « impayée » : la facture naît de l'encaissement (FAC-01).

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/invoice/`)
- `controller/InvoiceStatusController.java` ; `service/InvoiceStatusService.java`, `service/impl/InvoiceStatusServiceImpl.java`
- `repository/InvoiceRepository.java` (requête ajoutée), `repository/InvoiceStatusTotal.java` ; `dto/InvoiceStatusTotalsRDto.java`, `dto/InvoiceStatusTotalRDto.java`

**Frontend** (`valimbavaka-ui/src/app/features/sales/`)
- `pages/invoice-status-page/*`, `data-access/invoice-status-service.ts`, `invoice-status-models.ts`, `sales.routes.ts`, `pages/sales-page/sales-page.ts` et `.html` ; `valimbavaka-ui/public/i18n/fr.json`, `mg.json`

**Tests**
- Backend : `feature/invoice/controller/InvoiceStatusIT.java`, `feature/invoice/service/impl/InvoiceStatusServiceImplTest.java`
- Frontend : `pages/invoice-status-page/invoice-status-page.spec.ts`, `data-access/invoice-status-service.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** : [FAC-01](../FAC-01/guide.md) (table `invoices`, `Invoice.status`), [FAC-04](../FAC-04/guide.md) (statut `REVERSED`), [VTE-08](../VTE-08/guide.md) (modèle de l'écran : période, `reports.read`).

**Ces tickets s'appuient sur lui** : aucun ; [FAC-06](../FAC-06/guide.md) peut réutiliser `totalsByStatus`.

## Comment l'utiliser

```ts
this.invoiceStatusService.get({ from: '2026-11-01', to: '2026-11-30' }); // Observable<InvoiceStatusTotalsRDto>
```

## Points d'attention

- **Hypothèse (statuts)** : la description cite « payée / impayée / avoir ». Le serveur ne produit que `VALIDATED` (payée), `REVERSED` (avoir) et `CANCELLED` ; « impayée » n'existe pas puisque la facture est émise à l'encaissement. Je n'ai pas inventé de statut « impayée » à 0.
- **Hypothèse (période)** : les totaux portent sur la **date de la journée de la vente** (`business_date`), pas sur la date du changement de statut : une facture créditée aujourd'hui pour une vente d'hier compte en « Avoir » sur la journée d'hier.
- **Hypothèse (permission)** : `reports.read` (administrateur) plutôt qu'une nouvelle permission, car la story vise l'administrateur et la permission des rapports existe ; la vendeuse reçoit 403.
- **Renvoi non repris : statut « envoyée »** : FAC-03 renvoyait à FAC-05 un éventuel statut « envoyée » (`sent_at`, `POST /api/v1/invoices/{id}/sent`). Le critère de FAC-05 ne le demande pas (totaux par statut payée / impayée / avoir) : non livré. À reprendre dans un ticket dédié si le PO le veut.
- Les ventes annulées à chaud avant FAC-04 dont la facture reste `VALIDATED` comptent encore en « Payée » tant que l'administrateur n'a pas émis leur avoir (voir le guide FAC-04).
- Base de test partagée : `InvoiceStatusIT` insère ses factures sur l'année 2001 (sans clé étrangère) et les supprime avant et après chaque test ; seul le test du flux vente puis avoir lit la journée courante par différence.
