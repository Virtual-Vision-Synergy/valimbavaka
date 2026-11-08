# Guide d'intégration — TRF-02 · Génération automatique des transferts du matin

Statut : ✅ écrit le 2026-11-08 — tests verts en CI, voir « Résultat de l'exécution » du scénario · Ticket : [#55](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/55) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Les transferts du matin sont **générés par l'ouverture de journée de JR-01** (`POST /api/v1/days`) : un bon `MORNING` par destination, `VALIDATED` dès sa création, sans seconde saisie. TRF-02 ne les regénère pas ; il complète ce qui manquait au critère « apparaît immédiatement avec le statut Validé » : chaque bon **nomme sa journée** (note `Ouverture de la journée du 08/11/2026`), la carte de fin de l'ouverture **relit les bons dans l'historique** (pastille « Validé ») et **mène à l'historique filtré** `/transfers?origin=MORNING`, et des tests prouvent de bout en bout que les bons sont listés aussitôt, validés, avec l'administrateur comme émetteur.

## Contrat exposé

### Endpoints backend

Aucun nouvel endpoint. Utilisés tels quels :

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie | Ce que TRF-02 en attend |
|---|---|---|---|---|---|
| POST | `/api/v1/days` (JR-01) | `days.open` | `DayOpeningWDto` | 201 `BusinessDayRDto` | génère les bons `MORNING` / `VALIDATED`, désormais notés avec la journée |
| GET | `/api/v1/transfers?origin=MORNING` (TRF-01) | `transfers.read` | `PageQuery` + `origin` | 200 `PageResponse<TransferSummaryRDto>` | liste les bons du jour en tête, aussitôt après l'ouverture |

### Services et ports Java réutilisables

- Aucun nouveau port. `BusinessDayService.openDay` (`feature/day/service/BusinessDayService.java:23`) passe maintenant la note de journée à `TransferService.record` : `BusinessDayServiceImpl.openDay` (`feature/day/service/impl/BusinessDayServiceImpl.java:78`), `morningNote` (`:112`).

### Frontend

- Route `/transfers` : nouvelle entrée `origin` lue dans l'adresse (`/transfers?origin=MORNING`, liaison `withComponentInputBinding`) ; `TransferListPage.origin` (`input`, `transfer-list-page.ts:65`) initialise le filtre `originFilter` (`:66`), que les puces modifient ensuite. Valeur inconnue → « Toutes ».
- `features/day/pages/day-open-page` (carte de fin) :
  - `morningTransfers` (`day-open-page.ts:94`) : `TransferService.list({ page: 1, size: MORNING_HISTORY_SIZE (20), origin: 'MORNING' })` dès qu'il y a des bons à afficher et que l'utilisateur a `transfers.read` ;
  - `statusByNumber` (`:101`) : statut par numéro de bon, affiché par `vvs-status-badge` (`TRANSFER_STATUS_TONES`, `transfers.status.*`) ;
  - lien « Voir les transferts du matin » vers `/transfers?origin=MORNING`, seulement avec `transfers.read` (`canReadTransfers`, `:91`).
- Traductions (fr, mg) : `day.open.goTransfers`.

### Données

- Aucune migration. La note est la colonne existante `transfers.note` (TRF-03, 500 caractères au plus).
- Journal : inchangé, maintenant vérifié bon par bon — une ligne `TRANSFER` / `CREATE` (« Transfert du matin TRF-0093 · Entrepôt → Boutique : … ») par bon, plus la ligne `BUSINESS_DAY` / `CREATE` de JR-01 qui liste les numéros.

## Règles métier à respecter par les autres tickets

