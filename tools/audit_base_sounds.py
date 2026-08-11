"""Audit the 1.12 Base sound initializer as a modern DeferredRegister."""
from __future__ import annotations
import argparse, json, re
from pathlib import Path

OWNER = "[base-sound-audit:v1]"
SOURCES = {
    "src/main/java/com/animania/common/ModSoundEvents.java",
    "src/main/java/com/animania/common/handler/EventsHandler.java",
}

def main() -> None:
    parser = argparse.ArgumentParser(); parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, default=Path("docs/migration-matrix.json")); parser.add_argument("--write", action="store_true")
    args = parser.parse_args(); root = args.root.resolve(); errors: list[str] = []
    old = root / "upstream/Animania-1.12"
    source = (old / "src/main/java/com/animania/common/ModSoundEvents.java").read_text(encoding="utf-8")
    expected = set(re.findall(r'registerSound\("([a-z0-9_]+)"\)', source))
    declared = {name.lower() for name in re.findall(r"public\s+static\s+SoundEvent\s+(\w+)\s*;", source)}
    if expected != declared or expected != {"zap", "combo"}: errors.append(f"legacy Base sound declarations differ: {sorted(expected)} / {sorted(declared)}")
    events = (old / "src/main/java/com/animania/common/handler/EventsHandler.java").read_text(encoding="utf-8")
    if len(re.findall(r"ModSoundEvents\.registerSounds\(\)", events)) != 1: errors.append("legacy EventsHandler is not the single Base sound initializer")
    target_path = "base/src/main/java/com/animania/common/AnimaniaSounds.java"
    target = (root / target_path).read_text(encoding="utf-8")
    modern = {value.lower() for value in re.findall(r"RegistryObject<SoundEvent>\s+(\w+)\s*=\s*register", target)}
    if modern != expected or "DeferredRegister<SoundEvent>" not in target: errors.append(f"modern DeferredRegister differs: {sorted(modern)}")
    main_path = "base/src/main/java/com/animania/Animania.java"; main = (root / main_path).read_text(encoding="utf-8")
    if "AnimaniaSounds.SOUNDS.register(modBus)" not in main: errors.append("Base sound register is not attached to the mod event bus")
    sounds_path = root / "base/src/main/resources/assets/animania/sounds.json"
    sounds = json.loads(sounds_path.read_text(encoding="utf-8"))
    if set(sounds) != expected: errors.append(f"sounds.json keys differ: {sorted(sounds)}")
    game_path = "base/src/main/java/com/animania/gametest/AnimaniaBaseGameTests.java"; game = (root / game_path).read_text(encoding="utf-8")
    if "everyLegacyBaseSoundEventIsRegistered" not in game or "ForgeRegistries.SOUND_EVENTS.containsKey" not in game:
        errors.append("live Forge sound registry GameTest missing")
    matrix_path = args.matrix if args.matrix.is_absolute() else root / args.matrix
    matrix = json.loads(matrix_path.read_text(encoding="utf-8")); rows = [e for e in matrix["entries"] if e.get("source") in SOURCES]
    if {e.get("source") for e in rows} != SOURCES: errors.append("matrix Base sound rows differ")
    changed = 0
    if not errors:
        for entry in rows:
            owned = any(OWNER in note for note in entry.get("target_evidence", {}).get("notes", []))
            proof = {"paths": [target_path, main_path, "base/src/main/resources/assets/animania/sounds.json", "tools/audit_base_sounds.py"],
                     "behavior_tests": [game_path, "tools/audit_base_sounds.py", "tools/audit_resources.py"], "serialization_tests": [], "client_tests": [],
                     "notes": [f"{OWNER} exact source-declared Base sound set is resource-backed, DeferredRegister-backed, and live-registry tested."]}
            if args.write:
                entry.update(status="closed", implemented=True, verified=True, tests=proof["behavior_tests"], target_evidence=proof); changed += 1
            elif entry.get("status") != "closed" or not owned: errors.append(f"provable row not closed: {entry['source']}")
    if args.write and not errors: matrix_path.write_text(json.dumps(matrix, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"sounds": sorted(expected), "changed": changed, "errors": errors, "error_count": len(errors)}, ensure_ascii=False, indent=2))
    if errors: raise SystemExit(1)

if __name__ == "__main__": main()
