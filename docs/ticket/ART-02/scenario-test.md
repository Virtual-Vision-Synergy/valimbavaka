# Scénarios de test — ART-02 · Création / modification d'un article

Statut : ☐ tests écrits le 2026-10-26, **à lancer** (CI ou `mvn verify` / `ng test`) · Ticket : [#22](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/22) · [description](description.md) · [guide](guide.md)

Légende de la colonne « Statut » : ✅ auto = test automatique exécuté et vert · ✅ base = vérifié en base · 🔍 code = vérifié par lecture du code (aucun test) · ❌ écart = comportement différent de l'attendu · ⏭ hors périmètre = renvoyé à un autre ticket · 🧪 recette = à faire à la main · ☐ = écrit, pas encore exécuté. Détail : [CONSIGNE.md](../CONSIGNE.md#4-remplir-le-scénario-de-test).

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

Point de départ : [ART-01](../ART-01/guide.md) a livré l'API (`POST`, `PUT`, `PATCH /status` sur `/api/v1/articles`) et un premier formulaire d'écran. **ART-02 n'ajoute aucun endpoint** : il adapte la **maquette** `features/articles/pages/article-form-page` (sections guidées, aperçu caisse, brouillon, photo) pour qu'elle crée et modifie un article réel, et remplace par elle le formulaire d'ART-01 (`article-sheet-page`, supprimé). Il branche aussi `/articles/new` et le bouton « Modifier » du détail sur ce formulaire, et ferme un trou : une fiche illisible ne peut plus être enregistrée.

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
| 1 | Pré-remplissage complet | L'administrateur ouvre `/articles/<id>/edit` de « Savon noir 100 g » | code `ART-0031` en lecture seule ; nom, code-barres, gamme, type, prix, coût, unité, seuil, fournisseur pré-remplis ; le bouton « Enregistrer et créer un autre » n'existe pas | `article-form-page-edit.spec.ts` « should show the code and fill every field with the sheet read from the server » | ☐ |
| 1b | Matière première | Il ouvre une matière première (`RAW_MATERIAL`, kg, coût `12000`, seuil `5`) sans code-barres ni fournisseur | champ prix et choix de gamme absents ; code-barres vide ; unité « Kilogramme » | `article-form-page-edit.spec.ts` « should prefill a raw material without range, price, barcode or supplier » | ☐ |
| 1c | Enregistrer sans rien changer | Il ouvre la fiche puis clique sur « Enregistrer » | `PUT` avec exactement les valeurs lues et `version: 3` | `article-form-page-edit.spec.ts` « should send back exactly the values read when nothing was changed » | ☐ |
| 1d | Accès depuis le détail | L'administrateur ouvre le détail d'un article | un lien « Modifier » mène à `/articles/<id>/edit` | `article-detail-page.spec.ts` « should link the edit button to the edit form for an administrator » | ☐ |
| 1e | Lien masqué sans droit | La vendeuse ouvre le même détail | aucun lien « Modifier » | `article-detail-page.spec.ts` « should hide the edit button without the update permission » | ☐ |
| 1f | Fiche inconnue | `/articles/<id inconnu>/edit` | 404 ; message « Article introuvable » | `article-form-page-edit.spec.ts` « should show the server message when the sheet does not exist » ; `ArticleSecurityIT.getArticleById_unknownId_returns404` | ☐ |
| 1g | Fiche illisible : pas d'écrasement | La lecture échoue (500), il saisit un nom et clique sur « Enregistrer » | message affiché, bouton désactivé, **aucun `PUT`** (sinon la fiche serait écrasée par un formulaire vide) | `article-form-page-edit.spec.ts` « should send nothing when the sheet could not be read » | ☐ |
| 1h | Bouton désactivé pendant la lecture | La réponse n'est pas encore arrivée | « Enregistrer » désactivé ; actif une fois la fiche reçue | `article-form-page-edit.spec.ts` « should keep the save button disabled until the sheet is read » | ☐ |
| 1i | Code-barres déjà enregistré, clé invalide | Fiche dont le code-barres stocké est `3760123456789` (clé de contrôle fausse) | la fiche reste enregistrable sans retoucher le code-barres (la clé n'est vérifiée que pour un code-barres modifié) | `article-form-page-edit.spec.ts` « should still save a sheet whose stored barcode has a wrong check digit » | ☐ |
| 1j | Fournisseur désactivé depuis | La fiche garde un fournisseur devenu inactif | il reste proposé ; les autres fournisseurs inactifs ne le sont pas | `article-form-page-edit.spec.ts` « should only offer active suppliers, plus the one already linked » | ☐ |

### Critère 2 — L'enregistrement met à jour la fiche article

| # | Cas | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 2 | Modification depuis l'écran | Il change le nom en « Savon noir 120 g » et le seuil en `25`, puis enregistre | `PUT /api/v1/articles/<id>` avec les valeurs saisies et `version: 3`, retour à `/articles`, message « modifié » | `article-form-page-edit.spec.ts` « should save with PUT, sending the version read, then go back to the list » | ☐ |
| 2b | La fiche est réellement mise à jour | `PUT` de tous les champs | 200, fiche relue avec les nouvelles valeurs, code inchangé | `ArticleUpdateIT.updateArticle_byAdmin_changesEveryField` (ART-01) | ☐ |
| 2c | Tracée dans le journal (JNL-01) | `PUT` avec un seuil changé | une ligne `UPDATE` (ressource `ARTICLE`) | `ArticleUpdateIT.updateArticle_withVersionRead_savesAndTracesUpdateInJournal` (ART-01) | ☐ |
| 2d | Rien de changé : pas de journal | `PUT` avec les mêmes valeurs | 200, aucune nouvelle ligne de journal | `ArticleUpdateIT.updateArticle_withoutChange_leavesNoNewJournalLine` (ART-01) | ☐ |
| 2e | Changement de type | Produit fini → matière première | gamme et prix masqués, envoyés à `null` | `article-form-page-edit.spec.ts` « should send the type change and drop range and price when it becomes a raw material » | ☐ |
| 2f | Création depuis le catalogue | L'administrateur clique sur « Nouvel article » | `/articles/new` ouvre la maquette adaptée (plus de formulaire de démonstration) | `articles.routes.spec.ts` « should open the adapted article form on /articles/new, guarded by the create permission » | ☐ |
| 2g | Bouton de création réservé | La vendeuse ouvre `/articles` | pas de bouton « Nouvel article » | `article-list-page.spec.ts` « should hide the new article button without the create permission » | ☐ |
| 2h | Création enregistrée | Nom `"  Gommage café 200 g "`, gamme Botanika, prix `12500`, seuil par défaut `30` | `POST` sans version, nom nettoyé, retour à `/articles`, message « enregistré » | `article-form-page.spec.ts` « should create with POST, without version, then go back to the list » | ☐ |
| 2i | Enregistrer et créer un autre | Même saisie, bouton « Enregistrer et créer un autre » | `POST`, pas de navigation, formulaire vidé | `article-form-page.spec.ts` « should stay on the form with empty fields after "save and create another" » | ☐ |
| 2j | Type semi-fini | Il choisit « Semi-fini » | envoyé `SEMI_FINISHED`, sans gamme ni prix | `article-form-page.spec.ts` « should offer the semi-finished type and send it as such » | ☐ |
| 2k | Matière première | Type matière première, unité kg | pas de prix, `rangeId`/`price` à `null` | `article-form-page.spec.ts` « should create a raw material without range or price » | ☐ |
| 2l | Fournisseur choisi | Il choisit « Huiles Ravo » | `supplierId` envoyé | `article-form-page.spec.ts` « should send the supplier chosen » | ☐ |
| 2m | Ancienne route retirée | `/articles/sheet/new` | n'existe plus | `articles.routes.spec.ts` « should not keep the provisional sheet/new route » | ☐ |

## Sécurité et erreurs

ART-02 n'expose aucun nouvel endpoint : les cas d'API sont ceux d'ART-01 et restent couverts par ses tests. Ils sont repris ici parce que ce ticket les rend accessibles depuis l'écran.

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Modification sans jeton | `PUT /api/v1/articles/<id>` | 401 `UNAUTHENTICATED` | `ArticleUpdateIT.updateArticle_withoutToken_returns401` | ☐ |
| Modification par une vendeuse | jeton vendeuse | 403 `FORBIDDEN`, fiche inchangée | `ArticleUpdateIT.updateArticle_withSellerRole_returns403AndKeepsTheSheet` | ☐ |
| Création par une vendeuse | jeton vendeuse | 403 | `ArticleSecurityIT.createArticle_withSellerRole_returns403` | ☐ |
| Garde de route d'édition | vendeuse tape `/articles/<id>/edit` | redirigée (`articles.update` absente) | `articles.routes.spec.ts` « should guard the edit route with the update permission » | ☐ |
| Mass assignment | `{"id":"…","code":"HACK-1","active":false,…}` | `id`, `code`, `active` ignorés | `ArticleUpdateIT.updateArticle_massAssignmentOfIdCodeAndStatus_isIgnored` | ☐ |
| Données invalides | nom de 121 caractères, seuil `-1`, code-barres `12345` | 422 `VALIDATION_FAILED` (écran : message sous le champ, rien envoyé) | `ArticleUpdateIT.updateArticle_invalidData_returns422AndKeepsTheSheet` ; `article-form-page-validation.spec.ts` « should send nothing when the name exceeds 120 characters, and accept exactly 120 » | ☐ |
| Article inconnu | `PUT` d'un identifiant aléatoire | 404 `NOT_FOUND` | `ArticleUpdateIT.updateArticle_unknownId_returns404` | ☐ |
| Code-barres déjà pris | `3760123456789` porté par un autre article | 409 `CONFLICT`, message affiché, saisie conservée | `ArticleUpdateIT.updateArticle_barcodeOfAnotherArticle_returns409` ; `article-form-page.spec.ts` « should show the server message and keep the typing when the barcode is already used » | ☐ |
| Version périmée | A et B ouvrent la version 3 ; A enregistre, puis B | B reçoit 409, la saisie de B reste à l'écran | `ArticleUpdateIT.updateArticle_staleVersion_returns409AndKeepsTheSheet` ; `article-form-page-edit.spec.ts` « should show the server message when the sheet was modified in the meantime » | ☐ |

## Cas limites et hors ligne

| Cas | Exemple d'entrée | Résultat attendu | Comment c'est vérifié | Statut |
|---|---|---|---|---|
| Lecture en échec (réseau, 500) | `GET` de la fiche renvoie 500 | message affiché, enregistrement impossible, aucune écriture | `article-form-page-edit.spec.ts` « should send nothing when the sheet could not be read » | ☐ |
| Double clic sur Enregistrer | deux clics rapides | une seule requête, bouton désactivé pendant l'envoi | `article-form-page.spec.ts` « should disable the save buttons while saving so that a double click sends one request » | ☐ |
| Nom au maximum / maximum + 1 | 120 puis 121 caractères | 120 accepté, 121 refusé | `article-form-page-validation.spec.ts` « should send nothing when the name exceeds 120 characters, and accept exactly 120 » | ☐ |
| Type changé en modification | produit fini → matière première | gamme et prix masqués, envoyés à `null` | `article-form-page.spec.ts` « should hide the range and the price for a raw material » ; `article-form-page-edit.spec.ts` « should send the type change and drop range and price when it becomes a raw material » | ☐ |
| Fournisseur devenu inactif | la fiche garde un fournisseur désactivé depuis | il reste proposé et la fiche reste modifiable | `article-form-page-edit.spec.ts` « should only offer active suppliers, plus the one already linked » | ☐ |
| Désactivation de l'article | `PATCH /status {"active":false}` | jamais de suppression ; hors périmètre de l'écran de fiche | `ArticleUpdateIT.setActive_byAdmin_deactivatesThenReactivatesAndTracesOnlyRealChanges` (ART-01) | ☐ |
| Code-barres, clé de contrôle | `3760123456789` (clé fausse) saisi à la création | refusé, message « Clé de contrôle invalide » ; `3760123456784` accepté ; espaces retirés (`3760 1234 56784`) | `article-form-page-validation.spec.ts` « should refuse a barcode whose check digit is wrong » ; « should send a barcode typed with spaces without the spaces » ; « should send an empty barcode as null » | ☐ |
| Code-barres de longueur ou de forme invalide | `12345`, `abcdefghijklm`, `37601234567890` | refusés (« 13 chiffres ») | `article-form-page-validation.spec.ts` « should send nothing when the barcode is %s » (3 cas) | ☐ |
| Nom d'espaces | `"   "` | refusé | `article-form-page-validation.spec.ts` « should send nothing and ask for a name when the name is only spaces » | ☐ |
| Valeurs négatives et zéro | coût `-0.01`, prix `-1`, seuil `-5` puis `0` | refusés, puis zéro accepté | `article-form-page-validation.spec.ts` « should refuse a negative cost, price or threshold and accept zero » | ☐ |
| Produit fini incomplet | sans gamme ni prix | rien n'est envoyé, messages sous les champs | `article-form-page.spec.ts` « should send nothing when a finished product has no range or no price » | ☐ |
| Brouillon de création | saisie interrompue puis retour sur `/articles/new` | brouillon restauré ; jamais en modification ; effacé après enregistrement | `article-draft-store.ts`, `article-form-page.ts` | 🔍 code — pas de test ; à ajouter si le brouillon évolue |
| Éléments de la maquette sans équivalent serveur | `PACKAGING`, cases « fabriqué » et « utilisable au salon » | retirés du formulaire : le serveur ne les connaît pas (contrat ART-01) | `article-form-page.ts` | ⏭ hors périmètre (à trancher avec le métier avant ART-04) |
| Écriture hors ligne | réseau coupé pendant l'enregistrement | non prise en charge : l'erreur réseau s'affiche, la saisie est conservée | `ArticleService` (`HttpClient` direct) | ⏭ hors périmètre (SYS-07) |
| Session expirée pendant la saisie | jeton expiré avant « Enregistrer » | `errorInterceptor` : renouvellement du jeton ou retour à la connexion (SYS-05, AUTH-01) | — | ⏭ hors périmètre (AUTH-01) |
| Navigation d'une fiche à une autre sans quitter l'écran | `/articles/A/edit` puis `/articles/B/edit` | la fiche lue reste celle de A (lecture à l'ouverture seulement) ; non atteignable par l'interface actuelle | `article-form-page.ts` (`ngOnInit`) | ⏭ hors périmètre (à traiter si une navigation directe d'une fiche à l'autre apparaît) |
| Liste et détail encore sur données de démonstration | clic « Modifier » sur un article de démonstration (identifiant non UUID) | « Article introuvable » : la liste réelle arrive avec ART-05, le détail 360° avec ART-06 | `features/articles/data-access/article-fixtures.ts` | ⏭ hors périmètre (ART-05, ART-06) |
| Photo de l'article | dépôt d'une image | pas de stockage serveur | — | ⏭ hors périmètre (aucun ticket) |

## Tests existants

Frontend (`valimbavaka-ui/src/app/features/`) : `articles/pages/article-form-page/article-form-page.spec.ts`, `article-form-page-validation.spec.ts`, `article-form-page-edit.spec.ts` (harnais `article-form-page.harness.ts`, données `article-form-page.fixtures.ts`), `articles/articles.routes.spec.ts`, `articles/pages/article-detail-page/article-detail-page.spec.ts`, `articles/pages/article-list-page/article-list-page.spec.ts`, `article/data-access/article-service.spec.ts` (ART-01).

Backend (`valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/article/`) : `controller/ArticleUpdateIT.java`, `controller/ArticleSecurityIT.java`, `controller/ArticleCreationIT.java`, `service/impl/ArticleServiceImplTest.java` (tous d'ART-01, inchangés).

## Recette manuelle (🧪)

| # | Étape | Résultat attendu | Statut |
|---|---|---|---|
| R1 | Connecté en administrateur, ouvrir `/articles/<id d'un article réel>/edit`, changer le seuil, enregistrer, rouvrir la fiche | le seuil modifié est pré-rempli ; une ligne `UPDATE` apparaît dans `/journal` | 🧪 |
| R2 | Dans `/articles`, cliquer sur « Nouvel article », créer un produit fini | la maquette adaptée s'ouvre (aperçu en caisse mis à jour pendant la saisie ; code attribué après enregistrement) ; l'article existe via `GET /api/v1/articles` | 🧪 |
| R3 | Connecté en vendeuse, ouvrir `/articles` puis un détail | ni « Nouvel article » ni « Modifier » ; `/articles/<id>/edit` saisi à la main redirige | 🧪 |
| R4 | Couper le serveur, ouvrir `/articles/<id>/edit` | message d'erreur, bouton « Enregistrer » désactivé | 🧪 |

## Résultat de l'exécution

- Date : 2026-10-26 (tests écrits, **non exécutés** : consigne projet « ne pas lancer les tests »).
- Environnement : à lancer par la CI GitHub ou en local (`cd valimbavaka-ui && npx ng test --watch=false`).
- Tests lancés : aucun. Statuts laissés à ☐ ; à passer en ✅ auto une fois la CI verte.
- Anomalies ouvertes : aucun écart constaté. Corrigé par ce ticket : une fiche illisible pouvait être enregistrée (voir cas 1g). Reste à traiter ailleurs : liste et détail encore sur données de démonstration (ART-05, ART-06).
- Étapes 🧪 restantes : 4 (R1 à R4).
