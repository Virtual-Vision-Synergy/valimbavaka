# Scénarios de test — AUTH-04 · Activation / désactivation d'un utilisateur

Statut : ✅ exécuté le 2026-10-23 — reste la recette manuelle (🧪) · Ticket : [#13](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/13) · [description](description.md) · [guide](guide.md)

Légende de la colonne « Statut » : ✅ auto = test automatique vert · ✅ base = vérifié en base après migrations · 🔍 code = vérifié par lecture du code (aucun test) · ❌ écart = comportement différent de l'attendu · ⏭ hors périmètre = renvoyé à un autre ticket · 🧪 recette = à faire à la main. Détail : [CONSIGNE.md](../CONSIGNE.md#4-remplir-le-scénario-de-test).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (acteur), `SELLER` (cible et rôle refusé) |
| Données de départ | deux administrateurs actifs (A1, A2), une vendeuse active, une vendeuse désactivée |
| Tickets à avoir déployés avant | AUTH-03, JNL-01 |

## Scénarios issus des critères d'acceptation

| # | Critère | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Badge ACTIF / DÉSACTIVÉ | Quand la liste se charge | ACTIF pour un compte actif, DÉSACTIVÉ sinon | `user-list-page.spec.ts` « should show an ACTIF badge for an active user and DÉSACTIVÉ for a disabled one » | ✅ auto |
| 1b | Désactivation depuis la liste | Quand l'administrateur confirme | appel `PATCH /status`, liste rechargée | `user-list-page.spec.ts` « should deactivate an active user after confirmation and reload the list » | ✅ auto |
| 1c | idem | Quand il annule la confirmation | aucun appel | `user-list-page.spec.ts` « should not call the API when the confirmation is declined » | ✅ auto |
| 1d | idem | Quand le serveur refuse | message du serveur affiché | `user-list-page.spec.ts` « should show the server error when the status change is refused » | ✅ auto |
| 2 | Un désactivé n'apparaît plus dans les listes de sélection | Quand une liste de sélection (vente, livraison) se charge | aucun compte désactivé | aucune liste de sélection n'existe encore ; `GET /api/v1/users` n'a pas de filtre `active` | ⏭ hors périmètre (VTE-01, LIV-01, COM-03) |
| 3 | Un désactivé ne peut plus se connecter | Quand il se connecte avec le bon mot de passe | 401, message unique | `UserStatusIT.setActive_deactivatedUser_cannotLogInAnymore` ; `AuthServiceImplTest.login_inactiveAccount_throwsBadCredentialsEvenWithGoodPassword` | ✅ auto |
| 3b | idem (session ouverte) | Quand il renouvelle sa session | refusé, famille révoquée | `AuthServiceImplTest.refresh_deactivatedAccount_revokesFamilyAndThrowsUnauthorized` | ✅ auto |
| 4 | Dernier administrateur : pas de désactivation | Quand on désactive le dernier admin actif via `PATCH /status` | 422 « Le dernier administrateur actif ne peut pas être désactivé. », rien tracé | `UserServiceImplTest.setActive_lastActiveAdministrator_throwsBusinessExceptionAndRecordsNothing` | ✅ auto |
| 4b | idem (propre compte) | Quand A1 se désactive lui-même | 422 | `UserStatusIT.setActive_ownAccount_returns422` ; `UserServiceImplTest.setActive_ownAccount_throwsBusinessExceptionAndRecordsNothing` | ✅ auto |
| 4c | Dernier administrateur : pas de retrait de rôle | Quand on passe son `roleId` à `SELLER` via `PUT /api/v1/users/{id}` | 422 « Le dernier administrateur actif ne peut pas perdre son rôle. » ; sur son propre compte : « … retirer votre propre rôle d'administrateur. » | `UserServiceImplTest.updateUser_lastActiveAdministratorDemoted_throwsBusinessExceptionAndChangesNothing`, `updateUser_ownAdministratorRoleRemoved_throwsBusinessExceptionAndChangesNothing` ; `UserStatusIT.updateUser_ownAdministratorRoleRemoved_returns422AndKeepsTheRole` | ✅ auto (corrigé le 2026-10-23) |
| 4d | idem via `PUT` avec `active=false` | Quand on désactive le dernier admin ou son propre compte par `PUT` | 422 comme via `PATCH` | `UserServiceImplTest.updateUser_lastActiveAdministratorDeactivated_throwsBusinessExceptionAndChangesNothing`, `updateUser_ownAccountDeactivated_throwsBusinessExceptionAndChangesNothing` ; `UserStatusIT.updateUser_ownAccountDeactivated_returns422AndKeepsTheAccountActive` | ✅ auto (corrigé le 2026-10-23) |
| 4e | idem via `DELETE` | Quand on supprime un compte | 405 : la suppression n'existe plus | `UserStatusIT.deleteUser_isNotExposed` | ✅ auto (corrigé le 2026-10-23) |
| 4f | Rétrograder un admin quand un autre reste actif | A1 passe A2 en vendeuse | accepté, ligne `UPDATE` | `UserServiceImplTest.updateUser_administratorDemotedWhileAnotherIsActive_updatesAndRecordsInJournal` | ✅ auto |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | — | 401 `UNAUTHENTICATED` | `UserStatusIT.setActive_withoutToken_returns401` | ✅ auto |
| Mauvais rôle | jeton vendeuse | 403 `FORBIDDEN` | `UserStatusIT.setActive_withNonAdminRole_returns403` | ✅ auto |
| Désactivation tracée | A1 désactive la vendeuse | statut modifié, ligne `UPDATE` nominative | `UserStatusIT.setActive_byAdmin_togglesStatusAndTracesInJournal` ; `UserServiceImplTest.setActive_deactivateActiveSeller_deactivatesAndRecordsInJournal` | ✅ auto |
| Activation d'un désactivé | — | actif, ligne de journal | `UserServiceImplTest.setActive_activateInactiveUser_activatesAndRecordsInJournal` | ✅ auto |
| État déjà demandé | désactiver un désactivé | aucun effet, aucune trace | `UserServiceImplTest.setActive_stateUnchanged_changesNothingAndRecordsNothing` | ✅ auto |
| Corps sans `active` | `{}` | 422 `VALIDATION_FAILED` | `UserStatusIT.setActive_missingStatus_returns422` | ✅ auto |
| `role` glissé dans le corps | `{"active":false,"role":"ADMIN"}` | ignoré | `UserStatusIT.setActive_massAssignmentOfRole_isIgnored` | ✅ auto |
| Identifiant inconnu | UUID au hasard | 404 | `UserServiceImplTest.setActive_unknownId_throwsNotFound` | ✅ auto |

## Cas limites

| Cas | Exemple | Résultat attendu | Comment c'est vérifié | Statut |
|---|---|---|---|---|
| Session ouverte au moment de la désactivation | vendeuse connectée, désactivée par A1 | son jeton d'accès reste valable jusqu'à 15 min ; le refresh suivant échoue et la déconnecte | `AuthServiceImpl.currentUser` / `refresh` refusent un compte inactif ; le JWT n'est pas révoqué | 🔍 code |
| Deux administrateurs | A1 désactive A2, puis tente de se désactiver | 1ʳᵉ action acceptée, 2ᵉ refusée (propre compte et dernier admin) | `ensureCanDeactivate` + `countByRoleNameAndActiveTrue(ADMIN)` | 🔍 code |
| Réactivation d'un compte verrouillé | 5 échecs, puis désactivation et réactivation | le verrou (`locked_until`, `failed_attempts`) n'est pas levé par l'activation | `User#activate` ne touche que `active` | 🔍 code |
| Désactivations simultanées des deux derniers admins | A1 et A2 se désactivent mutuellement au même instant | une seule réussit | `UserRepository.findActiveByRoleNameForUpdate` verrouille les administrateurs actifs (`PESSIMISTIC_WRITE`) le temps de la transaction | 🔍 code (corrigé le 2026-10-23) |
| Désactivation hors ligne | file hors ligne | rejouée avec la même `Idempotency-Key`, une seule ligne de journal | voir SYS-02 (`IdempotencyIT`) | ✅ auto |

## Tests existants

- Backend : `feature/user/controller/UserStatusIT.java`, `feature/user/service/impl/UserServiceImplTest.java`, `security/service/impl/AuthServiceImplTest.java`
- Frontend : `features/user/pages/user-list-page/user-list-page.spec.ts`

## Recette manuelle (🧪)

| Étape | Attendu | Statut |
|---|---|---|
| Désactiver une vendeuse depuis la liste puis se connecter avec ce compte | badge DÉSACTIVÉ, connexion refusée | 🧪 recette |
| Avec un seul administrateur actif, tenter de le désactiver | message d'erreur | 🧪 recette |
| Ouvrir le journal | lignes d'activation et de désactivation présentes | 🧪 recette |

## Résultat de l'exécution

- **Date** : 2026-10-23.
- **Backend** : CI GitHub `mvn verify` vert sur un code identique à `develop`. **Frontend** : `ng test` local, 102 tests verts sur 102.
- **Anomalies** :
  1. ✅ Corrigé le 2026-10-23 (branche `fix/AUTH-04-garde-fous-administrateur`) : `PUT /api/v1/users/{id}` applique les mêmes garde-fous que `PATCH /status` (désactivation et retrait du rôle administrateur).
  2. ✅ Corrigé le 2026-10-23 : `DELETE /api/v1/users/{id}` n'existe plus (voir AUTH-03).
  3. ✅ Corrigé le 2026-10-23 : les administrateurs actifs sont verrouillés pendant la vérification ; pas de test de concurrence automatisé.
  4. ⏭ Critère 2 : à porter par les tickets qui créent les listes de sélection (ajouter `active` et `role` comme filtres de `GET /api/v1/users`).
- **Recette manuelle** : 3 étapes 🧪.
