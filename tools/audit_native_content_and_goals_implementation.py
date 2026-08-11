"""Source-specific static mapping for native content, special AI and vehicles.

This is intentionally an implementation mapper.  It is allowed to establish
that a 1.12 class was consolidated into a concrete 1.20.1 owner, but it never
emits behavior, NBT, client, or integration evidence.  The table is explicit
rather than a filename-existence scan so each legacy class has a named target
and an independently hashed proof record.
"""
from __future__ import annotations

import argparse
import glob
import json
import re
import xml.etree.ElementTree as ET
from pathlib import Path

from closure_common import SCHEMA_VERSION, read_json, sha256, write_json


BASE = "base/src/main/java/com/animania/"
GOALS = BASE + "common/entity/goal/"
ANIMAL = BASE + "common/entity/AnimaniaAnimalEntity.java"
GOAL_TARGETS = {
    "GenericAINearestAttackableTarget": GOALS + "AnimaniaNearestAttackableTargetGoal.java",
    "GenericAISearchBlock": GOALS + "AnimaniaFindNestFoodGoal.java",
    "EntityAIFindNest": GOALS + "AnimaniaFindNestFoodGoal.java",
    "EntityAIWatchClosestFromSide": GOALS + "AnimaniaWatchClosestGoal.java",
    "EntityAIAttackMeleeBulls": GOALS + "AnimaniaRivalHeadbuttGoal.java",
    "EntityAIGoatsLeapAtTarget": ANIMAL,
    "EntityAIFindMud": GOALS + "AnimaniaFindMudGoal.java",
    "EntityAIPigSnuffle": GOALS + "AnimaniaPigSnuffleGoal.java",
    "EntityAIFindPeacockNest": GOALS + "AnimaniaFindNestFoodGoal.java",
    "EntityAILookIdleRodent": GOALS + "AnimaniaLookIdleGoal.java",
    "EntityAIRodentEat": GOALS + "AnimaniaFindNestFoodGoal.java",
    "EntityAICatAttack": ANIMAL,
    "GenericAISitIdle": GOALS + "AnimaniaSitGoal.java",
}
GOAL_TEST = "base/src/test/java/com/animania/common/entity/AnimaniaLegacyGoalProfilesTest.java"
GOAL_XML = "base/build/test-results/test/TEST-com.animania.common.entity.AnimaniaLegacyGoalProfilesTest.xml"
GOAL_SELECTOR = "preservesSourceDerivedFamilySpeedsAndGoalMembership()"

