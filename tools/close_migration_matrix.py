"""Audit explicit per-entry migration evidence; never auto-close source entries."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from closure_common import SCHEMA_VERSION, validate_evidence, validate_matrix_shape, write_json


REQUIRED_BASELINE = {
    "registry_ids", "classes", "numeric_values", "behaviors", "save_fields", "client_representation"
}
REQUIRED_TARGET = {
    "paths", "behavior_tests", "serialization_tests", "client_tests", "notes"
}


def audit(root: Path, matrix: dict) -> list[str]:
    errors: list[str] = []
    entries = matrix.get("entries", [])
    for index, entry in enumerate(entries):
        label = f"{entry.get('module')}:{entry.get('source')}"
        baseline = entry.get("baseline")
        target = entry.get("target_evidence")
        if not isinstance(baseline, dict) or not REQUIRED_BASELINE.issubset(baseline):
            errors.append(f"{label}: incomplete baseline fields")
        if not isinstance(target, dict) or not REQUIRED_TARGET.issubset(target):
            errors.append(f"{label}: incomplete target evidence fields")
        if entry.get("status") != "closed":
            continue
        if not entry.get("implemented") or not entry.get("verified"):
            errors.append(f"{label}: closed without implemented+verified")
        paths = target.get("paths", []) if isinstance(target, dict) else []
        tests = []
        if isinstance(target, dict):
            tests = target.get("behavior_tests", []) + target.get("serialization_tests", []) + target.get("client_tests", [])
        if not paths or any(not (root / path).exists() for path in paths):
            errors.append(f"{label}: closed without existing target paths")
        if not tests or any(not (root / test).exists() for test in tests):
            errors.append(f"{label}: closed without existing dedicated tests")
        if entry.get("kind") == "java" and not target.get("behavior_tests", []):
            errors.append(f"{label}: Java entry lacks behavior test")
        if (baseline.get("save_fields") if isinstance(baseline, dict) else []) and not target.get("serialization_tests", []):
            errors.append(f"{label}: persisted fields lack serialization test")
        if (baseline.get("client_representation") if isinstance(baseline, dict) else []) and not target.get("client_tests", []):
            errors.append(f"{label}: client content lacks visual/client test")

    open_count = sum(entry.get("status") != "closed" for entry in entries)
    unverified = sum(not bool(entry.get("verified")) for entry in entries)
    matrix["release_audit"] = {
        "unstarted": sum(entry.get("status") == "unstarted" for entry in entries),
        "open": open_count,
        "unverified": unverified,
        "closed": len(entries) - open_count,
        # This compatibility auditor is not the central closure writer.  It
        # may report that its local checks are green, but it can never grant
        # the release bit.
        "release_allowed": False,
    }
    return errors


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, default=None)
    parser.add_argument("--check-only", action="store_true")
    args = parser.parse_args()
    matrix_path = args.matrix or args.root / "docs/migration-matrix.json"
    matrix = json.loads(matrix_path.read_text(encoding="utf-8"))
    if matrix.get("schema_version") == SCHEMA_VERSION:
        # Schema v2 is deliberately write-only through apply_verified_closure.py.
        # Keeping this compatibility command read-only prevents the historical
        # weak auditor from changing a release matrix by accident.
        if not args.check_only:
            print(json.dumps({
                "schema_version": SCHEMA_VERSION,
                "error": "schema v2 is centrally closed; run run_closure_audits.py then apply_verified_closure.py",
                "release_allowed": False,
            }, ensure_ascii=False, indent=2))
            raise SystemExit(2)
        errors = validate_matrix_shape(args.root, matrix)
        _, evidence_errors = validate_evidence(args.root, matrix, args.root / "build/audit-evidence")
        errors.extend(evidence_errors)
        closed = sum(entry.get("status") == "closed" for entry in matrix.get("entries", []))
        report = {
            "schema_version": SCHEMA_VERSION,
            "entries": len(matrix.get("entries", [])),
            "closed": closed,
            "open": len(matrix.get("entries", [])) - closed,
            "errors": errors[:100],
            "error_count": len(errors),
            "release_allowed": False,
            "read_only": True,
        }
        write_json(args.root / "build" / "migration-closure-report.json", report)
        print(json.dumps(report, ensure_ascii=False, indent=2))
        raise SystemExit(1 if errors or report["open"] else 0)
    errors = audit(args.root, matrix)
    if not args.check_only:
        matrix_path.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    report = {"entries": len(matrix.get("entries", [])), "errors": errors[:100],
              "error_count": len(errors), "release_audit": matrix["release_audit"], "matrix": str(matrix_path)}
    print(json.dumps(report, ensure_ascii=False, indent=2))
    if errors or not matrix["release_audit"]["release_allowed"]:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
