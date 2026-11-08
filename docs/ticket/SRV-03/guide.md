# Guide d'intégration — SRV-03 · Historique des prestations du jour

Statut : ✅ rédigé le 2026-11-08 · Ticket : [#83](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/83) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

La vendeuse voit, à droite du formulaire du salon (`/services`), l'encart « Mes prestations du jour » : ses prestations du jour, la plus récente d'abord, chacune avec l'heure, le service, la cliente et les produits consommés (nom, quantité, unité), et le total en Ar. Le filtre « mes prestations » et « du jour » est fait par le serveur (`GET /api/v1/service-records/mine`), sur le compte du jeton et sur le jour de Madagascar. Aucune migration : la lecture réutilise la permission `services.read`.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/service-records/mine` | `services.read` (admin, vendeuse) | `PageQuery` (`page`, `size` ≤ 100, `sortBy` ∈ `createdAt` (défaut, décroissant), `number`, `serviceName`, `price` ; `search` **non appliqué**) + `date` facultative `yyyy-MM-dd` (jour de Madagascar, aujourd'hui par défaut) | 200 `PageResponse<ServiceRecordRDto>` (DTO de SRV-02, inchangé) | 400 `MALFORMED_REQUEST` (date mal formée), 401, 403 |

- Le compte lu est **toujours** celui du jeton (`AuditorAware`) : pas de paramètre `sellerId`, une vendeuse ne peut pas lire l'historique d'une autre par cette route.
- Jour : de minuit à minuit en `Indian/Antananarivo` (`ServiceRecordServiceImpl.BUSINESS_ZONE`, `ServiceRecordServiceImpl.java:48`), bornes `[début, fin[` comparées à `createdAt` (= `performedAt`).
- Champs utiles à l'historique dans `ServiceRecordRDto` : `performedAt` (heure), `serviceName`, `customerName` (`null` si non renseignée), `products[]` (`articleName`, `articleCode`, `quantity` dans l'unité de l'article), `price`, `number`.
- Contrôleur : `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/salonservice/controller/ServiceRecordController.java:69`.

### Services et ports Java réutilisables

- `ServiceRecordService.getMyServiceRecords(PageQuery, @Nullable LocalDate)` (`service/ServiceRecordService.java:29`, implémentation `service/impl/ServiceRecordServiceImpl.java:85`) : `UnauthorizedException` sans compte courant.
- `ServiceRecordRepository.findBySellerBetween(sellerId, from, to, pageable)` (`repository/ServiceRecordRepository.java:46`) : prestations d'une vendeuse sur `[from, to[`, paginées. Réutilisable par COM-02 ou JR-03 pour une vendeuse précise.

### Frontend

