# Guide d'intégration — VTE-06 · Mise en attente d'un ticket

Statut : ✅ rédigé le 2026-11-11 · Ticket : [#67](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/67) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

La vendeuse met le ticket courant en attente avec le bouton « Mettre en attente » ou la touche `F8` : le panier est
vidé, **aucune vente n'est enregistrée** et le poste propose le numéro suivant. La maquette VTE-02 faisait déjà ce
geste (état local) ; ce ticket ajoute le garde-fou pendant l'envoi d'un encaissement, le retour d'information sur un
ticket vide, la traduction fr/mg et les tests.

## Contrat exposé

### Endpoints backend

Aucun : l'action est purement locale au poste (aucun appel réseau, rien dans la file hors ligne).

### Services et ports Java réutilisables

- Aucun.

### Frontend

- Route : `/pos` (inchangée).
- Méthode `PosPage.hold()` (`valimbavaka-ui/src/app/features/pos/pages/pos-page/pos-page.ts`), appelée par le bouton
  (`pos-ticket.html`, sortie `hold`) et par `F8` (`pos-shortcuts.ts`, action `hold`).
- Clés i18n : `pos.toast.held` (existante), `pos.toast.holdEmpty` (nouvelle, `public/i18n/fr.json` et `mg.json`).
- Compteur « En attente » : signal `heldCount` de `PosPage`, initialisé depuis `PosContextRDto.heldTickets`.

### Données

- Aucune table, aucune migration, aucune écriture au journal.

## Règles métier à respecter par les autres tickets

- Mettre en attente n'est pas une vente : aucun numéro de vente, aucun stock décrémenté, aucun journal.
- Ticket vide : l'action est refusée avec un avertissement. Pendant l'envoi d'un encaissement : ignorée.
- La vendeuse sélectionnée est conservée ; le règlement et le montant reçu sont remis à zéro (`PosCart.clear`).

## Fichiers concernés

- `valimbavaka-ui/src/app/features/pos/pages/pos-page/pos-page.ts` (modifié : `hold`)
- `valimbavaka-ui/src/app/features/pos/pages/pos-page/pos-page-hold.spec.ts` (nouveau)
- `valimbavaka-ui/public/i18n/fr.json`, `mg.json` (clé `pos.toast.holdEmpty`)

## Dépendances

**Ce ticket s'appuie sur** :
- [VTE-02](../VTE-02/description.md) · [guide](../VTE-02/guide.md) (poste, panier `PosCart`)
- [VTE-03](../VTE-03/description.md) · [guide](../VTE-03/guide.md) (touche `F8`)

**Ces tickets s'appuient sur lui** :
- _(rien)_

## Comment l'utiliser depuis un autre ticket

```text
PosPage.hold() : à appeler pour vider le poste sans vendre (ex. changement de vendeuse imposé).
```

## Points d'attention

- **Hypothèse** : le seul critère du ticket est « vider le panier sans enregistrer de vente ». Le contenu du ticket
  mis en attente n'est **pas conservé** : le compteur « En attente » est un simple compteur d'écran, et la **reprise**
  d'un ticket en attente n'existe pas. Elle demandera une décision (stockage local ou serveur) et un ticket dédié.
- Un ticket en attente ne survit pas au rechargement de la page ; le compteur repart de `heldTickets` fourni par le contexte.
- Tests exécutés en CI le 2026-11-11 (vert).
