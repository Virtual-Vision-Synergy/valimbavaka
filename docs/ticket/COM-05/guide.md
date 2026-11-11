# Guide d'intégration — COM-05 · Recalcul et export des commissions

Statut : ✅ rédigé · Ticket : [#89](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/89) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

L'administrateur recalcule les commissions non validées d'une période (une vendeuse ou toutes) après un changement de taux ou de règle, et exporte les commissions en Excel ou en PDF pour la paie. Le recalcul applique exactement la règle de COM-02 (décision PO du 07/11/2026 : service salon, puis canal, puis vendeuse, sans cumul) ; il ne touche jamais une commission validée ou annulée, est idempotent, borné à un an et tracé au journal. L'export réutilise les routes jumelles de SYS-01 : la vendeuse n'exporte que son relevé (le serveur impose son identifiant), l'administrateur exporte tout ou une vendeuse. Recalcul et export confirment leur exécution à l'écran.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| POST | `/api/v1/commissions/recalculation` | `commissions.recalculate` (nouvelle, `ADMIN` seulement) | `CommissionRecalculationWDto` : `from`, `to` (obligatoires, incluses), `sellerId` (facultatif : toutes les vendeuses sans lui) | `CommissionRecalculationRDto` : période, `sellerId`, `updatedCount`, `unchangedCount`, `validatedCount` (validées laissées telles quelles), `previousAmount`, `newAmount` (sur les commissions examinées) | 401, 403, 422 (champ absent, `from` après `to`, période d'un an ou plus) |
| GET | `/api/v1/commissions/export` et `/export/pdf` | `commissions.read-all` (`ADMIN`) | mêmes paramètres que `GET /commissions` : `from`, `to`, `sellerId`, tri ; sans `page` ni `size`, tout est exporté (10 000 lignes au plus) | xlsx (feuille « Commissions », `commissions.xlsx`) ou PDF | 401, 403, 422 (période inversée, plus de 10 000 lignes) |
| GET | `/api/v1/commissions/follow-up/lines/export` et `/export/pdf` | `commissions.read` (vendeuse et administrateur) | mêmes paramètres que le détail du suivi COM-04 : `month` (AAAA-MM), `sellerId` (ignoré pour une vendeuse) | xlsx (feuille « Relevé », `releve-commissions.xlsx`) ou PDF | 401, 403, 404 (compte non vendeuse), 422 (administrateur sans `sellerId`) |

Colonnes des fichiers (annotées sur `CommissionRDto`) : Date, Opération, Vendeuse, CA (Ar), Base (Ar), Taux (%), Commission (Ar), Statut (`CALCULATED`, `VALIDATED`, `CANCELLED`). Aucun identifiant n'en sort ; les commissions annulées sont dans le fichier avec leur statut.

### Services et ports Java réutilisables

- `CommissionRecalculationService.recalculate(request)` (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/commission/service/CommissionRecalculationService.java`), implémenté par `CommissionRecalculationServiceImpl.java:57`. Une transaction ; une ligne de journal par commission modifiée ; rien n'est écrit si rien ne change.
- `Commission.recalculate(purchaseCost, terms)` (`domain/Commission.java:136`) : rejoue `applyTerms`, le même calcul que `Commission.calculate` ; lève `IllegalStateException` si la commission n'est pas `CALCULATED` ; renvoie si quelque chose a changé.
- `ApplicableCommissionRDto.toTerms()` (`dto/ApplicableCommissionRDto.java:23`) : unique conversion du taux du résolveur vers `CommissionTerms`, utilisée par `CommissionServiceImpl.calculate` (`:76`) et par le recalcul (`CommissionRecalculationServiceImpl.java:94`). Le taux vient toujours de `CommissionRateResolver.resolve`.
- `CommissionRepository.findByStatusInPeriod` et `countByStatusInPeriod` (statut, vendeuse facultative, période).

### Frontend (`valimbavaka-ui/src/app/features/commissions/`)

- `commission-list-page` (Commissions calculées, `commissions.read-all`) : bouton « Recalculer » (réservé à `commissions.recalculate`, confirmation par `vvs-confirm-dialog`, message chiffré au retour, `confirmRecalculation()`), boutons « Exporter en Excel » et « Exporter en PDF » du mois affiché (`exportAs()`).
- `commissions-page` (Mes commissions) : « Exporter mon relevé » (Excel, clé `commissions.export` inchangée) et « Relevé en PDF » (`exportStatement()`), mois affiché et vendeuse de l'URL pour l'administrateur.
- `data-access/commission-service.ts` : `recalculate(request)` ; types `CommissionRecalculationWDto` et `CommissionRecalculationRDto` dans `commission-models.ts`. L'export passe par `ExportService.download` (SYS-01), qui télécharge le fichier et confirme par une notification.
- Clés de traduction `commissionList.recalculate.*`, `commissionList.export.*`, `commissions.exportPdf` (fr, mg).

### Données

- Migration `2026_11_11-62-changelog.sql` (rollback fourni) : permission `commissions.recalculate` donnée à `ADMIN`. Aucune table ni colonne nouvelle.
- Journal : `UPDATE` sur `COMMISSION`, « Commission VTE-… · vendeuse recalculée : ancien → nouveau Ar (taux x % → y %) ». L'export n'est pas journalisé (voir Points d'attention).

## Règles métier à respecter par les autres tickets

- Un seul calcul de commission : tout nouveau niveau de taux (ex. SRV-01) se branche dans `CommissionRateResolver` ; calcul et recalcul le suivent sans autre changement.
- Recalcul : seules les `CALCULATED` sont modifiées. `VALIDATED` est définitive (COM-03), `CANCELLED` ne compte plus. Le chiffre d'affaires, l'opération, la vendeuse, le canal et la journée ne changent jamais ; le coût d'achat d'une vente est relu (`SaleService.getPurchaseCost`).
- Idempotence : deuxième lancement identique = `updatedCount = 0`, aucune ligne de journal.
- Période obligatoire et bornée : de 0 à 365 jours d'écart (366 ou plus : 422).
- Export : toute colonne ajoutée à `CommissionRDto` avec `@ExcelColumn` sortira dans les deux fichiers, pour l'administrateur comme pour la vendeuse : n'y mettre que ce qu'une vendeuse peut voir de ses propres commissions.

## Fichiers concernés

**Créés** : `CommissionRecalculationService`, `CommissionRecalculationServiceImpl`, `CommissionRecalculationWDto`, `CommissionRecalculationRDto`, migration `2026_11_11-62`, `CommissionRecalculationIT`, `CommissionExportIT`, `CommissionRecalculationServiceImplTest`, `commission-list-page-actions.spec.ts`. **Modifiés** : `Commission`, `ApplicableCommissionRDto`, `CommissionServiceImpl` (utilise `toTerms`), `CommissionRDto` (`@ExcelColumn`), `CommissionRepository`, `CommissionController`, `CommissionTest`, `commission-list-page` (ts, html, css), `commissions-page` (ts, html, css, spec), `commission-service.ts` (+ spec), `commission-models.ts`, `fr.json`, `mg.json`, `auth-testing.ts`, `db.changelog-master.yaml`.

## Dépendances

**Ce ticket s'appuie sur** :
- [COM-02](../COM-02/description.md) · [guide](../COM-02/guide.md)
- [COM-03](../COM-03/description.md) · [guide](../COM-03/guide.md)
- [COM-04](../COM-04/description.md) · [guide](../COM-04/guide.md)
- [SYS-01](../SYS-01/description.md) · [guide](../SYS-01/guide.md)
- [JNL-01](../JNL-01/description.md) · [guide](../JNL-01/guide.md)

**Ces tickets s'appuient sur lui** :
- _(rien)_

## Comment l'utiliser depuis un autre ticket

```text
POST /api/v1/commissions/recalculation  { "from": "2026-11-01", "to": "2026-11-30", "sellerId": "<uuid ou absent>" }
GET  /api/v1/commissions/export?from=2026-11-01&to=2026-11-30&sellerId=<uuid>       (administrateur, xlsx ; /export/pdf pour le PDF)
GET  /api/v1/commissions/follow-up/lines/export?month=2026-11                       (vendeuse : son relevé)
```

## Points d'attention

- Hypothèse : seul l'administrateur recalcule (`commissions.recalculate`) ; la description ne nomme pas qui peut le lancer, le rôle « Administrateur » est celui de la user story.
- Hypothèse : les commissions `VALIDATED` et `CANCELLED` ne sont jamais recalculées (la description ne demande pas le contraire). Corriger le versement d'une commission déjà validée reste un geste manuel de l'administrateur (voir COM-03, vente annulée après validation).
- Hypothèse : le recalcul porte sur le mois affiché, toutes vendeuses, depuis l'écran ; l'API accepte une vendeuse et une période d'un an au plus (aucun écran ne choisit une vendeuse ni une période libre).
- Le recalcul applique les taux et règles en vigueur le jour du recalcul à des ventes anciennes : c'est voulu (corriger après un changement), mais il n'y a pas d'historique des taux ; une règle de canal désactivée entre-temps ne sera plus appliquée.
- L'export n'est pas tracé au journal : `journal_entries` n'accepte que CREATE / UPDATE / DELETE / CANCEL / REVERSE et SYS-01 ne publie qu'un événement `ExcelExportedEvent` sans écouteur (voir le guide SYS-01). Aucun ticket ne porte l'action `EXPORT` ; à créer côté JNL si le client l'exige.
- Les statuts sortent tels quels dans les fichiers (`CALCULATED`, `VALIDATED`, `CANCELLED`) et les annulées y figurent ; filtrer sur le statut dans Excel pour la paie, ou demander un filtre serveur si besoin.
- Les en-têtes de fichier sont en français uniquement (annotations Java), comme les autres exports.
- L'export de l'administrateur est celui du mois affiché (`from` et `to` du mois) ; l'API accepte n'importe quelle période et, sans `from`/`to`, exporte tout (10 000 lignes au plus).
- La concurrence validation / recalcul repose sur le verrou optimiste de `BaseEntity` ; non rejoué en test.
- Les pages sont dans `demo-pages.a11y.spec.ts` (Mes commissions) ; les nouveaux boutons ont un libellé texte, à vérifier en recette avec le dialogue de confirmation.
