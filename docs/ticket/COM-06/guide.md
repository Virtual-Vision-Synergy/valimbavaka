# Guide d'intégration — COM-06 · Ajout d'une règle de commission

Statut : ✅ rédigé · Ticket : [#90](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/90) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

L'ajout d'une règle de commission (formulaire, API, journal) et le calcul qui l'utilise sont **déjà livrés par [COM-01](../COM-01/guide.md)** : COM-06 ne change aucun code de production. Il apporte la preuve automatisée du critère « le formulaire enregistre la règle et la rend disponible pour le calcul » : une règle créée, modifiée, désactivée ou réactivée par l'API est prise **dès l'appel suivant** par `CommissionRateResolver` (celui que COM-02 appellera), et une règle saisie hors ligne n'y entre qu'après synchronisation.

## Contrat exposé

Aucun contrat nouveau : tout est celui de COM-01.

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée (`XxxWDto`) | Sortie (`XxxRDto`) | Erreurs |
|---|---|---|---|---|---|
| POST | `/api/v1/commission-rules` | `commission-rules.create` | `CommissionRuleWDto` (`name`, `salesChannelId`, `rate`, `base`) | 201 `CommissionRuleRDto` + `Location` | 400, 401, 403, 409, 422 (voir [guide COM-01](../COM-01/guide.md#contrat-exposé)) |

Les autres endpoints (lecture, PUT, PATCH `/status`) sont décrits dans le guide COM-01.

### Services et ports Java réutilisables

- `feature.commission.service.CommissionRateResolver.resolve(sellerId, salesChannelId, salonServiceId)` (COM-01, `CommissionRateResolver.java:23`) : lit la règle **active** du canal en base à chaque appel (`CommissionRuleRepository.findBySalesChannelIdAndActiveTrue`, `CommissionRateResolverImpl.java:51`). Pas de cache : une règle enregistrée est disponible pour le calcul dès la fin de la transaction d'écriture.

### Frontend

- Route `/commissions/rules/new` (guard `commission-rules.create`), page `features/commissions/pages/commission-rule-form-page` (COM-01). Enregistrement par `CommissionRuleService.create` → file hors ligne `OutboxService.submit` (SYS-07) : en ligne, POST immédiat avec `Idempotency-Key` ; hors ligne, la saisie reste en file, toast `common.queuedSaved`, retour à la liste.

### Données

- Aucune migration (table `commission_rules` et permissions posées par COM-01, `2026_11_08-90-changelog.sql`).
- Journal : ligne `CREATE`, `resourceType = COMMISSION_RULE` (COM-01).

## Règles métier à respecter par les autres tickets

- **Décision PO du 07/11/2026** : service salon > règle active du canal > taux de la vendeuse, sans cumul. Une règle ajoutée sur un canal remplace le taux de la vendeuse pour ce canal, **même s'il est plus bas** (prouvé par `CommissionRuleCalculationIT.createRule_byAdmin_isStoredAndAppliedWithItsRateAndBase` : règle 4,75 % contre vendeuse 5 %).
- La règle porte sa base (`REVENUE` ou `MARGIN`) jusqu'au calcul : COM-02 doit lire `ApplicableCommissionRDto.base()`, pas supposer le CA.
- Une règle ne vaut que pour son canal ; une règle désactivée sort du calcul (retour au taux de la vendeuse) et y revient à la réactivation.
- Le calcul ne connaît que ce que le serveur a enregistré : une règle refusée (422) ou encore dans la file hors ligne n'est pas appliquée.
- Pas de date d'effet : une nouvelle règle (ou un nouveau taux) s'applique aux calculs faits **après** son enregistrement. Le recalcul des ventes passées n'est pas prévu (voir Points d'attention).

## Fichiers concernés

**Créés par COM-06 (tests uniquement)** :
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/commission/controller/CommissionRuleCalculationIT.java` (5 tests : API → résolveur).
- `valimbavaka-ui/src/app/features/commissions/pages/commission-rule-form-page/commission-rule-form-page-offline.spec.ts` (4 tests : formulaire et file hors ligne).

**Code de production utilisé (COM-01, inchangé)** : `feature/commission/controller/CommissionRuleController.java`, `service/impl/CommissionRuleServiceImpl.java`, `service/impl/CommissionRateResolverImpl.java`, `repository/CommissionRuleRepository.java` ; front `features/commissions/pages/commission-rule-form-page/*`, `data-access/commission-rule-service.ts`.

## Dépendances

**Ce ticket s'appuie sur** :
- [COM-01](../COM-01/description.md) · [guide](../COM-01/guide.md) — formulaire, API, résolveur (PR #185, pas encore mergée : cette branche est empilée dessus).
- [JNL-01](../JNL-01/description.md) · [guide](../JNL-01/guide.md) — journal des créations.

**Ces tickets s'appuient sur lui** :
- _(aucun)_ ; [COM-02](../COM-02/description.md) consomme le résolveur de COM-01 et peut s'appuyer sur `CommissionRuleCalculationIT` comme modèle de test d'intégration.

## Comment l'utiliser depuis un autre ticket

```java
// COM-02 : une règle créée par l'administrateur est prise au calcul suivant, avec sa base
ApplicableCommissionRDto applicable = commissionRateResolver.resolve(sellerId, salesChannelId, null);
BigDecimal base = applicable.base() == CommissionBase.MARGIN ? margin : revenue;
```

## Points d'attention

- **Aucun écart fonctionnel trouvé** entre COM-01 et le critère de COM-06 : le ticket est livré sous forme de tests et de documentation (hypothèse : c'est le périmètre voulu, déjà annoncé dans les points d'attention du guide COM-01).
- **Pas d'historique daté des taux** : la règle active au moment du calcul s'applique. Si le métier veut qu'un changement de taux ne s'applique qu'à partir d'une date, ou garder le taux d'une vente déjà commissionnée, c'est à COM-02 de figer le taux dans la commission calculée (recommandé) ou à un ticket dédié d'ajouter une date d'effet. À trancher par le PO.
- Hors ligne : la règle n'est disponible pour le calcul qu'après la synchronisation de la file ; un 409 au rejeu (canal déjà couvert entre-temps) laisse la saisie en `FAILED` dans la file SYS-07 (`outbox-service.ts:197`), visible pour décision de l'administrateur : la règle n'entre alors pas dans le calcul.
- Branche empilée sur `feature/COM-01-regles-commission-canal` : à merger après la PR #185.
