"""Prove that the legacy mud-damage event was a completely commented no-op."""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

OWNER = "[dead-mud-damage-event-audit:v1]"
SOURCE = "src/main/java/com/animania/addons/farm/common/event/EventMudDamageCanceller.java"
SELF = "tools/audit_dead_mud_damage_event.py"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, default=Path("docs/migration-matrix.json"))
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()
    root = args.root.resolve()
    errors: list[str] = []
    old = (root / "upstream/Animania-1.12" / SOURCE).read_text(encoding="utf-8")
    without_comments = re.sub(r"/\*.*?\*/|//[^\n]*", "", old, flags=re.DOTALL)
    method = re.search(r"notifyAttack\s*\([^)]*\)\s*\{(.*?)\}", without_comments, re.DOTALL)
    if method is None or method.group(1).strip():
        errors.append("legacy notifyAttack is no longer provably an empty method")
    if "@SubscribeEvent" not in old or "LivingAttackEvent" not in old:
        errors.append("unexpected legacy source surface")

    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    matrix = json.loads(matrix_path.read_text(encoding="utf-8"))
    entry = next((row for row in matrix["entries"] if row.get("source") == SOURCE), None)
    changed = 0
    if not errors:
        if entry is None:
            errors.append(f"migration row missing: {SOURCE}")
        else:
            proof = {
                "paths": [SELF],
                "behavior_tests": [SELF],
                "serialization_tests": [],
                "client_tests": [],
                "notes": [f"{OWNER} Source-derived proof: the subscribed 1.12 method contains no executable statement; its intended mud-wall cancellation block was entirely commented out."],
            }
            owned = any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", []))
            if args.write:
                entry.update(status="closed", implemented=True, verified=True, tests=[SELF], target_evidence=proof)
                changed = 1
            elif entry.get("status") != "closed" or not owned:
                errors.append("provably dead row is not closed")
    if args.write and not errors:
        matrix_path.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"rows": 1, "changed": changed, "errors": errors,
                      "error_count": len(errors)}, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
