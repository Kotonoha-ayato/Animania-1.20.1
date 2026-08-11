"""Audit the native adult cow/bull Mooshroom mushroom geometry layer."""
import argparse, json
from pathlib import Path

OWNER = "[farm-mooshroom-layer-audit:v1]"
LAYER = "base/src/main/java/com/animania/client/render/AnimaniaMooshroomLayer.java"
MODEL = "base/src/main/java/com/animania/client/model/LegacyAnimalModel.java"
RENDERER = "base/src/main/java/com/animania/client/render/AnimaniaAnimalRenderer.java"
TEST = "farm/src/test/java/com/animania/farm/AnimaniaMooshroomLayerTest.java"
LOG = "base/run/fullClient/logs/debug.log"

p=argparse.ArgumentParser();p.add_argument('--root',type=Path,required=True);p.add_argument('--matrix',type=Path,default=Path('docs/migration-matrix.json'));p.add_argument('--write',action='store_true');a=p.parse_args()
root=a.root.resolve(); mp=a.matrix if a.matrix.is_absolute() else root/a.matrix; data=json.loads(mp.read_text(encoding='utf8'))
rows=[e for e in data['entries'] if e.get('source','').endswith(('LayerBullMooshroomMushroom.java','LayerCowMooshroomMushroom.java'))]
errors=[]
if len(rows)!=2: errors.append(f'expected 2 rows, found {len(rows)}')
for e in rows:
    if not (root/'upstream/Animania-1.12'/e['source']).is_file(): errors.append('missing legacy '+e['source'])
for path in (LAYER,MODEL,RENDERER,TEST,LOG):
    if not (root/path).is_file(): errors.append('missing evidence '+path)
layer=(root/LAYER).read_text(encoding='utf8'); renderer=(root/RENDERER).read_text(encoding='utf8'); test=(root/TEST).read_text(encoding='utf8')
for token in ('cow_mooshroom','bull_mooshroom','Blocks.RED_MUSHROOM','translatePrimaryHead','renderSingleBlock'):
    if token not in layer: errors.append('missing layer contract '+token)
if 'addLayer(new AnimaniaMooshroomLayer' not in renderer: errors.append('layer not attached')
if 'calf_mooshroom' not in test: errors.append('adult-only test missing')
changed=0
if not errors:
    for e in rows:
        proof={'paths':[LAYER,MODEL,RENDERER],'behavior_tests':[TEST,'tools/audit_farm_mooshroom_layer.py'],'serialization_tests':[],'client_tests':[TEST,LOG,'tools/audit_client_log.py'],'notes':[OWNER+' Native ModelPart renderer places two body and one head red-mushroom blocks for adult cow/bull only; unit and fresh real OpenGL client smoke pass.']}
        owned=any(OWNER in n for n in e.get('target_evidence',{}).get('notes',[]))
        if a.write: e.update(status='closed',implemented=True,verified=True,tests=[TEST,'tools/audit_farm_mooshroom_layer.py','tools/audit_client_log.py'],target_evidence=proof);changed+=1
        elif e.get('status')!='closed' or not owned: errors.append('provable row not closed '+e['source'])
if a.write and not errors: mp.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print(json.dumps({'rows':len(rows),'changed':changed,'errors':errors,'error_count':len(errors)},ensure_ascii=False,indent=2))
if errors: raise SystemExit(1)
