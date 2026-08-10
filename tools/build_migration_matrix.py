"""Build a reproducible, source-only migration inventory for Animania.

The upstream checkouts are intentionally treated as read-only inputs.  This
tool records every Java source and resource, plus the registry-like IDs that
can be extracted without executing the old Forge runtime.  Behavioural and
test fields are deliberately explicit so a release audit cannot silently
consider an item complete.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
from pathlib import Path


MODULES = ("base", "farm", "extra", "catsdogs")
ID_PATTERNS = (
    re.compile(r"(?:setRegistryName|registerBlock|registerItem|registerEntity|registerTileEntity)\s*\(\s*[\"']([^\"']+)", re.I),
    re.compile(r"new\s+ResourceLocation\s*\(\s*[\"'](?:animania|farm|extra|catsdogs)[\"']\s*,\s*[\"']([^\"']+)", re.I),
    re.compile(r"register\s*\(\s*[\"']([a-z0-9_./-]+)[\"']", re.I),
)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def module_for(path: Path) -> str:
    parts = {part.lower() for part in path.parts}
    for module in MODULES[1:]:
        if module in parts:
            return module
    return "base"


def ids_for(text: str) -> list[str]:
    values: set[str] = set()
    for pattern in ID_PATTERNS:
        values.update(match.group(1).lower() for match in pattern.finditer(text))
    return sorted(values)


def source_entries(source: Path) -> list[dict]:
    entries: list[dict] = []
    java_root = source / "src" / "main" / "java"
    for path in sorted(java_root.rglob("*.java")):
        text = path.read_text(encoding="utf-8", errors="replace")
        classes = re.findall(r"\bclass\s+([A-Za-z0-9_]+)", text)
        entries.append({
            "kind": "java",
            "module": module_for(path),
            "source": path.relative_to(source).as_posix(),
            "classes": classes,
            "ids": ids_for(text),
            "sha256": sha256(path),
            "status": "unstarted",
            "implemented": False,
            "verified": False,
            "tests": [],
        })

    resource_root = source / "src" / "main" / "resources"
    for path in sorted(resource_root.rglob("*")):
        if not path.is_file():
            continue
        relative = path.relative_to(source).as_posix()
        entries.append({
            "kind": "resource",
            "module": module_for(path),
            "source": relative,
            "resource_type": path.suffix.lower().lstrip(".") or "file",
            "resource_id": path.relative_to(resource_root).as_posix(),
            "sha256": sha256(path),
            "status": "unstarted",
            "implemented": False,
            "verified": False,
            "tests": [],
        })
    return entries


def _entry_key(entry: dict) -> tuple[str, str, str, str]:
    """Stable identity used to carry closure evidence across regenerations."""
    return (
        str(entry.get("kind", "")),
        str(entry.get("module", "")),
        str(entry.get("source", "")),
        str(entry.get("sha256", "")),
    )


def _carry_closure(entries: list[dict], output: Path) -> None:
    """Preserve verified migration evidence when the pinned source is rebuilt.

    Matrix generation is intentionally source-only, but a release matrix also
    contains target-side evidence.  Re-running the inventory must not erase
    that evidence as long as the source entry's hash is unchanged.  Changed
    source files remain unstarted until the closure tool reviews them again.
    """
    if not output.exists():
        return
    try:
        previous = json.loads(output.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return
    previous_entries = {
        _entry_key(entry): entry
        for entry in previous.get("entries", [])
        if isinstance(entry, dict)
    }
    evidence_fields = (
        "status", "implemented", "verified", "tests", "target_paths",
        "disposition", "notes", "evidence", "closure_version",
    )
    for entry in entries:
        old = previous_entries.get(_entry_key(entry))
        if not old or old.get("status") == "unstarted" or not old.get("verified"):
            continue
        for field in evidence_fields:
            if field in old:
                entry[field] = old[field]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    source = args.source.resolve()
    entries = source_entries(source)
    _carry_closure(entries, args.output)
    unstarted = sum(entry.get("status") == "unstarted" for entry in entries)
    open_entries = sum(entry.get("status") != "closed" for entry in entries)
    unverified = sum(not bool(entry.get("verified")) for entry in entries)
    closed = len(entries) - open_entries
    counts = {
        "java": sum(entry["kind"] == "java" for entry in entries),
        "resources": sum(entry["kind"] == "resource" for entry in entries),
        "entities": sum(
            entry["kind"] == "java" and any(name.lower().startswith("entity") for name in entry["classes"])
            for entry in entries
        ),
    }
    payload = {
        "schema_version": 1,
        "source": source.name,
        "source_revision": subprocess.check_output(["git", "-C", str(source), "rev-parse", "HEAD"], text=True).strip(),
        "baseline": "Animania 1.12",
        "target": {
            "minecraft": "1.20.1",
            "forge": "47.4.22",
            "java": "17",
            "release": "3.0.0",
        },
        "modules": list(MODULES),
        "counts": counts,
        "entries": entries,
        "release_audit": {
            "unstarted": unstarted,
            "open": open_entries,
            "unverified": unverified,
            "closed": closed,
            "release_allowed": not open_entries and not unverified,
        },
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps({"counts": counts, "entries": len(entries), "output": str(args.output)}, ensure_ascii=False))


if __name__ == "__main__":
    main()
