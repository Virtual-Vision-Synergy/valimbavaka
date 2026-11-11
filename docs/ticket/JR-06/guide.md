# Guide d'intégration — JR-06 · Verrouillage de la clôture du jour

Statut : ✅ écrit le 12/11/2026 — tests verts en CI le 12/11/2026 (voir « Résultat de l'exécution » du scénario) · Ticket : [#93](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/93) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

`POST /api/v1/days/closing/lock` verrouille la journée du jour. Le serveur relit la réconciliation, exige que **toutes les lignes soient comptées** (JR-05) et que **chaque écart ait son motif** sur le théorique d'aujourd'hui (une vente faite après le comptage change le théorique sans toucher le comptage enregistré), remplace les lignes de `day_closing_counts` dont le théorique a bougé (la clôture est figée sur ces lignes), passe la journée à `CLOSED` et écrit une ligne de journal qui nomme les écarts. Ensuite plus aucune vente, annulation, avoir, livraison réussie ni comptage n'est accepté pour cette date (409) ; la lecture reste possible. L'écran `/day/close` appelle ce verrouillage et affiche le message de confirmation.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie (`XxxRDto`) | Erreurs |
|---|---|---|---|---|---|
| POST | `/api/v1/days/closing/lock` | `days.close` (administrateur) | aucune (ni corps ni paramètre : le statut ne vient jamais du client) | 200 `DayClosingRDto` (la réconciliation de la journée clôturée, `status` `CLOSED`) | 401, 403, 409 (journée non ouverte, déjà clôturée, ou clôturée par une requête simultanée), 422 (ligne non comptée, écart sans motif) |

- Contrôleur : `feature/dayclosing/controller/DayClosingController.java` (méthode `lock`, même classe que JR-04 et JR-05, un seul appel de service).
- `GET /api/v1/days/closing` et `PUT /api/v1/days/closing/counts` : inchangés ; le second répond 409 « déjà clôturée » une fois la journée close.

### Services et ports Java réutilisables

| Port | Méthode | Rôle |
|---|---|---|
| `feature.dayclosing.service.DayClosingLockService` | `DayClosingRDto lock()` | Verrouillage complet, tout ou rien (`DayClosingLockServiceImpl.lock`). |
| `feature.day.service.BusinessDayService` | `BusinessDayRDto lockOpenDay()` | **Nouveau** : journée du jour lue sous verrou en écriture jusqu'à la fin de la transaction ; 409 si non ouverte ou déjà clôturée. |
| idem | `BusinessDayRDto closeDay()` | **Nouveau** : passe la journée du jour à `CLOSED` (sous verrou) ; 409 comme ci-dessus. Ne vérifie ni ne journalise rien : c'est le travail de l'appelant. |
| idem | `BusinessDayRDto requireOpenDay()` | Inchangé (409) ; le message distingue maintenant « n'est pas ouverte » de « est déjà clôturée ». |
| `feature.day.domain.BusinessDay` | `close()` | Statut `CLOSED` (`IllegalStateException` si la journée n'est pas ouverte). |
| `feature.day.repository.BusinessDayRepository` | `findByBusinessDateForUpdate(LocalDate)` | Lecture avec `PESSIMISTIC_WRITE`. |

Déroulé de `DayClosingLockServiceImpl.lock` (une transaction) :
1. `lockOpenDay()` : verrou de la journée ; un `saveCounts` simultané (JR-05, qui prend le même verrou en premier) attend, puis reçoit 409.
2. `DayClosingService.getClosing()` : réconciliation relue, théorique d'aujourd'hui.
3. Chaque ligne est rapprochée de son comptage enregistré : ligne sans comptage → 422 (liste bornée à 10 références) ; comptage ≠ théorique d'aujourd'hui sans motif → 422.
4. Les comptages dont le théorique a changé sont remplacés (`DayClosingCount.recount`) : théorique figé, motif conservé s'il y a encore un écart, effacé sinon (contrainte `ck_day_closing_counts_reason_gap`).
5. `closeDay()`, puis une ligne de journal `UPDATE` sur `BUSINESS_DAY`, puis relecture de la réconciliation.

### Ce qui est refusé une fois la journée `CLOSED`

Vérifié dans le code existant, rien n'a été recodé :
- **Vente** (`SaleServiceImpl.record` et `createSale`, donc aussi la vente d'une livraison réussie LIV-03 et la vente au comptoir) : `BusinessDayService.requireOpenDay()` → 409.
- **Annulation de vente** (`SaleServiceImpl.cancel`), donc **avoir FAC-04** (`CreditNoteServiceImpl`) et **annulation de livraison LIV-05** (`DeliveryUndoServiceImpl`) : `requireOpenDay()` ; 409 aussi si la vente appartient à une autre journée que la journée ouverte.
- **Comptage du soir** (`DayClosingCountServiceImpl.saveCounts`) : 409 « déjà clôturée ».
- **Ouverture** : `openDay` refuse déjà une journée déjà ouverte pour la date (409), close ou non ; il n'existe pas de réouverture.

### Frontend (`features/day`)

- `data-access/day-service.ts` : `lock()` → `POST /api/v1/days/closing/lock` par la file hors ligne (`OutboxService.submit`, sans corps) ; renvoie `SubmitResult<DayClosing>` (`body` = réconciliation de la journée clôturée). `saveCounts` et `lock` partagent `toClosingResult`. L'ancienne simulation (`DayLockWDto`, `ClosingState.toRequest`) est supprimée.
- `data-access/day-models.ts` : `DayClosing.status`, `ClosingLine.saved` (le serveur a déjà un comptage pour la ligne).
- `data-access/closing-state.ts` : `unsaved` (lignes dont le comptage ou le motif affichés diffèrent de ceux enregistrés).
- `pages/day-close-page` : `canLock` = journée ouverte, aucun motif manquant, `unsaved` à 0 ; `locked` (statut `CLOSED`) désactive les saisies, cache le bouton et affiche le bandeau ; `confirmLock` appelle `lock()`, affiche le toast de confirmation ou, en erreur, une alerte `role="alert"` ; hors ligne, toast « Saisie enregistrée hors connexion ».
- Traductions (fr, mg) : `day.close.lockHint` et `day.close.lockedToast` modifiées, `day.close.lockedBannerNoTime` ajoutée.

### Données

- **Aucune migration** : le statut `CLOSED` et sa contrainte `ck_business_days_status` existent depuis JR-01 ; le verrouillage réutilise `day_closing_counts` (JR-05).
- Journal : une ligne `UPDATE` sur `BUSINESS_DAY` (id de la journée) par verrouillage réussi : « Clôture de la journée du 11/11/2026 : 12 ligne(s) comptée(s), 2 écart(s) (ART-0031 -2 LOSS, ART-0032 +1 ENTRY_ERROR) » (10 écarts nommés au plus, puis « et N autre(s) » ; libellé tronqué à 500 caractères par JNL-01). Aucune ligne pour un refus.
- Aucune nouvelle permission : `days.close` (JR-04).

## Règles métier à respecter par les autres tickets

- **Écriture datée d'une journée** : une feature qui enregistre une opération rattachée à la journée du jour appelle `BusinessDayService.requireOpenDay()` (409 si close). C'est le seul point de contrôle.
- **Écart** = `counted − theoretical`, recalculé au verrouillage sur le théorique d'aujourd'hui ; les lignes de `day_closing_counts` d'une journée `CLOSED` sont définitives (théorique figé au verrouillage).
- **Journée close** : `DayClosingService.getClosing()` et `GET /api/v1/days/live` restent lisibles ; `status` vaut `CLOSED`.
- **Pas de réouverture** : non demandée par le ticket, non livrée. Corriger une journée close passe par une contre-opération du lendemain (texte de l'écran).
- **Lendemain** : l'ouverture (JR-01 / JR-02) ne dépend pas du statut de la veille ; la reprise des quantités lit les transferts du matin de la dernière journée ouverte avant aujourd'hui, close ou non.
- **Droit** : `days.close` (administrateur seul).

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/`)
- `dayclosing/controller/DayClosingController.java` ; `dayclosing/service/DayClosingLockService.java` ; `dayclosing/service/impl/DayClosingLockServiceImpl.java` ; `dayclosing/service/impl/DayClosingCountServiceImpl.java` (prend le verrou de la journée avant de lire)
- `day/domain/BusinessDay.java` (`close`) ; `day/repository/BusinessDayRepository.java` ; `day/service/BusinessDayService.java` ; `day/service/impl/BusinessDayServiceImpl.java`

**Frontend** (`valimbavaka-ui/src/app/`)
- `features/day/data-access/day-models.ts`, `day-service.ts`, `closing-state.ts` ; `features/day/pages/day-close-page/` (`.ts`, `.html`) ; `public/i18n/fr.json`, `mg.json`

**Tests**
- Backend : `feature/dayclosing/controller/DayClosingLockIT.java`, `feature/dayclosing/service/impl/DayClosingLockServiceImplTest.java`, `feature/day/service/impl/BusinessDayServiceImplTest.java`, `DayClosingCountServiceImplTest.java` (constructeur)
- Frontend : `features/day/pages/day-close-page/day-close-lock.spec.ts`, `features/day/data-access/day-service.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** :
- [JR-05](../JR-05/guide.md) : comptages et motifs (`day_closing_counts`, `DayClosingCountRepository`) ; [JR-04](../JR-04/guide.md) : réconciliation, permission `days.close` ; [JR-01](../JR-01/guide.md) / [JR-02](../JR-02/description.md) : journée, statuts, ouverture du lendemain ; [JNL-01](../JNL-01/guide.md) : journal ; [SYS-07](../SYS-07/guide.md) : file hors ligne.

**Ces tickets s'appuient sur lui** :
- aucun ; [SRV-04](../SRV-04/description.md) (consommation salon dans la clôture) n'est pas livré ici.

## Comment l'utiliser depuis un autre ticket

```java
businessDayService.requireOpenDay();   // 409 si la journée du jour est close ou absente : à appeler avant d'écrire
```

```ts
this.dayService.lock(); // Observable<SubmitResult<DayClosing>> : body.status === 'CLOSED'
```

## Points d'attention

- **Hypothèse (verrouillage = comptage complet)** : le critère parle d'un message de confirmation et de l'écart consigné ; le ticket n'en dit pas plus. Le verrouillage exige que toutes les lignes de la réconciliation soient comptées et motivées, comme le laissait entendre JR-05. À valider par le PO.
- **Hypothèse (« écart consigné au journal »)** : une seule ligne de journal par verrouillage, avec les écarts nommés (référence, quantité signée, motif), plutôt qu'une ligne par écart. Les motifs y apparaissent par leur code (`LOSS`, `BREAKAGE`…).
- **Hypothèse (comptage à enregistrer avant)** : l'écran envoie le verrouillage seulement quand le comptage affiché est celui enregistré (`unsaved` à 0) ; il ne l'enregistre pas à la place de l'administrateur.
- **Hypothèse (pas de réouverture)** : aucune route ne rouvre une journée close ; non demandé.
- **Trous non comblés** : seules les écritures qui passent par `requireOpenDay()` sont refusées après le verrouillage (vente, annulation, avoir, annulation de livraison, livraison réussie, comptage). Les écritures non rattachées à une journée ne le sont pas : sortie shooting et son retour (SHO), prestation salon (SRV-02), transfert (TRF), réception, production, inventaire. Les refuser toucherait à des gestes physiques (un retour de shooting ne se refuse pas) et à plusieurs tickets ; à décider avec le PO (un garde « jamais sur une journée close » dans la sortie shooting et la prestation salon serait peu coûteux).
- **Course vente / verrouillage** : une vente validée entre la relecture et le passage à `CLOSED` n'est pas dans le comptage figé ; la fenêtre est celle d'une transaction. Le verrou de la journée n'est pas pris par la vente (elle lit sans verrou).
- **Reste en démonstration** : la caisse (espèces attendues / comptées) et la liste de contrôle (livraisons soldées, tickets en attente, shootings en retard) ne viennent pas du serveur, ne bloquent pas le verrouillage et la route `/day/close` garde `underConstruction`. Consommation salon dans la clôture : SRV-04, non touché.
- **Traduction malgache** : les trois textes ajoutés en `mg.json` sont à faire relire.
- **Base de test partagée** : `DayClosingLockIT` clôture la journée du jour ; elle la remet à `OPEN` et supprime les lignes de comptage après chaque test. Toute nouvelle classe qui clôture la journée doit faire de même.
- La recette manuelle 🧪 reste à passer (six étapes, voir le scénario).
