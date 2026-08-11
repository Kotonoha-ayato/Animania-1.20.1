"""Emit selector-bound evidence for the small public API data contracts.

Only the three data types with dedicated behavior tests are covered here.
The broader legacy interface set remains open until each old override and
state transition has its own executable proof.
"""
from __future__ import annotations

import argparse
import json
import xml.etree.ElementTree as ET
from pathlib import Path

from closure_common import SCHEMA_VERSION, sha256, write_json


SPECS = {
    "src/main/java/com/animania/api/data/AnimalContainer.java": {
        "target": "base/src/main/java/com/animania/api/data/AnimalContainer.java",
        "test_code": "base/src/test/java/com/animania/api/AnimalContainerTest.java",
        "report": "base/build/test-results/test/TEST-com.animania.api.AnimalContainerTest.xml",
        "selector": "preservesLegacyIdentityEqualityHashAndStringContract()",
        "requirements": ("implementation", "behavior"),
    },
    "src/main/java/com/animania/api/data/EntityGender.java": {
        "target": "base/src/main/java/com/animania/api/data/EntityGender.java",
        "test_code": "base/src/test/java/com/animania/common/helper/LegacyUtilityTest.java",
        "report": "base/build/test-results/test/TEST-com.animania.common.helper.LegacyUtilityTest.xml",
        "selector": "legacyGenderResolutionPreservesEveryBranchIncludingRandom()",
        "requirements": ("implementation", "behavior"),
    },
    "src/main/java/com/animania/api/data/Pose.java": {
        "target": "base/src/main/java/com/animania/api/data/Pose.java",
        "test_code": "base/src/test/java/com/animania/common/helper/LegacyUtilityTest.java",
        "report": "base/build/test-results/test/TEST-com.animania.common.helper.LegacyUtilityTest.xml",
        "selector": "poseNamesRemainStable()",
        "requirements": ("implementation",),
    },
}


def green_selector(path: Path, selector: str) -> bool:
    if not path.is_file():
        return False
    try:
        root = ET.parse(path).getroot()
    except (OSError, ET.ParseError):
        return False
    for case in root.findall(".//testcase"):
        if case.attrib.get("name") == selector:
            return not case.findall("failure") and not case.findall("error") and not case.findall("skipped")
    return False


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
    by_source = {entry.get("source"): entry for entry in matrix.get("entries", [])}
    auditor_path = "tools/audit_api_data.py"
    auditor_hash = sha256(root / auditor_path)
    results = []
    errors = []
    for source, spec in SPECS.items():
        entry = by_source.get(source)
        target = root / spec["target"]
        test_code = root / spec["test_code"]
        report = root / spec["report"]
        if not entry:
            errors.append(f"matrix entry missing: {source}")
            continue
        if not target.is_file() or not test_code.is_file() or not report.is_file():
            errors.append(f"missing target/test/report for {source}")
            continue
        if not green_selector(report, spec["selector"]):
            errors.append(f"selector did not pass: {spec['selector']}")
            continue
        for requirement in spec["requirements"]:
            if requirement not in entry.get("requirements", []):
                errors.append(f"{source}: requirement not declared: {requirement}")
                continue
            results.append({
                "entry_id": entry["entry_id"],
                "requirement_id": requirement,
                "result": "pass",
                "source_sha256": entry["sha256"],
                "target_paths": [{"path": spec["target"], "sha256": sha256(target)}],
                "tests": [{"selector": f"{spec['report']}::{spec['selector']}", "result": "pass",
                            "artifact": spec["report"], "artifact_sha256": sha256(report)}],
                "evidence_kind": "executed_test",
                "test_code_path": spec["test_code"],
                "test_code_sha256": sha256(test_code),
                "notes": [f"Public API data contract is bound to the dedicated selector {spec['selector']} and its current target source hash."],
            })
    manifest = {
        "schema_version": SCHEMA_VERSION,
        "audit_id": "api-data",
        "audit_version": "v1",
        "source_revision": matrix.get("source_revision"),
        "command": "tools/audit_api_data.py --root . --matrix docs/migration-matrix.json",
        "auditor_path": auditor_path,
        "auditor_sha256": auditor_hash,
        "results": results,
        "errors": errors,
    }
    evidence_dir.mkdir(parents=True, exist_ok=True)
    write_json(evidence_dir / "api-data-v1.json", manifest)
    print(json.dumps({"results": len(results), "errors": errors}, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
