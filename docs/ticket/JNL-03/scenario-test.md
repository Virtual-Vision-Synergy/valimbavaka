# Scénarios de test — JNL-03 · Accès au journal complet

Statut : ☐ tests écrits, non exécutés · Ticket : [#104](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/104) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | ADMIN (permission `journal.read`) ; SELLER et DELIVERY_DRIVER pour les refus |
| Données de départ | quelques opérations journalisées aujourd'hui (UTC) et hier |
| Tickets à avoir déployés avant | JNL-01 |

Décision (validée avec le demandeur) : le « jour » est le **jour UTC** (début inclus 00:00:00Z, fin exclue 24:00:00Z), pour éviter tout décalage de fuseau.

## Scénarios issus des critères d'acceptation

| # | Critère d'acceptation | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Toutes les opérations du jour | `date=2026-10-29` | requête bornée `[2026-10-29T00:00Z ; 2026-10-30T00:00Z[` | `JournalServiceImplTest.getEntries_withDate_queriesTheUtcDayOldestFirst` | ☐ |
| 2 | Ordre chronologique | `date` fourni sans tri | tri `occurredAt` croissant | `JournalServiceImplTest.getEntries_withDate_queriesTheUtcDayOldestFirst` | ☐ |
| 2b | idem | `date` + `sortDirection=DESC` explicite | le choix de l'appelant est respecté | `JournalServiceImplTest.getEntries_withDateAndExplicitDirection_keepsTheRequestedDirection` | ☐ |
| 3 | Journal complet conservé | sans `date`, `search=" rasoa "` | tout le journal, plus récent d'abord, terme normalisé | `JournalServiceImplTest.getEntries_withoutDate_listsEverythingNewestFirst` | ☐ |
| 4 | Accès depuis la barre supérieure (ADMIN) | ADMIN connecté | lien « Journal » vers `/journal` | `MainLayout should link the administrator to the full journal from the top bar` | ☐ |
| 5 | Page : jour courant par défaut | ouverture de `/journal` | appel avec `date` = jour UTC courant | `JournalListPage should list today's UTC operations by default` | ☐ |
| 5b | Page : autre jour | choix du 2026-10-19 | appel `date=2026-10-19`, `page=1` | `JournalListPage should reload from the first page when another day is picked` | ☐ |
| 5c | Page : tout le journal | clic « Tout le journal » | appel sans `date` | `JournalListPage should show the whole journal without a date when asked` | ☐ |
| 5d | Service front | `list(query, '2026-10-29')` | paramètre `date` envoyé | `JournalService should send the UTC day when a date is given` | ☐ |
| 5e | Service front | `list(query)` | aucun paramètre `date` | `JournalService should not send any date parameter when none is given` | ☐ |
| 6 | API bout en bout | ADMIN · `date` = aujourd'hui UTC | 200, `items` est un tableau | `JournalSecurityIT.getEntries_withTodayUtcDate_listsTodaysOperationsOldestFirst` | ☐ |

## Sécurité et erreurs

| # | Cas | Quand | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| S1 | 401 | `GET /api/v1/journal` sans jeton | 401 | `JournalSecurityIT.getEntries_withoutToken_returns401` (JNL-01) | ☐ |
| S2 | 403 | livreur avec `date` | 403 | `JournalSecurityIT.getEntries_withDateAndNonAdminRole_returns403` | ☐ |
| S3 | 400 | `date=29/10/2026` | 400 `MALFORMED_REQUEST` | `JournalSecurityIT.getEntries_withMalformedDate_returns400` | ☐ |
| S4 | Lien masqué | vendeuse connectée | pas de lien dans la barre | `MainLayout should not offer the journal link in the top bar to a seller` | ☐ |
| S5 | Append-only | POST / DELETE sur le journal | 405 / 4xx (inchangé) | `JournalSecurityIT.journal_writeVerbs_areNotExposed` (JNL-01) | ☐ |

## Cas limites

| # | Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|---|
| L1 | Jour sans opération (`2000-01-01`) | 200, `items` vide | `JournalSecurityIT.getEntries_withDateInThePast_returnsNoTodayEntry` | ☐ |
| L2 | Opération à `23:59:59Z` / `00:00:00Z` | rattachée au bon jour (début inclus, fin exclue) | borne couverte par les instants du test n°1 ; pas de test dédié | ☐ |
| L3 | Date vide côté page (champ effacé) | traité comme « tout le journal » | couvert par le scénario 5c | ☐ |

## Recette manuelle

| # | Parcours | Attendu | OK |
|---|---|---|---|
| R1 | ADMIN : cliquer l'icône Journal dans la barre supérieure | page du jour, plus ancien d'abord, heures affichées en UTC | 🧪 |
| R2 | Changer de jour, puis « Tout le journal » | liste mise à jour, plus récent d'abord | 🧪 |
| R3 | SELLER : barre supérieure | aucune icône Journal ; `/journal` refusé | 🧪 |

## Résultat de l'exécution

À lancer (CI ou local) : `mvn verify` (`JournalServiceImplTest`, `JournalSecurityIT`) et `ng test` (journal, main-layout). Aucun test exécuté par le traiteur.
