# ADR 0001 — Synchronisation des saisies hors ligne (SYS-02)

## Contexte
Les vendeurs et livreurs saisent parfois sans réseau. Les saisies ne doivent pas être perdues, et
l'architecture doit exister avant les modules métier (LIV-08 en dépend).

## Décision
Pattern **outbox** côté Angular (`core/offline/`) :
- Toute écriture passe par `OutboxService.submit(method, url, body)`.
- Hors connexion (ou erreur réseau `status 0`), la saisie est persistée dans **IndexedDB** (`vvs-outbox`),
  jamais dans `localStorage`. Sans IndexedDB, repli en mémoire pour la session.
- Rejeu **FIFO séquentiel** au retour du réseau, à la connexion et au démarrage. Une nouvelle saisie
  attend derrière les plus anciennes pour préserver l'ordre.
- Erreurs transitoires (réseau, 401, 408, 429, 5xx) : jusqu'à 5 tentatives automatiques par cycle,
  première tentative comprise, avec un délai exponentiel de 1, 2, 4 puis 8 secondes entre relances.
  Après épuisement, l'entrée passe en `FAILED`, reste visible et ne bloque pas la suite (`retry` / `discard`).
- Un renvoi manuel réinitialise le compteur et démarre un nouveau cycle.
- Refus métier (autres 4xx) : l'entrée passe immédiatement en `FAILED`, reste visible et ne bloque pas
  la suite (`retry` / `discard`).
- Chaque entrée porte un UUID envoyé dans l'en-tête `Idempotency-Key` ; l'en-tête est autorisé en CORS.
- Chaque entrée est rattachée au compte qui l'a créée (`ownerId`) : une déconnexion **conserve** les saisies,
  qui ne sont rejouées qu'au retour de ce compte et ne partent jamais sous un autre (SYS-11). Seul
  `OutboxService.clear()` vide la file. *(Mise à jour du 2026-10-23 : la version initiale indiquait une file vidée
  à la déconnexion, ce que le code ne fait plus.)*

## Mise à jour du 2026-11-08 : ordre en cas de blocage (SYS-10)
Décision PO du 07/11/2026 (Randy) : **ordre strict par type**. Le rejeu reste séquentiel (un envoi à la fois),
mais une saisie en attente de relance ne retient plus que son « couloir » (`core/offline/sync-order.ts`) :
- **opérations métier** (journal, ventes, factures, avoirs, stock, transferts, production, inventaire, livraisons,
  tournées, journées, shootings) : un seul groupe en ordre strict. Elles attendent derrière une saisie du groupe
  bloquée par le réseau, y compris quand ses 5 tentatives sont épuisées (`FAILED` avec `networkFailure`), jusqu'à
  son renvoi ou son abandon ;
- **fiches et référentiels** (fournisseurs, articles, utilisateurs, promotions, catalogues, services, gammes,
  canaux, emplacements) : elles passent devant une saisie bloquée ; seules les saisies d'une même fiche restent
  dans l'ordre entre elles ;
- **adresse non classée** : file unique comme avant SYS-10.
Un refus métier (`FAILED` sans `networkFailure`) ne retient toujours rien.

## Mise à jour du 2026-11-08 : session expirée avec des saisies en attente (SYS-11)
- Une saisie part toujours avec le jeton du compte créateur. Au retour du réseau, un 401 déclenche un seul refresh
  (`core/http/session-interceptor.ts`) puis le rejeu avec le nouveau jeton et la **même** `Idempotency-Key`.
- Refresh **refusé par le serveur** (401, 403, compte différent) : la session est fermée et l'utilisateur renvoyé
  vers `/login?session=expired`, qui l'invite à se reconnecter ; la saisie n'est ni comptée comme un échec ni
  envoyée. Elle part dès que le **même** compte se reconnecte ; un autre compte ne la voit pas et ne l'envoie pas.
- Refresh **impossible** (réseau 0, 408, 429, 5xx) : la session est conservée, l'essai compte comme une erreur
  transitoire et la relance suivante retente le refresh.

## Côté serveur
`IdempotencyFilter` (`security/idempotency`, après l'authentification) traite les écritures
(POST/PUT/PATCH/DELETE) portant `Idempotency-Key` :
- la clé est réservée par un INSERT atomique dans `idempotency_keys` (clé primaire `(user_id, key)`) ;
- la réponse (< 500, hors 401/403) est mémorisée ; un rejeu reçoit la même réponse avec
  `Idempotency-Replayed: true` ; les erreurs 5xx libèrent la clé pour permettre un nouvel essai ;
- même clé pour une autre requête : 422 ; requête identique en cours : 409 ; clé non UUID : 400 ;
- conservation 24 h (purge à chaque réservation). L'en-tête `Location` n'est pas rejoué.

## Côté interface
Les entrées `FAILED` sont listées sur `/synchronisation` (badges du menu) : renvoyer ou abandonner.

## Conséquences
- Les features appellent `submit` au lieu de `HttpClient` pour leurs écritures.
- Hors périmètre : service worker / PWA, résolution de conflits métier.
