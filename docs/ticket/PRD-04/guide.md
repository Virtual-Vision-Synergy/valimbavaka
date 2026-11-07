# Guide d'intégration — PRD-04 · Indicateurs de production

Statut : ✅ rédigé le 2026-11-08 — **tests écrits, non exécutés** (voir le [scénario](scenario-test.md)) · Ticket : [#41](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/41) · [description](description.md)

## En bref

L'écran Production affiche en tête les indicateurs du **mois en cours (heure de Madagascar)** : lots terminés (et lots
en cours), unités produites avec la tendance sur le mois précédent, coût matière moyen par unité. Le serveur les calcule
en une lecture (`GET /api/v1/production/indicators`) à partir des lots validés (PRD-02) ; la quatrième carte
« Matières à racheter » est calculée côté écran à partir du stock déjà chargé.

## Contrat exposé

### Endpoint backend (`/api/v1/production`)

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/indicators` | `production.read` (administrateur, vendeuse) | — | 200 `ProductionIndicatorsRDto` | 401, 403 |

Contrôleur : `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/production/controller/ProductionController.java:140`.

`ProductionIndicatorsRDto` (`dto/ProductionIndicatorsRDto.java`) : `month` (`"2026-11"`), `completedBatches`,
`inProgressBatches`, `unitsProduced`, `previousMonthUnits`, `unitsTrendPercent` (entier arrondi, **null** si le mois
précédent n'a rien produit), `materialCost`, `materialCostPerUnit` (2 décimales, **null** si aucun lot chiffré),
`uncostedBatches`.

### Services et ports Java réutilisables

| Port | Méthode | Rôle |
|---|---|---|
| `feature.production.service.ProductionIndicatorService` | `getIndicators()` | Indicateurs du mois en cours (`service/impl/ProductionIndicatorServiceImpl.java:45`). Réutilisable par un tableau de bord. |
| `ProductionBatchRepository` | `findCompletedBetween(Instant, Instant)` (`:50`), `sumCompletedQuantityBetween(Instant, Instant)` (`:57`), `countByStatus(ProductionBatchStatus)` (`:60`) | Lots validés sur `[from, to)` (par `completed_at`), somme de leurs quantités, nombre de lots d'un statut. |

### Frontend (`features/production`)

- `data-access/production-service.ts:88` `indicators()` : `GET /api/v1/production/indicators` ; `context()` l'appelle
  avec les autres lectures et remplit `kpis` par `toKpis`.
- `data-access/production-mapping.ts:99` `toKpis(indicators, inputIds, stocks)` : recopie les indicateurs serveur et
  compte `materialsToRebuy` = entrants dont le stock total est sous un seuil non nul (règle STK-04).
- `data-access/production-models.ts` : miroir `ProductionIndicatorsRDto` ; `ProductionKpisRDto` (modèle d'écran) gagne
  `uncostedLots`, `unitsTrendPercent` et `averageCost` deviennent `number | null`.
- `pages/production-page/production-page.html:9` : cartes « Lots terminés ce mois » (note « N lot(s) en cours »),
  « Unités produites » (« +12 % vs mois dernier », rouge si baisse, « aucune production le mois dernier » si null),
  « Coût matière moyen » (« — » sans unité si null, note d'avertissement s'il y a des lots sans coût), « Matières à
  racheter ». Libellés `production.kpi.*` dans `public/i18n/fr.json` et `mg.json`.

### Données

- Aucune migration, aucune table : lecture de `production_batches` (`status`, `quantity`, `completed_at`) et
  `production_batch_inputs` (`quantity`, `unit_cost`).
- Aucune écriture, donc aucune ligne de journal.

## Règles métier pour les autres tickets

- **Mois** = mois civil à l'heure de Madagascar (`Indian/Antananarivo`, comme `StockAlertPolicy` et
  `InventoryOpeningPolicy`), quel que soit le fuseau du serveur.
- **Lot produit** = lot `COMPLETED`, daté par `completed_at` (PRD-02) : un lot créé en octobre et validé en novembre
  compte en novembre. Les lots en cours sont comptés **tous mois confondus** (aucune date de démarrage stockée, PRD-03).
- **Coût matière** = Σ `quantity × unit_cost` des entrants, **uniquement** pour les lots dont tous les entrants ont un
  coût figé. Un lot avec un entrant sans coût compte dans les lots et les unités, pas dans le coût ; il est signalé
  (`uncostedBatches`) au lieu d'être compté pour 0 (règle de PRD-02). PRD-05 doit garder la même règle pour le coût
  de revient d'un lot.
- Le coût moyen est un coût **matière** (pas de main-d'œuvre ni de frais) ; il ne relit jamais le coût actuel des
  articles, seulement le coût figé à la validation.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/production/`)
