"""Bind legacy Farm milk/honey fluid blocks to live fluid-state tests."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from closure_common import SCHEMA_VERSION, read_json, sha256, write_json


TEST_CODE = "farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java"
LOG = "farm/run/gameTestServer/logs/latest.log"
SELECTOR = "animania_farm:farmFluidsAndCheeseMoldProcess"
FEATURES = [
    {
        "source": "src/main/java/com/animania/addons/farm/common/block/fluids/BlockFluidHoney.java",
        "targets": ["farm/src/main/java/com/animania/farm/FarmFluids.java", "farm/src/main/java/com/animania/farm/FarmLegacyFluidBlock.java"],
        "note": "honey fluid is registered as a source/flowing pair, carries the legacy regeneration behavior and binds to the live hive fluid",
    },
    {
        "source": "src/main/java/com/animania/addons/farm/common/block/fluids/BlockFluidMilk.java",
        "targets": ["farm/src/main/java/com/animania/farm/FarmFluids.java", "farm/src/main/java/com/animania/farm/FarmLegacyFluidBlock.java"],
        "note": "all five legacy milk fluids are registered as source/flowing pairs and accepted by the modern cheese-mold process",
    },
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
    test_file, log_file = root / TEST_CODE, root / LOG
    test_text = test_file.read_text(encoding="utf-8", errors="replace") if test_file.is_file() else ""
    log_text = log_file.read_text(encoding="utf-8", errors="replace") if log_file.is_file() else ""
    auditor_path = "tools/audit_farm_fluid_behavior.py"
    auditor_hash = sha256(root / auditor_path)
    unique_dir = evidence_dir / "farm-fluid-behavior"
    unique_dir.mkdir(parents=True, exist_ok=True)
    results, rows, skipped, errors = [], [], [], []
    green = "All " in log_text and " required tests passed" in log_text and "required tests failed" not in log_text
    for feature in FEATURES:
        source = feature["source"]
        entry = by_source.get(source)
        if entry is None:
            errors.append(f"matrix entry missing: {source}")
            continue
        if not (root / "upstream/Animania-1.12" / source).is_file():
            errors.append(f"pinned source missing: {source}")
            continue
        missing_targets = [path for path in feature["targets"] if not (root / path).is_file()]
        missing_marker = f'AnimaniaGameTestEvidence.mark("{SELECTOR}")' not in test_text
        missing_runtime = f"[ANIMANIA_TEST_SELECTOR] {SELECTOR}" not in log_text
        if missing_targets or missing_marker or missing_runtime or not green:
            skipped.append({"source": source, "missing_targets": missing_targets,
                            "missing_marker": missing_marker, "missing_runtime": missing_runtime,
                            "green_log": green})
            continue
        unique_path = unique_dir / entry["entry_id"] / "evidence.json"
        write_json(unique_path, {"entry_id": entry["entry_id"], "source": source,
                                 "source_sha256": entry["sha256"], "targets": feature["targets"],
                                 "selector": SELECTOR, "test_code": TEST_CODE,
                                 "test_code_sha256": sha256(test_file), "log": LOG,
                                 "log_sha256": sha256(log_file)})
        targets = [{"path": path, "sha256": sha256(root / path)} for path in feature["targets"]]
        targets.append({"path": unique_path.relative_to(root).as_posix(), "sha256": sha256(unique_path)})
        tests = [{"selector": SELECTOR, "result": "pass", "artifact": LOG,
                  "artifact_sha256": sha256(log_file)}]
        notes = [f"[farm-fluid-behavior-v1] {source}: {feature['note']}."]
        for requirement in entry.get("requirements", []):
            results.append({"entry_id": entry["entry_id"], "requirement_id": requirement,
                            "result": "pass", "source_sha256": entry["sha256"],
                            "target_paths": targets, "tests": tests,
                            "evidence_kind": "executed_test", "test_code_path": TEST_CODE,
                            "test_code_sha256": sha256(test_file), "notes": notes})
        rows.append({"source": source, "selector": SELECTOR,
                     "requirements": entry.get("requirements", []), "result": "pass"})
    write_json(evidence_dir / "farm-fluid-behavior-v1-report.json", {
        "schema_version": 1, "audit": "farm-fluid-behavior", "audit_version": "v1",
        "rows": rows, "skipped": skipped, "errors": errors, "error_count": len(errors),
        "all_passed": not errors and not skipped})
    write_json(evidence_dir / "farm-fluid-behavior-v1.json", {
        "schema_version": SCHEMA_VERSION, "audit_id": "farm-fluid-behavior",
        "audit_version": "v1", "source_revision": matrix.get("source_revision"),
        "command": "tools/audit_farm_fluid_behavior.py --root . --matrix docs/migration-matrix.json",
        "auditor_path": auditor_path, "auditor_sha256": auditor_hash, "results": results, "errors": errors})
    print(json.dumps({"results": len(results), "rows": len(rows), "skipped": len(skipped), "errors": errors}, ensure_ascii=True, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
