"""Map pinned 1.12 translations onto active 1.20.1 registry keys.

Legacy Animania stored translations for every module in the Base language
files and used the single ``animania`` namespace.  Modern independent addons
need those values under their own item/block/entity/fluid description IDs.
This tool preserves every parseable old key and fans registered content out
to the canonical module locale without inventing translations.
"""
from __future__ import annotations

import argparse
import json
from collections import defaultdict
from pathlib import Path
from typing import Any


MODULES = {
    "base": "animania", "farm": "animania_farm",
    "extra": "animania_extra", "catsdogs": "animania_catsdogs",
}
LOCALE_ALIASES = {"en_uk": "en_gb"}
ITEM_ALIASES = {
    "bucket_animania_honey": "animania_farm:animania_honey_bucket",
}


def legacy_item_aliases(content_id: str) -> list[str]:
    if content_id == "random":
        return ["entity_egg_random"]
    if content_id.startswith("entity_egg_peafowl_"):
        color = content_id[len("entity_egg_peafowl_"):]
        # 1.12 named the female base class/egg "peafowl"; its interaction
        # handler explicitly rewrote that ID to "peahen".  Peacock and
        # peachick have their own legacy translation keys.
        return [f"entity_egg_peahen_{color}"]
    aliases = {
        "newzealand": "new_zealand", "nigeriandwarf": "nigerian_dwarf",
        "largeblack": "large_black", "largewhite": "large_white", "oldspot": "old_spot",
        "chick_plymouth": "chick_plymouth_rock", "hen_plymouth": "hen_plymouth_rock",
        "rooster_plymouth": "rooster_plymouth_rock",
        "chick_red": "chick_rhode_island_red", "hen_red": "hen_rhode_island_red",
        "rooster_red": "rooster_rhode_island_red",
        "draft_horse_foal": "foal_draft", "draft_horse_mare": "mare_draft",
        "draft_horse_stallion": "stallion_draft",
    }
    normalized = content_id
    for old, new in aliases.items():
        normalized = normalized.replace(old, new)
    return [normalized] if normalized != content_id else []


