# Guide d'intégration — SYS-02 · Sauvegarde automatique hors-ligne

Statut : ✅ complet au 2026-10-23 — vérifié sur le code, les tests (CI + exécution locale) et la base · Ticket : [#16](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/16) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Toute écriture (POST, PUT, PATCH, DELETE) faite depuis l'interface passe par `OutboxService.submit(...)`. Hors connexion, la saisie est stockée dans IndexedDB puis rejouée dans l'ordre au retour du réseau. Chaque saisie porte un UUID envoyé en en-tête `Idempotency-Key` : côté serveur, un rejeu ne crée jamais de doublon. Décision d'architecture : [docs/adr/0001-synchronisation-hors-ligne.md](../../adr/0001-synchronisation-hors-ligne.md).

## Contrat exposé

### Frontend — `core/offline`

```ts
// OutboxService (providedIn: 'root')
submit(method: 'POST'|'PUT'|'PATCH'|'DELETE', url: string, body?: unknown): Observable<{ queued: boolean }>
flush(): Promise<void>            // rejoue la file (appelé automatiquement)
retry(id: string): Promise<void>  // renvoie une saisie FAILED (compteur remis à zéro)
discard(id: string): Promise<void>
clear(): Promise<void>
// signaux : pendingCount, failedCount, failed, isSyncing
```

- `submit` renvoie `{ queued: false }` si la requête est partie normalement, `{ queued: true }` si elle est en file (réseau coupé, erreur transitoire, ou saisies plus anciennes encore en attente : l'ordre est préservé).
- **Refus métier** (400, 404, 409, 422…) : l'erreur est relancée à l'appelant et rien n'est mis en file lors de l'envoi direct ; lors d'un rejeu, l'entrée passe en `FAILED` sans bloquer les suivantes.
- **Erreurs transitoires** : statut 0 (réseau), 401, 408, 429 (et 5xx selon l'ADR) ; 5 tentatives par cycle, délais 1, 2, 4, 8 s ; puis `FAILED`.
- Sans session active, `submit` échoue en 401 `UNAUTHENTICATED`. Chaque entrée est rattachée à son compte (`ownerId`) et ne part jamais sous un autre compte.
- `ConnectivityService` : signal `isOnline` (événements `online` / `offline` du navigateur). `OutboxStorage` : IndexedDB `vvs-outbox` (repli en mémoire sans IndexedDB), jamais `localStorage`.
- `OutboxService` est instancié au démarrage (`provideAppInitializer` dans `app.config.ts`) ; le rejeu se déclenche à la connexion, au retour du réseau et au démarrage.
- Interface : composant `vvs-sync-status` dans la barre supérieure (en ligne / hors ligne, nombre de saisies en attente et refusées) ; page `/synchronisation` (`features/sync`) listant les saisies refusées avec « Renvoyer » et « Abandonner ».
- `core/http/session-interceptor.ts` : sur 401, un seul refresh puis rejeu de la requête ; c'est ce qui permet à une saisie rejouée avec un jeton expiré d'aboutir.

### Backend — `security/idempotency`

| Élément | Comportement |
|---|---|
| En-tête | `Idempotency-Key: <UUID>` (autorisé en CORS dans `WebConfig`) |
| Méthodes | POST, PUT, PATCH, DELETE ; sans l'en-tête, la requête est traitée normalement |
| Première requête | clé réservée par un INSERT atomique, réponse mémorisée si statut < 500 et différent de 401 / 403 |
| Rejeu identique | même statut et même corps, avec `Idempotency-Replayed: true` ; aucune nouvelle écriture |
| Clé non UUID | 400 `MALFORMED_REQUEST` |
| Même clé, autre méthode ou autre chemin | 422 `VALIDATION_FAILED` |
| Même clé pendant le traitement | 409 `CONFLICT` « Requête en cours de traitement. » |
| Sans jeton d'accès | le filtre ne fait rien, l'autorisation répond 401 |
| 5xx, 401, 403 | clé libérée : le client peut réessayer |
| Portée et durée | clé propre à chaque utilisateur ; conservation 24 h, purge à chaque réservation |

- Classes : `IdempotencyFilter` (inséré dans la chaîne de `SecurityConfig` après l'authentification) et `IdempotencyStore` (`JdbcClient`).
- Table `idempotency_keys` (clé primaire `(user_id, idempotency_key)`) : migration `2026_10_20-01-changelog.sql`.
- L'en-tête `Location` d'une création n'est pas rejoué (seuls statut et corps le sont).

## Règles pour les autres tickets

- **Aucune écriture métier directe avec `HttpClient`** : passer par `OutboxService.submit` pour bénéficier de la file et de l'idempotence. Les lectures restent en `HttpClient`.
- Un formulaire doit gérer `queued: true` (message « enregistré, sera synchronisé ») et ne pas s'attendre à recevoir l'entité créée.
- Tout formulaire hors ligne qui crée une ressource doit prévoir qu'il n'aura pas son identifiant tout de suite (identifiant côté serveur, voir [SYS-06](../SYS-06/guide.md)).
- Côté serveur, rien à faire : le filtre est global, mais les contrôleurs d'écriture doivent rester sans effet de bord hors transaction pour que le rejeu soit sûr.
- Les opérations d'annulation et de contre-passation ([JNL-02](../JNL-02/guide.md)) sont rejouables par la même mécanique.

## Fichiers concernés

**Frontend** (`valimbavaka-ui/src/app/`)
- `core/offline/outbox-service.ts`, `outbox-models.ts`, `outbox-storage.ts`, `connectivity-service.ts`
- `core/offline/sync-status.{ts,html,css}` (affiché par `core/layout/top-bar`)
- `features/sync/sync.routes.ts`, `features/sync/pages/sync-page/*`
- `core/http/session-interceptor.ts`, `app.config.ts` (initialisation), `app.routes.ts` (route `synchronisation`)
- Premier consommateur visible : `features/deliveries/pages/driver-round-page` (affiche `pendingCount`)

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/`)
- `security/idempotency/IdempotencyFilter.java`, `IdempotencyStore.java`, `package-info.java`
- `security/config/SecurityConfig.java` (enregistrement du filtre), `core/config/WebConfig.java` (CORS)
- `valimbavaka-core/src/main/resources/db/changelog/changes/2026_10_20-01-changelog.sql`

**Documentation** : `docs/adr/0001-synchronisation-hors-ligne.md`

**Tests**
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/security/idempotency/IdempotencyIT.java`
- `valimbavaka-ui/src/app/core/offline/*.spec.ts`, `features/sync/pages/sync-page/sync-page.spec.ts`, `core/http/session-interceptor.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** : [AUTH-01](../AUTH-01/guide.md) (session, jeton, refresh).

**Ces tickets s'appuient sur lui** : [LIV-08](../LIV-08/description.md), [SYS-07](../SYS-07/description.md), [SYS-08](../SYS-08/description.md), [SYS-09](../SYS-09/description.md), [SYS-10](../SYS-10/description.md), [SYS-11](../SYS-11/description.md), [SYS-12](../SYS-12/description.md).

## Comment l'utiliser depuis un autre ticket

```ts
private readonly outbox = inject(OutboxService);

save(): void {
  this.outbox.submit('POST', '/api/v1/suppliers', this.form.getRawValue()).subscribe({
    next: ({ queued }) => this.notify(queued ? 'sync.queued' : 'common.saved'),
    error: (error: ApiError) => this.showServerMessage(error), // refus métier : 422, 409…
  });
}
```

## Points d'attention

- **Écart doc / code** : l'ADR dit que la file est vidée à la déconnexion, mais le code conserve les saisies de chaque compte (`ownerId`) et ne les rejoue qu'au retour de ce compte ; seul `clear()` vide la file. À aligner dans l'ADR.
- Plusieurs tickets sont déjà prévus sur ce socle : l'ordre de synchronisation en cas de blocage ([SYS-10](../SYS-10/description.md)), la session expirée avec des saisies en attente ([SYS-11](../SYS-11/description.md)) et la reprise automatique ([SYS-08](../SYS-08/description.md)) sont ouverts ; une partie est déjà couverte par le code (relances, FIFO, 401) mais n'est pas validée par ces tickets.
- Aucun formulaire métier ne passe encore par `submit` hors maquette : le formulaire fournisseur (`features/supplier`) utilise `HttpClient` directement, c'est l'objet de [SYS-07](../SYS-07/description.md).
- Sans service worker, rien ne se synchronise application fermée : la file est rejouée à la réouverture. Hors périmètre de l'ADR : PWA et résolution de conflits métier.
- Un 5xx n'est pas mémorisé côté serveur : si l'écriture a réellement abouti avant l'erreur, le rejeu peut la refaire. Les services doivent donc rester idempotents sur leurs contraintes d'unicité (ex. 409 sur un nom déjà pris).
