# Guide d'intégration — FRS-02 · Création / modification d'un fournisseur

> **À compléter par le traiteur du ticket une fois le scénario de test rédigé et l'implémentation faite.** Ce guide est lu par les autres tickets pour utiliser cette fonctionnalité sans relire le code : quoi appeler, quels fichiers sont exposés, quel est le contrat.

Statut : ⚠ pré-rempli depuis le code au 2026-10-23 — à vérifier et compléter · Ticket : [#18](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/18) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

_(2 à 3 phrases : ce que la fonctionnalité fait et ce qu'elle offre aux autres.)_

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission | Entrée | Sortie |
|---|---|---|---|---|
| POST | `/api/v1/suppliers` | suppliers.create | `SupplierWDto` | `SupplierRDto` |
| PUT | `/api/v1/suppliers/{id}` | suppliers.update | `SupplierWDto` | `SupplierRDto` |
| PATCH | `/api/v1/suppliers/{id}/status` | suppliers.update | `SupplierStatusWDto` | `SupplierRDto` |

_Relevé dans les contrôleurs ; compléter erreurs possibles, filtres et exemples de corps._

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

- ⚠ Les routes `POST` / `PUT` / `PATCH status` existent déjà dans `SupplierController` alors que le ticket est encore ouvert : le traiteur vérifie ce qui est réellement couvert (validation, journal, formulaire) avant de coder.

## Fichiers concernés

**À exposer / créer (liste du traiteur)** : _(à compléter)_

Zone de départ (voir `description.md`) :

- Frontend :
  - `valimbavaka-ui/src/app/features/suppliers`
  - `valimbavaka-ui/src/app/features/supplier`
- Backend :
  - `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/supplier`

## Dépendances

**Ce ticket s'appuie sur** :
- [FRS-01](../FRS-01/description.md) · [guide](../FRS-01/guide.md)
- [JNL-01](../JNL-01/description.md) · [guide](../JNL-01/guide.md)

**Ces tickets s'appuient sur lui** :
- [SYS-07](../SYS-07/description.md) · [guide](../SYS-07/guide.md)

## Comment l'utiliser depuis un autre ticket

```text
(exemple d'appel ou d'injection à compléter)
```

## Points d'attention

- _(à compléter : pièges, limites connues, dette technique, décisions en attente du client)_
