#!/usr/bin/env python3
"""
Runs periodically (via GitHub Actions schedule). For every item on Project #15:
  - If Status = "In Progress" and Start date is empty -> sets Start date = today
  - If an Iteration is assigned and Target date is empty -> sets Target date =
    that iteration's start date + its duration (the sprint's end date)

Never overwrites a value that's already set — purely fills gaps.

Usage:
  python3 sync_dates.py --dry-run          # shows what would change, no writes
  python3 sync_dates.py                    # applies changes
  python3 sync_dates.py --test-issue 10    # only touches issue #10, for testing
"""

import json
import subprocess
import sys
import time
from datetime import date, timedelta

REPO = "Virtual-Vision-Synergy/valimbavaka"
ORG = "Virtual-Vision-Synergy"
PROJECT_NUMBER = 15

STATUS_FIELD_NAME = "Status"
ITERATION_FIELD_NAME = "Iteration"
IN_PROGRESS_OPTION_NAME = "In Progress"

START_DATE_FIELD_ID = 33339971
TARGET_DATE_FIELD_ID = 33339972

ITEMS_QUERY = """
query($org: String!, $number: Int!, $after: String) {
  organization(login: $org) {
    projectV2(number: $number) {
      items(first: 100, after: $after) {
        pageInfo { hasNextPage endCursor }
        nodes {
          id
          content {
            ... on Issue { number }
          }
          fieldValues(first: 20) {
            nodes {
              __typename
              ... on ProjectV2ItemFieldSingleSelectValue {
                name
                field { ... on ProjectV2FieldCommon { name } }
              }
              ... on ProjectV2ItemFieldIterationValue {
                startDate
                duration
                field { ... on ProjectV2FieldCommon { name } }
              }
            }
          }
        }
      }
    }
  }
}
"""


def run_gh(cmd, input=None, retries=2):
    last = None
    for attempt in range(retries + 1):
        last = subprocess.run(cmd, input=input, capture_output=True, text=True, encoding="utf-8")
        if last.returncode == 0:
            return last
        transient = any(s in (last.stderr or "") for s in
                         ["timeout", "TLS handshake", "connection reset", "EOF"])
        if not transient or attempt == retries:
            return last
        time.sleep(2)
    return last


def fetch_all_items():
    items = []
    after = "null"
    while True:
        cmd = [
            "gh", "api", "graphql",
            "-f", f"query={ITEMS_QUERY}",
            "-F", f"org={ORG}",
            "-F", f"number={PROJECT_NUMBER}",
        ]
        if after != "null":
            cmd += ["-F", f"after={after}"]
        result = run_gh(cmd)
        if result.returncode != 0:
            print(f"[FATAL] could not fetch project items: {result.stderr.strip()}")
            sys.exit(1)
        data = json.loads(result.stdout)
        page = data["data"]["organization"]["projectV2"]["items"]
        items.extend(page["nodes"])
        if not page["pageInfo"]["hasNextPage"]:
            break
        after = page["pageInfo"]["endCursor"]
    return items


def parse_item(item):
    status = None
    iteration_start = None
    iteration_duration = None
    for fv in item.get("fieldValues", {}).get("nodes", []):
        field_name = (fv.get("field") or {}).get("name")
        if fv.get("__typename") == "ProjectV2ItemFieldSingleSelectValue" and field_name == STATUS_FIELD_NAME:
            status = fv.get("name")
        elif fv.get("__typename") == "ProjectV2ItemFieldIterationValue" and field_name == ITERATION_FIELD_NAME:
            iteration_start = fv.get("startDate")
            iteration_duration = fv.get("duration")
    issue_number = (item.get("content") or {}).get("number")
    return issue_number, status, iteration_start, iteration_duration


def get_current_date_values(issue_number):
    """Reads the issue's current Start date / Target date via REST.

    IMPORTANT: the GET response is a bare JSON array (not wrapped in a
    dict), and each entry uses the key `issue_field_id` — NOT `field_id`.
    `field_id` is only the key used when *writing* (POST/PUT). Mixing
    the two up means every read silently returns None for every field,
    which defeats the "never overwrite an existing value" guarantee and
    for Start date actively corrupts it (re-set to today on every run).
    """
    result = run_gh(["gh", "api", f"repos/{REPO}/issues/{issue_number}/issue-field-values"])
    if result.returncode != 0:
        return None, None
    values = json.loads(result.stdout)
    if isinstance(values, dict):
        # defensive: current API returns a bare list, but guard against
        # a future/alternate shape wrapping it in a dict
        values = values.get("issue_field_values", [])
    start, target = None, None
    for v in values:
        field_id = v.get("issue_field_id")  # <-- the actual fix
        if field_id == START_DATE_FIELD_ID:
            start = v.get("value")
        elif field_id == TARGET_DATE_FIELD_ID:
            target = v.get("value")
    return start, target


def set_date_values(issue_number, updates):
    payload = json.dumps({"issue_field_values": updates})
    result = run_gh(
        ["gh", "api", f"repos/{REPO}/issues/{issue_number}/issue-field-values",
         "-X", "POST", "--header", "Accept: application/vnd.github+json",
         "--input", "-"],
        input=payload,
    )
    return result


def main():
    dry_run = "--dry-run" in sys.argv
    test_issue = None
    if "--test-issue" in sys.argv:
        idx = sys.argv.index("--test-issue")
        test_issue = int(sys.argv[idx + 1])

    items = fetch_all_items()
    print(f"Fetched {len(items)} project items.")

    changed, skipped = 0, 0

    for item in items:
        issue_number, status, it_start, it_duration = parse_item(item)
        if not issue_number:
            continue
        if test_issue and issue_number != test_issue:
            continue

        current_start, current_target = get_current_date_values(issue_number)

        updates = []

        if status == IN_PROGRESS_OPTION_NAME and not current_start:
            today = date.today().isoformat()
            updates.append({"field_id": START_DATE_FIELD_ID, "value": today})

        if it_start and it_duration and not current_target:
            end = (date.fromisoformat(it_start) + timedelta(days=it_duration)).isoformat()
            updates.append({"field_id": TARGET_DATE_FIELD_ID, "value": end})

        if not updates:
            continue

        if dry_run:
            print(f"[DRY RUN] #{issue_number}: would set {updates}")
            changed += 1
            continue

        result = set_date_values(issue_number, updates)
        if result.returncode != 0:
            print(f"[WARN] #{issue_number}: could not set dates: {result.stderr.strip()}")
            skipped += 1
        else:
            print(f"[OK] #{issue_number}: set {updates}")
            changed += 1

    print(f"\nDone. Updated: {changed} · Skipped/errored: {skipped} · Total items: {len(items)}")


if __name__ == "__main__":
    main()
