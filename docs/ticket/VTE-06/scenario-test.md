# Scénarios de test — VTE-06 · Mise en attente d'un ticket

Statut : ✅ rédigé et exécuté le 2026-11-11 · Ticket : [#67](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/67) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests frontend Vitest sur le comportement visible. Ticket **frontend seul** : aucun endpoint,
aucune permission, aucune migration (la maquette VTE-02 / VTE-03 portait déjà l'action ; ce ticket la fiabilise).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | Vendeuse (`SELLER`, permission `sales.create`) |
| Données de départ | `/pos` ouvert ; article « Savon noir 100 g », 8 500 Ar, stock 10 ; vendeuse Miora Rakoto ; ticket n° 42 ; 1 ticket déjà en attente |
| Tickets à avoir déployés avant | VTE-01 (enregistrement), VTE-02 (poste de caisse), VTE-03 (touche `F8`) |

## Scénarios issus des critères d'acceptation

| # | Critère d'acceptation | Exemple (étant donné / quand / alors) | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | L'action vide le panier sans enregistrer de vente. | 1 Savon noir au ticket ; touche `F8` | panier vide, `PosService.record` jamais appelé, toast « Ticket VTE-42 mis en attente » | `pos-page-hold.spec.ts` « F8 vide le panier sans enregistrer de vente et annonce le ticket mis en attente » | ✅ auto |
| 1b | Idem par le bouton | 1 Savon noir ; clic sur « Mettre en attente » | même résultat que `F8` ; le ticket affiché passe à VTE-43 | `pos-page-hold.spec.ts` « le bouton Mettre en attente donne le même résultat que F8 et passe au numéro suivant » | ✅ auto |
| 1c | La vendeuse est conservée | 1 Savon noir ; `F8` | la vendeuse sélectionnée reste Miora pour le ticket suivant | `pos-page-hold.spec.ts` « la vendeuse choisie est conservée pour le ticket suivant » | ✅ auto |

## Sécurité et erreurs

Aucun endpoint : pas de 401, 403, 404, 409 ni 422. Rien n'est envoyé au serveur lors de la mise en attente.

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Ticket vide | `F8` sans article | rien ne change, toast d'avertissement « Ticket vide : rien à mettre en attente » | `pos-page-hold.spec.ts` « F8 sur un ticket vide ne change rien et prévient la caissière » | ✅ auto |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Encaissement en cours d'envoi | `F12` (réponse non reçue) puis `F8` | la mise en attente est ignorée ; une seule vente envoyée ; le panier part avec la vente | `pos-page-hold.spec.ts` « F8 pendant l'envoi d'un encaissement ne met pas le ticket en attente » | ✅ auto |
| Touche maintenue | `F8` avec `repeat` | une seule mise en attente | ⏭ VTE-03 (`pos-shortcuts.spec.ts` « ne répète pas encaisser ni mettre en attente quand la touche reste enfoncée ») | ⏭ VTE-03 |
| Bouton désactivé | ticket vide | bouton « Mettre en attente » désactivé | non testé (template inchangé depuis VTE-02) | — |
| Hors ligne | `F8` sans réseau | identique : aucun appel réseau, rien en file | couvert par le cas 1 (aucun appel à `record`) | ✅ auto |

## Tests existants

- `valimbavaka-ui/src/app/features/pos/pages/pos-page/pos-page-hold.spec.ts` (nouveau, 5 tests)

## Recette manuelle (🧪)

| # | Parcours | Attendu | Statut |
|---|---|---|---|
| R1 | `/pos`, ajouter 2 articles, `F8` | panier vide, compteur « En attente » +1, numéro de ticket +1 ; aucune vente dans la liste des ventes | 🧪 |
| R2 | `/pos` sans article, `F8` | toast d'avertissement, rien ne change | 🧪 |

## Résultat de l'exécution

- Date : 2026-11-11
- Environnement : CI GitHub Actions (ci.yml, conventions.yml, Qodana) verte sur la branche ; 5 tests Vitest ajoutés dans pos-page-hold.spec.ts
- Anomalies ouvertes : aucune connue ; la reprise d'un ticket mis en attente n'est pas dans ce ticket (voir guide)
