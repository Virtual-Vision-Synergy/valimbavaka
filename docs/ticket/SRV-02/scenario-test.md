# Scénarios de test — SRV-02 · Enregistrement d'une prestation

Statut : ☐ tests écrits, à exécuter en CI · Ticket : [#82](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/82) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

**Choix de l'emplacement** : une prestation consomme ses produits dans l'emplacement **salon** (code `TRADE_SHOW`, libellé « Salon », SYS-05), jamais dans la boutique : c'est le « stock salon » de la user story, alimenté le matin par JR-01. Le mouvement est du type « salon » de STK-01 (`TRADE_SHOW`). **Décision PO du 07/11/2026** (COM-01 / SRV-01) : la commission d'une prestation suit le taux du service salon, sinon celui du canal, sinon celui de la vendeuse, sans cumul ; la prestation enregistre donc le service, la vendeuse et le canal (boutique) dont COM-02 a besoin.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `SELLER` (vendeuse « Miora Andriamasy », canal et taux 5,00 %) et `ADMIN` (`admin.test@valimbavaka.mg`) pour enregistrer ; `DELIVERY_DRIVER` pour le refus 403 |
| Données de départ | un article neuf par test (`ServiceRecordApiTestSupport.insertArticle`, unité `LITER`) ; réception de stock dans le salon (`TRADE_SHOW`) ou la boutique (`STORE`) par `StockMovementService.record` ; un service « Soin-<uuid> » (45 min, 35 000 Ar) créé par l'API du catalogue, recette « huile de coco 0,015 L » |
| Tickets à avoir déployés avant | SRV-01 (catalogue, branche de base), COM-01, STK-01, JNL-01, JNL-02 ; migrations `2026_11_08-90`, `-91`, puis `-92` |

## Scénarios issus des critères d'acceptation

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | L'enregistrement déduit automatiquement le stock salon des produits consommés | salon 1 L d'huile, boutique 5 L ; vendeuse enregistre le soin avec 0,020 L | 201, numéro `PRE-xxxx`, prix 35 000 copié, vendeuse « Miora Andriamasy », `locationId` = salon ; salon 0,980 L, boutique 5 L inchangée ; journal `CREATE` `SERVICE_RECORD` | `ServiceRecordIT.recordService_bySellerWithAdjustedProducts_deductsSalonStockAndTracesJournal` | ☐ |
| 1b | Recette du catalogue quand `products` est absent | `"products": null`, recette 0,015 L, salon 0,5 L | produits = recette, salon 0,485 L | `ServiceRecordIT.recordService_withoutProducts_consumesCatalogRecipe` ; `ServiceRecordPolicyTest.draft_withoutProducts_consumesCatalogRecipeFromSalonWithStoreChannel` | ☐ |
| 1c | Quantités ajustées par la vendeuse | recette 0,015 L, envoi 0,020 L | 0,020 L consommé, recette ignorée | `ServiceRecordPolicyTest.draft_withAdjustedProducts_usesSentQuantitiesInsteadOfRecipe` | ☐ |
| 1d | Un mouvement de sortie par produit, au salon, rattaché à la prestation | huile 0,015 + masque 1 | deux mouvements `TRADE_SHOW` de `-0.015` et `-1`, `referenceId` = id de la prestation ; journal contenant `PRE-0042` | `ServiceRecordServiceImplTest.recordService_withProducts_recordsOneNegativeSalonMovementPerProductAndJournal` | ☐ |
| 1e | Prestation sans produit | `"products": []` | aucun mouvement, journal « 0 produit(s) », numéro `PRE-0007`, prix copié | `ServiceRecordServiceImplTest.recordService_withoutProducts_recordsNoMovementButTracesJournal` ; `ServiceRecordPolicyTest.draft_emptyProducts_consumesNothing` | ☐ |
| 1f | Écran : soin, recette pré-remplie, compte connecté | soin visage (huile 0,015 L, masque 1) | « Réalisée par Hanta Rasolofoson » ; envoi `salonServiceId`, `customerName: null`, deux produits | `salon-form.spec.ts` › affiche la prestation au nom du compte connecté ; › envoie le premier soin avec la recette pré-remplie et sans cliente | ☐ |
| 1g | Écran : stock salon affiché par produit | stock boutique 3, salon 0,2 | produit proposé avec `stock: 0.2`, unité « L » | `service-record-mapping.spec.ts` › propose un article avec son unité courte et sa quantité dans le stock salon ; › compte 0 en salon pour un article absent de l'emplacement salon | ☐ |
| 1h | Écran : prestations du jour | prestation à 10:22 | ligne « Soin visage éclat », `10:22`, « Miora Andriamasy », 35 000 Ar ; une prestation de la veille n'apparaît pas | `service-record-mapping.spec.ts` › affiche une prestation à l'heure locale avec le soin, la vendeuse et le prix copiés | ☐ |
| 1i | Données pour COM-02 et JR-04 / SRV-04 | deux prestations (0,015 L et 0,010 L) dans la période | `getSalonConsumption` : 0,025 L pour l'article ; `getServiceRecordsBetween` : les deux prestations ; salon 0,975 L | `ServiceRecordIT.getSalonConsumption_twoServicesInPeriod_sumsQuantityPerArticle` | ☐ |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | POST `/api/v1/service-records` | 401 `UNAUTHENTICATED` | `ServiceRecordIT.recordService_withoutToken_returns401` | ☐ |
| Mauvais rôle | jeton livreur, POST | 403 `FORBIDDEN` | `ServiceRecordIT.recordService_withDeliveryDriverRole_returns403` | ☐ |
| Mass assignment | `"id"`, `"number":"PRE-9999"`, `"price":1`, `"sellerId"` d'un autre dans le corps | ignorés : id et numéro du serveur, prix du catalogue (35 000), vendeuse = compte connecté | `ServiceRecordIT.recordService_forbiddenFieldsInBody_areIgnored` | ☐ |
| Données invalides | quantité `0` | 422 `VALIDATION_FAILED` | `ServiceRecordIT.recordService_zeroQuantity_returns422` | ☐ |
| Service désactivé | service passé `active: false` | 422 `VALIDATION_FAILED`, rien n'est écrit | `ServiceRecordIT.recordService_inactiveService_returns422` ; `ServiceRecordPolicyTest.draft_inactiveService_throwsBusinessException` | ☐ |
| Même article deux fois | huile 0,010 puis 0,005 | 422 avant toute écriture | `ServiceRecordPolicyTest.draft_sameArticleTwice_throwsBusinessException` | ☐ |
| Service inconnu | UUID aléatoire | 404 `NOT_FOUND` | `ServiceRecordIT.recordService_unknownService_returns404` | ☐ |
| Prestation inconnue | GET `/api/v1/service-records/{uuid}` | 404 `NOT_FOUND` | `ServiceRecordIT.getServiceRecordById_unknownId_returns404` ; `ServiceRecordServiceImplTest.getServiceRecordById_unknownId_throwsNotFoundException` | ☐ |
| Stock salon insuffisant | 5 L en boutique, 0 au salon, recette 0,015 L | 409 `CONFLICT`, ni prestation ni mouvement ni journal | `ServiceRecordIT.recordService_insufficientSalonStock_returns409AndWritesNothing` ; `ServiceRecordServiceImplTest.recordService_insufficientSalonStock_propagatesConflictWithoutJournal` | ☐ |
| Journal | prestation enregistrée | ligne `CREATE` `SERVICE_RECORD` « Prestation PRE-xxxx : … » + une ligne `STOCK_MOVEMENT` par produit | `ServiceRecordIT.recordService_bySellerWithAdjustedProducts_deductsSalonStockAndTracesJournal` | ☐ |
| Lecture par la vendeuse | jeton vendeuse, GET `?size=1` | 200, prestation la plus récente en tête | `ServiceRecordIT.getServiceRecords_bySeller_returnsPageMostRecentFirst` | ☐ |
| Session absente côté service | pas d'auteur courant | 401 (`UnauthorizedException`) | `ServiceRecordPolicyTest.draft_withoutSession_throwsUnauthorizedException` | ☐ |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Quantité à plus de 3 décimales saisie à l'écran | `0,0204` | arrondie à `0.02` avant l'envoi (le serveur refuse au-delà de 3 décimales) | `salon-form.spec.ts` › n'envoie pas un produit ramené à zéro et arrondit la quantité à 3 décimales | ☐ |
| Produit ramené à 0 | masque `0` | ligne non envoyée (le serveur refuse une quantité nulle) | `salon-form.spec.ts` › n'envoie pas un produit ramené à zéro et arrondit la quantité à 3 décimales | ☐ |
| Quantité supérieure au stock salon affiché | 0,5 L pour 0,2 L | alerte « Stock salon insuffisant », rien n'est envoyé | `salon-form.spec.ts` › bloque l'envoi quand le stock salon est insuffisant | ☐ |
| Catalogue vide | aucun service actif | message « Aucun service actif au catalogue », envoi impossible | `salon-form.spec.ts` › n'envoie rien quand le catalogue n'a aucun service actif | ☐ |
| Article de la recette désactivé depuis | recette huile + article désactivé | seule l'huile est pré-remplie | `service-record-mapping.spec.ts` › pré-remplit la recette du catalogue sans les articles qui ne sont plus proposés | ☐ |
| Cliente et note : espaces | `"  Hanta  "`, note `"   "` | `Hanta`, note `null` | `ServiceRecordPolicyTest.draft_withoutProducts_consumesCatalogRecipeFromSalonWithStoreChannel` | ☐ |
| Emplacement salon absent du référentiel | code `TRADE_SHOW` introuvable | 422 « L'emplacement de stock salon est introuvable » | `ServiceRecordPolicyTest.draft_salonLocationMissing_throwsBusinessException` | ☐ |
| Recherche d'un emplacement / canal par code | `TRADE_SHOW`, `STORE`, `UNKNOWN` | identifiant du référentiel ; vide si inconnu | `ReferentialServiceImplIT.findStockLocationId_seededCodeAndUnknownCode_returnsIdOrEmpty` ; `ReferentialServiceImplIT.findSalesChannelId_seededCodeAndUnknownCode_returnsIdOrEmpty` | ☐ |
| Hors ligne | saisie sans réseau | POST mis en file avec `Idempotency-Key` (`OutboxService`), toast « Saisie enregistrée hors connexion » ; rejeu = même prestation (SYS-02) | `service-record-service.spec.ts` › enregistre une prestation par POST avec le service, la cliente et les produits consommés | ☐ |
| Deux sorties simultanées du même article au salon | deux prestations en même temps sur le dernier flacon | la seconde reçoit 409 (verrou `pg_advisory_xact_lock` de STK-01) | couvert par STK-01 (`StockMovementPolicyTest`), pas de test propre | ⏭ hors périmètre (STK-01) |
| Bornes de liste | `products` à 21 lignes, cliente à 101 caractères | 422 | validation `ServiceRecordWDto` (`@Size`) ; pas de test dédié | 🔍 code |
| Prestations du jour, fuseau | prestation à 23:59 puis 00:01 heure locale | même jour / jour suivant | `service-record-mapping.spec.ts` › affiche une prestation à l'heure locale avec le soin, la vendeuse et le prix copiés | ☐ |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/salonservice/service/impl/ServiceRecordPolicyTest.java` (7), `ServiceRecordServiceImplTest.java` (4), `feature/salonservice/controller/ServiceRecordIT.java` (12, support `ServiceRecordApiTestSupport.java`), `feature/referential/service/impl/ReferentialServiceImplIT.java` (+2).
- Frontend : `valimbavaka-ui/src/app/features/services/components/salon-form/salon-form.spec.ts` (5), `data-access/service-record-mapping.spec.ts` (4), `data-access/service-record-service.spec.ts` (2) ; `features/demo-pages.a11y.spec.ts` (page « services », inchangé).

## Recette manuelle (🧪)

| # | Étape | Résultat attendu | Statut |
|---|---|---|---|
| R1 | Admin : transférer 1 L d'huile de coco vers le salon (ou réception au salon), puis vendeuse : `/services`, soin visage, cliente « Hanta », enregistrer | toast « Prestation PRE-xxxx « Soin visage éclat » enregistrée : stock salon mis à jour » ; la prestation apparaît dans « Aujourd'hui » ; `/stocks` : salon 0,985 L | 🧪 recette |
| R2 | Même parcours avec une quantité supérieure au stock salon modifiée en base entre-temps | message d'erreur 409 au-dessus du formulaire, rien n'est enregistré | 🧪 recette |
| R3 | Couper le réseau, enregistrer, rétablir | toast « Saisie enregistrée hors connexion », puis la prestation apparaît une seule fois après synchronisation | 🧪 recette |
| R4 | Journal (`/journal`, admin) | une ligne `SERVICE_RECORD` et une ligne `STOCK_MOVEMENT` par produit | 🧪 recette |

## Résultat de l'exécution

- Date : à lancer (CI ou local).
- Environnement : à lancer (CI ou local).
- Anomalies ouvertes : aucune connue.
- Étapes 🧪 restantes : 4.