def parse_legacy_locale(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for raw in path.read_text(encoding="utf-8-sig", errors="replace").splitlines():
        line = raw.strip()
        if not line or line.startswith(("#", "!")) or "=" not in raw:
            continue
        key, value = raw.split("=", 1)
        key = key.strip()
        if key.endswith(".name"):
            key = key[:-5]
        if key:
            values[key] = value.strip()
    return values


def locale_name(path: Path) -> str:
    return LOCALE_ALIASES.get(path.stem.lower(), path.stem.lower())


def load_id_index(root: Path) -> dict[tuple[str, str], list[str]]:
    payload = json.loads((root / "docs/id-mapping.json").read_text(encoding="utf-8"))
    index: dict[tuple[str, str], list[str]] = defaultdict(list)
    for entry in payload.get("entries", []):
        kind = entry.get("kind")
        if kind == "vehicle":
            kind = "entity"
        index[(str(kind), str(entry.get("legacy_id")))].append(str(entry.get("modern_id")))
    return index


def load_active_keys(root: Path) -> dict[str, tuple[str, str]]:
    values: dict[str, tuple[str, str]] = {}
    for module, namespace in MODULES.items():
        path = root / module / "src/main/resources/assets" / namespace / "lang/en_us.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        values.update({key: (module, str(value)) for key, value in data.items()})
    return values


def _translation_key(kind: str, modern_id: str) -> tuple[str, str]:
    namespace, path = modern_id.split(":", 1)
    module = next(module for module, candidate in MODULES.items() if candidate == namespace)
    prefix = "block" if kind == "block" else kind
    return module, f"{prefix}.{namespace}.{path}"


def targets_for_key(key: str, id_index: dict[tuple[str, str], list[str]],
                    active: dict[str, tuple[str, str]]) -> tuple[list[tuple[str, str]], bool]:
    kind: str | None = None
    content_id: str | None = None
    if key.startswith("item.animania_") and ".desc" not in key:
        kind, content_id = "item", key[len("item.animania_"):]
    elif key.startswith("tile.animania_"):
        kind, content_id = "block", key[len("tile.animania_"):]
    elif key.startswith("entity.animania:"):
        kind, content_id = "entity", key[len("entity.animania:"):]
    elif key.startswith("fluid."):
        kind, content_id = "fluid", key[len("fluid."):]
    if kind is None or content_id is None:
        return [], False

    targets: set[tuple[str, str]] = set()
    legacy_id = f"animania:{content_id}"
    modern_ids = list(id_index.get((kind, legacy_id), []))
    if kind == "item" and content_id in ITEM_ALIASES:
        modern_ids.append(ITEM_ALIASES[content_id])
    for modern_id in modern_ids:
        try:
            targets.add(_translation_key(kind, modern_id))
        except (ValueError, StopIteration):
            continue

    prefixes = {
        "item": ("item.",), "block": ("block.",),
        "entity": ("entity.", "item."), "fluid": ("fluid.",),
    }[kind]
    for active_key, (module, _) in active.items():
        if active_key.startswith(prefixes) and active_key.rsplit(".", 1)[-1] == content_id:
            targets.add((module, active_key))
    if kind in {"item", "entity"}:
        for alias in legacy_item_aliases(content_id):
            for active_key, (module, _) in active.items():
                if active_key.startswith(("item.", "entity.")) and active_key.rsplit(".", 1)[-1] == alias:
                    targets.add((module, active_key))
    return sorted(targets), True


def verify_source_locale(root: Path, source: Path, source_module: str) -> tuple[list[Path], list[str], dict[str, int]]:
    id_index = load_id_index(root)
    active = load_active_keys(root)
    locale = locale_name(source)
    values = parse_legacy_locale(source)
    loaded: dict[tuple[str, str], tuple[Path, dict[str, str]]] = {}

    def read(module: str) -> tuple[Path, dict[str, str]]:
        key = (module, locale)
        if key not in loaded:
            namespace = MODULES[module]
            path = root / module / "src/main/resources/assets" / namespace / "lang" / f"{locale}.json"
            loaded[key] = (path, json.loads(path.read_text(encoding="utf-8")) if path.exists() else {})
        return loaded[key]

    errors: list[str] = []
    owner_path, owner = read(source_module)
    for key, value in values.items():
        if owner.get(key) != value:
            errors.append(f"owner locale lost {key}")
        targets, _ = targets_for_key(key, id_index, active)
        for module, modern_key in targets:
            _, data = read(module)
            if data.get(modern_key) != value:
                errors.append(f"{module} locale lost {key} -> {modern_key}")
    paths = sorted({path for path, _ in loaded.values()})
    stats = {"source_keys": len(values), "target_files": len(paths),
             "mapped_values": sum(len(targets_for_key(key, id_index, active)[0]) for key in values)}
    return paths, errors, stats


def source_locales(root: Path) -> list[tuple[str, Path]]:
    source_root = root / "upstream/Animania-1.12/src/main/resources/assets"
    result = [("base", path) for path in sorted((source_root / "animania/lang").glob("*.lang"))]
    result.extend(("catsdogs", path) for path in sorted((source_root / "catsdogs/animania/lang").glob("*.lang")))
    return result


def migrate(root: Path, write: bool) -> dict[str, Any]:
    id_index = load_id_index(root)
    active = load_active_keys(root)
    locale_data: dict[tuple[str, str], dict[str, str]] = {}

    def target_data(module: str, locale: str) -> dict[str, str]:
        key = (module, locale)
        if key not in locale_data:
            namespace = MODULES[module]
            path = root / module / "src/main/resources/assets" / namespace / "lang" / f"{locale}.json"
            locale_data[key] = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
        return locale_data[key]

    report: dict[str, Any] = {"source_locales": 0, "source_keys": 0, "mapped_values": 0,
                              "stale_registry_keys": set(), "files_changed": []}
    for source_module, source in source_locales(root):
        report["source_locales"] += 1
        locale = locale_name(source)
        values = parse_legacy_locale(source)
        report["source_keys"] += len(values)
        # Preserve every source string in a canonical packaged locale.  This
        # retains manual/advancement/config text and documents stale keys.
        owner = target_data(source_module, locale)
        owner.update(values)
        for legacy_key, translated in values.items():
            targets, registry_like = targets_for_key(legacy_key, id_index, active)
            if registry_like and not targets:
                report["stale_registry_keys"].add(legacy_key)
            for module, modern_key in targets:
                target_data(module, locale)[modern_key] = translated
                report["mapped_values"] += 1

    for (module, locale), data in sorted(locale_data.items()):
        namespace = MODULES[module]
        path = root / module / "src/main/resources/assets" / namespace / "lang" / f"{locale}.json"
        rendered = json.dumps(data, ensure_ascii=False, indent=2) + "\n"
        if not path.exists() or path.read_text(encoding="utf-8") != rendered:
            report["files_changed"].append(path.relative_to(root).as_posix())
            if write:
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(rendered, encoding="utf-8")
    report["stale_registry_keys"] = sorted(report["stale_registry_keys"])
    report["stale_registry_key_count"] = len(report["stale_registry_keys"])
    report["files_changed_count"] = len(report["files_changed"])
    return report


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()
    print(json.dumps(migrate(args.root.resolve(), args.write), ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
