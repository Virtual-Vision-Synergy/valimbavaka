# Valimbavaka — consignes pour les agents (Codex et autres)

Les règles du projet sont dans **`CLAUDE.md`** (convention VVS, structure, tests, Git, attribution) : le lire et
l'appliquer entièrement. Ce fichier ne les recopie pas.

## Traiter un ticket

Quand on te demande de traiter un ticket `<ID>` (ex. `ART-03`) :

1. Suivre le workflow de **`.claude/skills/ticket/SKILL.md`** en remplaçant `$ARGUMENTS` par l'identifiant du ticket.
   C'est la source unique : ne pas dupliquer ses étapes ici.
2. Ignorer ce qui est propre à Claude Code : la section « Modèles » (sous-agents Haiku et Sonnet, `/model`) et
   l'en-tête YAML. Faire la lecture ciblée toi-même (guides des dépendances, `grep` dans la convention).
3. Respecter `docs/ticket/CONSIGNE.md` pour le scénario de test et le guide.

## Rappels qui priment

- Ne pas lancer `mvn verify` ni `ng test` : écrire les tests, statuts à ☐, « Résultat de l'exécution » à lancer.
- Aucune mention d'un outil d'IA dans Git ou les PR : pas de `Co-Authored-By`, pas de « Generated with ».
- Plan de 15 lignes validé avant de coder ; accord de l'utilisateur avant le push et la PR.
