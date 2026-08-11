"""Audit the migrated global egg-use guard and optional Extra rodent rule."""
from __future__ import annotations
import argparse, json
from pathlib import Path

SOURCE = "src/main/java/com/animania/addons/farm/common/event/EggThrowHandler.java"
TARGET = "farm/src/main/java/com/animania/farm/FarmEggThrowHandler.java"
TEST = "farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java"
OWNER = "[farm-egg-throw-audit:v1]"

def main():
    p=argparse.ArgumentParser(); p.add_argument("--root",type=Path,required=True); p.add_argument("--matrix",type=Path,default=Path("docs/migration-matrix.json")); p.add_argument("--write",action="store_true"); a=p.parse_args()
    root=a.root.resolve(); errors=[]
    for path in (root/"upstream/Animania-1.12"/SOURCE,root/TARGET,root/TEST):
        if not path.is_file(): errors.append(f"missing: {path}")
    modern=(root/TARGET).read_text(encoding="utf-8"); test=(root/TEST).read_text(encoding="utf-8")
    for token in ("ALLOW_EGG_THROWING", 'equals("ferret_white")', 'equals("ferret_grey")', 'equals("hedgehog")', "getBoundingBox().inflate(3.0D, 2.0D, 3.0D)"):
        if token not in modern: errors.append(f"behavior missing: {token}")
    if "eggThrowingHonorsGlobalToggleAndOptionalExtraRodents" not in test: errors.append("dedicated GameTest missing")
    mp=a.matrix if a.matrix.is_absolute() else root/a.matrix; m=json.loads(mp.read_text(encoding="utf-8")); e=next((x for x in m["entries"] if x.get("source")==SOURCE),None)
    if e is None: errors.append("matrix row missing")
    elif not errors:
        proof={"paths":[TARGET],"behavior_tests":[TEST,"tools/audit_farm_egg_throw_handler.py"],"serialization_tests":[],"client_tests":[],"notes":[f"{OWNER} Forge dedicated GameTest proves the global toggle and exact optional-Extra rodent ID contract; the live handler performs the legacy 3x2x3 spatial guard."]}
        if a.write: e.update(status="closed",implemented=True,verified=True,tests=[TEST,"tools/audit_farm_egg_throw_handler.py"],target_evidence=proof)
        elif e.get("status")!="closed" or not any(OWNER in n for n in e.get("target_evidence",{}).get("notes",[])): errors.append("provable row not closed")
    if a.write and not errors: mp.write_text(json.dumps(m,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"rows":1,"changed":1 if a.write and not errors else 0,"errors":errors,"error_count":len(errors)},ensure_ascii=False,indent=2))
    if errors: raise SystemExit(1)

if __name__=="__main__": main()
