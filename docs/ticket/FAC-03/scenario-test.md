# Scénarios de test — FAC-03 · Impression / export / envoi de facture

Statut : ☐ tests automatiques écrits le 2026-11-11, à lancer (CI ou local) ; recette 🧪 à passer · Ticket : [#73](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/73) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | vendeuse (SELLER : Miora, et une collègue Lova), administrateur (ADMIN), livreur (DELIVERY_DRIVER, refusé) |
| Données de départ | journée ouverte, article « Savon coco » à 8 500 Ar, 10 unités au stock Boutique ; vente de 3 unités payée par MVola → facture `FAC-2026-0042` de 25 500 Ar |
| Permission | aucune nouvelle : `sales.read` (admin, vendeuse), comme la lecture de la facture ; aucune migration |
| Tickets à avoir déployés avant | FAC-01, FAC-02, SYS-01 (`PdfExporter`) |

Hypothèses prises (à valider par le PO, « envoi » sans serveur de messagerie ni fournisseur WhatsApp dans le projet) :
- **Imprimer** : impression du navigateur (déjà présente), confirmée par un message.
- **PDF** (« export ») : fichier produit par le serveur à partir de l'instantané de la facture, jamais recalculé.
- **Envoyer** : aucun envoi serveur (pas de SMTP). Le navigateur partage le PDF par le menu du système quand il sait partager un fichier (téléphone : WhatsApp, courriel…) ; sinon il enregistre le PDF et ouvre un message `mailto:` prérempli, sans destinataire (l'adresse du client n'est pas connue), que la vendeuse complète et envoie elle-même en joignant le PDF.
- Aucune trace au journal et aucun statut « envoyée » (FAC-05).

## Scénarios issus des critères d'acceptation

| # | Critère | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Les 3 actions sont disponibles et confirment leur exécution : Imprimer | clic sur « Imprimer » | impression du navigateur lancée, message « Impression de la facture … lancée. » | `invoice-panel.spec.ts` : « imprime la facture et confirme l'impression » | ☐ à lancer |
| 2 | idem : PDF | clic sur « PDF » sur la facture `FAC-2026-0042` | `GET /api/v1/invoices/{id}/pdf` (blob), fichier `FAC-2026-0042.pdf` enregistré, message « PDF de la facture … enregistré. » | `invoice-panel.spec.ts` : « enregistre le PDF de la facture émise sous le nom de son numéro et le confirme » | ☐ à lancer |
| 2b | idem : contenu du PDF | vente MVola 3 × 8 500 Ar, vendeuse Miora | PDF `application/pdf`, `Content-Disposition: attachment` avec `FAC-….pdf`, contient « Facture FAC-… », l'émetteur, « règlement : MVola », « Vendeuse : Miora Rakoto », « Total à payer : 25 500 Ar » | `InvoiceIT.getInvoicePdf_issuedInvoice_returnsAPdfNamedAfterTheNumberWithTheFrozenContent` ; `InvoiceDocumentServiceImplTest.renderPdf_issuedInvoice_isNamedAfterItsNumberAndPrintsTheFrozenContent` | ☐ à lancer |
| 2c | idem : remise et totaux | sous-total 25 500, remise 2 550 | lignes « Sous-total : 25 500 Ar », « Remise : -2 550 Ar », « Total à payer : 22 950 Ar » ; sans remise, pas de ligne Remise | `InvoiceDocumentServiceImplTest.renderPdf_invoiceWithADiscount_printsSubtotalDiscountAndTotal` | ☐ à lancer |
| 2d | idem : le PDF reste celui du jour de la vente | article renommé « Renommé » et prix 9 900 Ar après la vente | libellé d'origine et 17 000 Ar pour 2 unités | `InvoiceIT.getInvoicePdf_afterAnArticleRenameAndAPriceChange_keepsTheIssuedContent` | ☐ à lancer |
| 3 | idem : Envoyer, partage de fichier disponible | clic sur « Envoyer » (navigateur de téléphone) | menu de partage du système avec le PDF ; message « Facture … partagée. » | `invoice-panel.spec.ts` : « partage le PDF par le système quand le navigateur sait le faire » | ☐ à lancer |
| 3b | idem : Envoyer, sans partage de fichier (ordinateur) | clic sur « Envoyer » | PDF enregistré, `mailto:` prérempli (objet « Facture FAC-2026-0042 · By Valimbavaka Cosmetics », corps avec numéro, date, montant), message « …joignez-le au message… » | `invoice-panel.spec.ts` : « ouvre un message prérempli et enregistre le PDF à joindre quand le partage n'existe pas » | ☐ à lancer |
| 3c | idem : abandon du menu de partage | la vendeuse ferme le menu | aucun message de confirmation (rien n'a été envoyé) | `invoice-panel.spec.ts` : « ne confirme rien quand la vendeuse abandonne le menu de partage » | ☐ à lancer |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `GET /api/v1/invoices/{id}/pdf` sans `Authorization` | 401 `UNAUTHENTICATED` | `InvoiceIT.getInvoicePdf_withoutToken_returns401` | ☐ à lancer |
| Mauvais rôle | jeton livreur | 403 `FORBIDDEN` | `InvoiceIT.getInvoicePdf_deliveryDriver_returns403` | ☐ à lancer |
| Identifiant mal formé | `/invoices/pas-un-uuid/pdf` | 400 `MALFORMED_REQUEST` | `InvoiceIT.getInvoicePdf_malformedId_returns400` | ☐ à lancer |
| Facture inconnue | UUID aléatoire, jeton ADMIN | 404 `NOT_FOUND` | `InvoiceIT.getInvoicePdf_unknownInvoice_returns404` ; `InvoiceDocumentServiceImplTest.renderPdf_unknownInvoice_propagatesNotFound` | ☐ à lancer |
| Facture d'une collègue | Lova demande le PDF de la facture de Miora | 404 ; l'administrateur obtient 200 | `InvoiceIT.getInvoicePdf_sellerAskingForAColleagueInvoice_returns404ButAdministratorReadsIt` | ☐ à lancer |
| Mass assignment | sans objet : aucun corps | — | — | ⏭ sans objet |
| 422 / 409 | sans objet : lecture seule | — | — | ⏭ sans objet |
| Ne modifie rien | PDF téléchargé deux fois (vendeuse puis administrateur) | aucun numéro consommé, aucune facture créée | `InvoiceIT.getInvoicePdf_downloadedTwice_changesNothingAndUsesNoNumber` | ☐ à lancer |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Facture sans NIF ni vendeuse | NIF non configuré, vente de commande livrée | « NIF : [à compléter] » et « Vendeuse : - » | `InvoiceDocumentServiceImplTest.renderPdf_nifNotConfiguredAndNoSeller_printsPlaceholders` | ☐ à lancer |
| Facture annulée | statut `CANCELLED` | ligne « Statut : annulée » (avoir : FAC-04) | `InvoiceDocumentServiceImplTest.renderPdf_cancelledInvoice_printsItsStatus` | ☐ à lancer |
| Date d'émission | émise à 07:52 UTC | affichée 10:52 (heure de la boutique) | `InvoiceDocumentServiceImplTest.renderPdf_issuedInvoice_isNamedAfterItsNumberAndPrintsTheFrozenContent` | ☐ à lancer |
| Vente sans facture à l'écran | 404 au chargement : l'aperçu « À émettre » est affiché | PDF et Envoyer désactivés avec une note renvoyant à « Régénérer » ; Imprimer reste actif | `invoice-panel.spec.ts` : « désactive PDF et Envoyer tant que la vente n'a pas de facture émise » | ☐ à lancer |
| PDF en erreur / hors ligne | serveur injoignable ou 404 | message d'échec, rien n'est enregistré, bouton réactivé | `invoice-panel.spec.ts` : « signale l'échec du PDF sans rien enregistrer » | ☐ à lancer |
| Hors ligne | pas de file : le PDF vient du serveur ; seule l'impression de l'écran fonctionne sans réseau | échec signalé par un message | `invoice-panel.spec.ts` : « signale l'échec du PDF sans rien enregistrer » | ☐ à lancer |
| Double clic | boutons PDF et Envoyer désactivés pendant l'appel | un seul GET | 🔍 code : `InvoicePanel.working` | 🔍 code |
| Appel HTTP | `downloadPdf('facture-1')` | `GET /api/v1/invoices/facture-1/pdf`, `responseType` blob | `invoice-service.spec.ts` : « télécharge le PDF d'une facture en blob par un GET » | ☐ à lancer |
| Fonctions du navigateur | téléchargement, partage, `mailto:` | lien nommé puis adresse libérée ; partage refusé sans `navigator.canShare` ; fichier, titre et texte transmis à `navigator.share` | `invoice-delivery.spec.ts` (4 tests) | ☐ à lancer |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/invoice/controller/InvoiceIT.java` (8 tests ajoutés `getInvoicePdf_*`), `.../feature/invoice/service/impl/InvoiceDocumentServiceImplTest.java` (5 tests, nouveau).
- Frontend : `valimbavaka-ui/src/app/features/sales/components/invoice-panel/invoice-panel.spec.ts` (7 tests ajoutés), `.../data-access/invoice-service.spec.ts` (1 test ajouté), `.../data-access/invoice-delivery.spec.ts` (4 tests, nouveau).

## Recette manuelle (🧪)

| Étape | Résultat attendu | Statut |
|---|---|---|
| Écran `/sales`, vendeuse, vente du jour : « PDF » | fichier `FAC-….pdf` enregistré ; ouvrir : émetteur, numéro, lignes, total corrects et lisibles (accents) | 🧪 |
| « Imprimer » | boîte d'impression avec le seul document de la facture ; message de confirmation | 🧪 |
| « Envoyer » sur un téléphone | menu de partage avec le PDF joint | 🧪 |
| « Envoyer » sur ordinateur | PDF enregistré, messagerie ouverte avec objet et texte préremplis | 🧪 |
| Vente d'avant FAC-01 (aperçu) | PDF et Envoyer grisés ; « Régénérer » puis PDF disponible | 🧪 |

## Résultat de l'exécution

- Date : à lancer (CI ou local)
- Environnement : —
- Anomalies ouvertes : aucune connue
