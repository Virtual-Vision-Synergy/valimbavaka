# Guide d'intégration — SYS-09 · Suivi et gestion des saisies en échec

Statut : ✅ écrit le 2026-11-08 — **tests non exécutés** (voir « Résultat de l'exécution » du scénario) · Ticket : [#135](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/135) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

La page `/synchronisation` montre à chaque utilisateur ses saisies **en attente** (lecture seule, dans l'ordre de la file, avec la dernière erreur transitoire) et ses saisies **refusées** (date, action, adresse, motif du serveur). Une saisie refusée se renvoie (« Renvoyer ») ou s'abandonne après une boîte de confirmation. L'indicateur global `vvs-sync-status` de la barre supérieure (livré par [SYS-02](../SYS-02/guide.md)) donne les deux compteurs et mène à cette page. Ticket frontend seul.

## Contrat exposé

### Endpoints backend

Aucun. Les saisies rejouées appellent les endpoints de leur ticket, avec l'idempotence de [SYS-12](../SYS-12/guide.md).

### Frontend

- Route : `/synchronisation` (`app.routes.ts`, `features/sync/sync.routes.ts`), sans permission : chaque compte ne voit que ses saisies (`ownerId`).
- `OutboxService` (`core/offline/outbox-service.ts`) :
  - **nouveau** signal `pending` (`outbox-service.ts:50`) : saisies `PENDING` du compte courant, dans l'ordre de la file ; `pendingCount` en est dérivé ;
  - `retry(id)` (`outbox-service.ts:118`) : ne s'applique plus qu'à une saisie `FAILED` du compte ; elle repasse `PENDING` (tentatives à 0, motif effacé) et **l'état est rechargé avant le rejeu**, donc visible tout de suite même hors ligne ou pendant une synchronisation ;
  - `discard(id)` : inchangé (suppression définitive) ; la confirmation est à la charge de l'écran.
- Page `SyncPage` (`features/sync/pages/sync-page/sync-page.ts`) : `retry` (toast `sync.page.retried` ou `sync.page.retriedOffline`), `askDiscard` / `confirmDiscard` / `cancelDiscard` via `vvs-confirm-dialog` (bouton rouge), toast `sync.page.discarded`.
- Libellés : `sync.method.POST|PUT|PATCH|DELETE` (« Création », « Modification », « Mise à jour », « Suppression ») et `sync.page.*` dans `public/i18n/fr.json` et `mg.json`.

### Données

Aucune table ni migration. Les saisies restent dans IndexedDB (`vvs-outbox`, SYS-02). Pas de ligne de journal : rien n'est écrit côté serveur par la page elle-même.

## Règles métier à respecter par les autres tickets

1. Un abandon est **définitif** : la saisie n'est jamais envoyée ; toujours passer par une confirmation si un autre écran propose `discard`.
2. Seule une saisie `FAILED` se renvoie ; une saisie `PENDING` est gérée par le moteur (relances 1, 2, 4, 8 s).
3. Le motif affiché est le `detail` du `ProblemDetail` renvoyé par le serveur : chaque endpoint doit donc renvoyer un `detail` compréhensible par l'utilisateur (ex. « La fiche a été modifiée par un autre utilisateur. »).
4. Ne pas modifier l'ordre de la file depuis l'écran : c'est l'objet de [SYS-10](../SYS-10/description.md).

## Fichiers concernés

**Frontend** (`valimbavaka-ui/`)
- `src/app/core/offline/outbox-service.ts` (`pending`, `retry`)
- `src/app/features/sync/pages/sync-page/sync-page.{ts,html,css}`
- `public/i18n/fr.json`, `public/i18n/mg.json` (`sync.page.*`, `sync.method.*`)
- Inchangés mais concernés : `src/app/core/offline/sync-status.{ts,html}` (indicateur), `src/app/shared/components/confirm-dialog/*`

**Tests**
- `src/app/features/sync/pages/sync-page/sync-page.spec.ts`
- `src/app/core/offline/outbox-service.spec.ts` (« should list the account pending entries in queue order », « should show a retried entry as pending at once even when offline »)
- `src/app/core/offline/sync-status.spec.ts` (SYS-02)

## Dépendances

**Ce ticket s'appuie sur** :
- [SYS-02](../SYS-02/description.md) · [guide](../SYS-02/guide.md) (moteur, indicateur)
- [SYS-07](../SYS-07/description.md) · [guide](../SYS-07/guide.md) (formulaires branchés sur la file)

**Ces tickets s'appuient sur lui** : aucun. Liés : [SYS-10](../SYS-10/description.md) (ordre), [SYS-11](../SYS-11/description.md) (session expirée).

## Comment l'utiliser depuis un autre ticket

```ts
// afficher un rappel dans un écran métier (ex. tournée du livreur)
private readonly outbox = inject(OutboxService);
protected readonly pending = this.outbox.pending;      // saisies à envoyer, ordre de la file
protected readonly failedCount = this.outbox.failedCount;
// lien vers le suivi : <a routerLink="/synchronisation">…</a>
```

## Points d'attention

- Une saisie renvoyée garde son `createdAt` : elle reprend sa place d'origine dans la file, devant des saisies plus récentes. Ce comportement est celui de SYS-02 et n'est pas modifié ; la décision appartient à [SYS-10](../SYS-10/description.md) (needs-info).
- Une modification refusée en 409 (version périmée) échouera de nouveau si on la renvoie telle quelle : l'utilisateur doit l'abandonner et ressaisir sur la fiche à jour. Aucune édition de la saisie depuis la page (hypothèse : hors périmètre, ressaisie plus sûre).
- L'action est affichée par sa méthode HTTP et son adresse (`PUT /api/v1/suppliers/…`) : il n'existe pas de libellé métier par saisie. Un libellé (« Fiche fournisseur Huiles Ravo ») demanderait de le stocker dans `OutboxEntry` à la saisie : à décider si les utilisateurs le réclament.
- `retry` ignore désormais une entrée `PENDING` (auparavant elle remettait son compteur à zéro) : aucun appelant ne le faisait.
- Persistance après rechargement et navigation clavier : recette R1 à R3 du scénario.
- Tests non exécutés par l'auteur : voir le scénario.
