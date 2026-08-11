"""High-throughput, source-derived implementation audit for legacy Java models.

For every eligible 1.12 Java model this creates an isolated temporary source
tree, reruns the repository's ModelPart generator, and compares that model's
entire generated LayerDefinition method with the checked-in 1.20.1 target.
This is stronger than a class/bone-exists check, but deliberately supplies
only ``implementation``: visual texture, pose and screenshot requirements
remain open until the graphics capture gate has real artifacts.
"""
from __future__ import annotations

import argparse
import contextlib
import io
import json
import shutil
import sys
import tempfile
from pathlib import Path

from closure_common import SCHEMA_VERSION, read_json, sha256, write_json


TARGETS = {
    "base": "base/src/main/java/com/animania/client/model/BaseLegacyModelLayers.java",
    "farm": "farm/src/main/java/com/animania/farm/client/model/FarmLegacyModelLayers.java",
    "extra": "extra/src/main/java/com/animania/extra/client/model/ExtraLegacyModelLayers.java",
    "catsdogs": "catsdogs/src/main/java/com/animania/catsdogs/client/model/CatsDogsLegacyModelLayers.java",
}
TESTS = {
    "base": ("base/src/test/java/com/animania/client/BaseLegacyModelConversionTest.java",
             "base/build/test-results/test/TEST-com.animania.client.BaseLegacyModelConversionTest.xml",
             "BaseLegacyModelConversionTest#everyBaseJavaModelBakesItsLegacyGeometry"),
    "farm": ("farm/src/test/java/com/animania/farm/FarmRegistryTest.java",
             "farm/build/test-results/test/TEST-com.animania.farm.FarmRegistryTest.xml",
             "FarmRegistryTest#everyAnimalModelBakesGeometryAndEveryAnimationPathResolves"),
    "extra": ("extra/src/test/java/com/animania/extra/ExtraRegistryTest.java",
              "extra/build/test-results/test/TEST-com.animania.extra.ExtraRegistryTest.xml",
              "ExtraRegistryTest#everyAnimalModelBakesGeometryAndEveryAnimationPathResolves"),
    "catsdogs": ("catsdogs/src/test/java/com/animania/catsdogs/CatsDogsRegistryTest.java",
                 "catsdogs/build/test-results/test/TEST-com.animania.catsdogs.CatsDogsRegistryTest.xml",
                 "CatsDogsRegistryTest#everyPetModelBakesGeometryAndEveryAnimationPathResolves"),
}
BASE_METHODS = {
    "ModelSaltLick": "salt_lick", "ModelNest": "nest", "ModelTrough": "trough",
    "ModelWaterBottle": "water_bottle",
}


def method_body(text: str, name: str) -> str | None:
    prefix = f"    private static LayerDefinition {name}() {{"
    start = text.find(prefix)
    if start < 0:
        return None
    depth = 0
    for index in range(start, len(text)):
        if text[index] == "{":
            depth += 1
        elif text[index] == "}":
            depth -= 1
            if depth == 0:
                return text[start:index + 1]
    return None


def xml_green(path: Path, selector: str) -> bool:
    if not path.is_file():
        return False
    text = path.read_text(encoding="utf-8", errors="replace")
    return ("skipped=\"0\"" in text and "failures=\"0\"" in text and "errors=\"0\"" in text
            and selector.split("#", 1)[-1] in text)


