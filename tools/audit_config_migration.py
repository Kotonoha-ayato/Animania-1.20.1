"""Prove source-derived addon config defaults in ForgeConfigSpec and the converter."""
from __future__ import annotations
import argparse, json, re
from pathlib import Path

OWNER = "[config-audit:v1]"
MODULES = {
 "farm": ("upstream/Animania-1.12/src/main/java/com/animania/addons/farm/config/FarmConfig.java", "farm/src/main/java/com/animania/farm/FarmConfig.java", "farm/src/main/java/com/animania/farm/AnimaniaFarm.java", "farm/src/test/java/com/animania/farm/FarmRegistryTest.java"),
 "extra": ("upstream/Animania-1.12/src/main/java/com/animania/addons/extra/config/ExtraConfig.java", "extra/src/main/java/com/animania/extra/ExtraConfig.java", "extra/src/main/java/com/animania/extra/AnimaniaExtra.java", "extra/src/test/java/com/animania/extra/ExtraRegistryTest.java"),
 "catsdogs": ("upstream/Animania-1.12/src/main/java/com/animania/addons/catsdogs/config/CatsDogsConfig.java", "catsdogs/src/main/java/com/animania/catsdogs/CatsDogsConfig.java", "catsdogs/src/main/java/com/animania/catsdogs/AnimaniaCatsDogs.java", "catsdogs/src/test/java/com/animania/catsdogs/CatsDogsRegistryTest.java"),
}
KEY_ALIASES = {"hivePlayermadeHoneyRate":"hivePlayerHoneyRate", "hamsterWheelRFGeneration":"hamsterWheelGeneration"}
VALUE_ALIASES = {
 "animania:block_straw":"animania:straw", "minecraft:grass":"minecraft:grass_block",
 "animania:brown_egg":"animania_farm:brown_egg", "animania:peacock_egg_blue":"animania_extra:peacock_egg_blue",
 "animania:peacock_egg_white":"animania_extra:peacock_egg_white", "animania:prime_mutton":"animania_farm:raw_prime_mutton",
 "animania:prime_rabbit":"animania_extra:raw_prime_rabbit", "animania_prime_chicken":"animania_farm:raw_prime_chicken",
 "animania:hamster_food":"animania_extra:hamster_food", "animania:cat_bed_1":"animania_catsdogs:cat_bed_1",
 "animania:cat_bed_2":"animania_catsdogs:cat_bed_2", "animania:dog_pillow":"animania_catsdogs:dog_pillow",
}

def calls(text, name):
 out=[]
 for m in re.finditer(r"\b"+re.escape(name)+r"\s*\(", text):
  start=m.end(); depth=1; quote=escape=False
  for i in range(start,len(text)):
   c=text[i]
   if quote:
    if escape: escape=False
    elif c=="\\": escape=True
    elif c=='"': quote=False
   elif c=='"': quote=True
   elif c=='(': depth+=1
   elif c==')':
    depth-=1
    if depth==0: out.append(text[start:i]); break
 return out

def split_args(value):
 out=[]; start=depth=0; quote=escape=False
 for i,c in enumerate(value):
  if quote:
   if escape: escape=False
   elif c=="\\": escape=True
   elif c=='"': quote=False
  elif c=='"': quote=True
  elif c in "([{": depth+=1
  elif c in ")]}": depth-=1
  elif c==',' and depth==0: out.append(value[start:i].strip()); start=i+1
 out.append(value[start:].strip()); return out

def decode_java_string(raw):
 return json.loads('"'+raw+'"')

def parse_value(raw):
 raw=re.sub(r"^new\s+String\s*\[\s*]\s*", "", raw.strip())
 if raw.startswith("List.of(") and raw.endswith(")"): raw="{"+raw[8:-1]+"}"
 if raw.startswith("{") and raw.endswith("}"): return [decode_java_string(x) for x in re.findall(r'"((?:\\.|[^"\\])*)"',raw)]
 if raw.startswith('"') and raw.endswith('"'): return decode_java_string(raw[1:-1])
 if raw.lower() in ("true","false"): return raw.lower()=="true"
 n=re.sub(r"[fFdDlL]$","",raw)
 if re.fullmatch(r"-?\d+",n): return int(n)
 if re.fullmatch(r"-?(?:\d+\.\d*|\.\d+)",n): return float(n)
 raise ValueError("unsupported Java default: "+raw)

def legacy_defaults(text):
 p=re.compile(r"\bpublic\s+(boolean|int|float|double|String(?:\s*\[\s*])?)\s+(\w+)\s*=\s*(.*?);",re.S)
 return {name:parse_value(raw) for _,name,raw in p.findall(text)}

def modern_defaults(text):
 out={}
 for name in ("define","defineInRange","defineList"):
  for body in calls(text,name):
   a=split_args(body)
   if len(a)>=2 and re.fullmatch(r'"[A-Za-z0-9_]+"',a[0]): out[a[0][1:-1]]=parse_value(a[1])
 for body in calls(text,"defineBiome"):
  a=split_args(body)
  if len(a)>=4 and re.fullmatch(r'"[A-Za-z0-9_]+"',a[2]): out[a[2][1:-1]]=parse_value(a[3])
 return out

def normalize(value):
 if isinstance(value,list): return [normalize(x) for x in value]
 return VALUE_ALIASES.get(value,value) if isinstance(value,str) else value

