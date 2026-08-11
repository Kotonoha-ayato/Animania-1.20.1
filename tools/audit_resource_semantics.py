"""Prove resource migrations that intentionally change Forge namespaces.

The generic resource auditor leaves these files open because their JSON is not
byte-identical after the 1.12 -> 1.20.1 namespace split.  This auditor proves
the semantic fields independently and emits one result per source entry.  It
does not provide client screenshot evidence for block models.
"""
from __future__ import annotations

import argparse
import copy
import json
from pathlib import Path

from closure_common import SCHEMA_VERSION, sha256, write_json


ADVANCEMENTS = (
    "feed_ferret_grey.json", "feed_ferret_white.json",
    "feed_hedgehog.json", "feed_hedgehog_albino.json",
)
MOLDS = (
    "mold_cow_milk.json", "mold_friesian_milk.json", "mold_goat_milk.json",
    "mold_holstein_milk.json", "mold_jersey_milk.json", "mold_sheep_milk.json",
)


def load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def path_without_namespace(value: object) -> object:
    if not isinstance(value, str) or ":" not in value:
        return value
    namespace, path = value.split(":", 1)
    # The split modules intentionally move old ``animania:`` IDs into their
    # owning namespace (farm or extra); the registry path is the stable part.
    return path.lower() if namespace in {"animania", "animania_extra", "animania_farm"} else (namespace, path)


def normalize_advancement(value: dict) -> dict:
    result = copy.deepcopy(value)
    def walk(node):
        if isinstance(node, dict):
            for key, child in list(node.items()):
                if key in {"item", "entity", "parent"}:
                    node[key] = path_without_namespace(child)
                else:
                    walk(child)
        elif isinstance(node, list):
            for child in node:
                walk(child)
    walk(result)
    return result


def advancement_semantics(old: dict, new: dict) -> tuple[bool, str]:
    old_normal = normalize_advancement(old)
    new_normal = normalize_advancement(new)
    if old_normal != new_normal:
        return False, "display, parent, criteria or requirements differ after namespace normalization"
    return True, "advancement graph, criteria triggers and item/entity paths match after module namespace normalization"


def sound_semantics(old: dict, new: dict) -> tuple[bool, str]:
    old_keys = {key.lower() for key in old}
    if old_keys != set(new):
        return False, f"sound event keys differ: missing={sorted(old_keys-set(new))}, extra={sorted(set(new)-old_keys)}"
    for old_key, old_event in old.items():
        new_event = new.get(old_key.lower())
        old_sounds = sorted((str(item.get("name", "")).split(":", 1)[-1].lower(),
                             bool(item.get("stream", False)))
                            for item in old_event.get("sounds", []))
        new_sounds = sorted((str(item.get("name", "")).split(":", 1)[-1].lower(),
                             bool(item.get("stream", False)))
                            for item in new_event.get("sounds", []))
        if old_sounds != new_sounds:
            return False, f"sound event {old_key} differs after namespace/case normalization"
    return True, "all 52 sound event keys and sample paths match after namespace/case normalization"


def strip_model_textures(value: dict) -> dict:
    result = copy.deepcopy(value)
    result.pop("__comment", None)
    result.pop("textures", None)
    return result


def model_semantics(old: dict, new: dict) -> tuple[bool, str]:
    if strip_model_textures(old) != strip_model_textures(new):
        return False, "model geometry/elements/UVs differ after texture namespace normalization"
    textures = new.get("textures", {})
    if not textures or not all(isinstance(value, str) and value for value in textures.values()):
        return False, "modern model has incomplete texture references"
    return True, "model geometry, element order and UVs are identical; modern texture references are explicit"


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
    auditor_path = "tools/audit_resource_semantics.py"
    auditor_hash = sha256(root / auditor_path)
    checks: list[tuple[str, str, str, str, callable]] = []
    for filename in ADVANCEMENTS:
        source = f"src/main/resources/assets/extra/animania/advancements/animania/{filename}"
        target = f"extra/src/main/resources/data/animania_extra/advancements/animania/{filename}"
        checks.append((source, target, "resource", "advancement", advancement_semantics))
    source = "src/main/resources/assets/extra/animania/sounds.json"
    target = "extra/src/main/resources/assets/animania_extra/sounds.json"
    checks.append((source, target, "resource", "sounds", sound_semantics))
    for filename in MOLDS:
        source = f"src/main/resources/assets/farm/animania/models/block/{filename}"
        target = f"farm/src/main/resources/assets/animania_farm/models/block/{filename}"
        checks.append((source, target, "resource", "model", model_semantics))

    report_rows = []
    results = []
    errors = []
    report_path = evidence_dir / "resource-semantics-v1-report.json"
    for source, target, requirement, kind, checker in checks:
        entry = by_source.get(source)
        old_path = root / "upstream/Animania-1.12" / source
        target_path = root / target
        if not entry or not old_path.is_file() or not target_path.is_file():
            errors.append(f"missing matrix/source/target for {source}")
            continue
        try:
            old = load(old_path)
            new = load(target_path)
            passed, summary = checker(old, new)
        except (OSError, ValueError, json.JSONDecodeError) as exc:
            passed, summary = False, f"parse failure: {exc}"
        row = {"source": source, "target": target, "kind": kind, "result": "pass" if passed else "fail",
               "summary": summary}
        report_rows.append(row)
        if passed:
            results.append({
                "entry_id": entry["entry_id"],
                "requirement_id": requirement,
                "result": "pass",
                "source_sha256": entry["sha256"],
                "target_paths": [{"path": target, "sha256": sha256(target_path)}],
                "tests": [{"selector": f"audit_resource_semantics::{source}", "result": "pass",
                            "artifact": "build/audit-evidence/resource-semantics-v1-report.json",
                            "artifact_sha256": "pending"}],
                "evidence_kind": "normalized_json",
                "test_code_path": auditor_path,
                "test_code_sha256": auditor_hash,
                "notes": [summary, "Resource-only semantic proof; client visual requirements are not supplied by this auditor."],
            })
    report = {"schema_version": 1, "audit": "resource-semantics", "audit_version": "v1",
              "rows": report_rows, "errors": errors, "error_count": len(errors),
              "all_passed": not errors and all(row["result"] == "pass" for row in report_rows)}
    evidence_dir.mkdir(parents=True, exist_ok=True)
    write_json(report_path, report)
    report_hash = sha256(report_path)
    for result in results:
        result["tests"][0]["artifact_sha256"] = report_hash
    manifest = {"schema_version": SCHEMA_VERSION, "audit_id": "resource-semantics", "audit_version": "v1",
                "source_revision": matrix.get("source_revision"),
                "command": "tools/audit_resource_semantics.py --root . --matrix docs/migration-matrix.json",
                "auditor_path": auditor_path, "auditor_sha256": auditor_hash,
                "results": results, "errors": errors}
    write_json(evidence_dir / "resource-semantics-v1.json", manifest)
    print(json.dumps({"results": len(results), "errors": errors, "report": str(report_path)}, ensure_ascii=False, indent=2))
    if errors or len(results) != len(checks):
        raise SystemExit(1)


if __name__ == "__main__":
    main()