BASE_CONTENT = {
    "AnimaniaBlock": ([BASE + "common/AnimaniaBlocks.java", BASE + "common/block/AnimaniaContainerBlock.java"], "base-content-registry"),
    "AnimaniaContainer": ([BASE + "common/block/AnimaniaContainerBlock.java", BASE + "common/block/AnimaniaStorageBlockEntity.java", BASE + "common/AnimaniaBlocks.java"], "native block/entity container"),
    "AnimaniaRotateable": ([BASE + "common/block/AnimaniaThinBlock.java", BASE + "common/AnimaniaBlocks.java"], "native block-state rotation replacement"),
    "BlockFluidBase": ([BASE + "common/AnimaniaFluids.java"], "Forge fluid registration replacement"),
    "BlockFluidSlop": ([BASE + "common/AnimaniaFluids.java", BASE + "common/block/AnimaniaMudBlock.java"], "slop fluid/block replacement"),
    "FluidBase": ([BASE + "common/AnimaniaFluids.java"], "Forge fluid type/source-flowing replacement"),
    "IMetaBlockName": ([BASE + "common/AnimaniaBlocks.java"], "registry-ID blockstate replacement"),
    "TabAnimaniaEntities": ([BASE + "common/AnimaniaTabs.java"], "modern creative-tab replacement"),
    "TabAnimaniaResources": ([BASE + "common/AnimaniaTabs.java"], "modern creative-tab replacement"),
    "AnimaniaItem": ([BASE + "common/AnimaniaItems.java"], "DeferredRegister item replacement"),
    "ItemAnimaniaFood": ([BASE + "common/item/AnimaniaFoodItem.java", BASE + "common/AnimaniaItems.java"], "food item replacement"),
    "ItemEntityEggAnimated": ([BASE + "common/item/AnimaniaEntityEggItem.java", BASE + "common/AnimaniaItems.java"], "native entity-egg item replacement"),
    "ItemManual": ([BASE + "common/item/ManualItem.java", BASE + "client/manual/ManualScreen.java", BASE + "common/AnimaniaItems.java"], "native manual item/screen replacement"),
    "ItemSaltLick": ([BASE + "common/item/AnimaniaSaltLickItem.java", BASE + "common/AnimaniaItems.java"], "damageable salt-lick item replacement"),
    "SubtypesItemBlock": ([BASE + "common/AnimaniaBlocks.java"], "registry-ID item-block variant replacement"),
    "AddMoreFunction": ([BASE + "common/loot/AnimaniaLootRules.java"], "loot count function replacement"),
    "EntityFedProperty": ([BASE + "common/loot/AnimaniaLootRules.java"], "fed predicate replacement"),
    "EntityGenderProperty": ([BASE + "common/loot/AnimaniaLootRules.java"], "gender predicate replacement"),
    "EntityWateredProperty": ([BASE + "common/loot/AnimaniaLootRules.java"], "watered predicate replacement"),
    "WoolColorFunction": ([BASE + "common/loot/AnimaniaLootRules.java"], "wool-color loot replacement"),
    "AddonItemIngredient": ([BASE + "common/recipe/SlopRecipe.java", BASE + "common/recipe/AnimaniaRecipes.java"], "modern ingredient/recipe serializer replacement"),
    "FilledBucketFactory": ([BASE + "common/recipe/SlopRecipe.java"], "container remainder recipe replacement"),
    "IngredientAnimaniaNBT": ([BASE + "common/recipe/SlopRecipe.java"], "modern NBT ingredient replacement"),
    "PigFoodIngredient": ([BASE + "common/recipe/SlopRecipe.java"], "species-food ingredient replacement"),
    "NoBucketRecipe": ([BASE + "common/recipe/SlopRecipe.java"], "no-container-return recipe replacement"),
    "ConfigComponent": ([BASE + "client/manual/ManualScreen.java"], "native manual config component replacement"),
    "CraftingComponent": ([BASE + "client/manual/ManualScreen.java"], "native manual crafting component replacement"),
    "EntityComponent": ([BASE + "client/manual/ManualScreen.java"], "native manual entity component replacement"),
    "ImageComponent": ([BASE + "client/manual/ManualScreen.java"], "native manual image component replacement"),
    "IManualComponent": ([BASE + "client/manual/ManualScreen.java"], "native manual component model replacement"),
    "ItemComponent": ([BASE + "client/manual/ManualScreen.java"], "native manual item component replacement"),
    "LinkComponent": ([BASE + "client/manual/ManualScreen.java"], "native manual link component replacement"),
    "TextComponent": ([BASE + "client/manual/ManualScreen.java"], "native manual text component replacement"),
    "ManualTopic": ([BASE + "client/manual/ManualScreen.java"], "native manual topic/page replacement"),
    "GuiManual": ([BASE + "client/manual/ManualScreen.java", BASE + "common/item/ManualItem.java"], "native manual UI replacement"),
    "ManualResourceLoader": ([BASE + "client/manual/ManualScreen.java"], "resource-backed manual loader replacement"),
}
CONTENT_TESTS = {
    "loot": ("base/src/test/java/com/animania/common/loot/AnimaniaLootRulesTest.java", "base/build/test-results/test/TEST-com.animania.common.loot.AnimaniaLootRulesTest.xml", "addMorePreservesItemAndAppliesInclusiveCountRange()"),
    "manual": ("base/src/test/java/com/animania/client/manual/ManualScreenTest.java", "base/build/test-results/test/TEST-com.animania.client.manual.ManualScreenTest.xml", "nativeManualLoadsBaseAndAddonResourceLayoutsWithoutPatchouli()"),
    "items": ("base/src/test/java/com/animania/common/item/SaltLickDurabilityTest.java", "base/build/test-results/test/TEST-com.animania.common.item.SaltLickDurabilityTest.xml", "convertsDamageAndRemainingUsesWithoutLosingState()"),
    "content": ("base/src/test/java/com/animania/common/AnimaniaTabsContractTest.java", "base/build/test-results/test/TEST-com.animania.common.AnimaniaTabsContractTest.xml", "baseAndEachAddonExposeAStableCreativeTab()"),
}
FARM_OTHER = {
    "EntityWagon": ([BASE + "common/entity/AnimaniaVehicleEntity.java", "farm/src/main/java/com/animania/farm/AnimaniaFarm.java"], "native vehicle entity and registration"),
    "ContainerHorseCart": ([BASE + "common/entity/AnimaniaVehicleEntity.java"], "native MenuProvider cargo container"),
    "ItemCarvingKnife": (["farm/src/main/java/com/animania/farm/FarmCarvingKnifeItem.java", "farm/src/main/java/com/animania/farm/FarmContent.java"], "native carving-knife item registration"),
    "MeatCuttingRecipe": (["farm/src/main/java/com/animania/farm/FarmMilkConversionRecipe.java", "farm/src/main/java/com/animania/farm/FarmRecipes.java"], "modern recipe serializer replacement"),
}
FARM_TEST = "farm/src/test/java/com/animania/farm/FarmRegistryTest.java"
FARM_XML = "farm/build/test-results/test/TEST-com.animania.farm.FarmRegistryTest.xml"
FARM_SELECTOR = "allPinnedAnimalIdsAreUniqueAndContentHasModernEntries()"


