# Guide d'intégration — AUTH-01 · Connexion selon le rôle attribué

Statut : ✅ complet au 2026-10-23 — vérifié sur le code, les tests (CI + exécution locale) et la base · Ticket : [#10](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/10) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

L'utilisateur se connecte par email et mot de passe. Le serveur applique toujours le rôle stocké sur le compte (le corps de la requête ne peut pas le changer) et renvoie le jeton d'accès, le profil et la liste des permissions. L'interface construit son menu et ses routes à partir de ces permissions ; le backend refuse de toute façon (403) ce que le rôle n'autorise pas.

## Contrat exposé

### Endpoints backend (`/api/v1/auth`)

| Méthode | URL | Accès | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| POST | `/login` | public, limité en débit | `LoginWDto` (`email`, `password`) | 200 `AuthRDto` + cookie `vvs_refresh` | 401 `UNAUTHENTICATED`, 422, 429 `RATE_LIMITED` (+ `Retry-After`) |
| POST | `/refresh` | public, cookie `vvs_refresh` | - | 200 `AuthRDto` (rotation) + nouveau cookie | 401 `UNAUTHENTICATED`, 403 `FORBIDDEN` (origine) |
| GET | `/me` | jeton d'accès | - | `CurrentUserRDto` | 401 `UNAUTHENTICATED` |
| POST | `/password` | jeton d'accès | `PasswordChangeWDto` (`currentPassword`, `newPassword` 15 à 128 caractères) | 204 ; appeler ensuite `/refresh` | 401, 422 `VALIDATION_FAILED` (mot de passe actuel incorrect, nouveau trop court ou identique) |

`AuthRDto` : `accessToken`, `tokenType` (`Bearer`), `expiresIn` (secondes), `user`.
`CurrentUserRDto` : `id`, `email`, `firstName`, `lastName`, `role` (`ADMIN` / `SELLER` / `DELIVERY_DRIVER`), `roleLabel` (Administrateur / Vendeuse / Livreur), `permissions` (codes `module.action`), `passwordChangeRequired`.

Déconnexion : voir [AUTH-02](../AUTH-02/guide.md).

### Règles de sécurité à connaître

- Jeton d'accès JWT HS256, 15 min (`vvs.jwt.access-token-ttl`), secret fourni par `JWT_SECRET` (32 caractères minimum).
- Refresh token opaque, 7 jours, stocké haché (table `refresh_tokens`), avec rotation à chaque `/refresh`. Il voyage uniquement dans le cookie `vvs_refresh` : HttpOnly, SameSite=Strict, chemin `/api/v1/auth`, `Secure` piloté par `REFRESH_COOKIE_SECURE`.
- Réutilisation d'un refresh token déjà révoqué : toute la famille de session est révoquée. Un compte désactivé qui tente un refresh voit sa famille révoquée.
- Message de login unique « Identifiants invalides » pour email inconnu, mauvais mot de passe, compte désactivé ou verrouillé (un hash factice est comparé si l'email est inconnu, pour un temps de réponse comparable).
- Verrouillage après 5 échecs (`vvs.auth.max-failed-attempts`) : 5 min, puis +5 min par échec supplémentaire, plafonné à 1 h.
- Limitation de débit du login : 20 requêtes par IP et 10 par identifiant par minute (`vvs.rate-limit.*`).
- `/refresh` et `/logout` vérifient l'en-tête `Origin` (liste `CORS_ALLOWED_ORIGINS`) ; une origine étrangère reçoit 403.
- Mots de passe : Argon2id pour les nouveaux, les anciens hashs bcrypt restent vérifiables.
- Toute route hors `PUBLIC_ROUTES` exige un jeton (`anyRequest().authenticated()`).

### Permissions par rôle (migration `2026_10_19-01`, complétée par JNL-01 et FRS-01)

| Rôle | Permissions |
|---|---|
| `ADMIN` | toutes (`users.*`, `roles.read`, `journal.read`, `suppliers.*`, `referentials.read` et chaque `*.read` de module) |
| `SELLER` | `dashboard.read`, `articles.read`, `stocks.read`, `sales.read`, `promotions.read`, `commissions.read`, `services.read`, `referentials.read` |
| `DELIVERY_DRIVER` | `dashboard.read`, `deliveries.read` |

Pour protéger un endpoint : `@HasPermission("module.action")` sur la méthode du contrôleur (règle ArchUnit `endpoints_require_permission`). Pour ajouter une permission : une migration Liquibase qui insère dans `permissions` et `role_permissions`.

### Services et ports Java

- `security.service.AuthService` : `login`, `refresh`, `logout`, `currentUser` (implémentation `AuthServiceImpl`, volontairement sans `@Transactional` pour conserver les échecs enregistrés).
- Ports définis côté `security` et implémentés par les features : `AuthenticatedUserLoader` (implémenté par `feature.user`), `RefreshTokenStore` (implémenté par `feature.auth`).
- `security.jwt.JwtSubject.requireUserId(jwt)` : identifiant du compte connecté dans un contrôleur (`@AuthenticationPrincipal Jwt`).

### Frontend (`valimbavaka-ui/src/app`)

- `core/auth/auth-service.ts` : `login`, `refresh`, `restoreSession`, `logout`. Un seul refresh est envoyé pour plusieurs appelants simultanés.
- `core/auth/auth-store.ts` : jeton et utilisateur **en mémoire** (jamais `localStorage`), `isAuthenticated`, `currentUser`, `hasPermission(code)`.
- `core/auth/auth-guard.ts` (`authGuard`, `guestGuard`), `core/auth/permission-guard.ts` (`permissionGuard('code')`), `core/auth/has-permission-directive.ts` (`*vvsHasPermission="'users.create'"`).
- `core/http/auth-interceptor.ts` : ajoute `Authorization: Bearer` aux seuls appels de notre API. `core/http/session-interceptor.ts` : sur 401, un refresh puis rejeu ; déconnexion et redirection `/login` si le refresh échoue ; rien sur 403.
- `core/layout/menu.ts` : `MENU_SECTIONS`, chaque entrée porte la permission requise ; `main-layout` et `driver-layout` (espace livreur `/round`, permission `deliveries.read`).
- Routes (`app.routes.ts`) : `/login` (`guestGuard`), coquille principale (`authGuard`), un `permissionGuard` par module.

## Règles métier à respecter par les autres tickets

- Un nouveau module ajoute : sa permission `*.read` (migration), son `permissionGuard` dans `app.routes.ts`, son entrée dans `menu.ts`, et `@HasPermission` sur chaque endpoint.
- Ne jamais lire le rôle depuis un champ envoyé par le client ; `/me` relit le rôle en base à chaque appel.
- Le guard et le menu ne sont que du confort : le serveur décide.

## Fichiers concernés

**Backend**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/auth/controller/AuthController.java`, `RefreshCookieFactory.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/auth/{domain,dto,mapper,repository,service/impl}/` (`RefreshToken`, `UserDetailsImpl`, `AuthRDto`, `CurrentUserRDto`, `LoginWDto`, `AuthMapper`, `RefreshTokenRepository`, `RefreshTokenStoreImpl`)
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/security/service/AuthService.java`, `security/service/impl/AuthServiceImpl.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/security/{AuthProperties,LockPolicy,TokenPair,AuthenticatedUser,AuthenticatedUserLoader,RefreshTokenStore}.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/security/jwt/` (`JwtConfig`, `JwtProperties`, `JwtService`, `JwtSubject`, `TokenIssuer`)
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/security/config/` (`SecurityConfig`, `OriginCheckFilter`), `security/handler/`, `security/ratelimit/`, `security/annotation/HasPermission.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/user/domain/Permission.java`, `feature/user/service/impl/AuthenticatedUserLoaderImpl.java`

**Migrations Liquibase**
- `valimbavaka-core/src/main/resources/db/changelog/changes/2026_10_19-01-changelog.sql` (permissions, rôles/permissions, verrouillage, `refresh_tokens`)

**Frontend**
- `valimbavaka-ui/src/app/core/auth/*`, `core/http/*`, `core/layout/menu.ts`, `core/layout/main-layout/*`, `core/layout/driver-layout/*`, `core/layout/home-guard.ts`
- `valimbavaka-ui/src/app/features/auth/` (page de connexion)
- `valimbavaka-ui/src/app/app.routes.ts`, `app.config.ts`

**Tests**
- Backend : `feature/auth/controller/AuthSecurityIT.java`, `security/service/impl/AuthServiceImplTest.java`, `security/LockPolicyTest.java`, `security/ratelimit/LoginRateLimitIT.java`, `security/config/PasswordEncoderIT.java`
- Frontend : `core/auth/*.spec.ts`, `core/http/*.spec.ts`, `features/auth/pages/login-page/login-page.spec.ts`, `core/layout/menu.spec.ts`, `core/layout/main-layout/main-layout.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** :
- [SYS-05](../SYS-05/description.md) · [guide](../SYS-05/guide.md) : rôles, table `roles`, administrateur initial

**Ces tickets s'appuient sur lui** :
- [AUTH-02](../AUTH-02/guide.md) (déconnexion), [AUTH-03](../AUTH-03/guide.md) (utilisateurs), [JNL-01](../JNL-01/guide.md) (auteur nominatif des écritures), [SYS-02](../SYS-02/guide.md) (clé d'idempotence par utilisateur), [SYS-01](../SYS-01/guide.md), [SYS-04](../SYS-04/guide.md), [SYS-11](../SYS-11/guide.md)

## Comment l'utiliser depuis un autre ticket

Backend, protéger un endpoint :

```java
@GetMapping
@HasPermission("articles.read")
public PageResponse<ArticleRDto> getArticles(@Valid PageQuery query) { ... }
```

Frontend, cacher un bouton et garder une route :

```html
<button *vvsHasPermission="'users.create'">Nouvel utilisateur</button>
```

```ts
{ path: 'articles', canActivate: [permissionGuard('articles.read')], loadChildren: ... }
```

## Points d'attention

- Question client toujours ouverte (label `needs-info`) : connexion par email ou par identifiant / téléphone. Le code actuel n'accepte que l'email.
- `PUBLIC_ROUTES` dans `SecurityConfig` contient aussi `/api/v1/auth/register` et `/api/v1/auth/forgot-password`, alors qu'aucun contrôleur ne les expose : à nettoyer ou à justifier.
- **Mot de passe provisoire** (`passwordChangeRequired = true`, premier administrateur SYS-05) : le jeton et `/me` ne portent **aucune permission** tant que le mot de passe n'est pas changé (seules `/me`, `/password`, `/refresh`, `/logout` répondent). L'interface redirige vers `/password` (`authGuard`, `passwordChangeGuard`, page `features/auth/pages/password-change-page`) ; `AuthService.changePassword` enchaîne `POST /password` puis `/refresh` pour retrouver les droits du rôle. Un ticket qui ajoute un écran n'a rien à faire de plus.
- Le jeton d'accès porte les claims `roles` et `permissions` : après un changement de rôle par l'administrateur, l'ancien jeton garde ses droits jusqu'à son expiration (15 min). Seul `/me` relit le rôle en base.
