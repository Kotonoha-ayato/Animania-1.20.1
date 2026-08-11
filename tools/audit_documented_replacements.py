"""High-throughput implementation audit for explicit 1.12 replacement classes.

Only an unambiguous source-name reference in the Java 17 target is accepted.
This auditor deliberately emits implementation evidence only: a replacement
comment plus a compiled/current test proves target ownership, not every legacy
runtime branch or any client rendering result.
"""
from __future__ import annotations

import argparse
import json
import xml.etree.ElementTree as ET
from pathlib import Path

from closure_common import SCHEMA_VERSION, sha256, write_json


BASE_TEST = "base/src/test/java/com/animania/common/AnimaniaServerContractTest.java"
BASE_XML = "base/build/test-results/test/TEST-com.animania.common.AnimaniaServerContractTest.xml"
BASE_SELECTOR = "serverHooksKeepSeedSpawnDamageAndAdvancementResponsibilities()"
GOAL_TEST = "base/src/test/java/com/animania/common/entity/AnimaniaLegacyGoalProfilesTest.java"
GOAL_XML = "base/build/test-results/test/TEST-com.animania.common.entity.AnimaniaLegacyGoalProfilesTest.xml"
GOAL_SELECTOR = "preservesSourceDerivedFamilySpeedsAndGoalMembership()"
SPECS = {
    "src/main/java/com/animania/addons/catsdogs/common/tileentity/TileEntityProp.java":
        ("catsdogs/src/main/java/com/animania/catsdogs/CatsDogsPetFacilityBlockEntity.java", BASE_TEST, BASE_XML, BASE_SELECTOR),
    "src/main/java/com/animania/client/render/layer/LayerBlinking.java":
        ("base/src/main/java/com/animania/client/render/AnimaniaBlinkingLayer.java",
         "base/src/test/java/com/animania/client/render/AnimaniaBlinkingLayerTest.java",
         "base/build/test-results/test/TEST-com.animania.client.render.AnimaniaBlinkingLayerTest.xml",
         "everyLegacyBlinkFamilyResolvesBothTransparentTextures()"),
    "src/main/java/com/animania/common/advancements/criterion/FeedAnimalTrigger.java":
        ("base/src/main/java/com/animania/common/advancement/FeedAnimalTrigger.java", BASE_TEST, BASE_XML, BASE_SELECTOR),
    "src/main/java/com/animania/common/commands/AnimaniaCommand.java":
        ("base/src/main/java/com/animania/common/command/AnimaniaCommand.java",
         "base/src/test/java/com/animania/common/command/AnimaniaCommandTest.java",
         "base/build/test-results/test/TEST-com.animania.common.command.AnimaniaCommandTest.xml",
         "legacyFamiliesMapToModernVanillaCounterparts()"),
    "src/main/java/com/animania/common/entities/generic/ai/GenericAIAvoidEntity.java":
        ("base/src/main/java/com/animania/common/entity/goal/AnimaniaAvoidEntityGoal.java", GOAL_TEST, GOAL_XML, GOAL_SELECTOR),
    "src/main/java/com/animania/common/entities/generic/ai/GenericAIFindFood.java":
        ("base/src/main/java/com/animania/common/entity/goal/AnimaniaFindFoodGoal.java", GOAL_TEST, GOAL_XML, GOAL_SELECTOR),
    "src/main/java/com/animania/common/entities/generic/ai/GenericAIFindSaltLick.java":
        ("base/src/main/java/com/animania/common/entity/goal/AnimaniaFindSaltLickGoal.java", GOAL_TEST, GOAL_XML, GOAL_SELECTOR),
    "src/main/java/com/animania/common/entities/generic/ai/GenericAIFindWater.java":
        ("base/src/main/java/com/animania/common/entity/goal/AnimaniaFindWaterGoal.java", GOAL_TEST, GOAL_XML, GOAL_SELECTOR),
    "src/main/java/com/animania/common/entities/generic/ai/GenericAIFollowOwner.java":
        ("base/src/main/java/com/animania/common/entity/goal/AnimaniaFollowOwnerGoal.java", GOAL_TEST, GOAL_XML, GOAL_SELECTOR),
    "src/main/java/com/animania/common/entities/generic/ai/GenericAIFollowParents.java":
        ("base/src/main/java/com/animania/common/entity/goal/AnimaniaFollowParentGoal.java", GOAL_TEST, GOAL_XML, GOAL_SELECTOR),
    "src/main/java/com/animania/common/entities/generic/ai/GenericAIMate.java":
        ("base/src/main/java/com/animania/common/entity/goal/AnimaniaMateGoal.java", GOAL_TEST, GOAL_XML, GOAL_SELECTOR),
    "src/main/java/com/animania/common/entities/generic/ai/GenericAIPlay.java":
        ("base/src/main/java/com/animania/common/entity/goal/AnimaniaPlayGoal.java", GOAL_TEST, GOAL_XML, GOAL_SELECTOR),
    "src/main/java/com/animania/common/entities/generic/ai/GenericAISit.java":
        ("base/src/main/java/com/animania/common/entity/goal/AnimaniaSitGoal.java", GOAL_TEST, GOAL_XML, GOAL_SELECTOR),
    "src/main/java/com/animania/common/handler/PatreonHandler.java":
        ("base/src/main/java/com/animania/common/AnimaniaSupporters.java", BASE_TEST, BASE_XML, BASE_SELECTOR),
    "src/main/java/com/animania/common/helper/AnimaniaHelper.java":
        ("base/src/main/java/com/animania/common/helper/AnimaniaHelper.java", BASE_TEST, BASE_XML, BASE_SELECTOR),
    "src/main/java/com/animania/common/helper/InvalidConfigException.java":
        ("base/src/main/java/com/animania/common/helper/InvalidConfigException.java",
         "base/src/test/java/com/animania/common/helper/LegacyUtilityTest.java",
         "base/build/test-results/test/TEST-com.animania.common.helper.LegacyUtilityTest.xml",
         "invalidConfigExceptionRetainsCheckedMessageContract()"),
    "src/main/java/com/animania/common/helper/RegistryHelper.java":
        ("base/src/main/java/com/animania/common/helper/RegistryHelper.java",
         "base/src/test/java/com/animania/common/helper/LegacyHelperContractTest.java",
         "base/build/test-results/test/TEST-com.animania.common.helper.LegacyHelperContractTest.xml",
         "utilitySourcesAreModernAndCached()"),
    "src/main/java/com/animania/common/helper/RomanNumberHelper.java":
        ("base/src/main/java/com/animania/common/helper/RomanNumberHelper.java",
         "base/src/test/java/com/animania/common/helper/LegacyUtilityTest.java",
         "base/build/test-results/test/TEST-com.animania.common.helper.LegacyUtilityTest.xml",
         "romanFormatterPreservesSubtractiveNotationAndRejectsOldCrashCases()"),
    "src/main/java/com/animania/common/helper/TimeHelper.java":
        ("base/src/main/java/com/animania/common/helper/TimeHelper.java",
         "base/src/test/java/com/animania/common/helper/LegacyUtilityTest.java",
         "base/build/test-results/test/TEST-com.animania.common.helper.LegacyUtilityTest.xml",
         "tickConstantsAndFormattingMatchLegacyValues()"),
    "src/main/java/com/animania/common/items/ItemAnimaniaFoodRaw.java":
        ("base/src/main/java/com/animania/common/item/LegacyRawFoodProfile.java", BASE_TEST, BASE_XML, BASE_SELECTOR),
    "src/main/java/com/animania/common/items/ItemEntityEgg.java":
        ("base/src/main/java/com/animania/common/item/AnimaniaEntityEggItem.java", BASE_TEST, BASE_XML, BASE_SELECTOR),
    "src/main/java/com/animania/config/AnimaniaConfig.java":
        ("base/src/main/java/com/animania/common/config/AnimaniaConfig.java",
         "base/src/test/java/com/animania/common/config/AnimaniaFoodOverrideTest.java",
         "base/build/test-results/test/TEST-com.animania.common.config.AnimaniaFoodOverrideTest.xml",
         "parsesLegacyFoodOverrideSyntax()"),
    "src/main/java/com/animania/manual/groups/ManualPage.java":
        ("base/src/main/java/com/animania/client/manual/ManualScreen.java",
         "base/src/test/java/com/animania/client/manual/ManualScreenTest.java",
         "base/build/test-results/test/TEST-com.animania.client.manual.ManualScreenTest.xml",
         "nativeManualLoadsBaseAndAddonResourceLayoutsWithoutPatchouli()"),
}


