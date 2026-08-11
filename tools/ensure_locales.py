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

CATS_DOGS_ZH_BREEDS = {
    "american_shorthair": "美国短毛猫", "asiatic": "亚洲猫", "exotic": "异国短毛猫",
    "norwegian": "挪威森林猫", "ocelot": "豹猫", "ragdoll": "布偶猫",
    "siamese": "暹罗猫", "tabby": "虎斑猫", "blood_hound": "寻血猎犬",
    "chihuahua": "吉娃娃", "collie": "柯利犬", "corgi": "柯基犬",
    "dachshund": "腊肠犬", "fox": "狐狸", "german_shepherd": "德国牧羊犬",
    "great_dane": "大丹犬", "greyhound": "灵缇犬", "husky": "哈士奇",
    "labrador": "拉布拉多犬", "pomeranian": "博美犬", "poodle": "贵宾犬",
    "pug": "巴哥犬", "wolf": "狼",
}


def catsdogs_zh_cn_overrides(keys: set[str]) -> dict[str, str]:
    values = {
        "itemGroup.animania_catsdogs": "动物谷：猫与狗",
        "item.animania_catsdogs.entity_egg_cat_random": "随机猫刷怪蛋",
        "item.animania_catsdogs.entity_egg_dog_random": "随机狗刷怪蛋",
        "block.animania_catsdogs.pet_bowl": "宠物食盆",
        "block.animania_catsdogs.cat_bed_1": "猫床",
        "block.animania_catsdogs.cat_bed_2": "猫床",
        "block.animania_catsdogs.cat_tower": "猫爬架",
        "block.animania_catsdogs.dog_house": "狗屋",
        "block.animania_catsdogs.dog_pillow": "狗垫",
        "block.animania_catsdogs.litter_box": "猫砂盆",
    }
    for block in ("pet_bowl", "cat_bed_1", "cat_bed_2", "cat_tower", "dog_house", "dog_pillow", "litter_box"):
        block_key = f"block.animania_catsdogs.{block}"
        values[f"item.animania_catsdogs.{block}"] = values[block_key]
    roles = {
        "male": "公", "female": "母", "puppy": "幼犬",
        "tom": "公猫", "queen": "母猫", "kitten": "幼猫",
    }
    for key in keys:
        prefix = "entity.animania_catsdogs."
        egg_prefix = "item.animania_catsdogs.entity_egg_"
        if key.startswith(prefix):
            identifier = key.removeprefix(prefix)
            target_key = key
            suffix = ""
        elif key.startswith(egg_prefix):
            identifier = key.removeprefix(egg_prefix)
            target_key = key
            suffix = "刷怪蛋"
        else:
            continue
        for role, role_name in roles.items():
            marker = role + "_"
            if identifier.startswith(marker):
                breed = CATS_DOGS_ZH_BREEDS.get(identifier.removeprefix(marker))
                if breed is not None:
                    values[target_key] = f"{breed}（{role_name}）{suffix}"
                break
    return values


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
    parser.add_argument("--modules", nargs="*", choices=sorted(MODULES), default=list(MODULES))
    args = parser.parse_args()
    for module, namespace in MODULES.items():
        if module not in args.modules:
            continue
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
            if module == "catsdogs" and locale == "zh_cn":
                data.update(catsdogs_zh_cn_overrides(set(english)))
            destination.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"ensured {len(LOCALES)} locales for {', '.join(args.modules)}")


if __name__ == "__main__":
    main()
