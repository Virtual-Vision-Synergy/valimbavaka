# Scénarios de test — SYS-12 · Idempotence côté serveur des saisies renvoyées

Statut : ☐ tests écrits le 2026-10-26, **à lancer** (CI ou `mvn verify`) · Ticket : [#138](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/138) · [description](description.md) · [guide](guide.md)

Légende de la colonne « Statut » : ✅ auto = test automatique exécuté et vert · ✅ base = vérifié en base · 🔍 code = lu sans test · ❌ écart · ⏭ hors périmètre · 🧪 recette · ☐ = écrit, pas encore exécuté. Détail : [CONSIGNE.md](../CONSIGNE.md#4-remplir-le-scénario-de-test).

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL.

Point de départ : [SYS-02](../SYS-02/guide.md) avait livré le filtre `IdempotencyFilter` et la table `idempotency_keys`. Il refusait une clé réutilisée pour **une autre méthode ou un autre chemin**, mais pas pour **le même chemin avec un contenu différent** (critère 3). SYS-12 ajoute l'empreinte du corps (migration `2026_10_26-02`) et les tests manquants (clé en cours, libération sur erreur, purge à 24 h, une seule ligne de journal).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (création de fournisseurs et d'utilisateurs) |
| Données de départ | un administrateur ; une clé `Idempotency-Key` UUID par saisie ; fournisseur d'exemple « Rejeu <uuid> », type `LOCAL` |
| Tickets à avoir déployés avant | SYS-02 (filtre et table), JNL-01 (journal), FRS-01 (fournisseurs) ; migration `2026_10_26-02` appliquée |

## Scénarios issus des critères d'acceptation

### Critère 1 — Une même clé envoyée plusieurs fois ne crée qu'un enregistrement et renvoie le même résultat

| # | Cas | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Rejeu d'une création | `POST /api/v1/users` deux fois avec la même clé et le même corps | 201 les deux fois, même `id`, en-tête `Idempotency-Replayed: true` au second, aucun 409 « email déjà utilisé » | `IdempotencyIT.createUser_replayedWithSameKey_returnsSameResponseWithoutDuplicate` (SYS-02) | ☐ |
| 1b | Trois envois d'un fournisseur | `POST /api/v1/suppliers` trois fois, même clé, même corps | 201 trois fois, **un seul** fournisseur en base | `IdempotencyIT.createSupplier_replayedWithSameKey_writesOneRecordAndOneJournalLine` | ☐ |
| 1c | Même corps sans clé | deux `POST` identiques sans en-tête | pas d'idempotence : le 2e est refusé (409) | `IdempotencyIT.createUser_sameBodyWithoutKey_isNotDeduplicated` (SYS-02) | ☐ |
| 1d | Réponse mémorisée | première requête qui répond 201 avec un corps JSON | statut et corps conservés pour le rejeu | `IdempotencyFilterTest.doFilter_firstRequest_storesTheResponseSoItCanBeReplayed` | ☐ |
| 1e | Rejeu : réponse rendue sans exécuter la chaîne | clé déjà terminée, même corps | statut et corps mémorisés, `Idempotency-Replayed: true`, contrôleur non appelé | `IdempotencyFilterTest.doFilter_sameKeySameBody_writesTheStoredResponseAndMarksItReplayed` | ☐ |
| 1f | Même clé, deux utilisateurs | deux comptes envoient la même clé | indépendantes : les deux sont acceptées | `IdempotencyStoreIT.claim_sameKeyForTwoUsers_bothAreAccepted` | ☐ |
| 1g | Réservation atomique | deux réservations de la même clé | la 1re réussit, la 2e est refusée | `IdempotencyStoreIT.claim_sameKeyTwice_secondClaimIsRefused` | ☐ |

### Critère 2 — Le journal append-only ne reçoit qu'une seule écriture par saisie, même renvoyée

| # | Cas | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 2 | Une ligne de journal pour trois envois | création d'un fournisseur envoyée trois fois avec la même clé | `journal_entries` ne gagne qu'**une** ligne (ressource `SUPPLIER`, action `CREATE`) | `IdempotencyIT.createSupplier_replayedWithSameKey_writesOneRecordAndOneJournalLine` | ☐ |
| 2b | Le rejeu n'atteint pas le service | clé déjà traitée | la chaîne (contrôleur, service, journal) n'est pas exécutée | `IdempotencyFilterTest.doFilter_sameKeySameBody_writesTheStoredResponseAndMarksItReplayed` | ☐ |

### Critère 3 — Une clé déjà utilisée avec un contenu différent est refusée avec une erreur claire

| # | Cas | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 3 | Même chemin, corps différent | `POST /api/v1/users` avec la clé K et l'e-mail `un-…`, puis avec K et l'e-mail `deux-…` | 422 `VALIDATION_FAILED`, détail « Idempotency-Key déjà utilisée avec un contenu différent. » | `IdempotencyIT.createUser_keyReusedWithDifferentBody_returns422` ; `IdempotencyFilterTest.doFilter_sameKeyDifferentBody_returns422` | ☐ |
| 3b | Autre méthode ou autre chemin | `POST /users` avec K, puis `PATCH /users/<id>/status` avec K | 422, détail « … pour une autre requête. » | `IdempotencyIT.createUser_keyReusedForAnotherRequest_returns422` (SYS-02) ; `IdempotencyFilterTest.doFilter_sameKeyOtherMethod_returns422` | ☐ |
| 3c | Corps réécrit mais empreinte absente | ligne sans empreinte (écrite avant SYS-12, ou corps non lu) | rejeu accepté sans comparaison du contenu | `IdempotencyFilterTest.doFilter_storedRequestWithoutBodyHash_isReplayedWhateverTheBody` | ☐ |
| 3d | Le corps reste lisible | le filtre lit le corps pour l'empreinte | le contrôleur reçoit le même corps ; empreinte SHA-256 de 64 caractères | `IdempotencyFilterTest.doFilter_jsonBody_isHashedAndStillReadableByTheChain` | ☐ |
| 3e | Envoi de fichier | `multipart/form-data` | corps non lu, pas d'empreinte (seuls méthode et chemin sont comparés) | `IdempotencyFilterTest.doFilter_multipartBody_isNotReadSoNoHashIsKept` | ☐ |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Clé sans jeton d'accès | `POST` avec `Idempotency-Key`, sans `Authorization` | le filtre ne fait rien, l'autorisation répond 401 | `IdempotencyIT.createUser_keyWithoutToken_returns401` (SYS-02) | ☐ |
| Clé qui n'est pas un UUID | `Idempotency-Key: pas-un-uuid` | 400 `MALFORMED_REQUEST` | `IdempotencyIT.createUser_invalidKey_returns400` (SYS-02) ; `IdempotencyFilterTest.doFilter_keyIsNotAnUuid_returns400` | ☐ |
| Même clé pendant le traitement | clé réservée, réponse pas encore écrite | 409 `CONFLICT` « Requête en cours de traitement. », contrôleur non appelé | `IdempotencyFilterTest.doFilter_requestStillInProgress_returns409WithoutRunningTheChain` | ☐ |
| Erreur serveur (500, 503) | le contrôleur répond 5xx | clé libérée, le client peut réessayer avec la même clé | `IdempotencyFilterTest.doFilter_responseNotReplayable_releasesTheKeySoTheClientCanRetry` (500, 503) | ☐ |
| Refus d'accès (401, 403) | le contrôleur répond 401 ou 403 | clé libérée (une session renouvelée peut rejouer) | `IdempotencyFilterTest.doFilter_responseNotReplayable_releasesTheKeySoTheClientCanRetry` (401, 403) | ☐ |
| Exception dans la chaîne | le service lève une exception | clé libérée, l'exception remonte | `IdempotencyFilterTest.doFilter_chainThrows_releasesTheKey` | ☐ |
| Libération en base | `release` après `claim` | la clé peut être réservée de nouveau | `IdempotencyStoreIT.release_afterClaim_letsTheSameKeyBeClaimedAgain` | ☐ |
| Méthode de lecture ou absence d'en-tête | `GET`, ou `POST` sans clé | requête traitée normalement, aucun accès à la table | `IdempotencyFilterTest.doFilter_getOrNoKey_isNotFiltered` | ☐ |

## Cas limites

| Cas | Exemple d'entrée | Résultat attendu | Comment c'est vérifié | Statut |
|---|---|---|---|---|
| Clé de plus de 24 h | ligne créée il y a 25 h | purgée à la réservation suivante ; une ligne de 23 h est conservée | `IdempotencyStoreIT.claim_keysOlderThan24Hours_arePurgedAndYoungerOnesKept` | ☐ |
| Réutiliser une clé expirée | même utilisateur, même clé, 25 h plus tard | traitée comme une nouvelle saisie | `IdempotencyStoreIT.claim_oldKeyOfTheSameUser_canBeReusedAfterThePurge` | ☐ |
| Réservation lue avant la fin | `find` sur une clé réservée | `status` nul (« en cours »), empreinte relue | `IdempotencyStoreIT.find_reservedKey_isInProgressWithItsFingerprint` | ☐ |
| Réponse mémorisée puis relue | `complete(201, corps)` puis `find` | statut et corps identiques | `IdempotencyStoreIT.complete_thenFind_returnsTheStoredResponse` | ☐ |
| Corps de plus de 1 Mio ou longueur inconnue (chunked) | envoi volumineux | corps non lu, pas d'empreinte : seuls méthode et chemin comparés | `CachedBodyRequest.isCacheable` | 🔍 code — pas de test ; limite connue |
| Corps différent rejoué après une réponse 5xx | 1re saisie en 500, 2e saisie modifiée avec la même clé | clé libérée par le 5xx : la 2e saisie est traitée comme neuve | `IdempotencyFilterTest.doFilter_responseNotReplayable_releasesTheKeySoTheClientCanRetry` | ☐ |
| Deux rejeux simultanés | deux requêtes avec la même clé au même instant | une seule exécutée (`INSERT … ON CONFLICT DO NOTHING`), l'autre reçoit 409 ou la réponse | réservation atomique ; `IdempotencyStoreIT.claim_sameKeyTwice_secondClaimIsRefused` | ☐ (concurrence réelle non testée) |
| Migration | `2026_10_26-02` | colonne `request_hash` nullable ; rollback = suppression de la colonne | à rejouer sur une base neuve | ☐ base |

## Tests existants

Backend (`valimbavaka-core/src/test/java/org/vvs/valimbavaka/security/idempotency/`) : `IdempotencyFilterTest.java` (nouveau, 12 méthodes, 15 cas avec les valeurs paramétrées), `IdempotencyStoreIT.java` (nouveau, 7 cas), `IdempotencyIT.java` (SYS-02, 2 cas ajoutés).

Frontend : aucun changement (l'en-tête `Idempotency-Key` est déjà envoyé par `OutboxService`, voir [SYS-02](../SYS-02/guide.md)).

## Recette manuelle (🧪)

| # | Étape | Résultat attendu | Statut |
|---|---|---|---|
| R1 | Avec `curl`, envoyer deux fois `POST /api/v1/suppliers` avec la même clé et le même corps | 201 deux fois, un seul fournisseur dans la liste, une seule ligne `CREATE` dans `/journal` | 🧪 |
| R2 | Renvoyer la même clé avec un autre nom | 422 « contenu différent » | 🧪 |

## Résultat de l'exécution

- Date : 2026-10-26 (tests écrits, **non exécutés** : consigne projet « ne pas lancer les tests »). Compilation, formatage Spotless et Checkstyle passés en local.
- Environnement : CI GitHub ou `cd valimbavaka-core && mvn -B -ntp verify` (Docker requis pour Testcontainers).
- Anomalies ouvertes : aucune. Limites connues : corps de plus de 1 Mio ou de longueur inconnue non comparé ; concurrence réelle non testée.
- Étapes 🧪 restantes : 2 (R1, R2).
