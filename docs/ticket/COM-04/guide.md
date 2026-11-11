# Guide d'intégration — COM-04 · Suivi de commission et objectif

Statut : ✅ rédigé · Ticket : [#88](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/88) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

La vendeuse consulte, pour un mois, sa commission (totale, à valider, validée), son chiffre d'affaires commissionné, l'avancement vers son objectif mensuel et le détail de chaque vente (base, taux, commission, statut). Décision PO du 07/11/2026 : l'objectif mensuel est un champ de la fiche utilisateur de la vendeuse (`users.monthly_target`), fixé par l'administrateur seul ; aucun endpoint ne permet à la vendeuse de l'écrire. Le serveur impose l'identifiant de la vendeuse connectée (règle VTE-07) ; l'administrateur désigne la vendeuse.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie (`XxxRDto`) | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/commissions/follow-up?month=AAAA-MM&sellerId=` | `commissions.read` (existante : vendeuse et administrateur) | paramètres de requête, aucune écriture ; `month` par défaut : mois en cours à Madagascar ; `sellerId` ignoré pour une vendeuse | `CommissionFollowUpRDto` : vendeuse, mois, `from`/`to`, `commissionCount`, `revenue`, `commissionAmount`, `toValidateAmount`, `validatedAmount`, `monthlyTarget`, `targetProgressPercent` | 400 (mois mal formé), 401, 403, 404 (compte non vendeuse), 422 (administrateur sans `sellerId`) |
| GET | `/api/v1/commissions/follow-up/lines?month&sellerId&page&size&sortBy&sortDirection` | `commissions.read` | idem + pagination (base 1, size ≤ 100) | `PageResponse<CommissionRDto>` (base `baseAmount`, taux `rate`, `amount`, `status`), annulées comprises, tri par défaut `businessDate` décroissant | 400, 401, 403, 404, 422 |
| POST / PUT | `/api/v1/users`, `/api/v1/users/{id}` | `users.create` / `users.update` (`ADMIN` seulement, inchangées) | `CreateUserWDto` / `UpdateUserWDto` : nouveau champ `monthlyTarget` (facultatif, > 0, 2 décimales, vendeuse seulement) | `UserRDto` : nouveau champ `monthlyTarget` | 403 (vendeuse), 422 (≤ 0, trop de décimales, rôle autre que vendeuse) |

### Services et ports Java réutilisables

- `CommissionFollowUpService` (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/commission/service/CommissionFollowUpService.java`) : `getFollowUp(sellerId, month)` et `getLines(query, sellerId, month)` ; l'implémentation choisit la vendeuse (`CommissionFollowUpServiceImpl.java:103`) : la connectée si c'est une vendeuse active, sinon `sellerId` obligatoire.
- `CommissionRepository.sumRevenue` (CA hors annulées) ; les montants par statut réutilisent `totalsBySellerAndStatus` (COM-03).
- `SellerTerms` porte désormais `monthlyTarget` (constructeur à deux arguments conservé, objectif nul) ; `User.getMonthlyTarget()` et `UserRDto.monthlyTarget()` l'exposent.

### Frontend (`valimbavaka-ui/src/app/features/commissions/`)

- Route `/commissions` (garde `commissions.read`), page `commissions-page` (« Mes commissions ») branchée sur l'API ; les paramètres d'URL `sellerId` et `month` (administrateur) sont liés aux entrées du composant. Le marqueur « En construction » est retiré.
- `data-access/commission-service.ts` : `followUp(month, sellerId?)` et `followUpLines(query, month, sellerId?)` ; types `CommissionFollowUpRDto` (`commission-models.ts`) ; `commission-months.ts` (mois, jours restants).
- `commission-list-page` : bouton « Suivi » par vendeuse, qui ouvre `/commissions?sellerId=…&month=…`.
- Fiche utilisateur (`features/user/pages/user-form-page`) : champ « Objectif mensuel de ventes (Ar) » pour une vendeuse.

### Données

- Migration `2026_11_11-61-changelog.sql` (rollback fourni) : colonne `users.monthly_target NUMERIC(19,2)` et contrainte `ck_users_monthly_target` (nulle ou > 0). Aucune nouvelle permission.
- Journal : `UPDATE` sur `USER` ; la description reçoit « · objectif mensuel : ancien → nouveau Ar » quand l'objectif change (`UserServiceImpl.targetChange`).

## Règles métier à respecter par les autres tickets

- L'objectif est un chiffre d'affaires mensuel en ariary, comparé à la somme du CA des opérations commissionnées du mois (ventes et prestations) : `targetProgressPercent` = CA × 100 / objectif, une décimale, non plafonné. Hypothèse : le ticket ne précise pas l'unité de l'objectif.
- Les commissions `CANCELLED` sont exclues de tous les totaux (règle COM-02/COM-03) mais listées avec leur statut dans le détail.
- L'objectif est lu au moment de la consultation : pas d'historique des objectifs par mois.
- Une vendeuse ne peut jamais écrire son objectif : ne jamais l'ajouter à un DTO d'entrée accessible à la vendeuse (profil personnel, changement de mot de passe…).
- Le mois est celui des journées d'activité (`business_date`, heure de Madagascar).

## Fichiers concernés

**Créés** : `CommissionFollowUpService`, `CommissionFollowUpServiceImpl`, `CommissionFollowUpRDto`, migration `2026_11_11-61`, `CommissionFollowUpIT`, `SellerTargetIT`, `CommissionFollowUpServiceImplTest`, `commission-months.ts`, `commission-months.spec.ts`, `commissions-page.spec.ts`. **Modifiés** : `User`, `SellerTerms`, `UserRDto`, `CreateUserWDto`, `UpdateUserWDto`, `UserServiceImpl`, `CommissionController`, `CommissionRepository`, `commissions-page` (ts, html, css), `commission-service.ts`, `commission-models.ts`, `commission-list-page`, `user-form-page`, `user-models.ts`, `fr.json`, `mg.json`, `commissions.routes.ts`, constructeurs de `UserRDto` / `CreateUserWDto` / `UpdateUserWDto` dans les tests existants. **Supprimé** : `commission-fixtures.ts` (données de démonstration).

## Dépendances

**Ce ticket s'appuie sur** :
- [COM-02](../COM-02/description.md) · [guide](../COM-02/guide.md)
- [COM-03](../COM-03/description.md) · [guide](../COM-03/guide.md)

**Ces tickets s'appuient sur lui** :
- _(rien)_

## Comment l'utiliser depuis un autre ticket

```text
GET /api/v1/commissions/follow-up?month=2026-11                       (vendeuse : les siennes)
GET /api/v1/commissions/follow-up?month=2026-11&sellerId=<uuid>       (administrateur)
```

## Points d'attention

- Hypothèse : l'objectif est un chiffre d'affaires mensuel (Ar). Si le client le veut en montant de commission, il suffit de comparer `commissionAmount` à l'objectif côté API (un seul endroit : `CommissionFollowUpServiceImpl.progress`).
- L'écran de démonstration affichait une répartition par canal et un statut « Payée » : le backend n'a ni l'un ni l'autre (pas de ticket de paiement), ils sont retirés ; le détail montre l'origine (vente ou prestation) à la place du canal.
- « Jours restants » est calculé côté écran en jours calendaires (le calendrier ouvré du salon n'existe pas).
- L'export du relevé reste une démonstration : COM-05.
- Le détail charge 20 lignes par page (pagination serveur) ; l'administrateur n'a pas de sélecteur de vendeuse sur la page, il y arrive par le bouton « Suivi » de « Commissions calculées ».
- `monthKey` existe aussi dans `commission-list-page.ts` ; à factoriser lors d'un prochain passage sur cet écran.
- La page est dans `demo-pages.a11y.spec.ts` (état « choisir une vendeuse » pour l'administrateur) ; l'état avec données est à vérifier en recette.
