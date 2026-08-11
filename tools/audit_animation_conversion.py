"""Audit the eight CraftStudio animation resources through deterministic conversion.

The old binary-ish JSON clips are deliberately not packaged in 1.20.1.  This
auditor copies each pinned 1.12 clip into an isolated temporary archive,
invokes the repository converter, and compares the generated
AnimationDefinition source with the checked-in native class (ignoring only the
historical comment).  A passing JUnit bake/bone test is also required for the
module.  It closes only the resource requirement; client visual/pose evidence
remains a separate release gate.
"""
from __future__ import annotations

import argparse
import json
import shutil
import sys
import tempfile
from pathlib import Path

from closure_common import SCHEMA_VERSION, read_json, sha256, write_json


FEATURES = [
    {
        "module": "farm", "archive": "farm-craftstudio", "class": "FarmNativeAnimations",
        "target": "farm/src/main/java/com/animania/farm/client/model/FarmNativeAnimations.java",
        "test_code": "farm/src/test/java/com/animania/farm/FarmNativeModelConversionTest.java",
        "test_xml": "farm/build/test-results/test/TEST-com.animania.farm.FarmNativeModelConversionTest.xml",
    },
    {
        "module": "extra", "archive": "extra-craftstudio", "class": "ExtraNativeAnimations",
        "target": "extra/src/main/java/com/animania/extra/client/model/ExtraNativeAnimations.java",
        "test_code": "extra/src/test/java/com/animania/extra/ExtraNativeModelConversionTest.java",
        "test_xml": "extra/build/test-results/test/TEST-com.animania.extra.ExtraNativeModelConversionTest.xml",
    },
]


def normalized_generated(text: str) -> str:
    return text.replace(
        "// Generated native AnimationDefinitions from archived CraftStudio keyframes.",
        "// Generated native AnimationDefinitions from archived legacy native keyframes.",
    )


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
    by_source = {entry.get("source"): entry for entry in matrix.get("entries", [])}
    auditor_path = "tools/audit_animation_conversion.py"
    results, rows, skipped, errors = [], [], [], []
    sys.path.insert(0, str(root / "tools"))
    try:
        import convert_craftstudio_models as converter
    except Exception as exc:  # pragma: no cover - exercised by the audit command
        errors.append(f"converter import failed: {exc}")
        converter = None

    with tempfile.TemporaryDirectory(prefix="animania-animation-audit-") as temp_dir:
        temp_root = Path(temp_dir)
        for feature in FEATURES:
            source_root = root / "upstream/Animania-1.12/src/main/resources/assets" / feature["module"] / "animania/craftstudio/animations"
            target_file = root / feature["target"]
            test_file = root / feature["test_code"]
            xml_file = root / feature["test_xml"]
            source_paths = sorted(source_root.rglob("*.csjsmodelanim"))
            if not source_paths:
                errors.append(f"no pinned animation sources for {feature['module']}")
                continue
            module_rows = []
            archive_root = temp_root / "legacy-archive" / feature["archive"] / "animations"
            for source in source_paths:
                relative = source.relative_to(source_root)
                archive_file = archive_root / relative
                archive_file.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(source, archive_file)
                module_rows.append({"source": source, "archive": archive_file})
            if converter is not None:
                converter.emit_animations(temp_root, feature["module"], feature["archive"], feature["class"])
            generated = temp_root / feature["target"]
            xml_text = xml_file.read_text(encoding="utf-8", errors="replace") if xml_file.is_file() else ""
            test_text = test_file.read_text(encoding="utf-8", errors="replace") if test_file.is_file() else ""
            green = (xml_file.is_file() and 'failures="0"' in xml_text and 'errors="0"' in xml_text
                     and 'skipped="0"' in xml_text and "everyNativeModelAndAnimationBoneResolves" in xml_text)
            module_ok = (converter is not None and generated.is_file() and target_file.is_file()
                         and normalized_generated(generated.read_text(encoding="utf-8"))
                         == normalized_generated(target_file.read_text(encoding="utf-8"))
                         and test_file.is_file() and "everyNativeModelAndAnimationBoneResolves" in test_text and green)
            if not module_ok:
                skipped.append({"module": feature["module"], "source_count": len(source_paths),
                                "generated": generated.is_file(), "target": target_file.is_file(),
                                "unit_test_green": green, "converter_loaded": converter is not None})
                continue
            for source in source_paths:
                entry = by_source.get(source.relative_to(root / "upstream/Animania-1.12").as_posix())
                if entry is None:
                    errors.append(f"matrix entry missing: {source}")
                    continue
                evidence_path = evidence_dir / "animation-conversion" / entry["entry_id"] / "evidence.json"
                write_json(evidence_path, {
                    "entry_id": entry["entry_id"], "source": entry["source"],
                    "source_sha256": entry["sha256"], "target": feature["target"],
                    "generated_sha256": sha256(generated), "test_code": feature["test_code"],
                    "test_code_sha256": sha256(test_file), "junit_xml": feature["test_xml"],
                    "junit_xml_sha256": sha256(xml_file),
                    "comparison": "converter output equals target after comment-only normalization",
                })
                targets = [{"path": feature["target"], "sha256": sha256(target_file)},
                           {"path": feature["test_code"], "sha256": sha256(test_file)},
                           {"path": evidence_path.relative_to(root).as_posix(), "sha256": sha256(evidence_path)}]
                test = {"selector": f"{Path(feature['test_xml']).stem}#everyNativeModelAndAnimationBoneResolves",
                        "result": "pass", "artifact": feature["test_xml"], "artifact_sha256": sha256(xml_file)}
                notes = [
                    f"[animation-conversion-v1] All {len(source_paths)} pinned {feature['module']} CraftStudio clips were copied from the read-only 1.12 checkout into an isolated archive, deterministically converted, and compared with the checked-in native AnimationDefinition class; the module JUnit bake/bone selector passed.",
                    "This closes only the resource conversion requirement; no client screenshot or pose-regression claim is made.",
                ]
                results.append({"entry_id": entry["entry_id"], "requirement_id": "resource",
                                "result": "pass", "source_sha256": entry["sha256"],
                                "target_paths": targets, "tests": [test],
                                "evidence_kind": "source_mapping", "test_code_path": feature["test_code"],
                                "test_code_sha256": sha256(test_file), "notes": notes})
                rows.append({"source": entry["source"], "module": feature["module"],
                             "selector": test["selector"], "result": "pass"})
    report = {"schema_version": 1, "audit": "animation-conversion", "audit_version": "v1",
              "rows": rows, "skipped": skipped, "errors": errors, "error_count": len(errors),
              "all_passed": not errors and not skipped}
    write_json(evidence_dir / "animation-conversion-v1-report.json", report)
    write_json(evidence_dir / "animation-conversion-v1.json", {
        "schema_version": SCHEMA_VERSION, "audit_id": "animation-conversion",
        "audit_version": "v1", "source_revision": matrix.get("source_revision"),
        "command": "tools/audit_animation_conversion.py --root . --matrix docs/migration-matrix.json",
        "auditor_path": auditor_path, "auditor_sha256": sha256(root / auditor_path),
        "results": results, "errors": errors,
    })
    print(json.dumps({"results": len(results), "rows": len(rows), "skipped": len(skipped), "errors": errors}, ensure_ascii=True, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
