# Scénarios de test — DSH-03 · Alertes de stock sur le cockpit

Statut : ☐ tests écrits, à exécuter · Ticket : [#96](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/96) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

Ticket **frontend seul** : le cockpit lit l'endpoint existant `GET /api/v1/stocks/alerts` de STK-04, sans nouvel
endpoint, ni permission, ni migration. Le calcul des alertes (seuil, manque, quantité suggérée, ordre) est couvert
par le scénario de [STK-04](../STK-04/scenario-test.md).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (`stocks.read` + `stocks.restock`), `SELLER` (`stocks.read`), `DELIVERY_DRIVER` (`dashboard.read` sans `stocks.read`) |
| Données de départ | « Savon noir 100 g » (`ART-0031`, pièce, seuil 40) à 0 en stock ; « Gommage café 200 g » (`ART-0087`) à 3 ; un article à 12,5 / 40 ; un article au-dessus de son seuil |
| Tickets à avoir déployés avant | STK-04 (alertes de seuil), STK-05 (permission `stocks.restock`) |

## Scénarios issus des critères d'acceptation

### Critère 1 — Chaque article en alerte est cliquable vers sa fiche détaillée

| # | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|
| 1 | Le serveur renvoie l'alerte `ART-0031` (id `7f1c2a9e-…-000000000031`) | Le nom « Savon noir 100 g » est un lien vers `/articles/7f1c2a9e-0000-4000-8000-000000000031` | `stock-alerts-card.spec.ts` › « rend chaque article en alerte cliquable vers sa fiche détaillée » | ☐ |
| 1b | Administrateur ouvre le cockpit | Un seul `GET /api/v1/stocks/alerts?page=1&size=100` ; le bloc montre le lien vers la fiche et la pastille « Rupture » | `dashboard-page.spec.ts` › « lit les 100 alertes les plus urgentes et rend chaque article cliquable vers sa fiche » | ☐ |
| 1c | Alerte 12,5 / 40, quantité suggérée 30 pièces | Cellules « ART-0031 », « 12.5 / 40 », « 30 Pièce », « Stock faible » | `stock-alerts-card.spec.ts` › « affiche stock, seuil, quantité à commander avec son unité et le code article » | ☐ |
| 1d | Vue d'ensemble (`limit` 3), 4 alertes reçues sur `totalCount` 7 | Les 3 premières dans l'ordre du serveur (rupture d'abord), sous-titre « 7 article(s) sous le seuil minimum » | `stock-alerts-card.spec.ts` › « garde l’ordre du serveur, signale la rupture et limite la vue d’ensemble aux 3 plus urgents » | ☐ |
| 1e | Onglet Analyse (sans limite), 4 alertes | 4 lignes, 4 liens | `stock-alerts-card.spec.ts` › « affiche toutes les alertes reçues sans limite » | ☐ |
| 1f | Vendeuse (`stocks.read`) | Le bloc s'affiche aussi (même règle que la page des stocks, STK-04) | `dashboard-page.spec.ts` › « montre aussi les alertes à la vendeuse, qui a stocks.read » | ☐ |

## Sécurité et erreurs

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Livreur (`dashboard.read` sans `stocks.read`) | Aucun appel à `/api/v1/stocks/alerts`, bloc absent, le reste du cockpit s'affiche | `dashboard-page.spec.ts` › « n’appelle pas l’API et masque le bloc pour le livreur, sans stocks.read » | ☐ |
| Raccourci « Générer le bon de réappro. » pour la vendeuse (sans `stocks.restock`) | Masqué, seul « Tout voir » reste | `stock-alerts-card.spec.ts` › « réserve le raccourci de réapprovisionnement à stocks.restock (administrateur) » | ☐ |
| Raccourci pour l'administrateur | « Tout voir » et « Générer le bon de réappro. » | `stock-alerts-card.spec.ts` › « propose le raccourci de réapprovisionnement à l’administrateur » | ☐ |
| `GET /api/v1/stocks/alerts` sans jeton / livreur | 401 / 403 côté serveur | ⏭ hors périmètre : `StockSecurityIT.getAlerts_withoutToken_returns401`, `StockSecurityIT.getAlerts_withDeliveryDriverRole_returns403` (STK-04) | ⏭ |

Non applicable : mass assignment, 404, 409, 422 et journal (lecture seule, aucun endpoint écrit par ce ticket).

## Cas limites et hors ligne

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Aucune alerte (`totalCount` 0) | Pas de tableau ; « Aucun article sous son seuil » ; sous-titre « 0 article(s)… » | `stock-alerts-card.spec.ts` › « indique qu’aucun article n’est sous son seuil quand la liste est vide » | ☐ |
| Réponse pas encore reçue | Squelette (`role="status"`), pas de sous-titre | `stock-alerts-card.spec.ts` › « affiche un squelette tant que la page n’est pas reçue » | ☐ |
| Échec du chargement (composant) | État d'erreur avec bouton « Réessayer » qui émet `retry` | `stock-alerts-card.spec.ts` › « affiche un état d’erreur avec réessai quand le chargement échoue » | ☐ |
| Serveur 503 ou hors ligne (page) | Erreur dans le bloc seulement, KPI affichés ; « Réessayer » relance l'appel | `dashboard-page.spec.ts` › « affiche une erreur dans le bloc sans bloquer le cockpit, puis recharge au réessai » | ☐ |
| Accessibilité du cockpit (axe, WCAG 2.1 AA) | Aucune violation (liens nommés, tableau avec `caption`, états annoncés) | `demo-pages.a11y.spec.ts` › « dashboard should have no detectable accessibility violation » | ☐ |
| Plus de 100 alertes | Le cockpit lit les 100 plus urgentes ; le sous-titre donne le `totalCount` réel ; « Tout voir » mène à la page des stocks | 🔍 code : `StockService.alerts()` (STK-04), `StockAlertsCard.total` | 🔍 |
| Traductions fr / mg | Mêmes clés `dashboard.stockAlerts.*` dans les deux langues | `translations.spec.ts` › « should define every French key in Malagasy and no more » | ☐ |

Pas de file hors ligne (`Idempotency-Key`) : le bloc est en lecture seule.

## Tests existants

- Front : `valimbavaka-ui/src/app/features/dashboard/components/stock-alerts-card/stock-alerts-card.spec.ts`,
  `valimbavaka-ui/src/app/features/dashboard/pages/dashboard-page/dashboard-page.spec.ts`,
  `valimbavaka-ui/src/app/features/demo-pages.a11y.spec.ts` (page `dashboard` ajoutée),
  `valimbavaka-ui/src/app/core/i18n/translations.spec.ts`.
- Back : aucun nouveau test (endpoint STK-04 réutilisé tel quel) ; voir `StockAlertIT`, `StockSecurityIT`.

## Recette manuelle (🧪)

| # | Étape | Résultat attendu | OK |
|---|---|---|---|
| R1 | En administrateur, avec `ART-0031` à 0 / 40, ouvrir `/dashboard` | Bloc « Alertes de stock » avec l'article en tête, pastille « Rupture » | 🧪 |
| R2 | Cliquer sur « Savon noir 100 g » | Navigation vers `/articles/<id>` (la fiche reste une maquette tant qu'ART-06 n'est pas livré) | 🧪 |
| R3 | Porter le stock de l'article à 40 puis recharger le cockpit | L'article disparaît du bloc | 🧪 |
| R4 | Se connecter en livreur | Le cockpit s'affiche sans bloc d'alertes, aucune requête `/stocks/alerts` dans l'onglet Réseau | 🧪 |
| R5 | Mobile (≤ 640 px) | Colonnes « À commander » et jauge masquées, pas de défilement horizontal | 🧪 |

## Résultat de l'exécution

- Date : à lancer (CI ou local).
- Environnement : `tsc -p tsconfig.app.json`, `tsc -p tsconfig.spec.json`, `eslint` et `prettier` sans erreur sur les fichiers touchés.
- Anomalies ouvertes : aucune connue. Étapes 🧪 restantes : 5.
