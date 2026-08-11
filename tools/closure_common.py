"""Shared fail-closed validation for migration matrix v2 evidence."""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
from typing import Iterable

SCHEMA_VERSION = 2
REQUIREMENTS = {"implementation", "behavior", "serialization", "resource", "client", "integration"}
STATUSES = {"unstarted", "in_progress", "implemented_unverified", "blocked_external", "closed"}
ALLOWED_EVIDENCE = {
    "binary_identity", "normalized_json", "locale_mapping", "source_mapping",
    "executed_test", "client_snapshot", "integration_launch",
}

# The central writer only accepts auditors that are registered here.  Merely
# placing a JSON file under build/audit-evidence must never grant ownership of
# a requirement.  Protocol tests use the real resource auditor identity so
# they exercise the same allow-list as CI.
AUDITOR_REGISTRY = {
    ("tools/audit_resource_migration.py", "strict-resource"),
    ("tools/audit_java_migration.py", "strict-java"),
    ("tools/audit_game_test_evidence.py", "game-test-behavior"),
    ("tools/audit_manual_semantics.py", "manual-semantics"),
    ("tools/audit_api_data.py", "api-data"),
    ("tools/audit_api_legacy_interfaces.py", "api-legacy-interface"),
    ("tools/audit_public_api_facade_implementation.py", "public-api-facade-implementation"),
    ("tools/audit_resource_semantics.py", "resource-semantics"),
    ("tools/audit_breed_behavior.py", "breed-behavior"),
    ("tools/audit_base_block_behavior.py", "base-block-behavior"),
    ("tools/audit_base_registry_behavior.py", "base-registry-behavior"),
    ("tools/audit_farm_facility_behavior.py", "farm-facility-behavior"),
    ("tools/audit_farm_goal_behavior.py", "farm-goal-behavior"),
    ("tools/audit_farm_special_item_behavior.py", "farm-special-item-behavior"),
    ("tools/audit_farm_vehicle_behavior.py", "farm-vehicle-behavior"),
    ("tools/audit_farm_child_growth_behavior.py", "farm-child-growth-behavior"),
    ("tools/audit_farm_fluid_behavior.py", "farm-fluid-behavior"),
    ("tools/audit_extra_hamster_behavior.py", "extra-hamster-behavior"),
    ("tools/audit_extra_spawn_behavior.py", "extra-spawn-behavior"),
    ("tools/audit_catsdogs_profession_behavior.py", "catsdogs-profession-behavior"),
    ("tools/audit_catsdogs_bowl_behavior.py", "catsdogs-bowl-behavior"),
    ("tools/audit_special_breed_colors.py", "special-breed-colors"),
    ("tools/audit_sound_handler_behavior.py", "sound-handler-behavior"),
    ("tools/audit_animation_conversion.py", "animation-conversion"),
    ("tools/audit_config_defaults.py", "config-defaults"),
    ("tools/audit_java_model_implementation.py", "java-model-implementation"),
    ("tools/audit_documented_replacements.py", "documented-replacements"),
    ("tools/audit_generic_ai_behavior.py", "generic-ai-behavior"),
    ("tools/audit_animal_family_implementation.py", "animal-family-implementation"),
    ("tools/audit_consolidated_client_implementation.py", "consolidated-client-implementation"),
    ("tools/audit_legacy_bootstrap_implementation.py", "legacy-bootstrap-implementation"),
    ("tools/audit_legacy_client_compat_implementation.py", "legacy-client-compat-implementation"),
    ("tools/audit_native_content_and_goals_implementation.py", "native-content-and-goals-implementation"),
    ("tools/audit_remaining_explicit_consolidations.py", "remaining-explicit-consolidations"),
}


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def entry_id(entry: dict) -> str:
    payload = "\x1f".join((str(entry.get("kind", "")), str(entry.get("module", "")),
                           str(entry.get("source", "")), str(entry.get("sha256", ""))))
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()[:24]


def read_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8-sig"))


