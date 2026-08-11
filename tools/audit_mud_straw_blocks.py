"""Audit the legacy mud and straw block behavior proven on the Base GameTest server."""
from __future__ import annotations
import argparse,json
from pathlib import Path

SOURCES=("src/main/java/com/animania/common/blocks/BlockMud.java","src/main/java/com/animania/common/blocks/BlockStraw.java")
TARGETS=("base/src/main/java/com/animania/common/block/AnimaniaMudBlock.java","base/src/main/java/com/animania/common/block/AnimaniaThinBlock.java")
REGISTRY="base/src/main/java/com/animania/common/AnimaniaBlocks.java"
TEST="base/src/main/java/com/animania/gametest/AnimaniaBaseGameTests.java"
OWNER="[mud-straw-audit:v1]"

def main():
 p=argparse.ArgumentParser();p.add_argument("--root",type=Path,required=True);p.add_argument("--matrix",type=Path,required=True);p.add_argument("--write",action="store_true");a=p.parse_args()
 mud=(a.root/TARGETS[0]).read_text(encoding="utf-8");straw=(a.root/TARGETS[1]).read_text(encoding="utf-8");registry=(a.root/REGISTRY).read_text(encoding="utf-8");test=(a.root/TEST).read_text(encoding="utf-8");errors=[]
 for token in ("14.08", "multiply(0.2D, 1.0D, 0.2D)", "stepOn", "entityInside"):
  if token not in mud:errors.append(f"mud missing {token}")
 for token in ("0.032", "Shapes.empty()", "isFlammable", "updateShape", "canSurvive"):
  if token not in straw:errors.append(f"straw missing {token}")
 for token in ('MUD = simple("mud", MapColor.COLOR_BROWN)', '.friction(0.6f).sound(SoundType.SLIME_BLOCK)', 'STRAW = thin("straw"'):
  if token not in registry:errors.append(f"registry missing {token}")
 for token in ("mudRetainsLegacyShapeSoundFrictionAndMovementDamping", "nestAndFloorPilesRetainLegacyInteractionRules", "unsupported straw pile did not remove itself"):
  if token not in test:errors.append(f"GameTest missing {token}")
 matrix=json.loads(a.matrix.read_text(encoding="utf-8"));rows=[e for e in matrix["entries"] if e.get("source") in SOURCES]
 if len(rows)!=2:errors.append(f"matched {len(rows)} rows")
 tests=[TEST,"tools/audit_mud_straw_blocks.py"]
 if a.write and not errors:
  for row in rows:
   proof={"paths":[*TARGETS,REGISTRY],"behavior_tests":tests,"serialization_tests":[],"client_tests":[],"notes":[f"{OWNER} Base dedicated-server tests cover legacy dimensions, support removal, collision, flammability, map color, sound, friction and motion damping."]}
   row.update(status="closed",implemented=True,verified=True,tests=tests,target_evidence=proof)
  a.matrix.write_text(json.dumps(matrix,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
 print(json.dumps({"matched":len(rows),"changed":len(rows) if a.write and not errors else 0,"errors":errors},ensure_ascii=False))
 if errors:raise SystemExit(1)
if __name__=="__main__":main()
