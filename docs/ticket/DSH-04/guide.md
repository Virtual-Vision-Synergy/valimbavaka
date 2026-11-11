# Guide d'intégration — DSH-04 · Répartition du CA par gamme et canal

Statut : ✅ CI verte · Ticket : [#97](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/97) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Le cockpit (`/dashboard`, onglet « Analyse ») montre le chiffre d'affaires du jour réparti par gamme (barres) et par canal (barre empilée et légende) : chaque ligne affiche sa valeur et sa part relative en pourcentage. Les données viennent d'un endpoint dédié de `feature/dashboard`, lu à part de l'agrégat de DSH-01 : un échec de la carte ne casse pas les indicateurs.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/dashboard/revenue-split` | `reports.read` (ADMIN) | aucune | `RevenueSplitRDto` : `date`, `byRange[]` de `RangeShareRDto` (`rangeId`, `code`, `label`, `revenue`, `sharePercent`), `byChannel[]` de `ChannelShareRDto` (`salesChannelId`, `label` nullable, `revenue`, `sharePercent`) | 401, 403 |

Listes de taille fixe (une entrée par gamme active et par canal), donc non paginées. Une gamme ou un canal sans vente est présent, à 0.

### Services et ports Java réutilisables

- `RevenueSplitService.getToday()` (`feature/dashboard/service`), implémenté par `RevenueSplitServiceImpl` (`RevenueSplitServiceImpl.java:51`) : gammes = une lecture de `ReportSalesService.getSales(new ReportFilter(jour, null, rangeId))` par gamme active (`byRange`, ligne 57) ; canaux = `ChannelDashboardService.getDashboard(jour, jour).channels()` (`byChannel`, ligne 79). Aucun calcul de CA propre, donc aucune règle dupliquée (ventes `VALIDATED`, `business_date`). Constructeur package-private, il ne prend que des interfaces publiques.
- `PercentageShares.of(revenues)` (package-private, `PercentageShares.java:25`) : parts à une décimale qui totalisent exactement 100,0 (méthode du plus grand reste), toutes à 0,0 quand le total est nul.
- `RevenueSplitController` (nouveau, route `GET /api/v1/dashboard/revenue-split`) : un contrôleur à part pour ne pas toucher `DashboardController` (DSH-02 y ajoute sa route).

### Frontend

- Route : inchangée (`/dashboard`, onglet `?tab=analysis`).
- Service : `DashboardService.revenueSplit()` (`dashboard-service.ts:24`) ; la `rxResource` `revenueSplit` de `DashboardPage` (`dashboard-page.ts:114`) est gardée par `canSeeCockpit`.
- Types : `revenue-split-models.ts` (`RevenueSplitRDto`, `RangeShareRDto`, `ChannelShareRDto`, mêmes noms que le backend). Jeux de test : `revenue-split-testing.ts`.
- Composant : `RevenueSplitCard` prend `split` (absent pendant le chargement), `failed` et émet `retry` ; états chargement, erreur, vide (aucun CA) et données. La couleur d'une gamme vient de `data-range="<code>"` (`styles/range.css`), celle d'un canal de sa position (3 couleurs de la charte, répétées au-delà).
- Retirés : `revenueByRange` et `revenueByChannel` de `DashboardDemoRDto` et de `dashboard-fixtures.ts`, ainsi que les types `RangeRevenueRDto`, `ChannelRevenueRDto` et `SalesChannel`.

### Données

- Tables et migrations Liquibase : aucune.
- Écriture dans le journal : aucune (lecture seule).

## Règles métier à respecter par les autres tickets

- Seules les ventes validées comptent, par `business_date` (règle de `ReportSalesService` et de `ChannelDashboardService`) ; les ventes annulées sortent de la répartition.
- Période = le jour calendaire d'Antananarivo (`DashboardServiceImpl.BUSINESS_ZONE`), comme le reste du cockpit.
- Parts en pourcentage à une décimale, somme exacte de 100,0 par axe ; CA nul : toutes les parts à 0,0 (jamais de division par zéro) et état vide à l'écran.
- Droit `reports.read` comme DSH-01 : la vendeuse et le livreur n'ont pas la carte (403, aucun appel du front).

## Fichiers concernés

- Backend : `feature/dashboard/controller/RevenueSplitController`, `service/RevenueSplitService`, `service/impl/RevenueSplitServiceImpl`, `service/impl/PercentageShares`, `dto/RevenueSplitRDto`, `dto/RangeShareRDto`, `dto/ChannelShareRDto`.
- Frontend : `features/dashboard/components/revenue-split-card/*`, `data-access/revenue-split-models.ts`, `data-access/dashboard-service.ts`, `pages/dashboard-page/*`.
- Tests : `RevenueSplitIT`, `RevenueSplitServiceImplTest`, `PercentageSharesTest`, `revenue-split.spec.ts`, `dashboard-page.spec.ts`.

## Dépendances

**Ce ticket s'appuie sur** :
- [ART-03](../ART-03/description.md) · [guide](../ART-03/guide.md) (gamme des articles, couleur par code)
- [VTE-01](../VTE-01/description.md) · [guide](../VTE-01/guide.md) (ventes)
- [SYS-05](../SYS-05/description.md) · [guide](../SYS-05/guide.md) (référentiels gammes et canaux)
- [DSH-01](../DSH-01/description.md) · [guide](../DSH-01/guide.md) (feature `dashboard`, `reports.read`), RPT-01 (`ReportSalesService`), VTE-08 (`ChannelDashboardService`)

**Ces tickets s'appuient sur lui** :
- _(rien)_

## Comment l'utiliser depuis un autre ticket

```ts
// DashboardPage : la ressource de la répartition est indépendante de l'agrégat
protected readonly revenueSplit = rxResource({
  params: () => (this.canSeeCockpit() ? true : undefined),
  stream: () => this.dashboardService.revenueSplit(),
});
```

## Points d'attention

- **Hypothèse (période)** : le jour, comme le reste du cockpit (sous-titre « aujourd'hui »). Une répartition sur le mois ou une période choisie relève des rapports (RPT-03, RPT-04).
- **Hypothèse (droit)** : « administrateur » = `reports.read` (ADMIN seul), comme DSH-01, sans migration.
- **Deux totaux différents** : le CA par canal est celui des ventes (après remise de la vente) ; le CA par gamme est celui des lignes d'articles de la gamme (`ReportSaleRepository.totalsByRange`, avant la remise de toute la vente). Chaque axe est relatif à son propre total ; avec une remise, la somme des gammes dépasse celle des canaux.
- **Part des canaux recalculée** : `revenueSharePercent` de VTE-08 arrondit chaque canal seul (somme parfois 99,9 ou 100,1). La carte utilise donc `PercentageShares` sur les CA des canaux ; l'écart avec VTE-08 est de 0,1 point au plus.
- **Gammes désactivées** : seules les gammes actives sont listées (`ReferentialService.getProductRanges`) ; le CA d'une gamme désactivée depuis n'apparaît pas, contrairement aux canaux désactivés (libellé `null`, affiché « Canal désactivé »).
- **Coût** : 1 lecture du rapport de ventes par gamme active (3 aujourd'hui) et 1 lecture par canal (VTE-08), à chaque ouverture de l'onglet. Pas de relecture automatique.
- **Maquette** : les canaux fixes `SHOP`, `FACEBOOK`, `PHONE` de la démonstration deviennent ceux du référentiel (libellé de l'API) ; la légende affiche aussi la part de chaque canal.
- **Conflits attendus avec DSH-02** (branche non mergée) : `dashboard-page.*`, `dashboard-service.ts`, `dashboard-models.ts`, `dashboard-fixtures.ts` et `dashboard-page.spec.ts` sont touchés par les deux tickets, à des endroits voisins ; le reste (contrôleur, services, types, tests) est dans des fichiers distincts.