- **Un transfert du matin n'est créé que par l'ouverture de journée** (`BusinessDayService.openDay`) ; il n'existe pas d'autre moyen de générer des bons `MORNING`. Son statut vient de `TransferOrigin.initialStatus` (`VALIDATED`), jamais posé à la main.
- **Note** : `Ouverture de la journée du dd/MM/yyyy` (date d'Antananarivo de la journée ouverte). JR-04 (réconciliation) peut s'en servir pour l'affichage, mais doit retrouver les bons du jour par `business_day_transfers.transfer_id` (JR-01), pas en analysant la note.
- **Émetteur** : l'administrateur qui ouvre la journée (`createdBy` de l'audit, requête authentifiée) ; « Système » n'apparaît que pour un bon généré hors requête.
- **Tout ou rien** : un refus de l'ouverture (409, 422, 404) ne laisse aucun bon du matin.
- **Droits** : générer = `days.open` ; voir l'historique et le statut relu = `transfers.read` (administrateur).

## Fichiers concernés

**Backend**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/day/service/impl/BusinessDayServiceImpl.java` (note de journée)
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/day/service/BusinessDayService.java` (Javadoc)

**Migrations Liquibase** : aucune.

**Frontend** (`valimbavaka-ui/`)
- `src/app/features/day/pages/day-open-page/day-open-page.ts`, `.html`, `.css`
- `src/app/features/transfers/pages/transfer-list-page/transfer-list-page.ts`, `.html`
- `public/i18n/fr.json`, `mg.json`

**Tests**
- Backend : `src/test/java/org/vvs/valimbavaka/feature/day/controller/MorningTransferIT.java` (4, nouveau) ; `feature/day/service/impl/BusinessDayServiceImplTest.java` (1 ajouté)
- Frontend : `features/day/pages/day-open-page/day-open-page.spec.ts` (2 ajoutés) ; `features/transfers/pages/transfer-list-page/transfer-list-page.spec.ts` (1 ajouté)

## Dépendances

**Ce ticket s'appuie sur** :
- [JR-01](../JR-01/guide.md) : ouverture de journée et génération des bons du matin (branche de base)
- [JNL-01](../JNL-01/guide.md) : journal
- [TRF-03](../TRF-03/guide.md) : `TransferService.record`, statut par origine, note
- [TRF-01](../TRF-01/guide.md) : historique filtrable par origine
- [TRF-04](../TRF-04/guide.md) : détail du bon, où la note apparaît (fusionné dans `develop`, pas encore dans la base JR-01)

**Ces tickets s'appuient sur lui** :
- [JR-04](../JR-04/guide.md) · réconciliation de clôture : composante « transféré » = lignes des bons du jour

## Comment l'utiliser depuis un autre ticket

Mener à l'historique des transferts du matin (ou d'une autre origine) :

```html
<a routerLink="/transfers" [queryParams]="{ origin: 'MORNING' }">Transferts du matin</a>
```

Relire les bons du matin les plus récents (avec `transfers.read`) :

```ts
this.transferService.list({ page: 1, size: 20, origin: 'MORNING' });
```

## Points d'attention

- **Hypothèse (périmètre)** : JR-01 génère déjà les bons validés ; TRF-02 ne livre que l'écart (affichage du statut, lien, note de traçabilité, tests de bout en bout), sans migration ni endpoint.
- **Hypothèse (statut affiché)** : relu dans l'historique (`GET /api/v1/transfers`) plutôt que copié dans `business_day_transfers` : la carte montre le statut réel du serveur (y compris un futur `CANCELLED`, JNL-02). Une page de 20 bons du matin suffit (un par destination, une ouverture par jour) ; un bon absent de la page s'affiche sans pastille.
- **Hypothèse (note)** : texte fixe en français, comme les libellés du journal ; il n'est pas traduit à l'affichage.
- **Branche empilée** : la base est `feature/JR-01-repartition-matinale-stock`. TRF-04 (fusionné dans `develop`) modifie aussi `transfer-list-page.ts` / `.html` / `.spec.ts` ; les zones touchées ici sont distinctes (import `input`, signal `originFilter`, liaison `[(value)]`, test inséré au milieu du fichier), la fusion devrait être automatique.
- La note n'est visible à l'écran que dans le détail d'un bon (TRF-04) ; l'historique ne l'affiche pas.
