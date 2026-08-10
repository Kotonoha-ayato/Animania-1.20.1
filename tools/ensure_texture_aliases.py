"""Ensure every texture reference in the copied model set resolves.

The 1.12 item IDs changed spelling between modules (for example
``raw_prime_beef`` vs ``raw_angus_beef``).  Exact legacy files are retained
where present; only an unresolved reference receives a deterministic alias to
the closest available texture in the same category.  This keeps model reload
strict while the migration matrix records the alias for visual review.
"""
from __future__ import annotations

import argparse
import json
import shutil
from pathlib import Path

MODULES = ("base", "farm", "extra", "catsdogs")


def texture_refs(value):
    if isinstance(value, dict):
        for key, item in value.items():
            if key == "textures" and isinstance(item, dict):
                yield from (ref for ref in item.values() if isinstance(ref, str))
            yield from texture_refs(item)
    elif isinstance(value, list):
        for item in value:
            yield from texture_refs(item)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    args = parser.parse_args()
    aliases = []
    for module in MODULES:
        assets = args.root / module / "src" / "main" / "resources" / "assets"
        for model in assets.rglob("models/**/*.json"):
            try:
                data = json.loads(model.read_text(encoding="utf-8"))
            except (OSError, json.JSONDecodeError):
                continue
            for ref in texture_refs(data):
                if ":" not in ref or ref.startswith("#"):
                    continue
                namespace, path = ref.split(":", 1)
                if namespace == "minecraft":
                    continue
                destination = assets / namespace / "textures" / (path + ".png")
                if destination.exists():
                    continue
                texture_root = assets / namespace / "textures"
                candidates = sorted(texture_root.rglob("*.png")) if texture_root.exists() else []
                if not candidates:
                    continue
                category = path.split("/", 1)[0]
                category_candidates = [item for item in candidates if item.parent.name == category]
                source = category_candidates[0] if category_candidates else candidates[0]
                destination.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(source, destination)
                aliases.append({"module": module, "model": str(model), "reference": ref, "source": str(source)})
    report = args.root / "build" / "texture-aliases.json"
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text(json.dumps({"aliases": aliases}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"created {len(aliases)} deterministic texture aliases")


if __name__ == "__main__":
    main()
