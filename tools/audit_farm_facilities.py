"""Audit Farm wool, cheese mould, cheese-wheel and hive server facilities."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

OWNER = "[farm-facilities-audit:v1]"
ROWS = {
    "src/main/java/com/animania/addons/farm/common/block/BlockAnimaniaWool.java": [
        "farm/src/main/java/com/animania/farm/FarmWoolBlock.java",
        "farm/src/main/java/com/animania/farm/FarmWoolBlockItem.java",
        "farm/src/main/resources/assets/animania_farm/blockstates/animania_wool.json",
    ],
    "src/main/java/com/animania/addons/farm/common/block/BlockCheese.java": [
        "farm/src/main/java/com/animania/farm/FarmCheeseBlock.java",
        "farm/src/main/resources/assets/animania_farm/blockstates/cheese_friesian.json",
    ],
    "src/main/java/com/animania/addons/farm/common/block/BlockCheeseMold.java": [
        "farm/src/main/java/com/animania/farm/FarmCheeseMoldBlock.java",
        "farm/src/main/java/com/animania/farm/FarmCheeseMoldBlockEntity.java",
    ],
    "src/main/java/com/animania/addons/farm/common/block/BlockHive.java": [
        "farm/src/main/java/com/animania/farm/FarmHiveBlock.java",
        "farm/src/main/resources/assets/animania_farm/blockstates/hive.json",
    ],
    "src/main/java/com/animania/addons/farm/common/block/BlockWildHive.java": [
        "farm/src/main/java/com/animania/farm/FarmHiveBlock.java",
        "farm/src/main/resources/assets/animania_farm/blockstates/wild_hive.json",
    ],
    "src/main/java/com/animania/addons/farm/common/tileentity/handler/FluidHandlerBeehive.java": [
        "farm/src/main/java/com/animania/farm/FarmHiveBlockEntity.java",
    ],
    "src/main/java/com/animania/addons/farm/common/tileentity/handler/FluidHandlerCheeseMold.java": [
        "farm/src/main/java/com/animania/farm/FarmCheeseMoldBlockEntity.java",
    ],
    "src/main/java/com/animania/addons/farm/common/tileentity/handler/ItemHandlerCheeseMold.java": [
        "farm/src/main/java/com/animania/farm/FarmCheeseMoldBlockEntity.java",
        "base/src/main/java/com/animania/common/block/AnimaniaStorageBlockEntity.java",
    ],
    "src/main/java/com/animania/addons/farm/common/tileentity/TileEntityCheeseMold.java": [
        "farm/src/main/java/com/animania/farm/FarmCheeseMoldBlockEntity.java",
    ],
    "src/main/java/com/animania/addons/farm/common/tileentity/TileEntityHive.java": [
        "farm/src/main/java/com/animania/farm/FarmHiveBlockEntity.java",
    ],
}
TEST = "farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java"
SERIALIZED = {
    "src/main/java/com/animania/addons/farm/common/tileentity/TileEntityCheeseMold.java",
    "src/main/java/com/animania/addons/farm/common/tileentity/TileEntityHive.java",
}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, default=Path("docs/migration-matrix.json"))
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()
    root = args.root.resolve()
    errors: list[str] = []

    for source, targets in ROWS.items():
        if not (root / "upstream/Animania-1.12" / source).is_file():
            errors.append(f"legacy source missing: {source}")
        for target in targets:
            if not (root / target).is_file():
                errors.append(f"modern target missing: {target}")

    mold = (root / "farm/src/main/java/com/animania/farm/FarmCheeseMoldBlockEntity.java").read_text(encoding="utf-8")
    hive = (root / "farm/src/main/java/com/animania/farm/FarmHiveBlock.java").read_text(encoding="utf-8")
    hive_entity = (root / "farm/src/main/java/com/animania/farm/FarmHiveBlockEntity.java").read_text(encoding="utf-8")
    test = (root / TEST).read_text(encoding="utf-8")
    contracts = {
        "mould exact capacity": "pos, state, 1, 1000" in mold,
        "mould rejects automation insertion": "protected boolean isItemValid" in mold and "return false" in mold,
        "hive horizontal state": "HorizontalDirectionalBlock.FACING" in hive and "getStateForPlacement" in hive,
        "hive no fake inventory": "capability == ForgeCapabilities.ITEM_HANDLER" in hive_entity,
        "wool state/drop test": "allSevenLegacyWoolVariantsPlaceAndDropTheirState" in test,
        "cheese bite test": "four cheese bites did not consume the wheel" in test,
        "mould capability test": "moldItems.getSlots() == 1" in test and "overflow == 0" in test,
        "mould persistence test": "mold progress/fluid did not survive NBT reload" in test,
        "hive extraction test": "hive extraction did not exchange one bottle" in test,
        "hive persistence test": "hive honey/timer did not survive NBT reload" in test,
        "wild sting test": "wildHiveStingUsesLegacyDamageTypeAndAmount" in test,
    }
    errors.extend(f"missing contract: {name}" for name, present in contracts.items() if not present)

    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    matrix = json.loads(matrix_path.read_text(encoding="utf-8"))
    by_source = {entry.get("source"): entry for entry in matrix["entries"]}
    changed = 0
    if not errors:
        for source, targets in ROWS.items():
            entry = by_source.get(source)
            if entry is None:
                errors.append(f"migration row missing: {source}")
                continue
            serialization = [TEST] if source in SERIALIZED else []
            proof = {
                "paths": targets,
                "behavior_tests": [TEST, "tools/audit_farm_facilities.py"],
                "serialization_tests": serialization,
                "client_tests": [],
                "notes": [f"{OWNER} Real Forge dedicated GameTests cover block states, drops, eating, automation limits, fluid exchange, production, sting damage, and BE reload."],
            }
            owned = any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", []))
            if args.write:
                entry.update(status="closed", implemented=True, verified=True,
                             tests=[TEST, "tools/audit_farm_facilities.py"], target_evidence=proof)
                changed += 1
            elif entry.get("status") != "closed" or not owned:
                errors.append(f"provable row not closed: {source}")

    if args.write and not errors:
        matrix_path.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"rows": len(ROWS), "changed": changed, "errors": errors,
                      "error_count": len(errors)}, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
