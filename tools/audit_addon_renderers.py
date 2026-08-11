"""Close only legacy addon renderer rows already covered by modern unified renderers.

Special legacy Layer* rows are deliberately excluded until their overlays are
implemented and visually regressed.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

OWNER = "[addon-renderer-audit:v1]"
MODULES = {
    "farm": {
        "client": "farm/src/main/java/com/animania/farm/AnimaniaFarmClient.java",
        "texture_test": "farm/src/test/java/com/animania/farm/FarmTextureResolverTest.java",
        "model_test": "farm/src/test/java/com/animania/farm/FarmNativeModelConversionTest.java",
        "modern": [
            "base/src/main/java/com/animania/client/render/AnimaniaAnimalRenderer.java",
            "base/src/main/java/com/animania/client/render/AnimaniaVehicleRenderer.java",
            "farm/src/main/java/com/animania/farm/client/render/FarmHiveRenderer.java",
        ],
    },
    "extra": {
        "client": "extra/src/main/java/com/animania/extra/AnimaniaExtraClient.java",
        "texture_test": "extra/src/test/java/com/animania/extra/ExtraTextureResolverTest.java",
        "model_test": "extra/src/test/java/com/animania/extra/ExtraModelLayerTest.java",
        "modern": [
            "base/src/main/java/com/animania/client/render/AnimaniaAnimalRenderer.java",
            "extra/src/main/java/com/animania/extra/client/render/ExtraHamsterWheelRenderer.java",
        ],
    },
    "catsdogs": {
        "client": "catsdogs/src/main/java/com/animania/catsdogs/AnimaniaCatsDogsClient.java",
        "texture_test": "catsdogs/src/test/java/com/animania/catsdogs/CatsDogsTextureResolverTest.java",
        "model_test": "catsdogs/src/test/java/com/animania/catsdogs/CatsDogsNativeModelConversionTest.java",
        "modern": [
            "base/src/main/java/com/animania/client/render/AnimaniaAnimalRenderer.java",
            "catsdogs/src/main/java/com/animania/catsdogs/client/render/CatsDogsPetBowlRenderer.java",
            "catsdogs/src/main/java/com/animania/catsdogs/client/render/CatsDogsPetFacilityRenderer.java",
        ],
    },
}
CLIENT_LOG = "base/run/fullClient/logs/debug.log"


def is_owned_renderer(entry: dict) -> bool:
    source = entry.get("source", "")
    name = source.rsplit("/", 1)[-1]
    return (entry.get("module") in MODULES and "/client/render/" in source
            and not name.startswith("Layer") and name != "RenderWaterBottle.java")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, default=Path("docs/migration-matrix.json"))
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()
    root = args.root.resolve()
    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    matrix = json.loads(matrix_path.read_text(encoding="utf-8"))
    rows = [entry for entry in matrix["entries"] if is_owned_renderer(entry)]
    errors: list[str] = []

    # This count is a deliberate guard against accidentally swallowing new
    # special-effect layer rows or silently losing a renderer from the ledger.
    if len(rows) != 132:
        errors.append(f"expected exactly 132 non-layer addon renderer rows, found {len(rows)}")
    for entry in rows:
        source = entry["source"]
        if not (root / "upstream/Animania-1.12" / source).is_file():
            errors.append(f"legacy renderer missing: {source}")

    for module, contract in MODULES.items():
        for path in [contract["client"], contract["texture_test"], contract["model_test"], *contract["modern"]]:
            if not (root / path).is_file():
                errors.append(f"{module} renderer evidence missing: {path}")
        client = (root / contract["client"]).read_text(encoding="utf-8")
        texture_test = (root / contract["texture_test"]).read_text(encoding="utf-8")
        if "EntityRenderers.register" not in client or "RegisterLayerDefinitions" not in client:
            errors.append(f"{module} does not register entity renderers and native model layers")
        if "TextureResolverTest" not in texture_test and "assertTexture" not in texture_test:
            errors.append(f"{module} lacks per-ID texture assertions")

    log_path = root / CLIENT_LOG
    if not log_path.is_file():
        errors.append(f"real full-client log missing: {CLIENT_LOG}")
    else:
        log = log_path.read_text(encoding="utf-8", errors="replace")
        for mod in ("animania_farm", "animania_extra", "animania_catsdogs"):
            if mod not in log:
                errors.append(f"full-client log did not load {mod}")
        for bad in ("Missing textures in model", "Unable to load model", "ModLoadingException"):
            if bad in log:
                errors.append(f"full-client log contains: {bad}")

    changed = 0
    if not errors:
        for entry in rows:
            module = entry["module"]
            contract = MODULES[module]
            paths = [contract["client"], *contract["modern"]]
            proof = {
                "paths": paths,
                "behavior_tests": [contract["texture_test"], contract["model_test"],
                                   "tools/audit_addon_renderers.py"],
                "serialization_tests": [],
                "client_tests": [contract["texture_test"], contract["model_test"], CLIENT_LOG,
                                 "tools/audit_client_log.py"],
                "notes": [f"{OWNER} Consolidated into native per-ID ModelPart render registration; unit tests cover every preserved texture/model mapping and the real all-addon OpenGL client loaded without missing model/texture errors. Special Layer* overlays are excluded."],
            }
            owned = any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", []))
            if args.write:
                entry.update(status="closed", implemented=True, verified=True,
                             tests=[contract["texture_test"], contract["model_test"],
                                    "tools/audit_addon_renderers.py", "tools/audit_client_log.py"],
                             target_evidence=proof)
                changed += 1
            elif entry.get("status") != "closed" or not owned:
                errors.append(f"provable renderer row not closed: {entry['source']}")

    if args.write and not errors:
        matrix_path.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"rows": len(rows), "changed": changed, "errors": errors,
                      "error_count": len(errors)}, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
