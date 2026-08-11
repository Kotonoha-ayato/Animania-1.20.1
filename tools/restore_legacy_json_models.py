"""Restore client model payloads from the pinned baseline with modern IDs.

The initial asset generator emitted minimal item models and accidentally
dropped the 1.12 display transforms.  This deterministic repair is limited to
matrix model entries that have not passed migration audit yet; it preserves
the complete source display/texture payload while modernizing parent and
texture resource locations.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

from normalize_legacy_resources import rewrite_texture_refs


PARENT_ALIASES = {
    "builtin/generated": "minecraft:item/generated",
    "item/generated": "minecraft:item/generated",
    "item/handheld": "minecraft:item/handheld",
    "block/carpet": "minecraft:block/carpet",
    "block/cube_all": "minecraft:block/cube_all",
    "block/cube_bottom_top": "minecraft:block/cube_bottom_top",
    "block/cube_column": "minecraft:block/cube_column",
}
TEXTURE_ALIASES = {
    "blocks/planks_oak": "minecraft:block/oak_planks",
    "blocks/stone": "minecraft:block/stone",
    "blocks/water_still": "minecraft:block/water_still",
}

MODULE_NAMESPACES = {
    "base": "animania", "farm": "animania_farm",
    "extra": "animania_extra", "catsdogs": "animania_catsdogs",
}


def canonical_target(root: Path, entry: dict[str, Any]) -> Path | None:
    module = entry["module"]
    resource_id = entry["resource_id"]
    prefix = "assets/animania/" if module == "base" else f"assets/{module}/animania/"
    if not resource_id.startswith(prefix):
        return None
    tail = resource_id[len(prefix):]
    return root / module / "src/main/resources/assets" / MODULE_NAMESPACES[module] / tail


def modernize_parent(value: Any) -> Any:
    if not isinstance(value, dict):
        return value
    result = dict(value)
    parent = result.get("parent")
    if isinstance(parent, str) and parent in PARENT_ALIASES:
        result["parent"] = PARENT_ALIASES[parent]
    textures = result.get("textures")
    if isinstance(textures, dict):
        result["textures"] = {
            key: TEXTURE_ALIASES.get(texture, texture)
            for key, texture in textures.items()
        }
    return result


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, default=Path("docs/migration-matrix.json"))
    args = parser.parse_args()
    root = args.root.resolve()
    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    matrix = json.loads(matrix_path.read_text(encoding="utf-8"))
    changed: list[str] = []
    for entry in matrix.get("entries", []):
        if (entry.get("kind") != "resource"
                or entry.get("resource_type") != "json" or "/models/" not in entry.get("source", "")):
            continue
        source = root / "upstream/Animania-1.12" / entry["source"]
        target = canonical_target(root, entry)
        if target is None or not target.exists():
            continue
        data = json.loads(source.read_text(encoding="utf-8"))
        data = modernize_parent(rewrite_texture_refs(data, entry["module"]))
        rendered = json.dumps(data, ensure_ascii=False, indent=2) + "\n"
        if target.read_text(encoding="utf-8") != rendered:
            target.write_text(rendered, encoding="utf-8")
            changed.append(target.relative_to(root).as_posix())
    print(json.dumps({"changed": len(changed), "paths": changed}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
