"""Audit every registered Animania entity against the runtime texture resolver.

This is deliberately a read-only client/resource audit.  It does not close a
matrix entry: a file existing on disk is necessary for rendering, but it is
not a substitute for the client screenshot/geometry regression required by
the release plan.  The audit expands the exact variant sets used by
``AnimaniaAnimalEntity.initialVariant`` and checks the path selected by the
same resolver contract for every ID, including sheared adult coats.
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

VARIANTS = {
    "draft": ("black", "bw1", "bw2", "grey", "red", "white"),
    "hamster": ("black", "brown", "darkbrown", "darkgray", "gray", "plum", "tarou", "white", "gold"),
    "dartfrog": ("blue", "red", "yellow"),
    "frog": ("default", "green"),
    "dog01": ("0", "1"),
    "dog012": ("0", "1", "2"),
    "wolf": tuple(str(i) for i in range(8)),
    "lop": ("black", "brown", "golden", "olive", "patch_black", "patch_brown", "patch_grey"),
    "sheep": ("white", "brown"),
    "friesian": ("white", "black", "brown"),
}


def ids_from_source(root: Path, source: str) -> list[str]:
    text = (root / source).read_text(encoding="utf-8")
    body = text.split("List.of(", 1)[1].split(");", 1)[0]
    return re.findall(r'"([a-z0-9_]+)"', body)


def variants(entity_id: str) -> tuple[str, ...]:
    if entity_id == "hamster":
        return VARIANTS["hamster"]
    if entity_id == "dartfrog":
        return VARIANTS["dartfrog"]
    if entity_id == "frog":
        return VARIANTS["frog"]
    if entity_id.endswith("_draft"):
        return VARIANTS["draft"]
    if entity_id.endswith(("_chihuahua", "_collie")):
        return VARIANTS["dog01"]
    if entity_id.endswith(("_labrador", "_poodle")):
        return VARIANTS["dog012"]
    if entity_id.endswith("_wolf"):
        return VARIANTS["wolf"]
    if entity_id.endswith("_lop"):
        return VARIANTS["lop"]
    if entity_id.endswith(("_dorset", "_merino", "_suffolk")):
        return VARIANTS["sheep"]
    if entity_id.endswith("_friesian"):
        return VARIANTS["friesian"]
    return ("default",)


def resolved_path(namespace: str, entity_id: str, variant: str, sheared: bool = False) -> str:
    """Mirror the public resolver's path contract, without loading Minecraft."""
    if namespace == "animania_farm":
        if entity_id in {"cart", "wagon", "tiller"}:
            return f"props/{entity_id}.png"
        if entity_id.startswith(("cow_", "bull_", "calf_")):
            return f"cows/{entity_id}.png"
        if entity_id.startswith(("doe_", "buck_", "kid_")):
            normalized = entity_id.replace("nigerian_dwarf", "nigerian")
            if entity_id == "doe_angora":
                normalized = "buck_angora"
            if sheared and entity_id in {"buck_angora", "doe_angora"}:
                normalized = "buck_angora_sheared"
            return f"goats/{normalized}.png"
        if entity_id.startswith(("sow_", "hog_", "piglet_")):
            return f"pigs/{entity_id}.png"
        if entity_id.startswith(("mare_draft", "stallion_draft", "foal_draft")):
            return f"horses/draft_horse_{variant or 'black'}.png"
        if entity_id.startswith(("hen_", "rooster_", "chick_")):
            breed = entity_id.split("_", 1)[1]
            colour = {
                "leghorn": "white", "orpington": "golden", "plymouth_rock": "specked",
                "rhode_island_red": "red", "wyandotte": "brown",
            }.get(breed, "white")
            return f"chickens/{entity_id.split('_', 1)[0]}_{colour}.png"
        if entity_id.startswith(("ewe_", "ram_", "lamb_")):
            role, breed = entity_id.split("_", 1)
            if breed == "dorper":
                name = "sheep_dorper"
            elif breed == "jacob":
                name = "sheep_jacob_lamb" if role == "lamb" else "sheep_jacob"
            elif breed == "friesian":
                name = f"sheep_friesian_{variant or 'white'}" + ("_ram" if role == "ram" else "")
            else:
                sex = "ram" if role == "ram" else "ewe"
                name = f"sheep_{breed}_{variant or 'white'}_{sex}"
            if sheared and name == "sheep_jacob_lamb":
                name = "sheep_jacob"
            return f"sheep/{name}{'_sheared' if sheared else ''}.png"
    if namespace == "animania_extra":
        if entity_id == "dartfrog":
            return f"amphibians/dartfrogs/{variant or 'blue'}_dart_frog.png"
        if entity_id == "frog":
            return f"amphibians/frogs/{variant or 'default'}_frog.png"
        if entity_id == "toad":
            return "amphibians/toads/toad.png"
        if entity_id == "hamster":
            return f"rodents/hamster_{variant or 'black'}.png"
        if entity_id == "hedgehog":
            return "rodents/hedgehog.png"
        if entity_id == "hedgehog_albino":
            return "rodents/hedgehog_white.png"
        if entity_id.startswith("ferret_"):
            return f"rodents/{entity_id}.png"
        if entity_id.startswith(("doe_", "buck_", "kit_")):
            breed = entity_id.split("_", 1)[1]
            return f"rabbits/rabbit_{'lop_' + (variant or 'black') if breed == 'lop' else breed}.png"
        if entity_id.startswith(("peacock_", "peahen_", "peachick_")):
            colour = entity_id.split("_", 1)[1]
            role = "peacock" if entity_id.startswith("peacock_") else "peachick" if entity_id.startswith("peachick_") else "peafowl"
            return f"peacocks/{role}_{colour}.png"
    if namespace == "animania_catsdogs":
        if entity_id.startswith(("tom_", "queen_", "kitten_")):
            return f"cats/{entity_id.split('_', 1)[1]}.png"
        breed = entity_id.split("_", 1)[1]
        if breed in {"chihuahua", "collie", "labrador", "poodle", "wolf"}:
            return f"dogs/{breed}{variant or '0'}.png"
        return f"dogs/{breed}.png"
    return f"{entity_id}.png"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--output", type=Path, default=Path("build/texture-resolver-audit.json"))
    args = parser.parse_args()
    root = args.root.resolve()
    checked: list[dict] = []
    errors: list[str] = []
    for module, (namespace, source) in MODULES.items():
        resource_root = root / module / "src/main/resources/assets" / namespace / "textures/entity"
        for entity_id in ids_from_source(root, source):
            for variant in variants(entity_id):
                for sheared in ((False, True) if module == "farm" and entity_id.startswith(("ewe_", "ram_", "doe_angora", "buck_angora")) else (False,)):
                    selected = resolved_path(namespace, entity_id, variant, sheared)
                    direct = resource_root / selected
                    flat = resource_root / f"{entity_id}.png"
                    fallback = direct.exists() or flat.exists()
                    record = {"module": module, "entity_id": entity_id, "variant": variant,
                              "sheared": sheared, "selected": selected,
                              "selected_exists": direct.exists(), "flat_fallback_exists": flat.exists(),
                              "pass": fallback}
                    checked.append(record)
                    if not fallback:
                        errors.append(f"{module}:{entity_id}:{variant}: missing {selected} and flat fallback")
    report = {"schema_version": 1, "checked": len(checked), "passed": sum(r["pass"] for r in checked),
              "errors": errors, "error_count": len(errors), "entries": checked}
    output = args.output if args.output.is_absolute() else root / args.output
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({k: report[k] for k in ("schema_version", "checked", "passed", "error_count")}, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
