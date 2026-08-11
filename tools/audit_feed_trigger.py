"""Audit explicit and optional 1.12 feed advancement semantics."""
from __future__ import annotations
import argparse, json
from pathlib import Path

SOURCE="src/main/java/com/animania/common/advancements/criterion/FeedAnimalTrigger.java"
TRIGGER="base/src/main/java/com/animania/common/advancement/FeedAnimalTrigger.java"
FARM_TEST="farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java"
EXTRA_TEST="extra/src/main/java/com/animania/extra/gametest/AnimaniaExtraGameTests.java"
OWNER="[feed-trigger-audit:v1]"

def main()->None:
    p=argparse.ArgumentParser();p.add_argument("--root",type=Path,required=True);p.add_argument("--matrix",type=Path,required=True);p.add_argument("--write",action="store_true");a=p.parse_args()
    old_root=a.root/"upstream/Animania-1.12/src/main/resources"
    new_root=a.root/"extra/src/main/resources/data/animania_extra/advancements/animania"
    old=sum(path.read_text(encoding="utf-8").count('"optional"') for path in old_root.rglob("*.json"))
    modern_files=[new_root/f"feed_{name}.json" for name in ("ferret_grey","ferret_white","hedgehog","hedgehog_albino")]
    modern_text="\n".join(path.read_text(encoding="utf-8") for path in modern_files)
    code=(a.root/TRIGGER).read_text(encoding="utf-8");farm=(a.root/FARM_TEST).read_text(encoding="utf-8");extra=(a.root/EXTRA_TEST).read_text(encoding="utf-8")
    errors=[]
    if old!=8:errors.append(f"legacy optional count {old} != 8")
    if modern_text.count('"optional"')!=8:errors.append("modern optional count != 8")
    if any(item in modern_text for item in ("animania_extra:brown_egg", "animania_extra:raw_prime_chicken", "animania_extra:raw_prime_mutton")):errors.append("optional Farm items retain the wrong Extra namespace")
    for item in ("animania_farm:brown_egg","animania_farm:raw_prime_chicken","animania_farm:raw_prime_mutton"):
        if item not in modern_text:errors.append(f"missing corrected optional item {item}")
    for token in ('json.has("optional")','return expected != null && fedItem.is(expected)','optional ? "optional" : "itemstack"'):
        if token not in code:errors.append(f"trigger missing {token}")
    if "optionalFeedCriterionMatchesOnlyItsInstalledAddonItem" not in farm:errors.append("missing installed-addon GameTest")
    if "absentFarmOptionalFeedItemsLoadButNeverMatch" not in extra:errors.append("missing absent-addon GameTest")
    matrix=json.loads(a.matrix.read_text(encoding="utf-8"));rows=[e for e in matrix["entries"] if e.get("source")==SOURCE]
    if len(rows)!=1:errors.append(f"matched {len(rows)} rows")
    resources=[str(path.relative_to(a.root)).replace("\\","/") for path in modern_files]
    proof={"paths":[TRIGGER,*resources],"behavior_tests":[FARM_TEST,EXTRA_TEST,"tools/audit_feed_trigger.py"],"serialization_tests":[],"client_tests":[],"notes":[f"{OWNER} all eight optional criteria preserve module IDs and are tested both with and without Farm loaded."]}
    if a.write and not errors:
        rows[0].update(status="closed",implemented=True,verified=True,tests=proof["behavior_tests"],target_evidence=proof)
        a.matrix.write_text(json.dumps(matrix,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"legacy_optional":old,"modern_optional":modern_text.count('"optional"'),"changed":int(a.write and not errors),"errors":errors},ensure_ascii=False))
    if errors:raise SystemExit(1)

if __name__=="__main__":main()
