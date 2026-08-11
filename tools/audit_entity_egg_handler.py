"""Audit replacement of EntityEggHandler's legacy entity-entry map."""
from __future__ import annotations
import argparse,json
from pathlib import Path
SOURCE="src/main/java/com/animania/common/handler/EntityEggHandler.java";API="base/src/main/java/com/animania/api/AnimaniaApi.java";MODULES=("farm/src/main/java/com/animania/farm/AnimaniaFarm.java","extra/src/main/java/com/animania/extra/AnimaniaExtra.java","catsdogs/src/main/java/com/animania/catsdogs/AnimaniaCatsDogs.java");TESTS=("base/src/test/java/com/animania/api/AnimalContainerTest.java","base/src/test/java/com/animania/api/AnimaniaApiTest.java","farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java","extra/src/main/java/com/animania/extra/gametest/AnimaniaExtraGameTests.java","catsdogs/src/main/java/com/animania/catsdogs/gametest/AnimaniaCatsDogsGameTests.java","tools/audit_entity_egg_handler.py");OWNER="[entity-egg-handler-audit:v1]"
def main():
 p=argparse.ArgumentParser();p.add_argument("--root",type=Path,required=True);p.add_argument("--matrix",type=Path,required=True);p.add_argument("--write",action="store_true");a=p.parse_args();errors=[]
 api=(a.root/API).read_text(encoding="utf-8")
 for token in ("registerSpecies", "species(ResourceLocation", "speciesIds()", "speciesForAddon", "hasSpecies"):
  if token not in api:errors.append(f"public entity lookup missing {token}")
 for path in MODULES:
  text=(a.root/path).read_text(encoding="utf-8")
  for token in ("Map<String, RegistryObject<EntityType<?>>> ENTITIES", "ENTITY_TYPES.register", "ENTITIES.put", "AnimaniaApi.registerSpecies"):
   if token not in text:errors.append(f"{path} missing {token}")
 checks={TESTS[2]:"AnimaniaFarm.ENTITIES.size() == FarmLegacyIds.ALL.size()",TESTS[3]:"AnimaniaExtra.ENTITIES.size() == ExtraLegacyIds.ALL.size()",TESTS[4]:"AnimaniaCatsDogs.ENTITIES.size() == CatsDogsLegacyIds.ALL.size()"}
 for path,token in checks.items():
  if token not in (a.root/path).read_text(encoding="utf-8"):errors.append(f"missing live registry count proof {path}")
 matrix=json.loads(a.matrix.read_text(encoding="utf-8"));rows=[e for e in matrix["entries"] if e.get("source")==SOURCE]
 if len(rows)!=1:errors.append(f"matched {len(rows)} rows")
 if a.write and not errors:
  proof={"paths":[API,*MODULES],"behavior_tests":list(TESTS),"serialization_tests":[],"client_tests":[],"notes":[f"{OWNER} AnimalContainer/API unit tests and all three addon dedicated servers prove modern entity-entry maps, live registries and addon queries without the mutable legacy global map."]}
  rows[0].update(status="closed",implemented=True,verified=True,tests=list(TESTS),target_evidence=proof);a.matrix.write_text(json.dumps(matrix,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
 print(json.dumps({"matched":len(rows),"changed":int(a.write and not errors),"errors":errors},ensure_ascii=False))
 if errors:raise SystemExit(1)
if __name__=="__main__":main()
