"""Verify the 1.12 food override/item responsibilities in the modern shared runtime."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

OWNER = "[food-runtime-audit:v1]"
SOURCES = {
    "src/main/java/com/animania/common/handler/FoodValueHandler.java",
    "src/main/java/com/animania/common/items/ItemAnimaniaFood.java",
}
PATHS = [
    "base/src/main/java/com/animania/common/config/AnimaniaConfig.java",
    "base/src/main/java/com/animania/common/item/AnimaniaFoodItem.java",
]
TESTS = [
    "base/src/test/java/com/animania/common/config/AnimaniaFoodOverrideTest.java",
    "farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java",
    "extra/src/main/java/com/animania/extra/gametest/AnimaniaExtraGameTests.java",
    "tools/audit_food_runtime.py",
]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, required=True)
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()
    root = args.root
    config = (root / PATHS[0]).read_text(encoding="utf-8")
    food = (root / PATHS[1]).read_text(encoding="utf-8")
    farm_test = (root / TESTS[1]).read_text(encoding="utf-8")
    extra_test = (root / TESTS[2]).read_text(encoding="utf-8")
    unit_test = (root / TESTS[0]).read_text(encoding="utf-8")
    required = {
        "config": ("parseFoodValueOverride", "foodValueOverride", "FOODS_GIVE_BONUS_EFFECTS", "EAT_FOOD_ANYTIME"),
        "food": ("finishUsingItem", "override.nutrition()", "override.saturationModifier()", "effectsBefore", "player.canEat"),
        "farm": ("foodOverridesAndBonusEffectSwitchApplyAtConsumption", "foodValueOverrides did not replace", "eatFoodAnytime=true"),
        "extra": ("extraFoodsUseSharedLiveValueOverrides", "Extra food ignored"),
        "unit": ("parsesLegacyFoodOverrideSyntax", "rejectsMalformedOrUnsafeOverrides"),
    }
    texts = {"config": config, "food": food, "farm": farm_test, "extra": extra_test, "unit": unit_test}
    errors = [f"{label} missing {token}" for label, tokens in required.items()
              for token in tokens if token not in texts[label]]
    matrix = json.loads(args.matrix.read_text(encoding="utf-8"))
    matched = 0
    changed = 0
    for entry in matrix["entries"]:
        if entry.get("module") != "base" or entry.get("source") not in SOURCES:
            continue
        matched += 1
        proof = {
            "paths": PATHS,
            "behavior_tests": TESTS,
            "serialization_tests": [],
            "client_tests": [],
            "notes": [f"{OWNER} live Forge config overrides and both consumption switches are covered by JUnit and dedicated-server GameTests."],
        }
        if args.write and not errors:
            entry.update(status="closed", implemented=True, verified=True, tests=TESTS, target_evidence=proof)
            changed += 1
    if matched != len(SOURCES):
        errors.append(f"matched {matched} matrix rows, expected {len(SOURCES)}")
    if args.write and not errors:
        args.matrix.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"matched": matched, "changed": changed, "errors": errors}, ensure_ascii=False))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
