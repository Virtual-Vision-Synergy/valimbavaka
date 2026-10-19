#!/usr/bin/env python3
"""
Convertit les rapports Checkstyle (XML) et ESLint (JSON) en annotations GitHub, avec le niveau
de la grille §15 de la convention VVS porté par la sévérité configurée dans chaque outil :
  Checkstyle error   / ESLint error -> P0 (bloque la PR)
  Checkstyle warning / ESLint warn  -> P1
  Checkstyle info                   -> P2

Usage:
  python3 annotate_findings.py --checkstyle valimbavaka-core/target/checkstyle-result.xml
  python3 annotate_findings.py --eslint valimbavaka-ui/eslint-report.json
"""

import argparse
import json
import os
import sys
import xml.etree.ElementTree as ElementTree

from check_conventions import Report

CHECKSTYLE_LEVELS = {"error": "P0", "warning": "P1", "info": "P2"}
ESLINT_LEVELS = {2: "P0", 1: "P1"}


def relative(path):
    return os.path.relpath(path, os.getcwd()).replace(os.sep, "/")


def read_checkstyle(report, path):
    for file in ElementTree.parse(path).getroot().iter("file"):
        for error in file.iter("error"):
            # source = "<package>.<Nom>Check" ou "<package>.<Nom>Check#<id>" : l'id, plus parlant, prime.
            rule = error.get("source", "").rsplit(".", 1)[-1].removesuffix("Check").rsplit("#", 1)[-1]
            level = CHECKSTYLE_LEVELS.get(error.get("severity"), "P1")
            report.add(level, f"[{rule}] {error.get('message')}", relative(file.get("name")), error.get("line"))


def read_eslint(report, path):
    with open(path, encoding="utf-8") as source:
        for file in json.load(source):
            for message in file["messages"]:
                level = ESLINT_LEVELS.get(message["severity"], "P1")
                rule = message.get("ruleId") or "eslint"
                report.add(level, f"[{rule}] {message['message']}", relative(file["filePath"]), message.get("line"))


def main():
    parser = argparse.ArgumentParser(description="Annotations GitHub P0/P1/P2 depuis Checkstyle ou ESLint.")
    parser.add_argument("--checkstyle")
    parser.add_argument("--eslint")
    parser.add_argument("--title", default="Analyse statique")
    args = parser.parse_args()
    sys.stdout.reconfigure(encoding="utf-8")

    report = Report()
    if args.checkstyle:
        read_checkstyle(report, args.checkstyle)
    if args.eslint:
        read_eslint(report, args.eslint)
    counts = report.emit()
    report.write_summary(args.title, counts)
    return 1 if counts["P0"] else 0


if __name__ == "__main__":
    sys.exit(main())
