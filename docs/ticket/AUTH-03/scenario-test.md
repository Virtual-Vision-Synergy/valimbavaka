# Scénarios de test — AUTH-03 · Création et modification d'un utilisateur

Statut : ✔ rempli depuis les tests existants au 2026-10-23 — à valider par le traiteur · Ticket : [#12](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/12) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (autorisé), `SELLER` (refusé) |
| Données de départ | un administrateur actif, les 3 rôles, les canaux de vente du référentiel (Boutique, Facebook, Téléphone, Livraison) |
| Tickets à avoir déployés avant | AUTH-01, JNL-01, SYS-05, JNL-02 |

## Scénarios issus des critères d'acceptation

| # | Critère | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Le rôle choisi détermine la coquille de l'utilisateur | Quand l'administrateur crée un compte `SELLER` puis qu'il se connecte | menu et permissions de la vendeuse | `AuthSecurityIT.me_withSellerToken_returnsOnlyTheSellerPermissions` ; `menu.spec.ts` | ☐ |
| 2 | Un formulaire permet de saisir / modifier les champs | Quand l'administrateur remplit email, mot de passe, prénom, nom, rôle | compte créé, 201 | `UserServiceImplTest.createUser_validRequest_recordsCreationInJournal` | ☐ |
| 2b | idem (modification) | Quand il modifie un compte existant | formulaire pré-rempli, champs mis à jour, `UPDATE` tracé | _(à ajouter : aucun test de `updateUser` repéré côté service ni côté interface)_ | ☐ |
| 3 | Canal de vente et taux de commission | Quand le rôle est vendeuse avec canal et taux | `SellerTerms` enregistrés | `UserServiceImplTest.createUser_sellerWithChannelAndCommission_savesSellerTerms` | ☐ |
| 3b | idem | Quand la vendeuse n'a pas de commission | refus 422 | `UserServiceImplTest.createUser_sellerWithoutCommission_throwsBusinessException` | ☐ |
| 3c | idem | Quand un administrateur reçoit une commission | refus 422 | `UserServiceImplTest.createUser_administratorWithCommission_throwsBusinessException` | ☐ |
| 3d | idem | Quand le canal n'existe pas ou est inactif | refus 422 | `UserServiceImplTest.createUser_unknownSalesChannel_throwsBusinessException` | ☐ |
| 4 | Rôles proposés = Administrateur, Vendeuse, Livreur | Quand le formulaire charge les rôles | exactement trois rôles | `AdminAccountInitializerIT.migrations_freshDatabase_seedExactlyTheThreeBusinessRoles` ; `user-service.spec.ts` « should return the business roles from the first page » | ☐ |
| 5 | Création / modification réservées à l'admin, contrôlé côté serveur | Étant donné une vendeuse · Quand elle appelle `POST /api/v1/users` | 403 `FORBIDDEN` ; sans jeton 401 | `AuthSecurityIT.getUsers_withNonAdminRole_returns403`, `getUsers_withoutToken_returns401` _(à ajouter : le même contrôle sur `POST` et `PUT`)_ | ☐ |
| 6 | Un utilisateur ne se supprime jamais, il se désactive | Quand on ouvre la liste | l'action principale est la désactivation | voir AUTH-04 ; _(à clarifier : `DELETE /api/v1/users/{id}` existe)_ | ☐ |

## Sécurité et erreurs

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Email déjà utilisé (création) | 409 `CONFLICT`, rien dans le journal | `UserServiceImplTest.createUser_emailAlreadyUsed_throwsConflict`, `createUser_emailAlreadyUsed_recordsNothingInJournal` | ☐ |
| Email déjà utilisé par un autre compte (modification) | 409 | _(à ajouter)_ | ☐ |
| Champ `role`, `id` dans le corps | non pris en compte (le rôle passe uniquement par `roleId`) | ArchUnit `input_dtos_have_no_id_or_role` ; `UserStatusIT.setActive_massAssignmentOfRole_isIgnored` (statut) | ☐ |
| Données invalides (email mal formé, mot de passe < 8, taux > 100 ou > 2 décimales) | 422 `VALIDATION_FAILED` | _(à ajouter)_ | ☐ |
| Utilisateur introuvable | 404 `NOT_FOUND` | `UserServiceImplTest.getUserById_unknownId_throwsNotFound` | ☐ |
| Création tracée dans le journal | une ligne `CREATE` nominative et horodatée | `UserServiceImplTest.createUser_validRequest_recordsCreationInJournal` ; `JournalSecurityIT.createUser_byAdmin_leavesANominativeTimestampedLineInTheJournal` | ☐ |
| Création rejouée avec la même `Idempotency-Key` (file hors ligne) | un seul compte | `IdempotencyIT.createUser_replayedWithSameKey_returnsSameResponseWithoutDuplicate` | ☐ |
| Pagination et recherche | `page` base 1, `size` ≤ 100, tri en liste blanche | `user-service.spec.ts` « should request a page with the VVS pagination parameters and skip an empty search » | ☐ |

## Cas limites et hors ligne

- Taux de commission : `0`, `100`, `100.01`, `12.345`, valeur négative.
- Email avec majuscules et espaces : normalisé avant le contrôle d'unicité.
- Changement de rôle d'une vendeuse vers livreur : canal et commission doivent être renvoyés à `null`.
- Création de compte depuis la file hors ligne : voir SYS-02 et SYS-07.

## Tests existants (repérés dans le code)

- Backend : `feature/user/service/impl/UserServiceImplTest.java`, `feature/user/controller/UserStatusIT.java`, `feature/user/config/AdminAccountInitializerIT.java`, `feature/auth/controller/AuthSecurityIT.java`, `security/idempotency/IdempotencyIT.java`
- Frontend : `features/user/data-access/user-service.spec.ts`, `features/user/pages/user-list-page/user-list-page.spec.ts`

## À faire à la main (recette QA)

- Créer une vendeuse (canal + taux), un livreur et un administrateur ; vérifier que seul le formulaire de la vendeuse montre canal et commission.
- Se connecter avec chaque compte créé et contrôler le menu.
- Modifier le rôle d'un compte existant et vérifier le journal.
- Tenter la création avec un email déjà pris : message d'erreur serveur affiché.

## Résultat de l'exécution

- Date : _(à compléter)_
- Environnement : _(à compléter)_
- Anomalies ouvertes : _(à compléter)_
