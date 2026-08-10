"""Strict resource/data audit used by the release gate."""
from __future__ import annotations

import argparse
import json
import re
import struct
from pathlib import Path

MODULES = {"base": "animania", "farm": "animania_farm", "extra": "animania_extra", "catsdogs": "animania_catsdogs"}
ENTITY_SOURCES = {
    "farm": "farm/src/main/java/com/animania/farm/FarmLegacyIds.java",
    "extra": "extra/src/main/java/com/animania/extra/ExtraLegacyIds.java",
    "catsdogs": "catsdogs/src/main/java/com/animania/catsdogs/CatsDogsLegacyIds.java",
}
CONTENT_SOURCES = {
    "farm": "farm/src/main/java/com/animania/farm/FarmContent.java",
    "extra": "extra/src/main/java/com/animania/extra/ExtraContent.java",
    "catsdogs": "catsdogs/src/main/java/com/animania/catsdogs/CatsDogsContent.java",
}
RECIPE_TYPES = {
    "minecraft:crafting_shaped", "minecraft:crafting_shapeless", "minecraft:smelting",
    "minecraft:smoking", "minecraft:campfire_cooking", "minecraft:stonecutting",
    "minecraft:blasting", "minecraft:smithing_transform", "minecraft:smithing_trim",
}
VANILLA_LOOT_FUNCTIONS = {
    "minecraft:set_count", "minecraft:looting_enchant", "minecraft:furnace_smelt",
}
VANILLA_ADVANCEMENT_TRIGGERS = {
    "minecraft:impossible", "minecraft:tick", "minecraft:inventory_changed",
    "minecraft:recipe_crafted", "minecraft:player_killed_entity", "minecraft:entity_killed_player",
    "minecraft:player_hurt_entity", "minecraft:used_totem", "minecraft:consume_item",
    "minecraft:location", "minecraft:placed_block", "minecraft:enchanted_item",
}
SUPPORTED_ADVANCEMENT_TRIGGERS = VANILLA_ADVANCEMENT_TRIGGERS | {"animania:feed_animal"}


def _validate_ingredient(value: object, location: str, errors: list[str]) -> None:
    """Require modern item/tag ingredient objects (including shaped keys)."""
    if isinstance(value, list):
        for index, child in enumerate(value):
            _validate_ingredient(child, f"{location}[{index}]", errors)
        return
    if not isinstance(value, dict) or not (isinstance(value.get("item"), str) or isinstance(value.get("tag"), str)):
        errors.append(f"{location}: invalid ingredient {value!r}")


def _validate_recipe(data: dict, location: str, errors: list[str]) -> None:
    recipe_type = data.get("type")
    if recipe_type not in RECIPE_TYPES:
        errors.append(f"unsupported recipe type {recipe_type} in {location}")
    ingredients = data.get("ingredients")
    if ingredients is not None:
        _validate_ingredient(ingredients, f"{location}:ingredients", errors)
    key = data.get("key")
    if isinstance(key, dict):
        for symbol, ingredient in key.items():
            _validate_ingredient(ingredient, f"{location}:key.{symbol}", errors)
    result = data.get("result")
    if not isinstance(result, dict) or not isinstance(result.get("item"), str):
        errors.append(f"{location}: result must contain an item ID")


def _validate_loot(data: object, location: str, errors: list[str]) -> None:
    if isinstance(data, dict):
        function = data.get("function")
        if function is not None and function not in VANILLA_LOOT_FUNCTIONS:
            errors.append(f"{location}: unsupported loot function {function}")
        for key, value in data.items():
            _validate_loot(value, f"{location}.{key}", errors)
    elif isinstance(data, list):
        for index, value in enumerate(data):
            _validate_loot(value, f"{location}[{index}]", errors)


