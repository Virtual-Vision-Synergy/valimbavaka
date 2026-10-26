# Scénarios de test — SYS-07 · Brancher un premier formulaire sur la file hors ligne

Statut : ☐ tests écrits le 2026-10-26, **à lancer** (CI ou `ng test`) · Ticket : [#133](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/133) · [description](description.md) · [guide](guide.md)

Légende de la colonne « Statut » : ✅ auto = test automatique exécuté et vert · ✅ base = vérifié en base · 🔍 code = lu sans test · ❌ écart · ⏭ hors périmètre · 🧪 recette · ☐ = écrit, pas encore exécuté. Détail : [CONSIGNE.md](../CONSIGNE.md#4-remplir-le-scénario-de-test).

Convention (VVS V2.1) : tests frontend Vitest sur le comportement visible.

Point de départ : [SYS-02](../SYS-02/guide.md) a livré le moteur (`OutboxService`, IndexedDB, rejeu FIFO, `Idempotency-Key`) mais aucun écran ne l'utilisait. SYS-07 branche le **formulaire de référence** (création et modification d'un fournisseur, [FRS-02](../FRS-02/guide.md)) puis les autres écritures déjà livrées : articles, activation d'un fournisseur, modification et activation d'un compte. `OutboxService.submit` renvoie maintenant la réponse du serveur quand la saisie part tout de suite (`SubmitResult.body`). [SYS-12](../SYS-12/guide.md) garantit côté serveur qu'un renvoi ne crée pas de doublon.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (`suppliers.create`, `suppliers.update`, `articles.create`, `articles.update`) |
| Données de départ | un administrateur connecté ; fournisseur « Huiles Ravo » (`LOCAL`) en version 3 ; réseau simulé par `ConnectivityService` |
| Tickets à avoir déployés avant | SYS-02, FRS-02, SYS-12 (idempotence serveur), ART-02 |

## Scénarios issus des critères d'acceptation

### Critère 1 — Le formulaire de référence enregistre via la file hors ligne et non par un appel direct à l'API

| # | Cas | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Création en ligne | Il saisit « Huiles Ravo » et enregistre | `POST /api/v1/suppliers` avec un en-tête `Idempotency-Key` (UUID), retour à la liste, aucun message de file | `supplier-form-page-offline.spec.ts` « should send a creation with an idempotency key when online and go back to the list » | ☐ |
| 1b | Modification en ligne | Il change le nom d'une fiche en version 3 | `PUT` avec `version: 3` et la même clé d'idempotence par saisie | `supplier-form-page.spec.ts` « should save every edited field with PUT then go back to the list » (FRS-02, signe en administrateur) | ☐ |
| 1c | Refus serveur en ligne | Nom déjà pris (409) | message du serveur affiché, saisie conservée, **rien n'est mis en file** | `supplier-form-page-offline.spec.ts` « should keep the server refusal on screen when online, without queueing it » | ☐ |
| 1d | Aucun appel direct | `SupplierService.create / update / setActive` | passent par `OutboxService.submit` (méthode `POST`, `PUT`, `PATCH`) | `supplier-service.spec.ts` « désactive un fournisseur via PATCH /status, sans suppression » ; lecture de `supplier-service.ts` | ☐ |
| 1e | Connecté à d'autres écritures | article (création, modification), activation d'un fournisseur, modification et activation d'un compte | même mécanique | `article-form-page.spec.ts` « should send the creation with an idempotency key when online » ; `article-service.spec.ts` « crée une fiche avec POST », « modifie une fiche avec PUT en renvoyant la version lue » ; `user-list-page.spec.ts` « should deactivate an active user after confirmation and reload the list » | ☐ |
| 1f | Exception : création de compte | `POST /api/v1/users` porte un mot de passe | reste un appel direct : le mot de passe n'est jamais écrit dans IndexedDB | `user-service.ts` (`create` utilise `HttpClient`) | 🔍 code — pas de test ; à ajouter si la mécanique change |

### Critère 2 — Hors connexion, la saisie est conservée, survit au rechargement, et un indicateur affiche son statut

| # | Cas | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 2 | Création hors connexion | Réseau coupé, il saisit et enregistre | aucun `POST`, une saisie en attente (`pendingCount` = 1), message « Saisie enregistrée hors connexion… », retour à la liste | `supplier-form-page-offline.spec.ts` « should keep a creation made offline, say it will be synchronized and go back to the list » | ☐ |
| 2b | Modification hors connexion | La fiche (version 3) est lue, puis le réseau tombe, il modifie le nom | saisie en file ; au retour du réseau `PUT` avec `version: 3` | `supplier-form-page-offline.spec.ts` « should queue a modification made offline with the version read » | ☐ |
| 2c | Saisie invalide hors connexion | Nom d'espaces, réseau coupé | rien n'est mis en file, pas de message | `supplier-form-page-offline.spec.ts` « should queue nothing when the form is invalid, even offline » | ☐ |
| 2d | Article hors connexion | Il crée un produit fini réseau coupé | saisie en file, message de synchronisation, retour à `/articles` | `article-form-page.spec.ts` « should keep a creation made offline in the queue, say so and go back to the list » | ☐ |
| 2e | Activation hors connexion | Il désactive un fournisseur réseau coupé | message d'information, l'état affiché ne change pas, la liste n'est pas rechargée | `supplier-detail.spec.ts` « should keep an activation made offline in the queue and tell it without changing the state » | ☐ |
| 2f | Indicateur de statut | saisies en attente ou refusées | la barre supérieure affiche « hors ligne », le nombre en attente et refusées | `sync-status.spec.ts` « should warn that the user is offline » ; « should show the number of pending and refused entries » (SYS-02) | ☐ |
| 2g | Survie au rechargement de la page | saisie en file puis `F5` | la saisie est toujours là (IndexedDB `vvs-outbox`) | `outbox-storage.ts` ; non testable en jsdom (pas d'IndexedDB) | 🧪 recette R2 |

### Critère 3 — Au retour du réseau, synchronisation automatique, une seule fois en base

| # | Cas | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 3 | Rejeu au retour du réseau | saisie en file, le réseau revient | un seul `POST`, avec la même clé d'idempotence que celle de la saisie, file vidée après la réponse | `supplier-form-page-offline.spec.ts` « should send the queued creation once, with the same key, when the network is back » | ☐ |
| 3b | Ordre et reprise | plusieurs saisies en file | envoyées dans l'ordre d'arrivée | `outbox-service.spec.ts` « should replay queued requests in order once back online » (SYS-02) | ☐ |
| 3c | Pas de doublon côté serveur | la même saisie renvoyée trois fois | un seul fournisseur, une seule ligne de journal | `IdempotencyIT.createSupplier_replayedWithSameKey_writesOneRecordAndOneJournalLine` ([SYS-12](../SYS-12/scenario-test.md)) | ☐ |
| 3d | Réponse rendue au formulaire | saisie partie tout de suite | la réponse du serveur (`body`) est transmise (code article dans le message) | `outbox-service.spec.ts` « should send the request with an idempotency key when online » | ☐ |

### Critère 4 — Le principe est documenté pour être reproduit par les modules suivants

| # | Cas | Résultat attendu | Où | Statut |
|---|---|---|---|---|
| 4 | Guide d'intégration | marche à suivre en quelques lignes, extrait de code, pièges | [guide.md](guide.md) ; règle dans [README](../README.md) et [CONSIGNE](../CONSIGNE.md) ; dépendance « SYS-07 » ajoutée aux 29 tickets d'écriture | ✅ relu |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans session active | `submit` sans compte | 401 `UNAUTHENTICATED`, rien en file | `outbox-service.spec.ts` « should reject a submission without an authenticated account » (SYS-02) | ☐ |
| Saisie d'un autre compte | compte B connecté, saisie du compte A en file | non envoyée sous B | `outbox-service.spec.ts` « should not send one account’s queued requests while another account is connected » (SYS-02) | ☐ |
| Refus métier (400, 404, 409, 422) | nom déjà pris | relancé à l'écran, jamais mis en file | `supplier-form-page-offline.spec.ts` « should keep the server refusal on screen when online, without queueing it » | ☐ |
| Mot de passe dans la file | création de compte | jamais écrit dans IndexedDB (appel direct) | `user-service.ts` | 🔍 code |
| Fiche illisible | lecture 500 puis saisie d'un nom | bouton désactivé, aucun `PUT` (sinon la fiche serait écrasée) | `supplier-form-page-offline.spec.ts` « should send nothing when the sheet could not be read » ; « should keep the save button disabled until the sheet is read » | ☐ |

## Cas limites et hors ligne

| Cas | Exemple d'entrée | Résultat attendu | Comment c'est vérifié | Statut |
|---|---|---|---|---|
| Saisies plus anciennes en attente | une saisie en file, le réseau est revenu mais pas encore rejouée | la nouvelle saisie est mise **derrière** pour garder l'ordre (`queued: true` même en ligne) | `outbox-service.spec.ts` « should queue a new request behind older pending ones to keep the order » (SYS-02) | ☐ |
| Coupure pendant l'envoi | le réseau tombe au milieu du `POST` | saisie mise en file, rejouée avec la même clé | `outbox-service.spec.ts` « should queue the request when the network fails while sending » (SYS-02) | ☐ |
| Double clic sur Enregistrer | deux clics rapides | une seule saisie (bouton désactivé pendant l'envoi) | `article-form-page.spec.ts` « should disable the save buttons while saving so that a double click sends one request » | ☐ |
| Modification d'une fiche changée entre-temps | `PUT` rejoué avec une version périmée | refus 409 : saisie marquée `FAILED`, visible dans la page de synchronisation | comportement du moteur ; page des échecs : [SYS-09](../SYS-09/description.md) | ⏭ hors périmètre (SYS-09) |
| Création suivie d'une modification hors connexion | créer puis rouvrir la fiche créée sans réseau | impossible : l'identifiant n'existe qu'après synchronisation | contrainte du guide SYS-02 | ⏭ hors périmètre (à traiter si un ticket l'exige) |
| Session expirée avec saisies en attente | jeton expiré au retour du réseau | refresh puis rejeu | `session-interceptor.ts` | ⏭ hors périmètre ([SYS-11](../SYS-11/description.md)) |
| Compte désactivé entre-temps | saisie rejouée par un compte désactivé | 401/403 : saisie refusée, visible | moteur | ⏭ hors périmètre (SYS-09, SYS-11) |

## Tests existants

Frontend (`valimbavaka-ui/src/app/`) : `features/supplier/pages/supplier-form-page/supplier-form-page-offline.spec.ts` (nouveau, 8 cas), `supplier-form-page.spec.ts` (FRS-02, signe en administrateur), `features/supplier/data-access/supplier-service.spec.ts`, `features/article/data-access/article-service.spec.ts`, `features/articles/pages/article-form-page/article-form-page.spec.ts` (2 cas ajoutés), `features/suppliers/components/supplier-detail/supplier-detail.spec.ts` (1 cas ajouté), `core/offline/outbox-service.spec.ts` (résultat avec `body`).

Backend : `IdempotencyIT`, `IdempotencyFilterTest`, `IdempotencyStoreIT` (SYS-12).

## Recette manuelle (🧪)

| # | Étape | Résultat attendu | Statut |
|---|---|---|---|
| R1 | Connecté en administrateur, couper le réseau (onglet réseau « hors ligne »), créer un fournisseur | message « enregistrée hors connexion », compteur « 1 en attente » dans la barre supérieure | 🧪 |
| R2 | Recharger la page (`F5`) hors ligne, se reconnecter au même compte | le compteur « 1 en attente » est toujours là | 🧪 |
| R3 | Rétablir le réseau | la saisie part toute seule, le compteur retombe à 0, le fournisseur apparaît **une seule fois** dans `/suppliers` et une ligne `CREATE` dans `/journal` | 🧪 |
| R4 | Même parcours avec un article, puis l'activation d'un fournisseur | idem | 🧪 |

## Résultat de l'exécution

- Date : 2026-10-26 (tests écrits, **non exécutés** : consigne projet « ne pas lancer les tests »). Typage, compilation des gabarits, Prettier et ESLint passés en local.
- Environnement : CI GitHub ou `cd valimbavaka-ui && npx ng test --watch=false`.
- Anomalies ouvertes : aucune. Limites : persistance IndexedDB non testable en jsdom (recette R2) ; création de compte volontairement hors file (mot de passe).
- Corrigé au passage : en modification d'un fournisseur, une fiche illisible laissait un formulaire vide enregistrable (écrasement possible) ; le bouton est maintenant désactivé tant que la fiche n'est pas lue.
- Étapes 🧪 restantes : 4 (R1 à R4).
