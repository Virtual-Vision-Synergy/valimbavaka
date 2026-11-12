# Guide d'intégration — RPT-04 · Répartition du CA par gamme

Statut : ✅ écrit le 2026-11-12 · Ticket : [#101](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/101) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

`GET /api/v1/reports` (RPT-01) renvoie maintenant aussi `ranges` : le **chiffre d'affaires de chaque gamme** sur la période (et le canal choisis) et sa **part en pourcentage**, calculés en une seule requête SQL groupée par gamme. Les parts ont une décimale et totalisent exactement 100,0. Aucun nouvel endpoint, aucune migration, aucune permission. L'écran `/reports` affiche l'anneau et la légende avec le composant `RangeDonut`, branché sur l'API.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/reports?granularity=&date=&salesChannelId=&rangeId=` | `reports.read` (admin) | inchangée (RPT-01) | 200 `ReportRDto` enrichi de `ranges` | inchangées : 400, 401, 403, 422 |

- `ReportRDto.ranges` = `ReportRangesRDto` (`feature/report/dto/ReportRangesRDto.java`) : `revenue` (somme des CA des gammes, 2 décimales, 0 sans vente) et `items`.
- `items` = liste de `ReportRangeRDto` : `rangeId`, `code` (ex. `BOTANIKA`, sert à la couleur), `label`, `revenue` (2 décimales), `share` (% à 1 décimale). Triée par CA décroissant puis libellé ; **vide quand aucune vente** ; seules les gammes qui ont vendu y figurent.
- `sales`, `comparison` et `trend` sont inchangés.

### Services et types Java réutilisables

| Type | Rôle |
|---|---|
| `ReportSalesService#getRanges(ReportFilter)` | répartition d'une période et d'un canal ; **`filter.rangeId()` est ignoré** (`service/impl/ReportSalesServiceImpl.java`) |
| `ReportSaleRepository#revenueByRange` | un agrégat `group by` gamme, **même `where`** que `totals` / `totalsByRange` sans la restriction de gamme ; renvoie des `RangeRevenue(rangeId, code, label, revenue)` |
| `domain/ReportShare#distribute(List<BigDecimal>)` | parts en % à une décimale, total exactement 100,0 (plus fort reste) ; liste à total nul : parts à 0,0 |

La jointure lit `ProductRange` (référentiel) pour le code et le libellé, y compris d'une gamme désactivée depuis : l'historique reste lisible.

### Frontend

- `features/reports/data-access/report-models.ts` : `ReportRangeRDto`, `ReportRangesRDto`, `ReportRDto.ranges`.
- `components/range-donut/` : entrée `ranges` (`ReportRangesRDto`) ; arcs proportionnels aux parts de l'API (aucun recalcul), couleur par `data-range = code` (ART-03, repli gris pour un code inconnu), légende libellé / montant / part en « 66,7 % ». Aucune dépendance npm ajoutée.
- `pages/reports-page` : la section « CA par gamme » lit `summary` (même appel que la synthèse, mêmes période et canal) ; états chargement, erreur avec « Réessayer » et vide (« Aucune vente sur la période ») ; note quand le filtre gamme est actif, note d'écart quand les remises globales séparent la synthèse des gammes.
- `ReportDemoService` ne fournit plus les gammes : il reste le classement (RPT-05). Fixtures et type de la démo des gammes supprimés.
- Traductions : `reports.ranges.emptyTitle`, `emptyMessage`, `rangeIgnored`, `gap` (fr, mg).

### Données

Aucune migration, aucune permission, aucune écriture au journal (lecture).

## Règles métier à respecter par les autres tickets

- Ventes `VALIDATED`, jour = `sales.business_date`, canal facultatif : même `where` que RPT-01.
- **CA d'une gamme = somme des `line_total` des lignes d'articles de la gamme, avant la remise globale de la vente** (règle de RPT-01). `ranges.revenue` est donc la somme des lignes ; il égale `sales.revenue` sans filtre de gamme **quand aucune vente de la période n'a de remise globale**, sinon il lui est supérieur du total de ces remises.
- **Parts** : `share` = CA de la gamme / `ranges.revenue` × 100, une décimale, méthode du plus fort reste : le total fait toujours exactement 100,0 (un arrondi simple donnerait 99,9 ou 100,1). En cas d'égalité des restes, la gamme au plus fort CA gagne le dixième. CA nul : liste vide, ou parts à 0,0 si des lignes valent 0 ; jamais de division par zéro. C'est la même règle de rééquilibrage que celle voulue pour le cockpit (DSH-04).
- **Filtre gamme** : ignoré pour cette section (une seule gamme vaudrait toujours 100 %). Les filtres période et canal s'appliquent.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/report/`)
- `domain/ReportShare.java`, `dto/ReportRangeRDto.java`, `dto/ReportRangesRDto.java`, `dto/ReportRDto.java` (champ `ranges`), `repository/RangeRevenue.java`
- `repository/ReportSaleRepository.java` (`revenueByRange`), `service/ReportSalesService.java`, `service/impl/ReportSalesServiceImpl.java` (`getRanges`), `service/impl/ReportServiceImpl.java`

**Frontend** (`valimbavaka-ui/src/app/features/reports/`)
- `components/range-donut/*`, `pages/reports-page/reports-page.{ts,html,css}`, `data-access/report-models.ts`, `report-demo-service.ts`, `report-fixtures.ts` ; `public/i18n/fr.json`, `mg.json`

**Tests**
- Backend : `domain/ReportShareTest`, `service/impl/ReportSalesServiceImplTest`, `ReportServiceImplTest`, `controller/ReportIT`
- Frontend : `reports-page.spec.ts`, `report-service.spec.ts`, `features/demo-pages.a11y.spec.ts` (réponse `/api/v1/reports` avec `ranges`)

## Dépendances

**Ce ticket s'appuie sur** : [ART-03](../ART-03/guide.md) (couleur par code de gamme), [RPT-01](../RPT-01/guide.md) (période, filtre, repository), [RPT-02](../RPT-02/guide.md) et [RPT-03](../RPT-03/guide.md) (`ReportRDto` enrichi), [SYS-05](../SYS-05/guide.md) (gammes).

**Ces tickets s'appuient sur lui** : aucun. [RPT-06](../RPT-06/description.md) exporte le `ReportRDto` : il contient désormais `ranges`. DSH-04 peut appeler `ReportSalesService#getRanges`.

## Comment l'utiliser

```java
var ranges = reportSalesService.getRanges(new ReportFilter(period, channelId, null)); // ReportRangesRDto
```

## Points d'attention

- **Hypothèse (somme et CA total)** : le critère « la somme par gamme correspond au CA total » est vrai quand aucune remise ne porte sur la vente entière. Pour rester cohérent avec RPT-01 (remise globale non répartie par ligne), l'écart est affiché à l'écran plutôt que réparti au prorata. À valider par le PO ; la répartition au prorata serait un changement de `revenueByRange` et de la règle de RPT-01.
- **Hypothèse (filtre gamme)** : ignoré pour cette section, avec une note à l'écran. À valider par le PO.
- **Hypothèse (parts)** : total exactement 100,0 par la méthode du plus fort reste, pour s'accorder avec DSH-04 (non mergé au moment de l'écriture : à vérifier à son merge).
- Les traductions malgaches des quatre nouveaux textes sont de bonne foi : à faire relire.
- La section « Produits les plus vendus » reste en démonstration (RPT-05) : le marqueur « En construction » reste affiché.
