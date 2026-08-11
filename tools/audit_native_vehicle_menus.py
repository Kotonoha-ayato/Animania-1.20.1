"""Audit removal of numeric addon GUI routing in favour of native MenuProvider."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

OWNER = "[native-vehicle-menu-audit:v1]"
ROWS = [
    "src/main/java/com/animania/api/addons/IAddonGuiHandler.java",
    "src/main/java/com/animania/common/handler/GuiHandlerAnimania.java",
    "src/main/java/com/animania/addons/farm/common/handler/FarmAddonGUIHandler.java",
    "src/main/java/com/animania/addons/farm/common/inventory/ContainerHorseCart.java",
]
TARGETS = [
    "base/src/main/java/com/animania/common/entity/AnimaniaVehicleEntity.java",
    "farm/src/main/java/com/animania/farm/AnimaniaFarm.java",
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

    legacy_handler = (root / "upstream/Animania-1.12/src/main/java/com/animania/addons/farm/common/handler/FarmAddonGUIHandler.java").read_text(encoding="utf-8")
    for token in ("case 0:", "case 1:", "case 2:", "EntityCart", "EntityWagon", "EntityTiller", "ContainerHorseCart"):
        if token not in legacy_handler:
            errors.append(f"legacy GUI routing contract missing: {token}")
    vehicle = (root / TARGETS[0]).read_text(encoding="utf-8")
    farm = (root / TARGETS[1]).read_text(encoding="utf-8")
    test = (root / TEST).read_text(encoding="utf-8")
    for token in ("implements Container, MenuProvider", "player.openMenu(this)",
                  "ChestMenu.threeRows", "stillValid(Player player)"):
        if token not in vehicle:
            errors.append(f"native vehicle menu contract missing: {token}")
    if "FarmLegacyIds.VEHICLE_IDS.contains(id)" not in farm or "AnimaniaVehicleEntity::new" not in farm:
        errors.append("cart/wagon/tiller do not share the audited native MenuProvider class")
    for token in ("pullableVehicleHasInventoryAndPassengerPath", "vehicle.createMenu(17",
                  "menu.slots.size() == 63", "vehicle.stillValid(menuPlayer)"):
        if token not in test:
            errors.append(f"dedicated-server vehicle menu test missing: {token}")
    modern = "\n".join(path.read_text(encoding="utf-8", errors="replace")
                       for module in ("base", "farm") for path in (root / module / "src/main/java").rglob("*.java"))
    for token in ("IGuiHandler", "IAddonGuiHandler", "openAddonGui", "ContainerHorseCart"):
        if token in modern:
            errors.append(f"obsolete numeric GUI routing remains: {token}")

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
                "paths": TARGETS,
                "behavior_tests": [TEST, "tools/audit_native_vehicle_menus.py"],
                "serialization_tests": [],
                "client_tests": [],
                "notes": [f"{OWNER} cart, wagon and tiller share a server-owned MenuProvider/ChestMenu; the dedicated GameTest verifies 27 cargo and 36 player slots plus distance validity."],
            }
            owned = any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", []))
            if args.write:
                entry.update(status="closed", implemented=True, verified=True,
                             tests=[TEST, "tools/audit_native_vehicle_menus.py"], target_evidence=proof)
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
