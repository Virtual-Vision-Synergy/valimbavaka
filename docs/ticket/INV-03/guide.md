# Guide d'intégration — INV-03 · Motif d'écart par ligne

Statut : ✍ écrit le 2026-11-08 — **tests à lancer** (voir « Résultat de l'exécution » du scénario) · Ticket : [#79](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/79) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Sur la feuille de comptage (`/inventory`), chaque ligne **en écart** (comptée, compté ≠ théorique figé) propose une liste de 5 motifs ; le motif choisi est enregistré sur le serveur et tracé au journal. Le serveur refuse un motif sur une ligne non comptée ou sans écart, et efface le motif quand un recomptage ramène l'écart à 0. Aucune migration : la colonne `inventory_lines.reason` (INV-01) et la permission `inventory.update` (INV-02) existaient.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| PUT | `/api/v1/inventories/{id}/lines/{lineId}/reason` | `inventory.update` (administrateur) | `InventoryReasonWDto` : `reason` (`@NotNull`, `BREAKAGE` \| `LOSS` \| `ENTRY_ERROR` \| `UNDECLARED_SHOOTING` \| `OTHER`) | 200 `InventoryLineRDto` (contrat d'INV-01, `reason` renseigné) | 400 (motif hors liste), 401, 403, 404 (session inconnue, ou ligne absente de cette session), 409 (session plus `IN_PROGRESS`), 422 (motif absent, ligne non comptée ou sans écart) |

Changement de contrat d'INV-02 : `PUT …/lines/{lineId}/count` renvoie `reason: null` quand le recomptage ne laisse plus d'écart (le motif est effacé en base).

### Services et ports Java réutilisables

| Port | Méthode | Rôle |
|---|---|---|
| `feature.inventory.service.InventoryReasonService` | `InventoryLineRDto qualifyLine(UUID sessionId, UUID lineId, InventoryReasonWDto request)` | Motif d'une ligne (`InventoryReasonServiceImpl.java:59`) ; mêmes contrôles 404/409 que le comptage, puis 422 si `!line.hasGap()` (l. 64). |
| `feature.inventory.domain.InventoryLine` | `boolean hasGap()` (`InventoryLine.java:91`) | Ligne comptée **et** compté ≠ théorique figé. À réutiliser par INV-04 (lignes à motif obligatoire) et INV-05 (nombre d'écarts). |
| `feature.inventory.domain.InventoryLine` | `boolean qualify(InventoryReason)` (`InventoryLine.java:103`) | Seule façon d'écrire `reason` ; `false` si le même motif est rejoué. `count(...)` efface le motif quand il n'y a plus d'écart (l. 84). |

Contrôleur : `feature/inventory/controller/InventoryReasonController.java` (même préfixe `/api/v1/inventories`).

### Frontend (`features/inventory`)

- `data-access/inventory-service.ts` : `qualify(sessionId, lineId, { reason })` (l. 91), `PUT` via `OutboxService` (file hors ligne, `Idempotency-Key`).
- `data-access/inventory-models.ts` : `InventoryReasonWDto`.
- `pages/inventory-page` : `saveReason` (`inventory-page.ts:143`) sur `change` de la liste : le motif est affiché tout de suite (`InventoryState.setReason`) puis envoyé ; toast `danger` `inventory.reasonFailed` si le serveur refuse. Après un comptage enregistré, le motif renvoyé par le serveur remplace le motif local (l. 127). Template inchangé sauf le gestionnaire `(change)`.
- Traduction ajoutée : `inventory.reasonFailed` (fr, mg).

### Données

- Aucune migration (plage de la voie E non utilisée). Colonne `inventory_lines.reason` avec CHECK sur les 5 motifs (migration `2026_11_08-80`, INV-01).
- Journal : action `UPDATE`, ressource `INVENTORY_SESSION`, id de la session, libellé `Motif d'écart de ART-0031 dans INV-0012 : Casse` (libellés français de `InventoryReasonServiceImpl.label`, l. 96, identiques à `inventory.reason.*` en fr). Pas de ligne si le même motif est rejoué.

## Règles métier à respecter par les autres tickets

- **Motif seulement pour une ligne en écart** (`InventoryLine.hasGap`) ; refus 422 sinon.
- **Un recomptage sans écart efface le motif** ; un recomptage qui garde un écart (même de valeur différente) conserve le motif.
- **Pas d'effacement explicite** : `reason: null` est refusé (422). On change de motif, on ne le retire pas ; seul un recomptage sans écart le retire.
- **Session close** : 409, comme le comptage.
- **Clôture (INV-04)** : exiger que toute ligne `hasGap()` ait un `reason` non nul (côté serveur ; l'écran le fait déjà via `InventoryState.missingReasons`/`canClose`).
- **Droits** : administrateur seulement (`inventory.update`).

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/inventory/`)
- `controller/InventoryReasonController.java` (nouveau)
- `service/InventoryReasonService.java`, `service/impl/InventoryReasonServiceImpl.java` (nouveaux)
- `dto/InventoryReasonWDto.java` (nouveau)
- `domain/InventoryLine.java` (`hasGap`, `qualify`, effacement du motif dans `count`)

**Frontend** (`valimbavaka-ui/src/app/features/inventory/`)
- `data-access/inventory-service.ts`, `inventory-models.ts`
- `pages/inventory-page/inventory-page.ts`, `.html` ; `inventory.routes.ts` (commentaire)
- `valimbavaka-ui/public/i18n/fr.json`, `mg.json`

**Tests**
- Backend : `feature/inventory/service/impl/InventoryReasonServiceImplTest`, `feature/inventory/controller/InventoryReasonIT`, `InventoryCountServiceImplTest` (2 tests ajoutés)
- Frontend : `pages/inventory-page/inventory-page.spec.ts`, `data-access/inventory-service.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** :
- [INV-02](../INV-02/guide.md) (comptage, `inventory.update`, `InventoryLine.count`), [INV-01](../INV-01/guide.md) (colonne `reason`, `InventoryReason`), [JNL-01](../JNL-01/guide.md) (`JournalService.record`), [SYS-07](../SYS-07/guide.md) (file hors ligne).

**Ces tickets s'appuient sur lui** :
- Aucun déclaré ; utile à [INV-04](../INV-04/description.md) (clôture : motif obligatoire) et [INV-05](../INV-05/description.md) (historique).

## Comment l'utiliser depuis un autre ticket

Vérifier avant clôture (INV-04), côté Java :

```java
boolean missing = lines.stream().anyMatch(line -> line.hasGap() && line.getReason() == null);
```

## Points d'attention

- **Hypothèse : liste fermée de 5 motifs** (celle de la maquette et de la contrainte CHECK d'INV-01) ; pas de commentaire libre pour « Autre motif ». À confirmer par le PO.
- **Hypothèse : motif refusé (422) hors écart** plutôt qu'ignoré, pour que l'écran et le serveur ne divergent pas.
- **Hypothèse : recomptage sans écart = motif effacé** ; un écart qui change de valeur garde son motif (l'administrateur peut le corriger).
- **Retour sur « Choisir un motif… »** à l'écran : rien n'est envoyé (le serveur n'accepte pas `null`) ; le dernier motif enregistré revient au rechargement.
- **Concurrence** : pas de `version` dans `InventoryReasonWDto` ; deux administrateurs sur la même ligne → le dernier gagne.
- La clôture (INV-04) reste simulée : le bandeau « en construction » de la route reste affiché.
- Traduction malgache `inventory.reasonFailed` rédigée sans relecture native.
- Branche empilée sur `feature/INV-02-comptage-physique-ecart` : à merger après INV-02 (et INV-01).
