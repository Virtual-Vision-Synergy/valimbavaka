# Guide d'intégration — SYS-04 · Bascule thème clair/sombre

Statut : ✅ rédigé · Ticket : [#107](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/107) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Le thème (clair ou sombre) est une préférence d'affichage du navigateur, gérée par `UiPreferencesService`. La bascule
pose ou retire la classe `dark` sur `<html>` : tous les jetons CSS redéfinis sous `html.dark` changent d'un coup, sans
rechargement, sur toute l'interface et dans les autres onglets ouverts. Sans choix mémorisé, le thème suit le système.

## Contrat exposé

### Endpoints backend

Aucun : ticket frontend seul.

### Services et ports Java réutilisables

Aucun.

### Frontend

- Routes : aucune nouvelle route.
- Services : `UiPreferencesService` (`valimbavaka-ui/src/app/core/layout/ui-preferences-service.ts:20`, `providedIn: 'root'`,
  instancié au démarrage par `provideAppInitializer` dans `valimbavaka-ui/src/app/app.config.ts:36`) :
  - `theme: Signal<'light' | 'dark'>` (`:27`) : thème courant, en lecture seule ;
  - `toggleTheme(): void` (`:35`) : bascule, applique et mémorise (`localStorage["vvs.theme"]`) ;
  - type exporté `Theme`.
  - (même service, hors SYS-04 : `sidebarCollapsed` / `toggleSidebar()` pour le menu replié.)
- Boutons de bascule existants (aucun composant partagé dédié) :
  - barre supérieure : `valimbavaka-ui/src/app/core/layout/top-bar/top-bar.html:46` (lune/soleil, `aria-pressed`) ;
  - page de connexion : `valimbavaka-ui/src/app/features/auth/pages/login-page/login-page.html:30` ;
  - « Mon profil » livreur : `valimbavaka-ui/src/app/features/deliveries/pages/driver-profile-page/driver-profile-page.html:15`.
- Libellés : `layout.lightTheme` / `layout.darkTheme` dans `valimbavaka-ui/public/i18n/fr.json:98` (et `mg.json`).
- Styles : jetons clairs dans `valimbavaka-ui/src/styles/tokens.css`, surcharges sombres sous `html.dark` dans
  `valimbavaka-ui/src/styles/dark-theme.css:13`.

### Données

- Tables et migrations Liquibase : aucune.
- Journal : rien n'est tracé (préférence locale, aucune donnée métier).

## Règles métier à respecter par les autres tickets

- **Ne jamais coder une couleur en dur** dans un composant : utiliser les variables CSS (`var(--fond)`, `var(--blanc)`,
  `var(--gris-anthracite)`, `var(--primary-strong)`…). Une couleur en dur ne basculera pas en sombre.
  Exceptions voulues : texte blanc sur un fond de marque (bandeaux orange/teal) et l'aperçu de facture
  (`features/sales/components/invoice-document/invoice-document.css`), blanc comme le papier.
- Nouveau jeton de couleur : le déclarer dans `tokens.css` **et** sa valeur sombre dans `dark-theme.css`.
- Ne pas lire `localStorage["vvs.theme"]` directement ni poser la classe `dark` ailleurs : passer par le service.
- Priorité : choix mémorisé > préférence système (`prefers-color-scheme`) > clair.

## Fichiers concernés

- Frontend :
  - `valimbavaka-ui/src/app/core/layout/ui-preferences-service.ts` (service ; ajout de la synchronisation entre onglets)
  - `valimbavaka-ui/src/styles/dark-theme.css` (commentaire d'en-tête corrigé : nom du service)
- Tests :
  - `valimbavaka-ui/src/app/core/layout/ui-preferences-service.spec.ts`
  - `valimbavaka-ui/src/app/core/layout/main-layout/main-layout-theme.spec.ts`
- Backend : aucun.

## Dépendances

**Ce ticket s'appuie sur** :
- [AUTH-01](../AUTH-01/description.md) · [guide](../AUTH-01/guide.md) (page de connexion, qui porte déjà un bouton de thème)

**Ces tickets s'appuient sur lui** :
- _(rien)_

## Comment l'utiliser depuis un autre ticket

```ts
protected readonly preferences = inject(UiPreferencesService);
// template : <button type="button" (click)="preferences.toggleTheme()">…{{ preferences.theme() }}…</button>
```

## Points d'attention

- Hypothèse (pas de décision PO nécessaire) : la préférence est **par navigateur**, pas par compte. Elle n'est pas
  envoyée au serveur et survit à la déconnexion et au changement d'utilisateur sur le même poste. Un thème stocké
  côté compte demanderait un champ utilisateur et un endpoint : hors périmètre (1 SP).
- Ajout de la synchronisation entre onglets (événement `storage`) pour que la bascule vaille bien pour « toute
  l'interface » quand plusieurs onglets sont ouverts.
- Sans choix explicite, le thème est figé au démarrage selon le système ; un changement du thème système pendant
  la session n'est pas suivi en direct (rechargement nécessaire). Amélioration possible, non demandée.
- Le bouton de la barre supérieure combine `aria-pressed` et un libellé qui change (« Passer au thème clair/sombre ») :
  hérité d'AUTH-02, non modifié. À trancher en recette accessibilité si besoin (garder l'un ou l'autre).
- Couleurs en dur restantes dans quelques composants (texte blanc sur bandeaux de marque, voiles de dialogue
  `rgb(15 23 42 / 45%)`) : vérifiées, lisibles dans les deux thèmes ; à confirmer en recette R1.
