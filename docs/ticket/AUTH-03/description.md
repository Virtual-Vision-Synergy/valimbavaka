# AUTH-03 · Création et modification d'un utilisateur

> Ticket GitHub [#12](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/12) · Fermé · Milestone : 01 · Authentification & socle technique · 2 SP · Assigné(e) : And-Matia · Labels : needs-info

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux créer et modifier un utilisateur (nom, rôle, canal, taux commission) afin de gérer mon équipe.

## Dépendances

- Bloqué par `AUTH-01` (#10)
- Bloqué par `JNL-01` (#14)
- Bloqué par `SYS-05` (#111)
- Bloqué par `JNL-02` (#15)

## Critères d'acceptation

- [ ] Le rôle choisi détermine la coquille applicative de l'utilisateur.

<img width="1906" height="923" alt="Image" src="https://github.com/user-attachments/assets/4a376ff1-1023-4190-a1e9-70437bf3bceb" />

- [ ] Un formulaire permet de saisir/modifier ces champs.

<img width="1917" height="911" alt="Image" src="https://github.com/user-attachments/assets/70c4c6de-e5b9-414a-9ff5-dd51bfdf3acd" />

- [ ] Le formulaire inclut aussi le canal de vente et le taux de commission de l'utilisateur.

- [ ] Les rôles proposés sont exactement Administrateur, Vendeuse, Livreur (SYS-05).

- [ ] La création et la modification des utilisateurs sont réservées à l'administrateur, contrôlé côté serveur (pas seulement en masquant l'interface).

- [ ] Un utilisateur ne se supprime jamais : il se désactive (AUTH-04), conformément à JNL-02.

## Détails

| | |
|---|---|
| **ID Ticket** | `AUTH-03` |
| **Module** | Authentification |
| **Story points** | 2 |
| **Utilisateur concerné** | Administrateur |

A jouter champ de taux de commission

> Question client en attente : la commission est-elle propre à l'utilisateur (ce ticket), au canal (COM-01) ou au service salon (SRV-01) ? Et la connexion se fait-elle par email ou par identifiant/téléphone ?

## Ce ticket débloque

- [AUTH-04](../AUTH-04/description.md) · Activation / désactivation d'un utilisateur
- [LIV-01](../LIV-01/description.md) · Création d'une commande à livrer
- [VTE-01](../VTE-01/description.md) · Enregistrement d'une vente multicanal
- [COM-01](../COM-01/description.md) · Règles de commission par canal

## Dépend de

- [AUTH-01](../AUTH-01/description.md) · Connexion selon le rôle attribué (fermé)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)
- [SYS-05](../SYS-05/description.md) · Rôles métier, référentiels et compte administrateur initial (fermé)
- [JNL-02](../JNL-02/description.md) · Interdiction de suppression des opérations validées (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/user`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/user`

## Échanges sur le ticket

**And-Matia** :

> Relecture du code AUTH-03 (PR #110) par rapport aux règles métier, en complément des critères ajoutés au ticket :
>
> **À corriger dans ce ticket**
> - **Rôles** : le changelog crée `ADMIN` et `USER`, alors que les rôles réels sont Administrateur, Vendeuse et Livreur (voir #111 SYS-05). Il faut corriger avec un **nouveau** changeset, sans modifier `2026_10_07-01`, déjà appliqué.
> - **Champs manquants** : le canal de vente et le taux de commission (`User`, DTO, formulaire).
> - **Accès admin** : aucune restriction de rôle sur `UserController` (ni `@HasPermission`, ni `@PreAuthorize`). Le critère d'acceptation du ticket l'exige : création et modification « réservées à l'administrateur, contrôlé côté serveur ». Une fois la connexion en place (AUTH-01), n'importe quel utilisateur connecté pourrait créer des comptes ou changer son propre rôle. Il faut `@HasPermission` (ou équivalent) côté serveur.
> - **`DELETE /api/v1/users/{id}`** supprime vraiment l'utilisateur en base (`deleteById`). Le critère d'acceptation d'AUTH-03 dit déjà : « Un utilisateur ne se supprime jamais : il se désactive (AUTH-04), conformément à JNL-02 ». Ça cassera aussi les références dès qu'il y aura des ventes. Il faut retirer cet endpoint (et le bouton de suppression de l'UI) et utiliser `active=false`.
>
> **Précision sur le périmètre**
> Ces deux derniers points ne sont pas des lacunes hors périmètre. La protection des routes (`SecurityConfig`, `@EnableMethodSecurity`, annotation `HasPermission`) existe déjà dans le socle, et les deux critères ci-dessus figurent dans le ticket AUTH-03 lui-même, qui référence AUTH-04 et JNL-02. Seule l'implémentation complète du soft delete (JNL-02, #15, encore ouvert) et de la désactivation (AUTH-04) vient après. Pour AUTH-03, il suffit de ne pas exposer de suppression définitive et d'ajouter le contrôle admin. Si l'équipe préfère reporter ces points, il faut le décider explicitement et mettre à jour le ticket ou créer un ticket de suivi.
>
> **À savoir**
> - Pour l'instant, toutes les routes renvoient **401** : `SecurityConfig` exige d'être authentifié et le `JwtFilter` est encore vide. C'est pour ça que l'ordre est maintenant AUTH-01 → AUTH-03 : sans connexion, ce ticket ne peut pas passer en QA.
> - Aucun test sur le module `iam`. Il en faudrait au minimum sur : email en double (409), rôle introuvable (404), et accès refusé à un non-admin (403).
> - Le reste est propre : DTO séparés, MapStruct, BCrypt, contrôle d'unicité de l'email, URL relatives côté Angular (compatibles avec le proxy nginx du déploiement).
