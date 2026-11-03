# Guide d'intégration — FRS-04 · Délai de réapprovisionnement selon le type de fournisseur

Statut : ✅ rédigé (tests à exécuter) · Ticket : [#20](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/20) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Chaque fournisseur porte un délai de réapprovisionnement en jours (`leadTimeDays`, de 0 à 365). S'il n'est pas saisi, il prend le défaut de son type : 7 jours pour `LOCAL`, 30 jours pour `FOREIGN`. Les alertes de seuil (STK-04) affichent maintenant le fournisseur de l'article, son délai et la date de livraison prévue si l'on commande aujourd'hui. Le délai sert aussi à départager deux alertes de même urgence.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée (`XxxWDto`) | Sortie (`XxxRDto`) | Erreurs |
|---|---|---|---|---|---|
| `POST` / `PUT` | `/api/v1/suppliers`, `/api/v1/suppliers/{id}` | inchangées (FRS-01) | `SupplierWDto` + `leadTimeDays` (`Integer`, optionnel, `@Min(0)` `@Max(365)`) | `SupplierRDto` + `leadTimeDays` (`int`, toujours rempli) | 422 si hors bornes ; le reste comme FRS-01 |
| `GET` | `/api/v1/suppliers`, `/api/v1/suppliers/{id}` | inchangées | — | `SupplierRDto` avec `leadTimeDays` | comme FRS-01 |
| `GET` | `/api/v1/stocks/alerts` | inchangée (STK-04) | — | `StockAlertRDto` + `supplierId`, `supplierName`, `leadTimeDays`, `expectedDeliveryDate` (`yyyy-MM-dd`), tous `null` pour un article sans fournisseur | comme STK-04 |

Références : `feature/supplier/dto/SupplierWDto.java:43`, `feature/supplier/dto/SupplierRDto.java:20`, `feature/stock/dto/StockAlertRDto.java`.

### Services et ports Java réutilisables

- `SupplierType.defaultLeadTimeDays()` (`feature/supplier/domain/SupplierType.java:15`) : défaut du type, seule source de vérité côté serveur.
- `SupplierService.getLeadTimes(Collection<UUID>)` (`feature/supplier/service/SupplierService.java:36`) : lecture groupée, une seule requête (`SupplierRepository.findAllByIdIn`), qui renvoie `Map<UUID, SupplierLeadTimeRDto>` (`id`, `name`, `type`, `leadTimeDays`). Les fournisseurs désactivés sont inclus ; un identifiant inconnu est absent de la map ; une collection vide ne fait aucune requête.
- `StockAlertPolicy.toAlert(stock, supplier, today)` (`feature/stock/service/impl/StockAlertPolicy.java:45`) : `expectedDeliveryDate = today + leadTimeDays`.
- `StockAlertPolicy.MOST_URGENT_FIRST` (`StockAlertPolicy.java:33`) : couverture (stock / seuil) croissante, puis délai décroissant (sans fournisseur = 0), puis code.

### Frontend

- Routes : inchangées (`/suppliers/new`, `/suppliers/:id/edit`, page Stocks).
- `data-access` : `SupplierRDto.leadTimeDays`, `SupplierWDto.leadTimeDays`, constantes `DEFAULT_LEAD_TIME_DAYS` et `MAX_LEAD_TIME_DAYS` (`features/supplier/data-access/supplier-models.ts`) ; `StockAlertRDto` complété (`features/stocks/data-access/stock-models.ts`).
- Fiche fournisseur (`supplier-form-page.ts:83`, `:166`) : champ numérique obligatoire, prérempli à 7. Tant qu'il est resté au défaut de l'ancien type (ou vide), changer le type le fait passer au défaut du nouveau type ; un délai saisi à la main est gardé. Une fiche relue ne voit pas son délai remplacé (`setValue` sans émission, `:190`).
- Détail fournisseur (`suppliers/components/supplier-detail/supplier-detail.html:94`) : ligne « Délai de réapprovisionnement · N jours ».
- Alertes (`stocks/components/threshold-alerts/threshold-alerts.html:62`) : colonne « Fournisseur · délai » (nom, « N j · livraison prévue le JJ/MM/AAAA ») ou « Fabrication interne (atelier) ».
- Traductions : `supplier.field.leadTimeDays`, `supplier.form.leadTimeHint`, `supplier.form.leadTimeInvalid`, `suppliers.leadTime`, `suppliers.leadTimeValue`, `stocks.thresholdAlerts.col.supplier`, `stocks.thresholdAlerts.leadTime`, `stocks.thresholdAlerts.noSupplier` (fr et mg).

### Données

- Migration `valimbavaka-core/src/main/resources/db/changelog/changes/2026_11_03-01-changelog.sql` : colonne `suppliers.lead_time_days INTEGER NOT NULL`, contrainte `ck_suppliers_lead_time_days` (0 à 365), fournisseurs existants remplis à 7 (`LOCAL`) ou 30 (`FOREIGN`). Rollback : suppression de la colonne.
- Journal : pas de nouvelle action. Modifier le délai compte comme une modification de la fiche (`UPDATE` « Modification du fournisseur … »), via `Supplier.update` qui compare les `SupplierDetails`.

## Règles métier à respecter par les autres tickets

- Délai = entier de jours entre 0 et 365. `null` en écriture veut dire « défaut du type », **à la création comme à la modification** (`Supplier.java:85`).
- Défauts : `LOCAL` 7 jours, `FOREIGN` 30 jours. Côté front, ne pas les recopier : utiliser `DEFAULT_LEAD_TIME_DAYS` (même valeur que `SupplierType` côté serveur).
- La date de livraison prévue part du jour de Madagascar (`StockAlertPolicy.BUSINESS_ZONE` = `Indian/Antananarivo`), pas du jour UTC du serveur.
- Un article sans fournisseur (fabrication interne) n'a ni délai ni date : les quatre champs fournisseur de l'alerte sont `null`.
- La quantité suggérée de STK-04 **ne dépend pas** du délai (manque arrondi à la dizaine supérieure, inchangé).

## Fichiers concernés

- Backend :
  - `feature/supplier/domain/{SupplierType,SupplierDetails,Supplier}.java`
  - `feature/supplier/dto/{SupplierRDto,SupplierWDto,SupplierLeadTimeRDto}.java`
  - `feature/supplier/mapper/SupplierMapper.java`, `feature/supplier/repository/SupplierRepository.java`
  - `feature/supplier/service/SupplierService.java`, `feature/supplier/service/impl/SupplierServiceImpl.java`
  - `feature/stock/dto/StockAlertRDto.java`, `feature/stock/service/impl/{StockAlertPolicy,StockServiceImpl}.java`
- Migration : `db/changelog/changes/2026_11_03-01-changelog.sql` (déclarée dans `db.changelog-master.yaml`)
- Frontend :
  - `features/supplier/data-access/supplier-models.ts`, `features/supplier/pages/supplier-form-page/*`
  - `features/suppliers/components/supplier-detail/supplier-detail.html`
  - `features/stocks/data-access/stock-models.ts`, `features/stocks/components/threshold-alerts/*`
  - `public/i18n/{fr,mg}.json`
- Tests : voir [scénario de test](scenario-test.md#tests-existants).

## Dépendances

**Ce ticket s'appuie sur** :
- [FRS-01](../FRS-01/description.md) · [guide](../FRS-01/guide.md) : fiche fournisseur, type `LOCAL`/`FOREIGN`.
- [STK-04](../STK-04/description.md) · [guide](../STK-04/guide.md) : calcul des alertes de seuil.

**Ces tickets s'appuient sur lui** :
- Aucun en dépendance déclarée. [STK-05](../STK-05/description.md) (bon de réapprovisionnement) peut reprendre `leadTimeDays` et `expectedDeliveryDate` des alertes à la place des colonnes provisoires de `stock-mapping.ts`.

## Comment l'utiliser depuis un autre ticket

```java
// Délai des fournisseurs de plusieurs articles, en une seule requête
Map<UUID, SupplierLeadTimeRDto> leadTimes = supplierService.getLeadTimes(supplierIds);
```

## Points d'attention

- `StockServiceImpl` a maintenant 6 dépendances injectées (dont `SupplierService` et `Clock`) ; même cas que `AuthServiceImpl` et `JournalServiceImpl`.
- Le rôle `SELLER` lit les alertes (STK-04) : il voit donc le nom et le délai du fournisseur, alors que la liste des fournisseurs reste réservée à l'administrateur. À confirmer avec le PO ; sinon, masquer ces champs pour le vendeur (STK-04/FRS-04).
- Un client API qui omet `leadTimeDays` à la modification remet le délai au défaut du type. L'écran envoie toujours la valeur. Un futur import de fournisseurs doit transmettre le délai.
- Un délai décimal envoyé directement à l'API est probablement tronqué par Jackson (`ACCEPT_FLOAT_AS_INT`) au lieu d'être refusé : anomalie 1 du scénario, réglage transverse.
- La page Stocks garde ses colonnes provisoires de réapprovisionnement (`stock-mapping.ts`, `PENDING_RESTOCK_FIELDS`) : leur remplacement relève de STK-05.
- Le tableau de bord (DSH-03) a son propre `StockAlertRDto` dans `features/dashboard` : non modifié ici.
