# Scénarios de test — VTE-03 · Raccourcis clavier de caisse

Statut : ✅ rédigé le 2026-11-11 · Ticket : [#64](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/64) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests frontend Vitest sur le comportement visible. Ticket **frontend seul** : aucun endpoint,
aucune permission, aucune migration ; l'encaissement passe par `POST /api/v1/sales` de VTE-01 (sécurité couverte par
le [scénario VTE-01](../VTE-01/scenario-test.md)).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | Vendeuse (`SELLER`, permission `sales.create`) |
| Données de départ | `/pos` ouvert ; article « Savon noir 100 g », 8 500 Ar, stock 10 ; vendeuse Miora Rakoto ; règlement MVola |
| Tickets à avoir déployés avant | VTE-01 (enregistrement), VTE-02 (poste de caisse et calcul) |

## Scénarios issus des critères d'acceptation

| # | Critère d'acceptation | Exemple (étant donné / quand / alors) | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Les touches définies déclenchent encaissement… | 1 Savon noir au ticket, MVola ; touche `F12` | la vente est enregistrée une fois (comme le bouton « Encaisser »), le ticket est remis à zéro | `pos-page.spec.ts` « F12 encaisse la vente comme le bouton » | ✅ auto |
| 1b | … recherche… | `F2` depuis n'importe où dans la page | le champ de recherche reçoit le focus ; la touche n'ouvre pas autre chose (`preventDefault`) | `pos-page.spec.ts` « F2 donne le focus au champ de recherche » | ✅ auto |
| 1c | … et ajout d'article | Savon noir au ticket (quantité 1) ; touche `+` | quantité 2, net 17 000 Ar ; `−` deux fois retire la ligne | `pos-page.spec.ts` « + ajoute une unité du dernier article et − la retire » | ✅ auto |
| 1d | … et ajout d'article (ticket vide) | ticket vide ; touche `+` | aucune ligne ajoutée, le focus va à la recherche pour choisir l'article | `pos-page.spec.ts` « + sur un ticket vide ouvre la recherche pour ajouter un article » | ✅ auto |
| 1e | Table des touches | `F2` `F6` `F8` `F12` `+` `-` `−` (U+2212) | search, promo, hold, checkout, increase, decrease, decrease | `pos-shortcuts.spec.ts` « associe la touche %s à l'action %s » (7 cas) | ✅ auto |

## Sécurité et erreurs

Aucun endpoint nouveau : pas de 401, 403, 404, 409 ni 422 propres à ce ticket (le serveur reste seul juge à
l'encaissement ; le raccourci n'ajoute aucun droit, il appelle la même méthode que le bouton).

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Encaisser un ticket vide | `F12` sans article | rien n'est envoyé, toast d'avertissement « impossible d'encaisser » | `pos-page.spec.ts` « F12 sur un ticket vide n'enregistre rien et prévient la caissière » | ✅ auto |
| Touche non définie | `F5`, `a` | ignorée, comportement du navigateur conservé | `pos-shortcuts.spec.ts` « ignore les touches non définies » | ✅ auto |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Touche maintenue | `F12` puis `F12` avec `repeat` | une seule vente enregistrée | `pos-page.spec.ts` « F12 maintenu enfoncé n'encaisse qu'une fois » ; `pos-shortcuts.spec.ts` « ne répète pas encaisser ni mettre en attente quand la touche reste enfoncée » | ✅ auto |
| `+` maintenu | `+` avec `repeat` | reste actif (ajouts successifs, bornés par le stock de VTE-02) | `pos-shortcuts.spec.ts` (même test) | ✅ auto |
| Zoom du navigateur | `Ctrl` + `-`, `⌘` + `+`, `Alt` + `F12` | laissés au navigateur : ticket inchangé, pas de `preventDefault` | `pos-page.spec.ts` « Ctrl − reste le zoom du navigateur : le ticket ne change pas » ; `pos-shortcuts.spec.ts` « laisse au navigateur les combinaisons Ctrl, Alt et ⌘ » | ✅ auto |
| Saisie dans un champ | `+` ou `-` tapés dans la recherche ; `F12` dans la recherche | `+` et `-` restent du texte ; `F12` encaisse quand même | `pos-shortcuts.spec.ts` « garde + et − comme du texte dans un champ de saisie, mais F12 reste actif » | ✅ auto |
| Fenêtre modale ouverte | recherche globale (`role="dialog"`), `F12` | le poste ne réagit pas | `pos-shortcuts.spec.ts` « ne réagit pas dans une fenêtre modale (recherche globale) » | ✅ auto |
| Frappe déjà traitée | `F2` déjà `preventDefault` par un autre composant | ignorée par le poste | `pos-shortcuts.spec.ts` « respecte une frappe déjà traitée par un autre composant » | ✅ auto |
| Hors ligne | `F12` sans réseau | même parcours que le bouton : saisie en file (VTE-01), toast « Saisie enregistrée hors connexion » | ⏭ VTE-02 (`pos-page.spec.ts` « ne compare pas les montants d'une vente mise en file hors connexion ») | ⏭ VTE-02 |
| Stock | `+` au-delà du stock | refusé par `PosCart.add` (VTE-02) | ⏭ VTE-02 (`pos-cart.spec.ts` « refuse un ajout au-delà du stock… ») | ⏭ VTE-02 |

## Tests existants

- `valimbavaka-ui/src/app/features/pos/data-access/pos-shortcuts.spec.ts` (nouveau)
- `valimbavaka-ui/src/app/features/pos/pages/pos-page/pos-page.spec.ts` (7 tests ajoutés, 2 existants de VTE-02)

## Recette manuelle (🧪)

| # | Parcours | Attendu | Statut |
|---|---|---|---|
| R1 | `/pos` au clavier d'un vrai poste : `F2`, taper « savon », `Entrée`, `+`, `+`, `-`, `F12` | enchaînement sans souris ; la vente est enregistrée une fois | 🧪 |
| R2 | `F12` sous Chrome et Firefox | la vente s'enregistre et les outils de développement ne s'ouvrent pas | 🧪 |
| R3 | `Ctrl` + `-` pendant la vente | le navigateur dézoome, le ticket ne bouge pas | 🧪 |
| R4 | Clavier AZERTY / pavé numérique : touches `+` et `-` du pavé | ajoutent / retirent comme au clavier principal | 🧪 |

## Résultat de l'exécution

- Date : 2026-11-11, commit `e8749d7` de `feature/VTE-03-raccourcis-clavier-caisse`
- Environnement : CI GitHub Actions — `ci.yml` vert (Frontend dont `pos-shortcuts.spec.ts` 13 tests et `pos-page.spec.ts` 9 ; Backend inchangé), `conventions.yml` vert, Qodana for JVM vert sans annotation
- Recette manuelle 🧪 : 4 étapes, à passer par Njara après le merge.
- Anomalies ouvertes : aucune connue.
