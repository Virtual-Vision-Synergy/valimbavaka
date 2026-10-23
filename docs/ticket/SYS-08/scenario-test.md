# Scénarios de test — SYS-08 · Reprise automatique de la synchronisation

> **À compléter par le traiteur du ticket**, avant de passer en revue. Le scénario sert de base au QA (Njara) et de référence aux tickets liés.

Statut : ☐ à compléter · Ticket : [#134](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/134) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | _(à compléter)_ |
| Données de départ | _(à compléter)_ |
| Tickets à avoir déployés avant | SYS-02 |

## Scénarios issus des critères d'acceptation

| # | Critère d'acceptation | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Après une erreur réseau, la synchronisation est relancée automatiquement avec un délai croissant entre les tentatives, sans action de l'utilisateur. | _(à compléter)_ | _(à compléter)_ | _(à compléter)_ | ☐ |
| 2 | Le nombre de tentatives est limité ; au-delà, la saisie passe en échec et reste visible (voir `SYS-09`). | _(à compléter)_ | _(à compléter)_ | _(à compléter)_ | ☐ |
| 3 | Une saisie qui réussit à une relance n'est envoyée qu'une seule fois. | _(à compléter)_ | _(à compléter)_ | _(à compléter)_ | ☐ |

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

## Tests existants (repérés dans les PR liées)

- `valimbavaka-ui/src/app/core/offline/connectivity-service.spec.ts`
- `valimbavaka-ui/src/app/core/offline/outbox-service.spec.ts`
- `valimbavaka-ui/src/app/core/offline/sync-status.a11y.spec.ts`
- `valimbavaka-ui/src/app/core/offline/sync-status.spec.ts`
- `valimbavaka-ui/src/app/features/sync/pages/sync-page/sync-page.spec.ts`

## Résultat de l'exécution

- Date : _(à compléter)_
- Environnement : _(à compléter)_
- Anomalies ouvertes : _(à compléter)_
