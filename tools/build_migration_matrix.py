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
SCHEMA_VERSION = 2
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


def entry_id(kind: str, module: str, source: str, digest: str) -> str:
    """Stable identity; target-side evidence is never part of this hash."""
    value = "\x1f".join((kind, module, source, digest)).encode("utf-8")
    return hashlib.sha256(value).hexdigest()[:24]


def _java_baseline(text: str, path: Path) -> dict:
    parts = {part.lower() for part in path.parts}
    classes = re.findall(r"\bclass\s+([A-Za-z0-9_]+)", text)
    overrides = sorted(set(re.findall(r"@Override\s+(?:public|protected|private)?\s*[\w<>?\[\]]+\s+(\w+)\s*\(", text)))
    event_handlers = sorted(set(re.findall(r"@SubscribeEvent\s+(?:public|private|protected)?\s*\w+[<>?,.\[\]]*\s+(\w+)\s*\(", text)))
    declared_methods = sorted(set(re.findall(
        r"(?m)^\s*(?:(?:public|protected|private|static|final|abstract|default|synchronized)\s+)+"
        r"[\w<>?,.\[\]]+\s+(\w+)\s*\(", text)))
    interface_methods = sorted(set(re.findall(
        r"(?m)^\s*[\w<>?,.\[\]]+\s+(\w+)\s*\([^;{}]*[;{]", text)))
    save_fields = sorted(set(re.findall(r'(?:set|get|hasKey|put|getString|getInteger|getBoolean)\s*\(\s*"([^"]+)"', text)))
    behaviors = sorted(set(overrides + event_handlers + declared_methods + interface_methods +
                           re.findall(r"\b(?:shouldExecute|shouldContinueExecuting|canUse|canContinueToUse|tick|interact|mobInteract|register|init|render)\s*\(", text)))
    is_client = bool(parts & {"client", "models", "model", "render", "renderer"})
    no_runtime_behavior = None
    if not behaviors:
        if re.search(r"\b@interface\b|\benum\b", text):
            no_runtime_behavior = "declaration_only"
        elif re.search(r"\binterface\b", text):
            no_runtime_behavior = "interface_composition"
        elif path.name.endswith("Config.java"):
            no_runtime_behavior = "configuration_fields_only"
        elif path.name == "EntityHandler.java":
            no_runtime_behavior = "legacy_registry_holder"
        else:
            no_runtime_behavior = "declaration_only"
    result = {
        "registry_ids": ids_for(text),
        "classes": classes,
        "numeric_values": re.findall(r"\b(?:int|float|double|long)\s+[A-Za-z0-9_]+\s*=\s*([^;]+);", text),
        "behaviors": behaviors,
        "overrides": overrides,
        "event_handlers": event_handlers,
        "save_fields": save_fields,
        "client_representation": ["java_model_or_renderer"] if is_client else [],
        "source_dependencies": sorted(set(re.findall(r"\bimport\s+([\w.]+)", text))),
    }
    if no_runtime_behavior:
        result["no_runtime_behavior"] = no_runtime_behavior
    return result


def requirements_for(kind: str, module: str, relative: str, baseline: dict | None = None) -> list[str]:
    """Declare the minimum evidence domains before any target evidence exists."""
    if kind == "resource":
        requirements = ["resource"]
        lower = relative.lower()
        if any(token in lower for token in ("models/", "textures/", "blockstates/")):
            requirements.append("client")
        return requirements
    baseline = baseline or {}
    lower = relative.lower()
    requirements = ["implementation"]
    # A Java source entry is a behavior contract unless it is explicitly a
    # client-only model/renderer.  The closure validator rejects empty
    # behavior baselines rather than silently treating them as complete.
    if not baseline.get("no_runtime_behavior") and not any(token in lower for token in ("/client/", "/models/", "/model/", "/render/", "/renderer")):
        requirements.append("behavior")
    if baseline.get("save_fields"):
        requirements.append("serialization")
    if any(token in lower for token in ("/client/", "/models/", "/model/", "/render/", "/renderer")):
        requirements.append("client")
    if any(token in lower for token in ("compat", "/network/", "/event", "handler")):
        requirements.append("integration")
    return list(dict.fromkeys(requirements))


def source_entries(source: Path) -> list[dict]:
    entries: list[dict] = []
    java_root = source / "src" / "main" / "java"
    for path in sorted(java_root.rglob("*.java")):
        text = path.read_text(encoding="utf-8", errors="replace")
        relative = path.relative_to(source).as_posix()
        digest = sha256(path)
        baseline = _java_baseline(text, path)
        module = module_for(path)
        entries.append({
            "kind": "java",
            "module": module,
            "source": relative,
            "entry_id": entry_id("java", module, relative, digest),
            "classes": baseline["classes"],
            "ids": ids_for(text),
            "sha256": digest,
            "status": "unstarted",
            "implemented": False,
            "verified": False,
            "tests": [],
            "baseline": baseline,
            "requirements": requirements_for("java", module, relative, baseline),
            "target_evidence": {
                "paths": [], "behavior_tests": [], "serialization_tests": [],
                "client_tests": [], "notes": [],
            },
            "closure": None,
        })

    resource_root = source / "src" / "main" / "resources"
    for path in sorted(resource_root.rglob("*")):
        if not path.is_file():
            continue
        relative = path.relative_to(source).as_posix()
        digest = sha256(path)
        module = module_for(path)
        entries.append({
            "kind": "resource",
            "module": module,
            "source": relative,
            "entry_id": entry_id("resource", module, relative, digest),
            "resource_type": path.suffix.lower().lstrip(".") or "file",
            "resource_id": path.relative_to(resource_root).as_posix(),
            "sha256": digest,
            "status": "unstarted",
            "implemented": False,
            "verified": False,
            "tests": [],
            "baseline": {
                "registry_ids": [], "classes": [], "numeric_values": [],
                "behaviors": [], "save_fields": [],
                "client_representation": [relative],
            },
            "requirements": requirements_for("resource", module, relative),
            "target_evidence": {
                "paths": [], "behavior_tests": [], "serialization_tests": [],
                "client_tests": [], "notes": [],
            },
            "closure": None,
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
    if previous.get("schema_version") != SCHEMA_VERSION:
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
        if not old or old.get("status") == "unstarted" or not old.get("verified") or not old.get("closure"):
            continue
        for field in evidence_fields + ("closure", "requirements"):
            if field in old:
                entry[field] = old[field]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--reset-closure", action="store_true",
                        help="Explicitly document that all previous closure claims are discarded.")
    parser.add_argument("--carry-closure", action="store_true",
                        help="Carry only schema-v2 verified closures; never use this for a release baseline.")
    args = parser.parse_args()
    source = args.source.resolve()
    entries = source_entries(source)
    # A generated baseline is open by default.  Carrying target evidence is an
    # opt-in diagnostic operation; CI and release work must pass --reset-closure
    # so a committed closed matrix can never silently become trusted again.
    if args.carry_closure and not args.reset_closure:
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
        "schema_version": SCHEMA_VERSION,
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
            "schema_version": SCHEMA_VERSION,
            "unstarted": unstarted,
            "open": open_entries,
            "unverified": unverified,
            "closed": closed,
            "release_allowed": False,
        },
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps({"counts": counts, "entries": len(entries), "output": str(args.output)}, ensure_ascii=False))


if __name__ == "__main__":
    main()
