"""Bind Farm facility/handler migrations to real Forge GameTests."""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

from closure_common import SCHEMA_VERSION, read_json, sha256, write_json


TEST_CODE = "farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java"
LOG = "farm/run/gameTestServer/logs/latest.log"
FEATURES = [
    {
        "source": "src/main/java/com/animania/addons/farm/common/block/BlockAnimaniaWool.java",
        "targets": ["farm/src/main/java/com/animania/farm/FarmWoolBlock.java", "farm/src/main/java/com/animania/farm/FarmWoolBlockItem.java"],
        "selectors": ["animania_farm:allSevenLegacyWoolVariantsPlaceAndDropTheirState"],
        "note": "all seven metadata variants preserve placement and dropped state",
    },
    {
        "source": "src/main/java/com/animania/addons/farm/common/block/BlockCheese.java",
        "targets": ["farm/src/main/java/com/animania/farm/FarmCheeseBlock.java", "farm/src/main/java/com/animania/farm/FarmContent.java"],
        "selectors": ["animania_farm:hiveFluidAndCheeseBlockState"],
        "note": "bite stages, collision shrink, comparator output and edible interaction",
    },
    {
        "source": "src/main/java/com/animania/addons/farm/common/block/BlockCheeseMold.java",
        "targets": ["farm/src/main/java/com/animania/farm/FarmCheeseMoldBlock.java", "farm/src/main/java/com/animania/farm/FarmCheeseMoldBlockEntity.java"],
        "selectors": ["animania_farm:cheeseMoldAcceptsModernMilkFluid", "animania_farm:farmFluidsAndCheeseMoldProcess"],
        "note": "milk variant state, shape, processing completion and capability limits",
    },
    {
        "source": "src/main/java/com/animania/addons/farm/common/block/BlockHive.java",
        "targets": ["farm/src/main/java/com/animania/farm/FarmHiveBlock.java", "farm/src/main/java/com/animania/farm/FarmHiveBlockEntity.java"],
        "selectors": ["animania_farm:hiveFluidAndCheeseBlockState"],
        "note": "facing/rotation, honey capability, extraction interaction and state persistence",
    },
    {
        "source": "src/main/java/com/animania/addons/farm/common/block/BlockWildHive.java",
        "targets": ["farm/src/main/java/com/animania/farm/FarmHiveBlock.java", "farm/src/main/java/com/animania/farm/FarmHiveBlockEntity.java"],
        "selectors": ["animania_farm:wildHiveStingUsesLegacyDamageTypeAndAmount"],
        "note": "wild-hive sting damage type and amount",
    },
    {
        "source": "src/main/java/com/animania/addons/farm/common/tileentity/handler/FluidHandlerBeehive.java",
        "targets": ["farm/src/main/java/com/animania/farm/FarmHiveBlockEntity.java", "farm/src/main/java/com/animania/farm/FarmFluids.java"],
        "selectors": ["animania_farm:hiveFluidAndCheeseBlockState"],
        "note": "honey fluid capability accepts the registered source and survives reload",
    },
    {
        "source": "src/main/java/com/animania/addons/farm/common/tileentity/handler/FluidHandlerCheeseMold.java",
        "targets": ["farm/src/main/java/com/animania/farm/FarmCheeseMoldBlockEntity.java", "farm/src/main/java/com/animania/farm/FarmFluids.java"],
        "selectors": ["animania_farm:cheeseMoldAcceptsModernMilkFluid", "animania_farm:farmFluidsAndCheeseMoldProcess"],
        "note": "milk fluid capability accepts only registered milk and enforces capacity",
    },
    {
        "source": "src/main/java/com/animania/addons/farm/common/tileentity/handler/ItemHandlerCheeseMold.java",
        "targets": ["farm/src/main/java/com/animania/farm/FarmCheeseMoldBlockEntity.java"],
        "selectors": ["animania_farm:cheeseMoldAcceptsModernMilkFluid"],
        "note": "sided one-slot item capability rejects invalid automation input",
    },
    {
        "source": "src/main/java/com/animania/addons/farm/common/tileentity/TileEntityCheeseMold.java",
        "targets": ["farm/src/main/java/com/animania/farm/FarmCheeseMoldBlockEntity.java"],
        "selectors": ["animania_farm:cheeseMoldAcceptsModernMilkFluid", "animania_farm:farmFluidsAndCheeseMoldProcess"],
        "note": "processing ticks, visible state, cheese output and NBT reload",
    },
    {
        "source": "src/main/java/com/animania/addons/farm/common/tileentity/TileEntityHive.java",
        "targets": ["farm/src/main/java/com/animania/farm/FarmHiveBlockEntity.java"],
        "selectors": ["animania_farm:hiveFluidAndCheeseBlockState"],
        "note": "honey tank, production timer, bottle extraction and NBT reload",
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
    auditor_path = "tools/audit_farm_facility_behavior.py"
    auditor_hash = sha256(root / auditor_path)
    unique_dir = evidence_dir / "farm-facility-behavior"
    unique_dir.mkdir(parents=True, exist_ok=True)
    results, rows, skipped, errors = [], [], [], []
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
        if missing_targets or missing_markers or missing_runtime:
            skipped.append({"source": source, "missing_targets": missing_targets,
                            "missing_markers": missing_markers, "missing_runtime": missing_runtime})
            continue
        if not re.search(r"All \d+ required tests passed", log_text) or re.search(
                r"required tests failed|Game test server crashed|Exception in server tick loop", log_text):
            skipped.append({"source": source, "reason": "Farm GameTest log is not green"})
            continue
        unique_path = unique_dir / entry["entry_id"] / "evidence.json"
        write_json(unique_path, {
            "entry_id": entry["entry_id"], "source": source, "source_sha256": entry["sha256"],
            "targets": feature["targets"], "selectors": feature["selectors"],
            "test_code": TEST_CODE, "test_code_sha256": sha256(test_file),
            "log": LOG, "log_sha256": sha256(log_file),
        })
        targets = [{"path": path, "sha256": sha256(root / path)} for path in feature["targets"]]
        targets.append({"path": unique_path.relative_to(root).as_posix(), "sha256": sha256(unique_path)})
        tests = [{"selector": selector, "result": "pass", "artifact": LOG,
                  "artifact_sha256": sha256(log_file)} for selector in feature["selectors"]]
        notes = [f"[farm-facility-behavior-v1] {source}: {feature['note']}"]
        for requirement in entry.get("requirements", []):
            results.append({"entry_id": entry["entry_id"], "requirement_id": requirement,
                            "result": "pass", "source_sha256": entry["sha256"],
                            "target_paths": targets, "tests": tests,
                            "evidence_kind": "executed_test", "test_code_path": TEST_CODE,
                            "test_code_sha256": sha256(test_file), "notes": notes})
        rows.append({"source": source, "selectors": feature["selectors"],
                     "requirements": entry.get("requirements", []), "result": "pass"})
    evidence_dir.mkdir(parents=True, exist_ok=True)
    write_json(evidence_dir / "farm-facility-behavior-v1-report.json", {
        "schema_version": 1, "audit": "farm-facility-behavior", "audit_version": "v1",
        "rows": rows, "skipped": skipped, "errors": errors,
        "error_count": len(errors), "all_passed": not errors and not skipped})
    write_json(evidence_dir / "farm-facility-behavior-v1.json", {
        "schema_version": SCHEMA_VERSION, "audit_id": "farm-facility-behavior",
        "audit_version": "v1", "source_revision": matrix.get("source_revision"),
        "command": "tools/audit_farm_facility_behavior.py --root . --matrix docs/migration-matrix.json",
        "auditor_path": auditor_path, "auditor_sha256": auditor_hash,
        "results": results, "errors": errors})
    print(json.dumps({"results": len(results), "rows": len(rows), "skipped": len(skipped), "errors": errors},
                     ensure_ascii=True, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
