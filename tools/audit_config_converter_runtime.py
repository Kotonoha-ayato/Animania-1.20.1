"""Exercise the packaged configuration converter as a real executable.

The unit tests prove individual Java helpers.  This audit additionally runs
the built jar against a temporary 1.12-style directory, verifies that input
bytes are unchanged, checks all four generated TOML destinations and proves
that a second run refuses to overwrite existing output.
"""
from __future__ import annotations

import argparse
import json
import shutil
import subprocess
import tempfile
from pathlib import Path

from closure_common import sha256, write_json


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--jar", type=Path)
    parser.add_argument("--output", type=Path, default=Path("build/config-converter-audit.json"))
    args = parser.parse_args()
    root = args.root.resolve()
    jar = (args.jar if args.jar and args.jar.is_absolute() else root / (args.jar or "config-migrator/build/libs/animania-config-migrator-3.0.0.jar")).resolve()
    output = args.output if args.output.is_absolute() else root / args.output
    errors: list[str] = []
    run_records: list[dict] = []
    if not jar.is_file():
        errors.append(f"converter jar missing: {jar}")
    else:
        work = root / "build" / "audit-work" / "config-converter"
        if work.exists():
            shutil.rmtree(work)
        input_dir = work / "input"
        output_dir = work / "output"
        input_dir.mkdir(parents=True)
        config = ("hungerUpdateInterval=1200\n"
                  "hivePlayermadeHoneyRate=500\n"
                  "spawnProbabilityCows=11\n"
                  "legacyThing=true\n")
        input_file = input_dir / "animania.cfg"
        input_file.write_text(config, encoding="utf-8")
        before = sha256(input_file)
        first = subprocess.run(["java", "-jar", str(jar), "--input", str(input_dir), "--output", str(output_dir)],
                               cwd=root, capture_output=True, text=True, timeout=30)
        run_records.append({"run": "initial", "returncode": first.returncode,
                            "stdout": first.stdout[-4000:], "stderr": first.stderr[-4000:]})
        if first.returncode != 0:
            errors.append("initial converter invocation failed")
        if sha256(input_file) != before:
            errors.append("converter modified the read-only input")
        expected = ("animania-common.toml", "animania_farm-common.toml",
                    "animania_extra-common.toml", "animania_catsdogs-common.toml",
                    "animania-config-migration-report.json")
        missing = [name for name in expected if not (output_dir / name).is_file()]
        if missing:
            errors.append("converter did not emit: " + ", ".join(missing))
        report_text = (output_dir / "animania-config-migration-report.json").read_text(encoding="utf-8") if (output_dir / "animania-config-migration-report.json").is_file() else ""
        if '"status":"unmigratable"' not in report_text:
            errors.append("converter report does not contain an unmigratable entry")
        output_hashes = {name: sha256(output_dir / name) for name in expected if (output_dir / name).is_file()}
        second = subprocess.run(["java", "-jar", str(jar), "--input", str(input_dir), "--output", str(output_dir)],
                                cwd=root, capture_output=True, text=True, timeout=30)
        run_records.append({"run": "overwrite-rejection", "returncode": second.returncode,
                            "stdout": second.stdout[-4000:], "stderr": second.stderr[-4000:]})
        if second.returncode == 0:
            errors.append("converter overwrote an existing output directory")
        for name, digest in output_hashes.items():
            if sha256(output_dir / name) != digest:
                errors.append(f"existing output changed after overwrite rejection: {name}")

    report = {
        "schema_version": 1,
        "audit": "config-converter-runtime",
        "audit_version": "v1",
        "jar": str(jar.relative_to(root)).replace("\\", "/") if jar.is_relative_to(root) else str(jar),
        "jar_sha256": sha256(jar) if jar.is_file() else None,
        "runs": run_records,
        "errors": errors,
        "error_count": len(errors),
        "all_passed": not errors,
    }
    write_json(output, report)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(2)


if __name__ == "__main__":
    main()
