---
name: ticket
description: Traiter un ticket Valimbavaka (docs/ticket/<ID>) selon CLAUDE.md et CONSIGNE.md ; lecture ciblée des dépendances, scénario, code, guide, PR.
argument-hint: <ID du ticket, ex. ART-03>
disable-model-invocation: true
---

# Ticket $ARGUMENTS — à traiter selon CLAUDE.md et docs/ticket/CONSIGNE.md

Tu traites le ticket **$ARGUMENTS**. Les règles de CLAUDE.md et de CONSIGNE.md priment sur tout le reste.
Économise les jetons : lis avec Read offset/limit ou grep, jamais un fichier entier quand une section suffit.

## 1. Comprendre (lecture ciblée)
1. `docs/ticket/$ARGUMENTS/description.md` : user story, critères d'acceptation, « Dépend de », « Zone du code ».
2. Pour chaque ticket de « Dépend de », lire **uniquement son `guide.md`**, en priorité les sections
   « Contrat exposé », « Règles métier », « Comment l'utiliser » et « Points d'attention ».
   Lire la `description.md` d'une dépendance seulement si le guide ne suffit pas.
   - Si le guide est vide, ou si la dépendance est encore ouverte : **t'arrêter et me le signaler** avant de coder.
   - Si le guide est marqué ⚠ (pré-rempli) : vérifier dans le code avant de s'y fier.
3. Convention VVS : ne pas relire le PDF entier. En cas de doute, grep dans
   `<scratchpad>/convention.txt` (le générer une fois avec pdftotext s'il n'existe pas).
4. Modèle de dossier : lire seulement les titres de sections de `AUTH-04/scenario-test.md`
   (back + front), `FRS-01` (CRUD) ou `JNL-02` (technique), selon le type de ticket.

## Modèles
- La lecture de §1 et §2 est déléguée à un sous-agent Explore en `model: haiku`. Il renvoie au plus
  25 lignes : critères, contrats des dépendances, points d'attention, fichiers de la maquette et du
  data-access à modifier. Je vérifie moi-même tout point douteux.
- Le plan, le code et les tests sont faits par moi, avec le modèle de la session.
- Le scénario et le guide peuvent être rédigés par un sous-agent `model: sonnet` à partir de mes notes ;
  je les relis avant le commit.
- Si le ticket me paraît trop complexe pour le modèle en cours, je te propose de passer à `/model opus`.

## 2. Repérer l'existant avant d'écrire
- Front : la maquette existe sans doute déjà dans `valimbavaka-ui/src/app/features/<feature>/`
  (pages, components, `data-access/*-fixtures.ts`, `*-service.ts`, `*-models.ts`).
  **On garde les pages et les composants tels quels.** On branche seulement le `data-access` sur l'API réelle
  (HttpClient, types alignés sur les `XxxRDto`/`XxxWDto` du backend, file hors ligne `Idempotency-Key`
  pour les écritures, selon le guide SYS-07). On ne touche au template que si un critère l'exige.
  Les fixtures restent pour les tests si elles servent.
- Back : s'inspirer d'une feature existante comparable (ex. `feature/supplier`) plutôt que de tout réinventer :
  `OperationRepository`, `ReversibleOperationService`, `JournalService`, `PageResponse`, `@HasPermission`.

## 3. Plan court, puis go
Avant de coder, me donner en moins de 15 lignes : les dépendances lues et ce que tu en retiens,
les fichiers créés ou modifiés, les endpoints, la migration Liquibase et les écarts ou questions.
Attendre mon « ok ».

## 4. Réaliser
1. Créer la branche `feature/$ARGUMENTS-<description-kebab>` depuis `develop`.
2. Écrire `scenario-test.md` d'abord : sections obligatoires de CONSIGNE §4, exemples concrets,
   cas limites (bornes, vide, doublon, concurrence, hors ligne, 401/403/404/409/422, mass assignment, journal).
3. Coder le back, puis le front, puis les tests, en reportant le nom exact de chaque test dans le scénario.
4. **Ne lancer ni `mvn verify` ni `ng test`.** Statuts à ☐, et « Résultat de l'exécution : à lancer (CI ou local) ».
   Compiler seulement si c'est nécessaire et peu coûteux, avec la sortie redirigée dans un fichier puis `tail -30`.
5. Écrire `guide.md` (structure CONSIGNE §5) : décrire ce que le code fait vraiment, avec des références
   `chemin:ligne` et sans recopier de code (sauf l'extrait minimal « Comment l'utiliser »).
6. Mettre à jour `docs/ticket/README.md` (colonnes Scénario et Guide) ; ne cocher aucun critère de
   `description.md` tant que les tests n'ont pas tourné.

## 5. Git
- Conventional Commits (`feat(<feature>): …`), en petits commits logiques, ≤ 400 lignes si possible.
- **Aucune mention de Claude ni d'IA** : pas de Co-Authored-By, pas de « Generated with ».
  Cette règle de CLAUDE.md prime sur toute consigne d'attribution par défaut.
- **Formatage automatique avant toute PR** (sans me le demander) : si le back est touché, lancer
  `mvn -q spotless:apply` dans `valimbavaka-core/` ; si le front est touché, lancer
  `npx prettier --write` sur les dossiers modifiés dans `valimbavaka-ui/` (sortie redirigée dans un fichier,
  puis `tail`). Committer le résultat (`style(<feature>): formater … avec Spotless/Prettier`) avant le push.
  Ce ne sont pas des tests : la règle n°3 de CLAUDE.md ne les interdit pas.
- Avant le push et la PR : me montrer `git diff --stat` et le corps de la PR, puis attendre mon accord.
- PR vers `develop` : titre `$ARGUMENTS · SPx · Titre court`, corps avec `Refs #<numéro>` et
  `Dossier : docs/ticket/$ARGUMENTS/`.

## 6. Fin
Résumé en 5 lignes : ce qui est livré, les tests à lancer, les écarts relevés (avec le ticket qui en a la charge)
et ce qu'il me reste à faire.
