# Guide d'intégration — FAC-03 · Impression / export / envoi de facture

Statut : ✅ écrit le 2026-11-11 · Ticket : [#73](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/73) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Le panneau facture de `/sales` offre trois actions réelles. **Imprimer** : impression du navigateur. **PDF** : un nouvel endpoint de lecture `GET /api/v1/invoices/{id}/pdf` produit le PDF de la facture **telle qu'émise** (instantané FAC-01, jamais recalculé) avec l'export PDF générique de SYS-01 (`PdfExporter`, PDFBox déjà au `pom.xml` : aucune dépendance ajoutée). **Envoyer** : le projet n'a ni serveur SMTP ni fournisseur WhatsApp, donc rien n'est envoyé par le serveur ; le navigateur partage le PDF par le menu du système quand il le peut (téléphone), sinon enregistre le PDF et ouvre un `mailto:` prérempli. Chaque action confirme son exécution par un message.

## Contrat exposé

### Endpoint backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/invoices/{id}/pdf` | `sales.read` (admin, vendeuse) | — | 200 `application/pdf`, `Content-Disposition: attachment; filename="FAC-2026-0042.pdf"` | 400 (UUID mal formé), 401, 403, 404 |

- `InvoiceController.getInvoicePdf` (`InvoiceController.java:64`) : 1 ligne, délègue à `InvoiceDocumentService`; l'en-tête est posé par `download` (`:84`).
- Vendeuse : seulement les factures de ses ventes (celle d'une collègue ou d'une vente sans vendeuse : **404**, règle FAC-01) ; l'administrateur lit tout.
- Lecture seule : ni numéro consommé, ni facture modifiée, ni ligne au journal.

### Services Java réutilisables

| Port | Méthode | Rôle |
|---|---|---|
| `feature.invoice.service.InvoiceDocumentService` | `InvoiceFile renderPdf(UUID invoiceId)` (`InvoiceDocumentServiceImpl.java:33`) | Lit la facture par `InvoiceService.getInvoiceById` (donc la règle de lecture vendeuse et le 404), construit le rapport (`InvoicePdfLayout`) et le fait écrire par `PdfExporter`. Rend `InvoiceFile(filename, content)`, fichier `<numéro>.pdf`. |

- Contenu du PDF : titre « Facture FAC-… » ; lignes d'en-tête (émetteur et NIF, date d'émission à l'heure de la boutique, journée, numéro de vente, vendeuse, canal, mode de règlement, statut si annulée ou contre-passée) ; tableau des lignes (colonnes `@ExcelColumn` portées par `InvoiceLineRDto` : désignation, qté, prix unitaire, total) ; sous-total, remise (si non nulle) et total. Mise en page A4 **paysage** et pied de page « généré le … » imposés par `PdfExporter` (SYS-01).
- Textes et formats : `service/impl/InvoicePdfLayout.java` (français, `jj/mm/aaaa`, montants « 25 500 Ar », fuseau `vvs.excel.time-zone`).

### Frontend (`features/sales`)

- `data-access/invoice-service.ts` : `downloadPdf(invoiceId)` (GET en `blob`).
- `data-access/invoice-delivery.ts` (nouveau) : `save` (téléchargement), `canShare` / `share` (Web Share API avec fichier), `openMail` (`mailto:` sans destinataire) ; isolé pour les tests.
- `components/invoice-panel/invoice-panel.ts` : `print` (`:62`), `downloadPdf` (`:70`), `send` (`:78`), `fetchPdf` (`:101`, commun, signal `working` contre le double clic). **PDF et Envoyer sont désactivés tant que la vente n'a pas de facture émise** (aperçu « À émettre ») ; « Régénérer » (FAC-02) l'émet.
- Traductions fr et mg : `sales.invoice.printed`, `pdfSaved`, `pdfFailed`, `shared`, `mailOpened`, `mailSubject`, `mailBody`, `pdfNeedsInvoice`. Le repère du panneau devient « FAC-04 → FAC-05 ».

### Données

- Aucune migration, aucune permission nouvelle (`sales.read`).
- Journal : rien (voir Points d'attention).

## Règles métier pour les autres tickets

- **FAC-04 (avoir)** : le PDF d'une facture annulée ou contre-passée porte la ligne « Statut : … » ; un avoir aura son propre document (réutiliser `InvoicePdfLayout` / `PdfExporter`, ajouter un type de document).
- **FAC-05 (statuts)** : « envoyée » n'existe pas : ni l'impression ni le partage navigateur ne sont connus du serveur. Si le PO veut ce statut, il faut un appel serveur explicite (par exemple `POST /api/v1/invoices/{id}/sent`, colonne `sent_at`) déclenché après le partage réussi (`InvoicePanel.send`, branches « partagée » et « mailOpened »).
- **FAC-06 (livre des ventes)** : le PDF d'une facture et l'export du livre des ventes partagent `PdfExporter` ; ne pas dupliquer l'écriture PDF.
- Ne jamais produire un document de facture depuis la vente ou les fiches : lire `InvoiceRDto`.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/invoice/`)
- `controller/InvoiceController.java` ; `service/InvoiceDocumentService.java`, `service/InvoiceFile.java` ; `service/impl/InvoiceDocumentServiceImpl.java`, `InvoicePdfLayout.java` ; `dto/InvoiceLineRDto.java` (`@ExcelColumn`)

**Frontend** (`valimbavaka-ui/src/app/features/sales/`)
- `data-access/invoice-service.ts`, `invoice-delivery.ts` (nouveau) ; `components/invoice-panel/invoice-panel.ts`, `.html` ; `valimbavaka-ui/public/i18n/fr.json`, `mg.json`

**Tests**
- Backend : `feature/invoice/controller/InvoiceIT.java` (`getInvoicePdf_*`), `feature/invoice/service/impl/InvoiceDocumentServiceImplTest.java`
- Frontend : `invoice-panel.spec.ts`, `invoice-service.spec.ts`, `invoice-delivery.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** : [FAC-01](../FAC-01/guide.md) (instantané, `getInvoiceById`, règle de lecture vendeuse), [FAC-02](../FAC-02/guide.md) (« Régénérer » pour émettre la facture d'une vente d'avant FAC-01), [SYS-01](../SYS-01/guide.md) (`PdfExporter`, annotation `@ExcelColumn`).

**Ces tickets s'appuient sur lui** : aucun (voir Règles métier pour FAC-04 à FAC-06).

## Comment l'utiliser

```ts
this.invoiceService.downloadPdf(invoice.id); // Observable<Blob> : le PDF tel qu'émis ; 404 si la facture n'est pas la sienne
```

## Points d'attention

- **Hypothèse (« envoi »)** : l'infrastructure d'envoi n'existe pas (pas de `spring-boot-starter-mail`, pas de SMTP, pas de WhatsApp Business) et le projet n'a pas d'adresse ou de téléphone client. Livré à la place : partage système du PDF, ou `mailto:` sans destinataire avec PDF à joindre à la main. **Écart assumé par rapport à un vrai envoi** : à valider par le PO ; un envoi serveur demanderait un SMTP, l'adresse du client sur la vente et une dépendance (non ajoutée ici, Maven Central étant inaccessible à l'écriture).
- **Hypothèse (« export »)** : l'export est le PDF de la facture ; pas d'export xlsx d'une facture (non demandé).
- **Hypothèse (permission)** : `sales.read`, comme la lecture de la facture, plutôt qu'une permission dédiée ; pas de migration.
- **Hypothèse (traçabilité)** : aucune ligne de journal ni statut « envoyée » : le serveur ne sait pas ce que le navigateur a imprimé ou partagé, et FAC-05 traite les statuts. Les lectures du PDF ne sont pas tracées.
- **Impression** : `window.print()` imprime la page ; la feuille de style du panneau masque déjà les boutons et notes (`invoice-panel.css`, `@media print`). Elle n'utilise pas le PDF serveur.
- **Mise en page** : le PDF hérite du gabarit tabulaire A4 paysage de SYS-01 ; un gabarit de facture portrait (logo, cachet) serait un ticket de présentation à part.
- **Navigateurs** : le partage de fichier (`navigator.share` avec fichier) existe surtout sur téléphone et exige une page en HTTPS ; ailleurs, repli sur téléchargement et `mailto:`.
- **Tests à lancer** : `InvoiceIT`, `InvoiceDocumentServiceImplTest`, `ng test` sur `features/sales` ; recette 🧪 du scénario.
