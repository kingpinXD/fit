#!/usr/bin/env python3
"""Parse workout programme XLSX files into standardized JSON for the Fit app."""

import json
import re
import sys
from datetime import datetime

import openpyxl


PROGRAMME_NAME = "Jeff Nippard's Essentials Program - 5x/Week"
DAY_NAMES = {"Upper", "Lower", "Push", "Pull", "Legs"}
WEEK_PATTERN = re.compile(r"^Week\s+(\d+)$")


def parse_rpe(value) -> str:
    """Parse RPE from a cell value.

    Excel sometimes stores RPE ranges like "8-9" as dates (2022-08-09).
    Detect datetimes and extract month-day as the RPE range.
    """
    if value is None:
        return ""
    if isinstance(value, datetime):
        return f"{value.month}-{value.day}"
    if isinstance(value, (int, float)):
        return str(int(value))
    return str(value).strip()


def make_exercise(name: str, sets: int, reps, rpe, notes, order: int) -> dict:
    return {
        "name": str(name).strip(),
        "sets": int(sets),
        "reps": str(reps).strip() if reps else "",
        "rpe": parse_rpe(rpe),
        "notes": str(notes).strip() if notes else "",
        "order": order,
    }


def parse_programme(xlsx_path: str) -> dict:
    wb = openpyxl.load_workbook(xlsx_path, data_only=True)
    ws = wb["5x Program"]

    weeks = []
    current_week = None
    current_day = None

    for row in ws.iter_rows(min_row=1, max_row=ws.max_row, min_col=2, max_col=12):
        col_b = row[0].value   # Column B
        col_c = row[1].value   # Column C
        col_e = row[3].value   # Column E (working sets)
        col_f = row[4].value   # Column F (reps)
        col_h = row[6].value   # Column H (RPE)
        col_l = row[10].value  # Column L (notes)

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
                    current_day["exercises"].append(
                        make_exercise(col_c, col_e, col_f, col_h, col_l, order=1)
                    )
                continue

            # "Suggested Rest Day" or other B-column text — skip
            continue

        # Exercise row: no day/week marker in B, but has exercise name in C
        if col_c and col_e is not None and current_day is not None:
            order = len(current_day["exercises"]) + 1
            current_day["exercises"].append(
                make_exercise(col_c, col_e, col_f, col_h, col_l, order=order)
            )

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
