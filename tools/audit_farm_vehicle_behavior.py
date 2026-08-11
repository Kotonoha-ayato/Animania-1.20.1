"""Bind Farm cart/tiller/puller migrations to exact server GameTest selectors."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from closure_common import SCHEMA_VERSION, read_json, sha256, write_json


TEST_CODE = "farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java"
LOG = "farm/run/gameTestServer/logs/latest.log"
FEATURES = [
    {
        "source": "src/main/java/com/animania/addons/farm/common/entity/pullables/AnimatedEntityBase.java",
        "targets": ["base/src/main/java/com/animania/common/entity/AnimaniaVehicleEntity.java"],
        "selectors": ["animania_farm:pullableVehicleHasInventoryAndPassengerPath"],
        "note": "native vehicle base keeps passenger, hitch and server movement semantics",
    },
    {
        "source": "src/main/java/com/animania/addons/farm/common/entity/pullables/EntityCart.java",
        "targets": ["base/src/main/java/com/animania/common/entity/AnimaniaVehicleEntity.java"],
        "selectors": ["animania_farm:pullableVehicleHasInventoryAndPassengerPath", "animania_farm:vehicleDropsHonorModernDoEntityDropsRule"],
        "note": "cart cargo/menu/hitch state round-trips through NBT and respects the entity-drops gamerule",
    },
    {
        "source": "src/main/java/com/animania/addons/farm/common/entity/pullables/EntityTiller.java",
        "targets": ["base/src/main/java/com/animania/common/entity/AnimaniaVehicleEntity.java"],
        "selectors": ["animania_farm:pullableVehicleHasInventoryAndPassengerPath", "animania_farm:pulledTillerCultivatesThreeRowsAndConsumesSeed"],
        "note": "pulled tiller persists cargo/hitch state and cultivates three rows while consuming exactly three seeds",
    },
    {
        "source": "src/main/java/com/animania/addons/farm/common/inventory/CartChest.java",
        "targets": ["base/src/main/java/com/animania/common/entity/AnimaniaVehicleEntity.java"],
        "selectors": ["animania_farm:pullableVehicleHasInventoryAndPassengerPath"],
        "note": "cart chest exposes the legacy 27-slot cargo inventory through the native three-row menu",
    },
    {
        "source": "src/main/java/com/animania/addons/farm/common/item/ItemCart.java",
        "targets": ["base/src/main/java/com/animania/common/item/AnimaniaVehicleItem.java", "farm/src/main/java/com/animania/farm/FarmContent.java"],
        "selectors": ["animania_farm:vehicleItemsSpawnNamedEntitiesAtAirAndBlockTargets"],
        "note": "cart item spawns exactly one server entity and preserves a custom display name",
    },
    {
        "source": "src/main/java/com/animania/addons/farm/common/item/ItemTiller.java",
        "targets": ["base/src/main/java/com/animania/common/item/AnimaniaVehicleItem.java", "farm/src/main/java/com/animania/farm/FarmContent.java"],
        "selectors": ["animania_farm:vehicleItemsSpawnNamedEntitiesAtAirAndBlockTargets", "animania_farm:pulledTillerCultivatesThreeRowsAndConsumesSeed"],
        "note": "tiller item places above the clicked face and its pulled entity performs the three-row operation",
    },
    {
        "source": "src/main/java/com/animania/addons/farm/common/item/ItemWagon.java",
        "targets": ["base/src/main/java/com/animania/common/item/AnimaniaVehicleItem.java", "farm/src/main/java/com/animania/farm/FarmContent.java"],
        "selectors": ["animania_farm:vehicleItemsSpawnNamedEntitiesAtAirAndBlockTargets"],
        "note": "wagon item spawns the registered wagon entity server-side and preserves its custom name",
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
    auditor_path = "tools/audit_farm_vehicle_behavior.py"
    auditor_hash = sha256(root / auditor_path)
    unique_dir = evidence_dir / "farm-vehicle-behavior"
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
        notes = [f"[farm-vehicle-behavior-v1] {source}: {feature['note']}."]
        for requirement in entry.get("requirements", []):
            results.append({"entry_id": entry["entry_id"], "requirement_id": requirement,
                            "result": "pass", "source_sha256": entry["sha256"],
                            "target_paths": targets, "tests": tests,
                            "evidence_kind": "executed_test", "test_code_path": TEST_CODE,
                            "test_code_sha256": sha256(test_file), "notes": notes})
        rows.append({"source": source, "selectors": feature["selectors"],
                     "requirements": entry.get("requirements", []), "result": "pass"})
    write_json(evidence_dir / "farm-vehicle-behavior-v1-report.json", {
        "schema_version": 1, "audit": "farm-vehicle-behavior", "audit_version": "v1",
        "rows": rows, "skipped": skipped, "errors": errors,
        "error_count": len(errors), "all_passed": not errors and not skipped})
    write_json(evidence_dir / "farm-vehicle-behavior-v1.json", {
        "schema_version": SCHEMA_VERSION, "audit_id": "farm-vehicle-behavior",
        "audit_version": "v1", "source_revision": matrix.get("source_revision"),
        "command": "tools/audit_farm_vehicle_behavior.py --root . --matrix docs/migration-matrix.json",
        "auditor_path": auditor_path, "auditor_sha256": auditor_hash, "results": results, "errors": errors})
    print(json.dumps({"results": len(results), "rows": len(rows), "skipped": len(skipped), "errors": errors},
                     ensure_ascii=True, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
