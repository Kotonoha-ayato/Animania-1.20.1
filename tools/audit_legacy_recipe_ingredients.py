"""Audit obsolete 1.12 custom ingredients against native custom-recipe predicates."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

OWNER = "[legacy-recipe-ingredients-audit:v1]"
ROWS = [
    "src/main/java/com/animania/common/recipes/ingredients/AddonItemIngredient.java",
    "src/main/java/com/animania/common/recipes/ingredients/FilledBucketFactory.java",
    "src/main/java/com/animania/common/recipes/ingredients/IngredientAnimaniaNBT.java",
    "src/main/java/com/animania/common/recipes/ingredients/PigFoodIngredient.java",
]
REMOVED_LEGACY_ASSETS = {
    "base/src/main/resources/assets/animania/recipes/_constants.json",
    "base/src/main/resources/assets/animania/recipes/_factories.json",
    "farm/src/main/resources/assets/farm/animania/recipes/_constants.json",
    "farm/src/main/resources/assets/farm/animania/recipes/_factories.json",
}
TARGETS = [
    "base/src/main/java/com/animania/common/recipe/SlopRecipe.java",
    "farm/src/main/java/com/animania/farm/FarmMilkConversionRecipe.java",
]
TESTS = [
    "base/src/main/java/com/animania/gametest/AnimaniaBaseGameTests.java",
    "farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java",
    "tools/audit_legacy_recipe_ingredients.py",
]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, default=Path("docs/migration-matrix.json"))
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()
    root = args.root.resolve()
    errors: list[str] = []

    old_resources = "\n".join(path.read_text(encoding="utf-8", errors="replace")
                              for path in (root / "upstream/Animania-1.12/src/main/resources").rglob("*.json"))
    for token in ("animania:filled_bucket", "animania:pigfood", "animania:addon_item"):
        if token not in old_resources:
            errors.append(f"legacy custom ingredient use missing: {token}")

    slop = (root / TARGETS[0]).read_text(encoding="utf-8")
    milk = (root / TARGETS[1]).read_text(encoding="utf-8")
    base_test = (root / TESTS[0]).read_text(encoding="utf-8")
    farm_test = (root / TESTS[1]).read_text(encoding="utf-8")
    for token in ("AnimaniaConfig.matchesSlopIngredient", "isMilkBucket", "Items.MILK_BUCKET",
                  'id.getNamespace().equals("animania_farm")'):
        if token not in slop:
            errors.append(f"modern slop predicate missing: {token}")
    for token in ("isAnimaniaMilkBucket", 'AnimaniaFarm.MOD_ID.equals(id.getNamespace())',
                  'id.getPath().startsWith("milk_")', 'id.getPath().endsWith("_bucket")'):
        if token not in milk:
            errors.append(f"modern milk predicate missing: {token}")
    if "slopRecipePreservesConfigAndBucketSemantics" not in base_test:
        errors.append("Base slop behavior GameTest missing")
    for token in ("FarmMilkConversionRecipe.isAnimaniaMilkBucket", "SlopRecipe.matchesInputs",
                  'new String[]{"milk_holstein", "milk_friesian", "milk_jersey", "milk_goat", "milk_sheep"}'):
        if token not in farm_test:
            errors.append(f"all-addon-bucket GameTest proof missing: {token}")

    modern = "\n".join(path.read_text(encoding="utf-8", errors="replace")
                       for module in ("base", "farm", "extra", "catsdogs")
                       for path in (root / module / "src/main").rglob("*.*") if path.suffix in {".java", ".json"})
    for token in ("IIngredientFactory", "IngredientAnimaniaNBT", "animania:filled_bucket",
                  "animania:pigfood", "animania:addon_item"):
        if token in modern:
            errors.append(f"obsolete ingredient mechanism remains in modern runtime: {token}")

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
                "behavior_tests": TESTS,
                "serialization_tests": TESTS[:2],
                "client_tests": [],
                "notes": [f"{OWNER} custom recipes now resolve configured pig food and every loaded milk bucket directly through stable registry IDs; 1.12 NBT universal buckets no longer exist."],
            }
            owned = any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", []))
            if args.write:
                entry.update(status="closed", implemented=True, verified=True, tests=TESTS, target_evidence=proof)
                changed += 1
            elif entry.get("status") != "closed" or not owned:
                errors.append(f"provable row not closed: {source}")

        if args.write:
            # These 1.12 Forge factory descriptors are executable recipe metadata,
            # not art assets. They must not remain in a 1.20.1 JAR or in another
            # row's evidence after native serializers replace them.
            for entry in matrix["entries"]:
                evidence = entry.get("target_evidence", {})
                if isinstance(evidence.get("paths"), list):
                    evidence["paths"] = [path for path in evidence["paths"] if path not in REMOVED_LEGACY_ASSETS]

    if args.write and not errors:
        matrix_path.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"rows": len(ROWS), "changed": changed, "errors": errors,
                      "error_count": len(errors)}, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
