# Guide d'intégration — JR-05 · Motif obligatoire en cas d'écart

Statut : ✅ écrit le 11/11/2026 — tests écrits, à lancer (voir « Résultat de l'exécution » du scénario) · Ticket : [#92](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/92) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

`PUT /api/v1/days/closing/counts` enregistre le **comptage du soir** de la journée : pour chaque ligne (emplacement, article) la quantité comptée et, quand elle diffère du théorique calculé par le serveur, un motif (`BREAKAGE`, `LOSS`, `ENTRY_ERROR`, `SAMPLE`, `OTHER`). Un écart sans motif est refusé en 422, tout l'enregistrement est annulé, quel que soit l'écran qui appelle. Le comptage est stocké (table `day_closing_counts`, théorique figé à l'enregistrement) et `GET /api/v1/days/closing` le renvoie sur chaque ligne ; JR-06 s'appuiera sur ces lignes pour figer la clôture. Le verrouillage lui-même n'est pas livré ici.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée (`XxxWDto`) | Sortie (`XxxRDto`) | Erreurs |
|---|---|---|---|---|---|
| PUT | `/api/v1/days/closing/counts` | `days.close` (administrateur) | `DayClosingCountsWDto` : `counts` (1 à 1000 `ClosingCountWDto` : `locationId`, `articleId`, `counted` ≥ 0 à 3 décimales, `reason` facultatif) | 200 `DayClosingRDto` (la réconciliation relue avec les comptages) | 400 (motif inconnu), 401, 403, 404 (ligne hors clôture), 409 (journée non ouverte ou déjà clôturée, enregistrement simultané), 422 (écart sans motif, ligne en double, valeur invalide) |
| GET | `/api/v1/days/closing` | `days.close` | aucune | 200 `DayClosingRDto` | inchangé (JR-04) ; la réponse gagne `businessDayId` et, par ligne, `counted` et `reason` |

- `ClosingLineRDto` : deux champs ajoutés à la fin, `counted` (comptage enregistré, `null` tant que la ligne n'est pas comptée) et `reason` (`null` sans écart ou sans comptage). `gap` garde son sens JR-04 (stock du système − théorique), il **ne** mesure **pas** le comptage : l'écart du comptage est `counted − theoretical`.
- `DayClosingRDto` : champ `businessDayId` ajouté en tête.
- Contrôleur : `feature/dayclosing/controller/DayClosingController.java:55` (même classe que JR-04, `@PutMapping("/closing/counts")`).

### Services et ports Java réutilisables

| Port | Méthode | Rôle |
|---|---|---|
| `feature.dayclosing.service.DayClosingCountService` | `DayClosingRDto saveCounts(DayClosingCountsWDto)` | Valide et enregistre les comptages d'aujourd'hui, tout ou rien (`DayClosingCountServiceImpl.java:62`). |
| `feature.dayclosing.service.DayClosingService` | `DayClosingRDto getClosing()` | Inchangé, merge les comptages enregistrés dans les lignes (`DayClosingServiceImpl.java:77`). |
| `feature.dayclosing.repository.DayClosingCountRepository` | `findByBusinessDayId(UUID)`, `saveAll` | Lignes de comptage d'une journée ; pas de suppression (JNL-02). |

Règle d'enregistrement (`DayClosingCountServiceImpl`) : la réconciliation du jour est relue (`getClosing()`, 409 si la journée n'est pas ouverte), chaque comptage est rapproché de sa ligne (404 sinon, 422 si la même ligne est envoyée deux fois), puis tout écart sans motif refuse la requête entière (422, `requireReasons`, l'erreur nomme les articles). Les lignes sont créées ou remplacées (`DayClosingCount.create` / `recount`) ; un comptage identique à celui déjà enregistré ne change rien. Un motif envoyé sans écart est ignoré.

### Frontend (`features/day`)

- `data-access/day-service.ts` : `saveCounts(request)` → `PUT /api/v1/days/closing/counts` par la file hors ligne (`OutboxService.submit`) ; renvoie `SubmitResult<DayClosing>` (`body` = la réconciliation relue, convertie par `toDayClosing`). `closing()` reprend `counted` et `reason` du serveur ; sans comptage enregistré, le comptage proposé reste le stock du système.
- `data-access/day-models.ts` : `ClosingLineRDto.counted` / `reason`, `DayClosingRDto.businessDayId`, `ClosingLocation.locationId`, types `ClosingCountWDto` et `DayClosingCountsWDto`.
- `data-access/closing-state.ts` : `toCountsRequest()` (toutes les lignes affichées ; le motif n'est envoyé que si l'écart n'est pas nul), `missingReasons` (inchangé).
- `pages/day-close-page` : bloc « Comptage du soir » (bouton « Enregistrer le comptage », désactivé tant qu'un écart n'a pas de motif, alerte `role="alert"` pour un refus du serveur) ; après un enregistrement en ligne, la réponse remplace la réconciliation affichée.
- Traductions (fr, mg) : `day.close.count.{title,save,saved,missing,ready}`.

### Données

- Migration `2026_11_11-21-changelog.sql` (1 changeset, rollback fourni) : table `day_closing_counts` (`business_day_id`, `location_id`, `article_id`, `theoretical_quantity`, `counted_quantity`, `reason`, `counted_at` + champs d'audit et `version`), unicité (jour, emplacement, article), clés étrangères vers `business_days`, `stock_locations`, `articles`, contraintes `ck_day_closing_counts_counted` (≥ 0), `…_reason_value` (cinq valeurs), `…_reason_gap` (motif si et seulement si écart).
- Journal : une ligne `UPDATE` sur la ressource `BUSINESS_DAY` (id de la journée) par enregistrement qui change quelque chose : « Comptage de clôture du 11/11/2026 : 12 ligne(s) enregistrée(s), dont 3 écart(s) motivé(s) » ; aucune ligne si rien n'a changé.
- Aucune nouvelle permission : `days.close` (JR-04).

## Règles métier à respecter par les autres tickets

- **Écart** (comptage) = `counted − theoretical`, calculé par le serveur ; le client n'envoie jamais le théorique ni l'écart.
- **Motif** : obligatoire si et seulement si `counted ≠ theoretical` (comparaison sans tenir compte de l'échelle). Cinq valeurs : casse, perte ou vol, erreur de saisie, échantillon offert, autre.
- **Théorique figé** : `theoretical_quantity` est celui du dernier enregistrement. Une vente ou un mouvement posté ensuite change le théorique affiché par `GET /closing` sans toucher la ligne. **JR-06** doit donc, au verrouillage, relire la réconciliation, vérifier que chaque ligne est comptée, que `counted − theoretical` est toujours justifié par un motif, puis figer les lignes (par exemple en appelant `DayClosingService.getClosing()` puis en comparant à `DayClosingCountRepository.findByBusinessDayId`) ; ne pas se fier à `theoretical_quantity` seul.
- **Journée clôturée** : `saveCounts` refuse déjà une journée dont le statut n'est pas `OPEN` (409). JR-06 n'a qu'à passer le statut à `CLOSED` pour interdire tout nouveau comptage.
- **Lignes non comptées** : un enregistrement ne touche que les lignes envoyées ; rien n'oblige encore à compter toutes les lignes (exigence de JR-06).
- **Droit** : `days.close` (administrateur seul).

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/dayclosing/`)
- `controller/DayClosingController.java` (méthode `saveCounts`) ; `service/DayClosingCountService.java` ; `service/impl/DayClosingCountServiceImpl.java`, `DayClosingServiceImpl.java` (comptages fusionnés dans les lignes)
- `domain/DayClosingCount.java`, `CountResult.java`, `ClosingGapReason.java` ; `repository/DayClosingCountRepository.java`
- `dto/ClosingCountWDto.java`, `DayClosingCountsWDto.java` ; `ClosingLineRDto.java` et `DayClosingRDto.java` (champs ajoutés)
- Migration : `src/main/resources/db/changelog/changes/2026_11_11-21-changelog.sql`, déclarée dans `db.changelog-master.yaml`

**Frontend** (`valimbavaka-ui/src/app/`)
- `features/day/data-access/day-models.ts`, `day-service.ts`, `closing-state.ts` ; `features/day/pages/day-close-page/` (`.ts`, `.html`, `.css`) ; `public/i18n/fr.json`, `mg.json`

**Tests**
- Backend : `feature/dayclosing/controller/DayClosingCountIT.java`, `feature/dayclosing/service/impl/DayClosingCountServiceImplTest.java`, `DayClosingServiceImplTest.java` (2 tests ajoutés), `feature/dayclosing/domain/DayClosingCountTest.java`
- Frontend : `features/day/pages/day-close-page/day-close-page.spec.ts` (5 tests ajoutés), `features/day/data-access/day-service.spec.ts` (3 tests ajoutés)

## Dépendances

**Ce ticket s'appuie sur** :
- [JR-04](../JR-04/guide.md) : réconciliation (`DayClosingService.getClosing()`), permission `days.close`, écran `/day/close` ; [JR-01](../JR-01/guide.md) : journée ; [JNL-01](../JNL-01/guide.md) : journal ; [SYS-02](../SYS-02/guide.md) / [SYS-07](../SYS-07/guide.md) : idempotence et file hors ligne.

**Ces tickets s'appuient sur lui** :
- [JR-06](../JR-06/description.md) (verrouillage de la clôture du jour)

## Comment l'utiliser depuis un autre ticket

```java
var closing = dayClosingService.getClosing();                    // lignes avec counted / reason
var counts = dayClosingCountRepository.findByBusinessDayId(closing.businessDayId());
counts.forEach(c -> c.hasGap());                                 // écart du comptage figé ; reason non nul si écart
```

```ts
this.dayService.saveCounts(this.state.toCountsRequest()); // Observable<SubmitResult<DayClosing>>
```

## Points d'attention

- **Hypothèse (« clôture validée »)** : le critère parle d'une clôture « validée » ; comme le verrouillage relève de JR-06, JR-05 livre l'enregistrement du comptage, qui est le point où le motif est exigé et vérifié côté serveur. La page garde `underConstruction` tant que la caisse et le verrouillage sont en démonstration. À valider par le PO.
- **Hypothèse (liste de motifs)** : celle de la maquette (casse, perte ou vol, erreur de saisie, échantillon offert, autre), distincte de celle des sessions d'inventaire (INV-03 : « shooting non déclaré » à la place de « échantillon offert »). Pas de texte libre pour « Autre motif » : à ajouter si le PO veut plus de traçabilité.
- **Hypothèse (envoi de toutes les lignes)** : l'écran envoie tout le tableau, y compris les lignes que l'administrateur n'a pas touchées (comptage proposé = stock du système) ; pour une ligne dont le stock du système diffère du théorique (inventaire, production, réception du jour), un motif est donc demandé même sans saisie.
- **Théorique figé et nouvelle vente** : voir les règles ci-dessus ; l'écran désactive l'enregistrement tant qu'un écart affiché n'a pas de motif, le verrouillage (JR-06) devra refaire la vérification.
- **Contrainte de suppression** : la clé étrangère `fk_day_closing_counts_day` empêche de supprimer une journée qui a des comptages. Plusieurs classes de test font `delete from business_days` : `DayClosingCountIT` supprime ses lignes après chaque test ; toute nouvelle classe qui enregistre des comptages doit en faire autant.
- **Reste en démonstration** : la caisse (espèces attendues / comptées), la liste de contrôle et le verrouillage (`closingExtras()`, `DayService.lock()`) : JR-06. Consommation salon dans la clôture : SRV-04, non touché.
- **Idempotence** : l'écriture passe par la file hors ligne avec `Idempotency-Key` ; un rejeu identique est de toute façon sans effet (aucune écriture ni ligne de journal).
- La recette manuelle 🧪 reste à passer (cinq étapes, voir le scénario).
