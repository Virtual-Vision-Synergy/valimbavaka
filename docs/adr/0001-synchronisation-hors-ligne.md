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
- La file est vidée à la déconnexion : les saisies d'un compte ne partent jamais sous un autre.

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
