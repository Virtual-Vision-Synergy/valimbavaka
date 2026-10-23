# Scénarios de test — AUTH-01 · Connexion selon le rôle attribué

Statut : ✅ exécuté le 2026-10-23 — reste la recette manuelle (🧪) · Ticket : [#10](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/10) · [description](description.md) · [guide](guide.md)

Légende de la colonne « Statut » : ✅ auto = test automatique vert · ✅ base = vérifié en base après migrations · 🔍 code = vérifié par lecture du code (aucun test) · ❌ écart = comportement différent de l'attendu · 🧪 recette = à faire à la main dans le navigateur. Détail des sources : [CONSIGNE.md](../CONSIGNE.md#4-remplir-le-scénario-de-test).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (compte initial SYS-05), `SELLER`, `DELIVERY_DRIVER` |
| Données de départ | un compte actif par rôle ; rôles et permissions migrés |
| Comptes d'exemple | `admin.test@valimbavaka.mg`, `vendeuse.test@valimbavaka.mg`, `livreur.test@valimbavaka.mg` |
| Tickets à avoir déployés avant | SYS-05 |

## Scénarios issus des critères d'acceptation

| # | Critère | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Le rôle appliqué est celui du compte | Étant donné un compte `ADMIN` · Quand il se connecte avec le bon mot de passe | 200, `user.role = ADMIN`, permissions de l'administrateur, cookie `vvs_refresh` HttpOnly | `AuthSecurityIT.login_adminCredentials_returnsAdminRoleWithItsPermissionsAndHttpOnlyRefreshCookie` | ✅ auto |
| 1b | idem | Quand le corps contient `"role":"ADMIN"` pour un compte vendeuse | champ ignoré, rôle du compte conservé | `AuthSecurityIT.login_bodyCarryingAnotherRole_ignoresItAndKeepsTheRoleOfTheAccount` ; `login-page.spec.ts` « should send only the email and password, never a role » | ✅ auto |
| 1c | idem | Quand le service émet les jetons | rôle et permissions lus sur le compte stocké | `AuthServiceImplTest.login_validCredentials_returnsTokensForTheRoleStoredOnTheAccount` | ✅ auto |
| 2 | Le menu correspond au rôle | Étant donné un administrateur | tous les modules, dont Utilisateurs | `menu.spec.ts` « should show every module to the administrator » ; `main-layout.spec.ts` « should show every module including users to the administrator » | ✅ auto |
| 2b | idem | Étant donné une vendeuse | vente et catalogue, ni administration ni livraisons | `menu.spec.ts` « should show sales and catalogue modules but no administration to a seller » ; `main-layout.spec.ts` « should hide users and deliveries from a seller » | ✅ auto |
| 2c | idem | Étant donné un livreur | tableau de bord et livraisons seulement | `menu.spec.ts` « should show only the dashboard and deliveries to a delivery driver » | ✅ auto |
| 3 | Sécurité au niveau requête | Quand une vendeuse appelle `GET /api/v1/users` | 403 `FORBIDDEN` ; sans jeton 401 ; administrateur 200 | `AuthSecurityIT.getUsers_withNonAdminRole_returns403`, `getUsers_withoutToken_returns401`, `getUsers_withAdminRole_returns200` | ✅ auto |
| 3b | idem | Quand la vendeuse appelle `/api/v1/auth/me` | seulement ses permissions | `AuthSecurityIT.me_withSellerToken_returnsOnlyTheSellerPermissions` | ✅ auto |
| 3c | Toute route d'API exige une permission | Quand un développeur ajoute un endpoint sans `@HasPermission` | le build échoue | `ArchitectureTest.endpoints_require_permission` | ✅ auto |
| 4 | Accès direct par URL interdit | Quand une vendeuse ouvre `/users` | redirection vers l'accueil | `permission-guard.spec.ts` « should redirect to the home page when the role lacks the permission » | ✅ auto |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Mauvais mot de passe | bon email, `mauvais-mdp` | 401, message unique « Identifiants invalides » | `AuthSecurityIT.login_wrongPassword_returns401WithUniqueMessage` ; `login-page.spec.ts` « should show a generic message when the credentials are rejected » | ✅ auto |
| Email inconnu | `inconnu@valimbavaka.mg` | même exception et même message, comparaison avec un hash factice (pas d'énumération) | `AuthServiceImplTest.login_unknownEmail_throwsSameBadCredentialsAndComparesAgainstDummyHash` | ✅ auto |
| Compte désactivé, bon mot de passe | vendeuse désactivée | refusé comme un mauvais mot de passe | `AuthServiceImplTest.login_inactiveAccount_throwsBadCredentialsEvenWithGoodPassword` | ✅ auto |
| 5 échecs consécutifs | 5 mauvais mots de passe puis le bon | compte verrouillé 5 min (+5 min par échec, plafond 1 h) | `AuthSecurityIT.login_afterFiveFailures_locksTheAccountEvenWithTheRightPassword` ; `AuthServiceImplTest.login_lockedAccount_throwsBadCredentialsEvenWithGoodPassword` ; `LockPolicyTest.lockDurationFor_failedAttempts_growsAfterThresholdAndIsCapped` | ✅ auto |
| Trop de tentatives pour un identifiant | 11 essais en 1 min | 429 `RATE_LIMITED` avec `Retry-After` | `LoginRateLimitIT.login_moreAttemptsThanAllowedForOneIdentifier_returns429WithRetryAfter` | ✅ auto |
| Autre identifiant, même IP | 2ᵉ compte depuis le même poste | non limité | `LoginRateLimitIT.login_otherIdentifierOnSameIp_isNotThrottled` | ✅ auto |
| Formulaire invalide | email vide | aucun appel serveur | `login-page.spec.ts` « should not call the server while the form is invalid » | ✅ auto |
| Refresh valide | cookie `vvs_refresh` du jour | nouveau couple de jetons, ancien révoqué, même famille | `AuthServiceImplTest.refresh_validToken_revokesOldOneAndKeepsTheSameFamily` | ✅ auto |
| Refresh rejoué | ancien cookie réutilisé après rotation | refusé, toute la famille révoquée | `AuthSecurityIT.refresh_reusedCookie_isRejectedAndRevokesTheWholeSession` ; `AuthServiceImplTest.refresh_revokedToken_revokesWholeFamilyAndThrowsUnauthorized` | ✅ auto |
| Refresh sans cookie, expiré, compte désactivé | — | 401 | `AuthSecurityIT.refresh_withoutCookie_returns401` ; `AuthServiceImplTest.refresh_expiredToken_throwsUnauthorized`, `refresh_deactivatedAccount_revokesFamilyAndThrowsUnauthorized` | ✅ auto |
| Refresh depuis une origine étrangère | `Origin: https://evil.example` | 403 même avec un cookie valide ; origine autorisée : 200 | `AuthSecurityIT.refresh_fromForeignOrigin_returns403EvenWithAValidCookie`, `refresh_fromAllowedOrigin_returns200` | ✅ auto |
| Hachage du mot de passe | nouveau compte ; compte bcrypt hérité | Argon2id ; bcrypt encore vérifié | `PasswordEncoderIT.matches_legacyBcryptHash_stillVerifies` | ✅ auto |
| Documentation OpenAPI | `/v3/api-docs` avec jeton | les endpoints d'authentification sont documentés | `OpenApiAccessIT.apiDocs_withToken_documentsTheAuthEndpoints` | ✅ auto |

## Cas limites

| Cas | Exemple d'entrée | Résultat attendu | Comment c'est vérifié | Statut |
|---|---|---|---|---|
| Email avec majuscules et espaces | `  Admin.Test@Valimbavaka.MG ` | connexion acceptée (email normalisé `strip` + minuscules) | `User.normalizeEmail` ; compte de test créé avec `Admin.Test@Valimbavaka.mg` | 🔍 code |
| Jeton gardé en mémoire uniquement | connexion réussie puis rechargement | aucun jeton dans `localStorage` ; session restaurée par le cookie | `auth-service.spec.ts` « should store the session in memory after a successful login » | ✅ auto |
| Plusieurs requêtes simultanées en 401 | 3 appels d'API avec un jeton expiré | un seul `/refresh` envoyé | `auth-service.spec.ts` « should send a single refresh request for simultaneous callers » | ✅ auto |
| Cookie refusé au démarrage | cookie expiré | pas de session, retour au login | `auth-service.spec.ts` « should report no session when the refresh cookie is rejected » | ✅ auto |
| 401 sur un appel d'API | jeton expiré après 15 min | un refresh puis rejeu | `session-interceptor.spec.ts` « should refresh once and replay the request with the new token after a 401 » | ✅ auto |
| Refresh impossible | cookie révoqué | déconnexion, redirection `/login` | `session-interceptor.spec.ts` « should sign out and go to the login page when the refresh fails » | ✅ auto |
| 403 ou login refusé | — | aucune tentative de refresh | `session-interceptor.spec.ts` « should not try to refresh after a 403 », « should not try to refresh when the login itself is rejected » | ✅ auto |
| Changement de compte pendant un refresh | B se connecte pendant le refresh de A | la session de B n'est pas écrasée | `session-interceptor.spec.ts` « should not refresh a request after another account has signed in », « should not replace a new account when an earlier refresh completes » | ✅ auto |
| Jeton vers un domaine tiers | appel `https://api.tiers.com` | jeton jamais envoyé | `auth-interceptor.spec.ts` « should never send the token to a third-party domain » | ✅ auto |
| Utilisateur connecté qui ouvre `/login` | — | renvoyé vers l'accueil | `auth-guard.spec.ts` « guestGuard should send an authenticated user back to the home page » | ✅ auto |
| Permissions exactes du rôle | vendeuse connectée | `can('users.read')` faux | `auth-store.spec.ts` « should grant exactly the permissions of the signed in role » | ✅ auto |
| Accessibilité de la page de connexion | — | aucune violation détectée | `login-page.a11y.spec.ts` | ✅ auto |
| Compte avec changement de mot de passe obligatoire | administrateur initial | l'écran de changement est imposé | aucun écran ni endpoint de changement de mot de passe | ❌ écart |

## Tests existants

- Backend : `feature/auth/controller/AuthSecurityIT.java`, `OpenApiAccessIT.java`, `security/service/impl/AuthServiceImplTest.java`, `security/LockPolicyTest.java`, `security/ratelimit/LoginRateLimitIT.java`, `security/config/PasswordEncoderIT.java`, `ArchitectureTest.java`
- Frontend : `core/auth/auth-service.spec.ts`, `auth-store.spec.ts`, `auth-guard.spec.ts`, `permission-guard.spec.ts`, `core/http/auth-interceptor.spec.ts`, `session-interceptor.spec.ts`, `features/auth/pages/login-page/login-page.spec.ts`, `login-page.a11y.spec.ts`, `core/layout/menu.spec.ts`, `core/layout/main-layout/main-layout.spec.ts`

## Recette manuelle (🧪)

| Étape | Attendu | Statut |
|---|---|---|
| Se connecter avec chacun des trois rôles | menu et accueil propres au rôle | 🧪 recette |
| Taper l'URL d'un module interdit (ex. `/users` en vendeuse) | retour à l'accueil | 🧪 recette |
| Avec un jeton de vendeuse, `GET /api/v1/users` dans Swagger | 403 | 🧪 recette |
| Rester inactif plus de 15 min puis cliquer | la page se recharge sans déconnexion (refresh transparent) | 🧪 recette |

## Résultat de l'exécution

- **Date** : 2026-10-23.
- **Backend** : CI GitHub, job « Backend (Spring Boot) » (`mvn -B -ntp verify`, Testcontainers PostgreSQL 16, ArchUnit) vert sur le commit `b009e97`, dont le code est identique à `develop` au 2026-10-23. 27 classes de test, 138 `@Test`, 5 tests paramétrés, 16 règles ArchUnit.
- **Frontend** : `ng test --watch=false` relancé localement (Node 24) : 29 fichiers, 102 tests, 102 verts.
- **Anomalies** :
  1. ❌ Aucun écran ni endpoint de changement de mot de passe : le drapeau `passwordChangeRequired` du premier administrateur (SYS-05) ne peut pas être levé. Gravité moyenne, à traiter dans un ticket dédié.
  2. ⚠ `PUBLIC_ROUTES` (interface) liste `register` et `forgot-password`, qui n'existent pas. Gravité faible.
  3. ⚠ Connexion par email uniquement (question client ouverte sur la connexion par téléphone). Gravité faible.
- **Recette manuelle** : 4 étapes 🧪 à passer par le traiteur ou la QA sur l'environnement de test.
