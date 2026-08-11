"""Audit every legacy addon renderer consolidated into the native client stack.

1.12 kept one renderer/layer/pose class for most sex and breed combinations.
The Forge 1.20.1 port deliberately registers a native ModelPart layer and a
single ``AnimaniaAnimalRenderer`` for every source-derived entity ID.  This
audit proves that source-to-target implementation mapping at scale while
explicitly withholding the ``client`` requirement: baking a layer in JUnit is
not a visual atlas/screenshot regression.
"""
from __future__ import annotations

import argparse
import json
import xml.etree.ElementTree as ET
from pathlib import Path

from closure_common import SCHEMA_VERSION, read_json, sha256, write_json


MODULES = {
    "farm": {
        "client": "farm/src/main/java/com/animania/farm/AnimaniaFarmClient.java",
        "registry": "farm/src/main/java/com/animania/farm/AnimaniaFarm.java",
        "legacy_layers": "farm/src/main/java/com/animania/farm/client/model/FarmLegacyModelLayers.java",
        "native_layers": "farm/src/main/java/com/animania/farm/client/model/FarmNativeModelLayers.java",
        "test": "farm/src/test/java/com/animania/farm/FarmRegistryTest.java",
        "xml": "farm/build/test-results/test/TEST-com.animania.farm.FarmRegistryTest.xml",
        "selector": "everyAnimalModelBakesGeometryAndEveryAnimationPathResolves()",
        "facility_renderer": "farm/src/main/java/com/animania/farm/client/render/FarmHiveRenderer.java",
    },
    "extra": {
        "client": "extra/src/main/java/com/animania/extra/AnimaniaExtraClient.java",
        "registry": "extra/src/main/java/com/animania/extra/AnimaniaExtra.java",
        "legacy_layers": "extra/src/main/java/com/animania/extra/client/model/ExtraLegacyModelLayers.java",
        "native_layers": "extra/src/main/java/com/animania/extra/client/model/ExtraNativeModelLayers.java",
        "test": "extra/src/test/java/com/animania/extra/ExtraRegistryTest.java",
        "xml": "extra/build/test-results/test/TEST-com.animania.extra.ExtraRegistryTest.xml",
        "selector": "everyAnimalModelBakesGeometryAndEveryAnimationPathResolves()",
        "facility_renderer": "extra/src/main/java/com/animania/extra/client/render/ExtraHamsterWheelRenderer.java",
    },
    "catsdogs": {
        "client": "catsdogs/src/main/java/com/animania/catsdogs/AnimaniaCatsDogsClient.java",
        "registry": "catsdogs/src/main/java/com/animania/catsdogs/AnimaniaCatsDogs.java",
        "legacy_layers": "catsdogs/src/main/java/com/animania/catsdogs/client/model/CatsDogsLegacyModelLayers.java",
        "native_layers": "catsdogs/src/main/java/com/animania/catsdogs/client/model/CatsDogsNativeModelLayers.java",
        "test": "catsdogs/src/test/java/com/animania/catsdogs/CatsDogsRegistryTest.java",
        "xml": "catsdogs/build/test-results/test/TEST-com.animania.catsdogs.CatsDogsRegistryTest.xml",
        "selector": "everyPetModelBakesGeometryAndEveryAnimationPathResolves()",
        "facility_renderer": "catsdogs/src/main/java/com/animania/catsdogs/client/render/CatsDogsPetFacilityRenderer.java",
    },
}
ANIMAL_RENDERER = "base/src/main/java/com/animania/client/render/AnimaniaAnimalRenderer.java"
VEHICLE_RENDERER = "base/src/main/java/com/animania/client/render/AnimaniaVehicleRenderer.java"


def selector_passes(report: Path, selector: str) -> bool:
    try:
        suite = ET.parse(report).getroot()
    except (OSError, ET.ParseError):
        return False
    return any(case.attrib.get("name") == selector
               and not (case.findall("failure") or case.findall("error") or case.findall("skipped"))
               for case in suite.findall(".//testcase"))


def target_paths(root: Path, module: str, source: str) -> tuple[list[str], str]:
    """Select the concrete native owner used by this old renderer family."""
    spec = MODULES[module]
    values = [spec["client"], spec["registry"], spec["legacy_layers"], ANIMAL_RENDERER]
    if "/render/props/" in source:
        values += [VEHICLE_RENDERER, spec["native_layers"]]
        return values, "vehicle renderer registration"
    if "tileentity" in source or "/models/blocks/" in source or "/model/tileentity/" in source:
        values += [spec["facility_renderer"], spec["native_layers"]]
        return values, "block-entity/facility native renderer registration"
    if "/layer/" in source or "/layers/" in source:
        return values, "native shared renderer layer registration"
    if "/poses/" in source or "/model" in source or "/models/" in source:
        values += [spec["native_layers"]]
        return values, "ModelPart layer/profile conversion"
    return values, "entity renderer registration"


def java_model_owned_sources(root: Path) -> set[str]:
    """Do not compete with the geometry auditor for deterministic Java models."""
    report = root / "build/audit-evidence/java-model-implementation-v1-report.json"
    if not report.is_file():
        return set()
    try:
        data = read_json(report)
    except (OSError, json.JSONDecodeError):
        return set()
    return {str(row.get("source", "")).replace("\\", "/") for row in data.get("rows", [])}


