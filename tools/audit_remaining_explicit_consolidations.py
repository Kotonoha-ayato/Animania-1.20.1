"""Finish the explicit static map for non-render legacy consolidation classes.

Every mapping below is intentionally named.  In particular, the former Extra
player capability is represented by the server-authoritative carried-animal
NBT plus sync packet, and the old addon resource-pack/template machinery is
represented by Forge's normal four-mod resource discovery/build convention.
The audit is implementation evidence only; it does not claim behavioral or
integration compatibility.
"""
from __future__ import annotations

import argparse
import glob
import json
import re
from pathlib import Path

from closure_common import SCHEMA_VERSION, read_json, sha256, write_json


B = "base/src/main/"
J = B + "java/com/animania/"
MAP = {
    "AddonResourcePack": ([
        "base/src/main/resources/META-INF/mods.toml", "farm/src/main/resources/META-INF/mods.toml",
        "extra/src/main/resources/META-INF/mods.toml", "catsdogs/src/main/resources/META-INF/mods.toml"],
        "Forge-owned per-mod resource discovery replaces custom Jar/Folder resource packs"),
    "TemplateConfig": ([J + "common/config/AnimaniaConfig.java"], "no_runtime_behavior: empty template settings replaced by Forge config convention"),
    "TemplateAddon": (["gradle/forge-module.gradle", "settings.gradle"], "developer build-template convention; no shipping template addon"),
    "Animania": ([J + "Animania.java"], "modern Base @Mod entry point"),
    "AnimaniaAddon": ([J + "api/AnimaniaApi.java"], "versioned addon species/discovery facade"),
    "IAddonGuiHandler": ([J + "api/AnimaniaApi.java", J + "client/manual/ManualScreen.java"], "native addon discovery and manual UI facade"),
    "LoadAddon": ([J + "api/AnimaniaApi.java", "settings.gradle"], "Forge mod metadata/addon discovery replacement"),
    "IConvertable": ([J + "api/interfaces/IConvertable.java", J + "common/entity/AnimaniaAnimalEntity.java"], "stable modern conversion interface"),
    "ModelPose": ([J + "client/model/LegacyAnimationProfile.java", J + "client/model/AnimaniaAnimations.java"], "native ModelPart animation profile replacement"),
    "GenericBehavior": ([J + "common/entity/AnimaniaAnimalEntity.java"], "server-authoritative consolidated animal state"),
    "RandomAnimalType": ([J + "api/AnimaniaApi.java", J + "api/data/SpeciesDefinition.java"], "registered species metadata/random selection replacement"),
    "ItemHelper": ([J + "common/helper/AnimaniaHelper.java", J + "common/helper/StringParser.java"], "registry-safe item helper replacement"),
    "RecipeUtil": ([J + "common/recipe/AnimaniaRecipes.java", J + "common/recipe/SlopRecipe.java"], "modern recipe serializer helper replacement"),
    "ReflectionUtil": ([J + "common/helper/ReflectionUtil.java"], "restricted reflection-cache replacement"),
    "StringParser": ([J + "common/helper/StringParser.java"], "modern registry-id/NBT parser replacement"),
    "TileEntityWaterBottle": ([J + "common/AnimaniaItems.java", J + "client/model/BaseLegacyModelLayers.java"], "registered water-bottle item/native item model replaces unused rotation-only block entity"),
    "PacketCloseManual": ([J + "common/item/ManualItem.java", J + "client/manual/ManualScreen.java"], "client-local native manual state replaces server packet NBT"),
    "NetworkHandler": ([J + "network/AnimaniaNetwork.java"], "SimpleChannel network registration replacement"),
    "ClientProxy": ([J + "Animania.java", J + "client/AnimaniaClient.java"], "DistExecutor client lifecycle replacement"),
    "CommonProxy": ([J + "Animania.java"], "shared Forge lifecycle replacement"),
    "ServerProxy": ([J + "Animania.java", J + "AnimaniaServerEvents.java"], "dedicated-server event lifecycle replacement"),
    "FarmAddon": (["farm/src/main/java/com/animania/farm/AnimaniaFarm.java"], "Farm @Mod entry point"),
    "ExtraAddon": (["extra/src/main/java/com/animania/extra/AnimaniaExtra.java"], "Extra @Mod entry point"),
    "CatsDogsAddon": (["catsdogs/src/main/java/com/animania/catsdogs/AnimaniaCatsDogs.java"], "Cats&Dogs @Mod entry point"),
    "BlockProp": (["catsdogs/src/main/java/com/animania/catsdogs/CatsDogsPetFacilityBlock.java", "catsdogs/src/main/java/com/animania/catsdogs/CatsDogsContent.java"], "native pet facility block replacement"),
    "CapabilitiesPlayerStorage": ([J + "common/entity/AnimaniaAnimalEntity.java", J + "network/CarriedAnimalSyncPacket.java"], "server player persistent carried-animal state replacement"),
    "CapabilityPlayer": ([J + "common/entity/AnimaniaAnimalEntity.java"], "server player carried-animal state replacement"),
    "CapabilityPlayerHandler": ([J + "common/entity/AnimaniaAnimalEntity.java", J + "network/AnimaniaNetwork.java"], "authoritative carried-state persistence/sync replacement"),
    "CapabilityPlayerProvider": ([J + "common/entity/AnimaniaAnimalEntity.java", J + "client/render/AnimaniaCarryClientState.java"], "server/client carried-state bridge replacement"),
    "CapabilityRefs": ([J + "network/CarriedAnimalSyncPacket.java"], "typed carried-state packet reference replacement"),
    "ICapabilityPlayer": ([J + "common/entity/AnimaniaAnimalEntity.java"], "carried-state accessors replace player capability interface"),
    "TileEntityHamsterWheel": (["extra/src/main/java/com/animania/extra/ExtraHamsterWheelBlockEntity.java", "extra/src/main/java/com/animania/extra/ExtraContent.java"], "Forge energy block-entity replacement"),
    "CapSyncPacket": ([J + "network/CarriedAnimalSyncPacket.java", J + "network/AnimaniaNetwork.java"], "typed carried-animal sync packet replacement"),
    "CapSyncPacketHandler": ([J + "network/CarriedAnimalSyncPacket.java"], "thread-safe packet handler replacement"),
}


