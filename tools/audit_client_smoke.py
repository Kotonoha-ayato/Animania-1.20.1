"""Materialize a hashed client bootstrap audit from a Forge client log.

The report intentionally covers only startup/resource-manager readiness.  It
does not claim model geometry or screenshot regression; those remain a
separate stage-5 gate until captured artifacts are supplied.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from audit_client_log import read_log
from closure_common import sha256, write_json


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--log", type=Path, required=True)
    parser.add_argument("--debug-log", type=Path, required=True)
    args = parser.parse_args()
    root = args.root.resolve()
    log = args.log if args.log.is_absolute() else root / args.log
    debug = args.debug_log if args.debug_log.is_absolute() else root / args.debug_log
    text = read_log(log)
    debug_text = read_log(debug)
    required = ("animania", "animania_farm", "animania_extra", "animania_catsdogs")
    result = {
        "schema_version": 1,
        "log": str(log.relative_to(root)).replace("\\", "/") if log.is_relative_to(root) else str(log),
        "debug_log": str(debug.relative_to(root)).replace("\\", "/") if debug.is_relative_to(root) else str(debug),
        "block_atlas_initialized": "Created: " in text and "blocks.png-atlas" in text,
        "resource_reload_complete": "Reloading ResourceManager:" in text and "mod_resources" in text,
        "required_mods": {mod: (mod in debug_text or "{" + mod + "}" in debug_text) for mod in required},
        "errors": {"missing_textures": text.lower().count("missing textures in model"),
                   "missing_model": text.lower().count("missing model") + text.lower().count("unable to load model"),
                   "mod_construction": text.lower().count("failed to create mod instance")},
        "evidence": [
            {"path": str(log.relative_to(root)).replace("\\", "/") if log.is_relative_to(root) else str(log), "sha256": sha256(log)},
            {"path": str(debug.relative_to(root)).replace("\\", "/") if debug.is_relative_to(root) else str(debug), "sha256": sha256(debug)},
        ],
    }
    result["all_passed"] = (result["block_atlas_initialized"] and result["resource_reload_complete"]
                            and all(result["required_mods"].values())
                            and not any(result["errors"].values()))
    output = root / "build/client-smoke-audit.json"
    write_json(output, result)
    print(json.dumps(result, ensure_ascii=False, indent=2))
    if not result["all_passed"]:
        raise SystemExit(2)


if __name__ == "__main__":
    main()