def source_is_consolidated_renderer(entry: dict, model_owned: set[str]) -> bool:
    source = str(entry.get("source", "")).replace("\\", "/")
    module = entry.get("module")
    return (entry.get("kind") == "java" and entry.get("status") != "closed" and module in MODULES
            and f"addons/{module}/client/" in source and source not in model_owned
            and "implementation" in entry.get("requirements", []))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, required=True)
    parser.add_argument("--evidence-dir", type=Path, default=Path("build/audit-evidence"))
    args = parser.parse_args()
    root = args.root.resolve()
    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    evidence_dir = args.evidence_dir if args.evidence_dir.is_absolute() else root / args.evidence_dir
    matrix = read_json(matrix_path)
    auditor_path = "tools/audit_consolidated_client_implementation.py"
    results, rows, errors = [], [], []
    model_owned = java_model_owned_sources(root)
    for module, spec in MODULES.items():
        prerequisites = [root / spec[key] for key in ("client", "registry", "legacy_layers", "test", "xml")]
        prerequisites.append(root / ANIMAL_RENDERER)
        if not all(path.is_file() for path in prerequisites) or not selector_passes(root / spec["xml"], spec["selector"]):
            errors.append(f"{module}: required native client source or selected passing JUnit report is missing")
    for entry in matrix.get("entries", []):
        if not source_is_consolidated_renderer(entry, model_owned):
            continue
        module = entry["module"]
        source = str(entry["source"]).replace("\\", "/")
        old = root / "upstream/Animania-1.12" / source
        paths, mapping = target_paths(root, module, source)
        actual_paths = [root / relative for relative in paths]
        if not old.is_file() or not all(path.is_file() for path in actual_paths):
            errors.append(f"{module}: target mapping file is missing for {source}")
            continue
        spec = MODULES[module]
        client_text = (root / spec["client"]).read_text(encoding="utf-8", errors="replace")
        registry_text = (root / spec["registry"]).read_text(encoding="utf-8", errors="replace")
        animal_renderer_text = (root / ANIMAL_RENDERER).read_text(encoding="utf-8", errors="replace")
        # These are semantic guardrails against a mere filename-exists audit:
        # the client must loop the live module registry and build the shared
        # renderer from ID-indexed native layers; the central renderer must
        # actually bake the passed ModelPart layer.
        if not ("ENTITIES.forEach" in client_text and "AnimaniaAnimalRenderer" in client_text
                and "LegacyIds.ALL" in registry_text and "bakeLayer" in animal_renderer_text):
            errors.append(f"{module}: native renderer ownership guard failed for {source}")
            continue
        proof = evidence_dir / "consolidated-client-implementation" / entry["entry_id"] / "proof.json"
        write_json(proof, {
            "entry_id": entry["entry_id"], "source": source, "source_sha256": entry["sha256"],
            "legacy_classes": entry.get("classes", []), "mapping": mapping,
            "targets": paths, "source_line_count": len(old.read_text(encoding="utf-8", errors="replace").splitlines()),
            "guard": "module client enumerates live entity registry into ID-indexed native ModelPart layers and shared renderer",
            "test_selector": spec["selector"],
        })
        results.append({
            "entry_id": entry["entry_id"], "requirement_id": "implementation", "result": "pass",
            "source_sha256": entry["sha256"],
            "target_paths": ([{"path": relative, "sha256": sha256(root / relative)} for relative in paths]
                             + [{"path": proof.relative_to(root).as_posix(), "sha256": sha256(proof)}]),
            "tests": [{"selector": f"{spec['xml']}::{spec['selector']}", "result": "pass",
                       "artifact": spec["xml"], "artifact_sha256": sha256(root / spec["xml"])}],
            "evidence_kind": "source_mapping", "test_code_path": spec["test"],
            "test_code_sha256": sha256(root / spec["test"]),
            "notes": [
                f"[consolidated-client-implementation-v1] {Path(source).stem} ({mapping}) is represented by the {module} live-registry native client path. "
                f"The selected JUnit test baked the module's source-derived entity layers and resolved animation paths. This is implementation-only; no atlas load, texture existence, pose image, or client screenshot requirement is supplied."
            ],
        })
        rows.append({"entry_id": entry["entry_id"], "source": source, "module": module, "mapping": mapping, "result": "pass"})
    evidence_dir.mkdir(parents=True, exist_ok=True)
    write_json(evidence_dir / "consolidated-client-implementation-v1-report.json", {
        "schema_version": 1, "audit": "consolidated-client-implementation", "audit_version": "v1",
        "rows": rows, "errors": errors, "error_count": len(errors),
    })
    write_json(evidence_dir / "consolidated-client-implementation-v1.json", {
        "schema_version": SCHEMA_VERSION, "audit_id": "consolidated-client-implementation", "audit_version": "v1",
        "source_revision": matrix.get("source_revision"),
        "command": "tools/audit_consolidated_client_implementation.py --root . --matrix docs/migration-matrix.json",
        "auditor_path": auditor_path, "auditor_sha256": sha256(root / auditor_path),
        "results": results, "errors": errors,
    })
    print(json.dumps({"results": len(results), "rows": len(rows), "errors": errors}, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
