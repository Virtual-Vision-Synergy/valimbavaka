# Scénarios de test — SHO-03 · Suivi des sorties shooting en cours

Statut : ✅ rédigé le 2026-11-08 — **tests exécutés en CI, tous verts** (voir « Résultat de l'exécution ») · Ticket : [#45](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/45) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible. Légende : ✅ auto = exécuté en CI et vert · ☐ écrit, pas encore exécuté · 🧪 recette manuelle.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | Administrateur (`ADMIN`, « Rasoa Rakoto ») : `shootings.track` ; vendeuse (`SELLER`) et livreur (`DELIVERY_DRIVER`) pour les refus 403 |
| Données de départ | Article « Gommage café 200 g » au coût d'achat de 3 120,50 Ar : 5 reçus en boutique (`STORE`), sortie shooting de 2 (SHO-01) retour prévu demain → 1 sortie en cours, 0 en retard, 6 241,00 Ar immobilisés |
| Tickets à avoir déployés avant | SHO-01 (sortie shooting), SHO-02 (réintégration, PR empilée), STK-03 (coût d'achat lu par `ArticleService.getCosts`) |
| Migration | `2026_11_08-63-changelog.sql` (permission `shootings.track` donnée à `ADMIN`) |

**Les 3 indicateurs retenus** (le critère ne les nomme pas ; repris de la user story « sorties en cours et leur valeur immobilisée » et du guide SHO-01) :

1. **Sorties en cours** : nombre de sorties au statut `OUT` (Sorti, en retard compris).
2. **En retard** : sorties `OUT` dont le retour prévu est **strictement** passé (même règle que `late` de SHO-01), avec un badge « À relancer » s'il y en a au moins une.
3. **Valeur immobilisée** : Σ quantité sortie × coût d'achat de l'article, sur les sorties `OUT`, arrondie au centime une seule fois (comme la valorisation STK-03) ; les unités d'un article sans coût sont comptées à part (`unvaluedQuantity`) et signalées sous l'indicateur.

## Scénarios issus des critères d'acceptation

### Critère 1 — Les 3 indicateurs sont affichés en tête de l'écran Shooting

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| 1 · Nouvelle sortie | boutique 5, sortie de 2 (coût 3 120,5) ; `GET /api/v1/shootings/summary` avant / après | `outCount` +1, `lateCount` +0, `immobilizedValue` +6 241,00, `unvaluedQuantity` +0 | `ShootingSummaryIT.getSummary_exitRecorded_addsOneOutExitAndItsValueAtCost` | ✅ auto |
| 1b · Sortie en retard | retour prévu passé à « maintenant − 1 h » | `lateCount` +1 ; `outCount` et valeur inchangés (elle était déjà comptée) | `ShootingSummaryIT.getSummary_exitPastItsExpectedReturn_isCountedLate` | ✅ auto |
| 1c · Réintégration | sortie de 3 réintégrée (SHO-02) | `outCount` −1, `immobilizedValue` −9 361,50 | `ShootingSummaryIT.getSummary_afterReturn_removesTheExitAndItsValue` | ✅ auto |
| 1d · Article sans coût | article sans coût d'achat, sortie de 2,5 | `outCount` +1, `unvaluedQuantity` +2,5, valeur +0 | `ShootingSummaryIT.getSummary_articleWithoutCost_isCountedAsUnvaluedQuantityOnly` | ✅ auto |
| 1e · Calcul | 3 sorties dont 1 en retard ; 2 savons à 3 120,50 et 1,5 gommage à 4 000,333 | `outCount` 3, `lateCount` 1 (horloge 2026-11-08T15:00Z passée au repository), valeur 12 241,50 (12 241,4995 arrondi une fois) | `ShootingSummaryServiceImplTest.getSummary_exitsOut_countsOutAndLateExitsAndValuesTheQuantitiesAtCost` | ✅ auto |
| 1f · Coût absent | savon 2 à 1 000, gommage 2,5 sans coût | valeur 2 000,00, `unvaluedQuantity` 2,5 | `ShootingSummaryServiceImplTest.getSummary_articleWithoutCost_isCountedAsUnvaluedQuantity` | ✅ auto |
| 1g · Écran : appel | `summary()` | `GET /api/v1/shootings/summary`, réponse reprise telle quelle | `shooting-service.spec.ts` (« lit les indicateurs des sorties en cours par GET /summary (SHO-03) ») | ✅ auto |
| 1h · Écran : 3 indicateurs | `{ outCount: 3, lateCount: 1, immobilizedValue: 12241.5 }` | 3 cartes : « Sorties en cours » 3, « En retard » 1 avec badge « À relancer », « Valeur immobilisée » 12 242 Ar | `shooting-kpis.spec.ts` (« affiche les sorties en cours, les retards et la valeur immobilisée en ariary ») | ✅ auto |
| 1i · Écran : sans retard, unités sans coût | `lateCount` 0, `unvaluedQuantity` 2.5 | pas de badge ; « dont 2.5 unité(s) sans coût, non valorisée(s) » | `shooting-kpis.spec.ts` (« n'affiche pas de badge sans retard et signale les unités sans coût ») | ✅ auto |

## Sécurité et erreurs

Endpoint : `GET /api/v1/shootings/summary` (`shootings.track`, administrateur), lecture seule sans paramètre.

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `GET /api/v1/shootings/summary` | 401 `UNAUTHENTICATED` | `ShootingSummaryIT.getSummary_withoutToken_returns401` | ✅ auto |
| Mauvais rôle (vendeuse) | jeton vendeuse : elle lit les sorties (`services.read`) mais pas leur valeur au coût | 403 `FORBIDDEN` | `ShootingSummaryIT.getSummary_withSellerRole_returns403` | ✅ auto |
| Mauvais rôle (livreur) | jeton livreur | 403 `FORBIDDEN` | `ShootingSummaryIT.getSummary_withDeliveryDriverRole_returns403` | ✅ auto |
| Écran sans permission | vendeuse sur l'onglet Shooting | aucun appel à `/summary`, pas d'indicateur (`canTrackShooting` faux) ; liste et formulaire inchangés | lecture de `services-page.ts` (`shootingSummary`) | 🧪 |
| Mass assignment | — | sans objet : aucun corps accepté (GET) | — | ⏭ hors périmètre |
| Données invalides | — | sans objet : aucun paramètre | — | ⏭ hors périmètre |
| Ressource introuvable | — | sans objet : agrégat global, jamais 404 | — | ⏭ hors périmètre |
| Conflit | — | sans objet : lecture seule, pas de 409 | — | ⏭ hors périmètre |
| Journal | lecture des indicateurs | aucune ligne de journal (lecture seule, comme `GET /api/v1/stocks/valuation`) | — | ⏭ hors périmètre |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Aucune sortie dehors | toutes réintégrées | `0`, `0`, `0.00`, `0` ; les coûts ne sont pas lus | `ShootingSummaryServiceImplTest.getSummary_noExitOut_returnsZerosWithoutReadingTheCosts` | ✅ auto |
| Écran à zéro | indicateurs à 0 | « 0 », « 0 », « 0 Ar », pas de mention « sans coût » | `shooting-kpis.spec.ts` (« affiche des zéros quand aucune sortie n'est dehors ») | ✅ auto |
| Borne du retard | retour prévu = maintenant exactement | pas en retard (strictement avant maintenant, comme `Shooting.isLateAt`) | `ShootingSummaryServiceImplTest.getSummary_exitsOut_countsOutAndLateExitsAndValuesTheQuantitiesAtCost` (instant de l'horloge passé tel quel à `countByStatusAndExpectedReturnAtBefore`) | ✅ auto |
| Plus de 100 sorties en cours | 150 sorties `OUT` | indicateurs exacts : requêtes d'agrégat (`count`, `sum … group by article`), pas la page de 100 de la liste | lecture de `ShootingRepository` (`countByStatus`, `quantitiesByArticle`) ; `ShootingSummaryIT` vérifie le calcul sur une base partagée qui contient déjà d'autres sorties | 🔍 code |
| Même article dans plusieurs sorties | savon sorti 2 fois (1 + 2) | quantités additionnées par article avant valorisation (une seule lecture de coût) | lecture de `ShootingRepository.quantitiesByArticle` (`group by i.articleId`) ; pas de test dédié | 🔍 code |
| Coût modifié après la sortie | coût d'achat 3 120,5 → 3 500 | la valeur suit le **coût actuel** de l'article (comme STK-03), pas un coût figé à la sortie | — (hypothèse, voir guide) | 🧪 |
| Après une écriture | sortie enregistrée ou réintégrée depuis l'écran | indicateurs relus avec la liste et le stock (`reloadShootings`) | lecture de `services-page.ts` | 🧪 |
| Hors ligne | réseau coupé à l'ouverture de l'onglet | lecture seule : pas de file ; en cas d'échec, les indicateurs ne s'affichent pas, la liste garde son propre état d'erreur | — | 🧪 |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/shooting/service/impl/ShootingSummaryServiceImplTest.java` (nouveau), `controller/ShootingSummaryIT.java` (nouveau), support `controller/ShootingApiTestSupport.java` (`createArticle(String cost)`).
- Frontend : `valimbavaka-ui/src/app/features/services/components/shooting-kpis/shooting-kpis.spec.ts` (nouveau), `data-access/shooting-service.spec.ts` ; `src/app/features/demo-pages.a11y.spec.ts` (page Salon & shooting, onglet Salon par défaut).

## Recette manuelle (🧪)

1. En administrateur, **Salon & shooting → Shooting** : trois cartes en tête (« Sorties en cours », « En retard », « Valeur immobilisée … Ar »), au-dessus de la liste des sorties.
2. Enregistrer une sortie (2 × article à 3 120,5 Ar) : « Sorties en cours » +1 et valeur +6 241 Ar sans recharger la page ; la réintégrer : retour aux valeurs de départ.
3. Une sortie dont le retour prévu est dépassé : « En retard » ≥ 1 avec le badge « À relancer », et la ligne « En retard » dans la liste.
4. En vendeuse : l'onglet Shooting n'affiche pas les indicateurs ; `GET /api/v1/shootings/summary` forcé : 403.

## Résultat de l'exécution

- Date : 2026-11-08 — CI GitHub Actions lancée à la main sur `feature/SHO-03-suivi-sorties-shooting` : `ci.yml` vert sur `bbab8f8` et `6fc27ad` ; `conventions.yml` (Spotless, Checkstyle, Prettier, ESLint, migrations, gitleaks) vert sur `6fc27ad` après un reformatage Spotless (`0482b8b`).
- Environnement : `mvn -B -ntp verify` avec Testcontainers PostgreSQL (293 tests unitaires et 276 d'intégration au total, 0 échec) ; `ng test` Vitest (322 tests, 57 fichiers, 0 échec).
- Tests du ticket : 10 backend (3 unitaires `ShootingSummaryServiceImplTest`, 7 d'intégration `ShootingSummaryIT`) et 4 frontend (`shooting-kpis.spec.ts` 3, `shooting-service.spec.ts` +1), tous verts ; `demo-pages.a11y.spec.ts` vert.
- Anomalies ouvertes : aucune. 2 cas 🔍 code sans test dédié (plus de 100 sorties, même article dans plusieurs sorties) : faible, test d'intégration à ajouter si besoin. Hypothèses à confirmer par Randy : choix des 3 indicateurs, permission dédiée `shootings.track` réservée à l'administrateur, valorisation au coût d'achat actuel. Étapes 🧪 restantes : 4 + recette manuelle.
