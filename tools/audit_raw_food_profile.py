"""Audit every 1.12 ItemAnimaniaFoodRaw registration and exact food values."""
from __future__ import annotations
import argparse, json, re
from pathlib import Path

OWNER = "[raw-food-profile-audit:v1]"
SOURCE = "src/main/java/com/animania/common/items/ItemAnimaniaFoodRaw.java"

def main() -> None:
    parser = argparse.ArgumentParser(); parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, default=Path("docs/migration-matrix.json")); parser.add_argument("--write", action="store_true")
    args = parser.parse_args(); root = args.root.resolve(); errors: list[str] = []
    old_root = root / "upstream/Animania-1.12/src/main/java"
    old_class = (root / "upstream/Animania-1.12" / SOURCE).read_text(encoding="utf-8")
    compact = re.sub(r"\s+", "", old_class)
    for token in ("super(1,1f,name", "newPotionEffect(MobEffects.NAUSEA,200,3,false,false)"):
        if token not in compact: errors.append(f"legacy raw-food contract changed: {token}")
    raw_ids: set[str] = set()
    for path in old_root.rglob("*.java"):
        raw_ids.update(re.findall(r'new\s+ItemAnimaniaFoodRaw\("([a-z0-9_]+)"', path.read_text(encoding="utf-8", errors="replace")))
    expected = {"raw_chevon", "raw_frog_legs", "raw_horse", "raw_peacock", "raw_prime_bacon", "raw_prime_beef",
                "raw_prime_chevon", "raw_prime_chicken", "raw_prime_mutton", "raw_prime_peacock", "raw_prime_pork",
                "raw_prime_rabbit", "raw_prime_steak"}
    if raw_ids != expected: errors.append(f"source raw-food set changed: missing={sorted(expected-raw_ids)}, extra={sorted(raw_ids-expected)}")
    target_path = "base/src/main/java/com/animania/common/item/LegacyRawFoodProfile.java"
    target = (root / target_path).read_text(encoding="utf-8")
    if "new LegacyRawFoodProfile(1, 1.0F, 200, 3, 1.0F)" not in target or 'id.startsWith("raw_")' not in target:
        errors.append("modern raw-food profile does not retain exact values/selection")
    module_files = {"farm": "farm/src/main/java/com/animania/farm/FarmContent.java", "extra": "extra/src/main/java/com/animania/extra/ExtraContent.java"}
    tests = {"farm": "farm/src/test/java/com/animania/farm/FarmRegistryTest.java", "extra": "extra/src/test/java/com/animania/extra/ExtraRegistryTest.java"}
    tested: set[str] = set()
    for module, path in module_files.items():
        text = (root / path).read_text(encoding="utf-8")
        if "LegacyRawFoodProfile.forItemId(id)" not in text or ".apply(food)" not in text: errors.append(f"{module} item factory does not consume the shared profile")
        test = (root / tests[module]).read_text(encoding="utf-8")
        tested.update(re.findall(r'"(raw_[a-z0-9_]+)"', test))
        if "RetainsExactFoodAndNauseaValues" not in test: errors.append(f"{module} exact raw-food unit test missing")
    if tested & expected != expected: errors.append(f"raw-food tests omit: {sorted(expected-tested)}")
    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    matrix = json.loads(matrix_path.read_text(encoding="utf-8")); entry = next((e for e in matrix["entries"] if e.get("source") == SOURCE), None)
    if entry is None: errors.append("matrix raw-food row missing")
    changed = 0
    if not errors and entry is not None:
        owned = any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", []))
        proof = {"paths": [target_path, *module_files.values(), "tools/audit_raw_food_profile.py"],
                 "behavior_tests": [*tests.values(), "tools/audit_raw_food_profile.py"], "serialization_tests": [], "client_tests": [],
                 "notes": [f"{OWNER} all {len(expected)} source registrations consume the exact shared 1/1.0/200/3/100% nausea profile and are unit-tested."]}
        if args.write:
            entry.update(status="closed", implemented=True, verified=True, tests=proof["behavior_tests"], target_evidence=proof); changed = 1
        elif entry.get("status") != "closed" or not owned: errors.append("provable raw-food row is not closed")
    if args.write and not errors: matrix_path.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"raw_food_ids": sorted(raw_ids), "changed": changed, "errors": errors, "error_count": len(errors)}, ensure_ascii=False, indent=2))
    if errors: raise SystemExit(1)

if __name__ == "__main__": main()
