# Scénarios de test — SYS-05 · Rôles métier, référentiels et compte administrateur initial

Statut : ✔ rempli depuis les tests existants au 2026-10-23 — à valider par le traiteur · Ticket : [#111](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/111) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, Testcontainers PostgreSQL (jamais H2).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | sans objet pour le démarrage ; `ADMIN` / `SELLER` pour la lecture des référentiels |
| Données de départ | base **vierge** (migrations appliquées, aucun utilisateur) |
| Variables d'environnement | `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_FIRST_NAME`, `ADMIN_LAST_NAME` |
| Tickets à avoir déployés avant | aucun |

## Scénarios issus des critères d'acceptation

| # | Critère | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Rôles exactement Administrateur, Vendeuse, Livreur | Quand les migrations s'appliquent sur une base neuve | trois rôles, pas de `USER` | `AdminAccountInitializerIT.migrations_freshDatabase_seedExactlyTheThreeBusinessRoles` | ☐ |
| 2 | Canaux de vente disponibles | Quand on lit `/api/v1/sales-channels` sur une base neuve | boutique, Facebook, téléphone, livraison | `ReferentialServiceImplIT.getSalesChannels_freshDatabase_returnsSeededChannels` | ☐ |
| 2b | Gammes | Quand on lit `/api/v1/product-ranges` | Zezika, Botanika, Solika | `ReferentialServiceImplIT.getProductRanges_freshDatabase_returnsSeededRanges` | ☐ |
| 2c | Emplacements de stock | Quand on lit `/api/v1/stock-locations` | entrepôt, boutique, livraison, salon | `ReferentialServiceImplIT.getStockLocations_freshDatabase_returnsSeededLocations` | ☐ |
| 2d | Côté interface | Quand l'interface demande les trois listes | endpoints dédiés, tri par libellé | `referential-service.spec.ts` « should list sales channels sorted by label », « should call the dedicated endpoints for ranges and stock locations » | ☐ |
| 3 | Administrateur initial depuis l'environnement | Étant donné une base sans administrateur · Quand l'application démarre | compte créé, changement de mot de passe requis | `AdminAccountInitializerIT.run_atStartupWithoutAdministrator_createsAdministratorRequiringPasswordChange` | ☐ |
| 3b | Idempotence | Quand un administrateur existe déjà | rien n'est créé | `AdminAccountInitializerIT.run_administratorAlreadyExists_isIdempotent` | ☐ |

## Sécurité et erreurs

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Configuration complète | aucune violation | `AdminAccountPropertiesTest.violations_completeConfiguration_returnsNothing` | ☐ |
| Valeur invalide (courriel, mot de passe < 15, nom vide) | la variable fautive est nommée | `AdminAccountPropertiesTest.violations_invalidValue_namesTheVariable` | ☐ |
| Mot de passe dans les journaux ou le `toString` | masqué | `AdminAccountPropertiesTest.toString_always_masksPassword` | ☐ |
| Aucun mot de passe écrit dans le code ou un changelog | aucune occurrence | _(vérification manuelle, voir recette)_ | ☐ |
| Base sans administrateur et variables absentes | arrêt du démarrage avec message explicite | _(à ajouter : aucun test dédié repéré)_ | ☐ |
| Lecture des référentiels sans jeton / mauvais rôle | 401 / 403 (livreur sans `referentials.read`) | _(à ajouter : aucun test dédié repéré)_ | ☐ |

## Cas limites et hors ligne

- Redémarrage répété : un seul administrateur, aucune erreur.
- Base existante contenant l'ancien rôle `USER` : utilisateurs rattachés à Vendeuse, rôle `USER` supprimé (migration `2026_10_15-01-1`) ; à tester sur une copie de base avant la mise en production.
- Rollback des changesets : chaque changeset a un `rollback`.

## Tests existants (repérés dans le code)

- Backend : `feature/user/config/AdminAccountInitializerIT.java`, `AdminAccountPropertiesTest.java`, `feature/referential/service/impl/ReferentialServiceImplIT.java`
- Frontend : `features/referential/data-access/referential-service.spec.ts`

## À faire à la main (recette QA)

- Démarrer sur une base vide avec les quatre variables : se connecter avec le compte, vérifier que `/api/v1/auth/me` renvoie `passwordChangeRequired = true`.
- Démarrer sur une base vide sans variables : le démarrage échoue avec la liste des variables manquantes.
- Rechercher dans le dépôt toute occurrence d'un mot de passe réel dans le code ou les changelogs.
- Appeler les trois référentiels en vendeuse (200) et en livreur (403).

## Résultat de l'exécution

- Date : _(à compléter)_
- Environnement : _(à compléter)_
- Anomalies ouvertes : _(à compléter)_
