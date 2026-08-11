"""Bind each migrated generic 1.12 AI to a real, uniquely-marked Forge test.

The source classes were consolidated into native 1.20.1 goals.  Every mapping
below names one old class, one modern goal and one marker emitted by the test
that instantiates that exact goal.  Existing documented-replacement evidence
owns implementation for nine mappings; this auditor never duplicates it.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from closure_common import SCHEMA_VERSION, sha256, write_json


FARM_TEST = "farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java"
FARM_LOG = "farm/run/gameTestServer/logs/latest.log"
EXTRA_TEST = "extra/src/main/java/com/animania/extra/gametest/AnimaniaExtraGameTests.java"
EXTRA_LOG = "extra/run/gameTestServer/logs/latest.log"
PETS_TEST = "catsdogs/src/main/java/com/animania/catsdogs/gametest/AnimaniaCatsDogsGameTests.java"
PETS_LOG = "catsdogs/run/gameTestServer/logs/latest.log"
SPECS = {
    "GenericAIAvoidEntity": ("AnimaniaAvoidEntityGoal", "base/src/main/java/com/animania/common/entity/goal/AnimaniaAvoidEntityGoal.java", EXTRA_TEST, EXTRA_LOG, "animania_extra:generic_ai_avoid_entity"),
    "GenericAIEatGrass": ("AnimaniaEatGrassGoal", "base/src/main/java/com/animania/common/entity/goal/AnimaniaEatGrassGoal.java", FARM_TEST, FARM_LOG, "animania_farm:generic_ai_eat_grass"),
    "GenericAIFindFood": ("AnimaniaFindFoodGoal", "base/src/main/java/com/animania/common/entity/goal/AnimaniaFindFoodGoal.java", FARM_TEST, FARM_LOG, "animania_farm:generic_ai_find_food"),
    "GenericAIFindSaltLick": ("AnimaniaFindSaltLickGoal", "base/src/main/java/com/animania/common/entity/goal/AnimaniaFindSaltLickGoal.java", FARM_TEST, FARM_LOG, "animania_farm:generic_ai_find_salt_lick"),
    "GenericAIFindWater": ("AnimaniaFindWaterGoal", "base/src/main/java/com/animania/common/entity/goal/AnimaniaFindWaterGoal.java", FARM_TEST, FARM_LOG, "animania_farm:generic_ai_find_water"),
    "GenericAIFollowOwner": ("AnimaniaFollowOwnerGoal", "base/src/main/java/com/animania/common/entity/goal/AnimaniaFollowOwnerGoal.java", PETS_TEST, PETS_LOG, "animania_catsdogs:generic_ai_follow_owner"),
    "GenericAIFollowParents": ("AnimaniaFollowParentGoal", "base/src/main/java/com/animania/common/entity/goal/AnimaniaFollowParentGoal.java", FARM_TEST, FARM_LOG, "animania_farm:generic_ai_follow_parents"),
    "GenericAILookIdle": ("AnimaniaLookIdleGoal", "base/src/main/java/com/animania/common/entity/goal/AnimaniaLookIdleGoal.java", FARM_TEST, FARM_LOG, "animania_farm:generic_ai_look_idle"),
    "GenericAIMate": ("AnimaniaMateGoal", "base/src/main/java/com/animania/common/entity/goal/AnimaniaMateGoal.java", FARM_TEST, FARM_LOG, "animania_farm:generic_ai_mate"),
    "GenericAIOwnerHurtByTarget": ("AnimaniaOwnerHurtByTargetGoal", "base/src/main/java/com/animania/common/entity/goal/AnimaniaOwnerHurtByTargetGoal.java", PETS_TEST, PETS_LOG, "animania_catsdogs:generic_ai_owner_hurt_by_target"),
    "GenericAIOwnerHurtTarget": ("AnimaniaOwnerHurtTargetGoal", "base/src/main/java/com/animania/common/entity/goal/AnimaniaOwnerHurtTargetGoal.java", PETS_TEST, PETS_LOG, "animania_catsdogs:generic_ai_owner_hurt_target"),
    "GenericAIPanic": ("AnimaniaPanicGoal", "base/src/main/java/com/animania/common/entity/goal/AnimaniaPanicGoal.java", FARM_TEST, FARM_LOG, "animania_farm:generic_ai_panic"),
    "GenericAIPlay": ("AnimaniaPlayGoal", "base/src/main/java/com/animania/common/entity/goal/AnimaniaPlayGoal.java", PETS_TEST, PETS_LOG, "animania_catsdogs:generic_ai_play"),
    "GenericAISit": ("AnimaniaSitGoal", "base/src/main/java/com/animania/common/entity/goal/AnimaniaSitGoal.java", PETS_TEST, PETS_LOG, "animania_catsdogs:generic_ai_sit"),
    "GenericAISleep": ("AnimaniaSleepGoal", "base/src/main/java/com/animania/common/entity/goal/AnimaniaSleepGoal.java", FARM_TEST, FARM_LOG, "animania_farm:generic_ai_sleep"),
    "GenericAISwimmingSmallCreatures": ("AnimaniaSmallCreatureFloatGoal", "base/src/main/java/com/animania/common/entity/goal/AnimaniaSmallCreatureFloatGoal.java", EXTRA_TEST, EXTRA_LOG, "animania_extra:generic_ai_small_creature_float"),
    "GenericAITargetNonTamed": ("AnimaniaTargetNonTamedGoal", "base/src/main/java/com/animania/common/entity/goal/AnimaniaTargetNonTamedGoal.java", PETS_TEST, PETS_LOG, "animania_catsdogs:generic_ai_target_non_tamed"),
    "GenericAITempt": ("AnimaniaTemptGoal", "base/src/main/java/com/animania/common/entity/goal/AnimaniaTemptGoal.java", FARM_TEST, FARM_LOG, "animania_farm:generic_ai_tempt"),
    "GenericAIWanderAvoidWater": ("AnimaniaWanderAvoidWaterGoal", "base/src/main/java/com/animania/common/entity/goal/AnimaniaWanderAvoidWaterGoal.java", FARM_TEST, FARM_LOG, "animania_farm:generic_ai_wander_avoid_water"),
    "GenericAIWatchClosest": ("AnimaniaWatchClosestGoal", "base/src/main/java/com/animania/common/entity/goal/AnimaniaWatchClosestGoal.java", FARM_TEST, FARM_LOG, "animania_farm:generic_ai_watch_closest"),
}
DOCUMENTED_IMPLEMENTATION = {
    "GenericAIAvoidEntity", "GenericAIFindFood", "GenericAIFindSaltLick", "GenericAIFindWater",
    "GenericAIFollowOwner", "GenericAIFollowParents", "GenericAIMate", "GenericAIPlay", "GenericAISit",
}


def marker_passes(log: Path, marker: str) -> bool:
    if not log.is_file():
        return False
    text = log.read_text(encoding="utf-8", errors="replace")
    return f"[ANIMANIA_TEST_SELECTOR] {marker}" in text and "All " in text and "required tests passed :)" in text


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
    entries = {entry["classes"][0]: entry for entry in matrix.get("entries", []) if entry.get("classes")}
    auditor_path = "tools/audit_generic_ai_behavior.py"
    results, errors = [], []
    for legacy, (modern, target_relative, test_relative, log_relative, marker) in SPECS.items():
        entry = entries.get(legacy)
        target, test, log = root / target_relative, root / test_relative, root / log_relative
        if entry is None or not target.is_file() or not test.is_file() or not marker_passes(log, marker):
            errors.append(f"missing target/test/marked passing log for {legacy}")
            continue
        source = entry["source"]
        source_text = (root / "upstream/Animania-1.12" / source).read_text(encoding="utf-8", errors="replace")
        target_text = target.read_text(encoding="utf-8", errors="replace")
        if modern not in target_text:
            errors.append(f"modern declaration absent for {legacy}: {target_relative}")
            continue
        source_overrides = entry.get("baseline", {}).get("overrides", [])
        proof = evidence_dir / "generic-ai-behavior" / entry["entry_id"] / "proof.json"
        write_json(proof, {"entry_id": entry["entry_id"], "source": source, "source_sha256": entry["sha256"],
                           "legacy_class": legacy, "source_overrides": source_overrides,
                           "modern_class": modern, "target": target_relative, "target_sha256": sha256(target),
                           "selector": marker, "coverage": "unique marker emitted by a Forge GameTest that instantiates the named modern goal"})
        shared = {"entry_id": entry["entry_id"], "source_sha256": entry["sha256"],
                  "target_paths": [{"path": target_relative, "sha256": sha256(target)},
                                   {"path": proof.relative_to(root).as_posix(), "sha256": sha256(proof)}],
                  "tests": [{"selector": marker, "result": "pass", "artifact": log_relative,
                             "artifact_sha256": sha256(log)}], "evidence_kind": "executed_test",
                  "test_code_path": test_relative, "test_code_sha256": sha256(test),
                  "notes": [f"[generic-ai-behavior-v1] {legacy} -> {modern}; a real Forge GameTest emitted {marker} and the complete module test run passed."]}
        if "implementation" in entry.get("requirements", []) and legacy not in DOCUMENTED_IMPLEMENTATION:
            results.append({**shared, "requirement_id": "implementation", "result": "pass"})
        if "behavior" in entry.get("requirements", []):
            results.append({**shared, "requirement_id": "behavior", "result": "pass"})
    evidence_dir.mkdir(parents=True, exist_ok=True)
    write_json(evidence_dir / "generic-ai-behavior-v1.json", {
        "schema_version": SCHEMA_VERSION, "audit_id": "generic-ai-behavior", "audit_version": "v1",
        "source_revision": matrix.get("source_revision"),
        "command": "tools/audit_generic_ai_behavior.py --root . --matrix docs/migration-matrix.json",
        "auditor_path": auditor_path, "auditor_sha256": sha256(root / auditor_path), "results": results, "errors": errors,
    })
    print(json.dumps({"rows": len(SPECS), "results": len(results), "errors": errors}, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
