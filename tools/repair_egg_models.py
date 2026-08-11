"""Regenerate only source-derived addon spawn-egg models."""
from __future__ import annotations

import argparse
from pathlib import Path

from generate_basic_assets import MODULES, egg_model, ids, write_generated


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    args = parser.parse_args()
    changed = 0
    for module, (modid, _legacy_namespace) in MODULES.items():
        content_name = "FarmContent.java" if module == "farm" else "ExtraContent.java" if module == "extra" else "CatsDogsContent.java"
        java = args.root / module / "src/main/java/com/animania" / module / content_name
        legacy_name = "CatsDogsLegacyIds.java" if module == "catsdogs" else f"{module.title()}LegacyIds.java"
        entity_ids = ids(java.with_name(legacy_name), "ALL")
        targets = [entity for entity in entity_ids if entity not in {"cart", "wagon", "tiller"}]
        output = args.root / module / "src/main/resources/assets" / modid / "models/item"
        for target in dict.fromkeys(targets):
            path = output / f"entity_egg_{target}.json"
            before = path.read_bytes() if path.exists() else None
            write_generated(path, egg_model(module, modid, target))
            changed += before != path.read_bytes()
    print(f"repaired {changed} spawn-egg models")


if __name__ == "__main__":
    main()
