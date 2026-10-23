# Guide d'intégration — VTE-09 · Détail d'une vente

> **À compléter par le traiteur du ticket une fois le scénario de test rédigé et l'implémentation faite.** Ce guide est lu par les autres tickets pour utiliser cette fonctionnalité sans relire le code : quoi appeler, quels fichiers sont exposés, quel est le contrat.

Statut : ☐ à compléter · Ticket : [#70](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/70) · [description](description.md) · [scénario de test](scenario-test.md)

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

**À exposer / créer (liste du traiteur)** : _(à compléter)_

Zone de départ (voir `description.md`) :

- Frontend :
  - `valimbavaka-ui/src/app/features/pos`
  - `valimbavaka-ui/src/app/features/sales`
- Backend :
  - `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/sale (à créer)`

## Dépendances

**Ce ticket s'appuie sur** :
- [COM-02](../COM-02/description.md) · [guide](../COM-02/guide.md)
- [VTE-01](../VTE-01/description.md) · [guide](../VTE-01/guide.md)

**Ces tickets s'appuient sur lui** :
- _(rien)_

## Comment l'utiliser depuis un autre ticket

```text
(exemple d'appel ou d'injection à compléter)
```

## Points d'attention

- _(à compléter : pièges, limites connues, dette technique, décisions en attente du client)_