def write_json(path: Path, value: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def validate_matrix_shape(root: Path, matrix: dict, require_reset: bool = False) -> list[str]:
    errors: list[str] = []
    if matrix.get("schema_version") != SCHEMA_VERSION:
        errors.append(f"matrix schema_version must be {SCHEMA_VERSION}")
    entries = matrix.get("entries")
    if not isinstance(entries, list) or len(entries) != 2033:
        errors.append(f"matrix must contain exactly 2033 entries, found {len(entries) if isinstance(entries, list) else 'invalid'}")
        entries = entries if isinstance(entries, list) else []
    seen: set[str] = set()
    for index, entry in enumerate(entries):
        label = f"entry[{index}]"
        identifier = entry.get("entry_id")
        if identifier != entry_id(entry):
            errors.append(f"{label}: entry_id does not match kind/module/source/sha256")
        if identifier in seen:
            errors.append(f"{label}: duplicate entry_id {identifier}")
        seen.add(identifier)
        if entry.get("status") not in STATUSES:
            errors.append(f"{label}: invalid status {entry.get('status')!r}")
        requirements = entry.get("requirements")
        if not isinstance(requirements, list) or not requirements or any(item not in REQUIREMENTS for item in requirements):
            errors.append(f"{label}: invalid requirements")
        baseline = entry.get("baseline")
        if not isinstance(baseline, dict):
            errors.append(f"{label}: missing baseline")
        else:
            required_baseline = {"registry_ids", "classes", "numeric_values", "behaviors", "save_fields", "client_representation"}
            missing = required_baseline - set(baseline)
            if missing:
                errors.append(f"{label}: missing baseline fields {sorted(missing)}")
            if entry.get("kind") == "java" and not baseline.get("behaviors"):
                source = str(entry.get("source", ""))
                client_only = any(token in source.lower() for token in ("/client/", "/models/", "/model/", "/render/", "/renderer"))
                if not client_only and "no_runtime_behavior" not in baseline:
                    errors.append(f"{label}: Java baseline has no behavior claim: {source}")
        evidence = entry.get("target_evidence")
        if not isinstance(evidence, dict) or any(key not in evidence for key in ("paths", "behavior_tests", "serialization_tests", "client_tests", "notes")):
            errors.append(f"{label}: incomplete target_evidence")
        if entry.get("status") == "closed" and not entry.get("closure"):
            errors.append(f"{label}: closed without central closure record")
        if require_reset and (entry.get("status") == "closed" or entry.get("closure")):
            errors.append(f"{label}: reset matrix still contains closure state")
        source = root / "upstream/Animania-1.12" / str(entry.get("source", ""))
        if not source.is_file():
            errors.append(f"{label}: pinned source missing {entry.get('source')}")
        elif entry.get("sha256") != sha256(source):
            errors.append(f"{label}: pinned source hash changed {entry.get('source')}")
    return errors


def iter_evidence(evidence_dir: Path) -> Iterable[tuple[Path, dict]]:
    if not evidence_dir.exists():
        return
    for path in sorted(evidence_dir.glob("*.json")):
        # Reports/summaries are orchestration metadata, not evidence manifests.
        # Only manifests with an audit identity are eligible for closure.
        if path.name.endswith("-report.json") or path.name == "run-summary.json":
            continue
        try:
            yield path, read_json(path)
        except (OSError, json.JSONDecodeError):
            continue


def validate_evidence(root: Path, matrix: dict, evidence_dir: Path) -> tuple[dict[tuple[str, str], list[dict]], list[str]]:
    errors: list[str] = []
    by_id = {entry.get("entry_id"): entry for entry in matrix.get("entries", [])}
    results: dict[tuple[str, str], list[dict]] = {}
    copied_fingerprints: dict[tuple, list[tuple[str, dict]]] = {}
    for path, manifest in iter_evidence(evidence_dir):
        if manifest.get("schema_version") != SCHEMA_VERSION:
            errors.append(f"{path}: evidence schema_version must be {SCHEMA_VERSION}")
            continue
        audit_id = str(manifest.get("audit_id", ""))
        audit_version = str(manifest.get("audit_version", ""))
        if not audit_id or not audit_version.startswith("v"):
            errors.append(f"{path}: missing audit identity/version")
        auditor_path = str(manifest.get("auditor_path", ""))
        auditor_hash = manifest.get("auditor_sha256")
        if not auditor_path or auditor_path.startswith("/") or ".." in Path(auditor_path).parts:
            errors.append(f"{path}: missing or unsafe auditor_path")
        else:
            auditor_file = root / auditor_path
            if not auditor_file.is_file():
                errors.append(f"{path}: auditor source missing {auditor_path}")
            elif auditor_hash != sha256(auditor_file):
                errors.append(f"{path}: auditor source hash changed {auditor_path}")
        if (auditor_path, audit_id) not in AUDITOR_REGISTRY:
            errors.append(f"{path}: unknown or unregistered auditor {audit_id!r} at {auditor_path!r}")
        if manifest.get("source_revision") != matrix.get("source_revision"):
            errors.append(f"{path}: source revision differs from matrix")
        for result in manifest.get("results", []):
            identifier = result.get("entry_id")
            requirement = result.get("requirement_id")
            entry = by_id.get(identifier)
            if entry is None:
                errors.append(f"{path}: unknown entry_id {identifier}")
                continue
            if requirement not in REQUIREMENTS or requirement not in entry.get("requirements", []):
                errors.append(f"{path}: requirement {requirement!r} is not required by {identifier}")
                continue
            if result.get("source_sha256") != entry.get("sha256"):
                errors.append(f"{path}: stale source hash for {identifier}")
                continue
            kind = result.get("evidence_kind")
            if kind not in ALLOWED_EVIDENCE:
                errors.append(f"{path}: unknown evidence kind {kind!r}")
                continue
            target_paths = result.get("target_paths", [])
            if not target_paths:
                errors.append(f"{path}: {identifier}/{requirement} has no target paths")
                continue
            path_error = False
            for target in target_paths:
                target_path = str(target.get("path", ""))
                if not target_path or target_path.startswith("/") or ".." in Path(target_path).parts:
                    errors.append(f"{path}: unsafe target path {target_path!r}")
                    path_error = True
                    continue
                absolute = root / target_path
                if not absolute.is_file():
                    errors.append(f"{path}: target path missing {target_path}")
                    path_error = True
                    continue
                if target.get("sha256") != sha256(absolute):
                    errors.append(f"{path}: target hash changed {target_path}")
                    path_error = True
            tests = result.get("tests", [])
            if result.get("result") != "pass":
                errors.append(f"{path}: non-passing evidence result for {identifier}/{requirement}")
                continue
            test_code_path = str(result.get("test_code_path", ""))
            test_code_hash = result.get("test_code_sha256")
            if not test_code_path or test_code_path.startswith("/") or ".." in Path(test_code_path).parts:
                errors.append(f"{path}: {identifier}/{requirement} missing or unsafe test_code_path")
            else:
                test_code_file = root / test_code_path
                if not test_code_file.is_file():
                    errors.append(f"{path}: test code missing {test_code_path}")
                elif test_code_hash != sha256(test_code_file):
                    errors.append(f"{path}: test code hash changed {test_code_path}")
            if not tests:
                errors.append(f"{path}: {identifier}/{requirement} has no executed test selector")
            for test in tests:
                if test.get("result") != "pass":
                    errors.append(f"{path}: non-passing test for {identifier}/{requirement}")
                artifact = test.get("artifact")
                if artifact:
                    artifact_path = root / artifact
                    if not artifact_path.is_file() or test.get("artifact_sha256") != sha256(artifact_path):
                        errors.append(f"{path}: stale or missing test artifact {artifact}")
                else:
                    errors.append(f"{path}: evidence lacks a hashed test artifact")
            if requirement in {"behavior", "serialization", "client", "integration"}:
                if kind not in {"executed_test", "client_snapshot", "integration_launch"}:
                    errors.append(f"{path}: executable requirement uses non-executable evidence kind {kind}")
            if not path_error:
                enriched = {
                    **result,
                    "audit_id": audit_id,
                    "audit_version": audit_version,
                    "auditor_path": auditor_path,
                    "auditor_sha256": auditor_hash,
                    "manifest": str(path),
                }
                results.setdefault((identifier, requirement), []).append(enriched)
                # A broad module-level proof must never be copied to unrelated
                # source entries.  Identical target/evidence fingerprints are
                # permitted only when an explicit equivalence group carries a
                # source-structure signature for every member.
                selector_fingerprint = tuple(test.get("selector", "") for test in tests) \
                    if kind in {"binary_identity", "normalized_json", "locale_mapping"} else ()
                fingerprint = (
                    audit_id, requirement,
                    tuple(sorted(item.get("path", "") for item in target_paths)),
                    kind, tuple(result.get("notes", [])),
                    test_code_path,
                    selector_fingerprint,
                )
                copied_fingerprints.setdefault(fingerprint, []).append((identifier, result))
    for fingerprint, members in copied_fingerprints.items():
        if len(members) < 2:
            continue
        for identifier, result in members:
            if not result.get("equivalence_group") or not result.get("source_structure_signature"):
                errors.append(
                    f"copied evidence fingerprint for {identifier} has {len(members)} entries; "
                    "declare and prove an equivalence group or provide dedicated evidence"
                )
    for key, records in results.items():
        if len(records) > 1:
            errors.append(
                f"duplicate requirement evidence for {key[0]}/{key[1]}; "
                "one registered auditor must own each requirement"
            )
    return results, errors


def eligible_requirements(matrix: dict, evidence: dict[tuple[str, str], list[dict]]) -> tuple[set[str], dict[str, list[str]]]:
    eligible: set[str] = set()
    missing: dict[str, list[str]] = {}
    for entry in matrix.get("entries", []):
        identifier = entry.get("entry_id")
        missing_requirements = []
        for requirement in entry.get("requirements", []):
            candidates = evidence.get((identifier, requirement), [])
            if not candidates or any(item.get("result") != "pass" for item in candidates):
                missing_requirements.append(requirement)
        if missing_requirements:
            missing[identifier] = missing_requirements
        else:
            eligible.add(identifier)
    return eligible, missing


def evidence_digest(records: list[dict]) -> str:
    payload = json.dumps(records, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()