- `data-access/service-record-service.ts:34` : `ServiceRecordService.mine(query)` → `GET /api/v1/service-records/mine` (paramètre `date` envoyé seulement s'il est renseigné). Type `ServiceRecordDayQuery` (`PageQuery` + `date?`) dans `data-access/service-record-models.ts`.
- `data-access/service-service.ts` : `SalonService.context()` lit maintenant `mine` (100 plus récentes) au lieu de la liste générale filtrée côté navigateur ; `isSameLocalDay` est supprimée.
- `data-access/service-record-mapping.ts:40` : `toServiceRow(record, units)` construit la ligne affichée (`ServiceRecordRowRDto` : `number`, `treatmentName`, `time` `HH:MM` local, `customer`, `products[]` `{ name, quantity, unit }`, `price`). `units` = unité courte (`u.`, `kg`, `L`) des articles proposés ; un article absent du stock lu a une unité vide.
- `data-access/service-models.ts` : `ServiceRecordRowRDto` perd `staff` (toujours la vendeuse connectée) et gagne `number`, `customer`, `products` ; nouveau `ServiceRecordRowProductRDto`.
- `pages/services-page/services-page.html` : encart « Mes prestations du jour » (une ligne par produit, « Cliente non renseignée », « Aucun produit consommé », « Aucune prestation enregistrée aujourd'hui. »).
- Traductions (`public/i18n/fr.json`, `mg.json`) : `services.today` devient « Mes prestations du jour » ; nouvelles clés `services.noCustomer`, `services.withoutProduct`, `services.noServiceToday`.

### Données

- Aucune migration, aucune permission nouvelle (plage Liquibase 90-99 non utilisée par ce ticket). Requête sur `service_records` filtrée par `seller_id` et `created_at`, couverte par les index `seller_id` et `created_at` de la migration `2026_11_08-92` (SRV-02).

## Règles métier à respecter par les autres tickets

- « Mes prestations » = prestations dont `sellerId` est le compte connecté (SRV-02 : la vendeuse est le compte qui enregistre). Un administrateur qui enregistre une prestation la retrouve dans **son** historique.
- « Du jour » = jour calendaire de Madagascar, pas le jour UTC (une prestation à 01:30 heure locale appartient au jour local, même si elle est encore « la veille » en UTC). Les tickets de journée (JR-03, JR-04, SRV-04, COM-02) doivent calculer leurs bornes de la même façon (`LocalDate.atStartOfDay(Indian/Antananarivo)`).
- L'historique est en lecture seule : aucune annulation ni correction de prestation n'est livrée ici.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/salonservice/`) : `controller/ServiceRecordController.java`, `service/ServiceRecordService.java`, `service/impl/ServiceRecordServiceImpl.java`, `repository/ServiceRecordRepository.java`.

**Frontend** (`valimbavaka-ui/src/app/features/services/`) : `data-access/service-record-service.ts`, `service-record-models.ts`, `service-record-mapping.ts`, `service-service.ts`, `service-models.ts`, `service-fixtures.ts` ; `pages/services-page/services-page.html`, `services-page.ts` ; `public/i18n/fr.json`, `mg.json`.

**Tests** : voir [scénario](scenario-test.md#tests-existants).

## Dépendances

**Ce ticket s'appuie sur** :
- [SRV-02](../SRV-02/guide.md) — `ServiceRecordRDto`, `ServiceRecordRepository`, page `/services` ; branche de base de cette PR.
- [SRV-01](../SRV-01/guide.md) — catalogue des services (nom copié dans la prestation).

**Ces tickets s'appuient sur lui** : aucun (description). JR-03 et COM-02 peuvent réutiliser `findBySellerBetween` et la règle du jour de Madagascar.

## Comment l'utiliser depuis un autre ticket

```ts
// Prestations du compte connecté d'un jour donné (aujourd'hui si `date` est absente)
inject(ServiceRecordService).mine({ page: 1, size: 100, sortBy: 'createdAt', sortDirection: 'DESC', date: '2026-11-08' });
```

## Points d'attention

- **Hypothèse : route dédiée `/mine`** plutôt qu'un filtre `sellerId` sur la liste générale : le serveur décide du compte, ce qui évite qu'une vendeuse lise l'historique d'une autre en changeant un paramètre. La liste générale `GET /api/v1/service-records` (SRV-02) reste visible de tout `services.read` ; la restreindre relèverait d'une décision de Randy.
- **Hypothèse : jour de Madagascar** (`Indian/Antananarivo`) et non UTC. Le journal (JNL-01) filtre, lui, par jour UTC : les deux écrans peuvent différer entre 00:00 et 03:00 heure locale.
- **Hypothèse : pas de recherche** sur cette route (`search` ignoré, documenté dans Swagger) : une journée compte quelques dizaines de prestations et la règle « ≤ 4 paramètres » de la convention limite la requête à `sellerId`, `from`, `to`, `pageable`.
- L'écran lit les 100 prestations les plus récentes du jour ; au-delà, le total affiché est tronqué (la pagination serveur existe, l'écran ne pagine pas).
- L'unité d'un produit vient du stock lu par l'écran (100 articles) : un article désactivé depuis ou hors de ces 100 s'affiche sans unité ; la quantité reste juste.
- Le scénario de SRV-02 cite encore `isSameLocalDay` (ligne « Prestations du jour, fuseau ») : cette fonction est supprimée par ce ticket, le filtre étant fait côté serveur.
- PR empilée sur SRV-02 : à merger après la PR de `feature/SRV-02-enregistrement-prestation`. Conflit possible avec SHO-01 sur `service-models.ts`, `service-service.ts` et `services-page.*` (déjà signalé par SRV-02).
