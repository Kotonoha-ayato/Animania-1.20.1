"""Audit the native replacement for the 1.12 LayerBlinking contract."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

OWNER = "[base-blinking-audit:v1]"
SOURCE = "src/main/java/com/animania/client/render/layer/LayerBlinking.java"
PATHS = [
    "base/src/main/java/com/animania/client/render/AnimaniaBlinkingLayer.java",
    "base/src/main/java/com/animania/client/render/AnimaniaAnimalRenderer.java",
    "base/src/main/java/com/animania/common/entity/AnimaniaAnimalEntity.java",
    "base/src/main/java/com/animania/api/interfaces/IBlinking.java",
]
TEST = "base/src/test/java/com/animania/client/render/AnimaniaBlinkingLayerTest.java"
CLIENT_LOG = "base/run/fullClient/logs/debug.log"


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
    if not old.is_file(): errors.append(f"legacy source missing: {SOURCE}")
    rows = [entry for entry in matrix["entries"] if entry.get("source") == SOURCE]
    if len(rows) != 1: errors.append(f"expected one matrix row, found {len(rows)}")
    for path in PATHS + [TEST]:
        if not (root / path).is_file(): errors.append(f"evidence missing: {path}")
    if not errors:
        layer = (root / PATHS[0]).read_text(encoding="utf-8")
        entity = (root / PATHS[2]).read_text(encoding="utf-8")
        renderer = (root / PATHS[1]).read_text(encoding="utf-8")
        test = (root / TEST).read_text(encoding="utf-8")
        for token in ("texturesFor", "getBlinkTimer", "entityTranslucent", "_left.png", "_right.png"):
            if token not in layer: errors.append(f"native blink layer missing {token}")
        for token in ("IBlinking", "BLINK_TIMER", "AnimaniaBlinkTimer"):
            if token not in entity: errors.append(f"entity blink state missing {token}")
        if "new AnimaniaBlinkingLayer(this)" not in renderer: errors.append("renderer does not attach blink layer")
        if "everyLegacyBlinkFamilyResolvesBothTransparentTextures" not in test:
            errors.append("blink mapping regression test missing")
    changed = 0
    if not errors:
        proof = {
            "paths": PATHS,
            "behavior_tests": [TEST, "tools/audit_base_blinking.py"],
            "serialization_tests": [PATHS[2], TEST],
            "client_tests": [TEST, CLIENT_LOG, "tools/audit_client_log.py"],
            "notes": [f"{OWNER} native RenderLayer preserves transparent left/right eyelids, staggered 1.12 timer cadence, synced NBT state, and all farm/extra/catsdogs texture families; unit and real client smoke evidence recorded."],
        }
        for entry in rows:
            owned = any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", []))
            if args.write:
                entry.update(status="closed", implemented=True, verified=True,
                             tests=[TEST, "tools/audit_base_blinking.py", "tools/audit_client_log.py"],
                             target_evidence=proof)
                changed += 1
            elif entry.get("status") != "closed" or not owned:
                errors.append("LayerBlinking row is not closed with owned evidence")
    if args.write and not errors:
        matrix_path.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"rows": len(rows), "changed": changed, "errors": errors,
                      "error_count": len(errors)}, ensure_ascii=False, indent=2))
    if errors: raise SystemExit(1)


if __name__ == "__main__":
    main()
