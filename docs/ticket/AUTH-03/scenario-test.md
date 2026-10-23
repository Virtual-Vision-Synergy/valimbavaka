# Scénarios de test — AUTH-03 · Création et modification d'un utilisateur

> **À compléter par le traiteur du ticket**, avant de passer en revue. Le scénario sert de base au QA (Njara) et de référence aux tickets liés.

Statut : ☐ à compléter · Ticket : [#12](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/12) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | _(à compléter)_ |
| Données de départ | _(à compléter)_ |
| Tickets à avoir déployés avant | AUTH-01, JNL-01, SYS-05, JNL-02 |

## Scénarios issus des critères d'acceptation

| # | Critère d'acceptation | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Le rôle choisi détermine la coquille applicative de l'utilisateur. | _(à compléter)_ | _(à compléter)_ | _(à compléter)_ | ☐ |
| 2 | Un formulaire permet de saisir/modifier ces champs. | _(à compléter)_ | _(à compléter)_ | _(à compléter)_ | ☐ |
| 3 | Le formulaire inclut aussi le canal de vente et le taux de commission de l'utilisateur. | _(à compléter)_ | _(à compléter)_ | _(à compléter)_ | ☐ |
| 4 | Les rôles proposés sont exactement Administrateur, Vendeuse, Livreur (SYS-05). | _(à compléter)_ | _(à compléter)_ | _(à compléter)_ | ☐ |
| 5 | La création et la modification des utilisateurs sont réservées à l'administrateur, contrôlé côté serveur (pas seulement en masquant l'interface). | _(à compléter)_ | _(à compléter)_ | _(à compléter)_ | ☐ |
| 6 | Un utilisateur ne se supprime jamais : il se désactive (AUTH-04), conformément à JNL-02. | _(à compléter)_ | _(à compléter)_ | _(à compléter)_ | ☐ |

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

- `valimbavaka-ui/src/app/features/user/data-access/user-service.spec.ts`
- `valimbavaka-ui/src/app/features/user/pages/user-list-page/user-list-page.spec.ts`

## Résultat de l'exécution

- Date : _(à compléter)_
- Environnement : _(à compléter)_
- Anomalies ouvertes : _(à compléter)_
