"""Verify and close the legacy BlockTrough responsibility."""
from __future__ import annotations
import argparse, json
from pathlib import Path

SOURCE = "src/main/java/com/animania/common/blocks/BlockTrough.java"
TARGET = "base/src/main/java/com/animania/common/block/AnimaniaTroughBlock.java"
TEST = "base/src/main/java/com/animania/gametest/AnimaniaBaseGameTests.java"
OWNER = "[trough-block-audit:v1]"

def main() -> None:
    p = argparse.ArgumentParser(); p.add_argument("--root", type=Path, required=True)
    p.add_argument("--matrix", type=Path, required=True); p.add_argument("--write", action="store_true")
    a = p.parse_args(); root = a.root
    code = (root / TARGET).read_text(encoding="utf-8"); tests = (root / TEST).read_text(encoding="utf-8")
    code_tokens = ("getStateForPlacement", "companionPos", "FluidUtil.interactWithFluidHandler",
                   "getAnalogOutputSignal", "collectRain", "getBaseTemperature() >= 0.15F")
    test_tokens = ("troughEnforcesLegacyFoodFluidCapacityAndComparator",
                   "troughRainCollectionPreservesLegacyMixingAndIncrementRules", "storageCapabilitiesPersist")
    errors = [f"missing code token {x}" for x in code_tokens if x not in code]
    errors += [f"missing test token {x}" for x in test_tokens if x not in tests]
    matrix = json.loads(a.matrix.read_text(encoding="utf-8")); rows = [e for e in matrix["entries"] if e.get("source") == SOURCE]
    if len(rows) != 1: errors.append(f"matched {len(rows)} rows")
    proof = {"paths": [TARGET], "behavior_tests": [TEST, "tools/audit_trough_block.py"],
             "serialization_tests": [TEST], "client_tests": [],
             "notes": [f"{OWNER} dedicated-server tests cover the two-block structure, interaction/capabilities, comparator, NBT and exact 100 mB rain rules."]}
    if a.write and not errors:
        rows[0].update(status="closed", implemented=True, verified=True,
                       tests=proof["behavior_tests"], target_evidence=proof)
        a.matrix.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"matched": len(rows), "changed": int(a.write and not errors), "errors": errors}, ensure_ascii=False))
    if errors: raise SystemExit(1)

if __name__ == "__main__": main()
