# Scénarios de test — STK-06 · Réception d'une livraison fournisseur (entrée en stock)

> **À compléter par le traiteur du ticket**, avant de passer en revue. Le scénario sert de base au QA (Njara) et de référence aux tickets liés.

Statut : ☐ à compléter · Ticket : [#113](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/113) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | _(à compléter)_ |
| Données de départ | _(à compléter)_ |
| Tickets à avoir déployés avant | FRS-01, STK-01, JNL-02, JNL-01, SYS-05, STK-05 |

## Scénarios issus des critères d'acceptation

| # | Critère d'acceptation | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | La réception génère un mouvement d'entrée en stock à l'emplacement choisi (entrepôt par défaut), tracé au journal. | _(à compléter)_ | _(à compléter)_ | _(à compléter)_ | ☐ |
| 2 | Une réception peut reprendre un bon de réapprovisionnement (STK-05) ou être saisie librement. | _(à compléter)_ | _(à compléter)_ | _(à compléter)_ | ☐ |
| 3 | Une réception validée ne se supprime pas : une erreur se corrige par un mouvement inverse. | _(à compléter)_ | _(à compléter)_ | _(à compléter)_ | ☐ |

## Sécurité et erreurs

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Appel sans jeton | 401 `UNAUTHENTICATED` | _(à compléter)_ | ☐ |
| Appel avec un mauvais rôle | 403 `FORBIDDEN` | _(à compléter)_ | ☐ |
| Champ interdit dans le corps (`role`, `id`) | ignoré (mass assignment) | _(à compléter)_ | ☐ |
| Données invalides | 422 `VALIDATION_FAILED` | _(à compléter)_ | ☐ |
| Ressource introuvable | 404 `NOT_FOUND` | _(à compléter)_ | ☐ |
| Conflit / doublon | 409 `CONFLICT` | _(à compléter)_ | ☐ |

_Supprimer les lignes qui ne s'appliquent pas (ticket frontend seul, pas d'écriture, etc.)._

## Cas limites et hors ligne

- _(à compléter : valeurs extrêmes, listes vides, pagination, perte de connexion, rejeu avec `Idempotency-Key` si la saisie passe par la file hors ligne)_

## Résultat de l'exécution

- Date : _(à compléter)_
- Environnement : _(à compléter)_
- Anomalies ouvertes : _(à compléter)_
