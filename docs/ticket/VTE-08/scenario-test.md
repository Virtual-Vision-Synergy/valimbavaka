# Scénarios de test — VTE-08 · Tableau de bord des ventes par canal

Statut : ✅ rédigé le 2026-11-11 · Ticket : [#69](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/69) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | Administrateur `ADMIN` (a `reports.read`) ; vendeuse `SELLER` et livreur `DELIVERY_DRIVER` refusés (pas de `reports.read`) |
| Données de départ | Journée du jour **ouverte** (JR-01) ; article fini « Savon coco » (prix 8 500 Ar) avec 50 unités en Boutique ; vendeuse Miora Rakoto ; ventes validées : Boutique 1 × Savon coco (8 500 Ar) puis 2 × Savon coco (17 000 Ar), Facebook 1 × Savon coco (8 500 Ar) ; canal Salon sans vente |
| Tickets à avoir déployés avant | VTE-01 (ventes), VTE-07 (`SaleQueryRepository`), SYS-05 (canaux) |

## Scénarios issus des critères d'acceptation

| # | Critère d'acceptation | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Chaque canal affiche CA, nombre de ventes et part du CA total. | Novembre 2026 : Boutique 75 000 Ar (5 ventes), Facebook 25 000 Ar (2 ventes), Salon rien ; l'admin appelle `GET /api/v1/sales/channel-dashboard?from=2026-11-01&to=2026-11-30` | 200 ; `totalRevenue` 100 000, `totalSalesCount` 7 ; canaux triés par CA : Boutique (75,0 %), Facebook (25,0 %), Salon (0 %) | `ChannelDashboardServiceImplTest.getDashboard_threeChannels_computesTheShareOfTheTotalAndSortsByRevenue` | ☐ |
| 1b | idem (de bout en bout) | 2 ventes Boutique (1 et 2 savons) et 1 vente Facebook enregistrées | CA Boutique +25 500, CA Facebook +8 500 ; somme des parts ≈ 100 % | `ChannelDashboardIT.getDashboard_administrator_returnsRevenueCountAndShareOfEachChannel` | ☐ |
| 1c | idem (arrondi) | Boutique 2 000 Ar, Facebook 1 000 Ar | Part de Facebook 33,3 % (une décimale) | `ChannelDashboardServiceImplTest.getDashboard_oneThirdShare_roundsToOneDecimal` | ☐ |
| 1d | idem (écran) | L'admin ouvre `/sales/channels` | Tableau Canal / CA / Ventes / Part du CA total (barre et « 75 % »), totaux de la période | `channel-dashboard-page.spec.ts` « should show the revenue, the number of sales and the share of each channel », « should show the total number of sales of the period » | ☐ |
| 1e | idem (appel) | Période `2026-11-01` à `2026-11-30` | `GET /api/v1/sales/channel-dashboard?from=…&to=…` ; période vide non envoyée | `channel-dashboard-service.spec.ts` « lit le tableau de bord de la période demandée », « n'envoie pas de période vide : le serveur prend le mois en cours » | ☐ |

## Règles métier

| Cas | Exemple | Résultat attendu | Test auto | OK |
|---|---|---|---|---|
| Seules les ventes validées comptent | Une vente Boutique passée à `CANCELLED` | CA Boutique inchangé | `ChannelDashboardIT.getDashboard_cancelledSale_isNotCounted` | ☐ |
| Période par défaut | Aucun paramètre, 01/12/2026 00:30 à Madagascar (= 30/11 21:30 UTC) | `from` = `to` = 2026-12-01 (jour de Madagascar, premier du mois) | `ChannelDashboardServiceImplTest.getDashboard_withoutDates_readsTheMonthToDateInMadagascarTime` | ☐ |
| Période par défaut (API) | Aucun paramètre | 200, `from` et `to` renseignés | `ChannelDashboardIT.getDashboard_withoutDates_returnsTheCurrentMonth` | ☐ |
| Aucune vente | Période 2000-01-01 à 2000-01-31 | 200, total 0, tous les canaux actifs à 0, part 0 sans division par zéro | `ChannelDashboardServiceImplTest.getDashboard_noSaleAtAll_listsEveryActiveChannelAtZeroWithoutDividingByZero`, `ChannelDashboardIT.getDashboard_periodWithoutSale_listsEveryChannelAtZero` | ☐ |
| Canal désactivé depuis | Vente sur un canal désactivé | Canal listé, `salesChannelLabel` `null`, affiché « Canal désactivé » | `ChannelDashboardServiceImplTest.getDashboard_deactivatedChannelWithSales_staysListedWithoutLabel` | ☐ |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test auto | OK |
|---|---|---|---|---|
| Appel sans jeton | `GET /api/v1/sales/channel-dashboard` | 401 `UNAUTHENTICATED` | `ChannelDashboardIT.getDashboard_withoutToken_returns401` | ☐ |
| Mauvais rôle | Jeton vendeuse, puis livreur | 403 `FORBIDDEN` | `ChannelDashboardIT.getDashboard_sellerAndDeliveryDriver_return403` | ☐ |
| Période à l'envers | `from=2026-11-10&to=2026-11-01` | 422 `VALIDATION_FAILED` | `ChannelDashboardIT.getDashboard_fromAfterTo_returns422`, `ChannelDashboardServiceImplTest.getDashboard_fromAfterTo_throwsBusinessException` | ☐ |
| Date mal formée | `from=01-11-2026` | 400 `MALFORMED_REQUEST` | `ChannelDashboardIT.getDashboard_malformedDate_returns400` | ☐ |
| Mass assignment, 404, 409, journal | Sans objet : lecture seule, sans corps, aucune ressource par id, rien n'est écrit | — | — | ⏭ hors périmètre |

## Cas limites et hors ligne

- Bornes de la période : `from` et `to` sont inclus (jours ouvrés `sales.business_date`) ; `from` = `to` est valide.
- Seule `from` fournie : `to` = aujourd'hui ; seule `to` fournie : `from` = premier jour du mois de `to`.
- Total nul : toutes les parts valent 0 (pas de division par zéro).
- Écran : une période à l'envers n'envoie aucune requête et affiche le message « La date de début doit précéder la date de fin » (🧪 recette).
- Lecture seule : rien n'est mis en file hors ligne ; hors connexion, l'écran affiche l'état d'erreur avec « Réessayer ».
- Le lien « Ventes par canal » de l'écran `/sales` n'apparaît qu'avec `reports.read` ; le serveur reste l'autorité (403 sinon).

## Tests existants

- Backend : `feature/sale/service/impl/ChannelDashboardServiceImplTest.java`, `feature/sale/controller/ChannelDashboardIT.java`.
- Frontend : `features/sales/data-access/channel-dashboard-service.spec.ts`, `features/sales/pages/channel-dashboard-page/channel-dashboard-page.spec.ts`.

## Recette manuelle (🧪)

| Étape | Résultat attendu | Statut |
|---|---|---|
| Connecté en admin, ouvrir « Ventes du jour » puis « Ventes par canal » | Tableau du mois en cours, un canal par ligne avec CA, ventes, part | 🧪 recette |
| Changer « Du » et « Au », puis mettre « Du » après « Au » | Tableau recalculé ; message d'erreur sans appel serveur pour la période à l'envers | 🧪 recette |
| Connecté en vendeuse | Pas de lien « Ventes par canal » ; `/sales/channels` redirige | 🧪 recette |

## Résultat de l'exécution

- Date : à lancer (CI ou local).
- Environnement : à lancer (CI ou local).
- Anomalies ouvertes : aucune connue ; 3 étapes 🧪 restantes.
