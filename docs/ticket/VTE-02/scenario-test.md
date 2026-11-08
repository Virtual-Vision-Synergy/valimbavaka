# Scénarios de test — VTE-02 · Poste de caisse avec calcul automatique

Statut : ✅ rédigé le 2026-11-08 · Ticket : [#63](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/63) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests frontend Vitest sur le comportement visible. Ticket **frontend seul** : le calcul en
temps réel est fait au poste (il doit marcher hors connexion, SYS-07) avec la **même règle que le serveur**
(`Sale.record` / `SaleLine.of`, VTE-01), qui reste la référence à l'encaissement. Aucun endpoint, aucune migration.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | Vendeuse `SELLER` sur `/pos` (garde `sales.read`, encaissement `sales.create`) ; administrateur `ADMIN` (même écran) |
| Données de départ | Journée du jour ouverte (JR-01) ; Savon noir 100 g `ART-0031` à 8 500 Ar (stock Boutique 2 dans les tests du panier), Gommage café 200 g `ART-0087` à 12 500 Ar, Beurre de karité `ART-0154` à 15 000 Ar ; ticket de la maquette (`POS_FIXTURE`) : 3 × Savon noir + Beurre de karité + Huile + Gommage = 64 000 Ar, promotion « Octobre rose » −15 % sur Savon noir et Gommage = 5 700 Ar, net 58 300 Ar, reçu 60 000 Ar |
| Tickets à avoir déployés avant | VTE-01 (PR empilée : `feature/VTE-01-enregistrement-vente-multicanal`) |

## Scénarios issus des critères d'acceptation

| # | Critère d'acceptation | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | L'ajout d'un article met à jour en temps réel sous-total, remise et net à payer. | Ticket vide ; ajout de Savon noir (8 500), puis d'une 2ᵉ unité, puis de Gommage (12 500) | Sous-total 8 500 → 17 000 → 29 500 ; remise 0 ; net à payer égal au sous-total à chaque étape, sans action de la vendeuse | `pos-cart.spec.ts` « met à jour sous-total, remise et net à payer à chaque ajout d'article » | ☐ |
| 1b | idem (affichage) | Poste ouvert, ticket vide ; l'article est ajouté | Les zones « Sous-total », « Remise », « Net à payer » et le bouton « Encaisser 8 500 Ar » affichent les nouveaux montants dès l'ajout | `pos-ticket.spec.ts` « affiche sous-total, remise et net à payer mis à jour dès l'ajout d'un article » | ☐ |
| 1c | idem (remise de ligne) | Ticket de la maquette avec la promotion −15 % sur Savon noir et Gommage | Sous-total 64 000, remise 5 700 (3 825 + 1 875), net 58 300 ; les articles hors promotion n'ont pas de remise | `pos-cart.spec.ts` « applique la remise de ligne de la promotion aux seuls articles ciblés » | ☐ |
| 1d | idem (retrait) | 2 × Savon noir + Gommage ; retrait d'une unité de Savon noir, puis de la dernière | 21 000 → 12 500 ; puis retrait du Gommage : 0, ticket vide | `pos-cart.spec.ts` « recalcule les totaux quand une unité est retirée, jusqu'au ticket vide » | ☐ |
| 1e | idem (rendu monnaie) | Net 58 300, espèces ; reçu 60 000 puis 50 000 | Rendu 1 700 ; puis « Manque 8 300 Ar » et encaissement bloqué ; avec MVola, encaissement possible sans montant reçu | `pos-cart.spec.ts` « calcule le rendu monnaie et bloque l'encaissement en espèces si le reçu ne couvre pas le net », `pos-ticket.spec.ts` « affiche le rendu ou le manque selon le montant reçu » | ☐ |
| 1f | idem (lecteur d'écran) | Ajout de Savon noir | Annonce « Savon noir 100 g ajouté · quantité 1 · net à payer 8 500 Ar » (zone `aria-live`) | `pos-cart.spec.ts` « annonce le net à payer aux lecteurs d'écran après chaque ajout ou retrait » | ☐ |
| 1g | Le calcul fait foi côté serveur | Prix du Savon noir passé à 9 000 Ar après l'ouverture du poste ; encaissement de 1 unité affichée à 8 500 | Vente enregistrée au prix du serveur ; toast d'avertissement « Vente VTE-0042 encaissée au prix du serveur : 9 000 Ar (affiché 8 500 Ar). Vérifiez le rendu. » | `pos-page.spec.ts` « avertit quand le net encaissé par le serveur diffère du net affiché » | ☐ |
| 1h | idem | Le serveur confirme le net affiché | Toast de succès habituel « Vente VTE-0042 encaissée · 8 500 Ar », ticket vidé, totaux à 0 | `pos-page.spec.ts` « confirme l'encaissement et remet les totaux à zéro quand le serveur confirme le net affiché » | ☐ |

## Sécurité et erreurs

Aucun nouvel endpoint : l'écran utilise `GET /api/v1/sales/form` et `POST /api/v1/sales` de VTE-01, dont la sécurité
(401, 403, mass assignment : aucun prix ni total envoyé, 404, 409, 422) est couverte par `SaleIT` (voir le
[scénario VTE-01](../VTE-01/scenario-test.md)).

| Cas | Exemple | Résultat attendu | Test auto | OK |
|---|---|---|---|---|
| Totaux jamais envoyés | corps de `POST /api/v1/sales` | ni prix, ni remise, ni total : le serveur recalcule | `pos-mapping.spec.ts` « envoie canal, vendeuse, règlement et quantités sans aucun prix » (VTE-01) | ☐ |
| Refus du serveur | 409 journée non ouverte | toast « Vente refusée : … », ticket et totaux conservés | — (🧪 recette R3) | 🧪 |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test auto | OK |
|---|---|---|---|---|
| Arrondi d'une ligne comme le serveur | 1,5 × 3 333,33 = 4 999,995 ; 2,5 × 0,33 = 0,825 | 5 000,00 et 0,83 (HALF_UP, 2 décimales, comme `SaleLine.of`) | `pos-pricing.spec.ts` « calcule le total d'une ligne comme le serveur (HALF_UP à 2 décimales) » | ☐ |
| Virgule flottante | 3 × 0,10 ; 0,10 + 0,20 | 0,30 exactement (calcul en centimes entiers) | `pos-pricing.spec.ts` « additionne les lignes au centime près, sans erreur de virgule flottante » | ☐ |
| Remise d'une ligne | 25 500 × 15 % ; 12 345 × 15 % | 3 825 ; 1 852 (arrondie à l'ariary) | `pos-pricing.spec.ts` « arrondit la remise d'une ligne à l'ariary » | ☐ |
| Remise supérieure à la ligne | taux 120 % sur 8 500 | remise bornée à 8 500, net jamais négatif (même règle que la contrainte `0 ≤ remise ≤ sous-total` de `sales`) | `pos-pricing.spec.ts` « borne la remise au montant de la ligne : le net n'est jamais négatif » | ☐ |
| Ticket vide | aucune ligne | sous-total, remise, net à 0 ; encaissement bloqué | `pos-pricing.spec.ts` « renvoie des totaux nuls pour un ticket vide » | ☐ |
| Rendu | 60 000 − 58 300 ; 50 000 − 58 300 | 1 700 ; −8 300 (manque) | `pos-pricing.spec.ts` « calcule le rendu monnaie et le manque au centime près » | ☐ |
| Borne du stock | Savon noir, stock 2 ; 3ᵉ ajout | refusé (« Stock insuffisant pour Savon noir 100 g »), totaux inchangés (17 000) | `pos-cart.spec.ts` « refuse un ajout au-delà du stock sans changer les totaux » | ☐ |
| Ticket suivant | encaissement puis nouveau ticket | totaux et reçu à 0, règlement Espèces, vendeuse conservée | `pos-cart.spec.ts` « remet les totaux à zéro au ticket suivant en gardant la vendeuse » | ☐ |
| Hors ligne | encaissement sans réseau | totaux calculés au poste sans appel serveur ; saisie en file (VTE-01), toast « Saisie enregistrée hors connexion » sans comparaison de montant (pas de réponse) | `pos-page.spec.ts` « ne compare pas les montants d'une vente mise en file hors connexion » | ☐ |
| Concurrence | deux caisses vendent le dernier article | la seconde reçoit 409 (VTE-01 / STK-01) ; ticket conservé | — (couvert par `SaleIT.createSale_notEnoughStock_returns409AndRecordsNothing`) | ⏭ VTE-01 |

## Recette manuelle (🧪)

| # | Parcours | Attendu | OK |
|---|---|---|---|
| R1 | Vendeuse, `/pos` : toucher 3 articles du catalogue, puis +/− sur une ligne | Sous-total, remise et net à payer changent à chaque touche, sans délai ni rechargement ; bouton « Encaisser … » au même montant | 🧪 |
| R2 | Espèces : taper 60 000 au pavé pour un net de 58 300 | Rendu 1 700 Ar ; en tapant 50 000 : « Manque 8 300 Ar » et bouton Encaisser désactivé | 🧪 |
| R3 | Journée non ouverte : encaisser | Toast « Vente refusée : … », ticket et totaux conservés | 🧪 |
| R4 | Modifier le prix d'un article (`/articles`) pendant que le poste est ouvert, puis l'encaisser | Toast d'avertissement avec le net du serveur ; recharger le poste pour reprendre les nouveaux prix | 🧪 |
| R5 | NVDA / VoiceOver : ajouter un article | Annonce du nom, de la quantité et du net à payer | 🧪 |

## Résultat de l'exécution

- Date : à lancer (CI ou local)
- Environnement : CI GitHub Actions (`ci.yml`, `conventions.yml`)
- Anomalies ouvertes : aucune connue
