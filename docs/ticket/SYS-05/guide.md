# Guide d'intégration — SYS-05 · Rôles métier, référentiels et compte administrateur initial

Statut : ✔ rempli depuis le code et les tests au 2026-10-23 — à valider par le traiteur · Ticket : [#111](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/111) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Une base neuve démarre avec exactement trois rôles (Administrateur, Vendeuse, Livreur), trois référentiels métier prêts à l'emploi (canaux de vente, gammes, emplacements de stock) et, si aucun administrateur n'existe, un premier compte administrateur créé depuis des variables d'environnement. Ce compte doit changer son mot de passe à la première connexion.

## Contrat exposé

### Rôles

- Codes techniques : `ADMIN` (Administrateur), `SELLER` (Vendeuse), `DELIVERY_DRIVER` (Livreur) ; libellés français conservés. Contrainte `CHECK` sur `roles.name`.
- Historique des migrations : `2026_10_15-01-1` crée ADMINISTRATEUR / VENDEUSE / LIVREUR et supprime l'ancien `USER` (utilisateurs rattachés à VENDEUSE) ; `2026_10_16-01` renomme les codes en anglais. L'ancien changeset n'a pas été modifié.
- Les droits de chaque rôle sont dans [AUTH-01](../AUTH-01/guide.md) (table `permissions`).

### Référentiels (lecture seule)

| Méthode | URL | Permission | Sortie |
|---|---|---|---|
| GET | `/api/v1/sales-channels` | `referentials.read` | `PageResponse<SalesChannelRDto>` |
| GET | `/api/v1/product-ranges` | `referentials.read` | `PageResponse<ProductRangeRDto>` |
| GET | `/api/v1/stock-locations` | `referentials.read` | `PageResponse<StockLocationRDto>` |

Valeurs initiales (colonnes `code` / `label`, `is_active`) :
- Canaux de vente : `BOUTIQUE` Boutique, `FACEBOOK` Facebook, `TELEPHONE` Téléphone, `LIVRAISON` Livraison (liste à confirmer avec le métier).
- Gammes : `ZEZIKA`, `BOTANIKA`, `SOLIKA`.
- Emplacements de stock : `ENTREPOT` Entrepôt, `BOUTIQUE` Boutique, `LIVRAISON` Livraison, `SALON` Salon.

Aucun endpoint d'écriture sur les référentiels : ils évoluent par migration. `referentials.read` est donnée à `ADMIN` et `SELLER`.

### Compte administrateur initial

- Au démarrage, `AdminAccountInitializer` (`ApplicationRunner`) appelle `UserService#createInitialAdministratorIfMissing`. Idempotent : si un administrateur existe, rien n'est fait.
- Variables d'environnement (préfixe `vvs.bootstrap.admin`) : `ADMIN_EMAIL`, `ADMIN_PASSWORD` (15 à 128 caractères), `ADMIN_FIRST_NAME`, `ADMIN_LAST_NAME`. Exemple dans `.env.example`. Aucune valeur par défaut, jamais dans le code ni dans un changelog.
- Variables absentes ou invalides sur une base sans administrateur : le démarrage échoue avec `AdminAccountConfigurationException` qui nomme les variables en cause (le mot de passe n'apparaît jamais, `toString` le masque).
- Le compte est créé avec `isPasswordChangeRequired = true` (`User#createWithTemporaryPassword`) ; le champ remonte dans `UserRDto` et dans `CurrentUserRDto` (`/auth/me`) pour que l'interface force le changement.

### Frontend

- `features/referential/data-access/referential-service.ts` : `listSalesChannels()`, `listProductRanges()`, `listStockLocations()` (une seule page de 100, triée par libellé, renvoie un tableau).
- `features/user/data-access/sales-channel-service.ts` : usage du canal de vente dans le formulaire de vendeuse (AUTH-03).

## Règles métier pour les autres tickets

- **Ne jamais coder les rôles ni les listes en dur dans l'interface** : lire les référentiels via l'API.
- Un nouveau code de référentiel passe par une migration Liquibase additive (jamais en modifiant `2026_10_15-01`).
- Référencer un référentiel par son UUID (`id`), pas par son `code`, dans les tables métier ; le `code` sert aux règles (ex. emplacement `LIVRAISON`).
- Un rôle ne s'ajoute pas sans migration qui élargit la contrainte `ck_roles_name` et sans revoir `RoleName`.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/`)
- `referential/controller/ReferentialController.java`, `service/*`, `repository/*`, `domain/*`, `dto/*`, `mapper/ReferentialMapper.java`
- `user/config/AdminAccountInitializer.java`, `AdminAccountProperties.java` ; `user/service/impl/UserServiceImpl.java` (`createInitialAdministratorIfMissing`) ; `user/domain/User.java`

**Migrations**
- `valimbavaka-core/src/main/resources/db/changelog/changes/2026_10_15-01-changelog.sql` (rôles, `roles`, `is_active`, mot de passe temporaire, audit en UUID, trois référentiels)
- `valimbavaka-core/src/main/resources/db/changelog/changes/2026_10_16-01-changelog.sql` (codes en anglais)

**Configuration** : `.env.example` (variables `ADMIN_*`)

**Frontend** (`valimbavaka-ui/src/app/features/referential/`) : `data-access/referential-service.ts`, `referential-models.ts`

**Tests**
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/user/config/AdminAccountInitializerIT.java`, `AdminAccountPropertiesTest.java`
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/referential/service/impl/ReferentialServiceImplIT.java`
- `valimbavaka-ui/src/app/features/referential/data-access/referential-service.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** : aucun.

**Ces tickets s'appuient sur lui** : [AUTH-01](../AUTH-01/guide.md), [AUTH-03](../AUTH-03/guide.md), [SYS-06](../SYS-06/guide.md), [ART-01](../ART-01/description.md), [ART-03](../ART-03/description.md), [ART-05](../ART-05/description.md), [STK-01](../STK-01/description.md), [STK-03](../STK-03/description.md) et la suite.

## Comment l'utiliser depuis un autre ticket

```ts
// Liste déroulante de gammes
this.referentialService.listProductRanges().subscribe((ranges) => this.ranges.set(ranges));
```

Premier démarrage d'un environnement :

```
ADMIN_EMAIL=admin@valimbavaka.mg ADMIN_PASSWORD=<15 caractères minimum> \
ADMIN_FIRST_NAME=Admin ADMIN_LAST_NAME=Local ./mvnw spring-boot:run
```

## Points d'attention

- Les trois listes sont en lecture seule : si le métier veut les gérer depuis l'application (ajout d'un canal, désactivation d'une gamme), un ticket d'écriture est à créer ; `is_active` existe déjà.
- La liste des canaux de vente reste « à confirmer avec le métier » (critère du ticket) ; « Livraison » est à la fois un canal et un emplacement de stock.
- Aucun écran de changement de mot de passe ni endpoint associé n'existe encore (voir AUTH-01) : le drapeau `isPasswordChangeRequired` est transmis mais rien ne permet de le lever dans l'application.
- Le rôle de l'administrateur initial est lu dans la base ; si les migrations n'ont pas tourné, le démarrage échoue avec « Rôle ADMIN absent ».
- Les `created_by` / `updated_by` d'anciennes lignes ont été remis à `NULL` lors du passage en UUID (migration `2026_10_15-01-4`).
