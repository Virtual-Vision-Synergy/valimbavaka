# Scénarios de test — DSG-01 · Maquettes Claude Design de toutes les pages

> **À compléter par le traiteur du ticket**, avant de passer en revue. Le scénario sert de base au QA (Njara) et de référence aux tickets liés.

Statut : ☐ à compléter · Ticket : [#128](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/128) · [description](description.md) · [guide](guide.md)

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
| 1 | Une maquette existe pour chaque page couverte par le backlog (voir périmètre ci-dessous), réalisée avec Claude Design dans un projet unique partagé avec l'équipe. | _(à compléter)_ | _(à compléter)_ | _(à compléter)_ | ☐ |
| 2 | Les maquettes respectent la charte graphique du dépôt (`resources/design/charte-graphique.md` : palette, logos, tokens) et existent en thème clair et sombre (SYS-04). | _(à compléter)_ | _(à compléter)_ | _(à compléter)_ | ☐ |
| 3 | Chaque page montre l'état nominal, et quand c'est pertinent l'état vide, le chargement et l'erreur. | _(à compléter)_ | _(à compléter)_ | _(à compléter)_ | ☐ |
| 4 | Les pages de la tournée de livraison (LIV-02 à LIV-08) et du poste de caisse (VTE-02) sont aussi maquettées en version mobile/tablette. | _(à compléter)_ | _(à compléter)_ | _(à compléter)_ | ☐ |
| 5 | Le menu et la navigation sont maquettés pour chacun des trois rôles : Administrateur, Vendeuse, Livreur (SYS-05). | _(à compléter)_ | _(à compléter)_ | _(à compléter)_ | ☐ |
| 6 | Chaque maquette indique l'ID du ticket qu'elle illustre (ex. `ART-02`). | _(à compléter)_ | _(à compléter)_ | _(à compléter)_ | ☐ |
| 7 | Les écrans déjà développés (connexion, utilisateurs, journal) sont repris dans les maquettes pour garder la cohérence d'ensemble. | _(à compléter)_ | _(à compléter)_ | _(à compléter)_ | ☐ |
| 8 | Le lien vers le projet de maquettes est ajouté à ce ticket, puis relu et validé par l'équipe. | _(à compléter)_ | _(à compléter)_ | _(à compléter)_ | ☐ |

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

- `valimbavaka-ui/src/app/features/demo-pages.a11y.spec.ts`
- `valimbavaka-ui/src/app/features/journal/pages/journal-list-page/journal-list-page.spec.ts`
- `valimbavaka-ui/src/app/features/user/pages/user-list-page/user-list-page.spec.ts`
- `valimbavaka-ui/src/app/shared/components/confirm-dialog/confirm-dialog.spec.ts`

## Résultat de l'exécution

- Date : _(à compléter)_
- Environnement : _(à compléter)_
- Anomalies ouvertes : _(à compléter)_
