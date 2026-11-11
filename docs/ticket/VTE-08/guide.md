# Guide d'intégration — VTE-08 · Tableau de bord des ventes par canal

Statut : ✅ écrit le 2026-11-11 · Ticket : [#69](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/69) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

`GET /api/v1/sales/channel-dashboard` donne, pour une période de jours ouvrés (mois en cours par défaut), le chiffre d'affaires, le nombre de ventes et la part du CA total de **chaque canal** de vente, du meilleur au moins bon, ainsi que les totaux de la période. Seules les ventes validées comptent. Il est réservé à l'administrateur (`reports.read`). L'écran `/sales/channels` l'affiche ; un lien y mène depuis « Ventes du jour » pour qui a `reports.read`.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie (`XxxRDto`) | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/sales/channel-dashboard?from=&to=` | `reports.read` (admin) | paramètres d'URL `yyyy-MM-dd` facultatifs (aucun corps) | 200 `ChannelDashboardRDto` | 400 (date mal formée), 401, 403 (vendeuse, livreur), 422 (`from` après `to`) |

- `ChannelDashboardRDto` (`feature/sale/dto/ChannelDashboardRDto.java`) : `from`, `to` (période effective), `totalRevenue`, `totalSalesCount`, `channels`.
- `ChannelSalesRDto` : `salesChannelId`, `salesChannelLabel` (`null` si le canal a été désactivé depuis), `revenue`, `salesCount`, `revenueSharePercent` (pourcentage, une décimale, 0 si le total est nul).
- `to` absent = aujourd'hui en `Indian/Antananarivo` ; `from` absent = premier jour du mois de `to`. Bornes incluses, sur `sales.business_date`.
- Tri : CA décroissant, puis libellé. Les canaux actifs sans vente figurent à 0 ; un canal désactivé qui a vendu reste listé sans libellé.

### Services et ports Java réutilisables

| Port | Méthode | Rôle |
|---|---|---|
| `feature.sale.service.ChannelDashboardService` | `ChannelDashboardRDto getDashboard(@Nullable LocalDate from, @Nullable LocalDate to)` | Totaux par canal d'une période, ventes `VALIDATED` seulement ; `BusinessException` (422) si `from` > `to`. |

- Implémentation : `service/impl/ChannelDashboardServiceImpl.java` (période par défaut, parts, tri) ; requête d'agrégat `SaleQueryRepository.totalsByChannel` (`select new ChannelSalesTotal(...) … group by canal`, une seule requête) ; libellés lus une fois via `ReferentialService.getSalesChannels`.
- Contrôleur : `controller/ChannelDashboardController.java` (mince, `/api/v1/sales/channel-dashboard`).

### Frontend

- Route `/sales/channels` (`sales.routes.ts`, garde `permissionGuard('reports.read')`, sous la garde `sales.read` de `/sales`).
- `features/sales/data-access/channel-dashboard-service.ts` : `get({ from, to })` ; `channel-dashboard-models.ts` : `ChannelDashboardRDto`, `ChannelSalesRDto`, `ChannelDashboardQuery`.
- `pages/channel-dashboard-page` : sélecteurs « Du » / « Au » (mois en cours par défaut, jour local), deux totaux (CA, ventes), tableau Canal / CA / Ventes / Part du CA total avec barre. Une période à l'envers n'envoie rien et affiche un message.
- `pages/sales-page` : lien « Ventes par canal » visible avec `reports.read`.
- Retiré : le composant de maquette `components/channel-summary` et les types `SalesDayRDto` / `ChannelTotalRDto` (laissés par VTE-07, remplacés par ce ticket).
- Traductions ajoutées (fr, mg) : `sales.dashboard.*`.

### Données

- Aucune migration, aucune permission nouvelle (`reports.read` existe depuis AUTH-01, ADMIN seul). Aucune écriture au journal (lecture).

## Règles métier à respecter par les autres tickets

- **CA = somme de `sales.total` des ventes `VALIDATED`** (après remise) ; les ventes annulées ou contre-passées ne comptent pas (règle posée par VTE-07). RPT-* et DSH-* peuvent appeler `ChannelDashboardService` plutôt que réécrire l'agrégat.
- **Jour** = `sales.business_date`, jamais la date UTC de `created_at`.
- **Part** = CA du canal / CA total × 100, arrondie à une décimale (les parts peuvent donc totaliser 99,9 ou 100,1).

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/sale/`)
- `controller/ChannelDashboardController.java`, `service/ChannelDashboardService.java`, `service/impl/ChannelDashboardServiceImpl.java`
- `repository/ChannelSalesTotal.java`, `repository/SaleQueryRepository.java` (méthode `totalsByChannel`)
- `dto/ChannelDashboardRDto.java`, `dto/ChannelSalesRDto.java`

**Frontend** (`valimbavaka-ui/src/app/features/sales/`)
- `data-access/channel-dashboard-service.ts`, `channel-dashboard-models.ts`, `sales-models.ts` (types de maquette retirés)
- `pages/channel-dashboard-page/*`, `pages/sales-page/sales-page.ts`, `.html`, `sales.routes.ts`
- `public/i18n/fr.json`, `mg.json`

**Tests**
- Backend : `feature/sale/service/impl/ChannelDashboardServiceImplTest.java`, `feature/sale/controller/ChannelDashboardIT.java`
- Frontend : `data-access/channel-dashboard-service.spec.ts`, `pages/channel-dashboard-page/channel-dashboard-page.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** : [VTE-01](../VTE-01/guide.md) (entité `Sale`), [VTE-07](../VTE-07/guide.md) (`SaleQueryRepository`, écran `/sales`), [SYS-05](../SYS-05/guide.md) (canaux de vente, `ReferentialService`).

**Ces tickets s'appuient sur lui** : aucun.

## Comment l'utiliser depuis un autre ticket

```java
var dashboard = channelDashboardService.getDashboard(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
```

```ts
this.channelDashboardService.get({ from: '2026-11-01', to: '2026-11-30' });
```

## Points d'attention

- **Hypothèse (période)** : la description ne fixe pas de période ; mois en cours par défaut, modifiable par deux dates. À valider par le PO.
- **Hypothèse (permission)** : « administrateur » traduit par `reports.read` (seul l'ADMIN l'a), sans nouvelle permission ni migration.
- **Hypothèse (CA)** : CA = total après remise des ventes validées ; une vente annulée ou contre-passée sort du CA.
- **Hypothèse (canaux)** : tous les canaux actifs sont montrés, même à 0, pour comparer ; pas de filtre par vendeuse.
- Pas de graphique : tableau avec barre de part ; un graphique relève de DSH-* / RPT-*.
- Pas d'entrée de menu : l'accès se fait par le lien de l'écran « Ventes du jour » (à rediscuter avec le PO).