def make_generated(root: Path) -> tuple[dict[str, str], list[str]]:
    """Regenerate the four aggregate layer classes without touching the checkout."""
    sys.path.insert(0, str(root / "tools"))
    import convert_legacy_java_models as converter
    errors: list[str] = []
    generated: dict[str, str] = {}
    with tempfile.TemporaryDirectory(prefix="animania-java-model-audit-") as temp_dir:
        temp = Path(temp_dir)
        try:
            shutil.copytree(root / "upstream/Animania-1.12/src/main/java/com/animania/client",
                            temp / "upstream/Animania-1.12/src/main/java/com/animania/client")
            (temp / "base/src/main/java/com/animania/client/model").mkdir(parents=True, exist_ok=True)
            for module in ("farm", "extra", "catsdogs"):
                shutil.copytree(root / f"upstream/Animania-1.12/src/main/java/com/animania/addons/{module}/client",
                                temp / f"upstream/Animania-1.12/src/main/java/com/animania/addons/{module}/client")
                class_name = ("CatsDogs" if module == "catsdogs" else module.title()) + "LegacyIds.java"
                source_ids = root / module / "src/main/java/com/animania" / module / class_name
                target_ids = temp / module / "src/main/java/com/animania" / module / class_name
                target_ids.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(source_ids, target_ids)
                (temp / module / "src/main/java/com/animania" / module / "client/model").mkdir(parents=True, exist_ok=True)
            # The converter's progress lines are useful interactively but
            # would corrupt the orchestrator's JSON-only subprocess output.
            with contextlib.redirect_stdout(io.StringIO()):
                converter.emit_base_facilities(temp)
                for module in ("farm", "extra", "catsdogs"):
                    converter.emit(temp, module)
            for module, relative in TARGETS.items():
                output = temp / relative
                if not output.is_file():
                    errors.append(f"generator did not emit {relative}")
                else:
                    generated[module] = output.read_text(encoding="utf-8")
        except (OSError, ValueError, SystemExit) as exc:
            errors.append(f"isolated generator failed: {exc}")
    return generated, errors


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
    ledger = read_json(root / "docs/content-ledger.json")
    ledger_models = {source: item for item in ledger.get("entries", []) if item.get("kind") == "java_model"
                     for source in item.get("baseline", {}).get("source_paths", [])}
    generated, errors = make_generated(root)
    by_source = {entry.get("source"): entry for entry in matrix.get("entries", [])}
    auditor_path = "tools/audit_java_model_implementation.py"
    results, rows, skipped = [], [], []
    sys.path.insert(0, str(root / "tools"))
    import convert_legacy_java_models as converter
    for source, ledger_item in sorted(ledger_models.items()):
        entry = by_source.get(source)
        if entry is None or "implementation" not in entry.get("requirements", []):
            continue
        module = entry.get("module")
        target_relative = TARGETS.get(module)
        if not target_relative or target_relative not in ledger_item.get("target", {}).get("paths", []):
            # Special models use dedicated renderers/models and require a
            # separate mapping audit; no broad fallback is allowed here.
            skipped.append({"source": source, "reason": "not a deterministic aggregate LayerDefinition target"})
            continue
        old = root / "upstream/Animania-1.12" / source
        target = root / target_relative
        test_code_relative, xml_relative, selector = TESTS[module]
        test_code, xml = root / test_code_relative, root / xml_relative
        name = Path(source).stem
        method = BASE_METHODS.get(name) if module == "base" else converter.snake(name)
        if not method:
            skipped.append({"source": source, "reason": "no explicit Base method mapping"})
            continue
        try:
            model = converter.parse_model(old)
            expected = method_body(generated.get(module, ""), method)
            actual = method_body(target.read_text(encoding="utf-8", errors="replace"), method) if target.is_file() else None
        except (OSError, ValueError) as exc:
            skipped.append({"source": source, "reason": f"source parse failed: {exc}"})
            continue
        if expected is None or actual is None or expected != actual or not xml_green(xml, selector):
            skipped.append({"source": source, "reason": "generated method/target/JUnit selector mismatch",
                            "method": method, "generated": expected is not None, "target": actual is not None,
                            "junit": xml_green(xml, selector)})
            continue
        structure = {"texture": [model.width, model.height], "parts": len(model.parts),
                     "cubes": sum(len(part.boxes) for part in model.parts.values()),
                     "parent_links": sum(len(part.children) for part in model.parts.values()),
                     "method": method}
        proof = evidence_dir / "java-model-implementation" / entry["entry_id"] / "proof.json"
        write_json(proof, {"entry_id": entry["entry_id"], "source": source,
                           "source_sha256": entry["sha256"], "target": target_relative,
                           "target_sha256": sha256(target), "structure": structure,
                           "comparison": "isolated source generator method equals checked-in method"})
        paths = [{"path": target_relative, "sha256": sha256(target)},
                 {"path": "tools/convert_legacy_java_models.py", "sha256": sha256(root / "tools/convert_legacy_java_models.py")},
                 {"path": proof.relative_to(root).as_posix(), "sha256": sha256(proof)}]
        tests = [{"selector": selector, "result": "pass", "artifact": xml_relative, "artifact_sha256": sha256(xml)}]
        results.append({"entry_id": entry["entry_id"], "requirement_id": "implementation", "result": "pass",
                        "source_sha256": entry["sha256"], "target_paths": paths, "tests": tests,
                        "evidence_kind": "source_mapping", "test_code_path": test_code_relative,
                        "test_code_sha256": sha256(test_code),
                        "notes": [f"[java-model-implementation-v1] {source}: the pinned source model's complete {method} LayerDefinition method ({structure['parts']} parts, {structure['cubes']} cubes, {structure['parent_links']} parent links) exactly matches isolated converter output and the checked-in target; visual/client requirements are intentionally not supplied."]})
        rows.append({"source": source, "module": module, "method": method, "structure": structure, "result": "pass"})
    report = {"schema_version": 1, "audit": "java-model-implementation", "audit_version": "v1",
              "rows": rows, "skipped": skipped, "errors": errors, "error_count": len(errors),
              "all_passed": not errors and not skipped}
    write_json(evidence_dir / "java-model-implementation-v1-report.json", report)
    write_json(evidence_dir / "java-model-implementation-v1.json", {
        "schema_version": SCHEMA_VERSION, "audit_id": "java-model-implementation", "audit_version": "v1",
        "source_revision": matrix.get("source_revision"),
        "command": "tools/audit_java_model_implementation.py --root . --matrix docs/migration-matrix.json",
        "auditor_path": auditor_path, "auditor_sha256": sha256(root / auditor_path),
        "results": results, "errors": errors,
    })
    print(json.dumps({"results": len(results), "rows": len(rows), "skipped": len(skipped), "errors": errors}, ensure_ascii=True, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
