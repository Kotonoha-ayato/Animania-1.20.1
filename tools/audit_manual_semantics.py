"""Validate native handbook pages, ordering, icons and cross-module links."""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

MODULES = {"base": "animania", "farm": "animania_farm", "extra": "animania_extra", "catsdogs": "animania_catsdogs"}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--output", type=Path, default=Path("build/manual-semantic-audit.json"))
    args = parser.parse_args()
    root = args.root.resolve()
    pages: dict[tuple[str, str], Path] = {}
    errors: list[str] = []
    for module, namespace in MODULES.items():
        directory = root / module / "src/main/resources/assets" / namespace / "manual"
        for path in sorted(directory.rglob("*.json")):
            relative = "manual/" + path.relative_to(directory).as_posix()
            pages[(namespace, relative)] = path
    links = []
    for (namespace, relative), path in pages.items():
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exc:
            errors.append(f"{path.relative_to(root).as_posix()}: invalid JSON: {exc}")
            continue
        if not isinstance(data, dict) or not isinstance(data.get("contents"), list):
            errors.append(f"{path.relative_to(root).as_posix()}: page must contain an ordered contents array")
            continue
        name = data.get("name")
        if not isinstance(name, str) or not name:
            errors.append(f"{path.relative_to(root).as_posix()}: missing page name")
        for value in data["contents"]:
            if not isinstance(value, str):
                errors.append(f"{path.relative_to(root).as_posix()}: non-string content entry")
                continue
            match = re.match(r"@link@([^#\s]+)(?:#([^\s]+))?", value)
            if not match:
                continue
            target = match.group(1)
            if ":" in target:
                target_ns, target_path = target.split(":", 1)
            else:
                target_ns, target_path = namespace, target
            target_path = target_path.lstrip("/")
            # The 1.12 manual encoded addon pages under the Base namespace.
            # Modern resource packs keep each addon namespace independent, but
            # retain the `manual/farm|extra|catsdogs/...` path so old links
            # remain meaningful. Resolve both spellings explicitly.
            candidates = [(target_ns, target_path)]
            for addon in ("farm", "extra", "catsdogs"):
                if target_ns == "animania" and target_path.startswith(f"manual/{addon}/"):
                    candidates.append((f"animania_{addon}", target_path))
            existing = next((candidate for candidate in candidates if candidate in pages), None)
            exists = existing is not None
            links.append({"from": f"{namespace}:{relative}", "target": f"{target_ns}:{target_path}",
                          "resolved_target": f"{existing[0]}:{existing[1]}" if existing else None, "exists": exists})
            if not exists:
                errors.append(f"{path.relative_to(root).as_posix()}: broken handbook link {target_ns}:{target_path}")
    report = {"schema_version": 1, "pages": len(pages), "links": len(links), "broken_links": sum(not item["exists"] for item in links),
              "errors": errors, "error_count": len(errors), "entries": links}
    output = args.output if args.output.is_absolute() else root / args.output
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({k: report[k] for k in ("schema_version", "pages", "links", "broken_links", "error_count")}, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
