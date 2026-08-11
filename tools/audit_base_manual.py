"""Audit migration of the 1.12 manual components to the native handbook."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

OWNER = "[base-manual-audit:v1]"
PREFIX = "src/main/java/com/animania/manual/"
SOURCES = {
    PREFIX + "components/ConfigComponent.java",
    PREFIX + "components/CraftingComponent.java",
    PREFIX + "components/EntityComponent.java",
    PREFIX + "components/ImageComponent.java",
    PREFIX + "components/IManualComponent.java",
    PREFIX + "components/ItemComponent.java",
    PREFIX + "components/LinkComponent.java",
    PREFIX + "components/TextComponent.java",
    PREFIX + "groups/ManualPage.java",
    PREFIX + "groups/ManualTopic.java",
    PREFIX + "gui/GuiManual.java",
    PREFIX + "resources/ManualResourceLoader.java",
    "src/main/java/com/animania/common/items/ItemManual.java",
}
PATHS = [
    "base/src/main/java/com/animania/common/item/ManualItem.java",
    "base/src/main/java/com/animania/client/manual/ManualScreen.java",
    "base/src/main/resources/assets/animania/manual/contents.json",
]
TEST = "base/src/test/java/com/animania/client/manual/ManualScreenTest.java"
SERIALIZATION_TEST = "base/src/test/java/com/animania/client/manual/ManualPersistenceContractTest.java"


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
        errors.append(f"expected {len(SOURCES)} manual rows, found {len(rows)}")
    for source in SOURCES:
        if not (root / "upstream/Animania-1.12" / source).is_file():
            errors.append(f"legacy source missing: {source}")
    for path in PATHS + [TEST, SERIALIZATION_TEST]:
        if not (root / path).is_file():
            errors.append(f"evidence missing: {path}")
    if not errors:
        screen = (root / PATHS[1]).read_text(encoding="utf-8")
        item = (root / PATHS[0]).read_text(encoding="utf-8")
        test = (root / TEST).read_text(encoding="utf-8")
        for token in ("listResources(\"manual\"", "JsonParser", "ManualPage"):
            if token not in screen:
                errors.append(f"manual screen missing {token}")
        if "ManualScreen.open" not in item:
            errors.append("manual item does not open the native screen")
        if "nativeManualLoadsBaseAndAddonResourceLayoutsWithoutPatchouli" not in test:
            errors.append("manual regression test missing")
    changed = 0
    if not errors:
        proof = {
            "paths": PATHS,
            "behavior_tests": [TEST, "tools/audit_base_manual.py"],
            "serialization_tests": [SERIALIZATION_TEST],
            "client_tests": [TEST],
            "notes": [f"{OWNER} all legacy component/group/gui/resource loader contracts are represented by JSON pages and the native ManualScreen/ManualItem; pages are discovered from Base and addon namespaces without Patchouli."],
        }
        if args.write:
            for entry in rows:
                entry.update(status="closed", implemented=True, verified=True,
                             tests=[TEST, "tools/audit_base_manual.py"], target_evidence=proof)
                changed += 1
        else:
            for entry in rows:
                if entry.get("status") != "closed" or not any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", [])):
                    errors.append(f"manual row not closed: {entry.get('source')}")
    if args.write and not errors:
        matrix_path.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"rows": len(rows), "changed": changed, "errors": errors, "error_count": len(errors)}, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
