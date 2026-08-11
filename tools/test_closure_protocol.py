"""Regression tests for the fail-closed migration closure protocol.

These tests deliberately exercise the central validator rather than the legacy
auditors.  In particular, a copied module-level proof and a changed target
must remain open even when the files/classes happen to exist.
"""
from __future__ import annotations

import copy
import json
import tempfile
import unittest
from pathlib import Path

from closure_common import read_json, sha256, validate_evidence, validate_matrix_shape, write_json


ROOT = Path(__file__).resolve().parents[1]
MATRIX_PATH = ROOT / "docs" / "migration-matrix.json"


class ClosureProtocolTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.matrix = read_json(MATRIX_PATH)
        cls.resources = [entry for entry in cls.matrix["entries"] if entry["kind"] == "resource"]
        cls.auditor = "tools/audit_resource_migration.py"
        cls.target = "tools/audit_resource_migration.py"

    def _manifest(self, entries: list[dict], *, target_hash: str | None = None) -> dict:
        auditor_hash = sha256(ROOT / self.auditor)
        results = []
        for entry in entries:
            digest = target_hash or auditor_hash
            results.append({
                "entry_id": entry["entry_id"],
                "requirement_id": "resource",
                "result": "pass",
                "source_sha256": entry["sha256"],
                "target_paths": [{"path": self.target, "sha256": digest}],
                "tests": [{
                    "selector": f"resource::{entry['source']}",
                    "result": "pass",
                    "artifact": self.target,
                    "artifact_sha256": auditor_hash,
                }],
                "evidence_kind": "source_mapping",
                "test_code_path": self.auditor,
                "test_code_sha256": auditor_hash,
                "notes": ["same generic proof"],
            })
        return {
            "schema_version": 2,
            "audit_id": "strict-resource",
            "audit_version": "v1",
            "source_revision": self.matrix["source_revision"],
            "auditor_path": self.auditor,
            "auditor_sha256": auditor_hash,
            "results": results,
        }

    def test_reset_matrix_is_open(self) -> None:
        errors = validate_matrix_shape(ROOT, self.matrix)
        self.assertEqual([], errors)
        self.assertTrue(all(entry["status"] != "closed" or entry.get("closure") for entry in self.matrix["entries"]))

    def test_copied_generic_evidence_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            evidence_dir = Path(directory)
            write_json(evidence_dir / "copied.json", self._manifest(self.resources[:2]))
            _, errors = validate_evidence(ROOT, self.matrix, evidence_dir)
        self.assertTrue(any("copied evidence fingerprint" in error for error in errors), errors)

    def test_changed_target_hash_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            evidence_dir = Path(directory)
            write_json(evidence_dir / "stale.json", self._manifest(self.resources[:1], target_hash="0" * 64))
            _, errors = validate_evidence(ROOT, self.matrix, evidence_dir)
        self.assertTrue(any("target hash changed" in error for error in errors), errors)

    def test_reset_mode_rejects_any_closed_claim(self) -> None:
        matrix = copy.deepcopy(self.matrix)
        matrix["entries"][0]["status"] = "closed"
        matrix["entries"][0]["closure"] = {"audit_version": "test"}
        errors = validate_matrix_shape(ROOT, matrix, require_reset=True)
        self.assertTrue(any("reset matrix still contains closure state" in error for error in errors), errors)

    def test_unknown_auditor_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            evidence_dir = Path(directory)
            manifest = self._manifest(self.resources[:1])
            manifest["audit_id"] = "hand-edited"
            write_json(evidence_dir / "unknown.json", manifest)
            _, errors = validate_evidence(ROOT, self.matrix, evidence_dir)
        self.assertTrue(any("unknown or unregistered auditor" in error for error in errors), errors)

    def test_non_passing_result_cannot_supply_closure(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            evidence_dir = Path(directory)
            manifest = self._manifest(self.resources[:1])
            manifest["results"][0]["result"] = "skipped"
            write_json(evidence_dir / "skipped.json", manifest)
            _, errors = validate_evidence(ROOT, self.matrix, evidence_dir)
        self.assertTrue(any("non-passing evidence result" in error for error in errors), errors)

    def test_two_registered_auditors_cannot_own_one_requirement(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            evidence_dir = Path(directory)
            first = self._manifest(self.resources[:1])
            second = copy.deepcopy(first)
            second_path = "tools/audit_manual_semantics.py"
            second["audit_id"] = "manual-semantics"
            second["auditor_path"] = second_path
            second["auditor_sha256"] = sha256(ROOT / second_path)
            second["results"][0]["test_code_path"] = second_path
            second["results"][0]["test_code_sha256"] = sha256(ROOT / second_path)
            write_json(evidence_dir / "first.json", first)
            write_json(evidence_dir / "second.json", second)
            _, errors = validate_evidence(ROOT, self.matrix, evidence_dir)
        self.assertTrue(any("duplicate requirement evidence" in error for error in errors), errors)


if __name__ == "__main__":
    unittest.main(verbosity=2)
