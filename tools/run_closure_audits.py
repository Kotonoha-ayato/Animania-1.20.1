"""Run strict, read-only closure auditors and emit per-requirement evidence.

This command deliberately works on a temporary matrix copy.  Existing legacy
auditors may still have a ``--write`` option for historical reports, but they
cannot mutate the release matrix through this orchestrator.
"""
from __future__ import annotations

import argparse
import json
import shutil
import subprocess
import sys
from pathlib import Path

from closure_common import SCHEMA_VERSION, read_json, sha256, validate_matrix_shape, write_json


def run(command: list[str], root: Path) -> tuple[int, str]:
    # Python on Windows may select the active code page for child stdout when
    # the repository path contains non-ASCII characters.  Evidence files are
    # read from disk, so replacement decoding is safer than aborting the
    # entire audit orchestration on a cosmetic path encoding mismatch.
    completed = subprocess.run(command, cwd=root, text=True, encoding="utf-8", errors="ignore",
                               stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    return completed.returncode, completed.stdout


def proof_kind(notes: list[str]) -> str:
    text = " ".join(notes)
    if "intentional-modern-removal" in text or "normalized-json-equivalence" in text:
        return "normalized_json"
    if "sha256-identity" in text:
        return "binary_identity"
    if "active-locale-key-mapping" in text:
        return "locale_mapping"
    return "source_mapping"


def target_paths(root: Path, paths: list[str]) -> list[dict]:
    values = []
    for path in paths:
        absolute = root / path
        if absolute.is_file():
            values.append({"path": path.replace("\\", "/"), "sha256": sha256(absolute)})
    return values


def emit_resource_evidence(root: Path, matrix: dict, temporary: Path, evidence_dir: Path,
                           report_text: str) -> int:
    temp_matrix = read_json(temporary)
    auditor_path = "tools/audit_resource_migration.py"
    auditor_hash = sha256(root / auditor_path)
    report_path = evidence_dir / "strict-resource-v1-report.json"
    report_path.write_text(report_text, encoding="utf-8")
    report_artifact = "build/audit-evidence/strict-resource-v1-report.json"
    report_hash = sha256(report_path)
    results = []
    for entry in temp_matrix.get("entries", []):
        if entry.get("kind") != "resource" or entry.get("status") != "closed":
            continue
        evidence = entry.get("target_evidence", {})
        paths = target_paths(root, evidence.get("paths", []))
        if not paths:
            continue
        results.append({
            "entry_id": entry["entry_id"],
            "requirement_id": "resource",
            "result": "pass",
            "source_sha256": entry["sha256"],
            "target_paths": paths,
            "tests": [{
                "selector": f"audit_resource_migration::{entry['source']}",
                "result": "pass",
                "artifact": report_artifact,
                "artifact_sha256": report_hash,
            }],
            "evidence_kind": proof_kind(evidence.get("notes", [])),
            "test_code_path": auditor_path,
            "test_code_sha256": auditor_hash,
            "notes": evidence.get("notes", []),
        })
    manifest = {
        "schema_version": SCHEMA_VERSION,
        "audit_id": "strict-resource",
        "audit_version": "v1",
        "source_revision": matrix["source_revision"],
        "command": "tools/audit_resource_migration.py --write <temporary-matrix>",
        "auditor_path": auditor_path,
        "auditor_sha256": auditor_hash,
        "results": results,
    }
    write_json(evidence_dir / "strict-resource-v1.json", manifest)
    return len(results)


def emit_java_evidence(root: Path, matrix: dict, temporary: Path, evidence_dir: Path,
                       report_text: str) -> int:
    temp_matrix = read_json(temporary)
    auditor_path = "tools/audit_java_migration.py"
    auditor_hash = sha256(root / auditor_path)
    report_path = evidence_dir / "strict-java-v1-report.json"
    report_path.write_text(report_text, encoding="utf-8")
    report_artifact = "build/audit-evidence/strict-java-v1-report.json"
    report_hash = sha256(report_path)
    results = []
    current_by_source = {entry.get("source"): entry for entry in matrix.get("entries", [])}
    for entry in temp_matrix.get("entries", []):
        if entry.get("kind") != "java" or entry.get("status") != "closed":
            continue
        source = str(entry.get("source", "")).replace("\\", "/")
        current = current_by_source.get(entry.get("source"), {})
        # Consolidated animal classes have a stronger, per-ID Forge server
        # audit.  Do not let the historical broad Java mapper own their
        # implementation requirement as well; the central writer rejects
        # competing owners by design.  Already closed rows retain their
        # existing evidence until the reset/re-audit creates a replacement.
        if (current.get("status") != "closed" and entry.get("module") in {"farm", "extra", "catsdogs"}
                and "/entity/" in source and "/ai/" not in source
                and Path(source).stem != "EntityWagon"):
            continue
        evidence = entry.get("target_evidence", {})
        paths = target_paths(root, evidence.get("paths", []))
        if not paths:
            continue
        # This auditor proves source-to-target implementation mapping only.
        # Behavior/serialization/client requirements stay open until an actual
        # test artifact or snapshot is supplied by a later audit.
        results.append({
            "entry_id": entry["entry_id"],
            "requirement_id": "implementation",
            "result": "pass",
            "source_sha256": entry["sha256"],
            "target_paths": paths,
            "tests": [{
                "selector": f"audit_java_migration::{entry['source']}",
                "result": "pass",
                "artifact": report_artifact,
                "artifact_sha256": report_hash,
            }],
            "evidence_kind": "source_mapping",
            "test_code_path": auditor_path,
            "test_code_sha256": auditor_hash,
            "notes": evidence.get("notes", []),
        })
    manifest = {
        "schema_version": SCHEMA_VERSION,
        "audit_id": "strict-java",
        "audit_version": "v1",
        "source_revision": matrix["source_revision"],
        "command": "tools/audit_java_migration.py --write <temporary-matrix>",
        "auditor_path": auditor_path,
        "auditor_sha256": auditor_hash,
        "results": results,
    }
    write_json(evidence_dir / "strict-java-v1.json", manifest)
    (evidence_dir / "strict-java-v1-report.json").write_text(report_text, encoding="utf-8")
    return len(results)


def emit_manual_semantics_evidence(root: Path, matrix: dict, evidence_dir: Path) -> int:
    """Bind the two manual pages whose links were intentionally modernized.

    The generic normalized-resource auditor correctly leaves these pages open
    because their target JSON differs from the 1.12 serialization.  The
    semantic auditor proves the actual page graph instead, one source page at
    a time, without granting client visual evidence.
    """
    report = root / "build/manual-semantic-audit.json"
    if not report.is_file():
        return 0
    data = read_json(report)
    if data.get("schema_version") != 1 or data.get("error_count") != 0:
        return 0
    targets = {
        "src/main/resources/assets/animania/manual/blocks/salt_lick.json":
            "base/src/main/resources/assets/animania/manual/blocks/salt_lick.json",
        "src/main/resources/assets/farm/animania/manual/farm/items/equipment.json":
            "farm/src/main/resources/assets/animania_farm/manual/farm/items/equipment.json",
    }
    auditor_path = "tools/audit_manual_semantics.py"
    auditor_hash = sha256(root / auditor_path)
    report_artifact = "build/manual-semantic-audit.json"
    report_hash = sha256(report)
    results = []
    by_source = {entry.get("source"): entry for entry in matrix.get("entries", [])}
    for source, target in targets.items():
        entry = by_source.get(source)
        absolute = root / target
        if not entry or not absolute.is_file():
            continue
        results.append({
            "entry_id": entry["entry_id"],
            "requirement_id": "resource",
            "result": "pass",
            "source_sha256": entry["sha256"],
            "target_paths": [{"path": target, "sha256": sha256(absolute)}],
            "tests": [{"selector": f"audit_manual_semantics::{source}", "result": "pass",
                        "artifact": report_artifact, "artifact_sha256": report_hash}],
            "evidence_kind": "normalized_json",
            "test_code_path": auditor_path,
            "test_code_sha256": auditor_hash,
            "notes": ["manual semantic link graph passes with the explicit modern target mapping; source page was audited independently."],
        })
    manifest = {
        "schema_version": SCHEMA_VERSION,
        "audit_id": "manual-semantics",
        "audit_version": "v1",
        "source_revision": matrix["source_revision"],
        "command": "tools/audit_manual_semantics.py --root .",
        "auditor_path": auditor_path,
        "auditor_sha256": auditor_hash,
        "results": results,
    }
    write_json(evidence_dir / "manual-semantics-v1.json", manifest)
    return len(results)


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
    if errors:
        print(json.dumps({"errors": errors[:100], "error_count": len(errors)}, ensure_ascii=False, indent=2))
        raise SystemExit(1)
    if evidence_dir.exists():
        for path in evidence_dir.glob("*.json"):
            path.unlink()
    evidence_dir.mkdir(parents=True, exist_ok=True)
    work = root / "build" / "audit-work"
    if work.exists():
        shutil.rmtree(work)
    work.mkdir(parents=True, exist_ok=True)
    temporary = work / "matrix.json"
    reset = json.loads(json.dumps(matrix))
    for entry in reset["entries"]:
        entry["status"] = "unstarted"
        entry["implemented"] = False
        entry["verified"] = False
        entry["tests"] = []
        entry["target_evidence"] = {"paths": [], "behavior_tests": [], "serialization_tests": [], "client_tests": [], "notes": []}
        entry["closure"] = None
    reset["release_audit"] = {"schema_version": SCHEMA_VERSION, "unstarted": len(reset["entries"]),
                              "open": len(reset["entries"]), "unverified": len(reset["entries"]),
                              "closed": 0, "release_allowed": False}
    write_json(temporary, reset)

    resource_code, resource_report = run([sys.executable, "tools/audit_resource_migration.py",
                                          "--root", str(root), "--matrix", str(temporary), "--write"], root)
    java_code, java_report = run([sys.executable, "tools/audit_java_migration.py",
                                  "--root", str(root), "--matrix", str(temporary), "--write"], root)
    resource_count = emit_resource_evidence(root, matrix, temporary, evidence_dir, resource_report)
    java_count = emit_java_evidence(root, matrix, temporary, evidence_dir, java_report)
    manual_count = emit_manual_semantics_evidence(root, matrix, evidence_dir)
    resource_semantic_code, resource_semantic_report = run([sys.executable, "tools/audit_resource_semantics.py",
                                                             "--root", str(root), "--matrix", str(matrix_path),
                                                             "--evidence-dir", str(evidence_dir)], root)
    try:
        resource_semantic_summary = json.loads(resource_semantic_report)
    except json.JSONDecodeError:
        resource_semantic_summary = {"output": resource_semantic_report[-4000:]}
    breed_code, breed_report = run([sys.executable, "tools/audit_breed_behavior.py",
                                    "--root", str(root), "--matrix", str(matrix_path),
                                    "--evidence-dir", str(evidence_dir)], root)
    try:
        breed_summary = json.loads(breed_report)
    except json.JSONDecodeError:
        breed_summary = {"output": breed_report[-4000:]}
    base_block_code, base_block_report = run([sys.executable, "tools/audit_base_block_behavior.py",
                                              "--root", str(root), "--matrix", str(matrix_path),
                                              "--evidence-dir", str(evidence_dir)], root)
    try:
        base_block_summary = json.loads(base_block_report)
    except json.JSONDecodeError:
        base_block_summary = {"output": base_block_report[-4000:]}
    base_registry_code, base_registry_report = run([sys.executable, "tools/audit_base_registry_behavior.py",
                                                    "--root", str(root), "--matrix", str(matrix_path),
                                                    "--evidence-dir", str(evidence_dir)], root)
    try:
        base_registry_summary = json.loads(base_registry_report)
    except json.JSONDecodeError:
        base_registry_summary = {"output": base_registry_report[-4000:]}
    farm_facility_code, farm_facility_report = run([sys.executable, "tools/audit_farm_facility_behavior.py",
                                                    "--root", str(root), "--matrix", str(matrix_path),
                                                    "--evidence-dir", str(evidence_dir)], root)
    try:
        farm_facility_summary = json.loads(farm_facility_report)
    except json.JSONDecodeError:
        farm_facility_summary = {"output": farm_facility_report[-4000:]}
    farm_goal_code, farm_goal_report = run([sys.executable, "tools/audit_farm_goal_behavior.py",
                                            "--root", str(root), "--matrix", str(matrix_path),
                                            "--evidence-dir", str(evidence_dir)], root)
    try:
        farm_goal_summary = json.loads(farm_goal_report)
    except json.JSONDecodeError:
        farm_goal_summary = {"output": farm_goal_report[-4000:]}
    farm_special_item_code, farm_special_item_report = run([sys.executable, "tools/audit_farm_special_item_behavior.py",
                                                             "--root", str(root), "--matrix", str(matrix_path),
                                                             "--evidence-dir", str(evidence_dir)], root)
    try:
        farm_special_item_summary = json.loads(farm_special_item_report)
    except json.JSONDecodeError:
        farm_special_item_summary = {"output": farm_special_item_report[-4000:]}
    farm_vehicle_code, farm_vehicle_report = run([sys.executable, "tools/audit_farm_vehicle_behavior.py",
                                                   "--root", str(root), "--matrix", str(matrix_path),
                                                   "--evidence-dir", str(evidence_dir)], root)
    try:
        farm_vehicle_summary = json.loads(farm_vehicle_report)
    except json.JSONDecodeError:
        farm_vehicle_summary = {"output": farm_vehicle_report[-4000:]}
    farm_child_growth_code, farm_child_growth_report = run([sys.executable, "tools/audit_farm_child_growth_behavior.py",
                                                             "--root", str(root), "--matrix", str(matrix_path),
                                                             "--evidence-dir", str(evidence_dir)], root)
    try:
        farm_child_growth_summary = json.loads(farm_child_growth_report)
    except json.JSONDecodeError:
        farm_child_growth_summary = {"output": farm_child_growth_report[-4000:]}
    farm_fluid_code, farm_fluid_report = run([sys.executable, "tools/audit_farm_fluid_behavior.py",
                                               "--root", str(root), "--matrix", str(matrix_path),
                                               "--evidence-dir", str(evidence_dir)], root)
    try:
        farm_fluid_summary = json.loads(farm_fluid_report)
    except json.JSONDecodeError:
        farm_fluid_summary = {"output": farm_fluid_report[-4000:]}
    extra_hamster_code, extra_hamster_report = run([sys.executable, "tools/audit_extra_hamster_behavior.py",
                                                     "--root", str(root), "--matrix", str(matrix_path),
                                                     "--evidence-dir", str(evidence_dir)], root)
    try:
        extra_hamster_summary = json.loads(extra_hamster_report)
    except json.JSONDecodeError:
        extra_hamster_summary = {"output": extra_hamster_report[-4000:]}
    extra_spawn_code, extra_spawn_report = run([sys.executable, "tools/audit_extra_spawn_behavior.py",
                                                 "--root", str(root), "--matrix", str(matrix_path),
                                                 "--evidence-dir", str(evidence_dir)], root)
    try:
        extra_spawn_summary = json.loads(extra_spawn_report)
    except json.JSONDecodeError:
        extra_spawn_summary = {"output": extra_spawn_report[-4000:]}
    catsdogs_profession_code, catsdogs_profession_report = run([sys.executable, "tools/audit_catsdogs_profession_behavior.py",
                                                                 "--root", str(root), "--matrix", str(matrix_path),
                                                                 "--evidence-dir", str(evidence_dir)], root)
    try:
        catsdogs_profession_summary = json.loads(catsdogs_profession_report)
    except json.JSONDecodeError:
        catsdogs_profession_summary = {"output": catsdogs_profession_report[-4000:]}
    catsdogs_bowl_code, catsdogs_bowl_report = run([sys.executable, "tools/audit_catsdogs_bowl_behavior.py",
                                                    "--root", str(root), "--matrix", str(matrix_path),
                                                    "--evidence-dir", str(evidence_dir)], root)
    try:
        catsdogs_bowl_summary = json.loads(catsdogs_bowl_report)
    except json.JSONDecodeError:
        catsdogs_bowl_summary = {"output": catsdogs_bowl_report[-4000:]}
    special_colors_code, special_colors_report = run([sys.executable, "tools/audit_special_breed_colors.py",
                                                       "--root", str(root), "--matrix", str(matrix_path),
                                                       "--evidence-dir", str(evidence_dir)], root)
    try:
        special_colors_summary = json.loads(special_colors_report)
    except json.JSONDecodeError:
        special_colors_summary = {"output": special_colors_report[-4000:]}
    sound_handler_code, sound_handler_report = run([sys.executable, "tools/audit_sound_handler_behavior.py",
                                                     "--root", str(root), "--matrix", str(matrix_path),
                                                     "--evidence-dir", str(evidence_dir)], root)
    try:
        sound_handler_summary = json.loads(sound_handler_report)
    except json.JSONDecodeError:
        sound_handler_summary = {"output": sound_handler_report[-4000:]}
    animation_code, animation_report = run([sys.executable, "tools/audit_animation_conversion.py",
                                             "--root", str(root), "--matrix", str(matrix_path),
                                             "--evidence-dir", str(evidence_dir)], root)
    try:
        animation_summary = json.loads(animation_report)
    except json.JSONDecodeError:
        animation_summary = {"output": animation_report[-4000:]}
    config_defaults_code, config_defaults_report = run([sys.executable, "tools/audit_config_defaults.py",
                                                        "--root", str(root), "--matrix", str(matrix_path),
                                                        "--evidence-dir", str(evidence_dir)], root)
    try:
        config_defaults_summary = json.loads(config_defaults_report)
    except json.JSONDecodeError:
        config_defaults_summary = {"output": config_defaults_report[-4000:]}
    java_model_code, java_model_report = run([sys.executable, "tools/audit_java_model_implementation.py",
                                               "--root", str(root), "--matrix", str(matrix_path),
                                               "--evidence-dir", str(evidence_dir)], root)
    try:
        java_model_summary = json.loads(java_model_report)
    except json.JSONDecodeError:
        java_model_summary = {"output": java_model_report[-4000:]}
    api_code, api_report = run([sys.executable, "tools/audit_api_data.py",
                                "--root", str(root), "--matrix", str(matrix_path),
                                "--evidence-dir", str(evidence_dir)], root)
    try:
        api_summary = json.loads(api_report)
    except json.JSONDecodeError:
        api_summary = {"output": api_report[-4000:]}
    legacy_api_code, legacy_api_report = run([sys.executable, "tools/audit_api_legacy_interfaces.py",
                                              "--root", str(root), "--matrix", str(matrix_path),
                                              "--evidence-dir", str(evidence_dir)], root)
    try:
        legacy_api_summary = json.loads(legacy_api_report)
    except json.JSONDecodeError:
        legacy_api_summary = {"output": legacy_api_report[-4000:]}
    public_api_facade_code, public_api_facade_report = run([sys.executable, "tools/audit_public_api_facade_implementation.py",
                                                            "--root", str(root), "--matrix", str(matrix_path),
                                                            "--evidence-dir", str(evidence_dir)], root)
    try:
        public_api_facade_summary = json.loads(public_api_facade_report)
    except json.JSONDecodeError:
        public_api_facade_summary = {"output": public_api_facade_report[-4000:]}
    documented_replacements_code, documented_replacements_report = run([sys.executable, "tools/audit_documented_replacements.py",
                                                                         "--root", str(root), "--matrix", str(matrix_path),
                                                                         "--evidence-dir", str(evidence_dir)], root)
    try:
        documented_replacements_summary = json.loads(documented_replacements_report)
    except json.JSONDecodeError:
        documented_replacements_summary = {"output": documented_replacements_report[-4000:]}
    generic_ai_code, generic_ai_report = run([sys.executable, "tools/audit_generic_ai_behavior.py",
                                              "--root", str(root), "--matrix", str(matrix_path),
                                              "--evidence-dir", str(evidence_dir)], root)
    try:
        generic_ai_summary = json.loads(generic_ai_report)
    except json.JSONDecodeError:
        generic_ai_summary = {"output": generic_ai_report[-4000:]}
    animal_family_code, animal_family_report = run([sys.executable, "tools/audit_animal_family_implementation.py",
                                                    "--root", str(root), "--matrix", str(matrix_path),
                                                    "--evidence-dir", str(evidence_dir)], root)
    try:
        animal_family_summary = json.loads(animal_family_report)
    except json.JSONDecodeError:
        animal_family_summary = {"output": animal_family_report[-4000:]}
    consolidated_client_code, consolidated_client_report = run([sys.executable, "tools/audit_consolidated_client_implementation.py",
                                                                 "--root", str(root), "--matrix", str(matrix_path),
                                                                 "--evidence-dir", str(evidence_dir)], root)
    try:
        consolidated_client_summary = json.loads(consolidated_client_report)
    except json.JSONDecodeError:
        consolidated_client_summary = {"output": consolidated_client_report[-4000:]}
    legacy_bootstrap_code, legacy_bootstrap_report = run([sys.executable, "tools/audit_legacy_bootstrap_implementation.py",
                                                          "--root", str(root), "--matrix", str(matrix_path),
                                                          "--evidence-dir", str(evidence_dir)], root)
    try:
        legacy_bootstrap_summary = json.loads(legacy_bootstrap_report)
    except json.JSONDecodeError:
        legacy_bootstrap_summary = {"output": legacy_bootstrap_report[-4000:]}
    legacy_client_compat_code, legacy_client_compat_report = run([sys.executable, "tools/audit_legacy_client_compat_implementation.py",
                                                                   "--root", str(root), "--matrix", str(matrix_path),
                                                                   "--evidence-dir", str(evidence_dir)], root)
    try:
        legacy_client_compat_summary = json.loads(legacy_client_compat_report)
    except json.JSONDecodeError:
        legacy_client_compat_summary = {"output": legacy_client_compat_report[-4000:]}
    native_content_goals_code, native_content_goals_report = run([sys.executable, "tools/audit_native_content_and_goals_implementation.py",
                                                                  "--root", str(root), "--matrix", str(matrix_path),
                                                                  "--evidence-dir", str(evidence_dir)], root)
    try:
        native_content_goals_summary = json.loads(native_content_goals_report)
    except json.JSONDecodeError:
        native_content_goals_summary = {"output": native_content_goals_report[-4000:]}
    remaining_consolidations_code, remaining_consolidations_report = run([sys.executable, "tools/audit_remaining_explicit_consolidations.py",
                                                                          "--root", str(root), "--matrix", str(matrix_path),
                                                                          "--evidence-dir", str(evidence_dir)], root)
    try:
        remaining_consolidations_summary = json.loads(remaining_consolidations_report)
    except json.JSONDecodeError:
        remaining_consolidations_summary = {"output": remaining_consolidations_report[-4000:]}
    # Behavior evidence is generated only from a real Forge GameTest server
    # log.  The auditor emits one selector-bound result per confirmed gap and
    # never edits the matrix.
    feature_code, feature_report = run([sys.executable, "tools/audit_game_test_evidence.py",
                                        "--root", str(root), "--matrix", str(matrix_path),
                                        "--evidence-dir", str(evidence_dir)], root)
    try:
        feature_summary = json.loads(feature_report)
    except json.JSONDecodeError:
        feature_summary = {"output": feature_report[-4000:]}
    report = {"schema_version": SCHEMA_VERSION, "resource_exit": resource_code,
              "java_exit": java_code, "resource_results": resource_count,
              "java_implementation_results": java_count,
              "manual_resource_results": manual_count,
              "resource_semantics_exit": resource_semantic_code,
              "resource_semantics": resource_semantic_summary,
              "breed_behavior_exit": breed_code,
              "breed_behavior": breed_summary,
              "base_block_behavior_exit": base_block_code,
              "base_block_behavior": base_block_summary,
              "base_registry_behavior_exit": base_registry_code,
              "base_registry_behavior": base_registry_summary,
              "farm_facility_behavior_exit": farm_facility_code,
              "farm_facility_behavior": farm_facility_summary,
              "farm_goal_behavior_exit": farm_goal_code,
              "farm_goal_behavior": farm_goal_summary,
              "farm_special_item_behavior_exit": farm_special_item_code,
              "farm_special_item_behavior": farm_special_item_summary,
              "farm_vehicle_behavior_exit": farm_vehicle_code,
              "farm_vehicle_behavior": farm_vehicle_summary,
              "farm_child_growth_behavior_exit": farm_child_growth_code,
              "farm_child_growth_behavior": farm_child_growth_summary,
              "farm_fluid_behavior_exit": farm_fluid_code,
              "farm_fluid_behavior": farm_fluid_summary,
              "extra_hamster_behavior_exit": extra_hamster_code,
              "extra_hamster_behavior": extra_hamster_summary,
              "extra_spawn_behavior_exit": extra_spawn_code,
              "extra_spawn_behavior": extra_spawn_summary,
              "catsdogs_profession_behavior_exit": catsdogs_profession_code,
              "catsdogs_profession_behavior": catsdogs_profession_summary,
              "catsdogs_bowl_behavior_exit": catsdogs_bowl_code,
              "catsdogs_bowl_behavior": catsdogs_bowl_summary,
              "special_breed_colors_exit": special_colors_code,
              "special_breed_colors": special_colors_summary,
              "sound_handler_behavior_exit": sound_handler_code,
              "sound_handler_behavior": sound_handler_summary,
              "animation_conversion_exit": animation_code,
              "animation_conversion": animation_summary,
              "config_defaults_exit": config_defaults_code,
              "config_defaults": config_defaults_summary,
              "java_model_implementation_exit": java_model_code,
              "java_model_implementation": java_model_summary,
              "api_data_exit": api_code,
              "api_data": api_summary,
              "api_legacy_interface_exit": legacy_api_code,
              "api_legacy_interface": legacy_api_summary,
              "public_api_facade_implementation_exit": public_api_facade_code,
              "public_api_facade_implementation": public_api_facade_summary,
              "documented_replacements_exit": documented_replacements_code,
              "documented_replacements": documented_replacements_summary,
              "generic_ai_behavior_exit": generic_ai_code,
              "generic_ai_behavior": generic_ai_summary,
              "animal_family_implementation_exit": animal_family_code,
              "animal_family_implementation": animal_family_summary,
              "consolidated_client_implementation_exit": consolidated_client_code,
              "consolidated_client_implementation": consolidated_client_summary,
              "legacy_bootstrap_implementation_exit": legacy_bootstrap_code,
              "legacy_bootstrap_implementation": legacy_bootstrap_summary,
              "legacy_client_compat_implementation_exit": legacy_client_compat_code,
              "legacy_client_compat_implementation": legacy_client_compat_summary,
              "native_content_and_goals_implementation_exit": native_content_goals_code,
              "native_content_and_goals_implementation": native_content_goals_summary,
              "remaining_explicit_consolidations_exit": remaining_consolidations_code,
              "remaining_explicit_consolidations": remaining_consolidations_summary,
              "behavior_exit": feature_code, "behavior": feature_summary,
              "evidence_dir": str(evidence_dir)}
    write_json(evidence_dir / "run-summary.json", report)
    print(json.dumps(report, ensure_ascii=True, indent=2))
    if (resource_code != 0 or java_code != 0 or resource_semantic_code != 0
            or breed_code != 0 or base_block_code != 0 or base_registry_code != 0
            or farm_facility_code != 0 or farm_goal_code != 0 or farm_special_item_code != 0
            or farm_vehicle_code != 0 or farm_child_growth_code != 0 or farm_fluid_code != 0
            or extra_hamster_code != 0 or extra_spawn_code != 0
            or catsdogs_profession_code != 0 or catsdogs_bowl_code != 0
            or special_colors_code != 0 or sound_handler_code != 0 or animation_code != 0
            or config_defaults_code != 0 or java_model_code != 0
            or feature_code != 0 or api_code != 0
            or legacy_api_code != 0 or public_api_facade_code != 0
            or documented_replacements_code != 0 or generic_ai_code != 0
            or animal_family_code != 0 or consolidated_client_code != 0
            or legacy_bootstrap_code != 0 or legacy_client_compat_code != 0
            or native_content_goals_code != 0 or remaining_consolidations_code != 0):
        raise SystemExit(1)


if __name__ == "__main__":
    main()
