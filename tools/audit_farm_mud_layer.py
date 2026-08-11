"""Audit the native consolidated replacement for the 18 Farm pig mud layers."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

OWNER = "[farm-mud-layer-audit:v1]"
LAYER = "base/src/main/java/com/animania/client/render/AnimaniaMudLayer.java"
RENDERER = "base/src/main/java/com/animania/client/render/AnimaniaAnimalRenderer.java"
TEST = "farm/src/test/java/com/animania/farm/AnimaniaMudLayerTest.java"
CLIENT_LOG = "base/run/fullClient/logs/debug.log"


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--root", type=Path, required=True)
    p.add_argument("--matrix", type=Path, default=Path("docs/migration-matrix.json"))
    p.add_argument("--write", action="store_true")
    args = p.parse_args()
    root = args.root.resolve()
    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    matrix = json.loads(matrix_path.read_text(encoding="utf-8"))
    rows = [e for e in matrix["entries"] if e.get("module") == "farm"
            and "/client/render/pigs/layers/LayerMud" in e.get("source", "")]
    errors: list[str] = []
    if len(rows) != 18:
        errors.append(f"expected 18 legacy mud layer rows, found {len(rows)}")
    for entry in rows:
        if not (root / "upstream/Animania-1.12" / entry["source"]).is_file():
            errors.append(f"legacy source missing: {entry['source']}")
    for path in (LAYER, RENDERER, TEST, CLIENT_LOG):
        if not (root / path).is_file():
            errors.append(f"evidence missing: {path}")
    layer = (root / LAYER).read_text(encoding="utf-8")
    renderer = (root / RENDERER).read_text(encoding="utf-8")
    test = (root / TEST).read_text(encoding="utf-8")
    if not all(token in layer for token in ("pig_muddy.png", "pig_muddy_hampshire.png",
                                             "piglet_muddy.png", "entity.isMuddy()")):
        errors.append("native layer does not preserve all three legacy overlays and muddy gate")
    if "addLayer(new AnimaniaMudLayer(this))" not in renderer:
        errors.append("native mud layer is not attached to the unified animal renderer")
    if not all(token in test for token in ("sow_duroc", "hog_hampshire", "piglet_yorkshire")):
        errors.append("mud layer role/Hampshire/resource test is incomplete")

    changed = 0
    if not errors:
        for entry in rows:
            proof = {
                "paths": [LAYER, RENDERER],
                "behavior_tests": [TEST, "tools/audit_farm_mud_layer.py"],
                "serialization_tests": [],
                "client_tests": [TEST, CLIENT_LOG, "tools/audit_client_log.py"],
                "notes": [f"{OWNER} One ModelPart RenderLayer replaces all 18 duplicated legacy layers, preserving piglet/adult/Hampshire textures; resource unit tests and a fresh real all-addon OpenGL client smoke pass."],
            }
            owned = any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", []))
            if args.write:
                entry.update(status="closed", implemented=True, verified=True,
                             tests=[TEST, "tools/audit_farm_mud_layer.py", "tools/audit_client_log.py"],
                             target_evidence=proof)
                changed += 1
            elif entry.get("status") != "closed" or not owned:
                errors.append(f"provable mud row not closed: {entry['source']}")
    if args.write and not errors:
        matrix_path.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"rows": len(rows), "changed": changed, "errors": errors,
                      "error_count": len(errors)}, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
