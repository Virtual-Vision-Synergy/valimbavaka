# Scénarios de test — LIV-08 · Fonctionnement hors ligne de la tournée

Statut : ✅ CI verte le 2026-11-11 · Ticket : [#53](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/53) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests frontend Vitest sur le comportement visible. Ce ticket est **frontend seul** : aucun
endpoint, aucune migration, aucun changement backend (voir « Écart » dans le guide).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `DELIVERY_DRIVER` (la tournée) ; `ADMIN` voit les mêmes écrans |
| Données de départ | tournée du 2026-11-09 de Tojo : 3 arrêts à faire (`LIV-0001` à `LIV-0003`), journée ouverte, articles transférés en Livraison |
| Tickets à avoir déployés avant | LIV-03, LIV-04, LIV-05, SYS-02, SYS-07, SYS-10 |

## Scénarios issus des critères d'acceptation

| # | Critère d'acceptation | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Les actions hors connexion se synchronisent automatiquement au retour du réseau (s'appuie sur SYS-02). | Réseau coupé, le livreur touche « Livrée » puis « Échec » : après 5 s les saisies vont dans la file ; réseau revenu | La file part seule, dans l'ordre (SYS-10), sans action du livreur ; héritage LIV-03/04/05 | `DeliveryServiceSpec` : « marque la livraison livrée par la file hors ligne, sans corps… », « envoie l’échec et son motif par la file hors ligne… » ; `outbox-service.spec.ts` (SYS-02) ; `sync-order.spec.ts` (SYS-10) | ✅ auto |
| 1b | idem : la tournée reste juste pendant l'attente | Une saisie attend le réseau, la tournée est relue (retour sur la page) | L'arrêt reste « livré » ou « en échec » avec son motif, pas « à faire » | `delivery-queue.spec.ts` : « montre livrée une commande dont le marquage attend le réseau, sans toucher aux autres », « montre en échec avec son motif une commande dont l’échec attend le réseau » ; `delivery-service.spec.ts` : « reporte sur la tournée lue les saisies qui attendent encore le réseau (LIV-08) » | ✅ auto |
| 1c | idem : au retour du réseau l'écran montre l'état du serveur | La file passe de 2 saisies à 0 | La tournée est relue une fois (une saisie refusée par le serveur réapparaît « à faire ») | `driver-round-page.spec.ts` : « relit la tournée quand la file hors ligne se vide au retour du réseau » | ✅ auto |
| 1d | idem : rien ne se perd si l'onglet se ferme dans les 5 s | « Livrée » touché, onglet fermé avant la fin du délai d'annulation | La saisie part dans la file à la fermeture (`pagehide`) | `driver-round-page.spec.ts` : « envoie tout de suite la saisie en attente quand la page est masquée (onglet fermé) » | ✅ auto |

## Sécurité et erreurs

Aucun endpoint ajouté : les contrôles 401/403/404/409 sont ceux de LIV-03/04/05 (`DeliveryCompletionIT`,
`DeliveryFailureIT`, `DeliveryUndoIT`). Une saisie refusée au retour du réseau reste `FAILED` dans la file
(écran Synchronisation, SYS-09) et la tournée relue montre l'arrêt « à faire ».

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test auto |
|---|---|---|---|
| Aucune saisie en file | tournée relue en ligne | arrêts inchangés | `delivery-queue.spec.ts` : « garde les arrêts tels quels quand rien ne attend le réseau » |
| Annulation (LIV-05) en file derrière « Livrée » | « Livrée » puis annuler, réseau coupé | l'arrêt reprend le statut du serveur (`ON_ROUND`) | `delivery-queue.spec.ts` : « rend le statut du serveur quand une annulation suit le marquage en file » |
| Motif inconnu en file | `reason: 'BIZARRE'` | affiché « autre » | `delivery-queue.spec.ts` : « retombe sur « autre » quand le motif en file est inconnu » |
| Saisie qui n'est pas une action de tournée | `POST /deliveries`, `PUT …/deliver`, autre préfixe | ignorée | `delivery-queue.spec.ts` : « ignore les saisies qui ne sont pas des actions de tournée » |
| Pas de saisie en file depuis l'ouverture | file à 0 | pas de relecture inutile | `driver-round-page.spec.ts` : « ne relit pas la tournée tant qu’aucune saisie n’a attendu le réseau » |
| Rejeu | même `Idempotency-Key` | une seule vente / un seul retour | déjà couvert LIV-03/04 (`DeliveryCompletionIT`, `DeliveryFailureIT`) |
| Changement de compte | autre livreur sur le même téléphone | file filtrée par compte (`OutboxService.pending`) | `outbox-session.spec.ts` (SYS-02) |

## Tests existants

`valimbavaka-ui/src/app/features/deliveries/data-access/delivery-queue.spec.ts` (nouveau),
`delivery-service.spec.ts`, `pages/driver-round-page/driver-round-page.spec.ts`, `core/offline/*.spec.ts`.

## Recette manuelle (🧪)

1. 🧪 Sur téléphone, ouvrir `/round`, couper le réseau (mode avion), marquer deux arrêts : bandeau « hors ligne » et compteur de saisies en attente.
2. 🧪 Recharger la page réseau coupé n'est pas garanti (pas de service worker, voir guide) ; rester dans l'application.
3. 🧪 Rétablir le réseau : le compteur retombe à 0 sans action et les arrêts restent livrés / en échec.

## Résultat de l'exécution

- Date : 2026-11-11 (commit `f0a2da8f`)
- Environnement : CI GitHub (ci.yml frontend et backend, conventions.yml, Qodana : tous verts, 0 annotation Qodana) ; `ng test` non lancé en local (règle n°3)
- Anomalies ouvertes : aucune connue ; 3 étapes 🧪 restantes
