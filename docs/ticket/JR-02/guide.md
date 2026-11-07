# Guide d'intégration — JR-02 · Reprise des quantités de la veille

Statut : ☐ écrit le 2026-11-08, tests à lancer en CI (voir « Résultat de l'exécution » du scénario) · Ticket : [#56](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/56) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

La grille d'ouverture de JR-01 porte désormais, pour chaque article, les **quantités envoyées à chaque destination le dernier jour ouvré** (lues dans les lignes de ses transferts du matin). Le bouton « Reprendre les quantités d'hier » de l'écran remplace la saisie par ces quantités ; l'administrateur ajuste puis valide comme d'habitude (JR-01). Le ticket ajoute aussi au module transfert un port de lecture des transferts par identifiant, réutilisable par d'autres features.

## Contrat exposé

### Endpoints backend

Aucun nouvel endpoint. Le contrat de `GET /api/v1/days/opening` (JR-01, permission `days.open`) est **étendu** :

| Méthode | URL | Permission (`@HasPermission`) | Entrée (`XxxWDto`) | Sortie (`XxxRDto`) | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/days/opening` | `days.open` (administrateur) | — | 200 `DayOpeningRDto` : chaque `DayOpeningLineRDto` a un champ `yesterday` | 401, 403 |

- `DayOpeningLineRDto.yesterday` : liste de `DayQuantityRDto` (`locationId`, `quantity`), une entrée par destination servie la veille pour cet article (ordre non garanti) ; **vide** si l'article n'a pas été envoyé, s'il n'y a pas de veille ou si la veille a été ouverte sans transfert.
- `DayOpeningRDto.previousDay` (déjà là depuis JR-01) : date de cette veille (`null` la toute première fois).

### Services et ports Java réutilisables

| Port | Méthode | Rôle |
|---|---|---|
| `feature.transfer.service.TransferService` (`TransferService.java:44`) | `List<TransferRDto> getTransfersByIds(Collection<UUID> ids)` | Transferts avec leurs lignes, par identifiant ; ids inconnus ignorés, doublons fusionnés, ordre non garanti, liste vide sans requête si aucun id. Pas d'endpoint ni de contrôle de permission : réservé aux autres features (TRF-04 pourra s'en servir pour le détail, JR-04 pour la composante « transféré »). |
| `TransferRepository.findAllByIdIn` (`TransferRepository.java:25`) | | Lecture groupée (même forme que `SupplierRepository.findAllByIdIn`). |

Côté `day` (package-private) : `service/impl/PreviousDayQuantities.java` (`of(BusinessDay)` l.31) construit « article → quantités par destination » ; `DayOpeningGrid.build` (l.58) reçoit désormais la journée de la veille (entité) au lieu de sa seule date, et `BusinessDayServiceImpl.getOpening` (l.65) la lui passe.

### Frontend

- `data-access/day-models.ts` : type serveur `DayQuantityRDto`, champ `DayOpeningLineRDto.yesterday` ; `toOpeningGrid` remplit `DispatchLineRDto.yesterday` avec `toQuantities` (l.222) : emplacement → colonne via `destinationIds` (Boutique / Livraison / Salon), un emplacement sans colonne est ignoré.
- `pages/day-open-page` : `useYesterday()` (l.129) inchangé dans son principe (remplace toute la saisie, y compris les lignes sans envoi la veille, remises à 0) ; le bouton est **désactivé** quand `previousDay` est `null` (`day-open-page.html:51`).
- Aucune nouvelle route, permission ni traduction.

### Données

- Pas de migration (la plage Liquibase 20 à 39 de la voie n'est pas utilisée) : tout se lit dans `business_day_transfers` (JR-01) et `transfer_lines` (TRF-03).
- Pas d'écriture, donc rien au journal ; la validation reste celle de JR-01.

## Règles métier à respecter par les autres tickets

- **Dernier jour ouvré** = dernière journée **ouverte** avant aujourd'hui (`BusinessDayRepository.findFirstByBusinessDateLessThanOrderByBusinessDateDesc`), pas la date calendaire d'hier : le lundi, on reprend le vendredi (ou le samedi s'il a été ouvert).
- Les quantités reprises sont celles des **transferts du matin** de cette journée (ce qui est sorti de l'entrepôt), pas les ventes ni le reste du soir (reste de la veille : JR-04 / JR-06).
- Un article envoyé la veille mais absent de la grille du jour (plus de stock à l'entrepôt, désactivé, non fini) n'est pas repris. Une quantité reprise supérieure au stock du jour est appliquée telle quelle : l'écran marque la ligne en erreur et bloque la validation (règle JR-01), l'administrateur corrige.
- Droits : ceux de la grille, `days.open` (administrateur).

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/`)
- `day/dto/DayQuantityRDto.java` (nouveau), `day/dto/DayOpeningLineRDto.java` (champ `yesterday`)
- `day/service/impl/PreviousDayQuantities.java` (nouveau), `DayOpeningGrid.java`, `BusinessDayServiceImpl.java`
- `transfer/service/TransferService.java`, `transfer/service/impl/TransferServiceImpl.java`, `transfer/repository/TransferRepository.java`

