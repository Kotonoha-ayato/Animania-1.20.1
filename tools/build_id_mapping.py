"""Build the 1.12 -> 1.20.1 registry/module mapping table."""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

MODULES = {
    "farm": ("animania_farm", "FarmLegacyIds.java", "FarmContent.java"),
    "extra": ("animania_extra", "ExtraLegacyIds.java", "ExtraContent.java"),
    "catsdogs": ("animania_catsdogs", "CatsDogsLegacyIds.java", "CatsDogsContent.java"),
}
ALIASES = {
    # Base item aliases and the dynamic all-animal egg.
    "animania:entity_egg_random": "animania:entity_egg_random",
    "animania:cat_random": "animania_catsdogs:cat_random",
    "animania:dog_random": "animania_catsdogs:dog_random",
    "animania:peacock_random": "animania_extra:peacock_random",
    "animania:rabbit_random": "animania_extra:rabbit_random",
    "animania:dart_frog": "animania_extra:dart_frog",
    "animania:cow_random": "animania_farm:cow_random",
    "animania:chicken_random": "animania_farm:chicken_random",
    "animania:pig_random": "animania_farm:pig_random",
    "animania:goat_random": "animania_farm:goat_random",
    "animania:sheep_random": "animania_farm:sheep_random",
    "animania:bucket_slop": "animania:bucket_slop",
    "animania:block_straw": "animania:straw",
    "animania:block_mud": "animania:mud",
    "animania:block_nest": "animania:nest",
    "animania:block_trough": "animania:trough",
    "animania:animania_manual": "animania:manual",
    "animania:item_cart": "animania_farm:cart",
    "animania:item_wagon": "animania_farm:wagon",
    "animania:item_tiller": "animania_farm:tiller",
    "animania:bee_hive": "animania_farm:hive",
    "animania:wild_hive": "animania_farm:wild_hive",
    "animania:cheese_mold": "animania_farm:cheese_mold",
    "animania:cheese_friesian": "animania_farm:cheese_friesian",
    "animania:cheese_holstein": "animania_farm:cheese_holstein",
    "animania:cheese_jersey": "animania_farm:cheese_jersey",
    "animania:cheese_goat": "animania_farm:cheese_goat",
    "animania:cheese_sheep": "animania_farm:cheese_sheep",
    "animania:animania_wool": "animania_farm:animania_wool",
    "animania:animania_honey": "animania_farm:animania_honey",
    "animania:milk_holstein": "animania_farm:milk_holstein",
    "animania:milk_friesian": "animania_farm:milk_friesian",
    "animania:milk_jersey": "animania_farm:milk_jersey",
    "animania:milk_goat": "animania_farm:milk_goat",
    "animania:milk_sheep": "animania_farm:milk_sheep",
    "animania:milk_holstein_bucket": "animania_farm:milk_holstein_bucket",
    "animania:milk_friesian_bucket": "animania_farm:milk_friesian_bucket",
    "animania:milk_jersey_bucket": "animania_farm:milk_jersey_bucket",
    "animania:milk_goat_bucket": "animania_farm:milk_goat_bucket",
    "animania:milk_sheep_bucket": "animania_farm:milk_sheep_bucket",
    # Older resource packs used the family-first bucket spelling.
    "animania:holstein_bucket_milk": "animania_farm:milk_holstein_bucket",
    "animania:friesian_bucket_milk": "animania_farm:milk_friesian_bucket",
    "animania:jersey_bucket_milk": "animania_farm:milk_jersey_bucket",
    "animania:goat_bucket_milk": "animania_farm:milk_goat_bucket",
    "animania:sheep_bucket_milk": "animania_farm:milk_sheep_bucket",
    "animania:cow_bucket_milk": "animania_farm:milk_friesian_bucket",
    "animania:block_hamster_wheel": "animania_extra:hamster_wheel",
    "animania:hamster_ball_clear": "animania_extra:hamster_ball_clear",
    "animania:hamster_ball_colored": "animania_extra:hamster_ball_colored",
    "animania:hamster_food": "animania_extra:hamster_food",
}


def ids(path: Path) -> list[str]:
    return re.findall(r'"([a-z0-9_]+)"', path.read_text(encoding="utf-8"))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    entries = []
    seen = set()

    def add(entry: dict) -> None:
        key = (entry["legacy_id"], entry["modern_id"], entry["kind"])
        if key not in seen:
            seen.add(key)
            entries.append(entry)

    for module, (namespace, filename, content_filename) in MODULES.items():
        source = args.root / module / "src/main/java/com/animania" / module / filename
        for legacy_id in ids(source):
            add({
                "legacy_id": f"animania:{legacy_id}",
                "modern_id": f"{namespace}:{legacy_id}",
                "module": module,
                "kind": "entity",
                "status": "registered",
            })
        content = args.root / module / "src/main/java/com/animania" / module / content_filename
        content_text = content.read_text(encoding="utf-8")
        for field, kind in (("ITEM_IDS", "item"), ("BLOCK_IDS", "block")):
            match = re.search(field + r"\s*=\s*List\.of\((.*?)\);", content_text, re.S)
            if match:
                for legacy_id in re.findall(r'"([a-z0-9_]+)"', match.group(1)):
                    add({
                        "legacy_id": f"animania:{legacy_id}",
                        "modern_id": f"{namespace}:{legacy_id}",
                        "module": module,
                        "kind": kind,
                        "status": "registered",
                    })
    for old, new in ALIASES.items():
        add({"legacy_id": old, "modern_id": new, "module": new.split(":", 1)[0], "kind": "alias", "status": "converted"})
    payload = {
        "schema_version": 1,
        "source_baseline": "Animania 1.12",
        "target": {"minecraft": "1.20.1", "forge": "47.4.22", "release": "3.0.0"},
        "entries": sorted(entries, key=lambda item: (item["legacy_id"], item["modern_id"])),
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {len(entries)} ID mappings to {args.output}")


if __name__ == "__main__":
    main()
