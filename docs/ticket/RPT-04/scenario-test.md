# Scénarios de test — RPT-04 · Répartition du CA par gamme

Statut : ✅ rédigé et exécuté le 2026-11-12 · Ticket : [#101](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/101) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | Administrateur `ADMIN` (a `reports.read`) ; vendeuse et livreur refusés (cas de RPT-01) |
| Données de départ | Celles de RPT-01 : journée du jour ouverte, deux articles à 8 500 Ar de deux gammes (savon, crème), vendeuse, canaux Boutique et Facebook. Exemple : une vente boutique de 1 savon et 2 crèmes = 8 500 Ar (gamme savon) + 17 000 Ar (gamme crème) |
| Tickets à avoir déployés avant | ART-03 (couleur de la gamme par son code), RPT-01 (`ReportPeriod`, `ReportFilter`, `ReportSaleRepository`), SYS-05 (gammes) |

Les tests d'intégration partagent la base avec d'autres classes : aucune assertion ne suppose que la base ne contient que ses données (différence avant/après du CA d'une gamme, somme des gammes comparée à la somme de la même réponse, ou période sans aucune vente comme l'an 2000).

## Scénarios issus des critères d'acceptation

| # | Critère d'acceptation | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | La somme par gamme correspond au CA total de la période. | Une vente boutique : 1 savon (8 500) et 2 crèmes (17 000), sans remise globale | Le CA de la gamme savon augmente de 8 500, celui de la gamme crème de 17 000 ; `ranges.revenue` augmente comme `sales.revenue` ; la somme des `items[].revenue` = `ranges.revenue` ; la somme des parts = 100,0 | `ReportIT.getReport_ranges_splitTheRevenueOfEachRangeAndTheSharesAddUpTo100` | ✅ auto |
| 2 | Idem, calcul de la part | Deux gammes à 17 000 et 8 500 | Parts 66,7 % et 33,3 % ; `ranges.revenue` = 25 500 ; une seule requête SQL | `ReportSalesServiceImplTest.getRanges_twoRanges_givesRevenueAndSharesThatAddUpTo100` | ✅ auto |
| 3 | Les parts totalisent exactement 100,0 | 3 montants égaux ; 1/2/4 ; un montant minuscule face à un énorme | 33,4 + 33,3 + 33,3 ; 14,3 + 28,6 + 57,1 ; total 100,0 dans tous les cas | `ReportShareTest.distribute_threeEqualAmounts_giveTheMissingTenthToTheFirst`, `distribute_missingTenthsGoToTheLargestRemainders`, `distribute_tinyAmountNextToHugeOne_isZeroPointZeroAndTheTotalStays100`, `distribute_manyAmounts_alwaysAddUpTo100` | ✅ auto |
| 4 | Gammes triées | Une vente savon + crème | Le CA des gammes est décroissant | `ReportIT.getReport_ranges_areSortedByDescendingRevenue` | ✅ auto |
| 5 | Le rapport expose le bloc | `getReport` | `ranges` = résultat de `getRanges` avec la même période et le même canal | `ReportServiceImplTest.getReport_ranges_isTheSplitOfThePeriodWithTheSameFilters` | ✅ auto |
| 6 | Écran : anneau et légende | L'API renvoie Zezika 17 000 (66,7) et Botanika 8 500 (33,3) | Légende : libellé, montant, part avec virgule (« 66,7 % »), plus forte gamme d'abord ; 2 arcs ; couleur par `data-range` | `reports-page.spec.ts` « should list each range with its revenue and its share, the biggest first » | ✅ auto |

## Règles métier

| Règle | Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|---|
| Ventes annulées exclues | Vente `CANCELLED` | Le CA de la gamme ne bouge pas | `ReportIT.getReport_ranges_cancelledSaleIsNotCounted` | ✅ auto |
| Filtre canal appliqué | Vente Facebook (savon + crème) et vente boutique (savon) ; canal Facebook | Seule la vente Facebook compte : savon +8 500 | `ReportIT.getReport_ranges_channelFilterAppliesButRangeFilterIsIgnored` | ✅ auto |
| Filtre gamme ignoré pour cette section | `rangeId` = savon | La gamme crème figure toujours dans la répartition (+8 500 dans la même lecture) ; l'écran le dit | `ReportIT.getReport_ranges_channelFilterAppliesButRangeFilterIsIgnored`, `ReportSalesServiceImplTest.getRanges_rangeFilterIsIgnored_readsEveryRangeInOneQuery`, `reports-page.spec.ts` « should say that the range filter does not apply to the split » | ✅ auto |
| CA d'une gamme avant la remise globale | Vente avec remise globale | La somme des gammes dépasse `sales.revenue` du montant des remises ; l'écran affiche l'écart | `reports-page.spec.ts` « should explain the gap when the sale discounts make the summary lower than the ranges », « should not mention a gap when the ranges add up to the summary » | ✅ auto |
| Un seul agrégat | Lecture des gammes | Un seul appel `revenueByRange` (pas une requête par gamme) | `ReportSalesServiceImplTest.getRanges_twoRanges_givesRevenueAndSharesThatAddUpTo100` (`verifyNoMoreInteractions`) | ✅ auto |

## Sécurité et erreurs

Même endpoint que RPT-01, aucun nouveau paramètre ni corps : `ReportIT.getReport_withoutToken_returns401`, `getReport_sellerAndDeliveryDriver_return403`, `getReport_futureDay_returns422`, `getReport_unknownGranularity_returns400`, `getReport_malformedDateOrId_returns400` couvrent aussi la répartition (même réponse).

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Période qui n'a pas commencé | 422, aucune répartition lue | `ReportServiceImplTest.getReport_dayInTheFuture_doesNotReadTheCurve` | ✅ auto |
| Sans jeton, vendeuse, livreur | 401, 403 | `ReportIT` (RPT-01) | ✅ auto |
| Mass assignment, 404, 409 | Sans objet : lecture seule | — | — |

## Cas limites et hors ligne

- Aucune vente (an 2000) : `ranges.items` vide, `ranges.revenue` = 0, aucune division par zéro (`ReportIT.getReport_ranges_pastPeriodWithoutSale_isEmptyWithoutDividingByZero`, `ReportSalesServiceImplTest.getRanges_noSale_isEmptyWithZeroRevenue`) ; l'écran affiche l'état vide, sans « NaN » (`reports-page.spec.ts` « should show an empty state and no ring when no range sold anything »).
- Gammes sans CA (lignes à 0) : parts à 0,0 (`ReportSalesServiceImplTest.getRanges_rangesWithoutRevenue_haveZeroShares`, `ReportShareTest.distribute_zeroTotalOrEmpty_givesZerosWithoutDividingByZero`) ; l'écran affiche l'état vide.
- Une seule gamme : 100,0 % (`ReportShareTest.distribute_singleAmount_is100`).
- Gamme désactivée depuis : son CA historique reste dans la répartition (libellé et code lus dans le référentiel, actif ou non).
- Lecture seule : pas de file hors ligne ni de journal. La page `/reports` garde ses états chargement et erreur avec « Réessayer » pour cette section (même appel que la synthèse).
- Accessibilité : `features/demo-pages.a11y.spec.ts` (réponse `/api/v1/reports` avec `ranges` vide) ; l'anneau est décoratif, la légende porte les chiffres.

## Recette manuelle 🧪 (Njara)

| # | Étapes | Attendu | OK |
|---|---|---|---|
| R1 | Ouvrir `/reports`, période « Mois » | Section « CA par gamme » : anneau, une ligne par gamme, parts qui totalisent 100,0 | 🧪 |
| R2 | Choisir un canal | La répartition suit le canal | 🧪 |
| R3 | Choisir une gamme | La section affiche toujours toutes les gammes et le dit ; la synthèse, elle, se restreint | 🧪 |
| R4 | Choisir une période sans vente | État vide « Aucune vente sur la période » | 🧪 |

## Résultat de l'exécution

- Date : 2026-11-12
- Environnement : CI GitHub Actions sur la branche (ci.yml, conventions.yml, Qodana : verts, 0 annotation Qodana) ; backend 1019 tests dont `ReportIT` 29, frontend 140 fichiers de test, tous passés. Recette manuelle 🧪 : à passer par Njara après le merge
- Anomalies ouvertes : aucune connue
