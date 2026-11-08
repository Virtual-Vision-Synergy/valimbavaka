# Guide d'intégration — SRV-02 · Enregistrement d'une prestation

Statut : ✅ rédigé le 2026-11-08 — tests exécutés en CI, tous verts (voir « Résultat de l'exécution » du [scénario](scenario-test.md)) · Ticket : [#82](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/82) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

La vendeuse (ou l'administrateur) enregistre une **prestation** : un service actif du catalogue (SRV-01), une cliente facultative et les produits consommés (recette du service pré-remplie, ajustable). Dans une seule transaction, le serveur crée la prestation `PRE-xxxx` (copie du nom, de la durée et du prix du service), enregistre **un mouvement de sortie par produit dans l'emplacement salon** (STK-01) et trace l'opération au journal. La prestation porte tout ce dont la commission (COM-02) et la clôture (JR-04, SRV-04) ont besoin : service, vendeuse, canal, prix et quantités consommées.

## Contrat exposé

### Endpoints backend (`/api/v1/service-records`)

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/service-records` | `services.read` (admin, vendeuse) | `PageQuery` (`page`, `size` ≤ 100, `search` sur numéro, service, cliente et vendeuse, `sortBy` ∈ `createdAt` (défaut, décroissant), `number`, `serviceName`, `price`) | 200 `PageResponse<ServiceRecordRDto>` | 401, 403, 422 |
| GET | `/api/v1/service-records/{id}` | `services.read` | — | 200 `ServiceRecordRDto` | 401, 403, 404 `NOT_FOUND` |
| POST | `/api/v1/service-records` | `service-records.create` (admin, vendeuse) | `ServiceRecordWDto` | 201 `ServiceRecordRDto` + `Location` | 401, 403, 404 (service ou article inconnu), 409 (stock salon insuffisant), 422 (données invalides, service désactivé, article listé deux fois) |

- **`ServiceRecordWDto`** : `salonServiceId` (obligatoire), `customerName` (facultatif, 100 max, espaces retirés, vide = `null`), `products` (facultatif, 20 lignes max de `ConsumedProductWDto(articleId, quantity)` de SRV-01 : quantité > 0, 3 décimales ; **`null` = recette du catalogue**, `[]` = aucun produit), `note` (facultative, 255 max). Ni prix, ni vendeuse, ni canal, ni emplacement, ni numéro : tout champ de ce genre envoyé est ignoré (`ServiceRecordIT.recordService_forbiddenFieldsInBody_areIgnored`).
- **`ServiceRecordRDto`** : `id`, `number` (`PRE-0001`…), `salonServiceId`, `serviceName`, `durationMinutes`, `price` (Ar, copiés du catalogue au moment de l'enregistrement), `customerName`, `sellerId`, `sellerName` (compte connecté, nom figé), `salesChannelId` (canal boutique), `locationId` (emplacement salon), `products` (`ServiceRecordProductRDto(articleId, articleCode, articleName, quantity)`, code et nom copiés, quantité positive dans l'unité de l'article), `note`, `performedAt` (= `createdAt`).
- Pas de `PUT`, `PATCH` ni `DELETE` : une prestation validée ne se modifie pas (JNL-02). Une correction passera par une annulation à créer (voir « Points d'attention »).
- Contrôleur : `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/salonservice/controller/ServiceRecordController.java`.

### Services et ports Java réutilisables

`feature.salonservice.service.ServiceRecordService` (implémentation `service/impl/ServiceRecordServiceImpl.java`) :

| Méthode | Pour qui | Rôle |
|---|---|---|
| `getServiceRecords(PageQuery)`, `getServiceRecordById(UUID)` | contrôleur, SRV-03 | lecture paginée / unitaire |
| `recordService(ServiceRecordWDto)` (`ServiceRecordServiceImpl.java:79`) | contrôleur | règles (`ServiceRecordPolicy.draft`, `:60`), prestation, mouvements de stock, journal, tout ou rien |
| `getServiceRecordsBetween(Instant from, Instant to)` (`:106`) | **COM-02**, SRV-03, JR-03 | prestations de `[from, to[`, plus anciennes d'abord |
| `getSalonConsumption(Instant from, Instant to)` (`:113`) | **SRV-04**, **JR-04** | `SalonConsumptionRDto(articleId, quantity)` : quantité totale consommée par article sur `[from, to[` (requête `ServiceRecordRepository.consumptionBetween`) |

Ajouts à d'autres features : `ReferentialService.findSalesChannelId(String code)` et `findStockLocationId(String code)` (`feature/referential/service/ReferentialService.java:31`, `:34`), identifiant d'un canal ou d'un emplacement par son code, actif ou non (repositories `findByCode`).

### Frontend (`features/services`)

- Route `/services` inchangée (permission `services.read`, toujours `underConstruction` car l'onglet shooting reste en démonstration).
- `data-access/service-record-service.ts` (`ServiceRecordService`) : `list(query)`, `getById(id)`, `create(body)` ; l'écriture passe par la file hors ligne (`OutboxService`, `Idempotency-Key`). Types dans `data-access/service-record-models.ts` (`ServiceRecordRDto`, `ServiceRecordWDto`, `ServiceRecordProductRDto`, `SALON_LOCATION_CODE`, bornes).
- `data-access/service-record-mapping.ts` : `toSalonProduct` (article actif + quantité au salon + unité courte `u.`/`kg`/`L`), `toTreatment` (service du catalogue → soin, recette limitée aux articles proposés), `toServiceRow` (ligne « Aujourd'hui »), `isSameLocalDay`.
- `data-access/service-service.ts` (`SalonService.context()`) : lit en parallèle les services actifs (`SalonCatalogService.list`), le stock (`GET /api/v1/stocks`, 100 articles) et les 100 prestations les plus récentes, garde celles du jour ; les sorties shooting restent la démonstration (`SERVICES_FIXTURE.shootings`).
- `components/salon-form` : le soin par défaut est le premier du catalogue ; « Réalisée par » affiche le compte connecté (le sélecteur de personnel de la maquette est retiré) ; émet un `ServiceRecordWDto` (produits à 0 retirés, quantités arrondies à 3 décimales). `pages/services-page` : toast « Prestation PRE-xxxx … enregistrée », `common.queuedSaved` hors ligne, erreur serveur affichée au-dessus du formulaire, rechargement des prestations du jour et du stock.
- Renommage dans `data-access/service-models.ts` : la ligne d'affichage de la maquette s'appelle `ServiceRecordRowRDto` (les noms `ServiceRecordRDto` / `ServiceRecordWDto` désignent maintenant les DTO du backend) ; `staff` retiré de `ServicesContextRDto`.
- Traductions : `services.noTreatment` ajoutée, `services.registeredToast` mise à jour, `services.chooseStaff` et `services.staffRequired` retirées (`fr.json`, `mg.json`). Permission de test `service-records.create` ajoutée à `ADMIN` et `SELLER` (`core/auth/auth-testing.ts`).

### Données

- Migration `valimbavaka-core/src/main/resources/db/changelog/changes/2026_11_08-92-changelog.sql` (4 changesets, rollback fourni), déclarée après `2026_11_08-91` (SRV-01) :
  - séquence `service_record_number_seq` ;
  - table `service_records` (`number` unique, `salon_service_id` → `salon_services`, `service_name`, `duration_minutes` `CHECK` 1-600, `price NUMERIC(14,2)` `CHECK ≥ 0`, `customer_name`, `seller_id` → `users`, `seller_name`, `sales_channel_id` → `sales_channels`, `location_id` → `stock_locations`, `note`, audit, `version`) ; index sur `created_at` (décroissant), `salon_service_id`, `seller_id` ;
  - table `service_record_products` (clé `service_record_id` + `line_no`, `article_id` → `articles`, `article_code`, `article_name`, `quantity NUMERIC(12,3)` `CHECK > 0`, un article une fois par prestation) ;
  - permission `service-records.create` donnée à `ADMIN` et `SELLER` (la lecture réutilise `services.read`).
- Stock : un mouvement `TRADE_SHOW` de `−quantité` par produit, sur l'emplacement salon, `referenceId` = id de la prestation.
- Journal : ressource `SERVICE_RECORD`, action `CREATE`, libellé « Prestation PRE-0042 : Soin visage éclat (35000.00 Ar), 2 produit(s) consommé(s) au salon » ; plus une ligne `STOCK_MOVEMENT` par produit (STK-01).

## Règles métier à respecter par les autres tickets

- **Emplacement : le salon.** Une prestation consomme toujours dans l'emplacement de code `TRADE_SHOW` (libellé « Salon », SYS-05), jamais dans la boutique (`ServiceRecordPolicy.SALON_LOCATION_CODE`, `ServiceRecordPolicy.java:35`). Le stock salon doit donc être approvisionné (transfert du matin JR-01, ou réception) avant les prestations ; sinon 409.
- **Type de mouvement : « salon » de STK-01**, codé `TRADE_SHOW` (`ServiceRecordServiceImpl.SALON_MOVEMENT`, `:39`). Une prestation = un mouvement négatif par produit, dans la même transaction que la prestation ; si un produit manque (409) ou est inconnu (404), **rien** n'est écrit.
- **Tout est copié à l'enregistrement** : nom, durée et prix du service, code et nom des articles, nom de la vendeuse. Un changement ultérieur du catalogue ne modifie pas les prestations passées. Lire le prix d'une prestation dans `ServiceRecordRDto.price`, jamais dans le catalogue.
- **Vendeuse = compte connecté** (`sellerId`) : on ne saisit pas « qui a réalisé » ; un administrateur qui enregistre devient la « vendeuse » de la prestation.
- **COM-02 (commission, décision PO du 07/11/2026 : service salon > canal > utilisateur, sans cumul)** : pour chaque prestation, appeler `CommissionRateResolver.resolve(record.sellerId(), record.salesChannelId(), record.salonServiceId())` puis appliquer le taux à `record.price()` (chiffre d'affaires ; le taux d'un service et celui de la vendeuse sont en base `REVENUE`). `salesChannelId` est le canal **boutique** (`STORE`) : sans taux propre au service, c'est la règle de la boutique, puis le taux de la vendeuse qui s'applique. Pour une période : `getServiceRecordsBetween(from, to)`.
- **JR-04 / SRV-04 (clôture)** : la composante « salon » d'un article sur la journée = `getSalonConsumption(début, fin)` (quantité positive). Elle est égale à l'opposé de la somme des mouvements `TRADE_SHOW` dont `referenceId` est une prestation de la période. Ne pas sommer **tous** les mouvements `TRADE_SHOW` : ce type accepte aussi les entrées (Sign `ANY`) si un autre ticket l'utilise pour approvisionner le salon. La valeur (Ar) se calcule avec le coût des articles (STK-03 / `ArticleService`). Pour SRV-04, « total consommé en salon » = somme de ces quantités (ou de leur valeur), c'est exactement ce que JR-04 doit afficher.
- **SRV-03 (historique du jour)** : `GET /api/v1/service-records` (tri `createdAt` décroissant) ou `getServiceRecordsBetween` ; chaque prestation porte heure (`performedAt`), service, cliente et produits consommés. Le filtre « mes prestations » / « du jour » côté serveur reste à ajouter par SRV-03.
- **JR-03 (temps réel)** : relire `GET /api/v1/service-records` (ou `getServiceRecordsBetween`) ; aucun évènement poussé n'existe.
- Seuls les services **actifs** s'enregistrent (422 sinon) ; un service désactivé garde ses prestations passées.
- Référencer une prestation par son UUID ; le numéro `PRE-xxxx` sert à l'affichage et au journal.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/salonservice/`) :
- `controller/ServiceRecordController.java`
- `domain/ServiceRecord.java`, `ServiceRecordProduct.java`, `ServiceRecordDraft.java`
- `dto/ServiceRecordWDto.java`, `ServiceRecordRDto.java`, `ServiceRecordProductRDto.java`, `SalonConsumptionRDto.java` (réutilise `ConsumedProductWDto` de SRV-01)
- `mapper/ServiceRecordMapper.java`, `repository/ServiceRecordRepository.java`
- `service/ServiceRecordService.java`, `service/impl/ServiceRecordServiceImpl.java`, `service/impl/ServiceRecordPolicy.java`

**Modifiés ailleurs** : `feature/referential/service/ReferentialService.java` (+ `impl/ReferentialServiceImpl.java`), `feature/referential/repository/SalesChannelRepository.java`, `StockLocationRepository.java` (`findByCode`).

**Migration** : `valimbavaka-core/src/main/resources/db/changelog/changes/2026_11_08-92-changelog.sql` (+ `db.changelog-master.yaml`).

**Frontend** (`valimbavaka-ui/src/app/`) : `features/services/data-access/service-record-models.ts`, `service-record-service.ts`, `service-record-mapping.ts`, `service-service.ts`, `service-models.ts`, `service-fixtures.ts` ; `features/services/components/salon-form/*` ; `features/services/pages/services-page/*` ; `features/services/services.routes.ts` (commentaire) ; `core/auth/auth-testing.ts` ; `features/demo-pages.a11y.spec.ts` ; `public/i18n/fr.json`, `mg.json`.

**Tests** : voir [scénario](scenario-test.md#tests-existants).

## Dépendances

**Ce ticket s'appuie sur** :
- [SRV-01](../SRV-01/guide.md) — `SalonCatalogService.getServiceById` (service actif, prix, durée, recette), `ConsumedProductWDto` ; branche de base de cette PR.
- [STK-01](../STK-01/guide.md) — `StockMovementService.record` (contrôle du solde, verrou, journal du mouvement).
- [COM-01](../COM-01/guide.md) — résolveur `CommissionRateResolver`, utilisé par COM-02 avec les champs de la prestation.
- [JNL-01](../JNL-01/guide.md) / [JNL-02](../JNL-02/guide.md) — `JournalService.record`, `OperationRepository` (pas de suppression).
- [SYS-05](../SYS-05/guide.md) — emplacement « Salon » (`TRADE_SHOW`) et canal « Boutique » (`STORE`).

**Ces tickets s'appuient sur lui** :
- [COM-02](../COM-02/description.md) — commission des prestations (`getServiceRecordsBetween`, résolveur).
- [SRV-03](../SRV-03/description.md) — historique des prestations du jour.
- [SRV-04](../SRV-04/description.md) et [JR-04](../JR-04/description.md) — consommation salon dans la clôture (`getSalonConsumption`).
- [JR-03](../JR-03/description.md) — suivi temps réel de la journée.

## Comment l'utiliser depuis un autre ticket

```java
// COM-02 : commission des prestations d'une journée
for (ServiceRecordRDto record : serviceRecordService.getServiceRecordsBetween(dayStart, dayEnd)) {
    var rate = commissionRateResolver.resolve(record.sellerId(), record.salesChannelId(), record.salonServiceId());
    var amount = record.price().multiply(rate.rate()).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
}

// JR-04 / SRV-04 : colonne « salon » de la réconciliation
Map<UUID, BigDecimal> salon = serviceRecordService.getSalonConsumption(dayStart, dayEnd).stream()
        .collect(Collectors.toMap(SalonConsumptionRDto::articleId, SalonConsumptionRDto::quantity));
```

```ts
// Front : enregistrer une prestation avec la recette du catalogue
inject(ServiceRecordService).create({ salonServiceId, customerName: 'Hanta', products: null });
```

## Points d'attention

- **Hypothèse : emplacement salon** (`TRADE_SHOW`, libellé « Salon ») plutôt que boutique : c'est le « stock salon » de la user story et de JR-01 (sortie du matin vers boutique/livraison/salon). Le code `TRADE_SHOW` (« salon professionnel ») vient d'une traduction de « Salon » (`2026_10_16-01`) ; le renommer en `SALON` serait plus clair mais touche STK-01, STK-03 et le front des stocks (ticket à ouvrir si Randy le souhaite).
- **Hypothèse : type de mouvement `TRADE_SHOW`** = type « salon » de la liste de STK-01 ; aucune migration du type n'a été nécessaire. Si un ticket l'utilise aussi pour approvisionner le salon, JR-04 doit passer par `getSalonConsumption` (voir « Règles métier »).
- **Hypothèse : canal boutique (`STORE`)** pour la commission d'une prestation (le résolveur de COM-01 exige un canal). Si le métier veut un canal « Salon » distinct, il faudra l'ajouter au référentiel et changer `ServiceRecordPolicy.SALON_CHANNEL_CODE` (`ServiceRecordPolicy.java:38`) ; les prestations déjà enregistrées garderont la boutique.
- **Hypothèse : prix non modifiable** à l'enregistrement (copie du catalogue) ; pas de remise sur une prestation. À ouvrir si besoin.
- **Hypothèse : la vendeuse est le compte connecté** ; la maquette proposait un choix « Réalisée par » (liste du personnel), retiré car il permettait d'attribuer la commission à une autre vendeuse. Si plusieurs personnes partagent un poste, il faudra un champ `performedById` contrôlé côté serveur (décision Randy).
- **Stock salon insuffisant = 409** : la prestation est refusée tant que le salon n'a pas été approvisionné (règle STK-01 « jamais sous zéro »). Avant JR-01 / TRF, seule une réception directe au salon permet de tester.
- **Pas d'annulation** : une prestation saisie par erreur ne se corrige pas encore (ni endpoint `CANCEL`, ni contre-passation du stock). À prévoir dans SRV-03 ou un ticket dédié avec `ReversibleOperationService` (JNL-02) et des mouvements `TRADE_SHOW` positifs.
- La liste `GET /api/v1/service-records` est visible de toute personne ayant `services.read` (toutes les vendeuses) ; le filtre « mes prestations » relève de SRV-03.
- L'écran lit au plus 100 articles (stock) et 100 prestations récentes : au-delà, les prestations du jour peuvent être tronquées un jour très chargé (SRV-03 ajoutera un filtre de date serveur).
- Un article de la recette désactivé depuis n'est plus pré-rempli à l'écran (l'API, elle, l'accepte si `products` est `null`).
- Conflit attendu avec la PR de SHO-01 (`feature/SHO-01-enregistrement-sortie-shooting`) sur `service-models.ts`, `service-service.ts` et `services-page.*` : les deux PR modifient la page « Salon & shooting ». À résoudre au second merge (garder les deux branchements).
- La page `/services` reste dans `demo-pages.a11y.spec.ts` : les appels API y reçoivent une page vide (`answerApiCalls`, même aide que dans les PR de SHO-01 et PRD-01), donc le formulaire réel est contrôlé sans service ni produit ; un formulaire rempli se vérifie en recette.
- PR empilée sur SRV-01 (lui-même sur COM-01) : à merger après la PR de `feature/SRV-01-catalogue-services-salon`.
