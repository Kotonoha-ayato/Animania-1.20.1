"""Verify each historical public API facade has a concrete Java 17 target.

This is deliberately an *implementation* audit.  The 1.12 facades exposed
DataManager plumbing and a few broad helper methods that do not have a
one-to-one modern API equivalent.  A target file and a passing facade-contract
selector prove that the Java 17 facade exists and binds to the stable public
contract; they do not prove every historical runtime behavior.  Therefore this
auditor never emits a behavior result.  The central closer will only fully
close the one marker/aggregate facade whose matrix requirements are solely
implementation.
"""
from __future__ import annotations

import argparse
import json
import xml.etree.ElementTree as ET
from pathlib import Path

from closure_common import SCHEMA_VERSION, sha256, write_json


TEST_CODE = "base/src/test/java/com/animania/api/PublicApiContractTest.java"
REPORT = "base/build/test-results/test/TEST-com.animania.api.PublicApiContractTest.xml"
SPECS = {
    "src/main/java/com/animania/api/interfaces/AnimaniaType.java": "animaniaTypeContractIsStable()",
    "src/main/java/com/animania/api/interfaces/IAgeable.java": "ageableContractIsModernFacade()",
    "src/main/java/com/animania/api/interfaces/IAnimaniaAnimal.java": "animaniaAnimalContractIsModernFacade()",
    "src/main/java/com/animania/api/interfaces/IAnimaniaAnimalBase.java": "animaniaAnimalBaseContractIsModernFacade()",
    "src/main/java/com/animania/api/interfaces/IChild.java": "childContractIsModernFacade()",
    "src/main/java/com/animania/api/interfaces/IFoodEating.java": "foodEatingContractIsModernFacade()",
    "src/main/java/com/animania/api/interfaces/IGendered.java": "genderedContractIsModernFacade()",
    "src/main/java/com/animania/api/interfaces/IImpregnable.java": "impregnableContractIsModernFacade()",
    "src/main/java/com/animania/api/interfaces/IMateable.java": "mateableContractIsModernFacade()",
    "src/main/java/com/animania/api/interfaces/IPlaying.java": "playingContractIsModernFacade()",
    "src/main/java/com/animania/api/interfaces/ISleeping.java": "sleepingContractIsModernFacade()",
    "src/main/java/com/animania/api/interfaces/ISterilizable.java": "sterilizableContractIsModernFacade()",
    "src/main/java/com/animania/api/interfaces/IVariant.java": "variantContractIsModernFacade()",
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
            return not (case.findall("failure") or case.findall("error") or case.findall("skipped"))
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
    test_code, report = root / TEST_CODE, root / REPORT
    auditor_path = "tools/audit_public_api_facade_implementation.py"
    results, gaps, errors = [], [], []
    for source, selector in SPECS.items():
        entry = by_source.get(source)
        target_relative = "base/" + source
        target = root / target_relative
        if entry is None:
            errors.append(f"matrix entry missing: {source}")
            continue
        if not target.is_file() or not test_code.is_file() or not report.is_file():
            errors.append(f"missing target/test/report for {source}")
            continue
        if not green_selector(report, selector):
            errors.append(f"selector did not pass: {selector}")
            continue
        proof = evidence_dir / "public-api-facade-implementation" / entry["entry_id"] / "proof.json"
        source_text = (root / "upstream/Animania-1.12" / source).read_text(encoding="utf-8", errors="replace")
        old_methods = sorted({line.split("(", 1)[0].split()[-1] for line in source_text.splitlines()
                              if "(" in line and ("default " in line or "public " in line)})
        write_json(proof, {
            "entry_id": entry["entry_id"], "source": source, "source_sha256": entry["sha256"],
            "target": target_relative, "target_sha256": sha256(target), "selector": selector,
            "legacy_method_names": old_methods,
            "scope": "implementation only; behavior remains separately required where declared",
        })
        results.append({
            "entry_id": entry["entry_id"], "requirement_id": "implementation", "result": "pass",
            "source_sha256": entry["sha256"],
            "target_paths": [
                {"path": target_relative, "sha256": sha256(target)},
                {"path": proof.relative_to(root).as_posix(), "sha256": sha256(proof)},
            ],
            "tests": [{"selector": f"{REPORT}::{selector}", "result": "pass",
                       "artifact": REPORT, "artifact_sha256": sha256(report)}],
            "evidence_kind": "executed_test", "test_code_path": TEST_CODE,
            "test_code_sha256": sha256(test_code),
            "notes": [
                f"[public-api-facade-implementation-v1] {source} has its own Java 17 target and a passing "
                f"facade selector. Historical behavior is intentionally not inferred from source existence."
            ],
        })
        if "behavior" in entry.get("requirements", []):
            gaps.append({"entry_id": entry["entry_id"], "source": source,
                         "missing": "behavior", "reason": "no one-to-one runtime mapping evidence yet"})
    evidence_dir.mkdir(parents=True, exist_ok=True)
    write_json(evidence_dir / "public-api-facade-implementation-v1.json", {
        "schema_version": SCHEMA_VERSION, "audit_id": "public-api-facade-implementation", "audit_version": "v1",
        "source_revision": matrix.get("source_revision"),
        "command": "tools/audit_public_api_facade_implementation.py --root . --matrix docs/migration-matrix.json",
        "auditor_path": auditor_path, "auditor_sha256": sha256(root / auditor_path),
        "results": results, "errors": errors,
    })
    write_json(evidence_dir / "public-api-facade-implementation-v1-report.json", {
        "schema_version": 1, "audit": "public-api-facade-implementation", "audit_version": "v1",
        "results": len(results), "behavior_gaps": gaps, "errors": errors,
    })
    print(json.dumps({"results": len(results), "behavior_gaps": len(gaps), "errors": errors}, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
