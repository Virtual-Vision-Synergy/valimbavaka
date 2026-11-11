# Scénarios de test — FAC-02 · Régénération d'une facture existante

Statut : ✏️ rédigé le 2026-11-11, tests écrits, à lancer (CI ou local) · Ticket : [#72](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/72) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | vendeuse (SELLER : Miora, et une collègue Lova), administrateur (ADMIN), livreur (DELIVERY_DRIVER, refusé) |
| Données de départ | journée ouverte, article « Savon coco » à 8 500 Ar, 10 unités au stock Boutique ; vente de 3 unités payée par MVola → facture `FAC-2026-0042` de 25 500 Ar |
| Permission | `invoices.regenerate` (nouvelle, migration `2026_11_11-91-changelog.sql`) : ADMIN et SELLER |
| Tickets à avoir déployés avant | FAC-01, VTE-01, JNL-01, VTE-02 |

Hypothèses prises (à valider par le PO) : « régénérer » rend le **même document** (même numéro, même instantané) et ne crée jamais de nouveau numéro ; l'impression, le PDF et l'envoi restent à FAC-03. Une vente antérieure à FAC-01, sans facture, reçoit la sienne à la première régénération (rattrapage via `issueForSale`). La réédition est tracée au journal (`UPDATE`, libellé « Facture FAC-… régénérée · vente VTE-… »).

## Scénarios issus des critères d'acceptation

| # | Critère | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Une facture n'est générée qu'à partir d'une vente enregistrée, sans saisie libre | `POST /api/v1/invoices/sales/{saleId}/regenerate` sur une vente inconnue (UUID aléatoire) | 404 `NOT_FOUND`, aucune facture ni aucun numéro créé | `InvoiceIT.regenerateInvoice_unknownSale_returns404AndCreatesNoInvoice` ; `InvoiceServiceImplTest.regenerateForSale_unknownSale_returns404AndUsesNoNumber` | ☐ |
| 1b | idem : le corps ne peut pas fournir de lignes | corps `{"number":"FAC-1999-0001","total":1,"lines":[{"label":"Libre"}]}` | ignoré : numéro, total 8 500 et 1 ligne de la vente | `InvoiceIT.regenerateInvoice_bodyWithFreeLinesAndNumber_isIgnored` | ☐ |
| 1c | idem : facture d'une vente d'avant FAC-01 | vente sans facture (ligne supprimée en base), vendeuse Miora régénère | 200, facture émise avec le numéro suivant (compteur + 1), journal `CREATE` | `InvoiceIT.regenerateInvoice_saleWithoutInvoice_issuesItWithTheNextNumberAndTracesIt` ; `InvoiceServiceImplTest.regenerateForSale_saleWithoutInvoice_issuesItFromTheSaleAndTracesTheCreation` | ☐ |
| 1d | idem : deux régénérations d'une vente sans facture | deux POST à la suite | une seule facture, même réponse | `InvoiceIT.regenerateInvoice_twiceOnASaleWithoutInvoice_issuesOneInvoiceOnly` | ☐ |
| 2 | Le document reprend exactement lignes, montants et mode de paiement | vente ORANGE_MONEY, 2 × 8 500 Ar ; régénération | `paymentMethod`, `subtotal`, `discountAmount`, `total`, article, quantité et total de ligne égaux à ceux de `GET /api/v1/sales/{id}` | `InvoiceIT.regenerateInvoice_existingInvoice_repeatsTheLinesAmountsAndPaymentMethodOfTheSale` | ☐ |
| 2b | idem : même numéro et même contenu | vente MVola 3 × 8 500 ; régénération | réponse identique à `GET /invoices/sales/{id}` d'avant, compteur et nombre de factures inchangés | `InvoiceIT.regenerateInvoice_existingInvoice_returnsTheSameNumberAndContentWithoutUsingANumber` ; `InvoiceServiceImplTest.regenerateForSale_existingInvoice_returnsItUnchangedUsesNoNumberAndTracesTheReedition` | ☐ |
| 2c | idem : le contenu reste celui du jour de la vente | article renommé « Renommé » et prix 9 900 Ar après la vente | libellé d'origine et 8 500 Ar | `InvoiceIT.regenerateInvoice_afterAnArticleRenameAndAPriceChange_keepsTheIssuedContent` | ☐ |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | POST sans en-tête `Authorization` | 401 `UNAUTHENTICATED` | `InvoiceIT.regenerateInvoice_withoutToken_returns401` | ☐ |
| Mauvais rôle | jeton livreur | 403 `FORBIDDEN` | `InvoiceIT.regenerateInvoice_deliveryDriver_returns403` | ☐ |
| Champ glissé dans le corps | `number`, `total`, `lines` | ignorés (aucun DTO d'entrée) | `InvoiceIT.regenerateInvoice_bodyWithFreeLinesAndNumber_isIgnored` | ☐ |
| Identifiant mal formé | `/sales/pas-un-uuid/regenerate` | 400 `MALFORMED_REQUEST` | `InvoiceIT.regenerateInvoice_malformedSaleId_returns400` | ☐ |
| Vente inconnue | UUID aléatoire, jeton ADMIN | 404 `NOT_FOUND` | `InvoiceIT.regenerateInvoice_unknownSale_returns404AndCreatesNoInvoice` | ☐ |
| Vente d'une collègue (facture existante) | Lova régénère la facture de Miora | 404 ; l'administrateur obtient 200 | `InvoiceIT.regenerateInvoice_sellerAskingForAColleagueSale_returns404ButAdministratorRegenerates` ; `InvoiceServiceImplTest.regenerateForSale_existingInvoiceOfAColleague_returns404AndTracesNothing` | ☐ |
| Vente d'une collègue (sans facture) | Lova sur une vente de Miora sans facture | 404, aucune facture émise ni numéro pris | `InvoiceIT.regenerateInvoice_sellerAskingForAColleagueSaleWithoutInvoice_returns404AndIssuesNothing` ; `InvoiceServiceImplTest.regenerateForSale_saleOfAColleagueWithoutInvoice_returns404AndUsesNoNumber` | ☐ |
| Administrateur | ADMIN sur une vente sans facture | 200, facture émise | `InvoiceServiceImplTest.regenerateForSale_administratorOnASaleOfASeller_issuesItWhenMissing` | ☐ |
| 422 | sans objet : aucun corps | — | — | ⏭ sans objet |
| 409 | deux rattrapages simultanés de la même vente : la contrainte unique `uq_invoices_sale_id` refuse le second, le gestionnaire global répond 409 et le numéro est rendu (même transaction) | 409 `CONFLICT` | 🔍 code : `GlobalExceptionHandler.handleDataIntegrity` ; pas de test de concurrence (cas rare, ventes d'avant FAC-01 seulement) | 🔍 code |
| Traçabilité | deux régénérations (vendeuse puis administrateur) | 2 lignes journal `UPDATE` « Facture FAC-… régénérée · vente VTE-… » ; le rattrapage écrit `CREATE` | `InvoiceIT.regenerateInvoice_existingInvoice_tracesTheReeditionInTheJournal` | ☐ |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Régénérer plusieurs fois | 2 régénérations d'une facture existante | même numéro, même contenu, aucun numéro consommé, une ligne journal chacune | `InvoiceIT.regenerateInvoice_existingInvoice_tracesTheReeditionInTheJournal` | ☐ |
| Vente annulée / contre-passée | facture `CANCELLED` ou `REVERSED` | régénérée telle quelle (statut conservé) ; l'avoir relève de FAC-04 | ⏭ hors périmètre : FAC-04 | ⏭ |
| Hors ligne | pas de file : la régénération demande le serveur | échec signalé par un message, bouton réactivé | `invoice-panel.spec.ts` : « désactive le bouton pendant la régénération et signale l'échec » | ☐ |
| Double clic | bouton désactivé pendant l'appel | un seul POST | `invoice-panel.spec.ts` : « désactive le bouton pendant la régénération et signale l'échec » | ☐ |
| Vente sans facture à l'écran | 404 au chargement, puis clic sur « Régénérer » | l'aperçu « À émettre » est remplacé par la facture numérotée, la note « Aucune facture… » disparaît | `invoice-panel.spec.ts` : « émet la facture d'une vente qui n'en avait pas et remplace l'aperçu » | ☐ |
| Facture existante à l'écran | clic sur « Régénérer » | numéro inchangé, message « Facture FAC-2026-0042 régénérée… » | `invoice-panel.spec.ts` : « régénère la facture sans changer son numéro et prévient la vendeuse » | ☐ |
| Appel HTTP | `regenerate('vente-42')` | `POST /api/v1/invoices/sales/vente-42/regenerate`, corps `null` | `invoice-service.spec.ts` : « régénère la facture d'une vente par un POST sans corps » | ☐ |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/invoice/controller/InvoiceIT.java` (11 tests ajoutés `regenerateInvoice_*`), `.../feature/invoice/service/impl/InvoiceServiceImplTest.java` (6 tests ajoutés `regenerateForSale_*`).
- Frontend : `valimbavaka-ui/src/app/features/sales/components/invoice-panel/invoice-panel.spec.ts` (3 tests ajoutés), `.../data-access/invoice-service.spec.ts` (1 test ajouté).

## Recette manuelle (🧪)

| Étape | Résultat attendu | Statut |
|---|---|---|
| Écran `/sales`, vendeuse, vente du jour : bouton « Régénérer » | message « Facture FAC-… régénérée », numéro et lignes inchangés | 🧪 |
| Écran `/sales`, vente d'avant FAC-01 : « Régénérer » | la facture numérotée remplace l'aperçu | 🧪 |
| Vendeuse sur la vente d'une collègue | message d'échec, rien n'est émis | 🧪 |

## Résultat de l'exécution

- Date : 2026-11-11 (rédaction) ; tests à lancer (CI ou local).
- Environnement : `npx tsc` (app et spec) et `eslint` sans erreur sur les fichiers touchés ; backend non compilé localement (Maven Central inaccessible) : la CI compile.
- Anomalies ouvertes : aucune connue.
