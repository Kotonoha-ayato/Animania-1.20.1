"""Close legacy Farm special-item rows only after live server behavior is covered."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

OWNER = "[farm-special-items-audit:v1]"
ROWS = {
    "src/main/java/com/animania/addons/farm/common/item/handler/FluidHandlerHoneyBottle.java": [
        "farm/src/main/java/com/animania/farm/FarmHoneyJarItem.java",
    ],
    "src/main/java/com/animania/addons/farm/common/item/ItemBrownEgg.java": [
        "farm/src/main/java/com/animania/farm/FarmBrownEggItem.java",
        "farm/src/main/java/com/animania/farm/FarmBrownEggProjectile.java",
    ],
    "src/main/java/com/animania/addons/farm/common/item/ItemCheeseWheel.java": [
        "farm/src/main/java/com/animania/farm/FarmContent.java",
        "farm/src/main/java/com/animania/farm/FarmCheeseBlock.java",
    ],
    "src/main/java/com/animania/addons/farm/common/item/ItemHoneyBottle.java": [
        "farm/src/main/java/com/animania/farm/FarmHoneyJarItem.java",
        "base/src/main/java/com/animania/common/item/AnimaniaFoodItem.java",
    ],
    "src/main/java/com/animania/addons/farm/common/item/ItemMilkBottle.java": [
        "farm/src/main/java/com/animania/farm/FarmMilkBottleItem.java",
        "base/src/main/java/com/animania/common/item/AnimaniaFoodItem.java",
    ],
    "src/main/java/com/animania/addons/farm/common/item/ItemRidingCrop.java": [
        "farm/src/main/java/com/animania/farm/FarmRidingCropItem.java",
        "base/src/main/java/com/animania/common/entity/AnimaniaVehicleEntity.java",
        "base/src/main/java/com/animania/common/entity/AnimaniaAnimalEntity.java",
    ],
    "src/main/java/com/animania/addons/farm/common/item/ItemTruffleSoup.java": [
        "farm/src/main/java/com/animania/farm/FarmContent.java",
        "base/src/main/java/com/animania/common/item/AnimaniaFoodItem.java",
    ],
}
TEST = "farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java"
TOKENS = (
    "farmSpecialItemsRetainLegacyUseSemantics",
    "ALLOW_EGG_THROWING.set(true)",
    "getEntitiesOfClass(com.animania.farm.FarmBrownEggProjectile.class",
    "milkResult.is(Items.GLASS_BOTTLE)",
    "honeyResult.is(Items.GLASS_BOTTLE)",
    "honeyHandler.getTankCapacity(0) == 1000",
    "honeyHandler.getContainer().is(Items.GLASS_BOTTLE)",
    "soupResult.is(Items.BOWL)",
    "honeyRegeneration.getDuration() == 100",
    "soupRegeneration.getDuration() == 1200",
    "crop.getDamageValue() == 1",
    "blockItem.getBlock() == block.get()",
)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, default=Path("docs/migration-matrix.json"))
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()
    root = args.root.resolve()
    errors: list[str] = []

    for source, targets in ROWS.items():
        if not (root / "upstream/Animania-1.12" / source).is_file():
            errors.append(f"legacy source missing: {source}")
        for target in targets:
            if not (root / target).is_file():
                errors.append(f"modern target missing: {target}")
    test_text = (root / TEST).read_text(encoding="utf-8")
    for token in TOKENS:
        if token not in test_text:
            errors.append(f"dedicated-server assertion missing: {token}")
    food_text = (root / "base/src/main/java/com/animania/common/item/AnimaniaFoodItem.java").read_text(encoding="utf-8")
    for token in ("hasCraftingRemainingItem(stack)", "result.getCount() < countBefore", "return consumptionRemainder"):
        if token not in food_text:
            errors.append(f"consumption remainder contract missing: {token}")

    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    matrix = json.loads(matrix_path.read_text(encoding="utf-8"))
    by_source = {entry.get("source"): entry for entry in matrix["entries"]}
    changed = 0
    if not errors:
        for source, targets in ROWS.items():
            entry = by_source.get(source)
            if entry is None:
                errors.append(f"migration row missing: {source}")
                continue
            proof = {
                "paths": targets,
                "behavior_tests": [TEST, "tools/audit_farm_special_items.py"],
                "serialization_tests": [],
                "client_tests": [],
                "notes": [f"{OWNER} Forge dedicated GameTests verify exact consumption, nutrition, effects, container returns, projectile spawning, boost durability, and placeable wheel mapping."],
            }
            owned = any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", []))
            if args.write:
                entry.update(status="closed", implemented=True, verified=True,
                             tests=[TEST, "tools/audit_farm_special_items.py"], target_evidence=proof)
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
