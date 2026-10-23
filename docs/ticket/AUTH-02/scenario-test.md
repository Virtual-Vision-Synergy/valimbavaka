# Scénarios de test — AUTH-02 · Déconnexion utilisateur

Statut : ✔ rempli depuis les tests existants au 2026-10-23 — à valider par le traiteur · Ticket : [#11](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/11) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | tout rôle connecté (`ADMIN`, `SELLER`, `DELIVERY_DRIVER`) |
| Données de départ | un compte actif connecté (cookie `vvs_refresh` présent) |
| Tickets à avoir déployés avant | AUTH-01 |

## Scénarios issus des critères d'acceptation

| # | Critère | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Un bouton de déconnexion est visible en permanence | Étant donné un utilisateur connecté · Quand la coquille s'affiche | le nom, le libellé du rôle et un bouton de déconnexion sont visibles | `main-layout.spec.ts` « should display the name and the role label of the signed in user with a logout button » | ☐ |
| 2 | La déconnexion ramène au login et exige une nouvelle authentification | Quand l'utilisateur clique sur Se déconnecter | session vidée, redirection vers `/login` ; une URL protégée renvoie au login | `auth-service.spec.ts` « should clear the session on logout even if the server call fails » ; `auth-guard.spec.ts` « authGuard should redirect an anonymous visitor to the login page » | ☐ |
| 2b | idem (serveur) | Quand le cookie de la session déconnectée est rejoué sur `/refresh` | 401, aucune nouvelle session | `AuthSecurityIT.refresh_afterLogout_returns401` ; `AuthServiceImplTest.logout_knownToken_revokesItsFamily` | ☐ |
| 2c | Aucun changement de rôle sans reconnexion | Quand un autre compte se connecte après la déconnexion | le menu et les permissions sont ceux du nouveau compte uniquement | `auth-store.spec.ts` « should forget the user and the token on sign out » ; `session-interceptor.spec.ts` « should not replace a new account when an earlier refresh completes » | ☐ |

## Sécurité et erreurs

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Déconnexion sans cookie | 204 quand même, cookie effacé | _(à ajouter : aucun test dédié repéré)_ | ☐ |
| Déconnexion depuis une origine non autorisée | 403 `FORBIDDEN` | _(à ajouter : `OriginCheckFilter` testé sur `/refresh` seulement)_ | ☐ |
| Serveur injoignable pendant la déconnexion | l'interface se déconnecte quand même | `auth-service.spec.ts` « should clear the session on logout even if the server call fails » | ☐ |
| Un autre compte s'est connecté pendant l'appel de déconnexion | la nouvelle session n'est pas effacée | _(à ajouter : comportement du `finalize` de `AuthService.logout`)_ | ☐ |

## Cas limites et hors ligne

- Déconnexion avec des saisies en attente dans la file hors ligne : elles restent attribuées à leur compte et ne partent pas sous un autre (voir SYS-02 et SYS-11).
- Déconnexion de l'espace livreur : bouton du profil livreur, redirection vers `/login` (pas de test automatisé repéré).

## Tests existants (repérés dans le code)

- Backend : `feature/auth/controller/AuthSecurityIT.java` (`refresh_afterLogout_returns401`), `security/service/impl/AuthServiceImplTest.java` (`logout_knownToken_revokesItsFamily`)
- Frontend : `core/auth/auth-service.spec.ts`, `core/auth/auth-store.spec.ts`, `core/auth/auth-guard.spec.ts`, `core/layout/main-layout/main-layout.spec.ts`

## À faire à la main (recette QA)

- Se connecter, cliquer sur Se déconnecter, vérifier l'arrivée sur `/login`.
- Utiliser le bouton Précédent du navigateur : l'application protégée ne doit pas se réafficher sans connexion.
- Se connecter avec un autre rôle sur le même poste : menu du nouveau rôle uniquement.
- Livreur : se déconnecter depuis la page profil de l'espace `/round`.

## Résultat de l'exécution

- Date : _(à compléter)_
- Environnement : _(à compléter)_
- Anomalies ouvertes : _(à compléter)_
