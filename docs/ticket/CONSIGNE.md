# Consigne équipe — remplir le dossier de son ticket

À appliquer **à partir de maintenant, sur chaque ticket**, par la personne qui le traite (le « traiteur »). Les 10 tickets déjà fermés (AUTH-01 à AUTH-04, JNL-01, JNL-02, SYS-02, FRS-01, SYS-05, SYS-06) sont complets et servent de modèles : **[AUTH-04](AUTH-04/scenario-test.md)** pour un ticket backend + frontend, **[FRS-01](FRS-01/scenario-test.md)** pour une fiche CRUD, **[JNL-02](JNL-02/scenario-test.md)** pour un ticket purement technique.

## 1. Pourquoi

Chaque ticket a un dossier `docs/ticket/<ID>/` avec trois fichiers :

| Fichier | Qui l'écrit | Pour qui |
|---|---|---|
| `description.md` | généré depuis l'issue GitHub | tout le monde |
| `scenario-test.md` | **le traiteur**, en premier | la QA (Njara), les reviewers, le traiteur lui-même |
| `guide.md` | **le traiteur**, ensuite | les **autres** traiteurs qui vont utiliser son travail |

Le guide n'est pas une note pour soi : il dit aux autres comment utiliser ce qu'on a livré (quoi appeler, quels fichiers, quel contrat, quels pièges) sans relire le code.

## 2. Le cycle, étape par étape

| Moment | À faire | Fichier |
|---|---|---|
| **Avant de coder** | Lire `description.md`, puis le `guide.md` de chaque ticket listé dans « Dépend de ». Si un guide de dépendance est vide, demander à son traiteur avant de coder. | lecture |
| **Avant de coder** | Écrire les lignes du scénario à partir des critères d'acceptation : Étant donné / Quand / Alors + résultat attendu. Colonne « Test » encore vide, « Statut » à ☐. | `scenario-test.md` |
| **Pendant** | Écrire les tests en suivant le scénario ; reporter le **nom exact** de chaque test dans la colonne « Test ». | `scenario-test.md` |
| **Avant d'ouvrir la PR** | Lancer les tests, mettre les statuts (section 4), remplir « Résultat de l'exécution », puis écrire le guide. | les deux |
| **Dans la PR** | Les fichiers `docs/ticket/<ID>/*` font partie de la PR ; ajouter dans le corps : `Dossier : docs/ticket/<ID>/`. Pas de dossier rempli, pas d'approbation. | PR |
| **Après le merge (In Test)** | La QA passe les étapes 🧪 de la recette, met à jour leur statut et les anomalies. | `scenario-test.md` |
| **Fermeture** | Le ticket se ferme quand tous les critères sont ✅ ou qu'un écart est accepté et suivi dans une nouvelle issue. | `description.md` |

## 3. Lire avant de coder

- Un ticket qui **écrit** (créer, modifier, valider, annuler) dépend de [SYS-07](SYS-07/description.md) : son formulaire enregistre par `OutboxService.submit`, jamais par un `HttpClient` direct. Lire le guide de SYS-07 avant de coder l'écran.
- Toujours partir du **guide** des dépendances, pas du code : s'il dit « appeler `JournalService.record` dans la même transaction », on le fait.
- Les points d'attention des guides listent des écarts connus (ex. `DELETE /api/v1/users/{id}`, conflits renvoyés en 500) : ne pas les reproduire dans son ticket.
- Les tickets encore marqués ⚠ dans l'[index](README.md) ont un guide pré-rempli automatiquement depuis le code : à vérifier, pas à croire sur parole.

## 4. Remplir le scénario de test

### Colonnes

Chaque tableau a les colonnes **Cas / Exemple / Résultat attendu / Test / Statut**. Dans « Exemple », mettre une **vraie valeur** (`100.01`, `"  Huiles Ravo  "`, jeton vendeuse), jamais « une valeur invalide ».

### Statuts (colonne « Statut »)

