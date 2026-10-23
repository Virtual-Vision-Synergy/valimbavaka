# Guide d'intégration — DSG-01 · Maquettes Claude Design de toutes les pages

> **À compléter par le traiteur du ticket une fois le scénario de test rédigé et l'implémentation faite.** Ce guide est lu par les autres tickets pour utiliser cette fonctionnalité sans relire le code : quoi appeler, quels fichiers sont exposés, quel est le contrat.

Statut : ⚠ pré-rempli depuis le code au 2026-10-23 — à vérifier et compléter · Ticket : [#128](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/128) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

_(2 à 3 phrases : ce que la fonctionnalité fait et ce qu'elle offre aux autres.)_

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée (`XxxWDto`) | Sortie (`XxxRDto`) | Erreurs |
|---|---|---|---|---|---|
| _(à compléter)_ | | | | | |

### Services et ports Java réutilisables

- _(à compléter : interface de service, port `security`, événement publié, etc.)_

### Frontend

- Routes : _(à compléter)_
- Services / stores (`data-access`) : _(à compléter)_
- Composants réutilisables : _(à compléter)_

### Données

- Tables et migrations Liquibase : _(à compléter)_
- Écriture dans le journal (action, champs) : _(à compléter)_

## Règles métier à respecter par les autres tickets

- _(à compléter : statuts, états interdits, calculs, arrondis, droits par rôle)_


## Fichiers concernés

_Fichiers existants repérés dans les PR liées ; ajouter ceux modifiés ensuite._

_258 fichiers : résumé par dossier._

- `valimbavaka-ui/src/app/features/articles/articles.routes.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/features/articles/components/…` (6 fichiers)
- `valimbavaka-ui/src/app/features/articles/data-access/…` (6 fichiers)
- `valimbavaka-ui/src/app/features/articles/pages/…` (9 fichiers)
- `valimbavaka-ui/src/app/features/auth/pages/…` (3 fichiers)
- `valimbavaka-ui/src/app/features/commissions/commissions.routes.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/features/commissions/data-access/…` (3 fichiers)
- `valimbavaka-ui/src/app/features/commissions/pages/…` (3 fichiers)
- `valimbavaka-ui/src/app/features/dashboard/components/…` (21 fichiers)
- `valimbavaka-ui/src/app/features/dashboard/dashboard.routes.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/features/dashboard/data-access/…` (3 fichiers)
- `valimbavaka-ui/src/app/features/dashboard/pages/…` (3 fichiers)
- `valimbavaka-ui/src/app/features/day/components/…` (3 fichiers)
- `valimbavaka-ui/src/app/features/day/data-access/…` (4 fichiers)
- `valimbavaka-ui/src/app/features/day/day.routes.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/features/day/pages/…` (6 fichiers)
- `valimbavaka-ui/src/app/features/deliveries/components/…` (6 fichiers)
- `valimbavaka-ui/src/app/features/deliveries/data-access/…` (6 fichiers)
- `valimbavaka-ui/src/app/features/deliveries/deliveries.routes.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/features/deliveries/driver.routes.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/features/deliveries/pages/…` (12 fichiers)
- `valimbavaka-ui/src/app/features/demo-pages.a11y.spec.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/features/inventory/data-access/…` (4 fichiers)
- `valimbavaka-ui/src/app/features/inventory/inventory.routes.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/features/inventory/pages/…` (3 fichiers)
- `valimbavaka-ui/src/app/features/journal/pages/…` (4 fichiers)
- `valimbavaka-ui/src/app/features/module-placeholder/module-placeholder.html/…` (1 fichiers)
- `valimbavaka-ui/src/app/features/module-placeholder/module-placeholder.routes.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/features/module-placeholder/module-placeholder.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/features/pos/components/…` (9 fichiers)
- `valimbavaka-ui/src/app/features/pos/data-access/…` (4 fichiers)
- `valimbavaka-ui/src/app/features/pos/pages/…` (3 fichiers)
- `valimbavaka-ui/src/app/features/pos/pos.routes.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/features/production/data-access/…` (5 fichiers)
- `valimbavaka-ui/src/app/features/production/pages/…` (3 fichiers)
- `valimbavaka-ui/src/app/features/production/production.routes.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/features/promotions/components/…` (6 fichiers)
- `valimbavaka-ui/src/app/features/promotions/data-access/…` (4 fichiers)
- `valimbavaka-ui/src/app/features/promotions/pages/…` (3 fichiers)
- `valimbavaka-ui/src/app/features/promotions/promotions.routes.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/features/reports/components/…` (6 fichiers)
- `valimbavaka-ui/src/app/features/reports/data-access/…` (3 fichiers)
- `valimbavaka-ui/src/app/features/reports/pages/…` (3 fichiers)
- `valimbavaka-ui/src/app/features/reports/reports.routes.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/features/sales/components/…` (10 fichiers)
- `valimbavaka-ui/src/app/features/sales/data-access/…` (3 fichiers)
- `valimbavaka-ui/src/app/features/sales/pages/…` (3 fichiers)
- `valimbavaka-ui/src/app/features/sales/sales.routes.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/features/services/components/…` (6 fichiers)
- `valimbavaka-ui/src/app/features/services/data-access/…` (3 fichiers)
- `valimbavaka-ui/src/app/features/services/pages/…` (3 fichiers)
- `valimbavaka-ui/src/app/features/services/services.routes.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/features/stocks/components/…` (6 fichiers)
- `valimbavaka-ui/src/app/features/stocks/data-access/…` (4 fichiers)
- `valimbavaka-ui/src/app/features/stocks/pages/…` (3 fichiers)
- `valimbavaka-ui/src/app/features/stocks/stocks.routes.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/features/styleguide/pages/…` (3 fichiers)
- `valimbavaka-ui/src/app/features/styleguide/preview.routes.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/features/styleguide/styleguide.routes.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/features/suppliers/components/…` (3 fichiers)
- `valimbavaka-ui/src/app/features/suppliers/data-access/…` (4 fichiers)
- `valimbavaka-ui/src/app/features/suppliers/pages/…` (3 fichiers)
- `valimbavaka-ui/src/app/features/suppliers/suppliers.routes.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/features/transfers/components/…` (3 fichiers)
- `valimbavaka-ui/src/app/features/transfers/data-access/…` (5 fichiers)
- `valimbavaka-ui/src/app/features/transfers/pages/…` (3 fichiers)
- `valimbavaka-ui/src/app/features/transfers/transfers.routes.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/features/user/data-access/…` (1 fichiers)
- `valimbavaka-ui/src/app/features/user/pages/…` (6 fichiers)
- `valimbavaka-ui/src/app/shared/components/article-thumb/…` (2 fichiers)
- `valimbavaka-ui/src/app/shared/components/avatar-initials/…` (2 fichiers)
- `valimbavaka-ui/src/app/shared/components/bar-chart/…` (3 fichiers)
- `valimbavaka-ui/src/app/shared/components/confirm-dialog/…` (3 fichiers)

## Dépendances

**Ce ticket s'appuie sur** :
- _(rien)_

**Ces tickets s'appuient sur lui** :
- _(rien)_

## Comment l'utiliser depuis un autre ticket

```text
(exemple d'appel ou d'injection à compléter)
```

## Points d'attention

- _(à compléter : pièges, limites connues, dette technique, décisions en attente du client)_
