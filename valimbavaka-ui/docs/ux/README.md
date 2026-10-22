# Notes UX par écran

La maquette (`resources/Maquette Valimbavaka-png/`) sert de base, pas de calque : chaque écran est pensé selon qui s'en
sert, sur quel appareil, et pour quelle tâche n°1. Les pages marquées « En construction » utilisent des fixtures typées
(`data-access/*-fixtures.ts`) ; le basculement vers l'API ne touche que le service.

| Écran | Qui / appareil | Tâche n°1 | Écarts assumés vs maquette |
|---|---|---|---|
| Connexion (AUTH-01) | Tous, bureau ou mobile | Se connecter sans erreur | « Rester connecté » et « mot de passe oublié » omis (pas dans l'API) ; œil, alerte Caps Lock, bascule thème ajoutés |
| Utilisateurs (AUTH-03) | Admin, bureau | Trouver un compte, activer/désactiver | Confirmation par `<dialog>` natif au lieu de `confirm()` ; pas de filtre par rôle (l'API ne le filtre pas) |
| Journal (JNL-01) | Admin, bureau | Retrouver une opération | Bandeau « ajout seul » ; filtres date/module/auteur à venir avec l'API |
| Tableau de bord | Admin/vendeuse | Voir la journée d'un coup d'œil | Deux onglets (vue d'ensemble / analyse) |
| Ouverture / clôture (JR) | Admin, bureau puis tablette | Répartir le stock, réconcilier le soir | Grille au clavier (Entrée descend) ; panneau latéral sous le tableau < 1680 px |
| Livraisons (LIV) | Admin bureau ; livreur mobile | Suivre les 4 statuts ; dérouler la tournée | Panneau de détail docké ≥ 1600 px, tiroir sinon ; coquille mobile dédiée du livreur |
| Fournisseurs, transferts, inventaire, production, promotions | Admin, bureau | Consulter et saisir | Panneaux latéraux partagés (`vvs-side-panel`), filtres en pastilles |
| Commissions (COM) | Vendeuse, mobile/bureau | Savoir où j'en suis du mois | Anneau d'objectif + « reste à faire par jour » |
| Salon & shooting (SRV/SHO) | Vendeuse, tablette | Enregistrer une prestation vite | Produits consommés pré-remplis depuis la recette du soin, alerte de rupture |
| Rapports (RPT) | Admin, bureau | Comparer période / période précédente | Graphique apparié accessible (tableau équivalent masqué) |
| Recherche globale (SYS-03) | Tous, clavier | Aller à un article ou une action | Combobox ARIA (flèches/Entrée), surlignage sans `innerHTML` |

Principes communs : cibles ≥ 24 px (44 px mobile), focus visible, couleur jamais seule (point + libellé), états
chargement/vide/erreur via `vvs-list-state`, `prefers-reduced-motion` respecté, clair/sombre par jetons.
