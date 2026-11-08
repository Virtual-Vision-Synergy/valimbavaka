# Guide d'intégration — SHO-02 · Réintégration d'un produit shooting

Statut : ✅ écrit le 2026-11-08 — tests exécutés en CI, tous verts (voir « Résultat de l'exécution » du scénario) · Ticket : [#44](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/44) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

L'administrateur **réintègre** une sortie shooting (SHO-01) : la sortie passe au statut `RETURNED` (« Réintégré »), avec la date et l'auteur de la réintégration, et chaque article revient en stock par un mouvement `RETURN` positif **dans l'emplacement d'où il était sorti**, le tout dans une seule transaction. Le statut est final : une sortie ne se réintègre qu'une fois (409 sinon), même en cas de clics simultanés.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| POST | `/api/v1/shootings/{id}/return` | `shootings.return` (administrateur seulement) | aucun corps (tout corps envoyé est ignoré) | 200 `ShootingRDto` (`status` `RETURNED`) | 401, 403, 404 (sortie inconnue), 409 (déjà réintégrée), 422 (emplacement de la sortie désactivé entre-temps) |

- `ShootingRDto` gagne deux champs : `returnedAt` (instant de la réintégration, `null` tant que la sortie est dehors) et `returnedBy` (nom complet du compte qui a réintégré, `null` sinon). `late` vaut toujours `false` une fois réintégrée.
- La lecture reste celle de SHO-01 : `GET /api/v1/shootings?status=RETURNED` liste les sorties réintégrées.

### Services et ports Java réutilisables

| Élément | Rôle |
|---|---|
| `ShootingService.returnShooting(UUID)` (`feature/shooting/service/ShootingService.java:36`) | réintégration complète : verrou, statut, mouvements, journal |
| `Shooting.markReturned(Instant, String)` (`domain/Shooting.java:88`) | `OUT` → `RETURNED` avec date et auteur ; `ConflictException` si la sortie n'est plus `OUT` |
| `ShootingPolicy.markReturned(Shooting)` (`service/impl/ShootingPolicy.java:68`) | pose l'horloge injectée et le nom du compte connecté (401 sans session) |
| `ShootingRepository.findByIdForUpdate(UUID)` (`repository/ShootingRepository.java:28`) | lecture verrouillée `PESSIMISTIC_WRITE` : deux réintégrations de la même sortie s'exécutent l'une après l'autre, la seconde voit `RETURNED` et répond 409 |

### Frontend (`features/services`)

- `data-access/shooting-service.ts:51` `returnShooting(id)` : `POST /api/v1/shootings/{id}/return` par la file hors ligne (`OutboxService`, `Idempotency-Key`) ; résultat `SubmitResult<ShootingRDto>`.
- `data-access/service-models.ts` : `ShootingRDto.returnedAt`, `ShootingRDto.returnedBy` ; `ShootingRowRDto.id` (identifiant serveur gardé dans la ligne affichée, `shooting-mapping.ts`).
- `components/shooting-list` : nouvelle entrée `canReturn` (vrai par défaut) qui masque le bouton « Réintégrer » ; la sortie `returned` émet toujours le **numéro** de la sortie.
- `pages/services-page/services-page.ts:134` `returnShooting(number)` : retrouve l'id, appelle le serveur, toast « Sortie SHO-xxxx réintégrée : statut Réintégré, stock remis à jour », recharge les sorties et le stock ; hors ligne, toast `common.queuedSaved` et ligne affichée « Réintégré » en attendant ; refus, toast `services.shooting.returnFailed` (message du serveur). Le bouton n'apparaît qu'avec `shootings.return` (`canReturnShooting`).
- La simulation `SalonService.returnShooting` est supprimée.

### Données

- Migration `2026_11_08-62-changelog.sql` (3 changesets, rollback fourni) : colonnes `shootings.returned_at` (`TIMESTAMPTZ`) et `shootings.returned_by_name` (`VARCHAR(255)`), contrainte `ck_shootings_returned` (statut `RETURNED` ⇔ date et auteur renseignés) ; permission `shootings.return` donnée à `ADMIN`.
- Stock : un mouvement `RETURN` de `+quantité` par article sur `shootings.location_id`, `reference_id` = id de la sortie (STK-01, verrou et contrôles de `StockMovementPolicy`).
- Journal : ressource `SHOOTING`, action `UPDATE`, libellé « Réintégration shooting SHO-0042 : 2 article(s) remis en stock » ; plus une ligne `STOCK_MOVEMENT` par article (STK-01).

## Règles métier à respecter par les autres tickets

- **Statuts** : `OUT` (Sorti) → `RETURNED` (Réintégré), sans retour arrière ; « En retard » reste calculé (`late`) et disparaît après réintégration.
- **Emplacement** : la réintégration remet le stock **dans l'emplacement de sortie**, jamais ailleurs ; un déplacement vers l'entrepôt est un transfert (TRF-xx) à saisir à part.
- **Tout ou rien** : réintégration totale de toutes les quantités sorties ; pas de réintégration partielle.
- **Droits** : seul l'administrateur réintègre (`shootings.return`) ; la vendeuse sort (`shootings.create`) et lit (`services.read`).
- **SHO-03 (suivi)** : sorties en cours = `status = OUT` ; les réintégrées ont `returnedAt` / `returnedBy` pour l'historique.
- **JR-04 (clôture)** : une sortie réintégrée le même jour s'annule dans le stock (mouvement `SHOOTING` négatif puis `RETURN` positif, même `reference_id`). Les mouvements `RETURN` d'une réintégration se distinguent des retours clients par leur `reference_id` (id d'une ligne de `shootings`). La case « Shootings réintégrés » de la clôture = aucune sortie `OUT` dont le retour prévu est passé (`late`).

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/shooting/`)
- `controller/ShootingController.java` (action `returnShooting`)
- `service/ShootingService.java`, `service/impl/ShootingServiceImpl.java`, `service/impl/ShootingPolicy.java`
- `domain/Shooting.java` (`returnedAt`, `returnedByName`, `markReturned`)
- `repository/ShootingRepository.java` (`findByIdForUpdate`)
- `dto/ShootingRDto.java`, `mapper/ShootingMapper.java`

**Migrations Liquibase**
- `valimbavaka-core/src/main/resources/db/changelog/changes/2026_11_08-62-changelog.sql`, déclarée dans `db.changelog-master.yaml`

**Frontend** (`valimbavaka-ui/src/app/features/services/`)
- `data-access/shooting-service.ts`, `shooting-mapping.ts`, `service-models.ts`, `service-service.ts` (simulation retirée)
- `components/shooting-list/shooting-list.ts`, `.html` (entrée `canReturn`)
- `pages/services-page/services-page.ts`, `.html`
- `public/i18n/fr.json`, `mg.json` : `services.shooting.returnedToast` (sans « démonstration »), `services.shooting.returnFailed`

**Tests**
- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/shooting/` : `controller/ShootingReturnIT` (nouveau), `controller/ShootingSecurityIT`, `domain/ShootingTest`, `service/impl/ShootingPolicyTest`, `service/impl/ShootingServiceImplTest`, support `controller/ShootingApiTestSupport` (`createShooting`, `postReturn`)
- Frontend : `data-access/shooting-service.spec.ts`, `data-access/shooting-mapping.spec.ts`, `components/shooting-list/shooting-list.spec.ts` (nouveau)

## Dépendances

**Ce ticket s'appuie sur** :
- [SHO-01](../SHO-01/description.md) · [guide](../SHO-01/guide.md) — entité `Shooting`, statut `RETURNED` déjà prévu par la contrainte, écran Shooting
- [JNL-01](../JNL-01/description.md) · [guide](../JNL-01/guide.md) — `JournalService.record`
- [STK-01](../STK-01/guide.md) — `StockMovementService.record`, type `RETURN`

**Ces tickets s'appuient sur lui** :
- [JR-04](../JR-04/description.md) · [guide](../JR-04/guide.md)

## Comment l'utiliser depuis un autre ticket

```java
ShootingRDto returned = shootingService.returnShooting(shootingId); // 404, 409 si déjà réintégrée
```

```ts
inject(ShootingService).returnShooting(row.id).subscribe((result) => result.queued /* file hors ligne */);
```

## Points d'attention

- **Écart avec le critère (à confirmer par Randy)** : le critère dit « le stock **entrepôt** augmente ». Pour rester cohérent avec SHO-01 (sortie depuis la boutique par défaut), la réintégration remet le stock **dans l'emplacement d'origine** ; elle ne crée pas de transfert implicite boutique → entrepôt. Si le client veut l'entrepôt, il faudra soit sortir depuis l'entrepôt, soit ajouter un emplacement de retour explicite (nouveau ticket).
- Mouvement de type `RETURN` (le type `SHOOTING` est sortant seulement, `StockMovementType.SHOOTING(Sign.OUT)`) : JR-04 doit filtrer les `RETURN` par `reference_id` s'il veut séparer réintégrations shooting et retours clients.
- Pas de réintégration partielle ni de déclaration de casse à la réintégration : un produit abîmé se déclare ensuite par une perte / l'inventaire.
- Emplacement désactivé après la sortie : 422 (contrôle STK-01), la sortie reste `OUT` ; il faut réactiver l'emplacement pour réintégrer.
- Hors ligne : la ligne s'affiche « Réintégré » en attendant la synchronisation ; si le serveur refuse au rejeu (déjà réintégrée par un autre poste), la saisie reste en échec dans la file (SYS-07) et la ligne revient « Sorti » / « Réintégré » au rechargement selon le serveur.
- La page **Clôture** (`features/day`) affiche encore un « Shooting réintégré au stock (démonstration) » : à brancher par JR-04.
- Tests exécutés en CI le 2026-11-08 (18 backend et 3 frontend ajoutés, tous verts) ; recette manuelle 🧪 à passer.
- PR empilée sur SHO-01 (`feature/SHO-01-enregistrement-sortie-shooting`) : à merger après elle.
