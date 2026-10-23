# Scénarios de test — AUTH-04 · Activation / désactivation d'un utilisateur

Statut : ✔ rempli depuis les tests existants au 2026-10-23 — à valider par le traiteur · Ticket : [#13](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/13) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (acteur), `SELLER` (cible et rôle refusé) |
| Données de départ | deux administrateurs actifs, une vendeuse active, une vendeuse désactivée |
| Tickets à avoir déployés avant | AUTH-03, JNL-01 |

## Scénarios issus des critères d'acceptation

| # | Critère | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | La liste affiche un badge ACTIF / DÉSACTIVÉ | Quand la liste des utilisateurs se charge | badge ACTIF pour un compte actif, DÉSACTIVÉ pour un désactivé | `user-list-page.spec.ts` « should show an ACTIF badge for an active user and DÉSACTIVÉ for a disabled one » | ☐ |
| 1b | Désactivation depuis la liste | Quand l'administrateur confirme la désactivation | appel API, liste rechargée | `user-list-page.spec.ts` « should deactivate an active user after confirmation and reload the list » | ☐ |
| 1c | idem | Quand il refuse la confirmation | aucun appel API | `user-list-page.spec.ts` « should not call the API when the confirmation is declined » | ☐ |
| 1d | idem | Quand le serveur refuse | message du serveur affiché | `user-list-page.spec.ts` « should show the server error when the status change is refused » | ☐ |
| 2 | Un désactivé n'apparaît plus dans les listes de sélection opérationnelles | Quand une liste de sélection (vente, livraison) se charge | aucun compte désactivé proposé | _(à ajouter dans les tickets qui créent ces listes : le filtre `active` n'existe pas encore sur `GET /api/v1/users`)_ | ☐ |
| 3 | Un désactivé ne peut plus se connecter | Quand le compte est désactivé puis tente de se connecter avec le bon mot de passe | 401, message unique | `UserStatusIT.setActive_deactivatedUser_cannotLogInAnymore` ; `AuthServiceImplTest.login_inactiveAccount_throwsBadCredentialsEvenWithGoodPassword` | ☐ |
| 3b | idem (session ouverte) | Quand le compte désactivé renouvelle sa session | refusé, session révoquée | `AuthServiceImplTest.refresh_deactivatedAccount_revokesFamilyAndThrowsUnauthorized` | ☐ |
| 4 | Le dernier administrateur actif ne peut être ni désactivé ni privé de son rôle | Quand on désactive le dernier administrateur actif via `PATCH /status` | 422 « Le dernier administrateur actif ne peut pas être désactivé. », rien tracé | `UserServiceImplTest.setActive_lastActiveAdministrator_throwsBusinessExceptionAndRecordsNothing` | ☐ |
| 4b | idem (propre compte) | Quand l'administrateur désactive son propre compte | 422 | `UserStatusIT.setActive_ownAccount_returns422` ; `UserServiceImplTest.setActive_ownAccount_throwsBusinessExceptionAndRecordsNothing` | ☐ |
| 4c | idem (retrait du rôle) | Quand on change le rôle du dernier administrateur via `PUT /api/v1/users/{id}` | doit être refusé | _(écart : non couvert par le code ni par un test, voir le guide)_ | ☐ |

## Sécurité et erreurs

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Sans jeton | 401 `UNAUTHENTICATED` | `UserStatusIT.setActive_withoutToken_returns401` | ☐ |
| Mauvais rôle | 403 `FORBIDDEN` | `UserStatusIT.setActive_withNonAdminRole_returns403` | ☐ |
| Désactivation puis traçabilité | statut modifié et ligne de journal nominative | `UserStatusIT.setActive_byAdmin_togglesStatusAndTracesInJournal` ; `UserServiceImplTest.setActive_deactivateActiveSeller_deactivatesAndRecordsInJournal` | ☐ |
| Activation d'un compte désactivé | statut actif, ligne de journal | `UserServiceImplTest.setActive_activateInactiveUser_activatesAndRecordsInJournal` | ☐ |
| État déjà celui demandé | aucun effet, aucune trace | `UserServiceImplTest.setActive_stateUnchanged_changesNothingAndRecordsNothing` | ☐ |
| Corps sans `active` | 422 `VALIDATION_FAILED` | `UserStatusIT.setActive_missingStatus_returns422` | ☐ |
| Champ `role` glissé dans le corps | ignoré | `UserStatusIT.setActive_massAssignmentOfRole_isIgnored` | ☐ |
| Identifiant inconnu | 404 `NOT_FOUND` | `UserServiceImplTest.setActive_unknownId_throwsNotFound` | ☐ |
| Désactivation via `PUT` avec `active=false` sur son propre compte | doit être refusée comme via `PATCH` | _(écart : non couvert)_ | ☐ |
| `DELETE /api/v1/users/{id}` sur le dernier administrateur | doit être refusé | _(écart : non couvert)_ | ☐ |

## Cas limites et hors ligne

- Désactiver un compte qui a une session ouverte : le jeton d'accès reste valable jusqu'à 15 min, le refresh échoue.
- Deux administrateurs : désactiver l'un depuis l'autre réussit ; désactiver le second ensuite est refusé.
- Réactiver un compte verrouillé par des échecs de connexion : le verrou (`locked_until`) n'est pas levé par l'activation.

## Tests existants (repérés dans le code)

- Backend : `feature/user/controller/UserStatusIT.java`, `feature/user/service/impl/UserServiceImplTest.java`, `security/service/impl/AuthServiceImplTest.java`
- Frontend : `features/user/pages/user-list-page/user-list-page.spec.ts`

## À faire à la main (recette QA)

- Désactiver une vendeuse depuis la liste, vérifier le badge, puis tenter une connexion avec ce compte.
- Avec un seul administrateur actif, tenter de le désactiver : message d'erreur.
- Modifier le rôle du dernier administrateur depuis le formulaire (écart attendu : à signaler).
- Consulter le journal : les lignes d'activation et de désactivation y figurent.

## Résultat de l'exécution

- Date : _(à compléter)_
- Environnement : _(à compléter)_
- Anomalies ouvertes : _(à compléter)_
