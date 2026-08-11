"""Bind Base block/block-entity contracts to real Forge GameTest selectors.

This audit is deliberately narrow: every row names the old class, the modern
target symbols, and the exact GameTest selectors that exercise the migrated
shape, interaction, capability, or persistence contract.  It never edits the
migration matrix; the central closure writer owns that state transition.
"""
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
        "source": "src/main/java/com/animania/common/blocks/BlockInvisiblock.java",
        "targets": [
            "base/src/main/java/com/animania/common/block/AnimaniaInvisibleBlock.java",
            "base/src/main/java/com/animania/common/AnimaniaBlocks.java",
        ],
        "selectors": ["animania:troughRetainsTwoBlockStructureAndControllerCleanup"],
        "notes": "controller companion cleanup and sided proxy behavior",
    },
    {
        "source": "src/main/java/com/animania/common/tileentities/TileEntityInvisiblock.java",
        "targets": [
            "base/src/main/java/com/animania/common/block/AnimaniaInvisibleBlock.java",
            "base/src/main/java/com/animania/common/AnimaniaBlocks.java",
        ],
        "selectors": ["animania:troughRetainsTwoBlockStructureAndControllerCleanup"],
        "notes": "persisted companion block entity and capability proxy behavior",
    },
    {
        "source": "src/main/java/com/animania/common/blocks/BlockMud.java",
        "targets": [
            "base/src/main/java/com/animania/common/block/AnimaniaMudBlock.java",
        ],
        "selectors": ["animania:mudRetainsLegacyShapeSoundFrictionAndMovementDamping"],
        "notes": "legacy shape, friction, sound, map color and movement damping",
    },
    {
        "source": "src/main/java/com/animania/common/blocks/BlockNest.java",
        "targets": [
            "base/src/main/java/com/animania/common/block/AnimaniaThinBlock.java",
            "base/src/main/java/com/animania/common/AnimaniaBlocks.java",
        ],
        "selectors": ["animania:nestAndFloorPilesRetainLegacyInteractionRules"],
        "notes": "nest interaction and support-dependent thin-block behavior",
    },
    {
        "source": "src/main/java/com/animania/common/tileentities/TileEntityNest.java",
        "targets": [
            "base/src/main/java/com/animania/common/AnimaniaBlocks.java",
            "base/src/main/java/com/animania/common/block/AnimaniaStorageBlockEntity.java",
        ],
        "selectors": ["animania:nestAndFloorPilesRetainLegacyInteractionRules"],
        "notes": "egg filtering, extraction, variant migration and NBT round-trip",
    },
    {
        "source": "src/main/java/com/animania/common/blocks/BlockSaltLick.java",
        "targets": [
            "base/src/main/java/com/animania/common/block/AnimaniaSaltLickBlock.java",
            "base/src/main/java/com/animania/common/block/AnimaniaSaltLickBlockEntity.java",
            "base/src/main/java/com/animania/common/item/AnimaniaSaltLickItem.java",
        ],
        "selectors": ["animania:saltLickCareAndDurability"],
        "notes": "placement, configured durability bar, drops and block interaction",
    },
    {
        "source": "src/main/java/com/animania/common/tileentities/TileEntitySaltLick.java",
        "targets": [
            "base/src/main/java/com/animania/common/block/AnimaniaSaltLickBlockEntity.java",
            "base/src/main/java/com/animania/common/item/AnimaniaSaltLickItem.java",
        ],
        "selectors": ["animania:saltLickCareAndDurability"],
        "notes": "server tick stability, remaining-use state and NBT round-trip",
    },
    {
        "source": "src/main/java/com/animania/common/blocks/BlockSeeds.java",
        "targets": [
            "base/src/main/java/com/animania/common/block/AnimaniaThinBlock.java",
            "base/src/main/java/com/animania/common/AnimaniaBlocks.java",
            "base/src/main/java/com/animania/AnimaniaServerEvents.java",
        ],
        "selectors": [
            "animania:nestAndFloorPilesRetainLegacyInteractionRules",
            "animania:dispenserPlacesConfiguredSeedPileServerSide",
        ],
        "notes": "seed variant identity, support cleanup and server-side dispenser placement",
    },
    {
        "source": "src/main/java/com/animania/common/blocks/BlockStraw.java",
        "targets": [
            "base/src/main/java/com/animania/common/block/AnimaniaThinBlock.java",
            "base/src/main/java/com/animania/common/AnimaniaBlocks.java",
        ],
        "selectors": ["animania:nestAndFloorPilesRetainLegacyInteractionRules"],
        "notes": "non-colliding, flammable and support-dependent straw pile behavior",
    },
    {
        "source": "src/main/java/com/animania/common/blocks/BlockTrough.java",
        "targets": [
            "base/src/main/java/com/animania/common/block/AnimaniaTroughBlock.java",
            "base/src/main/java/com/animania/common/AnimaniaBlocks.java",
        ],
        "selectors": [
            "animania:troughRetainsTwoBlockStructureAndControllerCleanup",
            "animania:troughEnforcesLegacyFoodFluidCapacityAndComparator",
            "animania:troughRainCollectionPreservesLegacyMixingAndIncrementRules",
        ],
        "notes": "two-block placement, cleanup, comparator, automation and rain rules",
    },
    {
        "source": "src/main/java/com/animania/common/tileentities/TileEntityTrough.java",
        "targets": [
            "base/src/main/java/com/animania/common/AnimaniaBlocks.java",
            "base/src/main/java/com/animania/common/block/AnimaniaStorageBlockEntity.java",
        ],
        "selectors": [
            "animania:storageCapabilitiesPersist",
            "animania:troughEnforcesLegacyFoodFluidCapacityAndComparator",
            "animania:troughRainCollectionPreservesLegacyMixingAndIncrementRules",
        ],
        "notes": "item/fluid capabilities, capacity, mixing, comparator and NBT round-trip",
    },
]


