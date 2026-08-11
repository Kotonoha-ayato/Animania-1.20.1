"""Fail a Forge client smoke log on missing Animania assets or mod construction errors."""
from __future__ import annotations
import argparse, gzip, json, re
from pathlib import Path

PATTERNS = {
    "missing_textures": r"Missing textures in model",
    "missing_model": r"Missing model|Unable to load model",
    "missing_resource": r"FileNotFoundException|Resource .* not found",
    "mod_construction": r"Caught error during event|Failed to create mod instance|Exception caught during firing event",
}

def read_log(path: Path) -> str:
    """Read current or rotated Forge logs without silently ignoring gzip logs."""
    if not path.is_file():
        return ""
    opener = gzip.open if path.suffix == ".gz" else open
    with opener(path, "rt", encoding="utf-8", errors="replace") as stream:
        return stream.read()


def main():
    p=argparse.ArgumentParser(); p.add_argument("--log",type=Path,required=True); p.add_argument("--debug-log",type=Path); p.add_argument("--require-mod",action="append",default=[]); p.add_argument("--require-resource-reload",action="store_true"); a=p.parse_args()
    text=read_log(a.log)
    if not text:
        raise SystemExit(f"client log is missing or empty: {a.log}")
    counts={name:len(re.findall(pattern,text,re.IGNORECASE)) for name,pattern in PATTERNS.items()}
    atlas=bool(re.search(r"Created: .*blocks\.png-atlas",text))
    reload_complete=bool(re.search(r"Reloading ResourceManager: .*mod_resources",text))
    missing_mods=[]
    if a.require_mod:
        debug=read_log(a.debug_log or a.log)
        # Userdev launches report supplied coordinates as well as regular JAR
        # discovery.  Either is valid proof that Forge saw the requested mod.
        missing_mods=[mod for mod in a.require_mod if not re.search(
            rf"(?:Found valid mod file|supplied mod coordinates).*\b{re.escape(mod)}\b|\{{{re.escape(mod)}\}}",
            debug, re.IGNORECASE)]
    gate_errors=sum(counts.values())+(0 if atlas else 1)+len(missing_mods)
    if a.require_resource_reload and not reload_complete:
        gate_errors += 1
    result={"log":str(a.log),"debug_log":str(a.debug_log or a.log),"block_atlas_initialized":atlas,
            "resource_reload_complete":reload_complete,"errors":counts,"required_mods":a.require_mod,
            "missing_required_mods":missing_mods,"require_resource_reload":a.require_resource_reload,
            "error_count":gate_errors}
    print(json.dumps(result,ensure_ascii=False,indent=2))
    if result["error_count"]: raise SystemExit(1)

if __name__=="__main__": main()
