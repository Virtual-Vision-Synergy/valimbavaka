# Scénarios de test — SYS-05 · Rôles métier, référentiels et compte administrateur initial

> **À compléter par le traiteur du ticket**, avant de passer en revue. Le scénario sert de base au QA (Njara) et de référence aux tickets liés.

Statut : ☐ à compléter · Ticket : [#111](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/111) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | _(à compléter)_ |
| Données de départ | _(à compléter)_ |
| Tickets à avoir déployés avant | — |

## Scénarios issus des critères d'acceptation

| # | Critère d'acceptation | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Les rôles sont exactement : Administrateur, Vendeuse, Livreur (pas d'Opératrice, pas de rôle générique « USER »). Le changeset initial ADMIN/USER est corrigé par un nouveau changeset (jamais en modifiant l'ancien). | _(à compléter)_ | _(à compléter)_ | _(à compléter)_ | ☐ |
| 2 | Les référentiels sont disponibles : canaux de vente (boutique, Facebook, téléphone, livraison — liste à confirmer), gammes (Zezika, Botanika, Solika), emplacements de stock (entrepôt, boutique, livraison, salon). | _(à compléter)_ | _(à compléter)_ | _(à compléter)_ | ☐ |
| 3 | Au premier démarrage sur une base sans administrateur, un compte administrateur est créé à partir de variables d'environnement (jamais de mot de passe écrit dans le code ou dans un changelog), et doit changer son mot de passe à la première connexion. | _(à compléter)_ | _(à compléter)_ | _(à compléter)_ | ☐ |

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

- `valimbavaka-ui/src/app/features/referential/data-access/referential-service.spec.ts`

## Résultat de l'exécution

- Date : _(à compléter)_
- Environnement : _(à compléter)_
- Anomalies ouvertes : _(à compléter)_
