# Guide d'intégration — VTE-01 · Enregistrement d'une vente multicanal

> **À compléter par le traiteur du ticket une fois le scénario de test rédigé et l'implémentation faite.** Ce guide est lu par les autres tickets pour utiliser cette fonctionnalité sans relire le code : quoi appeler, quels fichiers sont exposés, quel est le contrat.

Statut : ☐ à compléter · Ticket : [#62](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/62) · [description](description.md) · [scénario de test](scenario-test.md)

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
- [ART-01](../ART-01/description.md) · [guide](../ART-01/guide.md)
- [AUTH-03](../AUTH-03/description.md) · [guide](../AUTH-03/guide.md)
- [JR-01](../JR-01/description.md) · [guide](../JR-01/guide.md)
- [STK-01](../STK-01/description.md) · [guide](../STK-01/guide.md)
- [JNL-02](../JNL-02/description.md) · [guide](../JNL-02/guide.md)
- [JNL-01](../JNL-01/description.md) · [guide](../JNL-01/guide.md)
- [SYS-05](../SYS-05/description.md) · [guide](../SYS-05/guide.md)

**Ces tickets s'appuient sur lui** :
- [JR-03](../JR-03/description.md) · [guide](../JR-03/guide.md)
- [VTE-02](../VTE-02/description.md) · [guide](../VTE-02/guide.md)
- [VTE-07](../VTE-07/description.md) · [guide](../VTE-07/guide.md)
- [VTE-08](../VTE-08/description.md) · [guide](../VTE-08/guide.md)
- [VTE-09](../VTE-09/description.md) · [guide](../VTE-09/guide.md)
- [FAC-02](../FAC-02/description.md) · [guide](../FAC-02/guide.md)
- [COM-02](../COM-02/description.md) · [guide](../COM-02/guide.md)
- [JR-04](../JR-04/description.md) · [guide](../JR-04/guide.md)
- [DSH-02](../DSH-02/description.md) · [guide](../DSH-02/guide.md)
- [DSH-04](../DSH-04/description.md) · [guide](../DSH-04/guide.md)
- [RPT-01](../RPT-01/description.md) · [guide](../RPT-01/guide.md)

## Comment l'utiliser depuis un autre ticket

```text
(exemple d'appel ou d'injection à compléter)
```

## Points d'attention

- _(à compléter : pièges, limites connues, dette technique, décisions en attente du client)_
