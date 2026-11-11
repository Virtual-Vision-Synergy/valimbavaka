# Scénarios de test — DSH-02 · Graphique du CA sur 7 jours

Statut : ☐ tests écrits, à lancer (CI ou local) · Ticket : [#95](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/95) · [description](description.md) · [guide](guide.md)

Légende de la colonne « Statut » : ✅ auto = test automatique vert · 🔍 code = vérifié par lecture · ⏭ hors périmètre = renvoyé à un autre ticket · 🧪 recette = à faire à la main · ☐ = test écrit, pas encore exécuté. Détail : [CONSIGNE.md](../CONSIGNE.md#4-remplir-le-scénario-de-test).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (`reports.read`) ; `SELLER` et `DELIVERY_DRIVER` pour les refus |
| Données de départ | journée du jour ouverte ; un article `SAV-001` à 8 500 Ar reçu en boutique (50 pièces) ; une vendeuse avec canal Boutique et commission 5 % ; ventes créées par le test |
| Tickets à avoir déployés avant | VTE-01, RPT-01, DSH-01 |

## Scénarios issus des critères d'acceptation

Critère : « Le graphique affiche une valeur par jour avec infobulle au survol. » Les tests d'agrégat comparent la lecture avant et après (la base est partagée entre classes de test).

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Une valeur par jour | `GET /api/v1/dashboard/revenue-trend` en `ADMIN` | 200, 7 points consécutifs du jour −6 à aujourd'hui (fuseau Antananarivo), plus ancien d'abord, chacun avec `revenue` et `salesCount` | `DashboardIT.getRevenueTrend_administrator_returnsSevenConsecutiveDaysEndingToday` ; `RevenueTrendServiceImplTest.getLastSevenDays_withSales_returnsOnePointPerDayOldestFirstEndingToday` | ☐ |
| 1b | Le graphique dessine 7 barres | `ADMIN` ouvre `/dashboard?tab=analysis` | 7 colonnes, la dernière (« Aujourd'hui ») mise en avant | `dashboard-page.spec.ts` « dessine le CA des 7 derniers jours lu sur l’API, une barre et une infobulle par jour » | ☐ |
| 1c | Infobulle au survol | survol de la colonne du 11/11 (1 284 000 Ar, 47 ventes) et de celle d'un jour sans vente | « mercredi 11 novembre : 1 284 000 Ar · 47 vente(s) » ; « … : 0 Ar · 0 vente(s) » (infobulle montrée par `:hover`, texte présent dans le DOM) | `dashboard-page.spec.ts` (même test : contenu des `.tip`) | ☐ |
| 1d | Un jour sans vente vaut 0 | aucune vente sur 7 jours | 7 points à 0, série continue | `RevenueTrendServiceImplTest.getLastSevenDays_noSaleAtAll_returnsSevenPointsAtZero` | ☐ |
| 1e | Une vente du jour monte le dernier point | vente de 2 × 8 500 Ar | dernier point +17 000, +1 vente, les 6 autres inchangés | `DashboardIT.getRevenueTrend_newSaleToday_raisesOnlyTheLastPoint` | ☐ |
| 1f | Une vente annulée sort du graphique | vente passée à `CANCELLED` | dernier point −8 500, −1 vente | `DashboardIT.getRevenueTrend_cancelledSaleToday_leavesTheLastPoint` | ☐ |
| 1g | Contrat front | `revenueTrend()` | `GET /api/v1/dashboard/revenue-trend`, 7 points | `dashboard-service.spec.ts` « should read the revenue of the last seven days from its own endpoint » | ☐ |
| 2 | Accessibilité | page `dashboard` chargée avec l'API simulée | aucune violation axe (le graphique est dans l'onglet « Analyse », non rendu par défaut ; son tableau équivalent `sr-only` est celui de `vvs-bar-chart`) | `demo-pages.a11y.spec.ts` (entrée `dashboard`, inchangée) | ☐ |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `GET /api/v1/dashboard/revenue-trend` | 401 `UNAUTHENTICATED` | `DashboardIT.getRevenueTrend_withoutToken_returns401` | ☐ |
| Mauvais rôle | jeton `SELLER`, puis `DELIVERY_DRIVER` | 403 `FORBIDDEN` | `DashboardIT.getRevenueTrend_sellerAndDeliveryDriver_return403` | ☐ |
| Vendeuse ou livreur sur l'écran | `SELLER` / `DELIVERY_DRIVER` ouvre `/dashboard` | aucune lecture du graphique (ressource au repos, `canSeeCockpit` faux) | `dashboard-page.spec.ts` « n’appelle aucune API et ne montre aucun indicateur au livreur » | ☐ |
| Échec de la lecture | 503 sur `/revenue-trend` | le bloc montre l'état d'erreur avec « Réessayer » ; indicateurs et autres blocs intacts ; le réessai relit | `dashboard-page.spec.ts` « garde les indicateurs quand le graphique du CA échoue, puis le relit au réessai » | ☐ |
| Champ `id` / `role` dans le corps | sans objet : lecture seule, aucune entrée | — | — | ⏭ sans objet |
| 404, 409, 422, journal | sans objet : aucun paramètre, aucune écriture, rien n'est tracé au journal | — | — | ⏭ sans objet |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Minuit à Madagascar | `2026-11-11T21:30:00Z` (00:30 le 12) | série du 6 au 12 novembre | `RevenueTrendServiceImplTest.getLastSevenDays_lateEveningUtc_usesTheAntananarivoDay` | ☐ |
| Changement de mois | 3 mars 2026 | 25 février → 3 mars, sans trou | `RevenueTrendServiceImplTest.getLastSevenDays_monthBoundary_crossesTheMonthWithoutGap` | ☐ |
| Un jour sans vente au milieu | jour −5 à 0 Ar entre deux jours de vente | point à 0 (barre minimale), pas de trou dans la série | `RevenueTrendServiceImplTest.getLastSevenDays_withSales_returnsOnePointPerDayOldestFirstEndingToday` | ☐ |
| Hors ligne | lecture seule, jamais mise en file | état d'erreur avec « Réessayer » dans le bloc | `dashboard-page.spec.ts` (test d'échec ci-dessus) | ☐ |
| « 4 semaines » de la maquette | bascule 7 jours / 4 semaines | non livré : la description ne demande que 7 jours ; le sélecteur et les chiffres de démonstration sont retirés | ⏭ hors périmètre | ⏭ hors périmètre |
| Répartition par gamme et canal | carte voisine | reste en démonstration | ⏭ DSH-04 | ⏭ hors périmètre |

## Résultat de l'exécution

- Date : à lancer (CI ou local)
- Environnement : tests écrits sans exécution locale (règle n°3 de CLAUDE.md)
- Anomalies ouvertes : aucune connue
