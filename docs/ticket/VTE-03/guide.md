# Guide d'intégration — VTE-03 · Raccourcis clavier de caisse

Statut : ✅ écrit le 2026-11-11 · Ticket : [#64](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/64) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Le poste de caisse `/pos` se pilote au clavier : `F2` recherche, `+` / `−` quantité du dernier article, `F6` code
promo, `F8` mise en attente, `F12` encaissement. La maquette VTE-02 les branchait déjà ; ce ticket les **fiabilise** :
table des touches isolée et testée, combinaisons Ctrl/Alt/⌘ laissées au navigateur, pas de répétition de `F12`/`F8`
touche maintenue, aucune réaction dans une fenêtre modale, `+` sur ticket vide qui ouvre la recherche. Ticket
**frontend seul** : aucun endpoint, aucune permission, aucune migration.

## Contrat exposé

### Endpoints backend

Aucun. `F12` appelle `PosPage.checkout()`, donc `POST /api/v1/sales` (`sales.create`) de VTE-01 (voir son guide).

### Services et ports Java réutilisables

- Aucun.

### Frontend (`valimbavaka-ui/src/app/features/pos/`)

- `data-access/pos-shortcuts.ts` (nouveau) : `shortcutAction(event)` renvoie `'search' | 'promo' | 'hold' | 'checkout' |
  'increase' | 'decrease'` ou `undefined` (touche laissée au navigateur) ; type `ShortcutAction`.
- `pages/pos-page/pos-page.ts` : `onKeydown` (écoute `document:keydown`) appelle `shortcutAction`, puis `run`
  exécute l'action (mêmes méthodes que les boutons : `checkout()`, `hold()`, `PosCart.add/decrement`,
  `PosCatalog.focusSearch()`).
- Aide visuelle inchangée : liste `pos.shortcuts.*` sous le catalogue, `<kbd>` et `aria-keyshortcuts` sur « Encaisser »
  et « Mettre en attente » (`components/pos-ticket/pos-ticket.html`), indication `F2` du champ de recherche.

### Données

- Aucune table, migration ni écriture au journal propre à ce ticket.

## Règles métier à respecter par les autres tickets

- **Un raccourci = le bouton correspondant** : il passe par la même méthode de la page, donc par les mêmes gardes
  (ticket non vide, vendeuse choisie, règlement valide, `submitting`). Ne pas coder de logique dans le raccourci.
- **Ajouter une touche** : l'ajouter dans `FUNCTION_KEYS` ou `QUANTITY_KEYS` de `pos-shortcuts.ts`, dans `ShortcutAction`,
  dans `run()` de `PosPage`, et dans la liste affichée (`pos.shortcuts.*`, fr et mg).
- **VTE-04 (code promo)** : `F6` appelle aujourd'hui `demoAction()` ; VTE-04 remplace ce handler par l'ouverture du champ.
- **VTE-05 (règlement)** et **VTE-06 (mise en attente)** : `F12` et `F8` appellent `checkout()` et `hold()` ; modifier
  ces méthodes suffit, la touche suit.
- `F2`/`F6`/`F8`/`F12` marchent aussi dans un champ ; `+` et `−` y restent du texte.

## Fichiers concernés

**Créés** : `features/pos/data-access/pos-shortcuts.ts`, `pos-shortcuts.spec.ts`.
**Modifiés** : `features/pos/pages/pos-page/pos-page.ts`, `pos-page.spec.ts` (7 tests).
Backend : aucun fichier.

## Dépendances

**Ce ticket s'appuie sur** :
- [VTE-02](../VTE-02/description.md) · [guide](../VTE-02/guide.md) : `PosCart.add` / `decrement` / `lastArticle`, `PosPage`

**Ces tickets s'appuient sur lui** :
- _(rien)_

## Comment l'utiliser depuis un autre ticket

```ts
const action = shortcutAction(event); // 'checkout' pour F12, undefined si la touche n'est pas pour la caisse
```

## Points d'attention

- **Hypothèse (« ajout d'article »)** : `+` ajoute une unité du **dernier article** du ticket ; sur ticket vide il ouvre la
  recherche (`F2`), où `Entrée` ajoute un code-barres exact ou un résultat unique (VTE-02). Pas de raccourci par numéro
  d'article : à créer si le métier le demande.
- **Hypothèse (touches)** : `F2`, `F6`, `F8`, `F12`, `+`, `−` sont les touches « définies » de la maquette VTE-02 ; aucun
  réglage par utilisateur.
- `F12` ouvre les outils de développement de certains navigateurs : `preventDefault` l'empêche, mais à vérifier en
  recette (R2).
- Le test `search-dialog.spec.ts` est connu comme instable en CI (hors périmètre).
