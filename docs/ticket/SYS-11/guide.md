# Guide d'intégration — SYS-11 · Session expirée avec des saisies en attente

Statut : ✅ écrit le 2026-11-08 — tests exécutés en CI (voir « Résultat de l'exécution » du scénario) · Ticket : [#137](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/137) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Une saisie faite hors ligne n'est jamais perdue ni envoyée sans session quand le jeton expire pendant la coupure. Au retour du réseau, le 401 déclenche un refresh puis le rejeu avec la même `Idempotency-Key` ; si le serveur refuse le refresh, l'utilisateur est renvoyé vers `/login?session=expired` (message dédié) et la saisie part dès que **le même compte** se reconnecte. Nouveauté de SYS-11 : un refresh qui échoue faute de réseau ou de serveur ne ferme plus la session. Ticket frontend seul.

## Contrat exposé

### Endpoints backend

Aucun. Aucune migration Liquibase, aucune ligne de journal. S'appuie sur `POST /api/v1/auth/refresh` ([AUTH-01](../AUTH-01/guide.md)) et sur l'idempotence serveur ([SYS-02](../SYS-02/guide.md)).

### Frontend

- `core/http/session-interceptor.ts` :
  - `SESSION_EXPIRED_QUERY_PARAMS` (`session-interceptor.ts:15`) : `{ session: 'expired' }`, ajouté à `/login` quand la session est fermée ;
  - `handleRefreshFailure` (`:98`) : déconnexion et redirection seulement si le refresh est **refusé** (401, 403, autre compte) ; `isTransientRefreshFailure` (`:108`) garde la session pour un statut 0, 408, 429 ou 5xx et relance l'erreur à l'appelant.
- `features/auth/pages/login-page` : propriété `sessionExpired` (`login-page.ts:39`), lu dans les paramètres de la route ; encadré `alert-info` `role="status"` (`login-page.html:51`), libellé `auth.login.sessionExpired` (`public/i18n/fr.json`, `mg.json`).
- `OutboxService` (`core/offline/outbox-service.ts`) : **inchangé**. Le comportement attendu par SYS-11 vient de `canSend` (`:279`, compte créateur connecté et réseau présent) et `isActive` (`:275`) : une réponse reçue après la fermeture de session n'est pas comptée comme un échec, la saisie reste `PENDING` avec ses tentatives.

### Données

Aucune. Les saisies restent dans IndexedDB (`vvs-outbox`) avec leur `ownerId` (SYS-02).

## Règles métier à respecter par les autres tickets

1. Une écriture passe par `OutboxService.submit` : c'est ce qui garantit qu'elle survit à une session expirée. Une écriture faite en `HttpClient` direct est perdue si le refresh est refusé.
2. Ne pas vider la file (`clear()`) à la déconnexion ni à l'expiration de session : les saisies attendent le retour de leur compte.
3. Un écran qui redirige vers `/login` pour une session expirée utilise `SESSION_EXPIRED_QUERY_PARAMS` pour afficher le même message.
4. Un refresh en échec réseau n'est pas une expiration : ne pas déconnecter l'utilisateur sur un statut 0 ou 5xx.

## Fichiers concernés

**Frontend**
- `valimbavaka-ui/src/app/core/http/session-interceptor.ts`
- `valimbavaka-ui/src/app/features/auth/pages/login-page/login-page.ts`, `login-page.html`
- `valimbavaka-ui/public/i18n/fr.json`, `mg.json` (`auth.login.sessionExpired`)

**Tests**
- `valimbavaka-ui/src/app/core/offline/outbox-session.spec.ts` (nouveau, intercepteurs réels `authInterceptor` → `sessionInterceptor` → `errorInterceptor`)
- `valimbavaka-ui/src/app/core/http/session-interceptor.spec.ts`
- `valimbavaka-ui/src/app/features/auth/pages/login-page/login-page.spec.ts`, `login-page.a11y.spec.ts`

**Docs** : `docs/adr/0001-synchronisation-hors-ligne.md` (section « session expirée avec des saisies en attente (SYS-11) »).

## Dépendances

**Ce ticket s'appuie sur** :
- [SYS-02](../SYS-02/description.md) · [guide](../SYS-02/guide.md) (file hors ligne, `ownerId`, `Idempotency-Key`)
- [AUTH-01](../AUTH-01/description.md) · [guide](../AUTH-01/guide.md) (refresh, intercepteur de session)
- [SYS-10](../SYS-10/description.md) · [guide](../SYS-10/guide.md) (base de la branche : moteur de la file) — PR empilée

**Ces tickets s'appuient sur lui** : _(aucun)_

## Comment l'utiliser depuis un autre ticket

```ts
import { SESSION_EXPIRED_QUERY_PARAMS } from '../../core/http/session-interceptor';

void this.router.navigate(['/login'], { queryParams: SESSION_EXPIRED_QUERY_PARAMS });
```

## Points d'attention

- Hypothèse : « refresh impossible » = statut 0, 408, 429 ou ≥ 500 ; tout autre échec (401, 403, cookie d'un autre compte) vaut session expirée. Avant SYS-11, toute erreur de refresh déconnectait l'utilisateur, y compris une coupure réseau au mauvais moment.
- Le message de la page de connexion ne donne pas le nombre de saisies conservées : sans session, on ne sait pas encore quel compte va se reconnecter. Le compteur réapparaît dans `vvs-sync-status` après la connexion.
- Rechargement de la page pendant la coupure : le jeton (en mémoire) est perdu et le refresh est impossible hors ligne, donc la page de connexion s'affiche sans pouvoir se connecter avant le retour du réseau ; les saisies restent dans IndexedDB. Recette R2.
- Après une reconnexion, l'utilisateur arrive sur l'accueil (`/`), pas sur la page qu'il quittait : pas de `returnUrl` aujourd'hui (hors périmètre).
- Un refresh en échec réseau compte comme une tentative transitoire de la saisie : après 5 échecs (≈ 15 s) elle passe `FAILED` avec `networkFailure` et retient les opérations suivantes (règle SYS-10) jusqu'à « Renvoyer ».
- PR empilée sur SYS-10 (`feature/SYS-10-ordre-synchronisation-blocage`) : à merger après elle.
