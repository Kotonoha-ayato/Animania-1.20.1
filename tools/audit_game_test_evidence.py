"""Bind real Forge GameTest log selectors to the confirmed behavior gaps.

The auditor is intentionally read-only with respect to the migration matrix.
Each result is tied to one legacy source entry, one modern target, one source
test method, and a runtime selector marker emitted by that method.  A green
aggregate test count without the selector marker is not accepted.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

from closure_common import SCHEMA_VERSION, read_json, sha256, validate_matrix_shape, write_json


FEATURES = [
    {
        "source": "src/main/java/com/animania/addons/extra/common/events/CarryRenderer.java",
        "target": [
            "base/src/main/java/com/animania/client/render/AnimaniaCarryRenderer.java",
            "base/src/main/java/com/animania/network/CarriedAnimalSyncPacket.java",
        ],
        "test_code": "extra/src/main/java/com/animania/extra/gametest/AnimaniaExtraGameTests.java",
        "selector": "animania_extra:hamster_carry_server_round_trip",
        "log": "base/run/gameTestServer/logs/latest.log",
        "notes": ["server-authoritative carrier state and native first/third-person renderer path"],
    },
    {
        "source": "src/main/java/com/animania/addons/extra/common/entity/rodents/ai/EntityAIFerretFindNests.java",
        "target": ["base/src/main/java/com/animania/common/entity/goal/AnimaniaFindNestFoodGoal.java"],
        "test_code": "extra/src/main/java/com/animania/extra/gametest/AnimaniaExtraGameTests.java",
        "selector": "animania_extra:ferret_forages_chicken_nest_egg",
        "log": "extra/run/gameTestServer/logs/latest.log",
        "notes": ["ferret-specific chicken egg removal, care-meter recovery, and server navigation"],
    },
    {
        "source": "src/main/java/com/animania/addons/extra/common/entity/rodents/ai/EntityAIHedgehogFindNests.java",
        "target": ["base/src/main/java/com/animania/common/entity/goal/AnimaniaFindNestFoodGoal.java"],
        "test_code": "extra/src/main/java/com/animania/extra/gametest/AnimaniaExtraGameTests.java",
        "selector": "animania_extra:hedgehog_forages_mature_crop",
        "log": "extra/run/gameTestServer/logs/latest.log",
        "notes": ["hedgehog-specific mature crop uprooting, care-meter recovery, and server navigation"],
    },
    {
        "source": "src/main/java/com/animania/addons/farm/common/entity/goats/GoatFainting.java",
        "target": ["base/src/main/java/com/animania/common/entity/AnimaniaAnimalEntity.java"],
        "test_code": "farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java",
        "selector": "animania_farm:fainting_goat_sprint_collision",
        "log": "farm/run/gameTestServer/logs/latest.log",
        "notes": ["sprinting-player collision starts a 20-tick spooked state and expires on the server"],
    },
    {
        "source": "src/main/java/com/animania/addons/farm/common/entity/goats/ai/EntityAIButtHeadsGoats.java",
        "target": ["base/src/main/java/com/animania/common/entity/goal/AnimaniaRivalHeadbuttGoal.java"],
        "test_code": "farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java",
        "selector": "animania_farm:buck_rivalry_selects_matching_family",
        "log": "farm/run/gameTestServer/logs/latest.log",
        "notes": ["adult buck rivalry selects a nearby buck, synchronizes UUIDs, and clears reciprocally"],
    },
    {
        "source": "src/main/java/com/animania/addons/farm/common/entity/sheep/ai/EntityAIButtHeadsSheep.java",
        "target": ["base/src/main/java/com/animania/common/entity/goal/AnimaniaRivalHeadbuttGoal.java"],
        "test_code": "farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java",
        "selector": "animania_farm:ram_rivalry_selects_matching_family",
        "log": "farm/run/gameTestServer/logs/latest.log",
        "notes": ["adult ram rivalry selects a nearby ram and synchronizes reciprocal rival UUIDs"],
    },
    {
        "source": "src/main/java/com/animania/addons/catsdogs/common/entity/canids/ai/AnimalAIGetDogHerded.java",
        "target": ["base/src/main/java/com/animania/common/entity/goal/AnimaniaHerdedByGermanShepherdGoal.java"],
        "test_code": "base/src/main/java/com/animania/gametest/AnimaniaBaseGameTests.java",
        "selector": "animania:germanShepherdHerdsFarmRuminantsWhenAllAddonsAreInstalled",
        "log": "base/run/gameTestServer/logs/latest.log",
        "notes": ["full-install integration selects a nearby tamed, non-sitting German shepherd"],
    },
]


def find_entry(matrix: dict, source: str) -> dict | None:
    return next((entry for entry in matrix.get("entries", []) if entry.get("source") == source), None)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, default=Path("docs/migration-matrix.json"))
    parser.add_argument("--evidence-dir", type=Path, default=Path("build/audit-evidence"))
    args = parser.parse_args()
    root = args.root.resolve()
    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    evidence_dir = args.evidence_dir if args.evidence_dir.is_absolute() else root / args.evidence_dir
    matrix = read_json(matrix_path)
    errors = validate_matrix_shape(root, matrix)
    if errors:
        print(json.dumps({"errors": errors[:100], "error_count": len(errors)}, ensure_ascii=False, indent=2))
        raise SystemExit(1)

    results = []
    for feature in FEATURES:
        entry = find_entry(matrix, feature["source"])
        if entry is None:
            errors.append(f"matrix entry missing: {feature['source']}")
            continue
        if "behavior" not in entry.get("requirements", []):
            errors.append(f"entry does not require behavior: {feature['source']}")
            continue
        test_file = root / feature["test_code"]
        log_file = root / feature["log"]
        if not test_file.is_file():
            errors.append(f"test source missing: {feature['test_code']}")
            continue
        if not log_file.is_file():
            errors.append(f"runtime GameTest log missing: {feature['log']}")
            continue
        source_text = test_file.read_text(encoding="utf-8")
        if feature["selector"] not in source_text:
            errors.append(f"selector marker is not in test source: {feature['selector']}")
            continue
        log_text = log_file.read_text(encoding="utf-8", errors="replace")
        if f"[ANIMANIA_TEST_SELECTOR] {feature['selector']}" not in log_text:
            errors.append(f"selector did not execute in runtime log: {feature['selector']}")
            continue
        if not re.search(r"All \d+ required tests passed", log_text):
            errors.append(f"runtime log has no all-required-tests-passed marker: {feature['log']}")
            continue
        if re.search(r"required tests failed|Game test server crashed|Exception in server tick loop", log_text):
            errors.append(f"runtime log contains a failed/crashed GameTest run: {feature['log']}")
            continue
        targets = []
        for target in feature["target"]:
            path = root / target
            if not path.is_file():
                errors.append(f"target missing: {target}")
            else:
                targets.append({"path": target, "sha256": sha256(path)})
        if len(targets) != len(feature["target"]):
            continue
        test_record = {
            "selector": feature["selector"],
            "result": "pass",
            "artifact": feature["log"],
            "artifact_sha256": sha256(log_file),
        }
        common = {
            "entry_id": entry["entry_id"],
            "result": "pass",
            "source_sha256": entry["sha256"],
            "target_paths": targets,
            "tests": [test_record],
            "test_code_path": feature["test_code"],
            "test_code_sha256": sha256(test_file),
            "notes": feature["notes"],
        }
        # Implementation ownership belongs to the source-to-ID mapping
        # auditor. This runtime auditor owns only the behavior requirement.
        results.append({**common, "requirement_id": "behavior", "evidence_kind": "executed_test"})

    evidence_dir.mkdir(parents=True, exist_ok=True)
    auditor_path = "tools/audit_game_test_evidence.py"
    manifest = {
        "schema_version": SCHEMA_VERSION,
        "audit_id": "game-test-behavior",
        "audit_version": "v1",
        "source_revision": matrix["source_revision"],
        "command": "tools/audit_game_test_evidence.py --root . --matrix docs/migration-matrix.json",
        "auditor_path": auditor_path,
        "auditor_sha256": sha256(root / auditor_path),
        "results": results,
    }
    write_json(evidence_dir / "game-test-behavior-v1.json", manifest)
    report = {"features": len(FEATURES), "results": len(results), "errors": errors,
              "error_count": len(errors), "evidence": "build/audit-evidence/game-test-behavior-v1.json"}
    write_json(evidence_dir / "game-test-behavior-v1-report.json", report)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
