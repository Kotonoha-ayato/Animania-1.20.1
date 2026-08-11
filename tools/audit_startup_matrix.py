"""Audit the install matrix encoded by the four published mods.toml files.

This is a deterministic preflight for every Base/addon combination.  It
checks the same dependency graph Forge uses and explicitly records the
expected missing-Base error for addon-only installs; a real client/server
launch is still run by the environment-specific release checklist.
"""
from __future__ import annotations

import argparse
import itertools
import json
import re
import zipfile
from pathlib import Path

MODULES = ("base", "farm", "extra", "catsdogs")
JAR_NAMES = {
    "base": "animania-base",
    "farm": "animania-farm",
    "extra": "animania-extra",
    "catsdogs": "animania-catsdogs",
}


def jar_for(root: Path, module: str, version: str) -> Path:
    candidates = sorted((root / module / "build" / "libs").glob(f"{JAR_NAMES[module]}-*-{version}.jar"))
    candidates = [path for path in candidates if not path.name.endswith("-sources.jar")]
    if len(candidates) != 1:
        raise FileNotFoundError(f"expected one {module} jar, found {len(candidates)}")
    return candidates[0]


def parse_mods(jar: Path) -> tuple[str, set[str], list[str]]:
    with zipfile.ZipFile(jar) as archive:
        text = archive.read("META-INF/mods.toml").decode("utf-8", "replace")
    mod_id_match = re.search(r"\[\[mods\]\].*?modId=\"([^\"]+)\"", text, re.S)
    if not mod_id_match:
        raise ValueError(f"{jar}: missing [[mods]] modId")
    dependencies = set(re.findall(r"\[\[dependencies\.[^\]]+\]\]\s*\nmodId=\"([^\"]+)\"\s*\nmandatory=true", text))
    optional = re.findall(r"\[\[dependencies\.[^\]]+\]\]\s*\nmodId=\"([^\"]+)\"\s*\nmandatory=false", text)
    return mod_id_match.group(1), dependencies, optional


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--version", required=True)
    args = parser.parse_args()
    metadata = {}
    errors: list[str] = []
    runtime_build = args.root / "base" / "build.gradle"
    runtime_text = runtime_build.read_text(encoding="utf-8") if runtime_build.is_file() else ""
    runtime_tokens = [
        "fullClient {",
        "gameTestServer {",
        "runFullGameTestServer",
        "animania_farm { source project(':farm').sourceSets.main }",
        "animania_extra { source project(':extra').sourceSets.main }",
        "animania_catsdogs { source project(':catsdogs').sourceSets.main }",
        "animania,animania_farm,animania_extra,animania_catsdogs",
        "dependsOn ':farm:classes', ':extra:classes', ':catsdogs:classes'",
    ]
    missing_runtime_tokens = [token for token in runtime_tokens if token not in runtime_text]
    if missing_runtime_tokens:
        errors.append(f"all-installed runtime configuration missing tokens: {missing_runtime_tokens}")
    for module in MODULES:
        try:
            jar = jar_for(args.root, module, args.version)
            mod_id, mandatory, optional = parse_mods(jar)
            metadata[module] = {"jar": str(jar), "mod_id": mod_id, "mandatory": sorted(mandatory), "optional": sorted(optional)}
        except (OSError, ValueError) as exc:
            errors.append(str(exc))
    combinations = []
    addon_modules = MODULES[1:]
    for mask in range(1 << len(addon_modules)):
        selected = ["base"] + [addon_modules[i] for i in range(len(addon_modules)) if mask & (1 << i)]
        ids = [metadata[module]["mod_id"] for module in selected if module in metadata]
        missing = sorted({dependency for module in selected for dependency in metadata.get(module, {}).get("mandatory", []) if dependency not in ids and dependency != "forge" and dependency != "minecraft"})
        combinations.append({"modules": selected, "status": "pass" if not missing else "fail", "missing_mandatory": missing})
    # Every addon-only installation must be rejected because Base is absent.
    missing_base_cases = []
    for module in addon_modules:
        if module not in metadata:
            continue
        mandatory = metadata[module]["mandatory"]
        if "animania" not in mandatory:
            errors.append(f"{module}: mandatory Base dependency not encoded")
        missing_base_cases.append({"modules": [module], "expected": "Forge dependency error for missing animania Base"})
    report = {"version": args.version, "modules": metadata, "combinations": combinations,
              "missing_base_cases": missing_base_cases,
              "all_installed_runtime": {"gradle": str(runtime_build), "missing_tokens": missing_runtime_tokens,
                                          "full_client_task": ":base:runFullClient",
                                          "full_gametest_task": ":base:runFullGameTestServer"},
              "errors": errors}
    output = args.root / "build" / "startup-matrix-audit.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))
    if errors or any(case["status"] != "pass" for case in combinations):
        raise SystemExit(1)


if __name__ == "__main__":
    main()
