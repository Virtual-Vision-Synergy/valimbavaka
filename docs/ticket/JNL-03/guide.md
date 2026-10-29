# Guide d'intégration — JNL-03 · Accès au journal complet

Statut : ✅ implémenté, tests écrits mais non exécutés (à lancer en CI) · Ticket : [#104](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/104) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

L'administrateur ouvre le journal depuis une icône de la barre supérieure. La page affiche par défaut les opérations du jour UTC en ordre chronologique ; un sélecteur de jour et un bouton « Tout le journal » permettent de remonter plus loin. Aucune migration : seul un filtre `date` optionnel s'ajoute à l'API de JNL-01.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/journal` | `journal.read` (ADMIN) | `PageQuery` + `date` optionnel (`yyyy-MM-dd`, jour UTC) | 200 `PageResponse<JournalEntryRDto>` | 400 `MALFORMED_REQUEST` (date illisible), 401, 403 |

Avec `date` : entrées dont `occurredAt` est dans `[date 00:00Z ; date+1 00:00Z[`, tri par défaut `occurredAt` croissant. Sans `date` : comportement JNL-01 inchangé (tout le journal, décroissant). `sortBy` / `sortDirection` explicites priment dans les deux cas.

### Services et ports Java réutilisables

- `JournalService.getEntries(PageQuery, LocalDate)` : la signature a changé (second paramètre nullable) ; `JournalService.record(...)` est inchangé.
- `JournalEntryRepository.searchBetween` : même recherche texte que `search`, bornée en temps.

### Frontend

- Routes : `/journal` (inchangée, `journal.routes.ts`).
- `JournalService.list(query, date?)` (`journal-service.ts`) : n'ajoute `date` que s'il est renseigné.
- `JournalListPage` : signal `day` initialisé au jour UTC courant, méthode `onDayChange` ; les heures sont affichées en UTC.
- `TopBar` : signal `canReadJournal` (permission `journal.read`) et lien `a.journal-link` vers `/journal`.

### Données

- Aucune table ni migration Liquibase.
- Aucune écriture dans le journal (consultation seule).

## Règles métier à respecter par les autres tickets

- Le « jour » du journal est le jour UTC, pas le jour local. Un ticket qui affiche des dates du journal doit le préciser.
- Le journal reste en lecture seule : aucune route d'écriture (`JournalSecurityIT.journal_writeVerbs_areNotExposed`).
- Le lien de la barre supérieure améliore l'UX ; le serveur décide via `@HasPermission("journal.read")`.

## Fichiers concernés

- Back : `JournalController` (paramètre `date`), `JournalService` / `JournalServiceImpl.getEntries`, `JournalEntryRepository.searchBetween`.
- Front : `journal-service.ts`, `journal-list-page.{ts,html}`, `top-bar.{ts,html}`, `public/i18n/{fr,mg}.json` (`journal.day.*`, `layout.journal`).
- Tests : `JournalServiceImplTest`, `JournalSecurityIT`, `journal-service.spec.ts`, `journal-list-page.spec.ts`, `main-layout.spec.ts`.

## Dépendances

**Ce ticket s'appuie sur** : [JNL-01](../JNL-01/description.md) · [guide](../JNL-01/guide.md)

**Ces tickets s'appuient sur lui** : _(rien)_

## Comment l'utiliser depuis un autre ticket

```text
GET /api/v1/journal?date=2026-10-29&page=1&size=20
```

## Points d'attention

- Les entrées proches de minuit local (Madagascar = UTC+3) tombent dans le jour UTC voisin : décision validée avec le demandeur pour éviter les bugs de fuseau.
- Changer la signature de `getEntries` impose de mettre à jour tout futur appelant.
- `searchBetween` n'est pas couvert par un test d'intégration avec données (seul un jour vide et le jour courant sont testés) : une recette manuelle R1 est prévue.
