# Scénarios de test — AUTH-01 · Connexion selon le rôle attribué

Statut : ✔ rempli depuis les tests existants au 2026-10-23 — à valider par le traiteur · Ticket : [#10](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/10) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (compte initial SYS-05), `SELLER`, `DELIVERY_DRIVER` |
| Données de départ | un compte par rôle, actif ; référentiel des rôles et permissions migré |
| Tickets à avoir déployés avant | SYS-05 |

## Scénarios issus des critères d'acceptation

| # | Critère | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Le rôle appliqué est celui du compte, non choisi par l'utilisateur | Étant donné un compte `ADMIN` · Quand il se connecte avec le bon mot de passe | 200, `user.role = ADMIN`, permissions de l'administrateur, cookie `vvs_refresh` HttpOnly | `AuthSecurityIT.login_adminCredentials_returnsAdminRoleWithItsPermissionsAndHttpOnlyRefreshCookie` | ☐ |
| 1b | idem | Quand le corps de la requête contient un autre rôle | le champ est ignoré, le rôle du compte est conservé | `AuthSecurityIT.login_bodyCarryingAnotherRole_ignoresItAndKeepsTheRoleOfTheAccount` ; `login-page.spec.ts` « should send only the email and password, never a role » | ☐ |
| 2 | L'interface correspond uniquement au rôle (menu) | Étant donné un administrateur · Quand le menu s'affiche | tous les modules, dont Utilisateurs | `menu.spec.ts` « should show every module to the administrator » ; `main-layout.spec.ts` « should show every module including users to the administrator » | ☐ |
| 2b | idem | Étant donné une vendeuse | modules de vente et catalogue, aucune administration, pas de livraisons | `menu.spec.ts` « should show sales and catalogue modules but no administration to a seller » ; `main-layout.spec.ts` « should hide users and deliveries from a seller » | ☐ |
| 2c | idem | Étant donné un livreur | seulement tableau de bord et livraisons | `menu.spec.ts` « should show only the dashboard and deliveries to a delivery driver » | ☐ |
| 3 | Sécurité aussi au niveau requête | Quand un compte sans la permission appelle l'API | 403 `FORBIDDEN` | `AuthSecurityIT.getUsers_withNonAdminRole_returns403`, `getUsers_withoutToken_returns401`, `getUsers_withAdminRole_returns200`, `me_withSellerToken_returnsOnlyTheSellerPermissions` | ☐ |
| 4 | Accès direct par URL à un module interdit | Quand une vendeuse ouvre une URL de module non autorisé | redirection vers l'accueil du rôle | `permission-guard.spec.ts` (autorise avec la permission, redirige sinon) | ☐ |

## Sécurité et erreurs

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Mauvais mot de passe | 401, message unique « Identifiants invalides » | `AuthSecurityIT.login_wrongPassword_returns401WithUniqueMessage` ; `login-page.spec.ts` « should show a generic message when the credentials are rejected » | ☐ |
| Email inconnu | même exception et même message, comparaison avec un hash factice | `AuthServiceImplTest.login_unknownEmail_throwsSameBadCredentialsAndComparesAgainstDummyHash` | ☐ |
| Compte désactivé avec le bon mot de passe | refusé comme un mauvais mot de passe | `AuthServiceImplTest.login_inactiveAccount_throwsBadCredentialsEvenWithGoodPassword` | ☐ |
| 5 échecs consécutifs | compte verrouillé même avec le bon mot de passe | `AuthSecurityIT.login_afterFiveFailures_locksTheAccountEvenWithTheRightPassword`, `AuthServiceImplTest.login_lockedAccount_...`, `LockPolicyTest` | ☐ |
| Trop de tentatives pour un identifiant | 429 `RATE_LIMITED` avec `Retry-After` | `LoginRateLimitIT.login_moreAttemptsThanAllowedForOneIdentifier_returns429WithRetryAfter` | ☐ |
| Autre identifiant sur la même IP | non limité | `LoginRateLimitIT.login_otherIdentifierOnSameIp_isNotThrottled` | ☐ |
| Formulaire invalide côté interface | aucun appel serveur | `login-page.spec.ts` « should not call the server while the form is invalid » | ☐ |
| Refresh valide | ancien jeton révoqué, même famille de session | `AuthServiceImplTest.refresh_validToken_revokesOldOneAndKeepsTheSameFamily` | ☐ |
| Refresh rejoué (jeton déjà utilisé) | refusé, toute la session révoquée | `AuthSecurityIT.refresh_reusedCookie_isRejectedAndRevokesTheWholeSession`, `AuthServiceImplTest.refresh_revokedToken_...` | ☐ |
| Refresh sans cookie / jeton expiré / compte désactivé | 401 | `AuthSecurityIT.refresh_withoutCookie_returns401`, `AuthServiceImplTest.refresh_expiredToken_...`, `refresh_deactivatedAccount_...` | ☐ |
| Refresh depuis une origine étrangère | 403 même avec un cookie valide | `AuthSecurityIT.refresh_fromForeignOrigin_returns403EvenWithAValidCookie` ; origine autorisée : `refresh_fromAllowedOrigin_returns200` | ☐ |
| Mot de passe stocké | Argon2id pour les nouveaux, bcrypt hérité encore vérifié | `PasswordEncoderIT` | ☐ |

## Cas limites et session côté interface

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Connexion réussie | jeton gardé en mémoire uniquement | `auth-service.spec.ts` « should store the session in memory after a successful login » | ☐ |
| Plusieurs appels simultanés qui demandent un refresh | un seul refresh envoyé | `auth-service.spec.ts` « should send a single refresh request for simultaneous callers » | ☐ |
| Cookie de refresh refusé au démarrage | aucune session restaurée, retour au login | `auth-service.spec.ts` « should report no session when the refresh cookie is rejected » | ☐ |
| 401 sur un appel d'API | un refresh puis rejeu avec le nouveau jeton | `session-interceptor.spec.ts` « should refresh once and replay the request with the new token after a 401 » | ☐ |
| Refresh impossible | déconnexion et redirection vers `/login` | `session-interceptor.spec.ts` « should sign out and go to the login page when the refresh fails » | ☐ |
| 403 ou refus du login lui-même | aucune tentative de refresh | `session-interceptor.spec.ts` « should not try to refresh after a 403 », « ... when the login itself is rejected » | ☐ |
| Jeton vers un domaine tiers | jamais envoyé | `auth-interceptor.spec.ts` « should never send the token to a third-party domain » | ☐ |
| Utilisateur déjà connecté ouvre `/login` | renvoyé vers l'accueil | `auth-guard.spec.ts` « guestGuard should send an authenticated user back to the home page » | ☐ |

## Tests existants (repérés dans le code)

- Backend : `feature/auth/controller/AuthSecurityIT.java`, `security/service/impl/AuthServiceImplTest.java`, `security/LockPolicyTest.java`, `security/ratelimit/LoginRateLimitIT.java`, `security/config/PasswordEncoderIT.java`
- Frontend : `core/auth/auth-service.spec.ts`, `auth-store.spec.ts`, `auth-guard.spec.ts`, `permission-guard.spec.ts`, `core/http/auth-interceptor.spec.ts`, `session-interceptor.spec.ts`, `features/auth/pages/login-page/login-page.spec.ts`, `login-page.a11y.spec.ts`, `core/layout/menu.spec.ts`, `core/layout/main-layout/main-layout.spec.ts`

## À faire à la main (recette QA)

- Se connecter avec chacun des trois rôles et vérifier le menu affiché et l'accueil.
- Taper dans la barre d'adresse l'URL d'un module interdit au rôle : redirection vers l'accueil.
- Avec un jeton de vendeuse, appeler `GET /api/v1/users` (Swagger ou cURL) : 403.

## Résultat de l'exécution

- Date : _(à compléter)_
- Environnement : _(à compléter)_
- Anomalies ouvertes : _(à compléter)_
