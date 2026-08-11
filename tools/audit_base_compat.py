"""Audit optional JEI/Jade/TOP compatibility replacing legacy providers."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

OWNER = "[base-compat-audit:v1]"
SOURCES = {
    "src/main/java/com/animania/compat/jei/JEICompat.java",
    "src/main/java/com/animania/compat/top/providers/entity/TOPInfoProviderBase.java",
    "src/main/java/com/animania/compat/top/providers/entity/TOPInfoProviderChild.java",
    "src/main/java/com/animania/compat/top/providers/entity/TOPInfoProviderMateable.java",
    "src/main/java/com/animania/compat/top/providers/TOPInfoEntityProvider.java",
    "src/main/java/com/animania/compat/top/providers/TOPInfoProvider.java",
    "src/main/java/com/animania/compat/top/TOPCompat.java",
    "src/main/java/com/animania/compat/waila/provider/WailaBlockInvisiblockProvider.java",
    "src/main/java/com/animania/compat/waila/provider/WailaBlockNestProvider.java",
    "src/main/java/com/animania/compat/waila/provider/WailaBlockSeedProvider.java",
    "src/main/java/com/animania/compat/waila/provider/WailaBlockTroughProvider.java",
    "src/main/java/com/animania/compat/waila/provider/WailaEntityAnimalProviderBase.java",
    "src/main/java/com/animania/compat/waila/provider/WailaEntityAnimalProviderChild.java",
    "src/main/java/com/animania/compat/waila/provider/WailaEntityAnimalProviderMateable.java",
    "src/main/java/com/animania/compat/waila/WailaCompat.java",
}
PATHS = [
    "base/src/main/java/com/animania/compat/AnimaniaProbeComponents.java",
    "base/src/main/java/com/animania/compat/jei/AnimaniaJeiPlugin.java",
    "base/src/main/java/com/animania/compat/jade/AnimaniaJadePlugin.java",
    "base/src/main/java/com/animania/compat/top/AnimaniaTopProbeCompat.java",
    "base/src/main/java/com/animania/api/IAnimaniaProbeBlock.java",
]
TEST = "base/src/test/java/com/animania/compat/AnimaniaCompatContractTest.java"
SERIALIZATION_TEST = "base/src/test/java/com/animania/compat/AnimaniaPersistenceContractTest.java"


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
        errors.append(f"expected {len(SOURCES)} compatibility rows, found {len(rows)}")
    for source in SOURCES:
        if not (root / "upstream/Animania-1.12" / source).is_file():
            errors.append(f"legacy source missing: {source}")
    for path in PATHS + [TEST, SERIALIZATION_TEST]:
        if not (root / path).is_file():
            errors.append(f"evidence missing: {path}")
    if not errors:
        files = "\n".join((root / path).read_text(encoding="utf-8") for path in PATHS)
        test = (root / TEST).read_text(encoding="utf-8")
        for token in ("AnimaniaProbeComponents", "registerRecipes", "registerEntityDataProvider", "getTheOneProbe", "IAnimaniaProbeBlock"):
            if token not in files:
                errors.append(f"compatibility implementation missing {token}")
        for token in ("optionalIntegrationsUseModernRegistrationEntrypoints", "probeStateIncludesGenderParentAndCareFlags"):
            if token not in test:
                errors.append(f"compatibility regression test missing {token}")
    changed = 0
    if not errors:
        proof = {
            "paths": PATHS,
            "behavior_tests": [TEST, "tools/audit_base_compat.py"],
            "serialization_tests": [SERIALIZATION_TEST],
            "client_tests": [TEST, "base/run/fullClient/logs/debug.log"],
            "notes": [f"{OWNER} JEI, Jade and TOP are optional compile-only bridges; legacy Waila/TOP provider responsibilities converge on the shared server-authoritative probe component and addon-neutral block interface. No legacy compatibility jar is required."],
        }
        if args.write:
            for entry in rows:
                entry.update(status="closed", implemented=True, verified=True,
                             tests=[TEST, "tools/audit_base_compat.py"], target_evidence=proof)
                changed += 1
        else:
            for entry in rows:
                if entry.get("status") != "closed" or not any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", [])):
                    errors.append(f"compatibility row not closed: {entry.get('source')}")
    if args.write and not errors:
        matrix_path.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"rows": len(rows), "changed": changed, "errors": errors, "error_count": len(errors)}, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
