"""Audit and hash the four independent release JARs."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import zipfile
from pathlib import Path

EXPECTED = ("animania-base", "animania-farm", "animania-extra", "animania-catsdogs")
OPTIONAL_COMPAT = ("jei", "jade", "theoneprobe")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def audit_jar(path: Path, module: str) -> list[str]:
    """Check the published archive is standalone and contains no retired APIs."""
    errors: list[str] = []
    forbidden = re.compile(r"craftstudio|geckolib|cofh|redstoneflux|patchouli", re.I)
    try:
        with zipfile.ZipFile(path) as archive:
            names = archive.namelist()
            for name in names:
                if forbidden.search(name):
                    errors.append(f"{path.name}: forbidden dependency/resource {name}")
            if "META-INF/mods.toml" not in names:
                errors.append(f"{path.name}: missing META-INF/mods.toml")
            classes = [name for name in names if name.endswith(".class")]
            if not classes:
                errors.append(f"{path.name}: no compiled classes")
            if module != "animania-base":
                mods = archive.read("META-INF/mods.toml").decode("utf-8", "replace") if "META-INF/mods.toml" in names else ""
                if not re.search(r'modId="animania"\s*\nmandatory=true', mods):
                    errors.append(f"{path.name}: missing mandatory Base dependency")
                own_prefix = {
                    "animania-farm": "com/animania/farm/",
                    "animania-extra": "com/animania/extra/",
                    "animania-catsdogs": "com/animania/catsdogs/",
                }.get(module)
                if own_prefix:
                    foreign = [name for name in classes if name.startswith("com/animania/") and not name.startswith(own_prefix)]
                    if foreign:
                        errors.append(f"{path.name}: bundled Base/foreign classes ({foreign[:3]})")
            if "META-INF/mods.toml" in names:
                mods = archive.read("META-INF/mods.toml").decode("utf-8", "replace")
                for optional_id in OPTIONAL_COMPAT:
                    pattern = (r'\[\[dependencies\.[^\]]+\]\]\s*\n'
                               r'modId="' + re.escape(optional_id) + r'"\s*\nmandatory=false')
                    if not re.search(pattern, mods):
                        errors.append(f"{path.name}: optional compatibility dependency {optional_id} is not declared")
    except (OSError, zipfile.BadZipFile) as exc:
        errors.append(f"{path.name}: invalid JAR: {exc}")
    return errors


def audit_converter(path: Path) -> list[str]:
    """Check the standalone read-only migration tool without treating it as a mod."""
    errors: list[str] = []
    forbidden = re.compile(r"craftstudio|geckolib|cofh|redstoneflux|patchouli", re.I)
    try:
        with zipfile.ZipFile(path) as archive:
            names = archive.namelist()
            if "META-INF/MANIFEST.MF" not in names:
                errors.append(f"{path.name}: missing manifest")
            manifest = archive.read("META-INF/MANIFEST.MF").decode("utf-8", "replace") if "META-INF/MANIFEST.MF" in names else ""
            if "Main-Class: com.animania.migrator.ConfigMigrator" not in manifest:
                errors.append(f"{path.name}: missing ConfigMigrator Main-Class")
            if not any(name.endswith("ConfigMigrator.class") for name in names):
                errors.append(f"{path.name}: missing converter class")
            for name in names:
                if forbidden.search(name):
                    errors.append(f"{path.name}: forbidden dependency/resource {name}")
    except (OSError, zipfile.BadZipFile) as exc:
        errors.append(f"{path.name}: invalid JAR: {exc}")
    return errors


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--version", required=True)
    parser.add_argument("--matrix", type=Path, default=None)
    args = parser.parse_args()
    artifacts = []
    missing = []
    content_errors = []
    for name in EXPECTED:
        candidates = sorted((args.root / name.removeprefix("animania-") / "build" / "libs").glob(f"{name}-*.jar"))
        candidates = [path for path in candidates if not path.name.endswith("-sources.jar") and args.version in path.name]
        if not candidates:
            missing.append(name)
            continue
        jar = candidates[-1]
        digest = sha256(jar)
        sha_file = jar.with_suffix(jar.suffix + ".sha256")
        sha_file.write_text(f"{digest}  {jar.name}\n", encoding="utf-8")
        source_candidates = sorted((jar.parent).glob(f"{name}-*-sources.jar"))
        source_candidates = [path for path in source_candidates if args.version in path.name]
        if not source_candidates:
            missing.append(f"{name}-sources")
            source = None
        else:
            source = source_candidates[-1]
            source_digest = sha256(source)
            source.with_suffix(source.suffix + ".sha256").write_text(
                f"{source_digest}  {source.name}\n", encoding="utf-8")
        content_errors.extend(audit_jar(jar, name))
        artifacts.append({
            "module": name,
            "file": str(jar),
            "bytes": jar.stat().st_size,
            "sha256": digest,
            "sources_file": str(source) if source else None,
            "sources_bytes": source.stat().st_size if source else 0,
            "sources_sha256": source_digest if source else None,
        })
    converter_dir = args.root / "config-migrator" / "build" / "libs"
    converter_candidates = sorted(converter_dir.glob(f"animania-config-migrator-{args.version}.jar"))
    converter_sources = sorted(converter_dir.glob(f"animania-config-migrator-{args.version}-sources.jar"))
    converter = None
    if not converter_candidates:
        missing.append("animania-config-migrator")
    else:
        converter_path = converter_candidates[-1]
        converter_digest = sha256(converter_path)
        converter_path.with_suffix(converter_path.suffix + ".sha256").write_text(
            f"{converter_digest}  {converter_path.name}\n", encoding="utf-8")
        converter_source = converter_sources[-1] if converter_sources else None
        converter_source_digest = None
        if converter_source:
            converter_source_digest = sha256(converter_source)
            converter_source.with_suffix(converter_source.suffix + ".sha256").write_text(
                f"{converter_source_digest}  {converter_source.name}\n", encoding="utf-8")
        else:
            missing.append("animania-config-migrator-sources")
        content_errors.extend(audit_converter(converter_path))
        converter = {
            "file": str(converter_path),
            "bytes": converter_path.stat().st_size,
            "sha256": converter_digest,
            "sources_file": str(converter_source) if converter_source else None,
            "sources_bytes": converter_source.stat().st_size if converter_source else 0,
            "sources_sha256": converter_source_digest,
        }
    matrix_path = args.matrix or (args.root / "docs" / "migration-matrix.json")
    matrix_open = None
    if matrix_path.exists():
        matrix = json.loads(matrix_path.read_text(encoding="utf-8"))
        matrix_open = int(matrix.get("release_audit", {}).get("open", 1)) + int(matrix.get("release_audit", {}).get("unverified", 0))
    report = {
        "version": args.version,
        "expected": list(EXPECTED),
        "missing": missing,
        "artifacts": artifacts,
        "config_migrator": converter,
        "matrix": str(matrix_path),
        "matrix_open": matrix_open,
        "content_errors": content_errors,
        "release_allowed": not missing and len(artifacts) == len(EXPECTED) and not content_errors and matrix_open == 0,
    }
    output = args.root / "build" / "release-artifact-audit.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))
    if missing:
        raise SystemExit("missing release artifacts: " + ", ".join(missing))
    if content_errors:
        raise SystemExit("release JAR content audit failed: " + "; ".join(content_errors))
    if matrix_open != 0:
        raise SystemExit(f"migration matrix is not closed: {matrix_open} open/unverified entries")


if __name__ == "__main__":
    main()
