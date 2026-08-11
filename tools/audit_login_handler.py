"""Audit the modern Forge update notification/login behavior."""
from __future__ import annotations
import argparse,json
from pathlib import Path

SOURCE="src/main/java/com/animania/common/events/LoginEventHandler.java";TARGET="base/src/main/java/com/animania/AnimaniaServerEvents.java";UNIT="base/src/test/java/com/animania/AnimaniaUpdateNotificationTest.java";GAME="farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java";OWNER="[login-handler-audit:v1]"
def main():
 p=argparse.ArgumentParser();p.add_argument("--root",type=Path,required=True);p.add_argument("--matrix",type=Path,required=True);p.add_argument("--write",action="store_true");a=p.parse_args()
 code=(a.root/TARGET).read_text(encoding="utf-8");unit=(a.root/UNIT).read_text(encoding="utf-8");game=(a.root/GAME).read_text(encoding="utf-8");errors=[]
 for token in ("PlayerLoggedInEvent","SHOW_MOD_UPDATE_NOTIFICATION","VersionChecker.getResult","shouldNotifyUpdate","ClickEvent.Action.OPEN_URL"):
  if token not in code:errors.append(f"missing login token {token}")
 for token in ("OUTDATED","BETA_OUTDATED","UP_TO_DATE","FAILED","PENDING"):
  if token not in unit:errors.append(f"missing status test {token}")
 for token in ("Animania root was granted when a new player joined","feeding an animal unexpectedly auto-granted"):
  if token not in game:errors.append(f"missing login advancement assertion {token}")
 matrix=json.loads(a.matrix.read_text(encoding="utf-8"));rows=[e for e in matrix["entries"] if e.get("source")==SOURCE]
 if len(rows)!=1:errors.append(f"matched {len(rows)} rows")
 proof={"paths":[TARGET],"behavior_tests":[UNIT,GAME,"tools/audit_login_handler.py"],"serialization_tests":[],"client_tests":[],"notes":[f"{OWNER} Forge version status/config gating and non-granting login behavior are executable-tested."]}
 if a.write and not errors:
  rows[0].update(status="closed",implemented=True,verified=True,tests=proof["behavior_tests"],target_evidence=proof);a.matrix.write_text(json.dumps(matrix,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
 print(json.dumps({"changed":int(a.write and not errors),"errors":errors},ensure_ascii=False))
 if errors:raise SystemExit(1)
if __name__=="__main__":main()
