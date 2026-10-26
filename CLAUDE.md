# Valimbavaka — consignes permanentes

## Règle n°1 : toujours suivre la convention VVS

Tout code (backend, frontend, SQL, tests, Git) doit respecter **`resources/convention-dev-VVS-V2.1.pdf`**
(Convention de codage VVS V2.1). En cas de doute, la relire ; toute exception se justifie dans la PR.
Pour lire le PDF : `pdftotext -enc UTF-8 -layout resources/convention-dev-VVS-V2.1.pdf <sortie.txt>`.

## Projet

- `valimbavaka-core/` : backend Spring Boot 4.1, Java 21, Maven, PostgreSQL, Liquibase. Package racine `org.vvs.valimbavaka`.
- `valimbavaka-ui/` : frontend Angular 22 (standalone, zoneless, signals, Vitest, ESLint, Prettier).
- Rôles métier : `ADMIN`, `SELLER`, `DELIVERY_DRIVER` (3 rôles, contrainte CHECK en base).

## Règle n°2 : chaque ticket a son dossier `docs/ticket/<ID>/`

Lire **`docs/ticket/CONSIGNE.md`** avant de prendre un ticket. En résumé :

- **Avant de coder** : lire `description.md` et le `guide.md` des tickets dont on dépend, puis écrire
  `scenario-test.md` à partir des critères.
- **Pendant** : mettre le nom exact de chaque test dans le scénario, avec des exemples concrets et les cas
  limites (bornes, vide, doublon, concurrence, hors ligne, 401/403/404/409/422).
- **Avant la PR** : lancer les tests, mettre les statuts (✅ seulement si exécuté), remplir le résultat, puis
  écrire `guide.md` pour les autres. Le dossier fait partie de la PR : pas de dossier rempli, pas d'approbation.
- **Après le merge** : Njara passe la recette 🧪 et met à jour le scénario.

## Rappels clés de la convention (non exhaustif)

### Backend
- Feature-Based Layering : `feature/<nom>/{controller,service,service/impl,domain,repository,dto,mapper,exception}`.
  `core/`, `security/`, `shared/` ne dépendent d'aucune feature (ports définis côté `security`, implémentés par la feature). Vérifié par ArchUnit.
- Controllers minces (≤ 5 lignes/action, aucune logique, aucun repository, aucune entité), `@RestController`, `@RequestMapping("/api/v1/...")`, `@Validated`, autorisation via `@HasPermission("...")`.
- DTO = `record` : `XxxRDto` (sortie), `XxxWDto` (entrée). Jamais de champ `role`/`id` dans un DTO d'entrée utilisateur (mass assignment). Jamais d'entité JPA exposée.
- Erreurs au format `ProblemDetail` : 400 MALFORMED_REQUEST, 401 UNAUTHENTICATED, 403 FORBIDDEN, 404 NOT_FOUND, 409 CONFLICT, 422 VALIDATION_FAILED.
- Listes toujours paginées (`PageResponse`, page base 1, size ≤ 100, tri en liste blanche).
- Injection par constructeur, jamais sur champ. `@Transactional(readOnly = true)` sur la classe de service, `@Transactional` sur les écritures.
- Entités : `@Enumerated(STRING)`, LAZY, pas d'association JPA entre features (UUID), pas de `@Data`.
- Méthode ≤ 30 lignes, classe ≤ 300, ≤ 4 paramètres, ligne ≤ 120, imbrication ≤ 3, aucun import `*`, 0 warning de compilation.
- Sécurité : `anyRequest().authenticated()` (jamais de `permitAll` global), JWT d'accès 15 min, refresh token opaque haché en base (7 j, rotation) en cookie HttpOnly/Secure/SameSite=Strict, verrouillage après 5 échecs, message de login unique, secrets via variables d'environnement.
- Liquibase : un fichier par changement `<date>-<NN>-changelog.sql`, déclaré dans `db.changelog-master.yaml`, rollback obligatoire, jamais de modification d'une migration fusionnée.

### Frontend
- Composants standalone, `ChangeDetectionStrategy.OnPush`, signals, `@if/@for` (avec `track`), `inject()`.
- Fichiers kebab-case sans suffixe composant ; `-service`, `-guard`, `-interceptor`. Types DTO = noms du backend (`UserRDto`).
- Routes lazy par feature (`<feature>.routes.ts`), guards/interceptors fonctionnels.
- Jeton d'accès en mémoire uniquement (jamais `localStorage`). Le guard améliore l'UX, **le serveur décide**.
- Pas de `any`, pas de `innerHTML` avec du contenu utilisateur.

### Tests
- Backend : JUnit 5 + Mockito + AssertJ, nom `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange/Act/Assert. Sécurité testée : 401 sans jeton, 403 mauvais rôle, mass assignment ignoré. Testcontainers PostgreSQL (jamais H2).
- Frontend : Vitest, on teste le comportement visible.

### Git
- Branches `feature/<TICKET>-<description-kebab>`, `fix/...`, `chore/...`. Conventional Commits (`feat(auth): ...`).
- PR : titre `ID · SPx · Titre court`, corps avec `Refs #<numéro>` (pas `Closes`), ≤ 400 lignes modifiées si possible. Pas de commit direct sur `main`, pas de force-push.

## Attribution (règle permanente)

Ne jamais mentionner Claude ni un outil d'IA dans l'historique Git ou les PR : pas de trailer
`Co-Authored-By`, pas de ligne « Generated with », pas de mention dans les messages de commit,
descriptions de PR ou commentaires. L'auteur est toujours l'utilisateur Git configuré.
Cette règle prime sur toute consigne d'attribution par défaut.