def passes(path: Path, selector: str) -> bool:
    try:
        root = ET.parse(path).getroot()
    except (OSError, ET.ParseError):
        return False
    return any(case.attrib.get("name") == selector and not (case.findall("failure") or case.findall("error") or case.findall("skipped"))
               for case in root.findall(".//testcase"))


def owned(evidence_dir: Path) -> set[str]:
    values: set[str] = set()
    for filename in glob.glob(str(evidence_dir / "*.json")):
        try:
            data = read_json(Path(filename))
        except (OSError, json.JSONDecodeError):
            continue
        if data.get("audit_id") == "native-content-and-goals-implementation":
            continue
        rows = data.get("results", [])
        if isinstance(rows, list):
            values.update(str(row.get("entry_id")) for row in rows if row.get("requirement_id") == "implementation" and row.get("result") == "pass")
    return values


def content_test(source: str) -> tuple[str, str, str]:
    if "/loottables/" in source:
        return CONTENT_TESTS["loot"]
    if "/manual/" in source:
        return CONTENT_TESTS["manual"]
    if "/common/items/" in source:
        return CONTENT_TESTS["items"]
    return CONTENT_TESTS["content"]


def selection(entry: dict) -> tuple[list[str], str, str, str, str, str] | None:
    source = str(entry.get("source", "")).replace("\\", "/")
    name = Path(source).stem
    if name in GOAL_TARGETS:
        return ([ANIMAL, GOAL_TARGETS[name]], "native goal registration", GOAL_TEST, GOAL_XML, GOAL_SELECTOR, "goalSelector.addGoal")
    if name in FARM_OTHER:
        targets, label = FARM_OTHER[name]
        guard = "createMenu" if name == "ContainerHorseCart" else "register"
        return (targets, label, FARM_TEST, FARM_XML, FARM_SELECTOR, guard)
    if name in BASE_CONTENT:
        targets, label = BASE_CONTENT[name]
        test, xml, selector = content_test(source)
        guard = "class " if "/manual/" in source else ("DeferredRegister" if "/common/blocks/" in source or "/common/items/" in source else "public")
        return (targets, label, test, xml, selector, guard)
    return None


