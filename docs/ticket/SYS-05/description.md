# SYS-05 · Rôles métier, référentiels et compte administrateur initial

> Ticket GitHub [#111](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/111) · Fermé · Milestone : 01 · Authentification & socle technique · 2 SP · Assigné(e) : ManaJudy

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux que l'application démarre avec les vrais rôles, les référentiels métier et un premier compte administrateur afin de pouvoir me connecter et créer mon équipe dès la première installation.

## Dépendances

- Aucune

## Critères d'acceptation

> Vérifiés le 2026-10-23 sur le code, les tests et la base : détail dans [scenario-test.md](scenario-test.md).

- [x] Les rôles sont exactement : Administrateur, Vendeuse, Livreur (pas d'Opératrice, pas de rôle générique « USER »). Le changeset initial ADMIN/USER est corrigé par un nouveau changeset (jamais en modifiant l'ancien). — *✅ vérifié en base.*
- [x] Les référentiels sont disponibles : canaux de vente (boutique, Facebook, téléphone, livraison — liste à confirmer), gammes (Zezika, Botanika, Solika), emplacements de stock (entrepôt, boutique, livraison, salon). — *✅ vérifié en base (codes en anglais depuis `2026_10_16-01`).*
- [x] Au premier démarrage sur une base sans administrateur, un compte administrateur est créé à partir de variables d'environnement (jamais de mot de passe écrit dans le code ou dans un changelog), et doit changer son mot de passe à la première connexion. — *✅ vérifié : aucune permission tant que le mot de passe n'est pas changé, écran `/password` (corrigé le 2026-10-23).*

## Détails

| | |
|---|---|
| **ID Ticket** | `SYS-05` |
| **Module** | Système |
| **Story points** | 2 |
| **Utilisateur concerné** | Administrateur |

Ajouté lors de la revue du backlog du 07/10 : sans ce ticket, personne ne peut se connecter sur une base neuve (staging/prod), et AUTH-03 ne peut pas proposer les bons rôles.

## Ce ticket débloque

- [AUTH-01](../AUTH-01/description.md) · Connexion selon le rôle attribué
- [AUTH-03](../AUTH-03/description.md) · Création et modification d'un utilisateur
- [ART-01](../ART-01/description.md) · Fiche article complète
- [ART-03](../ART-03/description.md) · Identification visuelle de la gamme
- [ART-05](../ART-05/description.md) · Recherche et filtrage des articles
- [STK-01](../STK-01/description.md) · Suivi du stock par emplacement
- [STK-03](../STK-03/description.md) · Quantité et valeur du stock par emplacement
- [TRF-03](../TRF-03/description.md) · Enregistrement d'un retour de stock
- [PRO-01](../PRO-01/description.md) · Création d'une promotion
- [VTE-01](../VTE-01/description.md) · Enregistrement d'une vente multicanal
- [VTE-08](../VTE-08/description.md) · Tableau de bord des ventes par canal
- [INV-01](../INV-01/description.md) · Ouverture d'une session d'inventaire
- [COM-01](../COM-01/description.md) · Règles de commission par canal
- [COM-02](../COM-02/description.md) · Calcul automatique de la commission
- [DSH-04](../DSH-04/description.md) · Répartition du CA par gamme et canal
- [RPT-01](../RPT-01/description.md) · Rapports filtrables journaliers/mensuels/annuels
- [RPT-04](../RPT-04/description.md) · Répartition du CA par gamme
- [SYS-06](../SYS-06/description.md) · Audit : auteur des opérations en UUID
- [STK-06](../STK-06/description.md) · Réception d'une livraison fournisseur (entrée en stock)

## Dépend de

- _(aucune dépendance)_

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/referential`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/referential`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/user`
