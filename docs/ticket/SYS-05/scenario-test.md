# Scénarios de test — SYS-05 · Rôles métier, référentiels et compte administrateur initial

Statut : ✅ exécuté le 2026-10-23 — reste la recette manuelle (🧪) · Ticket : [#111](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/111) · [description](description.md) · [guide](guide.md)

Légende de la colonne « Statut » : ✅ auto = test automatique vert · ✅ base = vérifié en base après migrations · 🔍 code = vérifié par lecture du code (aucun test) · ❌ écart = comportement différent de l'attendu · 🧪 recette = à faire à la main. Détail : [CONSIGNE.md](../CONSIGNE.md#4-remplir-le-scénario-de-test).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | aucun pour le démarrage ; `ADMIN`, `SELLER`, `DELIVERY_DRIVER` pour les référentiels |
| Données de départ | base **vierge** (migrations appliquées, aucun utilisateur) |
| Variables d'environnement | `ADMIN_EMAIL`, `ADMIN_PASSWORD` (≥ 15 caractères), `ADMIN_FIRST_NAME`, `ADMIN_LAST_NAME` |
| Tickets à avoir déployés avant | aucun |

## Scénarios issus des critères d'acceptation

| # | Critère | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Rôles exactement Administrateur, Vendeuse, Livreur | Migrations sur base neuve | `ADMIN`=Administrateur, `SELLER`=Vendeuse, `DELIVERY_DRIVER`=Livreur, pas de `USER` | `AdminAccountInitializerIT.migrations_freshDatabase_seedExactlyTheThreeBusinessRoles` ; lecture de `roles` en base | ✅ auto · ✅ base |
| 1b | Pas d'autre rôle possible | Insertion d'un rôle `USER` | refus `ck_roles_name` | insertion refusée en base | ✅ base |
| 1c | Ancien changeset corrigé par un nouveau | Historique Liquibase | `2026_10_07-01-3` inchangé, corrigé par `2026_10_15-01-1` puis `2026_10_16-01-1` | fichiers de migration | 🔍 code |
| 2 | Canaux de vente | `GET /api/v1/sales-channels` | `STORE` Boutique, `FACEBOOK` Facebook, `PHONE` Téléphone, `DELIVERY` Livraison | `ReferentialServiceImplIT.getSalesChannels_freshDatabase_returnsSeededChannels` ; lecture en base | ✅ auto · ✅ base |
| 2b | Gammes | `GET /api/v1/product-ranges` | `ZEZIKA`, `BOTANIKA`, `SOLIKA` | `ReferentialServiceImplIT.getProductRanges_freshDatabase_returnsSeededRanges` | ✅ auto · ✅ base |
| 2c | Emplacements de stock | `GET /api/v1/stock-locations` | `WAREHOUSE` Entrepôt, `STORE` Boutique, `DELIVERY` Livraison, `TRADE_SHOW` Salon | `ReferentialServiceImplIT.getStockLocations_freshDatabase_returnsSeededLocations` | ✅ auto · ✅ base |
| 2d | idem (interface) | Chargement des trois listes | endpoints dédiés, tri par libellé, 100 éléments | `referential-service.spec.ts` « should list sales channels sorted by label », « should call the dedicated endpoints for ranges and stock locations » | ✅ auto |
| 3 | Administrateur initial depuis l'environnement | Base sans administrateur · démarrage | compte créé, changement de mot de passe requis | `AdminAccountInitializerIT.run_atStartupWithoutAdministrator_createsAdministratorRequiringPasswordChange` ; `UserServiceImplTest.createInitialAdministratorIfMissing_validVariables_createsAdministratorRequiringPasswordChange` | ✅ auto |
| 3b | idem (idempotent) | Un administrateur existe déjà | rien n'est créé | `AdminAccountInitializerIT.run_administratorAlreadyExists_isIdempotent` ; `UserServiceImplTest.createInitialAdministratorIfMissing_administratorExists_createsNothing` | ✅ auto |
| 3c | Jamais de mot de passe dans le code ou un changelog | Recherche dans les migrations | aucune valeur de mot de passe | recherche dans les 10 fichiers de migration : seule la colonne `password` apparaît | ✅ base |
| 3d | « doit changer son mot de passe à la première connexion » | Première connexion de l'administrateur | écran de changement imposé | drapeau `is_password_change_required` posé, mais aucun écran ni endpoint pour le lever | ❌ écart |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Configuration complète | 4 variables valides | aucune violation | `AdminAccountPropertiesTest.violations_completeConfiguration_returnsNothing` | ✅ auto |
| Valeur invalide | email `abc`, mot de passe de 14 caractères, prénom vide | la variable fautive est nommée | `AdminAccountPropertiesTest.violations_invalidValue_namesTheVariable` (paramétré) | ✅ auto |
| Variables absentes sur base vide | aucune variable | démarrage refusé, message listant les variables | `UserServiceImplTest.createInitialAdministratorIfMissing_variablesMissing_refusesStartup` | ✅ auto |
| Mot de passe dans les messages | mot de passe trop court | jamais affiché | `UserServiceImplTest.createInitialAdministratorIfMissing_passwordTooShort_neverLeaksPasswordInMessage` ; `AdminAccountPropertiesTest.toString_always_masksPassword` | ✅ auto |
| Lecture des référentiels par rôle | vendeuse ; livreur | 200 ; 403 (`referentials.read` absent du livreur) | permissions vérifiées en base ; `@HasPermission("referentials.read")` | ✅ base · 🔍 code |

## Cas limites

| Cas | Exemple | Résultat attendu | Comment c'est vérifié | Statut |
|---|---|---|---|---|
| Redémarrages répétés | 3 démarrages successifs | un seul administrateur | `run_administratorAlreadyExists_isIdempotent` | ✅ auto |
| Mot de passe à la limite | 15 caractères ; 128 ; 129 | accepté ; accepté ; refusé | `AdminAccountProperties.MIN_PASSWORD_LENGTH = 15`, `MAX = 128` | 🔍 code |
| Email de l'administrateur avec majuscules | `Admin.Test@Valimbavaka.mg` | enregistré en minuscules | `User.normalizeEmail` | 🔍 code |
| Base existante avec l'ancien rôle `USER` | utilisateurs rattachés à `USER` | rattachés à Vendeuse, `USER` supprimé | `2026_10_15-01-1` ; chaîne complète rejouée sans erreur sur base vide | ✅ base |
| Rôle `ADMIN` absent (migrations non jouées) | — | démarrage refusé « Rôle ADMIN absent » | `createInitialAdministratorIfMissing` | 🔍 code |
| Rollback | — | chaque changeset a un `rollback` | lecture des fichiers | 🔍 code |

## Tests existants

- Backend : `feature/user/config/AdminAccountInitializerIT.java`, `AdminAccountPropertiesTest.java`, `feature/user/service/impl/UserServiceImplTest.java` (`createInitialAdministratorIfMissing_*`), `feature/referential/service/impl/ReferentialServiceImplIT.java`
- Frontend : `features/referential/data-access/referential-service.spec.ts`

## Recette manuelle (🧪)

| Étape | Attendu | Statut |
|---|---|---|
| Démarrer sur base vide avec les 4 variables, se connecter | connexion OK, `/api/v1/auth/me` renvoie `passwordChangeRequired = true` | 🧪 recette |
| Démarrer sur base vide sans variables | démarrage refusé avec la liste des variables | 🧪 recette |
| Appeler les 3 référentiels en vendeuse puis en livreur | 200 puis 403 | 🧪 recette |

## Résultat de l'exécution

- **Date** : 2026-10-23.
- **Backend** : CI GitHub `mvn verify` vert sur un code identique à `develop`. **Frontend** : `ng test` local, 102 tests verts sur 102. **Base** : les 10 migrations rejouées sur PostgreSQL 16 vierge sans erreur ; rôles, référentiels et permissions relus.
- **Anomalies** :
  1. ❌ Gravité moyenne : le changement de mot de passe obligatoire n'est pas réalisable (voir AUTH-01).
  2. ⚠ La liste des canaux de vente reste « à confirmer avec le métier ».
  3. ⚠ Le livreur n'a pas `referentials.read` : à revoir si l'espace livreur doit afficher des emplacements ou des canaux.
- **Recette manuelle** : 3 étapes 🧪.
