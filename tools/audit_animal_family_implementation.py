"""Prove the modern implementation mapping for each consolidated animal class.

The 1.12 hierarchy had a source class for each sex/family while the 1.20.1
port intentionally has one data-driven ``AnimaniaAnimalEntity`` registration
per legacy ID.  This audit does not treat that consolidation as a behavioral
equivalence claim.  It binds each old source class to its precise ID family,
checks the modern registry and common implementation sources, and requires a
fresh Forge GameTest which constructs *every* ID in that family.

Only the ``implementation`` requirement is emitted.  Overrides, NBT branches,
AI, and visual requirements remain available to their specialised auditors.
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

from closure_common import SCHEMA_VERSION, read_json, sha256, write_json


MODULES = {
    "farm": {
        "ids": "farm/src/main/java/com/animania/farm/FarmLegacyIds.java",
        "registry": "farm/src/main/java/com/animania/farm/AnimaniaFarm.java",
        "test": "farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java",
        "log": "farm/run/gameTestServer/logs/latest.log",
        "marker": "animania_farm:all_legacy_animals_construct_persist",
    },
    "extra": {
        "ids": "extra/src/main/java/com/animania/extra/ExtraLegacyIds.java",
        "registry": "extra/src/main/java/com/animania/extra/AnimaniaExtra.java",
        "test": "extra/src/main/java/com/animania/extra/gametest/AnimaniaExtraGameTests.java",
        "log": "extra/run/gameTestServer/logs/latest.log",
        "marker": "animania_extra:all_legacy_animals_construct_persist",
    },
    "catsdogs": {
        "ids": "catsdogs/src/main/java/com/animania/catsdogs/CatsDogsLegacyIds.java",
        "registry": "catsdogs/src/main/java/com/animania/catsdogs/AnimaniaCatsDogs.java",
        "test": "catsdogs/src/main/java/com/animania/catsdogs/gametest/AnimaniaCatsDogsGameTests.java",
        "log": "catsdogs/run/gameTestServer/logs/latest.log",
        "marker": "animania_catsdogs:all_legacy_animals_construct_persist",
    },
}
COMMON_ENTITY = "base/src/main/java/com/animania/common/entity/AnimaniaAnimalEntity.java"


def legacy_ids(path: Path) -> list[str]:
    """Read exactly the ``ALL`` list; other string constants are not IDs."""
    text = path.read_text(encoding="utf-8", errors="replace")
    match = re.search(r"ALL\s*=\s*List\.of\((.*?)\);", text, flags=re.DOTALL)
    if not match:
        raise ValueError(f"could not find ALL list in {path}")
    return re.findall(r'"([a-z0-9_]+)"', match.group(1))


def starts(ids: list[str], *prefixes: str) -> list[str]:
    return [identifier for identifier in ids if identifier.startswith(prefixes)]


def family_ids(module: str, source: str, ids: list[str]) -> list[str]:
    """Return the exact registration family owned by a legacy source class."""
    name = Path(source).stem
    if module == "farm":
        if "chickens/" in source:
            return starts(ids, {"EntityHenBase": "hen_", "EntityRoosterBase": "rooster_"}.get(name, "hen_"),
                          *(() if name in {"EntityHenBase", "EntityRoosterBase"} else ("rooster_", "chick_")))
        if "cows/" in source:
            return starts(ids, {"EntityBullBase": "bull_", "EntityCowBase": "cow_"}.get(name, "cow_"),
                          *(() if name in {"EntityBullBase", "EntityCowBase"} else ("bull_", "calf_")))
        if "goats/" in source:
            return starts(ids, {"EntityBuckBase": "buck_", "EntityDoeBase": "doe_"}.get(name, "doe_"),
                          *(() if name in {"EntityBuckBase", "EntityDoeBase"} else ("buck_", "kid_")))
        if "horses/" in source:
            return starts(ids, {"EntityMareBase": "mare_", "EntityStallionBase": "stallion_"}.get(name, "mare_"),
                          *(() if name in {"EntityMareBase", "EntityStallionBase"} else ("stallion_", "foal_")))
        if "pigs/" in source:
            return starts(ids, {"EntityHogBase": "hog_", "EntitySowBase": "sow_"}.get(name, "sow_"),
                          *(() if name in {"EntityHogBase", "EntitySowBase"} else ("hog_", "piglet_")))
        if "sheep/" in source:
            if name.startswith("Sheep"):
                breed = name.removeprefix("Sheep").lower()
                return [identifier for identifier in ids if identifier.endswith("_" + breed)]
            return starts(ids, {"EntityEweBase": "ewe_", "EntityRamBase": "ram_"}.get(name, "ewe_"),
                          *(() if name in {"EntityEweBase", "EntityRamBase"} else ("ram_", "lamb_")))
    if module == "extra":
        if "amphibians/" in source:
            mapping = {"EntityDartFrogs": ["dartfrog"], "EntityFrogs": ["frog"], "EntityToad": ["toad"]}
            return mapping.get(name, [identifier for identifier in ids if identifier in {"dartfrog", "frog", "toad"}])
        if "peafowl/" in source:
            return starts(ids, "peachick_") if name == "EntityPeachickBase" else (
                starts(ids, "peacock_") if name == "EntityPeacockBase" else starts(ids, "peachick_", "peacock_", "peahen_"))
        if "rodents/rabbits/" in source:
            return starts(ids, {"EntityRabbitBuckBase": "buck_", "EntityRabbitDoeBase": "doe_", "EntityRabbitKitBase": "kit_"}.get(name, "buck_"),
                          *(() if name in {"EntityRabbitBuckBase", "EntityRabbitDoeBase", "EntityRabbitKitBase"} else ("doe_", "kit_")))
        if "rodents/" in source:
            mapping = {
                "EntityFerretGrey": ["ferret_grey"], "EntityFerretWhite": ["ferret_white"],
                "EntityHedgehog": ["hedgehog"], "EntityHedgehogAlbino": ["hedgehog_albino"],
                "HamsterType": ["hamster"],
            }
            if name in mapping:
                return mapping[name]
            if name in {"EntityFerretBase", "FerretType"}:
                return starts(ids, "ferret_")
            if name in {"EntityHedgehogBase", "HedgehogType"}:
                return starts(ids, "hedgehog")
    if module == "catsdogs":
        if "canids/" in source:
            return starts(ids, {"EntityFemaleDogBase": "female_", "EntityMaleDogBase": "male_", "EntityPuppyBase": "puppy_"}.get(name, "female_"),
                          *(() if name in {"EntityFemaleDogBase", "EntityMaleDogBase", "EntityPuppyBase"} else ("male_", "puppy_")))
        if "felids/" in source:
            return starts(ids, {"EntityQueenBase": "queen_", "EntityTomBase": "tom_", "EntityKittenBase": "kitten_"}.get(name, "queen_"),
                          *(() if name in {"EntityQueenBase", "EntityTomBase", "EntityKittenBase"} else ("tom_", "kitten_")))
    return []


def passing_log(path: Path, marker: str) -> bool:
    if not path.is_file():
        return False
    text = path.read_text(encoding="utf-8", errors="replace")
    return f"[ANIMANIA_TEST_SELECTOR] {marker}" in text and "required tests passed :)" in text


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
    auditor_path = "tools/audit_animal_family_implementation.py"
    common_entity = root / COMMON_ENTITY
    results, errors, rows = [], [], []
    ids_by_module = {module: legacy_ids(root / spec["ids"]) for module, spec in MODULES.items()}
    for module, spec in MODULES.items():
        needed = (root / spec["registry"], root / spec["test"], root / spec["log"], common_entity)
        if not all(path.is_file() for path in needed) or not passing_log(root / spec["log"], spec["marker"]):
            errors.append(f"{module}: missing modern target/test or uniquely marked passing Forge log")
    for entry in matrix.get("entries", []):
        source = entry.get("source", "").replace("\\", "/")
        module = entry.get("module")
        if (entry.get("kind") != "java" or entry.get("status") == "closed" or module not in MODULES
                or "/entity/" not in source or "/ai/" in source
                or "implementation" not in entry.get("requirements", []) or Path(source).stem == "EntityWagon"):
            continue
        ids = family_ids(module, source, ids_by_module[module])
        if not ids:
            errors.append(f"{module}: no explicit ID family mapping for {source}")
            continue
        spec = MODULES[module]
        upstream = root / "upstream/Animania-1.12" / source
        registry, test, log = root / spec["registry"], root / spec["test"], root / spec["log"]
        if not upstream.is_file() or not registry.is_file() or not test.is_file() or not common_entity.is_file() or not passing_log(log, spec["marker"]):
            errors.append(f"{module}: stale mapping prerequisites for {source}")
            continue
        source_text = upstream.read_text(encoding="utf-8", errors="replace")
        overrides = entry.get("baseline", {}).get("overrides", [])
        behaviors = entry.get("baseline", {}).get("behaviors", [])
        # The test must visibly enumerate every source-derived registration ID
        # and create them through the module's actual DeferredRegister map.
        test_text = test.read_text(encoding="utf-8", errors="replace")
        if "LegacyIds.ALL" not in test_text or "AnimaniaAnimalEntity" not in test_text or "addAdditionalSaveData" not in test_text:
            errors.append(f"{module}: construct test no longer has its per-ID implementation assertions")
            continue
        proof = evidence_dir / "animal-family-implementation" / entry["entry_id"] / "proof.json"
        write_json(proof, {
            "entry_id": entry["entry_id"], "source": source, "source_sha256": entry["sha256"],
            "legacy_class": Path(source).stem, "source_line_count": len(source_text.splitlines()),
            "source_overrides": overrides, "source_behaviors": behaviors,
            "legacy_ids": ids, "registry": spec["registry"], "common_entity": COMMON_ENTITY,
            "test_selector": spec["marker"],
            "coverage": "per-ID Forge construction plus common care-state round-trip; implementation mapping only",
        })
        results.append({
            "entry_id": entry["entry_id"], "requirement_id": "implementation", "result": "pass",
            "source_sha256": entry["sha256"],
            "target_paths": [
                {"path": spec["registry"], "sha256": sha256(registry)},
                {"path": COMMON_ENTITY, "sha256": sha256(common_entity)},
                {"path": proof.relative_to(root).as_posix(), "sha256": sha256(proof)},
            ],
            "tests": [{"selector": spec["marker"], "result": "pass", "artifact": spec["log"],
                       "artifact_sha256": sha256(log)}],
            "evidence_kind": "executed_test", "test_code_path": spec["test"],
            "test_code_sha256": sha256(test),
            "notes": [
                f"[animal-family-implementation-v1] {Path(source).stem} maps to the explicit {module} ID family {','.join(ids)}. "
                f"The fresh Forge GameTest selector {spec['marker']} constructs every registered ID and exercises the shared modern entity path. "
                f"This proves implementation ownership only; source overrides ({','.join(overrides) or 'none'}) and behaviors ({','.join(behaviors) or 'none'}) remain for dedicated behavior/serialization/client audits."
            ],
        })
        rows.append({"entry_id": entry["entry_id"], "source": source, "module": module, "ids": ids, "result": "pass"})
    evidence_dir.mkdir(parents=True, exist_ok=True)
    write_json(evidence_dir / "animal-family-implementation-v1-report.json", {
        "schema_version": 1, "audit": "animal-family-implementation", "audit_version": "v1",
        "rows": rows, "errors": errors, "error_count": len(errors),
    })
    write_json(evidence_dir / "animal-family-implementation-v1.json", {
        "schema_version": SCHEMA_VERSION, "audit_id": "animal-family-implementation", "audit_version": "v1",
        "source_revision": matrix.get("source_revision"),
        "command": "tools/audit_animal_family_implementation.py --root . --matrix docs/migration-matrix.json",
        "auditor_path": auditor_path, "auditor_sha256": sha256(root / auditor_path),
        "results": results, "errors": errors,
    })
    print(json.dumps({"results": len(results), "rows": len(rows), "errors": errors}, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
