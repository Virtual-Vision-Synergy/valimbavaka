# DSG-01 · Maquettes Claude Design de toutes les pages

> Ticket GitHub [#128](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/128) · Ouvert · Milestone : — · 2 SP · Assigné(e) : ManaJudy

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'équipe de développement, je veux disposer de maquettes de toutes les pages de l'application, réalisées avec Claude Design, afin d'avoir une référence visuelle commune avant d'implémenter les écrans.

## Dépendances

- Aucune. À livrer avant l'implémentation des écrans des modules concernés.

## Critères d'acceptation

- [ ] Une maquette existe pour chaque page couverte par le backlog (voir périmètre ci-dessous), réalisée avec Claude Design dans un projet unique partagé avec l'équipe.
- [ ] Les maquettes respectent la charte graphique du dépôt (`resources/design/charte-graphique.md` : palette, logos, tokens) et existent en thème clair et sombre (SYS-04).
- [ ] Chaque page montre l'état nominal, et quand c'est pertinent l'état vide, le chargement et l'erreur.
- [ ] Les pages de la tournée de livraison (LIV-02 à LIV-08) et du poste de caisse (VTE-02) sont aussi maquettées en version mobile/tablette.
- [ ] Le menu et la navigation sont maquettés pour chacun des trois rôles : Administrateur, Vendeuse, Livreur (SYS-05).
- [ ] Chaque maquette indique l'ID du ticket qu'elle illustre (ex. `ART-02`).
- [ ] Les écrans déjà développés (connexion, utilisateurs, journal) sont repris dans les maquettes pour garder la cohérence d'ensemble.
- [ ] Le lien vers le projet de maquettes est ajouté à ce ticket, puis relu et validé par l'équipe.

## Détails

| | |
|---|---|
| **ID Ticket** | `DSG-01` |
| **Module** | Design / UX |
| **Story points** | 2 |
| **Utilisateur concerné** | Toute l'équipe |

## Périmètre : pages à maquetter

| Module | Pages |
|---|---|
| 01 · Authentification & socle | Connexion, liste et formulaire utilisateur, journal des opérations |
| 02 · Fournisseurs | Liste avec filtre par type, fiche fournisseur, création / modification |
| 03 · Articles | Liste (recherche, filtres), fiche article 360°, création / modification, import et export |
| 04 · Stock & transferts | Stock par emplacement (jauge, alertes), bon de réapprovisionnement, réception fournisseur, historique et détail des transferts, retour de stock |
| 05 · Production | Création et liste des lots, détail et coût de revient, indicateurs |
| 06 · Inventaire | Ouverture de session, comptage et écarts, clôture, historique |
| 07 · Services salon | Catalogue, enregistrement d'une prestation, historique du jour |
| 08 · Shooting | Sortie, réintégration, suivi des sorties en cours |
| 09 · Commande & livraison | Création de commande, tournée du jour, marquage et échec de livraison, suivi par statut, historique de statut |
| 10 · Ouverture de journée | Répartition matinale, suivi en temps réel |
| 11 · Vente | Poste de caisse, mise en attente, ventes du jour, détail d'une vente, tableau de bord par canal |
| 12 · Promotions | Création, liste et détail, statut et utilisation |
| 13 · Facture | Facture, avoir, suivi des statuts, livre des ventes |
| 14 · Commissions | Règles, suivi et objectif, validation, recalcul et export |
| 15 · Clôture de journée | Réconciliation, motif d'écart, verrouillage |
| 16 · Rapports & tableau de bord | Cockpit journalier, rapports filtrables, graphiques, classement produits, exports |
| 17 · Transverse | Recherche globale (palette de commandes), bascule thème clair / sombre, menu par rôle |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- _(aucune dépendance)_

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features`
- `valimbavaka-ui/src/app/shared/components`

**Backend (`valimbavaka-core`)**
- _(aucun)_
