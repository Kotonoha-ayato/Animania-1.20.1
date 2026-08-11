"""Build the content-level Animania migration ledger used by the release gate."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
from pathlib import Path

MODULE_IDS = {"base": "animania", "farm": "animania_farm", "extra": "animania_extra", "catsdogs": "animania_catsdogs"}
RESOURCE_KINDS = {
    "recipes": "recipe", "loot_tables": "loot_table", "advancements": "advancement",
    "manual": "manual_page", "models": "resource_model", "blockstates": "blockstate",
    "textures": "texture", "sounds": "sound", "lang": "language", "tags": "tag",
}


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def module_for(path: Path) -> str:
    lowered = {part.lower() for part in path.parts}
    for module in ("farm", "extra", "catsdogs"):
        if module in lowered: return module
    return "base"


def class_index(root: Path) -> dict[str, list[Path]]:
    result: dict[str, list[Path]] = {}
    for path in root.rglob("*.java"):
        text = path.read_text(encoding="utf-8", errors="replace")
        for name in re.findall(r"\b(?:class|enum|interface)\s+([A-Za-z0-9_]+)", text):
            result.setdefault(name, []).append(path)
    return result


def java_facts(path: Path | None, root: Path) -> dict:
    if path is None:
        return {"source_paths": [], "classes": [], "numeric_values": [], "behaviors": [], "save_fields": []}
    text = path.read_text(encoding="utf-8", errors="replace")
    return {
        "source_paths": [path.relative_to(root).as_posix()],
        "classes": re.findall(r"\b(?:class|enum|interface)\s+([A-Za-z0-9_]+)", text),
        "numeric_values": sorted(set(re.findall(r"\b[A-Z][A-Z0-9_]*\s*=\s*(-?[0-9.]+[FfDdLl]?)", text))),
        "behaviors": sorted(set(re.findall(r"\b(?:public|protected)\s+(?:static\s+)?(?:[A-Za-z0-9_<>, ?\[\].]+)\s+([a-zA-Z][A-Za-z0-9_]*)\s*\(", text))),
        "save_fields": sorted(set(re.findall(r'\.(?:set|get|hasKey|put\w*|get\w*)\s*\(\s*"([^"]+)"', text))),
    }


def target_for(module: str, kind: str, content_id: str) -> list[str]:
    prefix = f"{module}/src/main"
    java = f"{prefix}/java/com/animania/{module}"
    if module == "base": java = f"{prefix}/java/com/animania"
    if kind == "entity":
        name = "CatsDogs" if module == "catsdogs" else module.title()
        return ["base/src/main/java/com/animania/common/entity/AnimaniaAnimalEntity.java",
                f"{java}/{name}LegacyIds.java" if module != "base" else "base/src/main/java/com/animania/common/entity/AnimaniaAnimalEntity.java"]
    if kind in {"item", "block", "fluid", "vehicle", "block_entity"}:
        name = "CatsDogsContent.java" if module == "catsdogs" else f"{module.title()}Content.java"
        return [f"{java}/{name}"] if module != "base" else ["base/src/main/java/com/animania/common/AnimaniaItems.java", "base/src/main/java/com/animania/common/AnimaniaBlocks.java"]
    if kind == "config":
        name = "CatsDogsConfig.java" if module == "catsdogs" else f"{module.title()}Config.java"
        return [f"{java}/{name}"] if module != "base" else ["base/src/main/java/com/animania/common/config/AnimaniaConfig.java"]
    if kind == "java_model":
        name = "CatsDogs" if module == "catsdogs" else module.title()
        return [f"{java}/{name}LegacyModelLayers.java"] if module != "base" else ["base/src/main/java/com/animania/client/model/AnimaniaAnimalModel.java"]
    if kind in {"craftstudio_model", "animation"}:
        if module == "base":
            suffix = "Layers" if kind == "craftstudio_model" else "Animations"
            return [f"base/src/main/java/com/animania/client/model/BaseNative{suffix}.java"]
        name = "CatsDogs" if module == "catsdogs" else module.title()
        suffix = "Layers" if kind == "craftstudio_model" else "Animations"
        return [f"{java}/{name}Native{suffix}.java"]
    return []


def add(entries: list[dict], seen: set[str], *, key: str, module: str, kind: str, legacy_id: str,
        modern_id: str | None, baseline: dict, reference_paths: list[str] | None = None) -> None:
    if key in seen: return
    seen.add(key)
    entries.append({
        "key": key, "module": module, "kind": kind, "legacy_id": legacy_id, "modern_id": modern_id,
        "baseline": baseline, "migration_reference_1_18": reference_paths or [],
        "target": {"paths": target_for(module, kind, modern_id or legacy_id), "behavior": [], "save_fields": [], "client": []},
        "verification": {"unit_tests": [], "game_tests": [], "client_tests": [], "startup_tests": []},
        "status": "open", "implemented": False, "verified": False, "notes": [],
    })


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--reference", type=Path, required=True)
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    source = args.source.resolve(); reference = args.reference.resolve(); root = args.root.resolve()
    source_classes = class_index(source / "src/main/java")
    reference_classes = class_index(reference / "src/main/java")
    entries: list[dict] = []; seen: set[str] = set()

    registrations: dict[str, tuple[str, str]] = {}
    pattern = re.compile(r"register(Animal|Entity)\s*\(\s*([A-Za-z0-9_]+)\.class\s*,\s*\"([a-z0-9_]+)\"", re.S)
    for path in (source / "src/main/java").rglob("*.java"):
        text = path.read_text(encoding="utf-8", errors="replace")
        for registration_kind, class_name, content_id in pattern.findall(text): registrations[content_id] = (registration_kind, class_name, module_for(path))
    for content_id, (registration_kind, class_name, module) in sorted(registrations.items()):
        path = source_classes.get(class_name, [None])[0]
        reference_paths = [item.relative_to(reference).as_posix() for item in reference_classes.get(class_name, [])]
        facts = java_facts(path, source)
        kind = "vehicle" if registration_kind == "Entity" and class_name in {"EntityCart", "EntityWagon", "EntityTiller"} else "entity"
        add(entries, seen, key=f"{module}:{kind}:{content_id}", module=module, kind=kind,
            legacy_id=f"animania:{content_id}", modern_id=f"{MODULE_IDS[module]}:{content_id}", baseline=facts,
            reference_paths=reference_paths)

    mapping = json.loads((root / "docs/id-mapping.json").read_text(encoding="utf-8"))
    for item in mapping.get("entries", []):
        kind = item.get("kind")
        if kind not in {"item", "block", "fluid", "block_entity", "vehicle"}: continue
        modern = item["modern_id"]; namespace = modern.split(":", 1)[0]
        module = next((key for key, value in MODULE_IDS.items() if value == namespace), "base")
        source_hits = [evidence.rsplit(":", 1)[0] if evidence.rsplit(":", 1)[-1].isdigit() else evidence
                       for evidence in item.get("source_evidence", [])]
        baseline = {"source_paths": source_hits[:8], "classes": [], "numeric_values": [], "behaviors": [], "save_fields": []}
        legacy_path = item["legacy_id"].split(":", 1)[-1]
        add(entries, seen, key=f"{module}:{kind}:{legacy_path}", module=module, kind=kind,
            legacy_id=item["legacy_id"], modern_id=modern, baseline=baseline)

    for path in sorted((source / "src/main/java").rglob("*Config.java")):
        if "template" in {part.lower() for part in path.parts}: continue
        module = module_for(path)
        text = path.read_text(encoding="utf-8", errors="replace")
        for value_type, name, default in re.findall(r"\b(boolean|int|float|double|String)\s+([A-Za-z0-9_]+)\s*=\s*([^;]+);", text):
            baseline = {"source_paths": [path.relative_to(source).as_posix()], "classes": [path.stem],
                        "numeric_values": [default.strip()], "behaviors": [f"config_type:{value_type}"], "save_fields": [name]}
            add(entries, seen, key=f"{module}:config:{name}", module=module, kind="config", legacy_id=name,
                modern_id=name, baseline=baseline)

    # The release contract names these inventories explicitly: 104 legacy Java
    # model classes, 18 CraftStudio models and 8 CraftStudio animations.
    for path in sorted((source / "src/main/java").rglob("Model*.java")):
        module = module_for(path)
        facts = java_facts(path, source)
        add(entries, seen, key=f"{module}:java_model:{path.relative_to(source).as_posix()}",
            module=module, kind="java_model", legacy_id=path.relative_to(source).as_posix(), modern_id=None,
            baseline=facts)

    resource_root = source / "src/main/resources"
    for path in sorted(resource_root.rglob("*")):
        if not path.is_file(): continue
        relative = path.relative_to(resource_root).as_posix()
        if path.suffix == ".csjsmodel": kind = "craftstudio_model"
        elif path.suffix == ".csjsmodelanim": kind = "animation"
        else: kind = next((value for directory, value in RESOURCE_KINDS.items() if f"/{directory}/" in f"/{relative}"), None)
        if kind is None: continue
        module = module_for(path)
        baseline = {"source_paths": [path.relative_to(source).as_posix()], "sha256": sha(path),
                    "classes": [], "numeric_values": [], "behaviors": [], "save_fields": [],
                    "client_representation": [relative] if kind in {"java_model", "craftstudio_model", "animation", "resource_model", "blockstate", "texture", "sound", "language", "manual_page"} else []}
        add(entries, seen, key=f"{module}:{kind}:{relative}", module=module, kind=kind, legacy_id=relative,
            modern_id=None, baseline=baseline)

    payload = {
        "schema_version": 2,
        "source": {"name": source.name, "revision": subprocess.check_output(["git", "-C", str(source), "rev-parse", "HEAD"], text=True).strip()},
        "reference": {"name": reference.name, "revision": subprocess.check_output(["git", "-C", str(reference), "rev-parse", "HEAD"], text=True).strip()},
        "target": {"minecraft": "1.20.1", "forge": "47.4.22", "java": "17", "release": "3.0.0"},
        "counts": {kind: sum(entry["kind"] == kind for entry in entries) for kind in sorted({entry["kind"] for entry in entries})},
        "open": len(entries), "closed": 0, "release_allowed": False,
        "entries": entries,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"entries": len(entries), "counts": payload["counts"], "output": str(args.output)}, ensure_ascii=False, indent=2))


if __name__ == "__main__": main()
