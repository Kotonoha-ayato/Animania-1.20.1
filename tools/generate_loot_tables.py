"""Generate modern entity loot tables from the pinned legacy ID lists.

The 1.12 branch stored tables below ``assets/<addon>/animania`` and used
custom Forge serializers.  1.20.1 requires ``data/<namespace>/loot_tables``
and namespaced vanilla functions, so this deterministic generator keeps the
legacy drop intent while making every registered entity resolve to a table.
"""
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


def entry(item: str, minimum: int = 1, maximum: int = 2, *, smelt: bool = False) -> dict:
    functions: list[dict] = [
        {"function": "minecraft:set_count", "count": {"min": minimum, "max": maximum}},
        {"function": "minecraft:looting_enchant", "count": {"min": 0, "max": 1}},
    ]
    if smelt:
        functions.append({
            "function": "minecraft:furnace_smelt",
            "conditions": [{
                "condition": "minecraft:entity_properties",
                "entity": "this",
                "predicate": {"flags": {"is_on_fire": True}},
            }],
        })
    return {"type": "minecraft:item", "name": item, "weight": 1, "functions": functions}


def pool(name: str, *entries: dict) -> dict:
    return {"name": name, "rolls": 1, "entries": list(entries)}


def farm_drops(entity_id: str) -> list[dict]:
    if entity_id in {"cart", "wagon", "tiller"}:
        return [pool("vehicle", entry(f"animania_farm:{entity_id}", 1, 1))]
    if entity_id.startswith(("cow_", "bull_", "calf_")):
        meat = "animania_farm:raw_prime_beef"
        if "mooshroom" in entity_id:
            return [pool("leather", entry("minecraft:leather", 0, 2)), pool("beef", entry(meat, 1, 3, smelt=True)), pool("mushroom", entry("minecraft:red_mushroom", 0, 2))]
        return [pool("leather", entry("minecraft:leather", 0, 2)), pool("beef", entry(meat, 1, 3, smelt=True))]
    if entity_id.startswith(("ewe_", "ram_", "lamb_")):
        return [pool("wool", entry("minecraft:white_wool", 0, 2)), pool("mutton", entry("animania_farm:raw_prime_mutton", 1, 3, smelt=True))]
    if entity_id.startswith(("doe_", "buck_", "kid_")):
        return [pool("chevon", entry("animania_farm:raw_prime_chevon", 1, 3, smelt=True))]
    if entity_id.startswith(("sow_", "hog_", "piglet_")):
        return [pool("pork", entry("animania_farm:raw_prime_pork", 1, 3, smelt=True))]
    if entity_id.startswith(("hen_", "rooster_", "chick_")):
        return [pool("feather", entry("minecraft:feather", 0, 2)), pool("chicken", entry("animania_farm:raw_prime_chicken", 1, 2, smelt=True))]
    if entity_id.startswith(("mare_", "stallion_", "foal_")):
        return [pool("horse", entry("minecraft:leather", 0, 2)), pool("horse_meat", entry("animania_farm:raw_horse", 1, 3, smelt=True))]
    return []


def extra_drops(entity_id: str) -> list[dict]:
    if entity_id.startswith(("buck_", "doe_", "kit_")):
        return [pool("rabbit_hide", entry("minecraft:rabbit_hide", 0, 2)), pool("rabbit", entry("animania_extra:raw_prime_rabbit", 1, 3, smelt=True))]
    if entity_id.startswith(("peacock_", "peahen_", "peachick_")):
        color = entity_id.split("_", 1)[1]
        feather = f"animania_extra:{color}_peacock_feather"
        return [pool("feather", entry(feather, 0, 2)), pool("peacock", entry("animania_extra:raw_prime_peacock", 1, 2, smelt=True))]
    if entity_id in {"frog", "toad", "dartfrog"}:
        return [pool("frog_legs", entry("animania_extra:raw_frog_legs", 1, 2, smelt=True))]
    if entity_id.startswith("ferret"):
        return [pool("fur", entry("minecraft:string", 0, 2))]
    if entity_id.startswith("hedgehog"):
        return [pool("quills", entry("minecraft:feather", 0, 2))]
    if entity_id == "hamster":
        return [pool("fur", entry("minecraft:string", 0, 1))]
    return []


def catsdogs_drops(entity_id: str) -> list[dict]:
    if entity_id.startswith(("female_", "male_", "puppy_")):
        return [pool("dog", entry("minecraft:leather", 0, 2)), pool("fur", entry("minecraft:string", 0, 2))]
    if entity_id.startswith(("queen_", "tom_", "kitten_")):
        return [pool("cat", entry("minecraft:string", 0, 2)), pool("fur", entry("minecraft:leather", 0, 1))]
    return []


def generate(root: Path) -> int:
    written = 0
    for module, (namespace, source) in MODULES.items():
        ids = ids_from_java(root / source)
        target = root / module / "src/main/resources/data" / namespace / "loot_tables/entities"
        target.mkdir(parents=True, exist_ok=True)
        for entity_id in ids:
            if module == "farm":
                pools = farm_drops(entity_id)
            elif module == "extra":
                pools = extra_drops(entity_id)
            else:
                pools = catsdogs_drops(entity_id)
            table = {"type": "minecraft:entity", "pools": pools}
            path = target / f"{entity_id}.json"
            path.write_text(json.dumps(table, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
            written += 1
    return written


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()
    print(f"generated {generate(args.root)} entity loot tables")


if __name__ == "__main__":
    main()
