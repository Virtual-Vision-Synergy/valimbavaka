# Guide d'intégration — FAC-04 · Émission d'un avoir motivé

Statut : ✅ implémenté, CI verte · Ticket : [#74](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/74) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

L'administrateur contre-passe une facture émise par un avoir motivé (`AV-<année>-NNNN`), sans jamais la supprimer ni la modifier, sauf son statut qui passe à `REVERSED`. L'avoir annule la vente par `SaleService.cancel` (stock `RETURN`, commission annulée, écritures défaites) dans la même transaction. Toute vente annulée ailleurs (annulation d'une livraison, LIV-05) reçoit aussi son avoir automatiquement.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| POST | `/api/v1/invoices/{invoiceId}/credit-note` | `invoices.credit` (ADMIN) | `CreditNoteWDto` : `reason` (obligatoire, ≤ 500) | `CreditNoteRDto`, 201 | 400, 401, 403, 404, 409 (déjà créditée, journée close), 422 |
| GET | `/api/v1/invoices/{invoiceId}/credit-note` | `sales.read` (mêmes règles de lecture que la facture) | | `CreditNoteRDto` | 401, 403, 404 |

### Services et ports Java réutilisables

- `feature.invoice.service.CreditNoteService` : `issue(invoiceId, reason)`, `issueForCancelledSale(saleId)` (sans effet si pas de facture ou déjà créditée), `getCreditNoteByInvoiceId`.
- `SaleCancelledListener` écoute `SaleCancelledEvent` (synchrone, même transaction) : la feature `sale` ne connaît pas `invoice`. Une erreur d'avoir annule l'annulation de la vente.
- `Invoice.reverse()` : 409 sauf si `VALIDATED`.

### Frontend (`features/sales`)

- `data-access/credit-note-service.ts` (`issue`, `getByInvoice`), `credit-note-models.ts`.
- `components/credit-note-dialog` (motif obligatoire, `<dialog>` modal), `components/credit-note-notice` (numéro, date, motif de la facture créditée).
- `invoice-panel` : bouton « Avoir » ouvre le dialogue ; sortie `credited` rechargée par `sales-page`. Traductions fr et mg : `sales.credit.*`.

### Données

- Migrations `2026_11_11-94-changelog.sql` (`credit_note_counters`, `credit_notes`, `credit_note_lines`) et `2026_11_11-95-changelog.sql` (permission `invoices.credit`, rôle ADMIN).
- Journal : action `REVERSE`, ressource `INVOICE`, « Avoir AV-… de la facture FAC-… · vente VTE-… · total Ar : motif ».

## Règles métier à respecter par les autres tickets

- Un seul avoir par facture (contrainte unique) ; avoir total uniquement (pas d'avoir partiel).
- Numéro : compteur par année incrémenté par un `INSERT … ON CONFLICT … RETURNING` dans la transaction de l'avoir (modèle FAC-01) : sans trou, sûr en concurrence, numéro rendu si la transaction échoue.
- Facture `REVERSED` : l'écran l'affiche « avoir » (`CREDITED`). FAC-05 (statuts) et FAC-06 (livre des ventes) doivent traiter `REVERSED`.
- L'avoir est lui-même immuable (aucun PUT/DELETE).

## Fichiers concernés

`valimbavaka-core/.../feature/invoice/` : `controller/CreditNoteController`, `domain/CreditNote`, `service/impl/CreditNoteServiceImpl`, `service/impl/SaleCancelledListener`, `repository/CreditNoteRepository` (`nextNumber`), `mapper/CreditNoteMapper`, dto `CreditNoteRDto`/`CreditNoteWDto`. Front : composants et data-access cités ci-dessus.

## Dépendances

**Ce ticket s'appuie sur** :
- [FAC-01](../FAC-01/description.md) · [guide](../FAC-01/guide.md)
- [FAC-02](../FAC-02/description.md) · [guide](../FAC-02/guide.md)
- [LIV-05](../LIV-05/description.md) · [guide](../LIV-05/guide.md)
- [JNL-02](../JNL-02/description.md) · [JNL-01](../JNL-01/description.md)

**Ces tickets s'appuient sur lui** : FAC-05, FAC-06 (statut `REVERSED`).

## Points d'attention

- Hypothèse : l'avoir est total et réservé à l'administrateur ; seul le statut de la facture évolue.
- Hypothèse : le motif d'un avoir automatique (annulation de livraison) est « Annulation de la vente VTE-… ».
- Les ventes annulées à chaud avant FAC-04 dont la facture reste `VALIDATED` : l'administrateur peut émettre leur avoir ; le stock n'est pas rendu une seconde fois.
- Journée de la vente clôturée : 409, rien n'est émis.
- CI verte sur 72a8d47 (backend 807 tests, Qodana 0 annotation) ; aucun test lancé en local.
