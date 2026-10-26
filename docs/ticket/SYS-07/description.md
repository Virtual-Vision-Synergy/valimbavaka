# SYS-07 · Brancher un premier formulaire sur la file hors ligne

> Ticket GitHub [#133](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/133) · Ouvert · Milestone : 01 · Authentification & socle technique · 2 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'utilisateur, je veux que la saisie d'un formulaire réel soit conservée et synchronisée automatiquement quand je perds la connexion afin de voir concrètement qu'aucune de mes saisies n'est perdue.

## Dépendances

- Bloqué par `SYS-02` (#16)
- Bloqué par `FRS-02` (#18)
- Bloqué par `SYS-12` (#138)

## Critères d'acceptation

- [x] Le formulaire de référence (création / modification d'un fournisseur, FRS-02) enregistre via la file hors ligne de SYS-02 et non par un appel direct à l'API. — *⚠ partiel : tests écrits, non exécutés (cas 1 à 1f)*
- [x] Hors connexion, la saisie est conservée localement, reste présente après rechargement de la page, et un indicateur visible affiche son statut (en attente / synchronisé / échec). — *⚠ partiel : conservation et indicateur testés (cas 2 à 2f) ; survie au rechargement à vérifier à la main (recette R2)*
- [x] Au retour du réseau, la saisie est synchronisée automatiquement et n'existe qu'une seule fois en base (aucun doublon, même si la synchronisation est relancée). — *⚠ partiel : tests écrits, non exécutés (cas 3 à 3d)*
- [x] Le principe d'intégration est documenté en quelques lignes pour être reproduit par les modules métier suivants. — *✅ vérifié : [guide](guide.md)*

## Détails

| | |
|---|---|
| **ID Ticket** | `SYS-07` |
| **Module** | Transverse |
| **Story points** | 2 |
| **Utilisateur concerné** | Tous |

> Constat de test (20/10/2026) : le moteur de file d'attente (IndexedDB `vvs-outbox`, ordre d'arrivée, en-tête `Idempotency-Key`, statuts PENDING/FAILED) fonctionne, mais aucun écran ne l'utilise encore. Le formulaire « Nouvel utilisateur » envoie directement à l'API.

## Ce ticket débloque

- [SYS-09](../SYS-09/description.md) · Suivi et gestion des saisies en échec

## Dépend de

- [SYS-02](../SYS-02/description.md) · Sauvegarde automatique hors-ligne (fermé)
- [FRS-02](../FRS-02/description.md) · Création / modification d'un fournisseur (ouvert)
- [SYS-12](../SYS-12/description.md) · Idempotence côté serveur des saisies renvoyées (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/core/offline`
- `valimbavaka-ui/src/app/features/sync`
- `valimbavaka-ui/src/app/core/http/session-interceptor.ts`
- `valimbavaka-ui/src/app/features/suppliers`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/security/idempotency`
- `docs/adr/0001-synchronisation-hors-ligne.md`
