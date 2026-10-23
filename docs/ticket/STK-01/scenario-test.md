# Scénarios de test — STK-01 · Suivi du stock par emplacement

> **À compléter par le traiteur du ticket**, avant de passer en revue. Le scénario sert de base au QA (Njara) et de référence aux tickets liés.

Statut : ☐ à compléter · Ticket : [#30](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/30) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | _(à compléter)_ |
| Données de départ | _(à compléter)_ |
| Tickets à avoir déployés avant | ART-01, JNL-01, SYS-05 |

## Scénarios issus des critères d'acceptation

| # | Critère d'acceptation | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Le stock d'un article détaille la quantité par emplacement et le total. | _(à compléter)_ | _(à compléter)_ | _(à compléter)_ | ☐ |
| 2 | Le stock n'est jamais modifié directement : il est calculé à partir de mouvements typés (réception, transfert, vente, livraison, retour, salon, shooting, production, inventaire), chacun tracé au journal. C'est la base de la réconciliation de clôture (JR-04). | _(à compléter)_ | _(à compléter)_ | _(à compléter)_ | ☐ |

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
