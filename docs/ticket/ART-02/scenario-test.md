# Scénarios de test — ART-02 · Création / modification d'un article

Statut : ☐ tests écrits le 2026-10-26, **à lancer** (CI ou `mvn verify` / `ng test`) · Ticket : [#22](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/22) · [description](description.md) · [guide](guide.md)

Légende de la colonne « Statut » : ✅ auto = test automatique exécuté et vert · ✅ base = vérifié en base · 🔍 code = vérifié par lecture du code (aucun test) · ❌ écart = comportement différent de l'attendu · ⏭ hors périmètre = renvoyé à un autre ticket · 🧪 recette = à faire à la main · ☐ = écrit, pas encore exécuté. Détail : [CONSIGNE.md](../CONSIGNE.md#4-remplir-le-scénario-de-test).

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

Point de départ : [ART-01](../ART-01/guide.md) a déjà livré l'API (`POST`, `PUT`, `PATCH /status` sur `/api/v1/articles`) et la fiche réelle `features/article/pages/article-sheet-page`, qui sert à la création et à la modification. **ART-02 n'ajoute aucun endpoint.** Il rend cette fiche accessible depuis le catalogue (`/articles/new`, bouton « Modifier »), retire le formulaire de démonstration qu'elle remplace et ferme un trou de sécurité des données : une fiche qui n'a pas pu être lue ne doit plus pouvoir être enregistrée.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (`articles.create`, `articles.update`), `SELLER` (`articles.read` seulement), `DELIVERY_DRIVER` (aucune permission sur les articles) |
| Données de départ | gammes Zezika / Botanika / Solika (SYS-05) ; fournisseur « Huiles Ravo » actif (FRS-01) ; article « Savon noir 100 g » (`ART-0031`, `FINISHED`, Botanika, coût `3120.50`, prix `8500`, seuil `40`, code-barres `3760123456789`, version `3`) |
| Tickets à avoir déployés avant | ART-01 (migration `2026_10_26-01` appliquée), JNL-01 |

## Scénarios issus des critères d'acceptation

### Critère 1 — Le formulaire pré-remplit les valeurs existantes en modification

| # | Cas | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Pré-remplissage complet | L'administrateur ouvre `/articles/<id>/edit` de « Savon noir 100 g » | code `ART-0031` affiché en lecture seule ; nom, code-barres, type, gamme, unité, coût, prix, seuil et fournisseur pré-remplis avec la fiche lue | `article-sheet-page.spec.ts` « should show the code and fill every field with the sheet read from the server » (ART-01) | ☐ |
| 1b | Matière première sans gamme ni prix | Il ouvre une matière première `RAW_MATERIAL` sans code-barres ni fournisseur | champs gamme et prix absents ; code-barres et fournisseur vides (« Aucun fournisseur ») ; coût et seuil pré-remplis | `article-sheet-page-edit.spec.ts` « should prefill a raw material without range, price, barcode or supplier » | ☐ |
| 1c | Enregistrer sans rien changer | Il ouvre la fiche puis clique sur « Enregistrer » sans toucher aux champs | `PUT` avec exactement les valeurs lues, `version: 3` (aucune valeur effacée par le formulaire) | `article-sheet-page-edit.spec.ts` « should send back exactly the values read when nothing was changed » | ☐ |
| 1d | Accès depuis la fiche du catalogue | L'administrateur ouvre le détail d'un article | un lien « Modifier » mène à `/articles/<id>/edit` | `article-detail-page.spec.ts` « should link the edit button to the edit form for an administrator » | ☐ |
| 1e | Lien masqué sans droit | La vendeuse (lecture seule) ouvre le même détail | aucun lien « Modifier » | `article-detail-page.spec.ts` « should hide the edit button without the update permission » | ☐ |
| 1f | Fiche inconnue | `/articles/<id inconnu>/edit` | 404 ; message « Article introuvable » affiché | `article-sheet-page.spec.ts` « when the sheet does not exist » (ART-01) ; `ArticleSecurityIT.getArticleById_unknownId_returns404` | ☐ |
| 1g | Fiche illisible : pas d'écrasement | La lecture échoue (500) puis il saisit un nom et clique sur « Enregistrer » | le message du serveur s'affiche, le bouton est désactivé, **aucun `PUT` n'est envoyé** (sinon la fiche serait écrasée par un formulaire vide, sans version) | `article-sheet-page-edit.spec.ts` « should send nothing when the sheet could not be read » | ☐ |
| 1h | Bouton désactivé pendant la lecture | Il ouvre la fiche, la réponse n'est pas encore arrivée | bouton « Enregistrer » désactivé ; actif une fois la fiche reçue | `article-sheet-page-edit.spec.ts` « should keep the save button disabled until the sheet is read » | ☐ |

### Critère 2 — L'enregistrement met à jour la fiche article

| # | Cas | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 2 | Modification depuis l'écran | Il change le nom en « Savon noir 120 g » et le seuil en `25`, puis enregistre | `PUT /api/v1/articles/<id>` avec les valeurs saisies et `version: 3`, retour à `/articles` | `article-sheet-page.spec.ts` « should save with PUT, sending the version read, then go back to the list » (ART-01) | ☐ |
| 2b | La fiche est réellement mise à jour | `PUT` de tous les champs | 200, la fiche relue porte les nouvelles valeurs, code inchangé | `ArticleUpdateIT.updateArticle_byAdmin_changesEveryField` (ART-01) | ☐ |
| 2c | Modification tracée dans le journal (JNL-01) | `PUT` avec un seuil changé | une ligne `UPDATE` (ressource `ARTICLE`) en plus de la ligne `CREATE` | `ArticleUpdateIT.updateArticle_withVersionRead_savesAndTracesUpdateInJournal` (ART-01) | ☐ |
| 2d | Rien de changé : pas de journal | `PUT` avec les mêmes valeurs (espaces autour du nom) | 200, aucune nouvelle ligne de journal | `ArticleUpdateIT.updateArticle_withoutChange_leavesNoNewJournalLine` (ART-01) | ☐ |
| 2e | Création depuis le catalogue | L'administrateur clique sur « Nouvel article » dans `/articles` | arrive sur la fiche réelle vide (`POST`, pas de brouillon de démonstration) | `articles.routes.spec.ts` « should open the real article sheet on /articles/new, guarded by the create permission » | ☐ |
| 2f | Bouton de création réservé | La vendeuse ouvre `/articles` | pas de bouton « Nouvel article » | `article-list-page.spec.ts` « should hide the new article button without the create permission » | ☐ |
| 2g | Création enregistrée | Nom `"  Gommage café 200 g "`, gamme Botanika, prix `12500` | `POST` sans version, retour à `/articles` | `article-sheet-page.spec.ts` « should create with POST, without version, then go back to the list » (ART-01) | ☐ |
| 2h | Écran de démonstration retiré | `/articles/sheet/new` (ancienne route provisoire) | n'existe plus : seule `/articles/new` ouvre la fiche réelle | `articles.routes.spec.ts` « should not keep the provisional sheet/new route » | ☐ |

## Sécurité et erreurs

ART-02 n'expose aucun nouvel endpoint : les cas d'API sont ceux d'ART-01 et restent couverts par ses tests. Ils sont repris ici parce que ce ticket les rend accessibles depuis l'écran.

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Modification sans jeton | `PUT /api/v1/articles/<id>` | 401 `UNAUTHENTICATED` | `ArticleUpdateIT.updateArticle_withoutToken_returns401` | ☐ |
| Modification par une vendeuse | jeton vendeuse | 403 `FORBIDDEN`, fiche inchangée | `ArticleUpdateIT.updateArticle_withSellerRole_returns403AndKeepsTheSheet` | ☐ |
| Création par une vendeuse | jeton vendeuse | 403 | `ArticleSecurityIT.createArticle_withSellerRole_returns403` | ☐ |
| Garde de route d'édition | vendeuse tape `/articles/<id>/edit` | redirigée (`articles.update` absente) | `articles.routes.spec.ts` « should guard the edit route with the update permission » | ☐ |
| Mass assignment | `{"id":"…","code":"HACK-1","active":false,…}` | `id`, `code`, `active` ignorés | `ArticleUpdateIT.updateArticle_massAssignmentOfIdCodeAndStatus_isIgnored` | ☐ |
| Données invalides | nom de 121 caractères, seuil `-1`, code-barres `12345` | 422 `VALIDATION_FAILED` (écran : message sous le champ, rien envoyé) | `ArticleUpdateIT.updateArticle_invalidData_returns422AndKeepsTheSheet` ; `article-sheet-page.spec.ts` « should send nothing when the name exceeds 120 characters, and accept exactly 120 » | ☐ |
| Article inconnu | `PUT` d'un identifiant aléatoire | 404 `NOT_FOUND` | `ArticleUpdateIT.updateArticle_unknownId_returns404` | ☐ |
| Code-barres déjà pris | `3760123456789` porté par un autre article | 409 `CONFLICT`, message affiché, saisie conservée | `ArticleUpdateIT.updateArticle_barcodeOfAnotherArticle_returns409` ; `article-sheet-page.spec.ts` « should show the server message when the barcode is already used » | ☐ |
| Version périmée | A et B ouvrent la version 3 ; A enregistre, puis B | B reçoit 409, la saisie de B reste à l'écran | `ArticleUpdateIT.updateArticle_staleVersion_returns409AndKeepsTheSheet` ; `article-sheet-page.spec.ts` « should show the server message when the sheet was modified in the meantime » | ☐ |

## Cas limites et hors ligne

| Cas | Exemple d'entrée | Résultat attendu | Comment c'est vérifié | Statut |
|---|---|---|---|---|
| Lecture en échec (réseau, 500) | `GET` de la fiche renvoie 500 | message affiché, enregistrement impossible, aucune écriture | `article-sheet-page-edit.spec.ts` « should send nothing when the sheet could not be read » | ☐ |
| Double clic sur Enregistrer | deux clics rapides | une seule requête, bouton désactivé pendant l'envoi | `article-sheet-page.spec.ts` « should disable the save button while saving so that a double click sends one request » (ART-01) | ☐ |
| Nom au maximum / maximum + 1 | 120 puis 121 caractères | 120 accepté, 121 refusé | `article-sheet-page.spec.ts` « should send nothing when the name exceeds 120 characters, and accept exactly 120 » (ART-01) | ☐ |
| Type changé en modification | produit fini → matière première | gamme et prix masqués, envoyés à `null` | `article-sheet-page.spec.ts` « should hide the range and the price and send them as null for a raw material » (ART-01) | ☐ |
| Fournisseur devenu inactif | la fiche garde un fournisseur désactivé depuis | il reste proposé et la fiche reste modifiable | `article-sheet-page.spec.ts` « should only offer active suppliers, plus the one already linked » (ART-01) | ☐ |
| Désactivation de l'article | `PATCH /status {"active":false}` | jamais de suppression ; hors périmètre de l'écran de fiche | `ArticleUpdateIT.setActive_byAdmin_deactivatesThenReactivatesAndTracesOnlyRealChanges` (ART-01) | ☐ |
| Écriture hors ligne | réseau coupé pendant l'enregistrement | non prise en charge : l'erreur réseau s'affiche, la saisie est conservée | `ArticleService` (`HttpClient` direct) | ⏭ hors périmètre (SYS-07) |
| Session expirée pendant la saisie | jeton expiré avant « Enregistrer » | `errorInterceptor` : renouvellement du jeton ou retour à la connexion (SYS-05, AUTH-01) | — | ⏭ hors périmètre (AUTH-01) |
| Navigation d'une fiche à une autre sans quitter l'écran | `/articles/A/edit` puis `/articles/B/edit` | la fiche lue reste celle de A (lecture à l'ouverture seulement) ; non atteignable par l'interface actuelle | `article-sheet-page.ts` (`ngOnInit`) | ⏭ hors périmètre (à traiter si une navigation directe d'une fiche à l'autre apparaît) |
| Liste et détail encore sur données de démonstration | clic « Modifier » sur un article de démonstration (identifiant non UUID) | « Article introuvable » : la liste réelle arrive avec ART-05, le détail 360° avec ART-06 | `features/articles/data-access/article-fixtures.ts` | ⏭ hors périmètre (ART-05, ART-06) |
| Photo de l'article | dépôt d'une image | pas de stockage serveur | — | ⏭ hors périmètre (aucun ticket) |

## Tests existants

Frontend (`valimbavaka-ui/src/app/features/`) : `article/pages/article-sheet-page/article-sheet-page.spec.ts` (ART-01), `article/pages/article-sheet-page/article-sheet-page-edit.spec.ts` (ART-02), `articles/articles.routes.spec.ts` (ART-02), `articles/pages/article-detail-page/article-detail-page.spec.ts` (ART-02), `articles/pages/article-list-page/article-list-page.spec.ts` (ART-02), `article/data-access/article-service.spec.ts` (ART-01).

Backend (`valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/article/`) : `controller/ArticleUpdateIT.java`, `controller/ArticleSecurityIT.java`, `controller/ArticleCreationIT.java`, `service/impl/ArticleServiceImplTest.java` (tous d'ART-01, inchangés).

## Recette manuelle (🧪)

| # | Étape | Résultat attendu | Statut |
|---|---|---|---|
| R1 | Connecté en administrateur, ouvrir `/articles/<id d'un article réel>/edit`, changer le seuil, enregistrer, rouvrir la fiche | le seuil modifié est pré-rempli ; une ligne `UPDATE` apparaît dans `/journal` | 🧪 |
| R2 | Dans `/articles`, cliquer sur « Nouvel article », créer un produit fini | la fiche réelle s'ouvre (code attribué après enregistrement) ; l'article existe via `GET /api/v1/articles` | 🧪 |
| R3 | Connecté en vendeuse, ouvrir `/articles` puis un détail | ni « Nouvel article » ni « Modifier » ; `/articles/<id>/edit` saisi à la main redirige | 🧪 |
| R4 | Couper le serveur, ouvrir `/articles/<id>/edit` | message d'erreur, bouton « Enregistrer » désactivé | 🧪 |

## Résultat de l'exécution

- Date : 2026-10-26 (tests écrits, **non exécutés** : consigne projet « ne pas lancer les tests »).
- Environnement : à lancer par la CI GitHub ou en local (`cd valimbavaka-ui && npx ng test --watch=false`).
- Tests lancés : aucun. Statuts laissés à ☐ ; à passer en ✅ auto une fois la CI verte.
- Anomalies ouvertes : aucune écart constaté. Corrigé par ce ticket : une fiche illisible pouvait être enregistrée (voir cas 1g). Reste à traiter ailleurs : liste et détail encore sur données de démonstration (ART-05, ART-06).
- Étapes 🧪 restantes : 4 (R1 à R4).
