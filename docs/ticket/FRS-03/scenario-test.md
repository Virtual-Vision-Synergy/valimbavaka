# Scénarios de test — FRS-03 · Filtrage des fournisseurs par type

Statut : ☐ tests écrits, **à lancer** (CI ou `mvn verify` / `ng test`) — puis recette manuelle (🧪) · Ticket : [#19](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/19) · [description](description.md) · [guide](guide.md)

Légende de la colonne « Statut » : ✅ auto = test automatique vert · ✅ base = vérifié en base après migrations · 🔍 code = vérifié par lecture du code (aucun test) · ❌ écart = comportement différent de l'attendu · ⏭ hors périmètre = renvoyé à un autre ticket · 🧪 recette = à faire à la main · ☐ = test écrit, pas encore exécuté. Détail : [CONSIGNE.md](../CONSIGNE.md#4-remplir-le-scénario-de-test).

Point de départ : l'API filtrait déjà par `type`, `active` et `search` (FRS-01 : `SupplierController.getSuppliers`, `SupplierRepository.search`). L'écran de liste n'en exposait aucun. FRS-03 ajoute à la liste un sélecteur de type et une recherche, combinables.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (seul à voir la liste) ; `DELIVERY_DRIVER` / `SELLER` refusés |
| Données de départ | « Huiles Ravo » (`LOCAL`, contact « Rasoa ») et « Provence Essences » (`FOREIGN`) |
| Tickets à avoir déployés avant | FRS-01 |

## Scénarios issus des critères d'acceptation

| # | Critère | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | La liste se met à jour selon le filtre et la recherche | À l'ouverture de `/suppliers` | tous les types, sans recherche, page 1, tri par nom | `supplier-list-page.spec.ts` « should load every type, without filter, sorted by name on opening » | ☐ |
| 1b | idem, filtre | Il choisit « Étranger » | nouvelle requête `type=FOREIGN`, seule « Provence Essences » affichée, sans clic supplémentaire | « should reload with the chosen type and show only the returned suppliers » | ☐ |
| 1c | idem, retour à « Tous les types » | Il revient sur « Tous les types » | requête sans paramètre `type` | « should drop the type when "all types" is chosen again » | ☐ |
| 1d | idem, recherche + filtre | Type « Local » puis recherche `"  Rasoa "` | requête `type=LOCAL&search=Rasoa` (recherche nettoyée, filtre conservé) | « should combine the trimmed search with the chosen type » | ☐ |
| 1e | idem, côté serveur | `GET /api/v1/suppliers?type=FOREIGN&search=<préfixe en majuscules>` avec un local et un étranger de même préfixe | 200, un seul résultat, l'étranger | `SupplierSecurityIT.getSuppliers_byTypeAndSearch_returnsOnlyMatchingSuppliers` ; `SupplierRepositoryIT.search_withTypeAndStatusFilters_returnsOnlyMatchingSuppliers` (FRS-01) | ☐ |
| 1f | idem, recherche sur le contact | `search=Rasoa` | « Huiles Ravo » trouvé par son contact | `SupplierRepositoryIT.search_byContactName_findsSupplierWhoseNameDoesNotMatch` (FRS-01) | ☐ |
| 1g | idem, envoi des paramètres | `list({ type: 'FOREIGN', search: 'Rasoa' })` ; puis `search: ''` sans type | `type` et `search` envoyés ; valeurs vides non envoyées | `supplier-service.spec.ts` « envoie le type et la recherche choisis », « n'envoie ni type ni recherche quand ils sont vides » | ☐ |

## Sécurité et erreurs

Aucun nouvel endpoint : FRS-03 utilise `GET /api/v1/suppliers` de FRS-01, sans écriture (pas de journal, pas de mass assignment).

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Liste sans jeton | `GET /api/v1/suppliers?type=LOCAL` | 401 | `SupplierSecurityIT.getSuppliers_withoutToken_returns401` (FRS-01) | ☐ |
| Liste avec un autre rôle | jeton livreur | 403 | `SupplierSecurityIT.getSuppliers_withNonAdminRole_returns403` (FRS-01) | ☐ |
| Type inconnu | `type=IMPORT` | 400 `MALFORMED_REQUEST` (conversion de l'énumération refusée par Spring, `GlobalExceptionHandler.handleExceptionInternal`) | `SupplierSecurityIT.getSuppliers_unknownType_returns400` | ☐ |
| Erreur serveur à l'affichage | 403 renvoyé | message du `ProblemDetail` affiché dans l'alerte | « should show the server message when the list cannot be loaded » | ☐ |

## Cas limites

| Cas | Exemple d'entrée | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Filtre depuis une autre page | page 2 sur 3, puis type « Étranger » | retour en page 1 | « should go back to the first page when a filter changes » | ☐ |
| Aucun résultat filtré | recherche « Introuvable » | « Aucun fournisseur ne correspond au filtre ou à la recherche » | « should say that nothing matches when a filter returns no supplier » | ☐ |
| Liste vide sans filtre | aucun fournisseur | « Aucun fournisseur » (message distinct) | « should say that there is no supplier yet when the unfiltered list is empty » | ☐ |
| Deux changements rapides | « Local » puis « Étranger » avant la réponse | la première requête est annulée ; seule la réponse « Étranger » s'affiche | « should ignore the answer of a request overtaken by a newer filter » | ☐ |
| Recherche vide ou espaces | `"   "` | recherche ignorée (non envoyée) ; côté serveur `search` vide = tout (`PageQuery.searchTerm`) | `SupplierListPage.onSearch` (`trim`) et `toHttpParams` ; test service « n'envoie ni type ni recherche quand ils sont vides » | ☐ |
| Casse de la recherche | `HUILES` | trouve « Huiles Ravo » (`lower` des deux côtés, `SupplierRepository.search`) | `getSuppliers_byTypeAndSearch_returnsOnlyMatchingSuppliers` (préfixe en majuscules) | ☐ |
| Recherche avec `%` ou `_` | `50%` | interprétés comme jokers SQL `like`, pas comme texte | `SupplierRepository.search` (pas d'échappement) | 🔍 code |
| Fournisseurs désactivés | filtre « Local » | désactivés inclus (badge « DÉSACTIVÉ ») : la liste n'envoie pas `active` | `SupplierListPage.loadSuppliers` | 🔍 code |
| Filtre conservé | rechargement de la page, retour depuis une fiche | filtre et recherche perdus (non mis dans l'URL) | `SupplierListPage` (signaux locaux) | 🔍 code |
| Hors ligne | réseau coupé au changement de filtre | message d'erreur, liste précédente conservée | lecture en ligne seulement | ⏭ hors périmètre (SYS-07) |

## Tests existants

- Frontend : `valimbavaka-ui/src/app/features/supplier/pages/supplier-list-page/supplier-list-page.spec.ts` (nouveau, 9 tests), `valimbavaka-ui/src/app/features/supplier/data-access/supplier-service.spec.ts` (2 tests ajoutés).
- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/supplier/controller/SupplierSecurityIT.java` (2 tests ajoutés : `getSuppliers_byTypeAndSearch_returnsOnlyMatchingSuppliers`, `getSuppliers_unknownType_returns400`), `…/repository/SupplierRepositoryIT.java` (FRS-01, inchangé).

## Recette manuelle (🧪)

| Étape | Attendu | Statut |
|---|---|---|
| Choisir « Local », puis « Étranger », puis « Tous les types » | la liste change à chaque choix, sans bouton à cliquer | 🧪 recette |
| Avec « Local », chercher « Rasoa » | seule « Huiles Ravo » ; effacer la recherche et valider rend tous les locaux | 🧪 recette |
| Sur mobile (largeur 375 px) | sélecteur, recherche et bouton passent à la ligne, sans défilement horizontal | 🧪 recette |
| Lecteur d'écran sur le sélecteur et la recherche | libellés « Filtrer par type », « Rechercher un fournisseur » annoncés | 🧪 recette |

## Résultat de l'exécution

- **Date** : 2026-10-26 (écriture des tests).
- **Exécution** : non lancée par le traiteur (règle du projet : pas d'exécution des tests en local par l'assistant). À lancer : CI de la PR, ou `cd valimbavaka-core && mvn -B -ntp verify` et `cd valimbavaka-ui && npx ng test --watch=false`. Vérifié avant la PR : `tsc --noEmit -p tsconfig.spec.json` et `eslint src/app/features/supplier` sans erreur, `spotless:apply` passé.
- **Anomalies** :
  1. ⚠ Faible : les caractères `%` et `_` de la recherche agissent comme jokers SQL (`SupplierRepository.search`). Même comportement pour la recherche du journal ; à traiter globalement si gênant.
  2. ⚠ Faible : le filtre et la recherche ne sont pas gardés dans l'URL, ils se perdent au retour depuis une fiche.
  3. ℹ La liste n'expose pas de filtre actif / désactivé, bien que l'API l'accepte (`active`) : hors critère de FRS-03.
- **Recette manuelle** : 4 étapes 🧪.
