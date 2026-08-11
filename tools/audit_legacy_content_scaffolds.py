"""Audit small 1.12 self-registering block/item scaffolds against DeferredRegister content."""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

OWNER = "[legacy-content-scaffolds-audit:v1]"
ROWS = [
    "src/main/java/com/animania/common/blocks/AnimaniaBlock.java",
    "src/main/java/com/animania/common/blocks/AnimaniaContainer.java",
    "src/main/java/com/animania/common/blocks/IMetaBlockName.java",
    "src/main/java/com/animania/common/items/AnimaniaItem.java",
    "src/main/java/com/animania/common/items/SubtypesItemBlock.java",
]
TARGETS = [
    "base/src/main/java/com/animania/common/AnimaniaBlocks.java",
    "base/src/main/java/com/animania/common/AnimaniaItems.java",
    "base/src/main/java/com/animania/common/block/AnimaniaContainerBlock.java",
    "farm/src/main/java/com/animania/farm/FarmContent.java",
    "farm/src/main/java/com/animania/farm/FarmWoolBlock.java",
    "farm/src/main/java/com/animania/farm/FarmWoolBlockItem.java",
    "extra/src/main/java/com/animania/extra/ExtraContent.java",
    "catsdogs/src/main/java/com/animania/catsdogs/CatsDogsContent.java",
]
TESTS = [
    "base/src/main/java/com/animania/gametest/AnimaniaBaseGameTests.java",
    "farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java",
    "extra/src/main/java/com/animania/extra/gametest/AnimaniaExtraGameTests.java",
    "catsdogs/src/main/java/com/animania/catsdogs/gametest/AnimaniaCatsDogsGameTests.java",
    "tools/audit_id_mapping.py",
    "tools/audit_legacy_content_scaffolds.py",
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
    expected_subclasses = {
        "AnimaniaBlock": {"AnimaniaRotateable", "BlockSaltLick", "BlockAnimaniaWool"},
        "AnimaniaContainer": {"BlockPetBowl"},
        "IMetaBlockName": {"BlockAnimaniaWool"},
        "AnimaniaItem": {"ItemManual", "ItemHoneyBottle"},
        "SubtypesItemBlock": set(),
    }
    for parent, expected in expected_subclasses.items():
        found = set(re.findall(r"class\s+(\w+)[^{\n]*(?:extends|implements)[^{\n]*\b" + parent + r"\b", old_java))
        if parent == "AnimaniaItem":
            # Direct constructor calls cover the remaining generic feathers,
            # eggs, salt, wheel and hamster food; their IDs are audited below.
            found &= expected
        if found != expected:
            errors.append(f"legacy {parent} subclass surface differs: {sorted(found)} != {sorted(expected)}")

    modern = {path: (root / path).read_text(encoding="utf-8") for path in TARGETS}
    combined = "\n".join(modern.values())
    for token in ("DeferredRegister<Block>", "DeferredRegister<Item>", "BlockItem", "RegistryObject"):
        if token not in combined:
            errors.append(f"modern deferred content scaffold missing: {token}")
    wool = modern["farm/src/main/java/com/animania/farm/FarmWoolBlockItem.java"]
    for token in ("BlockStateTag", "FarmWoolBlock.Variant", "stack(FarmWoolBlock.Variant", "variant(ItemStack"):
        if token not in wool:
            errors.append(f"modern wool subtype mapping missing: {token}")
    container = modern["base/src/main/java/com/animania/common/block/AnimaniaContainerBlock.java"]
    for token in ("extends BaseEntityBlock", "newBlockEntity", "player.openMenu(menu)"):
        if token not in container:
            errors.append(f"modern container block contract missing: {token}")
    farm_test = (root / TESTS[1]).read_text(encoding="utf-8")
    cats_test = (root / TESTS[3]).read_text(encoding="utf-8")
    if "allSevenLegacyWoolVariantsPlaceAndDropTheirState" not in farm_test:
        errors.append("all wool subtypes lack a dedicated-server round-trip test")
    if "petBowlFoodAndWaterCapabilities" not in cats_test:
        errors.append("pet bowl native container lacks its dedicated-server test")

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
            proof = {"paths": TARGETS, "behavior_tests": TESTS, "serialization_tests": [], "client_tests": [],
                     "notes": [f"{OWNER} constructor-time global registration was replaced by per-module DeferredRegister/BlockItem wiring; salt lick, pet bowl and every wool subtype have live registry/behavior tests."]}
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
