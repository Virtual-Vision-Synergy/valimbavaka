# Scénarios de test — TRF-02 · Génération automatique des transferts du matin

Statut : ✅ tests verts en CI le 2026-11-08 ; recette manuelle (🧪) à passer · Ticket : [#55](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/55) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Écart traité par ce ticket

La génération elle-même est livrée par JR-01 : `POST /api/v1/days` crée un bon `MORNING` par destination, déjà `VALIDATED` (`TransferOrigin.initialStatus`), dans la même transaction que l'ouverture. TRF-02 ne regénère rien ; il livre seulement ce qui manquait au critère :

| Axe | Déjà livré (JR-01, TRF-01, TRF-03) | Ajouté par TRF-02 |
|---|---|---|
| Génération | un bon `MORNING` / `VALIDATED` par destination (`BusinessDayIT.openDay_twoDestinations_returns201WithOneValidatedMorningTransferPerDestination`) | rien |
| Contrôle | statut lu en base seulement | preuve de bout en bout : le bon est listé **aussitôt** par l'historique `GET /api/v1/transfers?origin=MORNING`, `VALIDATED`, émis par l'administrateur ; un refus (409) ne laisse aucun bon du matin |
| Affichage | carte de fin : numéros des bons | statut « Validé » relu dans l'historique sur chaque bon de la carte ; lien « Voir les transferts du matin » vers `/transfers?origin=MORNING` ; l'historique accepte `?origin=` dans l'adresse |
| Traçabilité | journal `BUSINESS_DAY` (numéros des bons) et `TRANSFER` (« Transfert du matin … ») | chaque bon porte la note « Ouverture de la journée du 08/11/2026 » (visible dans le détail TRF-04) ; trace `TRANSFER` vérifiée bon par bon |

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (permissions `days.open` et `transfers.read`) ; `SELLER` pour un utilisateur sans `transfers.read` (écran) |
| Données de départ | Entrepôt : « Savon noir » 20 (article fini actif) ; emplacements actifs `WAREHOUSE`, `STORE` (Boutique), `DELIVERY` (Livraison), `TRADE_SHOW` (Salon) ; aucune journée ouverte pour la date du jour (fuseau Indian/Antananarivo) |
| Tickets à avoir déployés avant | JR-01 (ouverture de journée, branche de base), TRF-03 (`TransferService.record`), TRF-01 (historique), JNL-01 (journal) |
| Migration | aucune |

## Scénarios issus des critères d'acceptation

### Critère 1 — Un transfert généré par l'ouverture apparaît immédiatement avec le statut Validé

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| 1 · Visible aussitôt dans l'historique | ouverture : Savon 12 → Boutique, Savon 5 → Livraison ; puis `GET /api/v1/transfers?origin=MORNING&search=<numéro>` pour chaque bon renvoyé | 200, 1 ligne par numéro : `origin` = `MORNING`, `status` = `VALIDATED`, départ = entrepôt, `createdBy` = id de l'administrateur, `createdByName` = son prénom et nom | `MorningTransferIT.openDay_twoDestinations_listsEachMorningTransferAtOnceAsValidatedAndIssuedByTheAdmin` | ✅ auto |
| 1b · Statut fixé à la création (rappel JR-01) | même ouverture, lecture de `transfers` | chaque bon `MORNING` / `VALIDATED` | `BusinessDayIT.openDay_twoDestinations_returns201WithOneValidatedMorningTransferPerDestination` (JR-01) | ✅ auto (CI JR-01) |
| 1c · Le bon nomme sa journée | ouverture : Savon 2 → Boutique, Savon 3 → Salon | `transfers.note` de chaque bon = `Ouverture de la journée du 08/11/2026` (date du jour renvoyée par l'ouverture) | `MorningTransferIT.openDay_validDispatch_notesTheOpeningDayOnEachMorningTransfer`, `BusinessDayServiceImplTest.openDay_twoDestinations_notesTheOpeningDayOnEachMorningTransfer` | ✅ auto |
| 1d · Écran : statut sur la carte de fin | administrateur ; ouverture Boutique 12, Livraison 5 → `TRF-0093`, `TRF-0094` ; l'historique renvoie ces deux bons `VALIDATED` | une requête `list({ page: 1, size: 20, origin: 'MORNING' })` ; chaque bon de la carte porte la pastille « Validé » ; lien « Voir les transferts du matin » → `/transfers?origin=MORNING` | `day-open-page.spec.ts` « relit les bons générés dans l’historique : statut Validé et lien vers les transferts du matin » | ✅ auto |
| 1e · Écran : historique ouvert sur les transferts du matin | `/transfers?origin=MORNING` | requête `origin=MORNING`, `page=1` ; puce « Répartition matinale » pressée | `transfer-list-page.spec.ts` « should open on the origin given in the address (link from the opening of the day) » | ✅ auto |

## Sécurité et erreurs

TRF-02 n'ajoute **aucun endpoint** : il s'appuie sur `POST /api/v1/days` (JR-01) et `GET /api/v1/transfers` (TRF-01), dont les refus sont déjà testés.

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Ouverture sans jeton / mauvais rôle | `POST /api/v1/days` sans `Authorization` ; jeton vendeuse | 401 `UNAUTHENTICATED` ; 403 `FORBIDDEN`, aucun bon | `BusinessDayIT.openDay_withoutToken_returns401`, `BusinessDayIT.openDay_withSellerRole_returns403` (JR-01) | ⏭ hors périmètre (JR-01) |
| Historique sans `transfers.read` | `GET /api/v1/transfers` avec un jeton vendeuse / livreur | 403 `FORBIDDEN` | `TransferHistoryIT.getTransfers_withSellerRole_returns403`, `getTransfers_withDeliveryDriverRole_returns403` (TRF-01) | ⏭ hors périmètre (TRF-01) |
| Écran sans `transfers.read` | ouverture affichée à un utilisateur sans `transfers.read` | aucune lecture de l'historique, ni pastille ni lien (le serveur refuserait de toute façon) | `day-open-page.spec.ts` « sans accès à l’historique, ne relit pas les bons et ne propose pas le lien » | ✅ auto |
| Mass assignment | `status`, `id`, `businessDate` glissés dans le corps de l'ouverture | ignorés ; les bons restent `VALIDATED` | `BusinessDayIT.openDay_massAssignmentOfIdDateAndStatus_isIgnored` (JR-01) | ⏭ hors périmètre (JR-01) |
| Refus 409 : aucun bon du matin | Savon 5 à l'entrepôt ; demandé 3 → Boutique + 3 → Livraison | 409 ; le nombre de transferts `MORNING` est inchangé (tout ou rien) | `MorningTransferIT.openDay_aboveWarehouseStock_generatesNoMorningTransfer` | ✅ auto |
| Journal | ouverture : Savon 4 → Boutique | une ligne `TRANSFER` / `CREATE` par bon, libellé `Transfert du matin TRF-… · …` ; la ligne `BUSINESS_DAY` liste les numéros (JR-01) | `MorningTransferIT.openDay_validDispatch_tracesEachMorningTransferInTheJournal`, `BusinessDayIT.openDay_validDispatch_isTracedInTheJournal` | ✅ auto |

## Cas limites

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Ouverture sans répartition | `{"lines":[]}` | journée ouverte, aucun bon ; la carte n'interroge pas l'historique (aucun bon à relire) | `BusinessDayIT.openDay_noLine_opensTheDayWithoutTransfer` (JR-01) ; garde `vouchers().length > 0` dans `DayOpenPage.morningTransfers` | ⏭ hors périmètre (JR-01) |
| Seconde ouverture le même jour | deux `POST /api/v1/days` | 409, aucun second jeu de bons | `BusinessDayIT.openDay_alreadyOpenedToday_returns409` (JR-01) | ⏭ hors périmètre (JR-01) |
| Rejeu hors ligne | même `Idempotency-Key` | une seule ouverture, mêmes bons | `BusinessDayIT.openDay_replayedWithSameIdempotencyKey_opensTheDayOnce` (JR-01) | ⏭ hors périmètre (JR-01) |
| Ouverture en file hors ligne | `queued: true` | pas de carte de fin, donc pas de lecture de l'historique | `day-open-page.spec.ts` « met l’ouverture en file hors connexion et empêche un second envoi » (JR-01) | ⏭ hors périmètre (JR-01) |
| Origine inconnue dans l'adresse | `/transfers?origin=TRUCK` | puce « Toutes », aucune origine envoyée (`TRANSFER_ORIGINS.find` → `ALL`) | lecture de `TransferListPage.originFilter` | 🔍 code |
| Bon absent de la page d'historique relue | plus de 20 transferts du matin plus récents que ceux du jour (impossible avec une ouverture par jour et 3 destinations) | le bon s'affiche sans pastille | lecture de `DayOpenPage.statusByNumber` | 🔍 code |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/day/controller/MorningTransferIT.java` (4, nouveau) ; `feature/day/service/impl/BusinessDayServiceImplTest.java` (1 ajouté) ; rappels JR-01 / TRF-01 : `BusinessDayIT.java`, `feature/transfer/controller/TransferHistoryIT.java`.
- Frontend : `valimbavaka-ui/src/app/features/day/pages/day-open-page/day-open-page.spec.ts` (2 ajoutés) ; `features/transfers/pages/transfer-list-page/transfer-list-page.spec.ts` (1 ajouté).

## Recette manuelle (🧪)

| Étape | Action | Résultat attendu | Statut |
|---|---|---|---|
| R1 | Administrateur : `/day/open`, saisir Boutique 12 et Livraison 5, valider | carte « Journée ouverte · 2 bons générés », chaque bon avec la pastille « Validé » | 🧪 recette |
| R2 | Cliquer « Voir les transferts du matin » | `/transfers?origin=MORNING`, puce « Répartition matinale » pressée, les 2 bons en tête, statut « Validé », émetteur = l'administrateur | 🧪 recette |
| R3 | Ouvrir le détail d'un bon (TRF-04 une fois fusionné) | note « Ouverture de la journée du <date du jour> » | 🧪 recette |
| R4 | Revenir sur `/day/open` le même jour | la carte de fin s'affiche directement avec les mêmes bons « Validé » | 🧪 recette |

## Résultat de l'exécution

- Date : 2026-11-08, CI GitHub Actions sur `feature/TRF-02-generation-transferts-matin` (commit `aca4d37`) : workflows « CI » (backend `mvn verify` avec Testcontainers, frontend `ng test`) et « Convention VVS » (Spotless, Checkstyle, Prettier, ESLint, commits et migrations), tous deux verts. « Qodana for JVM » : 2 problèmes « Class is exposed outside of its visibility scope », les mêmes que sur la base JR-01 (aucun dans les fichiers de TRF-02).
- Backend : 373 tests unitaires et 390 d'intégration, 0 échec ; dont TRF-02 : `MorningTransferIT` 4, `BusinessDayServiceImplTest` 5 (1 ajouté).
- Frontend : 66 fichiers, 0 échec ; dont `day-open-page.spec.ts` 9 (2 ajoutés), `transfer-list-page.spec.ts` 8 (1 ajouté).
- Premier passage : formatage Spotless d'une ligne de `MorningTransferIT` ; corrigé (`style(day)`).
- Anomalies : aucune.
- Étapes 🧪 restantes : 4.
