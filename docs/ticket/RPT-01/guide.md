# Guide d'intégration — RPT-01 · Rapports filtrables journaliers/mensuels/annuels

Statut : ✅ écrit le 2026-11-11 · Ticket : [#98](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/98) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

`GET /api/v1/reports` donne le chiffre d'affaires, le nombre de ventes validées et le panier moyen du jour, du mois ou de l'année civile qui contient une date, avec un filtre facultatif par canal de vente et par gamme. Il est réservé à l'administrateur (`reports.read`). Il pose le socle des rapports : une **période bornée** (`ReportPeriod`), un **filtre** (`ReportFilter`) et un **agrégat SQL réutilisable** (`ReportSalesService`) que RPT-02 à RPT-06 et DSH-01/02/04 appellent au lieu de réécrire l'agrégat. L'écran `/reports` affiche la synthèse réelle ; ses autres sections restent en démonstration.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie (`XxxRDto`) | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/reports?granularity=&date=&salesChannelId=&rangeId=` | `reports.read` (admin) | paramètres d'URL facultatifs : `granularity` = `DAY`/`MONTH`/`YEAR`, `date` = `yyyy-MM-dd`, `salesChannelId`, `rangeId` (UUID) ; aucun corps | 200 `ReportRDto` | 400 (valeur mal formée), 401, 403 (vendeuse, livreur), 422 (période qui n'a pas commencé) |

- `ReportRDto` (`feature/report/dto/ReportRDto.java`) : `granularity`, `from`, `to` (période effective), `salesChannelId`, `rangeId` (filtres appliqués, `null` = tous), `sales`, et depuis RPT-02 `comparison` (période précédente, voir [RPT-02](../RPT-02/guide.md)).
- `ReportSalesRDto` : `revenue`, `salesCount`, `averageBasket` (CA / ventes, 2 décimales, 0 sans vente). C'est le bloc que les rapports suivants renvoient aussi pour d'autres périodes.
- Défauts : `granularity` = `MONTH`, `date` = aujourd'hui en `Indian/Antananarivo`. Bornes incluses, sur `sales.business_date`.
- Jour = ce jour ; mois = du 1er au dernier jour du mois ; année = du 1er janvier au 31 décembre. Un mois ou une année en cours est rapporté en entier (jusqu'à sa fin).

### Services, ports et types Java réutilisables

| Type | Rôle |
|---|---|
| `feature.report.domain.ReportPeriod` | `containing(granularity, date)` donne la période bornée ; `previous()` donne la période précédente (jour/mois/année d'avant) ; `startsAfter(day)` |
| `feature.report.domain.ReportFilter` | période + canal (nullable) + gamme (nullable) |
| `feature.report.service.ReportSalesService` | `ReportSalesRDto getSales(ReportFilter)` : agrégat des ventes `VALIDATED` d'une période filtrée |
| `feature.report.service.ReportService` | cas d'usage de l'endpoint (défauts, refus d'une période future) |

- Implémentations : `service/impl/ReportSalesServiceImpl.java`, `ReportServiceImpl.java` ; contrôleur mince `controller/ReportController.java`.
- Requêtes : `repository/ReportSaleRepository.java` — `totals` (somme de `sales.total`, une requête) et `totalsByRange` (jointure ligne de vente → article, somme de `line_total`, `count(distinct sale)`), toutes deux en `select new SalesTotal(...)` : aucune vente chargée en mémoire.
- La feature `report` est une feature de lecture hors des features métier (modèle `dayoverview`) : elle dépend de `sale` et `article`, aucune ne dépend d'elle (ArchUnit : pas de cycle).

### Frontend

- Route `/reports` (`reports.routes.ts`, garde `permissionGuard('reports.read')` dans `app.routes.ts`).
- `features/reports/data-access/report-service.ts` : `get({ granularity, date, salesChannelId, rangeId })` ; types `ReportRDto`, `ReportSalesRDto`, `ReportQuery` dans `report-models.ts`.
- `pages/reports-page` : période (jour/mois/année), date de référence (max aujourd'hui), canal et gamme lus via `ReferentialService`, « Réinitialiser » ; section « Synthèse de la période » (CA, ventes, panier moyen) branchée sur l'API.
- Les autres sections (indicateurs comparés, courbe, gammes, classement) restent en démonstration : `report-demo-service.ts` (`ReportDemoService`, type `ReportDemoRDto`), à remplacer ticket par ticket. Le filtre vendeuse de la maquette est retiré (hors critère, non géré par l'API).
- Traductions : `reports.date`, `reports.summary.*` (fr, mg).

### Données

- Aucune migration, aucune permission nouvelle (`reports.read` existe depuis AUTH-01, ADMIN seul). Aucune écriture au journal (lecture).

## Règles métier à respecter par les autres tickets

- **CA = somme de `sales.total`** (après remise) des ventes `VALIDATED` ; une vente annulée ou contre-passée sort du CA (règle de VTE-07/VTE-08, identique : le total du rapport sans filtre égale le total du tableau par canal).
- **CA d'une gamme = somme des `line_total` des lignes d'articles de cette gamme**, avant la remise globale de la vente (non répartie par ligne). Le nombre de ventes d'une gamme compte les ventes qui ont au moins une ligne de cette gamme.
- **Jour** = `sales.business_date`, jamais la date UTC de `created_at`.
- Toujours passer par `ReportPeriod` : aucun rapport sans borne ; la période précédente se calcule avec `previous()`.
- Un identifiant de canal ou de gamme inconnu (ou désactivé depuis) ne donne pas d'erreur : le rapport est à zéro, ce qui permet de relire l'historique d'une gamme retirée.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/report/`)
- `controller/ReportController.java`, `service/ReportService.java`, `service/ReportSalesService.java`, `service/impl/ReportServiceImpl.java`, `service/impl/ReportSalesServiceImpl.java`
- `domain/ReportGranularity.java`, `domain/ReportPeriod.java`, `domain/ReportFilter.java`
- `repository/ReportSaleRepository.java`, `repository/SalesTotal.java`
- `dto/ReportRDto.java`, `dto/ReportSalesRDto.java`

**Frontend** (`valimbavaka-ui/src/app/features/reports/`)
- `data-access/report-service.ts`, `report-models.ts`, `report-demo-service.ts`, `report-fixtures.ts`
- `pages/reports-page/*`, `reports.routes.ts` ; `public/i18n/fr.json`, `mg.json`

**Tests**
- Backend : `feature/report/domain/ReportPeriodTest.java`, `feature/report/service/impl/ReportSalesServiceImplTest.java`, `ReportServiceImplTest.java`, `feature/report/controller/ReportIT.java`
- Frontend : `data-access/report-service.spec.ts`, `pages/reports-page/reports-page.spec.ts`, `features/demo-pages.a11y.spec.ts` (réponse `/api/v1/reports` ajoutée)

## Dépendances

**Ce ticket s'appuie sur** : [VTE-01](../VTE-01/guide.md) (entité `Sale`, lignes), [VTE-08](../VTE-08/guide.md) (règles de CA, `reports.read`), [SYS-05](../SYS-05/guide.md) (canaux, gammes, `ReferentialService`).

**Ces tickets s'appuient sur lui** :
- [RPT-02](../RPT-02/description.md) : appelle `getSales` pour `period` et `period.previous()` et calcule la variation en %.
- [RPT-03](../RPT-03/description.md) : ajoute un agrégat par jour (période mensuelle) ou par mois (période annuelle) dans `ReportSaleRepository` (même `where` que `totals`, `group by business_date`).
- [RPT-04](../RPT-04/description.md) : ajoute le CA par gamme (`totalsByRange` sans le filtre de gamme, `group by a.rangeId`).
- [RPT-05](../RPT-05/description.md) : ajoute le classement des articles (`group by l.articleId`, `order by`, limite).
- [RPT-06](../RPT-06/description.md) : exporte le `ReportRDto` d'une période filtrée.
- DSH-01, DSH-02, DSH-04 : appellent `ReportSalesService.getSales` avec `ReportPeriod.containing(DAY, …)` ou, pour 7 jours, un `ReportPeriod` construit par le ticket.

## Comment l'utiliser depuis un autre ticket

```java
var period = ReportPeriod.containing(ReportGranularity.MONTH, LocalDate.of(2026, 11, 11));
var current = reportSalesService.getSales(new ReportFilter(period, null, null));
var previous = reportSalesService.getSales(new ReportFilter(period.previous(), null, null));
```

```ts
this.reportService.get({ granularity: 'YEAR', date: '2026-11-11', salesChannelId, rangeId });
```

## Points d'attention

- **Hypothèse (période)** : « journaliers/mensuels/annuels » = le jour, le mois ou l'année civile qui contient une date de référence (aujourd'hui par défaut), pas de plage libre (la plage libre existe dans VTE-08 pour les canaux). À valider par le PO.
- **Hypothèse (périodes futures)** : une période qui n'a pas commencé est refusée (422) ; la période en cours est rapportée en entier.
- **Hypothèse (permission)** : « administrateur » traduit par `reports.read`, sans migration.
- **Hypothèse (gamme)** : voir la règle ci-dessus (lignes avant remise globale). Les promotions sont déjà dans `line_total` ; seule la remise de vente globale n'est pas répartie.
- **Périmètre** : seules les ventes sont agrégées. Les prestations salon (SRV), les livraisons (taux de réussite) et les commissions ne sont pas dans le CA de ce rapport ; ils relèvent de RPT-02 (livraisons) ou de DSH-01.
- **Filtre vendeuse** : retiré de la maquette (hors critère « gamme/canal »).
- L'écran garde des données de démonstration pour les sections RPT-02 à RPT-05 : le marqueur « En construction » reste affiché.
