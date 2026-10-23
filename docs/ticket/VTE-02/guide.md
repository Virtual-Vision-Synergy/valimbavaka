# Guide d'intégration — VTE-02 · Poste de caisse avec calcul automatique

> **À compléter par le traiteur du ticket une fois le scénario de test rédigé et l'implémentation faite.** Ce guide est lu par les autres tickets pour utiliser cette fonctionnalité sans relire le code : quoi appeler, quels fichiers sont exposés, quel est le contrat.

Statut : ☐ à compléter · Ticket : [#63](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/63) · [description](description.md) · [scénario de test](scenario-test.md)

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
- [VTE-01](../VTE-01/description.md) · [guide](../VTE-01/guide.md)

**Ces tickets s'appuient sur lui** :
- [PRO-02](../PRO-02/description.md) · [guide](../PRO-02/guide.md)
- [VTE-03](../VTE-03/description.md) · [guide](../VTE-03/guide.md)
- [VTE-04](../VTE-04/description.md) · [guide](../VTE-04/guide.md)
- [VTE-05](../VTE-05/description.md) · [guide](../VTE-05/guide.md)
- [VTE-06](../VTE-06/description.md) · [guide](../VTE-06/guide.md)
- [FAC-01](../FAC-01/description.md) · [guide](../FAC-01/guide.md)
- [FAC-02](../FAC-02/description.md) · [guide](../FAC-02/guide.md)

## Comment l'utiliser depuis un autre ticket

```text
(exemple d'appel ou d'injection à compléter)
```

## Points d'attention

- _(à compléter : pièges, limites connues, dette technique, décisions en attente du client)_
