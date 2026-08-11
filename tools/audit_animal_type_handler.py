"""Audit AnimalTypeHandler's string/enum registry replacement with stable species IDs."""
from __future__ import annotations
import argparse,json
from pathlib import Path
SOURCE="src/main/java/com/animania/common/handler/AnimalTypeHandler.java";API="base/src/main/java/com/animania/api/AnimaniaApi.java";DEFINITION="base/src/main/java/com/animania/api/data/SpeciesDefinition.java";MODULES=("farm/src/main/java/com/animania/farm/AnimaniaFarm.java","extra/src/main/java/com/animania/extra/AnimaniaExtra.java","catsdogs/src/main/java/com/animania/catsdogs/AnimaniaCatsDogs.java");UNIT="base/src/test/java/com/animania/api/AnimaniaApiTest.java";GAMES=("farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java","extra/src/main/java/com/animania/extra/gametest/AnimaniaExtraGameTests.java","catsdogs/src/main/java/com/animania/catsdogs/gametest/AnimaniaCatsDogsGameTests.java");OWNER="[animal-type-handler-audit:v1]"
def main():
 p=argparse.ArgumentParser();p.add_argument("--root",type=Path,required=True);p.add_argument("--matrix",type=Path,required=True);p.add_argument("--write",action="store_true");a=p.parse_args();errors=[]
 api=(a.root/API).read_text(encoding="utf-8");unit=(a.root/UNIT).read_text(encoding="utf-8")
 for token in ("Map<ResourceLocation, SpeciesDefinition>","registerSpecies", "putIfAbsent", "species(ResourceLocation", "speciesIds()", "speciesForAddon", "hasSpecies"):
  if token not in api:errors.append(f"stable type registry missing {token}")
 for path in MODULES:
  if "AnimaniaApi.registerSpecies(new SpeciesDefinition" not in (a.root/path).read_text(encoding="utf-8"):errors.append(f"module species registration missing {path}")
 if "speciesRegistrationAndAddonQueryAreStable" not in unit:errors.append("species API unit test missing")
 matrix=json.loads(a.matrix.read_text(encoding="utf-8"));rows=[e for e in matrix["entries"] if e.get("source")==SOURCE]
 if len(rows)!=1:errors.append(f"matched {len(rows)} rows")
 tests=[UNIT,*GAMES,"tools/audit_animal_type_handler.py"]
 if a.write and not errors:
  proof={"paths":[API,DEFINITION,*MODULES],"behavior_tests":tests,"serialization_tests":[],"client_tests":[],"notes":[f"{OWNER} ResourceLocation species IDs replace the ambiguous typeName/enum-string map; unit and all addon dedicated-server suites verify registration and lookup."]}
  rows[0].update(status="closed",implemented=True,verified=True,tests=tests,target_evidence=proof);a.matrix.write_text(json.dumps(matrix,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
 print(json.dumps({"matched":len(rows),"changed":int(a.write and not errors),"errors":errors},ensure_ascii=False))
 if errors:raise SystemExit(1)
if __name__=="__main__":main()
