# Guide d'intégration — RPT-05 · Classement des produits les plus vendus

Statut : ✅ écrit le 2026-11-12 · Ticket : [#102](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/102) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

`GET /api/v1/reports` (RPT-01) renvoie maintenant aussi `ranking` : les **10 articles les plus vendus** de la période, filtrés par canal et par gamme, triés par **chiffre d'affaires décroissant**, avec la quantité vendue, la marge brute et la part du CA. Un seul agrégat SQL (groupé par article, trié et coupé en base) plus une somme pour la base des parts. Aucun nouvel endpoint, aucune migration, aucune permission. L'écran `/reports` affiche le tableau branché sur l'API ; `ReportDemoService` et ses fixtures sont supprimés.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/reports?granularity=&date=&salesChannelId=&rangeId=` | `reports.read` (admin) | inchangée (RPT-01) | 200 `ReportRDto` enrichi de `ranking` | inchangées : 400, 401, 403, 422 |

- `ReportRDto.ranking` = `ReportRankingRDto` (`feature/report/dto/ReportRankingRDto.java`) : `size` (taille maximale, 10), `revenue` (CA de **toutes** les lignes de la période et des filtres, 2 décimales : la base des parts, pas la somme des lignes listées) et `items`.
- `items` = `ReportRankedArticleRDto` : `articleId`, `code`, `name`, `quantity` (3 décimales), `revenue` (2 décimales), `margin` (2 décimales, **`null` si le coût d'achat d'une ligne était inconnu**), `share` (% à une décimale). **Vide quand rien n'a été vendu.**
- `sales`, `comparison`, `trend` et `ranges` sont inchangés.

### Services et types Java réutilisables

| Type | Rôle |
|---|---|
| `ReportSalesService#getRanking(ReportFilter)` | classement d'une période ; **période, canal et gamme du filtre s'appliquent tous les trois** (`service/impl/ReportSalesServiceImpl.java:83`) |
| `ReportSaleRepository#topArticles` | un agrégat `group by` article, `order by` CA décroissant puis nom puis code, `limit 10` en base (`repository/ReportSaleRepository.java:117`) ; renvoie des `ArticleSales` |
| `ReportSaleRepository#linesRevenue` | somme des `line_total` du même `where` (sans la coupure), base des parts (`:130`) |
| `ReportSaleRepository.RANKING_SIZE` | taille du classement ; **le `limit` de la requête a la même valeur, à garder identique** (`:21`) |

### Frontend

- `features/reports/data-access/report-models.ts` : `ReportRankedArticleRDto`, `ReportRankingRDto`, `ReportRDto.ranking`.
- `pages/reports-page` : la section « Produits les plus vendus » (pleine largeur) lit `summary`, le même appel que la synthèse (mêmes période, canal et gamme) : tableau (rang, article avec son code, quantité, CA, marge, % du CA), états chargement, erreur avec « Réessayer » et vide (« Aucun article vendu sur la période »). Marge inconnue : « — » avec un texte pour les lecteurs d'écran.
- Supprimés : `ReportDemoService`, `report-fixtures.ts`, les types de démonstration, le bouton « Top 20 » (sans effet).
- Traductions : `reports.top.*` (fr, mg) ; `units` et `all` retirées.
- La route `/reports` **garde `underConstruction`** : il reste les boutons d'export PDF et XLSX en démonstration (toast), c'est RPT-06.

### Données

Aucune migration, aucune permission, aucune écriture au journal (lecture).

## Règles métier à respecter par les autres tickets

- Ventes `VALIDATED`, jour = `sales.business_date`, canal facultatif : même `where` que RPT-01.
- **CA d'un article = somme des `line_total` de ses lignes, avant la remise globale de la vente** (règle de RPT-01) : la somme des lignes peut dépasser `sales.revenue` s'il y a des remises globales.
- **Tri** : CA décroissant (et non la quantité : c'est la consigne du ticket). **Égalité** : nom de l'article, puis code (unique) : l'ordre est le même à chaque appel.
- **Marge brute** = CA de l'article − somme(quantité × coût unitaire figé sur la ligne de vente, `sale_lines.unit_cost`). Le coût est celui du moment de la vente, pas le coût actuel de la fiche. Si **une seule** ligne de l'article n'a pas de coût, la marge est `null` (on ne présente pas un coût partiel comme une marge). Pas de marge en pourcentage : l'écran n'affiche que le montant.
- **Part** = CA de l'article / `ranking.revenue` × 100, une décimale, arrondi classique (demi supérieur) ; CA total nul : 0,0, jamais de division par zéro. Les parts des 10 lignes ne totalisent pas 100 (le reste est hors classement) : la méthode du plus fort reste de RPT-04 ne s'applique pas ici.
- **Taille** : 10 lignes maximum, fixe (aucun paramètre d'appel, donc aucune borne à valider).
- Un article désactivé depuis reste dans le classement (historique lisible).

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/report/`)
- `dto/ReportRankingRDto.java`, `dto/ReportRankedArticleRDto.java`, `dto/ReportRDto.java` (champ `ranking`), `repository/ArticleSales.java`
- `repository/ReportSaleRepository.java` (`topArticles`, `linesRevenue`, `RANKING_SIZE`), `service/ReportSalesService.java`, `service/impl/ReportSalesServiceImpl.java` (`getRanking`), `service/impl/ReportServiceImpl.java`

**Frontend** (`valimbavaka-ui/src/app/features/reports/`)
- `pages/reports-page/reports-page.{ts,html,css}`, `data-access/report-models.ts`, `reports.routes.ts` ; `public/i18n/fr.json`, `mg.json`

**Tests**
- Backend : `service/impl/ReportSalesServiceImplTest`, `ReportServiceImplTest`, `controller/ReportRankingIT`
- Frontend : `reports-ranking.spec.ts`, `reports-page.spec.ts`, `report-service.spec.ts`, `features/demo-pages.a11y.spec.ts` (réponse `/api/v1/reports` avec `ranking`)

## Dépendances

**Ce ticket s'appuie sur** : [RPT-01](../RPT-01/guide.md) (période, filtre, repository), [RPT-02](../RPT-02/guide.md), [RPT-03](../RPT-03/guide.md) et [RPT-04](../RPT-04/guide.md) (`ReportRDto` enrichi, même schéma de requête).

**Ces tickets s'appuient sur lui** : aucun. [RPT-06](../RPT-06/description.md) exporte le `ReportRDto` : il contient désormais `ranking`.

## Comment l'utiliser

```java
var ranking = reportSalesService.getRanking(new ReportFilter(period, channelId, rangeId)); // ReportRankingRDto
```

## Points d'attention

- **Hypothèse (tri)** : par chiffre d'affaires, comme le critère le dit ; la quantité est affichée mais ne départage pas. À valider par le PO.
- **Hypothèse (marge)** : montant brut (CA − coût d'achat figé à la vente), `null` dès qu'un coût manque ; pas de taux de marge. À valider par le PO.
- **Hypothèse (taille)** : 10 articles fixes, sans « voir tout » (le bouton « Top 20 » de la maquette ne faisait rien et a été retiré). Un classement plus long ou paramétrable serait un nouveau paramètre en liste blanche.
- **Hypothèse (filtre gamme)** : appliqué au classement (contrairement à RPT-04), la part se calcule alors sur le CA de la gamme choisie.
- La requête s'appuie sur `limit 10` de HQL ; si la taille change, modifier `RANKING_SIZE` et le `limit` ensemble.
- Les traductions malgaches de `reports.top.*` sont de bonne foi : à faire relire.
- L'écran garde le marqueur « En construction » à cause des boutons d'export PDF/XLSX (RPT-06) ; toutes les sections de données sont réelles.
