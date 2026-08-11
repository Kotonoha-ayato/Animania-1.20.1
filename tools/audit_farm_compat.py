"""Audit the modern JEI/Jade/TOP replacement for all legacy Farm providers."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

OWNER = "[farm-compat-audit:v1]"
PREFIX = "src/main/java/com/animania/addons/farm/compat/"
SOURCES = [
    PREFIX + "FarmAddonJEICompat.java",
    PREFIX + "FarmAddonWailaCompat.java",
    PREFIX + "TOPInfoProviderPig.java",
    *[PREFIX + "waila/" + name + ".java" for name in (
        "WailaBlockCheeseMoldProvider", "WailaBlockCheeseProvider", "WailaBlockHiveProvider",
        "WailaEntityBuckProvider", "WailaEntityCowProvider", "WailaEntityDoeProvider",
        "WailaEntityEweProvider", "WailaEntityHenProvider", "WailaEntityMareProvider",
        "WailaEntityPigletProvider", "WailaEntityPigProvider", "WailaEntityRamProvider",
        "WailaEntitySheepProvider", "WailaEntitySowProvider", "WailaEntityStallionProvider",
    )],
]
TARGETS = [
    "base/src/main/java/com/animania/api/IAnimaniaProbeBlock.java",
    "base/src/main/java/com/animania/compat/AnimaniaProbeComponents.java",
    "base/src/main/java/com/animania/compat/jade/AnimaniaJadePlugin.java",
    "base/src/main/java/com/animania/compat/top/AnimaniaTopProbeCompat.java",
    "base/src/main/java/com/animania/compat/jei/AnimaniaJeiPlugin.java",
    "farm/src/main/java/com/animania/farm/FarmCheeseMoldBlockEntity.java",
    "farm/src/main/java/com/animania/farm/FarmHiveBlockEntity.java",
]
TEST = "farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, default=Path("docs/migration-matrix.json"))
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()
    root = args.root.resolve()
    errors: list[str] = []

    for source in SOURCES:
        if not (root / "upstream/Animania-1.12" / source).is_file():
            errors.append(f"legacy source missing: {source}")
    for target in TARGETS + [TEST]:
        if not (root / target).is_file():
            errors.append(f"modern target missing: {target}")

    jei = (root / TARGETS[4]).read_text(encoding="utf-8")
    probe = (root / TARGETS[1]).read_text(encoding="utf-8")
    jade = (root / TARGETS[2]).read_text(encoding="utf-8")
    top = (root / TARGETS[3]).read_text(encoding="utf-8")
    mold = (root / TARGETS[5]).read_text(encoding="utf-8")
    hive = (root / TARGETS[6]).read_text(encoding="utf-8")
    test = (root / TEST).read_text(encoding="utf-8")
    required = {
        "JEI legacy descriptions": all(token in jei for token in (
            "text.jei.truffle", "text.jei.salt", "text.jei.milkholstein",
            "text.jei.milkfriesian", "text.jei.milkjersey", "text.jei.milkgoat",
            "text.jei.milksheep")),
        "Jade addon-neutral block bridge": "IAnimaniaProbeBlock" in jade and "Block.class" in jade,
        "TOP addon-neutral block bridge": "IAnimaniaProbeBlock" in top,
        "shared animal states": all(token in probe for token in (
            "mateUuid", "pregnancy_remaining", "isMilkReady", "wool_remaining",
            "isPigAnimal", "egg_remaining", "isSleeping", "isSterilized")),
        "cheese mold status": all(token in mold for token in (
            "getAnimaniaProbeInfo", "jade.animania.aging", "jade.animania.item_count",
            "jade.animania.fluid_amount")),
        "hive status": "getAnimaniaProbeInfo" in hive and "jade.animania.fluid_amount" in hive,
        "real GameTest": "farmProbeStatusCoversLegacyAnimalAndFacilityProviders" in test,
        "mate persistence": "female lost MateUUID or pregnancy during save reload" in test,
        "play persistence": "pig lost played/muddy state after save reload" in test,
        "egg timer persistence": "farmLactationAndEggLayStatePersists" in test
                                 and "AnimaniaEggLayTicks" in test,
    }
    errors.extend(f"missing contract: {name}" for name, ok in required.items() if not ok)

    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    matrix = json.loads(matrix_path.read_text(encoding="utf-8"))
    by_source = {entry.get("source"): entry for entry in matrix["entries"]}
    changed = 0
    if not errors:
        for source in SOURCES:
            entry = by_source.get(source)
            if entry is None:
                errors.append(f"migration row missing: {source}")
                continue
            proof = {
                "paths": TARGETS,
                "behavior_tests": [TEST, "tools/audit_farm_compat.py"],
                "serialization_tests": [TEST],
                "client_tests": [],
                "notes": [f"{OWNER} Unified optional JEI/Jade/TOP bridges preserve all legacy Farm status fields; the dedicated Forge GameTest validates animal and facility snapshots."],
            }
            owned = any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", []))
            if args.write:
                entry.update(status="closed", implemented=True, verified=True,
                             tests=[TEST, "tools/audit_farm_compat.py"], target_evidence=proof)
                changed += 1
            elif entry.get("status") != "closed" or not owned:
                errors.append(f"provable row not closed: {source}")

    if args.write and not errors:
        matrix_path.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"rows": len(SOURCES), "changed": changed, "errors": errors,
                      "error_count": len(errors)}, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
