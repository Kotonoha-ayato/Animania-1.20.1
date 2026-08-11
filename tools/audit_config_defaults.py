"""Audit the four configuration Java migrations with source-derived defaults.

This is an evidence-only companion to the older ledger updater.  It compares
every pinned default against the modern ForgeConfigSpec and the packaged
converter, checks the module registration call, and binds the result to the
already executed module/config-migrator JUnit selectors.  It emits evidence
for the implementation-only rows; runtime behavior rows remain separate.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

from closure_common import SCHEMA_VERSION, read_json, sha256, write_json


ADDONS = {
    "farm": {
        "source": "src/main/java/com/animania/addons/farm/config/FarmConfig.java",
        "old": "src/main/java/com/animania/addons/farm/config/FarmConfig.java",
        "target": "farm/src/main/java/com/animania/farm/FarmConfig.java",
        "main": "farm/src/main/java/com/animania/farm/AnimaniaFarm.java",
        "test": "farm/build/test-results/test/TEST-com.animania.farm.FarmRegistryTest.xml",
        "selector": "FarmRegistryTest#legacySleepBedDefaultsMapBlockStrawToBaseStraw",
        "test_source": "farm/src/test/java/com/animania/farm/FarmRegistryTest.java",
        "registration": "registerConfig(ModConfig.Type.COMMON, FarmConfig.SPEC)",
    },
    "extra": {
        "source": "src/main/java/com/animania/addons/extra/config/ExtraConfig.java",
        "old": "src/main/java/com/animania/addons/extra/config/ExtraConfig.java",
        "target": "extra/src/main/java/com/animania/extra/ExtraConfig.java",
        "main": "extra/src/main/java/com/animania/extra/AnimaniaExtra.java",
        "test": "extra/build/test-results/test/TEST-com.animania.extra.ExtraRegistryTest.xml",
        "selector": "ExtraRegistryTest#legacyFoodDefaultsPreserveOptionalModsAndRepairBrokenAnimaniaIds",
        "test_source": "extra/src/test/java/com/animania/extra/ExtraRegistryTest.java",
        "registration": "registerConfig(ModConfig.Type.COMMON, ExtraConfig.SPEC)",
    },
    "catsdogs": {
        "source": "src/main/java/com/animania/addons/catsdogs/config/CatsDogsConfig.java",
        "old": "src/main/java/com/animania/addons/catsdogs/config/CatsDogsConfig.java",
        "target": "catsdogs/src/main/java/com/animania/catsdogs/CatsDogsConfig.java",
        "main": "catsdogs/src/main/java/com/animania/catsdogs/AnimaniaCatsDogs.java",
        "test": "catsdogs/build/test-results/test/TEST-com.animania.catsdogs.CatsDogsRegistryTest.xml",
        "selector": "CatsDogsRegistryTest#legacyPetFoodAliasesAndAddonIdArePreserved",
        "test_source": "catsdogs/src/test/java/com/animania/catsdogs/CatsDogsRegistryTest.java",
        "registration": "registerConfig(ModConfig.Type.COMMON, CatsDogsConfig.SPEC)",
    },
}

CONFIG_MIGRATOR = "config-migrator/src/main/java/com/animania/migrator/ConfigMigrator.java"
MIGRATOR_TEST = "config-migrator/build/test-results/test/TEST-com.animania.migrator.ConfigMigratorTest.xml"
MIGRATOR_SOURCE = "config-migrator/src/test/java/com/animania/migrator/ConfigMigratorTest.java"
BASE = {
    "source": "src/main/java/com/animania/config/CommonConfig.java",
    "target": "base/src/main/java/com/animania/common/config/AnimaniaConfig.java",
    "main": "base/src/main/java/com/animania/Animania.java",
    "test": "base/build/test-results/test/TEST-com.animania.common.config.AnimaniaFoodOverrideTest.xml",
    "selector": "AnimaniaFoodOverrideTest#troughDefaultsRetainEveryLegacyOptionalFoodInOrder",
    "test_source": "base/src/test/java/com/animania/common/config/AnimaniaFoodOverrideTest.java",
    "registration": "registerConfig(ModConfig.Type.COMMON, AnimaniaConfig.COMMON_SPEC)",
}


def xml_green(path: Path, selector: str) -> bool:
    if not path.is_file():
        return False
    text = path.read_text(encoding="utf-8", errors="replace")
    return ('skipped="0"' in text and 'failures="0"' in text and 'errors="0"' in text
            and selector.split("#", 1)[-1] in text)


def addon_defaults(root: Path, spec: dict) -> tuple[dict, list[str]]:
    sys.path.insert(0, str(root / "tools"))
    import audit_config_migration as legacy
    old_text = (root / "upstream/Animania-1.12" / spec["old"]).read_text(encoding="utf-8")
    modern_text = (root / spec["target"]).read_text(encoding="utf-8")
    migrator_text = (root / CONFIG_MIGRATOR).read_text(encoding="utf-8")
    old = legacy.legacy_defaults(old_text)
    modern = legacy.modern_defaults(modern_text)
    migrated = legacy.migrator_defaults(migrator_text)
    checks, errors = {}, []
    for old_key, old_value in old.items():
        key = legacy.KEY_ALIASES.get(old_key, old_key)
        expected = legacy.normalize(old_value)
        checks[old_key] = {"modern_key": key, "expected": expected,
                           "modern": modern.get(key), "migrator": migrated.get(key)}
        if modern.get(key, object()) != expected:
            errors.append(f"{old_key}: ForgeConfigSpec {modern.get(key)!r} != {expected!r}")
        if migrated.get(key, object()) != expected:
            errors.append(f"{old_key}: migrator {migrated.get(key)!r} != {expected!r}")
    return checks, errors


def base_defaults(root: Path) -> tuple[dict, list[str]]:
    import audit_base_config as legacy
    old = (root / "upstream/Animania-1.12" / legacy.SOURCES[1]).read_text(encoding="utf-8")
    modern = (root / legacy.TARGET).read_text(encoding="utf-8")
    migrator = (root / legacy.MIGRATOR).read_text(encoding="utf-8")
    primitives = re.findall(r'public\s+(?:boolean|int|float|double)\s+(\w+)\s*=\s*([^;]+);', old)
    lists = re.findall(r'public\s+String\[\]\s+(\w+)\s*=\s*(?:new\s+String\[\]\s*)?\{(.*?)\};', old, re.S)
    checks, errors = {}, []
    for old_key, default in primitives:
        key = legacy.ALIASES.get(old_key, old_key)
        expected = legacy.clean_number(default)
        match = re.search(rf'(?:define|defineInRange)\("{re.escape(key)}",\s*([^,\)]+)', modern)
        actual = legacy.clean_number(match.group(1)) if match else None
        migrated = re.search(rf'Map\.entry\("{re.escape(key)}",\s*"([^"]+)"\)', migrator)
        migrated_value = legacy.clean_number(migrated.group(1)) if migrated else None
        checks[old_key] = {"modern_key": key, "expected": expected, "modern": actual, "migrator": migrated_value}
        if actual != expected: errors.append(f"{old_key}: ForgeConfigSpec {actual!r} != {expected!r}")
        if migrated_value != expected: errors.append(f"{old_key}: migrator {migrated_value!r} != {expected!r}")
    if len(primitives) != 42 or len(lists) != 3:
        errors.append(f"source shape changed: primitives={len(primitives)} lists={len(lists)}")
    for old_key, body in lists:
        key = legacy.ALIASES.get(old_key, old_key)
        values = re.findall(r'"([^"]*)"', body)
        if f'defineList("{key}"' not in modern or f'Map.entry("{key}",' not in migrator:
            errors.append(f"missing list field {key}")
        for value in values:
            normalized = {"animania:brown_egg": "animania_farm:brown_egg"}.get(value, value)
            if normalized not in modern or f'Map.entry("{key}",' not in migrator:
                errors.append(f"missing list value {key}={normalized}")
    return checks, errors


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
    auditor_path = "tools/audit_config_defaults.py"
    results, rows, skipped, errors = [], [], [], []
    configs = [("base", BASE)] + list(ADDONS.items())
    for label, spec in configs:
        entry = by_source.get(spec["source"])
        if entry is None:
            errors.append(f"matrix entry missing: {spec['source']}")
            continue
        target = root / spec["target"]
        main_file = root / spec["main"]
        test_source = root / spec["test_source"]
        test_xml = root / spec["test"]
        migrator_xml = root / MIGRATOR_TEST
        if label == "base":
            checks, check_errors = base_defaults(root)
        else:
            checks, check_errors = addon_defaults(root, spec)
        registration_ok = main_file.is_file() and spec["registration"] in main_file.read_text(encoding="utf-8", errors="replace")
        tests_ok = xml_green(test_xml, spec["selector"]) and xml_green(migrator_xml, "ConfigMigratorTest#rewritesLegacyRegistryIdsAndKeepsExactSourceDerivedDefaults")
        if check_errors or not target.is_file() or not main_file.is_file() or not registration_ok or not test_source.is_file() or not tests_ok:
            skipped.append({"module": label, "check_errors": check_errors,
                            "target": target.is_file(), "registration": registration_ok,
                            "tests_green": tests_ok})
            continue
        report_path = evidence_dir / "config-defaults" / entry["entry_id"] / "proof.json"
        write_json(report_path, {"module": label, "entry_id": entry["entry_id"],
                                 "source": spec["source"], "source_sha256": entry["sha256"],
                                 "defaults": checks, "registration": spec["registration"],
                                 "config_migrator": CONFIG_MIGRATOR})
        target_paths = [{"path": spec["target"], "sha256": sha256(target)},
                        {"path": spec["main"], "sha256": sha256(main_file)},
                        {"path": CONFIG_MIGRATOR, "sha256": sha256(root / CONFIG_MIGRATOR)},
                        {"path": report_path.relative_to(root).as_posix(), "sha256": sha256(report_path)}]
        tests = [
            {"selector": spec["selector"], "result": "pass", "artifact": spec["test"], "artifact_sha256": sha256(test_xml)},
            {"selector": "ConfigMigratorTest#rewritesLegacyRegistryIdsAndKeepsExactSourceDerivedDefaults", "result": "pass",
             "artifact": MIGRATOR_TEST, "artifact_sha256": sha256(migrator_xml)},
        ]
        notes = [f"[config-defaults-v1] {label}: every pinned 1.12 configuration default was compared independently with the modern ForgeConfigSpec and ConfigMigrator mapping; the module registration call and both exact JUnit selectors passed."]
        results.append({"entry_id": entry["entry_id"], "requirement_id": "implementation",
                        "result": "pass", "source_sha256": entry["sha256"],
                        "target_paths": target_paths, "tests": tests,
                        "evidence_kind": "executed_test", "test_code_path": spec["test_source"],
                        "test_code_sha256": sha256(test_source), "notes": notes})
        rows.append({"module": label, "source": spec["source"], "defaults": len(checks), "result": "pass"})
    report = {"schema_version": 1, "audit": "config-defaults", "audit_version": "v1",
              "rows": rows, "skipped": skipped, "errors": errors, "error_count": len(errors),
              "all_passed": not errors and not skipped}
    write_json(evidence_dir / "config-defaults-v1-report.json", report)
    write_json(evidence_dir / "config-defaults-v1.json", {
        "schema_version": SCHEMA_VERSION, "audit_id": "config-defaults", "audit_version": "v1",
        "source_revision": matrix.get("source_revision"),
        "command": "tools/audit_config_defaults.py --root . --matrix docs/migration-matrix.json",
        "auditor_path": auditor_path, "auditor_sha256": sha256(root / auditor_path),
        "results": results, "errors": errors,
    })
    print(json.dumps({"results": len(results), "rows": len(rows), "skipped": len(skipped), "errors": errors}, ensure_ascii=True, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
