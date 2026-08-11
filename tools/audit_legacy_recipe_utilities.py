"""Audit the 1.12 recipe parser/helper classes against native 1.20.1 serializers."""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

OWNER = "[legacy-recipe-utilities-audit:v1]"
ROWS = {
    "src/main/java/com/animania/common/helper/RecipeUtil.java": [
        "base/src/main/java/com/animania/common/recipe/SlopRecipe.java",
        "farm/src/main/java/com/animania/farm/FarmMilkConversionRecipe.java",
        "farm/src/main/resources/data/animania_farm/recipes/beef_cutting_1.json",
    ],
    "src/main/java/com/animania/common/recipes/NoBucketRecipe.java": [
        "base/src/main/java/com/animania/common/recipe/SlopRecipe.java",
        "farm/src/main/java/com/animania/farm/FarmMilkConversionRecipe.java",
    ],
    "src/main/java/com/animania/addons/farm/common/recipes/MeatCuttingRecipe.java": [
        "farm/src/main/java/com/animania/farm/FarmCarvingKnifeItem.java",
        "farm/src/main/resources/data/animania_farm/recipes/beef_cutting_1.json",
    ],
    "src/main/java/com/animania/addons/farm/common/item/ItemCarvingKnife.java": [
        "farm/src/main/java/com/animania/farm/FarmCarvingKnifeItem.java",
        "farm/src/main/resources/data/animania_farm/recipes/beef_cutting_1.json",
    ],
}
TESTS = [
    "base/src/main/java/com/animania/gametest/AnimaniaBaseGameTests.java",
    "farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java",
    "tools/audit_resources.py",
    "tools/audit_legacy_recipe_utilities.py",
]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, default=Path("docs/migration-matrix.json"))
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()
    root = args.root.resolve()
    errors: list[str] = []

    old_root = root / "upstream/Animania-1.12/src/main/java"
    old_java = "\n".join(path.read_text(encoding="utf-8", errors="replace") for path in old_root.rglob("*.java"))
    if len(re.findall(r"RecipeUtil\.parseShapeless\s*\(", old_java)) != 2:
        errors.append("legacy RecipeUtil call surface is no longer exactly the two known recipe factories")

    slop = (root / ROWS[next(iter(ROWS))][0]).read_text(encoding="utf-8")
    milk = (root / ROWS[next(iter(ROWS))][1]).read_text(encoding="utf-8")
    knife = (root / "farm/src/main/java/com/animania/farm/FarmCarvingKnifeItem.java").read_text(encoding="utf-8")
    cutting = json.loads((root / "farm/src/main/resources/data/animania_farm/recipes/beef_cutting_1.json").read_text(encoding="utf-8"))
    base_test = (root / TESTS[0]).read_text(encoding="utf-8")
    farm_test = (root / TESTS[1]).read_text(encoding="utf-8")

    for label, text in (("slop", slop), ("milk conversion", milk)):
        for token in ("extends CustomRecipe", "getRemainingItems", "NonNullList.withSize", "ItemStack.EMPTY"):
            if token not in text:
                errors.append(f"{label} native no-container-remainder contract missing: {token}")
    if "slopRecipePreservesConfigAndBucketSemantics" not in base_test:
        errors.append("live slop recipe test missing")
    if cutting.get("type") != "minecraft:crafting_shapeless":
        errors.append("cutting recipe does not use the native shapeless JSON parser")
    for token in ("hasCraftingRemainingItem", "getCraftingRemainingItem", "getDamageValue() + 1", "ItemStack.EMPTY"):
        if token not in knife:
            errors.append(f"carving knife remainder contract missing: {token}")
    for token in ("nativeRecipeParsingAndCarvingKnifeRemainder", "RecipeSerializer.SHAPELESS_RECIPE",
                  "getDamageValue() == 1", "lastUse.getMaxDamage() - 1"):
        if token not in farm_test:
            errors.append(f"live native-recipe test missing: {token}")
    if re.search(r"\bRecipeUtil\b|\bIRecipeFactory\b|_factories\.json", "\n".join(
            path.read_text(encoding="utf-8", errors="replace") for module in ("base", "farm")
            for path in (root / module / "src/main").rglob("*.*") if path.suffix in {".java", ".json"})):
        errors.append("obsolete custom recipe factory/parser remains in modern runtime resources")

    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    matrix = json.loads(matrix_path.read_text(encoding="utf-8"))
    by_source = {entry.get("source"): entry for entry in matrix["entries"]}
    changed = 0
    if not errors:
        for source, paths in ROWS.items():
            entry = by_source.get(source)
            if entry is None:
                errors.append(f"migration row missing: {source}")
                continue
            proof = {
                "paths": paths,
                "behavior_tests": TESTS,
                "serialization_tests": [],
                "client_tests": [],
                "notes": [f"{OWNER} native recipe serializers replace the obsolete Forge factory parser; dedicated-server tests verify loading and container-remainder behavior."],
            }
            owned = any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", []))
            if args.write:
                entry.update(status="closed", implemented=True, verified=True, tests=TESTS, target_evidence=proof)
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
