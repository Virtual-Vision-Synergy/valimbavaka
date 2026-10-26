# Guide d'intégration — SYS-07 · Brancher un premier formulaire sur la file hors ligne

Statut : ✅ écrit le 2026-10-26 — **tests non exécutés** (voir « Résultat de l'exécution » du scénario) · Ticket : [#133](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/133) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Les écritures faites depuis l'interface ne partent plus directement : elles passent par `OutboxService.submit` ([SYS-02](../SYS-02/guide.md)). En ligne, la saisie part tout de suite avec une clé d'idempotence ; hors ligne, elle est conservée (IndexedDB), un message l'annonce, et elle est envoyée une seule fois au retour du réseau ([SYS-12](../SYS-12/guide.md) évite le doublon côté serveur). Le formulaire de référence est la fiche fournisseur ; les écritures des articles, l'activation d'un fournisseur et la modification / activation d'un compte sont branchées de la même façon. **Tout nouveau formulaire d'écriture doit suivre ce modèle** : c'est pourquoi les tickets qui écrivent dépendent de SYS-07.

## Contrat exposé

### Frontend

- `OutboxService.submit<T>(method, url, body?) : Observable<SubmitResult<T>>` (`core/offline/outbox-service.ts`) :
  - `{ queued: false, body }` : la saisie est partie, `body` est la réponse du serveur (l'entité créée ou modifiée) ;
  - `{ queued: true }` : la saisie est en file (réseau coupé, erreur transitoire, ou saisies plus anciennes en attente) ; **il n'y a pas d'entité** ;
  - erreur : refus métier (400, 404, 409, 422) relancé à l'appelant, jamais mis en file ; sans session : 401.
- Services qui écrivent désormais par la file (retour `Observable<SubmitResult<XxxRDto>>`) :
  - `SupplierService.create / update / setActive` ;
  - `ArticleService.create / update / setActive` ;
  - `UserService.update / setActive`.
- Exception : `UserService.create` reste un appel `HttpClient` direct. La création de compte porte un mot de passe, qui ne doit jamais être écrit dans IndexedDB.
- Message à l'utilisateur quand `queued` est vrai : clé de traduction `common.queuedSaved` (« Saisie enregistrée hors connexion : elle sera synchronisée automatiquement. ») affichée par `ToastService` (`'info'`) ou, dans la fiche latérale fournisseur, par un bandeau `alert-info`.
- Indicateur de statut : `vvs-sync-status` dans la barre supérieure (en ligne / hors ligne, nombre de saisies en attente et refusées) ; page `/synchronisation` pour les saisies refusées ([SYS-09](../SYS-09/description.md)).

### Backend

Aucun changement : voir [SYS-12](../SYS-12/guide.md) (clé d'idempotence, 422 si le contenu change).

## Règles métier à respecter par les autres tickets

1. **Pas de `HttpClient.post/put/patch/delete` dans un formulaire ni dans un service d'écriture** : passer par `OutboxService.submit`. Les lectures restent en `HttpClient`.
2. **Traiter les deux issues** : `result.queued` (annoncer « sera synchronisé », puis fermer le formulaire comme pour un succès) et `result.body` (utiliser la réponse du serveur, par exemple le code attribué). Ne jamais lire `result.body` sans test.
3. **Ne pas attendre d'identifiant** après une création : il n'existe qu'après synchronisation. Un formulaire ne doit pas enchaîner « créer puis rouvrir la fiche créée ».
4. **Ne jamais mettre dans la file** un mot de passe, un jeton ou une donnée personnelle sensible : appel direct à la place.
5. **Une modification renvoie la `version` lue** : si la fiche a changé entre-temps, le rejeu est refusé (409) et la saisie passe en échec (visible dans la page de synchronisation).
6. **Ne pas enregistrer une fiche non lue** : en modification, désactiver « Enregistrer » tant que la lecture n'a pas réussi (`canSave`), car un `PUT` remplace toute la fiche.
7. **Valider avant d'enregistrer** : un formulaire invalide ne met rien en file.
8. Une saisie est rattachée au compte qui l'a créée : elle n'est jamais envoyée sous un autre compte.

## Fichiers concernés

**Frontend** (`valimbavaka-ui/src/app/`)
- `core/offline/outbox-service.ts`, `outbox-models.ts` (`SubmitResult<T>` avec `body`)
- `features/supplier/data-access/supplier-service.ts`, `features/supplier/pages/supplier-form-page/*` (formulaire de référence, `canSave`)
- `features/suppliers/components/supplier-detail/*` (activation, message de file)
- `features/article/data-access/article-service.ts`, `features/articles/pages/article-form-page/article-form-page.ts`
- `features/user/data-access/user-service.ts`, `features/user/pages/user-form-page/user-form-page.ts`, `user-list-page.ts`
- `public/i18n/fr.json`, `mg.json` (`common.queuedSaved`)

**Tests** : `features/supplier/pages/supplier-form-page/supplier-form-page-offline.spec.ts` (nouveau), `supplier-form-page.spec.ts`, `features/suppliers/components/supplier-detail/supplier-detail.spec.ts`, `features/articles/pages/article-form-page/*.spec.ts` et `article-form-page.harness.ts`, `features/supplier/data-access/supplier-service.spec.ts`, `features/article/data-access/article-service.spec.ts`, `core/offline/outbox-service.spec.ts`.

## Dépendances

**Ce ticket s'appuie sur** : [SYS-02](../SYS-02/guide.md) (moteur), [FRS-02](../FRS-02/guide.md) (formulaire de référence), [SYS-12](../SYS-12/guide.md) (idempotence serveur).

**Ces tickets s'appuient sur lui** : [SYS-09](../SYS-09/description.md) et les tickets d'écriture listés dans la colonne « Dépend de » de [SYS-07 dans le README](../README.md) (ART-07, STK-05, STK-06, TRF-03, PRD-01, PRD-03, INV-01 à INV-04, SRV-01, SRV-02, SHO-01, SHO-02, LIV-01, LIV-03, LIV-04, LIV-05, LIV-08, JR-01, JR-05, JR-06, VTE-01, PRO-01, FAC-04, COM-01, COM-03, COM-05, COM-06).

## Comment l'utiliser depuis un autre ticket

```ts
// service d'écriture : toujours par la file
@Injectable({ providedIn: 'root' })
export class PromotionService {
  private readonly outbox = inject(OutboxService);

  create(body: PromotionWDto): Observable<SubmitResult<PromotionRDto>> {
    return this.outbox.submit<PromotionRDto>('POST', '/api/v1/promotions', body);
  }
}

// formulaire : traiter les deux issues
this.promotionService.create(dto).subscribe({
  next: (result) => {
    if (result.queued) {
      this.toast.show('common.queuedSaved', {}, 'info'); // pas d'entité : elle n'existe pas encore
    }
    void this.router.navigate(['/promotions']);
  },
  error: (error: unknown) => this.errorMessage.set(errorMessage(error)), // refus métier : à l'écran
});
```

## Points d'attention

- Les écrans de saisie déjà livrés avant SYS-07 (fournisseur, article, compte) sont branchés par ce ticket ; la création de compte reste directe (mot de passe).
- La persistance IndexedDB après rechargement n'est pas testable en jsdom : à vérifier à la main (recette R2).
- Une modification rejouée avec une version périmée passe en échec (409) : la page `/synchronisation` ([SYS-09](../SYS-09/description.md)) doit permettre de la renvoyer ou de l'abandonner.
- Un compte désactivé ou une session expirée avec des saisies en attente : voir [SYS-11](../SYS-11/description.md).
- Les écritures du détail fournisseur et de la liste des comptes affichent le message de file, mais l'état affiché ne change qu'au rejeu : un indicateur par ligne n'existe pas.
- Les tests de ce ticket n'ont pas été exécutés par l'auteur : voir le scénario avant de s'appuyer sur ce guide pour un comportement limite.
