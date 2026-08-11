"""Apply only current, per-requirement evidence to the migration matrix."""
from __future__ import annotations

import argparse
from collections import Counter
import json
from datetime import datetime, timezone
from pathlib import Path

from closure_common import SCHEMA_VERSION, evidence_digest, read_json, validate_evidence, validate_matrix_shape, write_json


def closure_state(requirements: list[str], missing: list[str]) -> tuple[str, str]:
    """Classify an open row without claiming an unproven code defect.

    ``closed_verified`` is the only release-eligible state.  A row may be
    implementation-complete but still await a real client launch; that is not
    a repair request and must never be shown alongside missing implementations.
    """
    if not missing:
        return "closed_verified", "all declared requirements have current passing evidence"
    absent = set(missing)
    if absent == {"client"}:
        return "client_verification_pending", "implementation and non-client behavior are proven; needs real client/atlas verification"
    if absent <= {"client", "integration"} and "client" in absent:
        return "client_verification_pending", "implementation is proven; needs real client and optional-compat runtime verification"
    if "implementation" not in absent:
        return "nonclient_verification_pending", "implementation is proven; needs behavior, serialization, or integration evidence"
    return "repair_required", "no implementation proof remains; requires a concrete source-specific migration repair before verification"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, default=Path("docs/migration-matrix.json"))
    parser.add_argument("--evidence-dir", type=Path, default=Path("build/audit-evidence"))
    args = parser.parse_args()
    root = args.root.resolve()
    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    evidence_dir = args.evidence_dir if args.evidence_dir.is_absolute() else root / args.evidence_dir
    matrix = read_json(matrix_path)
    errors = validate_matrix_shape(root, matrix)
    evidence, evidence_errors = validate_evidence(root, matrix, evidence_dir)
    errors.extend(evidence_errors)
    if errors:
        report = {"schema_version": SCHEMA_VERSION, "errors": errors[:100], "error_count": len(errors),
                  "release_allowed": False}
        write_json(root / "build" / "migration-closure-report.json", report)
        print(json.dumps(report, ensure_ascii=False, indent=2))
        raise SystemExit(1)

    closed = 0
    open_entries = 0
    blocked = 0
    audit_rows = []
    state_counts: Counter[str] = Counter()
    for entry in matrix.get("entries", []):
        identifier = entry["entry_id"]
        records = [record for requirement in entry.get("requirements", [])
                   for record in evidence.get((identifier, requirement), [])]
        missing = [requirement for requirement in entry.get("requirements", [])
                   if not evidence.get((identifier, requirement))]
        if missing:
            entry["status"] = "unstarted" if not records else "implemented_unverified"
            entry["implemented"] = bool(records)
            entry["verified"] = False
            entry["closure"] = None
            open_entries += 1
            state, action = closure_state(entry.get("requirements", []), missing)
            state_counts[state] += 1
            audit_rows.append({
                "entry_id": identifier,
                "source": entry.get("source"),
                "module": entry.get("module"),
                "status": entry.get("status"),
                "closure_state": state,
                "next_action": action,
                "requirements": [{"requirement_id": requirement, "status": "open",
                                  "reason": "no current pass evidence"}
                                 for requirement in entry.get("requirements", [])
                                 if requirement in missing],
            })
            continue
        entry["status"] = "closed"
        entry["implemented"] = True
        entry["verified"] = True
        tests = []
        paths = []
        notes = []
        for record in records:
            paths.extend(item["path"] for item in record.get("target_paths", []))
            notes.extend(record.get("notes", []))
            tests.extend(test["selector"] for test in record.get("tests", []))
        entry["tests"] = list(dict.fromkeys(tests))
        entry["target_evidence"] = {
            "paths": list(dict.fromkeys(paths)),
            "behavior_tests": [record["tests"][0]["selector"] for record in records
                                if record.get("requirement_id") == "behavior" and record.get("tests")],
            "serialization_tests": [record["tests"][0]["selector"] for record in records
                                     if record.get("requirement_id") == "serialization" and record.get("tests")],
            "client_tests": [record["tests"][0]["selector"] for record in records
                              if record.get("requirement_id") == "client" and record.get("tests")],
            "notes": list(dict.fromkeys(notes)),
        }
        auditor_fingerprints = sorted({
            f"{record['audit_id']}@{record['audit_version']}:{record['auditor_path']}:{record['auditor_sha256']}"
            for record in records
        })
        source_code_fingerprints = sorted({
            f"{item['path']}:{item['sha256']}"
            for record in records for item in record.get('target_paths', [])
        })
        test_fingerprints = sorted({
            f"{record.get('test_code_path')}:{record.get('test_code_sha256')}::{test.get('selector')}:{test.get('artifact_sha256')}"
            for record in records for test in record.get('tests', [])
        })
        entry["closure"] = {
            "audit_version": "matrix-v2",
            "source_sha256": entry["sha256"],
            "evidence_digest": evidence_digest(records),
            "requirements": list(entry.get("requirements", [])),
            "evidence_refs": sorted({record["manifest"] for record in records}),
            "auditor_fingerprints": auditor_fingerprints,
            "source_code_fingerprints": source_code_fingerprints,
            "test_fingerprints": test_fingerprints,
            "evidence_summary": {
                "requirements": {requirement: sum(1 for record in records if record.get("requirement_id") == requirement)
                                  for requirement in entry.get("requirements", [])},
                "pass_results": len(records),
            },
            "closed_at": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
        }
        closed += 1
        state_counts["closed_verified"] += 1
        audit_rows.append({
            "entry_id": identifier,
            "source": entry.get("source"),
            "module": entry.get("module"),
            "status": "closed",
            "closure_state": "closed_verified",
            "next_action": "none",
            "requirements": [{
                "requirement_id": requirement,
                "status": "pass",
                "auditors": sorted({record["audit_id"] for record in records
                                     if record.get("requirement_id") == requirement}),
                "tests": [test.get("selector") for record in records
                          if record.get("requirement_id") == requirement
                          for test in record.get("tests", [])],
            } for requirement in entry.get("requirements", [])],
        })

    global_gates = False
    gates_path = root / "build" / "release-gates.json"
    if gates_path.is_file():
        try:
            global_gates = bool(read_json(gates_path).get("all_passed"))
        except (OSError, json.JSONDecodeError):
            global_gates = False
    release_allowed = closed == len(matrix.get("entries", [])) and open_entries == 0 and blocked == 0 and global_gates
    matrix["release_audit"] = {
        "schema_version": SCHEMA_VERSION,
        "unstarted": sum(entry.get("status") == "unstarted" for entry in matrix.get("entries", [])),
        "open": sum(entry.get("status") != "closed" for entry in matrix.get("entries", [])),
        "unverified": sum(not bool(entry.get("verified")) for entry in matrix.get("entries", [])),
        "closed": closed,
        "blocked_external": blocked,
        "global_gates": global_gates,
        "release_allowed": release_allowed,
        "closure_states": dict(sorted(state_counts.items())),
    }
    write_json(matrix_path, matrix)
    report = {"schema_version": SCHEMA_VERSION, "entries": len(matrix.get("entries", [])),
              "closed": closed, "open": open_entries, "blocked_external": blocked,
              "release_allowed": release_allowed,
              "global_gates": global_gates,
              "closure_states": dict(sorted(state_counts.items())),
              "closure_state_definitions": {
                  "closed_verified": "all declared requirements are evidenced and centrally closed",
                  "client_verification_pending": "no code repair is inferred; real client/atlas or optional-compat launch evidence is missing",
                  "nonclient_verification_pending": "implementation is evidenced; behavior, serialization, or integration execution still needs proof",
                  "repair_required": "no implementation proof exists; a concrete source-specific migration repair is required before verification",
              },
              "error_count": 0,
              "entry_audits": audit_rows,
              "report": str(root / "build" / "migration-closure-report.json")}
    write_json(root / "build" / "migration-closure-report.json", report)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    if not release_allowed:
        raise SystemExit(2)


if __name__ == "__main__":
    main()
