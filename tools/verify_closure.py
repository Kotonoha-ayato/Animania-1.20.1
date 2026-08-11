"""Fail-closed validation of matrix v2 and all referenced evidence."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from closure_common import evidence_digest, read_json, validate_evidence, validate_matrix_shape, write_json


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, default=Path("docs/migration-matrix.json"))
    parser.add_argument("--evidence-dir", type=Path, default=Path("build/audit-evidence"))
    parser.add_argument("--check-only", action="store_true")
    args = parser.parse_args()
    root = args.root.resolve()
    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    evidence_dir = args.evidence_dir if args.evidence_dir.is_absolute() else root / args.evidence_dir
    matrix = read_json(matrix_path)
    errors = validate_matrix_shape(root, matrix)
    evidence, evidence_errors = validate_evidence(root, matrix, evidence_dir)
    errors.extend(evidence_errors)
    for entry in matrix.get("entries", []):
        identifier = entry["entry_id"]
        for requirement in entry.get("requirements", []):
            records = evidence.get((identifier, requirement), [])
            if entry.get("status") == "closed" and not records:
                errors.append(f"{identifier}: closed without evidence for {requirement}")
            if entry.get("status") == "closed" and any(record.get("result") != "pass" for record in records):
                errors.append(f"{identifier}: closed with non-pass {requirement} evidence")
        if entry.get("status") == "closed" and not entry.get("closure"):
            errors.append(f"{identifier}: missing closure record")
        if entry.get("status") == "closed":
            closure = entry.get("closure", {})
            all_records = [record for requirement in entry.get("requirements", [])
                           for record in evidence.get((identifier, requirement), [])]
            if closure.get("source_sha256") != entry.get("sha256"):
                errors.append(f"{identifier}: closure source hash mismatch")
            if closure.get("requirements") != entry.get("requirements"):
                errors.append(f"{identifier}: closure requirement list mismatch")
            if closure.get("evidence_digest") != evidence_digest(all_records):
                errors.append(f"{identifier}: closure evidence digest mismatch")
            if not isinstance(closure.get("auditor_fingerprints"), list) or not closure.get("auditor_fingerprints"):
                errors.append(f"{identifier}: closure missing auditor fingerprints")
            if not isinstance(closure.get("source_code_fingerprints"), list) or not closure.get("source_code_fingerprints"):
                errors.append(f"{identifier}: closure missing source-code fingerprints")
            if not isinstance(closure.get("test_fingerprints"), list) or not closure.get("test_fingerprints"):
                errors.append(f"{identifier}: closure missing test fingerprints")
            if not isinstance(closure.get("evidence_summary"), dict) or not closure.get("closed_at"):
                errors.append(f"{identifier}: closure missing evidence summary or closed_at")
    gates_path = root / "build" / "release-gates.json"
    gates = read_json(gates_path) if gates_path.is_file() else {}
    computed = {
        "schema_version": 2,
        "entries": len(matrix.get("entries", [])),
        "closed": sum(entry.get("status") == "closed" for entry in matrix.get("entries", [])),
        "open": sum(entry.get("status") != "closed" for entry in matrix.get("entries", [])),
        "unverified": sum(not bool(entry.get("verified")) for entry in matrix.get("entries", [])),
        "blocked_external": sum(entry.get("status") == "blocked_external" for entry in matrix.get("entries", [])),
        "global_gates": bool(gates.get("all_passed")),
        "release_allowed": not errors and all(entry.get("status") == "closed" for entry in matrix.get("entries", []))
                          and bool(gates.get("all_passed")),
    }
    if matrix.get("release_audit", {}).get("release_allowed") != computed["release_allowed"]:
        errors.append("release_allowed is not equal to the centrally computed result")
    # Keep the central writer's per-entry/per-requirement audit rows intact.
    # This command is read-only validation; it must not replace that report
    # with a short summary and thereby lose the migration audit trail.
    report_path = root / "build" / "migration-closure-report.json"
    try:
        existing = read_json(report_path) if report_path.is_file() else {}
    except (OSError, json.JSONDecodeError):
        existing = {}
    report = {**existing, **computed, "errors": errors[:100], "error_count": len(errors),
              "validation": {"check_only": bool(args.check_only), "evidence_dir": str(evidence_dir)}}
    write_json(report_path, report)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    if errors or not computed["release_allowed"]:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
