#!/usr/bin/env python3
"""
Controle de la convention VVS V2.1 sur ce que Checkstyle, PMD et ESLint ne voient pas :
hygiene de la PR (titre, corps, branche, commits, taille) et migrations Liquibase.

Chaque ecart porte un niveau de la grille §15 de la convention :
  P0 -> annotation "error"   (bloque la PR)
  P1 -> annotation "warning" (a corriger avant la fusion, ne bloque pas)
  P2 -> annotation "notice"  (suggestion)

Les metadonnees de la PR sont lues dans l'environnement (PR_TITLE, PR_BODY, HEAD_REF) ;
absentes (execution locale), les controles correspondants sont ignores.

Usage:
  python3 check_conventions.py --base origin/develop             # local
  python3 check_conventions.py --base $BASE_SHA --head $HEAD_SHA # CI
"""

import argparse
import os
import re
import subprocess
import sys

CHANGELOG_DIR = "valimbavaka-core/src/main/resources/db/changelog/"
CHANGES_DIR = CHANGELOG_DIR + "changes/"
MASTER_CHANGELOG = CHANGELOG_DIR + "db.changelog-master.yaml"
MAX_PR_LINES = 400

TITLE_PATTERN = re.compile(r"^[A-Z]+-\d+ · SP\d+ · \S.*$")
BRANCH_PATTERN = re.compile(r"^((feature|fix)/[A-Z]+-\d+-[a-z0-9-]+|chore/[a-z0-9-]+)$")
COMMIT_PATTERN = re.compile(r"^(feat|fix|refactor|test|docs|chore|perf|build|ci|style|revert)(\([a-z0-9-]+\))?!?: \S")
CHANGELOG_NAME_PATTERN = re.compile(r"^\d{4}_\d{2}_\d{2}-\d{2}-changelog\.sql$")
# Regle permanente du projet : aucune mention d'un outil d'IA dans l'historique Git ni dans les PR.
AI_MENTION_PATTERN = re.compile(r"(?i)(?<![\w.])(claude|anthropic|chatgpt|openai|copilot)(?!\.md)\b|generated with|🤖")
# Branches qui ne suivent pas le workflow ticket : PR de release et mises a jour automatiques.
EXEMPT_BRANCHES = re.compile(r"^(develop|dependabot/.+|renovate/.+)$")
# Exclus du calcul de taille (§13.2 : hors fichiers generes et migrations).
SIZE_EXCLUDED = re.compile(r"(package-lock\.json$|/changelog/changes/|qodana\.sarif\.json$|\.png$|\.svg$)")

LEVELS = {"P0": "error", "P1": "warning", "P2": "notice"}


class Report:
    def __init__(self):
        self.findings = []

    def add(self, level, message, file=None, line=None):
        self.findings.append((level, message, file, line))

    def emit(self):
        for level, message, file, line in self.findings:
            params = f"title={level} convention VVS"
            if file:
                params = f"file={file},line={line or 1},{params}"
            print(f"::{LEVELS[level]} {params}::{message}")
        counts = {level: sum(1 for f in self.findings if f[0] == level) for level in LEVELS}
        print(f"Convention VVS : P0={counts['P0']} P1={counts['P1']} P2={counts['P2']}")
        return counts

    @staticmethod
    def write_summary(title, counts):
        summary = os.environ.get("GITHUB_STEP_SUMMARY")
        if not summary:
            return
        with open(summary, "a", encoding="utf-8") as out:
            out.write(f"### {title}\n\n| P0 (bloquant) | P1 (avant fusion) | P2 (suggestion) |\n|---|---|---|\n")
            out.write(f"| {counts['P0']} | {counts['P1']} | {counts['P2']} |\n\n")


def git(*args):
    return subprocess.run(["git", *args], check=True, capture_output=True, text=True, encoding="utf-8").stdout


