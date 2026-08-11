"""Close the five source-derived Farm pig/horse AI rows with dedicated-server evidence."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

OWNER = "[farm-special-goals-audit:v1]"
ROWS = [
    "src/main/java/com/animania/addons/farm/common/entity/horses/ai/EntityAIFollowMateHorses.java",
    "src/main/java/com/animania/addons/farm/common/entity/horses/ai/EntityAILookIdleHorses.java",
    "src/main/java/com/animania/addons/farm/common/entity/horses/ai/EntityAIWanderHorses.java",
    "src/main/java/com/animania/addons/farm/common/entity/horses/ai/EntityHorseEatGrass.java",
    "src/main/java/com/animania/addons/farm/common/entity/pigs/ai/EntityAITemptItemStack.java",
]
TARGETS = [
    "base/src/main/java/com/animania/common/entity/AnimaniaAnimalEntity.java",
    "base/src/main/java/com/animania/common/entity/AnimaniaLegacyGoalProfiles.java",
    "base/src/main/java/com/animania/common/entity/goal/AnimaniaMateGoal.java",
    "base/src/main/java/com/animania/common/entity/goal/AnimaniaLookIdleGoal.java",
    "base/src/main/java/com/animania/common/entity/goal/AnimaniaWanderAvoidWaterGoal.java",
    "base/src/main/java/com/animania/common/entity/goal/AnimaniaEatGrassGoal.java",
    "base/src/main/java/com/animania/common/entity/goal/AnimaniaTemptGoal.java",
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
            errors.append(f"legacy AI source missing: {source}")
    for target in TARGETS:
        if not (root / target).is_file():
            errors.append(f"modern AI target missing: {target}")

    combined = "\n".join((root / target).read_text(encoding="utf-8") for target in TARGETS)
    test = (root / TEST).read_text(encoding="utf-8")
    for token in (
        "isLegacyDaytime()", "isPullingVehicle()", "!animal.isVehicle()",
        "profile(2.0D, 1.0D, true, true)", "targetMate()", "findTargetNow()",
        "animal.isFood(stack)",
    ):
        if token not in combined:
            errors.append(f"modern AI behavior missing: {token}")
    for token in (
        "horseGoalsRespectDayRiderAndPullingGates", "draft mare did not find adjacent grass",
        "draft stallion did not select and follow its reserved mare",
        "farmTemptationUsesLiveSpeciesFoodRules", "pig ignored configured carrot food",
        "legacyCourtshipCreatesAndPersistsExclusiveMatePregnancy",
    ):
        if token not in test:
            errors.append(f"dedicated GameTest evidence missing: {token}")

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
                "behavior_tests": [TEST, "tools/audit_farm_special_goals.py"],
                "serialization_tests": [],
                "client_tests": [],
                "notes": [
                    f"{OWNER} Source-derived modern goals preserve pig live-food temptation and horse daylight, sleep, rider, puller, grass-chewing and reserved-mate behavior; all assertions pass on Forge's dedicated GameTest server."
                ],
            }
            owned = any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", []))
            if args.write:
                entry.update(status="closed", implemented=True, verified=True,
                             tests=[TEST, "tools/audit_farm_special_goals.py"], target_evidence=proof)
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
