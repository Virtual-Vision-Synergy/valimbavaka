# Guide d'intégration — SRV-01 · Catalogue des services salon

Statut : ✅ rédigé · Ticket : [#29](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/29) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

L'administrateur tient le catalogue des soins du salon : nom, durée (minutes), prix (Ar), taux de commission facultatif (%) et produits habituellement consommés (article + quantité dans son unité). La vendeuse lit le catalogue ; rien ne se supprime (on désactive) et chaque écriture est tracée au journal. Le ticket branche aussi le niveau « service salon » du résolveur de COM-01 : **décision PO du 07/11/2026**, le taux du service passe devant le canal et la vendeuse, sans cumul. SRV-02 s'appuie sur ce catalogue pour pré-remplir les produits consommés d'une prestation.

## Contrat exposé

### Endpoints backend (`/api/v1/salon-services`)

| Méthode | URL | Permission (`@HasPermission`) | Entrée (`XxxWDto`) | Sortie (`XxxRDto`) | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/salon-services` | `services.read` (admin, vendeuse) | `PageQuery` (`page`, `size`, `search` sur le nom, `sortBy` ∈ `createdAt` (défaut, décroissant), `name`, `price`, `durationMinutes`) + `active` facultatif | 200 `PageResponse<SalonServiceRDto>` | 401, 403 |
| GET | `/api/v1/salon-services/{id}` | `services.read` | - | 200 `SalonServiceRDto` | 404 `NOT_FOUND` |
| POST | `/api/v1/salon-services` | `services.create` (admin) | `SalonServiceWDto` | 201 `SalonServiceRDto` + `Location` | 409 (nom pris, casse ignorée), 422 (données invalides, article inconnu, inactif ou listé deux fois) |
| PUT | `/api/v1/salon-services/{id}` | `services.update` (admin) | `SalonServiceWDto` | 200 `SalonServiceRDto` | 404, 409 (version périmée, nom), 422 |
| PATCH | `/api/v1/salon-services/{id}/status` | `services.update` | `SalonServiceStatusWDto` (`active`) | 200 `SalonServiceRDto` | 404, 422 |

**`SalonServiceWDto`** : `name` (obligatoire, 100 max, unique sans tenir compte de la casse), `durationMinutes` (entier 1 à 600), `price` (≥ 0, 2 décimales), `commissionRate` (facultatif, 0 à 100, 2 décimales ; `null` = pas de taux propre), `products` (facultatif, 20 lignes max : `ConsumedProductWDto(articleId, quantity)`, quantité > 0 avec 3 décimales max ; une ligne `null` est ignorée : élément déclaré `@Nullable`, filtré dans `SalonCatalogServiceImpl.toDetails`), `version` (facultatif ; périmé → 409). Ni `id` ni statut.
**`SalonServiceRDto`** : `id`, `name`, `durationMinutes`, `price`, `commissionRate` (ou `null`), `products` (`ConsumedProductRDto(articleId, quantity)`, triés par `articleId`), `active`, `createdAt`, `updatedAt`, `version`.
`PUT` est un remplacement complet : la liste `products` envoyée **remplace** la précédente (absente = vide). Toujours renvoyer tous les champs.
Contrôleur : `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/salonservice/controller/SalonServiceController.java`.

### Services et ports Java réutilisables

- **`feature.salonservice.service.SalonCatalogService`** : `getServices(PageQuery, Boolean active)`, `getServiceById(UUID)` (404 si inconnu), `createService`, `updateService`, `setActive` (implémentation `service/impl/SalonCatalogServiceImpl.java:61` à `:108`). SRV-02 lit un service par `getServiceById` : prix, durée et `products()` (recette par défaut).
- **Port COM-01 implémenté** : `service/impl/SalonServiceCommissionRatesImpl.java:30` (`findRate(UUID)`) — renvoie le taux du service, vide si le service est inconnu ou n'a pas de taux. Un service **désactivé garde son taux** (prestations passées). C'est le seul bean `SalonServiceCommissionRates` : ne pas en créer un second (`ObjectProvider.getIfAvailable()` échouerait).
- Ajouté dans ART-01 : `ArticleService.isActiveArticle(UUID)` (`feature/article/service/ArticleService.java:30`), contrôle croisé sur le modèle de `SupplierService.isActiveSupplier`.
- Contrôle des produits : `service/impl/ConsumedProductChecker.java:28` (un article une seule fois ; un article **nouvellement** listé doit être actif).

### Frontend (`features/services`)

- Routes (`services.routes.ts`, sous `/services`, route parente gardée par `services.read`) : `catalog` (liste), `catalog/new` (`services.create`), `catalog/:id/edit` (`services.update`). La route `''` (maquette salon & shooting) est inchangée.
- Menu : section Catalogue, entrée `menu.item.salonCatalog` → `/services/catalog` (permission `services.read`, `core/layout/menu.ts`).
- `data-access/salon-catalog-service.ts` (`SalonCatalogService`) : `list`, `getById`, `create`, `update`, `setActive` ; écritures par la file hors ligne `OutboxService` (SYS-07). Types dans `data-access/salon-catalog-models.ts` (`SalonServiceRDto`, `SalonServiceWDto`, `ConsumedProductRDto/WDto`, bornes).
- Pages : `pages/salon-catalog-list-page` (recherche, pagination, activation confirmée, lecture seule pour la vendeuse), `pages/salon-catalog-form-page` (champs + lignes de produits ; articles actifs lus par `features/article/data-access/article-service.ts`, unité courte `u.` / `kg` / `L`).
- Maquette : les types de prestation de `data-access/service-models.ts` s'appellent maintenant `ServiceRecordRDto` / `ServiceRecordWDto` (c'étaient `SalonServiceRDto` / `SalonServiceWDto`, qui désignent désormais le catalogue).
- Traductions : clés `salonCatalog.*` et `menu.item.salonCatalog` dans `public/i18n/fr.json` et `mg.json`.

### Données

- Table `salon_services` (`name` VARCHAR(100), `duration_minutes` INTEGER `CHECK` 1-600, `price NUMERIC(14,2)` `CHECK ≥ 0`, `commission_rate NUMERIC(5,2)` nullable `CHECK` 0-100, `is_active`, audit, `version`) ; index unique `uq_salon_services_name` sur `lower(name)`.
- Table `salon_service_products` (`salon_service_id` FK, `article_id` FK `articles`, `quantity NUMERIC(12,3)` `CHECK > 0`, clé primaire `(salon_service_id, article_id)`) ; index `ix_salon_service_products_article_id`. Côté JPA : `@ElementCollection` de `ConsumedProduct` (`domain/SalonService.java`), même feature.
- Migration `valimbavaka-core/src/main/resources/db/changelog/changes/2026_11_08-91-changelog.sql` (changesets 1 à 3, rollback fourni), déclarée dans `db.changelog-master.yaml` après `2026_11_08-90` (COM-01).
- Permissions `services.create` et `services.update` (nouvelles, `ADMIN`) ; `services.read` existait (AUTH-01, `ADMIN` et `SELLER`).
- Journal : `resourceType = SALON_SERVICE` ; `CREATE` « Création du service salon <nom> (<prix> Ar) », `UPDATE` « Modification… », « Activation… », « Désactivation… ». Rien n'est tracé si rien ne change.

## Règles métier à respecter par les autres tickets

- **Commission (décision PO du 07/11/2026)** : service salon > canal > utilisateur, sans cumul. Pour une prestation, appeler `CommissionRateResolver.resolve(sellerId, salesChannelId, salonServiceId)` (COM-01) ; ne jamais relire `salon_services.commission_rate` à la main. Le taux d'un service s'applique au chiffre d'affaires (`REVENUE`).
- **Pour SRV-02 (enregistrement d'une prestation)** :
  - proposer uniquement les services `active = true` (`GET /api/v1/salon-services?active=true`) ;
  - prix et durée du catalogue = valeurs par défaut de la prestation ; garder dans la prestation une **copie** du prix et des quantités consommées (le catalogue peut changer ensuite) ;
  - `products` = recette par défaut : quantité dans l'**unité de l'article** (`ArticleRDto.unit` : `PIECE`, `KILOGRAM`, `LITER`), 3 décimales. Exemple : huile de coco `0.015` L = 15 ml. La vendeuse ajuste avant validation, puis la déduction du stock salon passe par STK-01 ;
  - un article de la recette peut avoir été désactivé depuis : le signaler plutôt que de bloquer la prestation.
- Un service se désactive, ne se supprime pas (JNL-02). Le nom est unique sans tenir compte de la casse.
- Référencer un service par son UUID (`salonServiceId`), jamais par son nom.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/salonservice/`) :
- `controller/SalonServiceController.java`
- `domain/SalonService.java`, `SalonServiceDetails.java`, `ConsumedProduct.java`
- `dto/SalonServiceWDto.java`, `SalonServiceRDto.java`, `SalonServiceStatusWDto.java`, `ConsumedProductWDto.java`, `ConsumedProductRDto.java`
- `mapper/SalonServiceMapper.java`, `repository/SalonServiceRepository.java`
- `service/SalonCatalogService.java`, `service/impl/SalonCatalogServiceImpl.java`, `ConsumedProductChecker.java`, `SalonServiceCommissionRatesImpl.java`

