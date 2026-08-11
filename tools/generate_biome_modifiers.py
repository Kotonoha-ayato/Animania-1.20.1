"""Generate the datapack entry points for config-backed biome modifiers.

The spawn tables live in Java because Forge common config is not available to
static JSON. Keeping this generator prevents data generation from restoring the
obsolete all-biomes/all-adults tables.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path


MODULES = {
    "farm": "animania_farm",
    "extra": "animania_extra",
    "catsdogs": "animania_catsdogs",
}


def generate(root: Path) -> int:
    for module, namespace in MODULES.items():
        path = root / module / "src/main/resources/data" / namespace / "forge/biome_modifier/add_animals.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps({"type": f"{namespace}:configured_spawns"}, indent=2) + "\n", encoding="utf-8")
    return len(MODULES)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()
    print(f"generated {generate(args.root)} config-backed biome modifier entry points")


if __name__ == "__main__":
    main()