| Statut | Signification | Quand l'utiliser |
|---|---|---|
| ✅ auto | test automatique **exécuté et vert** | le nom du test est dans la colonne « Test » et la CI (ou `mvn verify` / `ng test` en local) est passée |
| ✅ base | vérifié directement en base | contraintes, triggers, types de colonnes, données initiales, sur une base migrée |
| 🔍 code | vérifié en lisant le code, **sans test** | à éviter : préciser la classe ou méthode lue, et prévoir le test manquant dans les anomalies |
| ❌ écart | le comportement ne respecte pas l'attendu | toujours repris dans « Anomalies » avec une gravité et une correction proposée |
| ⏭ hors périmètre | relève d'un autre ticket | nommer ce ticket |
| 🧪 recette | à vérifier à la main dans l'application | uniquement dans « Recette manuelle » ; la QA le passe en ✅ ou ❌ |
| ☐ à faire | pas encore traité | interdit au moment d'ouvrir la PR |

Ne jamais mettre ✅ sans avoir exécuté. Ne jamais citer un nom de test qui n'existe pas.

### Sections obligatoires

1. **Pré-requis** : rôles, données de départ (avec des exemples), tickets à déployer avant.
2. **Scénarios issus des critères** : au moins une ligne par critère d'acceptation, numérotée comme le critère (1, 1b, 1c…).
3. **Sécurité et erreurs** : pour chaque endpoint écrit, au minimum :
   - sans jeton → 401 `UNAUTHENTICATED` ;
   - mauvais rôle → 403 `FORBIDDEN` ;
   - champ `id` / `role` glissé dans le corps → ignoré (mass assignment) ;
   - données invalides → 422 `VALIDATION_FAILED` ;
   - ressource inconnue → 404 `NOT_FOUND` ;
   - doublon ou état incompatible → 409 `CONFLICT` ;
   - opération tracée dans le journal (ligne `CREATE` / `UPDATE` / `CANCEL` / `REVERSE`).
4. **Cas limites** : un tableau avec des exemples concrets. Passer en revue à chaque fois :
   - bornes exactes : minimum, maximum, maximum + 1 (longueurs, montants, quantités, taux, `size` = 100 et 101) ;
   - vide, `null`, espaces autour, majuscules / minuscules ;
   - liste vide, une seule page, dernière page, tri hors liste blanche ;
   - état déjà demandé (idempotence métier), état final (déjà annulé, déjà clos) ;
   - deux utilisateurs en même temps (même fiche, même nom) ;
   - hors ligne : saisie en file, rejeu avec la même `Idempotency-Key`, changement de compte ;
   - session expirée pendant l'action, compte désactivé entre-temps ;
   - dates : fuseau, minuit, fin de mois si le ticket manipule des périodes.
5. **Tests existants** : chemins des classes backend et des `spec.ts`.
6. **Recette manuelle (🧪)** : seulement ce qu'un test ne peut pas couvrir (parcours complet dans le navigateur, impression, mobile).
7. **Résultat de l'exécution** : date, ce qui a tourné (CI, `mvn verify`, `ng test`, nombre de tests), anomalies numérotées avec gravité (haute / moyenne / faible) et correction proposée, nombre d'étapes 🧪 restantes.

### Commandes

```bash
# Backend (Docker requis pour Testcontainers)
cd valimbavaka-core && mvn -B -ntp verify

# Frontend (Node ≥ 22.22.3 ou 24)
cd valimbavaka-ui && npm ci && npx ng test --watch=false
```

La CI GitHub lance les mêmes commandes sur chaque PR : un job vert vaut exécution.

## 5. Remplir le guide

Structure à garder (voir [AUTH-04/guide.md](AUTH-04/guide.md)) :

1. **En bref** : 2-3 phrases, ce que ça fait et ce que ça offre aux autres.
2. **Contrat exposé** : tableau des endpoints (méthode, URL, permission, entrée `XxxWDto`, sortie `XxxRDto`, erreurs possibles) ; services Java réutilisables avec leur signature ; côté frontend : routes, services `data-access`, composants réutilisables ; tables, migrations et lignes de journal.
3. **Règles métier pour les autres tickets** : ce que les autres doivent respecter (états, calculs, droits, filtres à appliquer).
4. **Fichiers concernés** : chemins complets, backend, migrations, frontend, tests.
5. **Dépendances** : liens vers les guides des tickets dont on dépend et qui dépendent de nous.
6. **Comment l'utiliser** : un extrait de code minimal (Java ou TypeScript) qu'un autre peut copier.
7. **Points d'attention** : écarts connus, limites, ce qui reste à faire, avec le ticket qui doit s'en charger.

