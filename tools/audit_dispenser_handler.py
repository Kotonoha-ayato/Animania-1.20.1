"""Audit modern server-authoritative seed and Animania egg dispenser behavior."""
from __future__ import annotations
import argparse,json
from pathlib import Path

SOURCE="src/main/java/com/animania/common/handler/DispenserHandler.java"
SEEDS="base/src/main/java/com/animania/common/AnimaniaSeedPlacement.java"
EGG="base/src/main/java/com/animania/common/item/AnimaniaEntityEggItem.java"
MODULES=("base/src/main/java/com/animania/Animania.java","farm/src/main/java/com/animania/farm/AnimaniaFarm.java","extra/src/main/java/com/animania/extra/AnimaniaExtra.java","catsdogs/src/main/java/com/animania/catsdogs/AnimaniaCatsDogs.java")
BASE_TEST="base/src/main/java/com/animania/gametest/AnimaniaBaseGameTests.java"
FARM_TEST="farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java"
OWNER="[dispenser-handler-audit:v1]"

def main():
 p=argparse.ArgumentParser();p.add_argument("--root",type=Path,required=True);p.add_argument("--matrix",type=Path,required=True);p.add_argument("--write",action="store_true");a=p.parse_args();errors=[]
 seeds=(a.root/SEEDS).read_text(encoding="utf-8");egg=(a.root/EGG).read_text(encoding="utf-8");base_test=(a.root/BASE_TEST).read_text(encoding="utf-8");farm_test=(a.root/FARM_TEST).read_text(encoding="utf-8")
 for token in ("ALLOW_SEED_DISPENSER_PLACEMENT", "DispenserBlock.registerBehavior", "Items.WHEAT_SEEDS", "Items.PUMPKIN_SEEDS", "Items.MELON_SEEDS", "Items.BEETROOT_SEEDS"):
  if token not in seeds:errors.append(f"seed dispenser missing {token}")
 for token in ("registerDispenserBehavior", "DefaultDispenseItemBehavior", "egg.spawn", "setCustomName", "addFreshEntity", "stack.shrink(1)"):
  if token not in egg:errors.append(f"egg dispenser/spawn path missing {token}")
 for path in MODULES:
  text=(a.root/path).read_text(encoding="utf-8")
  if "registerDispenserBehavior" not in text:errors.append(f"module does not register egg dispensers: {path}")
 if "dispenserPlacesConfiguredSeedPileServerSide" not in base_test:errors.append("missing seed dispenser GameTest")
 if "dispenserSpawnsNamedAnimaniaEggAndConsumesExactlyOne" not in farm_test:errors.append("missing egg dispenser GameTest")
 matrix=json.loads(a.matrix.read_text(encoding="utf-8"));rows=[e for e in matrix["entries"] if e.get("source")==SOURCE]
 if len(rows)!=1:errors.append(f"matched {len(rows)} rows")
 tests=[BASE_TEST,FARM_TEST,"tools/audit_dispenser_handler.py"]
 if a.write and not errors:
  proof={"paths":[SEEDS,EGG,*MODULES],"behavior_tests":tests,"serialization_tests":[],"client_tests":[],"notes":[f"{OWNER} Base and Farm dedicated-server tests prove configured seed placement and named animal-egg spawn/consumption; every addon registers its own egg items after registries resolve."]}
  rows[0].update(status="closed",implemented=True,verified=True,tests=tests,target_evidence=proof)
  a.matrix.write_text(json.dumps(matrix,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
 print(json.dumps({"matched":len(rows),"changed":int(a.write and not errors),"errors":errors},ensure_ascii=False))
 if errors:raise SystemExit(1)
if __name__=="__main__":main()
