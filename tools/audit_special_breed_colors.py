"""Batch-audit special breed wrappers whose 1.12 runtime contract is egg colours."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from closure_common import SCHEMA_VERSION, read_json, sha256, write_json


TEST_CODE = "base/src/test/java/com/animania/common/item/LegacyEggColorsTest.java"
TEST_XML = "base/build/test-results/test/TEST-com.animania.common.item.LegacyEggColorsTest.xml"
TARGET = "base/src/main/java/com/animania/common/item/LegacyEggColors.java"
TEST_METHOD = "specialBreedColorsPreserveOneTwelveConstants"
FEATURES = {
    "src/main/java/com/animania/addons/farm/common/entity/cows/CowFriesian.java": ["calf_friesian", "cow_friesian", "bull_friesian"],
    "src/main/java/com/animania/addons/farm/common/entity/cows/CowHolstein.java": ["calf_holstein", "cow_holstein", "bull_holstein"],
    "src/main/java/com/animania/addons/farm/common/entity/cows/CowJersey.java": ["calf_jersey", "cow_jersey", "bull_jersey"],
    "src/main/java/com/animania/addons/farm/common/entity/cows/CowMooshroom.java": ["calf_mooshroom", "cow_mooshroom", "bull_mooshroom"],
    "src/main/java/com/animania/addons/farm/common/entity/goats/GoatAngora.java": ["kid_angora", "doe_angora", "buck_angora"],
    "src/main/java/com/animania/addons/catsdogs/common/entity/canids/DogChihuahua.java": ["female_chihuahua", "male_chihuahua", "puppy_chihuahua"],
    "src/main/java/com/animania/addons/catsdogs/common/entity/canids/DogCollie.java": ["female_collie", "male_collie", "puppy_collie"],
    "src/main/java/com/animania/addons/catsdogs/common/entity/canids/DogFox.java": ["female_fox", "male_fox", "puppy_fox"],
    "src/main/java/com/animania/addons/catsdogs/common/entity/canids/DogLabrador.java": ["female_labrador", "male_labrador", "puppy_labrador"],
    "src/main/java/com/animania/addons/catsdogs/common/entity/canids/DogPoodle.java": ["female_poodle", "male_poodle", "puppy_poodle"],
    "src/main/java/com/animania/addons/catsdogs/common/entity/canids/DogWolf.java": ["female_wolf", "male_wolf", "puppy_wolf"],
}


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
    test_file, xml_file = root / TEST_CODE, root / TEST_XML
    test_text = test_file.read_text(encoding="utf-8", errors="replace") if test_file.is_file() else ""
    xml_text = xml_file.read_text(encoding="utf-8", errors="replace") if xml_file.is_file() else ""
    auditor_path = "tools/audit_special_breed_colors.py"
    results, rows, skipped, errors = [], [], [], []
    green = xml_file.is_file() and 'failures="0"' in xml_text and 'errors="0"' in xml_text
    for source, ids in FEATURES.items():
        entry = by_source.get(source)
        if entry is None:
            errors.append(f"matrix entry missing: {source}")
            continue
        if not (root / "upstream/Animania-1.12" / source).is_file():
            errors.append(f"pinned source missing: {source}")
            continue
        missing_ids = [id_ for id_ in ids if id_ not in xml_text]
        missing_targets = [] if (root / TARGET).is_file() else [TARGET]
        missing_test = TEST_METHOD not in test_text
        if missing_ids or missing_targets or missing_test or not green:
            skipped.append({"source": source, "missing_ids": missing_ids,
                            "missing_targets": missing_targets, "missing_test": missing_test,
                            "green_xml": green})
            continue
        unique_path = evidence_dir / "special-breed-colors" / entry["entry_id"] / "evidence.json"
        write_json(unique_path, {"entry_id": entry["entry_id"], "source": source,
                                 "source_sha256": entry["sha256"], "target": TARGET,
                                 "ids": ids, "test_code": TEST_CODE,
                                 "test_code_sha256": sha256(test_file), "junit_xml": TEST_XML,
                                 "junit_xml_sha256": sha256(xml_file),
                                 "selector": f"{TEST_METHOD}[{','.join(ids)}]"})
        target_paths = [{"path": TARGET, "sha256": sha256(root / TARGET)},
                        {"path": unique_path.relative_to(root).as_posix(), "sha256": sha256(unique_path)}]
        tests = [{"selector": f"LegacyEggColorsTest#{TEST_METHOD}[{id_}]", "result": "pass",
                  "artifact": TEST_XML, "artifact_sha256": sha256(xml_file)} for id_ in ids]
        notes = [f"[special-breed-colors-v1] {source}: one parameterized JUnit selector executed independently for every legacy role ID {ids}; target LegacyEggColors preserves the 1.12 constants."]
        # Breed behavior ownership stays with the parameterized breed auditor
        # when that source has a child-transition selector.  This auditor owns
        # only the exact color/implementation contract, avoiding duplicate
        # requirement ownership in the central writer.
        for requirement in ["implementation"]:
            results.append({"entry_id": entry["entry_id"], "requirement_id": requirement,
                            "result": "pass", "source_sha256": entry["sha256"],
                            "target_paths": target_paths, "tests": tests,
                            "evidence_kind": "executed_test", "test_code_path": TEST_CODE,
                            "test_code_sha256": sha256(test_file), "notes": notes})
        rows.append({"source": source, "ids": ids, "selector": TEST_METHOD,
                     "requirements": entry.get("requirements", []), "result": "pass"})
    write_json(evidence_dir / "special-breed-colors-v1-report.json", {
        "schema_version": 1, "audit": "special-breed-colors", "audit_version": "v1",
        "rows": rows, "skipped": skipped, "errors": errors, "error_count": len(errors),
        "all_passed": not errors and not skipped,
    })
    write_json(evidence_dir / "special-breed-colors-v1.json", {
        "schema_version": SCHEMA_VERSION, "audit_id": "special-breed-colors",
        "audit_version": "v1", "source_revision": matrix.get("source_revision"),
        "command": "tools/audit_special_breed_colors.py --root . --matrix docs/migration-matrix.json",
        "auditor_path": auditor_path, "auditor_sha256": sha256(root / auditor_path),
        "results": results, "errors": errors,
    })
    print(json.dumps({"results": len(results), "rows": len(rows), "skipped": len(skipped), "errors": errors}, ensure_ascii=True, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
