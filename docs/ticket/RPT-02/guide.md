# Guide d'intégration — RPT-02 · Indicateurs comparés à la période précédente

Statut : ✅ écrit le 2026-11-12 · Ticket : [#99](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/99) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

`GET /api/v1/reports` (RPT-01) renvoie maintenant, en plus des indicateurs de la période, ceux de la **période précédente** (la veille, le mois d'avant, l'année d'avant, avec les mêmes filtres canal et gamme) et la **variation en %** de chaque indicateur : chiffre d'affaires, ventes validées, panier moyen. Aucun nouvel endpoint, aucune migration, aucune permission : le contrat de RPT-01 est enrichi d'un bloc `comparison`. L'écran `/reports` affiche la valeur, le badge de variation et la valeur précédente de chaque indicateur.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/reports?granularity=&date=&salesChannelId=&rangeId=` | `reports.read` (admin) | inchangée (RPT-01) | 200 `ReportRDto` enrichi de `comparison` | inchangées : 400, 401, 403, 422 |

- `ReportRDto.comparison` = `ReportComparisonRDto` (`feature/report/dto/ReportComparisonRDto.java`) : `from`, `to` (période précédente, bornes incluses), `sales` (`ReportSalesRDto` de cette période : `revenue`, `salesCount`, `averageBasket`) et `variation`.
- `ReportVariationRDto` : `revenue`, `salesCount`, `averageBasket`, en pourcentage à **une décimale** (`14.2` pour +14,2 %, négatif pour une baisse). **`null` quand la valeur précédente vaut 0** : pas de pourcentage, jamais de division par zéro ni de valeur infinie. Le JSON porte alors `null`, pas une valeur absente.
- `sales` (période courante) est inchangé.

### Services et types Java réutilisables

| Type | Rôle |
|---|---|
| `feature.report.domain.ReportVariation` | `percent(BigDecimal current, BigDecimal previous)` et `percent(long, long)` : `(current − previous) / previous × 100`, `RoundingMode.HALF_UP`, une décimale, `null` si `previous` vaut 0 |
| `feature.report.service.impl.ReportServiceImpl#compare` | appelle `ReportSalesService.getSales` une seconde fois avec `period.previous()` et les mêmes filtres, puis calcule les trois variations |

Aucun agrégat dupliqué : la période précédente passe par le même `where` que la période courante (`ReportSaleRepository.totals` / `totalsByRange`). Le coût est une seconde requête d'agrégat par appel.

### Frontend

- `features/reports/data-access/report-models.ts` : types `ReportComparisonRDto`, `ReportVariationRDto` ; `ReportRDto.comparison`.
- `pages/reports-page` : la section « Synthèse de la période » (RPT-01 et RPT-02) affiche, pour chaque carte, la valeur, le badge `+14,2 %` / `−25,0 %` / `n.d.` (vert, rouge, neutre) et « vs … période précédente » ; la période précédente est écrite sous la période.
- `ReportDemoService` ne fournit plus les indicateurs : il reste la courbe (RPT-03), les gammes (RPT-04) et le classement (RPT-05). Le type `ReportKpiRDto`/`ReportKpisRDto` et la carte « Livraisons réussies » de la maquette sont retirés.
- Traductions : `reports.summary.previousPeriod`, `reports.variation.none` (fr, mg) ; `reports.kpis` et `reports.kpi.delivery` supprimées.

### Données

Aucune migration, aucune permission nouvelle, aucune écriture au journal (lecture).

## Règles métier à respecter par les autres tickets

- La période précédente est toujours `ReportPeriod.previous()` : un mois ou une année en cours est comparé **en entier** au mois ou à l'année précédente en entier (pas de comparaison « à date égale »).
- Le filtre canal et gamme s'applique des deux côtés de la comparaison.
- Variation = pourcentage de la valeur précédente, arrondi à 0,1 (demi vers le haut) ; `null` si la valeur précédente est 0. Un autre ticket qui compare deux périodes (DSH-*) appelle `ReportVariation.percent` au lieu de refaire le calcul.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/report/`)
- `domain/ReportVariation.java` (nouveau), `dto/ReportComparisonRDto.java` (nouveau), `dto/ReportVariationRDto.java` (nouveau)
- `dto/ReportRDto.java` (champ `comparison`), `service/impl/ReportServiceImpl.java` (`getReport`, `compare`), `service/ReportService.java` (Javadoc)

**Frontend** (`valimbavaka-ui/src/app/features/reports/`)
- `data-access/report-models.ts`, `report-demo-service.ts`, `report-fixtures.ts`, `report-service.ts` (Javadoc)
- `pages/reports-page/reports-page.{ts,html}` ; `public/i18n/fr.json`, `mg.json`

**Tests**
- Backend : `feature/report/domain/ReportVariationTest.java`, `feature/report/service/impl/ReportServiceImplTest.java`, `feature/report/controller/ReportIT.java`
- Frontend : `pages/reports-page/reports-page.spec.ts`, `data-access/report-service.spec.ts`, `features/demo-pages.a11y.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** : [RPT-01](../RPT-01/guide.md) (`ReportPeriod.previous()`, `ReportSalesService.getSales`, `ReportRDto`).

**Ces tickets s'appuient sur lui** : aucun ticket ne dépend de RPT-02. [RPT-06](../RPT-06/description.md) exporte le `ReportRDto` : il contient désormais `comparison`. DSH-01/02 peuvent réutiliser `ReportVariation`.

## Comment l'utiliser

```java
var variation = ReportVariation.percent(current.revenue(), previous.revenue()); // null si previous vaut 0
```

```ts
const badge = data.comparison.variation.revenue; // number | null, en %
```

## Points d'attention

- **Hypothèse (indicateurs)** : la description demande « chaque indicateur » sans les nommer. Sont livrés les trois indicateurs que l'API de RPT-01 calcule et qui respectent les filtres canal et gamme : chiffre d'affaires, ventes, panier moyen. Le taux de livraisons réussies de la maquette est **retiré de l'écran** : une livraison n'a ni canal ni gamme (les filtres ne s'y appliqueraient pas), sa définition (livrées / terminées, date de référence) n'est pas arrêtée, et la note de RPT-01 le citait sans que la description le demande. À arbitrer par le PO ; si on le veut, une carte « Livraisons réussies » s'ajoute au bloc `comparison` (variation en points plutôt qu'en %), côté `feature/delivery`.
- **Hypothèse (période en cours)** : comparaison en entier contre en entier ; une période en cours paraît donc en retard sur la précédente jusqu'à sa fin. À valider par le PO.
- **Hypothèse (précédente à 0)** : pas de pourcentage (`null`, « n.d. » à l'écran) plutôt que +100 % ou un infini.
- **Hypothèse (variation du panier)** : calculée sur le panier moyen arrondi au centime de chaque période.
- Les sections courbe, gammes et classement restent en démonstration (RPT-03 à RPT-05) ; le marqueur « En construction » reste affiché.
