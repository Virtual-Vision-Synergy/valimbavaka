# Guide d'intégration — SYS-10 · Règle d'ordre de synchronisation en cas de blocage

Statut : ✅ écrit le 2026-11-08 — tests exécutés en CI (voir « Résultat de l'exécution » du scénario) · Ticket : [#136](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/136) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

**Décision PO du 07/11/2026 (Randy) : « ordre strict par type ».** Le moteur de la file hors ligne ne bloque plus toute la file derrière une saisie en erreur réseau : les opérations métier (journal, ventes, factures, stock…) restent en ordre strict et attendent derrière une saisie bloquée de leur groupe ; les fiches et référentiels (fournisseurs, articles, utilisateurs, promotions, catalogues…) passent devant. Le classement se fait par préfixe d'adresse dans une table unique, à compléter par chaque ticket qui ajoute une écriture. Ticket frontend seul.

## Contrat exposé

### Endpoints backend

Aucun. Aucune migration Liquibase.

### Frontend

- `core/offline/sync-order.ts` :
  - `SYNC_ORDER_RULES` (`sync-order.ts:35`) : table `{ prefix, kind }`, `kind` = `ORDERED` (opération) ou `INDEPENDENT` (fiche) ;
  - `syncLane(url, rules?)` (`sync-order.ts:60`) : couloir d'une saisie. `ORDERED` → un seul groupe `group:operations` ; `INDEPENDENT` → un couloir par fiche (`record:/api/v1/suppliers/s-1`, les créations `POST` sur la collection partagent `record:/api/v1/suppliers/`) ; sinon `UNCLASSIFIED` (file unique) ;
  - `nextSendableEntry(entries, isDue)` (`sync-order.ts:85`) : prochaine saisie à envoyer selon la règle (fonction pure).
- `core/offline/retry-schedule.ts` : `RetrySchedule`, heures de relance (1, 2, 4, 8 s) en mémoire, par saisie.
- `OutboxEntry.networkFailure` (`outbox-models.ts:19`) : `true` si la saisie est `FAILED` après 5 échecs transitoires (réseau, 401, 408, 429, 5xx), `false` si refusée par le serveur. Remis à zéro par `retry`.
- `OutboxService` (`outbox-service.ts`) : API publique inchangée (`submit`, `flush`, `retry`, `discard`, `clear`, `pending`, `failed`). Moteur : `flushAccount` (`:252`) envoie un essai à la fois (`syncOnce`), planifie la relance au lieu de l'attendre, puis `pause` (`:205`) jusqu'à la plus proche relance ; une nouvelle saisie, un renvoi ou un abandon réveille la boucle. `discard` relance maintenant le rejeu (les saisies retenues derrière la saisie abandonnée partent).

### Données

Aucune table. Un champ optionnel `networkFailure` dans les entrées IndexedDB (`vvs-outbox`) ; les anciennes entrées sans ce champ comptent comme refus métier (elles ne retiennent rien).

## Règles métier à respecter par les autres tickets

1. **Toute nouvelle écriture passant par `submit` doit être classée** dans `SYNC_ORDER_RULES` : opération rejouable qui modifie des quantités, des montants ou une séquence (vente, facture, avoir, mouvement, transfert, lot, inventaire, livraison, clôture…) → `ORDERED` ; fiche ou paramétrage dont l'ordre ne compte que pour la même fiche → `INDEPENDENT`.
2. Toutes les opérations sont dans **un seul groupe** : une vente, sa facture et le mouvement de stock restent dans l'ordre entre eux. Ne pas créer de groupe séparé sans nouvelle décision PO.
3. Une opération `FAILED` par le réseau retient les opérations suivantes jusqu'à « Renvoyer » ou « Abandonner » (page `/synchronisation`, SYS-09). Une opération refusée par le serveur ne retient rien.
4. Une adresse non classée garde le comportement d'avant (file unique) : c'est un filet de sécurité, pas une cible.

## Fichiers concernés

**Frontend** (`valimbavaka-ui/src/app/core/offline/`)
- `sync-order.ts` (nouveau), `retry-schedule.ts` (nouveau)
- `outbox-service.ts` (moteur), `outbox-models.ts` (`networkFailure`)

**Tests**
- `sync-order.spec.ts`, `outbox-order.spec.ts` (nouveaux) ; `outbox-service.spec.ts` (inchangé, non-régression)

**Docs** : `docs/adr/0001-synchronisation-hors-ligne.md` (section « Mise à jour du 2026-11-08 »).

## Dépendances

**Ce ticket s'appuie sur** :
- [SYS-02](../SYS-02/description.md) · [guide](../SYS-02/guide.md) (file hors ligne)
- [SYS-09](../SYS-09/description.md) · [guide](../SYS-09/guide.md) (page `/synchronisation`, `retry` / `discard`) — PR empilée

**Ces tickets s'appuient sur lui** : tous les tickets qui ajoutent une écriture hors ligne (VTE, FAC, STK, TRF, PRD, INV, LIV, JR, SHO, PRO…).

## Comment l'utiliser depuis un autre ticket

```ts
// sync-order.ts : ajouter une ligne pour le nouvel endpoint d'écriture
{ prefix: '/api/v1/returns', kind: 'ORDERED' },
```

Puis ajouter un cas dans `sync-order.spec.ts` si le préfixe peut se confondre avec un autre.

## Points d'attention

- Hypothèse : « bloquée » = saisie en attente de relance réseau, ou `FAILED` après relances épuisées. Un refus métier (4xx) ne retient rien, comme le prévoyait l'ADR 0001 ; un renvoi ultérieur d'une opération refusée part donc après les suivantes déjà envoyées.
- Hypothèse : les préfixes des modules non livrés (`sales`, `invoices`, `credit-notes`, `transfers`, `production`, `inventories`, `deliveries`, `rounds`, `days`, `shootings`, `promotions`, `catalogs`, `services`) sont repris des branches en cours ; chaque ticket vérifie son préfixe réel.
- Une opération bloquée par le réseau retient toutes les opérations suivantes jusqu'à une action de l'utilisateur : elles restent visibles « en attente » sur `/synchronisation`, sans mention « derrière une saisie bloquée ». Un libellé dédié pourra être ajouté si les utilisateurs le demandent.
- L'envoi reste séquentiel (une requête à la fois) : une fiche passe devant une opération en attente de relance, pas pendant qu'une requête est en vol.
- Une fiche créée hors ligne n'a pas encore d'identifiant serveur : aucune opération hors ligne ne peut la référencer, d'où l'absence de dépendance fiche → opération dans la règle.
- Les heures de relance sont en mémoire : après un rechargement, une saisie déjà relancée attend son délai (`RetrySchedule.restore`).
- Le ticket #136 garde son label `needs-info` : la décision a été prise par Randy le 07/11/2026, le label est à retirer par lui.
