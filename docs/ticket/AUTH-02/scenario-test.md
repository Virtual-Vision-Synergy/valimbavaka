# Scénarios de test — AUTH-02 · Déconnexion utilisateur

Statut : ✅ exécuté le 2026-10-23 — reste la recette manuelle (🧪) · Ticket : [#11](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/11) · [description](description.md) · [guide](guide.md)

Légende de la colonne « Statut » : ✅ auto = test automatique vert · ✅ base = vérifié en base après migrations · 🔍 code = vérifié par lecture du code (aucun test) · ❌ écart = comportement différent de l'attendu · 🧪 recette = à faire à la main dans le navigateur. Détail : [CONSIGNE.md](../CONSIGNE.md#4-remplir-le-scénario-de-test).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | tout rôle connecté (`ADMIN`, `SELLER`, `DELIVERY_DRIVER`) |
| Données de départ | un compte actif connecté (cookie `vvs_refresh` présent) |
| Tickets à avoir déployés avant | AUTH-01 |

## Scénarios issus des critères d'acceptation

| # | Critère | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Bouton de déconnexion visible en permanence | Étant donné un utilisateur connecté · Quand la coquille s'affiche | nom, libellé du rôle et bouton de déconnexion visibles | `main-layout.spec.ts` « should display the name and the role label of the signed in user with a logout button » | ✅ auto |
| 1b | … dans la **barre supérieure** | Quand on regarde la barre supérieure | bouton « Déconnexion » présent (y compris sur mobile, où la barre latérale est repliée) ; un clic ferme la session et ramène à `/login` | `main-layout.spec.ts` « should keep a logout button in the top bar that ends the session » | ✅ auto (corrigé le 2026-10-23) |
| 2 | Retour au login et nouvelle authentification exigée | Quand l'utilisateur clique sur « Se déconnecter » | session vidée, redirection `/login` | `auth-service.spec.ts` « should clear the session on logout even if the server call fails » ; `auth-store.spec.ts` « should forget the user and the token on sign out » | ✅ auto |
| 2b | idem (URL protégée ensuite) | Quand il rouvre `/dashboard` | renvoyé au login | `auth-guard.spec.ts` « authGuard should redirect an anonymous visitor to the login page » | ✅ auto |
| 2c | idem (serveur) | Quand le cookie de la session fermée est rejoué sur `/refresh` | 401, aucune nouvelle session | `AuthSecurityIT.refresh_afterLogout_returns401` ; `AuthServiceImplTest.logout_knownToken_revokesItsFamily` | ✅ auto |
| 2d | Aucun changement de rôle sans reconnexion | Quand un autre compte se connecte ensuite | menu et permissions du nouveau compte uniquement | `session-interceptor.spec.ts` « should not replace a new account when an earlier refresh completes » ; `auth-store.spec.ts` « should grant exactly the permissions of the signed in role » | ✅ auto |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Déconnexion sans cookie | `POST /api/v1/auth/logout` sans `vvs_refresh` | 204 et cookie effacé (`Max-Age=0`) | `AuthSecurityIT.logout_withoutCookie_returns204AndClearsTheCookie` | ✅ auto |
| Déconnexion depuis une origine non autorisée | `Origin: https://evil.example` | 403 `FORBIDDEN`, la session n'est pas fermée | `AuthSecurityIT.logout_fromForeignOrigin_returns403` | ✅ auto |
| Serveur injoignable pendant la déconnexion | réseau coupé | l'interface se déconnecte quand même | `auth-service.spec.ts` « should clear the session on logout even if the server call fails » | ✅ auto |
| Un autre compte se connecte pendant l'appel de déconnexion | A clique « Se déconnecter », B se connecte avant la réponse | la session de B n'est pas effacée | `AuthService.logout` : `finalize` ne vide la session que si l'utilisateur courant est toujours A | 🔍 code |
| Jeton de refresh d'une autre famille | cookie d'une session déjà révoquée | 204, aucune autre session touchée | `AuthServiceImpl.logout` : révoque uniquement la famille du jeton trouvé | 🔍 code |

## Cas limites

| Cas | Exemple | Résultat attendu | Comment c'est vérifié | Statut |
|---|---|---|---|---|
| Jeton d'accès encore valide après déconnexion | jeton copié avant le clic, rejoué 5 min après | accepté jusqu'à son expiration (15 min max) : le JWT n'est pas révoqué, seul le refresh l'est | `AuthServiceImpl.logout` ne révoque que la famille de refresh | 🔍 code |
| Saisies en attente dans la file hors ligne | 2 saisies `PENDING` puis déconnexion | conservées pour leur compte, jamais envoyées sous un autre | `outbox-service.spec.ts` « should retain queued requests until their creating account reconnects », « should not send one account’s queued requests while another account is connected » | ✅ auto |
| Double clic sur « Se déconnecter » | 2 appels rapprochés | 2 réponses 204, aucune erreur | logout idempotent (famille déjà révoquée → rien) | 🔍 code |
| Déconnexion de l'espace livreur | profil livreur sur `/round` | retour `/login` | pas de test automatique | 🧪 recette |

## Tests existants

- Backend : `feature/auth/controller/AuthSecurityIT.java` (`refresh_afterLogout_returns401`), `security/service/impl/AuthServiceImplTest.java` (`logout_knownToken_revokesItsFamily`)
- Frontend : `core/auth/auth-service.spec.ts`, `auth-store.spec.ts`, `auth-guard.spec.ts`, `core/layout/main-layout/main-layout.spec.ts`, `core/http/session-interceptor.spec.ts`, `core/offline/outbox-service.spec.ts`

## Recette manuelle (🧪)

| Étape | Attendu | Statut |
|---|---|---|
| Se connecter, cliquer « Se déconnecter » | arrivée sur `/login` | 🧪 recette |
| Bouton Précédent du navigateur | l'application protégée ne se réaffiche pas | 🧪 recette |
| Se reconnecter avec un autre rôle sur le même poste | menu du nouveau rôle uniquement | 🧪 recette |
| Livreur : se déconnecter depuis le profil de `/round` | retour `/login` | 🧪 recette |

## Résultat de l'exécution

- **Date** : 2026-10-23.
- **Backend** : CI GitHub `mvn verify` vert sur un code identique à `develop` (voir AUTH-01 pour le détail).
- **Frontend** : `ng test` local, 102 tests verts sur 102.
- **Anomalies** :
  1. ✅ Corrigé le 2026-10-23 (branche `fix/AUTH-02-deconnexion-barre-superieure`) : bouton de déconnexion ajouté dans la barre supérieure ; celui de la zone compte de la barre latérale reste.
  2. ✅ Logout sans cookie et contrôle d'origine sur `/logout` sont maintenant testés ; seul le `finalize` lors d'un changement de compte reste vérifié par lecture du code.
- **Recette manuelle** : 4 étapes 🧪.
