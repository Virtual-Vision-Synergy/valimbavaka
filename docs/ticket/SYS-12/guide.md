# Guide d'intégration — SYS-12 · Idempotence côté serveur des saisies renvoyées

Statut : ✅ écrit le 2026-10-26 — **tests non exécutés** (voir « Résultat de l'exécution » du scénario) · Ticket : [#138](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/138) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Le serveur reconnaît l'en-tête `Idempotency-Key` sur toute écriture (`POST`, `PUT`, `PATCH`, `DELETE`) : une même clé n'exécute la requête qu'une fois par utilisateur, les renvois reçoivent la réponse déjà produite (`Idempotency-Replayed: true`), le journal ne reçoit donc qu'une ligne par saisie. SYS-12 complète [SYS-02](../SYS-02/guide.md) : une clé réutilisée avec **un contenu différent** est maintenant refusée (422), et les cas limites (clé en cours, libération sur erreur, purge à 24 h) sont testés.

## Contrat exposé

### Endpoints backend

Aucun nouvel endpoint : le filtre s'applique à toutes les écritures authentifiées.

| Situation | Réponse |
|---|---|
| Première requête avec la clé | traitée normalement ; réponse mémorisée si le statut est < 500 et différent de 401 / 403 |
| Rejeu, même méthode, même chemin, même corps | même statut et même corps, en-tête `Idempotency-Replayed: true`, aucune écriture, aucune ligne de journal |
| Même clé pendant le traitement | 409 `CONFLICT` « Requête en cours de traitement. » |
| Même clé, autre méthode ou autre chemin | 422 `VALIDATION_FAILED` « Idempotency-Key déjà utilisée pour une autre requête. » |
| **Même clé, même chemin, corps différent (nouveau)** | 422 `VALIDATION_FAILED` « Idempotency-Key déjà utilisée avec un contenu différent. » |
| Clé qui n'est pas un UUID | 400 `MALFORMED_REQUEST` |
| Réponse 5xx, 401 ou 403 | clé libérée : le client peut réessayer, même avec un corps modifié |
| Sans en-tête, ou méthode de lecture | requête traitée normalement, sans protection |

### Classes Java

- `security.idempotency.IdempotencyFilter` : réserve la clé (`claim`), exécute, mémorise (`complete`) ou libère (`release`).
- `security.idempotency.RequestFingerprint(method, path, bodyHash)` : empreinte d'une requête ; `sameTarget` compare méthode et chemin, `differentContent` compare les corps quand les deux sont connus.
- `security.idempotency.CachedBodyRequest` : lit le corps une fois pour calculer son SHA-256, puis le remet intact (`getInputStream`, `getReader`) à la chaîne. Les corps `multipart/*`, de plus de 1 Mio ou de longueur inconnue ne sont pas lus : pas d'empreinte, seuls méthode et chemin sont comparés.
- `security.idempotency.IdempotencyStore` : `claim(userId, key, RequestFingerprint)`, `find`, `complete`, `release` ; la purge des clés de plus de 24 h se fait à chaque réservation. `StoredRequest` porte maintenant `request` (l'empreinte) au lieu de `method` et `path`.

### Données

- Migration `valimbavaka-core/src/main/resources/db/changelog/changes/2026_10_26-02-changelog.sql` : colonne `idempotency_keys.request_hash` (`VARCHAR(64)`, nullable) ; rollback : suppression de la colonne.
- Les lignes écrites avant la migration n'ont pas d'empreinte : leur rejeu est accepté sans comparaison du contenu.

### Frontend

Aucun changement : `OutboxService` envoie déjà `Idempotency-Key` ([guide SYS-02](../SYS-02/guide.md)).

## Règles métier à respecter par les autres tickets

- **Une clé = une saisie.** Ne jamais réutiliser une clé pour une saisie modifiée : l'utilisateur qui corrige une saisie refusée (422, 409) doit obtenir une nouvelle clé. `OutboxService` le fait déjà (un UUID par saisie).
- Un contrôleur d'écriture ne doit produire aucun effet de bord hors transaction : le filtre protège contre le double envoi, pas contre un effet partiel dû à une exception (la clé est alors libérée et la saisie rejouée).
- Un contrôleur qui répond 5xx, 401 ou 403 libère la clé : prévoir que le corps puisse changer entre deux essais.
- Les opérations d'annulation et de contre-passation ([JNL-02](../JNL-02/guide.md)) sont rejouables par la même mécanique.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/security/idempotency/`)
- `IdempotencyFilter.java`, `IdempotencyStore.java`, `RequestFingerprint.java`, `CachedBodyRequest.java`

**Migration** : `valimbavaka-core/src/main/resources/db/changelog/changes/2026_10_26-02-changelog.sql`, déclarée dans `db.changelog-master.yaml`.

**Tests** (`valimbavaka-core/src/test/java/org/vvs/valimbavaka/security/idempotency/`) : `IdempotencyFilterTest.java`, `IdempotencyStoreIT.java`, `IdempotencyIT.java`.

## Dépendances

**Ce ticket s'appuie sur** : [SYS-02](../SYS-02/guide.md) (filtre, table, en-tête), [JNL-01](../JNL-01/guide.md) (journal : une ligne par saisie).

**Ces tickets s'appuient sur lui** : tous ceux qui écrivent par `OutboxService` ([SYS-07](../SYS-07/description.md) et les tickets qui en dépendent) ; [SYS-09](../SYS-09/description.md) pour le renvoi d'une saisie en échec.

## Comment l'utiliser depuis un autre ticket

```bash
# Même clé, même corps : un seul enregistrement, une seule ligne de journal
curl -X POST /api/v1/suppliers -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: 0195f0a0-0000-7000-8000-0000000000a1" \
  -H "Content-Type: application/json" -d '{"name":"Huiles Ravo","type":"LOCAL"}'
# renvoyé à l'identique : 201 + Idempotency-Replayed: true
# même clé avec un autre nom : 422 « contenu différent »
```

## Points d'attention

- Corps de plus de 1 Mio, envois de fichier et requêtes à longueur inconnue (`Transfer-Encoding: chunked`) : pas d'empreinte, donc un contenu différent n'est pas détecté. Les imports en masse ([ART-07](../ART-07/description.md)) y passeront ; à reconsidérer si un import peut être rejoué avec un fichier modifié.
- La lecture du corps pour l'empreinte est faite en mémoire (≤ 1 Mio) avant le contrôleur ; les écritures de l'application sont de petits JSON.
- Deux rejeux strictement simultanés sont départagés par l'`INSERT … ON CONFLICT DO NOTHING` de la table ; ce cas n'est pas couvert par un test de concurrence.
- Les tests n'ont pas été exécutés par l'auteur : voir le scénario avant de s'appuyer sur ce guide pour un comportement limite.
