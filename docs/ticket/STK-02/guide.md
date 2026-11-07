# Guide d'intégration — STK-02 · Jauge visuelle du stock

Statut : ✅ écrit le 2026-11-08 — **tests non exécutés** (voir « Résultat de l'exécution » du scénario) · Ticket : [#31](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/31) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Sur la page des stocks, vue « Tous », la jauge « Total / seuil » de chaque article est découpée en une part colorée
par emplacement (entrepôt, boutique, livraison, salon). Le survol ou le focus clavier de la jauge ouvre une bulle
avec la quantité et la part (%) de chaque emplacement, puis le total. Le composant partagé `vvs-stock-gauge` accepte
désormais une répartition : tout autre écran peut l'utiliser de la même façon.

## Contrat exposé

### Endpoints backend

Aucun. Ticket **frontend seul** : la quantité par emplacement vient déjà de `GET /api/v1/stocks`
(`StockRDto.locations`, voir [STK-01](../STK-01/guide.md)). Pas de permission nouvelle (`stocks.read`).

### Services et ports Java réutilisables

- Aucun.

### Frontend

- Routes : aucune nouvelle ; `/stocks` existante.
- Composant partagé `vvs-stock-gauge` (`valimbavaka-ui/src/app/shared/components/stock-gauge/stock-gauge.ts`) :
  - entrées existantes inchangées : `value`, `threshold`, `max`, `showThreshold` ;
  - nouvelle entrée `segments: readonly StockGaugeSegment[]` (`stock-gauge.ts:56`, défaut `[]`) ;
    `StockGaugeSegment` = `key` (unique, sert au `track`), `labelKey` (clé de traduction), `quantity` ;
  - sans `segments` : jauge de niveau décorative, comme avant (`aria-hidden="true"`) ;
  - avec `segments` : parts colorées par position (1re teal foncé, 2e orange, 3e bleu, 4e brun, 5e rose ;
    `stock-gauge.css`), bulle `role="tooltip"` ouverte au survol / focus, fermée en sortie, perte de focus, Échap
    ou défilement de la fenêtre ; part en % arrondie, total (`bars` et `total`, `stock-gauge.ts:75-90`).
- `data-access/stock-levels.ts:38` : `segmentsOf(row)` construit la répartition d'une `StockRowRDto` dans l'ordre
  de `STOCK_LOCATIONS` (zéros compris).
- `pages/stock-page/stock-page.ts:82` : `segments` n'est rempli que sur la vue « Tous » ; un emplacement choisi
  garde la jauge de niveau de cet emplacement (`stock-page.html:97`).
- Traductions : `stockGauge.label` et `stockGauge.total` (`public/i18n/fr.json`, `mg.json`).

### Données

- Tables et migrations Liquibase : aucune.
- Écriture dans le journal : aucune (lecture seule).

## Règles métier à respecter par les autres tickets

- La jauge **n'invente aucun chiffre** : elle affiche les quantités reçues ; la somme des parts est le total de
  l'article (même valeur que STK-01 `total`).
- Pleine échelle : `max` si fourni, sinon `seuil × 2`. Si le stock la dépasse, les parts sont réduites pour remplir
  exactement la jauge (proportions conservées).
- Le niveau (rupture / sous le seuil) reste visible en mode réparti : liseré rouge ou ocre autour de la jauge,
  repère du seuil si `showThreshold`.
- Les quantités négatives (impossibles d'après STK-01) sont comptées à 0.

## Fichiers concernés

**Frontend**
- `valimbavaka-ui/src/app/shared/components/stock-gauge/stock-gauge.ts`, `.html`, `.css`, `.spec.ts` (nouveau)
- `valimbavaka-ui/src/app/features/stocks/data-access/stock-levels.ts` (`segmentsOf`)
- `valimbavaka-ui/src/app/features/stocks/pages/stock-page/stock-page.ts`, `.html`, `.spec.ts`
- `valimbavaka-ui/public/i18n/fr.json`, `mg.json` (`stockGauge`)

**Backend / migrations** : aucun fichier.

**Tests**
- `stock-gauge.spec.ts` (8 tests : rendu, survol, clavier, accessibilité, bornes)
- `stock-page.spec.ts` : 2 tests STK-02 (détail au survol sur « Tous », jauge simple sur un emplacement)

## Dépendances

**Ce ticket s'appuie sur** :
- [STK-01](../STK-01/description.md) · [guide](../STK-01/guide.md) : `StockRDto.locations`, `toStockRow`.

**Ces tickets s'appuient sur lui** :
- _(rien)_ ; réutilisable par DSH-xx ou ART-06 pour une jauge par emplacement.

## Comment l'utiliser depuis un autre ticket

```html
<vvs-stock-gauge [value]="total" [threshold]="seuil" [segments]="segmentsOf(row)" [showThreshold]="true" />
```

## Points d'attention

- **Hypothèse** : la répartition n'est montrée que sur la vue « Tous ». Sur un emplacement choisi, la jauge compare
  cet emplacement à `seuil / 4` (règle provisoire, voir STK-04) ; la découper n'aurait pas de sens.
- **Hypothèse** : la jauge du tableau est conservée et enrichie plutôt que d'ajouter une colonne ; les couleurs des
  emplacements remplacent la couleur de niveau, qui passe en liseré. À valider visuellement avec le client.
- La page des stocks reste limitée à **100 articles** (limite STK-01) : la jauge ne change rien à ce point.
- Couleurs attribuées **par position** dans `segments`, pas par code d'emplacement : garder le même ordre
  (`STOCK_LOCATIONS`) partout pour que la légende reste cohérente. Au-delà de 5 parts, les suivantes sont sans couleur.
- La bulle est en `position: fixed` (pas coupée par le défilement horizontal du tableau) ; elle se ferme au
  défilement de la fenêtre, pas au défilement interne du tableau.
- Tests non exécutés (`ng test` non lancé) : voir le scénario.
