# Guide d'intégration — AUTH-02 · Déconnexion utilisateur

Statut : ✔ rempli depuis le code et les tests au 2026-10-23 — à valider par le traiteur · Ticket : [#11](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/11) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

La déconnexion révoque la session côté serveur (toute la famille de refresh tokens), efface le cookie `vvs_refresh` et vide l'état d'authentification en mémoire côté interface. Il faut ensuite se reconnecter pour accéder à l'application, avec le même compte ou un autre.

## Contrat exposé

### Endpoint backend

| Méthode | URL | Accès | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| POST | `/api/v1/auth/logout` | public (le cookie suffit, aucun jeton d'accès requis) | cookie `vvs_refresh` (facultatif) | 204, cookie `vvs_refresh` effacé (`Max-Age=0`) | 403 `FORBIDDEN` si l'en-tête `Origin` n'est pas autorisé |

- Sans cookie, l'appel réussit quand même (204) et efface le cookie.
- Avec un cookie, `AuthService.logout` révoque toute la famille de la session ; un `/refresh` ultérieur avec ce cookie renvoie 401.
- Le jeton d'accès déjà émis reste techniquement valable jusqu'à son expiration (15 min) : il n'est pas révoqué côté serveur.

### Services et ports Java

- `security.service.AuthService#logout(String rawRefreshToken)` ; `RefreshTokenStore#revokeFamily`.
- `feature.auth.controller.RefreshCookieFactory#clear()` fabrique le `Set-Cookie` d'effacement.

### Frontend

- `core/auth/auth-service.ts#logout()` : appelle `POST /api/v1/auth/logout`, ignore une erreur réseau, puis `AuthStore.signOut()` seulement si c'est toujours le même compte connecté (pas d'écrasement d'une nouvelle session).
- Boutons : `core/layout/main-layout` (zone du compte dans la barre latérale, libellé `layout.logout`) et `features/deliveries/pages/driver-profile-page` (espace livreur). Les deux redirigent vers `/login`.
- À la déconnexion, la file hors ligne conserve les saisies de chaque compte sans les envoyer sous un autre compte : voir [SYS-02](../SYS-02/guide.md) et [SYS-11](../SYS-11/description.md).

## Règles métier à respecter par les autres tickets

- Toute nouvelle coquille (autre espace que `main-layout` ou `driver-layout`) doit offrir un bouton de déconnexion qui appelle `AuthService.logout()` puis navigue vers `/login`.
- Ne jamais vider le jeton à la main : passer par `AuthService.logout()` ou `AuthStore.signOut()`.

## Fichiers concernés

**Backend**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/auth/controller/AuthController.java` (méthode `logout`)
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/auth/controller/RefreshCookieFactory.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/security/service/impl/AuthServiceImpl.java` (méthode `logout`)
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/security/config/OriginCheckFilter.java`, `SecurityConfig.java` (`PUBLIC_ROUTES`)

**Frontend**
- `valimbavaka-ui/src/app/core/auth/auth-service.ts`, `auth-store.ts`
- `valimbavaka-ui/src/app/core/layout/main-layout/main-layout.ts`, `main-layout.html`
- `valimbavaka-ui/src/app/features/deliveries/pages/driver-profile-page/driver-profile-page.ts`, `driver-profile-page.html`

**Tests**
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/auth/controller/AuthSecurityIT.java` (`refresh_afterLogout_returns401`)
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/security/service/impl/AuthServiceImplTest.java` (`logout_knownToken_revokesItsFamily`)
- `valimbavaka-ui/src/app/core/auth/auth-service.spec.ts` (« should clear the session on logout even if the server call fails »)
- `valimbavaka-ui/src/app/core/layout/main-layout/main-layout.spec.ts` (« should display the name and the role label of the signed in user with a logout button »)

## Dépendances

**Ce ticket s'appuie sur** :
- [AUTH-01](../AUTH-01/description.md) · [guide](../AUTH-01/guide.md) : jeton, cookie `vvs_refresh`, store d'authentification

**Ces tickets s'appuient sur lui** : aucun.

## Comment l'utiliser depuis un autre ticket

```ts
private readonly authService = inject(AuthService);
private readonly router = inject(Router);

protected logout(): void {
  this.authService.logout().subscribe(() => void this.router.navigate(['/login']));
}
```

## Points d'attention

- Le critère du ticket parle d'un bouton « dans la barre supérieure ». Dans le code, le bouton est dans la zone du compte de la barre latérale (`main-layout.html`) ; `top-bar` ne le contient pas. À confirmer avec le ticket et la maquette.
- Le jeton d'accès (15 min) n'est pas invalidé par la déconnexion : seul le refresh token l'est.
- Le cookie n'est effacé que si la réponse atteint le navigateur ; en cas d'erreur réseau, l'interface se déconnecte quand même localement.
