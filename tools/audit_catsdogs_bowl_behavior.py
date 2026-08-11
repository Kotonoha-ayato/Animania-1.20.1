"""Bind the Cats&Dogs pet-bowl block, handlers and storage to one parameterized live test."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from closure_common import SCHEMA_VERSION, read_json, sha256, write_json


TEST_CODE = "catsdogs/src/main/java/com/animania/catsdogs/gametest/AnimaniaCatsDogsGameTests.java"
LOG = "catsdogs/run/gameTestServer/logs/latest.log"
SELECTOR = "animania_catsdogs:petBowlFoodAndWaterCapabilities"
FEATURES = [
    ("src/main/java/com/animania/addons/catsdogs/common/block/BlockPetBowl.java",
     ["catsdogs/src/main/java/com/animania/catsdogs/CatsDogsPetBowlBlock.java", "catsdogs/src/main/java/com/animania/catsdogs/CatsDogsContent.java"],
     "native invisible bowl placement, food validation and facility block-entity creation"),
    ("src/main/java/com/animania/addons/catsdogs/common/tileentity/handler/FluidHandlerPetBowl.java",
     ["catsdogs/src/main/java/com/animania/catsdogs/CatsDogsPetBowlBlockEntity.java", "base/src/main/java/com/animania/common/block/AnimaniaStorageBlockEntity.java"],
     "water-only Forge fluid capability, lava rejection and sided automation gate"),
    ("src/main/java/com/animania/addons/catsdogs/common/tileentity/handler/ItemHandlerPetBowl.java",
     ["catsdogs/src/main/java/com/animania/catsdogs/CatsDogsPetBowlBlockEntity.java", "base/src/main/java/com/animania/common/block/AnimaniaStorageBlockEntity.java"],
     "Forge item capability presence, one-item food insertion and sided automation gate"),
    ("src/main/java/com/animania/addons/catsdogs/common/tileentity/TileEntityPetBowl.java",
     ["catsdogs/src/main/java/com/animania/catsdogs/CatsDogsPetBowlBlockEntity.java"],
     "server storage behavior and water/food state contract"),
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
    auditor_path = "tools/audit_catsdogs_bowl_behavior.py"
    green = "All 12 required tests passed" in log_text and "required tests failed" not in log_text and "Exception" not in log_text
    results, rows, skipped, errors = [], [], [], []
    for source, targets_for_feature, note in FEATURES:
        entry = by_source.get(source)
        if entry is None:
            errors.append(f"matrix entry missing: {source}")
            continue
        if not (root / "upstream/Animania-1.12" / source).is_file():
            errors.append(f"pinned source missing: {source}")
            continue
        missing_targets = [path for path in targets_for_feature if not (root / path).is_file()]
        missing_marker = f'AnimaniaGameTestEvidence.mark("{SELECTOR}")' not in test_text
        missing_runtime = f"[ANIMANIA_TEST_SELECTOR] {SELECTOR}" not in log_text
        if missing_targets or missing_marker or missing_runtime or not green:
            skipped.append({"source": source, "missing_targets": missing_targets,
                            "missing_marker": missing_marker, "missing_runtime": missing_runtime,
                            "green_log": green})
            continue
        unique_path = evidence_dir / "catsdogs-bowl-behavior" / entry["entry_id"] / "evidence.json"
        write_json(unique_path, {"entry_id": entry["entry_id"], "source": source,
                                 "source_sha256": entry["sha256"], "targets": targets_for_feature,
                                 "selector": SELECTOR, "test_code": TEST_CODE,
                                 "test_code_sha256": sha256(test_file), "log": LOG,
                                 "log_sha256": sha256(log_file), "note": note})
        targets = [{"path": path, "sha256": sha256(root / path)} for path in targets_for_feature]
        targets.append({"path": unique_path.relative_to(root).as_posix(), "sha256": sha256(unique_path)})
        tests = [{"selector": SELECTOR, "result": "pass", "artifact": LOG,
                  "artifact_sha256": sha256(log_file)}]
        notes = [f"[catsdogs-bowl-behavior-v1] {source}: {note}; the live test also checks the invisible native render shape and all registered pet-facility block entities."]
        for requirement in entry.get("requirements", []):
            results.append({"entry_id": entry["entry_id"], "requirement_id": requirement,
                            "result": "pass", "source_sha256": entry["sha256"],
                            "target_paths": targets, "tests": tests,
                            "evidence_kind": "executed_test", "test_code_path": TEST_CODE,
                            "test_code_sha256": sha256(test_file), "notes": notes})
        rows.append({"source": source, "selector": SELECTOR,
                     "requirements": entry.get("requirements", []), "result": "pass"})
    write_json(evidence_dir / "catsdogs-bowl-behavior-v1-report.json", {
        "schema_version": 1, "audit": "catsdogs-bowl-behavior", "audit_version": "v1",
        "rows": rows, "skipped": skipped, "errors": errors, "error_count": len(errors),
        "all_passed": not errors and not skipped,
    })
    write_json(evidence_dir / "catsdogs-bowl-behavior-v1.json", {
        "schema_version": SCHEMA_VERSION, "audit_id": "catsdogs-bowl-behavior",
        "audit_version": "v1", "source_revision": matrix.get("source_revision"),
        "command": "tools/audit_catsdogs_bowl_behavior.py --root . --matrix docs/migration-matrix.json",
        "auditor_path": auditor_path, "auditor_sha256": sha256(root / auditor_path),
        "results": results, "errors": errors,
    })
    print(json.dumps({"results": len(results), "rows": len(rows), "skipped": len(skipped), "errors": errors}, ensure_ascii=True, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
