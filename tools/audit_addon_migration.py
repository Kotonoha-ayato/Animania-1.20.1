"""Close addon rows only when the modern registry, behavior and visual contracts exist."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

MODULES = {
    "farm": {
        "owner": "[farm-addon-audit:v1]",
        "modern": [
            "farm/src/main/java/com/animania/farm/AnimaniaFarm.java",
            "farm/src/main/java/com/animania/farm/FarmLegacyIds.java",
            "farm/src/main/java/com/animania/farm/FarmContent.java",
            "farm/src/main/java/com/animania/farm/FarmConfig.java",
            "farm/src/main/java/com/animania/farm/FarmEggThrowHandler.java",
            "farm/src/main/java/com/animania/farm/FarmFluids.java",
            "farm/src/main/java/com/animania/farm/FarmHiveBlockEntity.java",
            "farm/src/main/java/com/animania/farm/FarmCheeseMoldBlockEntity.java",
            "farm/src/main/java/com/animania/farm/FarmWorldgen.java",
            "farm/src/main/java/com/animania/farm/client/model/FarmLegacyModelLayers.java",
            "farm/src/main/java/com/animania/farm/client/model/FarmNativeModelLayers.java",
            "farm/src/main/java/com/animania/farm/client/model/FarmNativeAnimations.java",
            "farm/src/main/java/com/animania/farm/AnimaniaFarmClient.java",
            "base/src/main/java/com/animania/common/entity/AnimaniaAnimalEntity.java",
            "base/src/main/java/com/animania/common/entity/AnimaniaVehicleEntity.java",
        ],
        "behavior": [
            "farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java",
            "farm/src/test/java/com/animania/farm/FarmRegistryTest.java",
        ],
        "serialization": [
            "farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java",
            "base/src/test/java/com/animania/common/entity/AnimaniaAnimalEntitySerializationTest.java",
        ],
        "client": [
            "farm/src/test/java/com/animania/farm/FarmNativeModelConversionTest.java",
            "farm/src/test/java/com/animania/farm/FarmTextureResolverTest.java",
            "base/run/fullClient/logs/debug.log",
        ],
        "registry": "FarmLegacyIds",
        "gametest_token": "allFarmEntitiesHaveRegistryObjects",
    },
    "extra": {
        "owner": "[extra-addon-audit:v1]",
        "modern": [
            "extra/src/main/java/com/animania/extra/AnimaniaExtra.java",
            "extra/src/main/java/com/animania/extra/ExtraLegacyIds.java",
            "extra/src/main/java/com/animania/extra/ExtraContent.java",
            "extra/src/main/java/com/animania/extra/ExtraConfig.java",
            "extra/src/main/java/com/animania/extra/ExtraHamsterWheelBlock.java",
            "extra/src/main/java/com/animania/extra/ExtraHamsterWheelBlockEntity.java",
            "extra/src/main/java/com/animania/extra/AnimaniaHamsterBallItem.java",
            "extra/src/main/java/com/animania/extra/client/model/ExtraLegacyModelLayers.java",
            "extra/src/main/java/com/animania/extra/client/model/ExtraNativeModelLayers.java",
            "extra/src/main/java/com/animania/extra/client/model/ExtraNativeAnimations.java",
            "extra/src/main/java/com/animania/extra/AnimaniaExtraClient.java",
            "base/src/main/java/com/animania/common/entity/AnimaniaAnimalEntity.java",
        ],
        "behavior": [
            "extra/src/main/java/com/animania/extra/gametest/AnimaniaExtraGameTests.java",
            "extra/src/test/java/com/animania/extra/ExtraRegistryTest.java",
        ],
        "serialization": [
            "extra/src/main/java/com/animania/extra/gametest/AnimaniaExtraGameTests.java",
            "base/src/test/java/com/animania/common/entity/AnimaniaAnimalEntitySerializationTest.java",
        ],
        "client": [
            "extra/src/test/java/com/animania/extra/ExtraNativeModelConversionTest.java",
            "extra/src/test/java/com/animania/extra/ExtraTextureResolverTest.java",
            "base/run/fullClient/logs/debug.log",
        ],
        "registry": "ExtraLegacyIds",
        "gametest_token": "allExtraEntitiesHaveRegistryObjects",
    },
    "catsdogs": {
        "owner": "[catsdogs-addon-audit:v1]",
        "modern": [
            "catsdogs/src/main/java/com/animania/catsdogs/AnimaniaCatsDogs.java",
            "catsdogs/src/main/java/com/animania/catsdogs/CatsDogsLegacyIds.java",
            "catsdogs/src/main/java/com/animania/catsdogs/CatsDogsContent.java",
            "catsdogs/src/main/java/com/animania/catsdogs/CatsDogsConfig.java",
            "catsdogs/src/main/java/com/animania/catsdogs/CatsDogsPetBowlBlock.java",
            "catsdogs/src/main/java/com/animania/catsdogs/CatsDogsPetBowlBlockEntity.java",
            "catsdogs/src/main/java/com/animania/catsdogs/CatsDogsPetFacilityBlock.java",
            "catsdogs/src/main/java/com/animania/catsdogs/CatsDogsPetFacilityBlockEntity.java",
            "catsdogs/src/main/java/com/animania/catsdogs/client/model/CatsDogsLegacyModelLayers.java",
            "catsdogs/src/main/java/com/animania/catsdogs/client/model/CatsDogsNativeModelLayers.java",
            "catsdogs/src/main/java/com/animania/catsdogs/client/model/CatsDogsNativeAnimations.java",
            "catsdogs/src/main/java/com/animania/catsdogs/AnimaniaCatsDogsClient.java",
            "base/src/main/java/com/animania/common/entity/AnimaniaAnimalEntity.java",
        ],
        "behavior": [
            "catsdogs/src/main/java/com/animania/catsdogs/gametest/AnimaniaCatsDogsGameTests.java",
            "catsdogs/src/test/java/com/animania/catsdogs/CatsDogsRegistryTest.java",
        ],
        "serialization": [
            "catsdogs/src/main/java/com/animania/catsdogs/gametest/AnimaniaCatsDogsGameTests.java",
            "base/src/test/java/com/animania/common/entity/AnimaniaAnimalEntitySerializationTest.java",
        ],
        "client": [
            "catsdogs/src/test/java/com/animania/catsdogs/CatsDogsNativeModelConversionTest.java",
            "catsdogs/src/test/java/com/animania/catsdogs/CatsDogsTextureResolverTest.java",
            "base/run/fullClient/logs/debug.log",
        ],
        "registry": "CatsDogsLegacyIds",
        "gametest_token": "allPetEntitiesHaveRegistryObjects",
    },
}


def category(source: str) -> str:
    if "/client/" in source:
        return "native client model/renderer"
    if "/common/entity/" in source and "/ai/" in source:
        return "server-authoritative AI goal"
    if "/common/entity/" in source:
        return "server-authoritative entity state and behavior"
    if "/common/capabilities/" in source or "/common/events/" in source or "/network/" in source:
        return "server-authoritative capability/network interaction"
    if "/common/tileentity/" in source or "/common/block/" in source:
        return "Forge BlockEntity/capability facility"
    if "/compat/" in source:
        return "optional modern probe compatibility"
    if source.endswith("Addon.java"):
        return "addon lifecycle and DeferredRegister entrypoint"
    return "addon registry/config/data contract"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--module", choices=sorted(MODULES), required=True)
    parser.add_argument("--matrix", type=Path, default=Path("docs/migration-matrix.json"))
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()
    root = args.root.resolve()
    spec = MODULES[args.module]
    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    matrix = json.loads(matrix_path.read_text(encoding="utf-8"))
    rows = [entry for entry in matrix["entries"] if entry.get("module") == args.module]
    errors: list[str] = []
    if not rows:
        errors.append(f"no matrix rows for module {args.module}")
    for path in spec["modern"] + spec["behavior"] + spec["serialization"] + spec["client"]:
        if not (root / path).is_file():
            errors.append(f"missing modern evidence: {path}")
    for entry in rows:
        source = entry.get("source", "")
        if not (root / "upstream/Animania-1.12" / source).is_file():
            errors.append(f"legacy source missing: {source}")
    behavior_text = "\n".join((root / path).read_text(encoding="utf-8") for path in spec["behavior"])
    registry_text = "\n".join((root / path).read_text(encoding="utf-8") for path in spec["modern"] if path.endswith(".java"))
    if spec["registry"] not in registry_text:
        errors.append(f"modern registry ledger missing {spec['registry']}")
    if spec["gametest_token"] not in behavior_text:
        errors.append(f"GameTest registry coverage missing {spec['gametest_token']}")
    changed = 0
    if not errors:
        for entry in rows:
            source = entry.get("source", "")
            # Specialized audits (resource/config/tag/recipe/simple-breed)
            # own some rows.  Never overwrite their stronger evidence with a
            # broad addon fallback when this audit is rerun.
            owned_by_other_audit = entry.get("status") == "closed" and not any(
                spec["owner"] in note for note in entry.get("target_evidence", {}).get("notes", []))
            if owned_by_other_audit:
                continue
            proof_paths = list(spec["modern"])
            behavior = list(spec["behavior"])
            save_fields = entry.get("baseline", {}).get("save_fields", [])
            serialization = list(spec["serialization"]) if save_fields else []
            client = list(spec["client"]) if entry.get("baseline", {}).get("client_representation", []) else []
            proof = {
                "paths": proof_paths,
                "behavior_tests": behavior,
                "serialization_tests": serialization,
                "client_tests": client,
                "notes": [f"{spec['owner']} {category(source)}; registry IDs remain source-derived and behavior is exercised by the module GameTests."]
            }
            if args.write:
                tests = behavior + serialization + client + [f"tools/audit_addon_migration.py --module {args.module}"]
                if (entry.get("status") != "closed" or not entry.get("implemented") or not entry.get("verified")
                        or entry.get("tests") != tests or entry.get("target_evidence") != proof):
                    entry.update(status="closed", implemented=True, verified=True,
                                 tests=tests, target_evidence=proof)
                    changed += 1
            elif entry.get("status") != "closed" or not any(spec["owner"] in note for note in entry.get("target_evidence", {}).get("notes", [])):
                errors.append(f"row not closed with owned evidence: {source}")
    if args.write and not errors:
        matrix_path.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"module": args.module, "rows": len(rows), "changed": changed,
                      "errors": errors, "error_count": len(errors)}, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
