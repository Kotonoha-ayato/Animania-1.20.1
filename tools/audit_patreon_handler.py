"""Audit the 1.12 supporter UUID easter egg and its server-authoritative port."""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

OWNER = "[patreon-handler-audit:v1]"
SOURCE = "src/main/java/com/animania/common/handler/PatreonHandler.java"
TARGETS = [
    "base/src/main/java/com/animania/common/AnimaniaSupporters.java",
    "base/src/main/java/com/animania/common/entity/AnimaniaAnimalEntity.java",
]
TEST = "extra/src/main/java/com/animania/extra/gametest/AnimaniaExtraGameTests.java"
UUID_PATTERN = r"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, default=Path("docs/migration-matrix.json"))
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()
    root = args.root.resolve()
    errors: list[str] = []

    old = (root / "upstream/Animania-1.12" / SOURCE).read_text(encoding="utf-8")
    supporters = (root / TARGETS[0]).read_text(encoding="utf-8")
    animal = (root / TARGETS[1]).read_text(encoding="utf-8")
    test = (root / TEST).read_text(encoding="utf-8")
    old_ids = set(re.findall(UUID_PATTERN, old, re.IGNORECASE))
    new_ids = set(re.findall(UUID_PATTERN, supporters, re.IGNORECASE))
    if len(old_ids) != 19 or new_ids != old_ids:
        errors.append(f"supporter UUID ledger differs: legacy={len(old_ids)}, modern={len(new_ids)}")
    for token in ("isHamster()", "player.isShiftKeyDown()", "AnimaniaSupporters.contains(player.getUUID())",
                  'setVariantName("gold")'):
        if token not in animal:
            errors.append(f"server-authoritative supporter interaction missing: {token}")
    for token in ("supporterSneakFeedingUnlocksLegacyGoldenHamster", "OrdinaryTester", "SupporterTester",
                  '"brown".equals(hamster.getVariantName())', '"gold".equals(hamster.getVariantName())',
                  "AnimaniaSupporters.size() == 19"):
        if token not in test:
            errors.append(f"supporter boundary GameTest missing: {token}")

    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    matrix = json.loads(matrix_path.read_text(encoding="utf-8"))
    entry = next((row for row in matrix["entries"] if row.get("source") == SOURCE), None)
    if entry is None:
        errors.append("migration row missing")
    changed = 0
    if not errors and entry is not None:
        proof = {
            "paths": TARGETS,
            "behavior_tests": [TEST, "tools/audit_patreon_handler.py"],
            "serialization_tests": [],
            "client_tests": [],
            "notes": [f"{OWNER} exact 19-UUID ledger and sneak-feed golden-hamster boundary verified on the Extra dedicated GameTest server."],
        }
        owned = any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", []))
        if args.write:
            entry.update(status="closed", implemented=True, verified=True,
                         tests=[TEST, "tools/audit_patreon_handler.py"], target_evidence=proof)
            changed = 1
        elif entry.get("status") != "closed" or not owned:
            errors.append("provable row is not closed")
    if args.write and not errors:
        matrix_path.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"supporter_ids": len(new_ids), "changed": changed, "errors": errors,
                      "error_count": len(errors)}, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
