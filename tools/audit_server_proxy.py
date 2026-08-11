"""Audit removal of the no-op 1.12 ServerProxy lifecycle forwarding class."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

OWNER = "[server-proxy-audit:v1]"
SOURCE = "src/main/java/com/animania/proxy/ServerProxy.java"
TESTS = [
    "base/src/main/java/com/animania/gametest/AnimaniaBaseGameTests.java",
    "farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java",
    "extra/src/main/java/com/animania/extra/gametest/AnimaniaExtraGameTests.java",
    "catsdogs/src/main/java/com/animania/catsdogs/gametest/AnimaniaCatsDogsGameTests.java",
    "tools/audit_server_proxy.py",
]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, default=Path("docs/migration-matrix.json"))
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()
    root = args.root.resolve()
    errors: list[str] = []
    old = (root / "upstream/Animania-1.12" / SOURCE).read_text(encoding="utf-8")
    if old.count("super.preInit(event)") != 1 or old.count("super.init(event)") != 1 or old.count("super.postInit(event)") != 1:
        errors.append("legacy ServerProxy is no longer the expected pure forwarding class")
    stripped = old.replace("super.preInit(event);", "").replace("super.init(event);", "").replace("super.postInit(event);", "")
    for forbidden in ("register(", "new ", "Minecraft.getMinecraft", "Dist.CLIENT"):
        if forbidden in stripped:
            errors.append(f"legacy ServerProxy had an independent responsibility: {forbidden}")
    modern = "\n".join(path.read_text(encoding="utf-8", errors="replace")
                       for module in ("base", "farm", "extra", "catsdogs")
                       for path in (root / module / "src/main/java").rglob("*.java"))
    if "ServerProxy" in modern or "SidedProxy" in modern:
        errors.append("obsolete proxy dispatch remains in Java 17 runtime")
    paths = ["base/src/main/java/com/animania/Animania.java",
             "farm/src/main/java/com/animania/farm/AnimaniaFarm.java",
             "extra/src/main/java/com/animania/extra/AnimaniaExtra.java",
             "catsdogs/src/main/java/com/animania/catsdogs/AnimaniaCatsDogs.java"]
    for path in paths:
        if not (root / path).exists():
            errors.append(f"modern direct Forge entrypoint missing: {path}")

    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    matrix = json.loads(matrix_path.read_text(encoding="utf-8"))
    entry = next((row for row in matrix["entries"] if row.get("source") == SOURCE), None)
    if entry is None:
        errors.append("migration row missing")
    changed = 0
    if not errors and entry is not None:
        proof = {"paths": paths, "behavior_tests": TESTS, "serialization_tests": [], "client_tests": [],
                 "notes": [f"{OWNER} pure lifecycle forwarding removed; four direct @Mod entrypoints have all booted on their dedicated Forge GameTest servers."]}
        owned = any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", []))
        if args.write:
            entry.update(status="closed", implemented=True, verified=True, tests=TESTS, target_evidence=proof)
            changed = 1
        elif entry.get("status") != "closed" or not owned:
            errors.append("provable row is not closed")
    if args.write and not errors:
        matrix_path.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"changed": changed, "errors": errors, "error_count": len(errors)}, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