**Modifiés ailleurs** : `feature/article/service/ArticleService.java` (+ `impl/ArticleServiceImpl.java`, `repository/ArticleRepository.existsByIdAndActiveTrue`).

**Migration** : `valimbavaka-core/src/main/resources/db/changelog/changes/2026_11_08-91-changelog.sql`.

**Frontend** (`valimbavaka-ui/src/app/`) : `features/services/services.routes.ts`, `features/services/data-access/salon-catalog-models.ts`, `salon-catalog-service.ts`, `features/services/pages/salon-catalog-list-page/*`, `features/services/pages/salon-catalog-form-page/*`, renommage dans `features/services/data-access/service-models.ts`, `service-service.ts`, `components/salon-form/salon-form.ts`, `pages/services-page/services-page.ts` ; `core/layout/menu.ts`, `core/auth/auth-testing.ts` ; `public/i18n/fr.json`, `mg.json`.

**Tests** : voir [scénario](scenario-test.md#tests-existants).

## Dépendances

**Ce ticket s'appuie sur** :
- [ART-01](../ART-01/description.md) · [guide](../ART-01/guide.md) — articles (produits consommés), `ArticleService`.
- [COM-01](../COM-01/description.md) · [guide](../COM-01/guide.md) — port `SalonServiceCommissionRates` et résolveur `CommissionRateResolver` (branche de base de cette PR).
- [JNL-01](../JNL-01/description.md) · [guide](../JNL-01/guide.md) — `JournalService.record`.

**Ces tickets s'appuient sur lui** :
- [SRV-02](../SRV-02/description.md) · [guide](../SRV-02/guide.md) — enregistrement d'une prestation (prix, durée, produits consommés pré-remplis).
- [COM-02](../COM-02/description.md) · [guide](../COM-02/guide.md) — calcul de la commission d'une prestation via le résolveur.

## Comment l'utiliser depuis un autre ticket

```java
// SRV-02 : valeurs par défaut d'une prestation à partir du catalogue
SalonServiceRDto service = salonCatalogService.getServiceById(request.salonServiceId());
if (!service.active()) throw new BusinessException("Service salon inactif : " + service.id());
List<ConsumedProductRDto> recipe = service.products(); // quantités dans l'unité de chaque article

// COM-02 : taux applicable à une prestation (le taux du service gagne s'il existe)
ApplicableCommissionRDto applicable = commissionRateResolver.resolve(sellerId, salesChannelId, service.id());
```

```ts
// Front SRV-02 : soins proposés à l'enregistrement
inject(SalonCatalogService).list({ page: 1, size: 100, active: true, sortBy: 'name', sortDirection: 'ASC' });
```

## Points d'attention

- **Taux et non forfait** : la maquette « Mes commissions » (`features/commissions/pages/commissions-page`) présente les prestations salon « au forfait », mais la décision PO du 07/11/2026 parle de **taux**. Le catalogue stocke donc un **taux en %** (`commission_rate`), conforme au port de COM-01. Si le métier veut un montant fixe par prestation, il faudra une colonne dédiée et faire évoluer `ApplicableCommissionRDto` avant COM-02 (à trancher par Randy).
- **Hypothèse : commission facultative**. Sans taux propre, le canal puis la vendeuse s'appliquent (affiché « Canal ou vendeuse » dans la liste). Un taux à `0` est un vrai taux : il annule la commission de la prestation.
- **Hypothèse : bornes** — durée 1 à 600 min, 20 produits max par service, quantité > 0 avec 3 décimales (comme le seuil d'un article). À ajuster par migration si besoin.
- **Hypothèse : n'importe quel article actif** peut être consommé (matière première, semi-fini ou produit fini) ; pas de filtre par type ni par emplacement « salon » (le stock par emplacement relève de STK-01 / SRV-02).
- Un article désactivé après coup reste dans la recette et la fiche reste modifiable (il n'est contrôlé que s'il est nouvellement ajouté) ; l'écran l'affiche « Article inactif ».
- L'écran charge au plus 100 articles actifs (plafond de page du serveur) pour la liste déroulante : au-delà, il faudra une recherche dans le sélecteur (ticket à ouvrir si le catalogue d'articles grossit).
- La maquette « Salon & shooting » (`/services`) reste sur ses données de démonstration : son branchement sur ce catalogue appartient à SRV-02.
- Les nouvelles pages ne sont pas dans `demo-pages.a11y.spec.ts` (réservé aux pages de démonstration) : vérifier l'accessibilité en recette (R4).
- PR empilée sur COM-01 (migration `2026_11_08-90` requise avant `2026_11_08-91`) : à merger après la PR de `feature/COM-01-regles-commission-canal`.
