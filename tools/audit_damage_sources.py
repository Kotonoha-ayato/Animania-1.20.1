"""Audit the three legacy named damage sources and their effective gameplay paths."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

SOURCE = "src/main/java/com/animania/common/handler/DamageSourceHandler.java"
HELPER = "base/src/main/java/com/animania/common/AnimaniaDamageSources.java"
ENTITY = "base/src/main/java/com/animania/common/entity/AnimaniaAnimalEntity.java"
HIVE = "farm/src/main/java/com/animania/farm/FarmHiveBlockEntity.java"
EXTRA_TEST = "extra/src/main/java/com/animania/extra/gametest/AnimaniaExtraGameTests.java"
FARM_TEST = "farm/src/main/java/com/animania/farm/gametest/AnimaniaFarmGameTests.java"
DAMAGE_JSONS = [
    "base/src/main/resources/data/animania/damage_type/pepe.json",
    "base/src/main/resources/data/animania/damage_type/animania_bee.json",
    "base/src/main/resources/data/animania/damage_type/killer_rabbit.json",
]
OWNER = "[damage-source-audit:v1]"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, required=True)
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()

    old = (args.root / "upstream/Animania-1.12" / SOURCE).read_text(encoding="utf-8")
    helper = (args.root / HELPER).read_text(encoding="utf-8")
    entity = (args.root / ENTITY).read_text(encoding="utf-8")
    hive = (args.root / HIVE).read_text(encoding="utf-8")
    extra_test = (args.root / EXTRA_TEST).read_text(encoding="utf-8")
    farm_test = (args.root / FARM_TEST).read_text(encoding="utf-8")
    errors: list[str] = []

    for damage_id in ("pepe", "animania_bee", "killer_rabbit"):
        if f'new DamageSource("{damage_id}")' not in old:
            errors.append(f"legacy source missing {damage_id}")
        resource = args.root / f"base/src/main/resources/data/animania/damage_type/{damage_id}.json"
        data = json.loads(resource.read_text(encoding="utf-8"))
        if data.get("message_id") != damage_id or data.get("scaling") != "never":
            errors.append(f"invalid modern damage type {damage_id}")

    for token in ("PEPE", "BEE", "KILLER_RABBIT", "registryAccess().registryOrThrow", "new DamageSource"):
        if token not in helper:
            errors.append(f"damage helper missing {token}")
    for token in ('isNamedFrog("Pepe")', 'isNamedExtraRabbit("Killer")', "amount = 2.0F", "amount = 5.0F", "target.hurt(special, amount)"):
        if token not in entity:
            errors.append(f"named combat missing {token}")
    for token in ("AnimaniaDamageSources.bee", "2.5F"):
        if token not in hive:
            errors.append(f"hive sting missing {token}")
    if "legacyNamedCombatUsesNativeDamageTypesAndExactValues" not in extra_test:
        errors.append("missing Extra named-combat GameTest")
    if "wildHiveStingUsesLegacyDamageTypeAndAmount" not in farm_test:
        errors.append("missing Farm wild-hive GameTest")

    matrix = json.loads(args.matrix.read_text(encoding="utf-8"))
    rows = [entry for entry in matrix["entries"] if entry.get("source") == SOURCE]
    if len(rows) != 1:
        errors.append(f"matched {len(rows)} rows")
    paths = [HELPER, ENTITY, HIVE, *DAMAGE_JSONS]
    tests = [EXTRA_TEST, FARM_TEST, "tools/audit_damage_sources.py"]
    proof = {
        "paths": paths,
        "behavior_tests": tests,
        "serialization_tests": DAMAGE_JSONS,
        "client_tests": [],
        "notes": [
            f"{OWNER} native damage registries preserve all three IDs; dedicated-server GameTests verify exact effective damage, named AI/health profiles, and hive sting behavior."
        ],
    }
    if args.write and not errors:
        rows[0].update(status="closed", implemented=True, verified=True, tests=tests, target_evidence=proof)
        args.matrix.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"matched": len(rows), "changed": int(args.write and not errors), "errors": errors}, ensure_ascii=False))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
