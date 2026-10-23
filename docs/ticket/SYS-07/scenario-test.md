# Scénarios de test — SYS-07 · Brancher un premier formulaire sur la file hors ligne

> **À compléter par le traiteur du ticket**, avant de passer en revue. Le scénario sert de base au QA (Njara) et de référence aux tickets liés.

Statut : ☐ à compléter · Ticket : [#133](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/133) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | _(à compléter)_ |
| Données de départ | _(à compléter)_ |
| Tickets à avoir déployés avant | SYS-02, FRS-02 |

## Scénarios issus des critères d'acceptation

| # | Critère d'acceptation | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Le formulaire de référence (création / modification d'un fournisseur, FRS-02) enregistre via la file hors ligne de SYS-02 et non par un appel direct à l'API. | _(à compléter)_ | _(à compléter)_ | _(à compléter)_ | ☐ |
| 2 | Hors connexion, la saisie est conservée localement, reste présente après rechargement de la page, et un indicateur visible affiche son statut (en attente / synchronisé / échec). | _(à compléter)_ | _(à compléter)_ | _(à compléter)_ | ☐ |
| 3 | Au retour du réseau, la saisie est synchronisée automatiquement et n'existe qu'une seule fois en base (aucun doublon, même si la synchronisation est relancée). | _(à compléter)_ | _(à compléter)_ | _(à compléter)_ | ☐ |
| 4 | Le principe d'intégration est documenté en quelques lignes pour être reproduit par les modules métier suivants. | _(à compléter)_ | _(à compléter)_ | _(à compléter)_ | ☐ |

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
