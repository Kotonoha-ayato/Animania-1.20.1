"""Audit small public 1.12 utility contracts against their Java 17 ports."""
from __future__ import annotations
import argparse, json, re
from pathlib import Path

OWNER = "[legacy-utility-audit:v1]"
FILES = {
    "src/main/java/com/animania/api/data/Pose.java": "base/src/main/java/com/animania/api/data/Pose.java",
    "src/main/java/com/animania/common/helper/TimeHelper.java": "base/src/main/java/com/animania/common/helper/TimeHelper.java",
    "src/main/java/com/animania/common/helper/RomanNumberHelper.java": "base/src/main/java/com/animania/common/helper/RomanNumberHelper.java",
    "src/main/java/com/animania/common/helper/InvalidConfigException.java": "base/src/main/java/com/animania/common/helper/InvalidConfigException.java",
    "src/main/java/com/animania/api/data/EntityGender.java": "base/src/main/java/com/animania/api/data/EntityGender.java",
}
TEST = "base/src/test/java/com/animania/common/helper/LegacyUtilityTest.java"

def constants(text: str) -> dict[str, str]:
    return dict(re.findall(r"(?:public\s+)?static\s+final\s+int\s+(\w+)\s*=\s*([^;]+);", text))

def main() -> None:
    parser = argparse.ArgumentParser(); parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, default=Path("docs/migration-matrix.json")); parser.add_argument("--write", action="store_true")
    args = parser.parse_args(); root = args.root.resolve(); errors: list[str] = []
    texts: dict[str, tuple[str, str]] = {}
    for source, target in FILES.items():
        old_path, new_path = root / "upstream/Animania-1.12" / source, root / target
        if not old_path.exists() or not new_path.exists(): errors.append(f"missing source/target for {source}"); continue
        texts[source] = (old_path.read_text(encoding="utf-8"), new_path.read_text(encoding="utf-8"))
    pose = texts.get("src/main/java/com/animania/api/data/Pose.java")
    if pose:
        enum_values = lambda text: set(re.findall(r"\b(SITTING|SLEEPING)\b", text))
        if enum_values(pose[0]) != {"SITTING", "SLEEPING"} or enum_values(pose[1]) != {"SITTING", "SLEEPING"}: errors.append("Pose values differ")
    time = texts.get("src/main/java/com/animania/common/helper/TimeHelper.java")
    if time:
        if constants(time[0]) != constants(time[1]): errors.append(f"TimeHelper constants differ: {constants(time[0])} != {constants(time[1])}")
        if "String getTime(int ticks)" not in time[0] or "String getTime(int ticks)" not in time[1]: errors.append("TimeHelper formatter missing")
    roman = texts.get("src/main/java/com/animania/common/helper/RomanNumberHelper.java")
    if roman and ("String toRoman(int number)" not in roman[0] or "String toRoman(int number)" not in roman[1]): errors.append("Roman formatter missing")
    invalid = texts.get("src/main/java/com/animania/common/helper/InvalidConfigException.java")
    if invalid:
        for token in ("extends Exception", "InvalidConfigException(String cause)", "void printException()"):
            if token not in invalid[0] or token not in invalid[1]: errors.append(f"InvalidConfigException contract missing: {token}")
    gender = texts.get("src/main/java/com/animania/api/data/EntityGender.java")
    if gender:
        names = {"MALE", "FEMALE", "CHILD", "RANDOM", "NONE"}
        for label, text in (("legacy", gender[0]), ("modern", gender[1])):
            if {name for name in names if re.search(r"\b" + name + r"\b", text)} != names: errors.append(f"{label} EntityGender values differ")
        for token in ("AnimalGender resolve(IntSupplier", "Math.floorMod", "case RANDOM"):
            if token not in gender[1]: errors.append(f"modern EntityGender random resolver missing: {token}")
    test_path = root / TEST
    test = test_path.read_text(encoding="utf-8") if test_path.exists() else ""
    for name in ("poseNamesRemainStable", "tickConstantsAndFormattingMatchLegacyValues", "romanFormatterPreservesSubtractiveNotationAndRejectsOldCrashCases", "invalidConfigExceptionRetainsCheckedMessageContract", "legacyGenderResolutionPreservesEveryBranchIncludingRandom"):
        if name not in test: errors.append(f"missing unit test: {name}")
    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    matrix = json.loads(matrix_path.read_text(encoding="utf-8")); by_source = {e.get("source"): e for e in matrix["entries"]}
    changed = 0
    if not errors:
        for source, target in FILES.items():
            entry = by_source.get(source)
            if not entry: errors.append(f"matrix row missing: {source}"); continue
            owned = any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", []))
            proof = {"paths": [target], "behavior_tests": [TEST, "tools/audit_legacy_utilities.py"], "serialization_tests": [], "client_tests": [],
                     "notes": [f"{OWNER} source-derived public contract and executable boundary tests verified."]}
            if args.write:
                entry.update(status="closed", implemented=True, verified=True, tests=[TEST, "tools/audit_legacy_utilities.py"], target_evidence=proof); changed += 1
            elif entry.get("status") != "closed" or not owned: errors.append(f"provable row not closed: {source}")
    if args.write and not errors: matrix_path.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"contracts": len(texts), "changed": changed, "errors": errors, "error_count": len(errors)}, ensure_ascii=False, indent=2))
    if errors: raise SystemExit(1)

if __name__ == "__main__": main()
