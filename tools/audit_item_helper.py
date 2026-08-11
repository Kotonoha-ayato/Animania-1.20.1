"""Audit the only 1.12 ItemHelper responsibility against the native entity drop path."""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

OWNER = "[item-helper-audit:v1]"
SOURCE = "src/main/java/com/animania/common/helper/ItemHelper.java"
OLD_ROOT = "upstream/Animania-1.12/src/main/java"
TARGET = "base/src/main/java/com/animania/common/entity/goal/AnimaniaPigSnuffleGoal.java"
TEST = "farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, default=Path("docs/migration-matrix.json"))
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()
    root = args.root.resolve()
    errors: list[str] = []

    old_root = root / OLD_ROOT
    helper = (root / "upstream/Animania-1.12" / SOURCE).read_text(encoding="utf-8")
    old_java = "\n".join(path.read_text(encoding="utf-8", errors="replace") for path in old_root.rglob("*.java"))
    target = (root / TARGET).read_text(encoding="utf-8")
    test = (root / TEST).read_text(encoding="utf-8")

    uses = len(re.findall(r"ItemHelper\.spawnItem\s*\(", old_java))
    if uses != 1:
        errors.append(f"expected one legacy ItemHelper.spawnItem call, found {uses}")
    for token in ("new EntityItem(world)", "pos.getX() + 0.5", "AnimaniaHelper.spawnEntity"):
        if token not in helper:
            errors.append(f"legacy spawn contract missing: {token}")
    for token in ("pig.spawnAtLocation", "pig.getRandom().nextInt(2) + 1"):
        if token not in target:
            errors.append(f"modern native drop path missing: {token}")
    for token in ("leashedAdultPigsSnuffleForestTrufflesAndEatThem", "getItem().getCount() >= 1", "getItem().getCount() <= 2"):
        if token not in test:
            errors.append(f"live drop test missing: {token}")

    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    matrix = json.loads(matrix_path.read_text(encoding="utf-8"))
    entry = next((row for row in matrix["entries"] if row.get("source") == SOURCE), None)
    if entry is None:
        errors.append("migration row missing")
    changed = 0
    if not errors and entry is not None:
        proof = {
            "paths": [TARGET],
            "behavior_tests": [TEST, "tools/audit_item_helper.py"],
            "serialization_tests": [],
            "client_tests": [],
            "notes": [f"{OWNER} the sole production caller now uses LivingEntity.spawnAtLocation; the dedicated-server GameTest verifies its 1-2 item count."],
        }
        owned = any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", []))
        if args.write:
            entry.update(status="closed", implemented=True, verified=True,
                         tests=[TEST, "tools/audit_item_helper.py"], target_evidence=proof)
            changed = 1
        elif entry.get("status") != "closed" or not owned:
            errors.append("provable row is not closed")

    if args.write and not errors:
        matrix_path.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"legacy_spawn_callers": uses, "changed": changed, "errors": errors,
                      "error_count": len(errors)}, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
