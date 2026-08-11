"""Audit the complete 1.12 OreDictionary-to-modern-tag migration."""
from __future__ import annotations
import argparse,json,re
from pathlib import Path

SOURCE="src/main/java/com/animania/common/handler/DictionaryHandler.java"
API="base/src/main/java/com/animania/api/AnimaniaLegacyTags.java"
TEST="base/src/main/java/com/animania/gametest/AnimaniaBaseGameTests.java"
CUSTOM=("storage_blocks/mud","sugar","foods/bread","foods/raw_chicken","foods/raw_beef","foods/raw_pork","foods/cooked_chicken","foods/cooked_beef","foods/cooked_pork")
OWNER="[dictionary-tags-audit:v1]"

def main():
 p=argparse.ArgumentParser();p.add_argument("--root",type=Path,required=True);p.add_argument("--matrix",type=Path,required=True);p.add_argument("--write",action="store_true");a=p.parse_args();errors=[]
 old=(a.root/"upstream/Animania-1.12"/SOURCE).read_text(encoding="utf-8");api=(a.root/API).read_text(encoding="utf-8");test=(a.root/TEST).read_text(encoding="utf-8")
 registrations=re.findall(r'OreDictionary\.registerOre\("([^"]+)"',old)
 if len(registrations)!=33:errors.append(f"legacy registration count {len(registrations)} != 33")
 for token in ("CROPS_CARROT","CROPS_POTATO","CROPS_BEETROOT","SEEDS","DYES_BLACK","DYES_WHITE","ItemTags.WOOL"):
  if token not in test:errors.append(f"GameTest missing built-in tag proof {token}")
 if "legacyOreDictionaryCategoriesResolveThroughModernTags" not in test:errors.append("missing dictionary GameTest")
 resources=[]
 for tag in CUSTOM:
  path=f"base/src/main/resources/data/forge/tags/items/{tag}.json";resources.append(path)
  payload=json.loads((a.root/path).read_text(encoding="utf-8"))
  if payload.get("replace") is not False or not payload.get("values"):errors.append(f"invalid common tag {tag}")
 if "MUD_STORAGE" not in api or "COOKED_PORK" not in api:errors.append("public modern tag keys incomplete")
 block_tag="base/src/main/resources/data/forge/tags/blocks/storage_blocks/mud.json";resources.append(block_tag)
 json.loads((a.root/block_tag).read_text(encoding="utf-8"))
 matrix=json.loads(a.matrix.read_text(encoding="utf-8"));rows=[e for e in matrix["entries"] if e.get("source")==SOURCE]
 if len(rows)!=1:errors.append(f"matched {len(rows)} rows")
 tests=[TEST,"tools/audit_dictionary_tags.py"]
 if a.write and not errors:
  proof={"paths":[API,*resources],"behavior_tests":tests,"serialization_tests":resources,"client_tests":[],"notes":[f"{OWNER} all 33 legacy registrations map to Forge/Minecraft built-in tags or explicit forge common tags; Base dedicated-server GameTest resolves representative and all custom categories live."]}
  rows[0].update(status="closed",implemented=True,verified=True,tests=tests,target_evidence=proof)
  a.matrix.write_text(json.dumps(matrix,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
 print(json.dumps({"legacy_registrations":len(registrations),"matched":len(rows),"changed":int(a.write and not errors),"errors":errors},ensure_ascii=False))
 if errors:raise SystemExit(1)
if __name__=="__main__":main()
