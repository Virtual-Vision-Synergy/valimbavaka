# Scénarios de test — TRF-04 · Détail d'un transfert

Statut : ✅ exécuté le 2026-11-08 — **CI verte** (voir « Résultat de l'exécution ») · Ticket : [#37](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/37) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible. Légende : ✅ auto = test automatique exécuté et vert en CI · 🧪 recette manuelle.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôles utilisés | administrateur Rasoa Rakoto (`admin.test@valimbavaka.mg`, `transfers.read`), vendeuse Tiana Ranaivoson (`transfers.return` sans `transfers.read`), livreur (aucune permission de transfert) |
| Données de départ | emplacements `STORE` (Boutique) et `WAREHOUSE` (Entrepôt) du référentiel SYS-05 ; deux articles avec 5 en boutique ; un retour saisi par la vendeuse : 2,5 du premier article puis 1 du second, de `STORE` vers `WAREHOUSE`, note « Invendus du samedi » ; un transfert du matin `MORNING` (1 de l'entrepôt vers la boutique) enregistré par `TransferService.record` hors requête |
| Tickets à avoir déployés avant | TRF-01 (historique, mergée dans `develop`), TRF-03 (modèle `transfers` / `transfer_lines`), SYS-05, AUTH-01 (permission `transfers.read` donnée à `ADMIN` par `2026_10_19-01-changelog.sql`) |
| Migration | aucune : tables `transfers` / `transfer_lines` (TRF-03) et permission `transfers.read` existent déjà |

## Scénarios issus des critères d'acceptation

### Critère 1 — Le clic sur un transfert ouvre son détail complet

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| 1 · Détail d'un retour saisi par la vendeuse | administrateur : `GET /api/v1/transfers/{id}` du retour `RET-…` | 200 : `number` `RET-…`, `origin` et `status` `RETURN`, `fromLocationLabel` « Boutique », `toLocationLabel` « Entrepôt », `note` « Invendus du samedi », `quantity` 3.5, `createdAt` renseigné, `createdByName` « Tiana Ranaivoson », 2 lignes **dans l'ordre saisi** : (code, nom, `PIECE`, 2.5) puis (second article, 1) | `TransferDetailIT.getTransfer_returnBySeller_showsEveryArticleAndQuantityInTheEnteredOrder` | ✅ auto |
| 1b · Assemblage de la fiche | retour `RET-0015` : Huile de coco 2,5 L puis Savon au curcuma 3 u., note « ␣Invendus du samedi␣ » | lignes dans l'ordre saisi avec code, nom et unité ; somme 5.5 ; libellés Boutique / Entrepôt ; note nettoyée ; émetteur « Système » sans `createdBy` | `TransferSheetTest.toRDto_returnWithTwoLines_showsArticlesQuantitiesRouteAndIssuer` | ✅ auto |
| 1c · Transfert généré hors requête | transfert du matin enregistré par `TransferService.record` | `origin` `MORNING`, `status` `VALIDATED`, `note` et `createdBy` absents, `createdByName` « Système » | `TransferDetailIT.getTransfer_transferGeneratedOutsideARequest_showsTheSystemAsIssuer` | ✅ auto |
| 1d · Service | identifiant connu | le bon est lu puis transformé par `TransferSheet` | `TransferServiceImplTest.getTransfer_existingTransfer_returnsItsSheet` | ✅ auto |
| 1e · Écran : clic sur un numéro | liste avec `RET-0015` et `TRF-0092`, clic sur `RET-0015` | requête `GET /api/v1/transfers/{id}` ; le panneau latéral affiche « Savon au curcuma », « ART-0001 », « 5.5 u. », « Tiana Ranaivoson » ; le numéro cliqué porte `aria-pressed="true"` | `transfer-list-page.spec.ts` (« should open the full detail of a transfer when its number is clicked ») | ✅ auto |
| 1f · Écran : lignes du bon | Huile de coco 2,5 L, Savon au curcuma 3 u. | « Huile de coco ART-0002 2.5 L » puis « Savon au curcuma ART-0001 3 u. », dans cet ordre | `transfer-detail.spec.ts` (« should show every article with its code, quantity and unit, in the entered order ») | ✅ auto |
| 1g · Écran : en-tête du bon | `RET-0015`, Boutique → Entrepôt, 5,5 u., vendeuse, note | numéro, statut « Retour », trajet, « −5.5 u. », `<time datetime="2026-10-19T16:05:00Z">`, émetteur et note | `transfer-detail.spec.ts` (« should show the reference, status, route, date, issuer and note of the transfer ») | ✅ auto |
| 1h · Écran : autre bon sélectionné | passage de `RET-0015` à un autre identifiant | nouvelle requête, en-tête `TRF-0092` | `transfer-detail.spec.ts` (« should load the transfer again when another one is selected ») | ✅ auto |
| 1i · Service front | `get('0195…c1')` | `GET /api/v1/transfers/0195…c1` | `transfer-service.spec.ts` (« lit le détail d'un transfert sur GET /api/v1/transfers/{id} ») | ✅ auto |

## Sécurité et erreurs

Endpoint en lecture seule : pas de corps (pas de mass assignment possible), pas de 409 ni de 422, pas de ligne de journal (consultation).

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `GET /api/v1/transfers/{uuid}` sans `Authorization` | 401 `UNAUTHENTICATED` | `TransferDetailIT.getTransfer_withoutToken_returns401` | ✅ auto |
| Vendeuse | jeton vendeuse (a `transfers.return`, pas `transfers.read`) | 403 `FORBIDDEN` | `TransferDetailIT.getTransfer_withSellerRole_returns403` | ✅ auto |
| Livreur | jeton livreur | 403 `FORBIDDEN` | `TransferDetailIT.getTransfer_withDeliveryDriverRole_returns403` | ✅ auto |
| Transfert inconnu | UUID aléatoire | 404 `NOT_FOUND` | `TransferDetailIT.getTransfer_unknownId_returns404`, `TransferServiceImplTest.getTransfer_unknownTransfer_throwsNotFound` | ✅ auto |
| Identifiant mal formé | `/api/v1/transfers/pas-un-uuid` | 400 `MALFORMED_REQUEST` | `TransferDetailIT.getTransfer_malformedId_returns400` | ✅ auto |
| Écran : erreur de chargement | 404 du serveur | état d'erreur avec « Réessayer » ; « Réessayer » relance la requête et affiche le bon | `transfer-detail.spec.ts` (« should offer to retry when the transfer cannot be loaded ») | ✅ auto |

## Cas limites

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Article désactivé depuis | article de la ligne passé à `active=false` (`PATCH /api/v1/articles/{id}/status`) | la ligne garde son code et son nom | `TransferDetailIT.getTransfer_articleDeactivatedSince_stillShowsItsCodeAndName` | ✅ auto |
| Pas de N+1 sur les articles | bon de 2 lignes | une seule lecture des articles (`ArticleService.getArticlesByIds([OIL, SOAP])`) et une des emplacements | `TransferSheetTest.toRDto_severalLines_readsAllTheArticlesOnce` | ✅ auto |
| Emplacement désactivé depuis | origine absente des emplacements actifs | identifiant gardé, libellé `null` ; l'écran affiche « Emplacement désactivé » ; note absente → pas de bloc « Note » | `TransferSheetTest.toRDto_locationDeactivatedSince_keepsTheIdentifierWithoutLabel`, `transfer-detail.spec.ts` (« should name a deactivated location and leave out an empty note ») | ✅ auto |
| Article introuvable (intégrité) | ligne dont l'article n'est pas renvoyé | `IllegalStateException` (500) plutôt qu'une ligne vide ; impossible en base (clé étrangère `transfer_lines.article_id`) | `TransferSheetTest.toRDto_articleMissing_failsInsteadOfShowingAnEmptyLine` | ✅ auto |
| Bon de 100 lignes (maximum TRF-03) | retour de 100 articles | toutes les lignes renvoyées et affichées (plus de résumé « + n autres lignes ») ; le panneau défile | — | 🧪 |
| Accessibilité | détail chargé | aucune violation axe (WCAG 2.2 AA, hors contraste) ; liste des lignes nommée « 2 article(s) transféré(s) » | `transfer-detail.spec.ts` (« should have no detectable accessibility violation once loaded ») | ✅ auto |
| Dates et fuseau | `createdAt` `2026-10-19T16:05:00Z` | affiché dans le fuseau du navigateur (`WhenPipe`) ; attribut `datetime` = valeur du serveur | `transfer-detail.spec.ts` (« should show the reference, status… ») | ✅ auto |
| Hors ligne | ouverture du détail sans réseau | lecture seule, hors file d'attente : état d'erreur avec « Réessayer » | — | 🧪 |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/transfer/controller/TransferDetailIT.java` (8), `…/transfer/service/impl/TransferSheetTest.java` (4), `…/transfer/service/impl/TransferServiceImplTest.java` (2 ajoutés).
- Frontend : `valimbavaka-ui/src/app/features/transfers/components/transfer-detail/transfer-detail.spec.ts` (6), `…/pages/transfer-list-page/transfer-list-page.spec.ts` (1 ajouté), `…/data-access/transfer-service.spec.ts` (1 ajouté).

## Recette manuelle (🧪)

| Étape | Attendu | Statut |
|---|---|---|
| Administrateur : saisir un retour de 2 articles avec une note, puis cliquer sur son numéro dans `/transfers` | le panneau affiche numéro, statut « Retour », trajet, date à l'heure locale, émetteur, note, puis chaque article (nom, code) avec sa quantité et son unité (u., kg, L) dans l'ordre saisi | 🧪 |
| Cliquer successivement sur deux numéros | le panneau change de bon ; le numéro sélectionné est mis en évidence | 🧪 |
| « Créer un retour » dans le panneau | ouvre `/transfers/return` | 🧪 |
| Bon de nombreuses lignes (≥ 20) | toutes les lignes visibles en faisant défiler le panneau | 🧪 |
| Couper le réseau puis cliquer sur un numéro | état d'erreur avec « Réessayer » ; au retour du réseau, « Réessayer » charge le bon | 🧪 |

## Résultat de l'exécution

- Date : 2026-11-08, CI GitHub Actions sur `feature/TRF-04-detail-transfert` (commit `4a3124b`, après fusion de `develop` contenant TRF-01) : workflows « CI » (backend `mvn verify` avec Testcontainers, frontend `ng test`) et « Convention VVS » (Spotless, Checkstyle, Prettier, ESLint, commits et migrations), tous deux verts.
- Backend : 367 tests unitaires et 374 d'intégration, 0 échec ; dont TRF-04 : `TransferDetailIT` 8, `TransferSheetTest` 4, `TransferServiceImplTest` 8 (2 ajoutés).
- Frontend : 65 fichiers, 395 tests, 0 échec ; dont `transfer-detail.spec.ts` 6, `transfer-list-page.spec.ts` 8 (1 ajouté), `transfer-service.spec.ts` 4 (1 ajouté).
- Passages précédents : formatage Spotless de deux chaînes `stream()` ; `TransferServiceImplTest.getTransfer_existingTransfer_returnsItsSheet` en échec (le service passait par `Optional.map`, un résultat `null` du simulacre devenait un 404) ; `transfer-detail.spec.ts` « should offer to retry… » en échec : **défaut réel** du composant, qui lisait `value()` d'une ressource en erreur (exception Angular) au lieu d'afficher l'état d'erreur. Tous corrigés (`fix(transfer)`, `fix(transfers)`).
- Anomalies ouvertes : aucune.
- Étapes 🧪 restantes : 5 (+ 2 cas limites 🧪).
