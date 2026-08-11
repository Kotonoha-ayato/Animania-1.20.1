"""Compute the global release gates without ever setting ``release_allowed``.

The migration matrix has per-entry evidence; this file records the separate
whole-build gates required by stage 7.  Missing runtime artifacts are a hard
failure, not an implicit pass.  The only component allowed to combine this
report with the matrix and write ``release_allowed`` is
``apply_verified_closure.py``.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import xml.etree.ElementTree as ET
from pathlib import Path

from closure_common import read_json, sha256, validate_matrix_shape, write_json


MODULES = ("base", "farm", "extra", "catsdogs")
GATE_NAMES = (
    "matrix_schema", "source_pins", "compile_and_unit", "gametests", "data_generation",
    "resources", "manual_semantics", "texture_resolver", "model_structure",
    "model_visual_regression", "client_bootstrap", "api_contract", "config_converter",
    "optional_compat_runtime", "startup_runtime_matrix", "multiplayer_matrix",
    "endurance", "artifacts", "license", "release_version",
)


def passed(name: str, evidence: list[str], details: dict | None = None) -> dict:
    return {"gate": name, "passed": True, "evidence": evidence, "details": details or {}}


def failed(name: str, reason: str, evidence: list[str] | None = None) -> dict:
    return {"gate": name, "passed": False, "evidence": evidence or [], "reason": reason}


def report_ok(path: Path, predicate) -> tuple[bool, str]:
    if not path.is_file():
        return False, f"missing report {path.as_posix()}"
    try:
        data = read_json(path)
    except (OSError, json.JSONDecodeError) as exc:
        return False, f"unreadable report {path.as_posix()}: {exc}"
    try:
        ok = bool(predicate(data))
    except (KeyError, TypeError, ValueError) as exc:
        return False, f"invalid report {path.as_posix()}: {exc}"
    return (True, "") if ok else (False, f"report predicate failed {path.as_posix()}")


def unit_reports(root: Path) -> tuple[bool, list[str], str]:
    reports = []
    total = 0
    bad = []
    for module in (*MODULES, "config-migrator"):
        directory = root / module / "build" / "test-results" / "test"
        files = sorted(directory.glob("TEST-*.xml"))
        if not files:
            bad.append(f"{module}: no JUnit XML")
            continue
        for path in files:
            try:
                suite = ET.parse(path).getroot()
                count = int(suite.attrib.get("tests", "0"))
                total += count
                if int(suite.attrib.get("failures", "0")) or int(suite.attrib.get("errors", "0")) or int(suite.attrib.get("skipped", "0")):
                    bad.append(f"{path.relative_to(root).as_posix()}: non-green tests")
                reports.append(path.relative_to(root).as_posix())
            except (OSError, ET.ParseError, ValueError) as exc:
                bad.append(f"{path.relative_to(root).as_posix()}: {exc}")
    return (not bad and total > 0, reports, "; ".join(bad) if bad else f"{total} JUnit tests")


def gametest_reports(root: Path) -> tuple[bool, list[str], str]:
    logs = {
        "base": root / "base/run/gametestserver/logs/latest.log",
        "farm": root / "farm/run/gameTestServer/logs/latest.log",
        "extra": root / "extra/run/gameTestServer/logs/latest.log",
        "catsdogs": root / "catsdogs/run/gameTestServer/logs/latest.log",
    }
    missing = []
    bad = []
    for module, path in logs.items():
        if not path.is_file():
            missing.append(module)
            continue
        text = path.read_text(encoding="utf-8", errors="replace")
        if not re.search(r"All \d+ required tests passed", text):
            bad.append(f"{module}: no all-required-tests-passed marker")
        if re.search(r"required tests failed|Game test server crashed|Exception in server tick loop", text):
            bad.append(f"{module}: failure/crash marker")
    if missing:
        bad.append("missing GameTest logs: " + ", ".join(missing))
    paths = [p.relative_to(root).as_posix() for p in logs.values() if p.is_file()]
    return (not bad, paths, "; ".join(bad))


def data_reports(root: Path) -> tuple[bool, list[str], str]:
    paths = []
    missing = []
    for module in MODULES:
        generated = root / module / "src/generated/resources"
        if not generated.is_dir() or not any(generated.rglob("*.json")):
            missing.append(module)
        else:
            paths.append(generated.relative_to(root).as_posix())
    return (not missing, paths, "missing generated data: " + ", ".join(missing) if missing else "")


def license_gate(root: Path) -> tuple[bool, list[str], str]:
    license_file = root / "LICENSE"
    if not license_file.is_file() or "GNU LESSER GENERAL PUBLIC LICENSE" not in license_file.read_text(encoding="utf-8", errors="replace"):
        return False, [], "root LGPL-3.0 license file is missing or does not identify LGPL"
    paths = [license_file.relative_to(root).as_posix()]
    for module in MODULES:
        mods = root / module / "src/main/resources/META-INF/mods.toml"
        text = mods.read_text(encoding="utf-8", errors="replace") if mods.is_file() else ""
        if "LGPL-3.0-or-later" not in text:
            return False, paths, f"{module}: mods.toml does not declare LGPL-3.0-or-later"
        paths.append(mods.relative_to(root).as_posix())
    return True, paths, ""


def external_gate(root: Path, name: str, file_name: str) -> dict:
    path = root / "build" / file_name
    ok, reason = report_ok(path, lambda data: data.get("schema_version") == 1 and data.get("all_passed") is True and bool(data.get("evidence")))
    return passed(name, [path.relative_to(root).as_posix()]) if ok else failed(name, reason)


def model_visual_gate(root: Path) -> dict:
    path = root / "build" / "model-visual-regression.json"
    if not path.is_file():
        return failed("model_visual_regression", f"missing report {path}")
    try:
        data = read_json(path)
    except (OSError, json.JSONDecodeError) as exc:
        return failed("model_visual_regression", f"unreadable report {path}: {exc}")
    auditor = root / "tools/audit_model_visual_regression.py"
    if (data.get("schema_version") != 1 or data.get("audit") != "model-visual-regression"
            or data.get("all_passed") is not True or data.get("expected_models") != 130
            or data.get("captured_models") != 130 or not data.get("evidence")
            or data.get("auditor_path") != "tools/audit_model_visual_regression.py"
            or not auditor.is_file() or data.get("auditor_sha256") != sha256(auditor)):
        return failed("model_visual_regression", "visual report is incomplete, untrusted, or does not cover all 130 model entries",
                      [path.relative_to(root).as_posix()])
    return passed("model_visual_regression", [path.relative_to(root).as_posix()])


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--write", action="store_true", help="write build/release-gates.json")
    args = parser.parse_args()
    root = args.root.resolve()
    matrix_path = root / "docs/migration-matrix.json"
    matrix = read_json(matrix_path)
    gates: list[dict] = []
    shape_errors = validate_matrix_shape(root, matrix)
    gates.append(passed("matrix_schema", [matrix_path.relative_to(root).as_posix()])) if not shape_errors else gates.append(failed("matrix_schema", "; ".join(shape_errors[:5])))
    pins = root / "upstream/Animania-1.12"
    gates.append(passed("source_pins", ["upstream/Animania-1.12"])) if pins.is_dir() else gates.append(failed("source_pins", "pinned 1.12 source tree is missing"))

    unit_ok, unit_paths, unit_reason = unit_reports(root)
    gates.append(passed("compile_and_unit", unit_paths, {"summary": unit_reason}) if unit_ok else failed("compile_and_unit", unit_reason, unit_paths))
    game_ok, game_paths, game_reason = gametest_reports(root)
    gates.append(passed("gametests", game_paths) if game_ok else failed("gametests", game_reason, game_paths))
    data_ok, data_paths, data_reason = data_reports(root)
    gates.append(passed("data_generation", data_paths) if data_ok else failed("data_generation", data_reason, data_paths))

    for name, report_name, predicate in (
        ("resources", "build/resource-audit.json", lambda d: not d.get("errors")),
        ("manual_semantics", "build/manual-semantic-audit.json", lambda d: d.get("error_count") == 0 and d.get("pages", 0) >= 140 and d.get("links", 0) > 0),
        ("texture_resolver", "build/texture-resolver-audit.json", lambda d: d.get("error_count") == 0 and d.get("checked", 0) > 0),
        ("model_structure", "docs/model-conversion-audit.json", lambda d: d.get("error_count") == 0 and d.get("total", 0) == 130),
        ("api_contract", "build/api-contract-audit.json", lambda d: d.get("schema_version") == 1 and d.get("all_passed") is True and d.get("error_count") == 0),
        ("config_converter", "build/config-converter-audit.json", lambda d: d.get("schema_version") == 1 and d.get("all_passed") is True and d.get("error_count") == 0),
    ):
        path = root / report_name
        ok, reason = report_ok(path, predicate)
        gates.append(passed(name, [report_name]) if ok else failed(name, reason))

    # A static layer/part inventory is not a visual regression.  Require a
    # separately captured client report with screenshots/geometry assertions;
    # absence is intentionally a hard failure rather than an implicit pass.
    gates.append(model_visual_gate(root))
    gates.append(external_gate(root, "client_bootstrap", "client-smoke-audit.json"))
    gates.append(external_gate(root, "optional_compat_runtime", "compat-runtime-audit.json"))
    gates.append(external_gate(root, "startup_runtime_matrix", "startup-runtime-matrix.json"))
    gates.append(external_gate(root, "multiplayer_matrix", "multiplayer-audit.json"))
    gates.append(external_gate(root, "endurance", "endurance-audit.json"))

    artifact_path = root / "build/release-artifact-audit.json"
    artifact_ok, artifact_reason = report_ok(artifact_path, lambda d: not d.get("missing") and not d.get("content_errors") and len(d.get("artifacts", [])) == 4)
    gates.append(passed("artifacts", ["build/release-artifact-audit.json"]) if artifact_ok else failed("artifacts", artifact_reason))
    license_ok, license_paths, license_reason = license_gate(root)
    gates.append(passed("license", license_paths) if license_ok else failed("license", license_reason, license_paths))
    props = root / "gradle.properties"
    version_ok = props.is_file() and re.search(r"^mod_version=3\.0\.0$", props.read_text(encoding="utf-8", errors="replace"), re.M)
    gates.append(passed("release_version", ["gradle.properties"]) if version_ok else failed("release_version", "mod_version is not 3.0.0"))

    by_name = {gate["gate"]: gate for gate in gates}
    missing_names = sorted(set(GATE_NAMES) - set(by_name))
    all_passed = not missing_names and all(gate.get("passed") is True for gate in gates)
    result = {"schema_version": 1, "gates": gates, "all_passed": all_passed,
              "missing_gates": missing_names, "generated_from": "tools/verify_release_gates.py",
              "evidence": [item for gate in gates for item in gate.get("evidence", [])]}
    output = root / "build/release-gates.json"
    if args.write:
        write_json(output, result)
    print(json.dumps(result, ensure_ascii=False, indent=2))
    if not all_passed:
        raise SystemExit(2)


if __name__ == "__main__":
    main()
