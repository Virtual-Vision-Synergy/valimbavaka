# Scénarios de test — SYS-04 · Bascule thème clair/sombre

> Le scénario sert de base au QA (Njara) et de référence aux tickets liés.

Statut : ✅ tests automatiques verts, recette 🧪 à passer · Ticket : [#107](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/107) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests frontend Vitest sur le comportement visible. Ticket **frontend seul** : aucun endpoint,
aucune migration, aucune écriture serveur.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | tous : `ADMIN`, `SELLER` (barre supérieure), `DELIVERY_DRIVER` (page « Mon profil » livreur), et visiteur non connecté (page de connexion) |
| Données de départ | `localStorage` vide (premier passage) ou `vvs.theme` = `"light"` / `"dark"` ; préférence système clair ou sombre |
| Tickets à avoir déployés avant | AUTH-01 (connexion), AUTH-02 (barre supérieure) |

## Scénarios issus des critères d'acceptation

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Le changement de thème s'applique immédiatement à toute l'interface | vendeuse connectée, thème clair, clic sur l'icône lune de la barre supérieure | la classe `dark` est posée sur `<html>` dans le même cycle : tous les jetons CSS (`html.dark` de `dark-theme.css`) basculent sans rechargement ; le bouton affiche le soleil, `aria-pressed="true"`, libellé « Passer au thème clair » | `main-layout-theme.spec.ts` › `should switch the whole interface to dark immediately on click` | ✅ auto |
| 1b | Retour au thème clair | second clic sur le bouton | `dark` retiré de `<html>`, libellé « Passer au thème sombre » | `main-layout-theme.spec.ts` › `should come back to light on a second click` ; `ui-preferences-service.spec.ts` › `should switch back to light on a second toggle` | ✅ auto |
| 1c | État initial du bouton | `vvs.theme` = `"light"` | bouton « Passer au thème sombre », `aria-pressed="false"` | `main-layout-theme.spec.ts` › `should offer the dark theme while the interface is light` | ✅ auto |
| 1d | Le choix est mémorisé | bascule vers sombre | `localStorage["vvs.theme"]` = `"dark"`, réappliqué au prochain chargement | `ui-preferences-service.spec.ts` › `should switch the whole interface to dark at once and remember the choice` | ✅ auto |
| 1e | Le choix s'applique aussi aux autres onglets ouverts | un autre onglet écrit `vvs.theme` = `"dark"` (événement `storage`) | l'onglet courant passe en sombre sans réécrire la préférence | `ui-preferences-service.spec.ts` › `should apply a theme chosen in another tab without writing it again` | ✅ auto |

## Sécurité et erreurs

Sans objet : aucun appel serveur. La préférence n'est pas une donnée sensible ; son stockage dans `localStorage`
est explicitement autorisé (§9 de la convention, exception commentée dans `ui-preferences-service.ts`).

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Premier passage, système clair | `localStorage` vide, `prefers-color-scheme: light` | thème clair | `ui-preferences-service.spec.ts` › `should start in light theme when nothing is stored and the system is light` | ✅ auto |
| Premier passage, système sombre | `localStorage` vide, `prefers-color-scheme: dark` | thème sombre dès le démarrage | `ui-preferences-service.spec.ts` › `should follow the dark system preference when nothing is stored` | ✅ auto |
| Choix mémorisé contraire au système | `vvs.theme` = `"light"`, système sombre | le choix de l'utilisateur gagne : clair | `ui-preferences-service.spec.ts` › `should prefer the remembered choice over the system preference` | ✅ auto |
| Valeur mémorisée invalide | `vvs.theme` = `"blue"` | ignorée, retour à la préférence système | `ui-preferences-service.spec.ts` › `should ignore an unsupported remembered value and fall back to the system` | ✅ auto |
| Stockage indisponible | `setItem` lève `QuotaExceededError` (navigation privée) | la bascule s'applique quand même, pour la session | `ui-preferences-service.spec.ts` › `should still switch the theme when the browser storage is unavailable` | ✅ auto |
| Autre onglet : autre clé ou valeur invalide | `vvs.lang` = `"dark"`, `vvs.theme` = `"blue"`, `vvs.theme` supprimé (`null`) | thème inchangé | `ui-preferences-service.spec.ts` › `should ignore another tab writing another key or an unsupported theme` | ✅ auto |
| Fin de l'application | injecteur racine détruit puis événement `storage` | plus d'écouteur (pas de fuite) | `ui-preferences-service.spec.ts` › `should stop following other tabs once the application is destroyed` | ✅ auto |
| Hors ligne | réseau coupé | sans effet : aucune requête, la bascule reste instantanée | 🔍 code : `UiPreferencesService.toggleTheme` n'appelle aucun service HTTP | 🔍 code |
| Changement de compte | vendeuse se déconnecte, l'administrateur se connecte sur le même poste | le thème est une préférence du navigateur, pas du compte : il est conservé | 🔍 code : clé `vvs.theme` non effacée à la déconnexion | 🔍 code |

## Tests existants

- `valimbavaka-ui/src/app/core/layout/ui-preferences-service.spec.ts` (10 tests, service)
- `valimbavaka-ui/src/app/core/layout/main-layout/main-layout-theme.spec.ts` (3 tests, bouton de la barre supérieure)
- Accessibilité des pages (bouton nommé) : `valimbavaka-ui/src/app/features/demo-pages.a11y.spec.ts`,
  `valimbavaka-ui/src/app/features/auth/pages/login-page/login-page.a11y.spec.ts` (inchangés)

## Recette manuelle (🧪)

| # | Étape | Résultat attendu | Statut |
|---|---|---|---|
| R1 | Se connecter en vendeuse, cliquer sur la lune de la barre supérieure, parcourir tableau de bord, articles, ventes, stocks | toutes les pages, menus, dialogues et panneaux sont sombres, lisibles, sans élément resté clair (hors aperçu de facture, voulu blanc comme le papier) | 🧪 |
| R2 | Ouvrir deux onglets, basculer le thème dans le premier | le second bascule aussi, sans rechargement | 🧪 |
| R3 | Recharger la page après avoir choisi sombre | la page revient en sombre | 🧪 |
| R4 | Page de connexion (non connecté) et « Mon profil » livreur sur mobile | le bouton de thème y est présent et bascule l'écran | 🧪 |
| R5 | Contrastes en thème sombre (outil du navigateur ou Lighthouse) | aucun texte sous 4,5:1 | 🧪 |

## Résultat de l'exécution

- Date : 07/11/2026
- Environnement : CI GitHub (workflow `CI`, run 42413146495, commit `04bf154`) : job Frontend vert, 54 fichiers et 315 tests passés, dont les 13 de SYS-04 (`ui-preferences-service.spec.ts` 10, `main-layout-theme.spec.ts` 3) ; job Backend vert (non touché)
- Anomalies ouvertes : aucune connue
- Étapes 🧪 restantes : 5
