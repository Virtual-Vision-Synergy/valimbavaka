# Guide d'intégration — DSH-02 · Graphique du CA sur 7 jours

Statut : ☐ à relire après la CI · Ticket : [#95](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/95) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Le cockpit (`/dashboard`, onglet « Analyse ») dessine le chiffre d'affaires des 7 derniers jours, aujourd'hui inclus : une barre par jour, la valeur exacte (date, CA, nombre de ventes) dans une infobulle au survol. Les données viennent d'un endpoint dédié de `feature/dashboard`, lu à part de l'agrégat de DSH-01 : un échec du graphique ne casse pas les indicateurs.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/dashboard/revenue-trend` | `reports.read` (ADMIN) | aucune | `RevenueTrendRDto` : `from`, `to`, `points[]` de `RevenueTrendPointRDto` (`date`, `revenue`, `salesCount`), 7 points, plus ancien d'abord | 401, 403 |

Série de taille fixe (7), donc non paginée. Un jour sans vente est un point à 0. Les jours sont ceux d'Antananarivo, aujourd'hui inclus.

### Services et ports Java réutilisables

- `RevenueTrendService.getLastSevenDays()` (`feature/dashboard/service`), implémenté par `RevenueTrendServiceImpl` : appelle `ReportSalesService.getSales` une fois par jour (7 lectures SQL, borné) ; n'ajoute aucun calcul propre. Constructeur package-private, il ne prend que des interfaces publiques.
- `DashboardController` porte désormais deux routes (`GET /api/v1/dashboard` et `GET /api/v1/dashboard/revenue-trend`).

### Frontend

- Route : inchangée (`/dashboard`, onglet `?tab=analysis`).
- Service : `DashboardService.revenueTrend()` ; la `rxResource` `revenueTrend` de `DashboardPage` est gardée par `canSeeCockpit`.
- Composant : `RevenueChartCard` prend `trend` (`RevenueTrendRDto`, absent pendant le chargement), `failed` et émet `retry`. Le graphique est `vvs-bar-chart`, dont `ChartBar` gagne un champ optionnel `tooltip` (infobulle CSS au survol de la colonne, aussi dans le DOM).
- Retirés : `revenueLast7Days` et `revenueLast4Weeks` de `DashboardDemoRDto` et de `dashboard-fixtures.ts` ; le sélecteur 7 jours / 4 semaines et ses libellés `dashboard.chart.week|month|window|thisWeek|titleMonth`.
- Jeu de test : `REVENUE_TREND_TEST_VIEW` (`dashboard-testing.ts`).

### Données

- Tables et migrations Liquibase : aucune.
- Écriture dans le journal : aucune (lecture seule).

## Règles métier à respecter par les autres tickets

- Seules les ventes validées comptent (règle de `ReportSalesService`) ; les ventes annulées sortent du graphique.
- Jour = jour calendaire d'Antananarivo (`DashboardServiceImpl.BUSINESS_ZONE`).
- Droit `reports.read` comme DSH-01 : la vendeuse et le livreur n'ont pas le graphique (403, aucun appel du front).

## Points d'attention

- **Hypothèse (4 semaines)** : la maquette proposait aussi un graphique « 4 semaines ». La description ne demande que les 7 jours : le sélecteur et les données de démonstration sont retirés, pas remplacés. Une vue hebdomadaire serait un autre ticket.
- **Hypothèse (aujourd'hui inclus)** : la fenêtre est « aujourd'hui − 6 jours » à « aujourd'hui » ; le dernier point (en cours de journée) est mis en avant et libellé « Aujourd'hui ».
- **Coût** : 7 lectures du rapport de ventes par ouverture de l'onglet. Quand la série du CA par jour de RPT-03 (un `group by` dans `feature/report`) sera dans `develop`, `RevenueTrendServiceImpl` pourra la lire en une seule requête ; le contrat de l'endpoint ne change pas.
- **Montants courts** : axe et valeurs en « M » dès un million, en « k » en dessous ; la valeur exacte est dans l'infobulle.
- **Accessibilité** : la zone graphique reste décorative (`aria-hidden`) ; les lecteurs d'écran lisent le tableau équivalent de `vvs-bar-chart`. L'infobulle apparaît au survol de la souris seulement : l'équivalent clavier est ce tableau.
- **DSH-04** (répartition par gamme et canal) reste en démonstration.
