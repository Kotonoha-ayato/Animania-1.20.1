"""Audit that every baseline registry mapping has a target declaration.

This is deliberately only a declaration audit.  It never changes an entry to
implemented/verified; behavior and persistence evidence belong to the content
ledger and release gate.
"""
from __future__ import annotations

import argparse
import json
import re
from collections import Counter
from pathlib import Path

NAMESPACE_MODULE = {
    "animania": "base", "animania_farm": "farm",
    "animania_extra": "extra", "animania_catsdogs": "catsdogs",
}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--mapping", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()
    root = args.root.resolve()
    payload = json.loads(args.mapping.read_text(encoding="utf-8"))

    indexes: dict[str, list[tuple[Path, str]]] = {}
    for module in ("base", "farm", "extra", "catsdogs"):
        values = []
        for path in (root / module / "src/main").rglob("*"):
            if path.is_file() and path.suffix.lower() in {".java", ".json", ".toml", ".mcmeta"}:
                values.append((path, path.read_text(encoding="utf-8", errors="replace")))
        indexes[module] = values

    results = []
    for entry in payload["entries"]:
        namespace, target_path = entry["modern_id"].split(":", 1)
        module = NAMESPACE_MODULE[namespace]
        exact = re.compile(r'\"' + re.escape(target_path) + r'\"')
        evidence = [path.relative_to(root).as_posix() for path, text in indexes[module] if exact.search(text)]
        if not evidence and entry["kind"] == "item" and target_path.startswith("entity_egg_"):
            entity_path = target_path.removeprefix("entity_egg_")
            generator_paths = [path for path, text in indexes[module] if '"entity_egg_"' in text]
            entity_paths = [path for path, text in indexes[module]
                            if re.search(r'\"' + re.escape(entity_path) + r'\"', text)]
            if generator_paths and entity_paths:
                evidence.extend(path.relative_to(root).as_posix() for path in generator_paths + entity_paths)
        if not evidence and entry["kind"] == "item" and target_path.endswith("_bucket"):
            fluid_path = target_path.removesuffix("_bucket")
            generator_paths = [path for path, text in indexes[module] if 'id + "_bucket"' in text]
            fluid_paths = [path for path, text in indexes[module]
                           if re.search(r'\"' + re.escape(fluid_path) + r'\"', text)]
            if generator_paths and fluid_paths:
                evidence.extend(path.relative_to(root).as_posix() for path in generator_paths + fluid_paths)
        declared = bool(evidence)
        results.append({
            "legacy_id": entry["legacy_id"], "modern_id": entry["modern_id"],
            "kind": entry["kind"], "module": module, "declared": declared,
            "target_evidence": sorted(set(evidence)),
        })

    missing = [item for item in results if not item["declared"]]
    report = {
        "schema_version": 1, "mapping_entries": len(results),
        "declared": len(results) - len(missing), "missing": len(missing),
        "passed": not missing,
        "missing_by_kind": dict(sorted(Counter(item["kind"] for item in missing).items())),
        "missing_by_module": dict(sorted(Counter(item["module"] for item in missing).items())),
        "entries": results,
    }
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({key: report[key] for key in ("mapping_entries", "declared", "missing", "passed", "missing_by_kind", "missing_by_module")}, ensure_ascii=False, indent=2))
    if missing:
        print("Missing target declarations:")
        for item in missing: print(f"  {item['kind']} {item['legacy_id']} -> {item['modern_id']}")
        raise SystemExit(1)


if __name__ == "__main__":
    main()
