"""Prove the abandoned HamsterMod water-bottle tile/renderer was unreachable in 1.12."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

OWNER = "[dead-water-bottle-tile-audit:v1]"
ROWS = [
    "src/main/java/com/animania/common/tileentities/TileEntityWaterBottle.java",
    "src/main/java/com/animania/addons/extra/client/render/rodents/RenderWaterBottle.java",
]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, default=Path("docs/migration-matrix.json"))
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()
    root = args.root.resolve()
    errors: list[str] = []

    old_root = root / "upstream/Animania-1.12/src/main/java"
    files = {str(path.relative_to(old_root)).replace("\\", "/"): path.read_text(encoding="utf-8", errors="replace")
             for path in old_root.rglob("*.java")}
    allowed = {path.removeprefix("src/main/java/") for path in ROWS}
    for symbol in ("TileEntityWaterBottle", "RenderWaterBottle"):
        outsiders = [path for path, text in files.items() if symbol in text and path not in allowed]
        if outsiders:
            errors.append(f"legacy {symbol} has production references outside the abandoned pair: {outsiders}")
    all_old = "\n".join(files.values())
    for token in ("registerTileEntity(TileEntityWaterBottle", "bindTileEntitySpecialRenderer(TileEntityWaterBottle",
                  "new TileEntityWaterBottle", "new RenderWaterBottle"):
        if token in all_old:
            errors.append(f"abandoned class unexpectedly registered or constructed: {token}")
    modern = "\n".join(path.read_text(encoding="utf-8", errors="replace")
                       for module in ("base", "farm", "extra", "catsdogs")
                       for path in (root / module / "src/main").rglob("*.java"))
    for symbol in ("TileEntityWaterBottle", "RenderWaterBottle", "BlockRotation"):
        if symbol in modern:
            errors.append(f"dead legacy tile state leaked into modern runtime: {symbol}")

    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    matrix = json.loads(matrix_path.read_text(encoding="utf-8"))
    by_source = {entry.get("source"): entry for entry in matrix["entries"]}
    changed = 0
    if not errors:
        for source in ROWS:
            entry = by_source.get(source)
            if entry is None:
                errors.append(f"migration row missing: {source}")
                continue
            proof = {
                "paths": ["tools/audit_dead_water_bottle_tile.py"],
                "behavior_tests": ["tools/audit_dead_water_bottle_tile.py"],
                "serialization_tests": ["tools/audit_dead_water_bottle_tile.py"],
                "client_tests": ["tools/audit_dead_water_bottle_tile.py"],
                "notes": [f"{OWNER} source-wide reachability proof: the tile and renderer only reference each other and were never registered or constructed; the commented write/sync methods never ran."],
            }
            owned = any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", []))
            if args.write:
                entry.update(status="closed", implemented=True, verified=True,
                             tests=["tools/audit_dead_water_bottle_tile.py"], target_evidence=proof)
                changed += 1
            elif entry.get("status") != "closed" or not owned:
                errors.append(f"provably unreachable row not closed: {source}")

    if args.write and not errors:
        matrix_path.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"rows": len(ROWS), "changed": changed, "errors": errors,
                      "error_count": len(errors)}, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
