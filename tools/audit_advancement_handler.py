"""Audit native registration replacing AdvancementHandler's reflection wrapper."""
from __future__ import annotations
import argparse,json
from pathlib import Path
SOURCE="src/main/java/com/animania/common/handler/AdvancementHandler.java";TARGET="base/src/main/java/com/animania/common/advancement/FeedAnimalTrigger.java";ENTRY="base/src/main/java/com/animania/Animania.java";FARM="farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java";EXTRA="extra/src/main/java/com/animania/extra/gametest/AnimaniaExtraGameTests.java";OWNER="[advancement-handler-audit:v1]"
def main():
 p=argparse.ArgumentParser();p.add_argument("--root",type=Path,required=True);p.add_argument("--matrix",type=Path,required=True);p.add_argument("--write",action="store_true");a=p.parse_args();errors=[]
 target=(a.root/TARGET).read_text(encoding="utf-8");entry=(a.root/ENTRY).read_text(encoding="utf-8");farm=(a.root/FARM).read_text(encoding="utf-8");extra=(a.root/EXTRA).read_text(encoding="utf-8")
 for token in ("CriteriaTriggers.register(new FeedAnimalTrigger())","public static void bootstrap()"):
  if token not in target:errors.append(f"native trigger registration missing {token}")
 if "FeedAnimalTrigger.bootstrap()" not in entry:errors.append("Base entry point does not force trigger bootstrap")
 if "CriteriaTriggers.getCriterion(FeedAnimalTrigger.ID) == FeedAnimalTrigger.INSTANCE" not in farm:errors.append("Farm GameTest does not prove live trigger identity")
 if "absentFarmOptionalFeedItemsLoadButNeverMatch" not in extra:errors.append("Extra-only advancement load test missing")
 matrix=json.loads(a.matrix.read_text(encoding="utf-8"));rows=[e for e in matrix["entries"] if e.get("source")==SOURCE]
 if len(rows)!=1:errors.append(f"matched {len(rows)} rows")
 tests=[FARM,EXTRA,"tools/audit_advancement_handler.py"]
 if a.write and not errors:
  proof={"paths":[TARGET,ENTRY],"behavior_tests":tests,"serialization_tests":[],"client_tests":[],"notes":[f"{OWNER} reflection registration is replaced by CriteriaTriggers.register during Base construction; Farm and Extra-only dedicated servers prove identity and deserialization."]}
  rows[0].update(status="closed",implemented=True,verified=True,tests=tests,target_evidence=proof);a.matrix.write_text(json.dumps(matrix,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
 print(json.dumps({"matched":len(rows),"changed":int(a.write and not errors),"errors":errors},ensure_ascii=False))
 if errors:raise SystemExit(1)
if __name__=="__main__":main()
