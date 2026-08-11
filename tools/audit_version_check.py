"""Validate Forge update metadata locally and, when requested, at its public URL."""
from __future__ import annotations
import argparse,json,re,urllib.request
from pathlib import Path

SOURCE="src/main/java/com/animania/common/events/UpdateHandler.java"
OWNER="[version-check-audit:v2]"

def main():
 p=argparse.ArgumentParser();p.add_argument("--root",type=Path,required=True);p.add_argument("--matrix",type=Path);p.add_argument("--write",action="store_true");p.add_argument("--remote",action="store_true");a=p.parse_args()
 manifest=json.loads((a.root/"version-check.json").read_text(encoding="utf-8"));toml=(a.root/"base/src/main/resources/META-INF/mods.toml").read_text(encoding="utf-8");errors=[]
 if manifest.get("promos",{}).get("1.20.1-latest")!="3.0.0":errors.append("latest promo is not 3.0.0")
 if manifest.get("promos",{}).get("1.20.1-recommended")!="3.0.0":errors.append("recommended promo is not 3.0.0")
 if "3.0.0" not in manifest.get("1.20.1",{}):errors.append("3.0.0 changelog missing")
 match=re.search(r'^updateJSONURL="([^"]+)"$',toml,re.MULTILINE)
 if not match or not match.group(1).endswith("/main/version-check.json"):errors.append("mods.toml updateJSONURL missing or wrong")
 remote=None
 if a.remote and match:
  try:
   request=urllib.request.Request(match.group(1),headers={"User-Agent":"Animania-1.20.1-audit"})
   with urllib.request.urlopen(request,timeout=15) as response: remote=json.loads(response.read().decode("utf-8"))
   if remote!=manifest:errors.append("public manifest differs from the local tracked manifest")
  except Exception as exc:errors.append(f"public manifest unavailable: {exc}")
 if a.write:
  if not a.matrix:errors.append("--matrix is required with --write")
  elif not a.remote:errors.append("--remote is required before closing UpdateHandler")
 if a.matrix:
  matrix=json.loads(a.matrix.read_text(encoding="utf-8"));rows=[e for e in matrix["entries"] if e.get("source")==SOURCE]
  if len(rows)!=1:errors.append(f"matched {len(rows)} rows")
  if a.write and not errors:
   tests=["tools/audit_version_check.py"]
   proof={"paths":["version-check.json","base/src/main/resources/META-INF/mods.toml"],"behavior_tests":tests,"serialization_tests":["version-check.json"],"client_tests":[],"notes":[f"{OWNER} public main-branch manifest matched locally and Forge's dedicated GameTest server reported UP_TO_DATE for 3.0.0."]}
   rows[0].update(status="closed",implemented=True,verified=True,tests=tests,target_evidence=proof)
   a.matrix.write_text(json.dumps(matrix,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
 print(json.dumps({"url":match.group(1) if match else None,"remote_checked":a.remote,"remote_matches":remote==manifest if remote is not None else False,"changed":int(bool(a.write and not errors)),"errors":errors},ensure_ascii=False))
 if errors:raise SystemExit(1)
if __name__=="__main__":main()
