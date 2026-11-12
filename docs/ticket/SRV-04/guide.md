# Guide d'intégration — SRV-04 · Consommation salon dans la clôture

Statut : ✅ rédigé le 2026-11-12 — tests exécutés en CI, tous verts, voir le [scénario](scenario-test.md) · Ticket : [#84](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/84) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

La colonne « Salon » de la clôture (JR-04) additionne déjà les mouvements `TRADE_SHOW` écrits par les prestations (SRV-02) : le critère « le total consommé en salon est la valeur utilisée en clôture » était tenu par construction, sans être vérifié. SRV-04 ajoute une lecture de rapprochement, `GET /api/v1/days/closing/salon` : par article, ce que les prestations du jour ont consommé, ce que la clôture compte en « Salon », l'écart, les deux totaux et un indicateur `matches`. Aucune migration, aucune écriture.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/days/closing/salon` | `days.close` (ADMIN) | aucune | 200 `ClosingSalonRDto` | 401, 403, 409 (journée du jour non ouverte) |

- `ClosingSalonRDto` : `businessDate`, `servicesCount` (prestations du jour), `totalConsumed` (somme des produits des prestations), `totalInClosing` (somme de la colonne « Salon » de la clôture), `matches`, `lines`.
- `ClosingSalonLineRDto` : `articleId`, `reference`, `name`, `consumed`, `inClosing`, `gap` (= `inClosing − consumed`), quantités positives, lignes triées par nom puis référence.
- Contrôleur : `feature/dayclosing/controller/DayClosingController.java` (méthode `getSalonConsumption`).

### Services et ports Java réutilisables

- `DayClosingSalonService.getSalonConsumption()` (implémentation `service/impl/DayClosingSalonServiceImpl.java`) : lit la clôture par `DayClosingService.getClosing()` (donc exactement la valeur affichée), les prestations par `ServiceRecordService.getSalonConsumption(from, to)` et `getServiceRecordsBetween(from, to)` (SRV-02), les noms par `ArticleService.getArticlesByIds`. `dayclosing` dépend de `salonservice`, jamais l'inverse (pas de cycle).

### Frontend

- Aucun changement : la colonne « Salon » de `/day/close` existe depuis JR-04. L'écran ne consomme pas encore ce rapprochement (voir « Points d'attention »).

### Données

- Aucune migration. Aucune écriture au journal (lecture seule).

## Règles métier à respecter par les autres tickets

- Jour métier : minuit à minuit à Madagascar (`DayClosingServiceImpl.BUSINESS_ZONE`), le même que la clôture ; la date est celle de la journée ouverte, aucun paramètre `date`.
- `matches` est vrai quand **chaque** article a la même quantité des deux côtés ; un écart positif signale une sortie `TRADE_SHOW` sans prestation, un écart négatif une prestation absente de la colonne.
- Le rapprochement ne bloque rien : le verrouillage (JR-06) ne le consulte pas.

## Fichiers concernés

- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/dayclosing/{controller/DayClosingController.java, dto/ClosingSalonRDto.java, dto/ClosingSalonLineRDto.java, service/DayClosingSalonService.java, service/impl/DayClosingSalonServiceImpl.java}`
- Tests : `feature/dayclosing/service/impl/DayClosingSalonServiceImplTest.java`, `feature/salonservice/controller/ClosingSalonConsumptionIT.java` (dans le paquet `salonservice` pour réutiliser `ServiceRecordApiTestSupport`).

## Dépendances

**Ce ticket s'appuie sur** :
- [JR-04](../JR-04/description.md) · [guide](../JR-04/guide.md)
- [SRV-02](../SRV-02/description.md) · [guide](../SRV-02/guide.md)

**Ces tickets s'appuient sur lui** :
- _(rien)_

## Comment l'utiliser depuis un autre ticket

```text
GET /api/v1/days/closing/salon   (jeton ADMIN)  ->  { servicesCount, totalConsumed, totalInClosing, matches, lines[] }
```

## Points d'attention

- **Prestation après verrouillage** (relevé de JR-06) : l'enregistrement d'une prestation ne passe pas par `requireOpenDay()`, donc une prestation reste acceptée une fois la journée `CLOSED`. La description de SRV-04 ne le demande pas : comportement **inchangé**, à décider par le PO (refuser en 409, ou accepter et rattacher au jour suivant).
- **Écran** : le rapprochement n'est pas encore affiché dans `/day/close` (la colonne « Salon » l'est). Hypothèse : le critère est un contrôle de cohérence côté serveur ; un encart « Consommation salon » reste possible.
- Le guide SRV-02 recommande de ne pas sommer tous les mouvements `TRADE_SHOW` ; la colonne de JR-04 le fait (`ClosingTally`, cas `TRADE_SHOW`). Aujourd'hui seule une prestation écrit ce type ; si un autre ticket l'utilise, `matches` passera à faux et l'écart l'indiquera.
- Les prestations sont lues sur leur date de création et les mouvements sur leur date d'écriture : une prestation enregistrée à la milliseconde de minuit pourrait être comptée d'un côté seulement.