Règles : décrire ce que le code **fait vraiment** (pas ce que le ticket demandait) ; mettre à jour le guide si une PR suivante change le contrat.

## 6. Mettre à jour l'index et la description

- Dans [README.md](README.md), passer les colonnes Scénario et Guide de son ticket à ✅ quand ils sont complets.
- Dans `description.md`, cocher chaque critère vérifié et ajouter le verdict à la fin de la ligne, comme dans les tickets fermés : `— *✅ vérifié …*`, `— *⚠ partiel : …*`, `— *❌ écart : …*`.

## 7. Écart trouvé dans un ticket déjà fermé

Ne pas corriger en silence dans son propre ticket. Ouvrir une issue `fix/…` qui cite la ligne d'anomalie du scénario concerné, et mettre à jour ce scénario quand la correction est fusionnée.

Écarts relevés le 2026-10-23 et leur correction (chaque PR met à jour les dossiers concernés) :

| Gravité | Écart | Correction | Dossier |
|---|---|---|---|
| haute | `DELETE /api/v1/users/{id}` supprimait réellement un compte (contraire à AUTH-03 et JNL-02) | PR #146 : endpoint, permission et bouton retirés ; ArchUnit `no_delete_endpoints` | [AUTH-03](AUTH-03/scenario-test.md#résultat-de-lexécution) |
| haute | `PUT /api/v1/users/{id}` permettait de désactiver ou de rétrograder le dernier administrateur | PR #146 : mêmes garde-fous que `PATCH /status`, verrou sur les admins actifs | [AUTH-04](AUTH-04/scenario-test.md#résultat-de-lexécution) |
| moyenne | aucun écran ni endpoint de changement de mot de passe (premier administrateur bloqué sur `passwordChangeRequired`) | PR #147 : `POST /api/v1/auth/password`, écran `/password`, aucune permission tant que le mot de passe est provisoire | [AUTH-01](AUTH-01/scenario-test.md#résultat-de-lexécution), [SYS-05](SYS-05/scenario-test.md#résultat-de-lexécution) |
| moyenne | mot de passe minimum 8 caractères au lieu de 15 (convention §9.2) | PR #146 (création de compte) et #147 (changement) : 15 caractères | [AUTH-03](AUTH-03/scenario-test.md#résultat-de-lexécution) |
| moyenne | conflits de version et violations d'unicité renvoyés en 500 au lieu de 409 | PR #148 : 409 dans `GlobalExceptionHandler`, version sur la fiche fournisseur | [FRS-01](FRS-01/scenario-test.md#résultat-de-lexécution), [SYS-06](SYS-06/scenario-test.md#résultat-de-lexécution) |
| moyenne | libellé de journal > 500 caractères : l'opération métier échouait en 500 | PR #148 : libellé tronqué | [JNL-01](JNL-01/scenario-test.md#résultat-de-lexécution) |
| faible | bouton de déconnexion dans la barre latérale et non la barre supérieure | PR #149 : bouton dans la barre supérieure | [AUTH-02](AUTH-02/scenario-test.md#résultat-de-lexécution) |
| faible | table technique `dev` encore auditée en `bigint` | PR #146 : tables `dev`, `revinfo`, `revchanges` supprimées | [SYS-06](SYS-06/scenario-test.md#résultat-de-lexécution) |
| faible | course entre deux désactivations simultanées des deux derniers admins | PR #146 : verrou pessimiste | [AUTH-04](AUTH-04/scenario-test.md#résultat-de-lexécution) |
| faible | ADR 0001 : « file vidée à la déconnexion », le code la conserve par compte | PR #149 : ADR corrigé | [SYS-02](SYS-02/scenario-test.md#résultat-de-lexécution) |

Restent ouverts (gravité faible) : refus des mots de passe courants (convention §9.2) ; `PUBLIC_ROUTES` liste `register` et `forgot-password` qui n'existent pas ; tests d'idempotence 409 « en cours », libération sur 5xx et purge 24 h (SYS-12).

## 8. À ne pas faire

- Relancer le script qui a généré les dossiers : il écraserait les scénarios et guides remplis.
- Laisser des `_(à compléter)_` ou des lignes du modèle qui ne s'appliquent pas : les supprimer.
- Écrire « testé » sans nom de test, ou ✅ sans exécution.
- Mettre des mots de passe, jetons ou données personnelles réelles dans les exemples.
