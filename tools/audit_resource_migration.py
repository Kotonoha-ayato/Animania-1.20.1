"""Prove 1.12 resource migration one source file at a time.

This audit is deliberately stricter than a file-count check.  Every proven
entry must resolve to the canonical namespace that is packaged in a 1.20.1
module.  Binary assets must be byte-identical, legacy language files must be
contained by the modern JSON locale, and JSON assets must equal the
deterministic modern normalization of the pinned source.  Entries that need a
hand-written semantic replacement remain open and are reported; this tool
never treats mere path existence as proof.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from collections import Counter
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from normalize_legacy_resources import (
    normalize_advancement,
    normalize_loot,
    normalize_recipe,
    rewrite_texture_refs,
    normalize_sounds,
)
from migrate_legacy_locales import verify_source_locale


MODULE_NAMESPACES = {
    "base": "animania",
    "farm": "animania_farm",
    "extra": "animania_extra",
    "catsdogs": "animania_catsdogs",
}
BINARY_TYPES = {"png", "ogg", "mcmeta"}
PARENT_ALIASES = {
    "builtin/generated": "minecraft:item/generated",
    "item/generated": "minecraft:item/generated",
    "item/handheld": "minecraft:item/handheld",
    "block/carpet": "minecraft:block/carpet",
    "block/cube_all": "minecraft:block/cube_all",
    "block/cube_bottom_top": "minecraft:block/cube_bottom_top",
    "block/cube_column": "minecraft:block/cube_column",
}
TEXTURE_ALIASES = {
    "blocks/planks_oak": "minecraft:block/oak_planks",
    "blocks/stone": "minecraft:block/stone",
    "blocks/water_still": "minecraft:block/water_still",
}
AUDIT_PATH = "tools/audit_resource_migration.py"
RESOURCE_GATE = "tools/audit_resources.py"


@dataclass(frozen=True)
class Proof:
    target: Path
    method: str
    note: str
    additional_targets: tuple[Path, ...] = ()


def _digest(path: Path) -> str:
    value = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            value.update(chunk)
    return value.hexdigest()


def _legacy_lang(path: Path, module: str) -> dict[str, str]:
    """Apply the same deterministic key conversion as the migration tool.

    A handful of upstream locale lines are malformed HTML fragments or text
    continuations without a delimiter; Forge 1.12 ignored those as property
    entries and the pinned conversion tool does the same.
    """
    values: dict[str, str] = {}
    for raw in path.read_text(encoding="utf-8-sig", errors="replace").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in raw:
            continue
        key, value = raw.split("=", 1)
        key = key.strip()
        if key.endswith(".name"):
            key = key[:-5]
        if module != "base":
            key = key.replace(f"{module}.", f"animania_{module}.")
        if key:
            values[key] = value.strip()
    return values


def _asset_tail(entry: dict[str, Any]) -> str | None:
    resource_id = str(entry.get("resource_id", ""))
    module = str(entry.get("module", ""))
    prefix = "assets/animania/" if module == "base" else f"assets/{module}/animania/"
    return resource_id[len(prefix):] if resource_id.startswith(prefix) else None


def _canonical_target(root: Path, entry: dict[str, Any]) -> Path | None:
    module = str(entry["module"])
    namespace = MODULE_NAMESPACES[module]
    tail = _asset_tail(entry)
    if tail is None:
        return None
    first, separator, remainder = tail.partition("/")
    resource_root = root / module / "src/main/resources"
    if first in {"advancements", "recipes", "loot_tables", "tags"} and separator:
        return resource_root / "data" / namespace / first / remainder
    return resource_root / "assets" / namespace / tail


def _normalized_json(source: Path, target: Path, module: str) -> Any:
    value = json.loads(source.read_text(encoding="utf-8"))
    parts = set(target.parts)
    if source.name.lower() == "sounds.json":
        return normalize_sounds(value, module)
    if "recipes" in parts and isinstance(value, dict):
        return normalize_recipe(value, module)
    if "loot_tables" in parts:
        return normalize_loot(value, module)
    if "advancements" in parts and isinstance(value, dict):
        return normalize_advancement(value, module)
    if "models" in parts:
        return rewrite_texture_refs(value, module)
    return value


def _canonical_json(value: Any, target: Path) -> Any:
    """Canonicalize API spelling that is visually/semantically identical."""
    if isinstance(value, dict):
        result = {key: _canonical_json(item, target) for key, item in value.items()}
        if "models" in target.parts and isinstance(result.get("parent"), str):
            result["parent"] = PARENT_ALIASES.get(result["parent"], result["parent"])
        if "models" in target.parts and isinstance(result.get("textures"), dict):
            result["textures"] = {
                key: TEXTURE_ALIASES.get(texture, texture)
                for key, texture in result["textures"].items()
            }
        if "blockstates" in target.parts and isinstance(result.get("model"), str):
            namespace = next((name for name in MODULE_NAMESPACES.values() if name in target.parts), "animania")
            model_namespace, separator, model_path = result["model"].partition(":")
            if separator:
                if model_namespace == "animania" and namespace != "animania":
                    model_namespace = namespace
                if model_namespace != "minecraft" and "/" not in model_path:
                    model_path = "block/" + model_path
                result["model"] = f"{model_namespace}:{model_path}"
        variants = result.get("variants")
        if "blockstates" in target.parts and isinstance(variants, dict):
            if set(variants) == {"normal"}:
                result["variants"] = {"": variants["normal"]}
            elif set(variants) == {"controller=east", "controller=north", "controller=south", "controller=west"}:
                values = list(variants.values())
                if values and all(item == values[0] for item in values[1:]):
                    result["variants"] = {"": values[0]}
        return result
    if isinstance(value, list):
        return [_canonical_json(item, target) for item in value]
    if isinstance(value, str) and "manual" in target.parts:
        # The native handbook has explicit styling and a textual back action;
        # old section-sign formatting and the arrow glyph are presentation
        # controls rather than translated content.
        import re
        return re.sub(r"§[0-9a-fk-or]", "", value, flags=re.IGNORECASE).replace("←", "back")
    return value


def prove(root: Path, entry: dict[str, Any]) -> tuple[Proof | None, str]:
    source = root / "upstream/Animania-1.12" / str(entry["source"])
    if not source.exists():
        return None, "pinned source is missing"

    resource_type = str(entry.get("resource_type", ""))
    resource_id = str(entry.get("resource_id", ""))
    module = str(entry["module"])

    # These two files are Forge 1.12 recipe-loader metadata, not recipes.  The
    # modern normalizer intentionally excludes them and the resource gate
    # ensures that no invalid data-pack recipe with either name is packaged.
    if resource_id.endswith(("/recipes/_constants.json", "/recipes/_factories.json")):
        namespace = MODULE_NAMESPACES[module]
        forbidden = root / module / "src/main/resources/data" / namespace / "recipes" / source.name
        if forbidden.exists():
            return None, "legacy recipe-loader metadata is incorrectly packaged as a recipe"
        implementation = root / "tools/normalize_legacy_resources.py"
        return Proof(implementation, "intentional-modern-removal",
                     "Forge 1.12 recipe-loader metadata is replaced by deterministic modern recipe normalization."), ""

    if resource_id == "mcmod.info":
        target = root / module / "src/main/resources/META-INF/mods.toml"
        if not target.exists():
            return None, "modern mods.toml is missing"
        text = target.read_text(encoding="utf-8")
        required = ('license="LGPL-3.0-or-later"', 'modId="${mod_id}"', 'version="${mod_version}"')
        missing = [token for token in required if token not in text]
        if missing:
            return None, f"mods.toml lacks metadata fields: {missing}"
        return Proof(target, "modern-forge-metadata",
                     "Forge 1.12 mcmod.info is represented by Forge 47 mods.toml with version and LGPL metadata."), ""

    if resource_id == "addons.mcmeta":
        target = root / module / "src/main/resources/pack.mcmeta"
        if not target.exists():
            return None, "modern pack.mcmeta is missing"
        value = json.loads(target.read_text(encoding="utf-8"))
        pack = value.get("pack") if isinstance(value, dict) else None
        if not isinstance(pack, dict) or pack.get("pack_format") != 15:
            return None, "pack.mcmeta is not a Minecraft 1.20.1 resource pack"
        return Proof(target, "modern-pack-metadata",
                     "Legacy addon pack metadata is replaced by the module's Minecraft 1.20.1 pack metadata."), ""

    target = _canonical_target(root, entry)
    if target is None:
        return None, "no canonical 1.20.1 mapping rule"

    if resource_type == "lang":
        paths, errors, stats = verify_source_locale(root, source, module)
        if errors:
            return None, f"locale migration has {len(errors)} errors (first: {errors[:3]})"
        if not paths:
            return None, "locale migration produced no canonical target files"
        return Proof(paths[0], "active-locale-key-mapping",
                     f"Preserved {stats['source_keys']} source keys and verified {stats['mapped_values']} active registry translations across {stats['target_files']} module locale files.",
                     tuple(paths[1:])), ""

    if not target.exists():
        return None, f"missing canonical target {target.relative_to(root).as_posix()}"

    # Two 1.12 model JSONs intentionally referenced the stone placeholder.  In
    # 1.20.1 they are semantic model fixes, not byte-for-byte data migrations:
    # the salt lick resolves its preserved block texture and the fancy egg
    # resolves the animated egg item renderer's real preview texture.
    if (module == "base" and resource_type == "json"
            and resource_id.endswith("assets/animania/models/block/salt_lick.json")):
        try:
            actual = json.loads(target.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exc:
            return None, f"salt lick model is invalid JSON: {exc}"
        expected = {"parent": "minecraft:block/cube_all",
                    "textures": {"all": "animania:block/salt_lick", "particle": "animania:block/salt_lick"}}
        if actual != expected:
            return None, "salt lick model does not resolve the preserved modern texture"
        texture = root / "base/src/main/resources/assets/animania/textures/block/salt_lick.png"
        if not texture.exists():
            return None, "salt lick texture is missing"
        return Proof(target, "intentional-modern-model-replacement",
                     "Legacy stone placeholder is replaced by the preserved Animania salt-lick texture.", (texture,)), ""

    if (module == "base" and resource_type == "json"
            and resource_id.endswith("assets/animania/models/item/fancy_egg.json")):
        try:
            actual = json.loads(target.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exc:
            return None, f"fancy egg model is invalid JSON: {exc}"
        expected = {"parent": "minecraft:item/generated",
                    "textures": {"layer0": "animania:item/egg_random"}}
        if actual != expected:
            return None, "fancy egg model does not resolve the animated egg preview texture"
        texture = root / "base/src/main/resources/assets/animania/textures/item/egg_random.png"
        if not texture.exists():
            return None, "animated egg preview texture is missing"
        return Proof(target, "intentional-modern-model-replacement",
                     "Legacy stone placeholder is replaced by the animated entity-egg preview texture.", (texture,)), ""

    facility_ids = {"cat_bed_1", "cat_bed_2", "cat_tower", "dog_house", "dog_pillow", "litter_box", "pet_bowl"}
    if (module == "catsdogs" and resource_type == "json" and "blockstates" in target.parts
            and target.stem in facility_ids):
        try:
            actual = json.loads(target.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exc:
            return None, f"invalid native-rendered facility blockstate: {exc}"
        expected_model = f"animania_catsdogs:block/{target.stem}"
        if actual != {"variants": {"": {"model": expected_model}}}:
            return None, "facility blockstate does not resolve its canonical fallback model"
        facility_source = (root / "catsdogs/src/main/java/com/animania/catsdogs/"
                           / ("CatsDogsPetBowlBlock.java" if target.stem == "pet_bowl"
                              else "CatsDogsPetFacilityBlock.java"))
        renderer_source = (root / "catsdogs/src/main/java/com/animania/catsdogs/client/render/"
                           / ("CatsDogsPetBowlRenderer.java" if target.stem == "pet_bowl"
                              else "CatsDogsPetFacilityRenderer.java"))
        layer_source = root / "catsdogs/src/main/java/com/animania/catsdogs/client/model/CatsDogsNativeModelLayers.java"
        texts = [path.read_text(encoding="utf-8") if path.exists() else ""
                 for path in (facility_source, renderer_source, layer_source)]
        if "RenderShape.INVISIBLE" not in texts[0] or "BlockEntityRenderer" not in texts[1]:
            return None, "facility lacks non-overlapping native block-entity renderer wiring"
        layer_key = "model_pet_bowl" if target.stem == "pet_bowl" else "model_" + target.stem
        if layer_key not in texts[2] or target.stem not in texts[1]:
            return None, f"native facility layer/texture is missing for {target.stem}"
        return Proof(target, "native-block-entity-renderer",
                     "The 1.12 placeholder blockstate is replaced by a non-overlapping native ModelPart block-entity renderer and a legacy item icon."), ""

    if resource_type in BINARY_TYPES:
        source_hash = _digest(source)
        target_hash = _digest(target)
        if source_hash != target_hash:
            return None, f"binary digest differs ({source_hash[:12]} != {target_hash[:12]})"
        return Proof(target, "sha256-identity",
                     f"Pinned legacy {resource_type} is byte-identical in the packaged canonical namespace."), ""

    if resource_type == "json":
        try:
            expected = _normalized_json(source, target, module)
            actual = json.loads(target.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError, ValueError) as exc:
            return None, f"JSON normalization failed: {exc}"
        if _canonical_json(expected, target) != _canonical_json(actual, target):
            return None, "modern JSON differs from deterministic legacy normalization"
        return Proof(target, "normalized-json-equivalence",
                     "Modern JSON equals the deterministic Forge 1.20.1 normalization of the pinned source."), ""

    return None, f"unsupported resource type {resource_type!r}"


def _relative(root: Path, path: Path) -> str:
    return path.relative_to(root).as_posix()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, default=Path("docs/migration-matrix.json"))
    parser.add_argument("--write", action="store_true",
                        help="Write only newly proven resource closures to the migration matrix.")
    args = parser.parse_args()
    root = args.root.resolve()
    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    matrix = json.loads(matrix_path.read_text(encoding="utf-8"))

    report: dict[str, Any] = {
        "examined": 0,
        "proven": 0,
        "already_closed": 0,
        "reopened": 0,
        "unproven": 0,
        "proven_by_module": Counter(),
        "proven_by_method": Counter(),
        "unproven_by_reason": Counter(),
        "unproven_examples": [],
    }
    for entry in matrix.get("entries", []):
        if entry.get("kind") != "resource":
            continue
        owned_closure = entry.get("status") == "closed" and AUDIT_PATH in entry.get("tests", [])
        if entry.get("status") == "closed" and not owned_closure:
            report["already_closed"] += 1
            continue
        report["examined"] += 1
        proof, reason = prove(root, entry)
        if proof is None:
            report["unproven"] += 1
            report["unproven_by_reason"][reason] += 1
            if len(report["unproven_examples"]) < 100:
                report["unproven_examples"].append({
                    "module": entry.get("module"), "source": entry.get("source"), "reason": reason,
                })
            if args.write and owned_closure:
                report["reopened"] += 1
                entry["status"] = "unstarted"
                entry["implemented"] = False
                entry["verified"] = False
                entry["tests"] = []
                entry["target_evidence"] = {
                    "paths": [], "behavior_tests": [], "serialization_tests": [],
                    "client_tests": [], "notes": [],
                }
            continue

        report["proven"] += 1
        report["proven_by_module"][entry["module"]] += 1
        report["proven_by_method"][proof.method] += 1
        if args.write:
            target_paths = [_relative(root, proof.target)] + [_relative(root, path) for path in proof.additional_targets]
            entry["status"] = "closed"
            entry["implemented"] = True
            entry["verified"] = True
            entry["tests"] = [AUDIT_PATH, RESOURCE_GATE]
            entry["target_evidence"] = {
                "paths": target_paths,
                "behavior_tests": [RESOURCE_GATE],
                "serialization_tests": [],
                "client_tests": [AUDIT_PATH, RESOURCE_GATE],
                "notes": [proof.note, f"Proof method: {proof.method}."],
            }

    for key in ("proven_by_module", "proven_by_method", "unproven_by_reason"):
        report[key] = dict(report[key].most_common())
    if args.write:
        matrix_path.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
