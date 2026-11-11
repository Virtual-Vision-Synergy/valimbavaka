# Scénarios de test — VTE-05 · Choix du mode de règlement

Statut : ✅ rédigé le 2026-11-11 · Ticket : [#66](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/66) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

Le choix du mode et son enregistrement avec la vente sont **déjà livrés** par VTE-01 (`sales.payment_method`, `SaleWDto.paymentMethod`) et VTE-02 (sélecteur du ticket). Ce ticket ne change aucun code de production : il **verrouille le comportement par des tests** et documente le contrat. La facture n'existe pas encore (FAC-01) : elle est dérivée de la vente, donc du même mode (voir « Facture » ci-dessous).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | Vendeuse `SELLER` (encaissement `sales.create`, lecture `sales.read`) ; administrateur `ADMIN` en lecture ; `DELIVERY_DRIVER` pour le 403 |
| Données de départ | Journée du jour ouverte (JR-01) ; canal Boutique ; article à 8 500 Ar en stock ; vendeuse active avec canal et taux de commission |
| Tickets à avoir déployés avant | VTE-01, VTE-02, VTE-07 (déjà dans `develop`) |

## Scénarios issus des critères d'acceptation

| # | Critère d'acceptation | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Le mode choisi est enregistré avec la vente et la facture. | Vente d'un article sur le canal Boutique, une fois pour chaque mode `CASH`, `MVOLA`, `ORANGE_MONEY`, `CARD`, `ON_DELIVERY` | 201 ; `sales.payment_method` = le mode envoyé ; `GET /api/v1/sales/{id}` renvoie le même `paymentMethod` | `SaleIT.createSale_eachPaymentMethod_isStoredWithTheSaleAndReturnedByGetById` | ☐ |
| 1b | idem (déjà couvert) | Vente en espèces | `paymentMethod` = `CASH` dans la réponse ; libellé du mode dans l'écriture du journal | `SaleIT.createSale_storeChannel_returns201AndTakesTheStockOutOfTheStore`, `SaleLedgerTest` | ☐ |
| 1c | idem (liste des ventes) | Ventes en `CASH`, `MVOLA`, `CARD` | `GET /api/v1/sales` renvoie le mode de chaque vente | `SaleQueryIT` (`paymentMethod` CASH, MVOLA, CARD) | ☐ |
| 2 | Choix du mode au poste (vendeuse) | Poste ouvert, ticket vide | 4 choix : Espèces, MVola, Orange Money, Carte ; Espèces choisi ; « À la livraison » non proposé | `pos-ticket.spec.ts` « propose espèces, MVola, Orange Money et carte, espèces étant choisi à l'ouverture » et « ne propose pas « à la livraison », réservé au livreur » | ☐ |
| 2b | idem | Clic sur « Carte » | Un seul mode sélectionné, le ticket garde `CARD` | `pos-ticket.spec.ts` « enregistre le mode choisi dans le ticket, un seul mode à la fois » | ☐ |
| 2c | idem (reçu) | 1 Savon à 8 500 Ar ; Espèces puis MVola | Espèces : saisie du reçu, Encaisser désactivé tant que reçu < net ; MVola : saisie masquée, Encaisser actif | `pos-ticket.spec.ts` « masque la saisie du reçu hors espèces et encaisse sans montant reçu » ; `pos-cart.spec.ts` « calcule le rendu monnaie… » | ☐ |
| 3 | Le mode choisi part avec la vente | Encaisser avec MVola, Orange Money, puis Carte | `POST /api/v1/sales` contient `paymentMethod` = le mode choisi | `pos-page.spec.ts` « enregistre la vente avec le mode de règlement choisi (%s) » | ☐ |
| 3b | idem (défaut) | Espèces, reçu 10 000 pour un net de 8 500 | `paymentMethod` = `CASH` | `pos-page.spec.ts` « enregistre la vente en espèces par défaut quand le reçu couvre le net » | ☐ |
| 3c | idem (mapping) | `toSaleWDto(..., 'MVOLA', ...)` | corps avec `paymentMethod: 'MVOLA'` | `pos-mapping.spec.ts` (existant) | ☐ |
| 4 | Le mode figure sur la vente et sa facture (aperçu, FAC-01 à venir) | Vente de la liste du jour avec chaque mode | La ligne « Règlement » du tableau et le document de facture portent le mode de la vente | `sales-mapping.spec.ts` « reporte le mode de règlement %s de la vente sur la ligne du tableau et sur la facture (VTE-05) » | ☐ |
| 5 | Ticket suivant | Encaissement avec Carte puis nouveau ticket | Règlement remis à Espèces, vendeuse gardée | `pos-cart.spec.ts` « remet les totaux à zéro au ticket suivant en gardant la vendeuse » | ☐ |

## Sécurité et erreurs

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Appel sans jeton | 401 `UNAUTHENTICATED` | `SaleIT.createSale_withoutToken_returns401` | ☐ |
| Mauvais rôle (livreur) | 403 `FORBIDDEN` | `SaleIT.createSaleAndForm_deliveryDriver_return403` | ☐ |
| Mode inconnu (`CHEQUE`) | 400 `MALFORMED_REQUEST`, aucune vente enregistrée | `SaleIT.createSale_unknownPaymentMethod_returns400AndRecordsNothing` | ☐ |
| Mode absent ou `null` | 422 `VALIDATION_FAILED` | `SaleIT.createSale_noLineOrZeroQuantityOrMissingPayment_returns422`, `SaleIT.createSale_nullPaymentMethod_returns422` | ☐ |
| Champ serveur dans le corps (`id`, `number`, `status`, `total`) | ignoré (mass assignment) | `SaleIT.createSale_serverFieldsInTheBody_areIgnored` | ☐ |
| Changer le mode après coup | impossible : colonne `updatable = false`, aucun endpoint de modification | — (voir guide, « Points d'attention ») | — |

## Cas limites et hors ligne

- **Hors ligne** : la vente saisie sans réseau est mise en file avec son `paymentMethod` dans le corps ; rejouée avec la même `Idempotency-Key`, elle garde le mode choisi (`pos-service.spec.ts`, mise en file de `POST /sales` avec `paymentMethod: 'CASH'`).
- **Raccourci F12** : encaisse avec le mode alors choisi, une seule fois (`pos-page.spec.ts` « F12 encaisse la vente comme le bouton »).
- **Mode « À la livraison »** : non proposé à la caisse ; accepté par l'API (il sert à LIV-03, vente créée à la livraison). Voir le guide.
- **Liste vide / pagination** : sans objet (aucune liste nouvelle).

## Recette manuelle (🧪)

| # | Parcours | Attendu | OK |
|---|---|---|---|
| R1 | Vendeuse, `/pos` : ajouter un article, choisir MVola, Encaisser | Pas de saisie de reçu ; toast « payé » ; la vente apparaît dans `/sales` avec « MVola » dans la colonne Règlement | 🧪 |
| R2 | Même chose avec Orange Money, Carte, puis Espèces (reçu ≥ net) | Chaque vente affiche son mode dans `/sales` ; Espèces exige un reçu suffisant | 🧪 |
| R3 | `/sales` : « Voir » la facture d'une vente payée par carte | L'aperçu de facture indique « Carte » | 🧪 |
| R4 | Navigation clavier sur les modes (flèches) puis F12 | Le mode au focus est choisi et encaissé | 🧪 |

## Résultat de l'exécution

- Date : à lancer (CI ou local)
- Environnement : Testcontainers PostgreSQL (backend), Vitest (frontend)
- Anomalies ouvertes : aucune connue
