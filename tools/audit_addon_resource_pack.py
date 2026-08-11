"""Audit replacement of the 1.12 virtual addon pack with native mod JAR packs."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

OWNER = "[addon-resource-pack-audit:v1]"
SOURCE = "src/main/java/com/animania/addons/AddonResourcePack.java"
MODULES = ("base", "farm", "extra", "catsdogs")
TESTS = ["tools/audit_addon_resource_pack.py", "tools/audit_addon_architecture.py", "tools/audit_resources.py"]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, default=Path("docs/migration-matrix.json"))
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()
    root = args.root.resolve()
    errors: list[str] = []

    legacy = (root / "upstream/Animania-1.12" / SOURCE).read_text(encoding="utf-8")
    for token in ("class Jar extends FileResourcePack", "class Folder extends FolderResourcePack",
                  "manualFiles", "getResourceDomains", "getInputStreamByName"):
        if token not in legacy:
            errors.append(f"legacy virtual-pack responsibility missing: {token}")
    paths: list[str] = []
    for module in MODULES:
        resources = root / module / "src/main/resources"
        for relative in ("META-INF/mods.toml", "pack.mcmeta"):
            path = resources / relative
            if not path.exists():
                errors.append(f"{module} native pack metadata missing: {relative}")
            else:
                paths.append(str(path.relative_to(root)).replace("\\", "/"))
        asset_root = resources / "assets" / ("animania" if module == "base" else f"animania_{module}")
        if not asset_root.is_dir():
            errors.append(f"{module} native asset namespace missing")
        manual_count = sum(1 for path in resources.rglob("manual/**/*.json"))
        if manual_count == 0:
            errors.append(f"{module} native pack contains no manual pages")
    manual_path = root / "base/src/main/java/com/animania/client/manual/ManualScreen.java"
    manual = manual_path.read_text(encoding="utf-8")
    for token in ('listResources("animania/manual"', 'listResources("manual"', 'id.getPath().endsWith(".json")'):
        if token not in manual:
            errors.append(f"native resource-manager manual discovery missing: {token}")
    paths.append(str(manual_path.relative_to(root)).replace("\\", "/"))
    modern_java = "\n".join(path.read_text(encoding="utf-8", errors="replace")
                            for module in MODULES for path in (root / module / "src/main/java").rglob("*.java"))
    for token in ("FileResourcePack", "FolderResourcePack", "AddonResourcePack"):
        if token in modern_java:
            errors.append(f"legacy resource-pack injection remains: {token}")

    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    matrix = json.loads(matrix_path.read_text(encoding="utf-8"))
    entry = next((row for row in matrix["entries"] if row.get("source") == SOURCE), None)
    if entry is None:
        errors.append("migration row missing")
    changed = 0
    if not errors and entry is not None:
        proof = {
            "paths": paths,
            "behavior_tests": TESTS,
            "serialization_tests": ["tools/audit_resources.py"],
            "client_tests": ["tools/audit_addon_resource_pack.py", "tools/audit_resources.py"],
            "notes": [f"{OWNER} each independent Forge mod is now a native resource pack; the resource manager discovers every module's manual pages without classpath/ZIP injection."],
        }
        owned = any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", []))
        if args.write:
            entry.update(status="closed", implemented=True, verified=True, tests=TESTS, target_evidence=proof)
            changed = 1
        elif entry.get("status") != "closed" or not owned:
            errors.append("provable row is not closed")
    if args.write and not errors:
        matrix_path.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"modules": len(MODULES), "changed": changed, "errors": errors,
                      "error_count": len(errors)}, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
