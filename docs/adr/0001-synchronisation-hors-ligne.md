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
- Erreurs transitoires (réseau, 401, 408, 429, 5xx) : la synchronisation s'arrête et reprend plus tard.
  Refus métier (autres 4xx) : l'entrée passe en `FAILED`, reste visible et ne bloque pas la suite
  (`retry` / `discard`).
- Chaque entrée porte un UUID envoyé dans l'en-tête `Idempotency-Key` ; l'en-tête est autorisé en CORS.
- La file est vidée à la déconnexion : les saisies d'un compte ne partent jamais sous un autre.

## Conséquences
- Les features appellent `submit` au lieu de `HttpClient` pour leurs écritures.
- **Suivi** : le backend doit dédupliquer sur `Idempotency-Key` (rejeu après coupure en cours d'envoi) ;
  l'UI de résolution des entrées `FAILED` reste à construire avec les premiers modules.
- Hors périmètre : service worker / PWA, résolution de conflits métier.
