"""Create complete Forge biome spawn modifiers for every registered animal."""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path


MODULES = {
    "farm": ("animania_farm", "farm/src/main/java/com/animania/farm/FarmLegacyIds.java"),
    "extra": ("animania_extra", "extra/src/main/java/com/animania/extra/ExtraLegacyIds.java"),
    "catsdogs": ("animania_catsdogs", "catsdogs/src/main/java/com/animania/catsdogs/CatsDogsLegacyIds.java"),
}


def ids_from_java(path: Path) -> list[str]:
    text = path.read_text(encoding="utf-8")
    body = text.split("List.of(", 1)[1].split(");", 1)[0]
    return re.findall(r'"([a-z0-9_]+)"', body)


def generate(root: Path) -> int:
    total = 0
    for module, (namespace, source) in MODULES.items():
        ids = ids_from_java(root / source)
        if module == "farm":
            ids = [item for item in ids if item not in {"cart", "wagon", "tiller"}]
        spawners = []
        for item in ids:
            if item.startswith(("kit_", "kid_", "calf_", "lamb_", "piglet_", "chick_", "foal_", "kitten_", "puppy_", "peachick_")):
                # Children are produced by breeding/eggs rather than natural
                # spawning, matching the 1.12 gameplay intent.
                continue
            if module == "farm":
                if item.startswith(("cow_", "bull_", "sow_", "hog_", "hen_", "rooster_")):
                    weight = 9
                elif item.startswith(("mare_", "stallion_")):
                    weight = 8
                else:
                    weight = 8  # goats and sheep
            elif module == "extra":
                weight = 18 if item in {"frog", "toad", "dartfrog"} else 8
                # The old rabbit handler made Dutch/Lop breeds half as likely.
                if item.startswith(("doe_dutch", "buck_dutch", "doe_lop", "buck_lop")):
                    weight = 4
            else:
                weight = 4 if item.startswith(("queen_", "tom_")) else 5
            spawners.append({"type": f"{namespace}:{item}", "weight": weight, "minCount": 1, "maxCount": 3})
        path = root / module / "src/main/resources/data" / namespace / "forge/biome_modifier/add_animals.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps({
            "type": "forge:add_spawns",
            "biomes": "#minecraft:is_overworld",
            "spawners": spawners,
        }, indent=2) + "\n", encoding="utf-8")
        total += len(spawners)
    return total


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()
    print(f"generated {generate(args.root)} biome spawner entries")


if __name__ == "__main__":
    main()
