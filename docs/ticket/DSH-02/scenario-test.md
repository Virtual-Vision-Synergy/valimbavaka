# Scénarios de test — DSH-02 · Graphique du CA sur 7 jours

Statut : ☐ tests écrits, à lancer en CI · Ticket : [#95](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/95) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

Endpoint en **lecture seule** `GET /api/v1/dashboard/revenue-trend` (permission `reports.read`, administrateur) :
aucune écriture, donc ni mass assignment, ni 404/409/422, ni journal, ni file hors ligne.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (`reports.read`) ; `SELLER` et `DELIVERY_DRIVER` (sans `reports.read`) pour les refus |
| Données de départ | Journée du jour ouverte (JR-01) ; « Savon » à 8 500 Ar, 10 en stock Boutique ; une vendeuse active (canal Boutique, 5 %) |
| Exemple de calcul (tests unitaires) | Aujourd'hui = dimanche 08/11/2026 (Antananarivo, UTC+3) ; ventes validées : 125 000,00 Ar le 06/11, 64 000,00 Ar le 08/11 ; prestation salon 15 000,00 Ar le 08/11 à 09:15 |
| Tickets à avoir déployés avant | VTE-01 (tables `sales` / `sale_lines`, PR #212), SRV-02 (prestations salon), DSH-03 (cockpit) |

## Scénarios issus des critères d'acceptation

### Critère 1 — Le graphique affiche une valeur par jour avec infobulle au survol

| # | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|
| 1 | Administrateur, `GET /api/v1/dashboard/revenue-trend` | 200 ; `last7Days` = 7 points, `last4Weeks` = 4 points, `last7Days[6].date` = `today` | `DashboardIT.getRevenueTrend_admin_returnsSevenDaysAndFourWeeksEndingToday` | ☐ |
| 1b | Aucune vente ni prestation sur la période | 7 jours du 02/11 au 08/11, tous à 0 ; 4 semaines à 0 (un jour vide garde sa barre) | `DashboardServiceImplTest.getRevenueTrend_noSaleNorService_returnsSevenDaysAndFourWeeksAtZero` | ☐ |
| 1c | Ventes 125 000,00 (06/11) et 64 000,00 (08/11) + prestation 15 000,00 (08/11) | 06/11 = 125 000,00 ; 07/11 = 0 ; 08/11 = 79 000,00 (ventes + prestations) | `DashboardServiceImplTest.getRevenueTrend_salesAndServices_sumsBothSourcesPerDay` | ☐ |
| 1d | Vente de 3 × 8 500 Ar saisie aujourd'hui, puis passée `CANCELLED` | Le point du jour augmente de 25 500,00, puis revient à sa valeur initiale | `DashboardIT.getRevenueTrend_afterASale_addsItsTotalToTodayButNotOnceCancelled` | ☐ |
| 1e | Réponse de 7 jours reçue par la carte | 7 barres, la dernière « Aujourd'hui » mise en avant (une seule) | `revenue-chart-card.spec.ts` › « affiche une barre par jour sur 7 jours, aujourd’hui en dernier et mis en avant » | ☐ |
| 1f | Survol de la barre du jeudi 05/11 (125 000) | Infobulle « jeudi 5 novembre : 125 000 Ar » ; jour sans vente : « mardi 3 novembre : 0 Ar » | `revenue-chart-card.spec.ts` › « donne dans l’infobulle de chaque jour sa date complète et son montant exact » | ☐ |
| 1g | Jour à 0 | La barre et la ligne du tableau accessible existent (« 0 M ») | `revenue-chart-card.spec.ts` › « garde une barre à 0 pour un jour sans vente » | ☐ |
| 1h | Total des 7 jours 4 334 000 Ar | Sous-titre « Total 4 334 000 Ar · moyenne 619 143 Ar » | `revenue-chart-card.spec.ts` › « résume le total et la moyenne journalière des 7 jours » | ☐ |
| 1i | Bascule « 4 semaines » | 4 barres, infobulle « Semaine du 12 octobre : 6 420 000 Ar », dernière barre « Cette semaine » | `revenue-chart-card.spec.ts` › « passe à 4 barres hebdomadaires avec « Semaine du … » dans l’infobulle » | ☐ |
| 1j | Semaines : ventes le lundi 12/10 (10 000), le dimanche 18/10 (5 000), le lundi 19/10 (7 000), le 08/11 (3 000) | Semaines du 12/10, 19/10, 26/10, 02/11 = 15 000 / 7 000 / 0 / 3 000 (lundi → dimanche) | `DashboardServiceImplTest.getRevenueTrend_lastFourWeeks_groupsMondayToSundayWithTheCurrentWeekLast` | ☐ |
| 1k | Administrateur ouvre l'onglet Analyse | Un `GET /api/v1/dashboard/revenue-trend`, 7 barres avec 7 infobulles | `dashboard-page.spec.ts` › « lit la tendance sur l’API et affiche une barre par jour avec son infobulle » | ☐ |

## Sécurité et erreurs

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Appel sans jeton | 401 `UNAUTHENTICATED` | `DashboardIT.getRevenueTrend_withoutToken_returns401` | ☐ |
| Vendeuse ou livreur (sans `reports.read`) | 403 `FORBIDDEN` | `DashboardIT.getRevenueTrend_sellerOrDeliveryDriver_returns403` | ☐ |
| Vendeuse sur le cockpit | Aucun appel à `/dashboard/revenue-trend`, graphique masqué, la répartition du CA reste affichée | `dashboard-page.spec.ts` › « n’appelle pas l’API et masque le graphique pour la vendeuse, sans reports.read » | ☐ |
| Erreur serveur (503) | Erreur avec « Réessayer » dans la carte seulement ; au réessai, nouvel appel et 7 barres | `dashboard-page.spec.ts` › « affiche une erreur dans le graphique sans bloquer le cockpit, puis recharge au réessai » | ☐ |
| Mass assignment, 404, 409, 422, journal | Sans objet : `GET` sans paramètre ni corps, rien n'est écrit | — | ⏭ hors périmètre |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test auto | OK |
|---|---|---|---|---|
| Fuseau : prestation juste après minuit local | Prestation à 21:30 UTC le 07/11 = 00:30 le 08/11 à Antananarivo | Comptée le 08/11, pas le 07/11 | `DashboardServiceImplTest.getRevenueTrend_serviceJustAfterLocalMidnight_countsOnItsLocalDay` | ☐ |
| Bornes de la période | Aujourd'hui dimanche 08/11 | Ventes lues du lundi 12/10 au 08/11 inclus ; prestations de `2026-10-11T21:00Z` à `2026-11-08T21:00Z` (minuits locaux) | `DashboardServiceImplTest.getRevenueTrend_readsServicesFromTheFirstLocalMondayToTheEndOfToday` | ☐ |
| Ventes annulées ou contre-passées | Statut `CANCELLED` / `REVERSED` | Jamais comptées (seules les `VALIDATED`) | `DashboardIT.getRevenueTrend_afterASale_addsItsTotalToTodayButNotOnceCancelled` | ☐ |
| Échelle sans aucune vente | Maximum 0 | Pas minimal 1 000 Ar (axe 0 / 1 k), pas de division par zéro | `revenue-chart-card.spec.ts` › niceStep « garde un pas minimal de 1 000 Ar quand rien n’a été vendu » | ☐ |
| Échelle « ronde » | Max 1 710 000 → pas 500 000 ; 7 640 000 → 2 000 000 ; 25 500 → 10 000 | Environ 4 graduations, montants en « M » au-delà d'1 M, sinon en « k » | `revenue-chart-card.spec.ts` › niceStep « choisit un pas rond donnant environ quatre graduations » | ☐ |
| Chargement | Réponse pas encore reçue | Squelette (`role="status"`), pas de sous-titre ni de barre | `revenue-chart-card.spec.ts` › « montre un squelette tant que la tendance n’est pas chargée » | ☐ |
| Erreur | `failed` | Message d'erreur, « Réessayer » émet `retry` | `revenue-chart-card.spec.ts` › « montre une erreur avec réessai si la lecture échoue » | ☐ |
| Vente du port sans vendeuse (LIV-03) | `seller_id` NULL | Comptée (le CA ne dépend pas de la vendeuse) | — (requête sans filtre vendeuse, `SaleRevenueRepository.sumTotalPerBusinessDate`) | 🔍 code |
| Hors ligne | Pas de réseau | Lecture seule : état d'erreur avec réessai, aucune file `Idempotency-Key` | idem « Erreur serveur » | ☐ |

## Tests existants

- Back : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/dashboard/service/impl/DashboardServiceImplTest.java` (5),
  `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/dashboard/controller/DashboardIT.java` (4) ;
  `ArchitectureTest` (règles de la nouvelle feature : contrôleur mince, `@HasPermission`, service en lecture seule, pas de cycle).
- Front : `valimbavaka-ui/src/app/features/dashboard/components/revenue-chart-card/revenue-chart-card.spec.ts` (9),
  `valimbavaka-ui/src/app/features/dashboard/pages/dashboard-page/dashboard-page.spec.ts` (3 ajoutés),
  `valimbavaka-ui/src/app/features/demo-pages.a11y.spec.ts` (non-régression de la page `dashboard`),
  `valimbavaka-ui/src/app/core/i18n/translations.spec.ts` (clés `tooltipDay`, `tooltipWeek` en fr et mg).

## Recette manuelle (🧪)

| # | Étape | Résultat attendu | OK |
|---|---|---|---|
| R1 | En administrateur, encaisser une vente de 25 500 Ar au poste (`/pos`), puis ouvrir `/dashboard?tab=analysis` | La barre « Aujourd'hui » a augmenté de 25 500 Ar | 🧪 |
| R2 | Survoler chaque barre à la souris | Infobulle avec la date complète et le montant exact (« jeudi 5 novembre : 125 000 Ar ») | 🧪 |
| R3 | Basculer « 4 semaines » puis survoler | 4 barres, infobulle « Semaine du … » | 🧪 |
| R4 | Enregistrer une prestation salon puis recharger | Le prix de la prestation s'ajoute à la barre du jour | 🧪 |
| R5 | Se connecter en vendeuse, ouvrir l'onglet Analyse | Pas de graphique du CA, aucune requête `/dashboard/revenue-trend` dans l'onglet Réseau | 🧪 |
| R6 | Interface en malgache | Libellés et infobulles traduits (« Herinandro nanomboka ny … ») | 🧪 |

## Résultat de l'exécution

- Résultat de l'exécution : à lancer (CI ou local).
- Avant push : `tsc -p tsconfig.app.json`, `tsc -p tsconfig.spec.json`, `eslint` et `prettier` sans erreur sur les fichiers
  touchés ; le backend n'a pas pu être compilé localement (Maven Central inaccessible) : la CI compile.
- Anomalies ouvertes : aucune connue. Étapes 🧪 restantes : 6.
