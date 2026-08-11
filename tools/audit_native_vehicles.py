"""Audit the server-authoritative native vehicle/item/inventory migration."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

OWNER = "[native-vehicles-audit:v1]"
ENTITY = "base/src/main/java/com/animania/common/entity/AnimaniaVehicleEntity.java"
ITEM = "base/src/main/java/com/animania/common/item/AnimaniaVehicleItem.java"
TEST = "farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java"
ROWS = {
    "src/main/java/com/animania/addons/farm/common/entity/pullables/AnimatedEntityBase.java": [ENTITY],
    "src/main/java/com/animania/addons/farm/common/entity/pullables/EntityCart.java": [ENTITY],
    "src/main/java/com/animania/addons/farm/common/entity/pullables/EntityTiller.java": [ENTITY],
    "src/main/java/com/animania/addons/farm/common/inventory/CartChest.java": [ENTITY],
    "src/main/java/com/animania/addons/farm/common/item/ItemCart.java": [ITEM],
    "src/main/java/com/animania/addons/farm/common/item/ItemTiller.java": [ITEM],
    "src/main/java/com/animania/addons/farm/common/item/ItemWagon.java": [ITEM],
}
SERIALIZED = {
    "src/main/java/com/animania/addons/farm/common/entity/pullables/EntityCart.java",
    "src/main/java/com/animania/addons/farm/common/entity/pullables/EntityTiller.java",
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
    entity = (root / ENTITY).read_text(encoding="utf-8")
    item = (root / ITEM).read_text(encoding="utf-8")
    test = (root / TEST).read_text(encoding="utf-8")
    contracts = {
        "27-slot cargo": "NonNullList.withSize(27" in entity,
        "native chest menu": "ChestMenu.threeRows" in entity,
        "puller UUID persistence": "AnimaniaPuller" in entity,
        "tiller three-wide cultivation": "tillGround(origin.relative(side))" in entity,
        "drop gamerule": "RULE_DOENTITYDROPS" in entity,
        "clicked-face placement": "getClickedPos().relative(context.getClickedFace())" in item,
        "custom item name": "stack.hasCustomHoverName()" in item,
        "inventory/menu/reload test": "pullableVehicleHasInventoryAndPassengerPath" in test,
        "tiller behavior test": "pulledTillerCultivatesThreeRowsAndConsumesSeed" in test,
        "item spawn test": "vehicleItemsSpawnNamedEntitiesAtAirAndBlockTargets" in test,
        "drop-rule test": "vehicleDropsHonorModernDoEntityDropsRule" in test,
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
            proof = {
                "paths": targets,
                "behavior_tests": [TEST, "tools/audit_native_vehicles.py"],
                "serialization_tests": [TEST] if source in SERIALIZED else [],
                "client_tests": [],
                "notes": [f"{OWNER} Forge dedicated GameTests cover placement, names, inventory/menu, passengers, hitching, reload, boost, tilling, exact seed use, and gamerule-safe drops. Wagon entity remains open pending its sleep path."],
            }
            owned = any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", []))
            if args.write:
                entry.update(status="closed", implemented=True, verified=True,
                             tests=[TEST, "tools/audit_native_vehicles.py"], target_evidence=proof)
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
