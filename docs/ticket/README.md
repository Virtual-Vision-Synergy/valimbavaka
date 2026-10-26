# Tickets — dossier de travail

Généré le 2026-10-23 depuis les issues GitHub (108 tickets). Un dossier par ticket :

```text
docs/ticket/<ID>/
├── description.md    # le ticket (user story, critères, dépendances, zone du code)
├── scenario-test.md  # à compléter par le traiteur en premier
└── guide.md          # à compléter par le traiteur ensuite, lu par les autres tickets
```

## Règle de travail

**La consigne complète est dans [CONSIGNE.md](CONSIGNE.md)** : à lire avant de prendre un ticket.

1. **Avant de commencer** un ticket : lire sa `description.md` et le `guide.md` de chaque ticket listé dans « Dépend de ».
2. **Avant de coder** : écrire le scénario de test à partir des critères ; **pendant** : y reporter le nom de chaque test.
3. **Avant la PR** : exécuter les tests, mettre les statuts et le résultat, puis écrire `guide.md` (endpoints, fichiers exposés, contrat, pièges). Les fichiers du dossier font partie de la PR.
4. **Après le merge** : la QA passe la recette manuelle (🧪) et met à jour le scénario.
5. Modèles complets : [AUTH-04](AUTH-04/scenario-test.md), [FRS-01](FRS-01/scenario-test.md), [JNL-02](JNL-02/scenario-test.md).
6. Les tickets marqués ⚠ ont un guide pré-rempli depuis le code : à vérifier, pas à croire sur parole.

## Règles communes (rappel)

- Convention de codage : `resources/convention-dev-VVS-V2.1.pdf` ; synthèse dans `CLAUDE.md`.
- API : `/api/v1/...`, DTO `XxxRDto` / `XxxWDto`, erreurs `ProblemDetail`, listes `PageResponse` (page base 1, taille ≤ 100), autorisation `@HasPermission("module.action")`.
- Opérations métier : repositories qui étendent `OperationRepository`, services via `ReversibleOperationService` (jamais de suppression, annulation / contre-passation).
- Écritures rejouables hors ligne : en-tête `Idempotency-Key` (voir `docs/adr/0001-synchronisation-hors-ligne.md`).
- Branche `feature/<TICKET>-<description-kebab>`, PR `ID · SPx · Titre`, `Refs #<numéro>`.

## Index

### 01 · Authentification & socle technique