def find_entry(matrix: dict, source: str) -> dict | None:
    return next((entry for entry in matrix.get("entries", []) if entry.get("source") == source), None)


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
    test_file = root / TEST_CODE
    log_file = root / LOG
    test_text = test_file.read_text(encoding="utf-8", errors="replace") if test_file.is_file() else ""
    log_text = log_file.read_text(encoding="utf-8", errors="replace") if log_file.is_file() else ""
    auditor_path = "tools/audit_base_block_behavior.py"
    auditor_hash = sha256(root / auditor_path)
    results = []
    rows = []
    errors = []
    skipped = []
    unique_dir = evidence_dir / "base-block-behavior"
    unique_dir.mkdir(parents=True, exist_ok=True)

    for feature in FEATURES:
        source = feature["source"]
        entry = find_entry(matrix, source)
        if entry is None:
            errors.append(f"matrix entry missing: {source}")
            continue
        old_source = root / "upstream/Animania-1.12" / source
        if not old_source.is_file():
            errors.append(f"pinned source missing: {source}")
            continue
        if not test_file.is_file() or not log_file.is_file():
            errors.append("Base GameTest source or runtime log is missing")
            continue
        missing_markers = [selector for selector in feature["selectors"]
                           if f'AnimaniaGameTestEvidence.mark("{selector}")' not in test_text]
        missing_runtime = [selector for selector in feature["selectors"]
                           if f"[ANIMANIA_TEST_SELECTOR] {selector}" not in log_text]
        missing_targets = [path for path in feature["targets"] if not (root / path).is_file()]
        if missing_markers or missing_runtime or missing_targets:
            skipped.append({"source": source, "missing_markers": missing_markers,
                            "missing_runtime": missing_runtime, "missing_targets": missing_targets})
            continue
        if not re.search(r"All \d+ required tests passed", log_text):
            skipped.append({"source": source, "reason": "Base GameTest aggregate is not green"})
            continue
        if re.search(r"required tests failed|Game test server crashed|Exception in server tick loop", log_text):
            skipped.append({"source": source, "reason": "Base GameTest log contains a failure"})
            continue

        entry_dir = unique_dir / entry["entry_id"]
        unique_path = entry_dir / "evidence.json"
        unique = {
            "entry_id": entry["entry_id"],
            "source": source,
            "source_sha256": entry["sha256"],
            "targets": feature["targets"],
            "selectors": feature["selectors"],
            "test_code": TEST_CODE,
            "test_code_sha256": sha256(test_file),
            "log": LOG,
            "log_sha256": sha256(log_file),
        }
        write_json(unique_path, unique)
        target_paths = [{"path": path, "sha256": sha256(root / path)} for path in feature["targets"]]
        target_paths.append({"path": unique_path.relative_to(root).as_posix(), "sha256": sha256(unique_path)})
        tests = [{"selector": selector, "result": "pass", "artifact": LOG,
                  "artifact_sha256": sha256(log_file)} for selector in feature["selectors"]]
        notes = [f"[base-block-behavior-v1] {source}: {feature['notes']}"]
        for requirement in entry.get("requirements", []):
            results.append({
                "entry_id": entry["entry_id"],
                "requirement_id": requirement,
                "result": "pass",
                "source_sha256": entry["sha256"],
                "target_paths": target_paths,
                "tests": tests,
                "evidence_kind": "executed_test",
                "test_code_path": TEST_CODE,
                "test_code_sha256": sha256(test_file),
                "notes": notes,
            })
        rows.append({"source": source, "requirements": entry.get("requirements", []),
                     "selectors": feature["selectors"], "result": "pass"})

    evidence_dir.mkdir(parents=True, exist_ok=True)
    write_json(evidence_dir / "base-block-behavior-v1-report.json", {
        "schema_version": 1, "audit": "base-block-behavior", "audit_version": "v1",
        "rows": rows, "skipped": skipped, "errors": errors,
        "error_count": len(errors), "all_passed": not errors and not skipped,
    })
    write_json(evidence_dir / "base-block-behavior-v1.json", {
        "schema_version": SCHEMA_VERSION,
        "audit_id": "base-block-behavior",
        "audit_version": "v1",
        "source_revision": matrix.get("source_revision"),
        "command": "tools/audit_base_block_behavior.py --root . --matrix docs/migration-matrix.json",
        "auditor_path": auditor_path,
        "auditor_sha256": auditor_hash,
        "results": results,
        "errors": errors,
    })
    print(json.dumps({"results": len(results), "rows": len(rows), "skipped": len(skipped),
                      "errors": errors}, ensure_ascii=True, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
