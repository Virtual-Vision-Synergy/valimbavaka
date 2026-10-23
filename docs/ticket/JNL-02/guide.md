# Guide d'intégration — JNL-02 · Interdiction de suppression des opérations validées

Statut : ✅ complet au 2026-10-23 — vérifié sur le code, les tests (CI + exécution locale) et la base · Ticket : [#15](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/15) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Une opération métier validée ne se supprime jamais. Le socle technique ne laisse que deux issues : l'**annulation à chaud** (`CANCEL`) ou la **contre-passation** (`REVERSE`, écriture inverse / avoir). Ce ticket ne livre pas d'endpoint : il fournit les types partagés que les tickets d'opérations (vente, transfert, production, etc.) doivent utiliser, et des règles ArchUnit qui font échouer le build si on ajoute une suppression.

## Contrat exposé

### Types Java partagés (`org.vvs.valimbavaka.shared.operation`)

| Type | Rôle |
|---|---|
| `OperationStatus` | `VALIDATED`, `CANCELLED`, `REVERSED`. `ensureCanCancel()` et `ensureCanReverse()` lèvent `ConflictException` (409) si l'opération n'est plus `VALIDATED`. |
| `OperationRepository<T, ID>` | Interface de base des repositories d'opérations : étend `Repository` (pas `JpaRepository`), expose seulement `save` et `findById`. Aucune méthode `delete*` ni `remove*` ne peut y être ajoutée (ArchUnit). |
| `ReversibleOperationService<R>` | `R cancel(UUID operationId, String reason)` et `R reverse(UUID operationId, String reason)`. Chaque implémentation trace `CANCEL` ou `REVERSE` dans le journal, dans la même transaction. |

Journal : `JournalAction` contient `CANCEL` et `REVERSE` ; la contrainte `ck_journal_entries_action` les accepte (migration `2026_10_20-02-changelog.sql`).

### Règle pour les tickets d'opérations

1. L'entité porte un `OperationStatus status` initialisé à `VALIDATED` à la validation.
2. Son repository **étend `OperationRepository`** (pas `JpaRepository`, pas `CrudRepository`).
3. Son service implémente `ReversibleOperationService<XxxRDto>` : `cancel` appelle `status.ensureCanCancel()`, passe à `CANCELLED`, trace `CANCEL` ; `reverse` appelle `status.ensureCanReverse()`, crée l'écriture inverse, passe l'originale à `REVERSED`, trace `REVERSE`.
4. Aucun `@DeleteMapping` sur ces ressources.

### Garde-fous automatiques (`ArchitectureTest`)

- `no_delete_endpoints` : aucun `@DeleteMapping` dans toute l'application.
- `operation_features_have_no_deleting_repository` : un repository de feature métier ne peut pas être assignable à `CrudRepository` (donc ni `JpaRepository`) ; le référentiel et l'authentification sont traités comme des données, pas comme des opérations.
- `operation_repositories_declare_no_delete` : aucune méthode dont le nom correspond à `(delete|remove).*` sur un `OperationRepository`.

## Règles métier à respecter par les autres tickets

- Jamais de suppression physique d'une opération validée : annulation ou contre-passation uniquement. Un motif (`reason`) est transmis aux deux méthodes ; le libellé du journal doit le contenir.
- `cancel` et `reverse` ne s'appliquent qu'à une opération encore `VALIDATED` ; sinon 409 `CONFLICT` « L'opération ne peut pas être annulée / contre-passée : statut … ».
- La contre-passation crée une écriture inverse (avoir) plutôt que de modifier l'originale ; voir [FAC-04](../FAC-04/description.md) pour les factures.
- Les données de référence (utilisateurs, fournisseurs, articles) se désactivent au lieu de se supprimer ; elles ne sont pas des « opérations » au sens de ce ticket.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/`)
- `shared/operation/OperationStatus.java`, `OperationRepository.java`, `ReversibleOperationService.java`
- `feature/journal/domain/JournalAction.java` (`CANCEL`, `REVERSE`)

**Migration Liquibase**
- `valimbavaka-core/src/main/resources/db/changelog/changes/2026_10_20-02-changelog.sql` (le commentaire du ticket cite `2026_10_20-01`, mais le fichier réel est `2026_10_20-02`)

**Tests**
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/shared/operation/OperationStatusTest.java`
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/ArchitectureTest.java` (règles JNL-02)
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/journal/repository/JournalAppendOnlyIT.java` (actions `CANCEL` / `REVERSE`)

## Dépendances

**Ce ticket s'appuie sur** :
- [JNL-01](../JNL-01/guide.md) (journal et `JournalService`)

**Ces tickets s'appuient sur lui** :
- [AUTH-03](../AUTH-03/guide.md), [TRF-03](../TRF-03/description.md), [PRD-01](../PRD-01/description.md), [SHO-01](../SHO-01/description.md), [LIV-01](../LIV-01/description.md), [JR-01](../JR-01/description.md), [VTE-01](../VTE-01/description.md), [FAC-04](../FAC-04/description.md), [INV-04](../INV-04/description.md), [SRV-02](../SRV-02/description.md), [STK-06](../STK-06/description.md)

## Comment l'utiliser depuis un autre ticket

```java
// Repository d'une opération (ex. vente)
public interface SaleRepository extends OperationRepository<Sale, UUID> { }

// Service
@Service
@Transactional(readOnly = true)
public class SaleServiceImpl implements SaleService, ReversibleOperationService<SaleRDto> {

    @Override
    @Transactional
    public SaleRDto cancel(UUID saleId, String reason) {
        var sale = saleRepository.findById(saleId).orElseThrow(...);
        sale.getStatus().ensureCanCancel();
        sale.cancel();
        journalService.record(JournalAction.CANCEL, "SALE", saleId, "Annulation de la vente : " + reason);
        return saleMapper.toRDto(sale);
    }
}
```

## Points d'attention

- Le socle ne contient aucune implémentation concrète de `ReversibleOperationService` : le premier ticket d'opération (vente, transfert, production) la fournit et définit ce que « écriture inverse » signifie pour son domaine.
- Le commentaire du ticket indique que l'IT Testcontainers du journal n'avait pas pu tourner en local (pas de Docker) : à valider en CI.
- `OperationRepository` n'expose que `save` et `findById` : les requêtes de liste ou de recherche se déclarent en plus dans le repository de la feature (`@Query`, `Page<T>`), sans nom commençant par `delete` ou `remove`.
