# Scénarios de test — SYS-10 · Règle d'ordre de synchronisation en cas de blocage

Statut : ✅ rédigé et exécuté le 2026-11-08 (CI verte) · Ticket : [#136](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/136) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests frontend Vitest sur le comportement visible. Ticket frontend seul (moteur de la file hors ligne), aucun endpoint ni migration.

## Règle décidée

**Décision PO du 07/11/2026 (Randy) : « ordre strict par type ».**

| Type de saisie | Adresses (préfixe) | Devant une saisie bloquée par le réseau |
|---|---|---|
| Opérations métier rejouables (un seul groupe) | `/api/v1/journal`, `sales`, `invoices`, `credit-notes`, `stocks`, `transfers`, `production`, `inventories`, `deliveries`, `rounds`, `days`, `shootings` | **attendent** dans l'ordre strict de saisie, tant que la saisie bloquée du groupe n'est ni envoyée ni abandonnée (même après ses 5 tentatives) |
| Fiches et référentiels | `/api/v1/suppliers`, `articles`, `users`, `promotions`, `catalogs`, `services`, `product-ranges`, `sales-channels`, `stock-locations` | **passent devant** ; seules les saisies d'une même fiche (`/suppliers/s-1` puis `/suppliers/s-1/status`) restent dans l'ordre |
| Adresse non classée | toute autre | file unique (comportement d'avant SYS-10) |

Une saisie **refusée par le serveur** (4xx métier) ne retient rien (ADR 0001 inchangé). Table : `SYNC_ORDER_RULES` (`valimbavaka-ui/src/app/core/offline/sync-order.ts:35`).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `SELLER` (tout rôle : la file est par compte) |
| Données de départ | file vide ; saisies faites hors ligne : vente `POST /api/v1/sales`, facture `POST /api/v1/invoices`, fiche fournisseur `PUT /api/v1/suppliers/s-1` |
| Tickets à avoir déployés avant | SYS-02 (file hors ligne), SYS-09 (page `/synchronisation`, PR empilée) |

## Scénarios issus des critères d'acceptation

| # | Critère | Exemple (Étant donné / Quand / Alors) | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Règle décidée et écrite | Décision PO du 07/11/2026 ci-dessus ; classement par préfixe d'adresse | journal, ventes, factures → `ORDERED` (groupe `operations`) ; fournisseurs → `INDEPENDENT` par fiche ; adresse inconnue → `UNCLASSIFIED` | `sync-order.spec.ts` : « should put journal, sales and invoices in the single operations group », « should give each record of a referential its own lane », « should leave an unknown address unclassified » || ✅ auto |
| 1b | Règle extensible | une règle `{ prefix: '/api/v1/returns', kind: 'ORDERED' }` ajoutée | `/api/v1/returns/r-1` classé `ORDERED` | « should accept an added rule for a future write endpoint » || ✅ auto |
| 2 | Une saisie bloquée par le réseau n'empêche pas les saisies indépendantes | file : vente puis fiche `s-1` ; la vente échoue (réseau) | la fiche part tout de suite, la vente est relancée à 1 s | `outbox-order.spec.ts` : « should send an independent record ahead of an operation blocked by the network » || ✅ auto |
| 2b | idem, relances épuisées | vente, facture, fiche `s-1` ; la vente échoue 5 fois | la fiche part ; vente `FAILED` (`networkFailure: true`) ; la facture reste en attente | « should hold later operations behind one whose network retries are exhausted until it is resent » || ✅ auto |
| 3 | Les saisies dépendantes partent dans l'ordre de saisie | vente puis facture ; la vente échoue (réseau) | aucune facture envoyée avant 1 s ; vente puis facture | « should keep operations in entry order behind a blocked one » || ✅ auto |
| 3b | idem, après renvoi | suite de 2b : « Renvoyer » sur la vente | la vente part puis la facture | « should hold later operations … until it is resent » || ✅ auto |
| 3c | idem, après abandon | vente bloquée (5 échecs) puis abandon confirmé | la facture part aussitôt | « should release the held operations when the blocked one is discarded » || ✅ auto |
| 3d | Une même fiche reste dans l'ordre | `PUT /suppliers/s-1` puis `PATCH /suppliers/s-1/status` ; le PUT échoue | le PATCH attend le PUT | « should keep the edits of one record in entry order » || ✅ auto |

## Sécurité et erreurs

Aucun endpoint écrit : 401/403/422/404/409 et mass assignment relèvent des tickets propriétaires des endpoints. Comportement de la file :

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Refus métier d'une opération | vente refusée en 422 | `FAILED` (`networkFailure: false`), la facture suivante part | « should not hold operations behind one refused by the server » ; `nextSendableEntry` « should hold operations behind a network failure but not behind a refusal » || ✅ auto |
| 401 (session expirée) | jeton expiré pendant le rejeu | traité comme transitoire (relance), donc bloque le groupe | couvert par SYS-02 / SYS-11 | ⏭ hors périmètre (SYS-11) |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| File vide | `[]` | rien à envoyer | « should return nothing for an empty queue » || ✅ auto |
| Préfixe proche | `/api/v1/stock-locations/l-1` vs `/api/v1/stocks/movements` | référentiel vs opération (pas de confusion de préfixe) | « should not confuse a prefix with a longer resource name » || ✅ auto |
| Adresse non classée bloquée | `/api/v1/items` en relance, puis `/api/v1/articles/a-1` | tout attend (file unique) | « should hold everything behind a waiting unclassified entry » || ✅ auto |
| Ancien comportement FIFO conservé (non classé) | `/api/v1/first` échoue 5 fois | `/api/v1/second` part après le `FAILED` | `outbox-service.spec.ts` « should retry transient failures with exponential backoff then fail and continue FIFO » || ✅ auto |
| Paramètres d'adresse | `/api/v1/invoices?print=true` | classé comme `/api/v1/invoices` | « should put journal, sales and invoices in the single operations group » || ✅ auto |
| Nouvelle saisie pendant une attente de relance | fiche saisie pendant que la vente attend 8 s | la boucle est réveillée, la fiche part sans attendre | code : `OutboxService.enqueue` → `endPause(true)` (`outbox-service.ts`) | 🔍 code |
| Rechargement de l'application | vente à 3 tentatives en IndexedDB | attend 4 s avant le prochain essai, la fiche part tout de suite | code : `RetrySchedule.restore` (`retry-schedule.ts`) | 🔍 code |
| Changement de compte | saisies d'un autre compte | jamais envoyées (SYS-02 inchangé) | `outbox-service.spec.ts` « should not send one account’s queued requests while another account is connected » || ✅ auto |

## Tests existants

- `valimbavaka-ui/src/app/core/offline/sync-order.spec.ts` (classement et choix de la prochaine saisie)
- `valimbavaka-ui/src/app/core/offline/outbox-order.spec.ts` (moteur, nouveau)
- `valimbavaka-ui/src/app/core/offline/outbox-service.spec.ts` (SYS-02/SYS-09, non modifié : doit rester vert)

## Recette manuelle (🧪)

| # | Étapes | Résultat attendu | Statut |
|---|---|---|---|
| R1 | Couper le serveur (pas le Wi-Fi) ; modifier une fiche fournisseur ; relancer le serveur pendant les relances | la fiche part ; aucune attente derrière une opération | 🧪 |
| R2 | Quand les ventes existeront (VTE) : saisir 2 ventes hors ligne, faire échouer la première jusqu'à `FAILED` | la 2ᵉ reste « en attente » sur `/synchronisation` ; « Renvoyer » la 1ʳᵉ fait partir les deux dans l'ordre | 🧪 |

## Résultat de l'exécution

- Date : 2026-11-08
- Environnement : CI GitHub `ci.yml` lancée sur la branche (run 42411980274) : jobs Backend et Frontend verts ; 53 fichiers et 307 tests frontend passés, dont `outbox-service.spec.ts` (17 tests), `outbox-order.spec.ts` (6) et `sync-order.spec.ts` (9)
- Anomalies ouvertes : aucune
- Étapes 🧪 restantes : 2 (R1, R2)
