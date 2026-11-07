# Scénarios de test — SYS-09 · Suivi et gestion des saisies en échec

Statut : ☐ tests écrits le 2026-11-08, **à lancer** (CI ou `ng test`) · Ticket : [#135](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/135) · [description](description.md) · [guide](guide.md)

Légende de la colonne « Statut » : ✅ auto = test automatique exécuté et vert · ✅ base = vérifié en base · 🔍 code = lu sans test · ❌ écart · ⏭ hors périmètre · 🧪 recette · ☐ = écrit, pas encore exécuté. Détail : [CONSIGNE.md](../CONSIGNE.md#4-remplir-le-scénario-de-test).

Convention (VVS V2.1) : tests frontend Vitest sur le comportement visible.

Point de départ : [SYS-02](../SYS-02/guide.md) a livré le moteur (`OutboxService`, statuts `PENDING` / `FAILED`, `retry`, `discard`), l'indicateur `vvs-sync-status` et une première page `/synchronisation` ; [SYS-07](../SYS-07/guide.md) y fait passer les formulaires. SYS-09 complète la page : saisies en attente visibles, date et action lisibles, motif toujours affiché, retour après « Renvoyer », **confirmation avant « Abandonner »**. Ticket **frontend seul** : aucun endpoint, aucune migration.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | tous (`ADMIN`, `SELLER`, `DELIVERY_DRIVER`) : la page ne demande aucune permission, chaque compte ne voit que ses saisies (`ownerId`) |
| Données de départ | une vendeuse connectée ; une modification de fournisseur `PUT /api/v1/suppliers/0195f0a0-…-b1` saisie le 07/11/2026 à 09:30 et refusée en 409 « La fiche a été modifiée par un autre utilisateur. » ; deux créations d'articles en file hors ligne ; réseau simulé par `ConnectivityService` |
| Tickets à avoir déployés avant | SYS-02, SYS-07 (et SYS-12 pour l'idempotence serveur) |

## Scénarios issus des critères d'acceptation

### Critère 1 — Un indicateur global affiche le nombre de saisies en attente et en échec

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Indicateur dans la barre supérieure | 3 saisies en attente, 1 refusée | pastilles « 3 saisie(s) en attente de synchronisation » et « 1 saisie(s) refusée(s) par le serveur », liens vers `/synchronisation` | `sync-status.spec.ts` « should show the number of pending and refused entries » (SYS-02) | ☐ |
| 1b | Rien à synchroniser | 0 / 0, en ligne | « Synchronisé », aucun lien | `sync-status.spec.ts` « should confirm that everything is synchronised when online with nothing pending » | ☐ |
| 1c | Hors ligne | réseau coupé | « Hors ligne : vos saisies sont conservées » | `sync-status.spec.ts` « should warn that the user is offline » | ☐ |
| 1d | Compteurs repris sur la page | 2 en attente, 1 refusée | en-tête de la page : « 2 saisie(s) en attente… · 1 saisie(s) refusée(s)… » | `sync-page.spec.ts` « should show how many entries are pending and refused » | ☐ |
| 1e | Compteur propre au compte | saisies d'un autre compte en file | ni comptées ni listées | `outbox-service.spec.ts` « should not send one account’s queued requests while another account is connected » (SYS-02) ; filtre `accountEntries` | ☐ |

### Critère 2 — La liste des saisies en échec affiche le message d'erreur de chacune

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 2 | Saisie refusée | `PUT` fournisseur refusé en 409 | ligne avec date, « Modification », l'adresse et « La fiche a été modifiée par un autre utilisateur. » | `sync-page.spec.ts` « should list each refused entry with its action, address and reason » | ☐ |
| 2b | Motif absent | entrée `FAILED` sans `errorMessage` (ancienne entrée) | « Motif non communiqué par le serveur. » | `sync-page.spec.ts` « should explain that no reason was given when the server sent none » | ☐ |
| 2c | Aucune saisie refusée | liste vide | « Aucune saisie refusée : tout est synchronisé. », pas de tableau | `sync-page.spec.ts` « should tell the user that everything is synchronised when nothing failed » | ☐ |
| 2d | Saisies en attente | création d'article relancée après « Serveur indisponible. » | section « Saisies en attente d'envoi », badge « En attente », « Dernière tentative : Serveur indisponible. », **aucun bouton** | `sync-page.spec.ts` « should list pending entries read-only with their last transient error » | ☐ |
| 2e | Ordre de la file | deux saisies hors ligne `/first` puis `/second` | `pending()` dans l'ordre de saisie | `outbox-service.spec.ts` « should list the account pending entries in queue order » | ☐ |
| 2f | Refus métier pendant le rejeu | 422 sur la première, 200 sur la seconde | la première passe `FAILED` avec son motif, la seconde part | `outbox-service.spec.ts` « should mark a refused request as failed without blocking the next one » (SYS-02) | ☐ |

### Critère 3 — L'utilisateur peut renvoyer une saisie en échec, ou l'abandonner après confirmation

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 3 | Renvoyer en ligne | clic « Renvoyer » | `OutboxService.retry('e1')`, toast « Saisie renvoyée. » | `sync-page.spec.ts` « should resend a refused entry and confirm it to the user » | ☐ |
| 3b | Renvoyer hors ligne | réseau coupé, clic « Renvoyer » | la saisie passe aussitôt en attente (compteur à 0 tentative, motif effacé), aucun envoi ; toast « Saisie remise en file : elle partira au retour du réseau. » | `outbox-service.spec.ts` « should show a retried entry as pending at once even when offline » ; `sync-page.spec.ts` « should warn that a resent entry waits for the network when offline » | ☐ |
| 3c | Renvoi abouti | retry puis 200 | la saisie quitte la file | `outbox-service.spec.ts` « should resend a failed request on retry and drop it on discard » (SYS-02) | ☐ |
| 3d | Renvoyer pendant une synchronisation | `isSyncing` vrai | bouton « Renvoyer » désactivé | `sync-page.spec.ts` « should not resend while a synchronisation is running » | ☐ |
| 3e | Abandonner : confirmation demandée | clic « Abandonner » | boîte « Abandonner cette saisie ? » rappelant `PUT /api/v1/suppliers/…`, rien n'est supprimé | `sync-page.spec.ts` « should ask for confirmation before abandoning an entry » | ☐ |
| 3f | Abandon confirmé | bouton rouge « Abandonner » du dialogue | `OutboxService.discard('e1')`, toast « Saisie abandonnée. » | `sync-page.spec.ts` « should drop an entry once the user confirms the abandon » | ☐ |
| 3g | Abandon annulé | « Annuler » (ou Échap) | dialogue fermé, saisie conservée | `sync-page.spec.ts` « should keep the entry when the user cancels the abandon » | ☐ |

## Sécurité et erreurs

Aucun endpoint écrit par ce ticket : les cas 401 / 403 / 404 / 409 / 422 / mass assignment relèvent des endpoints rejoués (chaque ticket métier) et de l'idempotence ([SYS-12](../SYS-12/guide.md)).

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Saisie d'un autre compte | `retry` / `discard` sur un id d'un autre `ownerId` | sans effet | lecture de `OutboxService.retry` (cherche dans `failed()`, filtré par compte) et `discard` (`accountEntries`) | 🔍 code — pas de test dédié ; couvert indirectement par les tests de compte de SYS-02 |
| Renvoi d'une saisie déjà en attente | `retry` sur une entrée `PENDING` | sans effet (ne remet pas à zéro un compteur en cours) | lecture de `OutboxService.retry` | 🔍 code — pas de test dédié |
| Motif affiché sans HTML | message serveur contenant `<b>` | affiché en texte (interpolation Angular, pas d'`innerHTML`) | lecture de `sync-page.html` | 🔍 code |
| Rejeu après un 409 | modification avec version périmée renvoyée telle quelle | nouveau refus 409, la saisie repasse `FAILED` avec le même motif : l'utilisateur doit l'abandonner et ressaisir | comportement de SYS-02 / SYS-07 | 🧪 recette R2 |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Liste vide | aucune saisie | message « tout est synchronisé », section « en attente » masquée | `sync-page.spec.ts` « should tell the user that everything is synchronised when nothing failed » | ☐ |
| Hors ligne avec des saisies en attente | réseau coupé | section « en attente » précédée de « Hors ligne : vos saisies sont conservées » | lecture de `sync-page.html` | 🔍 code |
| Ordre après renvoi | saisie refusée renvoyée alors que d'autres attendent | elle reprend sa place d'origine (`createdAt` inchangé) : l'ordre de la file n'est pas modifié par ce ticket | ⏭ hors périmètre : [SYS-10](../SYS-10/description.md) (needs-info) | ⏭ |
| Rechargement de la page | IndexedDB après F5 | les deux listes sont relues au démarrage | non testable en jsdom | 🧪 recette R1 |
| Changement de compte | autre compte connecté | il ne voit ni les saisies ni les compteurs du premier | `outbox-service.spec.ts` « should retain queued requests until their creating account reconnects » (SYS-02) | ☐ |
| Session expirée avec saisies en attente | jeton expiré | voir [SYS-11](../SYS-11/description.md) | — | ⏭ hors périmètre |

## Tests existants

- `valimbavaka-ui/src/app/features/sync/pages/sync-page/sync-page.spec.ts` (11 tests, réécrit)
- `valimbavaka-ui/src/app/core/offline/outbox-service.spec.ts` (2 tests ajoutés : « should list the account pending entries in queue order », « should show a retried entry as pending at once even when offline »)
- `valimbavaka-ui/src/app/core/offline/sync-status.spec.ts`, `sync-status.a11y.spec.ts` (SYS-02, inchangés)
- `valimbavaka-ui/src/app/core/i18n/translations.spec.ts` (parité `fr` / `mg` des nouvelles clés)

## Recette manuelle (🧪)

| # | Parcours | Résultat attendu | Statut |
|---|---|---|---|
| R1 | Couper le réseau (DevTools), créer un article, recharger la page, ouvrir `/synchronisation` | la saisie est listée « En attente » avec sa date ; l'indicateur affiche 1 en attente ; au retour du réseau elle part et disparaît | 🧪 |
| R2 | Ouvrir une fiche fournisseur dans deux onglets, couper le réseau dans le premier, modifier dans les deux, rétablir le réseau | la saisie du premier onglet passe « refusée » avec le motif 409 ; « Renvoyer » échoue de nouveau ; « Abandonner » demande confirmation puis la retire | 🧪 |
| R3 | Navigation clavier sur la page et dans la boîte de confirmation | focus piégé dans le dialogue, Échap annule, bouton de confirmation en rouge | 🧪 |

## Résultat de l'exécution

- Date : 2026-11-08. **Tests à lancer (CI ou local)** : aucun test n'a été exécuté par l'auteur.
- Vérifications faites : `npx tsc -p tsconfig.app.json --noEmit`, `npx tsc -p tsconfig.spec.json --noEmit`, `npx ngc -p tsconfig.app.json --noEmit` (templates), ESLint et Prettier sur les fichiers modifiés : sans erreur.
- Anomalies : aucune relevée à ce stade.
- Étapes 🧪 restantes : 3 (R1 à R3).
