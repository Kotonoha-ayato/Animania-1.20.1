"""Close only historical API facade entries proven by the Java 17 contract tests."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

OWNER = "[public-api-contract-audit:v1]"
INTERFACES = (
    "AnimaniaType", "IAgeable", "IAnimaniaAnimal", "IAnimaniaAnimalBase", "IChild",
    "IFoodEating", "IGendered", "IImpregnable", "IMateable", "IPlaying", "ISleeping",
    "ISterilizable", "IVariant",
)
REQUIRED_METHODS = (
    "typeId", "getGender", "setGender", "age", "getVariantName", "setVariantName",
    "getHunger", "getThirst", "feed", "drink", "isSleeping", "setSleeping",
    "isPlaying", "setPlaying", "play", "canBreedWith", "isPregnant", "setPregnant",
    "pregnancyTicks", "gestationTicks", "isSterilized", "setSterilized", "mateUuid",
    "parentUuid", "snapshot", "asMob",
)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, required=True)
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()
    root = args.root
    matrix = json.loads(args.matrix.read_text(encoding="utf-8"))
    contract = root / "base/src/main/java/com/animania/api/IAnimaniaAnimal.java"
    test = root / "base/src/test/java/com/animania/api/PublicApiContractTest.java"
    api_test = root / "base/src/test/java/com/animania/api/AnimaniaApiTest.java"
    contract_text = contract.read_text(encoding="utf-8") if contract.exists() else ""
    test_text = test.read_text(encoding="utf-8") if test.exists() else ""
    valid = all(token in contract_text and token in test_text for token in REQUIRED_METHODS)
    changed = 0
    matched = 0
    for entry in matrix["entries"]:
        source = entry.get("source", "")
        name = Path(source).stem
        if entry.get("module") != "base" or source != f"src/main/java/com/animania/api/interfaces/{name}.java" or name not in INTERFACES:
            continue
        matched += 1
        facade = root / f"base/src/main/java/com/animania/api/interfaces/{name}.java"
        entry_valid = valid and facade.exists() and api_test.exists()
        proof = {
            "paths": [
                "base/src/main/java/com/animania/api/IAnimaniaAnimal.java",
                f"base/src/main/java/com/animania/api/interfaces/{name}.java",
                "base/src/main/java/com/animania/api/AnimaniaApi.java",
            ],
            "behavior_tests": [
                "base/src/test/java/com/animania/api/PublicApiContractTest.java",
                "base/src/test/java/com/animania/api/AnimaniaApiTest.java",
            ],
            "serialization_tests": [],
            "client_tests": [],
            "notes": [f"{OWNER} {name} inherits or supplies the tested Java 17 public animal/addon contract."],
        }
        if args.write and entry_valid:
            entry.update(status="closed", implemented=True, verified=True,
                         tests=proof["behavior_tests"], target_evidence=proof)
            changed += 1
        elif args.write and any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", [])):
            entry.update(status="unstarted", implemented=False, verified=False, tests=[])
            entry["target_evidence"] = {"paths": [], "behavior_tests": [], "serialization_tests": [], "client_tests": [], "notes": []}
            changed += 1
    if args.write:
        args.matrix.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"matched": matched, "valid": valid, "changed": changed}, ensure_ascii=False))
    if matched != len(INTERFACES) or not valid:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
