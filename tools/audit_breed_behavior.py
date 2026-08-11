"""Bind per-breed Forge GameTest markers to simple legacy Java entries.

The module GameTests iterate the full legacy child registry.  This auditor only
accepts a source entry when every child ID referenced by that old wrapper/type
file has its own runtime marker in a green module log.  Entries without a
complete marker set remain open and are reported as skipped.
"""
from __future__ import annotations

import argparse
import json
import re
import shutil
from pathlib import Path

from closure_common import SCHEMA_VERSION, sha256, write_json


MODULE = {
    "farm": {
        "ids": "farm/src/main/java/com/animania/farm/FarmLegacyIds.java",
        "test": "farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java",
        "log": "farm/run/gameTestServer/logs/latest.log",
        "prefix": "animania_farm",
    },
    "extra": {
        "ids": "extra/src/main/java/com/animania/extra/ExtraLegacyIds.java",
        "test": "extra/src/main/java/com/animania/extra/gametest/AnimaniaExtraGameTests.java",
        "log": "extra/run/gameTestServer/logs/latest.log",
        "prefix": "animania_extra",
    },
    "catsdogs": {
        "ids": "catsdogs/src/main/java/com/animania/catsdogs/CatsDogsLegacyIds.java",
        "test": "catsdogs/src/main/java/com/animania/catsdogs/gametest/AnimaniaCatsDogsGameTests.java",
        "log": "catsdogs/run/gameTestServer/logs/latest.log",
        "prefix": "animania_catsdogs",
    },
}

CHILD = re.compile(r"\bEntity(?:[A-Z]\w*?)?(Chick|Calf|Kid|Lamb|Piglet|Foal|Puppy|Kitten|Peachick|Kit)([A-Z]\w*)\b")
EXCLUDED_SOURCES = {
    "src/main/java/com/animania/addons/extra/common/events/CarryRenderer.java",
    "src/main/java/com/animania/addons/extra/common/entity/rodents/ai/EntityAIFerretFindNests.java",
    "src/main/java/com/animania/addons/extra/common/entity/rodents/ai/EntityAIHedgehogFindNests.java",
    "src/main/java/com/animania/addons/farm/common/entity/goats/GoatFainting.java",
    "src/main/java/com/animania/addons/farm/common/entity/goats/ai/EntityAIButtHeadsGoats.java",
    "src/main/java/com/animania/addons/farm/common/entity/sheep/ai/EntityAIButtHeadsSheep.java",
    "src/main/java/com/animania/addons/catsdogs/common/entity/canids/ai/AnimalAIGetDogHerded.java",
    "src/main/java/com/animania/addons/catsdogs/common/handler/CatsDogsVillagerProfessions.java",
}


def snake(value: str) -> str:
    return re.sub(r"(?<!^)(?=[A-Z])", "_", value).lower()


def child_id(category: str, suffix: str) -> str:
    value = (category + suffix).removesuffix("Horse")
    return snake(value)