def _validate_advancement(data: object, location: str, errors: list[str]) -> None:
    if isinstance(data, dict):
        trigger = data.get("trigger")
        if trigger == "minecraft:tick":
            errors.append(f"{location}: unconditional tick criterion is forbidden")
        elif trigger is not None and trigger not in SUPPORTED_ADVANCEMENT_TRIGGERS:
            errors.append(f"{location}: unsupported advancement trigger {trigger}")
        for key, value in data.items():
            _validate_advancement(value, f"{location}.{key}", errors)
    elif isinstance(data, list):
        for index, value in enumerate(data):
            _validate_advancement(value, f"{location}[{index}]", errors)


def _legacy_entity_ids(root: Path, module: str) -> set[str]:
    source = root / ENTITY_SOURCES[module]
    if not source.exists():
        return set()
    text = source.read_text(encoding="utf-8")
    body = text.split("List.of(", 1)[1].split(");", 1)[0]
    return set(re.findall(r'"([a-z0-9_]+)"', body))


def _java_list(root: Path, source: str, field: str) -> set[str]:
    text = (root / source).read_text(encoding="utf-8")
    match = re.search(field + r"\s*=\s*List\.of\((.*?)\);", text, re.S)
    return set(re.findall(r'"([a-z0-9_]+)"', match.group(1))) if match else set()


def _validate_biome_modifier(data: object, location: str, namespace: str, errors: list[str]) -> None:
    if not isinstance(data, dict) or data.get("type") != "forge:add_spawns":
        errors.append(f"{location}: biome modifier must be forge:add_spawns")
        return
    biomes = data.get("biomes")
    if not isinstance(biomes, (str, list)):
        errors.append(f"{location}: biome modifier missing biomes tag/list")
    spawners = data.get("spawners")
    if not isinstance(spawners, list) or not spawners:
        errors.append(f"{location}: biome modifier must contain spawners")
        return
    for index, spawner in enumerate(spawners):
        if not isinstance(spawner, dict):
            errors.append(f"{location}:spawners[{index}] is not an object")
            continue
        entity_type = spawner.get("type")
        if not isinstance(entity_type, str) or ":" not in entity_type:
            errors.append(f"{location}:spawners[{index}] missing namespaced type")
        for key in ("weight", "minCount", "maxCount"):
                if not isinstance(spawner.get(key), int) or spawner[key] <= 0:
                    errors.append(f"{location}:spawners[{index}] invalid {key}")


