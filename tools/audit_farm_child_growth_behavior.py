"""Bind every Farm child base class to its live adult-transition GameTest."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from closure_common import SCHEMA_VERSION, read_json, sha256, write_json


TEST_CODE = "farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java"
LOG = "farm/run/gameTestServer/logs/latest.log"
SELECTOR = "animania_farm:everyFarmChildRegistryTypeGrowsIntoItsBreedAdult"
FEATURES = [
    ("src/main/java/com/animania/addons/farm/common/entity/chickens/EntityChickBase.java", "chick", "chicken child types"),
    ("src/main/java/com/animania/addons/farm/common/entity/cows/EntityCalfBase.java", "calf", "cow child types"),
    ("src/main/java/com/animania/addons/farm/common/entity/goats/EntityKidBase.java", "kid", "goat child types"),
    ("src/main/java/com/animania/addons/farm/common/entity/horses/EntityFoalBase.java", "foal", "horse child types"),
    ("src/main/java/com/animania/addons/farm/common/entity/pigs/EntityPigletBase.java", "piglet", "pig child types"),
    ("src/main/java/com/animania/addons/farm/common/entity/sheep/EntityLambBase.java", "lamb", "sheep child types"),
]
TARGETS = ["base/src/main/java/com/animania/common/entity/AnimaniaAnimalEntity.java", "farm/src/main/java/com/animania/farm/FarmLegacyIds.java"]


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
    test_file, log_file = root / TEST_CODE, root / LOG
    test_text = test_file.read_text(encoding="utf-8", errors="replace") if test_file.is_file() else ""
    log_text = log_file.read_text(encoding="utf-8", errors="replace") if log_file.is_file() else ""
    auditor_path = "tools/audit_farm_child_growth_behavior.py"
    auditor_hash = sha256(root / auditor_path)
    unique_dir = evidence_dir / "farm-child-growth-behavior"
    unique_dir.mkdir(parents=True, exist_ok=True)
    results, rows, skipped, errors = [], [], [], []
    green = "All " in log_text and " required tests passed" in log_text and "required tests failed" not in log_text
    for source, prefix, label in FEATURES:
        entry = by_source.get(source)
        if entry is None:
            errors.append(f"matrix entry missing: {source}")
            continue
        if not (root / "upstream/Animania-1.12" / source).is_file():
            errors.append(f"pinned source missing: {source}")
            continue
        missing_targets = [path for path in TARGETS if not (root / path).is_file()]
        missing_marker = f'AnimaniaGameTestEvidence.mark("{SELECTOR}")' not in test_text
        missing_runtime = f"[ANIMANIA_TEST_SELECTOR] {SELECTOR}" not in log_text
        if missing_targets or missing_marker or missing_runtime or not green:
            skipped.append({"source": source, "missing_targets": missing_targets,
                            "missing_marker": missing_marker, "missing_runtime": missing_runtime,
                            "green_log": green})
            continue
        unique_path = unique_dir / entry["entry_id"] / "evidence.json"
        write_json(unique_path, {"entry_id": entry["entry_id"], "source": source,
                                 "source_sha256": entry["sha256"], "family_prefix": prefix,
                                 "targets": TARGETS, "selector": SELECTOR,
                                 "test_code": TEST_CODE, "test_code_sha256": sha256(test_file),
                                 "log": LOG, "log_sha256": sha256(log_file)})
        targets = [{"path": path, "sha256": sha256(root / path)} for path in TARGETS]
        targets.append({"path": unique_path.relative_to(root).as_posix(), "sha256": sha256(unique_path)})
        tests = [{"selector": SELECTOR, "result": "pass", "artifact": LOG,
                  "artifact_sha256": sha256(log_file)}]
        notes = [f"[farm-child-growth-behavior-v1] {source}: dedicated iteration covers all registered {label}, verifies the 1.12 family prefix and care-gated adult registry transition."]
        owned_requirements = [requirement for requirement in entry.get("requirements", [])
                              if requirement != "implementation"]
        for requirement in owned_requirements:
            results.append({"entry_id": entry["entry_id"], "requirement_id": requirement,
                            "result": "pass", "source_sha256": entry["sha256"],
                            "target_paths": targets, "tests": tests,
                            "evidence_kind": "executed_test", "test_code_path": TEST_CODE,
                            "test_code_sha256": sha256(test_file), "notes": notes})
        rows.append({"source": source, "family_prefix": prefix, "selector": SELECTOR,
                     "requirements": owned_requirements, "result": "pass"})
    write_json(evidence_dir / "farm-child-growth-behavior-v1-report.json", {
        "schema_version": 1, "audit": "farm-child-growth-behavior", "audit_version": "v1",
        "rows": rows, "skipped": skipped, "errors": errors, "error_count": len(errors),
        "all_passed": not errors and not skipped})
    write_json(evidence_dir / "farm-child-growth-behavior-v1.json", {
        "schema_version": SCHEMA_VERSION, "audit_id": "farm-child-growth-behavior",
        "audit_version": "v1", "source_revision": matrix.get("source_revision"),
        "command": "tools/audit_farm_child_growth_behavior.py --root . --matrix docs/migration-matrix.json",
        "auditor_path": auditor_path, "auditor_sha256": auditor_hash, "results": results, "errors": errors})
    print(json.dumps({"results": len(results), "rows": len(rows), "skipped": len(skipped), "errors": errors}, ensure_ascii=True, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
