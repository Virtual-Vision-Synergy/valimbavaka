# Guide d'intégration — STK-01 · Suivi du stock par emplacement

> **À compléter par le traiteur du ticket une fois le scénario de test rédigé et l'implémentation faite.** Ce guide est lu par les autres tickets pour utiliser cette fonctionnalité sans relire le code : quoi appeler, quels fichiers sont exposés, quel est le contrat.

Statut : ☐ à compléter · Ticket : [#30](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/30) · [description](description.md) · [scénario de test](scenario-test.md)

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
  - `valimbavaka-ui/src/app/features/stocks`
- Backend :
  - `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/stock (à créer)`

## Dépendances

**Ce ticket s'appuie sur** :
- [ART-01](../ART-01/description.md) · [guide](../ART-01/guide.md)
- [JNL-01](../JNL-01/description.md) · [guide](../JNL-01/guide.md)
- [SYS-05](../SYS-05/description.md) · [guide](../SYS-05/guide.md)

**Ces tickets s'appuient sur lui** :
- [ART-06](../ART-06/description.md) · [guide](../ART-06/guide.md)
- [STK-02](../STK-02/description.md) · [guide](../STK-02/guide.md)
- [STK-03](../STK-03/description.md) · [guide](../STK-03/guide.md)
- [STK-04](../STK-04/description.md) · [guide](../STK-04/guide.md)
- [TRF-03](../TRF-03/description.md) · [guide](../TRF-03/guide.md)
- [PRD-01](../PRD-01/description.md) · [guide](../PRD-01/guide.md)
- [SHO-01](../SHO-01/description.md) · [guide](../SHO-01/guide.md)
- [LIV-01](../LIV-01/description.md) · [guide](../LIV-01/guide.md)
- [JR-01](../JR-01/description.md) · [guide](../JR-01/guide.md)
- [VTE-01](../VTE-01/description.md) · [guide](../VTE-01/guide.md)
- [INV-01](../INV-01/description.md) · [guide](../INV-01/guide.md)
- [SRV-02](../SRV-02/description.md) · [guide](../SRV-02/guide.md)
- [STK-06](../STK-06/description.md) · [guide](../STK-06/guide.md)

## Comment l'utiliser depuis un autre ticket

```text
(exemple d'appel ou d'injection à compléter)
```

## Points d'attention

- _(à compléter : pièges, limites connues, dette technique, décisions en attente du client)_