def _validate_png(path: Path, location: str, errors: list[str]) -> None:
    """Validate the PNG signature/header without requiring an image library."""
    try:
        header = path.read_bytes()[:24]
    except OSError as exc:
        errors.append(f"{location}: unreadable texture: {exc}")
        return
    if len(header) < 24 or header[:8] != b"\x89PNG\r\n\x1a\n" or header[12:16] != b"IHDR":
        errors.append(f"{location}: invalid PNG header")
        return
    width, height = struct.unpack(">II", header[16:24])
    if width <= 0 or height <= 0 or width > 2048 or height > 2048:
        errors.append(f"{location}: invalid dimensions {width}x{height}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    args = parser.parse_args()
    errors: list[str] = []
    report: dict[str, object] = {"modules": {}, "errors": errors}
    for module, namespace in MODULES.items():
        resource_root = args.root / module / "src" / "main" / "resources"
        module_report = {
            "json": 0,
            "models": 0,
            "recipes": 0,
            "loot_tables": 0,
            "locales": 0,
            "entity_textures": 0,
            "manual_pages": 0,
            "sounds": 0,
        }
        for path in resource_root.rglob("*"):
            if not path.is_file():
                continue
            relative = path.relative_to(resource_root).as_posix().lower()
            if path.suffix.lower() == ".json" and "manual" in path.relative_to(resource_root).parts:
                module_report["manual_pages"] += 1
            if re.search(r"craftstudio|\.csjs(model|anim)$", relative):
                errors.append(f"{module}: CraftStudio resource {relative}")
            if path.suffix.lower() == ".json":
                module_report["json"] += 1
                try:
                    data = json.loads(path.read_text(encoding="utf-8"))
                except (OSError, json.JSONDecodeError) as exc:
                    errors.append(f"{module}: invalid JSON {relative}: {exc}")
                    continue
                if path.name.lower() == "sounds.json" and isinstance(data, dict):
                    module_report["sounds"] += len(data)
                if "/models/" in f"/{relative}" and isinstance(data, dict):
                    module_report["models"] += 1
                    textures = data.get("textures", {})
                    if isinstance(textures, dict):
                        for ref in textures.values():
                            if not isinstance(ref, str) or ":" not in ref or ref.startswith("#"):
                                continue
                            ref_namespace, texture_path = ref.split(":", 1)
                            if texture_path.startswith(("items/", "blocks/")):
                                errors.append(f"{module}: legacy plural texture path {ref} referenced by {relative}")
                            if ref_namespace == "minecraft":
                                continue
                            texture = resource_root / "assets" / ref_namespace / "textures" / f"{texture_path}.png"
                            if not texture.exists():
                                errors.append(f"{module}: missing texture {ref} referenced by {relative}")
                if "/recipes/" in f"/{relative}" and "/data/" in f"/{relative}" and isinstance(data, dict):
                    module_report["recipes"] += 1
                    _validate_recipe(data, f"{module}:{relative}", errors)
                if "/loot_tables/" in f"/{relative}" and "/data/" in f"/{relative}":
                    module_report["loot_tables"] += 1
                    if "/entities/" in f"/{relative}":
                        if data.get("type") != "minecraft:entity" or not isinstance(data.get("pools"), list):
                            errors.append(f"{module}:{relative}: entity loot table requires minecraft:entity type and pools")
                    _validate_loot(data, f"{module}:{relative}", errors)
                if "/advancements/" in f"/{relative}" and "/data/" in f"/{relative}":
                    _validate_advancement(data, f"{module}:{relative}", errors)
                if "/forge/biome_modifier/" in f"/{relative}" and "/data/" in f"/{relative}":
                    _validate_biome_modifier(data, f"{module}:{relative}", namespace, errors)
        if module in ENTITY_SOURCES:
            entity_dir = resource_root / "data" / namespace / "loot_tables" / "entities"
            actual = {item.stem for item in entity_dir.glob("*.json")} if entity_dir.exists() else set()
            expected = _legacy_entity_ids(args.root, module)
            missing = sorted(expected - actual)
            extra = sorted(actual - expected)
            if missing:
                errors.append(f"{module}: missing entity loot tables: {', '.join(missing)}")
            if extra:
                errors.append(f"{module}: unregistered entity loot tables: {', '.join(extra)}")

            # Entity renderers resolve the stable legacy registry path to a
            # same-named texture at runtime.  Validate that path directly in
            # addition to the texture references found in block/item models;
            # this catches a visually invisible entity even when the shared
            # native ModelPart renderer still compiles and registers.
            texture_dir = resource_root / "assets" / namespace / "textures" / "entity"
            textures = {item.stem for item in texture_dir.glob("*.png")} if texture_dir.exists() else set()
            module_report["entity_textures"] = len(textures)
            for texture in sorted(texture_dir.glob("*.png")):
                _validate_png(texture, f"{module}:entity texture {texture.name}", errors)
            missing_textures = sorted(expected - textures)
            if missing_textures:
                errors.append(f"{module}: missing entity textures: {', '.join(missing_textures)}")

            content_items = _java_list(args.root, CONTENT_SOURCES[module], "ITEM_IDS")
            content_blocks = _java_list(args.root, CONTENT_SOURCES[module], "BLOCK_IDS")
            egg_items = {"entity_egg_" + entity for entity in expected
                         if entity not in {"cart", "wagon", "tiller"}}
            expected_item_models = content_items | content_blocks | egg_items
            model_dir = resource_root / "assets" / namespace / "models" / "item"
            actual_item_models = {item.stem for item in model_dir.glob("*.json")}
            missing_models = sorted(expected_item_models - actual_item_models)
            if missing_models:
                errors.append(f"{module}: missing registered item models: {', '.join(missing_models)}")

            generated_name = ("CatsDogs" if module == "catsdogs" else module.title()) + "LegacyModelLayers.java"
            generated = args.root / module / "src/main/java/com/animania" / module / "client/model" / generated_name
            generated_text = generated.read_text(encoding="utf-8") if generated.exists() else ""
            layer_ids = set(re.findall(r'LAYERS\.put\("([a-z0-9_]+)"', generated_text))
            missing_layers = sorted(expected - {"cart", "wagon", "tiller"} - layer_ids)
            if missing_layers:
                errors.append(f"{module}: missing breed-specific native model layers: {', '.join(missing_layers)}")

        locale_dir = resource_root / "assets" / namespace / "lang"
        locales = sorted(item.stem for item in locale_dir.glob("*.json")) if locale_dir.exists() else []
        module_report["locales"] = len(locales)
        if len(locales) != 25:
            errors.append(f"{module}: expected 25 locale JSON files, found {len(locales)}")
        if module != "base":
            mods_toml = resource_root / "META-INF" / "mods.toml"
            text = mods_toml.read_text(encoding="utf-8") if mods_toml.exists() else ""
            processed = resource_root.parent.parent.parent / "build" / "resources" / "main" / "META-INF" / "mods.toml"
            processed_text = processed.read_text(encoding="utf-8") if processed.exists() else text
            if "${base_dependency}" not in text and not re.search(r'modId="animania"\s*\nmandatory=true', processed_text):
                errors.append(f"{module}: missing mandatory Base dependency")
        report["modules"][module] = module_report

    # The manual is resource-driven and deliberately replaces Patchouli.  A
    # page-count parity check catches an addon whose book JSONs were omitted
    # from the modern source set even when the base screen itself compiles.
    legacy_manual_root = args.root / "upstream" / "Animania-1.12" / "src" / "main" / "resources"
    if legacy_manual_root.exists():
        legacy_manual = sum(1 for path in legacy_manual_root.rglob("*.json")
                            if "manual" in path.relative_to(legacy_manual_root).parts)
        modern_manual = sum(report["modules"][module]["manual_pages"] for module in MODULES)
        if modern_manual != legacy_manual:
            errors.append(f"manual page count mismatch: legacy {legacy_manual}, modern {modern_manual}")
        legacy_sounds = 0
        for path in legacy_manual_root.rglob("sounds.json"):
            try:
                data = json.loads(path.read_text(encoding="utf-8"))
            except (OSError, json.JSONDecodeError):
                continue
            if isinstance(data, dict):
                legacy_sounds += len(data)
        modern_sounds = sum(report["modules"][module]["sounds"] for module in MODULES)
        if modern_sounds != legacy_sounds:
            errors.append(f"sound event count mismatch: legacy {legacy_sounds}, modern {modern_sounds}")

    # CraftStudio clips are intentionally replaced by native 1.20.1
    # ModelPart/LayerDefinition/AnimationDefinition code.  Keep this check
    # independent of the resource walk so a stale source file cannot quietly
    # reintroduce the old runtime dependency.
    model_source = args.root / "base" / "src" / "main" / "java" / "com" / "animania" / "client" / "model"
    animal_model = model_source / "AnimaniaAnimalModel.java"
    animation_source = model_source / "AnimaniaAnimations.java"
    for path in (animal_model, animation_source):
        if not path.exists():
            errors.append(f"base: missing native model source {path.relative_to(args.root).as_posix()}")
    if animal_model.exists():
        text = animal_model.read_text(encoding="utf-8")
        for token in ("ModelPart", "LayerDefinition", "createBodyLayer"):
            if token not in text:
                errors.append(f"base: native animal model missing {token}")
    if animation_source.exists():
        text = animation_source.read_text(encoding="utf-8")
        for token in ("AnimationDefinition", "WALK", "RUN", "SLEEP", "EAT", "DRINK", "PLAY", "BREED", "GRAZE"):
            if token not in text:
                errors.append(f"base: native animation source missing {token}")
    output = args.root / "build" / "resource-audit.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
