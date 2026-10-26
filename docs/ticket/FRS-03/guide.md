# Guide d'intégration — FRS-03 · Filtrage des fournisseurs par type

Statut : ✅ rédigé au 2026-10-26 — tests écrits, exécution à confirmer par la CI · Ticket : [#19](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/19) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

La liste des fournisseurs (`/suppliers`) a maintenant un sélecteur de type (tous, local, étranger), appliqué dès le choix, et une recherche sur le nom du fournisseur ou du contact. Les deux se combinent. Côté serveur, rien de nouveau : FRS-03 branche l'écran sur les filtres que l'API de [FRS-01](../FRS-01/guide.md) acceptait déjà. Le service Angular sait désormais envoyer `type`, ce que d'autres écrans peuvent réutiliser (sélecteur de fournisseur local pour un réapprovisionnement, par exemple).

## Contrat exposé

### Endpoint backend (inchangé, FRS-01)

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/suppliers` | `suppliers.read` | `PageQuery` (`page`, `size` ≤ 100, `search`, `sortBy`, `sortDirection`) + `type` (`LOCAL` / `FOREIGN`) + `active` (booléen) | 200 `PageResponse<SupplierRDto>` | 401, 403, 400 `MALFORMED_REQUEST` (type inconnu), 422 (`size` > 100) |

- Paramètres lus dans `SupplierController.getSuppliers` (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/supplier/controller/SupplierController.java:57`).
- Requête dans `SupplierRepository.search` (`…/feature/supplier/repository/SupplierRepository.java:27-40`) : `search` insensible à la casse sur `name` **ou** `contactName` ; `type` / `active` absents = pas de filtre.
- Tri en liste blanche : `SupplierServiceImpl.SORT_FIELDS` (`…/service/impl/SupplierServiceImpl.java:38`) = `createdAt`, `name`, `type`.

### Frontend (`valimbavaka-ui/src/app/features/supplier/`)

- Type `SupplierListQuery` (`data-access/supplier-models.ts:37`) : `PageQuery` + `type?`.
- `SupplierService.list(query: SupplierListQuery)` (`data-access/supplier-service.ts:20`) : ajoute `type` aux paramètres seulement s'il est choisi ; `search` vide n'est pas envoyé (`shared/models/http-params.ts`, `toHttpParams`).
- Page `features/suppliers/pages/supplier-list-page/` (liste de la maquette, branchée sur l'API ; remplace `features/supplier/pages/supplier-list-page`) :
  - signaux `query` (recherche) et `type` (`'ALL'` ou `LOCAL` / `FOREIGN`) ; la recherche est rognée et temporisée (300 ms) avant d'être envoyée ;
  - `page` revient à 1 dès que le type ou la recherche change ;
  - `suppliers` est un `rxResource` : un nouveau filtre ou une nouvelle page annule la requête précédente, donc une réponse lente ne remplace pas la plus récente ;
  - filtre par type en pastilles (`vvs-filter-chips`), message vide distinct quand un filtre est actif.
- Traductions `supplier.filter.*` et `supplier.list.noMatch` dans `valimbavaka-ui/public/i18n/fr.json` (lignes 1295-1303) et `mg.json`.

### Données

- Aucune table, migration, permission ni ligne de journal : lecture seule.

## Règles métier pour les autres tickets

- Pour proposer des fournisseurs d'un type, appeler `SupplierService.list` avec `type` plutôt que filtrer côté client : la pagination reste juste.
- La liste d'administration montre aussi les fournisseurs désactivés ; un écran de **sélection** doit ajouter `active=true` (paramètre accepté par l'API ; `SupplierListQuery` ne l'a pas encore, à ajouter dans le ticket qui en a besoin, par exemple [FRS-04](../FRS-04/description.md) ou [STK-05](../STK-05/description.md)).
- Le type pilote le délai de réapprovisionnement ([FRS-04](../FRS-04/description.md)) : réutiliser les valeurs `LOCAL` / `FOREIGN` de `SupplierType` (`supplier-models.ts`), sans liste en dur.

## Fichiers concernés

**Frontend**
- `valimbavaka-ui/src/app/features/supplier/data-access/supplier-models.ts` (`SupplierListQuery`)
- `valimbavaka-ui/src/app/features/supplier/data-access/supplier-service.ts` (`list`)
- `valimbavaka-ui/src/app/features/suppliers/pages/supplier-list-page/supplier-list-page.ts`, `.html`, `.css`
- `valimbavaka-ui/public/i18n/fr.json`, `valimbavaka-ui/public/i18n/mg.json`

**Backend** : aucun changement de code.

**Tests**
- `valimbavaka-ui/src/app/features/suppliers/pages/supplier-list-page/supplier-list-page.spec.ts` (porté depuis l'ancienne liste)
- `valimbavaka-ui/src/app/features/supplier/data-access/supplier-service.spec.ts`
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/supplier/controller/SupplierSecurityIT.java` (`getSuppliers_byTypeAndSearch_returnsOnlyMatchingSuppliers`, `getSuppliers_unknownType_returns400`)

## Dépendances

**Ce ticket s'appuie sur** : [FRS-01](../FRS-01/guide.md) (API de liste, filtres `type` / `active` / `search`).

**Ces tickets s'appuient sur lui** : aucun directement ; [FRS-04](../FRS-04/description.md) et [STK-05](../STK-05/description.md) peuvent réutiliser `SupplierService.list` avec `type`.

## Comment l'utiliser depuis un autre ticket

- Injecter `SupplierService` et appeler `list` avec un `SupplierListQuery` contenant `type` : voir le `rxResource` `suppliers` de `SupplierListPage`.
- Appel direct de l'API : `GET /api/v1/suppliers?type=LOCAL&active=true&search=Rasoa` (exemple de test : `SupplierSecurityIT.getSuppliers_byTypeAndSearch_returnsOnlyMatchingSuppliers`).

## Points d'attention

- `%` et `_` dans la recherche agissent comme jokers SQL (`SupplierRepository.search` n'échappe pas) ; même chose pour le journal.
- Filtre et recherche ne sont pas dans l'URL : ils se perdent au retour depuis une fiche ou au rechargement.
- La recherche part au clic sur « Rechercher » ou à la touche Entrée (pas de recherche à la frappe) ; le type, lui, s'applique dès le choix.
- Pas de filtre actif / désactivé dans l'écran (l'API l'accepte) : à ajouter si un ticket le demande.
- Tests écrits mais non exécutés au moment de la rédaction : se fier au statut de la CI de la PR.
