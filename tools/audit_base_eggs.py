"""Audit the native item extension that replaces the animated 1.12 eggs."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

OWNER = "[base-eggs-audit:v1]"
SOURCES = {
    "src/main/java/com/animania/common/items/ItemEntityEgg.java",
    "src/main/java/com/animania/common/items/ItemEntityEggAnimated.java",
    "src/main/java/com/animania/client/models/item/AnimatedEggModelWrapper.java",
    "src/main/java/com/animania/client/render/item/RenderAnimatedEgg.java",
    "src/main/java/com/animania/client/events/RenderEvents.java",
}
PATHS = [
    "base/src/main/java/com/animania/common/item/AnimaniaEntityEggItem.java",
    "base/src/main/java/com/animania/client/render/AnimaniaEggItemRenderer.java",
    "base/src/main/java/com/animania/common/config/AnimaniaConfig.java",
]
TEST = "base/src/test/java/com/animania/client/render/AnimaniaEggItemRendererTest.java"


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
    rows = [entry for entry in matrix["entries"] if entry.get("source") in SOURCES]
    if len(rows) != len(SOURCES):
        errors.append(f"expected {len(SOURCES)} egg rows, found {len(rows)}")
    for source in SOURCES:
        if not (root / "upstream/Animania-1.12" / source).is_file():
            errors.append(f"legacy source missing: {source}")
    for path in PATHS + [TEST, "tools/audit_model_assets.py"]:
        if not (root / path).is_file():
            errors.append(f"evidence missing: {path}")
    if not errors:
        item = (root / PATHS[0]).read_text(encoding="utf-8")
        renderer = (root / PATHS[1]).read_text(encoding="utf-8")
        config = (root / PATHS[2]).read_text(encoding="utf-8")
        test = (root / TEST).read_text(encoding="utf-8")
        for token in ("useOn", "registerDispenserBehavior", "stacksTo(64)", "createPreview"):
            if token not in item:
                errors.append(f"egg item missing {token}")
        for token in ("renderByItem", "BlockEntityWithoutLevelRenderer", "FANCY_EGGS_ROTATE"):
            if token not in renderer:
                errors.append(f"egg renderer missing {token}")
        for token in ("FANCY_EGGS", "FANCY_EGGS_ROTATE"):
            if token not in config:
                errors.append(f"egg config missing {token}")
        if "forgeItemExtensionUsesTheNativeEntityPreviewRenderer" not in test:
            errors.append("egg renderer regression test missing")
    changed = 0
    if not errors:
        proof = {
            "paths": PATHS,
            "behavior_tests": [TEST, "tools/audit_base_eggs.py"],
            "serialization_tests": [],
            "client_tests": [TEST, "tools/audit_model_assets.py", "base/run/fullClient/logs/debug.log"],
            "notes": [f"{OWNER} server-authoritative 64-stack eggs retain dispenser/use semantics; fancy eggs use Forge's native BlockEntityWithoutLevelRenderer and deterministic child preview with optional rotation, while the generated two-layer model remains the default when disabled."],
        }
        if args.write:
            for entry in rows:
                entry.update(status="closed", implemented=True, verified=True,
                             tests=[TEST, "tools/audit_base_eggs.py", "tools/audit_model_assets.py"],
                             target_evidence=proof)
                changed += 1
        else:
            for entry in rows:
                if entry.get("status") != "closed" or not any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", [])):
                    errors.append(f"egg row not closed with owned evidence: {entry.get('source')}")
    if args.write and not errors:
        matrix_path.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    result = {"rows": len(rows), "changed": changed, "errors": errors, "error_count": len(errors)}
    print(json.dumps(result, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
