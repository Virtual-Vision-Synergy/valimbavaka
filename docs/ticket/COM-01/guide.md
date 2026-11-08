# Guide d'intégration — COM-01 · Règles de commission par canal

Statut : ✅ rédigé · Ticket : [#85](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/85) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

L'administrateur définit, pour chaque canal de vente, une règle de commission (nom, taux en %, base de calcul) ; un canal a au plus une règle active. Le ticket pose aussi le **résolveur de taux** `CommissionRateResolver` : pour une vendeuse, un canal et éventuellement un service salon, il renvoie le taux applicable selon la **décision PO du 07/11/2026** — le plus précis gagne, sans cumul : service salon, sinon canal, sinon utilisateur. COM-02 s'en sert pour calculer, SRV-01 y branche le niveau « service salon ».

## Contrat exposé

### Endpoints backend (`/api/v1/commission-rules`)

| Méthode | URL | Permission (`@HasPermission`) | Entrée (`XxxWDto`) | Sortie (`XxxRDto`) | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/commission-rules` | `commission-rules.read` | `PageQuery` (`page`, `size`, `search` sur le nom, `sortBy` ∈ `createdAt` (défaut, décroissant), `name`, `rate`) + `salesChannelId`, `active` facultatifs | 200 `PageResponse<CommissionRuleRDto>` | 401, 403 |
| GET | `/api/v1/commission-rules/{id}` | `commission-rules.read` | - | 200 `CommissionRuleRDto` | 404 `NOT_FOUND` |
| POST | `/api/v1/commission-rules` | `commission-rules.create` | `CommissionRuleWDto` | 201 `CommissionRuleRDto` + `Location` | 400 (base inconnue), 409 (nom pris, canal déjà couvert), 422 (données invalides, canal inconnu ou inactif) |
| PUT | `/api/v1/commission-rules/{id}` | `commission-rules.update` | `CommissionRuleWDto` | 200 `CommissionRuleRDto` | 404, 409 (version périmée, nom, canal couvert), 422 |
| PATCH | `/api/v1/commission-rules/{id}/status` | `commission-rules.update` | `CommissionRuleStatusWDto` (`active`) | 200 `CommissionRuleRDto` | 404, 409 (réactivation sur un canal déjà couvert), 422 |

**`CommissionRuleWDto`** : `name` (obligatoire, 100 max), `salesChannelId` (UUID obligatoire, canal actif), `rate` (0 à 100, 2 décimales), `base` (`REVENUE` | `MARGIN`), `version` (facultatif ; périmé → 409). Ni `id` ni statut.
**`CommissionRuleRDto`** : `id`, `name`, `salesChannelId`, `rate`, `base`, `active`, `createdAt`, `updatedAt`, `version`.
Contrôleur : `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/commission/controller/CommissionRuleController.java`.

### Services et ports Java réutilisables

- **`feature.commission.service.CommissionRateResolver`** (`CommissionRateResolver.java:23`) :
  `ApplicableCommissionRDto resolve(UUID sellerId, UUID salesChannelId, @Nullable UUID salonServiceId)`.
  Renvoie `ApplicableCommissionRDto(rate, base, source, ruleId)` ; `source` ∈ `SALON_SERVICE`, `SALES_CHANNEL`, `SELLER`, `NONE` (`CommissionRateSource`). Implémentation : `CommissionRateResolverImpl.java:46`. Lève `NotFoundException` seulement si le niveau vendeuse est atteint et que le compte n'existe pas.
- **Point d'extension SRV-01 : `feature.commission.service.SalonServiceCommissionRates`** (`SalonServiceCommissionRates.java:14`) :
  `Optional<BigDecimal> findRate(UUID salonServiceId)`. La feature services fournit **un** bean Spring qui implémente ce port ; le résolveur le récupère par `ObjectProvider` (`CommissionRateResolverImpl.java:63`). Sans bean (avant SRV-01) ou si le service n'a pas de taux (`Optional.empty()`), le niveau salon est sauté. Le sens de dépendance est `services → commission` (la feature services implémente le port), jamais l'inverse : pas de cycle (ArchUnit `no_cycles_between_features`).
- `feature.commission.service.CommissionRuleService` : `getRules`, `getRuleById`, `createRule`, `updateRule`, `setActive` (règles : `CommissionRuleServiceImpl.java:75`, `:90`, `:113` ; unicité par canal : `:144`).

### Frontend (`features/commissions`)

- Routes (`commissions.routes.ts`, sous `/commissions`) : `rules` (guard `commission-rules.read`), `rules/new` (`commission-rules.create`), `rules/:id/edit` (`commission-rules.update`). La page « Mes commissions » (`''`) reste la maquette de COM-02/COM-03.
- Menu : section Administration, entrée `menu.item.commissionRules` → `/commissions/rules` (`core/layout/menu.ts`).
- `data-access/commission-rule-service.ts` : `list`, `getById`, `create`, `update`, `setActive` ; écritures par la file hors ligne `OutboxService` (SYS-07). Types dans `data-access/commission-rule-models.ts` (`CommissionRuleRDto`, `CommissionRuleWDto`, `CommissionBase`, `COMMISSION_BASES`, bornes du taux).
- Pages : `pages/commission-rule-list-page` (recherche, pagination, activation confirmée), `pages/commission-rule-form-page` (nom, canal, taux, base). Canaux lus par `features/referential/data-access/referential-service.ts`.
- Traductions : clés `commissionRule.*` dans `public/i18n/fr.json` et `mg.json`.

### Données

- Table `commission_rules` (`name`, `sales_channel_id` FK `sales_channels`, `rate NUMERIC(5,2)` `CHECK` 0-100, `calculation_base` `CHECK IN ('REVENUE','MARGIN')`, `is_active`, audit, `version`) ; index uniques `uq_commission_rules_name` (sur `lower(name)`) et `uq_commission_rules_sales_channel_id_active` (partiel, `WHERE is_active`). Migration `valimbavaka-core/src/main/resources/db/changelog/changes/2026_11_08-90-changelog.sql` (changesets 1 à 3, rollback fourni).
- Permissions `commission-rules.read`, `commission-rules.create`, `commission-rules.update`, réservées à `ADMIN` (même migration). `commissions.read` (vendeuse) n'y donne pas accès.
- Journal : `resourceType = COMMISSION_RULE` ; `CREATE` « Création de la règle de commission <nom> (<taux> %) », `UPDATE` « Modification… », « Activation… », « Désactivation… ». Rien n'est tracé si rien ne change.

## Règles métier à respecter par les autres tickets

- **Ordre de priorité (décision PO du 07/11/2026)** : service salon > canal > utilisateur, **sans cumul**. Ne jamais additionner deux taux ; toujours passer par `CommissionRateResolver` plutôt que relire `users.commission_rate` ou `commission_rules` à la main.
- Une règle de canal **remplace** le taux propre de la vendeuse pour toutes les ventes de ce canal, même si la vendeuse a un taux plus élevé.
- **Base de calcul** : `REVENUE` = chiffre d'affaires de la vente après remises ; `MARGIN` = marge brute (CA − coût d'achat). Le taux d'un service salon et le taux de la vendeuse s'appliquent au chiffre d'affaires (`REVENUE`). Le calcul du montant (arrondi, remises, retours) appartient à COM-02.
- Une seule règle **active** par canal : pour changer de taux, modifier la règle (PUT) ou la désactiver puis en créer une autre. Une règle ne se supprime pas (JNL-02).
- Le taux renvoyé est en pourcentage (`3.50` = 3,5 %) ; `NONE` renvoie `0.00`.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/commission/`) :
- `controller/CommissionRuleController.java`
- `domain/CommissionRule.java`, `CommissionRuleDetails.java`, `CommissionBase.java`, `CommissionRateSource.java`
- `dto/CommissionRuleWDto.java`, `CommissionRuleRDto.java`, `CommissionRuleStatusWDto.java`, `ApplicableCommissionRDto.java`
- `mapper/CommissionRuleMapper.java`, `repository/CommissionRuleRepository.java`
- `service/CommissionRuleService.java`, `CommissionRateResolver.java`, `SalonServiceCommissionRates.java`
- `service/impl/CommissionRuleServiceImpl.java`, `CommissionRateResolverImpl.java`