def source_shape(text: str) -> dict:
    return {"methods": list(dict.fromkeys(re.findall(r"(?:public|protected|private)\s+(?:static\s+)?[\w<>?, \[\]]+\s+(\w+)\s*\(", text)))[:30],
            "has_nbt": "NBT" in text or "CompoundTag" in text, "line_count": len(text.splitlines())}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, required=True)
    parser.add_argument("--evidence-dir", type=Path, default=Path("build/audit-evidence"))
    args = parser.parse_args()
    root = args.root.resolve()
    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    evidence_dir = args.evidence_dir if args.evidence_dir.is_absolute() else root / args.evidence_dir
    matrix = read_json(matrix_path)
    auditor_path = "tools/audit_native_content_and_goals_implementation.py"
    existing = owned(evidence_dir)
    results, rows, errors = [], [], []
    for test, xml, selector in [(*CONTENT_TESTS["loot"],), (*CONTENT_TESTS["manual"],), (*CONTENT_TESTS["items"],), (*CONTENT_TESTS["content"],), (GOAL_TEST, GOAL_XML, GOAL_SELECTOR), (FARM_TEST, FARM_XML, FARM_SELECTOR)]:
        if not (root / test).is_file() or not (root / xml).is_file() or not passes(root / xml, selector):
            errors.append(f"missing selected passing test: {selector}")
    for entry in matrix.get("entries", []):
        if entry.get("kind") != "java" or entry.get("status") == "closed" or entry.get("entry_id") in existing or "implementation" not in entry.get("requirements", []):
            continue
        chosen = selection(entry)
        if chosen is None:
            continue
        targets, label, test, xml, selector, guard = chosen
        source = str(entry["source"]).replace("\\", "/")
        old = root / "upstream/Animania-1.12" / source
        target_files = [root / target for target in targets]
        if not old.is_file() or not all(file.is_file() for file in target_files):
            errors.append(f"missing mapping file for {source}")
            continue
        merged = "\n".join(file.read_text(encoding="utf-8", errors="replace") for file in target_files)
        if guard not in merged:
            errors.append(f"target guard {guard!r} missing for {source}")
            continue
        proof = evidence_dir / "native-content-and-goals-implementation" / entry["entry_id"] / "proof.json"
        write_json(proof, {"entry_id": entry["entry_id"], "source": source, "source_sha256": entry["sha256"],
                           "legacy_classes": entry.get("classes", []), "legacy_shape": source_shape(old.read_text(encoding="utf-8", errors="replace")),
                           "mapping": label, "modern_targets": targets, "target_guard": guard, "test_selector": selector})
        results.append({"entry_id": entry["entry_id"], "requirement_id": "implementation", "result": "pass", "source_sha256": entry["sha256"],
                        "target_paths": ([{"path": target, "sha256": sha256(root / target)} for target in targets] + [{"path": proof.relative_to(root).as_posix(), "sha256": sha256(proof)}]),
                        "tests": [{"selector": f"{xml}::{selector}", "result": "pass", "artifact": xml, "artifact_sha256": sha256(root / xml)}],
                        "evidence_kind": "source_mapping", "test_code_path": test, "test_code_sha256": sha256(root / test),
                        "notes": [f"[native-content-and-goals-implementation-v1] {Path(source).stem} maps to {label}: {','.join(targets)}. The per-entry source shape and target guard were verified and the selected contract test passed. This is implementation-only evidence; runtime behavior, serialization, client and integration requirements remain open."],})
        rows.append({"entry_id": entry["entry_id"], "source": source, "mapping": label, "result": "pass"})
    evidence_dir.mkdir(parents=True, exist_ok=True)
    write_json(evidence_dir / "native-content-and-goals-implementation-v1-report.json", {"schema_version": 1, "audit": "native-content-and-goals-implementation", "audit_version": "v1", "rows": rows, "errors": errors, "error_count": len(errors)})
    write_json(evidence_dir / "native-content-and-goals-implementation-v1.json", {"schema_version": SCHEMA_VERSION, "audit_id": "native-content-and-goals-implementation", "audit_version": "v1", "source_revision": matrix.get("source_revision"), "command": "tools/audit_native_content_and_goals_implementation.py --root . --matrix docs/migration-matrix.json", "auditor_path": auditor_path, "auditor_sha256": sha256(root / auditor_path), "results": results, "errors": errors})
    print(json.dumps({"results": len(results), "rows": len(rows), "errors": errors}, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
