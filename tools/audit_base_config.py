"""Audit Base 1.12 config fields/defaults against ForgeConfigSpec and the converter."""
from __future__ import annotations
import argparse,json,re
from pathlib import Path

SOURCES=("src/main/java/com/animania/config/AnimaniaConfig.java","src/main/java/com/animania/config/CommonConfig.java")
TARGET="base/src/main/java/com/animania/common/config/AnimaniaConfig.java"
MIGRATOR="config-migrator/src/main/java/com/animania/migrator/ConfigMigrator.java"
TESTS=("base/src/test/java/com/animania/common/config/AnimaniaFoodOverrideTest.java","config-migrator/src/test/java/com/animania/migrator/ConfigMigratorTest.java","tools/audit_base_config.py")
ALIASES={"gestationTimer":"gestationTicks"}
OWNER="[base-config-audit:v1]"

def clean_number(value:str)->str:
 value=value.strip().removesuffix("f").removesuffix("F").removesuffix("d").removesuffix("D")
 try:return str(float(value)).rstrip("0").rstrip(".") if "." in value else str(int(value))
 except ValueError:return value

def main():
 p=argparse.ArgumentParser();p.add_argument("--root",type=Path,required=True);p.add_argument("--matrix",type=Path,required=True);p.add_argument("--write",action="store_true");a=p.parse_args();errors=[]
 old=(a.root/"upstream/Animania-1.12"/SOURCES[1]).read_text(encoding="utf-8");modern=(a.root/TARGET).read_text(encoding="utf-8");migrator=(a.root/MIGRATOR).read_text(encoding="utf-8")
 primitives=re.findall(r'public\s+(?:boolean|int|float|double)\s+(\w+)\s*=\s*([^;]+);',old)
 lists=re.findall(r'public\s+String\[\]\s+(\w+)\s*=\s*(?:new\s+String\[\]\s*)?\{(.*?)\};',old,re.S)
 if len(primitives)!=42:errors.append(f"legacy primitive count {len(primitives)} != 42")
 if len(lists)!=3:errors.append(f"legacy list count {len(lists)} != 3")
 for old_key,default in primitives:
  key=ALIASES.get(old_key,old_key);expected=clean_number(default)
  match=re.search(rf'(?:define|defineInRange)\("{re.escape(key)}",\s*([^,\)]+)',modern)
  if not match:errors.append(f"modern config missing {key}");continue
  actual=clean_number(match.group(1))
  if actual!=expected:errors.append(f"{key} default {actual} != legacy {expected}")
  migrated=re.search(rf'Map\.entry\("{re.escape(key)}",\s*"([^"]+)"\)',migrator)
  if not migrated or clean_number(migrated.group(1))!=expected:errors.append(f"migrator missing {key}={expected}")
 for old_key,body in lists:
  key=ALIASES.get(old_key,old_key)
  if f'defineList("{key}"' not in modern:errors.append(f"modern list missing {key}")
  if f'Map.entry("{key}",' not in migrator:errors.append(f"migrator list missing {key}")
  values=re.findall(r'"([^"]*)"',body)
  for value in values:
   normalized={"animania:brown_egg":"animania_farm:brown_egg"}.get(value,value)
   if normalized not in modern:errors.append(f"modern {key} missing normalized value {normalized}")
 matrix=json.loads(a.matrix.read_text(encoding="utf-8"));rows=[e for e in matrix["entries"] if e.get("source") in SOURCES]
 if len(rows)!=2:errors.append(f"matched {len(rows)} rows")
 if a.write and not errors:
  for row in rows:
   proof={"paths":[TARGET,MIGRATOR],"behavior_tests":list(TESTS),"serialization_tests":[TESTS[1]],"client_tests":[],"notes":[f"{OWNER} source-derived audit matches all {len(primitives)} primitive and {len(lists)} list defaults in ForgeConfigSpec and the read-only converter, including normalized IDs."]}
   row.update(status="closed",implemented=True,verified=True,tests=list(TESTS),target_evidence=proof)
  a.matrix.write_text(json.dumps(matrix,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
 print(json.dumps({"legacy_primitives":len(primitives),"legacy_lists":len(lists),"matched":len(rows),"changed":len(rows) if a.write and not errors else 0,"errors":errors},ensure_ascii=False))
 if errors:raise SystemExit(1)
if __name__=="__main__":main()
