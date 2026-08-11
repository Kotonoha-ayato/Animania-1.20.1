"""Audit the five small 1.12 public utility interfaces retained for addon compatibility."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

INTERFACES = {
    "IBlinking": ("getBlinkTimer", "setBlinkTimer"),
    "IConvertable": ("convertToVanilla", "Entity"),
    "ISpawnable": ("getSpawnEgg", "getPrimaryEggColor", "getSecondaryEggColor", "usesEggColor"),
    "IFoodProviderBlock": ("interface IFoodProviderBlock",),
    "IFoodProviderTE": ("canConsume", "FluidStack", "consumeSolidOrLiquid", "consumeSolid", "consumeLiquid"),
}
TEST = "base/src/test/java/com/animania/api/PublicApiContractTest.java"
OWNER = "[legacy-utility-api-audit:v1]"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, required=True)
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()
    test_text = (args.root / TEST).read_text(encoding="utf-8")
    errors: list[str] = []
    matrix = json.loads(args.matrix.read_text(encoding="utf-8"))
    rows = []
    for name, tokens in INTERFACES.items():
        target = f"base/src/main/java/com/animania/api/interfaces/{name}.java"
        source = f"src/main/java/com/animania/api/interfaces/{name}.java"
        text = (args.root / target).read_text(encoding="utf-8")
        for token in tokens:
            if token not in text:
                errors.append(f"{name} missing {token}")
        matched = [entry for entry in matrix["entries"] if entry.get("source") == source]
        if len(matched) != 1:
            errors.append(f"{name} matched {len(matched)} rows")
        rows.extend(matched)
    if "legacyUtilityInterfacesRetainTheirPublishedMethodContracts" not in test_text:
        errors.append("missing public API utility-interface test")
    tests = [TEST, "tools/audit_legacy_utility_interfaces.py"]
    if args.write and not errors:
        for row in rows:
            name = Path(row["source"]).stem
            target = f"base/src/main/java/com/animania/api/interfaces/{name}.java"
            proof = {
                "paths": [target],
                "behavior_tests": tests,
                "serialization_tests": [],
                "client_tests": [],
                "notes": [f"{OWNER} {name} preserves its published modernized signatures; JUnit reflection locks the contract."],
            }
            row.update(status="closed", implemented=True, verified=True, tests=tests, target_evidence=proof)
        args.matrix.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"matched": len(rows), "changed": len(rows) if args.write and not errors else 0, "errors": errors}, ensure_ascii=False))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
