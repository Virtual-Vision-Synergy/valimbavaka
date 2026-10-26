# Guide d'intégration — ART-02 · Création / modification d'un article

Statut : ✅ écrit le 2026-10-26 — **tests non exécutés** (voir « Résultat de l'exécution » du scénario) · Ticket : [#22](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/22) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

La création et la modification d'un article utilisent **un seul formulaire : la maquette `ArticleFormPage`** (sections guidées, aperçu en caisse, brouillon, photo), adaptée à l'API d'[ART-01](../ART-01/guide.md). Sans `id` elle crée (`POST`), avec `id` elle pré-remplit la fiche lue et la modifie (`PUT`). Le bouton « Nouvel article » ouvre `/articles/new`, le bouton « Modifier » du détail ouvre `/articles/:id/edit`, chacun réservé à `articles.create` / `articles.update`. **Aucun endpoint n'a été ajouté.** Le formulaire d'écran qu'ART-01 avait écrit (`article-sheet-page`) a été supprimé au profit de la maquette.

## Contrat exposé

### Endpoints backend

Aucun nouvel endpoint. Voir le [guide ART-01](../ART-01/guide.md#endpoints-backend-apiv1articles) : `POST /api/v1/articles` (`articles.create`), `PUT /api/v1/articles/{id}` (`articles.update`, `version` obligatoire pour le verrou optimiste), `PATCH /api/v1/articles/{id}/status`. Chaque écriture réelle écrit une ligne `CREATE` / `UPDATE` (ressource `ARTICLE`) dans le journal ([JNL-01](../JNL-01/guide.md)).

### Frontend

- Routes (`features/articles/articles.routes.ts`, sous `/articles` déjà gardé par `articles.read`) : `/articles/new` (garde `articles.create`) et `/articles/:id/edit` (garde `articles.update`), toutes deux sur `ArticleFormPage`. L'ancienne route provisoire `/articles/sheet/new` n'existe plus.
- Boutons : « Nouvel article » (`article-list-page.html`) et « Modifier » (`article-detail-page.html`), sous `*vvsHasPermission`. Le serveur décide de toute façon (403).
- `ArticleFormPage` (`features/articles/pages/article-form-page`) : `input()` `id` lié à la route. Elle appelle les services réels de `features/article/data-access` (`ArticleService.create / update / getById`), `ReferentialService.listProductRanges()` (choix de gamme) et `SupplierService.list()` (fournisseurs actifs, plus celui déjà lié).
- Ce que le formulaire fait désormais :
  - **gamme** : choix par `rangeId` (UUID du référentiel), la couleur et le libellé viennent du référentiel ; **types** : produit fini, matière première, semi-fini (`SEMI_FINISHED`) ; **coût d'achat** ajouté (facultatif) ;
  - **prix** : 0 Ar ou plus (règle du serveur) ; gamme et prix absents pour un type qui n'est pas produit fini ;
  - **code-barres** : clé de contrôle EAN-13 vérifiée (maquette), sauf pour le code-barres déjà enregistré sur la fiche ouverte ;
  - **brouillon** (`ArticleDraftStore`) : seulement en création ; effacé après enregistrement ;
  - **message** de réussite en notification (`articles.form.created` / `articles.form.updated`) ; message du `ProblemDetail` sous le titre en cas d'erreur, saisie conservée ;
  - **« Enregistrer et créer un autre »** : création seulement.
- Garde-fou : en modification, « Enregistrer » est désactivé et `save()` ne fait rien tant que la fiche n'a pas été lue (réponse en attente, 404, 500, réseau coupé). Sans cela, une lecture échouée laissait un formulaire vide que l'on pouvait envoyer ; `PUT` étant un remplacement complet, la fiche était écrasée.
- Retirés de la maquette car le serveur ne les connaît pas : le type `PACKAGING`, les cases « fabriqué à l'atelier » et « utilisable au salon ». Le dépôt de photo reste affiché mais **n'enregistre rien** (aucun stockage serveur).
- Retirés (code de démonstration du formulaire) : méthodes `nextReference`, `suppliers`, `create` et types `ArticleWDto`, `SupplierOptionRDto`, `ArticleFormValue` du service de démonstration `features/articles/data-access/article-service.ts`. La création réelle est `features/article/data-access/article-service.ts`.
- Traductions : clés ajoutées dans `articles.form.*` (`editTitle`, `updated`, `cost`, `negativeError`, `thresholdError`, `rangeError`, `created` sans « démonstration ») et `articles.type.SEMI_FINISHED` dans `fr.json` et `mg.json` (**à relire par un locuteur malgache** : traductions mg écrites sans validation).

### Données

Aucune migration. Journal : voir ART-01.

Pour les tests frontend, `ROLE_PERMISSIONS.ADMIN` (`core/auth/auth-testing.ts`) contient maintenant `articles.create` et `articles.update`, comme le seed de la migration `2026_10_26-01`.

## Règles métier à respecter par les autres tickets

- Pour créer ou modifier un article depuis un autre écran, **réutiliser la fiche** (liens vers `/articles/new` et `/articles/:id/edit`), ne pas recréer de formulaire.
- Un lien d'écriture est toujours sous `*vvsHasPermission` et la route sous `permissionGuard` ; les gardes servent l'ergonomie, le serveur répond 403.
- `PUT` remplace toute la fiche : ne jamais l'appeler avec un formulaire non pré-rempli (voir garde-fou ci-dessus).
- Ne pas filtrer ni changer le type d'un article sans repasser par la fiche : gamme et prix sont effacés par le serveur pour un article qui n'est pas un produit fini ([ART-04](../ART-04/description.md)).

## Fichiers concernés

**Frontend** (`valimbavaka-ui/src/app/`)
- `features/articles/pages/article-form-page/article-form-page.{ts,html,css}` (maquette adaptée)
- `features/articles/data-access/article-draft-store.ts` (valeur du brouillon alignée sur l'API)
- `features/articles/articles.routes.ts` (`new` et `:id/edit` sur le formulaire, `sheet/new` retirée)
- `features/articles/pages/article-list-page/*`, `article-detail-page/*` (boutons sous permission)
- `features/articles/data-access/article-service.ts`, `article-models.ts`, `article-fixtures.ts` (code de démonstration retiré)
- `core/auth/auth-testing.ts` (permissions d'écriture de l'administrateur)
- `public/i18n/fr.json`, `mg.json`
- Supprimé : `features/article/pages/article-sheet-page/*` (formulaire d'ART-01)

**Tests** (`valimbavaka-ui/src/app/features/`)
- `articles/pages/article-form-page/article-form-page.spec.ts` (création), `article-form-page-validation.spec.ts` (règles de saisie), `article-form-page-edit.spec.ts` (modification), `article-form-page.harness.ts`, `article-form-page.fixtures.ts`
- `articles/articles.routes.spec.ts`, `articles/pages/article-list-page/article-list-page.spec.ts`, `articles/pages/article-detail-page/article-detail-page.spec.ts`

**Backend** : inchangé (voir ART-01).

## Dépendances

**Ce ticket s'appuie sur** : [ART-01](../ART-01/guide.md) (API et fiche), [JNL-01](../JNL-01/guide.md) (journal), [AUTH-01](../AUTH-01/guide.md) (permissions et `*vvsHasPermission`).

**Ces tickets s'appuient sur lui** : aucun listé ; [ART-05](../ART-05/description.md) et [ART-06](../ART-06/description.md) qui branchent la liste et le détail sur l'API doivent conserver les liens vers `/articles/new` et `/articles/:id/edit`.

## Comment l'utiliser depuis un autre ticket

```html
<!-- Lien vers la modification d'un article, depuis une liste ou un détail -->
<a [routerLink]="['/articles', article.id, 'edit']" *vvsHasPermission="'articles.update'">
  {{ 'articles.detail.edit' | translate }}
</a>
```

## Points d'attention

- **Liste et détail encore sur données de démonstration** : leurs identifiants (`art-art-0031`) ne sont pas des UUID du serveur. Le bouton « Modifier » du détail d'un article de démonstration ouvre donc une fiche qui répond « introuvable ». Cela se résout quand [ART-05](../ART-05/description.md) (liste) et [ART-06](../ART-06/description.md) (détail 360°) lisent `GET /api/v1/articles`. Avec un article réel, `/articles/<uuid>/edit` fonctionne dès maintenant.
- **À trancher avec le métier** : `PACKAGING` (emballage), « fabriqué » et « utilisable au salon » figuraient dans la maquette, pas dans le contrat d'ART-01 ; ils ont été retirés du formulaire (voir ART-04). La liste de démonstration filtre encore sur `PACKAGING`.
- **Photo** : `PhotoDrop` fait un aperçu local seulement ; aucun ticket ne prévoit le stockage.
- **Code-barres** : le formulaire exige la clé EAN-13, le serveur seulement 13 chiffres. Un code-barres saisi par une autre voie (import [ART-07](../ART-07/description.md)) peut donc avoir une clé fausse : il reste modifiable car la clé n'est contrôlée que pour un code-barres changé.
- La fiche lit l'article **à l'ouverture seulement** : passer de `/articles/A/edit` à `/articles/B/edit` sans quitter l'écran garderait la fiche de A. L'interface actuelle n'offre pas ce chemin.
- Reste dans `fr.json` / `mg.json` l'espace de noms `article.*` (écrit pour le formulaire supprimé) ; seuls `article.form.newTitle` et `article.form.editTitle` servent encore (titres de route). Le reste peut être nettoyé.
- Hors ligne : l'écriture n'est pas mise en file ([SYS-07](../SYS-07/description.md)) ; une erreur réseau affiche le message et conserve la saisie.
- Les tests de ce ticket n'ont pas été exécutés par l'auteur : voir le scénario avant de s'appuyer sur ce guide pour un comportement limite.
