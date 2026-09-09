# Charte graphique — Valimbavaka Cosmetics

> Extraite de l'ancienne UI (Vue.js, `Valimbavaka-cosmetics/ui`) le 2026-09-09, en vue du refonte Angular (`valimbavaka-ui`). Ce document est la **référence visuelle** du produit : elle survit au changement de stack. Le code (Vue) est jeté, la charte, elle, est reprise.
>
> - Aperçu visuel interactif : [`style-preview.html`](./style-preview.html) — à ouvrir dans un navigateur.
> - Tokens CSS portés en Angular : `valimbavaka-ui/src/styles/tokens.css` + `dark-theme.css` (voir § [Portage Angular](#portage-angular)).
> - Logos sources : [`assets/logo/`](./assets/logo/).

## 1. Logo

4 variantes disponibles dans [`assets/logo/`](./assets/logo/) :

| Fichier | Usage |
|---|---|
| `logo.png` | Logo plein, fond clair |
| `logo-min.png` | Version compacte (favicon, sidebar réduite, avatar) |
| `logo-negatif.png` | Fond sombre / dark mode |
| `logo-negatif-min.png` | Version compacte, fond sombre |

Le logo porte lui-même la palette de marque : orange (nom "Valimbavaka"), teal (feuille & arcs), brun (mot "COSMETICS"). Toute la charte ci-dessous en découle.

## 2. Couleurs

### 2.1 Primaire — Orange (identité "Valimbavaka")

| Token | Hex | Usage |
|---|---|---|
| `--primary` | `#F26716` | Couleur d'action principale (CTA, liens actifs, accents) |
| `--primary-dark` | `#F25116` | Hover / état pressé |
| `--primary-light` | `#F9A86B` | Accents secondaires, icônes sur fond clair |
| `--primary-xlight` | `#FEF0E6` | Fonds teintés (badges, icônes de stat-card) |

### 2.2 Secondaire — Teal (feuille & arcs du logo)

| Token | Hex | Usage |
|---|---|---|
| `--secondary` | `#06BFAD` | Focus ring, accents interactifs, liens hover |
| `--secondary-dark` | `#04BF9D` | Texte sur fond `--secondary-xlight` |
| `--secondary-light` | `#7DDDD6` | Scrollbar, bordures actives |
| `--secondary-xlight` | `#E0F7F5` | Fonds teintés (hover de ligne de tableau, badges) |

### 2.3 Brun (mot "COSMETICS" du logo)

| Token | Hex | Usage |
|---|---|---|
| `--brun` | `#583426` | Accent de marque ponctuel |
| `--brun-light` | `#8A5A4A` | Variante plus claire |

### 2.4 Neutres

| Token | Hex | Usage |
|---|---|---|
| `--fond` | `#F2F2F2` | Fond de page |
| `--fond-dark` | `#E5E5E5` | Fonds hover, séparateurs |
| `--gris-fond` | `#F8F8F8` | Fond d'en-tête de tableau |
| `--gris-anthracite` | `#2C3035` | Texte principal |
| `--gris-moyen` | `#7F8C8D` | Texte secondaire, labels |
| `--gris-clair` | `#B0BEC0` | Bordures de champs/boutons |
| `--blanc` | `#FFFFFF` | Surfaces (cards, modals, header) |
| `--noir` | `#1A1C1E` | Texte fort ponctuel |

### 2.5 Sémantique

| Rôle | Base | Light (fond badge) | Dark (texte sur fond light) |
|---|---|---|---|
| Succès | `--succes` `#07AE12` | `#D4F5D6` | `#057A0D` |
| Warning | `--warning` `#F1C40F` | `#FEF9C3` | `#B8950B` |
| Danger | `--danger` `#E50101` | `#FDDADA` | `#A80000` |

### 2.6 Mode sombre

Le mode sombre n'est **pas** un simple filtre : les fonds passent à des gris neutres à 3 niveaux d'élévation (page `#1A1A1A` → surface `#2E2E2E` → survol `#242424`), le texte devient chaud/clair, et primaire/secondaire restent identiques (ils ressortent naturellement sur fond sombre). Le détail complet est dans `dark-theme.css`. Activation historique : classe `.dark` sur `<html>`.

## 3. Typographie

- **Police unique** : [Inter](https://fonts.google.com/specimen/Inter) (Google Fonts), graisses 300 à 700, pour titres (`--font-display`) et corps de texte (`--font-body`) — pas de police secondaire.
- **Titres** : `letter-spacing: -0.02em`, tailles fluides via `clamp()` :
  - `h1` → `clamp(24px, 3vw, 32px)`, weight 700
  - `h2` → `clamp(20px, 2.5vw, 26px)`, weight 600
  - `h3` → `clamp(16px, 2vw, 20px)`, weight 600
- **Corps de texte** : 14px, `line-height: 1.6–1.7`, couleur `--gris-moyen` pour le texte secondaire (`<p>`), `--gris-anthracite` pour le texte principal.

## 4. Espacements (scale 4px)

`--space-1` à `--space-16` : `4 · 8 · 12 · 16 · 20 · 24 · 32 · 40 · 48 · 64` px.

## 5. Rayons de bordure

`--radius-sm` 2px · `--radius-md` 4px · `--radius-lg` 6px · `--radius-xl` 8px · `--radius-full` 9999px (pills, badges, avatars).

## 6. Ombres & élévation

- `--shadow-sm/md/lg` : ombres neutres teintées `--gris-anthracite` (montée en intensité au hover des cards).
- `--shadow-glow-primary` / `--shadow-glow-secondary` : halo coloré (30% d'opacité) utilisé au hover des boutons primaires et des éléments actifs — signature visuelle assez marquée de l'ancienne UI, à reprendre ou assouplir selon le goût du nouveau design.

## 7. Mouvement

- `--transition-fast` 150ms · `--transition-base` 250ms · `--transition-slow` 400ms `cubic-bezier(0.4,0,0.2,1)`.
- Micro-interactions systématiques : `translateY(-1px)` au hover des boutons/cards, effet "ripple" au clic des boutons, `scale(0.97)` à l'appui.

## 8. Layout

`--sidebar-width` 260px · `--sidebar-collapsed` 68px · `--header-height` 70px — layout classique sidebar + header, format back-office.

## 9. Langage de composants (référence, non porté tel quel)

L'ancienne UI définissait des classes CSS globales (`.btn`, `.card`, `.stat-card`, `.badge`, `.table`, `.modal`, `.toast`, `.form-input`, `.pagination`…) dans `components.css`. **Ces classes ne sont pas portées dans Angular** : la nouvelle convention (Feature-Based Layering, composants standalone) veut que chaque composant porte son propre `.css` scoped. Ce fichier reste la référence de comportement/style à reproduire composant par composant :

- **Boutons** : `primary` (fond `--primary`, glow au hover), `secondary` (bordure `--gris-clair`, devient teal au hover), `danger`, `ghost` (transparent, bordure teal). Tailles `sm/md/lg`.
- **Cards** : fond `--blanc`, `--radius-lg`, `--shadow-sm` → `--shadow-md` au hover, légère translation verticale.
- **Stat-cards** : icône carrée 36–40px sur fond `-xlight` de la couleur de variante, liseré coloré de 2px en bas au hover (variantes `green/sage/gold/danger/mint`).
- **Badges** : pill (`--radius-full`), fond `-light` + texte `-dark` de la couleur sémantique, tout en majuscules, `letter-spacing: 0.03em`.
- **Tableaux** : en-tête en majuscules sur `--gris-fond`, ligne survolée en `--secondary-xlight`.
- **Formulaires** : bordure `--gris-clair` → `--secondary` au focus avec halo `box-shadow` teal.
- **Modals/Toasts** : `--radius-xl`, `--shadow-lg`, apparition en `fadeInUp`/`slideInLeft`.
- **Tags "gamme"** (Zezika / Botanika / Solika) : chaque gamme produit a sa propre couleur d'accent — Zezika en rose (`#C2466A` sur `#FFF0F5`, hors palette principale), Botanika en teal (couleur secondaire), Solika en doré (couleur warning). À reconfirmer si ces 3 gammes existent toujours dans le nouveau produit.

## 10. Ce qui n'a **pas** été repris du fichier source

L'ancien `tokens.css` contenait une section "aliases de rétrocompatibilité" (`--vert-sauge`, `--beige-lin`, `--or-moutarde`, `--vert-menthe`, `--rouge-terre`…) — des noms de variables issus d'une itération antérieure de la charte, gardés uniquement pour ne pas casser du CSS existant pendant le vibe-coding. Ce sont des noms morts : **ils ne sont pas repris** dans le portage Angular. Si un ancien composant y fait encore référence en le regardant comme doc, mapper directement vers le token actuel correspondant (ex. `--vert-sauge` → `--secondary`).

## Portage Angular

Les tokens (couleurs, typo, espacements, rayons, ombres, transitions, layout) vivent maintenant dans :

```
valimbavaka-ui/src/styles/
├── tokens.css       // :root — palette claire + échelle typo/espacement/radius/ombre
├── dark-theme.css   // html.dark — overrides du mode sombre
└── base.css         // reset léger + typographie globale (body, h1-h3, p, scrollbar, focus-visible)
```

Importés dans cet ordre par `valimbavaka-ui/src/styles.css` (seul fichier déclaré dans `angular.json` → `styles`). La police Inter est chargée via `<link>` dans `src/index.html` (plutôt que `@import` en CSS, pour la performance de chargement).

Les logos sont dans `valimbavaka-ui/public/logo/` (servis tels quels par Angular, référençables en `/logo/logo.png`).

Pas encore porté, à faire au fil de la reconstruction des composants : la bibliothèque de classes de composants (`components.css` de l'ancienne UI, cf. §9) et les animations (`animations.css` : `fadeIn`, `fadeInUp`, `slideInLeft`, `shimmer`/skeleton, `spin`).