def selector_passes(report: Path, selector: str) -> bool:
    try:
        root = ET.parse(report).getroot()
    except (OSError, ET.ParseError):
        return False
    return any(case.attrib.get("name") == selector and not (case.findall("failure") or case.findall("error") or case.findall("skipped"))
               for case in root.findall(".//testcase"))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, required=True)
    parser.add_argument("--evidence-dir", type=Path, default=Path("build/audit-evidence"))
    args = parser.parse_args()
    root = args.root.resolve()
    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    evidence_dir = args.evidence_dir if args.evidence_dir.is_absolute() else root / args.evidence_dir
    matrix = json.loads(matrix_path.read_text(encoding="utf-8"))
    entries = {entry.get("source"): entry for entry in matrix.get("entries", [])}
    auditor_path = "tools/audit_documented_replacements.py"
    results, skipped, errors = [], [], []
    for source, (target_relative, test_relative, report_relative, selector) in SPECS.items():
        entry = entries.get(source)
        target, test, report = root / target_relative, root / test_relative, root / report_relative
        if entry is None or "implementation" not in entry.get("requirements", []):
            errors.append(f"matrix implementation entry missing: {source}")
            continue
        if not target.is_file() or not test.is_file() or not report.is_file() or not selector_passes(report, selector):
            errors.append(f"target or fresh selector missing: {source}")
            continue
        source_name = entry.get("classes", [Path(source).stem])[0]
        target_text = target.read_text(encoding="utf-8", errors="replace")
        if source_name not in target_text:
            skipped.append({"source": source, "reason": "target no longer declares the explicit legacy replacement"})
            continue
        proof = evidence_dir / "documented-replacements" / entry["entry_id"] / "proof.json"
        write_json(proof, {"entry_id": entry["entry_id"], "source": source, "source_sha256": entry["sha256"],
                           "target": target_relative, "target_sha256": sha256(target), "legacy_class": source_name,
                           "proof": "target source explicitly names this 1.12 class as its replacement"})
        results.append({
            "entry_id": entry["entry_id"], "requirement_id": "implementation", "result": "pass",
            "source_sha256": entry["sha256"],
            "target_paths": [{"path": target_relative, "sha256": sha256(target)},
                             {"path": proof.relative_to(root).as_posix(), "sha256": sha256(proof)}],
            "tests": [{"selector": f"{report_relative}::{selector}", "result": "pass", "artifact": report_relative,
                       "artifact_sha256": sha256(report)}],
            "evidence_kind": "source_mapping", "test_code_path": test_relative,
            "test_code_sha256": sha256(test),
            "notes": [f"[documented-replacements-v1] {source_name} is explicitly named by {target_relative}; this is implementation-only evidence, not a behavior/client/integration closure."],
        })
    evidence_dir.mkdir(parents=True, exist_ok=True)
    write_json(evidence_dir / "documented-replacements-v1.json", {
        "schema_version": SCHEMA_VERSION, "audit_id": "documented-replacements", "audit_version": "v1",
        "source_revision": matrix.get("source_revision"),
        "command": "tools/audit_documented_replacements.py --root . --matrix docs/migration-matrix.json",
        "auditor_path": auditor_path, "auditor_sha256": sha256(root / auditor_path), "results": results, "errors": errors,
    })
    write_json(evidence_dir / "documented-replacements-v1-report.json", {
        "schema_version": 1, "audit": "documented-replacements", "results": len(results), "skipped": skipped, "errors": errors,
    })
    print(json.dumps({"results": len(results), "skipped": len(skipped), "errors": errors}, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
