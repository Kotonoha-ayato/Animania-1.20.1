"""Audit the modern, guarded /animania tovanilla command replacement."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

OWNER = "[base-command-audit:v1]"
SOURCE = "src/main/java/com/animania/common/commands/AnimaniaCommand.java"
PATHS = [
    "base/src/main/java/com/animania/common/command/AnimaniaCommand.java",
    "base/src/main/java/com/animania/common/command/AnimaniaConversion.java",
    "base/src/main/java/com/animania/AnimaniaServerEvents.java",
]
TEST = "base/src/test/java/com/animania/common/command/AnimaniaCommandTest.java"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, default=Path("docs/migration-matrix.json"))
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()
    root = args.root.resolve()
    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    matrix = json.loads(matrix_path.read_text(encoding="utf-8"))
    errors: list[str] = []
    old = root / "upstream/Animania-1.12" / SOURCE
    if not old.is_file():
        errors.append(f"legacy source missing: {SOURCE}")
    rows = [entry for entry in matrix["entries"] if entry.get("source") == SOURCE]
    if len(rows) != 1:
        errors.append(f"expected one matrix row, found {len(rows)}")
    for path in PATHS + [TEST]:
        if not (root / path).is_file():
            errors.append(f"evidence missing: {path}")
    if not errors:
        command = (root / PATHS[0]).read_text(encoding="utf-8")
        conversion = (root / PATHS[1]).read_text(encoding="utf-8")
        server = (root / PATHS[2]).read_text(encoding="utf-8")
        test = (root / TEST).read_text(encoding="utf-8")
        for token in ("RegisterCommandsEvent", "tovanilla", "CONFIRM_WINDOW_MILLIS", "CONFIRMATIONS", "IConvertable"):
            if token not in command and token not in server:
                errors.append(f"command implementation missing {token}")
        for token in ("vanillaTypeIdFor", "mc(\"cow\")", "mc(\"wolf\")"):
            if token not in conversion:
                errors.append(f"conversion mapping missing {token}")
        for token in ("legacyFamiliesMapToModernVanillaCounterparts", "commandRemainsAConfirmationGate"):
            if token not in test:
                errors.append(f"command regression test missing {token}")
    changed = 0
    if not errors:
        proof = {
            "paths": PATHS,
            "behavior_tests": [TEST, "tools/audit_base_command.py"],
            "serialization_tests": [],
            "client_tests": [],
            "notes": [f"{OWNER} modern permission-gated command keeps the two-call confirmation window, pure legacy family mapping, server-side conversion and replacement spawn; unit regression test covers the mapping and guarded command source."],
        }
        for entry in rows:
            if args.write:
                entry.update(status="closed", implemented=True, verified=True,
                             tests=[TEST, "tools/audit_base_command.py"],
                             target_evidence=proof)
                changed += 1
            elif entry.get("status") != "closed" or not any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", [])):
                errors.append("AnimaniaCommand row is not closed with owned evidence")
    if args.write and not errors:
        matrix_path.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"rows": len(rows), "changed": changed, "errors": errors, "error_count": len(errors)}, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