**Migration** : `valimbavaka-core/src/main/resources/db/changelog/changes/2026_11_08-90-changelog.sql` (déclarée dans `db.changelog-master.yaml`).

**Frontend** (`valimbavaka-ui/src/app/`) : `features/commissions/commissions.routes.ts`, `features/commissions/data-access/commission-rule-models.ts`, `commission-rule-service.ts`, `features/commissions/pages/commission-rule-list-page/*`, `features/commissions/pages/commission-rule-form-page/*`, `core/layout/menu.ts`, `core/auth/auth-testing.ts` (permissions de test), `public/i18n/fr.json`, `public/i18n/mg.json`.

**Tests** : voir [scénario](scenario-test.md#tests-existants).

## Dépendances

**Ce ticket s'appuie sur** :
- [AUTH-03](../AUTH-03/description.md) · [guide](../AUTH-03/guide.md) — taux propre de la vendeuse (`UserService.getUserById(...).commissionRate()`), dernier niveau du résolveur.
- [JNL-01](../JNL-01/description.md) · [guide](../JNL-01/guide.md) — `JournalService.record`.
- [SYS-05](../SYS-05/description.md) · [guide](../SYS-05/guide.md) — canaux de vente (`ReferentialService.isActiveSalesChannel`).

**Ces tickets s'appuient sur lui** :
- [COM-02](../COM-02/description.md) · [guide](../COM-02/guide.md) — calcul automatique : appelle `CommissionRateResolver.resolve` pour chaque vente.
- [COM-06](../COM-06/description.md) · [guide](../COM-06/guide.md) — ajout d'une règle : l'API et le formulaire existent déjà ici (voir « Points d'attention »).
- [SRV-01](../SRV-01/description.md) — fournit le bean `SalonServiceCommissionRates`.

## Comment l'utiliser depuis un autre ticket

```java
// COM-02 : taux applicable à une vente (salonServiceId = null pour une vente de produits)
ApplicableCommissionRDto applicable = commissionRateResolver.resolve(sale.sellerId(), sale.salesChannelId(), null);
BigDecimal base = applicable.base() == CommissionBase.MARGIN ? margin : revenue;
BigDecimal amount = base.multiply(applicable.rate()).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

// SRV-01 : brancher le niveau « service salon » (dans feature/services/service/impl)
@Component
class SalonServiceCommissionRatesImpl implements SalonServiceCommissionRates {
    public Optional<BigDecimal> findRate(UUID salonServiceId) { /* lire le taux du service */ }
}
```

## Points d'attention

- **Décision PO du 07/11/2026** appliquée (service salon > canal > utilisateur, sans cumul) ; la question client du ticket est donc tranchée pour COM-01 et SRV-01.
- **Hypothèse : base de calcul** = `REVENUE` (CA après remises) ou `MARGIN` (marge brute). Liste à confirmer par Randy ; ajouter une valeur demande une migration (contrainte `ck_commission_rules_calculation_base`) et l'enum `CommissionBase`.
- **Hypothèse : une seule règle active par canal** (index unique partiel). Pas de taux par vendeuse *et* par canal combinés, pas de période de validité : si le métier veut un historique daté des taux, c'est COM-06 ou un ticket dédié.
- **Hypothèse : périmètre COM-01 / COM-06**. Le critère de COM-01 (« le formulaire couvre nom, canal, taux et base ») impose un formulaire qui enregistre ; la création, la modification et l'activation sont donc livrées ici. COM-06 (« enregistre la règle et la rend disponible pour le calcul ») se limite à vérifier en recette que la règle créée est prise par le résolveur, ou à ce que le PO voudra y ajouter.
- **SRV-01** : la maquette « Mes commissions » présente les prestations salon « au forfait » ; la décision PO parle de **taux**. Le port `SalonServiceCommissionRates` renvoie un taux (%). Si SRV-01 retient un montant fixe, il faudra faire évoluer `ApplicableCommissionRDto` (montant forfaitaire) avant COM-02.
- Si plusieurs beans `SalonServiceCommissionRates` existent, `ObjectProvider.getIfAvailable()` lève une erreur : en fournir un seul.
- Le libellé du canal est lu dans le référentiel actif : une règle sur un canal désactivé affiche « Canal inactif » (`commission-rule-list-page.ts`). Le résolveur, lui, applique encore la règle active d'un canal désactivé (aucune vente ne devrait y arriver).
- Le taux propre de la vendeuse (AUTH-03) reste obligatoire sur sa fiche : il sert de dernier recours.
- La page « Règles de commission » n'est pas dans `demo-pages.a11y.spec.ts` (réservé aux pages de démonstration) : vérifier l'accessibilité en recette.
