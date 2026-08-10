"""Emit the complete 25-locale JSON set for every module.

The upstream archive has 24 locale files.  ``zh_tw`` is retained as an
additional modern fallback so the release has a deterministic 25-locale
surface; untranslated values deliberately fall back to the English strings.
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

LOCALES = [
    "ar_sa", "bg_bg", "ca_es", "da_dk", "de_de", "en_gb", "en_us", "es_ar",
    "es_es", "fi_fi", "fr_fr", "he_il", "it_it", "ja_jp", "ko_kr", "lol_us",
    "nl_nl", "no_no", "pl_pl", "pt_br", "pt_pt", "ru_ru", "sv_se", "zh_cn", "zh_tw",
]
MODULES = {"base": "animania", "farm": "animania_farm", "extra": "animania_extra", "catsdogs": "animania_catsdogs"}


def humanize(identifier: str) -> str:
    words = identifier.replace("_", " ").split()
    return " ".join(word.capitalize() for word in words)


def discovered_keys(root: Path, module: str, namespace: str) -> dict[str, str]:
    """Build stable display keys for every modern registry/resource ID.

    The upstream archive has many items and entities whose language entries
    were implicit in 1.12 unlocalized names. Explicit JSON keys prevent the
    1.20.1 client from showing raw registry IDs while keeping every locale
    deterministic (non-English locales fall back to these English values).
    """
    resources = root / module / "src" / "main" / "resources"
    values: dict[str, str] = {}
    item_dir = resources / "assets" / namespace / "models" / "item"
    block_dir = resources / "assets" / namespace / "models" / "block"
    state_dir = resources / "assets" / namespace / "blockstates"
    entity_dir = resources / "data" / namespace / "loot_tables" / "entities"
    for path in item_dir.glob("*.json") if item_dir.exists() else ():
        values.setdefault(f"item.{namespace}.{path.stem}", humanize(path.stem))
    for path in block_dir.glob("*.json") if block_dir.exists() else ():
        values.setdefault(f"block.{namespace}.{path.stem}", humanize(path.stem))
    for path in state_dir.glob("*.json") if state_dir.exists() else ():
        values.setdefault(f"block.{namespace}.{path.stem}", humanize(path.stem))
    for path in entity_dir.glob("*.json") if entity_dir.exists() else ():
        values.setdefault(f"entity.{namespace}.{path.stem}", humanize(path.stem))
    # Explicit legacy entity IDs include child and sex registrations even
    # when a generated model is shared or a loot table is intentionally empty.
    for source in (resources.parent / "java").rglob("*LegacyIds.java"):
        try:
            text = source.read_text(encoding="utf-8")
        except OSError:
            continue
        for identifier in __import__("re").findall(r'"([a-z][a-z0-9_]+)"', text):
            if identifier in {"animania", "animania_farm", "animania_extra", "animania_catsdogs"}:
                continue
            values.setdefault(f"entity.{namespace}.{identifier}", humanize(identifier))
    manual_dir = resources / "assets" / namespace / "manual"
    if manual_dir.exists():
        for page in manual_dir.rglob("*.json"):
            try:
                document = json.loads(page.read_text(encoding="utf-8"))
            except (OSError, json.JSONDecodeError):
                continue
            def collect(value: object) -> None:
                if isinstance(value, str) and re.fullmatch(r"[a-z][a-z0-9_.-]*", value) and "." in value:
                    values.setdefault(value, humanize(value.rsplit(".", 1)[-1]))
                elif isinstance(value, list):
                    for child in value:
                        collect(child)
                elif isinstance(value, dict):
                    for child in value.values():
                        collect(child)
            collect(document)
    return values


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    args = parser.parse_args()
    for module, namespace in MODULES.items():
        lang_dir = args.root / module / "src" / "main" / "resources" / "assets" / namespace / "lang"
        source = lang_dir / "en_us.json"
        english = json.loads(source.read_text(encoding="utf-8")) if source.exists() else {}
        english.update({key: english.get(key, value) for key, value in discovered_keys(args.root, module, namespace).items()})
        if module == "base":
            english.setdefault("item.animania.slop_bucket", "Slop Bucket")
            english.setdefault("fluid.animania.slop", "Slop")
        for locale in LOCALES:
            destination = lang_dir / f"{locale}.json"
            if destination.exists() and locale not in {"en_gb", "zh_tw"}:
                try:
                    data = json.loads(destination.read_text(encoding="utf-8"))
                except (OSError, json.JSONDecodeError):
                    data = dict(english)
                for key, value in english.items():
                    data.setdefault(key, value)
            else:
                data = english if locale == "en_us" else dict(english)
            destination.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"ensured {len(LOCALES)} locales per module")


if __name__ == "__main__":
    main()
