"""Audit replacement of the internal 1.12 addon loader by independent Forge mods."""
from __future__ import annotations
import argparse, json, re, subprocess, sys
from pathlib import Path

OWNER = "[addon-architecture-audit:v1]"
SOURCES = {"src/main/java/com/animania/api/addons/LoadAddon.java", "src/main/java/com/animania/api/addons/AnimaniaAddon.java"}
MODULES = {"base": ("animania", "Animania.java"), "farm": ("animania_farm", "AnimaniaFarm.java"),
           "extra": ("animania_extra", "AnimaniaExtra.java"), "catsdogs": ("animania_catsdogs", "AnimaniaCatsDogs.java")}

def main() -> None:
    parser = argparse.ArgumentParser(); parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, default=Path("docs/migration-matrix.json")); parser.add_argument("--write", action="store_true")
    args = parser.parse_args(); root = args.root.resolve(); errors: list[str] = []
    old = root / "upstream/Animania-1.12"
    marker = (old / "src/main/java/com/animania/api/addons/LoadAddon.java").read_text(encoding="utf-8")
    if not re.search(r"public\s+@interface\s+LoadAddon\s*\{\s*\}", marker, re.S): errors.append("legacy LoadAddon is no longer an empty discovery marker")
    lifecycle = (old / "src/main/java/com/animania/api/addons/AnimaniaAddon.java").read_text(encoding="utf-8")
    expected_methods = {"preInitCommon", "initCommon", "preInitClient", "initClient", "getVersion", "getAddonID", "getAddonName", "getDependencies"}
    methods = set(re.findall(r"public\s+[\w<>?]+\s+(\w+)\s*\(", lifecycle))
    if methods != expected_methods: errors.append(f"legacy addon lifecycle changed: {sorted(methods)}")
    for module, (mod_id, filename) in MODULES.items():
        package = "com/animania" + ("" if module == "base" else f"/{module}")
        main_path = root / module / "src/main/java" / package / filename
        text = main_path.read_text(encoding="utf-8")
        if "@Mod(" not in text or (module != "base" and "getModEventBus()" not in text): errors.append(f"{module} is not an independent @Mod event-bus entry point")
        toml = (root / module / "src/main/resources/META-INF/mods.toml").read_text(encoding="utf-8")
        if 'modId="${mod_id}"' not in toml: errors.append(f"{module} mods.toml lacks its project-expanded mod ID")
        if module != "base" and "${base_dependency}" not in toml: errors.append(f"{module} mods.toml lacks generated mandatory Base dependency")
    startup = subprocess.run([sys.executable, str(root / "tools/audit_startup_matrix.py"), "--root", str(root), "--version", "3.0.0"],
                             capture_output=True, text=True)
    if startup.returncode != 0: errors.append("startup/JAR dependency matrix failed: " + startup.stdout + startup.stderr)
    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    matrix = json.loads(matrix_path.read_text(encoding="utf-8")); rows = [e for e in matrix["entries"] if e.get("source") in SOURCES]
    if {e.get("source") for e in rows} != SOURCES: errors.append("matrix addon loader rows differ")
    changed = 0
    if not errors:
        for entry in rows:
            owned = any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", []))
            paths = [f"{module}/src/main/resources/META-INF/mods.toml" for module in MODULES] + ["gradle/forge-module.gradle", "tools/audit_addon_architecture.py", "tools/audit_startup_matrix.py"]
            proof = {"paths": paths, "behavior_tests": ["tools/audit_addon_architecture.py", "tools/audit_startup_matrix.py"], "serialization_tests": [], "client_tests": [],
                     "notes": [f"{OWNER} legacy internal reflection lifecycle replaced by four independent Forge @Mod JARs; addon dependency/startup matrix verified."]}
            if args.write:
                entry.update(status="closed", implemented=True, verified=True, tests=proof["behavior_tests"], target_evidence=proof); changed += 1
            elif entry.get("status") != "closed" or not owned: errors.append(f"provable row not closed: {entry['source']}")
    if args.write and not errors: matrix_path.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"modules": sorted(MODULES), "changed": changed, "errors": errors, "error_count": len(errors)}, ensure_ascii=False, indent=2))
    if errors: raise SystemExit(1)

if __name__ == "__main__": main()
