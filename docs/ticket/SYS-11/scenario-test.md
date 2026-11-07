# Scénarios de test — SYS-11 · Session expirée avec des saisies en attente

Statut : ✅ rédigé le 2026-11-08 · Ticket : [#137](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/137) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests frontend Vitest sur le comportement visible. Ticket frontend seul (file hors ligne, intercepteur de session, page de connexion), aucun endpoint ni migration.

## Règle retenue

| Situation au retour du réseau | Comportement |
|---|---|
| Jeton d'accès expiré (15 min), cookie de refresh valide | 401 → un seul `POST /api/v1/auth/refresh` → rejeu avec le nouveau jeton et la **même** `Idempotency-Key` |
| Refresh **refusé** par le serveur (401, 403, cookie d'un autre compte) | session fermée, redirection `/login?session=expired` avec le message « Votre session a expiré… » ; la saisie reste `PENDING` (tentatives inchangées) et part après la reconnexion du **même** compte |
| Refresh **impossible** (réseau 0, 408, 429, 5xx) | session conservée, pas de redirection ; l'essai compte comme erreur transitoire et la relance suivante retente le refresh |
| Autre compte connecté sur le poste | ne voit pas les saisies du compte expiré et ne les envoie jamais |

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `SELLER` « Hanta » (`0195f0a0-…-0001`) créatrice des saisies ; `ADMIN` (`0195f0a0-…-0002`) second compte sur le même poste |
| Données de départ | file vide ; jeton d'accès `jeton-expire` ; saisie faite hors ligne : `POST /api/v1/suppliers` `{ "name": "Huiles Ravo" }` |
| Tickets à avoir déployés avant | SYS-02 (file hors ligne), AUTH-01 (connexion, refresh), SYS-09 et SYS-10 (base de la branche, PR empilée) |

## Scénarios issus des critères d'acceptation

| # | Critère | Exemple (Étant donné / Quand / Alors) | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Ni perdues ni envoyées sans authentification | Hanta saisit le fournisseur hors ligne, son jeton expire, le réseau revient, le refresh répond 401 | aucun nouvel envoi de `/api/v1/suppliers` ; la saisie reste `PENDING`, `attempts: 0` ; session fermée | `outbox-session.spec.ts` « should keep the entry and invite to sign in again when the refresh is refused, then send it after the same account signs in » | ☐ |
| 1b | Envoi sans session refusé | `submit` sans compte connecté | 401 `UNAUTHENTICATED`, rien en file | `outbox-service.spec.ts` « should reject a submission without an authenticated account » | ☐ |
| 2 | Session renouvelée automatiquement puis synchronisation | réseau revenu, `/suppliers` répond 401, refresh renvoie `jeton-neuf` | rejeu avec `Bearer jeton-neuf` et la même `Idempotency-Key` ; file vide ; pas de redirection | `outbox-session.spec.ts` « should renew the expired session on reconnection and send the queued entry with the new token » | ☐ |
| 2b | Invitation à se reconnecter puis synchronisation | refresh refusé (401) | redirection `/login` avec `session=expired` ; après connexion de Hanta, envoi avec `Bearer jeton-reconnexion` et la même clé | `outbox-session.spec.ts` « should keep the entry and invite to sign in again when the refresh is refused, then send it after the same account signs in » ; `session-interceptor.spec.ts` « should sign out and go to the login page when the refresh fails » | ☐ |
| 2c | Message sur la page de connexion | `/login?session=expired` | encadré `role="status"` : « Votre session a expiré. Reconnectez-vous avec le même compte : vos saisies en attente sont conservées… » | `login-page.spec.ts` « should invite the user to sign in again and say the pending entries are kept » | ☐ |
| 2d | Pas de message sur une visite normale | `/login` | aucun encadré `role="status"` | `login-page.spec.ts` « should not show the expired session message on a normal visit » | ☐ |
| 3 | Seul le compte créateur envoie | session de Hanta refusée, l'administrateur se connecte sur le poste | 0 saisie en attente ou refusée affichée pour lui ; sa propre saisie `/api/v1/articles` part avec `Bearer jeton-b` ; `/api/v1/suppliers` n'est pas envoyé ; il part au retour de Hanta avec son jeton | `outbox-session.spec.ts` « should not send the expired account entries when another account signs in on the same device » | ☐ |
| 3b | Idem, sans expiration | saisie hors ligne de Hanta, connexion d'un autre compte | non envoyée tant que Hanta n'est pas reconnectée | `outbox-service.spec.ts` « should not send one account’s queued requests while another account is connected » | ☐ |

## Sécurité et erreurs

Aucun endpoint écrit : 403/404/409/422 et mass assignment relèvent des tickets propriétaires des endpoints. Comportement de la session :

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Refresh refusé | `/refresh` → 401 `UNAUTHENTICATED` | déconnexion + `/login?session=expired`, erreur `ApiError` relancée | `session-interceptor.spec.ts` « should sign out and go to the login page when the refresh fails » | ☐ |
| Réseau coupé pendant le refresh | `/refresh` → statut 0 | session conservée, pas de redirection, erreur `ApiError` 0 | `session-interceptor.spec.ts` « should keep the session when the refresh fails because the network is down » | ☐ |
| Serveur indisponible pendant le refresh | `/refresh` → 503 | jeton conservé (`ancien-jeton`), pas de redirection | `session-interceptor.spec.ts` « should keep the session when the server is unavailable during the refresh » | ☐ |
| 403 sur un appel métier | `/api/v1/users` → 403 | ni refresh ni déconnexion | `session-interceptor.spec.ts` « should not try to refresh after a 403 » | ☐ |
| Refresh d'un autre compte | une autre personne s'est connectée pendant le refresh | la requête n'est pas rejouée sous le nouveau compte | `session-interceptor.spec.ts` « should not refresh a request after another account has signed in » | ☐ |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Réseau qui retombe pendant le refresh | `/refresh` → statut 0 puis réseau stable 1 s plus tard | saisie `PENDING`, `attempts: 1`, session gardée ; à la relance : 401 → refresh `jeton-neuf` → envoi, file vide | `outbox-session.spec.ts` « should keep the session and retry later when the network drops again during the refresh » | ☐ |
| Rejeu après reconnexion | même saisie envoyée deux fois (avant et après expiration) | même `Idempotency-Key` : aucun doublon côté serveur (SYS-02/SYS-12) | `outbox-session.spec.ts` (assertions `Idempotency-Key`) | ☐ |
| Déconnexion puis reconnexion du même compte | saisie hors ligne, `signOut`, `signIn` | saisie retrouvée puis envoyée | `outbox-service.spec.ts` « should retain queued requests until their creating account reconnects » | ☐ |
| Rechargement de la page hors ligne | jeton perdu (mémoire), refresh impossible | page de connexion ; saisies conservées dans IndexedDB, envoyées après connexion du même compte au retour du réseau | à vérifier en recette (R2) | 🧪 |
| Compte désactivé entre-temps | refresh refusé (famille révoquée, AUTH-01) | même chemin que « refresh refusé » : saisies conservées, jamais envoyées par un autre compte | couvert par la ligne 1 (refus 401) | ☐ |

## Tests existants

- `valimbavaka-ui/src/app/core/offline/outbox-session.spec.ts` (nouveau, chaîne d'intercepteurs réelle)
- `valimbavaka-ui/src/app/core/http/session-interceptor.spec.ts` (2 tests ajoutés, 1 adapté au paramètre `session=expired`)
- `valimbavaka-ui/src/app/features/auth/pages/login-page/login-page.spec.ts` (2 tests ajoutés) et `login-page.a11y.spec.ts` (routeur fourni)
- `valimbavaka-ui/src/app/core/offline/outbox-service.spec.ts` (SYS-02, non modifié : doit rester vert)

## Recette manuelle (🧪)

| # | Étapes | Résultat attendu | Statut |
|---|---|---|---|
| R1 | Se connecter (vendeuse), couper le Wi-Fi, modifier une fiche fournisseur, attendre 16 min, rétablir le Wi-Fi | la fiche part sans reconnexion (refresh automatique), compteur « en attente » à 0 | 🧪 |
| R2 | Comme R1, mais recharger la page hors ligne avant de rétablir le réseau | page de connexion ; après connexion avec le même compte, la fiche part | 🧪 |
| R3 | Comme R1, mais se déconnecter puis se connecter avec un autre compte sur le même poste | l'autre compte ne voit pas la saisie sur `/synchronisation` ; elle part au retour du premier compte | 🧪 |

## Résultat de l'exécution

- Date : à lancer
- Environnement : à lancer (CI ou local)
- Anomalies ouvertes : aucune connue
- Étapes 🧪 restantes : 3 (R1 à R3)