**Frontend** (`valimbavaka-ui/src/app/features/day/`)
- `data-access/day-models.ts` ; `pages/day-open-page/day-open-page.ts`, `.html`

**Tests**
- Backend : `feature/day/controller/BusinessDayIT.java` (2 tests), `feature/day/service/impl/PreviousDayQuantitiesTest.java` (nouveau, 3 tests), `feature/transfer/service/impl/TransferServiceImplTest.java` (2 tests)
- Frontend : `features/day/data-access/day-service.spec.ts` (1 test), `features/day/pages/day-open-page/day-open-page.spec.ts` (2 tests)

## Dépendances

**Ce ticket s'appuie sur** :
- [JR-01](../JR-01/guide.md) : journée, grille d'ouverture, `previousDay`, résumé `business_day_transfers`
- [TRF-03](../TRF-03/guide.md) : transferts et leurs lignes

**Ces tickets s'appuient sur lui** :
- _(aucun dans le backlog)_ ; le port `TransferService.getTransfersByIds` peut servir à TRF-04 et JR-04.

## Comment l'utiliser depuis un autre ticket

Lire des transferts connus par leur id, depuis un service d'une autre feature :

```java
var transfers = transferService.getTransfersByIds(dayTransferIds); // lignes incluses, ids inconnus ignorés
```

Côté interface, la grille fournit déjà `line.yesterday` (`DispatchQuantities`, par colonne).

## Points d'attention

- **Hypothèse (« hier »)** : le critère dit « dernier jour ouvré » ; on prend la dernière journée ouverte avant aujourd'hui, quelle que soit sa date (week-end, jour férié, fermeture).
- **Hypothèse (quantités reprises)** : celles des transferts du matin, telles qu'enregistrées ; si un jour l'annulation d'un transfert (JNL-02) arrive, un transfert annulé devra être exclu (filtrer sur le statut dans `PreviousDayQuantities`).
- **Hypothèse (remplacement)** : « Reprendre hier » remplace toute la saisie en cours (comportement de la maquette), il ne l'additionne pas.
- **Hypothèse (stock insuffisant)** : on n'écrête pas au stock du jour ; l'écran signale l'écart et l'administrateur corrige (écrêter masquerait la rupture).
- **Contrat étendu** : `DayOpeningLineRDto` gagne un champ ; un client qui construirait ce record en Java (aucun aujourd'hui en dehors de `DayOpeningGrid`) doit le fournir.
- Quantités décimales (3 décimales côté serveur) : l'écran les affiche telles quelles, mais sa saisie manuelle reste entière (comportement JR-01).
- Tests à lancer en CI ; la recette manuelle (🧪) reste à passer.
