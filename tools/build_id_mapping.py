"""Build a source-evidenced 1.12 -> 1.20.1 registry/module mapping table.

The 1.12 mod registered many objects from constructors (and one spawn egg per
animal) rather than from a central registry.  Consequently this tool reads the
immutable 1.12 sources; the target Content lists are never treated as the
baseline inventory.
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

MODULES = {
    "base": ("animania", "com/animania/common/handler"),
    "farm": ("animania_farm", "com/animania/addons/farm/common/handler"),
    "extra": ("animania_extra", "com/animania/addons/extra/common/handler"),
    "catsdogs": ("animania_catsdogs", "com/animania/addons/catsdogs/common/handler"),
}

# Intentional namespace/spelling changes.  Anything not listed keeps its path
# and merely moves to the owning addon's namespace.
ALIASES = {
    "animania:animania_manual": "animania:animania_manual",
    "animania:block_mud": "animania:mud",
    "animania:block_nest": "animania:nest",
    "animania:block_straw": "animania:straw",
    "animania:block_trough": "animania:trough",
    "animania:block_hamster_wheel": "animania_extra:hamster_wheel",
    "animania:block_hive": "animania_farm:hive",
    "animania:block_wild_hive": "animania_farm:wild_hive",
    "animania:wool": "animania_farm:animania_wool",
    "animania:bee_hive": "animania_farm:hive",
    "animania:item_cart": "animania_farm:cart",
    "animania:item_wagon": "animania_farm:wagon",
    "animania:item_tiller": "animania_farm:tiller",
    "animania:friesian_bucket_milk": "animania_farm:milk_friesian_bucket",
    "animania:holstein_bucket_milk": "animania_farm:milk_holstein_bucket",
    "animania:jersey_bucket_milk": "animania_farm:milk_jersey_bucket",
    "animania:goat_bucket_milk": "animania_farm:milk_goat_bucket",
    "animania:sheep_bucket_milk": "animania_farm:milk_sheep_bucket",
    "animania:animania_honey": "animania_farm:animania_honey",
    "animania:milk_holstein": "animania_farm:milk_holstein",
    "animania:milk_friesian": "animania_farm:milk_friesian",
    "animania:milk_jersey": "animania_farm:milk_jersey",
    "animania:milk_goat": "animania_farm:milk_goat",
    "animania:milk_sheep": "animania_farm:milk_sheep",
    "animania:TileEntityCheeseMold": "animania_farm:cheese_mold",
    "animania:TileEntityHive": "animania_farm:hive",
    "animania:TileEntityHamsterWheel": "animania_extra:hamster_wheel",
}

BASE_BLOCKS = {
    "block_mud", "block_trough", "invisiblock", "block_nest",
    "block_seeds", "block_straw", "salt_lick", "slop",
}
BASE_BLOCK_ITEMS = {"block_mud", "block_trough", "block_nest", "block_straw", "salt_lick"}
BASE_ITEMS = {"animania_manual", "entity_egg_random", "bucket_slop"}


def module_for(path: Path) -> str:
    parts = {part.lower() for part in path.parts}
    return next((name for name in ("farm", "extra", "catsdogs") if name in parts), "base")


def modern_id(module: str, legacy_path: str) -> str:
    old = f"animania:{legacy_path}"
    return ALIASES.get(old, f"{MODULES[module][0]}:{legacy_path}")


def source_ref(path: Path, source: Path, line: int | None = None) -> str:
    value = path.relative_to(source).as_posix()
    return f"{value}:{line}" if line else value


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--source", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    root = args.root.resolve()
    source = (args.source or root / "upstream/Animania-1.12").resolve()
    java_root = source / "src/main/java"
    entries: list[dict] = []
    seen: set[tuple[str, str]] = set()

    def add(module: str, kind: str, path: str, evidence: str, *, generated: bool = False) -> None:
        key = (kind, path)
        if key in seen:
            for entry in entries:
                if (entry["kind"], entry["legacy_id"].split(":", 1)[1]) == key and evidence not in entry["source_evidence"]:
                    entry["source_evidence"].append(evidence)
            return
        seen.add(key)
        old = f"animania:{path}"
        new = modern_id(module, path)
        entries.append({
            "legacy_id": old, "modern_id": new, "module": module, "kind": kind,
            "source_evidence": [evidence], "generated_registration": generated,
            "mapping": "renamed" if old != new and old in ALIASES else "namespace_move" if old != new else "preserved",
            "status": "open", "implemented": False, "verified": False,
        })

    base_handler = java_root / "com/animania/common/handler/BlockHandler.java"
    for path in sorted(BASE_BLOCKS): add("base", "block", path, source_ref(base_handler, source))
    for path in sorted(BASE_BLOCK_ITEMS): add("base", "item", path, source_ref(base_handler, source))
    item_handler = java_root / "com/animania/common/handler/ItemHandler.java"
    for path in sorted(BASE_ITEMS): add("base", "item", path, source_ref(item_handler, source), generated=path.startswith("entity_egg_"))
    add("base", "fluid", "slop", source_ref(base_handler, source))

    # Entity registrations are the authoritative list. registerAnimal creates
    # entity_egg_<id> unless the explicit registerEgg argument is false.
    reg = re.compile(r"register(Animal|Entity)\s*\(\s*([A-Za-z0-9_]+)\.class\s*,\s*\"([a-z0-9_]+)\"([^;]*);", re.S)
    for handler in sorted(java_root.rglob("*EntityHandler.java")):
        if "template" in {part.lower() for part in handler.parts}: continue
        text = handler.read_text(encoding="utf-8", errors="replace")
        module = module_for(handler)
        for reg_kind, class_name, entity_id, tail in reg.findall(text):
            line = text[:text.find(f'"{entity_id}"')].count("\n") + 1
            evidence = source_ref(handler, source, line)
            kind = "vehicle" if reg_kind == "Entity" and class_name in {"EntityCart", "EntityWagon", "EntityTiller"} else "entity"
            add(module, kind, entity_id, evidence)
            if reg_kind == "Animal" and not re.search(r",\s*false\s*\)\s*$", tail.strip()):
                add(module, "item", f"entity_egg_{entity_id}", evidence, generated=True)

    # Literal item constructions in the four handlers cover food and simple
    # items. Custom no-argument items are resolved below from their classes.
    for handler in sorted(java_root.rglob("*ItemHandler.java")):
        if "tileentity" in {part.lower() for part in handler.parts} or "template" in {part.lower() for part in handler.parts}: continue
        text = handler.read_text(encoding="utf-8", errors="replace")
        module = module_for(handler)
        statement = re.compile(r"new\s+(AnimaniaItem|ItemAnimaniaFood(?:Raw)?|ItemEntityEgg)\s*\((.*?)\);", re.S)
        for class_name, args_text in statement.findall(text):
            strings = re.findall(r'\"([a-z0-9_]+)\"', args_text)
            if not strings: continue
            item_id = ("entity_egg_" + strings[0]) if class_name == "ItemEntityEgg" else strings[-1]
            line = text[:text.find(args_text)].count("\n") + 1
            add(module, "item", item_id, source_ref(handler, source, line), generated=class_name == "ItemEntityEgg")

    # Constructor-specific registry paths which are not parameters in the item
    # handler. These literals are source facts, not a hand-authored target list.
    for path in sorted(java_root.rglob("*.java")):
        text = path.read_text(encoding="utf-8", errors="replace")
        module = module_for(path)
        if not ("item" in {part.lower() for part in path.parts} or path.stem.startswith("Item")): continue
        candidates = set(re.findall(r'setRegistryName\s*\([^;]*?\"([a-z0-9_]+)\"\s*\)', text))
        candidates.update(re.findall(r'private\s+String\s+name\s*=\s*\"([a-z0-9_]+)\"', text))
        candidates.update(re.findall(r'super\s*\([^;]*?\"([a-z0-9_]+)\"', text))
        for item_id in candidates:
            if item_id not in {"entity_egg"}:
                add(module, "item", item_id, source_ref(path, source))

    # Blocks constructed with a literal name (BlockCheese, BlockProp, etc.) and
    # class-local name fields used by old constructors.
    for handler in sorted(java_root.rglob("*BlockHandler.java")):
        if "template" in {part.lower() for part in handler.parts}: continue
        text = handler.read_text(encoding="utf-8", errors="replace")
        module = module_for(handler)
        for class_name, block_id in re.findall(r"new\s+(Block[A-Za-z0-9_]*)\s*\(\s*\"([a-z0-9_]+)\"", text):
            add(module, "block", block_id, source_ref(handler, source))
            if class_name == "BlockCheese" and block_id.startswith("cheese_"):
                add(module, "item", block_id.removeprefix("cheese_") + "_cheese_wheel", source_ref(handler, source))
            elif class_name == "BlockProp":
                add(module, "item", block_id, source_ref(handler, source))
        for fluid_id in re.findall(r"new\s+FluidBase\s*\(\s*\"([a-z0-9_]+)\"", text):
            add(module, "fluid", fluid_id, source_ref(handler, source))
        for te_id in re.findall(r"registerTileEntity\s*\([^,]+,\s*\"(?:animania:)?([A-Za-z0-9_]+)\"", text):
            add(module, "block_entity", te_id, source_ref(handler, source))

    # Fixed-name blocks whose handler calls a no-argument constructor.
    for path in sorted(java_root.rglob("Block*.java")):
        text = path.read_text(encoding="utf-8", errors="replace")
        module = module_for(path)
        candidates = set(re.findall(r'private\s+String\s+name\s*=\s*\"([a-z0-9_]+)\"', text))
        candidates.update(re.findall(r'super\s*\(\s*\"([a-z0-9_]+)\"', text))
        for block_id in candidates:
            add(module, "block", block_id, source_ref(path, source))
            if module != "base":
                item_id = {"block_hive": "bee_hive", "block_wild_hive": "wild_hive"}.get(block_id, block_id)
                add(module, "item", item_id, source_ref(path, source))

    # Forge 1.12 generated these bucket items when addBucketForFluid was called;
    # their actual resource/translation spelling is part of the old baseline.
    farm_blocks = java_root / "com/animania/addons/farm/common/handler/FarmAddonBlockHandler.java"
    for family in ("holstein", "friesian", "jersey", "goat", "sheep"):
        add("farm", "item", f"{family}_bucket_milk", source_ref(farm_blocks, source), generated=True)

    # Drop fragments captured from dynamic concatenation; only the derived
    # complete registry path above is real.
    entries[:] = [entry for entry in entries if entry["legacy_id"] != "animania:cheese_wheel"]

    payload = {
        "schema_version": 2,
        "source_baseline": {"name": "Animania 1.12", "path": source.as_posix()},
        "target": {"minecraft": "1.20.1", "forge": "47.4.22", "java": "17", "release": "3.0.0"},
        "counts": {kind: sum(e["kind"] == kind for e in entries) for kind in sorted({e["kind"] for e in entries})},
        "open": len(entries), "closed": 0, "release_allowed": False,
        "entries": sorted(entries, key=lambda item: (item["kind"], item["legacy_id"], item["modern_id"])),
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"entries": len(entries), "counts": payload["counts"], "output": str(args.output)}, ensure_ascii=False))


if __name__ == "__main__":
    main()