| Ticket | Titre | SP | Assigné(e) | GitHub | Dépend de | Scénario | Guide |
|---|---|---|---|---|---|---|---|
| [AUTH-01](AUTH-01/description.md) | Connexion selon le rôle attribué | 2 | And-Matia | [#10](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/10) ✅ | [SYS-05](SYS-05/description.md) | [✅](AUTH-01/scenario-test.md) | [✅](AUTH-01/guide.md) |
| [AUTH-02](AUTH-02/description.md) | Déconnexion utilisateur | 1 | And-Matia | [#11](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/11) ✅ | [AUTH-01](AUTH-01/description.md) | [✅](AUTH-02/scenario-test.md) | [✅](AUTH-02/guide.md) |
| [AUTH-03](AUTH-03/description.md) | Création et modification d'un utilisateur | 2 | And-Matia | [#12](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/12) ✅ | [AUTH-01](AUTH-01/description.md), [JNL-01](JNL-01/description.md), [SYS-05](SYS-05/description.md), [JNL-02](JNL-02/description.md) | [✅](AUTH-03/scenario-test.md) | [✅](AUTH-03/guide.md) |
| [AUTH-04](AUTH-04/description.md) | Activation / désactivation d'un utilisateur | 1 | And-Matia | [#13](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/13) ✅ | [AUTH-03](AUTH-03/description.md), [JNL-01](JNL-01/description.md) | [✅](AUTH-04/scenario-test.md) | [✅](AUTH-04/guide.md) |
| [JNL-01](JNL-01/description.md) | Journal append-only des opérations | 2 | And-Matia | [#14](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/14) ✅ | [AUTH-01](AUTH-01/description.md), [SYS-06](SYS-06/description.md) | [✅](JNL-01/scenario-test.md) | [✅](JNL-01/guide.md) |
| [JNL-02](JNL-02/description.md) | Interdiction de suppression des opérations validées | 2 | And-Matia | [#15](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/15) ✅ | [JNL-01](JNL-01/description.md) | [✅](JNL-02/scenario-test.md) | [✅](JNL-02/guide.md) |
| [SYS-02](SYS-02/description.md) | Sauvegarde automatique hors-ligne | 2 | And-Matia | [#16](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/16) ✅ | [AUTH-01](AUTH-01/description.md) | [✅](SYS-02/scenario-test.md) | [✅](SYS-02/guide.md) |
| [SYS-05](SYS-05/description.md) | Rôles métier, référentiels et compte administrateur initial | 2 | ManaJudy | [#111](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/111) ✅ | — | [✅](SYS-05/scenario-test.md) | [✅](SYS-05/guide.md) |
| [SYS-06](SYS-06/description.md) | Audit : auteur des opérations en UUID | 1 | ManaJudy | [#112](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/112) ✅ | [SYS-05](SYS-05/description.md) | [✅](SYS-06/scenario-test.md) | [✅](SYS-06/guide.md) |
| [SYS-07](SYS-07/description.md) | Brancher un premier formulaire sur la file hors ligne | 2 | And-Matia | [#133](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/133) 🟡 | [SYS-02](SYS-02/description.md), [FRS-02](FRS-02/description.md) | [☐](SYS-07/scenario-test.md) | [☐](SYS-07/guide.md) |
| [SYS-08](SYS-08/description.md) | Reprise automatique de la synchronisation | 1 | Kei-Shiro | [#134](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/134) 🟡 | [SYS-02](SYS-02/description.md) | [⚠](SYS-08/scenario-test.md) | [⚠](SYS-08/guide.md) |
| [SYS-09](SYS-09/description.md) | Suivi et gestion des saisies en échec | 2 | Kei-Shiro | [#135](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/135) 🟡 | [SYS-02](SYS-02/description.md), [SYS-07](SYS-07/description.md) | [☐](SYS-09/scenario-test.md) | [☐](SYS-09/guide.md) |
| [SYS-10](SYS-10/description.md) | Règle d'ordre de synchronisation en cas de blocage | 1 | And-Matia, Kei-Shiro | [#136](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/136) 🟡 | [SYS-02](SYS-02/description.md) | [☐](SYS-10/scenario-test.md) | [☐](SYS-10/guide.md) |
| [SYS-11](SYS-11/description.md) | Session expirée avec des saisies en attente | 1 | Kei-Shiro | [#137](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/137) 🟡 | [SYS-02](SYS-02/description.md), [AUTH-01](AUTH-01/description.md) | [⚠](SYS-11/scenario-test.md) | [⚠](SYS-11/guide.md) |
| [SYS-12](SYS-12/description.md) | Idempotence côté serveur des saisies renvoyées | 1 | ManaJudy | [#138](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/138) 🟡 | [SYS-02](SYS-02/description.md), [JNL-01](JNL-01/description.md) | [☐](SYS-12/scenario-test.md) | [☐](SYS-12/guide.md) |

### 02 · Fournisseurs

| Ticket | Titre | SP | Assigné(e) | GitHub | Dépend de | Scénario | Guide |
|---|---|---|---|---|---|---|---|
| [FRS-01](FRS-01/description.md) | Fiche fournisseur complète | 1 | And-Matia | [#17](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/17) ✅ | [JNL-01](JNL-01/description.md) | [✅](FRS-01/scenario-test.md) | [✅](FRS-01/guide.md) |
| [FRS-02](FRS-02/description.md) | Création / modification d'un fournisseur | 1 | And-Matia | [#18](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/18) 🟡 | [FRS-01](FRS-01/description.md), [JNL-01](JNL-01/description.md) | [✅](FRS-02/scenario-test.md) | [✅](FRS-02/guide.md) |
| [FRS-03](FRS-03/description.md) | Filtrage des fournisseurs par type | 1 | Njara-Niaina | [#19](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/19) 🟡 | [FRS-01](FRS-01/description.md) | [✅](FRS-03/scenario-test.md) | [✅](FRS-03/guide.md) |

### 03 · Articles

| Ticket | Titre | SP | Assigné(e) | GitHub | Dépend de | Scénario | Guide |
|---|---|---|---|---|---|---|---|
| [ART-01](ART-01/description.md) | Fiche article complète | 2 | Kei-Shiro | [#21](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/21) 🟡 | [FRS-01](FRS-01/description.md), [JNL-01](JNL-01/description.md), [SYS-05](SYS-05/description.md) | [☐](ART-01/scenario-test.md) | [☐](ART-01/guide.md) |
| [ART-02](ART-02/description.md) | Création / modification d'un article | 2 | ManaJudy | [#22](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/22) 🟡 | [ART-01](ART-01/description.md), [JNL-01](JNL-01/description.md) | [☐](ART-02/scenario-test.md) | [☐](ART-02/guide.md) |
| [ART-03](ART-03/description.md) | Identification visuelle de la gamme | 1 | ManaJudy | [#23](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/23) 🟡 | [ART-01](ART-01/description.md), [SYS-05](SYS-05/description.md) | [☐](ART-03/scenario-test.md) | [☐](ART-03/guide.md) |
| [ART-04](ART-04/description.md) | Type d'article et usage en production | 2 | Kei-Shiro | [#24](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/24) 🟡 | [ART-01](ART-01/description.md) | [☐](ART-04/scenario-test.md) | [☐](ART-04/guide.md) |
| [ART-05](ART-05/description.md) | Recherche et filtrage des articles | 1 | Njara-Niaina | [#25](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/25) 🟡 | [ART-01](ART-01/description.md), [SYS-05](SYS-05/description.md) | [☐](ART-05/scenario-test.md) | [☐](ART-05/guide.md) |
| [ART-07](ART-07/description.md) | Import d'articles en masse | 1 | ManaJudy | [#27](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/27) 🟡 | [ART-01](ART-01/description.md), [JNL-01](JNL-01/description.md) | [☐](ART-07/scenario-test.md) | [☐](ART-07/guide.md) |
| [ART-08](ART-08/description.md) | Export de la liste des articles | 1 | And-Matia | [#28](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/28) 🟡 | [ART-01](ART-01/description.md), [SYS-01](SYS-01/description.md) | [☐](ART-08/scenario-test.md) | [☐](ART-08/guide.md) |
| [SYS-01](SYS-01/description.md) | Export PDF/Excel généralisé | 2 | ManaJudy | [#105](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/105) 🟡 | [AUTH-01](AUTH-01/description.md) | [⚠](SYS-01/scenario-test.md) | [⚠](SYS-01/guide.md) |

### 04 · Stock & transferts

| Ticket | Titre | SP | Assigné(e) | GitHub | Dépend de | Scénario | Guide |
|---|---|---|---|---|---|---|---|
| [FRS-04](FRS-04/description.md) | Délai de réapprovisionnement selon le type de fournisseur | 1 | ManaJudy | [#20](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/20) 🟡 | [FRS-01](FRS-01/description.md), [STK-04](STK-04/description.md) | [☐](FRS-04/scenario-test.md) | [☐](FRS-04/guide.md) |
| [STK-01](STK-01/description.md) | Suivi du stock par emplacement | 2 | ManaJudy | [#30](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/30) 🟡 | [ART-01](ART-01/description.md), [JNL-01](JNL-01/description.md), [SYS-05](SYS-05/description.md) | [☐](STK-01/scenario-test.md) | [☐](STK-01/guide.md) |
| [STK-02](STK-02/description.md) | Jauge visuelle du stock | 1 | Njara-Niaina | [#31](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/31) 🟡 | [STK-01](STK-01/description.md) | [☐](STK-02/scenario-test.md) | [☐](STK-02/guide.md) |
| [STK-03](STK-03/description.md) | Quantité et valeur du stock par emplacement | 1 | Kei-Shiro | [#32](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/32) 🟡 | [STK-01](STK-01/description.md), [SYS-05](SYS-05/description.md) | [☐](STK-03/scenario-test.md) | [☐](STK-03/guide.md) |
| [STK-04](STK-04/description.md) | Alerte de seuil minimum | 2 | And-Matia | [#33](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/33) 🟡 | [STK-01](STK-01/description.md) | [☐](STK-04/scenario-test.md) | [☐](STK-04/guide.md) |
| [STK-05](STK-05/description.md) | Génération d'un bon de réapprovisionnement | 1 | ManaJudy | [#34](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/34) 🟡 | [FRS-01](FRS-01/description.md), [STK-04](STK-04/description.md), [JNL-01](JNL-01/description.md) | [☐](STK-05/scenario-test.md) | [☐](STK-05/guide.md) |
| [TRF-01](TRF-01/description.md) | Historique des transferts de stock | 1 | And-Matia | [#35](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/35) 🟡 | [TRF-03](TRF-03/description.md) | [☐](TRF-01/scenario-test.md) | [☐](TRF-01/guide.md) |
| [TRF-03](TRF-03/description.md) | Enregistrement d'un retour de stock | 1 | Kei-Shiro | [#36](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/36) 🟡 | [STK-01](STK-01/description.md), [JNL-02](JNL-02/description.md), [JNL-01](JNL-01/description.md), [SYS-05](SYS-05/description.md) | [☐](TRF-03/scenario-test.md) | [☐](TRF-03/guide.md) |
| [TRF-04](TRF-04/description.md) | Détail d'un transfert | 1 | Njara-Niaina | [#37](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/37) 🟡 | [TRF-01](TRF-01/description.md) | [☐](TRF-04/scenario-test.md) | [☐](TRF-04/guide.md) |
| [STK-06](STK-06/description.md) | Réception d'une livraison fournisseur (entrée en stock) | 2 | Kei-Shiro | [#113](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/113) 🟡 | [FRS-01](FRS-01/description.md), [STK-01](STK-01/description.md), [JNL-02](JNL-02/description.md), [JNL-01](JNL-01/description.md), [SYS-05](SYS-05/description.md), [STK-05](STK-05/description.md) | [☐](STK-06/scenario-test.md) | [☐](STK-06/guide.md) |

### 05 · Production

| Ticket | Titre | SP | Assigné(e) | GitHub | Dépend de | Scénario | Guide |
|---|---|---|---|---|---|---|---|
| [PRD-01](PRD-01/description.md) | Création d'un lot de production | 2 | And-Matia | [#38](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/38) 🟡 | [ART-04](ART-04/description.md), [STK-01](STK-01/description.md), [JNL-02](JNL-02/description.md), [JNL-01](JNL-01/description.md) | [☐](PRD-01/scenario-test.md) | [☐](PRD-01/guide.md) |
| [PRD-02](PRD-02/description.md) | Cohérence stock lors de la validation d'un lot | 2 | Kei-Shiro | [#39](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/39) 🟡 | [PRD-01](PRD-01/description.md), [JNL-01](JNL-01/description.md) | [☐](PRD-02/scenario-test.md) | [☐](PRD-02/guide.md) |
| [PRD-03](PRD-03/description.md) | Statut d'un lot de production | 1 | ManaJudy | [#40](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/40) 🟡 | [PRD-01](PRD-01/description.md) | [☐](PRD-03/scenario-test.md) | [☐](PRD-03/guide.md) |
| [PRD-04](PRD-04/description.md) | Indicateurs de production | 1 | Njara-Niaina | [#41](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/41) 🟡 | [PRD-02](PRD-02/description.md) | [☐](PRD-04/scenario-test.md) | [☐](PRD-04/guide.md) |
| [PRD-05](PRD-05/description.md) | Détail et coût de revient d'un lot | 1 | And-Matia | [#42](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/42) 🟡 | [PRD-02](PRD-02/description.md) | [☐](PRD-05/scenario-test.md) | [☐](PRD-05/guide.md) |

### 06 · Inventaire

| Ticket | Titre | SP | Assigné(e) | GitHub | Dépend de | Scénario | Guide |
|---|---|---|---|---|---|---|---|
| [INV-01](INV-01/description.md) | Ouverture d'une session d'inventaire | 1 | Kei-Shiro | [#77](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/77) 🟡 | [STK-01](STK-01/description.md), [JNL-01](JNL-01/description.md), [SYS-05](SYS-05/description.md) | [☐](INV-01/scenario-test.md) | [☐](INV-01/guide.md) |
| [INV-02](INV-02/description.md) | Comptage physique avec calcul d'écart | 2 | ManaJudy | [#78](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/78) 🟡 | [INV-01](INV-01/description.md), [JNL-01](JNL-01/description.md) | [☐](INV-02/scenario-test.md) | [☐](INV-02/guide.md) |
| [INV-03](INV-03/description.md) | Motif d'écart par ligne | 1 | And-Matia | [#79](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/79) 🟡 | [INV-02](INV-02/description.md) | [☐](INV-03/scenario-test.md) | [☐](INV-03/guide.md) |
| [INV-04](INV-04/description.md) | Clôture d'une session d'inventaire | 2 | And-Matia | [#80](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/80) 🟡 | [INV-02](INV-02/description.md), [JNL-02](JNL-02/description.md), [JNL-01](JNL-01/description.md) | [☐](INV-04/scenario-test.md) | [☐](INV-04/guide.md) |
| [INV-05](INV-05/description.md) | Historique des sessions d'inventaire | 1 | Njara-Niaina | [#81](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/81) 🟡 | [INV-04](INV-04/description.md) | [☐](INV-05/scenario-test.md) | [☐](INV-05/guide.md) |

### 07 · Services salon (catalogue + prestations)

| Ticket | Titre | SP | Assigné(e) | GitHub | Dépend de | Scénario | Guide |
|---|---|---|---|---|---|---|---|
| [SRV-01](SRV-01/description.md) | Catalogue des services salon | 1 | Kei-Shiro | [#29](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/29) 🟡 | [ART-01](ART-01/description.md) | [☐](SRV-01/scenario-test.md) | [☐](SRV-01/guide.md) |
| [SRV-02](SRV-02/description.md) | Enregistrement d'une prestation | 2 | ManaJudy | [#82](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/82) 🟡 | [SRV-01](SRV-01/description.md), [STK-01](STK-01/description.md), [JNL-02](JNL-02/description.md), [JNL-01](JNL-01/description.md) | [☐](SRV-02/scenario-test.md) | [☐](SRV-02/guide.md) |
| [SRV-03](SRV-03/description.md) | Historique des prestations du jour | 1 | And-Matia | [#83](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/83) 🟡 | [SRV-02](SRV-02/description.md) | [☐](SRV-03/scenario-test.md) | [☐](SRV-03/guide.md) |

### 08 · Shooting

| Ticket | Titre | SP | Assigné(e) | GitHub | Dépend de | Scénario | Guide |
|---|---|---|---|---|---|---|---|
| [SHO-01](SHO-01/description.md) | Enregistrement d'une sortie shooting | 1 | Kei-Shiro | [#43](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/43) 🟡 | [STK-01](STK-01/description.md), [JNL-02](JNL-02/description.md), [JNL-01](JNL-01/description.md) | [☐](SHO-01/scenario-test.md) | [☐](SHO-01/guide.md) |
| [SHO-02](SHO-02/description.md) | Réintégration d'un produit shooting | 1 | ManaJudy | [#44](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/44) 🟡 | [SHO-01](SHO-01/description.md), [JNL-01](JNL-01/description.md) | [☐](SHO-02/scenario-test.md) | [☐](SHO-02/guide.md) |
| [SHO-03](SHO-03/description.md) | Suivi des sorties shooting en cours | 1 | Njara-Niaina | [#45](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/45) 🟡 | [SHO-01](SHO-01/description.md) | [☐](SHO-03/scenario-test.md) | [☐](SHO-03/guide.md) |

### 09 · Commande & Livraison/Réception

| Ticket | Titre | SP | Assigné(e) | GitHub | Dépend de | Scénario | Guide |
|---|---|---|---|---|---|---|---|
| [LIV-01](LIV-01/description.md) | Création d'une commande à livrer | 2 | Kei-Shiro | [#46](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/46) 🟡 | [ART-01](ART-01/description.md), [AUTH-03](AUTH-03/description.md), [STK-01](STK-01/description.md), [JNL-02](JNL-02/description.md), [JNL-01](JNL-01/description.md) | [☐](LIV-01/scenario-test.md) | [☐](LIV-01/guide.md) |
| [LIV-02](LIV-02/description.md) | Consultation de la tournée du jour | 2 | ManaJudy | [#47](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/47) 🟡 | [LIV-01](LIV-01/description.md) | [☐](LIV-02/scenario-test.md) | [☐](LIV-02/guide.md) |
| [LIV-03](LIV-03/description.md) | Marquage rapide d'une livraison | 2 | And-Matia | [#48](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/48) 🟡 | [LIV-02](LIV-02/description.md), [JNL-01](JNL-01/description.md) | [☐](LIV-03/scenario-test.md) | [☐](LIV-03/guide.md) |
| [LIV-04](LIV-04/description.md) | Signalement d'un échec de livraison | 2 | ManaJudy | [#49](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/49) 🟡 | [LIV-02](LIV-02/description.md), [JNL-01](JNL-01/description.md) | [☐](LIV-04/scenario-test.md) | [☐](LIV-04/guide.md) |
| [LIV-05](LIV-05/description.md) | Annulation d'une action récente | 1 | Kei-Shiro | [#50](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/50) 🟡 | [LIV-03](LIV-03/description.md), [LIV-04](LIV-04/description.md), [JNL-01](JNL-01/description.md) | [☐](LIV-05/scenario-test.md) | [☐](LIV-05/guide.md) |
| [LIV-06](LIV-06/description.md) | Suivi des livraisons par statut | 1 | Njara-Niaina | [#51](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/51) 🟡 | [LIV-01](LIV-01/description.md) | [☐](LIV-06/scenario-test.md) | [☐](LIV-06/guide.md) |
| [LIV-07](LIV-07/description.md) | Historique de statut d'une livraison | 1 | And-Matia | [#52](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/52) 🟡 | [LIV-03](LIV-03/description.md), [SYS-06](SYS-06/description.md) | [☐](LIV-07/scenario-test.md) | [☐](LIV-07/guide.md) |
| [LIV-08](LIV-08/description.md) | Fonctionnement hors ligne de la tournée | 1 | Kei-Shiro | [#53](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/53) 🟡 | [LIV-03](LIV-03/description.md), [SYS-02](SYS-02/description.md) | [☐](LIV-08/scenario-test.md) | [☐](LIV-08/guide.md) |

### 10 · Ouverture de journée

| Ticket | Titre | SP | Assigné(e) | GitHub | Dépend de | Scénario | Guide |
|---|---|---|---|---|---|---|---|
| [JR-01](JR-01/description.md) | Répartition matinale du stock | 2 | ManaJudy | [#54](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/54) 🟡 | [STK-01](STK-01/description.md), [TRF-03](TRF-03/description.md), [JNL-02](JNL-02/description.md), [JNL-01](JNL-01/description.md) | [☐](JR-01/scenario-test.md) | [☐](JR-01/guide.md) |
| [TRF-02](TRF-02/description.md) | Génération automatique des transferts du matin | 2 | And-Matia | [#55](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/55) 🟡 | [JR-01](JR-01/description.md), [JNL-01](JNL-01/description.md) | [☐](TRF-02/scenario-test.md) | [☐](TRF-02/guide.md) |
| [JR-02](JR-02/description.md) | Reprise des quantités de la veille | 1 | Kei-Shiro | [#56](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/56) 🟡 | [JR-01](JR-01/description.md) | [☐](JR-02/scenario-test.md) | [☐](JR-02/guide.md) |

### 11 · Vente

| Ticket | Titre | SP | Assigné(e) | GitHub | Dépend de | Scénario | Guide |
|---|---|---|---|---|---|---|---|
| [VTE-01](VTE-01/description.md) | Enregistrement d'une vente multicanal | 2 | And-Matia | [#62](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/62) 🟡 | [ART-01](ART-01/description.md), [AUTH-03](AUTH-03/description.md), [JR-01](JR-01/description.md), [STK-01](STK-01/description.md), [JNL-02](JNL-02/description.md), [JNL-01](JNL-01/description.md), [SYS-05](SYS-05/description.md) | [☐](VTE-01/scenario-test.md) | [☐](VTE-01/guide.md) |
| [VTE-02](VTE-02/description.md) | Poste de caisse avec calcul automatique | 2 | Kei-Shiro | [#63](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/63) 🟡 | [VTE-01](VTE-01/description.md) | [☐](VTE-02/scenario-test.md) | [☐](VTE-02/guide.md) |
| [VTE-03](VTE-03/description.md) | Raccourcis clavier de caisse | 1 | ManaJudy | [#64](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/64) 🟡 | [VTE-02](VTE-02/description.md) | [☐](VTE-03/scenario-test.md) | [☐](VTE-03/guide.md) |
| [VTE-05](VTE-05/description.md) | Choix du mode de règlement | 2 | ManaJudy | [#66](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/66) 🟡 | [VTE-02](VTE-02/description.md) | [☐](VTE-05/scenario-test.md) | [☐](VTE-05/guide.md) |
| [VTE-06](VTE-06/description.md) | Mise en attente d'un ticket | 1 | Kei-Shiro | [#67](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/67) 🟡 | [VTE-02](VTE-02/description.md) | [☐](VTE-06/scenario-test.md) | [☐](VTE-06/guide.md) |
| [VTE-07](VTE-07/description.md) | Consultation des ventes du jour | 1 | Njara-Niaina | [#68](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/68) 🟡 | [VTE-01](VTE-01/description.md) | [☐](VTE-07/scenario-test.md) | [☐](VTE-07/guide.md) |
| [VTE-08](VTE-08/description.md) | Tableau de bord des ventes par canal | 1 | And-Matia | [#69](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/69) 🟡 | [VTE-01](VTE-01/description.md), [SYS-05](SYS-05/description.md) | [☐](VTE-08/scenario-test.md) | [☐](VTE-08/guide.md) |

### 12 · Promotions

| Ticket | Titre | SP | Assigné(e) | GitHub | Dépend de | Scénario | Guide |
|---|---|---|---|---|---|---|---|
| [PRO-01](PRO-01/description.md) | Création d'une promotion | 2 | ManaJudy | [#58](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/58) 🟡 | [ART-01](ART-01/description.md), [JNL-01](JNL-01/description.md), [SYS-05](SYS-05/description.md) | [☐](PRO-01/scenario-test.md) | [☐](PRO-01/guide.md) |
| [PRO-02](PRO-02/description.md) | Application automatique d'une promotion en caisse | 2 | And-Matia | [#59](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/59) 🟡 | [PRO-01](PRO-01/description.md), [VTE-02](VTE-02/description.md) | [☐](PRO-02/scenario-test.md) | [☐](PRO-02/guide.md) |
| [PRO-03](PRO-03/description.md) | Statut et utilisation d'une promotion | 1 | Kei-Shiro | [#60](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/60) 🟡 | [PRO-02](PRO-02/description.md) | [☐](PRO-03/scenario-test.md) | [☐](PRO-03/guide.md) |
| [PRO-04](PRO-04/description.md) | Liste et détail des promotions | 1 | Njara-Niaina | [#61](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/61) 🟡 | [PRO-01](PRO-01/description.md) | [☐](PRO-04/scenario-test.md) | [☐](PRO-04/guide.md) |
| [VTE-04](VTE-04/description.md) | Application d'un code promo | 1 | Kei-Shiro | [#65](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/65) 🟡 | [PRO-01](PRO-01/description.md), [VTE-02](VTE-02/description.md) | [☐](VTE-04/scenario-test.md) | [☐](VTE-04/guide.md) |

### 13 · Facture

| Ticket | Titre | SP | Assigné(e) | GitHub | Dépend de | Scénario | Guide |
|---|---|---|---|---|---|---|---|
| [FAC-01](FAC-01/description.md) | Génération automatique de facture | 2 | ManaJudy | [#71](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/71) 🟡 | [VTE-02](VTE-02/description.md), [JNL-01](JNL-01/description.md) | [☐](FAC-01/scenario-test.md) | [☐](FAC-01/guide.md) |
| [FAC-02](FAC-02/description.md) | Régénération d'une facture existante | 1 | And-Matia | [#72](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/72) 🟡 | [FAC-01](FAC-01/description.md), [VTE-01](VTE-01/description.md), [JNL-01](JNL-01/description.md), [VTE-02](VTE-02/description.md) | [☐](FAC-02/scenario-test.md) | [☐](FAC-02/guide.md) |
| [FAC-03](FAC-03/description.md) | Impression / export / envoi de facture | 2 | Kei-Shiro | [#73](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/73) 🟡 | [FAC-01](FAC-01/description.md), [SYS-01](SYS-01/description.md) | [☐](FAC-03/scenario-test.md) | [☐](FAC-03/guide.md) |
| [FAC-04](FAC-04/description.md) | Émission d'un avoir motivé | 2 | Kei-Shiro | [#74](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/74) 🟡 | [FAC-01](FAC-01/description.md), [JNL-02](JNL-02/description.md), [JNL-01](JNL-01/description.md) | [☐](FAC-04/scenario-test.md) | [☐](FAC-04/guide.md) |
| [FAC-05](FAC-05/description.md) | Suivi des statuts de facture | 1 | ManaJudy | [#75](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/75) 🟡 | [FAC-01](FAC-01/description.md) | [☐](FAC-05/scenario-test.md) | [☐](FAC-05/guide.md) |
| [FAC-06](FAC-06/description.md) | Export du livre des ventes | 1 | And-Matia | [#76](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/76) 🟡 | [FAC-01](FAC-01/description.md), [SYS-01](SYS-01/description.md) | [☐](FAC-06/scenario-test.md) | [☐](FAC-06/guide.md) |

### 14 · Commissions

| Ticket | Titre | SP | Assigné(e) | GitHub | Dépend de | Scénario | Guide |
|---|---|---|---|---|---|---|---|
| [VTE-09](VTE-09/description.md) | Détail d'une vente | 1 | Kei-Shiro | [#70](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/70) 🟡 | [COM-02](COM-02/description.md), [VTE-01](VTE-01/description.md) | [☐](VTE-09/scenario-test.md) | [☐](VTE-09/guide.md) |
| [COM-01](COM-01/description.md) | Règles de commission par canal | 2 | ManaJudy | [#85](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/85) 🟡 | [AUTH-03](AUTH-03/description.md), [JNL-01](JNL-01/description.md), [SYS-05](SYS-05/description.md) | [☐](COM-01/scenario-test.md) | [☐](COM-01/guide.md) |
| [COM-02](COM-02/description.md) | Calcul automatique de la commission | 2 | And-Matia | [#86](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/86) 🟡 | [COM-01](COM-01/description.md), [SRV-02](SRV-02/description.md), [VTE-01](VTE-01/description.md), [SYS-05](SYS-05/description.md) | [☐](COM-02/scenario-test.md) | [☐](COM-02/guide.md) |
| [COM-03](COM-03/description.md) | Validation des commissions | 1 | Kei-Shiro | [#87](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/87) 🟡 | [COM-02](COM-02/description.md), [JNL-01](JNL-01/description.md) | [☐](COM-03/scenario-test.md) | [☐](COM-03/guide.md) |
| [COM-04](COM-04/description.md) | Suivi de commission et objectif | 1 | ManaJudy | [#88](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/88) 🟡 | [COM-02](COM-02/description.md) | [☐](COM-04/scenario-test.md) | [☐](COM-04/guide.md) |
| [COM-05](COM-05/description.md) | Recalcul et export des commissions | 1 | Kei-Shiro | [#89](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/89) 🟡 | [COM-02](COM-02/description.md), [SYS-01](SYS-01/description.md), [JNL-01](JNL-01/description.md) | [☐](COM-05/scenario-test.md) | [☐](COM-05/guide.md) |
| [COM-06](COM-06/description.md) | Ajout d'une règle de commission | 1 | And-Matia | [#90](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/90) 🟡 | [COM-01](COM-01/description.md), [JNL-01](JNL-01/description.md) | [☐](COM-06/scenario-test.md) | [☐](COM-06/guide.md) |

### 15 · Clôture de journée

| Ticket | Titre | SP | Assigné(e) | GitHub | Dépend de | Scénario | Guide |
|---|---|---|---|---|---|---|---|
| [JR-03](JR-03/description.md) | Suivi temps réel de la journée | 2 | ManaJudy | [#57](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/57) 🟡 | [LIV-02](LIV-02/description.md), [SRV-02](SRV-02/description.md), [VTE-01](VTE-01/description.md) | [☐](JR-03/scenario-test.md) | [☐](JR-03/guide.md) |
| [SRV-04](SRV-04/description.md) | Consommation salon dans la clôture | 2 | Kei-Shiro | [#84](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/84) 🟡 | [JR-04](JR-04/description.md), [SRV-02](SRV-02/description.md) | [☐](SRV-04/scenario-test.md) | [☐](SRV-04/guide.md) |
| [JR-04](JR-04/description.md) | Réconciliation automatique de clôture | 2 | Kei-Shiro | [#91](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/91) 🟡 | [LIV-04](LIV-04/description.md), [SHO-02](SHO-02/description.md), [SRV-02](SRV-02/description.md), [TRF-02](TRF-02/description.md), [VTE-01](VTE-01/description.md), [JNL-01](JNL-01/description.md) | [☐](JR-04/scenario-test.md) | [☐](JR-04/guide.md) |
| [JR-05](JR-05/description.md) | Motif obligatoire en cas d'écart | 2 | And-Matia | [#92](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/92) 🟡 | [JR-04](JR-04/description.md) | [☐](JR-05/scenario-test.md) | [☐](JR-05/guide.md) |
| [JR-06](JR-06/description.md) | Verrouillage de la clôture du jour | 2 | And-Matia | [#93](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/93) 🟡 | [JNL-01](JNL-01/description.md), [JR-05](JR-05/description.md) | [☐](JR-06/scenario-test.md) | [☐](JR-06/guide.md) |

### 16 · Rapports & tableau de bord

| Ticket | Titre | SP | Assigné(e) | GitHub | Dépend de | Scénario | Guide |
|---|---|---|---|---|---|---|---|
| [ART-06](ART-06/description.md) | Fiche article unifiée (360°) | 1 | Kei-Shiro | [#26](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/26) 🟡 | [JNL-01](JNL-01/description.md), [PRD-01](PRD-01/description.md), [PRO-01](PRO-01/description.md), [STK-01](STK-01/description.md) | [☐](ART-06/scenario-test.md) | [☐](ART-06/guide.md) |
| [DSH-01](DSH-01/description.md) | Cockpit journalier | 2 | Kei-Shiro | [#94](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/94) 🟡 | [JR-04](JR-04/description.md) | [☐](DSH-01/scenario-test.md) | [☐](DSH-01/guide.md) |
| [DSH-02](DSH-02/description.md) | Graphique du CA sur 7 jours | 1 | ManaJudy | [#95](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/95) 🟡 | [VTE-01](VTE-01/description.md) | [☐](DSH-02/scenario-test.md) | [☐](DSH-02/guide.md) |
| [DSH-03](DSH-03/description.md) | Alertes de stock sur le cockpit | 2 | And-Matia | [#96](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/96) 🟡 | [STK-04](STK-04/description.md) | [☐](DSH-03/scenario-test.md) | [☐](DSH-03/guide.md) |
| [DSH-04](DSH-04/description.md) | Répartition du CA par gamme et canal | 1 | Njara-Niaina | [#97](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/97) 🟡 | [ART-03](ART-03/description.md), [VTE-01](VTE-01/description.md), [SYS-05](SYS-05/description.md) | [☐](DSH-04/scenario-test.md) | [☐](DSH-04/guide.md) |
| [RPT-01](RPT-01/description.md) | Rapports filtrables journaliers/mensuels/annuels | 2 | Kei-Shiro | [#98](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/98) 🟡 | [VTE-01](VTE-01/description.md), [SYS-05](SYS-05/description.md) | [☐](RPT-01/scenario-test.md) | [☐](RPT-01/guide.md) |
| [RPT-02](RPT-02/description.md) | Indicateurs comparés à la période précédente | 2 | ManaJudy | [#99](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/99) 🟡 | [RPT-01](RPT-01/description.md) | [☐](RPT-02/scenario-test.md) | [☐](RPT-02/guide.md) |
| [RPT-03](RPT-03/description.md) | Graphique d'évolution du CA | 1 | And-Matia | [#100](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/100) 🟡 | [RPT-01](RPT-01/description.md) | [☐](RPT-03/scenario-test.md) | [☐](RPT-03/guide.md) |
| [RPT-04](RPT-04/description.md) | Répartition du CA par gamme | 1 | And-Matia | [#101](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/101) 🟡 | [ART-03](ART-03/description.md), [RPT-01](RPT-01/description.md), [SYS-05](SYS-05/description.md) | [☐](RPT-04/scenario-test.md) | [☐](RPT-04/guide.md) |
| [RPT-05](RPT-05/description.md) | Classement des produits les plus vendus | 1 | ManaJudy | [#102](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/102) 🟡 | [RPT-01](RPT-01/description.md) | [☐](RPT-05/scenario-test.md) | [☐](RPT-05/guide.md) |
| [RPT-06](RPT-06/description.md) | Export des rapports en PDF/XLSX | 2 | And-Matia | [#103](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/103) 🟡 | [RPT-01](RPT-01/description.md), [SYS-01](SYS-01/description.md) | [☐](RPT-06/scenario-test.md) | [☐](RPT-06/guide.md) |

### 17 · Finitions transverses

| Ticket | Titre | SP | Assigné(e) | GitHub | Dépend de | Scénario | Guide |
|---|---|---|---|---|---|---|---|
| [JNL-03](JNL-03/description.md) | Accès au journal complet | 1 | Kei-Shiro | [#104](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/104) 🟡 | [JNL-01](JNL-01/description.md) | [☐](JNL-03/scenario-test.md) | [☐](JNL-03/guide.md) |
| [SYS-03](SYS-03/description.md) | Recherche globale (palette de commandes) | 1 | ManaJudy | [#106](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/106) 🟡 | [ART-01](ART-01/description.md) | [☐](SYS-03/scenario-test.md) | [☐](SYS-03/guide.md) |
| [SYS-04](SYS-04/description.md) | Bascule thème clair/sombre | 1 | And-Matia | [#107](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/107) 🟡 | [AUTH-01](AUTH-01/description.md) | [☐](SYS-04/scenario-test.md) | [☐](SYS-04/guide.md) |

### —

| Ticket | Titre | SP | Assigné(e) | GitHub | Dépend de | Scénario | Guide |
|---|---|---|---|---|---|---|---|
| [DSG-01](DSG-01/description.md) | Maquettes Claude Design de toutes les pages | 2 | ManaJudy | [#128](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/128) 🟡 | — | [⚠](DSG-01/scenario-test.md) | [⚠](DSG-01/guide.md) |

Légende : ✅ fermé (colonne GitHub) ou dossier complet et vérifié (colonnes Scénario et Guide) · 🟡 ouvert · ☐ à compléter · ⚠ pré-rempli depuis le code, à vérifier.
