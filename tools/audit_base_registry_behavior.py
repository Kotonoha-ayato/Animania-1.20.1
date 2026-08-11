"""Bind Base registry/tag compatibility contracts to live Forge GameTests."""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

from closure_common import SCHEMA_VERSION, read_json, sha256, write_json


TEST_CODE = "base/src/main/java/com/animania/gametest/AnimaniaBaseGameTests.java"
LOG = "base/run/gametestserver/logs/latest.log"
FEATURES = [
    {
        "source": "src/main/java/com/animania/common/ModSoundEvents.java",
        "targets": [
            "base/src/main/java/com/animania/common/AnimaniaSounds.java",
            "base/src/main/java/com/animania/Animania.java",
            "base/src/main/resources/assets/animania/sounds.json",
        ],
        "selector": "animania:everyLegacyBaseSoundEventIsRegistered",
        "note": "both legacy Base sound events are DeferredRegister-backed, resource-backed and live in Forge registry",
    },
    {
        "source": "src/main/java/com/animania/common/handler/DictionaryHandler.java",
        "targets": [
            "base/src/main/java/com/animania/api/AnimaniaLegacyTags.java",
            "base/src/main/java/com/animania/gametest/AnimaniaBaseGameTests.java",
        ],
        "selector": "animania:legacyOreDictionaryCategoriesResolveThroughModernTags",
        "note": "all legacy crop, seed, bread, sugar, meat, wool and sixteen dye categories resolve through live modern tags",
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
    test_file = root / TEST_CODE
    log_file = root / LOG
    test_text = test_file.read_text(encoding="utf-8", errors="replace") if test_file.is_file() else ""
    log_text = log_file.read_text(encoding="utf-8", errors="replace") if log_file.is_file() else ""
    auditor_path = "tools/audit_base_registry_behavior.py"
    auditor_hash = sha256(root / auditor_path)
    unique_dir = evidence_dir / "base-registry-behavior"
    unique_dir.mkdir(parents=True, exist_ok=True)
    results = []
    rows = []
    skipped = []
    errors = []
    for feature in FEATURES:
        source = feature["source"]
        entry = by_source.get(source)
        if entry is None:
            errors.append(f"matrix entry missing: {source}")
            continue
        old = root / "upstream/Animania-1.12" / source
        if not old.is_file():
            errors.append(f"pinned source missing: {source}")
            continue
        missing = [path for path in feature["targets"] if not (root / path).is_file()]
        marker = f'AnimaniaGameTestEvidence.mark("{feature["selector"]}")'
        if not test_file.is_file() or not log_file.is_file() or missing or marker not in test_text:
            skipped.append({"source": source, "reason": "source/target/test marker missing", "missing_targets": missing})
            continue
        if f"[ANIMANIA_TEST_SELECTOR] {feature['selector']}" not in log_text:
            skipped.append({"source": source, "reason": "selector not present in runtime log"})
            continue
        if not re.search(r"All \d+ required tests passed", log_text) or re.search(
                r"required tests failed|Game test server crashed|Exception in server tick loop", log_text):
            skipped.append({"source": source, "reason": "runtime log is not a green GameTest run"})
            continue
        unique_path = unique_dir / entry["entry_id"] / "evidence.json"
        write_json(unique_path, {
            "entry_id": entry["entry_id"], "source": source, "source_sha256": entry["sha256"],
            "selector": feature["selector"], "test_code": TEST_CODE,
            "test_code_sha256": sha256(test_file), "log": LOG, "log_sha256": sha256(log_file),
        })
        targets = [{"path": path, "sha256": sha256(root / path)} for path in feature["targets"]]
        targets.append({"path": unique_path.relative_to(root).as_posix(), "sha256": sha256(unique_path)})
        tests = [{"selector": feature["selector"], "result": "pass", "artifact": LOG,
                  "artifact_sha256": sha256(log_file)}]
        notes = [f"[base-registry-behavior-v1] {source}: {feature['note']}"]
        for requirement in entry.get("requirements", []):
            results.append({
                "entry_id": entry["entry_id"], "requirement_id": requirement, "result": "pass",
                "source_sha256": entry["sha256"], "target_paths": targets, "tests": tests,
                "evidence_kind": "executed_test", "test_code_path": TEST_CODE,
                "test_code_sha256": sha256(test_file), "notes": notes,
            })
        rows.append({"source": source, "selector": feature["selector"],
                     "requirements": entry.get("requirements", []), "result": "pass"})
    evidence_dir.mkdir(parents=True, exist_ok=True)
    write_json(evidence_dir / "base-registry-behavior-v1-report.json", {
        "schema_version": 1, "audit": "base-registry-behavior", "audit_version": "v1",
        "rows": rows, "skipped": skipped, "errors": errors,
        "error_count": len(errors), "all_passed": not errors and not skipped,
    })
    write_json(evidence_dir / "base-registry-behavior-v1.json", {
        "schema_version": SCHEMA_VERSION, "audit_id": "base-registry-behavior", "audit_version": "v1",
        "source_revision": matrix.get("source_revision"),
        "command": "tools/audit_base_registry_behavior.py --root . --matrix docs/migration-matrix.json",
        "auditor_path": auditor_path, "auditor_sha256": auditor_hash, "results": results,
        "errors": errors,
    })
    print(json.dumps({"results": len(results), "rows": len(rows), "skipped": len(skipped), "errors": errors},
                     ensure_ascii=True, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