- `controller/ProductionController.java` (endpoint, 3ᵉ dépendance)
- `dto/ProductionIndicatorsRDto.java` (créé)
- `repository/ProductionBatchRepository.java`
- `service/ProductionIndicatorService.java` (créé), `service/impl/ProductionIndicatorServiceImpl.java` (créé)

**Frontend** (`valimbavaka-ui/`)
- `src/app/features/production/data-access/production-service.ts`, `production-mapping.ts`, `production-models.ts`, `production-fixtures.ts`
- `src/app/features/production/pages/production-page/production-page.html`, `.css` (`.note.down`)
- `public/i18n/fr.json`, `mg.json` (`production.kpi.*`)

**Tests** : voir « Tests existants » du [scénario](scenario-test.md).

## Dépendances

**Ce ticket s'appuie sur** : [PRD-01](../PRD-01/guide.md) (lots, écran), [PRD-02](../PRD-02/guide.md)
(`completed_at`, `unit_cost`), [PRD-03](../PRD-03/guide.md) (statut `IN_PROGRESS`, PR empilée),
[STK-01](../STK-01/guide.md) (stock et seuils lus par l'écran).

**Ces tickets s'appuient sur lui** : aucun (un tableau de bord pourra réutiliser `ProductionIndicatorService`).

## Comment l'utiliser depuis un autre ticket

```java
var indicators = productionIndicatorService.getIndicators(); // mois en cours, heure de Madagascar
if (indicators.uncostedBatches() > 0) { /* signaler : coût moyen calculé sans ces lots */ }
```

```ts
this.productionService.indicators().subscribe(({ completedBatches, materialCostPerUnit }) => ...);
```

## Points d'attention

- **Hypothèses prises (session sans validation) — à faire valider par Randy** :
  - le ticket demande 3 indicateurs « lots, unités, coût matière » : ce sont les 3 premières cartes, **sur le mois en
    cours** (la maquette disait « ce mois ») ; le coût matière est affiché **en moyenne par unité** (la maquette
    affichait un coût par unité), le total `materialCost` est dans le RDto ;
  - permission `production.read` (administrateur **et vendeuse**) plutôt qu'une permission réservée à
    l'administrateur : la vendeuse voit déjà l'écran et les coûts d'achat des entrants, et PRD-05 lui montre le coût de
    revient. Pour réserver les indicateurs à l'administrateur, il faudrait une permission dédiée (migration) et masquer
    les cartes à l'écran ;
  - la carte « Matières à racheter » (hors ticket) n'est plus figée à 0 : elle compte les entrants sous leur seuil,
    à partir des 100 lignes de stock déjà chargées par l'écran (même limite d'une page que PRD-01).
- Libellés de la maquette changés : « Lots ce mois » → « Lots terminés ce mois », « dont N en cours de cure » →
  « N lot(s) en cours » (un lot en cours n'est pas un lot terminé), « Coût de revient moyen » → « Coût matière moyen ».
- Le calcul charge les lots validés du mois (entrants par lots de 100, `@BatchSize`) : quelques dizaines de lots par
  mois, sans souci ; si le volume grossissait, passer à une agrégation SQL.
- Pas de choix du mois (pas de paramètre `month`) : à ajouter si un rapport mensuel en a besoin.
- Compilation Java et Spotless non vérifiés en local (Maven Central inaccessible) : la CI tranche.
