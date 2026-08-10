"""Expose legacy breed textures under modern per-mod entity paths."""
from __future__ import annotations

import argparse
import re
import shutil
from pathlib import Path

MODULES = {
    "farm": ("animania_farm", "FarmLegacyIds.java"),
    "extra": ("animania_extra", "ExtraLegacyIds.java"),
    "catsdogs": ("animania_catsdogs", "CatsDogsLegacyIds.java"),
}


def ids(path: Path) -> list[str]:
    return re.findall(r'"([a-z0-9_]+)"', path.read_text(encoding="utf-8", errors="replace"))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    args = parser.parse_args()
    for module, (modid, legacy_file) in MODULES.items():
        legacy_assets = args.root / module / "src" / "main" / "resources" / "assets" / module / "animania" / "textures" / "entity"
        modern_assets = args.root / module / "src" / "main" / "resources" / "assets" / modid / "textures" / "entity"
        modern_assets.mkdir(parents=True, exist_ok=True)
        candidates = list(legacy_assets.rglob("*.png"))
        ids_file = args.root / module / "src" / "main" / "java" / "com" / "animania" / module / legacy_file
        copied = 0
        for item in ids(ids_file):
            matches = [path for path in candidates if path.stem.lower() == item]
            if not matches:
                matches = [path for path in candidates if path.stem.lower().startswith(item + "_") or item.startswith(path.stem.lower() + "_")]
            if not matches:
                family = item
                for prefix in ("female_", "male_", "puppy_", "tom_", "queen_", "kitten_", "chick_", "hen_", "rooster_", "calf_", "cow_", "bull_", "kid_", "doe_", "buck_", "lamb_", "ewe_", "ram_", "piglet_", "sow_", "hog_", "foal_", "mare_", "stallion_", "peachick_", "peacock_", "peahen_", "kit_", "buck_", "doe_"):
                    if family.startswith(prefix):
                        family = family[len(prefix):]
                        break
                matches = [path for path in candidates if path.stem.lower() == family]
            if not matches:
                tokens = [token for token in family.split("_") if token]
                matches = [path for path in candidates if all(token in path.stem.lower() for token in tokens)
                           and "blink" not in path.stem.lower() and "ball" not in path.stem.lower()]
            if not matches and module == "extra":
                if family in ("dartfrog", "frog"):
                    matches = [path for path in candidates if "frog" in path.stem.lower() and "blink" not in path.stem.lower()]
                elif family.startswith("hamster"):
                    matches = [path for path in candidates if path.stem.lower().startswith("hamster_") and "blink" not in path.stem.lower()]
                elif family.startswith("hedgehog"):
                    matches = [path for path in candidates if path.stem.lower().startswith("hedgehog") and "blink" not in path.stem.lower()]
                elif family.startswith("peahen"):
                    matches = [path for path in candidates if path.stem.lower().startswith("peafowl_") and "blink" not in path.stem.lower()]
            if not matches and module == "farm" and family in {"leghorn", "orpington", "plymouth_rock", "rhode_island_red", "wyandotte"}:
                role = item.split("_", 1)[0]
                matches = [path for path in candidates if path.stem.lower().startswith(role + "_") and "blink" not in path.stem.lower()]
            if not matches:
                continue
            shutil.copy2(matches[0], modern_assets / f"{item}.png")
            copied += 1
        print(module, copied, "of", len(ids(ids_file)))
    base = args.root / "base" / "src" / "main" / "resources" / "assets" / "animania" / "textures" / "entity"
    base.mkdir(parents=True, exist_ok=True)
    source = next((args.root / "base" / "src" / "main" / "resources" / "assets" / "animania" / "textures").rglob("*.png"))
    shutil.copy2(source, base / "default.png")


if __name__ == "__main__":
    main()