def source_child_ids(root: Path, entry: dict, valid_ids: set[str]) -> set[str]:
    source = root / "upstream/Animania-1.12" / entry["source"]
    if not source.is_file():
        return set()
    text = source.read_text(encoding="utf-8", errors="replace")
    return {child_id(match.group(1), match.group(2))
            for match in CHILD.finditer(text)
            if child_id(match.group(1), match.group(2)) in valid_ids}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, required=True)
    parser.add_argument("--evidence-dir", type=Path, default=Path("build/audit-evidence"))
    args = parser.parse_args()
    root = args.root.resolve()
    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    evidence_dir = args.evidence_dir if args.evidence_dir.is_absolute() else root / args.evidence_dir
    matrix = json.loads(matrix_path.read_text(encoding="utf-8"))
    auditor_path = "tools/audit_breed_behavior.py"
    auditor_hash = sha256(root / auditor_path)
    report_rows = []
    results = []
    skipped = []
    errors = []
    unique_dir = evidence_dir / "breed-behavior"
    if unique_dir.exists():
        shutil.rmtree(unique_dir)
    for entry in matrix.get("entries", []):
        module = entry.get("module")
        if entry.get("kind") != "java" or module not in MODULE or "behavior" not in entry.get("requirements", []):
            continue
        if entry.get("source") in EXCLUDED_SOURCES:
            continue
        filename = Path(entry.get("source", "")).name
        # Only wrappers/types that the strict implementation auditor already
        # proves are eligible for this parameterized runtime behavior check.
        if not (filename.endswith("Type.java") or re.match(r"(?:Chicken|Cow|Goat|Horse|Pig|Peafowl|Rabbit|EntityFerret|EntityHedgehog|Dog|Cat)[A-Z].*\.java$", filename)):
            continue
        cfg = MODULE[module]
        ids_file = root / cfg["ids"]
        test_file = root / cfg["test"]
        log_file = root / cfg["log"]
        if not ids_file.is_file() or not test_file.is_file() or not log_file.is_file():
            errors.append(f"missing runtime evidence files for {entry['source']}")
            continue
        valid_ids = set(re.findall(r'"([a-z0-9_]+)"', ids_file.read_text(encoding="utf-8")))
        expected = sorted(source_child_ids(root, entry, valid_ids))
        if not expected:
            skipped.append({"source": entry["source"], "reason": "no child IDs from this source are in the module registry"})
            continue
        source_text = test_file.read_text(encoding="utf-8", errors="replace")
        log_text = log_file.read_text(encoding="utf-8", errors="replace")
        method_token = "everyFarmBreedResolvesItsLegacyChildType" if module == "farm" else (
            "everyExtraBreedResolvesItsLegacyChildType" if module == "extra" else "everyPetBreedResolvesItsLegacyChildType")
        if method_token not in source_text or not re.search(r"All \d+ required tests passed", log_text):
            skipped.append({"source": entry["source"], "reason": "aggregate breed GameTest is not green"})
            continue
        selectors = [f"{cfg['prefix']}:breed_child:{value}" for value in expected]
        missing = [selector for selector in selectors if f"[ANIMANIA_TEST_SELECTOR] {selector}" not in log_text]
        row = {"source": entry["source"], "module": module, "expected_child_ids": expected,
               "selectors": selectors, "result": "pass" if not missing else "skipped",
               "missing_selectors": missing}
        report_rows.append(row)
        if missing:
            skipped.append({"source": entry["source"], "reason": "missing per-child runtime marker", "missing": missing})
            continue
        target_paths = []
        unique_path = unique_dir / f"{entry['entry_id']}.json"
        write_json(unique_path, {
            "entry_id": entry["entry_id"],
            "source": entry["source"],
            "source_sha256": entry["sha256"],
            "module": module,
            "child_ids": expected,
            "selectors": selectors,
            "log": cfg["log"],
            "log_sha256": sha256(log_file),
            "test_code": cfg["test"],
            "test_code_sha256": sha256(test_file),
        })
        target_paths.append({"path": unique_path.relative_to(root).as_posix(), "sha256": sha256(unique_path)})
        for path in (cfg["ids"], cfg["test"]):
            absolute = root / path
            target_paths.append({"path": path, "sha256": sha256(absolute)})
        results.append({
            "entry_id": entry["entry_id"],
            "requirement_id": "behavior",
            "result": "pass",
            "source_sha256": entry["sha256"],
            "target_paths": target_paths,
            "tests": [{"selector": selector, "result": "pass",
                        "artifact": cfg["log"], "artifact_sha256": sha256(log_file)} for selector in selectors],
            "evidence_kind": "executed_test",
            "test_code_path": cfg["test"],
            "test_code_sha256": sha256(test_file),
            "notes": ["Every child ID referenced by this legacy wrapper/type source has an independent runtime marker in the green breed GameTest loop."],
        })

    report_path = evidence_dir / "breed-behavior-v1-report.json"
    report = {"schema_version": 1, "audit": "breed-behavior", "audit_version": "v1",
              "rows": report_rows, "skipped": skipped, "errors": errors,
              "error_count": len(errors), "all_passed": not errors and not skipped}
    evidence_dir.mkdir(parents=True, exist_ok=True)
    write_json(report_path, report)
    manifest = {"schema_version": SCHEMA_VERSION, "audit_id": "breed-behavior", "audit_version": "v1",
                "source_revision": matrix.get("source_revision"),
                "command": "tools/audit_breed_behavior.py --root . --matrix docs/migration-matrix.json",
                "auditor_path": auditor_path, "auditor_sha256": auditor_hash,
                "results": results, "errors": errors}
    write_json(evidence_dir / "breed-behavior-v1.json", manifest)
    print(json.dumps({"results": len(results), "skipped": len(skipped), "errors": errors,
                      "report": str(report_path)}, ensure_ascii=True, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
