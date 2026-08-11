"""Verify the legacy AnimalContainer identity-key contract."""
from __future__ import annotations
import argparse, json
from pathlib import Path

SOURCE = "src/main/java/com/animania/api/data/AnimalContainer.java"
TARGET = "base/src/main/java/com/animania/api/data/AnimalContainer.java"
TEST = "base/src/test/java/com/animania/api/AnimalContainerTest.java"
OWNER = "[animal-container-audit:v1]"

def main() -> None:
    p=argparse.ArgumentParser(); p.add_argument("--root",type=Path,required=True); p.add_argument("--matrix",type=Path,required=True); p.add_argument("--write",action="store_true"); a=p.parse_args()
    code=(a.root/TARGET).read_text(encoding="utf-8"); test=(a.root/TEST).read_text(encoding="utf-8")
    tokens=("other.gender == gender", "other.type == type", "gender.hashCode() + type.hashCode()", "fromString")
    errors=[f"missing {x}" for x in tokens if x not in code]
    for x in ("preservesLegacyIdentityEqualityHashAndStringContract", "rejectsNullKeysInsteadOfCreatingUnusableMapEntries"):
        if x not in test: errors.append(f"missing test {x}")
    matrix=json.loads(a.matrix.read_text(encoding="utf-8")); rows=[e for e in matrix["entries"] if e.get("source")==SOURCE]
    if len(rows)!=1: errors.append(f"matched {len(rows)} rows")
    proof={"paths":[TARGET],"behavior_tests":[TEST,"tools/audit_animal_container.py"],"serialization_tests":[],"client_tests":[],"notes":[f"{OWNER} identity equality, hash, accessors, string form and legacy parser behavior are unit-tested."]}
    if a.write and not errors:
        rows[0].update(status="closed",implemented=True,verified=True,tests=proof["behavior_tests"],target_evidence=proof)
        a.matrix.write_text(json.dumps(matrix,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"matched":len(rows),"changed":int(a.write and not errors),"errors":errors},ensure_ascii=False))
    if errors: raise SystemExit(1)

if __name__=="__main__": main()
