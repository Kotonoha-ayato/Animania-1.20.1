"""Copy the pinned resource set into modern assets/data locations."""
from __future__ import annotations

import argparse
import json
import shutil
from pathlib import Path

MODULES = {"animania": "base", "farm": "farm", "extra": "extra", "catsdogs": "catsdogs"}
MOD_IDS = {"base": "animania", "farm": "animania_farm", "extra": "animania_extra", "catsdogs": "animania_catsdogs"}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    assets = args.source / "src" / "main" / "resources" / "assets"
    for namespace, module in MODULES.items():
        source = assets / namespace
        if not source.exists():
            continue
        target_assets = args.output / module / "src" / "main" / "resources" / "assets" / namespace
        target_assets.mkdir(parents=True, exist_ok=True)
        for item in source.rglob("*"):
            if not item.is_file():
                continue
            relative = item.relative_to(source)
            if relative.suffix.lower() == ".lang" and relative.parts[0] == "lang":
                data = {}
                for line in item.read_text(encoding="utf-8", errors="replace").splitlines():
                    if not line or line.startswith("#") or "=" not in line:
                        continue
                    key, value = line.split("=", 1)
                    normalized_key = key.strip()
                    if normalized_key.endswith(".name"):
                        normalized_key = normalized_key[:-5]
                    if namespace != "animania":
                        normalized_key = normalized_key.replace(f"{namespace}.", f"animania_{namespace}.")
                    data[normalized_key] = value.strip()
                out = target_assets / relative.with_name(relative.stem.lower() + ".json")
                out.parent.mkdir(parents=True, exist_ok=True)
                out.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            else:
                out = target_assets / relative
                out.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(item, out)

        old_data = source / "animania"
        for data_kind in ("recipes", "loot_tables", "advancements", "tags"):
            old_dir = old_data / data_kind
            if not old_dir.exists():
                continue
            target = args.output / module / "src" / "main" / "resources" / "data" / MOD_IDS[module] / data_kind
            target.mkdir(parents=True, exist_ok=True)
            for item in old_dir.rglob("*"):
                if item.is_file():
                    out = target / item.relative_to(old_dir)
                    out.parent.mkdir(parents=True, exist_ok=True)
                    shutil.copy2(item, out)
    print("converted legacy assets and data")


if __name__ == "__main__":
    main()
