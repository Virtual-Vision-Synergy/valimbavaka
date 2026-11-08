# Guide d'intégration — DSH-02 · Graphique du CA sur 7 jours

Statut : ✅ rédigé (tests à lancer en CI) · Ticket : [#95](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/95) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Le cockpit (`/dashboard`, onglet « Analyse ») affiche le chiffre d'affaires réel des 7 derniers jours, une barre par
jour avec une infobulle au survol (date complète et montant exact), et peut basculer sur les 4 dernières semaines.
Le serveur calcule la tendance (`GET /api/v1/dashboard/revenue-trend`) : ventes validées par date de journée +
prestations salon par jour local. Ce ticket crée la feature backend `dashboard` et un port de lecture du CA côté `sale`.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/dashboard/revenue-trend` | `reports.read` (ADMIN seulement) | — | 200 `RevenueTrendRDto` | 401, 403 |

- `RevenueTrendRDto` (`feature/dashboard/dto/RevenueTrendRDto.java`) : `today` (date du jour à Antananarivo),
  `last7Days` (7 `RevenuePointRDto`, de `today - 6` à `today`, le plus ancien d'abord), `last4Weeks`
  (4 `RevenuePointRDto`, `date` = lundi, semaines du lundi au dimanche, la semaine en cours en dernier, incomplète).
- `RevenuePointRDto` : `date` (`yyyy-MM-dd`), `amount` (`BigDecimal`, 0 si rien n'a été vendu : chaque jour a sa barre).

### Services et ports Java réutilisables

| Port | Méthode | Rôle |
|---|---|---|
| `feature.sale.service.SaleRevenueService` (`SaleRevenueService.java:15`) | `List<DailySalesRevenueRDto> getDailyRevenue(LocalDate from, LocalDate to)` | CA des ventes `VALIDATED` par `business_date` sur `[from, to]` (bornes incluses), dates sans vente absentes |
| `feature.dashboard.service.DashboardService` | `RevenueTrendRDto getRevenueTrend()` | Tendance complète (ventes + prestations), jours et semaines vides à 0 |

- `DailySalesRevenueRDto(businessDate, revenue)` (`feature/sale/dto/`) : `revenue` = Σ `sales.total` (après remise).
- Requête : `SaleRevenueRepository.sumTotalPerBusinessDate` (`feature/sale/repository/SaleRevenueRepository.java:25`),
  un `GROUP BY business_date` en JPQL (index `business_date` de VTE-01) ; repository en lecture seule distinct de
  `SaleRepository` pour ne pas entrer en conflit avec les autres tickets qui y ajoutent des requêtes.
- Calcul : `DashboardServiceImpl.getRevenueTrend` (`feature/dashboard/service/impl/DashboardServiceImpl.java:44`),
  agrégation par jour `revenuePerDay` (`:60`), somme hebdomadaire `weekTotal` (`:74`). Prestations lues par
  `ServiceRecordService.getServiceRecordsBetween` (SRV-02) entre minuits locaux.

### Frontend (`features/dashboard`)

- Route : aucune nouvelle ; carte dans l'onglet « Analyse » de `DashboardPage` (`pages/dashboard-page/dashboard-page.html:56`),
  rendue seulement avec `reports.read` (`canSeeRevenueTrend`, `dashboard-page.ts:83`).
- Données : `DashboardPage.revenueTrend` (`dashboard-page.ts:87`), `rxResource` sur `DashboardService.revenueTrend()`
  (`data-access/dashboard-service.ts:24`), au repos sans `reports.read` (aucun appel). `revenueTrendValue` /
  `revenueTrendFailed` (`:91`, `:94`) évitent de lire `value()` en erreur.
- Composant `vvs-revenue-chart-card` (`components/revenue-chart-card/revenue-chart-card.ts`), présentationnel :
  entrées `trend: RevenueTrendRDto | undefined` (absent = squelette), `failed` ; sortie `retry`. Bascule 7 jours /
  4 semaines locale à la carte. Échelle calculée par `niceStep` (`:31`, pas 1 / 2 / 2,5 / 5 × 10ⁿ, minimum 1 000 Ar),
  montants en « M » au-delà d'1 M, sinon en « k ». Infobulle construite par `tooltip` (`:117`).
- `vvs-bar-chart` (`shared/components/bar-chart`) : champ optionnel `ChartBar.tooltip` (`bar-chart.ts:10`), affiché au
  survol de la colonne (`bar-chart.html:16`) ; les autres usages (styleguide) ne changent pas. Le tableau `sr-only`
  reste l'équivalent accessible (le graphique est `aria-hidden`).
- Types : `RevenuePointRDto`, `RevenueTrendRDto` dans `data-access/dashboard-models.ts` ; l'ancien `DailyRevenueRDto`
  et les champs simulés `DashboardRDto.revenueLast7Days` / `revenueLast4Weeks` ont été supprimés (modèle et fixtures).
- Traductions `dashboard.chart.tooltipDay`, `dashboard.chart.tooltipWeek` (fr, mg).

### Données

- Ni table, ni migration, ni permission nouvelle (`reports.read` existe depuis AUTH-01, donnée au seul ADMIN).
  Rien n'est écrit au journal (lecture seule).

## Règles métier à respecter par les autres tickets

- **CA** = Σ `sales.total` des ventes `VALIDATED` (après remise) par `business_date` **+** Σ `price` des prestations
  salon par jour local de `performedAt` (Antananarivo). Les ventes `CANCELLED` / `REVERSED` ne comptent jamais ; les
  ventes du port sans vendeuse (LIV-03) comptent.
- **Jour** : fuseau `Indian/Antananarivo` (comme l'ouverture de journée JR-01) ; semaine du lundi au dimanche.
- **Droits** : tendance réservée à `reports.read` (administrateur). La vendeuse et le livreur n'ont ni appel ni carte.
- Pour un autre agrégat de CA par date (RPT-*, DSH-01/04), réutiliser `SaleRevenueService` plutôt que relire `sales`.

## Fichiers concernés

- Back (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/`) :
  - `dashboard/controller/DashboardController.java`, `dashboard/service/DashboardService.java`,
    `dashboard/service/impl/DashboardServiceImpl.java`, `dashboard/dto/RevenueTrendRDto.java`, `RevenuePointRDto.java`,
    `package-info.java` de chaque paquet
  - `sale/service/SaleRevenueService.java`, `sale/service/impl/SaleRevenueServiceImpl.java`,
    `sale/repository/SaleRevenueRepository.java`, `sale/dto/DailySalesRevenueRDto.java`
