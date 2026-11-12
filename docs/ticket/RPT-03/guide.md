# Guide d'intégration — RPT-03 · Graphique d'évolution du CA

Statut : ✅ écrit le 2026-11-12 · Ticket : [#100](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/100) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

`GET /api/v1/reports` (RPT-01) renvoie maintenant aussi `trend` : la **série du chiffre d'affaires de la période**, un point par jour pour un jour ou un mois, un point par mois pour une année, avec les mêmes filtres canal et gamme que la synthèse. Les jours sans vente valent 0 : la série est continue. Aucun nouvel endpoint, aucune migration, aucune permission. L'écran `/reports` dessine la courbe (barres) avec le composant du projet, rebaptisé `RevenueTrendChart`.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/reports?granularity=&date=&salesChannelId=&rangeId=` | `reports.read` (admin) | inchangée (RPT-01) | 200 `ReportRDto` enrichi de `trend` | inchangées : 400, 401, 403, 422 |

- `ReportRDto.trend` = liste de `ReportTrendPointRDto` (`feature/report/dto/ReportTrendPointRDto.java`) : `date` (jour, ou 1er jour du mois pour une année) et `revenue` (2 décimales, 0 sans vente). Triée par date, jamais vide.
- Nombre de points : 1 (`DAY`), 28 à 31 (`MONTH`), 12 (`YEAR`). Un mois ou une année en cours est rapporté en entier : les jours ou mois à venir valent 0.
- `sales` et `comparison` sont inchangés.

### Services et types Java réutilisables

| Type | Rôle |
|---|---|
| `ReportSalesService#getTrend(ReportFilter)` | série d'une période filtrée (`service/impl/ReportSalesServiceImpl.java`) ; appelable par DSH-* pour une courbe de 7 jours |
| `ReportPeriod#points()` | les points de la période : chaque jour, ou le 1er de chacun des 12 mois |
| `ReportPeriod#pointOf(day)` | le point auquel un jour appartient (lui-même, ou le 1er de son mois) |
| `repository/ReportSaleRepository#dailyRevenue` / `dailyRevenueByRange` | `group by business_date` avec le **même `where`** que `totals` / `totalsByRange` ; renvoient des `DailyRevenue(day, revenue)` |

Les lignes de SQL sont bornées par la période (au plus 366). Pour une année, le regroupement par mois se fait en Java sur ces lignes journalières (aucune vente n'est chargée).

### Frontend

- `features/reports/data-access/report-models.ts` : `ReportTrendPointRDto`, `ReportRDto.trend`.
- `components/revenue-trend-chart/` (ex `revenue-compare-chart`) : entrées `points`, `granularity`, `today`, `revenueLabel` ; une barre par point, jour d'aujourd'hui en relief, tableau équivalent masqué pour les lecteurs d'écran (colonne « Jour » ou « Mois »). Aucune dépendance npm ajoutée.
- `pages/reports-page` : la section « Évolution du chiffre d'affaires » lit `summary` (même appel que la synthèse, donc mêmes filtres) ; états chargement et erreur avec « Réessayer ».
- `ReportDemoService` ne fournit plus la courbe : il reste les gammes (RPT-04) et le classement (RPT-05). Fixtures et type `ReportPointRDto` de la courbe supprimés.
- Traductions : `reports.chart.month` (fr, mg) ; `reports.current` et `reports.previous` supprimées (inutilisées).

### Données

Aucune migration, aucune permission, aucune écriture au journal (lecture).

## Règles métier à respecter par les autres tickets

- Jour = `sales.business_date`, ventes `VALIDATED` uniquement, CA = somme de `sales.total` (ou de `line_total` pour une gamme) : somme de la série = `sales.revenue` sans filtre de gamme.
- Une série n'a jamais de trou : on complète avec 0 à partir de `ReportPeriod.points()`.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/report/`)
- `domain/ReportPeriod.java` (`points`, `pointOf`), `dto/ReportTrendPointRDto.java` (nouveau), `dto/ReportRDto.java` (champ `trend`)
- `repository/ReportSaleRepository.java`, `repository/DailyRevenue.java` (nouveau)
- `service/ReportSalesService.java`, `service/impl/ReportSalesServiceImpl.java`, `service/impl/ReportServiceImpl.java`

**Frontend** (`valimbavaka-ui/src/app/features/reports/`)
- `components/revenue-trend-chart/*`, `pages/reports-page/reports-page.{ts,html}`, `data-access/report-models.ts`, `report-demo-service.ts`, `report-fixtures.ts`, `report-service.ts` ; `public/i18n/fr.json`, `mg.json`

**Tests**
- Backend : `ReportPeriodTest`, `ReportSalesServiceImplTest`, `ReportServiceImplTest`, `ReportIT`
- Frontend : `reports-page.spec.ts`, `report-service.spec.ts`, `features/demo-pages.a11y.spec.ts` (réponse `/api/v1/reports` enrichie)

## Dépendances

**Ce ticket s'appuie sur** : [RPT-01](../RPT-01/guide.md) (période, filtre, repository), [RPT-02](../RPT-02/guide.md) (`ReportRDto` enrichi).

**Ces tickets s'appuient sur lui** : aucun. [RPT-06](../RPT-06/description.md) exporte le `ReportRDto` : il contient désormais `trend`.

## Comment l'utiliser

```java
var points = reportSalesService.getTrend(new ReportFilter(period, channelId, rangeId)); // List<ReportTrendPointRDto>
```

## Points d'attention

- **Hypothèse (granularité)** : « valeurs journalières » = un point par jour pour un mois ; une année est tracée par mois (366 barres seraient illisibles) ; un jour donne un point unique (pas de découpage par heure : la période est en jours métier).
- **Hypothèse (comparaison)** : la maquette traçait aussi la période précédente en barres appariées ; le critère ne demande que la période choisie, la courbe n'affiche donc que celle-ci. La comparaison reste dans les cartes de synthèse (RPT-02). À valider par le PO.
- **Hypothèse (période en cours)** : les jours à venir sont des barres à 0.
- Le regroupement par mois d'une année est fait en Java sur au plus 366 lignes journalières, pour rester en JPQL portable.
- Les sections gammes et classement restent en démonstration (RPT-04, RPT-05) : le marqueur « En construction » reste affiché.
