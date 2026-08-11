"""Bind Extra hamster wheel/ball behavior to live server selectors."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from closure_common import SCHEMA_VERSION, read_json, sha256, write_json


TEST_CODE = "extra/src/main/java/com/animania/extra/gametest/AnimaniaExtraGameTests.java"
LOG = "extra/run/gameTestServer/logs/latest.log"
FEATURES = [
    {
        "source": "src/main/java/com/animania/addons/extra/common/block/BlockHamsterWheel.java",
        "targets": ["extra/src/main/java/com/animania/extra/ExtraHamsterWheelBlock.java", "extra/src/main/java/com/animania/extra/ExtraHamsterWheelBlockEntity.java"],
        "selectors": ["animania_extra:hamsterWheelGeneratesForgeEnergy", "animania_extra:hamster_interaction_menu_and_state"],
        "note": "a stored fed hamster starts the wheel, the block entity produces Forge energy, and the one-slot food menu rejects invalid items",
    },
    {
        "source": "src/main/java/com/animania/addons/extra/common/entity/rodents/EntityHamster.java",
        "targets": ["base/src/main/java/com/animania/common/entity/AnimaniaAnimalEntity.java", "extra/src/main/java/com/animania/extra/ExtraContent.java", "extra/src/main/java/com/animania/extra/client/model/ExtraLegacyModelLayers.java"],
        "selectors": ["animania_extra:hamster_carry_server_round_trip", "animania_extra:hamsterDeathReturnsExactlyOneColourPreservingBall", "animania_extra:hamster_interaction_menu_and_state"],
        "note": "server-authoritative taming, cheek-pouch, standing, ball and carry state round-trip through NBT and lethal death returns exactly one colour-preserving ball",
    },
    {
        "source": "src/main/java/com/animania/addons/extra/common/item/ItemHamsterBall.java",
        "targets": ["extra/src/main/java/com/animania/extra/AnimaniaHamsterBallItem.java", "extra/src/main/java/com/animania/extra/ExtraContent.java"],
        "selectors": ["animania_extra:hamster_carry_server_round_trip", "animania_extra:hamsterDeathReturnsExactlyOneColourPreservingBall"],
        "note": "coloured ball NBT is consumed, restored and returned without colour loss",
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
    auditor_path = "tools/audit_extra_hamster_behavior.py"
    auditor_hash = sha256(root / auditor_path)
    unique_dir = evidence_dir / "extra-hamster-behavior"
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
        missing_markers = [selector for selector in feature["selectors"]
                           if f'AnimaniaGameTestEvidence.mark("{selector}")' not in test_text]
        missing_runtime = [selector for selector in feature["selectors"]
                           if f"[ANIMANIA_TEST_SELECTOR] {selector}" not in log_text]
        if missing_targets or missing_markers or missing_runtime or not green:
            skipped.append({"source": source, "missing_targets": missing_targets,
                            "missing_markers": missing_markers, "missing_runtime": missing_runtime,
                            "green_log": green})
            continue
        unique_path = unique_dir / entry["entry_id"] / "evidence.json"
        write_json(unique_path, {"entry_id": entry["entry_id"], "source": source,
                                 "source_sha256": entry["sha256"], "targets": feature["targets"],
                                 "selectors": feature["selectors"], "test_code": TEST_CODE,
                                 "test_code_sha256": sha256(test_file), "log": LOG,
                                 "log_sha256": sha256(log_file)})
        targets = [{"path": path, "sha256": sha256(root / path)} for path in feature["targets"]]
        targets.append({"path": unique_path.relative_to(root).as_posix(), "sha256": sha256(unique_path)})
        tests = [{"selector": selector, "result": "pass", "artifact": LOG,
                  "artifact_sha256": sha256(log_file)} for selector in feature["selectors"]]
        notes = [f"[extra-hamster-behavior-v1] {source}: {feature['note']}."]
        for requirement in entry.get("requirements", []):
            results.append({"entry_id": entry["entry_id"], "requirement_id": requirement,
                            "result": "pass", "source_sha256": entry["sha256"],
                            "target_paths": targets, "tests": tests,
                            "evidence_kind": "executed_test", "test_code_path": TEST_CODE,
                            "test_code_sha256": sha256(test_file), "notes": notes})
        rows.append({"source": source, "selectors": feature["selectors"],
                     "requirements": entry.get("requirements", []), "result": "pass"})
    write_json(evidence_dir / "extra-hamster-behavior-v1-report.json", {
        "schema_version": 1, "audit": "extra-hamster-behavior", "audit_version": "v1",
        "rows": rows, "skipped": skipped, "errors": errors, "error_count": len(errors),
        "all_passed": not errors and not skipped})
    write_json(evidence_dir / "extra-hamster-behavior-v1.json", {
        "schema_version": SCHEMA_VERSION, "audit_id": "extra-hamster-behavior",
        "audit_version": "v1", "source_revision": matrix.get("source_revision"),
        "command": "tools/audit_extra_hamster_behavior.py --root . --matrix docs/migration-matrix.json",
        "auditor_path": auditor_path, "auditor_sha256": auditor_hash, "results": results, "errors": errors})
    print(json.dumps({"results": len(results), "rows": len(rows), "skipped": len(skipped), "errors": errors}, ensure_ascii=True, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
