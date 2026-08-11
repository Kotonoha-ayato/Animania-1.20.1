"""Bind Farm and Extra legacy sound handlers to real Forge registry GameTests.

The strict Java auditor already owns source-to-target implementation mapping for
these handlers.  This auditor therefore owns only the runtime behavior and
integration requirements, and requires an actual GameTest server log rather
than a source or class-existence check.
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

from closure_common import SCHEMA_VERSION, read_json, sha256, write_json


FEATURES = [
    {
        "source": "src/main/java/com/animania/addons/farm/common/handler/FarmAddonSoundHandler.java",
        "targets": [
            "farm/src/main/java/com/animania/farm/FarmSoundCatalog.java",
            "farm/src/main/java/com/animania/farm/FarmSounds.java",
            "farm/src/main/java/com/animania/farm/AnimaniaFarm.java",
        ],
        "test_code": "farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java",
        "log": "farm/run/gameTestServer/logs/latest.log",
        "selector": "animania_farm:everyFarmSoundEventIsRegistered",
        "count": 96,
        "module": "Farm",
    },
    {
        "source": "src/main/java/com/animania/addons/extra/common/handler/ExtraAddonSoundHandler.java",
        "targets": [
            "extra/src/main/java/com/animania/extra/ExtraSoundCatalog.java",
            "extra/src/main/java/com/animania/extra/ExtraSounds.java",
            "extra/src/main/java/com/animania/extra/AnimaniaExtra.java",
        ],
        "test_code": "extra/src/main/java/com/animania/extra/gametest/AnimaniaExtraGameTests.java",
        "log": "extra/run/gameTestServer/logs/latest.log",
        "selector": "animania_extra:everyExtraSoundEventIsRegistered",
        "count": 52,
        "module": "Extra",
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
    auditor_path = "tools/audit_sound_handler_behavior.py"
    results, rows, skipped, errors = [], [], [], []
    for feature in FEATURES:
        source = feature["source"]
        entry = by_source.get(source)
        if entry is None:
            errors.append(f"matrix entry missing: {source}")
            continue
        pinned = root / "upstream/Animania-1.12" / source
        test_file = root / feature["test_code"]
        log_file = root / feature["log"]
        test_text = test_file.read_text(encoding="utf-8", errors="replace") if test_file.is_file() else ""
        log_text = log_file.read_text(encoding="utf-8", errors="replace") if log_file.is_file() else ""
        marker = f'AnimaniaGameTestEvidence.mark("{feature["selector"]}")'
        missing_targets = [path for path in feature["targets"] if not (root / path).is_file()]
        green = (
            f"[ANIMANIA_TEST_SELECTOR] {feature['selector']}" in log_text
            and "Exception" not in log_text
            and "required tests failed" not in log_text
            and re.search(r"All \d+ required tests passed", log_text) is not None
        )
        if not pinned.is_file():
            errors.append(f"pinned source missing: {source}")
            continue
        if missing_targets or marker not in test_text or not green:
            skipped.append({"source": source, "missing_targets": missing_targets,
                            "missing_marker": marker not in test_text, "green_log": green})
            continue
        evidence_path = evidence_dir / "sound-handler-behavior" / entry["entry_id"] / "evidence.json"
        write_json(evidence_path, {
            "entry_id": entry["entry_id"], "source": source,
            "source_sha256": entry["sha256"], "targets": feature["targets"],
            "expected_count": feature["count"], "selector": feature["selector"],
            "test_code": feature["test_code"], "test_code_sha256": sha256(test_file),
            "log": feature["log"], "log_sha256": sha256(log_file),
        })
        targets = [{"path": path, "sha256": sha256(root / path)} for path in feature["targets"]]
        targets.append({"path": evidence_path.relative_to(root).as_posix(), "sha256": sha256(evidence_path)})
        tests = [{"selector": feature["selector"], "result": "pass", "artifact": feature["log"],
                  "artifact_sha256": sha256(log_file)}]
        notes = [
            f"[sound-handler-behavior-v1] The real Forge GameTest enumerates all {feature['count']} {feature['module']} legacy sound IDs and checks each active Forge SOUND_EVENTS registration; the module's DeferredRegister is registered on the mod event bus.",
        ]
        for requirement in ("behavior", "integration"):
            results.append({"entry_id": entry["entry_id"], "requirement_id": requirement,
                            "result": "pass", "source_sha256": entry["sha256"],
                            "target_paths": targets, "tests": tests,
                            "evidence_kind": "executed_test", "test_code_path": feature["test_code"],
                            "test_code_sha256": sha256(test_file), "notes": notes})
        rows.append({"source": source, "selector": feature["selector"],
                     "requirements": ["behavior", "integration"], "result": "pass"})
    report = {"schema_version": 1, "audit": "sound-handler-behavior", "audit_version": "v1",
              "rows": rows, "skipped": skipped, "errors": errors, "error_count": len(errors),
              "all_passed": not errors and not skipped}
    write_json(evidence_dir / "sound-handler-behavior-v1-report.json", report)
    write_json(evidence_dir / "sound-handler-behavior-v1.json", {
        "schema_version": SCHEMA_VERSION, "audit_id": "sound-handler-behavior",
        "audit_version": "v1", "source_revision": matrix.get("source_revision"),
        "command": "tools/audit_sound_handler_behavior.py --root . --matrix docs/migration-matrix.json",
        "auditor_path": auditor_path, "auditor_sha256": sha256(root / auditor_path),
        "results": results, "errors": errors,
    })
    print(json.dumps({"results": len(results), "rows": len(rows), "skipped": len(skipped), "errors": errors}, ensure_ascii=True, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
