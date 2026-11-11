# Guide d'intégration — DSH-01 · Cockpit journalier

Statut : ✅ rédigé le 11/11/2026 — tests verts en CI (voir « Résultat de l'exécution » du scénario) · Ticket : [#94](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/94) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

`GET /api/v1/dashboard` renvoie en une lecture les 4 indicateurs d'**aujourd'hui** (jour d'Antananarivo) : chiffre d'affaires comparé à hier, ventes avec leur répartition par canal, livraisons du jour par statut, écart de stock de la réconciliation de clôture. Réservé à l'administrateur (`reports.read`). Le cockpit est une feature de **lecture** : elle compose des services existants et ne calcule aucun agrégat qui existait déjà. L'écran `/dashboard` affiche ces 4 cartes ; les autres zones (graphique 7 jours, répartition gamme et canal, flux, meilleurs articles, vendeuses, objectif mensuel) restent en démonstration et se branchent une à une.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie (`XxxRDto`) | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/dashboard` | `reports.read` (administrateur) | aucune (ni corps ni paramètre) | 200 `DashboardRDto` | 401, 403 (vendeuse, livreur) |

- `DashboardRDto` : `businessDate`, `status` (`OPEN`, `CLOSED`, `null` si la journée n'est pas ouverte), `generatedAt`, `revenue`, `sales`, `deliveries`, `stockGap` (`null` si la journée n'est pas ouverte).
- `RevenueKpiRDto` : `amount`, `previousAmount` (hier), `changePercent` (une décimale, `null` si hier n'a pas de CA).
- `SalesKpiRDto` : `count`, `previousCount`, `averageBasket`, `channels` (`ChannelSalesRDto` de VTE-08 : tous les canaux actifs, même à 0, meilleur CA d'abord ; libellé `null` pour un canal désactivé depuis).
- `DeliveryKpiRDto` : `total` (hors annulées), `delivered`, `onRound`, `toPrepare`, `failed`, `cancelled` (commandes dont `scheduled_date` = aujourd'hui).
- `StockGapKpiRDto` : `units` (somme signée, négative si du stock manque), `articles` (articles distincts avec un écart).
- Contrôleur : `feature/dashboard/controller/DashboardController.java` (`@HasPermission("reports.read")`, une méthode de 1 ligne).

### Services et ports Java réutilisables

| Port | Méthode | Rôle |
|---|---|---|
| `feature.dashboard.service.DashboardService` | `DashboardRDto getDashboard()` | Les 4 indicateurs d'aujourd'hui (`DashboardServiceImpl.getDashboard`). |

Sources réutilisées, sans agrégat dupliqué : `ReportSalesService.getSales(ReportFilter)` (RPT-01, jour et veille via `ReportPeriod.containing(DAY, date)` et `previous()`), `ChannelDashboardService.getDashboard(jour, jour)` (VTE-08), `DeliveryDayService.countByStatus(date)` (JR-03), `DayClosingService.getClosing()` (JR-04, JR-05), `BusinessDayService.findToday()` (JR-01). Aucun ajout dans `feature/report`.

Aucun nouvel agrégat de lecture n'a été nécessaire : tout vient de ces services. Le seul calcul propre au cockpit est la somme des écarts de lignes (`DashboardServiceImpl.stockGap`) et le pourcentage d'évolution.

### Frontend (`features/dashboard`)

- Route : aucune nouvelle (`/dashboard`, `dashboard.read`). La page ne lit l'agrégat qu'avec `reports.read` (`DashboardPage.canSeeCockpit`) ; sinon elle affiche un message de réservation et, avec `stocks.read`, le bloc d'alertes de DSH-03.
- `data-access/dashboard-service.ts` : `get()` → `GET /api/v1/dashboard` (type `DashboardRDto`) ; `demoZones()` → données de démonstration (type `DashboardDemoRDto`, ne contient plus aucun des 4 indicateurs).
- `data-access/dashboard-models.ts` : types du serveur (`DashboardRDto`, `RevenueKpiRDto`, `SalesKpiRDto`, `ChannelSalesRDto`, `DeliveryKpiRDto`, `StockGapKpiRDto`) et type de la démonstration `DashboardDemoRDto`. `data-access/dashboard-testing.ts` : `DASHBOARD_TEST_VIEW`, un cockpit réaliste pour les specs.
- `components/dashboard-kpis` : les 4 cartes, présentationnel (`data: DashboardRDto`).
- `pages/dashboard-page` : deux `rxResource` (`dashboard` réel, `demoZones` simulé) réunis dans `view` ; la date, l'heure de lecture et les étapes « Ventes » et « Livraisons » de la démonstration sont alignées sur les vrais chiffres (`alignedOnLive`). Les alertes de stock gardent leur propre ressource (guide DSH-03).
- Traductions (fr, mg) : `dashboard.restricted`, `dashboard.kpi.deliveriesCancelled`, `stockGapNone`, `unknownChannel` ajoutées ; `revenueGoal` et `collected` supprimées ; `revenueCompare` et `deliveriesDetail` reformulées.

### Données

- Ni table, ni migration, ni permission nouvelle (`reports.read` existe depuis AUTH-01, ADMIN seul). Rien n'est écrit au journal (lecture).

## Règles métier à respecter par les autres tickets

- **Jour** = jour d'Antananarivo (`DashboardServiceImpl.BUSINESS_ZONE`, horloge `Clock`) ; la veille est `ReportPeriod.previous()`.
- **CA et ventes** : ventes `VALIDATED` seulement (RPT-01 / VTE-08). Une vente issue d'une livraison (LIV-03) est déjà une vente du canal Livraison : elle n'est pas comptée une seconde fois dans `deliveries`, qui ne porte que des compteurs de commandes.
- **Livraisons** : `total` = livrées + en tournée + à préparer + échouées ; les annulées sont à part.
- **Écart de stock** : par ligne d'article, `counted - theoretical` si le comptage du soir existe (JR-05), sinon `gap` du serveur (`systemStock - theoretical`, JR-04). `units` est la somme signée de ces écarts ; `articles` compte les articles distincts (un article manquant dans deux emplacements compte une fois). JR-06 doit rester cohérent avec cette définition.
- **Droit** : `reports.read` côté serveur ; le front ne fait que masquer.

## Fichiers concernés

- Back (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/dashboard/`) : `controller/DashboardController.java`, `service/DashboardService.java`, `service/impl/DashboardServiceImpl.java`, `dto/DashboardRDto.java`, `RevenueKpiRDto.java`, `SalesKpiRDto.java`, `DeliveryKpiRDto.java`, `StockGapKpiRDto.java`, plus les `package-info.java`.
- Front (`valimbavaka-ui/src/app/`) : `features/dashboard/data-access/dashboard-models.ts`, `dashboard-service.ts`, `dashboard-fixtures.ts`, `dashboard-testing.ts` ; `features/dashboard/components/dashboard-kpis/` (`.ts`, `.html`, `.css`) ; `features/dashboard/pages/dashboard-page/` ; `components/dashboard-hero`, `revenue-chart-card`, `revenue-split-card` (type d'entrée `DashboardDemoRDto`) ; `public/i18n/fr.json`, `mg.json`.
- Tests : `feature/dashboard/service/impl/DashboardServiceImplTest.java`, `feature/dashboard/controller/DashboardIT.java` ; `dashboard-service.spec.ts`, `dashboard-kpis.spec.ts`, `dashboard-page.spec.ts`, `features/demo-pages.a11y.spec.ts`.

## Dépendances

**Ce ticket s'appuie sur** :
- [JR-04](../JR-04/guide.md) et [JR-05](../JR-05/guide.md) : réconciliation et comptage (écart de stock).
- [JR-03](../JR-03/guide.md) : `DeliveryDayService`, même découpage « feature de lecture » (`dayoverview`).
- [RPT-01](../RPT-01/guide.md) : `ReportSalesService`, `ReportPeriod`, `ReportFilter` ; [VTE-08](../VTE-08/guide.md) : CA par canal.
- [DSH-03](../DSH-03/guide.md) : alertes de stock, lues à part (voir ci-dessous).

**Ces tickets s'appuient sur lui** : [DSH-02](../DSH-02/description.md) et [DSH-04](../DSH-04/description.md) (même écran).

## Comment l'utiliser depuis un autre ticket

Brancher DSH-02 ou DSH-04 sans toucher aux 4 indicateurs :

1. **Back** : ajouter dans `feature/dashboard` leur propre endpoint (par exemple `GET /api/v1/dashboard/revenue-trend`) avec leur service et leur DTO ; ne pas ajouter de champ à `DashboardRDto` (un échec d'un bloc ne doit pas casser les autres). DSH-02 : appeler `ReportSalesService.getSales(new ReportFilter(ReportPeriod.containing(ReportGranularity.DAY, jour), null, null))` pour chacun des 7 jours (un aller-retour SQL par jour, 7 au total) ; DSH-04 : `ReportFilter` avec `salesChannelId` ou `rangeId`, et `ChannelDashboardService.getDashboard(jour, jour)` pour la part par canal (`revenueSharePercent`). Même droit `reports.read`.
2. **Front** : ajouter dans `DashboardService` une méthode qui lit le nouvel endpoint, une `rxResource` dans `DashboardPage` (gardée par `canSeeCockpit`), et supprimer de `DashboardDemoRDto` et de `dashboard-fixtures.ts` les champs remplacés (`revenueLast7Days`, `revenueLast4Weeks` pour DSH-02 ; `revenueByRange`, `revenueByChannel` pour DSH-04). `RevenueChartCard` et `RevenueSplitCard` prennent aujourd'hui `data: DashboardDemoRDto` ; ils passeront à un tableau de points ou de parts, sans que `DashboardKpis` change.

```ts
// DashboardPage : la ressource d'un nouveau bloc est indépendante de l'agrégat
protected readonly revenueTrend = rxResource({
  params: () => (this.canSeeCockpit() ? true : undefined),
  stream: () => this.dashboardService.revenueTrend(),
});
```

## Points d'attention

- **Hypothèse (droit)** : « administrateur » se traduit par `reports.read` (ADMIN seul), comme VTE-08 et JR-03, sans migration. La vendeuse et le livreur gardent l'accès à la page `/dashboard` (`dashboard.read`, inchangé) mais le serveur leur refuse l'agrégat (403) : ils ne voient que le message de réservation et, pour la vendeuse, les alertes de stock (DSH-03, `stocks.read`). Une version « chacun ses chiffres » (CA de la vendeuse, tournée du livreur) demanderait un agrégat par rôle : à décider avec le client.
- **Hypothèse (comparaison)** : hier, pas « même jour la semaine dernière » comme la maquette ; le libellé est devenu « vs hier ». La comparaison est aussi celle de RPT-02 (période précédente d'un jour = la veille).
- **Hypothèse (écart de stock)** : somme signée des écarts de toutes les lignes (boutique, livraison, salon) ; avant le comptage du soir, c'est l'écart du stock du système. Il est affiché « — » tant que la journée n'est pas ouverte.
- **Retirés de la maquette** (aucune source) : l'objectif journalier de CA (jauge et pourcentage sous le CA), « Encaissé à la livraison » et « tournées actives » sous les livraisons. L'objectif mensuel du bandeau reste en démonstration (COM-04 porte l'objectif de la vendeuse, pas celui de l'entreprise).
- **Restent en démonstration** : le graphique du CA (DSH-02), la répartition par gamme et par canal (DSH-04), le flux d'activité (JR-03 `GET /api/v1/days/live` le fournit déjà côté serveur), les meilleurs articles, les vendeuses, l'objectif mensuel, l'heure et l'auteur de l'ouverture du bandeau. La route garde donc `underConstruction: true`.
- **Coût d'une lecture** : 2 lectures de CA (jour, veille), 1 lecture par canal, 1 comptage de livraisons et la réconciliation de clôture (3 lectures du registre de stock). Pas de relecture automatique à intervalle : l'écran se charge à l'ouverture (JR-03 relit déjà toutes les 15 s sur `/day/live`).
- **Qodana** : `DashboardServiceImpl` a un constructeur package-private qui ne prend que des interfaces publiques.
