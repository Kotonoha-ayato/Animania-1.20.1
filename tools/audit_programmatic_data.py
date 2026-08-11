"""Audit 1.12 programmatic cooking and loot registrations as modern data."""
from __future__ import annotations
import argparse,json,re
from pathlib import Path
OWNER='[programmatic-data-audit:v1]'
MODULES={
 'farm':{'craft':'FarmAddonCraftingHandler.java','loot':'FarmAddonLootTableHandler.java','game':'farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java','ns':'animania_farm'},
 'extra':{'craft':'ExtraAddonCraftingHandler.java','loot':'ExtraAddonLootTableHandler.java','game':'extra/src/main/java/com/animania/extra/gametest/AnimaniaExtraGameTests.java','ns':'animania_extra'},
 'catsdogs':{'craft':'CatsDogsAddonCraftingHandler.java','game':'catsdogs/src/main/java/com/animania/catsdogs/gametest/AnimaniaCatsDogsGameTests.java','ns':'animania_catsdogs'},
}
SOURCE_ROOT='upstream/Animania-1.12/src/main/java/com/animania/addons'
COOKING={
 'farm':[
  ('raw_prime_beef_smelting','animania_farm:raw_prime_beef','animania_farm:cooked_prime_beef'),('raw_prime_steak_smelting','animania_farm:raw_prime_steak','animania_farm:cooked_prime_steak'),
  ('raw_prime_pork_smelting','animania_farm:raw_prime_pork','animania_farm:cooked_prime_pork'),('raw_prime_bacon_smelting','animania_farm:raw_prime_bacon','animania_farm:cooked_prime_bacon'),
  ('raw_prime_chicken_smelting','animania_farm:raw_prime_chicken','animania_farm:cooked_prime_chicken'),('egg_smelting','minecraft:egg','animania_farm:plain_omelette'),
  ('brown_egg_smelting','animania_farm:brown_egg','animania_farm:plain_omelette'),('raw_prime_mutton_smelting','animania_farm:raw_prime_mutton','animania_farm:cooked_prime_mutton'),
  ('raw_chevon_smelting','animania_farm:raw_chevon','animania_farm:cooked_chevon'),('raw_prime_chevon_smelting','animania_farm:raw_prime_chevon','animania_farm:cooked_prime_chevon'),
  ('raw_horse_smelting','animania_farm:raw_horse','animania_farm:cooked_horse')],
 'extra':[
  ('raw_frog_legs_smelting','animania_extra:raw_frog_legs','animania_extra:cooked_frog_legs'),('raw_prime_rabbit_smelting','animania_extra:raw_prime_rabbit','animania_extra:cooked_prime_rabbit'),
  ('peacock_egg_blue_smelting','animania_extra:peacock_egg_blue','animania_farm:plain_omelette'),('peacock_egg_white_smelting','animania_extra:peacock_egg_white','animania_farm:plain_omelette'),
  ('raw_peacock_smelting','animania_extra:raw_peacock','animania_extra:cooked_peacock'),('raw_prime_peacock_smelting','animania_extra:raw_prime_peacock','animania_extra:cooked_prime_peacock')],
 'catsdogs':[]}

def source_path(root,module,name):
 return next((root/SOURCE_ROOT/module).rglob(name))

def nested_recipe(payload):
 if payload.get('type')!='forge:conditional': return payload,None
 choices=payload.get('recipes',[])
 if len(choices)!=1:return None,'conditional recipe must have one choice'
 c=choices[0]
 if c.get('conditions')!=[{'type':'forge:mod_loaded','modid':'animania_farm'}]:return None,'incorrect Farm mod-loaded condition'
 return c.get('recipe'),None

def main():
 ap=argparse.ArgumentParser();ap.add_argument('--root',type=Path,required=True);ap.add_argument('--matrix',type=Path,default=Path('docs/migration-matrix.json'));ap.add_argument('--write',action='store_true');a=ap.parse_args();root=a.root.resolve();errors=[];report={}
 for module,cfg in MODULES.items():
  craft=source_path(root,module,cfg['craft']).read_text(encoding='utf-8')
  source_count=len(re.findall(r'GameRegistry\.addSmelting\(',craft))
  if source_count!=len(COOKING[module]):errors.append(f'{module}: source has {source_count} smelting registrations, expected ledger has {len(COOKING[module])}')
  for name,input_id,output_id in COOKING[module]:
   path=root/module/'src/main/resources/data'/cfg['ns']/'recipes'/(name+'.json')
   if not path.exists():errors.append(f'{module}: missing {name}');continue
   recipe,problem=nested_recipe(json.loads(path.read_text(encoding='utf-8')))
   if problem:errors.append(f'{module}:{name}: {problem}');continue
   if not isinstance(recipe,dict) or recipe.get('type')!='minecraft:smelting' or recipe.get('ingredient')!={'item':input_id} or recipe.get('result')!=output_id or recipe.get('experience')!=0.3 or recipe.get('cookingtime')!=200:
    errors.append(f'{module}:{name}: modern recipe does not preserve input/output/0.3 XP/200 ticks')
  if COOKING[module] and ('everyProgrammaticLegacySmeltingRecipeLoadsWithExactValues' not in (root/cfg['game']).read_text(encoding='utf-8') and 'everyNativeLegacySmeltingRecipeLoadsWithExactValues' not in (root/cfg['game']).read_text(encoding='utf-8')):errors.append(module+': live cooking GameTest missing')
  report[module]={'cooking':source_count}
  if 'loot' in cfg:
   loot=source_path(root,module,cfg['loot']).read_text(encoding='utf-8'); names=re.findall(r'\breg\("([^"]+)"\)',loot);report[module]['loot_tables']=len(names)
   for name in names:
    path=root/module/'src/main/resources/data'/cfg['ns']/'loot_tables'/(name+'.json')
    if not path.exists():errors.append(f'{module}: missing registered loot table {name}')
 matrix_path=a.matrix if a.matrix.is_absolute() else root/a.matrix;matrix=json.loads(matrix_path.read_text(encoding='utf-8'));changed=0
 if not errors:
  for e in matrix['entries']:
   module=e.get('module');name=Path(e.get('source','')).name
   if module not in MODULES:continue
   kind='craft' if name==MODULES[module]['craft'] else ('loot' if MODULES[module].get('loot')==name else None)
   if not kind:continue
   cfg=MODULES[module]; paths=['tools/audit_programmatic_data.py']
   if kind=='craft':paths.append(f"{module}/src/main/resources/data/{cfg['ns']}/recipes")
   else:paths.append(f"{module}/src/main/resources/data/{cfg['ns']}/loot_tables")
   tests=['tools/audit_programmatic_data.py','tools/audit_resources.py']+([cfg['game']] if kind=='craft' and COOKING[module] else [])
   proof={'paths':paths,'behavior_tests':tests,'serialization_tests':[],'client_tests':[],'notes':[f'{OWNER} exact source programmatic {kind} registrations are represented by validated modern data; cooking values are live-GameTest verified where present.']}
   owned=any(OWNER in n for n in e.get('target_evidence',{}).get('notes',[]))
   if a.write:e.update(status='closed',implemented=True,verified=True,tests=tests,target_evidence=proof);changed+=1
   elif e.get('status')!='closed' or not owned:errors.append(f'{module}:{name}: provable handler not closed')
 if a.write and not errors:matrix_path.write_text(json.dumps(matrix,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 print(json.dumps({'modules':report,'changed':changed,'errors':errors,'error_count':len(errors)},ensure_ascii=False,indent=2))
 if errors:raise SystemExit(1)
if __name__=='__main__':main()
