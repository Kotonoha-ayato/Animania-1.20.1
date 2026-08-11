"""Generate the complete legacy raw-to-cooked recipe surface."""
from __future__ import annotations

import argparse
import json
from pathlib import Path


PAIRS = {
    "farm": {
        "raw_prime_steak": "cooked_prime_steak", "raw_prime_beef": "cooked_prime_beef",
        "raw_horse": "cooked_horse", "raw_prime_pork": "cooked_prime_pork",
        "raw_prime_bacon": "cooked_prime_bacon", "raw_prime_chicken": "cooked_prime_chicken",
        "raw_chevon": "cooked_chevon", "raw_prime_chevon": "cooked_prime_chevon",
        "raw_prime_mutton": "cooked_prime_mutton",
    },
    "extra": {
        "raw_prime_rabbit": "cooked_prime_rabbit", "raw_frog_legs": "cooked_frog_legs",
        "raw_peacock": "cooked_peacock", "raw_prime_peacock": "cooked_prime_peacock",
    },
}
NAMESPACES = {"farm": "animania_farm", "extra": "animania_extra"}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    args = parser.parse_args()
    written = []
    for module, pairs in PAIRS.items():
        namespace = NAMESPACES[module]
        recipe_root = args.root / module / "src/main/resources/data" / namespace / "recipes"
        recipe_root.mkdir(parents=True, exist_ok=True)
        for raw, cooked in pairs.items():
            payload = {
                "type": "minecraft:smelting", "category": "food",
                "ingredient": {"item": f"{namespace}:{raw}"},
                "result": f"{namespace}:{cooked}", "experience": 0.3, "cookingtime": 200,
            }
            path = recipe_root / f"{raw}_smelting.json"
            rendered = json.dumps(payload, ensure_ascii=False, indent=2) + "\n"
            if not path.exists() or path.read_text(encoding="utf-8") != rendered:
                path.write_text(rendered, encoding="utf-8")
                written.append(path.relative_to(args.root).as_posix())
    print(json.dumps({"pairs": sum(map(len, PAIRS.values())), "written": written}, indent=2))


if __name__ == "__main__":
    main()
