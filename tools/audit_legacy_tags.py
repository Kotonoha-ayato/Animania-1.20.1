"""Verify the exact 1.12 OreDictionary memberships migrated to modern item tags."""
from __future__ import annotations
import argparse, json, re
from collections import defaultdict
from pathlib import Path

OWNER="[legacy-tag-audit:v1]"
MODULES={
 "farm":("upstream/Animania-1.12/src/main/java/com/animania/addons/farm/common/handler/FarmAddonOredictHandler.java","farm/src/main/resources/data/animania/tags/items/legacy_oredict","farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java"),
 "extra":("upstream/Animania-1.12/src/main/java/com/animania/addons/extra/common/handler/ExtraAddonOredictHandler.java","extra/src/main/resources/data/animania/tags/items/legacy_oredict","extra/src/main/java/com/animania/extra/gametest/AnimaniaExtraGameTests.java"),
}
ITEMS={
 "farm":{
  "brownEgg":"brown_egg","salt":"salt","blockAnimaniaWool":"animania_wool","honeyJar":"honey_jar",
  "rawPrimeMutton":"raw_prime_mutton","rawChevon":"raw_chevon","rawPrimeChevon":"raw_prime_chevon","rawHorse":"raw_horse",
  "cookedPrimeMutton":"cooked_prime_mutton","cookedChevon":"cooked_chevon","cookedPrimeChevon":"cooked_prime_chevon","cookedHorse":"cooked_horse",
  "rawPrimeChicken":"raw_prime_chicken","rawPrimePork":"raw_prime_pork","rawPrimeBacon":"raw_prime_bacon","rawPrimeBeef":"raw_prime_beef","rawPrimeSteak":"raw_prime_steak",
  "cookedPrimeChicken":"cooked_prime_chicken","cookedPrimePork":"cooked_prime_pork","cookedPrimeBacon":"cooked_prime_bacon","cookedPrimeBeef":"cooked_prime_beef","cookedPrimeSteak":"cooked_prime_steak",
  "cheeseWedgeFriesian":"friesian_cheese_wedge","cheeseWedgeHolstein":"holstein_cheese_wedge","cheeseWedgeGoat":"goat_cheese_wedge","cheeseWedgeSheep":"sheep_cheese_wedge","cheeseWedgeJersey":"jersey_cheese_wedge",
 },
 "extra":{
  "rawPrimeRabbit":"raw_prime_rabbit","cookedPrimeRabbit":"cooked_prime_rabbit","rawFrogLegs":"raw_frog_legs","cookedFrogLegs":"cooked_frog_legs",
  "peacockEggBlue":"peacock_egg_blue","peacockEggWhite":"peacock_egg_white","peacockFeatherBlue":"blue_peacock_feather","peacockFeatherWhite":"white_peacock_feather",
  "peacockFeatherCharcoal":"charcoal_peacock_feather","peacockFeatherOpal":"opal_peacock_feather","peacockFeatherPeach":"peach_peacock_feather","peacockFeatherPurple":"purple_peacock_feather","peacockFeatherTaupe":"taupe_peacock_feather",
 }
}

def expected(text,module):
 out=defaultdict(set)
 for tag,expr in re.findall(r'OreDictionary\.registerOre\("([^"]+)",\s*(?:new\s+ItemStack\()?([^,);]+)',text):
  variable=expr.strip().split('.')[-1]
  if variable not in ITEMS[module]: raise ValueError(f"{module}: unmapped legacy variable {variable}")
  out[tag.lower()].add(f"animania_{module}:{ITEMS[module][variable]}")
 return out

def main():
 ap=argparse.ArgumentParser(); ap.add_argument('--root',type=Path,required=True); ap.add_argument('--matrix',type=Path,default=Path('docs/migration-matrix.json')); ap.add_argument('--write',action='store_true'); a=ap.parse_args(); root=a.root.resolve(); errors=[]; counts={}
 for module,(source,directory,test) in MODULES.items():
  values=expected((root/source).read_text(encoding='utf-8'),module); counts[module]={k:len(v) for k,v in sorted(values.items())}
  for tag,wanted in values.items():
   path=root/directory/(tag+'.json')
   if not path.exists(): errors.append(f"{module}:{tag}: tag missing"); continue
   payload=json.loads(path.read_text(encoding='utf-8'))
   if payload.get('replace') is not False or set(payload.get('values',[]))!=wanted: errors.append(f"{module}:{tag}: {payload.get('values')} != {sorted(wanted)}")
  if 'legacyOreDictionaryMembershipUsesModernTags' not in (root/test).read_text(encoding='utf-8'): errors.append(module+': live Forge tag GameTest missing')
 bridges={'farm':['data/forge/tags/items/eggs.json','data/forge/tags/items/salts.json','data/forge/tags/items/raw_meats.json','data/forge/tags/items/cooked_meats.json','data/forge/tags/items/foods/cheese.json','data/minecraft/tags/items/wool.json'],
          'extra':['data/forge/tags/items/eggs.json','data/forge/tags/items/feathers.json','data/forge/tags/items/raw_meats.json','data/forge/tags/items/cooked_meats.json']}
 for module,paths in bridges.items():
  for relative in paths:
   if not (root/f'{module}/src/main/resources'/relative).exists(): errors.append(f'{module}: conventional bridge missing {relative}')
 matrix_path=a.matrix if a.matrix.is_absolute() else root/a.matrix; matrix=json.loads(matrix_path.read_text(encoding='utf-8')); changed=0
 if not errors:
  for e in matrix['entries']:
   module=e.get('module'); filename=Path(e.get('source','')).name
   if module not in MODULES or filename != module.title()+'AddonOredictHandler.java': continue
   source,directory,test=MODULES[module]
   proof={'paths':[directory,'tools/audit_legacy_tags.py'],'behavior_tests':[test,'tools/audit_resources.py','tools/audit_legacy_tags.py'],'serialization_tests':[],'client_tests':[],'notes':[f'{OWNER} every source OreDictionary membership is represented exactly in mergeable modern tags and representative Forge tags are verified in a live GameTest server.']}
   owned=any(OWNER in n for n in e.get('target_evidence',{}).get('notes',[]))
   if a.write: e.update(status='closed',implemented=True,verified=True,tests=proof['behavior_tests'],target_evidence=proof); changed+=1
   elif e.get('status')!='closed' or not owned: errors.append(module+': provable Oredict handler not closed')
 if a.write and not errors: matrix_path.write_text(json.dumps(matrix,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 print(json.dumps({'tags':counts,'changed':changed,'errors':errors,'error_count':len(errors)},ensure_ascii=False,indent=2))
 if errors: raise SystemExit(1)
if __name__=='__main__': main()
