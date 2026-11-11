# Scénarios de test — DSH-04 · Répartition du CA par gamme et canal

Statut : ☐ tests écrits, à lancer (CI ou local) · Ticket : [#97](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/97) · [description](description.md) · [guide](guide.md)

Légende de la colonne « Statut » : ✅ auto = test automatique vert · 🔍 code = vérifié par lecture · ⏭ hors périmètre = renvoyé à un autre ticket · 🧪 recette = à faire à la main · ☐ = test écrit, pas encore exécuté. Détail : [CONSIGNE.md](../CONSIGNE.md#4-remplir-le-scénario-de-test).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (`reports.read`) ; `SELLER` et `DELIVERY_DRIVER` pour les refus |
| Données de départ | journée du jour ouverte ; un article à 8 500 Ar reçu en boutique (50 pièces), rattaché à une gamme ; une vendeuse avec canal Boutique et commission 5 % ; ventes créées par le test |
| Tickets à avoir déployés avant | ART-03, VTE-01, SYS-05, RPT-01, VTE-08, DSH-01 |

## Scénarios issus des critères d'acceptation

Critère : « Chaque gamme/canal affiche sa valeur et sa proportion relative. » Les tests d'agrégat comparent la lecture avant et après (la base est partagée entre classes de test).

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Valeur et part par gamme et par canal | `GET /api/v1/dashboard/revenue-split` en `ADMIN` | 200, `date` = aujourd'hui (Antananarivo), `byRange[]` (`rangeId`, `code`, `label`, `revenue`, `sharePercent`) et `byChannel[]` (`salesChannelId`, `label`, `revenue`, `sharePercent`) | `RevenueSplitIT.getRevenueSplit_administrator_returnsTodayWithRangesAndChannels` | ☐ |
| 1b | Une vente monte sa gamme et son canal | vente de 2 × 8 500 Ar au comptoir | CA de la gamme de l'article +17 000, CA du canal Boutique +17 000, parts de chaque axe = 100,0 au total | `RevenueSplitIT.getRevenueSplit_newSaleToday_raisesItsRangeAndItsChannelAndKeepsSharesAtOneHundred` | ☐ |
| 1c | Une vente annulée sort de la répartition | vente de 8 500 Ar passée à `CANCELLED` | CA de la gamme −8 500, CA du canal −8 500 | `RevenueSplitIT.getRevenueSplit_cancelledSaleToday_leavesItsRangeAndItsChannel` | ☐ |
| 2 | Gammes triées, parts qui totalisent 100,0 | CA 286 000 / 562 000 / 436 000 Ar | ordre Zezika, Solika, Botanika ; parts 43,8 / 33,9 / 22,3 (arrondies seules : 43,8 + 34,0 + 22,3 = 100,1) | `RevenueSplitServiceImplTest.getToday_withSales_returnsRangesBestFirstWithSharesAddingUpToOneHundred` ; `PercentageSharesTest.of_unevenRevenues_roundsToOneDecimalAndKeepsTheOrder` | ☐ |
| 2b | Canaux : ordre de VTE-08 conservé, parts rééquilibrées | 3 canaux à 100 Ar chacun (dont un désactivé, libellé `null`) | 33,4 / 33,3 / 33,3 (somme 100,0) | `RevenueSplitServiceImplTest.getToday_withSales_returnsChannelsInTheOrderOfTheChannelDashboardWithBalancedShares` ; `PercentageSharesTest.of_threeEqualRevenues_sharesAddUpToExactlyOneHundred` | ☐ |
| 3 | Contrat front | `revenueSplit()` | `GET /api/v1/dashboard/revenue-split`, gammes et canaux (libellé `null` accepté) | `revenue-split.spec.ts` « should read the split by range and channel from its own endpoint » | ☐ |
| 3b | La carte montre valeur et part | `ADMIN` ouvre `/dashboard?tab=analysis` | « Zezika 562 000 Ar · 43,8 % », « Solika … 33,9 % », « Botanika … 22,3 % » ; canaux « Boutique 668 k 52,0 % », « Facebook 398 k 31,0 % », « Canal désactivé 218 k 17,0 % » ; barre de Zezika à 43,8 % de largeur | `dashboard-page.spec.ts` « affiche la valeur et la part de chaque gamme et de chaque canal lues sur l’API » | ☐ |
| 4 | Accessibilité | page `dashboard` chargée avec l'API simulée | aucune violation axe (la carte est dans l'onglet « Analyse », non rendu par défaut ; les barres sont `aria-hidden`, la valeur et la part sont en texte) | `demo-pages.a11y.spec.ts` (entrée `dashboard`, inchangée) | ☐ |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `GET /api/v1/dashboard/revenue-split` | 401 `UNAUTHENTICATED` | `RevenueSplitIT.getRevenueSplit_withoutToken_returns401` | ☐ |
| Mauvais rôle | jeton `SELLER`, puis `DELIVERY_DRIVER` | 403 `FORBIDDEN` | `RevenueSplitIT.getRevenueSplit_sellerAndDeliveryDriver_return403` | ☐ |
| Vendeuse ou livreur sur l'écran | `SELLER` / `DELIVERY_DRIVER` ouvre `/dashboard` | aucune lecture de la répartition (ressource au repos, `canSeeCockpit` faux) | `dashboard-page.spec.ts` « montre aux alertes seules la vendeuse… » et « n’appelle aucune API et ne montre aucun indicateur au livreur » | ☐ |
| Échec de la lecture | 503 sur `/revenue-split` | la carte montre l'état d'erreur avec « Réessayer » ; indicateurs et autres blocs intacts ; le réessai relit | `dashboard-page.spec.ts` « garde le cockpit quand la répartition du CA échoue, puis la relit au réessai » | ☐ |
| Champ `id` / `role` dans le corps | sans objet : lecture seule, aucune entrée | — | — | ⏭ sans objet |
| 404, 409, 422, journal | sans objet : aucun paramètre, aucune écriture, rien n'est tracé au journal | — | — | ⏭ sans objet |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| CA nul (aucune vente validée) | journée sans vente | listes présentes, tous les CA et toutes les parts à 0,0, pas de division par zéro ; l'écran affiche « Aucune vente validée aujourd'hui » à la place des barres | `RevenueSplitServiceImplTest.getToday_noSaleAtAll_returnsEveryRangeAndChannelAtZero` ; `PercentageSharesTest.of_zeroTotal_returnsZerosWithoutDividingByZero` ; `dashboard-page.spec.ts` « montre un état vide, sans barre, quand aucune vente n’est validée aujourd’hui » | ☐ |
| Une seule gamme vend | 8 500 Ar sur une gamme, 0 sur l'autre | 100,0 et 0,0 | `PercentageSharesTest.of_oneRevenueOnly_getsAllTheShare` | ☐ |
| Part minuscule | 1 Ar contre 1 000 000 Ar | 100,0 et 0,0 (jamais de part négative ni > 100) | `PercentageSharesTest.of_tinyRevenueNextToABigOne_keepsTheSmallOneAtZeroPointZero` | ☐ |
| Aucune gamme active | référentiel sans gamme active | liste de gammes vide | `RevenueSplitServiceImplTest.getToday_noActiveRange_returnsAnEmptyRangeList` ; `PercentageSharesTest.of_noRevenue_returnsAnEmptyList` | ☐ |
| Minuit à Madagascar | `2026-11-11T21:30:00Z` (00:30 le 12) | lecture du 12 novembre, pas du 11 | `RevenueSplitServiceImplTest.getToday_lateEveningUtc_readsTheAntananarivoDay` | ☐ |
| Hors ligne | lecture seule, jamais mise en file | état d'erreur avec « Réessayer » dans la carte | `dashboard-page.spec.ts` (test d'échec ci-dessus) | ☐ |
| Gamme dont l'article n'a pas de gamme | article sans gamme (non vendable) | non compté dans les gammes ; les canaux gardent la vente | 🔍 code (`ReportSaleRepository.totalsByRange` joint sur `a.rangeId`) | 🔍 code |

## Résultat de l'exécution

- Date : à lancer (CI ou local)
- Environnement : aucune exécution locale (règle n°3 de CLAUDE.md) ; recette manuelle 🧪 à faire par Njara après le merge
- Anomalies ouvertes : aucune connue
