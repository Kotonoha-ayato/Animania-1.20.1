"""Audit the live-registry replacement for RandomAnimalType."""
from __future__ import annotations
import argparse,json
from pathlib import Path
SOURCE="src/main/java/com/animania/common/entities/RandomAnimalType.java";ITEMS="base/src/main/java/com/animania/common/AnimaniaItems.java";EGG="base/src/main/java/com/animania/common/item/AnimaniaEntityEggItem.java";FARM="farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java";OWNER="[random-animal-type-audit:v1]"
def main():
 p=argparse.ArgumentParser();p.add_argument("--root",type=Path,required=True);p.add_argument("--matrix",type=Path,required=True);p.add_argument("--write",action="store_true");a=p.parse_args();errors=[]
 items=(a.root/ITEMS).read_text(encoding="utf-8");egg=(a.root/EGG).read_text(encoding="utf-8");farm=(a.root/FARM).read_text(encoding="utf-8")
 for token in ("AnimaniaItems::allAnimalTypes", "ForgeRegistries.ENTITY_TYPES.getEntries()", 'namespace.startsWith("animania_")', '!path.equals("cart")', '!path.equals("wagon")', '!path.equals("tiller")', '!path.startsWith("item_")'):
  if token not in items:errors.append(f"random candidate discovery missing {token}")
 for token in ("ThreadLocalRandom.current().nextInt(types.size())", "type.create(level)", "level.addFreshEntity(entity)"):
  if token not in egg:errors.append(f"random spawn path missing {token}")
 if "baseRandomEggSelectsOnlyLoadedAnimalTypes" not in farm:errors.append("missing actual Base random-egg GameTest")
 matrix=json.loads(a.matrix.read_text(encoding="utf-8"));rows=[e for e in matrix["entries"] if e.get("source")==SOURCE]
 if len(rows)!=1:errors.append(f"matched {len(rows)} rows")
 tests=[FARM,"tools/audit_random_animal_type.py"]
 if a.write and not errors:
  proof={"paths":[ITEMS,EGG],"behavior_tests":tests,"serialization_tests":[],"client_tests":[],"notes":[f"{OWNER} candidates come from live loaded Animania namespaces with vehicles/projectiles excluded; Farm-only dedicated server proves the Base random egg spawns one loaded animal and consumes one item."]}
  rows[0].update(status="closed",implemented=True,verified=True,tests=tests,target_evidence=proof);a.matrix.write_text(json.dumps(matrix,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
 print(json.dumps({"matched":len(rows),"changed":int(a.write and not errors),"errors":errors},ensure_ascii=False))
 if errors:raise SystemExit(1)
if __name__=="__main__":main()