- Front (`valimbavaka-ui/src/app/`) :
  - `features/dashboard/components/revenue-chart-card/revenue-chart-card.{ts,html}`
  - `features/dashboard/pages/dashboard-page/dashboard-page.{ts,html}`
  - `features/dashboard/data-access/dashboard-service.ts`, `dashboard-models.ts`, `dashboard-fixtures.ts`
  - `shared/components/bar-chart/bar-chart.{ts,html,css}`
  - `public/i18n/{fr,mg}.json`
- Tests : `DashboardServiceImplTest`, `DashboardIT`, `revenue-chart-card.spec.ts`, `dashboard-page.spec.ts`
  (détail dans le [scénario](scenario-test.md#tests-existants)).

## Dépendances

**Ce ticket s'appuie sur** :
- [VTE-01](../VTE-01/guide.md) : tables `sales` (`business_date`, `status`, `total`), règle « CA = Σ total des ventes validées ».
- [SRV-02](../SRV-02/guide.md) : `ServiceRecordService.getServiceRecordsBetween` (prestations salon).
- [DSH-03](../DSH-03/guide.md) : organisation du cockpit (bloc chargé à part, même modèle d'états).

**Ces tickets s'appuient sur lui** :
- [DSH-01](../DSH-01/guide.md) : cockpit réel (voir ci-dessous) ; [DSH-04](../DSH-04/guide.md), [RPT-01](../RPT-01/guide.md) : port `SaleRevenueService`.

## Comment l'utiliser depuis un autre ticket

```java
// CA validé par date de journée sur une période (RPT-*, DSH-*)
List<DailySalesRevenueRDto> days = saleRevenueService.getDailyRevenue(from, to);
```

```html
<vvs-revenue-chart-card [trend]="revenueTrendValue()" [failed]="revenueTrendFailed()"
  (retry)="revenueTrend.reload()" />
```

### Intégration par DSH-01 (cockpit journalier)

- Ne pas remettre `revenueLast7Days` / `revenueLast4Weeks` dans `DashboardRDto` : la carte garde son propre appel
  (droits différents, `reports.read` contre `dashboard.read` ; un échec ne casse pas le cockpit).
- DSH-01 ajoute son `GET /api/v1/dashboard` dans le même `DashboardController` / `DashboardService`.

## Points d'attention

- **Hypothèse : réservé à l'administrateur** via `reports.read` (permission existante, pas de migration), comme le
  suggère la user story. Pour l'ouvrir à la vendeuse, il faudra une permission dédiée.
- **Hypothèse : CA = ventes validées + prestations salon**, conformément au guide VTE-01 (« DSH-* qui veulent le CA
  total doivent additionner les deux sources »). Les ventes comptent sur leur `business_date`, les prestations (sans
  date de journée) sur leur jour local de réalisation.
- **Hypothèse : la bascule « 4 semaines » de la maquette est conservée** (au-delà du critère, même appel).
- Les prestations de la période (≤ 28 jours) sont chargées en entier par `getServiceRecordsBetween` puis sommées en
  Java : suffisant pour le volume d'un salon ; un agrégat SQL côté `salonservice` serait à ajouter si le volume grandit.
- L'infobulle n'apparaît qu'au survol souris (le graphique est `aria-hidden`) ; au clavier et au lecteur d'écran, le
  tableau de données équivalent donne les mêmes montants.
- Le reste du cockpit (KPI, répartition, activité) reste simulé (DSH-01, DSH-04) ; la route garde `underConstruction`.
- **PR empilée** sur VTE-01 (PR #212) : à merger après elle.
