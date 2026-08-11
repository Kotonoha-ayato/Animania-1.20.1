"""Bind the Farm horse and temptation goal migrations to live GameTests."""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

from closure_common import SCHEMA_VERSION, read_json, sha256, write_json


TEST_CODE = "farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java"
LOG = "farm/run/gameTestServer/logs/latest.log"
FEATURES = [
    ("src/main/java/com/animania/addons/farm/common/entity/horses/ai/EntityAIFollowMateHorses.java",
     "base/src/main/java/com/animania/common/entity/goal/AnimaniaMateGoal.java",
     "animania_farm:horseGoalsRespectDayRiderAndPullingGates",
     "reserved-mate selection, daylight gate and rider/pulling restrictions"),
    ("src/main/java/com/animania/addons/farm/common/entity/horses/ai/EntityAILookIdleHorses.java",
     "base/src/main/java/com/animania/common/entity/goal/AnimaniaLookIdleGoal.java",
     "animania_farm:horseGoalsRespectDayRiderAndPullingGates",
     "species daylight look-idle gate"),
    ("src/main/java/com/animania/addons/farm/common/entity/horses/ai/EntityAIWanderHorses.java",
     "base/src/main/java/com/animania/common/entity/goal/AnimaniaWanderAvoidWaterGoal.java",
     "animania_farm:horseGoalsRespectDayRiderAndPullingGates",
     "daylight, rider and pulling wander gates"),
    ("src/main/java/com/animania/addons/farm/common/entity/horses/ai/EntityHorseEatGrass.java",
     "base/src/main/java/com/animania/common/entity/goal/AnimaniaEatGrassGoal.java",
     "animania_farm:horseGoalsRespectDayRiderAndPullingGates",
     "legacy grass target, chewing duration and rider/pulling gate"),
    ("src/main/java/com/animania/addons/farm/common/entity/pigs/ai/EntityAITemptItemStack.java",
     "base/src/main/java/com/animania/common/entity/goal/AnimaniaTemptGoal.java",
     "animania_farm:farmTemptationUsesLiveSpeciesFoodRules",
     "species food selection, carrot-on-a-stick and 100-tick cooldown"),
]


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
    by_source = {entry.get("source"): entry for entry in matrix.get("entries", [])}
    test_file = root / TEST_CODE
    log_file = root / LOG
    test_text = test_file.read_text(encoding="utf-8", errors="replace") if test_file.is_file() else ""
    log_text = log_file.read_text(encoding="utf-8", errors="replace") if log_file.is_file() else ""
    auditor_path = "tools/audit_farm_goal_behavior.py"
    auditor_hash = sha256(root / auditor_path)
    unique_dir = evidence_dir / "farm-goal-behavior"
    unique_dir.mkdir(parents=True, exist_ok=True)
    results, rows, skipped, errors = [], [], [], []
    for source, target, selector, note in FEATURES:
        entry = by_source.get(source)
        if entry is None:
            errors.append(f"matrix entry missing: {source}")
            continue
        if not (root / "upstream/Animania-1.12" / source).is_file():
            errors.append(f"pinned source missing: {source}")
            continue
        marker = f'AnimaniaGameTestEvidence.mark("{selector}")'
        if not test_file.is_file() or not log_file.is_file() or not (root / target).is_file() or marker not in test_text:
            skipped.append({"source": source, "reason": "target/test marker missing"})
            continue
        if f"[ANIMANIA_TEST_SELECTOR] {selector}" not in log_text:
            skipped.append({"source": source, "reason": "selector absent from runtime log"})
            continue
        if not re.search(r"All \d+ required tests passed", log_text) or re.search(
                r"required tests failed|Game test server crashed|Exception in server tick loop", log_text):
            skipped.append({"source": source, "reason": "Farm GameTest log is not green"})
            continue
        unique_path = unique_dir / entry["entry_id"] / "evidence.json"
        write_json(unique_path, {"entry_id": entry["entry_id"], "source": source,
                                 "source_sha256": entry["sha256"], "target": target,
                                 "selector": selector, "test_code": TEST_CODE,
                                 "test_code_sha256": sha256(test_file), "log": LOG,
                                 "log_sha256": sha256(log_file)})
        targets = [{"path": target, "sha256": sha256(root / target)},
                   {"path": unique_path.relative_to(root).as_posix(), "sha256": sha256(unique_path)}]
        tests = [{"selector": selector, "result": "pass", "artifact": LOG,
                  "artifact_sha256": sha256(log_file)}]
        notes = [f"[farm-goal-behavior-v1] {source}: {note}"]
        for requirement in entry.get("requirements", []):
            results.append({"entry_id": entry["entry_id"], "requirement_id": requirement,
                            "result": "pass", "source_sha256": entry["sha256"],
                            "target_paths": targets, "tests": tests,
                            "evidence_kind": "executed_test", "test_code_path": TEST_CODE,
                            "test_code_sha256": sha256(test_file), "notes": notes})
        rows.append({"source": source, "selector": selector,
                     "requirements": entry.get("requirements", []), "result": "pass"})
    evidence_dir.mkdir(parents=True, exist_ok=True)
    write_json(evidence_dir / "farm-goal-behavior-v1-report.json", {
        "schema_version": 1, "audit": "farm-goal-behavior", "audit_version": "v1",
        "rows": rows, "skipped": skipped, "errors": errors,
        "error_count": len(errors), "all_passed": not errors and not skipped})
    write_json(evidence_dir / "farm-goal-behavior-v1.json", {
        "schema_version": SCHEMA_VERSION, "audit_id": "farm-goal-behavior", "audit_version": "v1",
        "source_revision": matrix.get("source_revision"),
        "command": "tools/audit_farm_goal_behavior.py --root . --matrix docs/migration-matrix.json",
        "auditor_path": auditor_path, "auditor_sha256": auditor_hash,
        "results": results, "errors": errors})
    print(json.dumps({"results": len(results), "rows": len(rows), "skipped": len(skipped), "errors": errors},
                     ensure_ascii=True, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
