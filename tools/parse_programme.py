#!/usr/bin/env python3
"""Parse workout programme XLSX files into standardized JSON for the Fit app."""

import json
import re
import sys

import openpyxl


PROGRAMME_NAME = "Jeff Nippard's Essentials Program - 5x/Week"
DAY_NAMES = {"Upper", "Lower", "Push", "Pull", "Legs"}
WEEK_PATTERN = re.compile(r"^Week\s+(\d+)$")


def parse_programme(xlsx_path: str) -> dict:
    wb = openpyxl.load_workbook(xlsx_path, data_only=True)
    ws = wb["5x Program"]

    weeks = []
    current_week = None
    current_day = None

    for row in ws.iter_rows(min_row=1, max_row=ws.max_row, min_col=2, max_col=6):
        col_b = row[0].value  # Column B
        col_c = row[1].value  # Column C
        col_e = row[3].value  # Column E (working sets)
        col_f = row[4].value  # Column F (reps)

        if col_b and isinstance(col_b, str):
            col_b = col_b.strip()

            # Week header row (e.g., "Week 1")
            week_match = WEEK_PATTERN.match(col_b)
            if week_match:
                current_week = {"week": int(week_match.group(1)), "days": []}
                weeks.append(current_week)
                current_day = None
                continue

            # Day label row (e.g., "Upper") — may also have first exercise in col C
            if col_b in DAY_NAMES and current_week is not None:
                current_day = {"day": col_b, "exercises": []}
                current_week["days"].append(current_day)
                if col_c and col_e is not None:
                    current_day["exercises"].append({
                        "name": str(col_c).strip(),
                        "sets": int(col_e),
                        "reps": str(col_f).strip() if col_f else "",
                    })
                continue

            # "Suggested Rest Day" or other B-column text — skip
            continue

        # Exercise row: no day/week marker in B, but has exercise name in C
        if col_c and col_e is not None and current_day is not None:
            current_day["exercises"].append({
                "name": str(col_c).strip(),
                "sets": int(col_e),
                "reps": str(col_f).strip() if col_f else "",
            })

    return {"name": PROGRAMME_NAME, "weeks": weeks}


def main():
    if len(sys.argv) != 3:
        print(f"Usage: {sys.argv[0]} <input.xlsx> <output.json>", file=sys.stderr)
        sys.exit(1)

    xlsx_path = sys.argv[1]
    json_path = sys.argv[2]

    programme = parse_programme(xlsx_path)

    with open(json_path, "w") as f:
        json.dump(programme, f, indent=2)

    # Summary
    total_weeks = len(programme["weeks"])
    for w in programme["weeks"]:
        days_summary = ", ".join(
            f"{d['day']}({len(d['exercises'])})" for d in w["days"]
        )
        print(f"  Week {w['week']}: {len(w['days'])} days — {days_summary}")
    print(f"\nWrote {total_weeks} weeks to {json_path}")


if __name__ == "__main__":
    main()
