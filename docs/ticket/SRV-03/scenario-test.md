# Scénarios de test — SRV-03 · Historique des prestations du jour

Statut : ☐ tests écrits, à lancer (CI) · Ticket : [#83](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/83) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `SELLER` (Miora, une autre vendeuse), `ADMIN` ; `DELIVERY_DRIVER` pour le 403 |
| Données de départ | un service actif « Soin visage éclat » (35 000 Ar) dont la recette contient 0,015 L d'huile de coco ; 1 L d'huile de coco au salon (`TRADE_SHOW`) |
| Tickets à avoir déployés avant | SRV-01, SRV-02 (branche de base de cette PR) |

## Scénarios issus des critères d'acceptation

| # | Critère d'acceptation | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Chaque prestation affiche heure, service, cliente et produits consommés. | Étant donné que Miora a enregistré aujourd'hui « Soin visage éclat » pour Hanta (sans produit) puis pour Voahirana (0,020 L d'huile de coco), et qu'une autre vendeuse a enregistré une prestation pour Lova ; quand Miora appelle `GET /api/v1/service-records/mine` ; alors elle reçoit ses 2 prestations, la plus récente d'abord | 200, `totalCount` = 2 ; `items[0]` : cliente Voahirana, `serviceName`, `performedAt`, produit huile de coco 0,02 ; `items[1]` : Hanta, `products` vide ; la prestation de Lova n'apparaît pas | `ServiceRecordHistoryIT.getMyServiceRecords_twoSellers_returnsOnlyOwnRecordsOfTodayMostRecentFirst` | ☐ |
| 1 | (écran) | Étant donné deux prestations du jour (10:22 soin visage pour Hanta avec huile de coco 0,015 L et masque 1 u. ; 09:05 manucure sans cliente ni produit) ; quand la vendeuse ouvre `/services` ; alors l'encart « Mes prestations du jour » liste chaque prestation | « Soin visage éclat », « 10:22 · Hanta », « Huile de coco vierge · 0.015 L », « Masque argile rose · 1 u. » ; « 09:05 · Cliente non renseignée », « Aucun produit consommé » | `services-page.spec.ts` › affiche l'heure, le service, la cliente et les produits consommés de chaque prestation | ☐ |
| 1 | (correspondance API → écran) | Une prestation `PRE-0001` à 10:22 heure locale, cliente Hanta, deux produits dont un article retiré de la liste des articles proposés | heure `10:22`, cliente, produits avec nom copié, quantité et unité courte (`L`), unité vide pour l'article inconnu | `service-record-mapping.spec.ts` › affiche une prestation à l'heure locale avec le soin, la cliente et les produits consommés | ☐ |
| 1 | (jour métier) | Horloge à 22:30 UTC le 08/11 (01:30 le 09/11 à Madagascar) ; sans `date` | le serveur lit le jour du 09/11 heure de Madagascar : `[2026-11-08T21:00Z, 2026-11-09T21:00Z[`, pour le compte connecté | `ServiceRecordServiceImplTest.getMyServiceRecords_withoutDate_readsCurrentSellerOnTodayInMadagascar` | ☐ |
| 1 | (jour choisi, tri) | `date=2026-11-01` | bornes `[2026-10-31T21:00Z, 2026-11-01T21:00Z[`, tri `createdAt` décroissant | `ServiceRecordServiceImplTest.getMyServiceRecords_withDate_readsThatBusinessDayMostRecentFirst` | ☐ |

## Sécurité et erreurs

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Appel sans jeton | 401 `UNAUTHENTICATED` | `ServiceRecordHistoryIT.getMyServiceRecords_withoutToken_returns401` | ☐ |
| Appel avec un mauvais rôle (livreur, sans `services.read`) | 403 `FORBIDDEN` | `ServiceRecordHistoryIT.getMyServiceRecords_withDeliveryDriverRole_returns403` | ☐ |
| Date mal formée (`08/11/2026`) | 400 `MALFORMED_REQUEST` | `ServiceRecordHistoryIT.getMyServiceRecords_malformedDate_returns400` | ☐ |
| Pas de compte courant (appel hors session) | `UnauthorizedException` (401), aucune lecture en base | `ServiceRecordServiceImplTest.getMyServiceRecords_withoutSession_throwsUnauthorizedException` | ☐ |
| Voir les prestations d'une autre vendeuse | impossible par `/mine` : le serveur filtre sur le compte du jeton, aucun paramètre `sellerId` n'est accepté | `ServiceRecordHistoryIT.getMyServiceRecords_twoSellers_returnsOnlyOwnRecordsOfTodayMostRecentFirst` | ☐ |

Pas d'écriture dans ce ticket : ni mass assignment, ni 404, 409 ou 422 propres.

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test auto | OK |
|---|---|---|---|---|
| Aucune prestation sur le jour demandé | `date=2000-01-01` | 200, page vide (`totalCount` = 0) | `ServiceRecordHistoryIT.getMyServiceRecords_anotherDay_returnsEmptyPage` | ☐ |
| Aucune prestation aujourd'hui (écran) | première connexion du matin | « Aucune prestation enregistrée aujourd'hui. » | `services-page.spec.ts` › annonce qu'aucune prestation n'a encore été enregistrée aujourd'hui | ☐ |
| Appel de l'écran | sans date, 100 premières, tri `createdAt` décroissant | `GET /api/v1/service-records/mine?page=1&size=100&sortBy=createdAt&sortDirection=DESC`, sans `date` | `service-record-service.spec.ts` › lit les prestations du jour du compte connecté sans date : le serveur prend aujourd'hui | ☐ |
| Jour donné | `date=2026-11-01` | paramètre `date` transmis tel quel | `service-record-service.spec.ts` › lit les prestations du compte connecté d'un jour donné | ☐ |
| Minuit à Madagascar | prestation à 23:59 puis 00:01 heure locale | jours différents (bornes calculées en `Indian/Antananarivo`, pas en UTC) | `ServiceRecordServiceImplTest.getMyServiceRecords_withoutDate_readsCurrentSellerOnTodayInMadagascar` | ☐ |
| Plus de 100 prestations dans la journée | journée très chargée | l'écran n'affiche que les 100 plus récentes (pagination serveur disponible) | pas de test dédié | 🔍 code |
| Hors ligne | lecture sans réseau | l'encart passe en erreur avec « Réessayer » (comportement commun `rxResource` / `vvs-list-state`) ; une prestation saisie hors ligne n'apparaît qu'après synchronisation | pas de test dédié | 🧪 recette |
| Accessibilité | page `/services` réelle | aucune violation axe | `demo-pages.a11y.spec.ts` › services should have no detectable accessibility violation | ☐ |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/salonservice/controller/ServiceRecordHistoryIT.java` (5, nouveau), `feature/salonservice/service/impl/ServiceRecordServiceImplTest.java` (+3).
- Frontend : `valimbavaka-ui/src/app/features/services/pages/services-page/services-page.spec.ts` (2, nouveau), `data-access/service-record-service.spec.ts` (+2), `data-access/service-record-mapping.spec.ts` (test de ligne réécrit) ; `features/demo-pages.a11y.spec.ts` (page « services », inchangé).

## Recette manuelle (🧪)

| # | Étape | Résultat attendu | Statut |
|---|---|---|---|
| R1 | Vendeuse A : `/services`, enregistrer un soin pour « Hanta » avec la recette, puis un second sans cliente ni produit | l'encart « Mes prestations du jour » montre les deux, la plus récente en haut : heure, service, « Hanta » / « Cliente non renseignée », produits avec quantité et unité / « Aucun produit consommé » ; total en Ar | 🧪 recette |
| R2 | Vendeuse B se connecte sur le même poste | son encart ne contient pas les prestations de A | 🧪 recette |
| R3 | Le lendemain matin | l'encart est vide : « Aucune prestation enregistrée aujourd'hui. » | 🧪 recette |
| R4 | `GET /api/v1/service-records/mine?date=<hier>` (Swagger) | prestations de la veille du compte connecté | 🧪 recette |

## Résultat de l'exécution

- Résultat de l'exécution : à lancer (CI ou local).
- Anomalies ouvertes : aucune connue.