def migrator_defaults(text):
 out={}
 for k,raw in re.findall(r'Map\.entry\("([A-Za-z0-9_]+)",\s*"((?:\\.|[^"\\])*)"\)',text):
  value=decode_java_string(raw)
  try: out[k]=json.loads(value)
  except json.JSONDecodeError:
   try: out[k]=parse_value(value)
   except ValueError: out[k]=value
 for body in calls(text,"putDefaults"):
  values=[decode_java_string(x) for x in re.findall(r'"((?:\\.|[^"\\])*)"',body)]
  if len(values)%2: raise ValueError("putDefaults requires pairs")
  for k,raw in zip(values[::2],values[1::2]):
   try: value=json.loads(raw)
   except json.JSONDecodeError:
    try: value=parse_value(raw)
    except ValueError: value=raw
   out.setdefault(k,value)
 return out

def main():
 ap=argparse.ArgumentParser(); ap.add_argument("--root",type=Path,required=True); ap.add_argument("--matrix",type=Path,default=Path("docs/migration-matrix.json")); ap.add_argument("--ledger",type=Path,default=Path("docs/content-ledger.json")); ap.add_argument("--write",action="store_true"); a=ap.parse_args(); root=a.root.resolve()
 migrator_file="config-migrator/src/main/java/com/animania/migrator/ConfigMigrator.java"
 migrator=migrator_defaults((root/migrator_file).read_text(encoding="utf-8")); expected={}; errors=[]
 for module,(old_path,target_path,main_path,test_path) in MODULES.items():
  old=legacy_defaults((root/old_path).read_text(encoding="utf-8")); modern=modern_defaults((root/target_path).read_text(encoding="utf-8")); expected[module]={}
  for old_key,old_value in old.items():
   key=KEY_ALIASES.get(old_key,old_key); value=normalize(old_value); expected[module][key]=value
   if modern.get(key,object())!=value: errors.append(f"{module}:{old_key}: Forge default {modern.get(key)!r} != {value!r}")
   if migrator.get(key,object())!=value: errors.append(f"{module}:{old_key}: migrator default {migrator.get(key)!r} != {value!r}")
  name="CatsDogsConfig" if module=="catsdogs" else module.title()+"Config"
  if f"registerConfig(ModConfig.Type.COMMON, {name}.SPEC)" not in (root/main_path).read_text(encoding="utf-8"): errors.append(module+": COMMON config not registered")
 matrix_path=a.matrix if a.matrix.is_absolute() else root/a.matrix; ledger_path=a.ledger if a.ledger.is_absolute() else root/a.ledger
 matrix=json.loads(matrix_path.read_text(encoding="utf-8")); ledger=json.loads(ledger_path.read_text(encoding="utf-8")); mc=lc=0
 if not errors:
  for e in matrix["entries"]:
   module=e.get("module"); filename=Path(e.get("source","")).name
   if module not in MODULES or filename != ("CatsDogsConfig.java" if module=="catsdogs" else module.title()+"Config.java"): continue
   target_path=MODULES[module][1]; test_path=MODULES[module][3]
   proof={"paths":[target_path,migrator_file,"tools/audit_config_migration.py"],"behavior_tests":[test_path,"config-migrator/src/test/java/com/animania/migrator/ConfigMigratorTest.java"],"serialization_tests":[],"client_tests":[],"notes":[f"{OWNER} all {len(expected[module])} source keys/defaults match ForgeConfigSpec and converter after ID normalization."]}
   owned=any(OWNER in n for n in e.get("target_evidence",{}).get("notes",[]))
   if a.write: e.update(status="closed",implemented=True,verified=True,tests=proof["behavior_tests"],target_evidence=proof); mc+=1
   elif e.get("status")!="closed" or not owned: errors.append(module+": config Java proof not closed")
  for e in ledger["entries"]:
   module=e.get("module"); old_key=e.get("legacy_id"); key=KEY_ALIASES.get(old_key,old_key)
   if e.get("kind")!="config" or module not in MODULES or key not in expected[module]: continue
   owned=any(OWNER in n for n in e.get("notes",[]))
   if a.write:
    e.update(status="closed",implemented=True,verified=True,notes=[OWNER+" exact normalized default and converter mapping verified."]); e["target"]["behavior"]=[key]; e["verification"]["unit_tests"]=[MODULES[module][3],"config-migrator/src/test/java/com/animania/migrator/ConfigMigratorTest.java"]; lc+=1
   elif e.get("status")!="closed" or not owned: errors.append(f"{module}:{old_key}: ledger proof not closed")
 if a.write and not errors:
  ledger["open"]=sum(e.get("status")!="closed" for e in ledger["entries"]); ledger["closed"]=len(ledger["entries"])-ledger["open"]; ledger["release_allowed"]=False
  matrix_path.write_text(json.dumps(matrix,ensure_ascii=False,indent=2)+"\n",encoding="utf-8"); ledger_path.write_text(json.dumps(ledger,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
 print(json.dumps({"modules":{m:len(v) for m,v in expected.items()},"matrix_changed":mc,"ledger_changed":lc,"errors":errors,"error_count":len(errors)},ensure_ascii=False,indent=2))
 if errors: raise SystemExit(1)

if __name__=="__main__": main()