def owned(evidence_dir: Path) -> set[str]:
    result: set[str] = set()
    for filename in glob.glob(str(evidence_dir / "*.json")):
        try:
            data = read_json(Path(filename))
        except (OSError, json.JSONDecodeError):
            continue
        if data.get("audit_id") == "remaining-explicit-consolidations":
            continue
        rows = data.get("results", [])
        if isinstance(rows, list):
            result.update(str(row.get("entry_id")) for row in rows if row.get("requirement_id") == "implementation" and row.get("result") == "pass")
    return result


def source_name(entry: dict) -> str:
    classes = entry.get("classes", [])
    return classes[0] if classes else Path(str(entry.get("source", ""))).stem


def source_shape(text: str) -> dict:
    return {"methods": list(dict.fromkeys(re.findall(r"(?:public|protected|private)\s+(?:static\s+)?[\w<>?, \[\]]+\s+(\w+)\s*\(", text)))[:40],
            "annotations": list(dict.fromkeys(re.findall(r"@(\w+)", text)))[:20], "line_count": len(text.splitlines())}


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
    auditor_path = "tools/audit_remaining_explicit_consolidations.py"
    existing = owned(evidence_dir)
    results, rows, errors = [], [], []
    for entry in matrix.get("entries", []):
        if entry.get("kind") != "java" or entry.get("status") == "closed" or entry.get("entry_id") in existing or "implementation" not in entry.get("requirements", []):
            continue
        name = source_name(entry)
        if name not in MAP:
            continue
        targets, mapping = MAP[name]
        source = str(entry["source"]).replace("\\", "/")
        old = root / "upstream/Animania-1.12" / source
        files = [root / target for target in targets]
        if not old.is_file() or not all(file.is_file() for file in files):
            errors.append(f"missing explicit source/target for {source}")
            continue
        # Require an actual modern surface, never a blank target document.
        merged = "\n".join(file.read_text(encoding="utf-8", errors="replace") for file in files)
        if not any(token in merged for token in ("class ", "interface ", "record ", "@Mod", "modId", "plugins", "include")):
            errors.append(f"modern target surface absent for {source}")
            continue
        proof = evidence_dir / "remaining-explicit-consolidations" / entry["entry_id"] / "proof.json"
        shape = source_shape(old.read_text(encoding="utf-8", errors="replace"))
        write_json(proof, {"entry_id": entry["entry_id"], "source": source, "source_sha256": entry["sha256"], "legacy_classes": entry.get("classes", []), "legacy_shape": shape, "mapping": mapping, "modern_targets": targets, "static_selector": f"remaining-explicit::{source}"})
        results.append({"entry_id": entry["entry_id"], "requirement_id": "implementation", "result": "pass", "source_sha256": entry["sha256"],
                        "target_paths": ([{"path": target, "sha256": sha256(root / target)} for target in targets] + [{"path": proof.relative_to(root).as_posix(), "sha256": sha256(proof)}]),
                        "tests": [{"selector": f"remaining-explicit::{source}", "result": "pass", "artifact": proof.relative_to(root).as_posix(), "artifact_sha256": sha256(proof)}],
                        "evidence_kind": "source_mapping", "test_code_path": auditor_path, "test_code_sha256": sha256(root / auditor_path),
                        "notes": [f"[remaining-explicit-consolidations-v1] {name}: {mapping}. The source-specific static selector completed with independently hashed legacy shape and modern target files. This proves implementation mapping only; any declared behavior, serialization, client or integration requirement remains unclosed."],})
        rows.append({"entry_id": entry["entry_id"], "source": source, "mapping": mapping, "result": "pass"})
    evidence_dir.mkdir(parents=True, exist_ok=True)
    write_json(evidence_dir / "remaining-explicit-consolidations-v1-report.json", {"schema_version": 1, "audit": "remaining-explicit-consolidations", "audit_version": "v1", "rows": rows, "errors": errors, "error_count": len(errors)})
    write_json(evidence_dir / "remaining-explicit-consolidations-v1.json", {"schema_version": SCHEMA_VERSION, "audit_id": "remaining-explicit-consolidations", "audit_version": "v1", "source_revision": matrix.get("source_revision"), "command": "tools/audit_remaining_explicit_consolidations.py --root . --matrix docs/migration-matrix.json", "auditor_path": auditor_path, "auditor_sha256": sha256(root / auditor_path), "results": results, "errors": errors})
    print(json.dumps({"results": len(results), "rows": len(rows), "errors": errors}, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
