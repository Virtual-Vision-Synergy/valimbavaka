# Scénarios de test — SRV-04 · Consommation salon dans la clôture

Statut : ☐ tests automatiques à lancer (CI ou local), recette 🧪 à passer · Ticket : [#84](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/84) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (permission `days.close`) ; `DELIVERY_DRIVER` pour le 403 |
| Données de départ | journée du jour ouverte ; un service du catalogue dont la recette contient 0,015 L d'huile de coco ; 1 L d'huile de coco au salon (`TRADE_SHOW`) |
| Tickets à avoir déployés avant | JR-04 (colonne « Salon »), SRV-02 (prestation et mouvement `TRADE_SHOW`) |

## Ce que JR-04 couvre déjà, et l'écart traité ici

La colonne « Salon » de `GET /api/v1/days/closing` additionne déjà les mouvements `TRADE_SHOW` du jour
(`DayClosingIT.getClosing_salonConsumption_showsTheSalonQuantity`). Ce qui manquait : rien ne **rapprochait** cette
colonne des prestations enregistrées. SRV-04 ajoute `GET /api/v1/days/closing/salon`, qui donne, par article,
la quantité consommée par les prestations du jour, la quantité de la colonne « Salon », l'écart, les deux totaux
et un indicateur `matches`.

## Scénarios issus des critères d'acceptation

| # | Critère d'acceptation | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Le total consommé en salon correspond exactement à la valeur utilisée en clôture. | Étant donné deux prestations du jour consommant 0,015 L puis 0,020 L d'huile de coco ; quand l'administrateur lit `GET /api/v1/days/closing/salon` puis `GET /api/v1/days/closing` | ligne huile : `consumed` = 0,035, `inClosing` = 0,035, `gap` = 0 ; la colonne `salon` de la ligne Salon de la clôture vaut 0,035 ; stock salon 0,965 | `ClosingSalonConsumptionIT.getSalonConsumption_twoServices_consumedEqualsTheSalonColumnOfTheClosing` | ☐ |
| 1 | (totaux) | Gel 2 u. et huile 0,035 L, identiques côté prestations et côté clôture | `matches` = true ; `totalConsumed` = `totalInClosing` = 2,035 ; lignes triées par nom (Gel, Huile) ; tous les écarts à 0 | `DayClosingSalonServiceImplTest.getSalonConsumption_salonColumnEqualsTheServices_matchesAndTotalsAreEqual` | ☐ |
| 1 | (nombre de prestations) | Deux prestations enregistrées aujourd'hui | `servicesCount` augmente de 2 | `ClosingSalonConsumptionIT.getSalonConsumption_servicesOfToday_areCounted` | ☐ |
| 1 | (jour métier) | Horloge au 11/11/2026 à Madagascar | les prestations sont lues sur `[2026-11-10T21:00Z, 2026-11-11T21:00Z[` | `DayClosingSalonServiceImplTest.getSalonConsumption_dayOfMadagascar_readsTheServicesFromMidnightToMidnightLocalTime` | ☐ |

## Sécurité et erreurs

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Appel sans jeton | 401 `UNAUTHENTICATED` | `ClosingSalonConsumptionIT.getSalonConsumption_withoutToken_returns401` | ☐ |
| Appel avec un mauvais rôle (livreur, sans `days.close`) | 403 `FORBIDDEN` | `ClosingSalonConsumptionIT.getSalonConsumption_withDeliveryDriverRole_returns403` | ☐ |
| Journée du jour non ouverte | 409 `CONFLICT` (même règle que la clôture), aucune lecture des prestations | `DayClosingSalonServiceImplTest.getSalonConsumption_dayNotOpened_propagatesTheConflictWithoutReadingTheServices` | ☐ |
| Paramètre `date` envoyé | ignoré : le jour est toujours celui d'aujourd'hui | `ClosingSalonConsumptionIT.getSalonConsumption_forbiddenQueryParameters_areIgnored` | ☐ |

Lecture seule : pas de corps, donc ni mass assignment, ni 404, ni 422 ; pas d'écriture au journal.

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test auto | OK |
|---|---|---|---|---|
| Sortie salon sans prestation | mouvement `TRADE_SHOW` de −2 gel, aucune prestation | ligne gel : `consumed` 0, `inClosing` 2, `gap` +2 ; `matches` = false | `ClosingSalonConsumptionIT.getSalonConsumption_movementWithoutService_showsAGapAndDoesNotMatch` ; `DayClosingSalonServiceImplTest.getSalonConsumption_salonExitWithoutService_showsAPositiveGapAndDoesNotMatch` | ☐ |
| Prestation sans ligne de clôture | 1 L consommé, colonne « Salon » vide | `inClosing` 0, `gap` −1, `matches` = false | `DayClosingSalonServiceImplTest.getSalonConsumption_serviceWithoutClosingLine_showsANegativeGap` | ☐ |
| Aucune prestation, aucune sortie salon | journée vide | `matches` = true, totaux à 0, `lines` vide | `DayClosingSalonServiceImplTest.getSalonConsumption_noServiceAndNoSalonExit_matchesWithZeroTotals` | ☐ |
| Hors ligne | lecture seule, rien à rejouer | sans objet | — | — |

Recette 🧪 (Njara) : enregistrer deux prestations au salon, ouvrir `/day/close` (colonne Salon), appeler `GET /api/v1/days/closing/salon`, vérifier que les totaux sont égaux.

## Résultat de l'exécution

- Date : à lancer (CI ou local)
- Environnement : à lancer (CI ou local)
- Anomalies ouvertes : aucune connue
