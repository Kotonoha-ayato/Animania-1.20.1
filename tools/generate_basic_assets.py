"""Fill missing basic assets without overwriting migrated hand-authored models."""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

MODULES = {"farm": ("animania_farm", "farm"), "extra": ("animania_extra", "extra"), "catsdogs": ("animania_catsdogs", "catsdogs")}


def write_missing(path: Path, payload: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if not path.exists():
        path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


def write_generated(path: Path, payload: dict) -> None:
    """Rewrite deterministic generated assets so stale placeholder models cannot survive."""
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


def ids(source: Path, field: str) -> list[str]:
    match = re.search(field + r"\s*=\s*List\.of\((.*?)\);", source.read_text(encoding="utf-8", errors="replace"), re.S)
    return re.findall(r'"([a-z0-9_]+)"', match.group(1)) if match else []


def egg_texture(module: str, entity_id: str) -> str:
    if module == "farm":
        family = "chicken" if any(x in entity_id for x in ("chick", "hen", "rooster")) else \
                 "cow" if any(x in entity_id for x in ("calf", "cow", "bull")) else \
                 "goat" if any(x in entity_id for x in ("kid", "doe", "buck")) else \
                 "pig" if any(x in entity_id for x in ("piglet", "sow", "hog")) else "sheep"
    elif module == "extra":
        family = "peacock" if any(x in entity_id for x in ("peachick", "peahen", "peacock")) else \
                 "rabbit" if any(x in entity_id for x in ("kit_", "doe_", "buck_")) else \
                 "ferret" if "ferret" in entity_id else "rabbit"
    else:
        family = "cat" if entity_id.startswith("cat_") or any(x in entity_id for x in ("kitten_", "queen_", "tom_")) else "dog"
    return f"egg_{family}_random"


def egg_gender(entity_id: str) -> str:
    if entity_id.startswith(("bull_", "rooster_", "hog_", "buck_", "ram_", "stallion_", "peacock_", "male_", "tom_")):
        return "male"
    if entity_id.startswith(("cow_", "hen_", "sow_", "doe_", "ewe_", "mare_", "peahen_", "female_", "queen_")):
        return "female"
    return "genderless"


def egg_model(module: str, modid: str, target: str) -> dict:
    if target.endswith("_random"):
        return {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"{modid}:item/{egg_texture(module, target)}"},
            "display": {
                "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
                "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
                "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
                "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
            },
        }
    if target == "dart_frog":
        return {"parent": "minecraft:item/generated", "textures": {"layer0": "animania:item/egg_frog_dart"}}
    textures = {
        "layer0": "animania:item/egg_layer_1",
        "layer1": "animania:item/egg_layer_2",
    }
    gender = egg_gender(target)
    if gender != "genderless":
        textures["layer2"] = f"animania:item/egg_layer_{gender}"
    return {"parent": "minecraft:item/generated", "textures": textures}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    args = parser.parse_args()
    base_resource = args.root / "base" / "src" / "main" / "resources" / "assets" / "animania"
    base_items = ["manual", "hay", "salt", "cheese", "water_bottle"]
    base_item_textures = {
        "manual": "animania:item/animania_manual",
        "hay": "animania:block/hay",
        "salt": "animania:item/salt",
        "cheese": "animania:item/cheese",
        "water_bottle": "animania:item/water_bottle",
    }
    base_blocks = ["trough", "nest", "salt_lick", "mud", "straw", "invisiblock", "block_seeds"]
    base_lang = {}
    for item in base_items:
        base_lang[f"item.animania.{item}"] = item.replace("_", " ").title()
        out = base_resource / "models" / "item" / f"{item}.json"
        write_missing(out, {"parent": "minecraft:item/generated", "textures": {"layer0": base_item_textures[item]}})
    for block in base_blocks:
        base_lang[f"block.animania.{block}"] = block.replace("_", " ").title()
        state = base_resource / "blockstates" / f"{block}.json"
        write_missing(state, {"variants": {"": {"model": f"animania:block/{block}"}}})
        model = base_resource / "models" / "block" / f"{block}.json"
        write_missing(model, {"parent": "minecraft:block/cube_all", "textures": {"all": f"animania:block/{block}"}})
        item_model = base_resource / "models" / "item" / f"{block}.json"
        write_missing(item_model, {"parent": f"animania:block/{block}"})
    base_lang_file = base_resource / "lang" / "en_us.json"
    base_lang_file.parent.mkdir(parents=True, exist_ok=True)
    existing_base = json.loads(base_lang_file.read_text(encoding="utf-8")) if base_lang_file.exists() else {}
    existing_base.update(base_lang)
    base_lang_file.write_text(json.dumps(existing_base, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    for module, (modid, legacy_namespace) in MODULES.items():
        java = args.root / module / "src" / "main" / "java" / "com" / "animania" / module / ("FarmContent.java" if module == "farm" else "ExtraContent.java" if module == "extra" else "CatsDogsContent.java")
        item_ids = ids(java, "ITEM_IDS")
        block_ids = ids(java, "BLOCK_IDS")
        legacy_java = java.with_name(module.title().replace("Catsdogs", "CatsDogs") + "LegacyIds.java")
        entity_ids = ids(legacy_java, "ALL")
        item_ids = list(dict.fromkeys(item_ids + ["entity_egg_" + entity for entity in entity_ids
                                                   if entity not in {"cart", "wagon", "tiller"}]))
        resource = args.root / module / "src" / "main" / "resources" / "assets" / modid
        lang = {}
        for item in item_ids:
            lang[f"item.{modid}.{item}"] = item.replace("_", " ").title()
            out = resource / "models" / "item" / f"{item}.json"
            # Entity eggs have the legacy builtin/generated display transform
            # and an egg_<family> texture. Never replace the hand-authored
            # model with a generic item stub when datagen is rerun.
            if item.startswith("entity_egg_"):
                target = item.removeprefix("entity_egg_")
                # Random/family eggs have hand-authored 1.12 display
                # transforms and are restored from the pinned resource.
                # Only per-entity eggs are deterministic generated assets.
                if target.endswith("_random") or target == "dart_frog":
                    write_missing(out, egg_model(module, modid, target))
                else:
                    write_generated(out, egg_model(module, modid, target))
            else:
                write_missing(out, {"parent": "minecraft:item/generated", "textures": {"layer0": f"{modid}:item/{item}"}})
        for block in block_ids:
            lang[f"block.{modid}.{block}"] = block.replace("_", " ").title()
            state = resource / "blockstates" / f"{block}.json"
            write_missing(state, {"variants": {"": {"model": f"{modid}:block/{block}"}}})
            model = resource / "models" / "block" / f"{block}.json"
            write_missing(model, {"parent": "minecraft:block/cube_all", "textures": {"all": f"{modid}:block/{block}"}})
            item_model = resource / "models" / "item" / f"{block}.json"
            write_missing(item_model, {"parent": f"{modid}:block/{block}"})
        lang_dir = resource / "lang"
        lang_dir.mkdir(parents=True, exist_ok=True)
        lang_file = lang_dir / "en_us.json"
        existing = json.loads(lang_file.read_text(encoding="utf-8")) if lang_file.exists() else {}
        existing.update(lang)
        lang_file.write_text(json.dumps(existing, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(module, len(item_ids), len(block_ids))


if __name__ == "__main__":
    main()
