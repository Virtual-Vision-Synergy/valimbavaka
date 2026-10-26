# Guide d'intégration — ART-02 · Création / modification d'un article

Statut : ✅ écrit le 2026-10-26 — **tests non exécutés** (voir « Résultat de l'exécution » du scénario) · Ticket : [#22](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/22) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

La création et la modification d'un article passent par un seul écran réel : la fiche `ArticleSheetPage`, livrée avec [ART-01](../ART-01/guide.md). ART-02 la branche sur le catalogue : le bouton « Nouvel article » ouvre `/articles/new`, le bouton « Modifier » du détail ouvre `/articles/:id/edit` (formulaire pré-rempli avec la fiche lue). Les deux boutons n'apparaissent qu'avec la permission `articles.create` / `articles.update`. **Aucun endpoint n'a été ajouté** : l'API est celle d'ART-01. Autre apport : en modification, la fiche ne s'enregistre plus tant qu'elle n'a pas été lue.

## Contrat exposé

### Endpoints backend

Aucun nouvel endpoint. Voir le [guide ART-01](../ART-01/guide.md#endpoints-backend-apiv1articles) : `POST /api/v1/articles` (`articles.create`), `PUT /api/v1/articles/{id}` (`articles.update`, `version` obligatoire pour le verrou optimiste), `PATCH /api/v1/articles/{id}/status`. Chaque écriture réelle écrit une ligne `CREATE` / `UPDATE` (ressource `ARTICLE`) dans le journal ([JNL-01](../JNL-01/guide.md)).

### Frontend

- Routes (`features/articles/articles.routes.ts`, sous `/articles` déjà gardé par `articles.read`) :
  - `/articles/new` : fiche vide, garde `articles.create` ; remplace le formulaire de démonstration et l'ancienne route provisoire `/articles/sheet/new` (supprimée) ;
  - `/articles/:id/edit` : fiche pré-remplie, garde `articles.update`.
- Boutons : « Nouvel article » dans `article-list-page.html` et « Modifier » dans `article-detail-page.html`, tous deux sous `*vvsHasPermission`. Le serveur décide de toute façon (403 sans permission).
- Pour lier vers la modification depuis une autre page : `[routerLink]="['/articles', article.id, 'edit']"` avec `*vvsHasPermission="'articles.update'"`.
- `ArticleSheetPage` (`features/article/pages/article-sheet-page`) : `input()` `id` lié à la route. Sans `id` : création (`POST`, sans `version`). Avec `id` : lecture à l'ouverture, pré-remplissage, puis `PUT` avec la `version` lue. Retour à `/articles` après enregistrement.
- Nouveau garde-fou : en modification, le bouton « Enregistrer » est désactivé et `save()` ne fait rien tant que la fiche n'a pas été lue (réponse en attente, 404, 500, réseau coupé). Sans cela, une lecture échouée laissait un formulaire vide que l'on pouvait envoyer : `PUT` étant un remplacement complet, la fiche était écrasée.
- Retirés (étaient la maquette de création) : `features/articles/pages/article-form-page/*`, `data-access/article-draft-store.ts`, et dans le service de démonstration `ArticleService` (`features/articles/data-access`) les méthodes `nextReference`, `suppliers`, `create` ainsi que les types `ArticleWDto`, `SupplierOptionRDto`, `ArticleFormValue`. Ne pas les réintroduire : la création réelle est `features/article/data-access/article-service.ts`.

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
- `features/articles/articles.routes.ts` (route `new` pointe sur la fiche réelle, `sheet/new` retirée)
- `features/articles/pages/article-list-page/article-list-page.{html,ts}` (bouton sous permission)
- `features/articles/pages/article-detail-page/article-detail-page.{html,ts}` (lien « Modifier », `demoAction` retiré)
- `features/article/pages/article-sheet-page/article-sheet-page.{html,ts}` (`canSave`, `sheetRead`)
- `features/articles/data-access/article-service.ts`, `article-models.ts`, `article-fixtures.ts` (code de démonstration retiré)
- `core/auth/auth-testing.ts` (permissions d'écriture de l'administrateur)

**Tests** (`valimbavaka-ui/src/app/features/`)
- `articles/articles.routes.spec.ts`
- `articles/pages/article-list-page/article-list-page.spec.ts`
- `articles/pages/article-detail-page/article-detail-page.spec.ts`
- `article/pages/article-sheet-page/article-sheet-page-edit.spec.ts` (le reste : `article-sheet-page.spec.ts`, ART-01)

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

- **Liste et détail encore sur données de démonstration** : leurs identifiants (`art-art-0031`) ne sont pas des UUID du serveur. Le bouton « Modifier » du détail d'un article de démonstration ouvre donc une fiche qui répond « introuvable ». Cela se résout quand [ART-05](../ART-05/description.md) (liste) et [ART-06](../ART-06/description.md) (détail 360°) lisent `GET /api/v1/articles`. Avec un article réel, ouvrir `/articles/<uuid>/edit` fonctionne dès maintenant.
- La fiche lit l'article **à l'ouverture seulement** : passer de `/articles/A/edit` à `/articles/B/edit` sans quitter l'écran garderait la fiche de A. L'interface actuelle n'offre pas ce chemin ; si elle l'offre un jour, remplacer `ngOnInit` par une lecture réactive à l'`id`.
- Restent orphelins, sans appelant, depuis la suppression du formulaire de démonstration : `features/articles/components/photo-drop`, `components/article-preview` et `data-access/ean13.ts` (photo, aperçu, clé de contrôle EAN-13). Le stockage de la photo n'a aucun ticket ; à rebrancher ou supprimer par le ticket qui le prendra. Les clés de traduction `articles.form.*` correspondantes restent dans `fr.json` et `mg.json`.
- Hors ligne : l'écriture n'est pas mise en file ([SYS-07](../SYS-07/description.md)) ; une erreur réseau affiche le message et conserve la saisie.
- Les tests de ce ticket n'ont pas été exécutés par l'auteur : voir le scénario avant de s'appuyer sur ce guide pour un comportement limite.
