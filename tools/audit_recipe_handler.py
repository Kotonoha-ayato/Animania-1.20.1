"""Audit replacement of the 1.12 development-time recipe writer with datapack recipes."""
from __future__ import annotations
import argparse,json
from pathlib import Path

SOURCE="src/main/java/com/animania/common/handler/RecipeHandler.java"
PATHS=("base/src/main/java/com/animania/common/recipe/AnimaniaRecipes.java","base/src/main/java/com/animania/common/recipe/SlopRecipe.java","farm/src/main/java/com/animania/farm/FarmRecipes.java","farm/src/main/java/com/animania/farm/FarmMilkConversionRecipe.java")
TESTS=("base/src/main/java/com/animania/gametest/AnimaniaBaseGameTests.java","farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java","extra/src/main/java/com/animania/extra/gametest/AnimaniaExtraGameTests.java","tools/audit_resources.py","tools/audit_recipe_handler.py")
OWNER="[recipe-handler-audit:v1]"

def main():
 p=argparse.ArgumentParser();p.add_argument("--root",type=Path,required=True);p.add_argument("--matrix",type=Path,required=True);p.add_argument("--write",action="store_true");a=p.parse_args();errors=[]
 old=(a.root/"upstream/Animania-1.12"/SOURCE).read_text(encoding="utf-8")
 if "FileWriter" not in old or "addShapedRecipe" not in old or "addShapelessRecipe" not in old:errors.append("legacy generator responsibilities changed")
 texts="\n".join((a.root/path).read_text(encoding="utf-8") for path in PATHS)
 for token in ("DeferredRegister<RecipeSerializer", "SimpleCraftingRecipeSerializer", "SlopRecipe", "FarmMilkConversionRecipe"):
  if token not in texts:errors.append(f"modern recipe implementation missing {token}")
 required={TESTS[0]:"slopRecipePreservesConfigAndBucketSemantics",TESTS[1]:"everyProgrammaticLegacySmeltingRecipeLoadsWithExactValues",TESTS[2]:"everyNativeLegacySmeltingRecipeLoadsWithExactValues"}
 for path,token in required.items():
  if token not in (a.root/path).read_text(encoding="utf-8"):errors.append(f"missing recipe GameTest {token}")
 recipe_files=[]
 for module in ("base","farm","extra","catsdogs"):
  recipe_files.extend((a.root/module/"src/main/resources/data").rglob("recipes/*.json"))
 for path in recipe_files:
  try:json.loads(path.read_text(encoding="utf-8"))
  except Exception as exc:errors.append(f"invalid recipe {path}: {exc}")
 if len(recipe_files)!=102:errors.append(f"modern recipe count {len(recipe_files)} != audited 102")
 matrix=json.loads(a.matrix.read_text(encoding="utf-8"));rows=[e for e in matrix["entries"] if e.get("source")==SOURCE]
 if len(rows)!=1:errors.append(f"matched {len(rows)} rows")
 if a.write and not errors:
  proof={"paths":list(PATHS),"behavior_tests":list(TESTS),"serialization_tests":["tools/audit_resources.py","tools/audit_recipe_handler.py"],"client_tests":[],"notes":[f"{OWNER} the old disk-writing helper is replaced by 102 valid datapack recipes and native serializers; Base/Farm/Extra dedicated-server tests exercise dynamic, smelting and optional-addon recipes."]}
  rows[0].update(status="closed",implemented=True,verified=True,tests=list(TESTS),target_evidence=proof)
  a.matrix.write_text(json.dumps(matrix,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
 print(json.dumps({"recipes":len(recipe_files),"matched":len(rows),"changed":int(a.write and not errors),"errors":errors},ensure_ascii=False))
 if errors:raise SystemExit(1)
if __name__=="__main__":main()
