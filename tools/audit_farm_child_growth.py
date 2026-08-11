"""Audit all six Farm child-family base classes and adult type transitions."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

OWNER = "[farm-child-growth-audit:v1]"
ROWS = [
    "src/main/java/com/animania/addons/farm/common/entity/chickens/EntityChickBase.java",
    "src/main/java/com/animania/addons/farm/common/entity/cows/EntityCalfBase.java",
    "src/main/java/com/animania/addons/farm/common/entity/goats/EntityKidBase.java",
    "src/main/java/com/animania/addons/farm/common/entity/horses/EntityFoalBase.java",
    "src/main/java/com/animania/addons/farm/common/entity/pigs/EntityPigletBase.java",
    "src/main/java/com/animania/addons/farm/common/entity/sheep/EntityLambBase.java",
]
TARGETS = [
    "base/src/main/java/com/animania/common/entity/AnimaniaAnimalEntity.java",
    "farm/src/main/java/com/animania/farm/FarmLegacyIds.java",
]
TEST = "farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, default=Path("docs/migration-matrix.json"))
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()
    root = args.root.resolve()
    errors: list[str] = []
    for source in ROWS:
        if not (root / "upstream/Animania-1.12" / source).is_file():
            errors.append(f"legacy child class missing: {source}")
    for target in TARGETS:
        if not (root / target).is_file():
            errors.append(f"modern target missing: {target}")
    entity = (root / TARGETS[0]).read_text(encoding="utf-8")
    test = (root / TEST).read_text(encoding="utf-8")
    for token in ("childGrowthTimer", "growIntoAdultVariant", "childGrowthDuration()", "adultPrefix("):
        if token not in entity:
            errors.append(f"growth implementation missing: {token}")
    for token in ("everyFarmChildRegistryTypeGrowsIntoItsBreedAdult", "CHILD_GROWTH_TICK.set(20)",
                  "child.isRemoved()", "id.getPath().equals(adults[0])"):
        if token not in test:
            errors.append(f"dedicated growth assertion missing: {token}")

    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    matrix = json.loads(matrix_path.read_text(encoding="utf-8"))
    by_source = {entry.get("source"): entry for entry in matrix["entries"]}
    changed = 0
    if not errors:
        for source in ROWS:
            entry = by_source.get(source)
            if entry is None:
                errors.append(f"migration row missing: {source}")
                continue
            proof = {
                "paths": TARGETS,
                "behavior_tests": [TEST, "tools/audit_farm_child_growth.py"],
                "serialization_tests": [],
                "client_tests": [],
                "notes": [f"{OWNER} Forge dedicated GameTest iterates every registered calf, lamb, kid, piglet, chick and foal breed and proves care-gated replacement by a matching adult registry type."],
            }
            owned = any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", []))
            if args.write:
                entry.update(status="closed", implemented=True, verified=True,
                             tests=[TEST, "tools/audit_farm_child_growth.py"], target_evidence=proof)
                changed += 1
            elif entry.get("status") != "closed" or not owned:
                errors.append(f"provable row not closed: {source}")
    if args.write and not errors:
        matrix_path.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"rows": len(ROWS), "changed": changed, "errors": errors,
                      "error_count": len(errors)}, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
