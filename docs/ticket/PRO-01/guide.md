# Guide d'intégration — PRO-01 · Création d'une promotion

Statut : ☐ rédigé le 2026-11-08 sur le code livré, tests à exécuter (CI) · Ticket : [#58](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/58) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

L'administrateur crée une promotion : type de remise (`PERCENT`, `AMOUNT`, `BUY_X`), valeur, périmètre (tout le catalogue, des gammes ou des articles), canaux de vente, période (jours inclus, heure de Madagascar), code promo facultatif et plafond d'utilisation facultatif. PRO-01 ne fait que le **référentiel** : aucune remise n'est encore calculée, la caisse (VTE-02) n'existant pas. Ce guide fixe le contrat que PRO-02 (application automatique), VTE-04 (code promo), PRO-03 (statut et compteur) et PRO-04 (liste et détail) doivent utiliser.

## Contrat exposé

### Endpoints backend (`/api/v1/promotions`)

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| `POST` | `/api/v1/promotions` | `promotions.create` (ADMIN) | `PromotionWDto` | 201 `PromotionRDto` + `Location` | 401, 403, 409 (code déjà pris), 422 (format ou règle métier) |
| `GET` | `/api/v1/promotions/{id}` | `promotions.read` (ADMIN, SELLER) | — | `PromotionRDto` | 401, 403, 404 |

`PromotionWDto` (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/promotion/dto/PromotionWDto.java`) : `name` (1–100), `description?` (≤ 500), `kind`, `value` (> 0, 12 chiffres + 2 décimales), `code?` (3–12 lettres ou chiffres, espaces et casse ignorés), `scope` (`ALL` / `RANGES` / `ARTICLES`), `targetIds?` (≤ 100 UUID), `startsOn`, `endsOn` (`yyyy-MM-dd`), `channelIds` (1–10 UUID de `sales_channels`), `usageLimit?` (1–1 000 000). Aucun `id`, `status` ni `usageCount` accepté.

`PromotionRDto` (`…/promotion/dto/PromotionRDto.java`) : les mêmes champs (code en majuscules, listes sans doublon) + `id`, `usageCount`, `status` (`SCHEDULED` / `ACTIVE` / `ENDED`, calculé à chaque lecture), `createdAt`, `updatedAt`, `version`.

### Service Java

`PromotionService` (`…/promotion/service/PromotionService.java`) :
- `PromotionRDto getPromotionById(UUID promotionId)` — 404 si inconnue.
- `PromotionRDto createPromotion(PromotionWDto request)` — contrôles, enregistrement, journal.

Ajout dans ART-01 : `ArticleService.getArticlesByIds(Collection<UUID>)` (`…/feature/article/service/ArticleService.java:36`), articles actifs ou non, identifiants inconnus absents. Il évite d'appeler `getArticleById` dans une transaction d'écriture (son 404 marquerait la transaction en échec).

### Frontend (`valimbavaka-ui/src/app/features/promotions`)

- `data-access/promotion-service.ts` : `context()` (liste de démonstration + gammes et canaux du référentiel), `create(draft, refs)` → `OutboxService.submit('POST', …)` avec `Idempotency-Key` (SYS-07).
- `data-access/promotion-request.ts` : `toPromotionWDto` (codes de gamme et de canal → UUID ; `SHOP`→`BOUTIQUE`, `PHONE`→`TELEPHONE`), `toCardView`.
- `data-access/promotion-models.ts` : `PromotionWDto` / `PromotionRDto` miroirs du backend ; `PromotionCardView` (carte de liste, encore alimentée par les fixtures) ; `PromotionDraft` (saisie du formulaire).
- Page `pages/promotion-list-page` : envoie le formulaire, ajoute la carte créée, toast de succès, de mise en file ou d'erreur (`promotions.createFailed`).

### Données et journal

Migration `valimbavaka-core/src/main/resources/db/changelog/changes/2026_11_08-60-changelog.sql` :
- `promotions` : `kind`, `discount_value NUMERIC(14,2)`, `code` (unique quand renseigné, `uq_promotions_code`), `scope`, `starts_on`, `ends_on`, `usage_limit`, `usage_count` (0), audit et `version`. Contraintes CHECK reprenant les règles ci-dessous.
- `promotion_channels (promotion_id, sales_channel_id)` → FK `sales_channels`.
- `promotion_targets (promotion_id, target_id)` : gamme ou article selon `scope` (pas de FK, cible polymorphe), index sur `target_id`.
- Permission `promotions.create` accordée à `ADMIN` ; `promotions.read` existait déjà (ADMIN, SELLER).
- Journal : `CREATE` / `PROMOTION` / « Création de la promotion <nom> ».

## Règles métier pour les autres tickets

Appliquées par `PromotionRules` (`…/promotion/service/impl/PromotionRules.java:44`), toutes en 422 :
- **Période** : `endsOn ≥ startsOn` et `endsOn ≥ aujourd'hui` ; jours inclus, jour métier en `Indian/Antananarivo` (`PromotionRules.BUSINESS_ZONE`). Un début dans le passé est accepté.
- **Valeur** : `PERCENT` ≤ 100 ; `AMOUNT` en ariary ; `BUY_X` entier de 2 à 100.
- **Périmètre** : `ALL` sans cible ; `RANGES` ≥ 1 gamme active ; `ARTICLES` ≥ 1 produit fini (`FINISHED`) actif.
- **Canaux** : ≥ 1 canal actif.
- **Code** : unique (409) ; stocké en majuscules.
- **Statut** : jamais stocké, `PromotionPeriod.statusOn(jour)` (`…/promotion/domain/PromotionPeriod.java:14`).

### Contrat pour PRO-02 (application automatique en caisse) et VTE-04 (code promo)

Hypothèses fixées par PRO-01, à respecter (ou à changer en mettant ce guide à jour) :
1. **Automatique ou par code** : une promotion **sans code** s'applique d'elle-même (PRO-02) ; une promotion **avec code** ne s'applique **que** si la vendeuse saisit le code (VTE-04).
2. **Éligibilité d'une ligne de panier** le jour J sur le canal C : `startsOn ≤ J ≤ endsOn` (Antananarivo), `C ∈ channelIds`, `usageLimit` nul ou `usageCount < usageLimit`, et l'article est visé (`ALL` ; `RANGES` : `rangeId` de l'article ∈ `targetIds` ; `ARTICLES` : `id` de l'article ∈ `targetIds`).
3. **Calcul** : `PERCENT` = `prix × value / 100` sur chaque ligne éligible ; `AMOUNT` = `value` ariary retirés une fois du total des lignes éligibles, plafonné à ce total ; `BUY_X` = pour chaque article éligible, `floor(quantité / value)` unités offertes. Arrondi à l'ariary, `RoundingMode.HALF_UP` (convention §6.4).
4. **Pas de cumul** : si plusieurs promotions automatiques sont éligibles, PRO-02 retient la plus avantageuse pour le client ; un code promo (VTE-04) remplace la promotion automatique s'il donne plus. Les chevauchements de période ne sont pas refusés à la création.
5. **Compteur** : chaque vente validée qui applique une promotion incrémente `usage_count` d'une unité (méthode à ajouter par PRO-02 / VTE-04 sous verrou, suivie par PRO-03) ; une annulation de vente ne le décrémente pas sans décision contraire.
6. **Méthodes à ajouter** (non livrées par PRO-01) dans `PromotionService` : `List<PromotionRDto> getApplicablePromotions(LocalDate day, UUID salesChannelId)` (sans code) et `Optional<PromotionRDto> findApplicableByCode(String code, LocalDate day, UUID salesChannelId)` ; requêtes sur `ix_promotions_period` et `uq_promotions_code`.

## Fichiers concernés

- Backend : `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/promotion/` — `controller/PromotionController.java`, `domain/{Promotion,PromotionDetails,PromotionPeriod,PromotionKind,PromotionScope,PromotionStatus}.java`, `dto/{PromotionWDto,PromotionRDto}.java`, `mapper/PromotionMapper.java`, `repository/PromotionRepository.java`, `service/PromotionService.java`, `service/impl/{PromotionServiceImpl,PromotionRules}.java`.
- Article : `feature/article/service/ArticleService.java`, `service/impl/ArticleServiceImpl.java:95`, `repository/ArticleRepository.java` (`findAllByIdIn`).
- Migration : `valimbavaka-core/src/main/resources/db/changelog/changes/2026_11_08-60-changelog.sql` (déclarée dans `db.changelog-master.yaml`).
- Frontend : `valimbavaka-ui/src/app/features/promotions/data-access/{promotion-models,promotion-request,promotion-service,promotion-fixtures,promotion-status}.ts`, `components/promotion-form/promotion-form.ts`, `components/promotion-card/promotion-card.ts`, `pages/promotion-list-page/promotion-list-page.ts`, `public/i18n/{fr,mg}.json`.
- Tests : voir [scénario](scenario-test.md#tests-existants).

## Dépendances

- Dépend de : [ART-01](../ART-01/guide.md) (articles, `ArticleService`), [JNL-01](../JNL-01/guide.md) (`JournalService`), [SYS-05](../SYS-05/guide.md) (gammes, canaux, `ReferentialService`), [SYS-07](../SYS-07/guide.md) (file hors ligne).
- Utilisé par : [PRO-02](../PRO-02/description.md), [VTE-04](../VTE-04/description.md), [PRO-03](../PRO-03/description.md), [PRO-04](../PRO-04/description.md), [ART-06](../ART-06/description.md).

## Comment l'utiliser

```java
// PRO-02 / VTE-04 : relire une promotion et son statut du jour
PromotionRDto promotion = promotionService.getPromotionById(promotionId);
boolean usable = promotion.status() == PromotionStatus.ACTIVE
        && promotion.channelIds().contains(salesChannelId)
        && (promotion.usageLimit() == null || promotion.usageCount() < promotion.usageLimit());
```

```ts
// Créer depuis un autre écran (refs = gammes et canaux du référentiel)
this.promotionService.create(draft, refs).subscribe((result) => result.queued || show(result.body));
```

## Points d'attention

- **Liste encore en démonstration** : `context()` renvoie la liste de `promotion-fixtures.ts` ; PRO-04 ajoute `GET /api/v1/promotions` paginé et remplace les fixtures. Les nombres d'articles affichés dans le choix du périmètre viennent aussi du catalogue de démonstration. La route garde `underConstruction`.
- **Formulaire inchangé** : il ne propose que « tout le catalogue » ou **une** gamme (pas la sélection d'articles ni de plusieurs gammes, ni la description) alors que l'API accepte `RANGES` multiples et `ARTICLES` : à ajouter dans l'écran par PRO-04 ou ART-06.
- **Canaux** : l'interface connaît `SHOP` / `FACEBOOK` / `PHONE`, le référentiel `BOUTIQUE` / `FACEBOOK` / `TELEPHONE` / `LIVRAISON` ; la correspondance est dans `CHANNEL_CODES` (`promotion-request.ts`). `LIVRAISON` n'est pas proposé à l'écran.
- **Pas de modification ni d'annulation** de promotion dans PRO-01 (aucun endpoint `PUT`/`PATCH`) ; pas de suppression (JNL-02). À prévoir par PRO-04 si besoin (désactivation).
- **Plafond atteint** : le statut ne tient pas compte de `usageLimit` ; un statut « épuisée » éventuel relève de PRO-03.
- **Hypothèses non validées par le PO** (voir PR) : code unique sur toutes les promotions (même terminées), `BUY_X` = « N achetés, 1 offert », `AMOUNT` appliqué une fois au panier, pas de cumul, fin ≥ aujourd'hui.
