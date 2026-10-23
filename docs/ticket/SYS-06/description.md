# SYS-06 · Audit : auteur des opérations en UUID

> Ticket GitHub [#112](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/112) · Fermé · Milestone : 01 · Authentification & socle technique · 1 SP · Assigné(e) : ManaJudy

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux que chaque enregistrement mémorise l'utilisateur qui l'a créé et modifié afin que la traçabilité soit nominative.

## Dépendances

- Bloqué par `SYS-05` (#111)

## Critères d'acceptation

- [x] Les colonnes created_by / updated_by sont du même type que l'identifiant utilisateur (UUID), corrigé par un nouveau changeset avant la première mise en production.
- [x] L'auteur est renseigné automatiquement à partir de l'utilisateur connecté (pas de saisie manuelle).

## Détails

| | |
|---|---|
| **ID Ticket** | `SYS-06` |
| **Module** | Système |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

Ajouté lors de la revue du backlog du 07/10 : BaseEntity utilise aujourd'hui Long (BIGINT) alors que les utilisateurs ont un UUID. Changer le type après la première mise en prod serait une migration non additive.

## Ce ticket débloque

- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations
- [LIV-07](../LIV-07/description.md) · Historique de statut d'une livraison

## Dépend de

- [SYS-05](../SYS-05/description.md) · Rôles métier, référentiels et compte administrateur initial (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- _(aucun)_

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/security/audit`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/shared/domain`