def check_pull_request(report):
    title = os.environ.get("PR_TITLE")
    body = os.environ.get("PR_BODY") or ""
    branch = os.environ.get("HEAD_REF")
    if not title or not branch:
        return
    if EXEMPT_BRANCHES.match(branch):
        return
    if not TITLE_PATTERN.match(title):
        report.add("P1", f"Titre de PR « {title} » : format attendu « ID · SPx · Titre court » (§13.2).")
    if not BRANCH_PATTERN.match(branch):
        report.add("P1", f"Branche « {branch} » : attendu feature/<TICKET>-<desc>, fix/<TICKET>-<desc> ou chore/<desc>.")
    if re.search(r"(?i)\b(close[sd]?|fix(e[sd])?|resolve[sd]?) #\d+", body):
        report.add("P0", "Le corps de la PR ferme un ticket (Closes/Fixes #) : utiliser « Refs #<numéro> » (§13.2).")
    elif not re.search(r"Refs #\d+", body):
        report.add("P1", "Le corps de la PR doit référencer le ticket avec « Refs #<numéro> » (§13.2).")
    if AI_MENTION_PATTERN.search(title + "\n" + body):
        report.add("P0", "La PR mentionne un outil d'IA : interdit par la règle d'attribution du projet.")


def check_commits(report, base, head):
    log = git("log", "--no-merges", "--format=%H%x1f%s%x1f%B%x1e", f"{base}..{head}")
    for entry in filter(None, (e.strip() for e in log.split("\x1e"))):
        sha, subject, message = entry.split("\x1f", 2)
        if not COMMIT_PATTERN.match(subject):
            report.add("P1", f"Commit {sha[:7]} « {subject} » : format Conventional Commits attendu (type(scope): ...).")
        if AI_MENTION_PATTERN.search(message):
            report.add("P0", f"Commit {sha[:7]} mentionne un outil d'IA : interdit par la règle d'attribution du projet.")


def check_size(report, base, head):
    changed = 0
    for row in git("diff", "--numstat", f"{base}...{head}").splitlines():
        added, deleted, path = row.split("\t", 2)
        if added != "-" and not SIZE_EXCLUDED.search(path):
            changed += int(added) + int(deleted)
    if changed > MAX_PR_LINES:
        report.add("P2", f"PR de {changed} lignes modifiées (cible ≤ {MAX_PR_LINES}, §13.2) : envisager de la découper.")


def check_liquibase(report, base, head):
    for row in git("diff", "--name-status", f"{base}...{head}", "--", CHANGES_DIR).splitlines():
        status, *paths = row.split("\t")
        path = paths[-1]
        if status[0] in "MDR":
            report.add("P0", "Migration déjà fusionnée modifiée, supprimée ou renommée : créer un nouveau changelog.",
                       paths[0], 1)
        elif status[0] == "A":
            check_new_changelog(report, path, head)


def check_new_changelog(report, path, head):
    name = path.rsplit("/", 1)[-1]
    if not CHANGELOG_NAME_PATTERN.match(name):
        report.add("P0", f"Nom de changelog « {name} » : format attendu AAAA_MM_JJ-NN-changelog.sql.", path, 1)
    if f"changes/{name}" not in git("show", f"{head}:{MASTER_CHANGELOG}"):
        report.add("P0", f"« {name} » n'est pas déclaré dans db.changelog-master.yaml.", path, 1)
    changeset_line, has_rollback = None, True
    for number, line in enumerate(git("show", f"{head}:{path}").splitlines(), start=1):
        if line.startswith("-- changeset"):
            if not has_rollback:
                report.add("P0", "Changeset sans instruction « -- rollback » (rollback obligatoire).", path, changeset_line)
            changeset_line, has_rollback = number, False
        elif line.startswith("-- rollback"):
            has_rollback = True
    if not has_rollback:
        report.add("P0", "Changeset sans instruction « -- rollback » (rollback obligatoire).", path, changeset_line)


def main():
    parser = argparse.ArgumentParser(description="Contrôle de la convention VVS (PR et migrations).")
    parser.add_argument("--base", default="origin/develop")
    parser.add_argument("--head", default="HEAD")
    args = parser.parse_args()
    sys.stdout.reconfigure(encoding="utf-8")

    report = Report()
    check_pull_request(report)
    check_commits(report, args.base, args.head)
    check_size(report, args.base, args.head)
    check_liquibase(report, args.base, args.head)
    counts = report.emit()
    report.write_summary("PR et migrations", counts)
    return 1 if counts["P0"] else 0


if __name__ == "__main__":
    sys.exit(main())
