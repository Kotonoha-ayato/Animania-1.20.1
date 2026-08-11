"""Generate deterministic block-drop tables for every solid Animania block."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

SIMPLE = {
    "base": ("animania", ("trough", "nest", "salt_lick", "mud", "straw", "invisiblock", "block_seeds")),
    "farm": ("animania_farm", ("hive", "wild_hive", "cheese_mold")),
    "extra": ("animania_extra", ("hamster_wheel",)),
    "catsdogs": ("animania_catsdogs", ("pet_bowl", "cat_bed_1", "cat_bed_2", "cat_tower", "dog_house", "dog_pillow", "litter_box")),
}
CHEESE = ("friesian", "holstein", "jersey", "goat", "sheep")


def table(item: str, conditions: list[dict] | None = None) -> dict:
    entry: dict = {"type": "minecraft:item", "name": item}
    if conditions:
        entry["conditions"] = conditions
    return {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [entry]}]}


def write(path: Path, payload: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    args = parser.parse_args()
    count = 0
    for module, (namespace, ids) in SIMPLE.items():
        root = args.root / module / "src/main/resources/data" / namespace / "loot_tables/blocks"
        for content_id in ids:
            write(root / f"{content_id}.json", table(f"{namespace}:{content_id}"))
            count += 1
    root = args.root / "farm/src/main/resources/data/animania_farm/loot_tables/blocks"
    for family in CHEESE:
        block = f"cheese_{family}"
        condition = {"condition": "minecraft:block_state_property", "block": f"animania_farm:{block}",
                     "properties": {"bites": "0"}}
        write(root / f"{block}.json", table(f"animania_farm:{family}_cheese_wheel", [condition]))
        count += 1
    print(json.dumps({"generated": count}))


if __name__ == "__main__":
    main()
