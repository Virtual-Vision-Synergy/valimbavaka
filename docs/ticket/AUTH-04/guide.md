# Guide d'intégration — AUTH-04 · Activation / désactivation d'un utilisateur

Statut : ✅ complet au 2026-10-23 — vérifié sur le code, les tests (CI + exécution locale) et la base · Ticket : [#13](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/13) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

L'administrateur active ou désactive un compte au lieu de le supprimer. Un compte désactivé ne peut plus ouvrir de session ni renouveler la sienne. Le serveur interdit à un administrateur de désactiver son propre compte et de désactiver le dernier administrateur actif. Chaque changement d'état est tracé dans le journal.

## Contrat exposé

### Endpoint backend

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| PATCH | `/api/v1/users/{id}/status` | `users.update` | `UserStatusWDto` (`active`, obligatoire) | 200 `UserRDto` | 401, 403, 404 `NOT_FOUND`, 422 `VALIDATION_FAILED` (action interdite ou `active` absent) |

### Règles métier portées par le serveur (`UserServiceImpl#setActive`)

- Idempotent : si le compte est déjà dans l'état demandé, la réponse est identique, rien n'est modifié et rien n'est tracé.
- Désactivation interdite (422) : sur son propre compte (« Vous ne pouvez pas désactiver votre propre compte. ») et sur le dernier administrateur actif (« Le dernier administrateur actif ne peut pas être désactivé. »). Le décompte utilise `UserRepository#countByRoleNameAndActiveTrue(ADMIN)`.
- Activation : aucun garde-fou particulier.
- Journal : `UPDATE` sur la ressource `USER`, description « Activation de l'utilisateur … » ou « Désactivation de l'utilisateur … ».
- Effet sur l'authentification : un compte désactivé ne peut plus se connecter (même avec le bon mot de passe), et son `/refresh` échoue en révoquant la session ([AUTH-01](../AUTH-01/guide.md)). Le jeton d'accès déjà émis reste valable jusqu'à son expiration (15 min).

### Services et ports Java

- `feature.user.service.UserService#setActive(UUID userId, boolean active)` ; `User#activate()` / `User#deactivate()`.
- `UserRDto.active` indique l'état ; le port `AuthenticatedUserLoader` (implémenté par `feature.user`) expose le drapeau `enabled` que `AuthServiceImpl` contrôle à la connexion, au refresh et sur `/me`.

### Frontend (`features/user`)

- `data-access/user-service.ts#setActive(id, active)` appelle `PATCH /api/v1/users/{id}/status`.
- `pages/user-list-page` : badge ACTIF / DÉSACTIVÉ (composant `shared/components/status-badge`), bouton d'activation / désactivation avec dialogue de confirmation (`shared/components/confirm-dialog`), rechargement de la liste après succès, message du serveur affiché en cas de refus.

## Règles métier à respecter par les autres tickets

- **Listes de sélection opérationnelles** (vendeuse d'une vente, livreur d'une livraison, etc.) : ne proposer que des comptes `active = true` du bon rôle. `GET /api/v1/users` ne filtre pas encore sur l'état ni sur le rôle : le ticket qui crée la liste de sélection ajoute le filtre côté serveur.
- Les données historiques (ventes, livraisons, journal) gardent l'UUID de l'utilisateur désactivé ; ne jamais les supprimer ni les réattribuer.
- Une ressource liée à un utilisateur désactivé reste lisible ; seule la création de nouvelles opérations à son nom est refusée.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/user/`)
- `controller/UserController.java` (méthode `setActive`), `dto/UserStatusWDto.java`
- `service/UserService.java`, `service/impl/UserServiceImpl.java` (`setActive`, `ensureCanDeactivate`)
- `domain/User.java` (`activate`, `deactivate`), `repository/UserRepository.java` (`countByRoleNameAndActiveTrue`)
- `service/impl/AuthenticatedUserLoaderImpl.java`, `valimbavaka-core/src/main/java/org/vvs/valimbavaka/security/service/impl/AuthServiceImpl.java` (refus des comptes inactifs)

**Frontend** (`valimbavaka-ui/src/app/`)
- `features/user/data-access/user-service.ts`, `features/user/pages/user-list-page/*`
- `shared/components/status-badge/*`, `shared/components/confirm-dialog/*`

**Tests**
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/user/controller/UserStatusIT.java`
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/user/service/impl/UserServiceImplTest.java` (`setActive_*`)
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/security/service/impl/AuthServiceImplTest.java` (compte désactivé)
- `valimbavaka-ui/src/app/features/user/pages/user-list-page/user-list-page.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** :
- [AUTH-03](../AUTH-03/guide.md) (comptes), [JNL-01](../JNL-01/guide.md) (journal)

**Ces tickets s'appuient sur lui** : aucun ticket ne le référence explicitement, mais tout sélecteur d'utilisateur ([VTE-01](../VTE-01/description.md), [LIV-01](../LIV-01/description.md), [COM-03](../COM-03/description.md)) doit respecter l'état actif.

## Comment l'utiliser depuis un autre ticket

```ts
this.userService.setActive(user.id, false).subscribe({
  next: () => this.reload(),
  error: (error) => this.showServerMessage(error), // 422 : propre compte ou dernier administrateur
});
```

## Points d'attention (écarts constatés dans le code)

- **Le garde-fou « dernier administrateur » ne couvre que `PATCH /status`.** `PUT /api/v1/users/{id}` accepte `active` et `roleId` et appelle directement `User#update` : on peut ainsi désactiver, ou retirer le rôle `ADMIN` du dernier administrateur actif (ou de son propre compte) sans passer par `ensureCanDeactivate`. Le critère 4 du ticket (« ni désactivé ni privé de son rôle ») n'est donc garanti que pour la désactivation par `PATCH`.
- `DELETE /api/v1/users/{id}` n'a aucun de ces garde-fous (ni propre compte, ni dernier administrateur) et supprime réellement la ligne.
- Critère 2 du ticket (utilisateurs désactivés absents des listes de sélection opérationnelles) : aucune liste de ce type n'existe encore ; `GET /api/v1/users` renvoie tous les comptes, actifs ou non, ce qui est voulu pour l'écran d'administration.
- Le jeton d'accès émis avant la désactivation reste utilisable jusqu'à son expiration.
