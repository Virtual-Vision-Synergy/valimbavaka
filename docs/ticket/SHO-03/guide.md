# Guide d'intégration — SHO-03 · Suivi des sorties shooting en cours

Statut : ✅ écrit le 2026-11-08 — tests exécutés en CI, tous verts (voir « Résultat de l'exécution » du scénario) · Ticket : [#45](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/45) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

L'administrateur voit en tête de l'onglet **Shooting** trois indicateurs calculés par le serveur sur toutes les sorties encore dehors (statut `OUT`) : le **nombre de sorties en cours**, le **nombre en retard** (retour prévu dépassé) et la **valeur immobilisée** au coût d'achat. Le calcul passe par des requêtes d'agrégat, pas par la page de 100 sorties de la liste ; il est réservé à la nouvelle permission `shootings.track` (administrateur).

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/shootings/summary` | `shootings.track` (administrateur seulement) | aucune | 200 `ShootingSummaryRDto` | 401, 403 |

- `ShootingSummaryRDto` (`feature/shooting/dto/ShootingSummaryRDto.java`) : `outCount` (sorties `OUT`, retards compris), `lateCount` (sorties `OUT` dont `expectedReturnAt` < maintenant), `immobilizedValue` (Σ quantité × coût d'achat, en ariary, 2 décimales, arrondi `HALF_UP` une seule fois), `unvaluedQuantity` (unités d'articles sans coût, exclues de la valeur).
- Endpoint déclaré dans `ShootingController` (`controller/ShootingController.java:67`), avant `/{id}` ; lecture seule, aucune ligne de journal (comme `GET /api/v1/stocks/valuation`).

### Services et ports Java réutilisables

| Élément | Rôle |
|---|---|
| `feature.shooting.service.ShootingSummaryService#getSummary()` | indicateurs des sorties en cours |
| `service/impl/ShootingSummaryServiceImpl` (`:39`) | compte `OUT`, compte `OUT` en retard avec le `Clock` injecté, additionne les quantités par article puis les valorise par `ArticleService.getCosts` (une seule lecture des coûts, aucune si rien n'est dehors) |
| `ShootingRepository.countByStatus`, `countByStatusAndExpectedReturnAtBefore` (`repository/ShootingRepository.java:47`, `:50`) | comptages |
| `ShootingRepository.quantitiesByArticle(status)` (`:60`) | `List<ShootingArticleQuantity>` : Σ quantité par article sur les sorties d'un statut (`sum … group by`, sur `shooting_items`) |

### Frontend (`features/services`)

- `data-access/shooting-service.ts:41` `summary()` : `GET /api/v1/shootings/summary` → `ShootingSummaryRDto` (type ajouté dans `data-access/service-models.ts`).
- `components/shooting-kpis` (nouveau, `vvs-shooting-kpis`, entrée `summary`) : trois `vvs-kpi-card` (« Sorties en cours », « En retard » avec badge « À relancer » si au moins une, « Valeur immobilisée … Ar » avec la mention « dont n unité(s) sans coût » si besoin).
- `pages/services-page` : `canTrackShooting` (`services-page.ts:67`) ; `shootingSummary` (`:77`) chargé seulement à l'ouverture de l'onglet Shooting avec `shootings.track` ; relu avec la liste et le stock après une sortie ou une réintégration acceptée (`reloadShootings`, `:176`). L'onglet Shooting est désormais un `div role="tabpanel"` qui contient les indicateurs puis la carte des sorties (`services-page.html:68`).
- i18n : `services.shooting.kpi.*` dans `public/i18n/fr.json` et `mg.json`.

### Données

- Migration `2026_11_08-63-changelog.sql` (2 changesets, rollback fourni) : permission `shootings.track` (« Suivre les sorties shooting en cours et leur valeur immobilisée »), donnée à `ADMIN`.
- Aucune table ni colonne nouvelle ; aucune écriture dans le journal.

## Règles métier à respecter par les autres tickets

- **Sortie en cours** = statut `OUT` (Sorti ou En retard) ; une sortie réintégrée (`RETURNED`, SHO-02) sort des trois indicateurs.
- **En retard** = `OUT` et retour prévu **strictement** avant maintenant : même règle que `Shooting.isLateAt` et le champ `late` de SHO-01.
- **Valeur immobilisée** = quantité sortie × **coût d'achat actuel** de l'article (`articles.cost`, comme la valorisation STK-03), pas un coût figé au moment de la sortie. Un article sans coût n'est pas valorisé : ses unités sont dans `unvaluedQuantity`.
- **Droits** : la valeur révèle le coût d'achat, d'où une permission dédiée réservée à l'administrateur (comme `stocks.value`) ; la vendeuse garde la liste (`services.read`) sans les indicateurs.
- **JR-04 (clôture)** : la case « Shootings réintégrés » peut s'appuyer sur `lateCount == 0` (aucune sortie en retard) via `ShootingSummaryService` plutôt que de relire la liste.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/shooting/`)
- `controller/ShootingController.java` (action `getSummary`)
- `service/ShootingSummaryService.java`, `service/impl/ShootingSummaryServiceImpl.java` (nouveaux)
- `repository/ShootingRepository.java` (3 requêtes), `repository/ShootingArticleQuantity.java` (nouveau)
- `dto/ShootingSummaryRDto.java` (nouveau)

**Migrations Liquibase**
- `valimbavaka-core/src/main/resources/db/changelog/changes/2026_11_08-63-changelog.sql`, déclarée dans `db.changelog-master.yaml`

**Frontend** (`valimbavaka-ui/src/app/features/services/`)
- `components/shooting-kpis/*` (nouveau)
- `data-access/shooting-service.ts`, `data-access/service-models.ts`
- `pages/services-page/services-page.ts`, `.html`, `.css`
- `public/i18n/fr.json`, `mg.json` : `services.shooting.kpi.*`

**Tests**
- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/shooting/service/impl/ShootingSummaryServiceImplTest.java`, `controller/ShootingSummaryIT.java` (nouveaux) ; `controller/ShootingApiTestSupport.java` (`createArticle(String cost)`)
- Frontend : `components/shooting-kpis/shooting-kpis.spec.ts` (nouveau), `data-access/shooting-service.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** :
- [SHO-01](../SHO-01/description.md) · [guide](../SHO-01/guide.md) — entité `Shooting`, statut `OUT`, règle du retard, écran Shooting
- [SHO-02](../SHO-02/description.md) · [guide](../SHO-02/guide.md) — statut `RETURNED` (PR empilée)
- [STK-03](../STK-03/guide.md) — `ArticleService.getCosts` (coût d'achat), modèle de valorisation

**Ces tickets s'appuient sur lui** :
- _(aucun ticket déclaré ; JR-04 peut l'utiliser, voir les règles ci-dessus)_

## Comment l'utiliser depuis un autre ticket

```java
ShootingSummaryRDto summary = shootingSummaryService.getSummary(); // outCount, lateCount, immobilizedValue
```

```ts
inject(ShootingService).summary().subscribe((summary) => summary.lateCount);
```

## Points d'attention

- **Hypothèse (le critère ne nomme pas les 3 indicateurs)** : sorties en cours, sorties en retard, valeur immobilisée, d'après la user story et le guide SHO-01. À confirmer par Randy.
- **Hypothèse** : permission dédiée `shootings.track` réservée à l'administrateur (user story « En tant qu'administrateur », valeur au coût d'achat sensible) plutôt que `services.read`. Si la vendeuse doit voir les deux compteurs sans la valeur, il faudra scinder l'endpoint.
- Valeur au coût d'achat **actuel** : si le coût d'un article change pendant qu'il est dehors, la valeur immobilisée change aussi.
- Si l'appel échoue (réseau, 5xx), les indicateurs ne s'affichent simplement pas ; la liste garde son propre état d'erreur. Lecture seule, donc pas de file hors ligne.
- Les indicateurs ne sont pas affichés dans l'encart « Sorties en cours » de l'onglet Salon (le critère vise l'écran Shooting).
- `ROLE_PERMISSIONS` (`core/auth/auth-testing.ts`) n'a pas été complété avec les permissions shooting (comme pour SHO-01/02) : la page a11y reste testée sur l'onglet Salon.
- Tests exécutés en CI le 2026-11-08 (10 backend, 4 frontend ajoutés, tous verts) ; recette manuelle 🧪 à passer.
- PR empilée sur SHO-02 (`feature/SHO-02-reintegration-produit-shooting`) : à merger après elle.
